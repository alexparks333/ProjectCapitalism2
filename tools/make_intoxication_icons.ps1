# Effect icons for Tipsy and Drunk (player/Intoxication): 18x18, pixel art.
#  tipsy.png: an upright shot glass of amber liquor with a little bubble.
#  drunk.png: the same glass tipped over, spilling, with dizzy stars.
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\mob_effect'

function C($a, $r, $g, $b) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }
$outline = C 255 40 30 30
$glass   = C 255 200 225 235
$shine   = C 255 245 252 255
$amber   = C 255 232 160 60
$amberD  = C 255 176 104 32
$star    = C 255 255 230 110

function P($bmp, $x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt 18 -and $y -lt 18) { $bmp.SetPixel($x, $y, $c) } }

# a shot glass, 8 wide at the top, 6 at the bottom, rows 4..14; fill = liquor rows from $fillFrom down
function ShotGlass($bmp, $ox, $oy, $fillFrom) {
    for ($row = 0; $row -le 10; $row++) {
        $half = 4 - [int][math]::Floor($row / 5)
        $y = $oy + $row
        for ($dx = -$half; $dx -lt $half; $dx++) {
            $x = $ox + $dx
            if ($dx -eq -$half -or $dx -eq $half - 1 -or $row -eq 10) { P $bmp $x $y $outline }
            elseif ($row -ge $fillFrom) { P $bmp $x $y ($(if ($row -ge 8) { $amberD } else { $amber })) }
            else { P $bmp $x $y $glass }
        }
    }
    for ($row = 1; $row -le 8; $row++) { P $bmp ($ox - 2) ($oy + $row) $shine }
}

# Tipsy
$bmp = New-Object System.Drawing.Bitmap 18, 18
ShotGlass $bmp 9 4 4
P $bmp 10 6 $shine
P $bmp 8 2 $glass; P $bmp 11 1 $glass
$bmp.Save((Join-Path $out 'tipsy.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

# Drunk: draw the glass upright on its own, then rotate it 40 degrees onto the icon, plus a spill and stars
$glassBmp = New-Object System.Drawing.Bitmap 18, 18
ShotGlass $glassBmp 9 4 5
$bmp = New-Object System.Drawing.Bitmap 18, 18
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.TranslateTransform(9, 11)
$g.RotateTransform(-40)
$g.TranslateTransform(-9, -9)
$g.DrawImage($glassBmp, 0, 0)
$g.Dispose()
# spill pooling out of the rim, bottom left
foreach ($p in @(@(2, 15), @(3, 15), @(4, 15), @(5, 16), @(3, 16), @(4, 16), @(2, 14))) { P $bmp $p[0] $p[1] $amber }
P $bmp 1 15 $amberD; P $bmp 6 16 $amberD
# dizzy stars
foreach ($s in @(@(13, 2), @(16, 6), @(4, 2))) {
    $x = $s[0]; $y = $s[1]
    P $bmp $x $y $star; P $bmp ($x - 1) $y $star; P $bmp ($x + 1) $y $star; P $bmp $x ($y - 1) $star; P $bmp $x ($y + 1) $star
}
$bmp.Save((Join-Path $out 'drunk.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
$glassBmp.Dispose()
Write-Output 'tipsy.png, drunk.png written'

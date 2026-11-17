# Builds the shop signs that go over a double door (decor/ShopSignBlock, Town.md > Shop signs): a Minecraft wooden
# board (spruce planks in a dark oak log frame) two blocks wide and one tall, with the name standing out from it in
# chunky 3D block letters (a 5 x 7 pixel font, every row of a letter one raised box, gold / light blue...). Real geometry,
# Minecraft's own textures: block models models/block/<sign>_left.json and _right.json (each half its own model; a box
# crossing the middle is cut in two), and textures/item/<sign>.png (32 x 32, the same sign in pixels).
#   powershell -File tools/make_shop_signs.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$inv = [System.Globalization.CultureInfo]::InvariantCulture

# the pixel font: 5 wide, 7 tall, '#' is a block
$Glyphs = @{
    'A' = @('.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#')
    'B' = @('####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.')
    'H' = @('#...#', '#...#', '#...#', '#####', '#...#', '#...#', '#...#')
    'K' = @('#...#', '#..#.', '#.#..', '##...', '#.#..', '#..#.', '#...#')
    'L' = @('#....', '#....', '#....', '#....', '#....', '#....', '#####')
    'N' = @('#...#', '##..#', '##..#', '#.#.#', '#..##', '#..##', '#...#')
    'O' = @('.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.')
    'P' = @('####.', '#...#', '#...#', '####.', '#....', '#....', '#....')
    'S' = @('.####', '#....', '#....', '.###.', '....#', '....#', '####.')
    'T' = @('#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..')
    'W' = @('#...#', '#...#', '#...#', '#.#.#', '#.#.#', '##.##', '#...#')
}

# id, lines of text, the letters' texture and colour (for the icon)
$Signs = @(
    @{ id = 'town_hall_sign'; lines = @('TOWN', 'HALL'); tex = 'minecraft:block/gold_block'; color = @(250, 210, 70) },
    @{ id = 'bank_sign'; lines = @('BANK'); tex = 'minecraft:block/gold_block'; color = @(250, 210, 70) },
    @{ id = 'boat_shop_sign'; lines = @('BOAT', 'SHOP'); tex = 'minecraft:block/light_blue_concrete'; color = @(70, 170, 230) }
)
$BoardTex = 'minecraft:block/spruce_planks'
$FrameTex = 'minecraft:block/dark_oak_log'

# In "viewer" units: vx 0..32 left to right as you face the sign, vy 0..16 up; z toward the wall (16 = the wall).
$BoardZ = 14.5; $FrameZ = 13.5; $LetterZ = 12.6

function F([double]$v) { return [Math]::Round($v, 4).ToString($inv) }

# a box (viewer units) -> elements in the left and right models (the left half is vx 0..16; in its model x = 16 - vx)
function AddBox($left, $right, [double]$vx0, [double]$vx1, [double]$vy0, [double]$vy1, [double]$z0, [double]$z1, [string]$tex) {
    foreach ($half in @(@($left, 0.0), @($right, 16.0))) {
        $a = [Math]::Max($vx0, $half[1]); $b = [Math]::Min($vx1, $half[1] + 16)
        if ($b - $a -le 0.0001) { continue }
        $x0 = $half[1] + 16 - $b; $x1 = $half[1] + 16 - $a
        $half[0].Add((Element $x0 $x1 $vy0 $vy1 $z0 $z1 $tex)) | Out-Null
    }
}

function Element([double]$x0, [double]$x1, [double]$y0, [double]$y1, [double]$z0, [double]$z1, [string]$tex) {
    $n = "[$(F (16 - $x1)), $(F (16 - $y1)), $(F (16 - $x0)), $(F (16 - $y0))]"
    $u = "[$(F $x0), $(F $z0), $(F $x1), $(F $z1)]"
    $s = "[$(F $z0), $(F (16 - $y1)), $(F $z1), $(F (16 - $y0))]"
    return "    { `"from`": [$(F $x0), $(F $y0), $(F $z0)], `"to`": [$(F $x1), $(F $y1), $(F $z1)], `"faces`": {" +
        " `"north`": { `"uv`": $n, `"texture`": `"#$tex`" }," +
        " `"up`": { `"uv`": $u, `"texture`": `"#$tex`" }, `"down`": { `"uv`": $u, `"texture`": `"#$tex`" }," +
        " `"east`": { `"uv`": $s, `"texture`": `"#$tex`" }, `"west`": { `"uv`": $s, `"texture`": `"#$tex`" } } }"
}

function Model($elements, [string]$letterTex) {
    return "{`n  `"ambientocclusion`": false,`n  `"textures`": {`n    `"board`": `"$BoardTex`",`n    `"frame`": `"$FrameTex`",`n" +
        "    `"letter`": `"$letterTex`",`n    `"outline`": `"minecraft:block/coal_block`",`n    `"particle`": `"$BoardTex`"`n  },`n  `"elements`": [`n" + ($elements -join ",`n") + "`n  ]`n}`n"
}

$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism'
foreach ($sign in $Signs) {
    $left = New-Object System.Collections.ArrayList
    $right = New-Object System.Collections.ArrayList
    # the board, and its frame standing proud of it
    AddBox $left $right 0.5 31.5 0.5 15.5 $BoardZ 16 'board'
    AddBox $left $right 0.5 31.5 14.5 15.5 $FrameZ $BoardZ 'frame'
    AddBox $left $right 0.5 31.5 0.5 1.5 $FrameZ $BoardZ 'frame'
    AddBox $left $right 0.5 1.5 1.5 14.5 $FrameZ $BoardZ 'frame'
    AddBox $left $right 30.5 31.5 1.5 14.5 $FrameZ $BoardZ 'frame'
    # the letters: as big as fits inside the frame (vx 2.5 .. 29.5, vy 2 .. 14), lines centred
    $lines = $sign.lines
    $longest = ($lines | ForEach-Object { $_.Length } | Measure-Object -Maximum).Maximum
    $gap = 1.5
    $sx = 27.0 / ($longest * 6 - 1)
    $sy = 12.0 / ($lines.Count * 7 + ($lines.Count - 1) * $gap)
    $s = [Math]::Min($sx, $sy)
    $blockH = ($lines.Count * 7 + ($lines.Count - 1) * $gap) * $s
    $top = 8 + $blockH / 2
    for ($li = 0; $li -lt $lines.Count; $li++) {
        $line = $lines[$li]
        $lineW = ($line.Length * 6 - 1) * $s
        $x = 16 - $lineW / 2
        $rowTop = $top - $li * (7 + $gap) * $s
        foreach ($ch in $line.ToCharArray()) {
            $glyph = $Glyphs[[string]$ch]
            for ($r = 0; $r -lt 7; $r++) {
                $row = $glyph[$r]
                $c = 0
                while ($c -lt 5) {
                    if ($row[$c] -ne '#') { $c++; continue }
                    $start = $c
                    while ($c -lt 5 -and $row[$c] -eq '#') { $c++ }
                    # the letter row (a little fatter than its pixels) on a dark outline sticking out round it
                    $fat = 0.12 * $s; $rim = 0.42 * $s
                    $ax = $x + $start * $s; $bx = $x + $c * $s; $ay = $rowTop - ($r + 1) * $s; $by = $rowTop - $r * $s
                    AddBox $left $right ($ax - $rim) ($bx + $rim) ($ay - $rim) ($by + $rim) ($FrameZ - 0.35) $FrameZ 'outline'
                    AddBox $left $right ($ax - $fat) ($bx + $fat) ($ay - $fat) ($by + $fat) $LetterZ ($FrameZ - 0.35) 'letter'
                }
            }
            $x += 6 * $s
        }
    }
    Set-Content -Encoding ascii -Path (Join-Path $res ("models\block\" + $sign.id + "_left.json")) -Value (Model $left $sign.tex)
    Set-Content -Encoding ascii -Path (Join-Path $res ("models\block\" + $sign.id + "_right.json")) -Value (Model $right $sign.tex)

    # the icon: the same sign in pixels (frame, planks, letters a pixel each, a shadow under them)
    $icon = New-Object System.Drawing.Bitmap 32, 32, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frame = [System.Drawing.Color]::FromArgb(255, 60, 42, 24)
    for ($y = 6; $y -lt 26; $y++) {
        for ($x = 0; $x -lt 32; $x++) {
            $edge = $y -eq 6 -or $y -eq 25 -or $x -eq 0 -or $x -eq 31
            if ($edge) { $icon.SetPixel($x, $y, $frame); continue }
            $plank = [Math]::Floor(($y - 7) / 4)
            $shade = if ((($y - 7) % 4) -eq 3) { 0.8 } else { 1.0 - 0.06 * ($plank % 2) }
            $icon.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [int](116 * $shade), [int](86 * $shade), [int](52 * $shade)))
        }
    }
    $iy = 16 - [Math]::Floor(($lines.Count * 8 - 1) / 2)
    foreach ($line in $lines) {
        $ix = 16 - [Math]::Floor(($line.Length * 6 - 1) / 2)
        foreach ($ch in $line.ToCharArray()) {
            $glyph = $Glyphs[[string]$ch]
            for ($r = 0; $r -lt 7; $r++) {
                for ($c = 0; $c -lt 5; $c++) {
                    if ($glyph[$r][$c] -ne '#') { continue }
                    $px = $ix + $c; $py = $iy + $r
                    if ($px + 1 -lt 32 -and $py + 1 -lt 26) { $icon.SetPixel($px + 1, $py + 1, [System.Drawing.Color]::FromArgb(255, 40, 28, 16)) }
                }
            }
            for ($r = 0; $r -lt 7; $r++) {
                for ($c = 0; $c -lt 5; $c++) {
                    if ($glyph[$r][$c] -ne '#') { continue }
                    $k = if ($r -lt 2) { 1.0 } else { 0.85 }
                    $icon.SetPixel($ix + $c, $iy + $r, [System.Drawing.Color]::FromArgb(255, [int]($sign.color[0] * $k), [int]($sign.color[1] * $k), [int]($sign.color[2] * $k)))
                }
            }
            $ix += 6
        }
        $iy += 8
    }
    $icon.Save((Join-Path $res ("textures\item\" + $sign.id + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "saved $($sign.id): $($left.Count + $right.Count) boxes"
}

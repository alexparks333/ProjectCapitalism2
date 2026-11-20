# Butcher Shop Ice (farmer/ButcherShopIce, Farmer.md): vanilla ice made frostier, with seams showing the four blocks of
# ice it was pressed from and a frosted rim. Needs vanilla's block/ice.png: pass its path, or extract the client jar's
# assets to %LOCALAPPDATA%\Temp\vr (as the other scripts expect).
# Run: powershell -ExecutionPolicy Bypass -File tools\make_butcher_ice.ps1 [path\to\ice.png]
param([string]$Ice = (Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\block\ice.png'))
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block\butcher_shop_ice.png'

$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Ice))
$b = New-Object System.Drawing.Bitmap 16, 16
function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $c = $src.GetPixel($x, $y)
        # frostier: a touch whiter and a little more solid than plain ice
        $r = Clamp ($c.R * 0.85 + 40); $g = Clamp ($c.G * 0.85 + 44); $bl = Clamp ($c.B * 0.9 + 30)
        $a = Clamp ($c.A + 40)
        # the seams between the four pressed blocks (a dark line and a light line beside it)
        if ($x -eq 7 -or $y -eq 7) { $r = Clamp ($r - 22); $g = Clamp ($g - 16); $bl = Clamp ($bl - 6) }
        elseif ($x -eq 8 -or $y -eq 8) { $r = Clamp ($r + 18); $g = Clamp ($g + 16); $bl = Clamp ($bl + 10) }
        # a frosted rim
        if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $r = Clamp ($r + 26); $g = Clamp ($g + 24); $bl = Clamp ($bl + 14); $a = Clamp ($a + 20) }
        $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, $r, $g, $bl))
    }
}
$src.Dispose()
$b.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$b.Dispose()
Write-Output 'butcher_shop_ice.png'

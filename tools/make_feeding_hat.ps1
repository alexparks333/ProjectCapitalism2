# Draws the Feeding Hat (only from the Traveling Merchant): a woven straw sun hat with a little red feed bowl on top and
# a carrot poking out of it. Worn by a pet: textures/entity/pet_hat_feeding_hat.png (64x64, client/PetHatLayer's boxes:
# brim 12x1x12 at 0,0; crown 6x3x6 at 0,16; bowl 4x1x4 at 32,16; carrot 1x2x1 at 48,16). Item: textures/item/feeding_hat.png.
# Run: powershell -File tools\make_feeding_hat.ps1
Add-Type -AssemblyName System.Drawing
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 23
function P($b, $x, $y, $r, $g, $bl) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [Math]::Max(0, [Math]::Min(255, $r)), [Math]::Max(0, [Math]::Min(255, $g)), [Math]::Max(0, [Math]::Min(255, $bl)))) }
function Straw($b, $x0, $y0, $w, $h) { for ($y = $y0; $y -lt $y0 + $h; $y++) { for ($x = $x0; $x -lt $x0 + $w; $x++) { $weave = (($x + $y) % 3 -eq 0); $n = $rng.Next(-8, 9); if ($weave) { P $b $x $y (176 + $n) (140 + $n) (62 + $n) } else { P $b $x $y (222 + $n) (190 + $n) (100 + $n) } } } }
function Fill($b, $x0, $y0, $w, $h, $c) { for ($y = $y0; $y -lt $y0 + $h; $y++) { for ($x = $x0; $x -lt $x0 + $w; $x++) { $n = $rng.Next(-6, 7); P $b $x $y ($c[0] + $n) ($c[1] + $n) ($c[2] + $n) } } }
$e = New-Object System.Drawing.Bitmap 64, 64
Straw $e 0 0 48 13          # the brim's faces
Straw $e 0 16 24 9          # the crown's faces
for ($x = 0; $x -lt 24; $x++) { P $e $x 24 120 40 30 }   # a red band round the crown's foot
Fill $e 32 16 16 5 @(190, 44, 36)     # the bowl
Fill $e 36 16 4 4 @(150, 110, 60)     # feed in it
Fill $e 48 16 4 3 @(240, 130, 30)     # the carrot
P $e 49 16 70 150 50
$e.Save((Join-Path $tex 'entity\pet_hat_feeding_hat.png'), [System.Drawing.Imaging.ImageFormat]::Png); $e.Dispose()

$i = New-Object System.Drawing.Bitmap 16, 16
for ($y = 9; $y -lt 12; $y++) { for ($x = 1; $x -lt 15; $x++) { if ($y -eq 11 -and ($x -lt 2 -or $x -gt 13)) { continue }; $n = $rng.Next(-8, 9); if ((($x + $y) % 3) -eq 0) { P $i $x $y (176 + $n) (140 + $n) (62 + $n) } else { P $i $x $y (222 + $n) (190 + $n) (100 + $n) } } }
for ($y = 5; $y -lt 9; $y++) { for ($x = 4; $x -lt 12; $x++) { $n = $rng.Next(-8, 9); if ((($x + $y) % 3) -eq 0) { P $i $x $y (176 + $n) (140 + $n) (62 + $n) } else { P $i $x $y (222 + $n) (190 + $n) (100 + $n) } } }
for ($x = 4; $x -lt 12; $x++) { P $i $x 8 140 40 30 }
for ($x = 5; $x -lt 11; $x++) { P $i $x 4 190 44 36 }; for ($x = 6; $x -lt 10; $x++) { P $i $x 3 150 110 60 }
P $i 9 2 240 130 30; P $i 10 1 240 130 30; P $i 11 0 70 150 50
$i.Save((Join-Path $tex 'item\feeding_hat.png'), [System.Drawing.Imaging.ImageFormat]::Png); $i.Dispose()
'saved'

# Draws the item icons for the Blood Merchant's Halloween gear (Town.md > The Blood Merchant > Spooky), 16 x 16:
#  - ghost_sheet.png: a little white bedsheet ghost, two black eye holes, a ragged hem
#  - witchs_broom.png: a crooked wooden handle running corner to corner, a bundle of straw tied with a purple band
# (the decorations' icons are painted by tools/BloodDecorTex.cs; the sheet as worn is client/CosmeticHatLayer)
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'

function New-Icon { $b = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb); return ,$b }
function C($r,$g,$b) { [Drawing.Color]::FromArgb(255,$r,$g,$b) }
function Px($bmp,$x,$y,$c) { if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $bmp.SetPixel($x,$y,$c) } }

# ── the Ghost Sheet ──
$bmp = New-Icon
$white = C 244 244 248; $shade = C 200 202 214; $edge = C 150 152 168; $black = C 20 18 24
for ($y = 1; $y -le 14; $y++) {
  # a rounded head flaring out into the sheet
  $half = if ($y -le 2) { 2 + $y } elseif ($y -le 6) { 5 } else { [Math]::Min(7, 5 + [int](($y - 6) / 3)) }
  for ($x = 8 - $half; $x -lt 8 + $half; $x++) {
    $c = $white
    if ($x -eq 8 - $half) { $c = $edge } elseif ($x -eq 8 + $half - 1) { $c = $shade }
    Px $bmp $x $y $c
  }
}
# the ragged hem: bites out of the bottom row
foreach ($x in 2, 5, 9, 12) { Px $bmp $x 14 ([Drawing.Color]::Transparent); Px $bmp ($x + 1) 14 ([Drawing.Color]::Transparent) }
foreach ($x in 3, 10) { Px $bmp $x 13 $shade }
# eye holes and a little "oh" mouth
foreach ($p in @(5,5),@(5,6),@(6,5),@(6,6),@(9,5),@(9,6),@(10,5),@(10,6),@(7,9),@(8,9)) { Px $bmp $p[0] $p[1] $black }
$bmp.Save((Join-Path $out 'ghost_sheet.png'), [Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

# ── the Witch's Broom ──
$bmp = New-Icon
$wood = C 110 74 42; $woodL = C 150 104 60; $woodD = C 70 46 26; $straw = C 222 186 96; $strawD = C 170 134 60; $band = C 120 50 150
# the handle: from the top right corner down toward the bristles, a little crooked
for ($i = 0; $i -le 9; $i++) { $x = 15 - $i; $y = $i; if ($i -eq 4) { $x-- }; Px $bmp $x $y $wood; Px $bmp ($x - 1) $y $woodD; if ($i % 3 -eq 0) { Px $bmp $x $y $woodL } }
# the band
Px $bmp 6 9 $band; Px $bmp 5 10 $band; Px $bmp 6 10 $band; Px $bmp 7 10 $band
# the bristles fanning out to the bottom left
for ($i = 0; $i -lt 6; $i++) {
  for ($j = -$i; $j -le [Math]::Min(1, $i); $j++) {
    $x = 5 - $i + [Math]::Max(0, $j); $y = 11 + $i + [Math]::Min(0, $j)
    Px $bmp $x $y ($(if ((($x + $y) % 3) -eq 0) { $strawD } else { $straw }))
  }
}
Px $bmp 0 15 $strawD; Px $bmp 1 15 $straw; Px $bmp 0 14 $straw
$bmp.Save((Join-Path $out 'witchs_broom.png'), [Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
'saved'

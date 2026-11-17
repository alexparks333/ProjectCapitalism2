# Draws the Mayoral Door (town/MayoralDoorBlock): a grand dark mahogany door with gold trim and raised panels, a gold
# crest (a crown over the town seal) on its top half, a gold handle and kick plate on the bottom. Fully opaque (no
# windows). textures/block/mayoral_door_top.png, mayoral_door_bottom.png (16x16 each), and the item icon.
# Run: powershell -File tools\make_mayoral_door.ps1
Add-Type -AssemblyName System.Drawing
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 29
function P($b, $x, $y, $c) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [Math]::Max(0, [Math]::Min(255, $c[0])), [Math]::Max(0, [Math]::Min(255, $c[1])), [Math]::Max(0, [Math]::Min(255, $c[2])))) }
$wood = @(92, 34, 24); $dark = @(56, 20, 14); $gold = @(226, 180, 60); $goldDark = @(160, 116, 30)
function Door($top) {
  $b = New-Object System.Drawing.Bitmap 16, 16
  for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $n = $rng.Next(-6, 7); $g = if ($y % 5 -eq 0) { -10 } else { 0 }; P $b $x $y @(($wood[0] + $n + $g), ($wood[1] + $n + $g), ($wood[2] + $n + $g)) } }
  # gold frame round the edge
  for ($i = 0; $i -lt 16; $i++) { P $b $i 0 $gold; P $b 0 $i $gold; P $b 15 $i $goldDark; if ($top) { } else { P $b $i 15 $goldDark } }
  # a raised panel, darker inside, gold beading
  for ($y = 3; $y -lt 13; $y++) { for ($x = 3; $x -lt 13; $x++) { $n = $rng.Next(-5, 6); P $b $x $y @(($dark[0] + $n), ($dark[1] + $n), ($dark[2] + $n)) } }
  for ($i = 3; $i -lt 13; $i++) { P $b $i 3 $goldDark; P $b $i 12 $goldDark; P $b 3 $i $goldDark; P $b 12 $i $goldDark }
  if ($top) {
    # the crest: a little crown over a round seal
    foreach ($p in @(@(6,5), @(8,5), @(10,5), @(6,6), @(7,6), @(8,6), @(9,6), @(10,6))) { P $b $p[0] $p[1] $gold }
    for ($y = 8; $y -lt 12; $y++) { for ($x = 6; $x -lt 11; $x++) { if (($x -eq 6 -or $x -eq 10) -and ($y -eq 8 -or $y -eq 11)) { continue }; P $b $x $y $gold } }
    P $b 8 9 @(150, 20, 30); P $b 8 10 @(150, 20, 30)
  } else {
    # the handle, and a gold kick plate along the bottom
    P $b 13 5 $gold; P $b 13 6 $gold; P $b 14 5 $goldDark; P $b 14 6 $goldDark
    for ($x = 1; $x -lt 15; $x++) { P $b $x 13 $goldDark; P $b $x 14 $gold }
  }
  return $b
}
$t = Door $true; $t.Save((Join-Path $tex 'block\mayoral_door_top.png'), [System.Drawing.Imaging.ImageFormat]::Png); $t.Dispose()
$d = Door $false; $d.Save((Join-Path $tex 'block\mayoral_door_bottom.png'), [System.Drawing.Imaging.ImageFormat]::Png); $d.Dispose()
# the item: the whole door, small
$i = New-Object System.Drawing.Bitmap 16, 16
for ($y = 1; $y -lt 15; $y++) { for ($x = 4; $x -lt 12; $x++) { $n = $rng.Next(-6, 7); P $i $x $y @(($wood[0] + $n), ($wood[1] + $n), ($wood[2] + $n)) } }
for ($y = 1; $y -lt 15; $y++) { P $i 4 $y $gold; P $i 11 $y $goldDark }; for ($x = 4; $x -lt 12; $x++) { P $i $x 1 $gold; P $i $x 14 $goldDark }
P $i 7 3 $gold; P $i 8 3 $gold; P $i 7 4 $gold; P $i 8 4 $gold; P $i 10 8 $gold; for ($x = 5; $x -lt 11; $x++) { P $i $x 12 $gold }
$i.Save((Join-Path $tex 'item\mayoral_door.png'), [System.Drawing.Imaging.ImageFormat]::Png); $i.Dispose()
'saved'

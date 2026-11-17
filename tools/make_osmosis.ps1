# Draws the Bartender's Spring Water kit (Bartending.md > Spring Water): Spring Water (bottle and bucket: the Purified
# Water ones with the water turned a bright, clear spring blue and a few sparkles), the Osmosis Filter (charcoal-grey
# pleated cartridge) and Advanced Osmosis Filter (prismarine-teal), and the Advanced Osmosis Machine (steel panels and
# pipes, two glass tanks on top, a front window with the membrane; lit: the water inside glowing and bubbling).
# Run: powershell -File tools\make_osmosis.ps1
Add-Type -AssemblyName System.Drawing
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 47
function P($b, $x, $y, $c, $a = 255) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, [Math]::Max(0, [Math]::Min(255, $c[0])), [Math]::Max(0, [Math]::Min(255, $c[1])), [Math]::Max(0, [Math]::Min(255, $c[2])))) }
function Spring($src, $dst) {
  $b = [System.Drawing.Bitmap]::new((Join-Path $tex $src)); $o = New-Object System.Drawing.Bitmap $b.Width, $b.Height
  for ($y = 0; $y -lt $b.Height; $y++) { for ($x = 0; $x -lt $b.Width; $x++) {
    $c = $b.GetPixel($x, $y)
    if ($c.A -gt 0 -and $c.B -gt $c.R + 25) { $l = ($c.R + $c.G + $c.B) / 765.0; P $o $x $y @([int](60 + 120 * $l), [int](190 + 60 * $l), 255) $c.A } else { $o.SetPixel($x, $y, $c) }
  } }
  $b.Dispose()
  for ($k = 0; $k -lt 3; $k++) { for ($t = 0; $t -lt 40; $t++) { $x = $rng.Next($o.Width); $y = $rng.Next($o.Height); $c = $o.GetPixel($x, $y); if ($c.A -gt 0 -and $c.B -eq 255 -and $c.G -gt 180) { P $o $x $y @(255, 255, 255); break } } }
  $o.Save((Join-Path $tex $dst), [System.Drawing.Imaging.ImageFormat]::Png); $o.Dispose()
}
Spring 'item\purified_water.png' 'item\spring_water.png'
Spring 'item\purified_water_bucket.png' 'item\spring_water_bucket.png'

function DrawFilter($name, $body, $pleat, $cap) {
  $b = New-Object System.Drawing.Bitmap 16, 16
  for ($y = 2; $y -lt 14; $y++) { for ($x = 4; $x -lt 12; $x++) { $n = $rng.Next(-6, 7); $c = if (($x % 2) -eq 0) { $pleat } else { $body }; P $b $x $y @(($c[0] + $n), ($c[1] + $n), ($c[2] + $n)) } }
  for ($x = 3; $x -lt 13; $x++) { P $b $x 1 $cap; P $b $x 2 $cap; P $b $x 13 $cap; P $b $x 14 $cap }
  P $b 7 0 $cap; P $b 8 0 $cap
  $b.Save((Join-Path $tex "item\$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
}
DrawFilter 'osmosis_filter' @(60, 60, 64) @(40, 40, 44) @(200, 200, 210)
DrawFilter 'advanced_osmosis_filter' @(80, 170, 160) @(50, 130, 124) @(220, 230, 235)

function Img($fill, $noise) { $b = New-Object System.Drawing.Bitmap 16, 16; for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $n = $rng.Next(-$noise, $noise + 1); P $b $x $y @(($fill[0] + $n), ($fill[1] + $n), ($fill[2] + $n)) } }; return $b }
function Frame($b) { for ($i = 0; $i -lt 16; $i++) { P $b $i 0 @(90, 94, 100); P $b $i 15 @(60, 62, 68); P $b 0 $i @(90, 94, 100); P $b 15 $i @(60, 62, 68) } }
# side: steel with rivets and a copper pipe
$b = Img @(150, 154, 160) 6; Frame $b
foreach ($p in @(@(2,2), @(13,2), @(2,13), @(13,13))) { P $b $p[0] $p[1] @(100, 104, 110) }
for ($y = 1; $y -lt 15; $y++) { P $b 7 $y @(190, 110, 70); P $b 8 $y @(160, 90, 56) }
$b.Save((Join-Path $tex 'block\osmosis_machine_side.png'), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
# top: steel with two round glass tanks of water
$b = Img @(140, 144, 150) 6; Frame $b
foreach ($cx in @(4.5, 11.5)) { for ($y = 3; $y -lt 13; $y++) { for ($x = 1; $x -lt 15; $x++) { $d = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - 8) * ($y - 8)); if ($d -lt 3.2) { P $b $x $y @(90, 180, 230) } elseif ($d -lt 3.9) { P $b $x $y @(200, 220, 230) } } } }
$b.Save((Join-Path $tex 'block\osmosis_machine_top.png'), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
# front: a glass window onto the membrane (dark), lit: water glowing and bubbling
foreach ($lit in @($false, $true)) {
  $b = Img @(150, 154, 160) 6; Frame $b
  for ($y = 3; $y -lt 13; $y++) { for ($x = 3; $x -lt 13; $x++) { $n = $rng.Next(-8, 9); if ($lit) { P $b $x $y @((80 + $n), (200 + $n), 255) } else { P $b $x $y @((40 + $n), (60 + $n), (70 + $n)) } } }
  for ($y = 3; $y -lt 13; $y++) { P $b 8 $y @(220, 220, 225) }   # the membrane
  for ($i = 2; $i -lt 14; $i++) { P $b $i 2 @(80, 84, 90); P $b $i 13 @(80, 84, 90); P $b 2 $i @(80, 84, 90); P $b 13 $i @(80, 84, 90) }
  if ($lit) { foreach ($p in @(@(5,10), @(6,7), @(10,9), @(11,5), @(4,5))) { P $b $p[0] $p[1] @(240, 255, 255) } }
  $name = if ($lit) { 'osmosis_machine_front_on' } else { 'osmosis_machine_front' }
  $b.Save((Join-Path $tex "block\$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
}
'saved'

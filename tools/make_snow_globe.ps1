# Draws the Traveler's Snow Globe (town/TravelerBlocks.SnowGlobe, model block/travelers_snow_globe.json) at 32x32 for the
# fine detail: a polished walnut base and a gold band; a log cabin with a plank door, a wreath and two warm lit windows
# (one at the side), shingled red roof under thick snow, a stone chimney; snow-dusted pines; a snowman with coal eyes,
# a carrot nose and a red scarf; frosted glass with a shine and snowflakes drifting in it.
# Run: powershell -File tools\make_snow_globe.ps1
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$rng = New-Object System.Random 77
function New-Img($n) { New-Object System.Drawing.Bitmap $n, $n }
function Cl($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function P($b, $x, $y, $r, $g, $bl, $a = 255) { if ($x -ge 0 -and $x -lt $b.Width -and $y -ge 0 -and $y -lt $b.Height) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, (Cl $r), (Cl $g), (Cl $bl))) } }
function Rect($b, $x, $y, $w, $h, $c, $noise, $a = 255) { for ($j = 0; $j -lt $h; $j++) { for ($i = 0; $i -lt $w; $i++) { $n = $rng.Next(-$noise, $noise + 1); P $b ($x + $i) ($y + $j) ($c[0] + $n) ($c[1] + $n) ($c[2] + $n) $a } } }
function Save($b, $name) { $b.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# ── base: polished walnut (the sides use its top quarter: a carved groove and a bright edge)
$b = New-Img 16
Rect $b 0 0 16 16 @(78, 50, 30) 5
for ($x = 0; $x -lt 16; $x++) { P $b $x 0 138 96 58; P $b $x 1 104 70 40; P $b $x 2 60 38 22; P $b $x 3 44 28 16 }
for ($k = 0; $k -lt 10; $k++) { $y = $rng.Next(4, 16); for ($x = 0; $x -lt 16; $x++) { if ($rng.Next(3) -gt 0) { P $b $x $y 70 44 26 } } }   # grain
Save $b 'snow_globe_base'
# ── gold band
$b = New-Img 16; Rect $b 0 0 16 16 @(220, 176, 70) 10
for ($x = 0; $x -lt 16; $x += 3) { P $b $x 0 255 236 150 }
Save $b 'snow_globe_gold'
# ── snow: bright, a little blue in the hollows, sparkles
$b = New-Img 16; Rect $b 0 0 16 16 @(240, 246, 252) 4
for ($k = 0; $k -lt 14; $k++) { P $b $rng.Next(16) $rng.Next(16) 214 226 242 }
for ($k = 0; $k -lt 6; $k++) { P $b $rng.Next(16) $rng.Next(16) 255 255 255 }
Save $b 'snow_globe_snow'
# ── trunk / chimney
$b = New-Img 16; Rect $b 0 0 16 16 @(92, 62, 36) 8; for ($y = 0; $y -lt 16; $y++) { if ($y % 4 -eq 0) { for ($x = 0; $x -lt 16; $x++) { P $b $x $y 70 46 26 } } }
Save $b 'snow_globe_trunk'
$b = New-Img 16; Rect $b 0 0 16 16 @(120, 116, 112) 10
for ($y = 0; $y -lt 16; $y += 4) { for ($x = 0; $x -lt 16; $x++) { P $b $x $y 80 78 76 } }
for ($y = 0; $y -lt 16; $y++) { $off = if ([int]($y / 4) % 2 -eq 0) { 0 } else { 4 }; for ($x = $off; $x -lt 16; $x += 8) { P $b $x $y 80 78 76 } }
Rect $b 0 0 16 3 @(60, 50, 46) 6
Save $b 'snow_globe_chimney'

# ── the cabin's logs (32x32): round log ends at the corners, warm lit windows
function Logs($b) {
  for ($y = 0; $y -lt 32; $y++) {
    $row = $y % 5
    $c = if ($row -eq 0) { @(70, 44, 24) } elseif ($row -eq 1) { @(150, 104, 60) } else { @(128, 86, 48) }
    for ($x = 0; $x -lt 32; $x++) { $n = $rng.Next(-6, 7); P $b $x $y ($c[0] + $n) ($c[1] + $n) ($c[2] + $n) }
  }
  for ($y = 2; $y -lt 32; $y += 5) { foreach ($x in @(0, 1, 30, 31)) { P $b $x $y 186 140 90; P $b $x ($y + 1) 170 124 76 } }
}
function Window($b, $x0, $y0, $w, $h) {
  Rect $b ($x0 - 1) ($y0 - 1) ($w + 2) ($h + 2) @(60, 38, 20) 2
  for ($j = 0; $j -lt $h; $j++) { for ($i = 0; $i -lt $w; $i++) { $k = 1 - ([Math]::Abs($i - $w / 2) + [Math]::Abs($j - $h / 2)) / ($w + $h); P $b ($x0 + $i) ($y0 + $j) (230 + 25 * $k) (150 + 70 * $k) (50 + 60 * $k) } }
  for ($j = 0; $j -lt $h; $j++) { P $b ($x0 + [int]($w / 2)) ($y0 + $j) 70 44 24 }
  for ($i = 0; $i -lt $w; $i++) { P $b ($x0 + $i) ($y0 + [int]($h / 2)) 70 44 24 }
  for ($i = -1; $i -le $w; $i++) { P $b ($x0 + $i) ($y0 + $h + 1) 236 242 250 }   # snow on the sill
}
$b = New-Img 32; Logs $b
# the door: planks, a wreath, a brass handle, a snowy step
Rect $b 12 14 8 18 @(96, 60, 32) 5
for ($y = 14; $y -lt 32; $y++) { P $b 15 $y 70 44 24; P $b 18 $y 70 44 24; P $b 11 $y 50 32 18; P $b 20 $y 50 32 18 }
for ($x = 11; $x -lt 21; $x++) { P $b $x 13 50 32 18 }
P $b 18 23 230 190 90; P $b 18 24 200 160 70
foreach ($p in @(@(15, 16), @(16, 16), @(17, 16), @(14, 17), @(18, 17), @(14, 18), @(18, 18), @(15, 19), @(17, 19), @(16, 19))) { P $b $p[0] $p[1] 40 110 50 }
P $b 16 20 200 30 30; P $b 15 20 200 30 30
Window $b 3 15 6 6; Window $b 23 15 6 6
Save $b 'snow_globe_cabin_front'
$b = New-Img 32; Logs $b; Window $b 12 13 8 7
Save $b 'snow_globe_cabin_side'

# ── the roof (32x32): red shingles; the top under a thick blanket of snow, icicles along the edge (its bottom strip)
$b = New-Img 32
for ($y = 0; $y -lt 32; $y++) { for ($x = 0; $x -lt 32; $x++) {
  $shift = if ([int]($y / 4) % 2 -eq 0) { 0 } else { 3 }
  $edge = (($y % 4) -eq 3) -or ((($x + $shift) % 6) -eq 0)
  $n = $rng.Next(-8, 9); if ($edge) { P $b $x $y (110 + $n) (26 + $n) (22 + $n) } else { P $b $x $y (168 + $n) (44 + $n) (36 + $n) } } }
# snow over most of the top, a ragged edge
for ($x = 0; $x -lt 32; $x++) { $d = 18 + $rng.Next(0, 4); for ($y = 0; $y -lt $d; $y++) { $n = $rng.Next(-4, 5); P $b $x $y (240 + $n) (246 + $n) (252 + $n) } }
# the edge strip (the sides use rows 24-31): snow on top, red, icicles
for ($x = 0; $x -lt 32; $x++) { P $b $x 24 240 246 252; P $b $x 25 230 238 248; if ($x % 3 -eq 0) { P $b $x 30 210 230 250; P $b $x 31 190 220 246 } }
Save $b 'snow_globe_roof'

# ── pine needles (32x32): dark green, lighter tips, snow resting on the branches
$b = New-Img 32
for ($y = 0; $y -lt 32; $y++) { for ($x = 0; $x -lt 32; $x++) { $n = $rng.Next(-10, 11); P $b $x $y (30 + $n) (84 + $n) (44 + $n) } }
for ($k = 0; $k -lt 90; $k++) { P $b $rng.Next(32) $rng.Next(32) 58 124 66 }
for ($row = 3; $row -lt 32; $row += 8) { for ($x = 0; $x -lt 32; $x++) { if ($rng.Next(3) -gt 0) { P $b $x $row 238 244 250; if ($rng.Next(2) -eq 0) { P $b $x ($row + 1) 220 232 246 } } } }
Save $b 'snow_globe_pine'

# ── the snowman (32x32): white snow with soft shading, three coal buttons, a red scarf at the top
$b = New-Img 32
for ($y = 0; $y -lt 32; $y++) { for ($x = 0; $x -lt 32; $x++) { $shade = [Math]::Abs($x - 15.5) / 16 * 22; $n = $rng.Next(-3, 4); P $b $x $y (248 - $shade + $n) (250 - $shade + $n) (255 - $shade * 0.6 + $n) } }
Rect $b 0 0 32 5 @(196, 36, 36) 8; for ($x = 0; $x -lt 32; $x += 4) { P $b $x 2 150 20 20 }
foreach ($y in @(11, 18, 25)) { Rect $b 14 $y 4 4 @(30, 30, 34) 4 }
Save $b 'snow_globe_snowman'
$b = New-Img 32
for ($y = 0; $y -lt 32; $y++) { for ($x = 0; $x -lt 32; $x++) { $shade = [Math]::Abs($x - 15.5) / 16 * 22; $n = $rng.Next(-3, 4); P $b $x $y (248 - $shade + $n) (250 - $shade + $n) (255 - $shade * 0.6 + $n) } }
Rect $b 8 9 5 5 @(26, 26, 30) 3; Rect $b 19 9 5 5 @(26, 26, 30) 3              # coal eyes
for ($i = 0; $i -lt 7; $i++) { Rect $b (14 + $i) (16 + [int]($i / 3)) 3 (3 - [int]($i / 3)) @(240, 130, 30) 6 }   # carrot nose
foreach ($x in @(10, 13, 16, 19, 22)) { Rect $b $x 24 2 2 @(40, 40, 44) 3 }    # a coal smile
Save $b 'snow_globe_snowman_face'

# ── frosted glass (32x32): mostly clear, frost creeping up from the bottom corners, a bright shine, drifting flakes
$b = New-Img 32
for ($y = 0; $y -lt 32; $y++) { for ($x = 0; $x -lt 32; $x++) {
  $frost = [Math]::Max(0, ($y - 20) / 12.0) * [Math]::Max(0, ([Math]::Abs($x - 15.5) - 6) / 10.0)
  $a = 40 + [int](150 * $frost)
  P $b $x $y 220 238 252 $a } }
for ($i = 0; $i -lt 32; $i++) { P $b $i 0 236 246 255 140; P $b 0 $i 236 246 255 120; P $b 31 $i 236 246 255 120 }
for ($i = 3; $i -lt 14; $i++) { P $b 5 $i 255 255 255 200; P $b 6 ($i - 1) 255 255 255 150; P $b 7 ($i + 3) 255 255 255 90 }
for ($k = 0; $k -lt 26; $k++) { $x = $rng.Next(2, 30); $y = $rng.Next(2, 30); P $b $x $y 255 255 255 230; if ($rng.Next(3) -eq 0) { P $b ($x + 1) $y 255 255 255 160; P $b $x ($y + 1) 255 255 255 160 } }
Save $b 'snow_globe_glass'

'saved'

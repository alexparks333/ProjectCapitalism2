# Draws the Farmer's textures (Farmer.md): the Fertilizer and Super Fertilizer sacks, the Scythes, the Decomposter block
# (a hollow bin: slatted wooden sides, its compost, the compost bubbling while it works) and its menu
# (textures/gui/container/decomposter.png, 176x166). Slot positions must match farmer/DecomposterMenu and
# client/DecomposterScreen.
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function New([int]$w,[int]$h){ New-Object Drawing.Bitmap $w,$h,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Px($b,[int]$x,[int]$y,[int]$r,[int]$g,[int]$bl,[int]$a=255){ if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb($a,(C $r),(C $g),(C $bl))) } }
function Rect($b,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$bl){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ Px $b $x $y $r $g $bl } } }
function Save($b,[string]$path){ $full = Join-Path $root $path; New-Item -ItemType Directory -Force (Split-Path $full) | Out-Null; $b.Save($full,[Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# ── the sacks: a burlap bag tied at the neck, dark soil showing at the top, a green sprout (Super: a light grey gem
# of a sack with facets and glints, and a red tie; Fast: a sky-blue tie and a yellow lightning bolt) ──
function Sack([bool]$super,[bool]$fast=$false){
  $b = New 16 16; $rng = New-Object System.Random 4
  # body rows: (y, x0, x1)
  $rows = @(@(6,5,10),@(7,4,11),@(8,3,12),@(9,3,12),@(10,2,13),@(11,2,13),@(12,2,13),@(13,3,12),@(14,4,11))
  # burlap brown, or (Super) a light grey with a cool gemstone tint
  $base = if($super){ @(196,202,210) } else { @(176,140,92) }
  $edge = if($super){ @(92,98,110) } else { @(70,50,28) }
  foreach($r in $rows){ for($x=$r[1];$x -le $r[2];$x++){ $n=$rng.Next(-8,9); $shade = if($x -le $r[1]+1){ -22 } elseif($x -ge $r[2]-1){ -34 } else { 0 }
    $weave = if((($x + $r[0]) % 3) -eq 0){ -10 } else { 0 }
    if($super){ $n = [int]($n / 2); $weave = 0 }
    Px $b $x $r[0] ($base[0]+$n+$shade+$weave) ($base[1]+$n+$shade+$weave) ($base[2]+$n+$shade+$weave) } }
  # outline
  foreach($r in $rows){ Px $b ($r[1]-1) $r[0] $edge[0] $edge[1] $edge[2]; Px $b ($r[2]+1) $r[0] $edge[0] $edge[1] $edge[2] }
  for($x=4;$x -le 11;$x++){ Px $b $x 15 $edge[0] $edge[1] $edge[2] }
  # Super: cut-gem facets (a bright band down the upper left, a shine spot, white glints)
  if($super){
    foreach($p in @(@(4,8),@(4,9),@(5,7),@(3,10),@(3,11),@(4,10))){ Px $b $p[0] $p[1] 236 242 250 }
    Px $b 5 8 252 254 255
    foreach($p in @(@(11,13),@(10,14),@(12,12),@(11,12))){ Px $b $p[0] $p[1] 140 148 162 } }
  # the neck and its tie
  for($x=6;$x -le 9;$x++){ if($super){ Px $b $x 5 214 220 228 } else { Px $b $x 5 150 116 72 } }
  for($x=5;$x -le 10;$x++){ if($super){ Px $b $x 6 190 30 30 } elseif($fast){ Px $b $x 6 70 160 230 } else { Px $b $x 6 96 70 40 } }
  # soil poking out of the top, and a sprout
  for($x=5;$x -le 10;$x++){ Px $b $x 4 62 42 26 }
  for($x=6;$x -le 9;$x++){ Px $b $x 3 74 52 32 }
  Px $b 8 2 80 170 60; Px $b 8 1 90 190 70; Px $b 7 1 70 160 50; Px $b 9 0 110 210 80; Px $b 6 0 90 190 70
  if($fast){ # a printed lightning bolt
    foreach($p in @(@(9,8),@(8,9),@(9,9),@(7,10),@(8,10),@(9,10),@(10,10),@(8,11),@(9,11),@(7,12),@(8,12),@(7,13))){ Px $b $p[0] $p[1] 250 210 40 }
    Px $b 10 9 200 150 20; Px $b 6 13 200 150 20 }
  else { # a printed leaf on the sack
    foreach($p in @(@(7,10),@(8,9),@(8,10),@(9,9),@(8,11),@(7,11))){ Px $b $p[0] $p[1] 86 150 50 }
    Px $b 8 12 60 110 40 }
  if($super){ foreach($p in @(@(11,9),@(6,13),@(10,11))){ Px $b $p[0] $p[1] 255 255 255 }
    Px $b 6 3 250 60 50; Px $b 9 4 230 40 30 }
  return $b
}
Save (Sack $false) 'item\fertilizer.png'
Save (Sack $true) 'item\super_fertilizer.png'
Save (Sack $false $true) 'item\fast_fertilizer.png'

# ── the Scythe: a long wooden snath from bottom right up to the top, the iron blade curving away to the left ──
# (the Diamond Scythe: the same, its blade diamond blue)
function Scythe([int[]]$edge,[int[]]$face,[int[]]$back,[int[]]$tip){
  $b = New 16 16
  for($i=0;$i -le 12;$i++){ $x = 13 - [int]($i * 0.35); $y = 15 - $i; Px $b $x $y 120 84 46; Px $b ($x+1) $y 84 58 30 }
  # a grip half way up
  Px $b 11 9 70 48 26; Px $b 12 9 70 48 26
  # the blade: a curve from the top of the snath out to the left, light on its edge
  $blade = @(@(10,2),@(9,1),@(8,1),@(7,1),@(6,1),@(5,2),@(4,2),@(3,3),@(2,4),@(1,5))
  foreach($p in $blade){ Px $b $p[0] $p[1] $edge[0] $edge[1] $edge[2]; Px $b $p[0] ($p[1]+1) $face[0] $face[1] $face[2] }
  foreach($p in @(@(9,0),@(8,0),@(7,0),@(6,0))){ Px $b $p[0] $p[1] $back[0] $back[1] $back[2] }
  Px $b 0 6 $tip[0] $tip[1] $tip[2]; Px $b 11 2 90 94 104; Px $b 11 3 90 94 104
  return $b }
Save (Scythe @(196,202,212) @(140,146,158) @(90,94,104) @(230,236,244)) 'item\scythe.png'
Save (Scythe @(150,240,232) @(44,182,176) @(22,110,108) @(222,255,252)) 'item\diamond_scythe.png'

# ── the Decomposter block ──
$rng = New-Object System.Random 11
function Slats(){
  $b = New 16 16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-7,8)
    $gap = (($y % 5) -eq 4)
    if($gap){ Px $b $x $y (40+$n) (28+$n) (16+$n) } else { $grain = if((($x*3 + $y) % 7) -eq 0){ -12 } else { 0 }
      Px $b $x $y (128+$n+$grain) (92+$n+$grain) (54+$n+$grain) } } }
  # corner posts
  for($y=0;$y -lt 16;$y++){ foreach($x in @(0,1,14,15)){ $n=$rng.Next(-5,6); Px $b $x $y (96+$n) (66+$n) (36+$n) } ; Px $b 0 $y 66 44 22; Px $b 15 $y 66 44 22 }
  return $b }
Save (Slats) 'block\decomposter_side.png'
function Top([bool]$working){
  $b = New 16 16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-10,11)
    if($x -le 1 -or $y -le 1 -or $x -ge 14 -or $y -ge 14){ Px $b $x $y (110+$n) (78+$n) (44+$n) }
    else { $bit = $rng.Next(0,9); if($bit -eq 0){ Px $b $x $y (90+$n) (70+$n) (30+$n) } elseif($bit -eq 1){ Px $b $x $y (70+$n) (96+$n) (40+$n) } else { Px $b $x $y (58+$n) (40+$n) (24+$n) } } } }
  if($working){ foreach($p in @(@(4,5),@(10,4),@(7,9),@(11,11),@(4,11))){ Px $b $p[0] $p[1] 150 220 90; Px $b ($p[0]+1) $p[1] 110 190 70; Px $b $p[0] ($p[1]+1) 110 190 70 } }
  return $b }
# (only its wooden rim shows: the tops of the bin's walls)
Save (Top $false) 'block\decomposter_top.png'
# the compost it fills with (block/decomposter), and the same bubbling while it works: 8 frames of bubbles swelling
# and popping (block/decomposter_working, animated by its .mcmeta)
function Compost([System.Drawing.Bitmap]$b,[int]$oy,[int]$seed){
  $r = New-Object System.Random $seed
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$r.Next(-10,11); $bit = $r.Next(0,9)
    if($bit -eq 0){ Px $b $x ($oy+$y) (90+$n) (70+$n) (30+$n) } elseif($bit -eq 1){ Px $b $x ($oy+$y) (70+$n) (96+$n) (40+$n) } else { Px $b $x ($oy+$y) (58+$n) (40+$n) (24+$n) } } } }
$b = New 16 16; Compost $b 0 21; Save $b 'block\decomposter_compost.png'
$frames = 8; $b = New 16 (16*$frames)
# bubbles: (x, y, the frame it starts on); each grows for 3 frames, then pops
$bubbles = @(@(4,5,0),@(10,4,2),@(7,9,4),@(11,11,1),@(4,11,5),@(8,3,6),@(12,7,3))
for($f=0;$f -lt $frames;$f++){
  Compost $b (16*$f) 21
  foreach($p in $bubbles){ $age = ($f - $p[2] + $frames) % $frames; $x=$p[0]; $y=16*$f+$p[1]
    if($age -eq 0){ Px $b $x $y 110 190 70 }
    elseif($age -eq 1){ Px $b $x $y 150 220 90; Px $b ($x+1) $y 110 190 70; Px $b $x ($y+1) 110 190 70 }
    elseif($age -eq 2){ Px $b $x $y 190 240 130; Px $b ($x+1) $y 150 220 90; Px $b $x ($y+1) 150 220 90; Px $b ($x+1) ($y+1) 110 190 70; Px $b ($x-1) $y 110 190 70; Px $b $x ($y-1) 110 190 70 }
    elseif($age -eq 3){ Px $b ($x-1) ($y-1) 150 220 90; Px $b ($x+2) ($y-1) 150 220 90; Px $b ($x-1) ($y+2) 150 220 90; Px $b ($x+2) ($y+2) 150 220 90 } } }
Save $b 'block\decomposter_compost_working.png'
Set-Content -Path (Join-Path $root 'block\decomposter_compost_working.png.mcmeta') -Value '{ "animation": { "frametime": 3 } }' -Encoding ascii
$b = New 16 16; for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-6,7); Px $b $x $y (92+$n) (64+$n) (34+$n) } }; Save $b 'block\decomposter_bottom.png'

# ── the menu (176x166): a beveled frame of nailed planks; a dark compost title band with sprouts; a soil bed for the
# work (grass tufts along its top, a worm, a mushroom) with the three ingredient slots in a little wooden crate, a dark
# trough of an arrow (the screen fills it green), and the Fertilizer slot in a leafy wreath with flowers ──
$W = 176; $H = 166; $g = New 256 256; $rng = New-Object System.Random 5
# planks 10px tall, each its own tone, with grain, a joint and nails
$tones = @(0,-10,6,-4,10,-14,2,-8,8,-2,4,-12,0,-6,6,-10,2)
for($y=0;$y -lt $H;$y++){ $p = [int][Math]::Floor($y / 10); $t = $tones[$p % $tones.Count]; $joint = 24 + (($p * 53) % 128)
  for($x=0;$x -lt $W;$x++){ $n=$rng.Next(-4,5)
    $grain = if((($y % 10) -eq 3 -or ($y % 10) -eq 6) -and (($x + $p*11) % 19) -lt 9){ -9 } else { 0 }
    $v = $n + $t + $grain
    if(($y % 10) -eq 9){ $v -= 34 } elseif(($y % 10) -eq 0){ $v += 14 } elseif($x -eq $joint){ $v -= 30 } elseif($x -eq $joint+1){ $v += 10 }
    Px $g $x $y (172+$v) (134+$v) (86+$v) }
  if(($y % 10) -eq 2 -or ($y % 10) -eq 6){ Px $g ($joint-3) $y 70 66 62; Px $g ($joint+4) $y 70 66 62 }
  if(($y % 10) -eq 1 -or ($y % 10) -eq 5){ Px $g ($joint-3) $y 150 148 144; Px $g ($joint+4) $y 150 148 144 } }
# the frame: a dark outline, a light bevel top-left, a dark bevel bottom-right, rounded corners
for($x=0;$x -lt $W;$x++){ Px $g $x 0 40 26 12; Px $g $x ($H-1) 40 26 12; Px $g $x 1 226 196 140; Px $g $x 2 206 172 118; Px $g $x ($H-2) 92 64 34; Px $g $x ($H-3) 120 88 50 }
for($y=0;$y -lt $H;$y++){ Px $g 0 $y 40 26 12; Px $g ($W-1) $y 40 26 12; Px $g 1 $y 226 196 140; Px $g 2 $y 206 172 118; Px $g ($W-2) $y 92 64 34; Px $g ($W-3) $y 120 88 50 }
foreach($c in @(@(0,0),@(1,0),@(0,1),@(174,0),@(175,0),@(175,1),@(0,164),@(0,165),@(1,165),@(175,164),@(175,165),@(174,165))){ $g.SetPixel($c[0],$c[1],[Drawing.Color]::FromArgb(0,0,0,0)) }
Px $g 1 1 40 26 12; Px $g 174 1 40 26 12; Px $g 1 164 40 26 12; Px $g 174 164 40 26 12
# corner nails
foreach($c in @(@(5,5),@(169,5),@(5,159),@(169,159))){ Px $g $c[0] $c[1] 180 178 172; Px $g ($c[0]+1) $c[1] 120 118 112; Px $g $c[0] ($c[1]+1) 120 118 112; Px $g ($c[0]+1) ($c[1]+1) 70 68 64 }

# compost soil with bits in it (rotting leaves, twigs, green specks)
function Soil([int]$x0,[int]$y0,[int]$x1,[int]$y1){
  for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ $n=$rng.Next(-7,8); $bit=$rng.Next(0,22)
    if($bit -eq 0){ Px $g $x $y (118+$n) (74+$n) (30+$n) } elseif($bit -eq 1){ Px $g $x $y (74+$n) (104+$n) (40+$n) } elseif($bit -eq 2){ Px $g $x $y (96+$n) (82+$n) (60+$n) }
    else { Px $g $x $y (56+$n) (38+$n) (22+$n) } } } }
# the title band
for($x=8;$x -le 167;$x++){ Px $g $x 4 34 22 10; Px $g $x 16 34 22 10 }
for($y=4;$y -le 16;$y++){ Px $g 8 $y 34 22 10; Px $g 167 $y 34 22 10 }
Soil 9 5 166 15
for($x=9;$x -le 166;$x++){ Px $g $x 5 30 20 10 }
# sprouts at its ends
foreach($s in @(12,16,160,164)){ Px $g $s 15 70 140 50; Px $g $s 14 80 160 60; Px $g ($s-1) 13 100 190 70; Px $g ($s+1) 13 90 180 60 }

# the soil bed for the work: a dark wooden rim, compost inside, an inner shadow along its top
$bx0 = 8; $by0 = 22; $bx1 = 167; $by1 = 67
for($x=$bx0;$x -le $bx1;$x++){ Px $g $x $by0 46 30 14; Px $g $x $by1 214 184 130 }
for($y=$by0;$y -le $by1;$y++){ Px $g $bx0 $y 46 30 14; Px $g $bx1 $y 214 184 130 }
Soil ($bx0+1) ($by0+1) ($bx1-1) ($by1-1)
for($x=$bx0+1;$x -lt $bx1;$x++){ Px $g $x ($by0+1) 30 20 10; Px $g $x ($by0+2) 42 28 16 }
# grass tufts poking up over the rim
foreach($tx in @(14,19,38,96,104,118,160)){ Px $g $tx ($by0-1) 76 150 52; Px $g $tx ($by0-2) 96 176 62; Px $g ($tx+1) ($by0-1) 70 140 48; Px $g ($tx-1) ($by0-1) 86 164 58; Px $g ($tx+1) ($by0-3) 110 196 74 }
# a worm (lower left) and a little red mushroom (lower right)
foreach($w in @(@(12,62),@(13,61),@(14,61),@(15,62),@(16,63),@(17,63),@(18,62))){ Px $g $w[0] $w[1] 206 120 120 }
Px $g 18 61 230 150 150; Px $g 12 63 170 96 96
Px $g 160 64 226 216 196; Px $g 160 63 226 216 196; Px $g 160 62 226 216 196
foreach($m in @(@(158,61),@(159,61),@(160,61),@(161,61),@(162,61),@(159,60),@(160,60),@(161,60))){ Px $g $m[0] $m[1] 196 40 36 }
Px $g 159 60 250 240 230; Px $g 161 61 250 240 230; Px $g 157 62 140 30 26; Px $g 163 62 140 30 26
# a few pebbles
foreach($p in @(@(100,60),@(116,30),@(30,62),@(150,27))){ Px $g $p[0] $p[1] 128 122 112; Px $g ($p[0]+1) $p[1] 98 92 84 }

# a slot: a dark frame top-left, light bottom-right, a warm inside
function Slot([int]$x,[int]$y){ # the slot's item spot is at x,y (16x16); the frame around it
  Rect $g ($x) ($y) ($x+15) ($y+15) 100 74 44
  for($i=0;$i -le 15;$i++){ Px $g ($x+$i) $y 80 58 34; Px $g $x ($y+$i) 86 62 36 }
  for($i=-1;$i -le 16;$i++){ Px $g ($x+$i) ($y-1) 52 34 16; Px $g ($x-1) ($y+$i) 52 34 16; Px $g ($x+$i) ($y+16) 226 196 142; Px $g ($x+16) ($y+$i) 226 196 142 } }
# the crate holding the ingredient slots: a plank box with dark slats and corner posts
Rect $g 20 29 87 56 132 94 52
for($x=20;$x -le 87;$x++){ Px $g $x 29 60 40 20; Px $g $x 56 60 40 20; Px $g $x 30 186 146 92; Px $g $x 55 108 76 40 }
for($y=29;$y -le 56;$y++){ foreach($x in @(20,87)){ Px $g $x $y 60 40 20 }; foreach($x in @(21,22)){ Px $g $x $y 120 86 48 }; foreach($x in @(85,86)){ Px $g $x $y 120 86 48 } }
foreach($c in @(@(21,31),@(85,31),@(21,53),@(85,53))){ Px $g $c[0] $c[1] 170 168 160 }
foreach($i in 0..2){ Slot (26 + 20*$i) 35 }
# the arrow: a dark trough the screen fills green (inside 92..121 x 39..46), a wooden head
Rect $g 91 38 122 47 40 28 14
Rect $g 92 39 121 46 26 18 10
for($x=92;$x -le 121;$x++){ Px $g $x 46 46 32 18 }
for($i=0;$i -le 5;$i++){ for($y=(37+$i);$y -le (48-$i);$y++){ Px $g (123+$i) $y 40 28 14 } }
for($i=0;$i -le 3;$i++){ for($y=(39+$i);$y -le (46-$i);$y++){ Px $g (123+$i) $y 150 110 62 } }
# the Fertilizer slot in a wreath of leaves with a few flowers
$cx = 141.5; $cy = 42.5
for($a=0;$a -lt 360;$a+=15){ $r = [Math]::PI * $a / 180; $rr = 13.5 + 0.8 * [Math]::Sin($a / 9.0)
  $lx = [int]($cx + $rr * [Math]::Cos($r)); $ly = [int]($cy + $rr * [Math]::Sin($r)); $tone = if(($a / 15) % 2 -eq 0){ 0 } else { -22 }
  Px $g $lx $ly (74+$tone) (150+$tone) (48+$tone); Px $g ($lx+1) $ly (60+$tone) (126+$tone) (40+$tone); Px $g $lx ($ly+1) (60+$tone) (126+$tone) (40+$tone); Px $g ($lx+1) ($ly+1) (40+$tone) (96+$tone) (30+$tone)
  if(($a % 45) -eq 0){ Px $g $lx $ly (130+$tone) (206+$tone) (90+$tone) } }
foreach($f in @(@(128,30,250,214,60),@(154,31,240,130,170),@(127,54,240,130,170),@(155,54,250,214,60),@(141,27,250,250,240))){
  Px $g $f[0] $f[1] $f[2] $f[3] $f[4]; Px $g ($f[0]-1) $f[1] $f[2] ($f[3]-40) ($f[4]-30); Px $g ($f[0]+1) $f[1] $f[2] ($f[3]-40) ($f[4]-30); Px $g $f[0] ($f[1]-1) $f[2] ($f[3]-40) ($f[4]-30); Px $g $f[0] ($f[1]+1) $f[2] ($f[3]-40) ($f[4]-30); Px $g $f[0] $f[1] 250 240 120 }
Slot 134 35
# inventory: its slots sit on a slightly darker shelf
for($y=82;$y -le 159;$y++){ for($x=6;$x -le 169;$x++){ $c = $g.GetPixel($x,$y); Px $g $x $y ($c.R-14) ($c.G-12) ($c.B-8) } }
for($x=6;$x -le 169;$x++){ Px $g $x 81 92 64 34 }
for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Slot (8 + 18*$c) (84 + 18*$r) } }
for($c=0;$c -lt 9;$c++){ Slot (8 + 18*$c) 142 }
Save $g 'gui\container\decomposter.png'

# ── Fertilized Farmland (farmer/FertilizedFarmlandBlock): vanilla farmland's top (dry and moist, read from the
# Minecraft jar the build unpacks) richer and darker, sprinkled with what is on it: green sprout specks (Yield),
# silver glints (Super Yield) and yellow and sky-blue specks (Fast) ──
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = Join-Path $PSScriptRoot '..\build\neoForm\neoFormJoined26.3-1\raw.jar'
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $jar))
function Vanilla([string]$name){ $e = $zip.GetEntry("assets/minecraft/textures/block/$name.png"); $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); $m.Position = 0; return [Drawing.Bitmap]::FromStream($m) }
$looks = @{
  'yield' = @(@(96,176,60),@(130,206,80));
  'super_yield' = @(@(200,206,216),@(250,252,255));
  'fast' = @(@(240,200,40),@(90,180,240))
}
$specks = @(@(2,2),@(7,1),@(12,3),@(4,6),@(10,7),@(14,9),@(1,10),@(6,12),@(11,13),@(3,14),@(9,4),@(13,15))
foreach($look in $looks.Keys){ foreach($moist in @($false,$true)){
  $src = Vanilla $(if($moist){ 'farmland_moist' } else { 'farmland' })
  $b = New 16 16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $src.GetPixel($x,$y); Px $b $x $y ([int]($c.R*0.82)) ([int]($c.G*0.8)) ([int]($c.B*0.74)) } }
  $i = 0
  foreach($p in $specks){ $col = $looks[$look][$i % 2]; Px $b $p[0] $p[1] $col[0] $col[1] $col[2]
    if(($i % 3) -eq 0){ Px $b ($p[0]+1) $p[1] ([int]($col[0]*0.7)) ([int]($col[1]*0.7)) ([int]($col[2]*0.7)) }
    $i++ }
  $src.Dispose()
  Save $b ('block\fertilized_farmland_' + $look + $(if($moist){ '_moist' } else { '' }) + '.png') } }
$zip.Dispose()
'saved'

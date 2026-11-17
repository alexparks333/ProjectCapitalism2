# Draws the Designer's textures (Designer.md):
#  - gui/container/sewing_machine.png (176x222): black enamel with gold scrollwork, a dark green cloth bed in the middle
#    (client/SewingMachineScreen draws the slots, the patch and the seams on it), the inventory on walnut
#  - gui/container/designer_table.png (176x222): plum velvet with gold trim, a green cutting mat (with a grid and a
#    ruler) under the row of patch slots, the inventory on walnut (client/DesignerTableScreen draws the slots)
#  - item/: food_satchel, seed_satchel, leather_patch, strap_patch, pantry_patch, seed_pocket_patch,
#    diamond_sewing_needle (the Backpack's icon is tools/make_backpack.ps1)
#  - block/: designer_table_top (cutting mat), designer_table_side, designer_table_wood, designer_table_cloth,
#    sewing_machine_enamel, sewing_machine_metal, sewing_machine_wood, sewing_machine_spool
#  - entity/bag_backpack, bag_food_satchel, bag_seed_satchel (64x32, client/BagLayer's UV layout: the body box 7x10x3
#    at 0,0 (its outer face at 13,3), the flap 7x3x4 at 0,14, the shoulder straps at 24,0 and 40,0)
# Run from anywhere: powershell -ExecutionPolicy Bypass -File tools\make_designer_textures.ps1
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 26
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function NewBmp([int]$w,[int]$h){ New-Object Drawing.Bitmap $w,$h,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function P($bmp,[int]$x,[int]$y,[int[]]$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $c[0]),(C $c[1]),(C $c[2]))) } }
function Fill($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int]$noise){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ $n=$rng.Next(-$noise,$noise+1); P $bmp $x $y @(($c[0]+$n),($c[1]+$n),($c[2]+$n)) } } }
function Shade([int[]]$c,[double]$f){ @((C ($c[0]*$f)),(C ($c[1]*$f)),(C ($c[2]*$f))) }
function Save($bmp,[string]$rel){ $bmp.Save((Join-Path $res $rel),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
function Frame($bmp,[int]$x0,[int]$y0,[int]$s,[int[]]$in,[int[]]$dark,[int[]]$light){
  Fill $bmp ($x0+1) ($y0+1) ($x0+$s-2) ($y0+$s-2) $in 0
  for($i=0;$i -lt $s;$i++){ P $bmp ($x0+$i) $y0 $dark; P $bmp $x0 ($y0+$i) $dark; P $bmp ($x0+$i) ($y0+$s-1) $light; P $bmp ($x0+$s-1) ($y0+$i) $light } }
function Inventory($bmp,[int]$top,[int[]]$in,[int[]]$dark,[int[]]$light){
  for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Frame $bmp (7 + 18*$c) ($top - 1 + 18*$r) 18 $in $dark $light } }
  for($c=0;$c -lt 9;$c++){ Frame $bmp (7 + 18*$c) ($top + 57) 18 $in $dark $light } }
# A rounded panel with a dark outline, a highlight along the top-left and a shadow along the bottom-right, the color
# getting a little darker towards the bottom
function Panel($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int]$noise){
  $H = $y1 - $y0
  for($y=$y0;$y -le $y1;$y++){ $t = ($y - $y0) / [Math]::Max(1,$H); for($x=$x0;$x -le $x1;$x++){
    $corner = (($x -eq $x0 -or $x -eq $x1) -and ($y -eq $y0 -or $y -eq $y1)); if($corner){ continue }
    $n=$rng.Next(-$noise,$noise+1); P $bmp $x $y @(($c[0]-12*$t+$n),($c[1]-10*$t+$n),($c[2]-8*$t+$n)) } }
  $dk = Shade $c 0.3; $hi = Shade $c 1.35; $lo = Shade $c 0.6
  for($x=$x0+1;$x -lt $x1;$x++){ P $bmp $x $y0 $dk; P $bmp $x $y1 $dk; P $bmp $x ($y0+1) $hi; P $bmp $x ($y1-1) $lo }
  for($y=$y0+1;$y -lt $y1;$y++){ P $bmp $x0 $y $dk; P $bmp $x1 $y $dk; P $bmp ($x0+1) $y $hi; P $bmp ($x1-1) $y $lo } }
# A gold line with little diamonds along it (the scrollwork)
function GoldLine($bmp,[int]$x0,[int]$x1,[int]$y,[int[]]$gold,[int[]]$dark){
  for($x=$x0;$x -le $x1;$x++){ P $bmp $x $y $gold; if((($x - $x0) % 12) -eq 6){ P $bmp $x ($y-1) $gold; P $bmp $x ($y+1) $gold; P $bmp ($x-1) $y $dark; P $bmp ($x+1) $y $dark } } }
function Walnut($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1){
  for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ $grain = [Math]::Sin(($x + 3*[Math]::Sin($y/5.0)) / 2.5) * 6; $n=$rng.Next(-3,4)
    P $bmp $x $y @((86+$grain+$n),(58+$grain*0.7+$n),(38+$grain*0.5+$n)) } } }

$gold = @(226,188,92); $goldDk = @(142,110,38)

# ── Sewing Machine menu ──
$sm = NewBmp 256 256
Panel $sm 0 0 175 221 @(30,40,35) 3
# gold pinstripe border and scrolls in the corners
for($x=4;$x -le 171;$x++){ P $sm $x 4 $goldDk; P $sm $x 125 $goldDk }
for($y=4;$y -le 125;$y++){ P $sm 4 $y $goldDk; P $sm 171 $y $goldDk }
foreach($pt in @(@(4,4),@(171,4),@(4,125),@(171,125))){ Fill $sm ($pt[0]-1) ($pt[1]-1) ($pt[0]+1) ($pt[1]+1) $gold 0 }
GoldLine $sm 34 140 16 $gold $goldDk
# the cloth bed in the middle where the patch and its seams are drawn (round 88,50)
Fill $sm 34 20 142 84 @(24,52,40) 3
for($i=34;$i -le 142;$i++){ if(($i % 4) -lt 2){ P $sm $i 20 @(70,110,88); P $sm $i 84 @(70,110,88) } }
for($i=20;$i -le 84;$i++){ if(($i % 4) -lt 2){ P $sm 34 $i @(70,110,88); P $sm 142 $i @(70,110,88) } }
# weave on the cloth
for($y=22;$y -le 82;$y+=3){ for($x=36;$x -le 140;$x++){ if((($x + $y) % 7) -eq 0){ P $sm $x $y @(32,62,48) } } }
# a strip under the material slots, and the Sew / Unpick shelf
Fill $sm 36 88 136 111 @(20,28,24) 2
for($x=36;$x -le 136;$x++){ P $sm $x 88 $goldDk }
# the right column: a little spool by the needle slot, the output plate
Fill $sm 146 22 171 46 @(22,30,26) 2
Fill $sm 160 50 166 51 @(120,82,46) 0; Fill $sm 161 52 165 60 @(236,226,200) 0; Fill $sm 160 61 166 62 @(120,82,46) 0
for($y=53;$y -le 59;$y+=2){ P $sm 161 $y @(206,192,160) }
Fill $sm 144 86 171 115 @(22,30,26) 2
# the walnut inventory area
Walnut $sm 3 128 172 218
for($x=3;$x -le 172;$x++){ P $sm $x 127 $gold }
Inventory $sm 140 @(40,28,18) @(22,14,8) @(150,112,72)
Save $sm 'gui\container\sewing_machine.png'

# ── Designer Table menu ──
$dt = NewBmp 256 256
Panel $dt 0 0 175 221 @(78,34,62) 3
# velvet: a soft diagonal sheen
for($y=2;$y -le 125;$y++){ for($x=2;$x -le 173;$x++){ $s = [Math]::Sin(($x + $y) / 9.0); if($s -gt 0.92){ $c = $dt.GetPixel($x,$y); P $dt $x $y @(($c.R+14),($c.G+8),($c.B+12)) } } }
for($x=4;$x -le 171;$x++){ P $dt $x 4 $goldDk; P $dt $x 125 $goldDk }
for($y=4;$y -le 125;$y++){ P $dt 4 $y $goldDk; P $dt 171 $y $goldDk }
foreach($pt in @(@(4,4),@(171,4),@(4,125),@(171,125))){ Fill $dt ($pt[0]-1) ($pt[1]-1) ($pt[0]+1) ($pt[1]+1) $gold 0 }
GoldLine $dt 8 168 46 $gold $goldDk
# the cutting mat under the patch row (slots at 27 + 20i, 62): green with a grid, a ruler along its top
Fill $dt 18 50 158 86 @(34,88,58) 2
for($x=18;$x -le 158;$x++){ for($y=50;$y -le 86;$y++){ if((($x - 18) % 8) -eq 0 -or (($y - 50) % 8) -eq 0){ P $dt $x $y @(56,116,80) } } }
for($x=18;$x -le 158;$x++){ P $dt $x 50 @(230,214,160); if((($x - 18) % 4) -eq 0){ P $dt $x 51 @(120,100,60) }; if((($x - 18) % 8) -eq 0){ P $dt $x 52 @(120,100,60) } }
for($x=18;$x -le 158;$x++){ P $dt $x 86 @(20,52,34) }
for($y=50;$y -le 86;$y++){ P $dt 18 $y @(20,52,34); P $dt 158 $y @(20,52,34) }
# a pincushion by the leather / needle slots, and a plate under the bag
Fill $dt 6 92 52 118 @(60,24,48) 2
Fill $dt 142 90 171 118 @(60,24,48) 2
for($x=142;$x -le 171;$x++){ P $dt $x 90 $goldDk }
Walnut $dt 3 128 172 218
for($x=3;$x -le 172;$x++){ P $dt $x 127 $gold }
Inventory $dt 140 @(40,28,18) @(22,14,8) @(150,112,72)
Save $dt 'gui\container\designer_table.png'
'guis saved'

# ── Item icons (16x16) ──
function Icon(){ NewBmp 16 16 }
# a filled rectangle with an outline
function Box($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int[]]$edge,[int]$noise){
  Fill $bmp $x0 $y0 $x1 $y1 $c $noise
  for($x=$x0;$x -le $x1;$x++){ P $bmp $x $y0 $edge; P $bmp $x $y1 $edge }
  for($y=$y0;$y -le $y1;$y++){ P $bmp $x0 $y $edge; P $bmp $x1 $y $edge } }
# stitches (every other pixel) round the inside of a box
function Stitches($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c){
  for($x=$x0;$x -le $x1;$x+=2){ P $bmp $x $y0 $c; P $bmp $x $y1 $c }
  for($y=$y0+2;$y -le $y1-2;$y+=2){ P $bmp $x0 $y $c; P $bmp $x1 $y $c } }

# a satchel: a strap arching over the top, the body, a darker flap with an emblem drawn by $emblem
function Satchel([string]$name,[int[]]$body,[int[]]$flap,[scriptblock]$emblem){
  $b = Icon
  $strap = @(96,58,30)
  foreach($pt in @(@(3,5),@(3,4),@(4,3),@(5,2),@(6,1),@(7,1),@(8,1),@(9,1),@(10,2),@(11,3),@(12,4),@(12,5))){ P $b $pt[0] $pt[1] $strap }
  Box $b 2 5 13 14 $body (Shade $body 0.45) 5
  for($x=3;$x -le 12;$x++){ P $b $x 13 (Shade $body 0.75) }
  Box $b 2 5 13 9 $flap (Shade $flap 0.45) 4
  for($x=3;$x -le 12;$x++){ P $b $x 6 (Shade $flap 1.25) }
  Stitches $b 3 11 12 13 @(238,226,196)
  & $emblem $b
  Save $b ("item\" + $name + ".png") }
Satchel 'food_satchel' @(204,170,112) @(150,92,48) { param($b) P $b 7 7 @(214,40,40); P $b 8 7 @(214,40,40); P $b 7 8 @(170,24,24); P $b 8 8 @(190,30,30); P $b 8 6 @(60,150,50); P $b 9 9 @(255,218,96) }
Satchel 'seed_satchel' @(92,132,62) @(120,78,40) { param($b) foreach($pt in @(@(6,7),@(8,7),@(10,7),@(7,8),@(9,8))){ P $b $pt[0] $pt[1] @(232,210,110) }; P $b 7 7 @(140,190,80) }

# patches: a square of cloth with stitches round it and a picture on it
function Patch([string]$name,[int[]]$cloth,[scriptblock]$picture){
  $b = Icon
  Box $b 2 2 13 13 $cloth (Shade $cloth 0.4) 6
  for($x=3;$x -le 12;$x++){ P $b $x 3 (Shade $cloth 1.2); P $b $x 12 (Shade $cloth 0.8) }
  Stitches $b 3 3 12 12 @(240,228,198)
  & $picture $b
  Save $b ("item\" + $name + ".png") }
Patch 'leather_patch' @(140,86,48) { param($b) Fill $b 6 6 9 9 (Shade @(140,86,48) 0.82) 3; P $b 7 7 @(170,112,66) }
Patch 'pantry_patch' @(214,186,128) { param($b) foreach($y in 5..10){ P $b 7 $y @(176,140,50) }; foreach($pt in @(@(6,5),@(8,5),@(6,7),@(8,7),@(6,9),@(8,9))){ P $b $pt[0] $pt[1] @(232,196,80) }; P $b 7 4 @(232,196,80) }
Patch 'seed_pocket_patch' @(96,136,66) { param($b) for($x=4;$x -le 11;$x++){ P $b $x 8 (Shade @(96,136,66) 0.6) }; foreach($pt in @(@(5,10),@(7,10),@(9,10),@(6,11),@(8,11),@(10,11))){ P $b $pt[0] $pt[1] @(232,210,110) }; P $b 7 5 @(150,96,52); P $b 8 5 @(150,96,52) }
# the strap patch: a leather strap across with a brass buckle
$sp = Icon
Fill $sp 1 6 14 9 @(120,72,36) 5
for($x=1;$x -le 14;$x++){ P $sp $x 6 @(150,96,52); P $sp $x 9 @(70,40,18) }
for($x=2;$x -le 13;$x+=2){ P $sp $x 7 @(236,220,180) }
foreach($pt in @(@(6,4),@(7,4),@(8,4),@(9,4),@(6,11),@(7,11),@(8,11),@(9,11))){ P $sp $pt[0] $pt[1] @(230,190,80) }
foreach($y in 4..11){ P $sp 6 $y @(230,190,80); P $sp 9 $y @(200,160,60) }
P $sp 7 8 @(150,150,160); P $sp 8 8 @(150,150,160)
Save $sp 'item\strap_patch.png'
# the Diamond Sewing Needle: a long silver needle on the diagonal, a diamond tip, a red thread through its eye
$nd = Icon
for($i=0;$i -le 9;$i++){ P $nd (3+$i) (12-$i) @(214,220,230); P $nd (4+$i) (12-$i) @(150,156,168) }
P $nd 2 13 @(110,116,128); P $nd 3 13 @(150,156,168)
P $nd 13 2 @(120,240,230); P $nd 14 1 @(200,255,250); P $nd 13 1 @(80,200,200); P $nd 14 2 @(80,200,200)
P $nd 4 12 @(40,40,48)
foreach($pt in @(@(5,13),@(6,14),@(7,14),@(8,13),@(9,12),@(10,12),@(11,13),@(12,14))){ P $nd $pt[0] $pt[1] @(200,40,40) }
Save $nd 'item\diamond_sewing_needle.png'
'icons saved'

# ── Block textures (16x16) ──
# Designer Table: a green cutting mat on top (grid, a ruler edge), dark walnut sides with a plum cloth drape, walnut legs,
# a bolt of plum cloth with a gold stripe
$top = Icon
Fill $top 0 0 15 15 @(36,92,60) 3
for($i=0;$i -le 15;$i++){ for($j=0;$j -le 15;$j++){ if(($i % 4) -eq 0 -or ($j % 4) -eq 0){ P $top $i $j @(60,122,84) } } }
for($x=0;$x -le 15;$x++){ P $top $x 0 @(230,214,160); P $top $x 15 @(30,60,40) }
for($x=0;$x -le 15;$x+=2){ P $top $x 1 @(150,126,80) }
Save $top 'block\designer_table_top.png'
$wood = Icon
Walnut $wood 0 0 15 15
for($y=0;$y -le 15;$y++){ P $wood 0 $y @(54,34,20); P $wood 15 $y @(54,34,20) }
Save $wood 'block\designer_table_wood.png'
$side = Icon
Walnut $side 0 0 15 15
Fill $side 0 0 15 5 @(110,44,86) 4
foreach($x in 0..15){ $d = 6 + [int](1.5 + 1.5*[Math]::Sin($x / 2.0)); Fill $side $x 6 $x $d @(96,36,74) 3; P $side $x ($d+1) @(60,20,46) }
for($x=0;$x -le 15;$x++){ P $side $x 1 @(232,192,98) }
Save $side 'block\designer_table_side.png'
$cloth = Icon
Fill $cloth 0 0 15 15 @(120,48,94) 4
for($y=0;$y -le 15;$y++){ for($x=0;$x -le 15;$x++){ if((($x + $y) % 5) -eq 0){ P $cloth $x $y @(140,62,112) } } }
for($y=0;$y -le 15;$y++){ P $cloth 7 $y @(232,192,98); P $cloth 8 $y @(200,160,70) }
Save $cloth 'block\designer_table_cloth.png'

# Sewing Machine: black enamel with gold pinstripes and a scroll, polished steel, a light oak base, a spool of thread
$en = Icon
Fill $en 0 0 15 15 @(22,24,24) 2
for($x=0;$x -le 15;$x++){ P $en $x 1 @(46,50,50); P $en $x 2 @(190,150,60); P $en $x 13 @(190,150,60) }
foreach($pt in @(@(6,6),@(7,5),@(8,5),@(9,6),@(9,7),@(8,8),@(7,8),@(6,9),@(6,10),@(7,11),@(8,11),@(9,10))){ P $en $pt[0] $pt[1] @(226,188,92) }
Save $en 'block\sewing_machine_enamel.png'
$mt = Icon
for($y=0;$y -le 15;$y++){ for($x=0;$x -le 15;$x++){ $v = 170 + 40*[Math]::Sin(($x - $y) / 4.0) + $rng.Next(-4,5); P $mt $x $y @(($v-6),($v-2),($v+8)) } }
Save $mt 'block\sewing_machine_metal.png'
$ow = Icon
for($y=0;$y -le 15;$y++){ for($x=0;$x -le 15;$x++){ $grain = [Math]::Sin(($x + 2*[Math]::Sin($y/4.0)) / 2.0) * 7; $n=$rng.Next(-3,4); P $ow $x $y @((176+$grain+$n),(132+$grain+$n),(84+$grain*0.6+$n)) } }
for($x=0;$x -le 15;$x++){ P $ow $x 0 @(120,86,50); P $ow $x 15 @(120,86,50) }
Save $ow 'block\sewing_machine_wood.png'
$sp2 = Icon
Fill $sp2 0 0 15 15 @(238,228,204) 3
for($y=1;$y -le 15;$y+=2){ for($x=0;$x -le 15;$x++){ P $sp2 $x $y @(214,204,178) } }
for($x=0;$x -le 15;$x++){ P $sp2 $x 0 @(150,104,60); P $sp2 $x 15 @(150,104,60) }
Save $sp2 'block\sewing_machine_spool.png'
'blocks saved'

# ── The bags on players' backs (64x32, client/BagLayer) ──
# body 7x10x3 at (0,0): top (3,0) 7x3, bottom (10,0), right side (0,3) 3x10, front/chest side (3,3) 7x10, left side
# (10,3) 3x10, outer face (13,3) 7x10. flap 7x3x4 at (0,14): its outer face (15,18) 7x3, top (4,14) 7x4.
# shoulder straps 1x1x5 at (24,0); straps down the chest 1x8x0.5 at (40,0)
function BagTexture([string]$name,[int[]]$body,[int[]]$flap,[int[]]$strap,[scriptblock]$front){
  $b = NewBmp 64 32
  Fill $b 0 0 22 12 $body 5
  Fill $b 0 14 25 20 $flap 4
  Fill $b 24 0 39 6 $strap 4
  Fill $b 40 0 44 9 $strap 4
  # outer face: darker edges, a pocket with stitches, then the bag's own picture
  for($y=3;$y -le 12;$y++){ P $b 13 $y (Shade $body 0.7); P $b 19 $y (Shade $body 0.7) }
  for($x=13;$x -le 19;$x++){ P $b $x 12 (Shade $body 0.6) }
  for($x=14;$x -le 18;$x++){ P $b $x 8 (Shade $body 0.75) }
  for($x=14;$x -le 18;$x+=2){ P $b $x 11 @(236,224,196) }
  & $front $b
  # the flap's edge and a brass buckle on it
  for($x=15;$x -le 21;$x++){ P $b $x 20 (Shade $flap 0.6) }
  P $b 18 19 @(232,192,90); P $b 18 20 @(200,160,60)
  Save $b ("entity\" + $name + ".png") }
BagTexture 'bag_backpack' @(128,80,44) @(104,62,32) @(84,50,26) { param($b) P $b 16 6 @(232,192,90); P $b 16 5 @(200,160,60) }
BagTexture 'bag_food_satchel' @(204,170,112) @(150,92,48) @(110,70,36) { param($b) P $b 16 5 @(214,40,40); P $b 15 5 @(214,40,40); P $b 16 6 @(170,24,24); P $b 15 6 @(190,30,30); P $b 16 4 @(60,150,50) }
BagTexture 'bag_seed_satchel' @(92,132,62) @(120,78,40) @(96,60,30) { param($b) foreach($pt in @(@(14,5),@(16,5),@(18,5),@(15,6),@(17,6))){ P $b $pt[0] $pt[1] @(232,210,110) } }
'bags saved'

# ── Fancy and Luxury patches, and the Iron Sewing Needle ──
Patch 'velvet_patch' @(110,48,150) { param($b) for($y=4;$y -le 11;$y++){ for($x=4;$x -le 11;$x++){ if((($x + $y) % 4) -eq 0){ P $b $x $y @(150,82,196) } } } }
Patch 'embroidered_patch' @(220,206,170) { param($b) foreach($pt in @(@(7,5),@(8,5),@(6,6),@(9,6),@(7,7),@(8,7))){ P $b $pt[0] $pt[1] @(200,50,80) }; foreach($y in 8..10){ P $b 7 $y @(60,130,60); P $b 8 $y @(60,130,60) }; P $b 6 9 @(80,160,70); P $b 9 9 @(80,160,70); P $b 7 6 @(240,200,90) }
Patch 'gold_trim_patch' @(120,70,40) { param($b) for($i=2;$i -le 13;$i++){ P $b $i 2 @(240,200,80); P $b $i 13 @(200,160,50); P $b 2 $i @(240,200,80); P $b 13 $i @(200,160,50) }; Fill $b 6 6 9 9 @(230,190,70) 6 }
Patch 'silk_patch' @(236,170,196) { param($b) for($y=4;$y -le 11;$y++){ $x = 4 + (($y * 3) % 8); P $b $x $y @(255,230,240); P $b ([Math]::Min(11,$x+1)) $y @(255,214,228) } }
# Luxury: a brand's colors and motif on dark leather, with a gold edge
function Luxury([string]$name,[int[]]$base,[scriptblock]$motif){
  $b = Icon
  Box $b 2 2 13 13 $base @(232,192,90) 4
  for($x=3;$x -le 12;$x++){ P $b $x 3 (Shade $base 1.2) }
  & $motif $b
  for($i=3;$i -le 12;$i+=3){ P $b $i 2 @(255,234,150) }
  Save $b ("item\" + $name + ".png") }
Luxury 'gucci_patch' @(64,48,36) { param($b) for($y=4;$y -le 11;$y++){ P $b 6 $y @(40,110,50); P $b 7 $y @(190,30,40); P $b 8 $y @(190,30,40); P $b 9 $y @(40,110,50) } }
Luxury 'burberry_patch' @(196,160,108) { param($b) foreach($x in @(5,10)){ for($y=3;$y -le 12;$y++){ P $b $x $y @(30,30,30) } }; foreach($y in @(5,10)){ for($x=3;$x -le 12;$x++){ P $b $x $y @(30,30,30) } }; foreach($x in 3..12){ P $b $x 8 @(200,40,40) }; foreach($y in 3..12){ P $b 8 $y @(200,40,40) } }
Luxury 'louis_vuitton_patch' @(108,72,40) { param($b) foreach($pt in @(@(4,4),@(8,4),@(6,6),@(10,6),@(4,8),@(8,8),@(6,10),@(10,10))){ P $b $pt[0] $pt[1] @(226,190,110); P $b ($pt[0]+1) $pt[1] @(200,164,90) } }
Luxury 'prada_patch' @(30,30,34) { param($b) foreach($pt in @(@(7,5),@(8,5),@(6,6),@(9,6),@(5,7),@(10,7),@(5,8),@(6,8),@(7,8),@(8,8),@(9,8),@(10,8))){ P $b $pt[0] $pt[1] @(200,206,216) }; P $b 7 7 @(120,126,140); P $b 8 7 @(120,126,140) }
$in = Icon
for($i=0;$i -le 9;$i++){ P $in (3+$i) (12-$i) @(196,200,206); P $in (4+$i) (12-$i) @(120,124,132) }
P $in 2 13 @(90,94,100); P $in 3 13 @(120,124,132); P $in 13 2 @(230,232,236); P $in 14 1 @(250,250,252)
P $in 4 12 @(40,40,48)
foreach($pt in @(@(5,13),@(6,14),@(7,14),@(8,13),@(9,12),@(10,12),@(11,13),@(12,14))){ P $in $pt[0] $pt[1] @(80,110,200) }
Save $in 'item\iron_sewing_needle.png'
'fancy saved'

# ── Fancy and Luxury bags (icons and on the back) and the pet hats ──
# a backpack icon: loop on top, rounded body, flap with a buckle, a pocket
function Pack([string]$name,[int[]]$body,[int[]]$flap,[int[]]$trim){
  $b = Icon
  foreach($x in 6..9){ P $b $x 1 (Shade $body 0.4) }; P $b 5 2 (Shade $body 0.4); P $b 10 2 (Shade $body 0.4)
  Box $b 3 3 12 14 $body (Shade $body 0.4) 5
  Box $b 4 4 11 8 $flap (Shade $flap 0.5) 4
  P $b 7 8 $trim; P $b 8 8 $trim; P $b 7 9 (Shade $trim 0.7)
  Box $b 5 10 10 13 (Shade $body 0.9) (Shade $body 0.5) 3
  for($x=5;$x -le 11;$x+=2){ P $b $x 5 $trim }
  Save $b ("item\" + $name + ".png") }
Pack 'fancy_backpack' @(110,48,150) @(80,30,110) @(232,192,90)
Pack 'uniform_backpack' @(40,60,110) @(30,44,84) @(220,220,230)
Satchel 'fancy_food_satchel' @(200,90,120) @(120,40,70) { param($b) P $b 7 7 @(232,192,90); P $b 8 7 @(232,192,90); P $b 7 8 @(214,40,40); P $b 8 8 @(190,30,30); P $b 8 6 @(60,150,50) }
Satchel 'fancy_seed_satchel' @(236,170,196) @(110,48,150) { param($b) foreach($pt in @(@(6,7),@(10,7))){ P $b $pt[0] $pt[1] @(232,210,110) }; P $b 8 7 @(220,60,80); P $b 8 6 @(250,200,90); P $b 8 8 @(60,150,50) }
# a handbag: two handles, a trapezoid body in the brand's colors, a gold clasp
function Handbag([string]$name,[int[]]$body,[scriptblock]$motif){
  $b = Icon
  foreach($pt in @(@(5,2),@(5,3),@(5,4),@(6,1),@(7,1),@(8,1),@(9,1),@(10,2),@(10,3),@(10,4))){ P $b $pt[0] $pt[1] @(70,46,26) }
  for($y=5;$y -le 13;$y++){ $half = 4 + [int](($y - 5) / 3); for($x=(8-$half);$x -le (7+$half);$x++){ $edge = ($x -eq 8-$half -or $x -eq 7+$half -or $y -eq 5 -or $y -eq 13); if($edge){ P $b $x $y (Shade $body 0.45) } else { $n = $rng.Next(-5,6); P $b $x $y @(($body[0]+$n),($body[1]+$n),($body[2]+$n)) } } }
  & $motif $b
  P $b 7 6 @(240,200,90); P $b 8 6 @(240,200,90); P $b 7 7 @(200,160,60); P $b 8 7 @(200,160,60)
  Save $b ("item\" + $name + ".png") }
Handbag 'gucci_bag' @(70,52,38) { param($b) for($y=8;$y -le 12;$y++){ P $b 6 $y @(40,110,50); P $b 7 $y @(190,30,40); P $b 8 $y @(190,30,40); P $b 9 $y @(40,110,50) } }
Handbag 'burberry_bag' @(196,160,108) { param($b) foreach($x in @(5,10)){ for($y=8;$y -le 12;$y++){ P $b $x $y @(30,30,30) } }; for($x=4;$x -le 11;$x++){ P $b $x 10 @(200,40,40) } }
Handbag 'louis_vuitton_bag' @(108,72,40) { param($b) foreach($pt in @(@(5,9),@(9,9),@(7,11),@(11,11),@(4,12),@(10,12))){ P $b $pt[0] $pt[1] @(226,190,110) } }
Handbag 'prada_bag' @(30,30,34) { param($b) foreach($pt in @(@(7,9),@(8,9),@(6,10),@(9,10),@(6,11),@(7,11),@(8,11),@(9,11))){ P $b $pt[0] $pt[1] @(200,206,216) } }
# hats
$th = Icon
Fill $th 5 2 10 10 @(30,30,34) 3; for($y=2;$y -le 10;$y++){ P $th 5 $y @(14,14,16); P $th 10 $y @(50,50,56) }
for($x=5;$x -le 10;$x++){ P $th $x 8 @(40,110,50); P $th $x 9 @(190,30,40) }
Fill $th 2 11 13 12 @(24,24,28) 2; for($x=2;$x -le 13;$x++){ P $th $x 12 @(12,12,14) }
Save $th 'item\gucci_top_hat.png'
$sh = Icon
foreach($y in 4..11){ $half = [int](($y - 3) * 0.75) + 1; for($x=(7-$half);$x -le (7+$half);$x++){ $c = if((($x + $y) % 4) -eq 0){ @(30,30,30) } elseif((($x - $y) % 5) -eq 0){ @(200,40,40) } else { @(196,160,108) }; P $sh $x $y $c } }
Fill $sh 1 12 14 13 @(240,240,236) 3
P $sh 9 2 @(196,160,108); P $sh 10 2 @(196,160,108); P $sh 11 3 @(196,160,108); Fill $sh 12 3 13 4 @(244,244,240) 2
Save $sh 'item\burberry_santa_hat.png'
$cp = Icon
for($y=4;$y -le 10;$y++){ $half = [int](3 + [Math]::Min(3, ($y - 4))); for($x=(7-$half);$x -le (6+$half);$x++){ P $cp $x $y @(108,72,40) } }
foreach($pt in @(@(5,6),@(8,6),@(6,8),@(9,8),@(4,9))){ P $cp $pt[0] $pt[1] @(226,190,110) }
P $cp 7 4 @(226,190,110)
Fill $cp 8 10 14 11 @(84,54,30) 2
Save $cp 'item\louis_vuitton_cap.png'
$fd = Icon
for($y=4;$y -le 9;$y++){ for($x=4;$x -le 11;$x++){ P $fd $x $y @(40,40,46) } }
P $fd 7 4 @(24,24,28); P $fd 8 4 @(24,24,28); P $fd 7 5 @(24,24,28)
for($x=4;$x -le 11;$x++){ P $fd $x 8 @(200,206,216) }
Fill $fd 1 10 14 11 @(34,34,40) 2; for($x=1;$x -le 14;$x++){ P $fd $x 11 @(18,18,22) }
Save $fd 'item\prada_fedora.png'
# the new bags on players' backs
BagTexture 'bag_fancy_backpack' @(110,48,150) @(80,30,110) @(60,24,80) { param($b) P $b 16 6 @(232,192,90); P $b 16 5 @(200,160,60) }
BagTexture 'bag_uniform_backpack' @(40,60,110) @(30,44,84) @(24,34,64) { param($b) P $b 16 6 @(220,220,230); P $b 16 5 @(180,180,190) }
BagTexture 'bag_fancy_food_satchel' @(200,90,120) @(120,40,70) @(90,30,50) { param($b) P $b 16 5 @(232,192,90); P $b 15 5 @(232,192,90); P $b 16 6 @(214,40,40) }
BagTexture 'bag_fancy_seed_satchel' @(236,170,196) @(110,48,150) @(80,30,110) { param($b) P $b 15 5 @(232,210,110); P $b 17 5 @(220,60,80); P $b 16 6 @(60,150,50) }
BagTexture 'bag_gucci_bag' @(70,52,38) @(56,40,28) @(40,110,50) { param($b) for($y=3;$y -le 11;$y++){ P $b 15 $y @(40,110,50); P $b 16 $y @(190,30,40); P $b 17 $y @(40,110,50) } }
BagTexture 'bag_burberry_bag' @(196,160,108) @(150,120,80) @(60,40,24) { param($b) foreach($x in @(14,18)){ for($y=3;$y -le 11;$y++){ P $b $x $y @(30,30,30) } }; for($x=13;$x -le 19;$x++){ P $b $x 7 @(200,40,40) } }
BagTexture 'bag_louis_vuitton_bag' @(108,72,40) @(84,54,30) @(60,38,20) { param($b) foreach($pt in @(@(14,4),@(17,4),@(15,6),@(18,6),@(14,8),@(17,8),@(15,10))){ P $b $pt[0] $pt[1] @(226,190,110) } }
BagTexture 'bag_prada_bag' @(30,30,34) @(20,20,24) @(14,14,16) { param($b) foreach($pt in @(@(15,5),@(16,5),@(14,6),@(17,6),@(14,7),@(15,7),@(16,7),@(17,7))){ P $b $pt[0] $pt[1] @(200,206,216) } }
'luxury saved'

# ── Pet hats on animals' heads (entity/pet_hat_<hat>, 64x64, client/PetHatLayer's UV layout) ──
function Check($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ if(($x % 4) -eq 0 -or ($y % 4) -eq 0){ P $bmp $x $y @(30,30,30) } elseif((($x + 2) % 8) -eq 0 -or (($y + 2) % 8) -eq 0){ P $bmp $x $y @(200,40,40) } } } }
$h = NewBmp 64 64
Fill $h 0 0 45 12 @(26,26,30) 3; Fill $h 0 14 29 29 @(30,30,34) 3
for($y=14;$y -le 29;$y++){ P $h 9 $y @(56,56,64); P $h 10 $y @(48,48,56) }
Fill $h 0 32 29 41 @(40,110,50) 3; for($x=0;$x -le 29;$x++){ P $h $x 39 @(190,30,40); P $h $x 40 @(190,30,40) }
for($x=0;$x -le 29;$x++){ P $h $x 35 @(190,30,40) }
Save $h 'entity\pet_hat_gucci_top_hat.png'
$h = NewBmp 64 64
Fill $h 0 0 37 12 @(242,242,238) 4
Fill $h 0 14 29 25 @(196,160,108) 3; Check $h 0 14 29 25
Fill $h 0 26 21 34 @(196,160,108) 3; Check $h 0 26 21 34
Fill $h 24 26 37 31 @(196,160,108) 3
Fill $h 40 0 49 5 @(250,250,248) 3
Save $h 'entity\pet_hat_burberry_santa_hat.png'
$h = NewBmp 64 64
Fill $h 0 0 35 12 @(108,72,40) 4
for($y=0;$y -le 12;$y+=3){ for($x=($y % 6);$x -le 35;$x+=6){ P $h $x $y @(226,190,110) } }
Fill $h 0 14 25 19 @(84,54,30) 3
Fill $h 36 0 45 3 @(226,190,110) 2
Save $h 'entity\pet_hat_louis_vuitton_cap.png'
$h = NewBmp 64 64
Fill $h 0 0 49 13 @(28,28,32) 3; Fill $h 0 16 33 28 @(40,40,46) 3
for($x=0;$x -le 33;$x++){ P $h $x 20 @(34,34,40) }
Fill $h 0 32 33 41 @(200,206,216) 4
Save $h 'entity\pet_hat_prada_fedora.png'
'pet hats saved'

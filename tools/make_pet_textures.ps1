# Draws the Pet Trainer's items (PetTrainer.md):
#  - item/ownership_tag: vanilla's name tag recolored gold, with a diamond set in it and a red wax seal
#  - item/training_stick: a polished stick held at an angle, a looped lead tied near the top, a diamond on the tip and
#    a fishing line with a little treat dangling from it
#  - block/feeder_copper, feeder_iron, feeder_compacted_iron (Small / Medium / Large troughs and bowls), food_trough_food
#    (the heap in a filled trough), water_bowl_water
#    (models and the rest: tools/make_pet_assets.js)
#  - particle/petting: the little patting hand that floats up on the first pet of the day
#  - entity/animal_bjorn (the carrier worn on the chest), item/animal_bjorn, gui/sprites/container/slot/back (the empty
#    Back slot)
# The tag is recolored from vanilla's name_tag.png; unzip it first (Git Bash, from the project folder):
#   unzip -o -d "$LOCALAPPDATA/Temp/vr" .gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/item/name_tag.png'
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'
$vr = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\item'
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function P($bmp,[int]$x,[int]$y,[double]$r,[double]$g,[double]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }

# ── Ownership Tag ──
$tag = [Drawing.Bitmap]::FromFile((Join-Path $vr 'name_tag.png'))
$out = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $tag.GetPixel($x,$y); if($c.A -eq 0){ continue }
  $l = ($c.R + $c.G + $c.B) / 3.0 / 255.0
  # the paper turns gold, the string stays a dark cord
  if($c.R -gt 150 -and $c.G -gt 130){ P $out $x $y (150 + 105*$l) (110 + 110*$l) (30 + 40*$l) }
  else { P $out $x $y ($c.R*0.8) ($c.G*0.7) ($c.B*0.6) } } }
$tag.Dispose()
# find the tag's middle (the brightest gold) and set a diamond there
$sx = 0; $sy = 0; $n = 0
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $out.GetPixel($x,$y); if($c.A -gt 0 -and $c.R -gt 200){ $sx += $x; $sy += $y; $n++ } } }
$mx = [int]($sx / [Math]::Max(1,$n)); $my = [int]($sy / [Math]::Max(1,$n))
P $out $mx ($my-1) 200 250 250; P $out ($mx-1) $my 90 220 220; P $out $mx $my 160 245 240; P $out ($mx+1) $my 60 180 190; P $out $mx ($my+1) 40 140 150
P $out ($mx+2) ($my+2) 170 30 30; P $out ($mx+3) ($my+2) 200 50 40; P $out ($mx+2) ($my+3) 140 20 20
$out.Save((Join-Path $res 'ownership_tag.png'),[Drawing.Imaging.ImageFormat]::Png); $out.Dispose()

# ── Training Stick ──
$st = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
# the stick: bottom left to top right, two pixels thick, a light edge and a dark edge
for($i=0;$i -le 10;$i++){ $x = 2 + $i; $y = 13 - $i
  P $st $x $y (150 - $i*2) (100 - $i) 52; P $st ($x+1) $y (104) (66) 34 }
# a grip wrapped in leather at the bottom
foreach($i in 0..2){ P $st (2+$i) (13-$i) 90 52 30; P $st (3+$i) (13-$i) 70 40 24 }
# the lead tied round it near the top, a loop hanging off
foreach($p in @(@(10,5),@(11,5),@(10,6),@(11,6))){ P $st $p[0] $p[1] 196 160 110 }
P $st 9 7 176 140 96; P $st 8 8 176 140 96; P $st 8 9 160 126 84; P $st 9 10 176 140 96; P $st 10 9 196 160 110; P $st 10 8 160 126 84
# the diamond tip
P $st 13 2 220 255 255; P $st 14 1 160 240 240; P $st 14 2 90 210 210; P $st 13 1 120 230 230; P $st 15 0 200 255 255; P $st 12 3 60 170 180
# the fishing line down from the tip, with a treat (a bit of meat) on the end
foreach($y in 3..9){ P $st 14 $y 220 220 220 }
P $st 14 10 190 70 60; P $st 13 11 170 50 46; P $st 14 11 210 96 80; P $st 15 11 170 50 46; P $st 14 12 150 40 36
$st.Save((Join-Path $res 'training_stick.png'),[Drawing.Imaging.ImageFormat]::Png); $st.Dispose()
'saved'

# ── Food Trough and Water Bowl (blocks) ──
$blk = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$rng = New-Object System.Random 7
function NB(){ New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
# the three metals the troughs and bowls are made of: riveted plates, a light top edge and a dark bottom edge
#   feeder_copper (Small): warm copper with green patina flecks
#   feeder_iron (Medium): light iron
#   feeder_compacted_iron (Large): dark blue-grey plates with heavy bands and rivets
function Metal([int[]]$c,[int]$noise,[int[]]$band,[int[]]$rivet,[int[]]$fleck,[string]$name){
  $m = NB
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n = $rng.Next(-$noise,$noise+1); P $m $x $y ($c[0]+$n) ($c[1]+$n) ($c[2]+$n) } }
  for($x=0;$x -lt 16;$x++){ P $m $x 0 ($c[0]+40) ($c[1]+40) ($c[2]+40); P $m $x 15 ($c[0]-50) ($c[1]-50) ($c[2]-50) }
  # a seam between two plates, and bands near the ends
  for($y=1;$y -lt 15;$y++){ P $m 7 $y ($c[0]-30) ($c[1]-30) ($c[2]-30); P $m 8 $y ($c[0]+18) ($c[1]+18) ($c[2]+18) }
  if($band){ foreach($x in @(2,13)){ for($y=1;$y -lt 15;$y++){ P $m $x $y $band[0] $band[1] $band[2] } } }
  foreach($p in @(@(3,3),@(12,3),@(3,12),@(12,12))){ P $m $p[0] $p[1] $rivet[0] $rivet[1] $rivet[2]; P $m ($p[0]+1) ($p[1]+1) ($rivet[0]-60) ($rivet[1]-60) ($rivet[2]-60) }
  if($fleck){ for($i=0;$i -lt 9;$i++){ P $m $rng.Next(0,16) $rng.Next(1,15) $fleck[0] $fleck[1] $fleck[2] } }
  $m.Save((Join-Path $blk "$name.png"),[Drawing.Imaging.ImageFormat]::Png); $m.Dispose() }
Metal @(196,112,72) 9 $null @(236,160,112) @(96,176,140) 'feeder_copper'
Metal @(176,178,184) 7 $null @(226,228,232) $null 'feeder_iron'
Metal @(84,92,108) 6 @(58,64,78) @(150,160,178) $null 'feeder_compacted_iron'
foreach($old in @('food_trough_wood','water_bowl_stone')){ $p = Join-Path $blk "$old.png"; if(Test-Path $p){ Remove-Item $p } }
# trough food: a heap of grain and hay with carrot bits and green tops
$f = NB
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n = $rng.Next(-18,19); P $f $x $y (196 + $n) (164 + $n) (70 + $n/2) } }
for($i=0;$i -lt 22;$i++){ $x = $rng.Next(0,16); $y = $rng.Next(0,16); P $f $x $y 150 116 40 }
for($i=0;$i -lt 7;$i++){ $x = $rng.Next(0,15); $y = $rng.Next(0,16); P $f $x $y 236 120 30; P $f ($x+1) $y 214 98 20 }
for($i=0;$i -lt 5;$i++){ $x = $rng.Next(0,16); $y = $rng.Next(0,16); P $f $x $y 80 160 50 }
$f.Save((Join-Path $blk 'food_trough_food.png'),[Drawing.Imaging.ImageFormat]::Png); $f.Dispose()
# bowl water: clear blue with a few light ripples
$wa = NB
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n = $rng.Next(-6,7); P $wa $x $y (46 + $n) (98 + $n) (210 + $n) } }
foreach($p in @(@(3,4),@(4,4),@(5,4),@(10,9),@(11,9),@(12,9),@(6,12),@(7,12))){ P $wa $p[0] $p[1] 140 190 250 }
$wa.Save((Join-Path $blk 'water_bowl_water.png'),[Drawing.Imaging.ImageFormat]::Png); $wa.Dispose()
'blocks saved'

# ── particle/petting (the first pet of the day): a little hand patting down, fingers spread ──
$pd = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\particle'
New-Item -ItemType Directory -Force $pd | Out-Null
$sm = New-Object Drawing.Bitmap 8,8,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rows = @('.o.o.o..','oso#oso.','osssssoo','osssssso','.ossssso','..ossdso','...osdso','....ooo.')
for($y=0;$y -lt 8;$y++){ for($x=0;$x -lt 8;$x++){ $c = $rows[$y][$x]
  switch($c){ 'o' { P $sm $x $y 110 60 40 } '#' { P $sm $x $y 110 60 40 } 's' { P $sm $x $y 244 196 156 } 'd' { P $sm $x $y 214 156 116 } } } }
$sm.Save((Join-Path $pd 'petting.png'),[Drawing.Imaging.ImageFormat]::Png); $sm.Dispose()
$old = Join-Path $pd 'smiley.png'; if(Test-Path $old){ Remove-Item $old }
'petting hand saved'

# ── The Animal Bjorn: the carrier worn on the chest (entity/animal_bjorn, 64x32, client/BjornLayer's UV layout: the pouch
# at 0,0, its padded rim at 0,12, the straps at 26,0 and 40,0, the waist band at 0,18), its item icon, and the empty Back
# slot's faint backpack outline (gui/sprites/container/slot/back) ──
$ent = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity'
$spr = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\gui\sprites\container\slot'
New-Item -ItemType Directory -Force $ent | Out-Null; New-Item -ItemType Directory -Force $spr | Out-Null
$bj = New-Object Drawing.Bitmap 64,32,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
function Area($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int]$noise){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ if($x -lt $bmp.Width -and $y -lt $bmp.Height){ $n = $rng.Next(-$noise,$noise+1); $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C ($c[0]+$n)),(C ($c[1]+$n)),(C ($c[2]+$n)))) } } } }
Area $bj 0 0 25 11 @(52,112,140) 7          # teal canvas pouch
Area $bj 0 12 27 17 @(236,224,196) 6        # cream padded rim
Area $bj 26 0 39 7 @(120,78,44) 5           # leather straps over the shoulders
Area $bj 40 0 44 11 @(110,70,40) 5          # straps down the back
Area $bj 0 18 19 20 @(110,70,40) 5          # waist band
# stitching round the pouch front (north face: 2,2 to 9,9, the pouch is 2 deep) and a cream paw print on it
for($x=2;$x -le 9;$x++){ if($x % 2 -eq 0){ $bj.SetPixel($x,2,[Drawing.Color]::FromArgb(255,210,220,226)); $bj.SetPixel($x,9,[Drawing.Color]::FromArgb(255,210,220,226)) } }
foreach($p in @(@(5,6),@(6,6),@(5,7),@(6,7),@(4,4),@(7,4),@(3,5),@(8,5))){ $bj.SetPixel($p[0],$p[1],[Drawing.Color]::FromArgb(255,240,232,210)) }
$bj.Save((Join-Path $ent 'animal_bjorn.png'),[Drawing.Imaging.ImageFormat]::Png); $bj.Dispose()

# the item: empty (animal_bjorn) and with an animal in it (animal_bjorn_full: a furry head with ears peeking over the rim;
# items/animal_bjorn.json picks by the CARRIED_ANIMAL component)
foreach($full in @($false,$true)){
$ic = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
# two shoulder-strap loops, the padded rim, the teal pouch with a paw, a darker outline
foreach($p in @(@(3,1),@(4,1),@(2,2),@(2,3),@(5,2),@(5,3),@(10,1),@(11,1),@(10,2),@(10,3),@(13,2),@(13,3))){ P $ic $p[0] $p[1] 120 78 44 }
if($full){
  for($y=1;$y -le 4;$y++){ for($x=5;$x -le 10;$x++){ $n = $rng.Next(-8,9); P $ic $x $y (206+$n) (156+$n) (98+$n/2) } }
  P $ic 5 0 150 104 60; P $ic 10 0 150 104 60; P $ic 4 1 150 104 60; P $ic 11 1 150 104 60
  P $ic 6 2 30 22 16; P $ic 9 2 30 22 16; P $ic 7 3 236 140 150; P $ic 8 3 236 140 150
}
for($x=1;$x -le 14;$x++){ P $ic $x 4 236 224 196; P $ic $x 5 214 200 168 }
for($y=6;$y -le 14;$y++){ $w = if($y -ge 13){ 1 } else { 0 }; for($x=(2+$w);$x -le (13-$w);$x++){ $n = $rng.Next(-6,7); P $ic $x $y (52+$n) (112+$n) (140+$n) } }
for($y=6;$y -le 13;$y++){ P $ic 1 $y 30 70 90; P $ic 14 $y 30 70 90 }
for($x=3;$x -le 12;$x++){ P $ic $x 15 30 70 90 }
foreach($p in @(@(7,10),@(8,10),@(7,11),@(8,11),@(6,8),@(9,8),@(5,9),@(10,9))){ P $ic $p[0] $p[1] 240 232 210 }
$name = if($full){ 'animal_bjorn_full.png' } else { 'animal_bjorn.png' }
$ic.Save((Join-Path $res $name),[Drawing.Imaging.ImageFormat]::Png); $ic.Dispose()
}

$sl = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$ghost = [Drawing.Color]::FromArgb(255,139,139,139)
$rows = @('......####......','.....#....#.....','...##########...','..#..........#..','..#..######..#..','..#..#....#..#..','..#..######..#..','..#..........#..','..#..........#..','..#..........#..','..#..........#..','..#..........#..','...##########...')
for($y=0;$y -lt $rows.Count;$y++){ for($x=0;$x -lt 16;$x++){ if($rows[$y][$x] -eq '#'){ $sl.SetPixel($x,$y+1,[Drawing.Color]::FromArgb(255,198,198,198)) } } }
$sl.Save((Join-Path $spr 'back.png'),[Drawing.Imaging.ImageFormat]::Png); $sl.Dispose()
'bjorn saved'

# Draws the Lava Creator's menu (textures/gui/container/lava_creator.png) in the smith tables' dark molten style.
# Slot and widget positions must match LavaCreatorMenu / LavaCreatorScreen.
Add-Type -AssemblyName System.Drawing
$out = 'C:\Users\alexh\Desktop\ProjectCapitalism2\src\main\resources\assets\capitalism\textures\gui\container\lava_creator.png'
$W = 176; $H = 166
$bmp = New-Object Drawing.Bitmap 256,256,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 7

function Px([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt 256 -and $y -lt 256){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,[Math]::Max(0,[Math]::Min(255,$r)),[Math]::Max(0,[Math]::Min(255,$g)),[Math]::Max(0,[Math]::Min(255,$b)))) } }
function Blend([int]$x,[int]$y,[int]$r,[int]$g,[int]$b,[double]$a){ if($x -lt 0 -or $y -lt 0 -or $x -ge $W -or $y -ge $H){return}; $c=$bmp.GetPixel($x,$y); Px $x $y ([int]($c.R+($r-$c.R)*$a)) ([int]($c.G+($g-$c.G)*$a)) ([int]($c.B+($b-$c.B)*$a)) }
function Rect([int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$b){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ Px $x $y $r $g $b } } }

# ── charred forge stone with a little grain ──
for($y=0;$y -lt $H;$y++){ for($x=0;$x -lt $W;$x++){
  $n = $rng.Next(-5,6); $blot = if((($x*7 + $y*13) % 23) -lt 2){ -4 } else { 0 }
  Px $x $y (32+$n+$blot) (24+$n+$blot) (26+$n+$blot) } }

# ── glowing lava cracks across the forge part (drawn under everything) ──
function Crack($pts){
  for($i=0;$i -lt $pts.Count-1;$i++){
    $a=$pts[$i]; $b=$pts[$i+1]; $steps=[Math]::Max([Math]::Abs($b[0]-$a[0]),[Math]::Abs($b[1]-$a[1]))
    for($s=0;$s -le $steps;$s++){ $x=[int][Math]::Round($a[0]+($b[0]-$a[0])*$s/[Math]::Max(1,$steps)); $y=[int][Math]::Round($a[1]+($b[1]-$a[1])*$s/[Math]::Max(1,$steps))
      foreach($d in @(@(-1,0),@(1,0),@(0,-1),@(0,1))){ Blend ($x+$d[0]) ($y+$d[1]) 170 55 20 0.35 }
      Blend $x $y 255 150 45 0.9 } } }

# ── frame: black edge, blackened iron with light/shadow, thin ember line inside ──
for($i=0;$i -lt $W;$i++){ Px $i 0 8 6 7; Px $i ($H-1) 8 6 7 }
for($i=0;$i -lt $H;$i++){ Px 0 $i 8 6 7; Px ($W-1) $i 8 6 7 }
for($t=1;$t -le 3;$t++){
  for($x=$t;$x -lt $W-$t;$x++){ Px $x $t (70-$t*6) (60-$t*6) (62-$t*6); Px $x ($H-1-$t) 22 17 18 }
  for($y=$t;$y -lt $H-$t;$y++){ Px $t $y (70-$t*6) (60-$t*6) (62-$t*6); Px ($W-1-$t) $y 22 17 18 } }
for($x=4;$x -lt $W-4;$x++){ Blend $x 4 200 85 28 0.85; Blend $x ($H-5) 140 55 20 0.6 }
for($y=4;$y -lt $H-4;$y++){ Blend 4 $y 200 85 28 0.85; Blend ($W-5) $y 140 55 20 0.6 }
# rivets in the corners
function Rivet([int]$x,[int]$y){ Px $x $y 120 108 110; Px ($x+1) $y 92 82 84; Px $x ($y+1) 92 82 84; Px ($x+1) ($y+1) 40 32 34 }
foreach($p in @(@(6,6),@(168,6),@(6,157),@(168,157),@(6,76),@(168,76))){ Rivet $p[0] $p[1] }

# ── title plaque (the title is drawn on it by the screen, at y 6) ──
Rect 46 3 129 14 16 11 12
for($x=46;$x -le 129;$x++){ Px $x 3 210 95 30; Px $x 14 120 45 15 }
for($y=3;$y -le 14;$y++){ Px 46 $y 210 95 30; Px 129 $y 120 45 15 }
for($x=47;$x -le 128;$x++){ Blend $x 4 255 170 70 0.25 }

# ── a recessed slot: dark well, black top-left lip, ember-lit bottom-right lip ──
function Slot([int]$x0,[int]$y0,[int]$size){
  $x1=$x0+$size-1; $y1=$y0+$size-1
  Rect $x0 $y0 $x1 $y1 18 13 14
  for($x=$x0;$x -le $x1;$x++){ Px $x $y0 6 4 5; Px $x $y1 190 80 28 }
  for($y=$y0;$y -le $y1;$y++){ Px $x0 $y 6 4 5; Px $x1 $y 190 80 28 }
  Px $x1 $y0 70 40 30; Px $x0 $y1 70 40 30
  for($x=$x0+1;$x -lt $x1;$x++){ Blend $x ($y1-1) 120 45 15 0.35 }
  for($y=$y0+1;$y -lt $y1;$y++){ Blend ($x1-1) $y 120 45 15 0.35 } }

# player inventory: 3 rows + hotbar, slot items at (8+18c, 84+18r) and (8+18c, 142)
for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Slot (7+18*$c) (83+18*$r) 18 } }
for($c=0;$c -lt 9;$c++){ Slot (7+18*$c) 141 18 }

# ── a few lava cracks, away from the tank and slots ──
Crack @(@(130,20),@(136,24),@(134,29),@(141,33))
Crack @(@(150,40),@(156,44),@(152,50),@(158,55))
Crack @(@(62,22),@(68,26),@(66,31),@(72,34))
Crack @(@(124,62),@(130,66),@(136,64))

# ── the water tank (inside 20,19 to 35,73): a glass-fronted iron tank with measure marks ──
for($x=19;$x -le 36;$x++){ Px $x 18 6 4 5; Px $x 74 190 80 28 }
for($y=18;$y -le 74;$y++){ Px 19 $y 6 4 5; Px 36 $y 190 80 28 }
Rect 20 19 35 73 14 18 26
for($m=0;$m -lt 4;$m++){ $y = 19 + [int](55 * $m / 4); Px 20 $y 90 110 140; Px 21 $y 90 110 140 }
for($y=20;$y -le 72;$y++){ Blend 34 $y 160 190 230 0.12 }

# ── water slot (item 44,57) with a little water drop above it ──
Slot 43 56 18
$drop=@('..#..','.###.','#####','##*##','.###.')
for($r=0;$r -lt 5;$r++){ for($c=0;$c -lt 5;$c++){ $ch=$drop[$r][$c]; if($ch -eq '#'){ Px (49+$c) (48+$r) 60 110 220 } elseif($ch -eq '*'){ Px (49+$c) (48+$r) 190 220 255 } } }

# ── Blaze Powder slot (item 98,22), the lava channel (inside 103,41 to 108,54), the bucket slot (item 98,57) ──
Slot 97 21 18
Slot 97 56 18
foreach($p in @(@(96,20,1,1),@(115,20,-1,1),@(96,74,1,-1),@(115,74,-1,-1))){ $x=$p[0]; $y=$p[1]
  for($i=0;$i -lt 4;$i++){ Px ($x+$p[2]*$i) $y 255 150 45; Px $x ($y+$p[3]*$i) 255 150 45 } }
for($y=40;$y -le 55;$y++){ Px 102 $y 6 4 5; Px 109 $y 190 80 28 }
for($x=102;$x -le 109;$x++){ Px $x 40 6 4 5; Px $x 55 190 80 28 }
Rect 103 41 108 54 16 11 12
# a small flame next to the Blaze Powder
$flame=@('..#...','..##..','.###..','.####.','######','##**##','#****#','#****#','.#**#.','..##..')
for($r=0;$r -lt $flame.Count;$r++){ for($c=0;$c -lt 6;$c++){ $ch=$flame[$r][$c]; if($ch -eq '#'){ Px (120+$c) (25+$r) 224 80 24 } elseif($ch -eq '*'){ Px (120+$c) (25+$r) 255 200 80 } } }

# ── molten seam between the machine and the inventory ──
for($x=5;$x -lt $W-5;$x++){ Px $x 79 255 140 40; Blend $x 78 170 55 20 0.5; Blend $x 80 170 55 20 0.5 }

$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

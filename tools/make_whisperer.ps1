# Draws the Whisperer's skin (textures/entity/whisperer.png, 64x64) for client/WhispererModel: a worn grey-brown hooded
# cloak with a faded violet trim, the face lost in shadow but for two pale eyes, pale bony hands at the end of the
# sleeves, dark trousers and boots, and a gnarled brown cane with a darker knob. The hood's front rim is a frame (see
# through its middle to the shadowed face). The boxes below match WhispererModel's texOffs and sizes (u, v, w, h, d).
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity\whisperer.png'
$bmp = New-Object Drawing.Bitmap 64,64,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 13
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function P([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $x -lt 64 -and $y -ge 0 -and $y -lt 64){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Clear([int]$x,[int]$y){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(0,0,0,0)) }
function Rect([int]$x,[int]$y,[int]$w,[int]$h,[int[]]$c,[int]$noise){
  for($j=0;$j -lt $h;$j++){ for($i=0;$i -lt $w;$i++){ $n = $rng.Next(-$noise, $noise + 1); P ($x+$i) ($y+$j) ($c[0]+$n) ($c[1]+$n) ($c[2]+$n) } } }
# a box's six faces: top lighter, bottom darker, sides the base color
function Box([int]$u,[int]$v,[int]$w,[int]$h,[int]$d,[int[]]$base,[int]$noise){
  $top = @(($base[0]+16),($base[1]+16),($base[2]+16)); $bot = @(($base[0]-22),($base[1]-22),($base[2]-22))
  Rect ($u+$d) $v $w $d $top $noise
  Rect ($u+$d+$w) $v $w $d $bot $noise
  Rect $u ($v+$d) $d $h $base $noise
  Rect ($u+$d) ($v+$d) $w $h $base $noise
  Rect ($u+$d+$w) ($v+$d) $d $h @(($base[0]-10),($base[1]-10),($base[2]-10)) $noise
  Rect ($u+2*$d+$w) ($v+$d) $w $h @(($base[0]-6),($base[1]-6),($base[2]-6)) $noise }

$cloak = @(78,68,60); $hood = @(64,55,50); $trim = @(96,72,120)
# hood (head) 8x8x8 at 0,0: the front is an old man's face in the hood's shadow (front face: x 8-15, y 8-15)
Box 0 0 8 8 8 $hood 6
$skin = @(186,156,134); $skinDark = @(150,118,100); $white = @(222,220,214); $grey = @(176,174,170)
Rect 8 8 8 8 $skin 5
# the hood's shadow over his brow and down the sides
Rect 8 8 8 1 @(22,16,22) 2; Rect 8 9 1 7 @(30,22,28) 2; Rect 15 9 1 7 @(30,22,28) 2; Rect 9 9 6 1 @(70,52,50) 3
# wrinkles on the forehead
P 10 9 140 110 92; P 11 9 140 110 92; P 13 9 140 110 92
# bushy white brows
Rect 9 10 2 1 $white 6; Rect 13 10 2 1 $white 6; P 11 10 196 194 188; P 12 10 196 194 188
# deep-set eyes: dark sockets with a pale glint
P 9 11 70 48 46; P 10 11 200 220 240; P 13 11 200 220 240; P 14 11 70 48 46
P 11 11 $skinDark[0] $skinDark[1] $skinDark[2]; P 12 11 $skinDark[0] $skinDark[1] $skinDark[2]
# hollow cheeks and the shadow of the nose
Rect 9 12 2 1 $skinDark 3; Rect 13 12 2 1 $skinDark 3; P 11 12 120 92 78; P 12 12 120 92 78
# a drooping white moustache and the start of the beard
Rect 9 13 6 1 $white 8; P 9 14 $grey[0] $grey[1] $grey[2]; P 14 14 $grey[0] $grey[1] $grey[2]
Rect 10 14 4 2 $white 10
# the nose 1x2x1 at 56,10 (a little red at the tip)
Box 56 10 1 2 1 $skin 4
P 57 12 176 118 104
# the beard 6x5x1 at 40,10: long white strands, greyer at the edges
Box 40 10 6 5 1 $white 10
for($y=11;$y -le 15;$y++){ P 41 $y $grey[0] $grey[1] $grey[2]; P 46 $y $grey[0] $grey[1] $grey[2] }
P 43 15 190 188 182; P 44 14 196 194 188; P 42 13 238 236 230
# the hood's front rim 9x9x1 at 32,0: cloth round the edge, open in the middle
Box 32 0 9 9 1 $hood 6
for($y=2;$y -le 8;$y++){ for($x=34;$x -le 40;$x++){ Clear $x $y } }
for($y=2;$y -le 8;$y++){ for($x=44;$x -le 50;$x++){ Clear $x $y } }
# hood peak 3x2x3 at 52,0
Box 52 0 3 2 3 $hood 5
# body 8x11x5 at 0,16, cloak skirt 9x4x6 at 26,16, hump 6x5x2 at 30,30
Box 0 16 8 11 5 $cloak 7
Box 26 16 9 4 6 $cloak 7
Box 30 30 6 5 2 @(70,60,54) 6
# a faded violet trim down the front of the cloak and round the skirt's hem
for($y=21;$y -le 31;$y++){ P 8 $y $trim[0] $trim[1] $trim[2]; P 9 $y $trim[0] $trim[1] $trim[2] }
for($x=26;$x -le 55;$x++){ P $x 25 $trim[0] $trim[1] $trim[2] }
# patches and tears on the cloak
P 3 24 60 52 46; P 4 25 60 52 46; P 12 26 92 80 70; P 15 23 58 50 44
# legs 3x7x3 at 0,40 and 12,40: dark trousers, boots at the bottom
Box 0 40 3 7 3 @(46,40,40) 4
Box 12 40 3 7 3 @(46,40,40) 4
Rect 0 47 12 3 @(32,24,20) 3; Rect 12 47 12 3 @(32,24,20) 3
# sleeves 3x9x3 at 40,40 and 52,40, with pale bony hands at the ends
Box 40 40 3 9 3 $cloak 6
Box 52 40 3 9 3 $cloak 6
Rect 40 50 12 2 @(196,180,160) 6; Rect 52 50 12 2 @(196,180,160) 6
Rect 43 40 3 3 @(196,180,160) 4; Rect 55 40 3 3 @(196,180,160) 4
# the cane 1x13x1 at 24,40 and its knob 2x2x2 at 28,40
Box 24 40 1 13 1 @(104,72,44) 8
Box 28 40 2 2 2 @(62,40,24) 4
$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

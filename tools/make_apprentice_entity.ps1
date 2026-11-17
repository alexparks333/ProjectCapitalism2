# Draws the Apprentice's skin (textures/entity/apprentice.png, 64x64) for client/ApprenticeModel: a polished iron
# little figure (big shiny eyes, pink cheeks, a small smile, rivets and a violet rune gem on its chest) under a violet
# wizard hat with a gold band and a gold star. Violet and gold on purpose, so it doesn't look like Minecraft's witch.
# The boxes below match ApprenticeModel's texOffs and sizes (u, v, width, height, depth).
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity\apprentice.png'
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
$bmp = New-Object Drawing.Bitmap 64,64,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 7
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function P([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $x -lt 64 -and $y -ge 0 -and $y -lt 64){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Rect([int]$x,[int]$y,[int]$w,[int]$h,[int[]]$c,[int]$noise){
  for($j=0;$j -lt $h;$j++){ for($i=0;$i -lt $w;$i++){ $n = $rng.Next(-$noise, $noise + 1); P ($x+$i) ($y+$j) ($c[0]+$n) ($c[1]+$n) ($c[2]+$n) } } }
# Paint a box's six faces: top lighter, bottom darker, sides the base color
function Box([int]$u,[int]$v,[int]$w,[int]$h,[int]$d,[int[]]$base,[int]$noise){
  $top = @(($base[0]+22),($base[1]+22),($base[2]+22)); $bot = @(($base[0]-30),($base[1]-30),($base[2]-30))
  Rect ($u+$d) $v $w $d $top $noise
  Rect ($u+$d+$w) $v $w $d $bot $noise
  Rect $u ($v+$d) $d $h $base $noise
  Rect ($u+$d) ($v+$d) $w $h $base $noise
  Rect ($u+$d+$w) ($v+$d) $d $h @(($base[0]-12),($base[1]-12),($base[2]-12)) $noise
  Rect ($u+2*$d+$w) ($v+$d) $w $h @(($base[0]-8),($base[1]-8),($base[2]-8)) $noise }

$iron = @(200,204,210); $ironDark = @(158,162,170)
$violet = @(106,63,181); $gold = @(232,193,74)

# head 8x7x7 at (0,0), body 6x5x4 at (0,16), arms 2x4x2 at (24,16), legs 2x3x2 at (24,24)
Box 0 0 8 7 7 $iron 5
Box 0 16 6 5 4 $iron 5
Box 24 16 2 4 2 $iron 5
Box 24 24 2 3 2 $ironDark 5
# a shine line across the top of the head's front, and a darker chin row
for($i=0;$i -lt 8;$i++){ P (7+$i) 7 230 233 238; P (7+$i) 13 172 176 183 }

# the face (head front: x 7..14, y 7..13)
$fx = 7; $fy = 7
foreach($e in @(1,5)){
  P ($fx+$e) ($fy+2) 255 255 255; P ($fx+$e+1) ($fy+2) 42 36 64
  P ($fx+$e) ($fy+3) 42 36 64;    P ($fx+$e+1) ($fy+3) 60 50 96 }
P ($fx+1) ($fy+4) 242 160 184; P ($fx+6) ($fy+4) 242 160 184
P ($fx+3) ($fy+5) 90 58 74; P ($fx+4) ($fy+5) 90 58 74

# body front (x 4..9, y 20..24): rivets in the corners and a glowing violet rune gem
$bx = 4; $by = 20
foreach($p in @(@(0,0),@(5,0),@(0,4),@(5,4))){ P ($bx+$p[0]) ($by+$p[1]) 125 129 136 }
P ($bx+2) ($by+1) 201 164 255; P ($bx+3) ($by+1) 160 110 240
P ($bx+2) ($by+2) 140 90 220;  P ($bx+3) ($by+2) 201 164 255
# a belt line
for($i=0;$i -lt 20;$i++){ P $i ($by+3) 132 136 144 }

# the hat: brim 10x1x10 at (0,32), tiers 6x3x6 at (40,0), 4x3x4 at (40,12), 2x3x2 at (40,22), tip 1x2x1 at (50,22)
Box 0 32 10 1 10 $violet 6
Box 40 0 6 3 6 $violet 6
Box 40 12 4 3 4 $violet 6
Box 40 22 2 3 2 @(126,80,205) 6
Box 50 22 1 2 1 @(150,105,225) 4
# gold band round the bottom of the first tier (its side faces' last row)
for($i=40;$i -lt 64;$i++){ P $i 8 ($gold[0]) ($gold[1]) ($gold[2]) }
# a gold star on the hat's front (first tier front: x 46..51, y 6..8)
P 48 6 255 230 140; P 49 6 232 193 74; P 48 7 232 193 74; P 49 7 255 240 170; P 47 7 200 160 60; P 50 6 200 160 60
# the brim's rim, a touch darker
for($i=0;$i -lt 40;$i++){ P $i 42 80 45 140 }

$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

# Draws the Lapis Compressor menu (textures/gui/container/lapis_compressor.png, 176x166): a riveted dark-steel panel,
# the Lapis slot, the press chamber in the middle (the head and the lapis chunk are drawn by the screen), and the XP
# chute on the right. Positions must match client/LapisCompressorScreen and LapisCompressorMenu.
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\gui\container\lapis_compressor.png'
$W = 176; $H = 166
$bmp = New-Object Drawing.Bitmap 256,256,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 9
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt 256 -and $y -lt 256){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Rect([int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$b){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ Px $x $y $r $g $b } } }
# a sunken frame (x0..x1, y0..y1): dark top-left, light bottom-right
function Inset([int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$b){
  Rect ($x0+1) ($y0+1) ($x1-1) ($y1-1) $r $g $b
  for($x=$x0;$x -le $x1;$x++){ Px $x $y0 24 26 30; Px $x $y1 150 156 166 }
  for($y=$y0;$y -le $y1;$y++){ Px $x0 $y 24 26 30; Px $x1 $y 150 156 166 } }

# ── brushed steel panel with a bevel and rivets ──
for($y=0;$y -lt $H;$y++){ for($x=0;$x -lt $W;$x++){ $n=$rng.Next(-3,4); $streak = if((($y*7 + $x) % 23) -eq 0){ 6 } else { 0 }
  Px $x $y (96+$n+$streak) (101+$n+$streak) (110+$n+$streak) } }
for($x=0;$x -lt $W;$x++){ Px $x 0 20 22 26; Px $x ($H-1) 20 22 26; Px $x 1 170 176 186; Px $x ($H-2) 48 50 56 }
for($y=0;$y -lt $H;$y++){ Px 0 $y 20 22 26; Px ($W-1) $y 20 22 26; Px 1 $y 170 176 186; Px ($W-2) $y 48 50 56 }
foreach($p in @(@(5,5),@(170,5),@(5,160),@(170,160))){ Px $p[0] $p[1] 190 196 206; Px ($p[0]+1) ($p[1]+1) 40 42 48 }

# ── the Lapis slot (at 44,35) with a lapis-blue rim ──
Inset 43 34 60 51 34 36 44
for($x=41;$x -le 62;$x++){ Px $x 32 50 90 200; Px $x 53 30 60 150 }
for($y=32;$y -le 53;$y++){ Px 41 $y 50 90 200; Px 62 $y 30 60 150 }
# feed arrow into the press
for($x=64;$x -le 69;$x++){ Px $x 42 70 110 220 }
Px 68 41 70 110 220; Px 68 43 70 110 220

# ── the press chamber (inside 73..104 x 19..59), with hazard stripes along its lip ──
Inset 72 18 105 60 18 20 26
for($x=73;$x -le 104;$x++){ if(((($x - 73) / 3) % 2) -lt 1){ Px $x 61 220 180 40 } else { Px $x 61 30 30 30 } }
# floor plate
Rect 73 59 104 59 70 74 82

# ── the XP chute (orb at 128,37) with a green rim ──
Inset 115 24 141 50 14 18 14
for($x=113;$x -le 143;$x++){ Px $x 22 90 200 70; Px $x 52 50 130 40 }
for($y=22;$y -le 52;$y++){ Px 113 $y 90 200 70; Px 143 $y 50 130 40 }
for($x=107;$x -le 112;$x++){ Px $x 37 90 200 70 }

# ── inventory ──
for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Inset (7 + 18*$c) (83 + 18*$r) (24 + 18*$c) (100 + 18*$r) 52 56 62 } }
for($c=0;$c -lt 9;$c++){ Inset (7 + 18*$c) 141 (24 + 18*$c) 158 52 56 62 }

$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

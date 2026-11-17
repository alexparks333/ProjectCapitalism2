# Draws the Lapis Compressor (textures/block/lapis_compressor_*.png): a riveted dark-steel press. Sides show lapis being
# squeezed between two steel plates behind a window; the front is the chute the XP comes out of (dark when idle, glowing
# XP green when lit); the top is the press head. Models: models/block/lapis_compressor(_lit).json.
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$rng = New-Object System.Random 3
function New16 { New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px($b,[int]$x,[int]$y,[int]$r,[int]$g,[int]$bl){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $bl))) }
function Save($b,$name){ $b.Save((Join-Path $dir "$name.png"),[Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
# dark steel with a bevel and corner rivets
function Steel($b){
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-5,6); Px $b $x $y (72+$n) (76+$n) (84+$n) } }
  for($i=0;$i -lt 16;$i++){ Px $b $i 0 120 126 136; Px $b 0 $i 120 126 136; Px $b $i 15 34 36 42; Px $b 15 $i 34 36 42 }
  foreach($p in @(@(2,2),@(13,2),@(2,13),@(13,13))){ Px $b $p[0] $p[1] 150 156 166; Px $b ($p[0]+1) ($p[1]+1) 40 42 48 } }
# lapis: deep blue with lighter flecks
function Lapis($b,$x,$y){ $n=$rng.Next(-8,9); if($rng.Next(0,6) -eq 0){ Px $b $x $y 90 140 230 } else { Px $b $x $y (30+$n) (60+$n) (160+$n) } }

# ── side: a window with lapis squeezed between two steel plates ──
$b = New16; Steel $b
for($y=4;$y -le 11;$y++){ for($x=4;$x -le 11;$x++){ Lapis $b $x $y } }
for($x=3;$x -le 12;$x++){ Px $b $x 3 44 46 54; Px $b $x 4 140 146 156; Px $b $x 11 140 146 156; Px $b $x 12 44 46 54 }
for($y=3;$y -le 12;$y++){ Px $b 3 $y 44 46 54; Px $b 12 $y 44 46 54 }
Save $b 'lapis_compressor_side'

# ── top: the press head, a round plate with a lapis ring ──
$b = New16; Steel $b
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $d=[Math]::Sqrt([Math]::Pow($x-7.5,2)+[Math]::Pow($y-7.5,2))
  if($d -le 5.6 -and $d -ge 4.6){ Px $b $x $y 50 90 200 } elseif($d -lt 4.6){ $n=$rng.Next(-4,5); Px $b $x $y (98+$n) (104+$n) (114+$n) } } }
Px $b 7 7 60 64 72; Px $b 8 8 60 64 72; Px $b 7 8 60 64 72; Px $b 8 7 60 64 72
Save $b 'lapis_compressor_top'

# ── bottom: plain steel ──
$b = New16; Steel $b; Save $b 'lapis_compressor_bottom'

# ── front: the chute (idle: dark; lit: glowing XP green with a lapis rim) ──
foreach($lit in @($false,$true)){
  $b = New16; Steel $b
  for($y=5;$y -le 13;$y++){ for($x=4;$x -le 11;$x++){
    if($lit){ $d=[Math]::Abs($x-7.5)+[Math]::Abs($y-10); $g=[int](60-$d*9); Px $b $x $y (150+$g) (230+$g) (70+$g) }
    else { Px $b $x $y 16 16 22 } } }
  for($x=3;$x -le 12;$x++){ Px $b $x 4 50 90 200; Px $b $x 14 30 60 150 }
  for($y=4;$y -le 14;$y++){ Px $b 3 $y 50 90 200; Px $b 12 $y 30 60 150 }
  # a small gauge above the chute
  for($x=5;$x -le 10;$x++){ Px $b $x 2 20 22 28 }
  $fill = if($lit){ 10 } else { 5 }
  for($x=5;$x -le $fill;$x++){ Px $b $x 2 120 220 80 }
  Save $b ($(if($lit){'lapis_compressor_front_lit'}else{'lapis_compressor_front'}))
}
'saved'

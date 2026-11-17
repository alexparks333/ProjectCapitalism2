# Draws the Enchanter's Table block textures (textures/block/enchanters_table_*.png): dark oak sides framing a deep
# violet panel with a glowing rune, a velvet top with a faint rune circle, a red leather book cover and its pages.
# The candles are vanilla (purple_candle_lit). Model: models/block/enchanters_table.json.
# Needs vanilla dark_oak_planks.png unzipped first, e.g. (Git Bash, from the project folder):
#   cd /tmp/vr && unzip -o .../.gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/block/dark_oak_planks.png'
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$vanilla = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\block'
$rng = New-Object System.Random 5
function New16 { New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px($b,[int]$x,[int]$y,[int]$r,[int]$g,[int]$bl){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $bl))) }
function Save($b,$name){ $b.Save((Join-Path $dir "$name.png"),[Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
$oak = [Drawing.Bitmap]::FromFile((Join-Path $vanilla 'dark_oak_planks.png'))
function Wood($b,$x,$y){ $c=$oak.GetPixel($x,$y); Px $b $x $y $c.R $c.G $c.B }

# ── side (the model uses rows 4-15): dark oak frame, violet panel, a glowing rune in the middle ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  $frame = $x -le 1 -or $x -ge 14 -or $y -le 5 -or $y -ge 14
  if($frame){ Wood $b $x $y } else { $n=$rng.Next(-4,5); Px $b $x $y (40+$n) (24+$n) (66+$n) } } }
# inner shadow / highlight of the panel
for($x=2;$x -le 13;$x++){ Px $b $x 6 18 10 30; Px $b $x 13 84 62 120 }
for($y=6;$y -le 13;$y++){ Px $b 2 $y 18 10 30; Px $b 13 $y 84 62 120 }
# the rune: an eye-like glyph
$rune = @('..XXXX..','.X....X.','X..XX..X','.X....X.','..XXXX..')
for($r=0;$r -lt 5;$r++){ for($c=0;$c -lt 8;$c++){ if($rune[$r][$c] -eq 'X'){ Px $b (4+$c) (7+$r) 190 130 255 } } }
Px $b 7 9 240 220 255; Px $b 8 9 240 220 255
Save $b 'enchanters_table_side'

# ── top: dark oak rim, purple velvet with a faint rune circle ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ Wood $b $x $y; continue }
  $n=$rng.Next(-5,6); $weave = if((($x + $y) % 2) -eq 0){ 4 } else { 0 }
  $d=[Math]::Sqrt([Math]::Pow($x-7.5,2)+[Math]::Pow($y-7.5,2))
  if($d -ge 5.5 -and $d -le 6.4){ Px $b $x $y 120 70 170 } else { Px $b $x $y (78+$n+$weave) (30+$n) (96+$n+$weave) } } }
Save $b 'enchanters_table_top'

# ── book cover: red leather, a darker spine down the middle, a faint violet sigil ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-6,7)
  if($x -eq 7 -or $x -eq 8){ Px $b $x $y (70+$n) (20+$n) (22+$n) } else { Px $b $x $y (124+$n) (36+$n) (34+$n) } } }
Save $b 'enchanters_table_book_cover'

# ── pages: cream, the left page x 0-7 and the right page x 8-15, lines of violet runes ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-5,6)
  $shade = if($x -eq 7 -or $x -eq 8){ -24 } else { 0 }
  Px $b $x $y (230+$n+$shade) (220+$n+$shade) (190+$n+$shade) } }
for($y=3;$y -le 12;$y+=3){ foreach($x in @(2,3,4,5,10,11,12,13)){ if($rng.Next(0,4) -ne 0){ Px $b $x $y 110 60 160 } } }
Save $b 'enchanters_table_pages'
$oak.Dispose()
'saved'

# Draws the Apprentice Wand (textures/item/apprentice_wand.png) and the Apprentice item (textures/item/apprentice.png).
#  - Wand: the vanilla Stick's shape in bamboo greens, with a glowing poison-green orb on its tip and two sparkles.
#  - Apprentice: the little iron figure in its violet wizard hat (drawn pixel by pixel).
# Needs the vanilla textures unzipped first, e.g. (Git Bash, from the project folder):
#   unzip -o -d "$LOCALAPPDATA/Temp/vr" .gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/item/stick.png'
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'
$vr = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\item'
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }

# ── Wand ──
$stick = [Drawing.Bitmap]::FromFile((Join-Path $vr 'stick.png'))
$wand = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
function W([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16){ $wand.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
$tipX = 0; $tipY = 16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $stick.GetPixel($x,$y)
  if($c.A -gt 0){
    # stick brightness -> bamboo green; a darker node ring every few pixels
    $l = ($c.R + $c.G + $c.B) / 3
    $node = (($x + $y) % 5) -eq 0
    if($node){ W $x $y 92 120 36 } else { W $x $y ([int](70 + $l * 0.55)) ([int](110 + $l * 0.45)) ([int](30 + $l * 0.15)) }
    if($y -lt $tipY){ $tipY = $y; $tipX = $x } } } }
# poison orb on the tip: dark rim, bright green core, white glint
$ox = [Math]::Min(13, $tipX); $oy = [Math]::Max(2, $tipY)
for($dy=-2;$dy -le 2;$dy++){ for($dx=-2;$dx -le 2;$dx++){ $d = [Math]::Sqrt($dx*$dx + $dy*$dy)
  if($d -le 1.2){ W ($ox+$dx) ($oy+$dy) 120 230 60 } elseif($d -le 2.3){ W ($ox+$dx) ($oy+$dy) 40 120 30 } } }
W ($ox-1) ($oy-1) 230 255 210
# sparkles
W ($ox-4) ($oy+1) 180 255 140
W ($ox+1) ($oy+4) 140 230 110
$wand.Save((Join-Path $dir 'apprentice_wand.png'),[Drawing.Imaging.ImageFormat]::Png)
$wand.Dispose(); $stick.Dispose()

# ── Apprentice: the little iron figure in its violet wizard hat (like the entity, client/ApprenticeModel) ──
$app = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
function A([int]$x,[int]$y,[int[]]$c){ if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16){ $app.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,$c[0],$c[1],$c[2])) } }
function Row([int]$y,[int]$x0,[int]$x1,[int[]]$c){ for($x=$x0;$x -le $x1;$x++){ A $x $y $c } }
$vl = @(150,105,225); $v = @(106,63,181); $vd = @(74,42,133); $gold = @(232,193,74); $star = @(255,236,150)
$iron = @(200,204,210); $ironL = @(228,231,236); $ironD = @(150,154,162); $eye = @(42,36,64); $white = @(255,255,255); $pink = @(242,160,184)
# hat: tip leaning right, tiers, gold band, wide brim
A 10 0 $vl
Row 1 9 10 $vl
Row 2 8 10 $v; A 8 2 $vl
Row 3 7 10 $v; A 7 3 $vl
Row 4 6 10 $v; A 8 4 $star
Row 5 5 11 $gold
Row 6 3 12 $vd; Row 6 4 11 $v
# head
Row 7 4 11 $ironL
for($y=8;$y -le 11;$y++){ Row $y 4 11 $iron; A 4 $y $ironD; A 11 $y $ironD }
A 5 8 $white; A 6 8 $eye; A 5 9 $eye; A 6 9 $eye
A 9 8 $white; A 10 8 $eye; A 9 9 $eye; A 10 9 $eye
A 5 10 $pink; A 10 10 $pink; A 7 10 @(90,58,74); A 8 10 @(90,58,74)
Row 11 4 11 $ironD
# body, arms, rune gem
Row 12 5 10 $iron; Row 13 5 10 $iron
A 4 12 $ironD; A 4 13 $ironD; A 11 12 $ironD; A 11 13 $ironD
A 7 12 @(201,164,255); A 8 12 @(160,110,240)
# legs
A 5 14 $ironD; A 6 14 $ironD; A 9 14 $ironD; A 10 14 $ironD
A 5 15 $ironD; A 6 15 $ironD; A 9 15 $ironD; A 10 15 $ironD
$app.Save((Join-Path $dir 'apprentice.png'),[Drawing.Imaging.ImageFormat]::Png)
$app.Dispose()
'saved'

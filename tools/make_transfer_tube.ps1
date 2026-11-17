# Draws the Transfer Tube's own textures (textures/block/transfer_tube_*.png): an obsidian floor with a violet rune ring,
# violet glass, and an oak sign board with a burnt-in arrow (right / left). Everything else on the tube is vanilla
# (dark oak planks and logs, a bookshelf, polished blackstone, crying obsidian). Models: models/block/transfer_tube_*.json;
# the item model is built from them by make_transfer_tube_item.pl.
# The sign board needs vanilla oak_planks.png unzipped first, e.g. (Git Bash, from the project folder):
#   cd /tmp/vr && unzip -o .../.gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/block/oak_planks.png'
Add-Type -AssemblyName System.Drawing
$blockDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$vanilla = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\block'
$rng = New-Object System.Random 7

function New16 { New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px($b,[int]$x,[int]$y,[int]$r,[int]$g,[int]$bl,[int]$a=255){ if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb((C $a),(C $r),(C $g),(C $bl))) } }
function Save($b,$name){ $b.Save((Join-Path $blockDir "$name.png"),[Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# ── floor: obsidian with a violet rune ring, a darker edge ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $n=$rng.Next(-6,7); $fleck = if($rng.Next(0,14) -eq 0){ 18 } else { 0 }
  Px $b $x $y (30+$n+$fleck) (20+$n) (44+$n+$fleck*2) } }
for($i=0;$i -lt 16;$i++){ Px $b $i 0 14 9 22; Px $b $i 15 14 9 22; Px $b 0 $i 14 9 22; Px $b 15 $i 14 9 22 }
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $d=[Math]::Sqrt([Math]::Pow($x-7.5,2)+[Math]::Pow($y-7.5,2))
  if($d -ge 4.6 -and $d -le 5.6){ Px $b $x $y 170 100 225 }
  elseif($d -lt 1.6){ Px $b $x $y 210 170 240 } } }
foreach($p in @(@(7,3),@(8,12),@(3,8),@(12,7))){ Px $b $p[0] $p[1] 110 190 230 }
Save $b 'transfer_tube_floor'

# ── glass: see-through violet with a streak ──
$b = New16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ Px $b $x $y 170 120 230 60 } }
# no rim: the posts frame it, and the two booth blocks join without a line
for($i=3;$i -lt 9;$i++){ Px $b $i ($i-1) 245 235 255 150; Px $b ($i+1) ($i-1) 245 235 255 110 }
Save $b 'transfer_tube_glass'

# ── sign board: oak planks with a dark burnt-in arrow (right / left) ──
$oak = [Drawing.Bitmap]::FromFile((Join-Path $vanilla 'oak_planks.png'))
foreach($dir in @('right','left')){
  $b = New16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c=$oak.GetPixel($x,$y); Px $b $x $y $c.R $c.G $c.B } }
  # arrow pointing right: shaft rows 7-8 from x 3 to 8, head from x 9 to 12
  $pts = New-Object System.Collections.ArrayList
  for($x=3;$x -le 8;$x++){ [void]$pts.Add(@($x,7)); [void]$pts.Add(@($x,8)) }
  for($k=0;$k -le 3;$k++){ for($y=(4+$k);$y -le (11-$k);$y++){ [void]$pts.Add(@((9+$k),$y)) } }
  foreach($p in $pts){ $x = if($dir -eq 'right'){ $p[0] } else { 15 - $p[0] }
    $c = $oak.GetPixel($x,$p[1]); $shade = $rng.Next(0,10)
    Px $b $x $p[1] (46+$shade) (28+$shade) (16+$shade) }
  # a scorched rim around the arrow
  foreach($p in $pts){ foreach($o in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){ $x = $(if($dir -eq 'right'){ $p[0] } else { 15 - $p[0] }) + $o[0]; $y = $p[1] + $o[1]
    if($x -lt 0 -or $y -lt 0 -or $x -gt 15 -or $y -gt 15){ continue }
    $c = $b.GetPixel($x,$y); if($c.R -gt 90){ Px $b $x $y ([int]($c.R*0.72)) ([int]($c.G*0.68)) ([int]($c.B*0.64)) } } }
  Save $b "transfer_tube_sign_$dir"
}
$oak.Dispose()
'saved'

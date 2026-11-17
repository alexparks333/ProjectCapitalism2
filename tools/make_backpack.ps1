# Draws item/backpack (Back.md): a brown leather backpack seen from the front, a top flap with a brass buckle, a
# front pocket and a carry loop on top. A plain crafting ingredient for now (the Animal Bjorn); the Designer will make them.
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function P($bmp,[int]$x,[int]$y,[int[]]$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $c[0]),(C $c[1]),(C $c[2]))) } }
$rng = New-Object Random 7

$outline = @(52,30,16); $body = @(140,86,48); $shade = @(112,66,36); $light = @(170,110,64)
$flap = @(122,74,40); $pocket = @(128,78,42); $brass = @(222,180,70); $brassDark = @(160,120,40); $stitch = @(196,150,100)

$bp = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
# carry loop
foreach($x in 6..9){ P $bp $x 1 $outline }; P $bp 5 2 $outline; P $bp 10 2 $outline; P $bp 6 2 $shade; P $bp 9 2 $shade
# body (rounded top corners)
for($y=3;$y -le 14;$y++){ for($x=3;$x -le 12;$x++){
  if(($y -eq 3) -and ($x -eq 3 -or $x -eq 12)){ continue }
  $edge = ($x -eq 3 -or $x -eq 12 -or $y -eq 14 -or ($y -eq 3) -or ($y -eq 4 -and ($x -eq 3 -or $x -eq 12)))
  $n = $rng.Next(-6,7)
  if($edge){ P $bp $x $y $outline }
  elseif($x -eq 4){ P $bp $x $y @(($light[0]+$n),($light[1]+$n),($light[2]+$n)) }
  elseif($x -eq 11){ P $bp $x $y @(($shade[0]+$n),($shade[1]+$n),($shade[2]+$n)) }
  else { P $bp $x $y @(($body[0]+$n),($body[1]+$n),($body[2]+$n)) } } }
# top flap with a stitched edge
for($y=4;$y -le 7;$y++){ for($x=4;$x -le 11;$x++){ $n = $rng.Next(-5,6); P $bp $x $y @(($flap[0]+$n),($flap[1]+$n),($flap[2]+$n)) } }
foreach($x in 4..11){ P $bp $x 8 $outline }
foreach($x in @(5,7,9,11)){ P $bp $x 7 $stitch }
# buckle strap and brass buckle
foreach($y in 6..10){ P $bp 7 $y $shade; P $bp 8 $y $shade }
P $bp 7 8 $brass; P $bp 8 8 $brass; P $bp 7 9 $brassDark; P $bp 8 9 $brass
# front pocket
for($y=10;$y -le 13;$y++){ for($x=5;$x -le 10;$x++){ $n = $rng.Next(-5,6); P $bp $x $y @(($pocket[0]+$n),($pocket[1]+$n),($pocket[2]+$n)) } }
foreach($x in 5..10){ P $bp $x 10 $outline }
P $bp 5 11 $outline; P $bp 10 11 $outline; P $bp 5 12 $outline; P $bp 10 12 $outline; P $bp 5 13 $outline; P $bp 10 13 $outline
foreach($x in @(6,8)){ P $bp $x 12 $stitch }
$bp.Save((Join-Path $res 'backpack.png'),[Drawing.Imaging.ImageFormat]::Png); $bp.Dispose()
'backpack saved'

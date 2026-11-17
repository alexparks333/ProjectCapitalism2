# Draws item/training_feed (PetTrainer.md > Training Feed): a little burlap feed sack, open at the top, heaped with seeds
# and red bits of raw meat.
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function P($bmp,[int]$x,[int]$y,[int[]]$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $c[0]),(C $c[1]),(C $c[2]))) } }
$rng = New-Object Random 11

$outline = @(70,48,24); $sack = @(196,160,104); $sackDark = @(160,124,74); $tie = @(120,80,40)
$bmp = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
# the sack: wider at the bottom, rows 7..14
for($y=7;$y -le 14;$y++){
  $half = if($y -le 8){ 4 } elseif($y -le 12){ 5 } else { 5 }
  for($x=(8-$half);$x -le (7+$half);$x++){
    $edge = ($x -eq 8-$half -or $x -eq 7+$half -or $y -eq 14)
    $n = $rng.Next(-8,9)
    if($edge){ P $bmp $x $y $outline }
    elseif($x -ge 9){ P $bmp $x $y @(($sackDark[0]+$n),($sackDark[1]+$n),($sackDark[2]+$n)) }
    else { P $bmp $x $y @(($sack[0]+$n),($sack[1]+$n),($sack[2]+$n)) } } }
# a band round the neck
foreach($x in 4..11){ P $bmp $x 9 $tie }
# the heap on top: seeds and meat, rows 3..8
$seedCols = @(@(232,214,120),@(200,180,90),@(120,170,70),@(236,236,220))
$meatCols = @(@(200,60,60),@(230,110,110),@(170,40,40))
for($y=3;$y -le 8;$y++){
  $half = [int](1 + ($y - 3) * 0.8)
  for($x=(8-$half);$x -le (7+$half);$x++){
    $c = if($rng.Next(0,4) -eq 0){ $meatCols[$rng.Next(0,3)] } else { $seedCols[$rng.Next(0,4)] }
    P $bmp $x $y $c } }
P $bmp 7 2 @(230,110,110); P $bmp 8 2 @(232,214,120)
$bmp.Save((Join-Path $res 'training_feed.png'),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
'training feed saved'

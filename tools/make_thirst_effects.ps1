# Draws the Camel pet's two effect icons (textures/mob_effect, 18x18, PetAnimals.md):
#  - refill_thirst: a full water drop with a white plus (fills the thirst bar at once)
#  - no_water_draining: a water drop held in a gold ring (the thirst bar doesn't go down)
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\mob_effect'
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function P($bmp,[int]$x,[int]$y,[int[]]$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt 18 -and $y -lt 18){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $c[0]),(C $c[1]),(C $c[2]))) } }

# a drop centred at (cx, 10): pointed top, round bottom; shaded light top-left, dark bottom-right
function Drop($bmp,[double]$cx,[double]$r){
  for($y=0;$y -lt 18;$y++){ for($x=0;$x -lt 18;$x++){
    $dx = $x + 0.5 - $cx; $dy = $y + 0.5 - 11
    $inside = if($dy -ge 0){ ($dx*$dx + $dy*$dy) -le $r*$r } else { [Math]::Abs($dx) -le $r * (1 + $dy / ($r * 1.9)) -and $dy -ge -$r*1.9 }
    if($inside){
      $edge = $false
      foreach($o in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){
        $ex = $dx + $o[0]; $ey = $dy + $o[1]
        $in2 = if($ey -ge 0){ ($ex*$ex + $ey*$ey) -le $r*$r } else { [Math]::Abs($ex) -le $r * (1 + $ey / ($r * 1.9)) -and $ey -ge -$r*1.9 }
        if(-not $in2){ $edge = $true } }
      if($edge){ P $bmp $x $y @(20,50,110) }
      else { $s = ($dx + $dy) * 6; P $bmp $x $y @((60 - $s),(140 - $s),(230 - $s)) } } } }
}

$a = New-Object Drawing.Bitmap 18,18,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
Drop $a 9 5.5
P $a 6 9 @(200,230,255); P $a 6 10 @(200,230,255); P $a 7 8 @(200,230,255)    # shine
foreach($d in -2..2){ P $a (11 + $d) 12 @(255,255,255); P $a 11 (12 + $d) @(255,255,255) }   # plus
$a.Save((Join-Path $out 'refill_thirst.png'),[Drawing.Imaging.ImageFormat]::Png); $a.Dispose()

$b = New-Object Drawing.Bitmap 18,18,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
# gold ring round the drop
for($y=0;$y -lt 18;$y++){ for($x=0;$x -lt 18;$x++){
  $d = [Math]::Sqrt([Math]::Pow($x + 0.5 - 9, 2) + [Math]::Pow($y + 0.5 - 9, 2))
  if($d -ge 7.4 -and $d -le 8.8){ $g = if($y -lt 9){ 40 } else { 0 }; P $b $x $y @((220 + $g),(170 + $g),40) } } }
Drop $b 9 4.2
P $b 7 9 @(200,230,255); P $b 7 10 @(200,230,255)
$b.Save((Join-Path $out 'no_water_draining.png'),[Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
'thirst effects saved'

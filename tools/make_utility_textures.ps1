# The Designer's Utility things (Designer.md > Utility): the Frozen Patch and the Freezer Sling (its icon and how it
# looks on a player's back), made from the Silk Patch and the Fancy Food Satchel recoloured to ice: an icy blue (each
# pixel keeps its light and shade), white frost specks, and on the sling a little snowflake and icicles along its
# bottom edge. Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_utility_textures.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'

function Hash([double]$x,[double]$y,[double]$s){ $v=[Math]::Sin($x*12.9898+$y*78.233+$s*37.719)*43758.5453; $v-[Math]::Floor($v) }
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,[Math]::Round($v))) }

# Recolour to ice: each pixel's brightness onto a ramp from deep ice blue to frost white, with specks of frost
function Icy($src, $dst, $seed, [double]$frost){
  $b = New-Object Drawing.Bitmap $src
  $out = New-Object Drawing.Bitmap $b.Width, $b.Height
  for($y=0;$y -lt $b.Height;$y++){ for($x=0;$x -lt $b.Width;$x++){
    $c = $b.GetPixel($x,$y)
    if($c.A -lt 8){ continue }
    $l = ($c.R*0.3 + $c.G*0.59 + $c.B*0.11) / 255
    $l = [Math]::Pow($l, 1.6)
    $r = 30 + 190*$l; $g = 80 + 160*$l; $bl = 150 + 105*$l
    if((Hash $x $y $seed) -lt $frost){ $r = 240; $g = 250; $bl = 255 }
    $out.SetPixel($x,$y,[Drawing.Color]::FromArgb($c.A,(C $r),(C $g),(C $bl)))
  } }
  $b.Dispose()
  ,$out
}

$patch = Icy (Join-Path $tex 'item\silk_patch.png') 'frozen_patch' 3 0.08
$patch.Save((Join-Path $tex 'item\frozen_patch.png'))

$sling = Icy (Join-Path $tex 'item\fancy_food_satchel.png') 'freezer_sling' 5 0.06
# a little snowflake on the flap
foreach($p in @(@(8,7),@(7,7),@(9,7),@(8,6),@(8,8),@(7,6),@(9,8),@(7,8),@(9,6))){
  if($sling.GetPixel($p[0],$p[1]).A -gt 0){ $sling.SetPixel($p[0],$p[1],[Drawing.Color]::FromArgb(255,250,252,255)) }
}
$sling.Save((Join-Path $tex 'item\freezer_sling.png'))

$back = Icy (Join-Path $tex 'entity\bag_fancy_food_satchel.png') 'bag_freezer_sling' 7 0.05
# icicles hanging off the bottom edges of the painted parts
for($x=0;$x -lt $back.Width;$x++){
  for($y=$back.Height-2;$y -ge 1;$y--){
    $here = $back.GetPixel($x,$y); $below = $back.GetPixel($x,$y+1)
    if($here.A -gt 0 -and $below.A -eq 0 -and (Hash $x $y 11) -lt 0.35){
      $back.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,214,240,255))
    }
  }
}
$back.Save((Join-Path $tex 'entity\bag_freezer_sling.png'))
"made the utility textures"

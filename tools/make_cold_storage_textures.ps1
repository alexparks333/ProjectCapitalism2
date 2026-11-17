# The Fridge and Freezer blocks (farmer/ColdStorageBlock) and the Rotten Meat item (farmer/Freshness). Fridge: an
# upright white fridge, a freezer door over the main door, chrome handles, a little light (green while it runs).
# Freezer: an icy-blue chest freezer gone frosty all over (frost creeping in from the edges, icicles hanging under
# the lid, a frosted lid), a chrome handle, a grille at the bottom, a blue light while it runs.
# Rotten Meat: a grey-green lump with mould and a fly. Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_cold_storage_textures.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'

function Hash([double]$x,[double]$y,[double]$s){ $v=[Math]::Sin($x*12.9898+$y*78.233+$s*37.719)*43758.5453; $v-[Math]::Floor($v) }
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,[Math]::Round($v))) }
function Px($b,$x,$y,$c){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $c[0]),(C $c[1]),(C $c[2]))) }
function Shade($c,[double]$f){ @(($c[0]*$f),($c[1]*$f),($c[2]*$f)) }

$WHITE = @(234,239,242); $EDGE = @(168,178,188); $CHROME = @(200,206,214); $CHROME_HI = @(250,252,255); $CHROME_LO = @(128,136,146)

# white enamel with a soft top-to-bottom sheen and a darker 1px edge
function Enamel($seed){
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $f = 1.02 - $y*0.006 - [Math]::Abs($x-6)*0.002 + ((Hash $x $y $seed)-0.5)*0.025
    $c = Shade $WHITE $f
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $EDGE }
    Px $b $x $y $c
  } }
  ,$b
}
function Handle($b,$x,$y0,$y1){
  for($y=$y0;$y -le $y1;$y++){ Px $b $x $y $CHROME_HI; Px $b ($x+1) $y $CHROME_LO }
  Px $b $x $y0 $CHROME; Px $b $x $y1 $CHROME
}
function Light($b,$x,$y,$on,$col){
  $c = $(if($on){ $col } else { @(70,76,84) })
  Px $b $x $y $c
  if($on){ Px $b ($x+1) $y (Shade $col 0.7) } else { Px $b ($x+1) $y @(56,60,66) }
}
function Save($b,$path){ New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null; $b.Save($path, [Drawing.Imaging.ImageFormat]::Png) }

# ── the Fridge ──
foreach($on in $false,$true){
  $b = Enamel 1
  # the freezer door over the main door
  for($x=1;$x -lt 15;$x++){ Px $b $x 5 $EDGE; Px $b $x 6 (Shade $WHITE 1.05) }
  Handle $b 12 1 3
  Handle $b 12 8 13
  Light $b 2 13 $on @(90,230,110)
  Save $b (Join-Path $res "block\fridge_front$(if($on){'_on'}).png")
}
$b = Enamel 2; for($x=1;$x -lt 15;$x++){ Px $b $x 5 (Shade $WHITE 0.92) }; Save $b (Join-Path $res 'block\fridge_side.png')
$b = Enamel 3; for($x=3;$x -lt 13;$x+=2){ for($y=11;$y -lt 14;$y++){ Px $b $x $y (Shade $WHITE 0.8) } }; Save $b (Join-Path $res 'block\fridge_top.png')

# ── the Freezer ──
$ICE = @(140,194,232); $ICE_EDGE = @(70,120,170); $FROST = @(236,248,255); $ICICLE = @(226,246,255); $ICICLE_TIP = @(110,170,220)
# icy-blue body with frost creeping in from the edges (thicker at the corners), and glinting ice crystals
function Icy($seed){
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $f = 1.03 - $y*0.008 + ((Hash $x $y $seed)-0.5)*0.05
    $c = Shade $ICE $f
    $edge = [Math]::Min([Math]::Min($x,15-$x),[Math]::Min($y,15-$y))
    $corner = [Math]::Min([Math]::Min($x,15-$x),1) + [Math]::Min([Math]::Min($y,15-$y),1)
    $reach = 0.6 + 1.5*(Hash ([Math]::Floor($x/2)) ([Math]::Floor($y/2)) ($seed+3))
    if($edge -lt $reach -or ($corner -le 1 -and $edge -lt $reach+1.5)){ $c = $(if((Hash $x $y ($seed+5)) -lt 0.5){ $FROST } else { Shade $FROST 0.94 }) }
    if((Hash $x $y ($seed+7)) -gt 0.95){ $c = @(255,255,255) }
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $ICE_EDGE }
    Px $b $x $y $c
  } }
  ,$b
}
# icicles hanging under a line (the lid's edge), of random lengths
function Icicles($b,$y0,$seed){
  for($x=1;$x -lt 15;$x++){
    $len = [int](1 + 4.2*(Hash $x 0 $seed))
    if(($x % 2) -eq 1){ $len = [Math]::Max(0,$len-1) }
    for($k=0;$k -lt $len;$k++){ Px $b $x ($y0+$k) $(if($k -eq $len-1){ $ICICLE_TIP } else { $ICICLE }) }
  }
}
foreach($on in $false,$true){
  $b = Icy 4
  # the lid (frosted white), its edge, icicles under it, its handle, the grille
  for($x=1;$x -lt 15;$x++){ for($y=1;$y -le 3;$y++){ Px $b $x $y $(if((Hash $x $y 31) -lt 0.6){ $FROST } else { Shade $FROST 0.95 }) }; Px $b $x 4 $ICE_EDGE }
  Icicles $b 5 9
  for($x=6;$x -le 9;$x++){ Px $b $x 5 $CHROME_HI; Px $b $x 6 $CHROME_LO }
  for($y=11;$y -le 13;$y+=2){ for($x=3;$x -le 9;$x++){ Px $b $x $y (Shade $ICE 0.62) } }
  Light $b 12 12 $on @(110,220,255)
  Save $b (Join-Path $res "block\freezer_front$(if($on){'_on'}).png")
}
$b = Icy 5
for($x=1;$x -lt 15;$x++){ for($y=1;$y -le 3;$y++){ Px $b $x $y (Shade $FROST 0.97) }; Px $b $x 4 $ICE_EDGE }
Icicles $b 5 13
Save $b (Join-Path $res 'block\freezer_side.png')
# the lid from above: thick frost, a frosted seam round it, a few crystals
$b = New-Object Drawing.Bitmap 16,16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  $c = $(if((Hash $x $y 41) -lt 0.55){ $FROST } else { Shade $FROST 0.93 })
  if((Hash ([Math]::Floor($x/3)) ([Math]::Floor($y/3)) 43) -gt 0.75){ $c = Shade $ICE 1.08 }
  if($x -eq 2 -or $x -eq 13 -or $y -eq 2 -or $y -eq 13){ if($x -ge 2 -and $x -le 13 -and $y -ge 2 -and $y -le 13){ $c = Shade $ICE 0.92 } }
  if((Hash $x $y 47) -gt 0.95){ $c = @(255,255,255) }
  if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = $ICE_EDGE }
  Px $b $x $y $c
} }
Save $b (Join-Path $res 'block\freezer_top.png')

# ── Rotten Meat ──
$b = New-Object Drawing.Bitmap 16,16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  $u = ($x+0.5-8)/6.2; $v = ($y+0.5-9)/4.6
  $r = [Math]::Sqrt($u*$u+$v*$v) + 0.18*[Math]::Sin([Math]::Atan2($v,$u)*3+1)
  if($r -gt 1){ continue }
  $c = @(118,112,74)
  $n = Hash $x $y 9
  if($n -lt 0.25){ $c = @(92,86,58) } elseif($n -gt 0.82){ $c = @(150,96,82) }
  if((Hash $x $y 11) -gt 0.88){ $c = @(198,206,168) }   # mould
  if($r -gt 0.86){ $c = Shade $c 0.62 }                  # the dark rim
  elseif($y -lt 7){ $c = Shade $c 1.12 }                 # lit from above
  Px $b $x $y $c
} }
# a fly
Px $b 12 2 @(30,30,34); Px $b 11 1 @(170,190,210); Px $b 13 1 @(170,190,210)
Save $b (Join-Path $res 'item\rotten_meat.png')
"made the cold storage textures"

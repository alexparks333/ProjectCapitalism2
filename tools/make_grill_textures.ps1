# The Grill block (farmer/GrillBlock: black iron, a grate over coals that glow brighter while a steak is on) and the
# Seared cuts (farmer/Sear): each made from its raw cut's texture, the meat browned to a golden crust, the fat
# toasted, the bone left as it is, with dark grill marks across it. Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_grill_textures.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'

function Hash([double]$x,[double]$y,[double]$s){ $v=[Math]::Sin($x*12.9898+$y*78.233+$s*37.719)*43758.5453; $v-[Math]::Floor($v) }
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,[Math]::Round($v))) }
function Px($b,$x,$y,$r,$g,$bl,$a=255){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb($a,(C $r),(C $g),(C $bl))) }
function Mix($a,$b,[double]$t){ @(($a[0]+($b[0]-$a[0])*$t),($a[1]+($b[1]-$a[1])*$t),($a[2]+($b[2]-$a[2])*$t)) }

# ── the Seared cuts ──
$ids = 'beef_chuck','beef_brisket','beef_round','beef_rib','pork_shoulder','pork_belly','pork_ham','pork_loin',
       'mutton_shoulder','mutton_leg','mutton_rack','chicken_leg_quarter','chicken_breast','rabbit_hind_legs','rabbit_saddle'
foreach($id in $ids){
  $raw = New-Object Drawing.Bitmap (Join-Path $tex "item\raw_$id.png")
  $out = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $c = $raw.GetPixel($x,$y)
    if($c.A -lt 10){ continue }
    $rgb = @($c.R,$c.G,$c.B)
    $bright = ($c.R + $c.G + $c.B) / 3
    $sat = [Math]::Max($c.R,[Math]::Max($c.G,$c.B)) - [Math]::Min($c.R,[Math]::Min($c.G,$c.B))
    if($bright -gt 205 -and $sat -lt 50){
      # bone / fat: fat toasts golden, bone stays
      $n = Mix $rgb @(236,196,120) 0.45
    } elseif($c.R -gt $c.G + 30){
      # meat: browned to a crust, darker at the edges of the shape
      $edge = $false
      foreach($o in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){ $xx=$x+$o[0]; $yy=$y+$o[1]; if($xx -lt 0 -or $yy -lt 0 -or $xx -gt 15 -or $yy -gt 15 -or $raw.GetPixel($xx,$yy).A -lt 10){ $edge = $true } }
      $crust = $(if($edge){ @(96,52,26) } else { @(150,86,44) })
      $n = Mix $rgb $crust 0.72
      $n = Mix $n @(190,120,60) ((Hash $x $y 3) * 0.25)
    } else {
      $n = Mix $rgb @(120,70,40) 0.4
    }
    # grill marks: dark diagonal stripes
    if((($x + $y) % 5) -eq 0 -and -not ($bright -gt 205 -and $sat -lt 50)){ $n = Mix $n @(40,22,12) 0.75 }
    Px $out $x $y $n[0] $n[1] $n[2] $c.A
  } }
  $raw.Dispose()
  $out.Save((Join-Path $tex "item\seared_$id.png"), [Drawing.Imaging.ImageFormat]::Png)
}

# ── the Grill block ──
function Iron($seed){
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $v = 38 + (Hash $x $y $seed)*14 - $y*0.6
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $v = 22 }
    Px $b $x $y $v $v ($v+4)
  } }
  ,$b
}
$b = Iron 1
# the side: a vent of slots and a little door with a handle
for($y=4;$y -le 10;$y+=3){ for($x=3;$x -le 12;$x++){ Px $b $x $y 14 14 16 } }
for($x=6;$x -le 9;$x++){ Px $b $x 13 150 154 160 }
$b.Save((Join-Path $tex 'block\grill_side.png'))
$b = Iron 2; $b.Save((Join-Path $tex 'block\grill_bottom.png'))
foreach($lit in $false,$true){
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $g = (Hash $x $y 7)
    if($lit){ $r = 170 + $g*80; $gg = 50 + $g*90; $bl = 10 } else { $r = 60 + $g*40; $gg = 26 + $g*14; $bl = 16 }
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $r = 22; $gg = 22; $bl = 26 }
    elseif(($x % 3) -eq 1){ $r = 74; $gg = 74; $bl = 80; if($lit){ $r = 110; $gg = 90; $bl = 80 } }   # the grate's bars
    Px $b $x $y $r $gg $bl
  } }
  $b.Save((Join-Path $tex "block\grill_top$(if($lit){'_lit'}).png"))
}
"made the grill textures"

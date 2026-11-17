# The Butcher Knives (farmer/ButcherKnifeItem), handle bottom-left, blade top-right, blood smeared along the edge.
# Iron: a broad butcher's knife with a curved belly and a riveted wooden handle. Diamond: a heavy square cleaver with a
# hanging hole, a black handle with gold rivets and a gold bolster, its blade glinting. Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_butcher_knives.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'

function Hash([double]$x,[double]$y,[double]$s){ $v=[Math]::Sin($x*12.9898+$y*78.233+$s*37.719)*43758.5453; $v-[Math]::Floor($v) }

$kinds = @{
  'iron_butcher_knife'    = @{ shape='knife';   spine=@(92,98,108); body=@(176,184,194); light=@(214,222,230); edge=@(246,250,255); outline=@(40,42,48)
                              wood=@(92,54,30); woodDark=@(58,32,18); woodLight=@(128,80,44); rivet=@(208,210,214); bolster=@(70,72,80); bolsterLight=@(110,112,120); blood=0.6 }
  'diamond_butcher_knife' = @{ shape='cleaver'; spine=@(22,104,112); body=@(64,206,196); light=@(150,244,232); edge=@(226,255,252); outline=@(12,40,46)
                              wood=@(40,30,38); woodDark=@(20,14,20); woodLight=@(70,54,66); rivet=@(252,214,84); bolster=@(196,146,36); bolsterLight=@(246,206,88); blood=0.4 }
}
$WOOD = @(92,54,30); $WOOD_DARK = @(58,32,18); $WOOD_LIGHT = @(128,80,44); $RIVET = @(208,210,214); $BOLSTER = @(70,72,80)
$BLOOD = @(128,14,16); $BLOOD_DARK = @(84,6,10); $BLOOD_WET = @(186,34,34)

# the knife's axis: from the handle's butt (2.2,13.8) to the tip (14.6,1.4)
$ax = 2.2; $ay = 13.8; $dx = 14.6-2.2; $dy = 1.4-13.8; $len = [Math]::Sqrt($dx*$dx+$dy*$dy); $ux = $dx/$len; $uy = $dy/$len
# across: + is the edge side (down-right)
$nx = -$uy; $ny = $ux; if($nx -lt 0){ $nx = -$nx; $ny = -$ny }

function Part([double]$x,[double]$y){
  $px = $x+0.5-$ax; $py = $y+0.5-$ay
  $t = $px*$ux + $py*$uy
  $s = $px*$nx + $py*$ny
  if($t -lt 0 -or $t -gt $len){ return $null }
  if($script:shape -eq 'cleaver'){
    if($t -lt 5.0){ if([Math]::Abs($s) -le 1.15){ return @('handle',$t,$s) } else { return $null } }
    if($t -lt 6.1){ if($s -ge -1.7 -and $s -le 2.3){ return @('bolster',$t,$s) } else { return $null } }
    # a square blade, its edge bowed out a little, with a hole by the spine to hang it up
    if($t -gt 15.0){ return $null }
    $edge = 4.4 - 0.5*[Math]::Pow(($t-10.6)/4.4,2)
    if([Math]::Sqrt([Math]::Pow($t-13.4,2)+[Math]::Pow($s+0.3,2)) -lt 0.8){ return $null }
    if($s -ge -1.6 -and $s -le $edge){ return @('blade',$t,$s,-1.6,$edge) }
    return $null
  }
  if($t -lt 5.6){ if([Math]::Abs($s) -le 1.05){ return @('handle',$t,$s) } else { return $null } }
  if($t -lt 6.7){ if($s -ge -1.5 -and $s -le 1.9){ return @('bolster',$t,$s) } else { return $null } }
  # the blade: a straight spine, a deep belly curving up to the tip
  $b = ($t-6.7)/($len-6.7)
  $edge = 4.1*[Math]::Sqrt([Math]::Max(0,1-[Math]::Pow($b,2.4))) - 0.2
  $spine = -1.4 + 1.2*[Math]::Pow($b,3)
  if($s -ge $spine -and $s -le $edge){ return @('blade',$t,$s,$spine,$edge) }
  return $null
}

foreach($name in $kinds.Keys){
  $k = $kinds[$name]
  $script:shape = $k.shape
  $WOOD = $k.wood; $WOOD_DARK = $k.woodDark; $WOOD_LIGHT = $k.woodLight; $RIVET = $k.rivet; $BOLSTER = $k.bolster
  $bmp = New-Object Drawing.Bitmap 16,16
  $parts = @{}
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $parts["$x,$y"] = Part $x $y } }
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $p = $parts["$x,$y"]
    $c = $null
    if($p -eq $null){
      # outline round the shape
      $touch = $false
      foreach($o in @(@(1,0),@(-1,0),@(0,1),@(0,-1))){ $q = $parts["$($x+$o[0]),$($y+$o[1])"]; if($q -ne $null){ $touch = $true } }
      if($touch){ $c = $(if((Part $x $y) -eq $null -and $parts["$($x-1),$($y)"] -ne $null -and $parts["$($x-1),$($y)"][0] -eq 'handle'){ $WOOD_DARK } else { $k.outline }) }
    } else {
      switch($p[0]){
        'handle' {
          $t=$p[1]; $s=$p[2]
          $c = $(if($s -lt -0.35){ $WOOD_LIGHT } elseif($s -gt 0.45){ $WOOD_DARK } else { $WOOD })
          if([Math]::Abs($t-1.6) -lt 0.5 -or [Math]::Abs($t-4.0) -lt 0.5){ if([Math]::Abs($s) -lt 0.45){ $c = $RIVET } }
        }
        'bolster' { $c = $(if($p[2] -lt 0){ $k.bolsterLight } else { $BOLSTER }) }
        'blade' {
          $s=$p[2]; $spine=$p[3]; $edge=$p[4]; $t=$p[1]
          $f = ($s-$spine)/[Math]::Max(0.01,$edge-$spine)
          $c = $(if($f -lt 0.22){ $k.spine } elseif($f -gt 0.8){ $k.edge } elseif($f -lt 0.5){ $k.light } else { $k.body })
          # blood: smeared up from the edge, thickest near the heel, with a drip or two
          $h = Hash $x $y 3
          $reach = 0.22 + 0.3*(1-($t-6.7)/($len-6.7))
          # the diamond glints
          if($k.shape -eq 'cleaver' -and $f -gt 0.25 -and $f -lt 0.75 -and $h -gt 0.86){ $c = @(255,255,255) }
          if($f -gt (1-$reach) -and $h -lt $k.blood){ $c = $(if($h -lt 0.18){ $BLOOD_WET } elseif($h -lt 0.5){ $BLOOD } else { $BLOOD_DARK }) }
          elseif($h -gt 0.97 -and $f -gt 0.3){ $c = $BLOOD }
        }
      }
    }
    if($c -ne $null){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,$c[0],$c[1],$c[2])) }
  } }
  $bmp.Save((Join-Path $out "$name.png"), [Drawing.Imaging.ImageFormat]::Png)
  # a big preview to check it by eye
  $big = New-Object Drawing.Bitmap 160,160; $g=[Drawing.Graphics]::FromImage($big); $g.InterpolationMode='NearestNeighbor'; $g.PixelOffsetMode='Half'
  $g.Clear([Drawing.Color]::FromArgb(139,139,139)); $g.DrawImage($bmp,0,0,160,160); $big.Save((Join-Path $env:TEMP "$name-preview.png"))
}
"made the butcher knives"

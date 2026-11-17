# Draws the NPC shop menu (textures/gui/container/npc_shop.png, 200x222): a market stall. Positions must match
# client/NpcShopScreen.
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\gui\container\npc_shop.png'
$W = 200; $H = 222
$bmp = New-Object Drawing.Bitmap 256,256,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 11
function Px([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt 256 -and $y -lt 256){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,[Math]::Max(0,[Math]::Min(255,$r)),[Math]::Max(0,[Math]::Min(255,$g)),[Math]::Max(0,[Math]::Min(255,$b)))) } }
function Rect([int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$b){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ Px $x $y $r $g $b } } }

# â”€â”€ warm oak planks (8 px boards, grain, dark seams, staggered joints) â”€â”€
for($y=0;$y -lt $H;$y++){ $board=[Math]::Floor($y/8); $tone = @(0,6,-4,3,-2,5,-6,2)[$board % 8]
  for($x=0;$x -lt $W;$x++){
    $n=$rng.Next(-4,5); $grain = if((($x*3 + $board*17) % 13) -lt 2){ -10 } else { 0 }
    $seam = ($y % 8 -eq 7) -or (($x + $board*37) % 64 -eq 0)
    if($seam){ Px $x $y 92 60 34 } else { Px $x $y (168+$tone+$n+$grain) (122+$tone+$n+$grain) (74+$tone+$n+$grain) } } }

# â”€â”€ frame: dark edge, lighter inner line â”€â”€
for($x=0;$x -lt $W;$x++){ Px $x 0 46 30 18; Px $x ($H-1) 46 30 18; Px $x 1 92 62 36; Px $x ($H-2) 92 62 36 }
for($y=0;$y -lt $H;$y++){ Px 0 $y 46 30 18; Px ($W-1) $y 46 30 18; Px 1 $y 92 62 36; Px ($W-2) $y 92 62 36 }

# â”€â”€ striped awning (rows 0-13) with a scalloped edge (rows 14-17) â”€â”€
for($y=0;$y -le 13;$y++){ for($x=2;$x -lt $W-2;$x++){ $red = ([Math]::Floor(($x-2)/10) % 2) -eq 0
  $shade = if($y -le 1){ 18 } elseif($y -ge 12){ -20 } else { 0 }
  if($red){ Px $x $y (196+$shade) (52+$shade) (46+$shade) } else { Px $x $y (240+$shade) (226+$shade) (190+$shade) } } }
for($s=0;$s*10+2 -lt $W-2;$s++){ $cx = $s*10 + 2 + 4.5; $red = ($s % 2) -eq 0
  for($y=14;$y -le 17;$y++){ $half = [Math]::Sqrt([Math]::Max(0, 25 - [Math]::Pow(($y-13)*1.6,2)))
    for($x=[int]($cx-$half);$x -le [int]($cx+$half);$x++){ if($x -lt 2 -or $x -ge $W-2){continue}
      if($red){ Px $x $y 170 40 36 } else { Px $x $y 214 200 166 } } } }

# â”€â”€ title plaque (title drawn at y 22) â”€â”€
Rect 40 19 159 32 70 44 26
for($x=40;$x -le 159;$x++){ Px $x 19 222 178 70; Px $x 32 150 110 40 }
for($y=19;$y -le 32;$y++){ Px 40 $y 222 178 70; Px 159 $y 150 110 40 }

# â”€â”€ 7 offer rows (y 52 + 20i, 18 tall): an inset shelf with an icon frame at x 10-27 â”€â”€
for($i=0;$i -lt 7;$i++){ $y0 = 52 + 20*$i; $y1 = $y0 + 17
  Rect 8 $y0 191 $y1 120 84 50
  for($x=8;$x -le 191;$x++){ Px $x $y0 70 46 26; Px $x $y1 196 156 104 }
  for($y=$y0;$y -le $y1;$y++){ Px 8 $y 70 46 26; Px 191 $y 196 156 104 }
  Rect 10 ($y0+1) 27 ($y1-1) 58 38 22
  for($x=10;$x -le 27;$x++){ Px $x ($y0+1) 36 22 12; Px $x ($y1-1) 150 110 70 } }

# â”€â”€ footer strip (your gold / discount, drawn at y 198 and 209) â”€â”€
Rect 6 194 193 217 64 40 22
for($x=6;$x -le 193;$x++){ Px $x 194 222 178 70 }

$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

# Real estate art (RealEstate.md): the three sign boards (a painted board with a border and pixel lettering: For Sale
# red, For Lease blue, For Sale by the City navy with gold) and the Survey Stake (a pointed wooden stake with an orange
# flag tied on). Each board's painting fills the top 28x16 of a 32x32 texture (the model maps just that part onto the
# 14x8 board). Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_real_estate_textures.ps1
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'

# a 3x5 pixel font
$font = @{
  'A'=@('.#.','#.#','###','#.#','#.#'); 'C'=@('.##','#..','#..','#..','.##'); 'E'=@('###','#..','##.','#..','###')
  'F'=@('###','#..','##.','#..','#..'); 'I'=@('###','.#.','.#.','.#.','###'); 'L'=@('#..','#..','#..','#..','###')
  'O'=@('.#.','#.#','#.#','#.#','.#.'); 'R'=@('##.','#.#','##.','#.#','#.#'); 'S'=@('.##','#..','.#.','..#','##.')
  'T'=@('###','.#.','.#.','.#.','.#.'); 'Y'=@('#.#','#.#','.#.','.#.','.#.')
}
function Px($b,$x,$y,$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,$c[0],$c[1],$c[2])) } }
function Word($b,[string]$word,[int]$y,$color,[int]$width){
  $w = $word.Length * 4 - 1
  $x0 = [int](($width - $w) / 2)
  for($i=0;$i -lt $word.Length;$i++){
    $g = $font[[string]$word[$i]]
    for($r=0;$r -lt 5;$r++){ for($k=0;$k -lt 3;$k++){ if($g[$r][$k] -eq '#'){ Px $b ($x0 + $i*4 + $k) ($y + $r) $color } } }
  }
}
function Board([string]$name, $bg, $edge, $text, [string]$line1, [string]$line2){
  $b = New-Object Drawing.Bitmap 32,32
  for($y=0;$y -lt 32;$y++){ for($x=0;$x -lt 32;$x++){
    $c = $bg
    if($x -ge 28 -or $y -ge 16){ $c = @(90,64,40) }                      # unused: plain wood
    elseif($x -eq 0 -or $y -eq 0 -or $x -eq 27 -or $y -eq 15){ $c = $edge }
    elseif($x -eq 1 -or $y -eq 1 -or $x -eq 26 -or $y -eq 14){ $c = @([Math]::Min(255,$bg[0]+30),[Math]::Min(255,$bg[1]+30),[Math]::Min(255,$bg[2]+30)) }
    Px $b $x $y $c
  } }
  Word $b $line1 2 $text 28
  Word $b $line2 9 $text 28
  $b.Save((Join-Path $tex "block\$name.png"))
}
Board 'for_sale_sign' @(176,36,32) @(240,236,224) @(250,248,240) 'FOR' 'SALE'
Board 'for_lease_sign' @(36,78,168) @(240,236,224) @(250,248,240) 'FOR' 'LEASE'
Board 'city_sale_sign' @(28,36,72) @(232,190,80) @(240,204,96) 'CITY' 'LOT'

# the Survey Stake: a stake pointing down-left, an orange flag near its top
$b = New-Object Drawing.Bitmap 16,16
for($i=0;$i -lt 12;$i++){ $x = 3 + $i; $y = 14 - $i; Px $b $x $y @(150,108,64); Px $b ($x+1) $y @(110,78,44) }
Px $b 2 15 @(90,64,40); Px $b 3 15 @(120,86,50)
foreach($p in @(@(10,3),@(11,3),@(12,3),@(9,4),@(10,4),@(11,4),@(12,4),@(9,5),@(10,5),@(11,5),@(8,6),@(9,6))){ Px $b $p[0] $p[1] @(250,128,24) }
foreach($p in @(@(12,2),@(13,3),@(12,5),@(10,6))){ Px $b $p[0] $p[1] @(200,90,16) }
$b.Save((Join-Path $tex 'item\survey_stake.png'))
"made the real estate textures"

# the City Chest: a navy strongbox banded with gold, a gold lock on the front, the city's crest on the lid
function Chest([string]$name, [scriptblock]$extra){
  $b = New-Object Drawing.Bitmap 16,16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $c = @(32,42,84)
    if((($x * 7 + $y * 3) % 11) -eq 0){ $c = @(40,52,100) }
    if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = @(200,160,60) }
    elseif($x -eq 3 -or $x -eq 12){ $c = @(176,138,52) }
    Px $b $x $y $c
  } }
  & $extra $b
  $b.Save((Join-Path $tex "block\$name.png"))
}
Chest 'city_chest_side' { param($b) for($x=1;$x -lt 15;$x++){ Px $b $x 5 @(200,160,60) } }
Chest 'city_chest_front' { param($b)
  for($x=1;$x -lt 15;$x++){ Px $b $x 5 @(200,160,60) }
  for($y=4;$y -le 9;$y++){ for($x=6;$x -le 9;$x++){ Px $b $x $y @(232,196,88) } }
  Px $b 7 7 @(40,30,20); Px $b 8 7 @(40,30,20); Px $b 7 8 @(40,30,20)
}
Chest 'city_chest_top' { param($b)
  foreach($p in @(@(7,5),@(8,5),@(6,6),@(9,6),@(6,7),@(7,7),@(8,7),@(9,7),@(6,8),@(9,8),@(7,9),@(8,9),@(7,10),@(8,10))){ Px $b $p[0] $p[1] @(232,196,88) }
}
"made the city chest"

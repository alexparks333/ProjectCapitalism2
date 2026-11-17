# Draws the butchering textures (Farmer.md > Butchering): the fine cuts a Meat Slab gives
# when cut up well (Raw Beef Rib...), and the Butcher's Table block (an end-grain chopping block with a cleaver in it).
# Each item is a small pixel map: M meat, m darker meat, F fat (or chicken skin), S pork / chicken skin, B bone,
# K marrow; colored by the animal, with the top edge lit and the other edges shaded.
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function New([int]$w,[int]$h){ New-Object Drawing.Bitmap $w,$h,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Px($b,[int]$x,[int]$y,[int]$r,[int]$g,[int]$bl,[int]$a=255){ if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,[Drawing.Color]::FromArgb($a,(C $r),(C $g),(C $bl))) } }
function Save($b,[string]$path){ $full = Join-Path $root $path; New-Item -ItemType Directory -Force (Split-Path $full) | Out-Null; $b.Save($full,[Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# ── the animals' colors ──
$palettes = @{
  'beef'    = @{ M=@(184,50,44); D=@(142,34,32);  F=@(242,232,204); S=@(242,232,204) }
  'pork'    = @{ M=@(232,150,142); D=@(200,112,106); F=@(250,242,226); S=@(240,192,168) }
  'mutton'  = @{ M=@(170,68,56); D=@(130,46,38);  F=@(244,234,208); S=@(244,234,208) }
  'chicken' = @{ M=@(242,188,166); D=@(224,156,136); F=@(246,226,162); S=@(248,220,176) }
  'rabbit'  = @{ M=@(200,120,106); D=@(164,90,78);  F=@(242,230,206); S=@(242,230,206) }
}
$bone = @(239,232,212); $marrow = @(160,48,40)

# ── the shapes (16 rows of 16) ──
$shapes = @{
  # ── the cuts ──
  # a steak, fat down one edge
  'steak' = @(
    '................','................','................','....FFFFFFF.....','..FFMMMMMMMMF...','.FMMMmMMMMMMMF..',
    '.FMMMMMMFMMMMMF.','.FMMMMMFMMMmMMF.','..FMmMMMMMMMMMF.','...FMMMMMMMMMF..','....FFMMMMMFF...','......FFFFF.....',
    '................','................','................','................')
  # a slice of brisket under its fat
  'slice' = @(
    '................','................','................','................','................','..FFFFFFFFFFFF..',
    '.FFFFFFFFFFFFFF.','.MMMMMMmMMMMMMM.','.MMmMMMMMMMMmMM.','.MMMMMMMMMMMMMM.','..MMMMMMMmMMMM..','................',
    '................','................','................','................')
  # a round steak with its little bone
  'roundsteak' = @(
    '................','................','................','.....FFFFFF.....','...FFMMMMMMFF...','..FMMMmMMMMMMF..',
    '..FMMMMBBMMMMF..','..FMMMBKKBMMMF..','..FMMmMBBMMMMF..','..FMMMMMMMMmMF..','...FFMMMMMMFF...','.....FFFFFF.....',
    '................','................','................','................')
  # a chop: a steak on the end of a rib bone
  'chop' = @(
    '................','..............BB','.............BB.','............BB..','...........BB...','....FFFFFFBB....',
    '...FMMMMMMBF....','..FMMMmMMMMMF...','..FMMMMMMMMMF...','..FMMMMMMmMMF...','..FMmMMMMMMMF...','...FMMMMMMMF....',
    '....FFFFFFF.....','................','................','................')
  # a strip of belly
  'strip' = @(
    '................','................','................','................','................','.SSSSSSSSSSSSS..',
    '.FFFFFFFFFFFFFF.','.MMMMmMMMMMMMMM.','.FFFFFFFFFFFFFF.','.MMMMMMMMMmMMMM.','..MMMMMMMMMMMMM.','................',
    '................','................','................','................')
  # a drumstick
  'drumstick' = @(
    '................','............BB..','...........BKKB.','..........BBBB..','.........BB.....','........MMM.....',
    '......MMMMMM....','....MMMMMMMMM...','...MMMMmMMMMM...','..MMMMMMMMMMM...','..MMMMMMMMMM....','..MMMmMMMMM.....',
    '...MMMMMMM......','....MMMMM.......','................','................')
  # a medallion of loin in its ring of fat
  'medallion' = @(
    '................','................','................','.....FFFFFF.....','...FFMMMMMMFF...','..FMMMMMMmMMMF..',
    '..FMMmMMMMMMMF..','..FMMMMMMMMMMF..','..FMMMMMmMMMMF..','...FFMMMMMMFF...','.....FFFFFF.....','................',
    '................','................','................','................')
  # a fillet of breast
  'fillet' = @(
    '................','................','................','................','...MMMMM........','..MMMMMMMMM.....',
    '.MMMMmMMMMMMM...','.MMMMMMMMMMMMMM.','..MMMMMMMmMMMMM.','...MMMMMMMMMMM..','.....MMMMMMM....','................',
    '................','................','................','................')
}

function Meat([string]$shape,[string]$animal,[string]$name,[int]$seed){
  $rows = $shapes[$shape]; $pal = $palettes[$animal]; $rng = New-Object System.Random $seed
  $b = New 16 16
  function At([int]$x,[int]$y){ if($x -lt 0 -or $y -lt 0 -or $x -gt 15 -or $y -gt 15){ return '.' } return $rows[$y][$x] }
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    $c = [string]$rows[$y][$x]; if($c -eq '.'){ continue }
    $col = switch -CaseSensitive ($c){ 'M' { $pal.M } 'm' { $pal.D } 'F' { $pal.F } 'S' { $pal.S } 'B' { $bone } 'K' { $marrow } }
    $f = 1.0 + ($rng.Next(-5,6) / 100.0)
    if((At $x ($y-1)) -eq '.'){ $f *= 1.12 }
    elseif((At $x ($y+1)) -eq '.' -or (At ($x-1) $y) -eq '.' -or (At ($x+1) $y) -eq '.'){ $f *= 0.72 }
    Px $b $x $y ([int]($col[0]*$f)) ([int]($col[1]*$f)) ([int]($col[2]*$f)) } }
  Save $b "item/$name.png"
}

# each cut: (id, animal, shape) (the Meat Slabs themselves are blocks now: tools/make_meat_slab_blocks.ps1)
$cuts = @(
  @('beef_chuck','beef','steak'), @('beef_brisket','beef','slice'), @('beef_round','beef','roundsteak'), @('beef_rib','beef','chop'),
  @('pork_shoulder','pork','steak'), @('pork_belly','pork','strip'), @('pork_ham','pork','roundsteak'), @('pork_loin','pork','chop'),
  @('mutton_shoulder','mutton','steak'), @('mutton_leg','mutton','drumstick'), @('mutton_rack','mutton','chop'),
  @('chicken_leg_quarter','chicken','drumstick'), @('chicken_breast','chicken','fillet'),
  @('rabbit_hind_legs','rabbit','drumstick'), @('rabbit_saddle','rabbit','medallion'))
$i = 0
foreach($c in $cuts){ $i++; Meat $c[2] $c[1] "raw_$($c[0])" (100 + $i) }

# ── the Butcher's Table: an end-grain board on top (squares of growth rings, a cleaver stuck in, an old stain),
# thick dark-wood sides with the board's edge showing as a light band ──
$rng = New-Object System.Random 7
$top = New 16 16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  $tx = [int][Math]::Floor($x / 4); $ty = [int][Math]::Floor($y / 4); $light = (($tx + $ty) % 2) -eq 0
  $base = if($light){ @(166,118,70) } else { @(152,104,60) }
  $lx = $x % 4; $ly = $y % 4; $ring = ($lx -eq 1 -or $lx -eq 2) -and ($ly -eq 1 -or $ly -eq 2)
  $f = 1.0 + ($rng.Next(-4,5) / 100.0); if($ring){ $f *= 0.9 }; if($lx -eq 0 -or $ly -eq 0){ $f *= 0.93 }
  if($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $f *= 0.75 }
  Px $top $x $y ([int]($base[0]*$f)) ([int]($base[1]*$f)) ([int]($base[2]*$f)) } }
foreach($p in @(@(10,10),@(11,10),@(10,11),@(11,11),@(12,11),@(11,12))){ Px $top $p[0] $p[1] 120 52 40 }
# the cleaver, blade down in the wood: its back edge and handle
for($x=3;$x -le 9;$x++){ Px $top $x 4 200 206 214; Px $top $x 5 150 156 164 }
Px $top 2 4 230 236 244
for($x=10;$x -le 13;$x++){ Px $top $x 4 92 60 34; Px $top $x 5 64 40 22 }
Save $top 'block/butchers_table_top.png'

$side = New 16 16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
  $f = 1.0 + ($rng.Next(-5,6) / 100.0)
  if($y -le 4){ $base = @(150,104,62); if($y -eq 4){ $f *= 0.7 }; if(($x % 4) -eq 0){ $f *= 0.9 } }
  else { $base = @(92,60,34); if(($x % 5) -eq 2){ $f *= 0.86 }; if($y -eq 15){ $f *= 0.7 } }
  if($y -ge 5 -and ($x -le 1 -or $x -ge 14)){ $f *= 1.1 }
  Px $side $x $y ([int]($base[0]*$f)) ([int]($base[1]*$f)) ([int]($base[2]*$f)) } }
Save $side 'block/butchers_table_side.png'

$bottom = New 16 16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $f = 1.0 + ($rng.Next(-5,6) / 100.0); if(($y % 4) -eq 0){ $f *= 0.88 }
  Px $bottom $x $y ([int](80*$f)) ([int](52*$f)) ([int](30*$f)) } }
Save $bottom 'block/butchers_table_bottom.png'
Write-Output 'done'

# Draws the Librarian's textures (Librarian.md):
#  - blocks: stitching_table_top / _side / _front (spruce worktable, leather mat with stitches, a drawer of thread),
#    librarian_table_top (dark oak rim, green felt) and librarian_table_cover (green leather book cover)
#  - items: rough_book (patchy rawhide, thick dark X-stitches, a twine cord), delicate_book (cream leather, diamond and gold stitching),
#    knowledge_piece (a torn parchment scrap with ink and a glowing rune), skill_book_cover (grey, tinted per skill by
#    items/skill_book.json) + skill_book_pages (untinted)
#  - item/skill_books/<skill>.png: every skill book's own cover (its color + an emblem), and mob_effect/book_<lesson>.png
#    (keep the list matching librarian/Skill)
#  - gui/container/stitching_table.png (176x222) and librarian_table.png (176x166): slot positions must match
#    StitchingTableMenu / LibrarianTableMenu and the screens.
# The books are recolored from vanilla's book.png; unzip it first (Git Bash, from the project folder):
#   unzip -o -d "$LOCALAPPDATA/Temp/vr" .gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/item/book.png'
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$vr = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\item'
foreach($d in @('block','item','mob_effect','gui\container')){ New-Item -ItemType Directory -Force (Join-Path $res $d) | Out-Null }
$rng = New-Object System.Random 11
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function NewBmp([int]$w,[int]$h){ New-Object Drawing.Bitmap $w,$h,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function P($bmp,[int]$x,[int]$y,[double]$r,[double]$g,[double]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Fill($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int]$noise){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ $n=$rng.Next(-$noise,$noise+1); P $bmp $x $y ($c[0]+$n) ($c[1]+$n) ($c[2]+$n) } } }
function Save($bmp,[string]$rel){ $bmp.Save((Join-Path $res $rel),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }

# ── Blocks ──
function Planks($bmp,[int[]]$c){ Fill $bmp 0 0 15 15 $c 6; foreach($y in @(3,7,11,15)){ for($x=0;$x -lt 16;$x++){ P $bmp $x $y ($c[0]-28) ($c[1]-22) ($c[2]-14) } } }
$spruce = @(112,82,50)

$top = NewBmp 16 16
Fill $top 0 0 15 15 $spruce 5
Fill $top 2 2 13 13 @(138,86,48) 8
for($i=3;$i -le 12;$i++){ if($i % 2 -eq 1){ P $top $i 3 232 212 170; P $top $i 12 232 212 170; P $top 3 $i 232 212 170; P $top 12 $i 232 212 170 } }
for($i=0;$i -lt 16;$i++){ P $top $i 0 80 58 34; P $top 0 $i 80 58 34; P $top $i 15 80 58 34; P $top 15 $i 80 58 34 }
Save $top 'block\stitching_table_top.png'

$side = NewBmp 16 16
Planks $side $spruce
Fill $side 0 3 15 5 @(88,62,36) 4
Fill $side 0 6 1 15 @(84,60,34) 4; Fill $side 14 6 15 15 @(84,60,34) 4
Save $side 'block\stitching_table_side.png'

$front = NewBmp 16 16
Planks $front $spruce
Fill $front 0 3 15 5 @(88,62,36) 4
Fill $front 0 6 1 15 @(84,60,34) 4; Fill $front 14 6 15 15 @(84,60,34) 4
Fill $front 3 7 12 12 @(96,68,40) 3
for($x=3;$x -le 12;$x++){ P $front $x 7 70 50 30; P $front $x 12 70 50 30 }
for($y=7;$y -le 12;$y++){ P $front 3 $y 70 50 30; P $front 12 $y 70 50 30 }
P $front 7 9 200 170 90; P $front 8 9 200 170 90
# thread hanging out of the drawer
P $front 5 13 200 40 40; P $front 5 14 200 40 40; P $front 10 13 60 90 200; P $front 10 14 60 90 200; P $front 10 15 60 90 200
Save $front 'block\stitching_table_front.png'

$ltop = NewBmp 16 16
Fill $ltop 0 0 15 15 @(64,42,24) 4
Fill $ltop 2 2 13 13 @(44,100,62) 6
foreach($p in @(@(1,1),@(14,1),@(1,14),@(14,14))){ P $ltop $p[0] $p[1] 220 180 80 }
Save $ltop 'block\librarian_table_top.png'

$lcover = NewBmp 16 16
Fill $lcover 0 0 15 15 @(46,112,72) 6
for($i=0;$i -lt 16;$i++){ P $lcover $i 0 214 176 74; P $lcover 0 $i 214 176 74; P $lcover $i 15 214 176 74; P $lcover 15 $i 214 176 74 }
Save $lcover 'block\librarian_table_cover.png'

# ── Books, from vanilla's book.png: grey pixels are the pages, the rest the cover (the "spine" line is 522e10) ──
$vbook = [Drawing.Bitmap]::FromFile((Join-Path $vr 'book.png'))
function IsPage($c){ $c.R -eq $c.G -and $c.G -eq $c.B }
function IsSpine($c){ $c.R -eq 0x52 -and $c.G -eq 0x2e }
# A recolored book: cover in $base (shaded like the vanilla one), pages in $paper; $spine($x,$y) may paint the spine
function Book([int[]]$base,[int[]]$paper,[scriptblock]$spine,[int]$size,[int]$off){
  $bmp = NewBmp $size $size
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $vbook.GetPixel($x,$y); if($c.A -eq 0){ continue }
    if(IsPage $c){ $f = $c.R / 214.0; P $bmp ($x+$off) ($y+$off) ($paper[0]*$f) ($paper[1]*$f) ($paper[2]*$f); continue }
    $f = (($c.R + $c.G + $c.B) / 3.0) / 58.0
    P $bmp ($x+$off) ($y+$off) ($base[0]*$f) ($base[1]*$f) ($base[2]*$f)
    if($spine -and (IsSpine $c)){ & $spine $bmp ($x+$off) ($y+$off) } } }
  return $bmp }
$cream = @(250,244,226)

# Rough Book: a patchy pale rawhide cover (lighter and darker blotches), thick dark X-stitches down the spine, yellowed
# pages, and a twine cord tied round it with a knot - nothing like a neat vanilla book
$rough = Book @(168,128,84) @(214,198,160) { param($b,$x,$y) if((($x + $y) % 2) -eq 0){ P $b $x $y 52 34 22 } else { P $b $x $y 92 66 40 } } 16 0
$hideRng = New-Object System.Random 3
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $vbook.GetPixel($x,$y)
  if($c.A -eq 0 -or (IsPage $c) -or (IsSpine $c)){ continue }
  $roll = $hideRng.Next(0,10)
  if($roll -lt 2){ P $rough $x $y 196 160 110 } elseif($roll -lt 3){ P $rough $x $y 120 86 52 } } }
# the twine cord across the cover (a diagonal band), with a knot
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $vbook.GetPixel($x,$y)
  if($c.A -eq 0 -or (IsPage $c)){ continue }
  if(($x - $y) -eq 2){ P $rough $x $y 214 190 126 } elseif(($x - $y) -eq 3){ P $rough $x $y 150 120 70 } } }
P $rough 9 7 120 90 40; P $rough 10 7 214 190 126; P $rough 10 8 120 90 40
Save $rough 'item\rough_book.png'
$delicate = Book @(236,228,212) $cream { param($b,$x,$y) if((($x + $y) % 2) -eq 0){ P $b $x $y 110 232 222 } else { P $b $x $y 232 193 74 } } 16 0
Save $delicate 'item\delicate_book.png'
# the skill book: a light grey cover (tinted per skill) and the pages on their own layer
$cover = NewBmp 16 16; $pages = NewBmp 16 16
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $vbook.GetPixel($x,$y); if($c.A -eq 0){ continue }
  if(IsPage $c){ $f = $c.R / 214.0; P $pages $x $y (250*$f) (244*$f) (226*$f) }
  elseif(IsSpine $c){ P $pages $x $y 232 193 74 }
  else { $f = [Math]::Min(1.0, (($c.R + $c.G + $c.B) / 3.0) / 58.0); P $cover $x $y (60 + 195*$f) (60 + 195*$f) (60 + 195*$f) } } }
Save $cover 'item\skill_book_cover.png'; Save $pages 'item\skill_book_pages.png'

# every skill book's own cover (item/skill_books/<skill>.png) and, for lessons (not instant books), its effect icon (mob_effect/book_<skill>.png):
# the book in the skill's color with a 5x5 emblem stamped on the cover (# = emblem; light emblem on a dark cover, dark
# on a light one). Traits get a gold spine. Keep the list matching librarian/Skill.
New-Item -ItemType Directory -Force (Join-Path $res 'item\skill_books') | Out-Null
$covers = [ordered]@{
  running_club        = @(0x4FA3E0, $false, @('.##..','.##..','.##..','.####','.####'))
  mining_monthly      = @(0x8A6A3A, $false, @('.###.','#.#.#','..#..','..#..','..#..'))
  swimming_lessons    = @(0x2F7FD0, $false, @('.#.#.','#.#.#','.....','.#.#.','#.#.#'))
  soft_landings       = @(0xE8E0C8, $false, @('...#.','..##.','.###.','.##..','#....'))
  holding_your_breath = @(0x5FD0D0, $false, @('.##..','#..#.','.##..','...#.','..#.#'))
  anglers_almanac     = @(0x2E9A6A, $false, @('.....','#.##.','#####','#.##.','.....'))
  water_rationing     = @(0x3A8AE8, $false, @('..#..','.###.','#####','#####','.###.'))
  butchery_basics     = @(0xB8402E, $false, @('..##.','.####','.####','#.##.','#....'))
  lumberjacks_log     = @(0x6A4A26, $false, @('.##..','####.','.##..','..#..','..#..'))
  archery_drills      = @(0xC8A050, $false, @('..###','...##','..#.#','.#...','#....'))
  horse_whispering    = @(0x9A6A44, $false, @('#...#','#...#','#...#','#...#','.###.'))
  quiet_steps         = @(0x4A4A5A, $false, @('#....','##...','...#.','...##','.....'))
  fishermans_patience = @(0x3070A0, $false, @('...#.','...#.','#..#.','#..#.','.##..'))
  stonemasons_guide   = @(0x8A8A8A, $false, @('#####','..#..','#####','#...#','#####'))
  first_aid_basics    = @(0xE04040, $false, @('..#..','..#..','#####','..#..','..#..'))
  trail_rations       = @(0xC89A50, $false, @('.###.','#####','#.#.#','#####','.....'))
  beekeepers_notes    = @(0xE8C030, $false, @('.#.#.','#####','.....','#####','.###.'))
  lucky_librarian     = @(0x50C878, $false, @('.#.#.','#####','.###.','..#..','.#...'))
  merchants_ledger    = @(0xD4AF37, $false, @('.###.','#.#.#','##.##','#.#.#','.###.'))
  brawlers_handbook   = @(0xB02A2A, $false, @('....#','...#.','#.#..','.#...','#.#..'))
  reaching_further    = @(0x9A6AD0, $false, @('..#..','...#.','#####','...#.','..#..'))
  ice_in_the_pants    = @(0x8FD8F0, $false, @('#.#.#','.#.#.','#####','#.#.#','#.#.#'))
  hot_pepper          = @(0xD8361E, $false, @('...#.','..#..','.###.','.###.','..#..'))
  revenants_bane      = @(0x9A1020, 'blood', @('#.#.#','#####','.#.#.','.###.','.#.#.'))
  witchs_bane         = @(0x8A2A6A, 'blood', @('..#..','.###.','#####','..#..','.#.#.'))
  wolfsbane           = @(0x6A1818, 'blood', @('#...#','##.##','#####','.#.#.','..#..'))
  blood_fury          = @(0xC8202A, 'blood', @('..#..','..#..','.###.','#####','.###.'))
  blood_ward          = @(0x5A2A3A, 'blood', @('#####','#.#.#','#####','.###.','..#..'))
  fresh_start         = @(0x7FD87F, $false, @('.###.','#...#','#.#.#','#...#','.###.'))
  iron_stomach        = @(0x8A9098, $true,  @('.###.','#...#','#.#.#','#...#','.###.'))
  nimble              = @(0x70D0A0, $true,  @('####.','...#.','.###.','.#...','.####'))
  night_owl           = @(0x3A3A7A, $true,  @('.###.','##...','#....','##...','.###.'))
  bookworm            = @(0x7A5A9A, $true,  @('#....','#.##.','#.#.#','.#..#','.....'))
  steady_aim          = @(0xA0522D, $true,  @('..#..','.#.#.','##.##','.#.#.','..#..'))
  sure_footed         = @(0x9AD0E8, $true,  @('..#..','..#..','..##.','####.','#.#.#'))
  light_footed        = @(0xF0D060, $true,  @('#....','##...','###..','####.','.####'))
  deep_lungs          = @(0x3AB0E0, $true,  @('..#..','.###.','##.##','##.##','.#.#.'))
  born_miner          = @(0xD08A3A, $true,  @('.###.','#####','.###.','..#..','.....'))
  lucky_star          = @(0x60E070, $true,  @('..#..','.###.','#####','.###.','#...#'))
  fast_learner        = @(0x80FF40, $true,  @('..##.','.##..','####.','..##.','.##..'))
  camel               = @(0xD8B070, $true,  @('.....','.#.#.','#####','#...#','#...#'))
  used_to_hell        = @(0x8A1A10, $true,  @('#.#.#','#####','.###.','.###.','..#..'))
  winter_survival     = @(0xB8E0F0, $true,  @('#.#.#','.###.','##.##','.###.','#.#.#'))
}
# $trait: $true = a trait (gold spine), $false = a lesson, 'blood' = a Blood Moon Book (a deep crimson spine)
function Cover([int]$v,[object]$trait,[string[]]$emblem,[int]$size,[int]$off){
  $base = @((($v -shr 16) -band 255), (($v -shr 8) -band 255), ($v -band 255))
  $spine = if($trait -is [string]){ { param($b,$x,$y) P $b $x $y 200 20 40 } } elseif($trait -eq $true){ { param($b,$x,$y) P $b $x $y 240 200 80 } } else { { param($b,$x,$y) if((($x + $y) % 2) -eq 0){ P $b $x $y 236 228 210 } } }
  $bmp = Book $base $cream $spine $size $off
  $lum = 0.3*$base[0] + 0.59*$base[1] + 0.11*$base[2]
  $ink = if($lum -gt 150){ @(52,36,24) } else { @(250,242,214) }
  for($r=0;$r -lt 5;$r++){ for($c=0;$c -lt 5;$c++){ if($emblem[$r][$c] -eq '#'){ P $bmp (7 + $c + $off) (3 + $r + $off) $ink[0] $ink[1] $ink[2] } } }
  return $bmp }
foreach($k in $covers.Keys){ $d = $covers[$k]
  Save (Cover $d[0] $d[1] $d[2] 16 0) "item\skill_books\$k.png"
  if(($d[1] -is [string] -or $d[1] -eq $false) -and $k -ne 'fresh_start'){ Save (Cover $d[0] $d[1] $d[2] 18 1) "mob_effect\book_$k.png" } }
$vbook.Dispose()

# the Stitching Needle (only drawn on the Stitching Table while it sews): silver, an eye at the top, red thread
$nd = NewBmp 16 16
for($i=0;$i -le 9;$i++){ P $nd (3 + $i) (13 - $i) (190 + $i*5) (196 + $i*5) (206 + $i*4); P $nd (4 + $i) (13 - $i) 120 124 132 }
P $nd 13 3 210 214 222; P $nd 12 2 210 214 222; P $nd 13 2 40 40 48; P $nd 14 2 210 214 222; P $nd 13 1 210 214 222
foreach($p in @(@(14,3),@(15,4),@(15,5),@(14,6),@(13,7),@(13,8),@(14,9))){ P $nd $p[0] $p[1] 200 40 40 }
Save $nd 'item\stitching_needle.png'

# Knowledge Piece: a torn parchment scrap, ink lines, a glowing rune in the corner
$kp = NewBmp 16 16
$rows = @(@(5,10),@(4,11),@(3,12),@(3,12),@(2,12),@(3,13),@(3,12),@(2,12),@(3,12),@(3,13),@(4,12),@(4,11),@(5,10))
for($i=0;$i -lt $rows.Count;$i++){ $y = $i + 2; $r = $rows[$i]
  for($x=$r[0];$x -le $r[1];$x++){ $n=$rng.Next(-8,9); P $kp $x $y (226+$n) (208+$n) (160+$n) }
  P $kp $r[0] $y 150 120 80; P $kp $r[1] $y 150 120 80 }
for($x=5;$x -le 10;$x++){ P $kp $x 2 150 120 80; P $kp $x 14 150 120 80 }
foreach($line in @(@(5,4,10),@(7,4,9),@(9,4,8))){ for($x=$line[1];$x -le $line[2];$x++){ if(($x % 3) -ne 0){ P $kp $x $line[0] 62 46 30 } } }
P $kp 10 10 70 220 200; P $kp 9 11 70 220 200; P $kp 11 11 70 220 200; P $kp 10 12 70 220 200; P $kp 10 11 210 255 245
Save $kp 'item\knowledge_piece.png'

# ── GUIs ──
function Frame($bmp,[int]$x0,[int]$y0,[int]$s,[int[]]$in,[int[]]$dark,[int[]]$light){
  Fill $bmp ($x0+1) ($y0+1) ($x0+$s-2) ($y0+$s-2) $in 0
  for($i=0;$i -lt $s;$i++){ P $bmp ($x0+$i) $y0 $dark[0] $dark[1] $dark[2]; P $bmp $x0 ($y0+$i) $dark[0] $dark[1] $dark[2]
    P $bmp ($x0+$i) ($y0+$s-1) $light[0] $light[1] $light[2]; P $bmp ($x0+$s-1) ($y0+$i) $light[0] $light[1] $light[2] } }
function Panel($bmp,[int]$W,[int]$H,[int[]]$c,[int[]]$hi,[int[]]$lo){
  for($y=0;$y -lt $H;$y++){ $t = $y / $H; for($x=0;$x -lt $W;$x++){ $n=$rng.Next(-3,4); P $bmp $x $y ($c[0]-14*$t+$n) ($c[1]-12*$t+$n) ($c[2]-10*$t+$n) } }
  for($x=0;$x -lt $W;$x++){ P $bmp $x 0 20 12 6; P $bmp $x ($H-1) 20 12 6; P $bmp $x 1 $hi[0] $hi[1] $hi[2]; P $bmp $x ($H-2) $lo[0] $lo[1] $lo[2] }
  for($y=0;$y -lt $H;$y++){ P $bmp 0 $y 20 12 6; P $bmp ($W-1) $y 20 12 6; P $bmp 1 $y $hi[0] $hi[1] $hi[2]; P $bmp ($W-2) $y $lo[0] $lo[1] $lo[2] } }
function Inventory($bmp,[int]$top,[int[]]$in,[int[]]$dark,[int[]]$light){
  for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Frame $bmp (7 + 18*$c) ($top - 1 + 18*$r) 18 $in $dark $light } }
  for($c=0;$c -lt 9;$c++){ Frame $bmp (7 + 18*$c) ($top + 57) 18 $in $dark $light } }

# Stitching Table: warm leather panel, a darker mat under the ring with a dashed stitch border
$st = NewBmp 256 256
Panel $st 176 222 @(122,82,50) @(186,140,96) @(64,40,22)
# the mat round the ring (the ring of 12 is the 5x5 box of slots at 20,28 without corners and middle)
Fill $st 12 20 115 123 @(92,58,32) 4
for($i=12;$i -le 115;$i++){ if(($i % 4) -lt 2){ P $st $i 21 236 214 168; P $st $i 122 236 214 168 } }
for($i=20;$i -le 123;$i++){ if(($i % 4) -lt 2){ P $st 13 $i 236 214 168; P $st 114 $i 236 214 168 } }
$sin = @(60,38,20); $sdk = @(34,20,10); $slt = @(170,130,90)
$RX = 20; $RY = 28
for($i=0;$i -lt 3;$i++){
  Frame $st ($RX + 18*($i+1) - 1) ($RY - 1) 18 $sin $sdk $slt
  Frame $st ($RX + 72 - 1) ($RY + 18*($i+1) - 1) 18 $sin $sdk $slt
  Frame $st ($RX + 18*($i+1) - 1) ($RY + 72 - 1) 18 $sin $sdk $slt
  Frame $st ($RX - 1) ($RY + 18*($i+1) - 1) 18 $sin $sdk $slt }
# the big Paper slot in the middle (item at 57,65), with a paper-colored rim, and round it the faint dotted line the
# stitches follow (client/StitchingTableScreen sews thread along it: x 48..81, y 56..89)
Frame $st 52 60 26 @(70,48,28) $sdk @(236,224,190)
for($i=48;$i -le 81;$i++){ if(($i % 3) -eq 0){ P $st $i 56 120 84 52; P $st $i 89 120 84 52 } }
for($i=56;$i -le 89;$i++){ if(($i % 3) -eq 0){ P $st 48 $i 120 84 52; P $st 81 $i 120 84 52 } }
# thread slot (144,38) with a little spool beside it
Frame $st 143 37 18 $sin $sdk $slt
Fill $st 129 40 135 41 @(112,82,50) 0; Fill $st 130 42 134 49 @(190,50,50) 0; Fill $st 129 50 135 51 @(112,82,50) 0
for($y=43;$y -le 48;$y+=2){ P $st 130 $y 230 110 110 }
# the countdown plaque (walnut with a brass rim, the screen writes the time on it) and the progress groove under it
Fill $st 125 59 171 83 @(200,160,70) 0; Fill $st 126 60 170 82 @(36,24,12) 0; Fill $st 127 61 169 62 @(60,40,22) 0
Fill $st 125 86 171 90 @(200,160,70) 0; Fill $st 126 87 170 89 @(28,18,8) 0
# the finished book (144,101)
Frame $st 139 96 26 @(70,48,28) $sdk @(232,193,74)
Inventory $st 140 $sin $sdk $slt
Save $st 'gui\container\stitching_table.png'

# Librarian Table: dark oak panel, green felt under the slots, a page strip for the drifting letters
$lt = NewBmp 256 256
Panel $lt 176 166 @(84,58,36) @(140,104,70) @(40,26,14)
Fill $lt 8 16 167 47 @(46,98,64) 5
$lin = @(30,62,40); $ldk = @(16,34,22); $llt = @(120,180,140)
Frame $lt 17 21 26 $lin $ldk @(214,176,74)
Frame $lt 61 25 18 $lin $ldk $llt
Frame $lt 131 21 26 $lin $ldk @(214,176,74)
# + between the slots, an arrow to the result
Fill $lt 48 33 54 34 @(214,176,74) 0; Fill $lt 50 31 52 36 @(214,176,74) 0
for($x=86;$x -le 120;$x++){ if(($x % 3) -ne 0){ P $lt $x 33 214 176 74; P $lt $x 34 160 128 50 } }
for($k=0;$k -le 4;$k++){ for($y=(29+$k);$y -le (38-$k);$y++){ P $lt (121+$k) $y 232 193 74 } }
# the page (letters drift across it, client/LibrarianTableScreen)
Fill $lt 30 50 145 67 @(236,224,190) 4
for($x=30;$x -le 145;$x++){ P $lt $x 50 200 184 150; P $lt $x 67 180 164 130 }
Inventory $lt 84 @(52,36,22) @(26,16,8) @(140,104,70)
Save $lt 'gui\container\librarian_table.png'
'saved'

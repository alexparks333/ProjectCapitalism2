# Draws the Knowledge Creator and Knowledge Piece textures (Librarian.md):
#  - block/knowledge_creator_side, _top: dark polished deepslate with a strip of books and a violet rune (the dark style)
#  - gui/container/knowledge_creator.png (176x166): deepslate panel, the book slot (35,35), the channel between the
#    slots (62..116, y 42), the piece slot (125,35), a strip for the preview line, the inventory. Must match
#    librarian/KnowledgeCreatorMenu and client/KnowledgeCreatorScreen.
#  - item/knowledge_piece_clear / _bright / _radiant: the Knowledge Piece (item/knowledge_piece.png, Faded) recolored
#    for each quality (librarian/KnowledgeQuality)
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 5
function C([double]$v){ [int][Math]::Max(0,[Math]::Min(255,$v)) }
function NewBmp([int]$w,[int]$h){ New-Object Drawing.Bitmap $w,$h,([Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function P($bmp,[int]$x,[int]$y,[double]$r,[double]$g,[double]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Fill($bmp,[int]$x0,[int]$y0,[int]$x1,[int]$y1,[int[]]$c,[int]$noise){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ $n=$rng.Next(-$noise,$noise+1); P $bmp $x $y ($c[0]+$n) ($c[1]+$n) ($c[2]+$n) } } }
function Save($bmp,[string]$rel){ $bmp.Save((Join-Path $res $rel),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }
$deep = @(52,52,60); $deepDark = @(30,30,36); $deepLight = @(84,84,94); $violet = @(150,96,230)

# ── block: side (the model uses rows 7-15: the pedestal is 9 high) ──
$side = NewBmp 16 16
Fill $side 0 0 15 15 $deep 4
Fill $side 0 7 15 7 $deepLight 3
Fill $side 0 15 15 15 $deepDark 2
Fill $side 0 7 0 15 $deepDark 2; Fill $side 15 7 15 15 $deepDark 2
# a strip of books
$books = @(@(120,40,40),@(50,80,130),@(60,110,60),@(140,110,50),@(90,50,120),@(130,70,40),@(40,90,100),@(110,40,80))
for($x=2;$x -le 13;$x++){ $b = $books[($x * 3) % $books.Count]; $top = 9; if(($x % 3) -eq 0){ $top = 10 }
  for($y=$top;$y -le 12;$y++){ P $side $x $y $b[0] $b[1] $b[2] }; P $side $x $top ($b[0]+40) ($b[1]+40) ($b[2]+40) }
for($x=1;$x -le 14;$x++){ P $side $x 8 $deepDark[0] $deepDark[1] $deepDark[2]; P $side $x 13 $deepDark[0] $deepDark[1] $deepDark[2] }
# a violet rune under the books
foreach($p in @(@(7,14),@(8,14),@(6,14),@(9,14))){ P $side $p[0] $p[1] $violet[0] $violet[1] $violet[2] }
Save $side 'block\knowledge_creator_side.png'

$top = NewBmp 16 16
Fill $top 0 0 15 15 $deep 4
for($i=0;$i -lt 16;$i++){ P $top $i 0 $deepLight[0] $deepLight[1] $deepLight[2]; P $top 0 $i $deepLight[0] $deepLight[1] $deepLight[2]; P $top $i 15 $deepDark[0] $deepDark[1] $deepDark[2]; P $top 15 $i $deepDark[0] $deepDark[1] $deepDark[2] }
# a glowing violet ring round the jar (the jar covers 3-12)
for($i=2;$i -le 13;$i++){ if(($i % 2) -eq 0){ P $top $i 2 $violet[0] $violet[1] $violet[2]; P $top $i 13 $violet[0] $violet[1] $violet[2]; P $top 2 $i $violet[0] $violet[1] $violet[2]; P $top 13 $i $violet[0] $violet[1] $violet[2] } }
Save $top 'block\knowledge_creator_top.png'

# ── GUI ──
$gui = NewBmp 256 256
for($y=0;$y -lt 166;$y++){ $t = $y / 166.0; for($x=0;$x -lt 176;$x++){ $n=$rng.Next(-3,4); P $gui $x $y (40 - 12*$t + $n) (36 - 10*$t + $n) (52 - 12*$t + $n) } }
for($x=0;$x -lt 176;$x++){ P $gui $x 0 8 6 12; P $gui $x 165 8 6 12; P $gui $x 1 120 100 170; P $gui $x 164 20 16 30 }
for($y=0;$y -lt 166;$y++){ P $gui 0 $y 8 6 12; P $gui 175 $y 8 6 12; P $gui 1 $y 120 100 170; P $gui 174 $y 20 16 30 }
# faint runes scattered on the panel
for($i=0;$i -lt 40;$i++){ $x=$rng.Next(6,170); $y=$rng.Next(18,74); P $gui $x $y 70 56 100 }
function Frame($bmp,[int]$x0,[int]$y0,[int]$s,[int[]]$in,[int[]]$dark,[int[]]$light){
  Fill $bmp ($x0+1) ($y0+1) ($x0+$s-2) ($y0+$s-2) $in 0
  for($i=0;$i -lt $s;$i++){ P $bmp ($x0+$i) $y0 $dark[0] $dark[1] $dark[2]; P $bmp $x0 ($y0+$i) $dark[0] $dark[1] $dark[2]
    P $bmp ($x0+$i) ($y0+$s-1) $light[0] $light[1] $light[2]; P $bmp ($x0+$s-1) ($y0+$i) $light[0] $light[1] $light[2] } }
$in = @(20,16,30); $dk = @(8,6,12); $lt = @(110,90,160)
Frame $gui 30 30 26 $in $dk @(176,130,255)
Frame $gui 120 30 26 $in $dk @(232,193,74)
# the channel between them
Fill $gui 61 41 117 47 @(14,10,22) 0
for($x=61;$x -le 117;$x++){ P $gui $x 41 8 6 12; P $gui $x 47 90 70 130 }
# the preview strip
Fill $gui 8 54 167 72 @(26,20,38) 2
for($x=8;$x -le 167;$x++){ P $gui $x 54 12 8 18; P $gui $x 72 90 70 130 }
for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Frame $gui (7 + 18*$c) (83 + 18*$r) 18 $in $dk $lt } }
for($c=0;$c -lt 9;$c++){ Frame $gui (7 + 18*$c) 141 18 $in $dk $lt }
Save $gui 'gui\container\knowledge_creator.png'

# ── Knowledge Pieces: recolor the Faded one (parchment -> tint, rune -> glow color) ──
$base = [Drawing.Bitmap]::FromFile((Join-Path $res 'item\knowledge_piece.png'))
$pieces = [ordered]@{
  clear   = @(@(200,226,240), @(90,190,255))
  bright  = @(@(222,200,250), @(190,110,255))
  radiant = @(@(255,232,160), @(255,250,200)) }
foreach($k in $pieces.Keys){ $paper = $pieces[$k][0]; $glow = $pieces[$k][1]
  $bmp = NewBmp 16 16
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $base.GetPixel($x,$y); if($c.A -eq 0){ continue }
    $isRune = $c.G -gt 150 -and $c.B -gt 150 -and $c.R -lt 230
    if($isRune){ P $bmp $x $y $glow[0] $glow[1] $glow[2]; continue }
    $l = ($c.R + $c.G + $c.B) / 3.0 / 200.0
    P $bmp $x $y ($paper[0]*$l) ($paper[1]*$l) ($paper[2]*$l) } }
  if($k -eq 'radiant'){ foreach($p in @(@(2,3),@(13,2),@(14,12),@(1,11))){ P $bmp $p[0] $p[1] 255 240 150 } }
  Save $bmp "item\knowledge_piece_$k.png" }
$base.Dispose()
'saved'

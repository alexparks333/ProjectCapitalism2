# Draws the Enchanter's Table menu (textures/gui/container/enchanters_table.png, 176x222): a night-sky violet panel with
# faint stars and runes, a glowing frame for the big slot and the result, a Book slot, a rune arrow, the inset where the
# enchantment rows go, and the inventory. Positions must match client/EnchantersTableScreen and EnchantersTableMenu.
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\gui\container\enchanters_table.png'
$W = 176; $H = 222
$bmp = New-Object Drawing.Bitmap 256,256,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
$rng = New-Object System.Random 23
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $y -ge 0 -and $x -lt 256 -and $y -lt 256){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }
function Rect([int]$x0,[int]$y0,[int]$x1,[int]$y1,[int]$r,[int]$g,[int]$b){ for($y=$y0;$y -le $y1;$y++){ for($x=$x0;$x -le $x1;$x++){ Px $x $y $r $g $b } } }

# ── panel: deep violet, darker toward the bottom, with stars ──
for($y=0;$y -lt $H;$y++){ $t = $y / $H
  for($x=0;$x -lt $W;$x++){ $n = $rng.Next(-3,4)
    Px $x $y ([int](46 - 18*$t)+$n) ([int](30 - 12*$t)+$n) ([int](72 - 22*$t)+$n) } }
for($i=0;$i -lt 70;$i++){ $x=$rng.Next(4,$W-4); $y=$rng.Next(4,$H-4); $s=$rng.Next(0,3)
  if($s -eq 0){ Px $x $y 200 185 255 } elseif($s -eq 1){ Px $x $y 140 120 200 } else { Px $x $y 110 170 230 } }
# bevelled edge: bright violet top-left, dark bottom-right, black outline
for($x=0;$x -lt $W;$x++){ Px $x 0 10 6 18; Px $x ($H-1) 10 6 18; Px $x 1 150 120 220; Px $x 2 110 86 170; Px $x ($H-2) 18 10 32; Px $x ($H-3) 26 16 44 }
for($y=0;$y -lt $H;$y++){ Px 0 $y 10 6 18; Px ($W-1) $y 10 6 18; Px 1 $y 150 120 220; Px 2 $y 110 86 170; Px ($W-2) $y 18 10 32; Px ($W-3) $y 26 16 44 }

# a standard inset slot frame (18x18 at x0,y0), violet-tinted
function Slot([int]$x0,[int]$y0){
  Rect ($x0+1) ($y0+1) ($x0+16) ($y0+16) 24 16 38
  for($i=0;$i -lt 18;$i++){ Px ($x0+$i) $y0 12 8 20; Px $x0 ($y0+$i) 12 8 20; Px ($x0+$i) ($y0+17) 120 100 170; Px ($x0+17) ($y0+$i) 120 100 170 } }
# a big 26x26 frame with a glowing rune ring around it
function BigSlot([int]$x0,[int]$y0){
  Rect ($x0+1) ($y0+1) ($x0+24) ($y0+24) 20 12 34
  for($i=0;$i -lt 26;$i++){ Px ($x0+$i) $y0 12 8 20; Px $x0 ($y0+$i) 12 8 20; Px ($x0+$i) ($y0+25) 130 110 185; Px ($x0+25) ($y0+$i) 130 110 185 }
  for($i=-2;$i -lt 28;$i++){ foreach($p in @(@(($x0+$i),($y0-2)),@(($x0+$i),($y0+27)),@(($x0-2),($y0+$i)),@(($x0+27),($y0+$i)))){
    if((($i + 40) % 4) -lt 2){ Px $p[0] $p[1] 176 120 255 } else { Px $p[0] $p[1] 92 62 150 } } } }

BigSlot 17 21        # the enchanted item (slot at 22,26)
Slot 61 25           # the Book (slot at 62,26)
BigSlot 131 21       # the result (slot at 136,26)

# ── arrow from the Book slot to the result: a dotted rune line and a head ──
for($x=86;$x -le 120;$x++){ if(($x % 3) -ne 0){ Px $x 33 170 120 250; Px $x 34 120 80 200 } }
for($k=0;$k -le 4;$k++){ for($y=(29+$k);$y -le (38-$k);$y++){ Px (121+$k) $y 190 140 255 } }
# a little + between the item and the Book
Rect 48 33 54 34 150 120 220; Rect 50 31 52 36 150 120 220

# ── the enchantment list: a dark inset (rows drawn by the screen) ──
Rect 8 49 167 125 16 10 28
for($x=8;$x -le 167;$x++){ Px $x 49 8 5 14; Px $x 125 110 90 160 }
for($y=49;$y -le 125;$y++){ Px 8 $y 8 5 14; Px 167 $y 110 90 160 }

# ── inventory: 3 rows at y 140, hotbar at 198 ──
for($r=0;$r -lt 3;$r++){ for($c=0;$c -lt 9;$c++){ Slot (7 + 18*$c) (139 + 18*$r) } }
for($c=0;$c -lt 9;$c++){ Slot (7 + 18*$c) 197 }

$bmp.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
'saved'

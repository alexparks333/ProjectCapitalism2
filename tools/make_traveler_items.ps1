# Draws the Traveling Merchant's own goods (Town.md > The Traveling Merchant), 16x16 each:
#  items: bottomless_waterskin (a stitched leather skin with a cork and strap, a blue drop burned on it), hand_warmer (a
#  little brass tin with a glowing grille), smiths_chisel (a steel chisel on a wooden handle), phoenix_feather (a feather
#  burning red to gold); blocks: the Snow Globe's parts (dark wood base, snow, a cabin, a red roof, a pine, frosty glass)
#  and the Traveler's Lantern's (dark iron, old brass, smoky glass dark and lit).
# Run: powershell -File tools\make_traveler_items.ps1
Add-Type -AssemblyName System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 41
function New-Img { New-Object System.Drawing.Bitmap 16, 16 }
function P($b, $x, $y, $r, $g, $bl, $a = 255) { if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, [Math]::Max(0, [Math]::Min(255, $r)), [Math]::Max(0, [Math]::Min(255, $g)), [Math]::Max(0, [Math]::Min(255, $bl)))) } }
function Fill($b, $r, $g, $bl, $noise, $a = 255) { for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $n = $rng.Next(-$noise, $noise + 1); P $b $x $y ($r + $n) ($g + $n) ($bl + $n) $a } } }
function Save($b, $rel) { $b.Save((Join-Path $res $rel), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }
# draw from a picture: one char per pixel, '.' = clear
function Pic($rows, $palette) {
  $b = New-Img
  for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $ch = [string]$rows[$y][$x]; if ($palette.ContainsKey($ch)) { $c = $palette[$ch]; $n = $rng.Next(-6, 7); P $b $x $y ($c[0] + $n) ($c[1] + $n) ($c[2] + $n) } } }
  return $b
}

# ── items ──
$skin = Pic @(
  '......cc........',
  '......cc........',
  '.....LLLL.......',
  '....sLLLLs......',
  '...s.LmmL.s.....',
  '..s.LLmmLL.s....',
  '.s.LLLmmLLL.s...',
  '.s.LLmbbmLL..s..',
  's.LLLb22bLLL.s..',
  's.LLLb22bLLL..s.',
  's.LLLLbbLLLLL.s.',
  's.LLLLLLLLLLL.s.',
  '.s.LLLLLLLLL.s..',
  '..s.LLLLLLL.s...',
  '...ss.LLL.ss....',
  '.....ssss.......') @{ 'c' = @(196, 164, 110); 'L' = @(132, 88, 48); 'm' = @(164, 116, 66); 's' = @(70, 44, 24); 'b' = @(60, 120, 200); '2' = @(130, 190, 250) }
Save $skin 'item\bottomless_waterskin.png'

$warmer = Pic @(
  '................',
  '................',
  '................',
  '...bbbbbbbbbb...',
  '..b2222222222b..',
  '..b2gggggggg2b..',
  '..b2gOgOgOgO2b..',
  '..b2gggggggg2b..',
  '..b2gOgOgOgO2b..',
  '..b2gggggggg2b..',
  '..b2222222222b..',
  '..bdddddddddd b.',
  '...bbbbbbbbbb...',
  '................',
  '................',
  '................') @{ 'b' = @(110, 76, 30); '2' = @(200, 158, 70); 'g' = @(60, 40, 24); 'O' = @(255, 150, 40); 'd' = @(150, 110, 46) }
Save $warmer 'item\hand_warmer.png'

$chisel = Pic @(
  '............ss..',
  '...........s33s.',
  '..........s33s..',
  '.........s33s...',
  '........s33s....',
  '.......s33s.....',
  '......i33s......',
  '.....i44i.......',
  '....h55i........',
  '...h55h.........',
  '..h55h..........',
  '.h55h...........',
  'h55h............',
  'h5h.............',
  '.h..............',
  '................') @{ 's' = @(120, 124, 136); '3' = @(210, 214, 224); 'i' = @(70, 70, 80); '4' = @(110, 110, 120); 'h' = @(90, 56, 28); '5' = @(150, 100, 52) }
Save $chisel 'item\smiths_chisel.png'

$feather = Pic @(
  '...........y6...',
  '..........y66o..',
  '.........y66oo..',
  '........y66oor..',
  '.......o66oorr..',
  '......oo6oorr...',
  '.....oo6oorr....',
  '....roo6orr.....',
  '...rroo6rr......',
  '...rro6rr.......',
  '..rro6rr........',
  '..rr6rr.........',
  '..r6r...........',
  '.w6.............',
  'w...............',
  '................') @{ 'y' = @(255, 220, 90); '6' = @(255, 240, 160); 'o' = @(255, 140, 40); 'r' = @(200, 40, 24); 'w' = @(120, 60, 30) }
Save $feather 'item\phoenix_feather.png'

# (the Snow Globe's textures are drawn by tools/make_snow_globe.ps1)

# ── the Traveler's Lantern's parts ──
$b = New-Img; Fill $b 54 54 60 6; for ($k = 0; $k -lt 12; $k++) { P $b $rng.Next(16) $rng.Next(16) 120 70 40 }; Save $b 'block\travelers_lantern_iron.png'   # rust spots
$b = New-Img; Fill $b 176 136 60 10; for ($k = 0; $k -lt 16; $k++) { P $b $rng.Next(16) $rng.Next(16) 110 120 70 }; Save $b 'block\travelers_lantern_brass.png'   # verdigris
# smoky glass, dark (no flame), and lit (a candle flame glowing inside)
$b = New-Img; Fill $b 70 60 46 6 200; for ($y = 0; $y -lt 16; $y++) { P $b 7 $y 40 34 26 220; P $b 8 $y 40 34 26 220 }; Save $b 'block\travelers_lantern_glass.png'
$b = New-Img
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $d = [Math]::Sqrt(($x - 7.5) * ($x - 7.5) + ($y - 9) * ($y - 9)); $k = [Math]::Max(0, 1 - $d / 9); P $b $x $y (200 + 55 * $k) (130 + 110 * $k) (50 + 120 * $k * $k) 230 } }
for ($y = 5; $y -lt 11; $y++) { P $b 7 $y 255 250 220; P $b 8 $y 255 236 170 }
Save $b 'block\travelers_lantern_glass_lit.png'
'saved'

# Cocktail item textures (16x16), in the style of the first cocktails (Bartending.md): a glass with a light left wall,
# a darker right wall and bottom, faint glass where it's empty, a white shine down the left, the drink with a lighter
# top line, and a garnish. Run: powershell -ExecutionPolicy Bypass -File tools\make_cocktails.ps1 [name ...]
# (no names = all of them).
param([string[]]$Only)
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'

function Hex($h) { [System.Drawing.Color]::FromArgb([Convert]::ToInt32($h, 16)) }   # "AARRGGBB"
function Lighter($c, $amt = 40) { [System.Drawing.Color]::FromArgb($c.A, [Math]::Min(255, $c.R + $amt), [Math]::Min(255, $c.G + $amt), [Math]::Min(255, $c.B + $amt)) }
function Darker($c, $amt = 40) { [System.Drawing.Color]::FromArgb($c.A, [Math]::Max(0, $c.R - $amt), [Math]::Max(0, $c.G - $amt), [Math]::Max(0, $c.B - $amt)) }

$WALL_L = Hex 'FFAAD2E2'
$WALL_R = Hex 'FF688C9E'
$EMPTY  = Hex '50E2F2FA'
$SHINE  = Hex 'BEFFFFFF'

function New-Icon { New-Object System.Drawing.Bitmap 16, 16 }
function P($b, $x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16) { $b.SetPixel($x, $y, $c) } }

# A glass: $rows = interior rows @(y, x0, x1) top to bottom; the drink fills from row $fill down; $layers = @(@(fromY, color), ...)
# (each row takes the last layer starting at or above it). A dark bottom row goes under the last interior row
# unless $noBottom (a stemmed glass draws its own).
function Glass($b, $rows, $fill, $layers, [switch]$noBottom, [switch]$noShine) {
    $last = $rows[$rows.Count - 1]
    foreach ($r in $rows) {
        $y = $r[0]; $x0 = $r[1]; $x1 = $r[2]
        P $b ($x0 - 1) $y $WALL_L
        P $b ($x1 + 1) $y $WALL_R
        for ($x = $x0; $x -le $x1; $x++) {
            if ($y -lt $fill) { P $b $x $y $EMPTY; continue }
            $col = $null
            foreach ($l in $layers) { if ($l[0] -le $y) { $col = $l[1] } }
            if ($y -eq $fill) { $col = Lighter $col 45 }
            P $b $x $y $col
        }
        if (-not $noShine -and $y -gt $fill -and $y -lt $last[0] - 1 -and ($x1 - $x0) -ge 2) { P $b $x0 $y $SHINE }
    }
    if (-not $noBottom) { for ($x = $last[1] - 1; $x -le $last[2] + 1; $x++) { P $b $x ($last[0] + 1) $WALL_R } }
}

# a stem from row $y0 to $y1 at x 7..8, and a foot on row $y1+1
function Stem($b, $y0, $y1) {
    for ($y = $y0; $y -le $y1; $y++) { P $b 7 $y $WALL_L; P $b 8 $y $WALL_R }
    for ($x = 5; $x -le 10; $x++) { P $b $x ($y1 + 1) ($(if ($x -lt 8) { $WALL_L } else { $WALL_R })) }
}

function Save($b, $name) { $b.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose(); Write-Output "$name.png" }
function Want($name) { -not $Only -or $Only -contains $name }

# glass shapes
$ROCKS    = @(@(7,5,10), @(8,5,10), @(9,5,10), @(10,5,10), @(11,5,10), @(12,5,10), @(13,5,10), @(14,5,10))
$HIGHBALL = @(3..14 | ForEach-Object { ,@($_, 6, 9) })
$COUPE    = @(@(5,3,12), @(6,4,11), @(7,5,10), @(8,6,9))
$MARTINI  = @(@(3,2,13), @(4,3,12), @(5,4,11), @(6,5,10), @(7,6,9), @(8,7,8))
$WINE     = @(@(3,5,10), @(4,4,11), @(5,4,11), @(6,4,11), @(7,4,11), @(8,5,10), @(9,6,9))

# ── Margarita: pale lime, a salted rim, a lime wedge ──
if (Want 'margarita') {
    $b = New-Icon
    Glass $b $COUPE 5 @(,@(0, (Hex 'FFC8E07A'))) -noBottom
    Stem $b 9 13
    foreach ($x in 2..13) { if ($x -ne 7) { P $b $x 4 (Hex 'FFF4F8F8') } }   # salt on the rim
    P $b 12 3 (Hex 'FF5AA03C'); P $b 13 3 (Hex 'FF7CC24E'); P $b 13 2 (Hex 'FF5AA03C'); P $b 12 2 (Hex 'FFD8F0A0')
    Save $b 'margarita'
}

# ── Berry Daiquiri: deep berry red in a coupe, a berry on the rim ──
if (Want 'berry_daiquiri') {
    $b = New-Icon
    Glass $b $COUPE 5 @(,@(0, (Hex 'FFC8344E'))) -noBottom
    Stem $b 9 13
    P $b 11 3 (Hex 'FFB0203A'); P $b 12 3 (Hex 'FFD84A60'); P $b 11 4 (Hex 'FF8A1A2C'); P $b 12 2 (Hex 'FF468232')
    Save $b 'berry_daiquiri'
}

# ── Cosmopolitan: pink in a martini glass, an orange twist ──
if (Want 'cosmopolitan') {
    $b = New-Icon
    Glass $b $MARTINI 4 @(,@(0, (Hex 'FFE6708A'))) -noBottom -noShine
    P $b 4 5 $SHINE; P $b 5 6 $SHINE
    Stem $b 9 13
    P $b 12 2 (Hex 'FFF0A030'); P $b 13 1 (Hex 'FFF0A030'); P $b 13 2 (Hex 'FFD88020')
    Save $b 'cosmopolitan'
}

# ── Tequila Sunrise: yellow to orange to red in a highball, an orange slice and a cherry ──
if (Want 'tequila_sunrise') {
    $b = New-Icon
    Glass $b $HIGHBALL 4 @(@(0, (Hex 'FFF2CC40')), @(7, (Hex 'FFF2A034')), @(10, (Hex 'FFE8702E')), @(12, (Hex 'FFCC3A30')))
    P $b 10 2 (Hex 'FFF0A030'); P $b 10 3 (Hex 'FFF8C860'); P $b 11 3 (Hex 'FFF0A030'); P $b 11 2 (Hex 'FFD88020')
    P $b 9 1 (Hex 'FFC81E2E'); P $b 9 0 (Hex 'FF468232')
    Save $b 'tequila_sunrise'
}

# ── Moscow Mule: a hammered copper mug with a handle, lime and mint on top ──
if (Want 'moscow_mule') {
    $b = New-Icon
    $cu = Hex 'FFC07040'; $cuL = Hex 'FFE8A070'; $cuD = Hex 'FF8A4826'
    for ($y = 6; $y -le 14; $y++) {
        for ($x = 4; $x -le 10; $x++) {
            $c = $cu
            if ($x -eq 4) { $c = $cuL } elseif ($x -eq 10 -or $y -eq 14) { $c = $cuD } elseif (($x + $y) % 4 -eq 0) { $c = Lighter $cu 25 }
            P $b $x $y $c
        }
    }
    foreach ($x in 4..10) { P $b $x 5 (Hex 'FFF0B888') }               # rim
    foreach ($y in 7..11) { P $b 12 $y $cuD }; P $b 11 7 $cuD; P $b 11 11 $cuD   # handle
    P $b 5 4 (Hex 'FF7CC24E'); P $b 6 4 (Hex 'FF5AA03C'); P $b 6 3 (Hex 'FF468232'); P $b 8 4 (Hex 'FFC8E07A'); P $b 9 4 (Hex 'FF5AA03C')
    Save $b 'moscow_mule'
}

# ── Rum Punch: red-orange in a highball, a pineapple-ish wedge and a little paper umbrella ──
if (Want 'rum_punch') {
    $b = New-Icon
    Glass $b $HIGHBALL 4 @(@(0, (Hex 'FFE85A3A')), @(10, (Hex 'FFD0402E')))
    P $b 3 0 (Hex 'FF3CA0E0'); P $b 4 0 (Hex 'FF3CA0E0'); P $b 5 0 (Hex 'FF3CA0E0'); P $b 4 1 (Hex 'FF2A78B0'); P $b 2 1 (Hex 'FF2A78B0'); P $b 6 1 (Hex 'FF2A78B0')
    P $b 5 2 (Hex 'FF9A7A50'); P $b 6 3 (Hex 'FF9A7A50')                 # umbrella stick
    P $b 10 2 (Hex 'FFF2CC40'); P $b 11 2 (Hex 'FFF8E080'); P $b 10 3 (Hex 'FFD8A830')
    Save $b 'rum_punch'
}

# ── Rosé Spritz: pink in a wine glass, little bubbles ──
if (Want 'rose_spritz') {
    $b = New-Icon
    Glass $b $WINE 5 @(,@(0, (Hex 'FFF09AAE'))) -noBottom
    foreach ($x in 6..9) { P $b $x 10 $WALL_R }
    Stem $b 11 13
    P $b 7 7 (Hex 'FFFFE0E8'); P $b 9 6 (Hex 'FFFFE0E8'); P $b 8 8 (Hex 'FFFFE0E8')
    P $b 10 3 (Hex 'FFE0405A'); P $b 11 2 (Hex 'FF468232')
    Save $b 'rose_spritz'
}

# ── Pumpkin Spice Flip: pumpkin orange under a cream foam, dusted with cinnamon ──
if (Want 'pumpkin_spice_flip') {
    $b = New-Icon
    Glass $b $ROCKS 7 @(@(0, (Hex 'FFF2E4C8')), @(9, (Hex 'FFE08A38')), @(13, (Hex 'FFC87028')))
    P $b 7 8 (Hex 'FFF2E4C8'); P $b 9 8 (Hex 'FFF2E4C8'); P $b 6 8 (Hex 'FFF2E4C8')
    P $b 6 7 (Hex 'FF8C5028'); P $b 9 7 (Hex 'FF8C5028'); P $b 8 8 (Hex 'FF8C5028')
    P $b 10 5 (Hex 'FF8C5028'); P $b 10 6 (Hex 'FFA86434'); P $b 11 4 (Hex 'FF8C5028')   # cinnamon stick
    Save $b 'pumpkin_spice_flip'
}

# ══ The second round of Specialty cocktails ══

# ── Forgefire Old Fashioned: deep amber on the rocks, an ice cube, a glowing ember on the rim ──
if (Want 'forgefire_old_fashioned') {
    $b = New-Icon
    Glass $b $ROCKS 8 @(@(0, (Hex 'FFC8742A')), @(12, (Hex 'FFA85A1E')))
    P $b 7 9 (Hex 'E8F4FAFF'); P $b 8 9 (Hex 'E8F4FAFF'); P $b 7 10 (Hex 'C8DCEAF4'); P $b 8 10 (Hex 'FFC8742A')
    P $b 10 6 (Hex 'FFFF5A1E'); P $b 11 6 (Hex 'FFFFA030'); P $b 10 5 (Hex 'FFFFD060'); P $b 11 7 (Hex 'FFC83A14')
    Save $b 'forgefire_old_fashioned'
}

# ── Angler's Sunrise: kelp green fading into glow-berry gold, a little fish on the rim ──
if (Want 'anglers_sunrise') {
    $b = New-Icon
    Glass $b $HIGHBALL 4 @(@(0, (Hex 'FF5AA05A')), @(8, (Hex 'FFA8B848')), @(11, (Hex 'FFF2B83C')))
    P $b 10 2 (Hex 'FF8CB4C8'); P $b 11 2 (Hex 'FF8CB4C8'); P $b 12 2 (Hex 'FF6A90A8'); P $b 13 1 (Hex 'FF6A90A8'); P $b 13 3 (Hex 'FF6A90A8'); P $b 10 1 (Hex 'FF202830')
    Save $b 'anglers_sunrise'
}

# ── Butcher's Bloody Mary: thick tomato red in a highball, a bone for a stirrer ──
if (Want 'butchers_bloody_mary') {
    $b = New-Icon
    Glass $b $HIGHBALL 4 @(@(0, (Hex 'FFB8282A')), @(11, (Hex 'FF961C20')))
    P $b 9 0 (Hex 'FFF4EEDC'); P $b 10 0 (Hex 'FFF4EEDC'); P $b 9 1 (Hex 'FFE8E0C8'); P $b 9 2 (Hex 'FFE8E0C8'); P $b 9 3 (Hex 'FFD8CCAC')
    P $b 6 5 (Hex 'FFE85A50'); P $b 8 7 (Hex 'FF7A1418')
    Save $b 'butchers_bloody_mary'
}

# ── Hot Toddy: honey amber in a glass mug, steam rising, a lemon slice ──
if (Want 'hot_toddy') {
    $b = New-Icon
    Glass $b $ROCKS 8 @(,@(0, (Hex 'FFE8A040')))
    foreach ($y in 9..12) { P $b 12 $y $WALL_R }; P $b 11 9 $WALL_R; P $b 11 12 $WALL_R
    P $b 6 5 (Hex 'A0FFFFFF'); P $b 7 4 (Hex 'A0FFFFFF'); P $b 6 3 (Hex '80FFFFFF'); P $b 9 5 (Hex 'A0FFFFFF'); P $b 8 4 (Hex '80FFFFFF'); P $b 9 2 (Hex '60FFFFFF')
    P $b 10 7 (Hex 'FFF8E040'); P $b 11 7 (Hex 'FFE8C020'); P $b 11 6 (Hex 'FFF8E880')
    Save $b 'hot_toddy'
}

# ── Midnight Oil Martini: espresso dark in a martini glass, a cream top and three beans ──
if (Want 'midnight_oil_martini') {
    $b = New-Icon
    Glass $b $MARTINI 3 @(@(0, (Hex 'FFE0C8A0')), @(4, (Hex 'FF4A2A1A')))
    Stem $b 9 13
    P $b 6 3 (Hex 'FF3A1E10'); P $b 8 3 (Hex 'FF3A1E10'); P $b 10 3 (Hex 'FF3A1E10')
    Save $b 'midnight_oil_martini'
}

# ── Haggler's Highball: pale gold and fizzy, a gold nugget on the rim ──
if (Want 'hagglers_highball') {
    $b = New-Icon
    Glass $b $HIGHBALL 4 @(,@(0, (Hex 'FFF0D890')))
    P $b 7 7 (Hex 'FFFFFBE8'); P $b 8 10 (Hex 'FFFFFBE8'); P $b 7 12 (Hex 'FFFFFBE8'); P $b 8 6 (Hex 'FFFFFBE8')
    P $b 10 2 (Hex 'FFFFE040'); P $b 11 2 (Hex 'FFF0B820'); P $b 10 3 (Hex 'FFD89818'); P $b 11 1 (Hex 'FFFFF6A0')
    Save $b 'hagglers_highball'
}

# ── Desert Rose: sunset pink-orange in a coupe, a tiny cactus on the rim ──
if (Want 'desert_rose') {
    $b = New-Icon
    Glass $b $COUPE 5 @(@(0, (Hex 'FFF08060')), @(7, (Hex 'FFE85A6A')))
    Stem $b 9 13
    P $b 12 3 (Hex 'FF4E9A3A'); P $b 12 2 (Hex 'FF4E9A3A'); P $b 12 1 (Hex 'FF5EAE46'); P $b 13 2 (Hex 'FF3C7E2C'); P $b 11 2 (Hex 'FF3C7E2C'); P $b 12 0 (Hex 'FFF070A0')
    Save $b 'desert_rose'
}

# ── Moonstalker Sangria: dark crimson in a wine glass, a pale crescent moon on the rim ──
if (Want 'moonstalker_sangria') {
    $b = New-Icon
    Glass $b $WINE 5 @(@(0, (Hex 'FF8A1430')), @(8, (Hex 'FF6A0C22')))
    foreach ($x in 6..9) { P $b $x 10 $WALL_R }
    Stem $b 11 13
    P $b 11 1 (Hex 'FFF4ECB0'); P $b 12 2 (Hex 'FFF4ECB0'); P $b 12 3 (Hex 'FFE8DC90'); P $b 11 4 (Hex 'FFE8DC90'); P $b 10 1 (Hex 'FFFFFAD8')
    Save $b 'moonstalker_sangria'
}

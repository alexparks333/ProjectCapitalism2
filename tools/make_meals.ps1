# Meal item textures (16x16) for the newer Cook dishes (Cook.md), Uncooked and Gourmet. Each one reuses a container
# from an existing meal so they all match: the plate of Lamb Chops, the bowl of Seafood Chowder, the pie tin of Chicken
# Pot Pie. The food in it is cleared and painted fresh: a speckled fill (a few colors mixed by position) and hand-placed
# pixels. Run: powershell -ExecutionPolicy Bypass -File tools\make_meals.ps1 [name ...] (no names = all).
param([string[]]$Only)
Add-Type -AssemblyName System.Drawing
$items = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'

function Hex($h) { [System.Drawing.Color]::FromArgb([Convert]::ToInt32("FF$h", 16)) }   # "RRGGBB"
function Key($c) { '{0:X2}{1:X2}{2:X2}' -f $c.R, $c.G, $c.B }
function Want($name) { -not $Only -or $Only -contains $name }
function P($b, $x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16) { $b.SetPixel($x, $y, $c) } }

# The containers: the base picture, and which of its colors are food (to clear / repaint)
$PLATE = @{ file = 'lamb_chops'; food = @('FCF8EA', '968C7A', 'F8DC82', 'E6BA5C', 'E2C48C', 'C89C3E', 'BC7040', '5C963C', '96522E'); wipe = 'DEE2E8' }
$BOWL  = @{ file = 'seafood_chowder'; food = @('DCC8A0', 'F0E2C2', 'FAF8F0', 'EC8054', '78A046', '3C6E32') }
$TIN   = @{ file = 'chicken_pot_pie'; food = @('F0BE68', 'B6702E', 'D69646', '703E1A', 'ECD6A0', 'EC8022') }

# a copy of the container with its food pixels listed (and, for the plate, wiped back to bare plate)
function Base($kind) {
    $src = [System.Drawing.Bitmap]::FromFile((Join-Path $items "$($kind.file).png"))
    $b = New-Object System.Drawing.Bitmap 16, 16
    $mask = New-Object System.Collections.ArrayList
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = $src.GetPixel($x, $y)
            if ($c.A -gt 0 -and $kind.food -contains (Key $c)) {
                [void]$mask.Add(@($x, $y))
                if ($kind.ContainsKey('wipe')) { $c = Hex $kind['wipe'] }
            }
            $b.SetPixel($x, $y, $c)
        }
    }
    $src.Dispose()
    # the plate's bone tips poke out past the rim: rub out anything outside the plate
    if ($kind.file -eq 'lamb_chops') { foreach ($p in @(@(9,3), @(9,4), @(10,4), @(14,4), @(14,5), @(15,5), @(14,6), @(15,6))) { $b.SetPixel($p[0], $p[1], [System.Drawing.Color]::Transparent) } }
    return @{ bmp = $b; mask = $mask }
}

# fill the food pixels with a speckle of these colors (repeat a color to make it commoner)
function Speckle($base, $colors, $seed = 1) {
    foreach ($p in $base.mask) {
        $x = $p[0]; $y = $p[1]
        $i = [Math]::Abs(($x * 7 + $y * 13 + $seed * 31 + $x * $y * 3 + ($x -bxor $y) * 5) % $colors.Count)
        P $base.bmp $x $y (Hex $colors[$i])
    }
}

# draw rows of pixels: @(y, x0, x1, color) each
function Rows($b, $rows) { if ($rows.Count -gt 0 -and $rows[0] -isnot [array]) { $rows = ,$rows }; foreach ($r in $rows) { for ($x = $r[1]; $x -le $r[2]; $x++) { P $b $x $r[0] (Hex $r[3]) } } }
function Dots($b, $dots) { foreach ($d in $dots) { P $b $d[0] $d[1] (Hex $d[2]) } }

function Save($b, $name) { $b.Save((Join-Path $items "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose(); Write-Output "$name.png" }

# ── Pulled Pork Sandwich (on a plate): bun, pulled pork, beet slaw, bun ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'pulled_pork_sandwich' } else { 'uncooked_pulled_pork_sandwich' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $bun = $(if ($cooked) { 'D08A40' } else { 'EEDCB0' }); $bunL = $(if ($cooked) { 'E8AC5C' } else { 'F8ECCC' }); $bunD = $(if ($cooked) { 'A86428' } else { 'D8C494' })
    $pork = $(if ($cooked) { '9A4A26' } else { 'E8A0A0' }); $porkL = $(if ($cooked) { 'C0683A' } else { 'F4C0BC' })
    Rows $b @(@(5, 6, 10, $bunD), @(6, 5, 11, $bun), @(7, 4, 12, $bun))
    Dots $b @(@(6, 6, $bunL), @(7, 5, $bunL), @(8, 6, $bunL), @(10, 6, 'F8F0D8'), @(7, 6, 'F8F0D8'), @(9, 7, 'F8F0D8'))
    Rows $b @(@(8, 4, 12, $pork), @(9, 4, 12, $pork))
    Dots $b @(@(5, 8, $porkL), @(8, 8, $porkL), @(11, 8, $porkL), @(6, 9, $porkL), @(10, 9, $porkL), @(3, 9, $pork), @(13, 8, $pork))
    Rows $b @(@(10, 4, 12, 'A8285A'))
    Dots $b @(@(5, 10, 'D04A80'), @(9, 10, 'D04A80'), @(12, 10, 'D04A80'))
    Rows $b @(@(11, 4, 12, $bunD), @(12, 5, 11, $bunD))
    Save $b $name
}

# ── Brisket & Potatoes (on a plate): three slices and two potatoes, a bit of carrot ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'brisket_and_potatoes' } else { 'uncooked_brisket_and_potatoes' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $edge = $(if ($cooked) { '3C1E10' } else { 'B83C48' }); $meat = $(if ($cooked) { '8A4428' } else { 'D05A62' }); $mid = $(if ($cooked) { 'B86A4A' } else { 'E8848A' })
    $fat = $(if ($cooked) { 'D8A070' } else { 'F4E4DC' })
    foreach ($s in @(@(3, 6), @(5, 7), @(7, 8))) {
        $x = $s[0]; $y = $s[1]
        Rows $b @(@($y, $x, ($x + 4), $edge), @(($y + 1), $x, ($x + 4), $meat), @(($y + 2), $x, ($x + 4), $mid))
        P $b ($x + 4) ($y + 1) (Hex $fat)
    }
    $pot = $(if ($cooked) { 'E8C060' } else { 'C8A868' }); $potD = $(if ($cooked) { 'B8863A' } else { 'A08850' }); $potL = $(if ($cooked) { 'F8E098' } else { 'DCC488' })
    Rows $b @(@(7, 10, 12, $pot), @(8, 9, 12, $pot), @(9, 9, 12, $potD), @(10, 10, 13, $pot), @(11, 10, 13, $potD))
    Dots $b @(@(10, 7, $potL), @(10, 8, $potL), @(11, 10, $potL))
    Dots $b @(@(6, 11, 'F08A28'), @(7, 11, 'F08A28'), @(8, 12, 'D86E18'))
    Save $b $name
}

# ── Shepherd's Pie (in a bowl): mashed potato on top, browned peaks when baked ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'shepherds_pie' } else { 'uncooked_shepherds_pie' })
    if (-not (Want $name)) { continue }
    $base = Base $BOWL; $b = $base.bmp
    if ($cooked) { Speckle $base @('F0D9A0', 'F0D9A0', 'E8C070', 'E8C070', 'C8904A', 'F8ECC8') 3 }
    else { Speckle $base @('F4EAD0', 'F4EAD0', 'E8DCC0', 'FAF4E4', 'E8DCC0', 'DCCBA8') 3 }
    Dots $b @(@(4, 7, 'F08A28'), @(11, 8, 'F08A28'), @(7, 9, $(if ($cooked) { '8A4A2A' } else { 'C86A6A' })))
    Save $b $name
}

# ── Ham & Egg Breakfast (on a plate): a slice of ham, two eggs, a slice of bread ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'ham_and_egg_breakfast' } else { 'uncooked_ham_and_egg_breakfast' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $ham = $(if ($cooked) { 'D87A6A' } else { 'F0A0A4' }); $hamD = $(if ($cooked) { 'B05A4A' } else { 'E08088' }); $hamF = $(if ($cooked) { 'F0C8A8' } else { 'FAE4E0' })
    Rows $b @(@(6, 3, 7, $hamF), @(7, 2, 8, $ham), @(8, 2, 8, $ham), @(9, 2, 8, $hamD), @(10, 3, 7, $hamF))
    Dots $b @(@(4, 8, $hamF), @(6, 7, $hamF))
    if ($cooked) {
        foreach ($e in @(@(9, 5), @(9, 9))) {
            $x = $e[0]; $y = $e[1]
            Rows $b @(@($y, ($x + 1), ($x + 3), 'FAFAF4'), @(($y + 1), $x, ($x + 4), 'FAFAF4'), @(($y + 2), ($x + 1), ($x + 3), 'ECECE0'))
            Dots $b @(@(($x + 2), ($y + 1), 'F4B820'), @(($x + 2), $y, 'F8D460'))
        }
    } else {
        foreach ($e in @(@(10, 5), @(10, 9))) {
            $x = $e[0]; $y = $e[1]
            Rows $b @(@($y, $x, ($x + 1), 'F0E2C8'), @(($y + 1), ($x - 1), ($x + 2), 'E8D6B4'), @(($y + 2), $x, ($x + 1), 'CDB894'))
        }
    }
    $bread = $(if ($cooked) { 'C8803A' } else { 'E8CC90' })
    Rows $b @(@(11, 4, 7, $bread), @(12, 4, 7, 'F0DEB0'))
    Save $b $name
}

# ── Hunter's Rabbit Pie (pie tin): a deeper brown crust with a pastry leaf; pale and raw before baking ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'hunters_rabbit_pie' } else { 'uncooked_hunters_rabbit_pie' })
    if (-not (Want $name)) { continue }
    $base = Base $TIN; $b = $base.bmp
    if ($cooked) { Speckle $base @('C07838', 'C07838', 'D89850', 'D89850', 'A86028', 'E4AC64') 5 }
    else { Speckle $base @('F2E2BC', 'F2E2BC', 'E8D4A8', 'FAF0D6', 'E8D4A8', 'DCC498') 5 }
    $vent = $(if ($cooked) { '5A2E12' } else { 'B89C70' })
    Dots $b @(@(6, 6, $vent), @(9, 6, $vent), @(7, 9, $vent), @(10, 8, $vent))
    $leaf = $(if ($cooked) { 'E8B060' } else { 'FFF6E2' }); $vein = $(if ($cooked) { 'A86028' } else { 'D8C498' })
    Dots $b @(@(7, 7, $leaf), @(8, 7, $leaf), @(8, 8, $leaf), @(9, 7, $leaf), @(8, 6, $leaf), @(8, 7, $vein))
    Save $b $name
}

# ── Fish Tacos (on a plate): two folded tortillas with fish, carrot and berries ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'fish_tacos' } else { 'uncooked_fish_tacos' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $shell = $(if ($cooked) { 'E8C060' } else { 'F2E4BC' }); $shellD = $(if ($cooked) { 'C09030' } else { 'DCC898' })
    $fish = $(if ($cooked) { 'F4ECDC' } else { 'E8C4BC' })
    foreach ($t in @(@(2, 6), @(8, 7))) {
        $x = $t[0]; $y = $t[1]
        Rows $b @(@($y, ($x + 1), ($x + 4), $fish), @(($y + 1), $x, ($x + 5), $fish))
        Dots $b @(@(($x + 1), $y, 'F08A28'), @(($x + 3), ($y + 1), 'C8243C'), @(($x + 4), $y, '5C963C'), @(($x + 2), ($y + 1), 'F08A28'))
        Rows $b @(@(($y + 2), $x, ($x + 5), $shell), @(($y + 3), $x, ($x + 5), $shell), @(($y + 4), ($x + 1), ($x + 4), $shellD))
    }
    Save $b $name
}

# ── Mushroom Risotto (in a bowl): creamy rice with brown and red mushroom bits, a green herb when cooked ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'mushroom_risotto' } else { 'uncooked_mushroom_risotto' })
    if (-not (Want $name)) { continue }
    $base = Base $BOWL; $b = $base.bmp
    if ($cooked) { Speckle $base @('F0E0B8', 'F0E0B8', 'E8D29C', 'F8F0D8', 'E8D29C') 7 }
    else { Speckle $base @('F8F6F0', 'F8F6F0', 'ECEAE2', 'FFFFFF', 'ECEAE2') 7 }
    Dots $b @(@(4, 7, '8C6440'), @(9, 8, '8C6440'), @(12, 7, '8C6440'), @(6, 8, 'C83C30'), @(11, 9, 'C83C30'))
    if ($cooked) { Dots $b @(@(7, 7, '5C963C'), @(10, 6, '5C963C')) }
    Save $b $name
}

# ── Apple Crumble (pie tin): a crumbly speckled top with apple showing through ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'apple_crumble' } else { 'uncooked_apple_crumble' })
    if (-not (Want $name)) { continue }
    $base = Base $TIN; $b = $base.bmp
    if ($cooked) { Speckle $base @('C8904A', 'E0B060', 'A86A30', 'E0B060', 'F0CC80', 'C8904A') 9 }
    else { Speckle $base @('F0E0C0', 'E4D2A8', 'F0E0C0', 'FAF0DA', 'E4D2A8') 9 }
    $apple = $(if ($cooked) { 'E8C870' } else { 'F4ECB8' }); $skin = $(if ($cooked) { 'B83A2A' } else { 'D8423A' })
    Dots $b @(@(5, 6, $apple), @(6, 6, $skin), @(10, 7, $apple), @(11, 7, $skin), @(7, 9, $apple), @(8, 9, $skin), @(12, 9, $apple))
    Save $b $name
}

# ══ The second round of Specialty meals ══

# ── Ironworker's Stew (bowl): a dark beefy stew, potato and carrot chunks, a glint of iron ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'ironworkers_stew' } else { 'uncooked_ironworkers_stew' })
    if (-not (Want $name)) { continue }
    $base = Base $BOWL; $b = $base.bmp
    if ($cooked) { Speckle $base @('7A3E22', '7A3E22', '944C2A', '6A321A', '944C2A') 11 }
    else { Speckle $base @('C8505A', 'C8505A', 'D86A70', 'B84048', 'E8D8C8') 11 }
    Dots $b @(@(4, 7, 'E8C060'), @(10, 8, 'E8C060'), @(7, 9, 'F08A28'), @(12, 7, 'F08A28'), @(8, 7, 'D8DCE4'), @(9, 7, 'A8ACB8'))
    Save $b $name
}

# ── Lumberjack Flapjacks (plate): a stack of pancakes, honey dripping, a pat of butter ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'lumberjack_flapjacks' } else { 'uncooked_lumberjack_flapjacks' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    if ($cooked) {
        foreach ($y in 7, 9, 11) { Rows $b @(@($y, 4, 11, 'D89848'), @(($y + 1), 4, 11, 'B07030')) }
        Rows $b @(@(6, 5, 10, 'E8B060'))
        Dots $b @(@(7, 6, 'FFF4A0'), @(8, 6, 'FFF4A0'), @(5, 7, 'F0A020'), @(5, 8, 'F0A020'), @(11, 9, 'F0A020'), @(11, 10, 'F0A020'), @(10, 7, 'F0A020'))
    } else {
        Rows $b @(@(7, 4, 11, 'F4E8C8'), @(8, 3, 12, 'F4E8C8'), @(9, 3, 12, 'EADCB4'), @(10, 3, 12, 'F4E8C8'), @(11, 4, 11, 'EADCB4'))
        Dots $b @(@(6, 9, 'F8D870'), @(9, 8, 'FFFFFF'), @(10, 10, 'F0A020'))
    }
    Save $b $name
}

# ── Builder's Brick-Oven Pizza (plate): a round pizza, red sauce, ham and mushrooms ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'brick_oven_pizza' } else { 'uncooked_brick_oven_pizza' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $crust = $(if ($cooked) { 'C8843A' } else { 'F0E0B8' }); $sauce = $(if ($cooked) { 'C83A28' } else { 'D84A3A' }); $cheese = $(if ($cooked) { 'F4C850' } else { 'F8F0D8' })
    Rows $b @(@(5, 5, 10, $crust), @(6, 4, 11, $crust), @(7, 3, 12, $crust), @(8, 3, 12, $crust), @(9, 3, 12, $crust), @(10, 3, 12, $crust), @(11, 4, 11, $crust), @(12, 5, 10, $crust))
    Rows $b @(@(6, 5, 10, $sauce), @(7, 4, 11, $sauce), @(8, 4, 11, $cheese), @(9, 4, 11, $sauce), @(10, 4, 11, $cheese), @(11, 5, 10, $sauce))
    Dots $b @(@(5, 7, $cheese), @(9, 7, $cheese), @(7, 9, $cheese), @(10, 9, $cheese))
    $ham = $(if ($cooked) { 'D87A6A' } else { 'F0A0A4' })
    Dots $b @(@(6, 8, $ham), @(10, 8, $ham), @(8, 10, $ham), @(5, 10, $ham), @(7, 6, '8C6440'), @(9, 10, '8C6440'), @(11, 7, '8C6440'))
    Save $b $name
}

# ── Prime Rib Dinner (plate): a thick rib roast slice, a baked potato, carrots, a golden carrot ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'prime_rib_dinner' } else { 'uncooked_prime_rib_dinner' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $edge = $(if ($cooked) { '5A2A14' } else { 'B83C48' }); $out = $(if ($cooked) { '9A4A2A' } else { 'D05A62' }); $pink = $(if ($cooked) { 'E07A7A' } else { 'E8848A' })
    Rows $b @(@(5, 3, 8, $edge), @(6, 2, 9, $out), @(7, 2, 9, $pink), @(8, 2, 9, $pink), @(9, 2, 9, $out), @(10, 3, 8, $edge))
    Dots $b @(@(9, 6, 'F4E4DC'), @(9, 7, 'F4E4DC'), @(4, 7, 'F0A0A0'))
    Rows $b @(@(8, 10, 12, 'C8904A'), @(9, 10, 13, 'B07838'), @(10, 10, 12, 'C8904A'))
    Dots $b @(@(11, 8, 'F8F0D8'), @(5, 11, 'F08A28'), @(6, 12, 'F08A28'), @(8, 11, 'F0C030'), @(9, 12, 'F8D860'))
    Save $b $name
}

# ── Angler's Fish Pie (pie tin): a golden mash top with a pastry fish ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'anglers_fish_pie' } else { 'uncooked_anglers_fish_pie' })
    if (-not (Want $name)) { continue }
    $base = Base $TIN; $b = $base.bmp
    if ($cooked) { Speckle $base @('E8B860', 'E8B860', 'F0CC80', 'C8904A', 'F0CC80') 13 }
    else { Speckle $base @('F4EAD0', 'F4EAD0', 'E8DCC0', 'FAF4E4') 13 }
    $fish = $(if ($cooked) { 'B87030' } else { 'D8C498' })
    Dots $b @(@(6, 7, $fish), @(7, 7, $fish), @(8, 7, $fish), @(9, 7, $fish), @(7, 6, $fish), @(7, 8, $fish), @(10, 6, $fish), @(10, 8, $fish), @(5, 7, '303030'))
    Save $b $name
}

# ── Comfort Soup (bowl): golden broth, chicken, carrot coins, rice ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'comfort_soup' } else { 'uncooked_comfort_soup' })
    if (-not (Want $name)) { continue }
    $base = Base $BOWL; $b = $base.bmp
    if ($cooked) { Speckle $base @('F0C860', 'F0C860', 'E8B848', 'F8DC80') 15 }
    else { Speckle $base @('E8DCB8', 'E8DCB8', 'F0E8D0', 'DCCCA8') 15 }
    $chicken = $(if ($cooked) { 'F4E4C8' } else { 'F0C0B8' })
    Dots $b @(@(4, 7, $chicken), @(5, 7, $chicken), @(10, 8, $chicken), @(8, 6, 'F08A28'), @(12, 7, 'F08A28'), @(6, 9, 'F08A28'), @(9, 7, 'FAFAF4'), @(11, 9, 'FAFAF4'), @(7, 8, '5C963C'))
    Save $b $name
}

# ── Trail Mix Bar (plate): two granola bars studded with berries and cocoa ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'trail_mix_bar' } else { 'uncooked_trail_mix_bar' })
    if (-not (Want $name)) { continue }
    $base = Base $PLATE; $b = $base.bmp
    $bar = $(if ($cooked) { 'C8944A' } else { 'E8D4A0' }); $barD = $(if ($cooked) { 'A07030' } else { 'D0B880' })
    foreach ($s in @(@(3, 6), @(5, 9))) {
        $x = $s[0]; $y = $s[1]
        Rows $b @(@($y, $x, ($x + 7), $bar), @(($y + 1), $x, ($x + 7), $bar), @(($y + 2), $x, ($x + 7), $barD))
        Dots $b @(@(($x + 1), $y, 'C8243C'), @(($x + 4), ($y + 1), '5A3218'), @(($x + 6), $y, 'C8243C'), @(($x + 2), ($y + 1), '5A3218'))
    }
    Save $b $name
}

# ── Night Watch Hotpot (bowl): a dark, rich broth with pork, mushrooms and carrot, a star of anise ──
foreach ($cooked in $true, $false) {
    $name = $(if ($cooked) { 'night_watch_hotpot' } else { 'uncooked_night_watch_hotpot' })
    if (-not (Want $name)) { continue }
    $base = Base $BOWL; $b = $base.bmp
    if ($cooked) { Speckle $base @('4A3A5A', '4A3A5A', '5A4868', '3A2C48') 17 }
    else { Speckle $base @('F8F6F0', 'ECEAE2', 'F8F6F0', 'E0D8CC') 17 }
    $pork = $(if ($cooked) { 'D89A80' } else { 'F0B0A8' })
    Dots $b @(@(4, 7, $pork), @(5, 7, $pork), @(10, 8, $pork), @(11, 8, $pork), @(8, 7, '8C6440'), @(6, 9, '8C6440'), @(12, 7, 'F08A28'), @(8, 9, 'F08A28'))
    if ($cooked) { Dots $b @(@(9, 6, 'F4E8A0'), @(8, 6, 'C8B860'), @(10, 6, 'C8B860')) }
    Save $b $name
}

# Draws the cosmetic hats (client/CosmeticHatLayer, only from the Traveling Merchant): the palette they're built from
# (textures/entity/cosmetic_hats.png, 256x256, BoatKit's zones painted the colours these hats use) and their item icons
# (textures/item/<hat>.png, 16x16): Chef's Hat, Welder's Mask, Wizard's Hat, Farmer's Hat, Traveler's Pipe.
# Run: powershell -File tools\make_cosmetic_hats.ps1
Add-Type -AssemblyName System.Drawing
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 61
function Cl($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function P($b, $x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, (Cl $c[0]), (Cl $c[1]), (Cl $c[2]))) } }
function Zone($b, $u, $v, $w, $h, $c, $noise, $pattern) {
  for ($y = $v; $y -lt $v + $h; $y++) { for ($x = $u; $x -lt $u + $w; $x++) {
    $n = $rng.Next(-$noise, $noise + 1); $k = 0
    if ($pattern -eq 'weave' -and (($x + $y) % 3 -eq 0)) { $k = -34 }
    if ($pattern -eq 'grain' -and ($y % 4 -eq 0)) { $k = -18 }
    if ($pattern -eq 'brushed' -and ($x % 3 -eq 0)) { $k = 10 }
    P $b $x $y @(($c[0] + $n + $k), ($c[1] + $n + $k), ($c[2] + $n + $k)) } }
}
$p = New-Object System.Drawing.Bitmap 256, 256
# (the zones: BoatKit) WHITE 64,84 64x32; CANVAS 0,84 64x32; STEEL 192,116 64x32; TINT 0,52 48x32; BLACK 232,20 24x12;
# LEATHER 128,20 40x24; BRASS 128,44 40x40; NAVY 64,68 32x16; GOLD 224,32 32x12; RED 192,32 32x12; WOOD 128,84 64x32;
# DARKWOOD 192,84 64x32; FIRE 64,60 16x8
Zone $p 64 84 64 32 @(244, 244, 240) 4 ''          # chef's white
Zone $p 0 84 64 32 @(214, 184, 100) 8 'weave'      # straw (and the chef's pleats read as soft shadow lines)
Zone $p 192 116 64 32 @(130, 134, 140) 6 'brushed' # the welder's steel
Zone $p 0 52 48 32 @(30, 60, 40) 4 ''              # the dark green visor
Zone $p 232 20 24 12 @(26, 26, 30) 3 ''
Zone $p 128 20 40 24 @(96, 62, 34) 6 ''            # leather straps
Zone $p 128 44 40 40 @(200, 156, 70) 8 ''          # brass
Zone $p 64 68 32 16 @(34, 44, 120) 8 ''            # the wizard's navy
Zone $p 224 32 32 12 @(240, 196, 70) 8 ''          # gold
Zone $p 192 32 32 12 @(176, 36, 32) 6 ''           # red
Zone $p 128 84 64 32 @(150, 104, 60) 6 'grain'     # pipe stem wood
Zone $p 192 84 64 32 @(84, 54, 30) 6 'grain'       # the bowl
Zone $p 64 60 16 8 @(255, 120, 40) 10 ''           # the ember
$p.Save((Join-Path $tex 'entity\cosmetic_hats.png'), [System.Drawing.Imaging.ImageFormat]::Png); $p.Dispose()

function Icon($name, $rows, $pal) {
  $b = New-Object System.Drawing.Bitmap 16, 16
  for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $ch = [string]$rows[$y][$x]; if ($pal.ContainsKey($ch)) { $c = $pal[$ch]; $n = $rng.Next(-6, 7); P $b $x $y @(($c[0] + $n), ($c[1] + $n), ($c[2] + $n)) } } }
  $b.Save((Join-Path $tex "item\$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
}
Icon 'chefs_hat' @(
 '....wwwwwww.....',
 '...wwwwwwwww....',
 '..wwwwwwwwwww...',
 '..wwwwwwwwwwws..',
 '..wwwwwwwwwwws..',
 '..swwwwwwwwwss..',
 '...sswwwwwss....',
 '...wwwwwwwww....',
 '...wswswswsw....',
 '...wswswswsw....',
 '...wwwwwwwww....',
 '................',
 '................',
 '................',
 '................',
 '................') @{ 'w' = @(246, 246, 242); 's' = @(196, 196, 200) }
Icon 'welders_mask' @(
 '................',
 '..ggggggggggg...',
 '.g22222222222g..',
 '.g22222222222g..',
 '.g2kkkkkkkkk2g..',
 '.g2kvvvvvvvk2g..',
 '.g2kkkkkkkkk2g..',
 '.g22222222222g..',
 '.g22222222222g..',
 '.g22222222222g..',
 '..g222222222g...',
 '...ggggggggg....',
 '................',
 '................',
 '................',
 '................') @{ 'g' = @(80, 84, 90); '2' = @(140, 144, 150); 'k' = @(26, 26, 30); 'v' = @(40, 90, 60) }
Icon 'wizards_hat' @(
 '..........nn....',
 '.........nnn....',
 '........nnnn....',
 '.......nnnnn....',
 '.......nnyn.....',
 '......nnnnn.....',
 '......nnnnnn....',
 '.....nnynnnn....',
 '.....nnnnnnn....',
 '....nnnnnnnnn...',
 '....yyyyyyyyy...',
 '..nnnnnnnnnnnnn.',
 '.nnnnnnnnnnnnnnn',
 '................',
 '................',
 '................') @{ 'n' = @(34, 44, 120); 'y' = @(240, 196, 70) }
Icon 'farmers_hat' @(
 '................',
 '................',
 '..........y.....',
 '.........yy.....',
 '.....sssssy.....',
 '....s3s3s3ss....',
 '....3s3s3s3s....',
 '....rrrrrrrr....',
 '.s3s3s3s3s3s3s..',
 's3s3s3s3s3s3s3s.',
 '.ssssssssssssss.',
 '................',
 '................',
 '................',
 '................',
 '................') @{ 's' = @(214, 184, 100); '3' = @(176, 146, 70); 'r' = @(176, 36, 32); 'y' = @(240, 196, 70) }
Icon 'travelers_pipe' @(
 '................',
 '................',
 '................',
 '................',
 '..........o.....',
 '.........bbb....',
 '.........bbb....',
 '.........bbb....',
 '........wbbb....',
 '.......ww.......',
 '......ww........',
 '.....ww.........',
 '....ww..........',
 '...ww...........',
 '................',
 '................') @{ 'b' = @(84, 54, 30); 'w' = @(150, 104, 60); 'o' = @(255, 120, 40) }
'saved'

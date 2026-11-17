# Draws the Purge Switch and the Blood Moon Switch (event/EventSwitchBlock): a heavy plate, a lever housing, a handle and
# a knob. Purge: black plate with a yellow-and-black hazard border, a red knob, dark steel. Blood Moon: a crimson plate
# with a gold edge, a glowing blood-red crystal knob, blackened iron. textures/block/<purge|blood>_switch_<plate|knob|metal>.png
# Run: powershell -File tools\make_event_switches.ps1
Add-Type -AssemblyName System.Drawing
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\block'
$rng = New-Object System.Random 13
function P($b, $x, $y, $c) { $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, [Math]::Max(0, [Math]::Min(255, $c[0])), [Math]::Max(0, [Math]::Min(255, $c[1])), [Math]::Max(0, [Math]::Min(255, $c[2])))) }
function Img($fill, $noise) { $b = New-Object System.Drawing.Bitmap 16, 16; for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) { $n = $rng.Next(-$noise, $noise + 1); P $b $x $y @(($fill[0] + $n), ($fill[1] + $n), ($fill[2] + $n)) } }; return $b }
function Save($b, $name) { $b.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# Purge
$b = Img @(24, 24, 28) 4
for ($i = 0; $i -lt 16; $i++) { foreach ($e in @(0, 1, 14, 15)) { $c = if ((($i + $e) % 4) -lt 2) { @(230, 190, 30) } else { @(20, 20, 20) }; P $b $i $e $c; P $b $e $i $c } }
P $b 7 7 @(200, 30, 30); P $b 8 7 @(200, 30, 30); P $b 7 8 @(200, 30, 30); P $b 8 8 @(200, 30, 30)
Save $b 'purge_switch_plate'
$b = Img @(200, 24, 24) 10; for ($i = 3; $i -lt 7; $i++) { P $b $i 3 @(255, 120, 110) }; Save $b 'purge_switch_knob'
$b = Img @(70, 72, 78) 8; for ($y = 0; $y -lt 16; $y += 3) { for ($x = 0; $x -lt 16; $x++) { P $b $x $y @(50, 52, 58) } }; Save $b 'purge_switch_metal'
# Blood Moon
$b = Img @(110, 14, 24) 8
for ($i = 0; $i -lt 16; $i++) { foreach ($e in @(0, 15)) { P $b $i $e @(214, 168, 64); P $b $e $i @(214, 168, 64) } }
for ($k = 0; $k -lt 10; $k++) { P $b $rng.Next(2, 14) $rng.Next(2, 14) @(70, 6, 14) }
Save $b 'blood_switch_plate'
$b = Img @(220, 30, 40) 14; for ($i = 2; $i -lt 8; $i++) { P $b $i $i @(255, 150, 150) }; Save $b 'blood_switch_knob'
$b = Img @(36, 30, 32) 6; Save $b 'blood_switch_metal'
'saved'

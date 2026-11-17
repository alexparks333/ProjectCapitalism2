# Builds the job NPC skins (src/main/resources/assets/capitalism/textures/entity/npc/<job>.png): a vanilla villager in a
# vanilla job outfit, plus a recolored farmer straw hat for some (a white chef's hat, a dark leather smith's hat).
# Needs the vanilla villager textures unzipped first, e.g. (Git Bash, from the project folder):
#   cd /tmp/vr && unzip -o .../.gradle/caches/minecraft/versions/26.3/client.jar \
#     'assets/minecraft/textures/entity/villager/villager.png' 'assets/minecraft/textures/entity/villager/type/plains.png' \
#     'assets/minecraft/textures/entity/villager/profession/*.png'
Add-Type -AssemblyName System.Drawing
$v = Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\entity\villager'
$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\entity\npc'
New-Item -ItemType Directory -Force $out | Out-Null

function Load($n){ [Drawing.Bitmap]::FromFile("$v\$n.png") }
# draw src over dst (alpha blended)
function Over($dst,$src){ for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){ $c=$src.GetPixel($x,$y); if($c.A -gt 0){ if($c.A -eq 255){ $dst.SetPixel($x,$y,$c) } else { $d=$dst.GetPixel($x,$y); $a=$c.A/255.0; $dst.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,[int]($d.R+($c.R-$d.R)*$a),[int]($d.G+($c.G-$d.G)*$a),[int]($d.B+($c.B-$d.B)*$a))) } } }} }

# the farmer's straw hat lives in the villager model's hat box and brim regions; recolor it by brightness
$farmer = Load 'profession\farmer'
function InHat($x,$y){ ($y -le 19 -and $x -ge 30) -or ($y -ge 45 -and $x -ge 28 -and $x -le 50) }
function Hat($dst,$lo,$hi){ for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){ if(-not (InHat $x $y)){continue}; $c=$farmer.GetPixel($x,$y); if($c.A -eq 0){continue}
  $l=(0.3*$c.R+0.59*$c.G+0.11*$c.B)/255.0
  $dst.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,[int]($lo[0]+($hi[0]-$lo[0])*$l),[int]($lo[1]+($hi[1]-$lo[1])*$l),[int]($lo[2]+($hi[2]-$lo[2])*$l))) }} }

# job id, vanilla outfit, extra hat
$jobs = @(
  @('cook','profession\butcher','chef'),
  @('bartender','profession\librarian',''),
  @('weaponsmith','profession\weaponsmith','smith'),
  @('toolsmith','profession\toolsmith',''),
  @('armorsmith','profession\armorer',''),
  @('enchanter','profession\cleric',''),
  # the Boat Master (npc/BoatMaster; not a job): the fisherman's outfit and a navy captain's hat
  @('boat_master','profession\fisherman','captain'))
foreach($j in $jobs){
  $bmp = New-Object Drawing.Bitmap 64,64,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $base = Load 'villager'; $type = Load 'type\plains'; $prof = Load $j[1]
  Over $bmp $base; Over $bmp $type; Over $bmp $prof
  if($j[2] -eq 'chef'){ Hat $bmp @(150,150,155) @(255,255,255) }
  if($j[2] -eq 'smith'){ Hat $bmp @(28,20,16) @(110,80,55) }
  if($j[2] -eq 'captain'){ Hat $bmp @(12,18,44) @(64,86,150) }
  $bmp.Save("$out\$($j[0]).png",[Drawing.Imaging.ImageFormat]::Png)
  $base.Dispose(); $type.Dispose(); $prof.Dispose(); $bmp.Dispose()
}
$farmer.Dispose()
'saved'

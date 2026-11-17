# Draws the Ench-Removal (textures/item/ench_removal.png + .mcmeta): the vanilla Book, scorched darker and redder, with
# flames licking up from it, as a 4-frame flickering animation (16x64).
# Needs vanilla item/book.png unzipped first, e.g. (Git Bash, from the project folder):
#   unzip -o -d "$LOCALAPPDATA/Temp/vr" .gradle/caches/minecraft/versions/26.3/client.jar 'assets/minecraft/textures/item/book.png'
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item'
$book = [Drawing.Bitmap]::FromFile((Join-Path $env:LOCALAPPDATA 'Temp\vr\assets\minecraft\textures\item\book.png'))
$bmp = New-Object Drawing.Bitmap 16,64,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
function C([int]$v){ [Math]::Max(0,[Math]::Min(255,$v)) }
function Px([int]$x,[int]$y,[int]$r,[int]$g,[int]$b){ if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 64){ $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,(C $r),(C $g),(C $b))) } }

# flame heights per column (0 = none), one row per frame: the fire flickers
$flames = @(
  @(0,0,2,4,3,5,7,5,6,8,6,4,5,3,1,0),
  @(0,1,3,3,5,4,6,8,5,7,7,5,3,4,2,0),
  @(0,0,3,5,4,6,5,7,8,6,5,6,4,2,2,0),
  @(0,1,2,4,5,7,6,6,7,8,6,4,4,3,1,0))
for($f=0;$f -lt 4;$f++){ $oy = $f * 16
  # the book, scorched: darker, warmer
  for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){ $c = $book.GetPixel($x,$y)
    if($c.A -gt 0){ Px $x ($oy+$y) ([int]($c.R*0.85+20)) ([int]($c.G*0.55)) ([int]($c.B*0.45)) } } }
  # embers on the cover
  $rng = New-Object System.Random (100 + $f)
  for($i=0;$i -lt 5;$i++){ $x=$rng.Next(3,13); $y=$rng.Next(5,14); if($book.GetPixel($x,$y).A -gt 0){ Px $x ($oy+$y) 255 150 40 } }
  # flames rising from the top edge of the book: white-yellow core, orange, red tips
  for($x=0;$x -lt 16;$x++){ $h = $flames[$f][$x]; if($h -eq 0){ continue }
    # where the book's top is in this column
    $top = 16; for($y=0;$y -lt 16;$y++){ if($book.GetPixel($x,$y).A -gt 0){ $top = $y; break } }
    if($top -ge 16){ $top = 8 }
    for($k=0;$k -lt $h;$k++){ $y = $top + 2 - $k; if($y -lt 0){ break }
      $t = $k / [Math]::Max(1,$h-1)
      if($t -lt 0.35){ Px $x ($oy+$y) 255 236 140 } elseif($t -lt 0.7){ Px $x ($oy+$y) 255 150 30 } else { Px $x ($oy+$y) 214 60 20 } } } }
$bmp.Save((Join-Path $dir 'ench_removal.png'),[Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); $book.Dispose()
Set-Content -Path (Join-Path $dir 'ench_removal.png.mcmeta') -Value '{ "animation": { "frametime": 3 } }' -Encoding ascii
'saved'

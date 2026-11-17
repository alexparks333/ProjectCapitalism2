# Draws the level numerals laid over a lesson book's cover in the bottom-right corner (items/skill_book.json, layer1 of
# models/item/skill_books/<lesson>_i / _ii): item/skill_books/numeral_i.png and numeral_ii.png, 16x16, transparent
# except the numeral (pale gold with serifs, outlined in near-black so it reads on any cover).
Add-Type -AssemblyName System.Drawing
$dir = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures\item\skill_books'
function Numeral([int[]]$stems,[int]$x0,[int]$x1,[string]$name){
  $bmp = New-Object Drawing.Bitmap 16,16,([Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $on = @{}
  for($x=$x0;$x -le $x1;$x++){ $on["$x,10"] = 1; $on["$x,14"] = 1 }
  foreach($s in $stems){ for($y=11;$y -le 13;$y++){ $on["$s,$y"] = 1 } }
  $ink = [Drawing.Color]::FromArgb(255,28,20,14)
  foreach($k in $on.Keys){ $p = $k.Split(','); $x = [int]$p[0]; $y = [int]$p[1]
    for($dy=-1;$dy -le 1;$dy++){ for($dx=-1;$dx -le 1;$dx++){ $bmp.SetPixel($x+$dx,$y+$dy,$ink) } } }
  foreach($k in $on.Keys){ $p = $k.Split(','); $x = [int]$p[0]; $y = [int]$p[1]
    $g = if($y -le 11){ 255 } else { 226 }
    $bmp.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,255,$g,(C2 $y))) }
  $bmp.Save((Join-Path $dir "$name.png"),[Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
}
# a little shading: brighter at the top of the numeral
function C2([int]$y){ if($y -le 11){ 170 } else { 110 } }
Numeral @(13) 12 14 'numeral_i'
Numeral @(11,13) 10 14 'numeral_ii'

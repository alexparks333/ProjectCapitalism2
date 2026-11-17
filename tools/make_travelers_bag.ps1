# Draws the Traveler's Bag (designer/Bag.TRAVELERS_BAG, only from the Traveling Merchant): the Backpack's textures
# re-dyed a weathered road-brown with patches, and a little gold star stitched on (it's magic: it opens out far from
# town). Item: textures/item/travelers_bag.png; worn on the back: textures/entity/bag_travelers_bag.png.
# Run: powershell -File tools\make_travelers_bag.ps1
Add-Type -AssemblyName System.Drawing
$tex = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$rng = New-Object System.Random 19
function Redye($src, $dst, $patches) {
  $b = [System.Drawing.Bitmap]::new($src); $o = New-Object System.Drawing.Bitmap $b.Width, $b.Height
  for ($y = 0; $y -lt $b.Height; $y++) { for ($x = 0; $x -lt $b.Width; $x++) {
    $c = $b.GetPixel($x, $y)
    if ($c.A -eq 0) { $o.SetPixel($x, $y, $c); continue }
    $l = ($c.R * 0.3 + $c.G * 0.59 + $c.B * 0.11) / 255.0
    $o.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, [int](40 + 150 * $l), [int](26 + 100 * $l), [int](14 + 60 * $l))) } }
  $b.Dispose()
  # patches of other cloth, and the gold star
  for ($k = 0; $k -lt $patches; $k++) {
    $px = $rng.Next(1, $o.Width - 3); $py = $rng.Next(1, $o.Height - 3)
    for ($j = 0; $j -lt 2; $j++) { for ($i = 0; $i -lt 2; $i++) { if ($o.GetPixel($px + $i, $py + $j).A -gt 0) { $o.SetPixel($px + $i, $py + $j, [System.Drawing.Color]::FromArgb(255, 96, 104, 70)) } } }
  }
  $cx = [int]($o.Width / 2); $cy = [int]($o.Height / 2)
  foreach ($p in @(@(0, 0), @(1, 0), @(-1, 0), @(0, 1), @(0, -1))) { $x = $cx + $p[0]; $y = $cy + $p[1]; if ($o.GetPixel($x, $y).A -gt 0) { $o.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 214, 90)) } }
  $o.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png); $o.Dispose()
}
Redye (Join-Path $tex 'item\backpack.png') (Join-Path $tex 'item\travelers_bag.png') 2
Redye (Join-Path $tex 'entity\bag_backpack.png') (Join-Path $tex 'entity\bag_travelers_bag.png') 6
'saved'

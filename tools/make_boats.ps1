# Builds the Premium boats' textures (Boats.md):
#  - tools/BoatTex.cs: the Speedboat, Yacht, Longship, Paddlewheeler, Swamp Raft and the rest (client/BoatKit's layout)
#  - tools/PirateShipTex.cs: the Pirate Ship and the Spectral Galleon (client/PirateShipModel's layout)
# Each: textures/entity/<boat>.png (1024 x 1024), <boat>_glow.png (only what glows), textures/item/<boat>.png (32 x 32).
Add-Type -AssemblyName System.Drawing
# (BoatTex is in two files: tools/BloodDecorTex.cs paints the Blood Moon decorations)
Add-Type -Path @((Join-Path $PSScriptRoot 'BoatTex.cs'), (Join-Path $PSScriptRoot 'BloodDecorTex.cs')) -ReferencedAssemblies System.Drawing
Add-Type -Path (Join-Path $PSScriptRoot 'PirateShipTex.cs') -ReferencedAssemblies System.Drawing
$res = Join-Path $PSScriptRoot '..\src\main\resources\assets\capitalism\textures'
$entity = (Resolve-Path (Join-Path $res 'entity')).Path
$item = (Resolve-Path (Join-Path $res 'item')).Path
[BoatTex]::MakeAll($entity, $item)
[PirateShipTex]::Make($entity, $item)
[PirateShipTex]::MakeGhost($entity, $item)

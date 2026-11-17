# Meat Slabs as placeable blocks (Farmer.md > Butchering). For each slab: a sculpted block model (tools/MeatSculpt.cs:
# the cut's real shape, in half- or quarter-pixel boxes of meat, lean, fat, skin, bone and marrow), the 64x64 material
# textures it uses (tools/MeatTex.cs), the blockstate (4 facings), the item json (the item shows the 3D block), the loot
# table (a fresh slab keeps its timer and lean, farmer/Freshness; a rotten one gives Rotten Meat), the rotten look
# (a child model with the textures gone grey-green and mouldy, MeatTex.Rot), and farmer/MeatSlabShapes.java (each slab's outline box, read by MeatSlabBlock). Run from the project root:
#   powershell -ExecutionPolicy Bypass -File tools/make_meat_slab_blocks.ps1
Add-Type -AssemblyName System.Drawing
Add-Type -Path (Join-Path $PSScriptRoot 'MeatTex.cs') -ReferencedAssemblies System.Drawing
Add-Type -Path (Join-Path $PSScriptRoot 'MeatSculpt.cs') -ReferencedAssemblies System.Drawing
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources'
$assets = Join-Path $root 'assets\capitalism'
$texDir = Join-Path $assets 'textures\block\meat_slab'
New-Item -ItemType Directory -Force $texDir | Out-Null
$utf8 = New-Object Text.UTF8Encoding $false

# palettes: M meat, D dark meat, L light (lean), F fat, S skin
$pal = @{
  'beef'    = @{ M=@(170,34,34);  D=@(104,14,20);  L=@(212,78,70);  F=@(244,234,208); S=@(244,234,208) }
  'pork'    = @{ M=@(226,128,120); D=@(180,82,80); L=@(246,174,162); F=@(252,244,230); S=@(240,196,166) }
  'mutton'  = @{ M=@(158,46,42);  D=@(98,24,26);   L=@(198,88,76);  F=@(242,230,200); S=@(242,230,200) }
  'chicken' = @{ M=@(244,192,170); D=@(222,150,132); L=@(254,220,204); F=@(250,228,168); S=@(246,218,160) }
  'rabbit'  = @{ M=@(198,108,98); D=@(146,68,64);  L=@(224,148,136); F=@(242,230,208); S=@(228,218,224) }
}

# the slabs: id, animal, skin kind, marbling (0..1), voxel size (16ths: 0.5, or 0.25 for small and fine cuts)
$cuts = @(
  @{ id='beef_chuck';          a='beef';    top='fatcap';      marble=0.7;  res=0.5; yield=3 }
  @{ id='beef_brisket';        a='beef';    top='fatcap';      marble=0.4;  res=0.5; yield=3 }
  @{ id='beef_round';          a='beef';    top='fatcap';      marble=0.25; res=0.5; yield=4 }
  @{ id='beef_rib';            a='beef';    top='fatcap';      marble=0.9;  res=0.5; yield=5 }
  @{ id='pork_shoulder';       a='pork';    top='pigskin';     marble=0.3;  res=0.5; yield=3 }
  @{ id='pork_belly';          a='pork';    top='pigskin';     marble=0;    res=0.5; yield=3 }
  @{ id='pork_ham';            a='pork';    top='pigskin';     marble=0.2;  res=0.5; yield=4 }
  @{ id='pork_loin';           a='pork';    top='fatcap';      marble=0.1;  res=0.5; yield=5 }
  @{ id='mutton_shoulder';     a='mutton';  top='fatcap';      marble=0.45; res=0.5; yield=2 }
  @{ id='mutton_leg';          a='mutton';  top='fatcap';      marble=0.25; res=0.5; yield=3 }
  @{ id='mutton_rack';         a='mutton';  top='fatcap';      marble=0.45; res=0.5; yield=4 }
  @{ id='chicken_leg_quarter'; a='chicken'; top='chickenskin'; marble=0;    res=0.25; yield=2 }
  @{ id='chicken_breast';      a='chicken'; top='chickenskin'; marble=0;    res=0.25; yield=3 }
  @{ id='rabbit_hind_legs';    a='rabbit';  top='silver';      marble=0;    res=0.25; yield=2 }
  @{ id='rabbit_saddle';       a='rabbit';  top='silver';      marble=0;    res=0.25; yield=3 }
)

function Arr($a){ [double[]]@([double]$a[0],[double]$a[1],[double]$a[2]) }
function WriteJson($path,$obj){ New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null; [IO.File]::WriteAllText($path, ($obj | ConvertTo-Json -Depth 12), $utf8) }
function F($v){ ([double]$v).ToString([Globalization.CultureInfo]::InvariantCulture) }

$seedSpec = New-Object MeatCutSpec
$seedSpec.M = Arr $pal.beef.M; $seedSpec.D = Arr $pal.beef.D; $seedSpec.L = Arr $pal.beef.L; $seedSpec.F = Arr $pal.beef.F; $seedSpec.S = Arr $pal.beef.S; $seedSpec.Seed = 4242
function SaveBoth($bmp, $name, $seed){
  $bmp.Save((Join-Path $texDir "$name.png"), [Drawing.Imaging.ImageFormat]::Png)
  ([MeatTex]::Rot($bmp, $seed)).Save((Join-Path $texDir "$($name)_rotten.png"), [Drawing.Imaging.ImageFormat]::Png)
}
SaveBoth ([MeatTex]::Bone(77)) 'bone' 1
SaveBoth ([MeatTex]::Material($seedSpec, 'marrow')) 'marrow' 2

$java = New-Object Text.StringBuilder
# a sheet of quick 3D looks at the shapes, to check them by eye
$sheet = New-Object Drawing.Bitmap (5*200), (3*200)
$gs = [Drawing.Graphics]::FromImage($sheet)
$i = 0
foreach($cut in $cuts){
  $p = $pal[$cut.a]
  $id = $cut.id
  $spec = New-Object MeatCutSpec
  $spec.M = Arr $p.M; $spec.D = Arr $p.D; $spec.L = Arr $p.L; $spec.F = Arr $p.F; $spec.S = Arr $p.S
  $spec.Top = $cut.top; $spec.Marble = [double]$cut.marble; $spec.Small = ($cut.a -eq 'chicken' -or $cut.a -eq 'rabbit'); $spec.Seed = ($i+1)*7919

  $r = [MeatSculpt]::Build($id, [double]$cut.res, "capitalism:block/meat_slab/$($id)_")
  [IO.File]::WriteAllText((Join-Path $assets "models\block\$($id)_slab.json"), $r.Json, $utf8)
  $rottenTex = [ordered]@{ particle="capitalism:block/meat_slab/$($id)_meat_rotten" }
  foreach($m in 'meat','lean','fat','skin','bone','marrow'){
    $used = $r.Used[[Array]::IndexOf([MeatSculpt]::Names, $m)]
    if($m -eq 'meat' -or $used){
      if($m -ne 'bone' -and $m -ne 'marrow'){ SaveBoth ([MeatTex]::Material($spec, $m)) "$($id)_$m" ($i*10+3) }
      $rottenTex[$m] = $(if($m -eq 'bone' -or $m -eq 'marrow'){ "capitalism:block/meat_slab/$($m)_rotten" } else { "capitalism:block/meat_slab/$($id)_$($m)_rotten" })
    }
  }
  # the rotten look: the same model with the rotten textures
  WriteJson (Join-Path $assets "models\block\$($id)_slab_rotten.json") ([ordered]@{ parent="capitalism:block/$($id)_slab"; textures=$rottenTex })
  $b = $r.Bounds
  [void]$java.AppendLine("            case `"$id`" -> new double[]{$(F $b[0]), $(F $b[1]), $(F $b[2]), $(F $b[3]), $(F $b[4]), $(F $b[5])};")
  "{0,-22} {1,4} boxes" -f $id, $r.Boxes
  $colors = [double[][]]@((Arr @(0,0,0)), (Arr $p.M), (Arr $p.L), (Arr $p.F), (Arr $(if($cut.top -eq 'silver'){@(214,190,196)}else{$p.S})), (Arr @(236,228,206)), (Arr @(168,58,52)))
  $gs.DrawImage([MeatSculpt]::Preview($id, [double]$cut.res, $colors, 200), ($i % 5)*200, [Math]::Floor($i/5)*200)

  $variants = [ordered]@{}
  $rot = @{ north=0; east=90; south=180; west=270 }
  foreach($f in 'north','east','south','west'){
    foreach($rotten in 'false','true'){
      $v = [ordered]@{ model="capitalism:block/$($id)_slab$(if($rotten -eq 'true'){'_rotten'})" }; if($rot[$f] -ne 0){ $v.y = $rot[$f] }
      $variants["facing=$f,rotten=$rotten"] = $v
    }
  }
  WriteJson (Join-Path $assets "blockstates\$($id)_slab.json") ([ordered]@{ variants=$variants })
  WriteJson (Join-Path $assets "items\$($id)_slab.json") ([ordered]@{ model=[ordered]@{ type='minecraft:model'; model="capitalism:block/$($id)_slab" } })
  # rotten: Rotten Meat (as much as the cuts would have been); fresh: the slab, keeping its timer and lean
  $loot = [ordered]@{
    type='minecraft:block'
    pools=@([ordered]@{
      condition=[ordered]@{ type='minecraft:survives_explosion' }
      entries=@([ordered]@{ type='minecraft:alternatives'; children=@(
        [ordered]@{ type='minecraft:item'; name='capitalism:rotten_meat'
          condition=[ordered]@{ type='minecraft:match_block'; blocks="capitalism:$($id)_slab"; state=[ordered]@{ rotten='true' } }
          modifier=[ordered]@{ type='minecraft:set_count'; count=$cut.yield } },
        [ordered]@{ type='minecraft:item'; name="capitalism:$($id)_slab"
          modifier=[ordered]@{ type='minecraft:copy_components'; source='block_entity'; include=@('capitalism:rots_at','capitalism:lean') } }
      ) })
      rolls=1
    })
    random_sequence="capitalism:blocks/$($id)_slab"
  }
  WriteJson (Join-Path $root "data\capitalism\loot_table\blocks\$($id)_slab.json") $loot
  $i++
}

$src = @"
package com.alexh.capitalism.farmer;

// GENERATED by tools/make_meat_slab_blocks.ps1 (don't edit by hand): each Meat Slab's outline box, facing north,
// {x0, y0, z0, x1, y1, z1} in 16ths, from its sculpted model (tools/MeatSculpt.cs). Read by MeatSlabBlock.
final class MeatSlabShapes {

    private MeatSlabShapes() {
    }

    static double[] bounds(String id) {
        return switch (id) {
$($java.ToString().TrimEnd())
            default -> new double[]{2, 0, 2, 14, 8, 14};
        };
    }
}
"@
[IO.File]::WriteAllText((Join-Path $PSScriptRoot '..\src\main\java\com\alexh\capitalism\farmer\MeatSlabShapes.java'), $src.Replace("`r`n","`n"), $utf8)
$sheet.Save((Join-Path $env:TEMP 'meat_sculpt_preview.png'), [Drawing.Imaging.ImageFormat]::Png)
"made $($cuts.Count) meat slab blocks"

# Farmer (Fun job)

Only a Farmer gets the full harvest from crops; everyone else gets just enough to keep a field going. Farmers also
rot Fertilizers down in the **Decomposter**, fertilize farmland for bigger harvests or faster growth, make **Scythes** (iron and diamond),
and get **Meat Slabs** from animals to cut up at the **Butcher's Table**; the meat rots (**Freshness**), so Farmers
and Cooks keep it in **Fridges** and **Freezers**. (Cooks sear the cuts on the **Grill**: `Cook.md`.)
Code: `farmer/`:
- harvests and fertilizer: `FarmHarvestModifier`, `FertilizedSoil`, `FertilizedFarmlandBlock`, `FertilizerItem`,
  `FarmerEvents`, `ScytheItem`, `DecomposterBlock`(+`Entity`, `Menu`);
- butchering: `MeatCut`, `MeatItems` (the slab and cut items), `MeatSlabBlock`(+`Entity`, `MeatSlabShapes`, generated),
  `ButcherKnifeItem`, `ButchersTableBlock`(+`Entity`, `Menu`);
- freshness: `Freshness`, `FreshnessEvents`, `Chill`, `ColdStorageBlock`(+`Entity`, `Menu`);
- the Cook's side: `GrillBlock`(+`Entity`, `Menu`), `Sear`.
Client: `DecomposterScreen`, `ButchersTableScreen`, `ButcheryGame`, `ColdStorageScreen`, `MeatSlabHud`, `GrillScreen`,
`SearGame`, `FarmersGuideScreen`. Art: `tools/make_farmer_textures.ps1` (Decomposter, fertilizers, farmland),
`tools/make_butcher_textures.ps1` (the table, the raw cuts), `tools/make_meat_slab_blocks.ps1` + `MeatSculpt.cs` +
`MeatTex.cs` (the slab blocks), `tools/make_butcher_knives.ps1`, `tools/make_cold_storage_textures.ps1` (Fridge,
Freezer, Rotten Meat), `tools/make_grill_textures.ps1` (the Grill, the seared cuts). Tests: `farmer/FarmerTests`.

## Recipes (Farmer-only, `capitalism:job_shaped`)

**No Hay Bale → Wheat:** the vanilla recipe (9 wheat from a Hay Bale) is turned off for everyone
(`data/minecraft/recipe/wheat.json` and the advancement that unlocks it, `advancement/recipes/misc/wheat.json`, both
turned off with a `neoforge:never` condition; recipes are a registry in 26.3, so both must go), so village hay isn't free food. Wheat → Hay Bale
still works, it just can't be undone.

| Item | Recipe |
|---|---|
| **Decomposter** | Planks, Iron Ingot, Planks / Planks, Composter, Planks / 3 Planks |
| **Scythe** | 3 Iron Ingots across the top, 2 Sticks down the right (400 uses) |
| **Diamond Scythe** | 3 Diamonds across the top, 2 Sticks down the right (1600 uses) |
| **Yield Fertilizer** x2 | Bone Meal, Rotten Flesh, Dirt in the Decomposter |
| **Super Yield Fertilizer** x1 | Bone Meal, Redstone, Redstone in the Decomposter |
| **Fast Fertilizer** x2 | Bone Meal, Sugar, Dirt in the Decomposter |
| **Butcher's Table** | Iron Ingot top left / 3 Wooden Slabs / Log, gap, Log |
| **Iron / Diamond Butcher Knife** | Farmers and Toolsmiths make them: _ I I / _ I _ / Stick _ _ (Diamond: 2 Diamonds on top) |
| **Fridge** | Farmers and Cooks: 3 Iron Ingots / Iron, Chest, Iron / Iron, Redstone, Iron |
| **Freezer** | Farmers and Cooks: 3 Iron Ingots / Iron, Chest, Iron / Iron, Packed Ice, Iron |

## Harvests (`FarmHarvestModifier`, a global loot modifier: `data/capitalism/loot_modifiers/farm_harvest.json`)
A **ripe** crop broken (or a bush picked) by:
- **a Farmer:** the full vanilla harvest (Fortune still works).
- **anyone else, or nothing at all** (water, pistons, villagers, explosions): **one crop and one seed** back, so
  replanting leaves one crop of profit. A crop that is its own seed gives **two** of it (carrots, potatoes, nether
  wart, cocoa beans, breaking a ripe sweet berry bush). A melon gives **one slice**; a pumpkin is still one pumpkin;
  picking a sweet berry bush gives **one berry**.
- Covered: wheat, beetroot, carrots, potatoes, rice, nether wart, cocoa, sweet berries, melons, pumpkins.
- **Unripe** crops are untouched (they give their seed back as usual).

## Animal meat (`FarmerEvents.onMeatDrops`)
An animal killed by a **Farmer** drops its full vanilla meat. Killed by **anyone else** (or by lava, fire, a fall...): **1 piece of meat**. Other drops (leather, feathers, wool...) are untouched. Butchery Basics (`LibrarianBooks.md`) can still add one more.

## Fertilizer (`FertilizerItem`, `FertilizedSoil`)
Three kinds (item ids `fertilizer`, `super_fertilizer`, `fast_fertilizer`; `FertilizerItem.Kind`):
- **Only Farmers** can use them (others: "Only a Farmer can use Fertilizer.").
- Right-click **farmland** (or a crop growing on it). Green sparkles (Fast: wax sparkles), the bone meal sound.
- **Yield Fertilizer:** fertilized for **2 Farmer harvests** (`FertilizerItem.HARVESTS`); **Super Yield**: **3**
  (`SUPER_HARVESTS`). Each **Farmer** harvest of a ripe crop on it gives **50% to 90% more** of the crop (**Super
  Yield: 60% to 115%**, `SUPER_BONUS_MIN`/`SUPER_BONUS_MAX`) (`BONUS_MIN`/`BONUS_MAX`; not of a separate seed; a part crop is a chance of one more) and uses one harvest
  ("Fertilized: 1 harvest left"). Other people's harvests don't use it up (and get the small harvest anyway).
- **Fast Fertilizer:** crops on it grow **1.75x as fast** (`FAST_SPEED`) for **2 harvests** (`FAST_HARVESTS`): each
  time a crop on it grows a stage (NeoForge `CropGrowEvent.Post`, `FarmerEvents`), 75% of the time it grows one more
  (`FertilizerItem.grow`; wheat, carrots, potatoes, beetroot, melon / pumpkin stems, rice). A seed that hasn't sprouted
  yet also gets a small extra chance (`SPROUT_CHANCE`, 10%) to sprout on each growth check (`CropGrowEvent.Pre`,
  `FertilizerItem.sprout`), so seeds on it don't sit bare for long. **Any** harvest of a ripe
  crop on it uses one up, whoever (or whatever) harvests it, since the speed-up already happened.
- **One at a time, no stacking:** a piece of farmland holds **one fertilizer of any kind**. Until it's used up nothing
  else goes on it, not even the same kind to top it up ("Already fertilized: 2 harvests left", "Already has Fast
  Fertilizer: 1 harvest left").
- Melons and pumpkins: fertilize the farmland **under the stem**.
- **It shows:** fertilized farmland turns into **Fertilized Farmland** (`FertilizedFarmlandBlock`), farmland in every
  way (gets wet, dries, gets trampled, holds crops: it's in the same block tags) but its top is darker and speckled with
  what's on it (`FERTILIZER`: green for Yield, silver for Super Yield, yellow and blue for Fast). Back to plain farmland (still wet) once it's all used up.
- **Trampled (or dried out / covered) to dirt, its fertilizer is lost:** hoeing it again gives plain farmland.
- Saved per chunk (`ModAttachments.FERTILIZED_SOIL`: Yield harvests left, which spots are Super, Fast harvests left); forgotten once used up or once the spot isn't farmland.

## Decomposter (`DecomposterBlockEntity`)
A hollow slatted wooden bin. It stands **empty** until ingredients go in, then fills with dark compost (`FULL`:
any ingredient in it); while it works the compost **bubbles** (an animated texture), green specks and bubbles rise
out of it with a soft popping (`DecomposterBlock.animateTick`), and every 2 seconds it gives the composter sound.
Like a furnace without fuel: **three ingredient slots** (any order) and an output
slot. A recipe takes **10 seconds** (`TICKS`), then uses one of each ingredient; it keeps going while they last and the
output has room. A **Recipes** panel on the menu's right shows all three recipes (ingredients, arrow, result; hover for names). Only Farmers can make or open it. Breaking it drops what's in it.

## Scythes (`ScytheItem`)
Break a **ripe** crop with one and every ripe crop in a patch comes down too: the **Scythe** cuts **5 wide and 3
deep** (the crop you hit and the two beyond it (the way you face), and two either side of each); the **Diamond
Scythe** cuts the same patch and lasts 1600 uses (400 for the iron one). Unripe crops are left standing. The harvest is
the same as by hand (full for a Farmer). One durability per swing. Only Farmers can make one; **anyone can use it**.

## Butchering (`MeatCut`, `ButchersTableMenu`, `client/ButcheryGame`)
**Meat Slabs:** when a **Farmer** kills a **grown** cow, mooshroom, pig, hoglin, sheep, chicken or rabbit with a
**diamond or netherite sword or axe** (item tag `#capitalism:butchering_blades`), and only then, there's a **15%**
chance (`SLAB_CHANCE`) it drops a Meat Slab **as well as** its usual drops. A **Bone** set in the sword's Molten
Socket (`Sockets.md`) adds **+15%** (`BONE_CHANCE`: **30%**). A second roll picks which slab, by weight (goats,
camels and horses drop no meat, so no slabs). **Luck** (Angler's Almanac, Lucky Star, a Luck potion: the Luck
attribute) adds **+5% chance per point** (`LUCK_CHANCE`, up to 60%) and makes the rarer slabs more likely (their
weight **+25% per point per step of rarity**, `LUCK_RARE`). Butchery Basics doesn't change slabs (it adds plain meat).

| Animal | Slab (weight, yield) | Fine cut |
|---|---|---|
| Cow / Mooshroom (beef) | Chuck 35 (3), Brisket 35 (3), Round 20 (4), Rib 10 (5) | Raw Beef Chuck / Brisket / Round / Rib |
| Pig / Hoglin (porkchop) | Shoulder 35 (3), Belly 35 (3), Ham 20 (4), Loin 10 (5) | Raw Pork Shoulder / Belly / Ham / Loin |
| Sheep (mutton) | Shoulder 50 (2), Leg 35 (3), Rack 15 (4) | Raw Mutton Shoulder / Leg / Rack |
| Chicken | Leg Quarter 65 (2), Breast 35 (3) | Raw Chicken Leg Quarter / Breast |
| Rabbit | Hind Legs 65 (2), Saddle 35 (3) | Raw Rabbit Hind Legs / Saddle |

**Meat Slabs are blocks** (`MeatSlabBlock`): set one down and it's that chunk of the animal, sculpted in 3D: a marbled
chuck with its blade bone, a fat-capped brisket, a round roast with the bone through it, a standing rib roast with its
frenched ribs curving up, a pork belly in layers, a ham and a leg of lamb narrowing to the knuckle, a rolled loin with
its chine bones, a lamb rack, a chicken leg quarter (thigh and drumstick), a double chicken breast, rabbit hind legs and
twin loins. `tools/make_meat_slab_blocks.ps1` builds them: `tools/MeatSculpt.cs` defines each cut's shape (signed
distance functions) and fills it with half- or quarter-pixel voxels of meat, lean, fat, skin, bone and marrow, merged into
boxes (about 90-480 per model); `tools/MeatTex.cs` draws the 64x64 material textures, mapped by position so they flow
across the boxes. It also writes `MeatSlabShapes.java` (each slab's outline box). The item shows the 3D model too.

**Butcher Knives** (`ButcherKnifeItem`, `tools/make_butcher_knives.ps1`; Iron is a curved butcher knife with a wooden handle, Diamond a square cleaver with a black and gold handle): the fuel for the table. The knife goes in
the table's knife slot (top left of the board) and **stays there** between uses (`ButchersTableBlockEntity`; breaking
the table drops it); every slab cut up uses **one cut** of its sharpness (`KNIFE_CUTS`
component): **Iron 4, Diamond 10**. It's sharpness, not durability: the bar under it shows what's left, and a
**Tool Whetstone** (the Toolsmith's, `WhetstoneBlock` kind PICKAXE) sharpens it back to full. Use the last cut
and the knife is spent: it breaks on that slab. Farmers and Toolsmiths can both make them.

**The Butcher's Table** (only Farmers): put a slab in, and a Butcher Knife with sharpness left, and press **Cut
Slab**. The slab and one cut of the knife are used up right away and the **butchery game** starts. Done cleanly it gives **yield x the fine cut** (Raw Beef Rib...: eaten like the plain
raw meat, Cooks won't eat them raw, they count for Training Feed; Cooks' recipes for them are coming). Botched (or
given up with Esc, or the menu closed) it gives **yield x plain raw meat** (beef, porkchop...). The yield is less for
a fatty slab (see Freshness: lean).

## Freshness (`Freshness`, `FreshnessEvents`, `ColdStorageBlockEntity`, `MeatSlabBlockEntity`)
Meat Slabs and the fine cuts **rot**. Plain (botched) meat doesn't.
- **A timer on every piece:** the game tick it rots at (`ModComponents.ROTS_AT`; game time, so it stops while the
  server is off). The tooltip shows "Fresh for 12:34" (yellow under 5 minutes, red under 2).
- **Shelf life** of a slab when it drops (`shelfLife`): common slabs **15 min**, uncommon **20**, rare (Beef Rib, Pork
  Loin, Mutton Rack) **25**; both chicken slabs **10**.
- **Lean** (`ModComponents.LEAN`): each slab rolls **60-100%** lean. The leaner it is, the **less fat to trim** in the butchery
  game (fewer, thinner patches and fewer loose specks; a 60% slab has the most). **Under 85%: one less meat** when it's cut up,
  **under 70%: two less** (`leanPenalty`; never below 1). Shown on the slab's tooltip.
- **Slabs don't stack** (each has its own timer and lean).
- **Cutting a slab up** doesn't reset the clock: the cuts get **the time the slab had left plus 25%** (`CUT_BONUS`).
- **Nothing that rots stacks**: slabs and cuts are one piece per item (`MeatItems`); a clean cut of 5 gives 5 pieces.
- **A freshness bar** under each piece, like durability (`MeatItems`): full when fresh, emptying as it rots, green to
  yellow to red; icy blue while it's chilled in a Fridge or Freezer.
- **Rotting:** when the time's up it turns into **Rotten Meat**: a slab into as many as its cut's yield, cuts one for
  one. Checked every second in players' inventories, on the cursor and in the container they have open (the Butcher's
  Table's slab slot too), on dropped items, in item frames, held or worn by mobs and armor stands, in Fridges and
  Freezers and on placed slabs; anything in a closed container (a chest...) the moment it's opened (`FreshnessEvents`).
  Rotten Meat is plain (no timer), so it all stacks to 64.
  Meat with no timer yet (from the creative menu) gets one the first time it's checked.
- **Placed slabs on ice** (the block under it in `#minecraft:ice`: Ice, Packed, Blue or Frosted Ice) rot like in a
  Freezer, **6x slower** (`ICE_SLOWER`): going onto the ice it notes when and how long it had left, and runs down from
  those (exact, unloaded too; the client only hears of it going on or off the ice). Off the ice it carries on as normal.
- **Looking at a placed slab** (`client/MeatSlabHud`, the block entity is synced): a small card under the crosshair with
  its name, lean, and "Fresh for 12:34" (+ "Set it on ice to keep it longer"), or on ice "❄ On ice: rotting 6x
  slower", "Keeps 1:14:00 here", "(12:20 off the ice)", or "Rotten".
- **Placed slabs keep rotting** (`MeatSlabBlockEntity` holds the timer and lean; breaking a fresh one gives them back
  to the slab, `copy_components` in its loot table). When the time's up the block goes **rotten**: grey-green and
  mouldy (`MeatSlabBlock.ROTTEN`, a child model with rotten textures, `MeatTex.Rot`), with a little drifting spore;
  breaking it gives Rotten Meat (the cut's yield).
- **Rotten Meat** (`rotten_meat`): not people food. **Pet food:** Cooks can make Training Feed with it instead of raw
  meat (`training_feed_from_rotten_meat.json`: any seed + Rotten Meat = 6). **Fertilizer:** it does for Rotten Flesh
  in the Decomposter (Yield Fertilizer).
- **Fridge and Freezer** (`ColdStorageBlock`): chests (27 slots) with a fuel slot; **anyone** can use one, Farmers
  and Cooks make them. While fuel burns, meat in them rots **2x slower (Fridge)** or **6x slower (Freezer)**:
  - Fridge fuel: **Coal or Charcoal**, 5 minutes each (`FRIDGE_FUEL`). Freezer fuel: an **Ice** block, 5 minutes
    (`ICE_FUEL`), **Packed Ice** 15 minutes (`PACKED_ICE_FUEL`), **Blue Ice** 45 minutes (`BLUE_ICE_FUEL`), or a
    **Snowball**, only 30 seconds (`SNOWBALL_FUEL`).
  - **The tooltip in there** (`Chill`, `ModComponents.CHILL`, put on by the running Fridge / Freezer, taken off as soon
    as the meat is out or the fuel runs out): "❄ Chilled: rotting 2x slower" (Freezer: "❄ Frozen: ... 6x"), "Keeps
    26:42 in here" (counting down at the normal pace) and "(13:21 if taken out)". It counts down smoothly (the real
    timer is only pushed back once a second, which made the plain countdown jump back and forth).
  - The fuel line on its screen: "Burning 4:12", "Idle: no meat" (fuel but nothing to keep), or "Add Ice" / "Add Coal".
  - It only burns while there's meat in it to keep. Every second it works out how much of the time since it last
    looked was powered (pulling in more fuel as needed, so time spent in an unloaded chunk counts the same) and pushes
    its meat's timers back by that, less the slowed-down share. Out of fuel it's just a chest.
  - It shows: the Fridge is plain white enamel with a green light; the Freezer is frozen all over (icy blue, frost
    creeping in from the edges, icicles under the lid, a frosted lid) with a blue light; both give off cold wisps while
    they run. The Freezer's screen is frozen too (ice-blue panel crusted with frost, icicles, drifting snow, frosted
    slots). Its screen
    (`client/ColdStorageScreen`): the rows, the fuel slot and a gauge, "Meat lasts 2x / 6x".
  - Textures: `tools/make_cold_storage_textures.ps1` (also Rotten Meat).

**The butchery game** (`client/ButcheryGame`): the slab lies on the chopping block as a grid of meat, fat and hidden bone.
The fat comes in jagged patches round the edge (`patches`, `jag`) plus 5-10 loose specks of it a little way in.
1. **Trim the fat** (no clock: it's about accuracy): hold and drag the cleaver over the pale fat round the edges until
   **every bit** of it is gone. The ring round the mouse (`KNIFE`, 2.0 cells) is what it takes; **Accuracy** is the meat
   kept, and below **95%** (more than 5% lost, `MEAT_LIMIT`) it's botched. Once only 15% of the fat is left, what's left glows so the slivers can be found.
   All the words go in a strip along the bottom of the board, never over the meat.
2. **Scan for bones**: hold the mouse to look through the scanner lens; bones glow under it (and fade soon after),
   and it ticks faster the nearer one is. The battery lasts **2.5 s** of scanning (`SCAN_TIME`). Right-click drops a pin (up to 10;
   right-click one to pick it up); pins stay for step 3. Space ends the scan early.
3. **Make two cuts** (20 s): each one in one go, starting at the **top edge of the meat** (what's left after trimming,
   in that column; gold ticks mark the top and bottom edges) and going down out past its **bottom edge**, at least a
   little way from the first. The cut may wind about, but **touching a bone** ("Clunk!") botches it.
Every slab has two clear ways through (lanes kept free of bone, about a third and two thirds across). Bones by cut:
shoulder blades (chuck, shoulders), small bones (brisket, belly), a leg bone with knobbly ends (round, ham, legs),
ribs running top to bottom (rib, rack: cut between them), a row of vertebrae (loin, breast, saddle); one more or one fewer of those at random, a quarter of them sometimes another kind of bone, plus 1-5 little
bones. Every bone is **curved** (a chain of short capsules along a bent curve) and where it lies, how long, how bent
and how thick are all random (each slab also rolls how bent its bones are, `bendScale`), so no two slabs are alike. Rarer cuts have more bones and tighter gaps. Played on the client like the sewing game; only the result goes to the server.

## Farmer's Guide (`client/FarmersGuideScreen`)
Workshop: Blocks (Decomposter, Butcher's Table, Fridge, Freezer), Items (Yield, Super Yield and Fast Fertilizer, Scythe, Diamond Scythe, Butcher Knives). Information: Harvests,
Fertilizer, Butchering (Meat Slabs, Luck, Showing Them Off, Butcher Knives, Sharpening, Cutting a Slab, the three steps,
Botched, Fine Cuts), Freshness (Meat Rots, the Freshness Bar, Cutting Buys Time, Lean, the Fridge, the Freezer, On Ice,
Rotten Meat), Player Debuffs.

## Testing (`farmer/FarmerTests`)
Harvests (anyone else: wheat 1+1, carrots 2, unripe just the seed, nobody 2 potatoes, a Farmer much more, picking a
bush 1 berry); Fertilizer (only Farmers, 2 harvests, not twice, others don't use it up, Super 3, no Yield on Super, nothing goes
with Fast, anyone's harvest uses up Fast, an extra stage about 75% of the time, 50-90% more
wheat); Fertilized Farmland (shows each kind, stays wet, back to farmland when used up, trampled to dirt and
hoed again it has nothing on it);
the Scythe (14 ripe crops of the 5x3 patch, the unripe one and the row beyond left, one wear, Farmer harvests); the
Diamond Scythe (15 of the 5x3 patch, the row beyond left, 1600 uses); the Decomposter (empty, then full and working
with ingredients, empty again; takes its time, all three recipes in any order, anything else makes nothing);
`farmer_freshness`: shelf lives, lean 60-100 and its penalty, cuts get a quarter more time, rotten slabs and cuts turn
to Rotten Meat (as much as the cuts), fresh meat stays, undated meat gets a timer, plain meat doesn't rot, a running
Fridge (600 ticks take 300 off, one coal at a time, no ice), a running Freezer on a Snowball (600 ticks take 100 off),
Ice runs 10x a Snowball, out of fuel meat rots as normal, Rotten Meat in the Decomposter;
Meat Slabs (only Farmers with a butchering blade, not babies or other animals, about 15% of kills, 30% with a Bone in
the sword's socket (which only fits swords), Luck raises the chance and the rare slabs); the Butcher's Table (no knife
no cutting, Cut Slab takes the slab and one cut of the knife, the cuts get the slab's time plus a quarter, a clean
cut gives the fine cut one piece at a time, a botched one plain meat, the knife stays in the table, its last cut
spends it, closing mid-cut counts as botched, Diamond knife has 10 cuts, sharpening).

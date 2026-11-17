# Designer (Fun job)

Makes **luxury bags** worn on the back (`Back.md`) that add a few slots for certain things. Two blocks: the **Sewing
Machine** sews **Patches** with a needle minigame, the **Designer Table** stitches Patches + Leather into a bag.
`Job.DESIGNER` (the last job). Code: `designer/` (`Bag`, `Bags`, `BagSlot`, `Pattern`, `DesignerBlock`,
`DesignerBlockEntity`, `SewingMachineBlock`(+`Entity`, `Menu`), `DesignerTableBlock`(+`Entity`, `Menu`),
`DesignerTests`), `client/SewingGame`, `SewingMachineScreen`, `DesignerTableScreen`, `DesignerDraw`, `BagPanel`,
`BagLayer`, `DesignersGuideScreen`, `mixin/InventoryMenuMixin` (bag slots), `InventoryScreenMixin` (the pouch),
`EffectsInInventoryMixin`, `CreativeBackSlotMixin`. Art: `tools/make_designer_textures.ps1` (menus, icons, blocks, the
bags on players' backs), `tools/make_backpack.ps1` (the Backpack's icon).

## Recipes (`capitalism:job_shaped` "designer": Designers only)
| Item | Recipe |
|---|---|
| **Designer Table** | Leather, Shears, Leather / Planks, Crafting Table, Planks / Planks, gap, Planks |
| **Sewing Machine** | 3 Iron Ingots / Iron Ingot, Redstone, String / 3 Wooden Slabs |
| **Iron Sewing Needle** x6 | Iron Ingot top right, Stick in the middle, String bottom left (a diagonal). Sews **Generic patches** (only these), and stitches Standard bags at the Designer Table. |
| **Diamond Sewing Needle** x6 | The same with a Diamond. Sews **Fancy, Luxury and Utility patches** (only these; `Pattern.needle`), and stitches every bag but Standard ones, and hats, at the Designer Table. The cloth feeds at the same speed whichever needle it is. |

Needles **stack to 64** and have no durability: **every slip in the needle game breaks one**, and the Designer Table
uses up **2 needles** for every bag or hat it stitches (`DesignerTableBlockEntity.NEEDLES_USED`): Iron for Standard bags,
Diamond for Fancy and Luxury bags and the hats (`needleFor`), so even a small bag costs something.

Both blocks: only Designers can use them ("Only a Designer can use the ..."), they face the player who placed them,
breaking one drops what's in it.

## Patches (`Pattern`) and the Sewing Machine
Three tiers, the machine's **tabs** (sticking up above it): **Generic** (easy, difficulty .2, and its cloth feeds 25% faster so it isn't dull), **Fancy** (medium, .55, its cloth feeds 20% faster),
**Luxury** (hard, .9, but its seam 12% wider than that would make it: hard but fair; its cloth feeds 12% faster). Generic patches are sewn with Iron needles, Fancy and Luxury ones with Diamond needles.

| Tier | Patch | Sewn from, in order |
|---|---|---|
| Generic | **Leather Patch** | Leather, Leather, String |
| Generic | **Strap Patch** | Leather, String, Iron Nugget |
| Generic | **Pantry Patch** | Leather, Wheat, Honeycomb, String |
| Generic | **Seed Pocket Patch** | Leather, Wheat Seeds, Bone Meal, String |
| Fancy | **Velvet Patch** | Wool (any color), Leather, String |
| Fancy | **Embroidered Patch** | Leather, String, String, Gold Nugget |
| Fancy | **Gold Trim Patch** | Leather, Gold Ingot, String |
| Fancy | **Silk Patch** | String, String, Wool (any color), String |
| Luxury | **Gucci Patch** | Leather, Green Wool, Red Wool, Compacted Diamond |
| Luxury | **Burberry Patch** | Emerald, Brown Wool, Black Wool, Emerald |
| Luxury | **Louis Vuitton Patch** | Emerald, Brown Wool, Gold Ingot, Emerald |
| Luxury | **Prada Patch** | Leather, Black Wool, Compacted Iron, Compacted Diamond |
| Utility | **Frozen Patch** | Blue Wool, Ice (any: Ice, Packed Ice or Blue Ice), Snowball, Iron Ingot |

**Utility** (the 4th tab, `Pattern.Tier.UTILITY`): patches for bags that *do* something. Medium-hard (difficulty .72,
between Fancy and Luxury; cloth 15% faster, seam 5% wider), Diamond needles.

The bags only use Generic patches so far; Fancy and Luxury ones are for the bags to come.

**The machine's menu:** (switching tabs picks that tab's first pattern; half way through a patch the tabs are locked: Unpick first. Hovering an empty slot names exactly what goes in it, e.g. "Needs: Green Wool": you can't always tell wool colors apart by their ghosts) the tier tabs on top, that tier's pattern tiles down the left (hover: what it's made of); the patch big in the middle on a round
of chalked seams, one per material, filling in with gold thread as they're sewn on (the needle bobs over the next);
under it the row of 4 material slots with **ghosts** of what goes in each (a gold arrow over the next one, unused
slots stitched shut); the needle and the finished patches on the right. **Sew** plays the needle game for the next
material; **Unpick** starts the patch again (what's sewn on is lost). The pattern can only change while nothing is
sewn. When the last material is on, the Patch goes to the output and the machine is ready to sew it again.

### The needle game (`client/SewingGame`)
Drawn over the whole screen like the Smiths' dent game. The cloth (colored like the material) feeds **down** the
screen with the seam chalked on it; the needle sits near the bottom. **Only moving the mouse left and right moves the
needle.** Click to start once the needle is on the seam. Keep it inside the chalk lines:
- The seam starts straight, then winds. The **tier** sets how hard: width (23% - 12%·difficulty of the cloth), speed
  (72 + 88·difficulty px/s), length (3.3 + 1.2·difficulty seconds, after a short straight start).
- The **material** sets how it winds (`Pattern.Style`): **string and wool** sway wide side to side, some swings quicker or wider than others;
  **leather** (and wheat, seeds...) wanders smoothly at random (every seam widens on its steep stretches, so it's never thinner across than a straight one; string and wool and get 12% more width on Fancy patches and 20% on Luxury,
  `SewingGame.LUXURY_WAVY_WIDTH`, since their swings are the widest); **iron and gold** run straight then jump sharply on a
  diagonal (Compacted Iron too; its seam is 10% wider all along, `SewingGame.RIGID_WIDTH`, the jumps may widen up to 3 times,
  `RIGID_WIDEN`, so they aren't thin, and come every 90 - 26·difficulty pixels of cloth); **diamond** (and emerald, Compacted Diamond) twitches from side to side at random (its seam widens on steep stretches, so it's never thinner across than a straight one);
  **ice** (Ice, Packed / Blue Ice, Snowball, Snow Block: `Style.ICY`) is a **frost crack**: straight zig-zags snapping
  sharply from side to side (a corner every 80 - 14·difficulty pixels of cloth), and **frost creeps in and out**:
  where it's frozen the seam pinches to as little as 56% of its width (`SewingGame.freeze`), glowing icy blue so you
  see it coming; to be fair the seam is 22% wider all along (`ICY_WIDTH`). Frost crystals glint on the cloth.
- The cloth is the material's color. On light cloth the chalk lines are dark navy (white on dark cloth) and the cloth
  outside the seam is shaded, so the seam always shows.
- **Touching a line ends the try** (red flash, a cracking sound, "A needle broke!"): **one needle breaks** (even in
  creative), the **material is kept** and that spot **isn't checked off**: sew it again.
- Sewn stitches trail away below the needle; a spool on the right climbs as the seam gets done; the needles you have
  show on the left, crossed out as they break.
- At the end the number of slips goes to the server (`SewingMachineMenu.SEW + slips`): a needle
  breaks and nothing else happens if you slipped; otherwise the material is used and checked off. Esc stops before the
  end with nothing used. Only the LAST material needs room in the output ("Take the X out" otherwise); the problem shows on a dark plate in
  place of the "Inventory" label.

## Bags (`Bag`) and the Designer Table
| Bag | Slots | Holds | Patches (in the table's row, in order) |
|---|---|---|---|
| **Backpack** | 2 | anything | Leather, Leather, Strap, Strap |
| **Food Satchel** | 4 | food (anything with food) | Leather, Leather, Strap, Pantry, Pantry |
| **Seed Satchel** | 10 | seeds (`#capitalism:seeds`, rice too) | Leather, Leather, Strap, Strap, Seed Pocket, Seed Pocket |
| **Fancy Backpack** (Fancy) | 3 | anything | Velvet, Velvet, Strap, Strap, Embroidered |
| **Fancy Food Satchel** (Fancy) | 6 | food or water (anything eaten or drunk) | Velvet, Silk, Pantry, Pantry, Gold Trim |
| **Fancy Seed Satchel** (Fancy) | 15 | seeds or flowers | Silk, Silk, Seed Pocket, Seed Pocket, Embroidered, Gold Trim |
| **Uniform Backpack** (Fancy) | 4 | 2 anything, 2 food or water | Embroidered, Embroidered, Strap, Pantry, Gold Trim |
| **Gucci Bag** (Luxury) | 8 | anything | 3 Gucci, Gold Trim, Strap, Silk |
| **Burberry Bag** (Luxury) | 11 | 8 anything, 3 food or water | 3 Burberry, Gold Trim, Strap, Pantry |
| **Louis Vuitton Bag** (Luxury) | 14 | 4 food or water, 6 anything, 4 weapons or tools | 3 Louis Vuitton, Gold Trim, Strap, Embroidered |
| **Prada Bag** (Luxury) | 14 | 10 gems or ores (gems, ores, raw ore, ingots, nuggets, coal, redstone, scrap), 2 anything, 2 food | 3 Prada, Gold Trim, Strap, Velvet |

| **Freezer Sling** (Utility) | 10 | food or meat (anything eaten, and Meat Slabs / fine cuts) | Silk, Frozen, Frozen, Silk |

The **Freezer Sling** keeps what's in it **like a Freezer, no fuel**: meat in it rots **6x slower** (`Bag.slower`,
`farmer/FreshnessEvents`: its time runs down from when it went in, exact even while you're offline; the tooltip shows
"❄ Frozen: rotting 6x slower"). Meat in any other worn bag rots as normal. Art: `tools/make_utility_textures.ps1` (the
Frozen Patch, the sling's icon and the bag on your back, recoloured to ice from the Silk Patch and Fancy Food Satchel).

The first three are the **Standard** tab; **Utility Bags** is the table's 5th tab (`Design.Line.UTILITY`). In the bag pouch each group of slots is shaded differently, empty slots
show a faint picture of their group, and hovering one names what it takes.

## Hats (`Hat`): for pets
The table's **Hats** tab. A pet wears one (the pet window's Actions tab, `PetTrainer.md` > Hats).

| Hat | Does | Patches |
|---|---|---|
| **Gucci Top Hat** | hunger drops at 70% speed | 2 Gucci, Silk, Velvet |
| **Burberry Santa Hat** | thirst drops at 70% speed | 2 Burberry, Silk, Velvet |
| **Louis Vuitton Baseball Cap** | starving hurts it half as often (lasts twice as long) | 2 Louis Vuitton, Embroidered, Strap |
| **Prada Fedora** | +2 hearts, +2 drumsticks, +2 water drops | 2 Prada, Gold Trim, Velvet |

### The Designer Table's tabs
**Standard Bags, Fancy Bags, Luxury Bags, Hats** (sticking up above the table, `Design.Line`); a tab shows its
designs (`Design`: a Bag or a Hat; the table saves its choice as a place in `Design.all()`: bags, then hats). The
chosen design's name and what it holds or does show beside them (squeezed to fit; hover for the full list).

Plus **1 Leather** and **2 Sewing Needles**, used up: Iron for Standard bags, Diamond for the rest (only that kind will do). **The table's menu:** the bags along the top
(the chosen one in gold, hover for its patches), its slots and what it holds; the green cutting mat with the row of 6
patch slots (ghosts of the patches it needs, spare slots stitched shut, a red edge round a wrong patch); Leather and the
needle bottom left, the bag bottom right. **Stitch** runs a needle along the row sewing gold thread across the patches,
then the bag pops out with a sparkle. Shift-click a Patch: it goes to the empty row slot that wants it.
The **Backpack** also goes into the Animal Bjorn (`PetTrainer.md`), so Pet Trainers buy them from Designers.
~16 bags are planned; new ones go at the END of `Bag` (and patterns at the end of `Pattern`), and every bag must be in
the item tag `capitalism:back_items` or it can't go in the Back slot (`designer_freezer_sling` checks them all).

### Wearing a bag (`Bags`, `BagSlot`)
- In the **Back slot**. Its slots show in a **stitched leather pouch** hanging off the right side of the survival
  inventory (`client/BagPanel`: the bag's name on a brass plate, faint pictures of what goes in the empty slots, a
  tooltip saying what it holds). Potion effects move right to make room. The creative inventory doesn't show them.
- The inventory always has `Bag.MAX_SLOTS` (16) bag slots after the Back slot (indexes 47-62); a bag uses its first ones.
- Only what the bag holds fits; never another bag, a shulker box or a bundle.
- Shift-clicking from the inventory puts what the bag holds into it first.
- **A bag only holds things while it's worn.** The items live in the worn bag (`minecraft:container`), but when the bag
  comes off your back (picked out of the Back slot, swapped for something, shift-clicked off, thrown) everything in it
  **spills on the ground** and you get the empty bag (`Bags.spill`, called from `Back.set`, `BackSlot.remove` and the
  inventory's shift-click). **Dying:** the things drop and **the bag is destroyed** (it burns: bags keep being bought).
  So there's no stacking full bags.
- Everyone sees the bag on your back (`client/BagLayer`): the bag with its flap and buckle, straps over the shoulders.

## Testing
`designer/DesignerTests` (`gradlew runGameTestServer`): bag slots (how many per bag, what fits, no bags in bags), the
seeds kept in the worn satchel, shift-clicking into it; spills (taking it off, swapping, shift-clicking it off, dying:
things drop, the bag is gone when you die); the Sewing Machine (no pattern / no needle can't sew, a clean seam sews one on, the
pattern can't change half way, a slip breaks one needle and keeps the material (the spot isn't done), Fancy patches take any wool, Generic patches need Iron needles and Fancy and Luxury ones Diamond, the last
material makes the Patch, unpicking); `designer_freezer_sling` (the Frozen Patch: Utility, Diamond needle, icy seam; the
sling: Utility Bags, 10 slots, its patches, takes food and slabs only, meat in it keeps 6x as long); the Designer Table (a wrong patch, making a Backpack, what's used, too few or the wrong kind of needles can't stitch, 2 needles used up, a hat needs Diamond).

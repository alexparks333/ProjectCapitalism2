# Lava Creator

A smith's machine that brews **Refined Buckets of Lava**: fuel worth **2 buckets** in any smith table (a normal Lava
Bucket is 1). Code: `weaponsmith/LavaCreatorBlock`, `LavaCreatorBlockEntity`, `LavaCreatorMenu`, `client/LavaCreatorScreen`.

## Making one
- `IBI / ICI / III` (I = Iron Ingot, B = Blaze Rod, C = Cauldron). **Only smiths can craft it** (Weaponsmith, Toolsmith
  or Armorsmith; `job_shaped` with a job list). **Only smiths can use it too** (opening it, pouring water in: `occupation/JobEvents`). Pickaxe to break; it keeps nothing when broken
  except the items in it.
- **Look:** polished blackstone bricks with a glowing iron grate on the sides and a basin on top: dark when idle,
  bubbling lava (with lava pops and smoke) while it brews. Glows a little, more while brewing.

## Using it
1. **Water:** put a **Water Bucket** in the water slot (left, under the drop), or **right-click the block** with one.
   It's poured into the tank (holds **4 buckets**); you get the empty bucket back.
2. **Blaze Powder** in the top slot.
3. An **empty Bucket** in the bottom slot.
4. After **1 minute** it uses 1 Blaze Powder and **half a bucket of water**, and the Bucket becomes a
   **Refined Bucket of Lava**. Take it out and put in another Bucket for the next one.
- **Menu:** the smith tables' dark molten look; the water tank fills blue (hover it for "Water: 2.5 / 4 buckets"), the
  channel between the Blaze Powder and the bucket fills with lava as it brews. Empty slots say what goes in them.
  Drawn by `tools/make_lava_creator_gui.ps1`.
- **Hoppers:** Water Buckets and Blaze Powder in from the top, empty Buckets from the sides; Refined Buckets (and the
  water slot's empty buckets) out the bottom.

## Numbers (change any of them, `LavaCreatorBlockEntity`)
- Tank: 4 buckets (`WATER_CAPACITY`). Water per Refined Bucket: half a bucket (`WATER_PER_BUCKET`).
- Brew time: 1 minute (1200 ticks) (`BREW_TICKS`). Fuel value in a smith table: 2 buckets (`SmithTableBlockEntity.fuelValue`).

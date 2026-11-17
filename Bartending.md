# Bartending — Juicer, Fermenter & Bartending Table

Everything the **Bartender** job uses: how each machine works, every recipe, and where to change things in the code. Only Bartenders can use the Fermenter and the Bartending Table (see section 6 and `Jobs.md`); the Juicer is open to everyone.

The drink pipeline:

```
Fruit ──► JUICER ─────► Juice buckets ──► BARTENDING TABLE ──► 6 Juices ─┐
                                                                           ├──► BARTENDING TABLE ──► Cocktails
Crops ──► FERMENTER ──► Liquor buckets ─► BARTENDING TABLE ──► 4 Shots ──┘
(+ Sugar)
```

## In-game guide

Type **`/guides`** to open the Guides screen, then click **Bartender's Guide**. **Only Bartenders see it** (`/guides` only lists the guides for your jobs, plus the General Guide), and only Bartenders can read the Bartender's Guide book item ("Only Bartenders can make sense of this book."; creative ignores it). It has three sections, always in this order: **Bar**, **Recipes**, **Information** (same layout as the Cook's Guide; it opens on Bar).

**Bar** (two tabs)
- **Blocks:** the Bartender's machines (Juicer, Fermenter, Bartending Table) as **black silhouettes until you've crafted one**. Click one for its **crafting grid** (always shown) and, once unlocked, its name and what it does. Grids are hand-written in `BartendersGuideScreen.buildBlocks`: keep them matching `data/capitalism/recipe/`.
- **Ingredients:** everything you make to go *into* drinks: juice buckets (Juicer), liquor buckets (Fermenter), shots, juices, Simple Syrup and Bitters (Bartending Table). Click one for how it's made (machine, ingredients, time).

**Recipes** (two tabs)
- **Drinks** (plain drinks: wine glasses, Beet Nightcap, the classic cocktails) and **Specialty Drinks** (cocktails with effects). A drink goes in Specialty Drinks automatically if it gives any effect.
- **Click any drink or item** to see its recipe: machine, ingredients, glass bottles, time and water drops.
- **Gold-outlined ingredients** can be made too. Click one to jump to its recipe. (e.g. a juice in a drink jumps to Bar > Ingredients.)
- **Undiscovered recipes (Smash Bros style):** anything you haven't made yet shows as a **black silhouette**, and its page says **"???"** instead of the name, with its drops / drumsticks / effects hidden ("Make it once to unlock its name and info!"). The **recipe itself still shows** (ingredients aren't hidden), so you can work out how to make it. Make it **once** (take it out of the machine's result slot) and the picture, name and info unlock for good. Per player, kept through death.
  - Counts as "made": crafting it (blocks, plates), smelting it (Cooked Rice), taking it out of the Juicer / Fermenter bucket slot, the Bartending Table result, the Sushi Table output, the Cooking Table result, or an Oven meal slot, by hand (clicking or shift-clicking). Pulling it out with a hopper doesn't count. Buying it doesn't count.
  - Ops: `/discoveries all [player]` unlocks everything, `/discoveries reset [player]` locks it all again.
  - Code: `guide/Discoveries.java` (+ `DiscoveryData`, `ClientDiscoveries`, `network/DiscoverySyncPacket`), silhouettes drawn by `client/Silhouettes.java` (reads each item's model and draws its texture layers in black).
- **Esc** goes back to the Guides screen.

**Information**: two tabs, **Bartending** (Your Machines, Glass Bottles, Golden Drops, Hoppers) and **Player Debuffs** (Locked Machines: others can't use the Fermenter or Bartending Table), as clickable title boxes like the other guides. A line or two each, no times, numbers or effects: players find those out. Machine crafting is in Bar > Blocks and every drink is in Recipes.

It's built from the actual recipes, so it's always up to date.

---

## 1. Juicer

A wooden machine with copper corners, spinning gears and a juice window on the front.

| | |
|---|---|
| **Crafting** | `Copper Ingot, Hopper, Copper Ingot` / `Planks, Grindstone, Planks` / `Copper Ingot, Planks, Copper Ingot` |
| **Mined with** | Axe |
| **Time per bucket** | 30 seconds (config `juicerTicks` = 600) |

**How to use**
1. Put **6 of the same fruit** in the 6 top slots (marked F-R-U-I-T-S). **One fruit per slot.**
2. Put **1 empty Bucket** in the big slot. **Only an empty bucket fits there.**
3. The gears spin, the arrow and gauge fill in the juice's color, and after 30 seconds the bucket turns into juice **in the same slot**.
4. Take the juice out and put another empty bucket in to go again.

**Hoppers:** fruit in from the **top**, empty buckets from the **sides** (one at a time), finished juice out the **bottom**.

**On the block:** the gears spin only while juicing. The front window fills with that juice's color and stays full while a finished bucket is waiting.

### Juice recipes

| 6× Ingredient | Result | Color |
|---|---|---|
| Melon Slice | Bucket o' Watermelon Juice | Red-pink |
| Apple | Bucket o' Apple Juice | Golden yellow |
| Carrot | Bucket o' Carrot Juice | Orange |
| Beetroot | Bucket o' Beetroot Juice | Deep red |
| Chorus Fruit | Bucket o' Chorus Fruit Juice | Lavender |
| Kelp | Bucket o' Kelp Juice | Green |
| Sweet Berries | Bucket o' Sweet Berry Juice | Berry red |
| Glow Berries | Bucket o' Glow Berry Juice | Glowing amber |
| Golden Apple | Bucket o' Golden Apple Juice | Bright gold |
| Sugar Cane | Bucket o' Simple Syrup | Pale gold |

---

## 2. Fermenter

A **2-block-tall** machine: an oak cask with a brass spigot on the bottom and a glass fermenting jug with a copper airlock on top. Breaking either half breaks both; right-clicking either half opens it.

| | |
|---|---|
| **Crafting** | `Glass, Glass, Glass` / `Iron Ingot, Barrel, Iron Ingot` / `Planks, Planks, Planks` |
| **Needs** | 2 blocks of space to place |
| **Mined with** | Axe |
| **Item icon** | The real 3D model, both halves stacked and shrunk to fit a slot, with amber liquid in the jug (`models/item/fermenter_block.json`, built from `fermenter_lower`, `fermenter_upper` and `fermenter_liquid_still_3`; rebuild it if those change) |
| **Time per bucket** | Half a Minecraft day = **10 real minutes** (config `fermenterTicks` = 12000) |
| **Sugar** | 1 every 1 min 30 s of fermenting (config `fermenterSugarTicks` = 1800) |

**How to use**
1. Put **6 of the same fermentable** in the 6 top slots. **One per slot.**
2. Put **Sugar** in the sugar slot (it shows a faded sugar and holds a full stack). Sugar is the fuel: **1 sugar burns every 1 minute 30 seconds** while fermenting (about 7 per batch). The bar under the slot shows how much of the current sugar is left. **Out of sugar = paused**, not reset; add more and it carries on.
3. Put **1 empty Bucket** in the bucket slot.
4. The jug bubbles and fills with the liquor's color. After 10 minutes the bucket turns into liquor **in the same slot**, using up the 6 fermentables.

**Hoppers:** fermentables in from the **top**, sugar and empty buckets from the **sides**, finished liquor out the **bottom**.

### Liquor recipes

| 6× Ingredient | Result | Why |
|---|---|---|
| Cactus | Bucket o' Tequila | Real tequila comes from agave, a desert succulent |
| Potato | Bucket o' Vodka | Classic potato vodka |
| Sugar Cane | Bucket o' Rum | Rum is made from sugarcane |
| Sweet Berries | Bucket o' Red Wine | Red grapes stand-in |
| Glow Berries | Bucket o' White Wine | Pale golden "grapes" |
| Wheat | Bucket o' Whiskey | Grain spirit |

> Sugar Cane makes **Simple Syrup** in the Juicer but **Rum** in the Fermenter.

---

## 3. Bartending Table

A bar counter: mahogany paneling, brass foot rail, polished countertop, and a black bar mat with a shaker, mixing glass, jigger and a bottle with a pour spout.

| | |
|---|---|
| **Crafting** | `Black Wool ×3` / `Iron Ingot ×3` / `Planks ×3` |
| **Mined with** | Axe |

**How to use**
1. Put ingredients in the **5 slots** on the bar mat, in **any order**.
2. Put **Empty Glass Bottles** in the **glass slot** on the right (it holds a stack). **Every drink uses 1 glass bottle** (4 shots = 4 bottles). Not enough bottles = nothing can be made.
3. When everything matches a recipe, the result appears in the drink slot. Taking it uses **1 of each ingredient** (plus the bottles).
4. **Juice and liquor buckets give their empty bucket back. Purified Water, Bitters, shots, juices and Simple Syrup used as ingredients give their glass bottle back, straight into the glass slot.**
5. Shift-clicking the result makes as many as your ingredients allow.
6. Ingredients stay on the counter if you walk away.

## 3b. Spring Water (Excellent water; `block/OsmosisMachineBlock`)
The only way to Excellent Water Purity. Bartenders only (crafting and using the machine).
- **Advanced Osmosis Machine:** `IGI / GCG / IBI` (Iron Ingot, Glass, Cauldron, Bucket), Bartenders only (`job_shaped`).
  No menu, right-clicks: an **Osmosis Filter** goes in (one at a time), then a **Purified Water Bucket**; it presses
  the water through the filter for **1 minute** (hums, bubbles, the window lights up), then right-click (empty hand or a
  bucket) for a **Spring Water Bucket**. Each bucket wears the filter: an **Osmosis Filter lasts 2 buckets**, an
  **Advanced Osmosis Filter 4**, then it crumbles. Empty-handed it tells you what it's doing; shift + empty hand takes
  the filter out (when idle). Broken, it drops the filter and any Spring Water inside.
- **Filters** (anyone can craft them): Osmosis Filter = 3 Charcoal on top, 3 Paper below (bottom row empty), or the
  same with 3 Prismarine Shards; Advanced Osmosis Filter = 3 Prismarine Crystals over 3 Paper.
- **Bottling:** a Spring Water Bucket at the Bartending Table = **6 Spring Water** (6 glass bottles; the bucket comes back).
- **Spring Water** (a bottle): drinks like Purified Water but pulls purity toward **100**, three times as hard
  (`WaterPurity.EXCELLENT_PULL`): two or three bottles take you from Good to Excellent. Raw water drags you back down.
- **No Spring Water drinks:** alcoholic and other Bartender drinks never touch Water Purity, Spring Water or not: they
  only give thirst points (and gold drops) and their effects.
- NPC shops never sell Spring Water (players only). The Purifying Waterskin stays Purified (Good).
- Tests: `town/TownTests` > `spring_water`.

## Drink Ingredients

Things you make to put *into* drinks: **shots**, **juices**, **Simple Syrup** (poured from the Juicer's bucket) and **Bitters**.

### Shots
Liquor only comes as **buckets** (from the Fermenter) and **shots**. There are no bottles of liquor.

| Ingredients | Glass bottles | Result |
|---|---|---|
| Bucket o' Tequila | 4 | 4× Shot of Tequila |
| Bucket o' Vodka | 4 | 4× Shot of Vodka |
| Bucket o' Rum | 4 | 4× Shot of Rum |
| Bucket o' Red Wine | 4 | 4× Shot of Red Wine |
| Bucket o' White Wine | 4 | 4× Shot of White Wine |
| Bucket o' Whiskey | 4 | 4× Shot of Whiskey |

Made at the Bartending Table: put the bucket in any ingredient slot and **4 Glass Bottles** in the glass slot. **Every drink takes one glass**, so no bottles = no shots. The empty bucket comes back, and each shot gives its glass back when you drink it.

Shots can be drunk, even when full. **Tequila, Vodka, Rum and Whiskey shots give 2 water drops; wine shots give 1** (gold if your water bar is already full).

### Juices and Simple Syrup
Juice buckets (from the Juicer) pour into glasses the same way liquor pours into shots, but you get **6**:

| Bucket | Glass bottles | Result |
|---|---|---|
| Bucket o' Watermelon / Apple / Carrot / Beetroot / Chorus Fruit / Kelp / Sweet Berry / Glow Berry / Golden Apple Juice | 6 | 6× that Juice (e.g. Watermelon Juice) |
| Bucket o' Simple Syrup | 6 | 6× Simple Syrup |

- Put the bucket in any ingredient slot and **6 Glass Bottles** in the glass slot. The empty bucket comes back.
- **Juices** come in a **glass pitcher** (tinted the juice's color); **Simple Syrup** in a **syrup dispenser** (chrome cap and spout).
- **None of them can be drunk.** They're ingredients for cocktails and other drinks. Their glass goes back into the glass slot when they're used in a drink.
- Cocktails use the **glasses**, not the buckets (e.g. the Beet Nightcap takes 2 Beetroot Juice and 1 Simple Syrup).
- Code: `ModItems.JUICES`, recipe loop in `BartenderRecipe`. Textures: `juice_pourer(_liquid).png`, `syrup_pourer(_liquid).png` (scratchpad `make_pourers.ps1`), one model each, tinted per juice in `items/<id>.json`.

### Bitters

| Ingredients | Glass bottles | Result |
|---|---|---|
| Purified Water, Cocoa Beans, Sugar | 4 | 4× Bitters |

Bitters are an ingredient only (you can't drink them). Each one comes in its own glass bottle, so making 4 uses 4 bottles. The Purified Water's bottle comes back, and so does each Bitters bottle when it's used in a cocktail.

## Drinks

**Every alcoholic drink warms you** (shots, wine, cocktails: `ModItems.isAlcoholic`): **Warmed Up for 2 minutes**, so the cold can't reach you. It only holds you at 70 (just right), never makes you hot (`WellnessEvents.ALCOHOL_WARMTH_TICKS`, `Wellness.md` > Body heat).

Finished drinks, made at the Bartending Table.

### Wine by the glass

| Ingredients | Glass bottles | Result | Gives |
|---|---|---|---|
| Shot of Red Wine ×3 | 1 | Glass of Red Wine | 6 water drops |
| Shot of White Wine ×3 | 1 | Glass of White Wine | 6 water drops |

A glass of wine hands back its glass bottle when you drink it.

### Cocktails

| Drink | Ingredients | Glass bottles | Gives |
|---|---|---|---|
| **Beet Nightcap** | Beetroot Juice, Beetroot Juice, Simple Syrup, Bitters, Shot of Tequila | 1 | 6 water drops (overflow turns gold) |
| **Glowberry Gold Rush** | Shot of Whiskey, Glow Berry Juice, Simple Syrup, Honey Bottle, Sugar | 1 | **Luminescent Aura** 2:00 |
| **Nether Fireball Shot** | Shot of Tequila, Bitters, Blaze Powder, Nether Wart, Magma Cream | 1 | Fire Resistance + **Magma Trail** 1:30 |
| **Ender Tele-Tini** | Shot of Vodka, Chorus Fruit Juice, Bitters, Ender Pearl, Simple Syrup | 1 | **Phase Step** + Slow Falling 0:10 |
| **Pirate's High Seas Grog** | Shot of Rum, Kelp Juice, Cactus (the lime), Sugar, Water Bucket | 1 | Dolphin's Grace 1:00 + **Aquatic Treasure Vision** 2:30 |
| **Miner's Moonshine** | Shot of Vodka, Beetroot Juice, Carrot Juice, Sugar, Gold Ingot | 1 | Haste II + **Fortune Boost** 2:00 |
| **Vampiric Mulled Wine** | Shot of Red Wine, Bitters, Apple Juice, Sugar, Sweet Berries | 1 | **Life Siphon** 1:00 |
| **Overworld Mojito** | Shot of Rum, Watermelon Juice, Simple Syrup, Sugar, Oak Leaves (the mint) | 1 | Clears all bad effects + Speed II 0:45 |
| **Phantom's Nightcap** | Shot of Whiskey, Apple Juice, Phantom Membrane, Simple Syrup, Bitters | 1 | **Insomnia Cleanser** + **Phantom Camouflage** 3:00 |
| **Zombie's Hangover Cure** | Shot of Vodka, Carrot Juice, Beetroot Juice, Rotten Flesh, Milk Bucket | 1 | **Iron Stomach** + Regeneration II 1:30 |
| **Ender Twilight Fizz** | Shot of Vodka, Shot of White Wine, Chorus Fruit Juice, Glow Berry Juice, Sugar | 1 | **Spatial Dislocation** 1:30 |
| **Drunken Brawler's Stout** | Shot of Tequila, Shot of Whiskey, Shot of Rum, Bitters, Leather | 1 | **Berserker's Rage** 1:00 (Strength II + no knockback + Nausea) |

**Classic cocktails** (no effects: just the 6 water drops, gold when full, like every cocktail):

| Drink | Ingredients | Glass bottles |
|---|---|---|
| **Classic Old Fashioned** | Shot of Whiskey, Bitters, Simple Syrup, Sweet Berries, Snowball (the ice) | 1 |
| **Whiskey Sour** | Shot of Whiskey, Apple Juice (the sour), Simple Syrup, Egg (the foam), Bitters | 1 |
| **Watermelon Breeze** | Shot of Vodka, Watermelon Juice, Apple Juice, Simple Syrup, Sugar | 1 |
| **Sangria Pitcher** | Shot of Red Wine, Apple Juice, Watermelon Juice, Sweet Berries, Sugar | 1 |
| **White Wine Spritzer** | Shot of White Wine, Apple Juice, Simple Syrup, Water Bucket, Sugar | 1 |
| **Tropical Kelp Cooler** | Shot of Rum, Kelp Juice, Watermelon Juice, Simple Syrup, Sugar | 1 |
| **Beetroot Velvet Sour** | Shot of Tequila, Beetroot Juice, Bitters, Simple Syrup, Egg | 1 |

Every cocktail gives **6 water drops** (overflow turns gold), can be drunk even when your bars are full, hands back its glass bottle.  Ingredients go in any order. Honey Bottles give their glass back into the glass slot; Water and Milk Buckets leave their empty bucket.

### Cocktail effects
- **Luminescent Aura** (Glowberry Gold Rush): a bright light (level 15) follows your head, so you don't need torches while it lasts. It's an invisible light block that moves with you (in air or water) and disappears when the effect ends or you log out.
- **Magma Trail** (Nether Fireball Shot): you leave a trail of little flames where you walk (each lasts 5 seconds). They don't hurt you or blocks, but **hostile mobs and other players** that step in them take half a heart of fire damage and catch fire for 4 seconds (Fire Resistance protects them; creative players are left alone).
- **Phase Step** (Ender Tele-Tini): the moment you finish drinking it, you're **teleported back to where you were standing 10 seconds earlier** (same dimension only), with portal particles, and get **Slow Falling for 10 seconds** so you land safely.
- **Aquatic Treasure Vision** (Pirate's High Seas Grog): chests that are part of a **shipwreck** or **buried treasure**, within about 48 blocks, **show as a glowing cyan spot, through the ground and water**: a small glowing orb sits in the middle of each chest (a client-only display entity: a shrunken sea lantern with a cyan glow outline), plus soft sparkles rising from it when it's in view. Only you see it (the server never knows about it), it has no hitbox so it can't get in your way, and it disappears when you leave or the effect ends. **Opening the chest doesn't stop the glow**, only breaking it does: the first time Treasure Vision spots an unopened treasure chest it tags that chest for good (`ModAttachments.TREASURE_CHEST`, saved on the chest). A chest someone opened before anyone saw it with Treasure Vision can't be told apart from a normal chest, so it won't glow. The list updates every 2 seconds; opened chests stop glowing. Plus Dolphin's Grace for fast swimming (1:00). (Skimming the surface fast with Dolphin's Grace used to get you kicked for "flying" by vanilla's anti-fly check; while you have Dolphin's Grace and water is within 3 blocks under you, the server now forgives it. Also covers the Ocean Sovereign Roll.)
- **Fortune Boost** (Miner's Moonshine): every item an ore drops has a **1 in 3 chance of one extra**, on top of Fortune. (Silk-touched ore blocks don't count.) Plus Haste II.
- **Life Siphon** (Vampiric Mulled Wine): when you hit a **hostile mob in melee**, you heal **25% of the damage** you dealt (red particles). Arrows don't count.
- **Overworld Mojito**: instantly removes every **bad** potion effect (Poison, Hunger, Slowness, Weakness...), then Speed II for 45 seconds. Good effects stay.
- **Insomnia Cleanser** (Phantom's Nightcap): resets your "time since rest" right away, like sleeping: **phantoms stop spawning for you**. **Phantom Camouflage** (3:00): phantoms can't target you.
- **Iron Stomach** (Zombie's Hangover Cure): cures Hunger (food poisoning) right away, and you **can't get Hunger** while it lasts (raw chicken, rotten flesh, dirty water...). **Cooks can eat raw meat and fish** while it lasts. Plus Regeneration II. (It doesn't stop Poison.)
- **Spatial Dislocation** (Ender Twilight Fizz): a hit of **3 hearts or more**, or one that **would kill you**, teleports you to a safe random spot up to 8 blocks away (like eating a Chorus Fruit). A killing blow does no damage at all; a heavy one still hurts. At most once every 2 seconds, so a fast pile of hits can still get you, and the void or /kill can't be dodged.
- **Berserker's Rage** (Drunken Brawler's Stout): you **can't be knocked back**, with **Strength II**, but also **Nausea** (the wobbly screen) for the whole minute.
- Code: plain potion effects are on the drink items (`ModItems.cocktail`); the custom effects are registered in `core/ModEffects.java` and run in `player/DrinkEffects.java`; the treasure glow is `client/TreasureSightClient.java` (`network/TreasureSightPacket`). Textures: scratchpad `make_cocktails.ps1` and `make_cocktails2.ps1` (drinks + effect icons), `heart.ps1` (Life Siphon icon), `make_nightcap.ps1` (Beet Nightcap).

---

## 4. Golden bars (Cook & Bartender bonus)

Bartender drinks (and the Cook's Gourmet food) can give **golden food drumsticks** and **golden water drops**, an extra bar on top of the normal one. (Gourmet food's gold depends on how it's served: half by hand, full off a Silver Plate, 1.5x off a Golden Plate; see `Cook.md`.)

- A gold-giving item adds food and/or water points. They **fill your normal bar first**, and **everything past full turns gold**, up to **one full extra bar** (10 gold icons) for each.
- Gold shows **from right to left** over the normal bar.
- **Gold drains first, at exactly the same speed as the normal bar.** Your normal bar doesn't go down until the gold is gone: gold keeps it topped up, including a bonus 11th icon (Balanced diet / Excellent water purity, see `Wellness.md`). The gold bar is as long as the normal one, so it gets an 11th gold icon too when you have the bonus slot.
- **Gold-giving food and drinks can be used even when you're full.** Normal food and water can't, and they never make gold.
- Example: a meal worth a full bar + 3 icons, eaten while empty, fills your food bar and adds 3 golden drumsticks.

| Item | Food | Water |
|---|---|---|
| Shot of Tequila | 0 | +4 points (2 drops) |
| Shot of Vodka | 0 | +4 points (2 drops) |
| Shot of Rum | 0 | +4 points (2 drops) |
| Shot of Whiskey | 0 | +4 points (2 drops) |
| Shot of Red / White Wine | 0 | +2 points (1 drop) |
| Glass of Red / White Wine | 0 | +12 points (6 drops) |
| Beet Nightcap | 0 | +12 points (6 drops) |
| Every other cocktail (Glowberry Gold Rush, Nether Fireball Shot...) | 0 | +12 points (6 drops) |

(2 points = 1 icon.) Set per item in `core/GoldenBonus.java`.

---

## 5. Bartenders only

Only players with the **Bartender** job can use the **Fermenter** and the **Bartending Table** (see `Jobs.md`; `occupation/JobEvents.onRightClickBlock`). Everyone else gets "Only Bartenders can use ..." and the menu doesn't open. Creative ignores it. The **Juicer** is open to everyone. In the Bartender's Guide: Information > Player Debuffs.

## For developers

| What | File |
|---|---|
| Juices (fruit, color, item id) | `src/main/java/com/alexh/capitalism/block/JuiceType.java` |
| Juicer rules | `block/JuicerRecipe.java`, `block/JuicerBlockEntity.java` |
| Liquors (ingredient, color, item id) | `block/LiquorType.java` |
| Fermenter rules | `block/FermenterRecipe.java`, `block/FermenterBlockEntity.java` |
| Shots, Bitters, Cocktails | `block/BartenderRecipe.java` → `buildRecipes()` |
| Drink items | `core/ModItems.java` (`SHOTS`, `BITTERS`, `BEET_NIGHTCAP`) |
| Machine times | `core/CapitalismConfig.java` (`juicerTicks`, `fermenterTicks`), or `runs/server/config/capitalism-server.toml` in-game |
| Names | `src/main/resources/assets/capitalism/lang/en_us.json` |
| Bartender's Guide screen | `client/BartendersGuideScreen.java` |
| `/guides` screen (list of guides) | `client/GuidesScreen.java`, registered in `client/ClientModEvents.java` |

**Adding a juice or liquor:** add a line to `JuiceType` / `LiquorType`, a name in `en_us.json`, and an `items/<id>.json`. Copy an existing bucket file and change the `value` color; it's the juice's hex color as a decimal number.

**Adding a cocktail:** register the drink item in `ModItems` (copy `BEET_NIGHTCAP`), add its name to `en_us.json` and an `items/<id>.json` + texture, then add a line in `BartenderRecipe.buildRecipes()`:

```java
new BartenderRecipe(ModItems.BEET_NIGHTCAP, 1, 1,   // result, how many, glass bottles
    () -> ModItems.JUICE_BUCKETS.get(JuiceType.BEETROOT).get(),
    () -> ModItems.JUICE_BUCKETS.get(JuiceType.BEETROOT).get(),
    () -> ModItems.JUICE_BUCKETS.get(JuiceType.SIMPLE_SYRUP).get(),
    () -> ModItems.BITTERS.get(),
    () -> ModItems.SHOTS.get(LiquorType.TEQUILA).get())
```

**Colors on the machines:** one gray texture is tinted per juice or liquor, so the Juicer window, the Fermenter jug, the menu gauges and the bucket icons all use the colors in `JuiceType` / `LiquorType`.

## Testing
`player/FoodDrinkTests` (run with `gradlew runGameTestServer`) eats / drinks every meal and drink with effects the real way and checks the effects, levels and times it gives and what it hands back; the gold bars (cocktails, shots, Gourmet food); every special effect that can run on a server (Scholar's Focus, Green Thumb Aura, Ender Leap, Luminescent Aura, Magma Trail, Phase Step, Overworld Mojito, Phantom's Nightcap, Zombie's Hangover Cure + Cooks eating raw food, Berserker's Rage, Dolphin's Grace skimming, Life Siphon, Spatial Dislocation, Fortune Boost); and that no NPC shop sells anything with effects. Not covered: Lava Swimmer (client movement) and Treasure Sight (needs a real shipwreck).

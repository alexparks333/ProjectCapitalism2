# Cook

Everything the **Cook** job has: the kitchen (Sushi Table, Cooking Table, Oven, Sink, plates), every sushi and Oven meal with its recipe, cook time, food values and effects, Gourmet food and the golden food bar, Rice, and the Cook's Guide. (Moved here from `Jobs.md`, which keeps the job list and the server-wide debuffs.)

## Where it is in the code
- **Job:** `occupation/Job.java` (COOK), what it does in `occupation/JobEvents.java` (Cook-only stations, won't eat raw food).
- **Recipes:** `block/KitchenRecipes.java`: SUSHI (Sushi Table), COOKING (Cooking Table, exact slots clockwise from the top middle), the Oven bakes (`Baking(input, result, cookSeconds, burnSeconds)`), `isGourmet`.
- **Items and food values:** `core/ModItems.java` (`gourmet(...)`, `sushi(...)`, every Uncooked / cooked meal, BURNT_FOOD, SQUASHED_SUSHI). Diet split per food: `player/DietValues.java`.
- **Stations:** `block/AssemblyTableBlock` (+Entity) / `menu/AssemblyTableMenu` / `client/AssemblyTableScreen` (Sushi Table AND Cooking Table), `block/OvenBlock` (+Entity) / `menu/OvenMenu` / `client/OvenScreen`, `block/SinkBlock`.
- **Plates:** `block/PlateBlock` (+Entity), `item/PlateItem`, `client/PlateRenderer`.
- **Gourmet food / gold drumsticks:** `occupation/GourmetFood.java`; the normal-food debuff: `occupation/FoodNerf.java` (see `Jobs.md`).
- **Meal effects:** `core/ModEffects.java` (Lava Swimmer, Ender Leap, Green Thumb, Scholar's Focus, Iron Stomach...), behaviour in `player/MealEffects.java`, client keys in `client/ClientMealEffects.java` (Ender Leap -> `network/EnderLeapPacket`).
- **Rice:** `block/RiceBlock.java` + world gen in `data/capitalism/worldgen/` and `data/capitalism/neoforge/biome_modifier/`.
- **Guide:** `client/CooksGuideScreen.java` (Kitchen + Recipes), unlocks in `guide/Discoveries.java`, silhouettes `client/Silhouettes.java`, crafting grids `client/HowToMake.java`.
- **Tooltips** (Gourmet star, Hunger drumsticks, Diet): `client/ClientModEvents.onItemTooltip`.
- **Textures:** PowerShell scripts in the scratchpad (`make_kitchen_*.ps1`, `make_sushi*.ps1`, `make_meals*.ps1` with `meal_lib.ps1` / `pie_lib.ps1`, `make_pot_pie.ps1`, `make_plates*.ps1`, `make_rice*.ps1`).

## Rules we decided on
- **Names:** a dish keeps its name through the Oven: "Uncooked X" bakes into "X" (no other renaming).
- Every Gourmet food stacks to **16**.
- Burnt time is about **half** the cook time.
- Cook-made food keeps its full saturation; normal food is cut to 1/2 (`Jobs.md`).
- A Cook can eat raw food only with **Iron Stomach** (Zombie's Hangover Cure, a Bartender drink).

## The Cook job
- **Pro: Cook-made vanilla food.** Ordinary food a Cook makes is **Gourmet**: anything cooked in **a Cook's own Furnace, Smoker or Blast Furnace** (the one they placed, while they're online) is Gourmet **right in the output slot**; cooked food a Cook takes out of anyone's furnace (click or shift-click) becomes Gourmet; and so does food they craft (bread, pumpkin pie, stews, cookies...). A furnace's new food always matches what's already in its output slot so it never stalls (ordinary porkchops already sitting there? it keeps making ordinary ones until they're taken out). It **escapes the food debuff** (full vanilla hunger and saturation: a Cook's Cooked Porkchop fills 4 drumsticks, not 2), shows **★ Gourmet**, and overflows into gold drumsticks like other Gourmet food. It doesn't stack with ordinary food of the same kind. In your inventory it has a **tiny gold "G"** in the bottom-left corner of its icon. Food pulled out by a hopper isn't Cook-made (nobody took it). Code: `occupation/CookMadeFood`, `mixin/AbstractFurnaceBlockEntityMixin` (the furnace itself), `mixin/AbstractFurnaceMenuMixin` (shift-click), `mixin/ItemMixin` (click, crafting), `FoodNerf.fullFood`.
- **Pro:** makes Gourmet food (sushi at the Sushi Table, baked meals from the Cooking Table + Oven), which escapes the food debuff and feeds the golden food bar (see below).
- **Decided:** Gourmet / Cook-made meals keep their FULL hidden saturation (or more), so they keep you full much longer. Normal food has its saturation cut along with its hunger points (see below). That difference is a big part of why people buy from Cooks.
- **Con:** can't eat raw meat or fish: raw beef, porkchop, chicken, mutton, rabbit, cod, salmon, tropical fish, pufferfish, rotten flesh, spider eye. Trying says "You're a Cook: you won't eat that raw!" (creative ignores it). List: `JobEvents.RAW`.
- **Cook-only stations** (others get "Only Cooks can use the Oven." etc.; creative ignores it):
  - **Sushi Table**: 4 ingredient slots in a row on a bamboo mat, and a red **ROLL** button under it, which is a **timing game**: click and **hold** ROLL, the bar fills up (a different speed every attempt, 0.85-1.5 s) while the bamboo mat rolls over the ingredients (a white rice roll), and a **green zone** sits somewhere in the back half of the bar (a new spot AND a new width every attempt, from a tiny 8% to a generous 20% of the bar; the button says "NOW!" while you're in it). **Let go in the green** = the sushi is made ("Perfect!", bamboo sound). **Too early OR too late** (or holding to the end) = **Squashed Sushi** ("Too early!" / "Too late!", squish sound): the ingredients are used up. Results go into the table's own output slot (kept in the table, so closing the menu never loses it). The button is grey when the ingredients don't make anything or the output slot is full. Recipes: see Sushi below. Crafted from `KCK / PPP / B B` (K = dried kelp, C = raw cod, P = bamboo planks, B = bamboo). Axe.
  - **Cooking Table**: 5 ingredient slots in a circle on a plate, for putting a (non-sushi) dish together. Instant (no minigame). The slots count **clockwise from the top middle** (top, right, bottom right, bottom left, left), and every dish needs each ingredient in its exact slot. Makes the *uncooked* dish that goes in the Oven. Crafted from `SSS / PTP / PPP` (S = smooth stone slab, T = crafting table, P = any planks). Axe.
  - **Oven**: 3 dish slots stacked on the left, each with its own arrow and meal slot, plus one fuel slot. Every row bakes at the same time while the fire burns, each meal at its own pace; a row pauses if its meal slot is full. A soft bell **dings** each time a meal comes out. Fuel: coal/charcoal (1600 ticks), coal block (16000), blaze rod (2400), logs/planks (300), sticks (100). The door window glows and the chimney smokes while it's lit (light level 13). Hoppers: dishes in the top, fuel in the sides, meals out the bottom. Crafted from `BBB / BFB / BIB` (B = bricks, F = furnace, I = iron ingot). Pickaxe.
  - **Burnt food:** Gourmet meals can be overcooked. A finished meal left in the Oven's meal slot while the fire is burning turns into **Burnt Food** after that meal's **burn time** (about half its cook time; listed per meal in the Oven meals table), the whole stack, with a hiss. Burnt Food can be eaten in a pinch for **half a drumstick** (1 hunger, almost no saturation) and doesn't change your diet at all. That row stops until it's taken out. As a meal gets close to burning its slot pulses red, faster and faster. If the fire goes out, nothing burns.
  - **Recipes so far:** the Sushi Table has 16 sushi (below), and the Cooking Table + Oven have their meals listed under **Oven meals** below. Recipes go in `block/KitchenRecipes.java`, one line each.
  - Code: `AssemblyTableBlock` (+Entity, `AssemblyTableMenu`, `AssemblyTableScreen`; one class for both tables), `OvenBlock` (+Entity, `OvenMenu`, `OvenScreen`). Textures: scratchpad `make_kitchen_gui.ps1`, `make_kitchen_blocks.ps1`, models `make_kitchen_json.sh`.

## Cook's Guide (in game)
Type **`/guides`** and click **Cook's Guide** (oven icon). **Only Cooks see it** in the list (`/guides` only shows the guides for your jobs; the General Guide is for everyone). Three sections:
- **Kitchen:** every Cook item in two tabs, **Blocks** (Sushi Table, Cooking Table, Oven, Sink) and **Accessories** (Silver/Golden Plate, Dirty Plate, Rice Seeds, Cooked Rice, Burnt Food, Squashed Sushi), as **black silhouettes until you've made one**. Click one: the right side shows **How to make** (the crafting grid, or for things that aren't crafted a row of icons and a note, e.g. "Smelt Rice in a Furnace or Smoker"), and once unlocked its name and what it does. Unlocks by crafting it (blocks, plates), smelting it (Cooked Rice), picking it up (wild Rice / Rice Seeds), picking up a dirty plate, or getting it out of the Oven / Sushi Table (Burnt Food, Squashed Sushi). The grids are hand-written in `CooksGuideScreen.buildKitchen`: keep them matching `data/capitalism/recipe/`.
- **Information:** one tab, **Player Debuffs** (Weaker Food, Locked Kitchen, No Serving: what everyone without the Cook job can't do), as clickable title boxes like the other guides.
- **Recipes:** four tabs, **Sushi** (plain sushi), **Specialty Sushi** (sushi with effects, e.g. Ocean Sovereign Roll), **Food** (plain Cooking Table dishes) and **Specialty Food** (Cooking Table dishes with effects, e.g. Fiery Mushroom Bowl), each with a grid of dishes. Click one for its recipe:
  - Sushi shows the slots in order (numbered slots first, the rest any order).
  - Cooking Table dishes show the same round plate as the table (rim, ring and shine) with each ingredient in its exact slot, then Uncooked dish > Oven > meal, the bake time and when it burns.
  - Both show the drumsticks and any effects.
- Built straight from `KitchenRecipes` and the items, so new dishes appear on their own. Only the Kitchen lines are hand-written (`CooksGuideScreen.buildKitchen`); keep them matching this file.
- **Undiscovered recipes (Smash Bros style):** anything you haven't made yet shows as a **black silhouette**, and its page says **"???"** instead of the name, with its drops / drumsticks / effects hidden ("Make it once to unlock its name and info!"). The **recipe itself still shows** (ingredients aren't hidden), so you can work out how to make it. Make it **once** (take it out of the machine's result slot) and the picture, name and info unlock for good. Per player, kept through death.
  - Counts as "made": crafting it (blocks, plates), smelting it (Cooked Rice), taking it out of the Juicer / Fermenter bucket slot, the Bartending Table result, the Sushi Table output, the Cooking Table result, or an Oven meal slot, by hand (clicking or shift-clicking). Pulling it out with a hopper doesn't count. Buying it doesn't count.
  - Ops: `/discoveries all [player]` unlocks everything, `/discoveries reset [player]` locks it all again.
  - Code: `guide/Discoveries.java` (+ `DiscoveryData`, `ClientDiscoveries`, `network/DiscoverySyncPacket`), silhouettes drawn by `client/Silhouettes.java` (reads each item's model and draws its texture layers in black).
- **Esc** goes back to the Guides screen. Code: `client/CooksGuideScreen.java`.

## Sushi (Sushi Table, Cooks only)
Every roll puts **Cooked Rice in slot 1** and **Dried Kelp in slot 2** (when it has kelp); the other ingredients go in the remaining slots in any order. All sushi is **Gourmet** (gold "★ Gourmet" on the tooltip): full strength, full saturation, not hit by the 1/2 food debuff. 1 hunger point = half a drumstick. Diet = how the roll counts toward Vegetables / Fruit / Protein / Carbs.

| Sushi | Slots (in order) | Hunger | Saturation | Diet |
|---|---|---|---|---|
| Salmon Nigiri | Cooked Rice, Raw Salmon | 5 (2.5) | 0.6 | 60% protein, 40% carbs |
| Cod Nigiri | Cooked Rice, Raw Cod | 5 (2.5) | 0.6 | 60% protein, 40% carbs |
| Tropical Nigiri | Cooked Rice, Tropical Fish | 4 (2) | 0.5 | 60% protein, 40% carbs |
| Puffer Nigiri | Cooked Rice, Pufferfish | 4 (2) | 0.6 | 60% protein, 40% carbs |
| Cod Roll | Cooked Rice, Dried Kelp, Raw Cod | 6 (3) | 0.6 | 50% protein, 40% carbs, 10% veg |
| Kelp Roll | Cooked Rice, Dried Kelp, Carrot | 5 (2.5) | 0.5 | 50% veg, 50% carbs |
| Tropical Roll | Cooked Rice, Dried Kelp, Tropical Fish, Sweet Berries | 7 (3.5) | 0.6 | 35% protein, 30% carbs, 25% fruit, 10% veg |
| Rainbow Roll | Cooked Rice, Dried Kelp, Raw Salmon, Raw Cod | 8 (4) | 0.7 | 60% protein, 30% carbs, 10% veg |
| Puffer Roll | Cooked Rice, Dried Kelp, Pufferfish, Carrot | 7 (3.5) | 0.7 | 40% protein, 35% carbs, 25% veg |
| Vegetable Roll | Cooked Rice, Dried Kelp, Brown Mushroom, Carrot | 6 (3) | 0.6 | 60% veg, 40% carbs |
| Baked Salmon Roll | Cooked Rice, Dried Kelp, Cooked Salmon, Cooked Salmon | 12 (6) | 0.8 | 70% protein, 25% carbs, 5% veg |
| Fruit Roll | Cooked Rice, Dried Kelp, Sweet Berries, Melon Slice | 6 (3) | 0.4 | 50% fruit, 45% carbs, 5% veg |
| Steak Roll | Cooked Rice, Dried Kelp, Steak, Steak | 14 (7) | 0.8 | 75% protein, 20% carbs, 5% veg |
| Cooked Potato Roll | Cooked Rice, Dried Kelp, Baked Potato, Baked Potato | 11 (5.5) | 0.6 | 90% carbs, 10% veg |
| Sweet Bread Dessert Roll | Cooked Rice, Cocoa Beans, Bread, Bread | 10 (5) | 0.5 | 90% carbs, 10% fruit |
| Ocean Sovereign Roll | Cooked Rice, Dried Kelp, Turtle Scute, Tropical Fish | 9 (4.5) | 0.7 | 50% protein, 40% carbs, 10% veg. **Dolphin's Grace for 1:00 + Water Breathing for 1:30** (very fast swimming, no running out of air) |

Balance notes: a roll is always worth more than its main ingredients eaten normally (e.g. 2 steaks fill 12 with the debuff; the Steak Roll fills 14 and keeps you full much longer), so Cooks add real value without the rolls being crazy. The big meals are the two-main rolls (Steak, Baked Salmon, Potato, Sweet Bread); the small ones (Nigiri, Kelp) are snacks. Kelp is only a pinch of vegetables.

**Squashed Sushi:** what you get when the ROLL is let go outside the green zone (too early or too late) or held to the end. Inedible; the ingredients are gone.

Code: recipes `KitchenRecipes` (SUSHI), items + food values `ModItems` (sushi(...)), diet groups `DietValues`. Icons: scratchpad `make_sushi.ps1`.

## Gourmet food and the golden food bar
Gourmet food (sushi and Oven meals) fills your normal food bar first; the drumsticks that **don't fit** become **gold drumsticks** (they drain first, like the Bartender's gold water). How much gold depends on how it's served:

| Served | Gold from what doesn't fit | Steak Roll (7 drumsticks) on a full bar |
|---|---|---|
| Eaten by hand | x0.5 (`gourmetGoldHand`) | 3.5 gold drumsticks |
| Off a **Silver Plate** | x1 (`gourmetGoldSilverPlate`) | 7 gold drumsticks |
| Off a **Golden Plate** | x1.5 (`gourmetGoldGoldenPlate`) | 10 (the gold bar tops out at 10, or 11 with the bonus drumstick) |

Gold always goes first: while you have gold drumsticks they keep your normal food bar topped up (including the bonus drumstick from a Balanced diet), so the normal drumsticks only start going down once the gold is all gone. Gold water drops work the same way.

**Gold rush (a quick heal):** saturation (the hidden fullness behind vanilla's fast healing) only comes from filling the normal bar, like vanilla (the Balanced diet's bonus drumstick counts as part of it). Gourmet food eaten while the bar is already full only makes gold, so it gives no saturation of its own; instead each bite gives **1.5 seconds of fast healing** (saturation set to 6 for 30 ticks, then back to 0). It doesn't stack: if you still have saturation from a normal meal, nothing changes. After that you heal at the normal rate for a full bar. Code: `ThirstData.goldOnlyMeal` (by hand: `ModServerEvents`; off a plate: `GourmetFood.eatFromPlate`).

Gourmet food can be eaten even with a full food bar (like a golden apple), so a good meal is always worth it. All Gourmet food (sushi and Cooking Table / Oven meals) **stacks to 16**. Going to a restaurant and eating off a Cook's plate gets you the most for your money. Code: `occupation/GourmetFood.java`.

## Oven meals
Each meal comes in three versions: put the ingredients together at the **Cooking Table** to get the **Uncooked** dish (can't be eaten), bake that in the **Oven** to get the **Gourmet** meal, and leave the meal in the hot Oven past its burn time (about half the cook time) and it becomes **Burnt Food** (one shared item for every burnt meal; edible for half a drumstick, no diet effect). Cooking Table slots go clockwise from the top middle, and each ingredient must be in its exact slot; a dish with fewer than 5 ingredients fills the first slots and leaves the rest empty. A Milk Bucket leaves its empty bucket behind in the slot. Some dishes (like Fruit Salad) skip the Oven: the Cooking Table makes them already Gourmet.

| Meal | Cooking Table (top, right, bottom right, bottom left, left) | Cook time | Burns after | Hunger | Saturation | Diet |
|---|---|---|---|---|---|---|
| Chicken Pot Pie | Raw Chicken, Raw Chicken, Carrot, Bread, Potato | 15 s | 8 s | 14 (7 drumsticks) | 0.8 | 40% protein, 35% carbs, 25% vegetables |
| Loaded Baked Potato | Potato, Raw Beef, Milk Bucket (bucket back), -, - | 12 s | 6 s | 10 (5 drumsticks) | 0.7 | 45% protein, 55% carbs |
| Chicken and Rice | Raw Chicken, Raw Chicken, Cooked Rice, Cooked Rice, - | 15 s | 8 s | 12 (6) | 0.7 | 55% protein, 45% carbs |
| Lamb Chops | Raw Mutton, Bone, Raw Mutton, Bone, Potato | 18 s | 9 s | 14 (7) | 0.8 | 75% protein, 25% carbs |
| Seafood Pasta | Raw Salmon, Raw Cod, Wheat, Wheat, Wheat | 14 s | 7 s | 12 (6) | 0.7 | 45% protein, 55% carbs |
| Squid Ink Pasta | Raw Salmon, Ink Sac, Wheat, Wheat, Wheat | 14 s | 7 s | 12 (6) | 0.7 | 40% protein, 60% carbs |
| Fruit Salad | Melon Slice, Apple, Melon Slice, Wheat, - | no Oven (made Gourmet at the Cooking Table) | - | 8 (4) | 0.5 | 75% fruit, 25% carbs |
| Seafood Chowder Bowl | Raw Cod, Raw Salmon, Kelp, Milk Bucket (bucket back), Bowl | 14 s | 7 s | 12 (6) | 0.7 | 60% protein, 30% carbs, 10% vegetables |
| Fiery Mushroom Bowl | Magma Cream, Raw Porkchop, Crimson Fungus, Nether Wart, Bowl | 16 s | 8 s | 10 (5) | 0.6 | 50% protein, 40% vegetables, 10% carbs |
| Sweet Berry Tart | Sweet Berries, Sweet Berries, Sugar, Wheat, Egg | 12 s | 6 s | 8 (4) | 0.5 | 50% fruit, 40% carbs, 10% protein |
| Stuffed Pumpkin | Pumpkin, Cooked Mutton, Carrot, Potato, Bread | 20 s | 10 s | 16 (8) | 0.8 | 35% vegetables, 35% protein, 30% carbs |
| Golden Glazed Feast | Golden Carrot, Raw Beef, Honey Bottle (bottle back), Brown Mushroom, Bowl | 22 s | 11 s | 24 (12) | 1.0 | 50% protein, 25% vegetables, 25% carbs |
| Miner's Casserole | Glow Berries, Cooked Porkchop, Baked Potato, Brown Mushroom, Bowl | 18 s | 9 s | 14 (7) | 0.8 | 45% protein, 35% carbs, 10% vegetables, 10% fruit |
| Garden Veggie Calzone | Bread, Red Mushroom, Carrot, Potato, Beetroot | 15 s | 8 s | 12 (6) | 0.7 | 60% vegetables, 40% carbs |
| Nether Dragon Pot Pie | Chorus Fruit, Blaze Powder, Raw Porkchop, Pumpkin, Bread | 18 s | 9 s | 14 (7) | 0.8 | 40% protein, 30% carbs, 20% vegetables, 10% fruit |
| Honey-Glazed Wing Roast | Honey Bottle (bottle back), Raw Chicken, Carrot, Sweet Berries, Feather | 15 s | 8 s | 12 (6) | 0.7 | 60% protein, 15% vegetables, 15% fruit, 10% carbs |
| Beef Skewer | Raw Beef, Apple, Raw Beef, Stick, Stick | 12 s | 6 s | 12 (6) | 0.8 | 80% protein, 20% fruit |
| Chicken Skewer | Raw Chicken, Apple, Raw Chicken, Stick, Stick | 12 s | 6 s | 10 (5) | 0.7 | 80% protein, 20% fruit |
| Lamb Skewer | Raw Mutton, Apple, Raw Mutton, Stick, Stick | 12 s | 6 s | 11 (5.5) | 0.8 | 80% protein, 20% fruit |
| Fish Skewer | Raw Cod, Apple, Raw Cod, Stick, Stick | 10 s | 5 s | 10 (5) | 0.7 | 80% protein, 20% fruit |
| Sugar Rush Berry Crepe | Wheat, Egg, Milk Bucket (bucket back), Sugar, Sweet Berries | 10 s | 5 s | 8 (4) | 0.5 | 50% carbs, 35% fruit, 15% protein |
| Farmer's Harvest Loaf | Wheat, Carrot, Potato, Beetroot, Bone Meal | 16 s | 8 s | 12 (6) | 0.7 | 50% vegetables, 50% carbs |
| Scholar's Brain Stew | Slime Ball, Spider Eye, Bone Meal, Sugar, Bowl | 16 s | 8 s | 8 (4) | 0.6 | 60% protein, 40% carbs |
| Campfire Chili | Raw Beef, Beetroot, Beetroot, Brown Mushroom, Bowl | 45 s (simmered extra long: the longest bake) | 15 s | 12 (6) | 0.7 | 55% protein, 35% vegetables, 10% carbs |
| Frosty Melon Sorbet | Melon Slice, Snowball, Sugar, Sweet Berries, Bowl | no Oven (made Gourmet at the Cooking Table) | - | 6 (3) | 0.4 | 75% fruit, 25% carbs |

**Names:** a dish keeps its name through the Oven: "Uncooked X" bakes into "X".

**Leftovers:** meals made with a Bowl (Seafood Chowder, Fiery Mushroom Bowl, Golden Glazed Feast, Miner's Casserole, Scholar's Brain Stew, Campfire Chili, Frosty Melon Sorbet) give the empty bowl back when eaten, and skewers give back a Stick, by hand or off a plate.

**Meal effects** (by hand or off a plate):
- **Fiery Mushroom Bowl:** Fire Resistance for 4 minutes, plus **Lava Swimmer** for the first minute: you move **3x as fast** through lava (our own effect, with a lava-drop icon). Code: `core/ModEffects.java`, the movement in `player/MealEffects.java` (runs on your own client, since players move client-side).
- **Miner's Casserole:** Night Vision and Haste for 2 minutes.
- **Nether Dragon Pot Pie:** Fire Resistance and **Ender Leap** for 3 minutes. With Ender Leap, **sneak + hold jump** to blink up to 8 blocks forward (the way you're facing), like a short Ender Pearl: it stops before walls, can hop up one block, never gives fall damage, and costs half a hunger point each time (gold drumsticks pay first). Once a second while you hold the keys; no leaping on an empty food bar. Code: the keys in `client/ClientMealEffects.java` -> `EnderLeapPacket` -> `MealEffects.enderLeap`.
- **Ocean Sovereign Roll (sushi):** Dolphin's Grace for 1:00 and Water Breathing for 1:30.
- **Sugar Rush Berry Crepe:** Speed III and Jump Boost II for 45 seconds.
- **Farmer's Harvest Loaf:** **Green Thumb Aura** for 1 minute: once a second, every crop, sapling, stem, berry bush, cocoa pod and rice plant within 5 blocks (2 up or down) has a 1 in 4 chance of a free bone meal (green sparkles). A wheat field around you is ripe in about 10 seconds; saplings turn into trees. Grass is left alone (no flower spam). Only after eating; holding the loaf does nothing.
- **Scholar's Brain Stew:** **Scholar's Focus** for 3 minutes: every mob you kill drops **double experience**.
- **Campfire Chili:** **Warmed Up** for 5 minutes: the cold can't reach you (body heat, `Wellness.md`).
- **Frosty Melon Sorbet:** **Cooled Off** for 5 minutes: you can't get hot (never above 70).

The chili and sorbet textures (recolored chowder bowls) and their effect icons are drawn by scratchpad `meals.ps1`.

Textures: scratchpad `make_pot_pie.ps1` (pot pie), `make_meals.ps1` and `make_meals2.ps1` `make_meals3.ps1`, `make_meals4.ps1` (the rest; shared helpers in `meal_lib.ps1` and `pie_lib.ps1`; effect icons in `textures/mob_effect/`). Ocean Sovereign Roll: `make_sushi2.ps1`. Pastas and Chicken and Rice come in a red baking dish, Lamb Chops on a plate, Fruit Salad in a bowl.

## Plates (Cooks only)
- **Silver Plate** = crafting table: 5 Iron Ingots in a minecart shape with 1 Iron Nugget in the gap (`INI / III`). **Golden Plate** = the same with Gold Ingots and a Gold Nugget. Expensive on purpose. (Anyone can craft them, but only Cooks can place, break or serve on them.)
- **Placing:** only Cooks can set a plate down, on top of any block that can hold something in its middle (full blocks, slabs, fence or wall posts...), so any "table" works. Not on a Sink. Non-Cooks get "Only Cooks can set out plates."
- **Breaking:** only Cooks can break plates (for anyone else it simply won't mine). Any food on it drops too. Pickaxe.
- **Serving:** a Cook right-clicks the plate holding Gourmet food to put down **one meal**. A plate holds one meal at a time ("There's already a meal on this plate."). The meal shows lying on the plate.
- **Taking it back:** a Cook sneak-right-clicks with an empty hand.
- **Eating:** ANYONE right-clicks the plate to eat the meal (instantly, with crumbs at your mouth, crunching and a burp). A gold message says what you got, e.g. "Steak Roll: +3 drumsticks, +2 gold (Silver Plate)", or "you're completely stuffed (gold bar full)" if there was no room. The gold bar is as long as your food bar (10 gold drumsticks, or 11 with a Balanced diet's bonus drumstick), so a meal can only add as much gold as there's room for. It counts toward their diet and as a treat (Bored), and gives gold at the plate's rate.
- **Dirty plates:** once the meal is eaten the plate is empty and **dirty** (crumbs and sauce smudges show on it). A Cook can't put new food on it ("This plate is dirty..."). The cycle: serve one meal, it gets eaten, pick up the dirty plate, wash it in the Sink, put it back. The Cook picks it up with an **empty-hand right-click**: it becomes a **Dirty Silver/Golden Plate** item, which can't be placed. An empty-hand right-click also picks up a clean, empty plate. (Breaking it works too.)
- **Sink:** right-click it with a Dirty Plate to get the clean plate back (splash). That's all it does. Anyone can use it. Crafted ` I / SCS` (iron ingot faucet over a cauldron between smooth stone). Axe. Code: `block/SinkBlock.java`.
- Payment isn't built in: sell servings however you like (a Shopfront next to the table, `/pay`, trust...).
- **Look:** a solid, thick octagon-shaped dish with a raised rim (silver or gold, with a shine); dirty ones show crumbs and sauce.
- Code: `block/PlateBlock.java` (+Entity), `item/PlateItem.java`, `client/PlateRenderer.java`, `block/SinkBlock.java`. Textures: scratchpad `make_plates.ps1` (item icons), `make_plates2.ps1` (block textures), `make_dirty.ps1`; plate model `make_plate_model.sh`; sink model `make_sink_json.sh`.

## Training Feed (for Pet Trainers)
Cook-only crafting (`capitalism:job_shaped` "cook", `training_feed.json`): any seed next to any raw meat or fish makes
**6 Training Feed**. Only Pet Trainers can use it (an animal fed it follows them for a few seconds: `PetTrainer.md`).
In the Cook's Guide under Accessories. The NPC Cook sells it too (`NPCs.md`). A seed with **Rotten Meat** makes 6 too
(`training_feed_from_rotten_meat.json`).

## Fridge and Freezer
Cooks (and Farmers) make them: they keep Meat Slabs and fine cuts fresh 2x / 6x longer while they have fuel (coal /
charcoal; ice / snowballs). See `Farmer.md` > Freshness. In the Cook's Guide: Kitchen > Blocks (Fridge, Freezer) and
Accessories (fine cuts, Rotten Meat).

## Dry-aging (`farmer/Freshness`, `ColdStorageBlockEntity`)
A fine cut kept in a **running Fridge** (not a Freezer: freezing stops it) ages (`ModComponents.AGING`, ticks of
running-Fridge time): **Dry-Aged** after 8 minutes (`DRY_AGED`), **Prime Dry-Aged** after 16 (`PRIME_AGED`). It keeps
rotting the whole time (slower, as in any Fridge), so aging is a gamble. The tooltip shows "Dry-aging: 45%", then
"★ Dry-Aged (Prime: 30%)", then "★★ Prime Dry-Aged"; taken out, it stays aged. Aged cuts sear into better steaks.

## The Grill (`farmer/GrillBlock`, `GrillBlockEntity`, `GrillMenu`, `Sear`, `client/GrillScreen`, `client/SearGame`)
**Cooks only** (craft: 3 Iron Bars / Iron, Campfire, Iron / Iron, gap, Iron; pickaxe). Its screen: a kitchen brick wall,
the grill (**three cut slots** on the glowing grate, the fuel slot in the firebox door), a chalkboard of the doneness
temperatures, **Sear!**. Fuel: a Coal or Charcoal is **4 steaks** of heat, a Blaze Rod **6** (`COAL_SEARS`,
`BLAZE_SEARS`). Sear! puts every cut in the slots on the grill (one steak of heat each, as far as the heat goes:
`GrillMenu.onGrill`) and starts the **Busy Grill** game (the Grill glows and smokes):
- Each steak cooks at **its own pace** (3-5.4°F a second), so they come due at different times: juggle them.
- Each has a **doneness bar** under it (the bands, the goal in gold, a needle) and its letters + temperature.
  **PULL** it (its button, or Q / W / E) at **Medium Rare** (chicken: Well Done): it rests on the board 3-5°F more.
- Each needs **turning**: after 3-6 s on a side a **FLIP!** bubble pops up over it; flip it (click it, or 1 / 2 / 3)
  before the bubble's bar runs out (1.5 s), or it **goes up in flames: burnt** (Burnt Food).
- Esc pulls everything off; closing the menu mid-sear burns whatever's still on. Each steak's result goes to the server
  as it comes off (`GrillMenu.RESULT` + slot x 256 + its final temperature).
- Bands (after resting): under 120 Raw Inside, 120-129 Rare, **130-139 Medium Rare (the aim)**, 140-149 Medium, 150-159
  Medium Well, 160-174 Well Done, **175+ Burnt** (Burnt Food).
- The result: **Seared [cut]** (`seared_<id>`, 15 of them; textures from the raw cuts, browned with grill marks:
  `tools/make_grill_textures.ps1`, which also draws the Grill block). Its icon has its **doneness letters** in the
  top-left corner (R, MR, M, MW, WD; RAW in red; gold when it's cooked just right; `ClientModEvents`, an item
  decoration). Tooltip: its doneness ("★ Perfectly Cooked" for Medium Rare; chicken: Well Done) and its aging. It's
  **Cook-made**: Gourmet, gold drumsticks, plates.
- **Why cook it well** (`Sear.food`): the better it's cooked the more it fills, so the more gold it gives (and more off a
  plate). Base: beef / pork 8, mutton 7, chicken / rabbit 6, + the cut's rarity; then **Medium Rare +4** (and more
  saturation), Rare / Medium +2, Medium Well +1, Well Done +0 (chicken: Well Done +4, the only safe way); **Dry-Aged
  +2, Prime +4**; Raw Inside halves it. Customers who like it Well Done can still ask for it. **Undercooked** (chicken
  that isn't Well Done, or any meat Raw Inside) gives food poisoning (Hunger and Nausea). A **Prime Dry-Aged** cut seared
  just right (Medium Rare; chicken: Well Done) gives **Strength I for 2 minutes** (`Sear.strengthens`; on its tooltip).
- In the Cook's Guide: Kitchen > Blocks (Grill), Accessories (Seared steak, Dry-aged cuts).
- Tests: `cook_dry_aging_and_grill` (in `farmer/FarmerTests`: aging, heat, three steaks at once, walking away burns).

## Ingredient: Rice
- **Two items, like wheat:** **Rice Seeds** (tan husked grains) are what you plant; **Rice** (white grains) is the cooking ingredient and can't be planted.
- **Where:** wild (ripe) rice grows in small patches in **shallow water** in swamps, mangrove swamps, rivers and jungles (about one patch every 4 chunks there; only in newly explored land). Breaking it gives Rice and Rice Seeds.
- **Farming:** plant Rice Seeds in **shallow water**: a water source with dirt, grass, mud, farmland or clay under it and **air above** (1 block deep). Click the ground under the water to plant. Deeper water, or no water, and it won't stay. It stands in the water like seagrass and pokes up above the surface.
- **Growth:** 4 stages (shoots -> tall green -> heads forming -> golden), and the ripe golden heads show little **white rice speckles** so you can tell it's ready, about one stage every few minutes in daylight or light 9+. Bone meal pushes it a stage.
- **Drops (like wheat):** a ripe (golden) plant gives 1-3 Rice plus 0-3 Rice Seeds (more with Fortune); an unripe plant gives its Rice Seed back. Breaking it leaves the water.
- **Cooked Rice:** Rice has to be cooked before it goes into sushi. **Furnace** (10 s) or **Smoker** (5 s); not the Oven. Just an ingredient for now (not edible).
- Nori = vanilla **Dried Kelp** (no new item).
- Code: `block/RiceBlock.java`; models `rice_stage0-3` (24 tall, 16x32 textures); world gen `data/capitalism/worldgen/feature/wild_rice.json`, `placed_feature/wild_rice.json`, `neoforge/biome_modifier/wild_rice.json`. Scripts: scratchpad `make_rice.ps1`, `make_rice_json.sh`.


## Testing
`player/FoodDrinkTests` (run with `gradlew runGameTestServer`) eats / drinks every meal and drink with effects the real way and checks the effects, levels and times it gives and what it hands back; the gold bars (cocktails, shots, Gourmet food); every special effect that can run on a server (Scholar's Focus, Green Thumb Aura, Ender Leap, Luminescent Aura, Magma Trail, Phase Step, Overworld Mojito, Phantom's Nightcap, Zombie's Hangover Cure + Cooks eating raw food, Berserker's Rage, Dolphin's Grace skimming, Life Siphon, Spatial Dislocation, Fortune Boost); and that no NPC shop sells anything with effects. Not covered: Lava Swimmer (client movement) and Treasure Sight (needs a real shipwreck).

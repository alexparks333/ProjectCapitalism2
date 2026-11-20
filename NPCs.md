# Job NPCs (planned)

**NPCs fill in for missing players, and sometimes compete with them.** Each job can have an NPC shopkeeper. It is
always there while **no player with that job is online**, so job goods are always for sale, and on top of that it has
its own **35% chance each day** of coming to town anyway (all day, even with players of its job online), a little
competition for them (`NpcManager.inTownToday`, `DAILY_CHANCE`).

## Rules (decided)
- **Admins designate each job NPC's spot** with a command (e.g. `/npc setspot cook` where you stand).
- **No player with that job online:** the job's NPC appears at its spot and its shop is open.
- **A player with that job logs in:** the NPC stays for about **10 minutes**, then disappears. It comes back when the last
  player with that job logs out. Not on its town days: then it stays all day. Each job rolls separately, so some NPCs are
  in town and some aren't (the same all day; `/npc list` says "in town today").
- **Paid in gold** (nuggets / ingots / blocks, change given back, like `/pay`), through a **custom themed shop screen**
  (item list, prices, your discount, buy buttons).
- **NPCs charge extra** (around +50%) so buying from a real player with the job is always the better deal.
- **They sell their real job goods:** the Cook NPC sells actual Gourmet meals, the Weaponsmith NPC real swords, etc.
- **Every price goes through `economy/NpcPricing.npcPrice(fairNuggets, player)`** so Emerald-socketed armor gives its
  discount (7.5% per piece). See `CLAUDE.md`.

## Steps
1. **The NPC (done):** a shopkeeper entity, `/npc setspot <job>` / `/npc removespot <job>` / `/npc list`, and appearing /
   disappearing based on who's online (with the 10-minute grace period). Right-clicking it just says the shop opens soon.
2. **The shop screen (done):** right-click a job NPC whose job has stock. A market stall (striped awning, oak planks;
   `tools/make_npc_shop_gui.ps1`): a tab per section, up to 7 offers each (icon with its tooltip, name, NPC price in gold,
   **Buy**; **shift-click buys 4**), and **"Restocks in 17:51"** at the bottom (plus your Emerald discount, only if you have one). Prices you can't afford show red
   and their Buy button is greyed out. Paid with `GoldMoney.take` (change given back); the food goes to your inventory
   (or your feet if it's full). Only works while the NPC is still there and you're within 8 blocks.
   Code: `npc/NpcShops` (stock), `npc/NpcShopMenu` (buying), `client/NpcShopScreen`.
3. **Stock, one job at a time:** the **Cook** is stocked (below). The other jobs' NPCs still just say their shop opens soon.

## What they're like (step 1, built)
- **Look:** a villager (nose and all) in the job's outfit, holding something from the job across its folded arms:
  | Job | Outfit | Hat | Holds |
  |---|---|---|---|
  | Cook | white butcher's apron | white chef's hat | Mushroom Stew |
  | Bartender | librarian robes | the librarian's hat | a Potion |
  | Weaponsmith | leather smith's apron | dark leather hat | Iron Sword |
  | Toolsmith | toolsmith's apron | its cap | Iron Pickaxe |
  | Armorsmith | armorer's apron and welding mask | | Shield |
  Skins: `textures/entity/npc/<job>.png`, built by `tools/make_npc_skins.ps1`; drawn by `client/JobNpcRenderer`.
- **Completely silent.** Its job's name in gold above its head. Stands still at its spot and looks at nearby players.
- **Can't be killed by anything:** no damage of any kind (Creative players, `/kill`, the void, lava, fire, explosions,
  potions, mobs; mobs don't even target it). Can't be hit, pushed, leashed or put in a boat or minecart. No gravity, so it
  can't fall into lava or the void if the floor is dug out. Never despawns, drops nothing. The only ways it goes:
  `/npc removespot <job>`, or leaving on its own when a player with its job has been online 10 minutes.
- **Checks every 30 seconds** (`NpcManager.CHECK_TICKS`): spawns the NPC where it should be, removes it (a silent puff
  of smoke) when it should go. `/npc setspot` places it straight away. Only spots in loaded chunks are touched; the NPC
  also checks itself every 30 seconds. The 10-minute timer restarts if the server restarts.
- **Commands (ops):** `/npc setspot <job>` (where you stand, facing your way; moves it if it already had a spot),
  `/npc removespot <job>`, `/npc list` (each spot, and whether it's open, leaving soon or away).
- Code: `npc/JobNpc`, `npc/NpcManager`, `npc/NpcSpots` (saved spots), `core/ModEntities`.

## Prices
Stock lists the **fair** price (what a real player with the job might ask); the NPC charges
`NpcPricing.npcPrice` of it: **+50%**, then **7.5% off per Emerald-socketed armor piece** you wear.

## Limited stock
- Each offer has a **limited stock per restock, shared by every player** (`NpcShops.Offer.maxStock`): if one player buys
  the last one, it's sold out for everyone. **Everything restocks every 20 minutes** of game time (`NpcStock`, saved with
  the world, so restarting the server doesn't refill it early). The point: nobody can stock up and never visit the real
  Cook again.
- The shop shows **"3 left"** under each price (or **SOLD OUT** in red, Buy greyed out) and **"Restocks in 12:34"** at the
  bottom, live for everyone with the shop open. Shift-click buys up to 4, or what's left.
- The Cook: **1 of each Gourmet Meal, 3 of each Gourmet Bite** per restock.

### The Cook's shop
| Gourmet Meals (1 each) | Fair | NPC price |
|---|---|---|
| Golden Glazed Feast | 5 gold 3 nuggets | **8 gold** |
| Stuffed Pumpkin | 4 gold | **6 gold** |
| Lamb Chops | 3 gold 3 nuggets | **5 gold** |
| Chicken Pot Pie | 3 gold 3 nuggets | **5 gold** |
| Seafood Chowder | 2 gold 6 nuggets | **4 gold** |

| Gourmet Bites (3 each) | Fair | NPC price |
|---|---|---|
| Beef Skewer | 1 gold 3 nuggets | **2 gold** |
| Chicken Skewer | 1 gold | **1 gold 5 nuggets** |
| Vegetable Roll | 1 gold | **1 gold 5 nuggets** |
| Fruit Salad | 1 gold | **1 gold 5 nuggets** |
| Salmon Nigiri | 6 nuggets | **1 gold** |
| Cod Nigiri | 6 nuggets | **1 gold** |
| Kelp Roll | 6 nuggets | **1 gold** |

| Cooked (30 each, **Cook-made**: full strength, Gourmet) | Fair | NPC price |
|---|---|---|
| Steak, Cooked Porkchop | 2 nuggets | **3 nuggets** |
| Cooked Chicken, Mutton, Salmon, Cod, Baked Potato | 1 nugget | **2 nuggets** |

| Pets | Fair | NPC price |
|---|---|---|
| Training Feed x5 (`Offer.per`: one purchase gives 5; 8 per restock) | 6 nuggets | **1 gold** |
| Wheat x4, Wheat Seeds x8 (animal food; 16 bundles of each per restock) | 1 nugget | **2 nuggets** |
| Raw Beef, Raw Salmon (raw, NOT Cook-made or Gourmet; 16 each per restock) | 2 nuggets | **3 nuggets** |

The tabs are **Meals / Bites / Cooked / Pets**. **Nothing with effects is ever sold** (Miner's Casserole, Nether Dragon Pot Pie, Ocean Sovereign Roll, every Specialty drink...): only real Cooks and Bartenders make those.

### The Bartender's shop
Three tabs, **Cocktails / Shots / Water**. No Specialty drinks (they all have effects). 3 of each drink per restock.

| Cocktails (3 each) | Fair | NPC price |
|---|---|---|
| Classic Old Fashioned, Whiskey Sour, Watermelon Breeze, Sangria Pitcher, Tropical Kelp Cooler, White Wine Spritzer, Beetroot Velvet Sour | 1 gold | **1 gold 5 nuggets** |

| Shots (3 each) | Fair | NPC price |
|---|---|---|
| Tequila, Vodka, Rum and Whiskey Shots, Glass of Red Wine, Glass of White Wine | 6 nuggets | **1 gold** |
| Beet Nightcap | 1 gold | **1 gold 5 nuggets** |

| Water | Fair | NPC price | Stock |
|---|---|---|---|
| Purified Water (a bottle) | 1 nugget | **2 nuggets** | **75** |

### The smiths' shops (Weaponsmith, Toolsmith, Armorsmith)
All three are the same, each for its own gear. Two tabs, **Repair / Shop**.

**Repair** lists everything you carry (worn armor and offhand too) that the NPC can fix and that's damaged: the
Weaponsmith fixes weapons, the Toolsmith tools, the Armorsmith armor (the same items as their smith table's Forge;
axes at either the Weaponsmith or the Toolsmith). **Fix** gives back **30% of its max durability** (never past full).
No stock: repair as often as you pay for. It **doesn't sharpen** or remove dents (that's the Whetstones) and keeps the
item's rolled stats. Scroll the list if you carry more than 7. A real smith's Forge repair is a lot cheaper.

| One repair, by material | NPC price |
|---|---|
| Wood, leather | **2 gold** |
| Stone, copper, bow | **4 gold** |
| Iron, gold, chainmail, crossbow, turtle shell | **10 gold** |
| Diamond, trident, mace | **20 gold** |
| Netherite | **50 gold** |

| Shop (8 per restock) | Fair | NPC price |
|---|---|---|
| Sword / Pickaxe / Armor Whetstone (their own one) | 2 gold | **3 gold** |
| Bow (Weaponsmith only: bows are Weaponsmith-only to craft) | 2 gold | **3 gold** |
| Scythe (Toolsmith only: scythes are Toolsmith-only to craft; 4 per restock) | 4 gold | **6 gold** |
| Iron Butcher Knife (Toolsmith only: knives are Toolsmith-only to craft, Farmers need them; 4 per restock) | 2 gold 6 nuggets | **4 gold** |

Code: `npc/NpcRepairs` (what, how much, price), `NpcShopMenu.repair`, `client/NpcShopScreen` (the Repair tab).


## The Boat Master
Not a job NPC: a town NPC who is always there once placed with `/npc setspot boat_master`, selling boats and the Premium
boats (the Pirate Ship). See `Boats.md`.

## The Butcher
Not a job NPC: a town shopkeeper (`npc/Butcher`; he and the Boat Master are `npc/Shopkeeper`s) placed with
`/npc setspot butcher` (`/npc removespot butcher`; `/npc list` says whether he's in town today). Why: ten Cook meals need
a Farmer's fine cut (four of them specialties: Prime Rib Dinner, Comfort Soup, Night Watch Hotpot, Ironworker's Stew),
a Cook can never be a Farmer, and Farmer is a Fun job nobody may take. He makes those meals *sometimes* possible; a real
Farmer is still the steady, cheaper supply. Looks like a villager in the butcher's apron with a deep red cap, a Beef Rib
across his arms (`textures/entity/npc/butcher.png`, `tools/make_npc_skins.ps1`).

- **Comes to town on 35% of days** (`NpcManager.DAILY_CHANCE`, like the job NPCs; his own roll each Calendar day,
  `NpcManager.shopkeeperInTown`). The other days his spot stays empty.
- **4 random cuts a day** (`NpcShops.CUTS_A_DAY`, `Butcher.todaysCuts`: the same all day, new ones the next), in one
  **Cuts** tab. (A second tab is planned, not decided yet.) His menu's shop key is `butcher:<mask>` so the client shows
  the same 4 cuts and that day's stock is kept apart.

| Cut | Per restock (20 min) | Fair | NPC price |
|---|---|---|---|
| Common (Chuck, Brisket, Pork Shoulder, Belly, Mutton Shoulder, Chicken Leg Quarter, Rabbit Hind Legs) | 4 | 6 nuggets | **1 gold** |
| Better (Beef Round, Pork Ham, Mutton Leg, Chicken Breast, Rabbit Saddle) | 2 | 12 nuggets | **2 gold** |
| Rare (Beef Rib, Pork Loin, Mutton Rack) | 1 | 2 gold | **3 gold** |

Raw cuts give no effects, so the NPC rule holds. They start rotting the moment they're bought (`farmer/Freshness`
stamps them like a fresh cut). Prices go through `npcPrice` like every NPC.

## Health bars (`client/MobHealthBars`)
When a mob (or another player) loses health, a little bar floats over its head for 5 seconds after the last hit, then
fades: a red heart, the bar in a dark rounded frame (green, yellow under half, red under a quarter, with a shine on top
and faint quarter marks), and the health in numbers after it. What a hit takes flashes pale and then drains away
behind the bar, like a fighting game. Each hit pops out a number that springs up big, settles, arcs up and fades: red
"-4.5", gold "-9!" for a big hit (a third of its health or more), "☠ -6" for the killing blow, green "+2" for healing.
It sits above the mob's name if it has one and turns to face you. Only within 32 blocks, never over yourself or armor
stands. Client only (every mob's health is already sent to everyone).

## Mob levels (`core/TougherMobs`, `client/MobLevels`)
Every hostile mob spawns at a level, rolled once and saved with it: A mob that goes through a portal keeps its level and its wounds on the other side (`TougherMobs.onTravel`: the new copy would otherwise roll a fresh level at full health).

| Level | Chance (config) | Health | Hits | Loot | XP | Mark |
|---|---|---|---|---|---|---|
| **I** | the rest | +10% (`hostileMobHealthMultiplier`) | normal | normal | normal | grey **I** |
| **II** | 30% (`level2MobChance`) | +30% | ×1.25 | normal | ×1.4 | gold **II** |
| **Boss** | 10% (`bossMobChance`) | ×2 | ×1.75 | **double** | **double** | white **☠** (bigger) |

Bosses also get something by kind:
- **Skeletons** (and strays, bogged...) shoot 1.5x as fast (`mixin/AbstractSkeletonMixin`: the wait after each 20-tick
  bow draw is cut so the whole cycle takes 2/3 as long).
- **Ghasts** and **Blazes** shoot 1.5x as fast (`mixin/GhastAttackMixin`, `BlazeAttackMixin`: their attack goal runs an
  extra step every other tick).
- **Zombies** (husks, drowned, zombie villagers, zombified piglins) walk 1.5x as fast (`boss_zombie_speed` modifier).
- **Spiders** (cave spiders too) poison what they bite: Poison for 7 seconds.
- **Creepers**: just the Boss health and harder blast.

- The real bosses (Wither, Ender Dragon, Warden, Elder Guardian) are always Level I. Animals, villagers and golems have
  no level.
- Harder hits count for anything the mob does: a bite, its arrow, a creeper's blast.
- The mark floats over its head only up close: full within 8 blocks, fading out by 14, like a pet's name. It's drawn as
  part of the world, so walls hide it. The health bar sits above it when both show.
- The level is the mob's max-health modifier (`tougher_mobs`, `mob_level_2`, `mob_level_boss`), which the game already
  sends to every player, so the client knows it without anything extra. Mobs from before this keep Level I.

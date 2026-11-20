# Jobs

Every job is either **Essential** or **Fun** (`Job.Category`). Every player starts with **one of each**: 1 Essential job
and 1 Fun job, picked for good (a Wishing Star can add one more of a kind). Each job has pros and cons. Some jobs also lift a **server-wide passive debuff** that everyone lives with
(e.g. only a Cook's Gourmet food escapes the food debuff).

| Essential (what the server needs: food, gear, enchanting) | Fun |
|---|---|
| Cook, Weaponsmith, Armorsmith, Toolsmith, Enchanter | Bartender, Librarian, Pet Trainer, Designer, Farmer, Real Estate Agent |

Essential jobs have NPCs that stand in while nobody with the job is online (`NPCs.md`).

Code: `occupation/` (`Job` = the list and each job's category, `JobData` = a player's jobs and job slots, saved and kept through death, `JobEvents` = what the jobs do, `JobCommands`, `FoodNerf`).

## Picking jobs (`client/JobPickScreen`, `JobCommands.togglePicked`)
- **Everyone joins with no jobs.** On joining they're told to type **`/pick`**.
- **`/pick`** (anyone): Essential jobs on the left, Fun jobs on the right, each column showing its slots ("0 of 1
  picked"). Click a job and a bar asks **"Become a Cook? This is for good." [Yes] [No]**. A full column is greyed out.
  Hover a job for all its pros and cons.
- **A Cook can never be a Farmer, and a Farmer can never be a Cook** (`Job.clashesWith`), even with a Wishing Star slot or a Changing Star: the other one is greyed out on the job screen.
- **Jobs are for good:** clicking a job you hold does nothing (the server says so). Only a Changing Star changes them.
- **Slots:** 1 Essential and 1 Fun to start. A Wishing Star wished on the Wishing Well adds one of its kind, up to
  **2 each** (`JobData.MAX_SLOTS`), so 2 jobs can become 3 (or 4).

## The job drafts (`occupation/JobDraft`, `/admin draft essential|fun ...`)
How the server starts: players take turns picking, in an order the admin builds up. There are **two separate drafts**,
the **Essential** draft and the **Fun** draft, each with its own list. **No cap:** any number of players can take the
same job (five Cooks is fine; it just leaves a gap in the market). Ops only; `<kind>` = `essential` or `fun`.
- **`/admin draft <kind> add <player>`** puts a player (online) at the end of that list. Everyone is told ("Bob joins
  the Essential draft (#3)"). You can keep adding **while the draft runs**: they go to the back of the line.
- **`/admin draft <kind> start`** starts it: one player at a time, top of the list down.
- On your turn: everyone is told ("It's Bob's turn to pick an Essential job (2 of 10)"), you hear a bell and the job
  screen opens. Picking (with the usual "This is for good" Yes/No) hands the turn on. Anyone who already has no free
  slot of that kind is passed over by itself.
- **Once anyone is on a list, that kind of job is only picked through the draft** (even before it starts): everyone
  else's column is greyed out and the screen says who is picking. The other kind isn't held up (the Fun draft doesn't
  stop Essential picks, and the other way round).
- When the last player on the list has picked, that draft is **over** and anyone can /pick that kind again. (Adding
  someone after that starts a new list: `start` it again.)
- **`/admin draft <kind> skip`** passes over the current picker (offline, AFK); they can /pick freely once it's over.
  A picker who logs back in on their turn gets the screen again. **`remove <name>`** takes someone off the list;
  **`status`** shows the list (`[name]` = picking now); **`stop`** ends it early.
- A Changing Star, `/admin jobs reset` and `/admin jobs clear` aren't part of the drafts. Kept in memory only: a server restart ends both (add whoever is left and start again).

## The Wishing Well (`occupation/WishingWellBlock`, `client/WishingWellModel`, `WishingWellRenderer`)
- A round fieldstone well, **3 x 3 blocks and 3 high**: two courses of stones under a capstone rim, moss, clear
  see-through water over a pebble floor with coins thrown in, two braced dark oak posts holding an axle with coiled rope and a crank, an oak
  bucket (staves, iron hoops and handle) swaying on the rope, a pitched red-shingled roof with a ridge cap and plank gables, a lantern under the ridge. Glints
  rise off the water. Gives off a little light.
- **Collision everywhere:** 21 parts (one block each): the 9 of the ring (the middle one draws the whole well), the 2
  posts and the axle, the 9 of the roof. Each part's shape is worked out from the real well (2 x 2 pixel columns): you
  bump into the round wall, stand on the floor inside it under the water, walk round the posts, the roof overhead.
- **Placed** from the middle (it faces you), needing room for all 21 parts. **Can't be broken** in survival; an admin
  in Creative breaking any part takes the whole well away.
- **Wishing:** right-click it holding a **Fun Wishing Star** or an **Essential Wishing Star** (the Essential one far
  rarer): the star sinks in a burst of light and a chime, and you get one more job slot of that kind ("Your wish is
  granted! ... Type /pick"). Already at 2: "The well can't grant you another ... job." Empty-handed: "Bring a Wishing
  Star and make a wish."
- **How players get the stars:** not decided yet (for now admins hand them out: `/give @p capitalism:fun_wishing_star`).

## Changing jobs (`occupation/ChangingStar`)
- The **Changing Star** comes from an admin or the Traveling Merchant. **Right-click the Wishing Well with it** (or right-click it anywhere; used up): the job screen opens straight away, click a job you hold to let it
  go, then pick another. Once every slot is full again, the change is over.

## Commands and changing (`occupation/JobCommands`)
- The old `/job` commands (`add`, `remove`, `slots`, `change`, `reset`) are **gone**: jobs only come from `/pick`, Wishing Stars and Changing Stars (`occupation/JobCommands` is just the `/pick` logic now).
- **`/admin jobs reset <player>`** (ops, `core/AdminCommands`): puts someone **back to the very start**, as if they'd just joined: no jobs, **one Essential and one Fun slot** (any extra slots from Wishing Stars are gone, and the stars aren't given back). If they have extra slots it doesn't do it straight away: it says how many they'd lose and asks for **`/admin jobs reset <player> confirm`**. The job screen opens and they pick again.
- **`/admin jobs clear <player>`**: for fixing someone's jobs later on (a bug, a mistake). Clears their jobs and the job screen opens, but **their job slots from Wishing Stars are kept** (someone with 2 + 2 slots picks 4 jobs again).
- Both let them pick straight away, **with no job draft in the way** (saved as `JobData.freePick`, so it lasts through a relog; once every slot is full the drafts apply to them as usual).
- **A Changing Star changes ONE job:** click one job you hold to let it go, then pick one new job of the **same kind** (Essential or Fun); that ends it (`JobData.released`). A second job can't be let go, and a job of the other kind can't be picked.
- Older saves with 3 jobs keep the first job of each category.

## Guides (`/guides`)
Each job's guide only shows for players with that job (the General Guide is for everyone): Cook's, Bartender's,
Weaponsmith's, Toolsmith's, Armorsmith's, Enchanter's, Librarian's, Pet Trainer's, Designer's, Farmer's and Real Estate Agent's Guides. The smith and Enchanter guides share one layout
(`client/JobGuideScreen`: sections, icon tabs, an entry grid, details; our items are black silhouettes until made);
each guide is just its colors, tabs and entries (`ToolsmithsGuideScreen`, `ArmorsmithsGuideScreen`, `EnchantersGuideScreen`, `LibrariansGuideScreen`, `PetTrainersGuideScreen`).
The Weaponsmith's Guide uses it too (`WeaponsmithsGuideScreen`). Every job guide has an Information > **Player Debuffs** tab: what everyone without that job can't do.
- Smith blocks anyone can use but only smiths can craft: the Lava Creator (`LavaCreator.md`).

## The jobs

### Cook
**Everything about the Cook is in `Cook.md`** (kitchen, sushi, Oven meals, plates, rice, Gourmet food, the Cook's Guide).
- **Pro:** makes Gourmet food (sushi, Oven meals): full strength, overflows into gold drumsticks, some with effects.
- **Pro:** the only job that can use the Sushi Table, Cooking Table and Oven, and place / serve on plates.
- **Pro:** the only job that can make **Training Feed** (any seed + any raw meat or fish = 6) for Pet Trainers (`PetTrainer.md`).
- **Con:** can't eat raw meat or fish.

### Bartender
- **Pro:** the only job that can use the **Fermenter** and the **Bartending Table** (others get "Only Bartenders can use the Fermenter." and the menu doesn't open; creative ignores it). The Juicer is open to everyone.
- **Pro:** makes Excellent Purified Water (pushes Water Purity to Excellent). *(Not built yet.)*
- **Con:** *(none yet)*

### Weaponsmith
**Everything about the Weaponsmith is in `Weaponsmith.md`.**
- **Pro:** the only job that can make Bows, Crossbows and Netherite Swords (Smithing Table); Netherite Axes too (shared with the Toolsmith).
- **Pro:** makes Shields from a plain Iron Ingot (others need Compacted Iron); the only one who repairs weapons and shields (bows, crossbows and tridents mend with another of the same).
- **Pro:** makes Iron and Diamond Swords and Axes from plain materials; everyone else needs Compacted Iron / Diamonds (double the material).
- **Pro:** 4 arrows per craft (everyone else gets 2).
- **Pro:** forges better swords: Sharpness 78-120 and Edge 200-300 (rare Grail rolls: 135 / 350), against 60-110 and 150-225 for everyone else (and every sword found or traded).
- **Pro:** the only job that can use the Weaponsmith Table, so the only one who can make Whetstones (anyone can use them to sharpen dull swords).
- **Pro:** the only job that can repair weapons (Weaponsmith Table; nobody can repair at the anvil, grindstone or crafting grid); everyone else has to bring them to a Weaponsmith.
- **Con:** *(none yet)*

### Armorsmith
**Everything about the Armorsmith is in `Armorsmith.md`.**
- **Pro:** makes Iron and Diamond armor from plain materials; everyone else needs Compacted Iron / Diamonds (double the material).

### Toolsmith
**Everything about the Toolsmith is in `Toolsmith.md`.**
- **Pro:** makes Iron and Diamond Pickaxes, Shovels, Hoes and Axes from plain materials; everyone else needs Compacted Iron / Diamonds (double the material).
- **Pro:** the only job that can make Netherite Pickaxes, Shovels and Hoes (and Axes, shared with the Weaponsmith).
- **Pro:** crafts faster tools: Efficiency 85-110 (rare Grail: 125), against 75-105 for everyone else.
- **Pro:** the only job that can use the Toolsmith Table.
- **Con:** *(none yet)*

### Enchanter
**Everything about the Enchanter is in `Enchanter.md`.**
- **Pro:** the only job that can enchant past level 10 at an Enchanting Table (up to 30, on anything but books); everyone else is capped at 10.
- **Pro:** the only job that can put enchantments on gear at an anvil (books onto items, items together).
- **Pro:** the only job that can craft and work the Transfer Tube (moves all of one player's XP to another).
- **Pro:** the only job that can craft the Lapis Compressor (turns Lapis Lazuli into XP; anyone can use one).
- **Pro:** the only job that can use the Enchanter's Table: makes Ench-Removals and burns an unwanted enchantment (curses too) off an item for 5 levels.
- **Pro:** the only job that can make and use the Apprentice Wand: turns an armor stand in a full armor set into an Apprentice that stores XP (`Enchanter.md` > Apprentices).
- **Con:** can't enchant books or combine Enchanted Books (that's the Librarian).

### Librarian (Fun)
**Everything about the Librarian is in `Librarian.md`.**
- **Pro:** the only job that can enchant books at an Enchanting Table (up to 30; Enchanters can't either).
- **Pro:** the only job that can combine Enchanted Books at an anvil.
- **Pro:** the only job that can make and use the **Stitching Table** (sews Rough / Delicate Books) and the **Librarian
  Table** (writes skill books from a blank book + a Knowledge Piece). Anyone can read a skill book.
- **Con:** *(none yet)*

### Pet Trainer (Fun)
**Everything about the Pet Trainer is in `PetTrainer.md`.**
- **Pro:** the only job that can craft **Ownership Tags** (Name Tag + Diamond). Anyone can use one to claim a trained pet.
- **Pro:** the only job that can make and use the **Training Stick**: 25 levels turns an animal into a pet (a
  name, hearts, hunger and thirst).
- **Pro:** the only job that can make **Food Troughs** and **Water Bowls** (pets nearby eat and drink from them).
- **Pro:** the only job that can make the **Animal Bjorn** (carries an animal, worn in the Back slot; anyone else can use one).
- **Con:** can't carry animals in an Animal Bjorn themselves (no scooping up wild animals to carry back to the shop).
- **Pro:** the only job that can use **Training Feed** (Cooks make it): an animal fed it follows them for a few seconds.

### Designer (Fun)
**Everything about the Designer is in `Designer.md`.**
- **Pro:** the only job that can make and use the **Sewing Machine** (sews Patches: a needle minigame) and the
  **Designer Table** (stitches Patches and Leather into bags: Backpack, Food Satchel, Seed Satchel).
- **Pro:** the only job that can make and use **Diamond Sewing Needles**.
- Anyone can wear a bag. **Con:** *(none yet)*

### Real Estate Agent (Fun)
**Everything about the Real Estate Agent is in `RealEstate.md`.** Only agents can buy town lots from the city, put up
For Sale / For Lease signs, and sell or lease lots to players (`/properties`); they collect the rent every day.

### Farmer (Fun)
**Everything about the Farmer is in `Farmer.md`.**
- **Pro:** the only job that gets the **full harvest** from ripe crops. Everyone else (and water, pistons, villagers,
  explosions) gets one crop and one seed back (two of a crop that plants itself, like carrots).
- **Pro:** the only job that can make and use the **Decomposter** (Yield, Super Yield and Fast Fertilizer, no fuel).
- **Pro:** the only job that can use **Fertilizer** (Yield: 50% to 90% more crops; Fast: crops grow 1.75x as fast; one at a time).
- Scythes are made by **Toolsmiths** now (Farmers buy them; `Toolsmith.md`).
- **Pro:** the only job that gets the **full meat drops** from animals. Everyone else (and lava, fire, falls) gets 1 piece of meat per animal (Butchery Basics can still add one).
- **Pro:** animals killed with a diamond or netherite sword or axe may drop a **Meat Slab** (15%, 30% with a Bone in the
  sword's socket), cut up at the **Butcher's Table** into fine cuts that rot (Fridges and Freezers keep them). See `Farmer.md`.
- **Con:** *(none yet)*

### Job-only recipes
Any crafting recipe can be made job-only (or give a job more) with the recipe type `capitalism:job_shaped` (`"job": "weaponsmith"`, optional `"others_count"`). See `Weaponsmith.md` > Code.

## Server-wide passive debuffs

### Non-Gourmet food fills only 1/2
**Except Cook-made food:** the same vanilla food made by a Cook (cooked in a furnace / smoker, or crafted) keeps its full values and counts as Gourmet (`Cook.md`).

Always on. Every vanilla food fills 1/2 of its normal hunger. **Meat and fish (raw or cooked) round up** to the nearest hunger point (half a drumstick); **everything else (vegetables, fruit, bread, stews, snacks) rounds down, never below 1**, so living on a pile of carrots or potatoes bought off a Farmer is no match for a Cook's meal. Saturation (the hidden "fullness" that drains before the drumsticks and speeds up healing) shrinks by the same amount, on purpose: normal food shouldn't keep you full for long. Gourmet food (made by a Cook) has its own full-strength values and isn't affected. Done by changing the foods' default components at startup (`FoodNerf`), so tooltips, eating and the diet all see the new numbers. To change a food, edit its line in `FoodNerf.table()`.

1 hunger point = half a drumstick.

| Food | Vanilla | Now |
|---|---|---|
| Cooked Beef (Steak) | 8 (4 drumsticks) | 4 (2) |
| Cooked Porkchop | 8 (4) | 4 (2) |
| Pumpkin Pie | 8 (4) | 4 (2) |
| Rabbit Stew | 10 (5) | 5 (2.5) |
| Cooked Chicken | 6 (3) | 3 (1.5) |
| Cooked Mutton | 6 (3) | 3 (1.5) |
| Cooked Salmon | 6 (3) | 3 (1.5) |
| Golden Carrot | 6 (3) | 3 (1.5) |
| Beetroot Soup | 6 (3) | 3 (1.5) |
| Mushroom Stew | 6 (3) | 3 (1.5) |
| Suspicious Stew | 6 (3) | 3 (1.5) |
| Honey Bottle | 6 (3) | 3 (1.5) |
| Bread | 5 (2.5) | **2 (1)** (rounds down) |
| Baked Potato | 5 (2.5) | **2 (1)** (rounds down) |
| Cooked Cod | 5 (2.5) | 3 (1.5) |
| Cooked Rabbit | 5 (2.5) | 3 (1.5) |
| Apple | 4 (2) | 2 (1) |
| Golden Apple | 4 (2) | 2 (1) |
| Enchanted Golden Apple | 4 (2) | 2 (1) |
| Chorus Fruit | 4 (2) | 2 (1) |
| Rotten Flesh | 4 (2) | 2 (1) |
| Carrot | 3 (1.5) | **1 (0.5)** (rounds down) |
| Raw Beef | 3 (1.5) | 2 (1) |
| Raw Porkchop | 3 (1.5) | 2 (1) |
| Raw Rabbit | 3 (1.5) | 2 (1) |
| Melon Slice | 2 (1) | 1 (0.5) |
| Sweet Berries | 2 (1) | 1 (0.5) |
| Glow Berries | 2 (1) | 1 (0.5) |
| Cookie | 2 (1) | 1 (0.5) |
| Raw Chicken | 2 (1) | 1 (0.5) |
| Raw Mutton | 2 (1) | 1 (0.5) |
| Raw Cod | 2 (1) | 1 (0.5) |
| Raw Salmon | 2 (1) | 1 (0.5) |
| Spider Eye | 2 (1) | 1 (0.5) |
| Poisonous Potato | 2 (1) | 1 (0.5) |
| Beetroot | 1 (0.5) | 1 (0.5) |
| Potato | 1 (0.5) | 1 (0.5) |
| Dried Kelp | 1 (0.5) | 1 (0.5) |
| Tropical Fish | 1 (0.5) | 1 (0.5) |
| Pufferfish | 1 (0.5) | 1 (0.5) |

Most foods have even values, so the rounding only matters for the odd ones: meat and fish (Cooked Cod, Cooked Rabbit, raw Beef, Porkchop, Rabbit) round up, while Bread, Baked Potato and Carrot round down. 1-point foods (Potato, Beetroot, Dried Kelp) stay at 1 rather than 0. A Cook-made version of any of these is full strength (Baked Potato 2.5 drumsticks instead of 1).

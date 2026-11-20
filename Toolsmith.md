# Toolsmith

The Toolsmith makes mining and building tools. They make iron and diamond tools cheaper than anyone else, are the only ones who can make Netherite tools, and their tools mine faster.

## Perks
| | Toolsmith | Everyone else |
|---|---|---|
| **Iron / Diamond Pickaxe** | 3 of the material + 2 Sticks (vanilla) | 3 **Compacted** of it + 2 Sticks (double the material) |
| **Iron / Diamond Shovel** | 1 of the material + 2 Sticks (vanilla) | 1 **Compacted** of it + 2 Sticks |
| **Iron / Diamond Hoe** | 2 of the material + 2 Sticks (vanilla) | 2 **Compacted** of it + 2 Sticks |
| **Iron / Diamond Axe** | 3 of the material + 2 Sticks (vanilla), **same as a Weaponsmith** | 3 **Compacted** of it + 2 Sticks |
| **Netherite Pickaxe / Shovel / Hoe** | Smithing Table as normal (with the template) | **can't** ("Only Toolsmiths can make Netherite Tools.") |
| **Netherite Axe** | Smithing Table as normal (a Weaponsmith can too) | **can't** ("Only Weaponsmiths and Toolsmiths can make Netherite Axes.") |
| **Tool Efficiency** (rolled when crafted) | **85-110**; 1 in 150: **125** (Grail) | **75-105** |
| **Toolsmith Table** | uses it | can craft it, can't use it |

Creative mode ignores the crafting and Netherite rules, but **not** the Efficiency roll: that follows your real job. A **Crafter** block counts as "everyone else". Gold tools are vanilla for everyone.

## Efficiency (mining speed)
Every **pickaxe, shovel and axe** in the world gets an Efficiency stat, rolled once and saved on the tool (hoes don't get one):
- **Efficiency** = the tool's own mining speed as a % of normal (92 = 92% speed, 125 = 125%).
- It only scales the tool's own speed: the **Efficiency enchantment, Haste and Beacons still add on top in full**, like vanilla.
- **When it's rolled:** crafted tools as they leave the result slot (Toolsmith ranges if a Toolsmith made it); tools in generated loot (chests, mob drops, fishing) as the loot is generated; anything else (villager trades, the creative menu) within a second of landing in a player's inventory. A Diamond tool upgraded to Netherite keeps its roll.
- **Grail:** 1 in 150 for a Toolsmith only, exactly 125 (never 111-124).
- **Tooltip:** "Efficiency: 92 (92% mining speed)" (green 100+, yellow 85+, orange below). Before it's rolled (e.g. in the crafting result slot): "Efficiency: ?/105" (a Toolsmith sees ?/110). A Grail shows in light purple with "★ Grail".
- Axes are both: a Weaponsmith or a Toolsmith can make them from plain materials, but only a **Toolsmith** crafts them with the Toolsmith Efficiency range.

## Toolsmith Table
- **Crafting:** `ISI / PMP / P P` (I = Iron Ingot, S = Iron Pickaxe, M = Smithing Table, P = any planks): the Weaponsmith Table's recipe with a pickaxe instead of a sword. Anyone can craft it; **only Toolsmiths can use it**. Axe.
- **Look:** the Weaponsmith Table's build in **light oak**, with a pickaxe and shovel on the front rack and a pickaxe lying on the iron work top next to the little anvil.
- **Menu:** exactly like the Weaponsmith Table: a big slot + a small slot, result slot, fuel slot, lava gauge, Forge button.
- **Forging repairs:** a damaged **pickaxe, shovel, hoe or axe** + 1 of its repair material, then **Forge**: the same dent game as the Weaponsmith Table (see `Weaponsmith.md`: more damaged = more, faster dents; only on the tool's pixels; XP by material: Netherite 5, Diamond 2, else 1; 100 lava). Axes can be repaired at either table. The anvil doesn't repair anything (see `Weaponsmith.md` > Repairs). **Runs on lava** (4-bucket tank, every recipe burns some).
- **Recipes** (`weaponsmith/ToolsmithTableRecipes`):
  | Big slot + small slot | Makes | Lava |
  |---|---|---|
  | Name Tag + Iron Ingot | Iron Reroll Token | 100 |
  | Name Tag + Diamond | Diamond Reroll Token | 100 |
  | Name Tag + Netherite Ingot | Netherite Reroll Token | 100 |
- **Reroll Tokens** (gold coins with the gem in the middle): give a sword or tool of that material brand new stats. **Pickaxes, shovels and axes** here (new Efficiency, the Toolsmith's ranges and Grail chance), **swords** at the Weaponsmith Table. Tool in the big slot + its token in the small slot, 100 lava, no XP. **Every smith** makes tokens at their own table (Weaponsmith, Toolsmith and Armorsmith Tables, `ToolsmithTableRecipes.rerollTokens`). Code: `weaponsmith/Rerolling`.

## Code
- **Job:** `occupation/Job.java` (TOOLSMITH, added at the end).
- **Recipes:** vanilla overrides `data/minecraft/recipe/<iron|diamond>_<pickaxe|shovel|hoe>.json` (`"job": "toolsmith"`) and `<iron|diamond>_axe.json` (`"job": ["weaponsmith", "toolsmith"]`: the `job` field takes a list), plus `data/capitalism/recipe/<same>_from_compacted.json` for everyone.
- **Netherite:** `weaponsmith/WeaponsmithRules.checkSmithing` (Smithing Table).
- **Efficiency:** `weaponsmith/ToolForging.java` (ranges, roll, mining speed in `PlayerEvent.BreakSpeed`, inventory catch-all), component `tool_efficiency`, rolled from `mixin/ItemMixin` and `SwordLootModifier`; tooltip in `client/ClientModEvents.onItemTooltip`.
- **Table:** the shared smith table: `weaponsmith/SmithTable` (which table: job, name, recipes), `SmithTableBlock`, `SmithTableBlockEntity` (lava), `SmithTableMenu`, `client/SmithTableScreen`. Model `models/block/toolsmith_table.json`, textures `toolsmith_front` / `toolsmith_side` (the rest shared with the Weaponsmith Table).

## Salvaging
Like every smith table, this one salvages gear with a **Salvage Hammer** (gear + hammer -> Salvage button -> 1 of its material, 100 lava, 1 of 35 hammer uses). See `Weaponsmith.md` > Weaponsmith Table.

## Dull pickaxes (wear)
- Every block a pickaxe, shovel or axe mines (when it's the right tool) uses one block of its edge; **Obsidian and Crying Obsidian use 100**. How long the edge is depends on the material: **wood 113, gold 158, stone 225, copper 270, iron 450, diamond 675, netherite 900** (`pickaxeBlocksBeforeDull` is the iron number; the rest scale from it, `Wear.tier`). Then it's **Dull**: half its own mining speed (the Efficiency enchantment and Haste still add on top in full), "Dull Iron Pickaxe" in the tooltip, a red chat message.
- Tooltip "Edge: 450/900 blocks"; once used, a thin bar along the bottom of the icon (under the durability bar) (like swords).
- **Tool Whetstone** (sandstone-colored): made **only at the Toolsmith Table** (Grindstone + Compacted Iron, 250 lava). It also sharpens Farmers' **Butcher Knives** (`Farmer.md` > Butchering) back to full. Anyone can place and use one: right-click it holding the pickaxe, shovel or axe (or knife), with a Water Bucket in your inventory, and it's sharp again. **One use:** it breaks after (no drop). Others have to buy them from a Toolsmith.
- **Butcher Knives** (Iron, Diamond): **Only Toolsmiths** make them at a crafting table (job recipe: _ I I / _ I _ / Stick _ _, Diamond has 2 Diamonds on top) for Farmers, who cut Meat Slabs up with them (`Farmer.md` > Butchering). The Toolsmith NPC sells the iron one (4 gold, 4 per restock), and the iron Scythe (6 gold).
- **Scythes** (Iron: `III / ..S / ..S`, Diamond: the same with Diamonds): **only Toolsmiths** make them, for Farmers (`Farmer.md` > Scythes). Repaired at the **Toolsmith Table** like a tool (with Iron / Diamonds), or by the Toolsmith NPC. The Toolsmith NPC sells the iron Scythe (4 gold fair, **6 gold** at the NPC, 4 per restock).
- Code: `weaponsmith/Wear` (counting, `BreakBlockEvent`), `ToolForging.onBreakSpeed` (the half speed), `WhetstoneBlock` (kind PICKAXE).

## Guide
The **Toolsmith's Guide** in `/guides` (Toolsmiths only, `client/ToolsmithsGuideScreen`): Forge (Blocks: table, whetstone;
Items: Compacted metals, Reroll Tokens, Molten Socket; Using the Table: repairs, rerolls, salvaging, sockets), Recipes
(crafting only) and Information (Efficiency, dull pickaxes, perks). Short lines, no numbers or gem effects (same rules as
the Weaponsmith's Guide, see `Weaponsmith.md`).

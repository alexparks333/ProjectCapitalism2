# The Smiths

Three smith jobs: **Weaponsmith**, **Toolsmith**, **Armorsmith**. They share one set of systems (tables, lava,
repairs, rerolls, sockets, salvage, wear). This page is the overview; the details live in `Weaponsmith.md`,
`Toolsmith.md`, `Armorsmith.md`, `Sockets.md` and `LavaCreator.md`. Most of the code is in `weaponsmith/`.

## Who makes what
| Gear (iron / diamond) | Weaponsmith | Toolsmith | Armorsmith | Everyone else |
|---|---|---|---|---|
| Swords | plain material | Compacted | Compacted | Compacted (double cost) |
| Axes | plain | plain | Compacted | Compacted |
| Pickaxes, Shovels, Hoes | Compacted | plain | Compacted | Compacted |
| Helmets, Chestplates, Leggings, Boots | Compacted | Compacted | plain | Compacted |
| Crossbows | yes | no | no | no |
| Arrows / Spectral / Tipped | 4 / 4 / 16 | 2 / 2 / 8 | 2 / 2 / 8 | 2 / 2 / 8 |
| Netherite Sword | yes | no | no | no |
| Netherite Axe | yes | yes | no | no |
| Netherite Pickaxe / Shovel / Hoe | no | yes | no | no |
| Netherite armor | anyone (vanilla) | | | |

- **Compacted Iron / Diamond:** 2 of the material side by side, anyone can make them. And back: one in any crafting
  slot gives the 2 back (`iron_ingot_from_compacted_iron`, `diamond_from_compacted_diamond`). Gold, wood, stone, leather gear is vanilla for everyone.
- Recipes are `capitalism:job_shaped` (`"job"` = one job or a list) and `capitalism:job_imbue` (Tipped Arrows) overriding `data/minecraft/recipe/`. They only win because `neoforge.mods.toml` loads this mod AFTER NeoForge.
- Creative mode skips the crafting rules, but not the stat rolls (those follow your real job).

## Rolled stats (every sword and tool in the world)
| | Everyone else / loot / trades | Weaponsmith / Toolsmith | Grail (1 in 150, smith only) |
|---|---|---|---|
| **Sword Sharpness** (damage %) | 60-110 | 78-120 | 135 |
| **Sword Edge** (hits before dull) | 113-169 | 150-225 | 263 |
| **Tool Efficiency** (pickaxe, shovel, axe mining speed %) | 75-105 | 85-110 | 125 |

- Rolled once: as the item leaves the crafting result slot, when loot is generated, or within a second of landing in an inventory. The result slot shows "?/max" (the best **you** can roll, Grails hidden).
- Sharpness scales the sword's own Attack Damage; Efficiency scales the tool's own speed. Enchantments, Strength and Haste add on top in full.
- **Reroll Tokens** (Iron / Diamond / Netherite; any smith table: Name Tag + the gem, 100 lava): item + the token of its material at its smith table = brand new stats, rolled with the smith's ranges, 100 lava.

## Wear: dull swords, dull pickaxes, dented armor
| | Wears by | Lasts | Worn out | Fixed at |
|---|---|---|---|---|
| Sword | each hit | its rolled Edge | half its damage | **Sword Whetstone** (Weaponsmith) |
| Pickaxe, Shovel, Axe | each block (Obsidian = 100) | wood 113, gold 158, stone 225, copper 270, iron 450, diamond 675, netherite 900 | half its mining speed | **Tool Whetstone** (Toolsmith) |
| Armor (each piece) | each hit you take that armor blocks | leather 45, gold 54, chain/copper 68, iron 90, diamond 135, netherite 180 | half its armor & toughness | **Armor Whetstone** (Armorsmith) |

- Signs: red chat message, "Dull" / "Dented" in front of the name, red tooltip lines, the hairline bar under the durability bar turns grey.
- Whetstones: Grindstone + Compacted Iron at that smith's table. **Only that smith can make one; anyone can place and use it** (right-click with the item and a Water Bucket in your inventory). **One use:** the Whetstone breaks (no drop) after it sharpens / un-dents something, so you need a new one each time (creative keeps it).

## The smith tables
Weaponsmith Table (walnut), Toolsmith Table (oak), Armorsmith Table (crimson). Same block, same menu
(`SmithTable`, `SmithTableBlock`, `SmithTableBlockEntity`, `SmithTableMenu`, `client/SmithTableScreen`). Only that job can use it.
- **Look:** dark molten forge (`tools/make_smith_gui.ps1`), a faded emblem of the table's gear behind the slots, embers off the lava gauge.
- **Slots:** big slot (the item) + small slot (material / ingredient), result, fuel slot, lava gauge.
- **Lava:** Lava Bucket = 1 bucket, **Refined Bucket of Lava** = 2. Tank holds 4. Lost if the table is broken.

| Big + small | Weaponsmith | Toolsmith | Armorsmith | Cost |
|---|---|---|---|---|
| Grindstone + Compacted Iron | Sword Whetstone | Tool Whetstone | Armor Whetstone | 250 lava |
| Name Tag + Iron / Diamond / Netherite | Reroll Token | Reroll Token | Reroll Token | 100 lava |
| Item + its Reroll Token | swords | pickaxes, shovels, axes | | 100 lava |
| Item + Molten Socket | swords | pickaxes and axes | armor | 30 XP levels |
| Damaged item + its material, **Forge** | weapons | pickaxes, shovels, hoes, axes | armor | dent game, lava, XP |
| Gear + Salvage Hammer, **Salvage** | any gear | any gear | any gear | 100 lava, 1 hammer use |

### Repairs (the dent game)
The **only** way to repair gear (the anvil, grindstone and crafting grid don't repair anything; the anvil is for
enchanting). Damaged item + 1 of its material, press **Forge**: glowing dents pop up on the item's own pixels; click them
before they cool. More damaged = more dents (4-14), faster. All dents = a full anvil repair (25%); Compacted Iron /
Diamond = double the repair for double the XP. **XP:** Netherite 5, Diamond 2, else 1 (x2 Compacted). Lava: 100.
While no smith of that job is online, their **NPC** repairs too: 30% per repair for 2-50 gold by material, and sells
that smith's Whetstone (3 gold, 8 per restock). See `NPCs.md`.

### Salvage
**Salvage Hammer** (iron T: `III / .I. / .I.`, only smiths can craft it, 35 uses). Gear + hammer -> 1 of its material
(gold, iron, diamond, netherite, copper), into your inventory, plus the gem from its socket.

## Molten Sockets (see `Sockets.md`)
1% drop from really hard mobs (2% with an Emerald sword); the Wither and Ender Dragon always drop one. A smith sets it
for 30 levels; the owner right-clicks gems in and out.

| Gem | Sword | Pickaxe | Armor (each piece, they add up) |
|---|---|---|---|
| Lapis | +50% kill XP | +50% mining XP | +20% kill XP |
| Redstone | 75% less swing recharge | +20% mining speed | +5% movement speed |
| Emerald | 2% Molten Socket drops | 7.5% bonus Compacted Iron / Diamond from ore | 7.5% off NPC shop prices |
| Quartz | crits 2x | | |
| Prismarine | | | +8% swim speed |
| Echo Shard | | | helmet only: hostile mobs within 24 blocks glow through walls |

## Lava Creator (see `LavaCreator.md`)
Only smiths craft it, anyone uses it: water + Blaze Powder + an empty Bucket -> a Refined Bucket of Lava every minute.

## Testing helpers
- `/dmg <amount>` (ops): damages the item in your hand (never breaks it).
- `/pick` (a fresh player) and the creative tab have everything. Restart the server after the mod is rebuilt.

## Known limits / not done yet
- **No Toolsmith's or Armorsmith's Guide** in `/guides` (only the Weaponsmith has one; their info is in the General Guide and these docs).
- **Armor has no rolled stats yet** (planned: Protection + a second stat). Shields, bows and crossbows have no stats or wear.
- **Swords aren't tiered by material** (iron and diamond roll the same Edge); pickaxes and armor are.
- **Netherite armor** can be made by anyone (only swords, axes and tools are job-locked).
- **The dent game runs on the player's computer:** a hacked client could fake a perfect score.
- **Lava in a smith table and water in a Lava Creator** are lost when the block is broken.
- A Weaponsmith crafting an axe rolls the everyone-else Efficiency (only Toolsmiths forge better tools).
- **Placeholder number:** repair lava (100). Everything else has been tuned.
- **Emerald armor discount** waits for NPC shops (`economy/NpcPricing`); vanilla villagers aren't affected.

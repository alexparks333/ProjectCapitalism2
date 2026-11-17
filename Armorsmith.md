# Armorsmith

The Armorsmith makes armor. They make iron and diamond armor cheaper than anyone else.

## Perks
| | Armorsmith | Everyone else |
|---|---|---|
| **Iron / Diamond Helmet** | 5 of the material (vanilla) | 5 **Compacted** of it (= 10) |
| **Iron / Diamond Chestplate** | 8 of the material (vanilla) | 8 **Compacted** of it (= 16) |
| **Iron / Diamond Leggings** | 7 of the material (vanilla) | 7 **Compacted** of it (= 14) |
| **Iron / Diamond Boots** | 4 of the material (vanilla) | 4 **Compacted** of it (= 8) |
| **Shield** | 6 planks + 1 Iron Ingot (vanilla; a Weaponsmith can too) | 6 planks + 1 **Compacted Iron** |
| **Repairing shields** | at the Armorsmith Table (planks), like a Weaponsmith at theirs | **no** |

Creative mode ignores this. A **Crafter** block counts as "everyone else". An Armorsmith can still use Compacted metals too (it just costs them double).

Compacted Iron / Diamond: 2 of the material side by side in any crafting grid (see `Weaponsmith.md` > Compacted metals).

## Armorsmith Table
- **Crafting:** `ISI / PMP / P P` (I = Iron Ingot, S = Iron Chestplate, M = Smithing Table, P = any planks). Anyone can craft it; **only Armorsmiths can use it**. Axe.
- **Look:** the smith table build in **crimson wood**, with an iron chestplate and helmet on the front rack and a helmet on the iron work top next to the little anvil.
- **Menu:** the same as the other smith tables: big slot + small slot, result, fuel slot, lava gauge (runs on lava, 4-bucket tank), Forge button. Recipes: Armor Whetstone (Grindstone + Compacted Iron, 250 lava) and Reroll Tokens (Name Tag + Iron Ingot / Diamond / Netherite Ingot, 100 lava each: armor can't be rerolled, but Weaponsmiths and Toolsmiths use them, so they sell).
- **Forging repairs:** a damaged **helmet, chestplate, leggings or boots** + 1 of its repair material (Iron Ingot, Diamond, Netherite Ingot, Leather...), then **Forge**: the same dent game as the Weaponsmith Table (more damaged = more, faster dents; dents only on the armor piece's pixels; XP by material: Netherite 5, Diamond 2, else 1; 100 lava). The anvil doesn't repair anything (see `Weaponsmith.md` > Repairs).
- Code: it's `SmithTable.ARMORSMITH` (what it repairs: `SmithTable.repairs`); model `armorsmith_table.json`, textures `armorsmith_front` / `armorsmith_side`.

## Code
- **Job:** `occupation/Job.java` (ARMORSMITH, added at the end).
- **Recipes:** vanilla overrides `data/minecraft/recipe/<iron|diamond>_<helmet|chestplate|leggings|boots>.json` (`capitalism:job_shaped`, `"job": "armorsmith"`, job only), and `data/capitalism/recipe/<same>_from_compacted.json` for everyone.

## Not done yet
- No cons yet. (The Armorsmith's Guide is in `/guides`: `client/ArmorsmithsGuideScreen`. Forge: Blocks, Items, Using the Table (repairs, salvaging, sockets); Recipes: crafting only; Information: dented armor, gems (only that each does something), perks. Short lines, no numbers or gem effects.)
- Golden armor is vanilla for everyone on purpose.
- Netherite armor (Smithing Table) is still vanilla.

## Salvaging
Like every smith table, this one salvages gear with a **Salvage Hammer** (gear + hammer -> Salvage button -> 1 of its material, 100 lava, 1 of 35 hammer uses). See `Weaponsmith.md` > Weaponsmith Table.

## Dented armor (wear)
- Every hit you take that armor would block (not starving, falling into the void...) dents **every armor piece you wear** by one. How many hits a piece takes depends on its material: **leather 45, gold 54, chainmail / copper 68, iron 90, diamond 135, netherite 180** (`armorHitsBeforeDented` is the iron number; the rest scale from it, `Wear.tier`). Then the piece is **Dented**: half its armor and toughness, "Dented Iron Chestplate" in the tooltip, a red chat message.
- Tooltip "Condition: 60/90 hits"; once hit, a half-pixel line in the black strip under the durability bar.
- **Armor Whetstone** (dark basalt): made **only at the Armorsmith Table** (Grindstone + Compacted Iron, 250 lava). Anyone can place and use one: right-click it holding the armor piece, with a Water Bucket in your inventory, and its dents are hammered out. **One use:** it breaks after (no drop). Others have to buy them from an Armorsmith.
- Code: `weaponsmith/Wear` (counting in `LivingIncomingDamageEvent`, half armor in `ItemAttributeModifierEvent`), `WhetstoneBlock` (kind ARMOR).

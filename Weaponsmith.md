# Weaponsmith

The Weaponsmith makes and looks after weapons. They forge better swords than anyone else, make iron and diamond swords and axes cheaper, are the only ones who can make Whetstones, and the only ones who can repair weapons.

## Perks (all of them in one place)
| | Weaponsmith | Everyone else |
|---|---|---|
| **Sword stats** (rolled when crafted) | Sharpness **78-120**, Edge **200-300**; 1 in 150: Sharpness **135** / Edge **350** (Grail) | Sharpness **60-110**, Edge **150-225** |
| **Crossbow** | crafts it normally | **can't** craft it (no result) |
| **Netherite Sword** | Smithing Table as normal (with the template) | **can't** (the Smithing Table gives nothing: "Only Weaponsmiths can make Netherite Swords.") |
| **Netherite Axe** | Smithing Table as normal (a Toolsmith can too) | **can't** |
| **Iron / Diamond Sword** | 2 Iron Ingots / Diamonds + Stick (vanilla) | 2 **Compacted Iron / Diamonds** + Stick (double the material) |
| **Iron / Diamond Axe** | 3 of the material + 2 Sticks (vanilla); a Toolsmith can too | 3 **Compacted** of it + 2 Sticks (double the material) |
| **Arrows** (Flint, Stick, Feather) | **4** per craft | **2** per craft |
| **Spectral Arrows** (4 Glowstone Dust around an Arrow) | **4** per craft | **2** per craft |
| **Tipped Arrows** (8 Arrows around a Lingering Potion) | **16** per craft | **8** per craft (`capitalism:job_imbue`) |
| **Whetstone** | the only one who can make it (Weaponsmith Table: Grindstone + Compacted Iron) | can buy one, place it and sharpen on it |
| **Weaponsmith Table** | uses it | can craft it, can't use it ("Only Weaponsmiths can use the Weaponsmith Table.") |
| **Repairing weapons** (Weaponsmith Table only; the anvil, grindstone and crafting grid don't repair for anyone) | yes | **no**: bring it to a Weaponsmith (or, while none is online, pay their NPC: 30% per repair, see `NPCs.md`) |

Creative mode ignores all of these, except sword stats: those follow your real job (a non-Weaponsmith in Creative still rolls the everyone-else ranges). A **Crafter** block counts as "everyone else" (2 arrows, no Crossbows, no plain-material swords/axes, the lower sword stats).

A Weaponsmith can still use Compacted metals for swords and axes too (it just costs them double).

## Compacted metals
**2 of the material side by side** in any crafting grid (the 2x2 inventory grid works) = 1 Compacted Diamond / Iron. Anyone can make them. They are what everyone else needs for iron and diamond **swords and axes** (Weaponsmith), **tools** (Toolsmith, see `Toolsmith.md`) and **armor** (Armorsmith, see `Armorsmith.md`), so for everyone else those cost double, which is the point: buying from the smith is cheaper. Compacted Iron is also part of the Whetstone.

Gold is left alone on purpose: golden swords, tools and armor are vanilla for everyone.

## Forged swords: Sharpness and Edge
Every sword in the world gets two stats, rolled once and saved on the sword:
- **Sharpness** = the sword's own Attack Damage as a % of normal (87 = 87%, 135 = 135%): a Diamond Sword at 120 shows "8.4 Attack Damage" in its tooltip. **Enchantments (Sharpness, Smite...) and Strength add their full bonus on top**, unscaled, exactly like vanilla.
- **Edge** = how many hits it lands before it goes **dull**.

| Who crafted it | Sharpness | Edge |
|---|---|---|
| **Weaponsmith** | 78-120, or **135** on a 1 in 150 roll | 200-300, or **350** on a 1 in 150 roll |
| **Everyone else**, a Crafter block, and every sword that wasn't crafted | 60-110 | 150-225 |

- The two rare "Grail" rolls are separate (1 in 150 each), and there's nothing in between: a Weaponsmith's edge is 200-300 or exactly 350, never 301-349. Same for 121-134 sharpness.
- **When it's rolled:** crafted swords as they leave the result slot; swords in generated loot (chests, barrels, mob drops, fishing) as the loot is generated (`SwordLootModifier`); anything else (villager trades, the creative menu) within a second of landing in a player's inventory. Any sword counts, wood to netherite.
- Crafted swords: the numbers are rolled on the real item as it leaves the result slot (click, shift-click or drop), so rearranging the grid can't re-roll them.
- A Diamond Sword upgraded to Netherite at the Smithing Table **keeps its stats**.
- **Before it's rolled** (e.g. in the crafting result slot) the tooltip shows "Sharpness: ?/110" and "Edge: ?/225 hits": the best **you** can roll (a Weaponsmith sees ?/120 and ?/300; Creative mode doesn't count). The Grail values are never shown there.
- **Tooltip:** "Sharpness: 87 (87% damage)" (green 100+, yellow 80+, orange below) and "Edge: 187/213 hits". A Grail stat shows in light purple with "★ Grail".

## Sharp and dull swords
- Every melee hit a sword lands on a mob or player uses one hit of its edge (sweep hits count too, one per mob hit).
- When its edge runs out it's **dull**: its own Attack Damage is **halved** (`dullDamageMultiplier` = 0.5; enchantment bonuses still add on top in full), and its **name says "Dull"** (e.g. "Dull Iron Sword"). Its owner gets a red chat message and a dull "clink".
- It stays dull until it's **sharpened at a Whetstone**: back to its full edge.
- Creative players don't wear swords down.
- **On the icon:** once a sword has been used, a thin bar along the **bottom** of its icon, under the durability bar, shows the edge left (teal when sharp, through yellow, to red), grey when dull. (The vanilla durability bar is still at the bottom; edge and durability are separate.)

## Repairs only happen at the smith tables
Durability and edge are two different things. **Durability** (the normal bar at the bottom of the icon) is fixed by repairing; the **edge** (dull or not) is fixed at a Whetstone.

The only way to repair gear is the **Forge** dent game at a smith table: weapons at the Weaponsmith Table, tools at the Toolsmith Table, armor at the Armorsmith Table. Everyone else takes their gear to the right smith. Weapons = swords, axes, spears, bows, crossbows, tridents, maces (`WeaponsmithTableRecipes.isWeapon`).
- **Anvil (everyone, every item):** no repairs with a material ("Repairs are done at a smith table."). Combining two items still **merges their enchantments** (**Enchanters only**, see `Enchanter.md`), and the result keeps the left item's durability; if combining would only repair, it's blocked. Renaming works for everyone.
- **Grindstone:** combining two items is off for everyone. Taking enchantments off one item still works.
- **Crafting grid** (two damaged items): off for everyone.
- Code: `weaponsmith/WeaponRepairs.java` (anvil = NeoForge `AnvilUpdateEvent`, grindstone = `GrindstoneEvent.OnPlaceItem`), `weaponsmith/JobRepairItemRecipe.java` (replaces vanilla `data/minecraft/recipe/repair_item.json` with `capitalism:repair_item`, which makes nothing).

## Blocks

### Weaponsmith Table
- **Crafting:** `ISI / PMP / P P` (I = Iron Ingot, S = Iron Sword, M = Smithing Table, P = any planks). Anyone can craft it; **only Weaponsmiths can use it**. Axe.
- **Look:** a dark walnut cabinet with iron corner bands and a weapon rack (sword and axe) on the front, an iron work top with a little anvil and a sword lying on it.
- **Menu look (all three smith tables):** a **dark molten forge**: charred stone with glowing lava cracks, a blackened-iron riveted frame with an ember-lit inner edge, the title on an iron plaque, recessed slots whose lower edges glow orange (glowing corners on the big and result slots), a real lava channel for the gauge with **embers drifting up off it** (animated, `SmithTableScreen`), and a molten seam above your inventory. Drawn by `tools/make_smith_gui.ps1` (run it in PowerShell to redraw `textures/gui/container/weaponsmith_table.png`).
- **Menu:** **a big slot (the weapon, or a Grindstone) and a small slot (the material) joined by "+"**, an arrow that **glows ember orange** when the items make something, the result slot, a **fuel slot** (with a little flame) and the **lava gauge** (the glowing strip, drains from the right; hover it for "Lava: 2.5 / 4 buckets").
- **Refined Bucket of Lava** (from the Lava Creator, see `LavaCreator.md`) counts as **2 buckets** of fuel.
- **Runs on lava:** put a **Lava Bucket** in the fuel slot and it's poured into the table's tank (lava sizzle; the empty Bucket is left in the slot). The tank holds **4 buckets** and stays in the table when you close it (it's lost if the table is broken). **Every recipe burns some lava**; if the tank is too low the result stays empty and **"Needs Lava"** shows in red. Shift-clicking a Lava Bucket puts it in the fuel slot. If the tank is full the bucket waits in the slot until there's room. Items left in the slots go back to you when you close it (like a crafting table). The **Forge** button (under the arrow) starts a repair: see below.
- **Recipes** (more to come):
  | Big slot + small slot | Makes | Lava |
  |---|---|---|
  | Grindstone + Compacted Iron | Sword Whetstone | 1/4 bucket |
- **Forging repairs (the dent game):** a **damaged weapon** in the big slot + **1 of its repair material** (what an anvil takes: Iron Ingot, Diamond, planks, Breeze Rod...) in the small slot lights up **Forge**. Press it and the screen turns into the game: the weapon drawn as big as the screen allows (up to 11x), and **glowing dents** pop up on it one after another, **only on the weapon's own pixels** (read from its item texture), never on or right next to an earlier dent (2 icon pixels apart, 4 from the last 3); each glows white-hot, then yellow, orange and red as it cools. **Click a dent before it cools** to hammer it (anvil clang). **The more damaged the weapon, the harder:** barely scratched = 4 dents, one every 0.6 s, 1.25 s each; half broken = 8 dents, 0.5 s, 1.05 s; nearly broken = 14 dents, 0.35 s, 0.72 s (smooth in between; `DentForging`). Hitting them all gives back a full anvil repair (25% of max durability); each dent is its share of that. When the last dent is gone the hits go to the server, which checks the slots again and repairs the weapon **in place** (it stays in the big slot), with "Forged! 6/8 dents: +47 durability". **Esc** stops the game and nothing is used.
  - **Cost:** the material, **100 lava** (1/10 bucket, still a placeholder) and **XP by material: Netherite Ingot 5 levels, Diamond 2, Iron Ingot 1, anything else 1** (`DentForging.xpCost`), even if you miss every dent. **Compacted Iron / Compacted Diamond** work too, as 2 Iron Ingots / Diamonds: **double the repair** (all dents = 50%) for **double the XP** (Compacted Iron 2 levels, Compacted Diamond 4). Same at the Toolsmith and Armorsmith Tables. **Forge sits right under the result box**, with the XP cost to its left in anvil green (red if you don't have the levels). It's greyed out with "Needs more lava" / "Needs 2 XP levels". Creative needs no XP.
  - Weapon + same weapon (anvil-style combining) isn't in yet.
  - Code: `weaponsmith/DentForging` (rules, costs, repair), `client/DentGame` (the game), `SmithTableMenu.clickMenuButton` (the score comes back as menu button `100 + hits`). The game runs on the client, so a hacked client could fake a perfect score.
- **Rerolling:** a **sword** (big slot) + the **Reroll Token of its material** (Iron / Diamond / Netherite Reroll Token: Name Tag + the gem at any smith table, this one included) gives it **brand new Sharpness and Edge**, for **100 lava**, no game, no XP. The result slot shows it with "?/max" stats; the new ones are rolled when it's taken out, by the Weaponsmith taking it (their ranges and Grail chance). Enchantments, name, durability and hits since sharpening stay. Wood, stone, gold and copper swords have no token. Code: `weaponsmith/Rerolling`.
- **Salvaging** (every smith table): **gear** in the big slot + a **Salvage Hammer** in the small slot shows a **Salvage** button (where Forge sits). It breaks the gear down into **1 of its material** (the anvil repair material: gold -> 1 Gold Ingot, iron or chainmail -> 1 Iron Ingot, diamond -> 1 Diamond, netherite -> 1 Netherite Ingot, copper -> 1 Copper Ingot), straight into your inventory, for **100 lava** and **1 of the hammer's 35 uses**. Leather, wood and stone gear can't be salvaged. **Salvage Hammer:** `III / .I. / .I.` (Iron Ingots), a `job_shaped` recipe only Weaponsmiths, Toolsmiths and Armorsmiths can craft. Code: `weaponsmith/Salvaging`.
- Taking the result plays the smithing table sound and counts as "made" for the guide.

### Sword Whetstone
- There are three Whetstones, one per smith: **Sword** (Weaponsmith Table), **Pickaxe** (Toolsmith Table, see `Toolsmith.md`) and **Armor** (Armorsmith Table, see `Armorsmith.md`). Only that smith can make it; **anyone** can place and use it, so the others buy them. All three: Grindstone + Compacted Iron at their table.
- Called **Sword Whetstone** in game (its id is still `capitalism:whetstone`). It only sharpens swords: right-click with anything else does nothing ("Right-click with a sword to sharpen it. One use."). No tools.
- **Made at the Weaponsmith Table:** Grindstone (big slot) + Compacted Iron. So only Weaponsmiths can make one, but **anyone can place it and use it**: Weaponsmiths can sell them.
- **Use:** right-click it **holding a sword**, with a **Water Bucket** in your off hand or anywhere in your inventory (off hand is used first). The water is poured on the stone: the Water Bucket becomes an **empty Bucket**, splash + grindstone sound, sparks, "Sharpened! 213 hits until it's dull again. The Whetstone is worn out." (the sword's own edge).
  - No Water Bucket: "You need a Water Bucket to wet the stone." (nothing happens). Creative doesn't need one.
  - Already sharp: "This sword is already razor sharp." (no water used). Empty hand: "Right-click with a sword to sharpen it. One use."
- **One use:** after a sharpening the Whetstone breaks (break particles, no drop), so every sharpening takes a new one (creative keeps it). Unused, anyone can break it with a pickaxe and pick it back up.
- **Look:** a grey-blue sharpening stone (worn shinier in the middle) clamped between two dark wooden blocks on a spruce board, with iron pins.

## Weaponsmith's Guide (in game)
`/guides` -> **Weaponsmith's Guide** (only Weaponsmiths see it). Same layout as the other guides (dark gunmetal with ember-orange trim):
- **Forge:** **Blocks** (Weaponsmith Table, Sword Whetstone), **Items** (Compacted Diamond and Iron, Reroll Tokens, Molten Socket) and **Using the Table** (repairs, rerolling, salvaging, sockets). Every Weaponsmith Table recipe is here, shown as big slot + small slot with "At the Weaponsmith Table." under it. Black silhouettes ("???") until you have made one.
- **Recipes:** **Crafting** only (Iron and Diamond Swords and Axes, Crossbow, Arrows, Salvage Hammer).
- **Information:** **Sharpness** (forged swords, dull swords, sharpening) and **Perks**. No recipes. Laid out as a column of clickable title boxes (not the icon grid), and its pictures have no slots; topics are never "???" (`JobGuideScreen` does this for every guide's Information section).
- **Writing:** a line or two per entry, kept short. No stat ranges, percentages, XP costs or exact gem effects: players find those out by testing.
- Hand-written in `client/WeaponsmithsGuideScreen.buildEntries` (it uses the shared `JobGuideScreen` layout, like the other smith guides): keep it matching this file and the recipes.

## Code
- **Job:** `occupation/Job.java` (WEAPONSMITH, added at the end), Weaponsmith Table gating in `occupation/JobEvents.onRightClickBlock`.
- **Job recipes:** `occupation/JobShapedRecipe.java` = recipe type **`capitalism:job_shaped`**: a shaped recipe with `"job"` (who gets `"result"`) and optional `"others_count"` (what everyone else gets; left out = job only). Registered in `core/ModRecipes.java`. Used by the vanilla overrides in `data/minecraft/recipe/` (`iron_`/`diamond_` `sword` and `axe`, `crossbow`, `arrow`, plus the Toolsmith tools and the Armorsmith armor). `"job"` can be a list (`["weaponsmith", "toolsmith"]` for axes). Can be used for any future job.
- **Load order matters:** `neoforge.mods.toml` has `ordering="AFTER"` on the NeoForge dependency. In dev, NeoForge's data pack carries copies of the vanilla recipes; without it, those replace our `data/minecraft/recipe/` overrides and every job recipe silently stops working.
- **Who's crafting:** vanilla doesn't tell recipes who's crafting, so `mixin/CraftingMenuMixin` sets NeoForge's crafting player while the crafting grid works out its result (crafting table and inventory grid). Mixins are listed in `src/main/resources/capitalism.mixins.json` (and `[[mixins]]` in `neoforge.mods.toml`).
- **Forged sword stats:** `weaponsmith/SwordForging.java` (ranges and the roll), rolled from `mixin/ItemMixin` (`Item.onCraftedBy` for players, `Item.onCraftedPostProcess` for the Crafter block), `weaponsmith/SwordLootModifier` (loot; registered in `core/ModLootModifiers`, turned on by `data/capitalism/loot_modifiers/forge_swords.json`) and a once-a-second inventory check in `SwordForging`. Components `sword_sharpness` and `sword_edge` in `core/ModComponents.java`.
- **Smithing Table:** `mixin/SmithingMenuMixin` (+ `ItemCombinerMenuAccessor`) -> `weaponsmith/WeaponsmithRules.checkSmithing`.
- **Sharpness / dull:** `weaponsmith/Sharpness.java` (sharpness and dull scale the sword's `base_attack_damage` modifier in `ItemAttributeModifierEvent`, so enchantments stay on top; hit counting in `LivingIncomingDamageEvent`), the `sword_hits` component, tooltip and icon bar in `client/ClientModEvents` (`onItemTooltip`, `onRegisterItemDecorations`).
- **Blocks:** `weaponsmith/WeaponsmithTableBlock`, `WeaponsmithTableBlockEntity` (the lava tank), `WeaponsmithTableMenu`, `WeaponsmithTableRecipes`, `client/WeaponsmithTableScreen`; `weaponsmith/WhetstoneBlock`.
- **Recipes:** `data/capitalism/recipe/compacted_diamond|iron.json`, `<metal>_sword_from_compacted.json`, `<metal>_axe_from_compacted.json`, `weaponsmith_table.json`.
- **Config:** `[weaponsmith]` in `capitalism-server.toml`: `swordHitsBeforeDull` (200, only used for a sword that somehow has no stats yet), `dullDamageMultiplier` (0.5).

## Numbers (change any of them)
- Sword stats: see the table above (`SwordForging` constants). Grail odds: **1 in 150** per stat.
- Dull = **half damage**.
- Sharpening costs **one Water Bucket** (you keep the empty bucket).
- Whetstone recipe: **Grindstone + Compacted Iron** at the Weaponsmith Table. It burns **1/4 Lava Bucket**.
- Weaponsmith Table tank: **4 Lava Buckets**.
- Weaponsmith Table recipe: `ISI / PMP / P P`.
- Only swords go dull and have stats (axes don't).
- No cons yet.

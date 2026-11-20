# Molten Sockets

A rare, secret upgrade for **swords, pickaxes, axes and armor**. Code: `weaponsmith/Socketing.java` (where gems fit, the older effects) and `weaponsmith/SocketEffects.java` (the newer effects); tests in `weaponsmith/SocketTests.java`.

## Finding one
- A **1% drop** when a **player** kills one of the really hard mobs: **Wither Skeleton, Enderman, Hoglin, Zoglin, Piglin Brute, Ravager, Evoker, Elder Guardian, Warden** (`Socketing.DROPPED_BY`, `DROP_CHANCE`). Arrows and other kills by a player count.
- **Bosses always drop one:** the **Wither** and the **Ender Dragon**, every time, whoever kills them (`Socketing.BOSSES`).
- **Purple (epic) name**, survives lava. Its tooltip only says: *"Mysterious Item: Take it to a Smith of Your Choice to figure out what it does."*

## Setting it (smiths)
- At the smith table for the item: **sword, mace or trident** (they take the sword gems) at the Weaponsmith Table, **pickaxe or axe** at the Toolsmith Table, **armor piece** at the Armorsmith Table.
- Item in the **big slot**, Molten Socket in the **small slot**: the result is the **exact same item** (enchantments, name, stats, durability, wear all kept) with **one empty socket**.
- Costs **30 XP levels** (`SOCKET_XP`), shown under the result box (green, red if you can't afford it; the result can't be taken without the levels). No lava. Creative is free.
- **One socket per item.** A full set of armor = 4 sockets.

## Gems (set by the item's smith, at their table)
Changed 2026-11-19: players used to set their own gems in any inventory, which was too strong. Now only the smith can.
- **Set a gem:** at the **same table that gave the item its socket** (Weaponsmith Table for swords, maces and tridents,
  Toolsmith Table for pickaxes and axes, Armorsmith Table for armor): the socketed item in the **big slot**, a gem that
  fits it in the **small slot** (an empty socket's tooltip lists them) = the same item with the gem set. Costs **1 XP
  level** (`Socketing.GEM_XP`), shown under the result box; no lava. One gem is used.
- **Break it out:** the item **alone in the big slot** (nothing in the small slot) = the same item with an **empty
  socket**. Free, but **the gem is destroyed** ("Breaks the gem" under the result box). Then set another.
- A repair comes first: a damaged, socketed diamond axe + a Diamond is a repair (Forge), not a Diamond gem. Repair it,
  then set the gem.
- In an inventory, a gem on the cursor right-clicked onto a socketed item does nothing but say who sets it ("Gems are
  set by an Armorsmith at their table.").
- **The Smith's Chisel** (only from the Traveling Merchant, `Town.md`), the one exception: right-click a socketed item
  with it and the Molten Socket pops back out, **its gem too**, to be set into something else. Used up.
- Code: `Socketing.canSetGem / canBreakGem`, `SmithTableMenu` (`GEM_READY`, `GEM_NEEDS_XP`, `GEM_BREAK`), the cost text in
  `client/SmithTableScreen`.

| Gem | Sword | Pickaxe | Axe | Armor (each piece) |
|---|---|---|---|---|
| **Lapis Lazuli** | +50% XP from kills | +50% XP from mining | - | **+20%** XP from kills |
| **Emerald** | Molten Sockets drop **2%** of the time (not 1%) from the mobs that drop them | **7.5%** chance of a bonus **Compacted Iron** from iron ore / **Compacted Diamond** from diamond ore (not with Silk Touch) | - | **7.5% off NPC shop prices** per piece (4 pieces = 30%) (town NPCs; `economy/NpcPricing`) |
| **Redstone** | 75% less recharge time per swing (4x attack speed) | +20% mining speed | - | +5% movement speed |
| **Quartz** | Critical hits do **2x** damage (not 1.5x) | **Stone and deepslate break 25% faster** | - | - |
| **Prismarine** (shard) | Hits **50% harder while you're underwater** | Mines **3x as fast underwater** | - | **+8% swim speed** (4 pieces = +32%) |
| **Echo Shard** | - | - | - | **Helmet only:** hostile mobs within 24 blocks **glow through walls**, on your screen only |
| **Bone** | a Farmer's kills: **+15% chance of a Meat Slab** (diamond / netherite sword, `Farmer.md` > Butchering) | - | - | **5% less projectile damage** (arrows, tridents, fireballs...; 4 pieces = 20%) |
| **Ice** (any: Ice, Packed Ice, Blue Ice; comes back out as plain Ice) | **20% chance to slow** what you hit (Slowness II, 3 s) | - | - | **Any piece (one is enough):** you **can't get hot**, but every cold is **10 colder** (`Wellness.md` > Body heat) |
| **Blaze Powder** | **Lights up the area around you like a torch** while held (either hand) | **Lights up the area** while held | **Logs come out as Charcoal** | **Any piece (one is enough):** the **cold can't reach you**, but somewhere hot you **burn** (100). Ice + Blaze: neither |
| **Amethyst** (shard) | **Each kill heals half a heart** | - | - | **+5% knockback resistance** (4 pieces = 20%) |
| **Glowstone** (dust) | What you hit **glows for 10 seconds** | - | - | - |
| **Ghast Tear** | **+20% damage to Nether mobs** (Blaze, Ghast, Magma Cube, Piglins, Hoglin, Zoglin, Strider, Wither Skeleton, Wither) | - | - | - |
| **Slime Ball** | - | - | - | **Boots only: no fall damage** |
| **Rabbit's Foot** | - | - | - | **+1 Luck** (4 pieces = +4): better loot and fishing, the rare catches too (`Fishing.md`) |
| **Diamond** | - | - | **A log splits into 8 planks** of its wood (logs, wood, stems, hyphae, bamboo blocks; stripped too) | - |
| **Flint** | - | - | **25% chance to make what you hit bleed** (Bleeding: 1 damage a second for 4 s) | - |

- Bonuses from armor pieces **add up**: 4 Emerald pieces = 30% off, 4 Prismarine = +32% swim speed. Lapis: a lapis sword in hand (+50%) + 4 lapis armor pieces (+20% each) = **+130%** XP from kills. Redstone movement adds up too (4 pieces = +20%).
- Redstone's bonuses show in the item's own attribute lines (a sword shows "+300% Attack Speed").
- **Inventory icon:** a tiny 4x4 socket in the top-left corner of the item (molten rim; dark inside when empty, blue with Lapis, red with Redstone).
- Tooltip: "◇ Empty Socket" (gold) or "◆ Lapis Socket: +50% XP from kills" (blue) / "◆ Redstone Socket: ..." (red), with how to change it.

## For NPC shops
All NPC buying prices must go through `economy/NpcPricing.npcPrice(fairNuggets, player)` so the Emerald discount applies (noted in `CLAUDE.md`).

## Works with everything else
- Repairs (dent game), rerolls, sharpening and the Netherite upgrade keep the socket and its gem.
- **Salvaging** a socketed item gives the gem back too (the socket itself is lost).

## Ideas for later
More gems (Emerald, Amethyst, Quartz, Glowstone...), a second socket for Grail items, a chance for the Molten Socket to "crack" on setting.

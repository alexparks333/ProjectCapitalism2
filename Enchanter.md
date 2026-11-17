# Enchanter

The Enchanter is the only one who can enchant at high levels, and runs the **Transfer Tube**, which moves XP from one
player to another (so people can sell their XP to the Enchanter, or the Enchanter can sell it on, e.g. to a
Librarian). **Books aren't theirs:** enchanting books and combining Enchanted Books is the Librarian's (`Librarian.md`).
Enchanters buy their books from Librarians and put them on gear.

## Perks
| | Enchanter | Everyone else |
|---|---|---|
| **Enchanting Table** | up to level **30**, as normal, on tools, weapons and armor. **Not books**: a book gets no options (only Librarians enchant books) | capped at **level 10**: each of the three options costs at most 10, and rolls level-10 enchantments. Books: no options either |
| **Anvil** | puts enchantments on gear: Enchanted Books onto items, enchanted items together. **Not book + book** (Librarians only) | **can't**: anything enchanted in the right slot gives nothing ("Only an Enchanter can put enchantments on gear."). Renaming still works |
| **Enchanter's Table** | makes Ench-Removals; burns one unwanted enchantment (curses too) off an item for 5 levels | **can't** use it |
| **Lapis Compressor** | the only one who can craft it | can use one (Lapis in, XP orbs out) |
| **Transfer Tube** | the only one who can craft it, flip its arrow and press its button | can stand in a booth (and give or get XP) |

Creative players aren't capped. Code: `enchanter/EnchantingLimit`, `mixin/EnchantmentMenuMixin` (caps the table's costs
when an item goes in, and gives books 0 = no options unless Librarian). Anvil: `enchanter/AnvilEnchanting` (NeoForge
`AnvilUpdateEvent`; book + book needs a Librarian, anything else enchanted needs an Enchanter).

## The Enchanter NPC (`enchanter/NpcEnchanting`, `mixin/EnchantmentMenuMixin`)
While no Enchanter is online, the stand-in Enchanter (`NPCs.md`) is an **Enchanting Table you pay**. Right-click him:
the real enchanting screen, as if 15 bookshelves stood round it (options up to **level 30**, no level-10 cap). Put the
item in, pick an option. **No lapis:** the lapis slot shows some you can't take. Each enchant costs its levels as at a
table, plus **gold** by how high the option is:

| Option | Gold (NPC price) |
|---|---|
| under level 15 | 10 |
| 15 to 29 | 20 |
| 30 | 50 |

The gold goes through `NpcPricing` like every NPC shop (fair 60 / 120 / 300 nuggets, +50%), so Emerald armor and sale
days take some off. Not enough gold: "That one's 50 gold. Come back with the gold." Books still get no options (only
Librarians enchant books). It stays open while you're within 8 blocks of him; closing gives your item back.

## Enchanter's Table
- **What it does:** two things, **on XP only** (no fuel), Enchanters only:
  - **Book** (big slot) + **Blaze Rod** (small slot) = an **Ench-Removal** (a flaming book, stacks to 16, fireproof).
    Free besides the items.
  - **An enchanted item** (big slot) + an **Ench-Removal** (small slot): its enchantments are listed underneath (curses
    in red), each costing **5 levels**. Click one to pick it: the result is the item **without it, gone for good**
    (curses too). Taking the result costs the 5 levels and the Ench-Removal. An Enchanted Book with 2 or more
    enchantments works the same. Scroll the list if there are more than 5.
- Items left in the slots go back to you when you close it.
- **Look:** a dark oak desk with violet rune panels, a purple velvet top, an open red spellbook and two lit purple
  candles (flames and smoke), glyphs drifting off the book. Gives off a little light (6).
- **GUI:** a night-sky violet panel: the big slot, the small slot and the result across the top with glowing frames, the
  enchantments as glowing rows like the Enchanting Table's offers. With nothing to list, enchanting runes drift up
  through the empty space (no recipe hints: players find those out).
- **Recipe (Enchanters only, `job_shaped`):** `CBC / BIB / PPP`: any Candle, Book, Compacted Iron, any Planks.
- **Code:** `enchanter/EnchantersTableBlock`, `EnchantersTableMenu`, `EnchantRemoval` (cost: `EnchantRemoval.COST`),
  `client/EnchantersTableScreen`. Textures: `tools/make_enchanters_table.ps1` (block), `tools/make_enchanters_table_gui.ps1`
  (menu), `tools/make_ench_removal.ps1` (the Ench-Removal, animated flames).

## Lapis Compressor
- **What it does:** turns **Lapis Lazuli into XP**, slowly. Right-click it to open its menu and put Lapis in the slot
  (or feed it with a hopper from any side); it holds up to **64**. Every **2 seconds** it presses 1 Lapis into
  **4 XP points**, spat out of its front as XP orbs onto the ground. Breaking it drops the Lapis inside.
- **Menu:** riveted steel. The Lapis slot on the left; the press in the middle, where a steel head comes down and
  squashes a lapis chunk flat; the XP chute on the right, where an XP orb grows and glows until it pops out.
- **Who:** only Enchanters can **craft** it; **anyone** can place and use it (so Enchanters can sell them).
- **Look:** a riveted dark-steel press: lapis squeezed between plates behind a window on the sides, a press head on top,
  and a chute on the front that glows XP green while it works (light 9), with sparks, smoke and the odd piston thump.
- **Recipe (Enchanters only, `job_shaped`):** `IPI / LCL / III`: Iron Ingot, Piston, Lapis Block, Compacted Iron.
- **Numbers:** `LapisCompressorBlockEntity.XP_PER_LAPIS` (4), `PRESS_TICKS` (40). A stack of 64 = 256 XP points
  (about level 13 from zero), over about 2 minutes. Level 29 -> 30 = 27 Lapis; a max enchant (27 -> 30) = 77 Lapis;
  0 -> 30 = 349 Lapis.
- **Code:** `enchanter/LapisCompressorBlock`, `LapisCompressorBlockEntity`, `LapisCompressorMenu`,
  `client/LapisCompressorScreen`. Textures: `tools/make_lapis_compressor.ps1` (block), `tools/make_lapis_compressor_gui.ps1` (menu).

## Transfer Tube
- **Shape:** 3 blocks wide, 2 high, placed from the middle (it faces you). A **glass booth** on the left and on the
  right (open at the front, walk in), and a **console** in the middle: a **wooden sign with an arrow** on top and a
  **bookshelf with a stone button** under it. Old-school look: dark oak posts and trim, violet glass, obsidian rune floors. Needs room for all 6 blocks; breaking any of it breaks it all and gives
  the item back once.
- **Arrow sign (top middle):** an Enchanter right-clicks it to flip which way the XP goes, left booth -> right booth or
  right -> left (as you look at the front).
- **Button (bottom middle):** an Enchanter presses it; the button presses in and glows (crying obsidian), and **2 seconds later** the transfer
  happens with whoever is standing in the booths **then** (so the Enchanter can press it and run into a booth).
- **The transfer:** exactly one player (or one Apprentice, see Apprentices) must stand in each booth. **All** the XP (levels and the bar) of the player the
  arrow points away from goes to the other player. Enchanting glyphs stream between them; everyone within 8 blocks
  sees "X gave N XP (L levels) to Y, now level Z." When the button is pressed it says what will happen (who gives how many points, and the levels both end up at) if both booths are filled. If a booth is empty or crowded, or the giver has no XP, it stops and says why.
- **Recipe (Enchanters only, `job_shaped`):** `GLG / YEY / OOO`: Glass, Lapis Block, Ender Pearl, Enchanting Table, Obsidian.
- **Code:** `enchanter/TransferTubeBlock` (parts, placing, breaking, arrow, button, 2 s scheduled tick),
  `enchanter/XpTransfer` (who's in the booths, moving the XP). Textures: `tools/make_transfer_tube.ps1`; the item is the whole tube shrunk down (`tools/make_transfer_tube_item.pl` builds it from the block models).

## Apprentices (storing XP)
A fun way for an Enchanter to bank XP: an enchanted armor stand that holds an amount of XP.
- **Apprentice Wand:** made at the **Enchanter's Table**: **Bamboo** (big slot) + **Potion of Poison** (small slot,
  any strength or length; the empty bottle comes back). Free besides the items. **4 uses.** Enchanters only.
- **Making an Apprentice:** put a **full set of one kind of armor** on an armor stand (all 4 pieces the same
  material), then an Enchanter **hits it with the wand**: it turns into an Apprentice (same armor, same spot, owned by
  them), with enchanting glyphs and a spell sound. Anything in the stand's hands drops; the armor stand is used up. Not a
  full set: "It needs a full set of one kind of armor." Non-Enchanters: "Only Enchanters can use the Apprentice Wand."
  The wand never knocks a stand over.
- **What it holds:** XP **points**, not levels (`Apprentice.Kind`). Each kind's size is the XP it takes to get from
  level 0 to its level, so a high-level player loses fewer levels filling it (25 -> 18 is 469 XP)
  and the same XP comes back. Everything shows amounts ("469 / 679 XP"), not levels, so it isn't confusing:

  | Armor | Leather | Chainmail | Copper | Gold | Iron | Diamond | Netherite |
  |---|---|---|---|---|---|---|---|
  | From level 0 to | 15 | 15 | 10 | 28 | 22 | 32 | 100 |
  | XP held | 315 | 315 | 160 | 1186 | 679 | 1628 | 30970 |

- **The Apprentice:** a cute little polished-iron figure (big shiny eyes, pink cheeks, a violet rune gem on its chest) in a
  tall violet wizard hat with a gold band, a gold star and a floppy tip (violet and gold, not the vanilla witch's hat). It
  bobs, sways its arms and its hat tip wobbles. Its armor is only data (how much it holds): it isn't drawn. Glyphs drift round it and a floating name like
  "Iron Apprentice: 469 / 679 XP". It **can't be hurt, broken or blown up** (only `/kill` or the void), and its armor
  can't be taken off. Right-click: "Holds 469 of 679 XP."
- **Naming it:** its **owner** right-clicks it holding the **Apprentice Wand**: a naming box opens (Enter or Done;
  up to `Apprentice.MAX_NAME` = 24 letters; empty = back to "Iron Apprentice"). Free, no wand use. The name replaces
  "Iron Apprentice" over its head ("Merlin: 469 / 679 XP") and in the Transfer Tube messages, and goes with it as the
  item's name (an anvil-renamed Apprentice item is named that when placed). Packets: `network/OpenApprenticeNamePacket`
  (server -> client, opens `client/ApprenticeNameScreen`) and `RenameApprenticePacket` (client -> server; checks owner,
  Enchanter, within 8 blocks, wand in hand).
- **Enchanters only:** only Enchanters (or creative) can make, name or pick up Apprentices ("Only Enchanters can pick
  up Apprentices."). Anyone can right-click one to see what it holds.
- **Picking it up:** its **owner** shift + right-clicks it with an empty hand: it becomes an **Apprentice item** that
  keeps its armor, XP and name (tooltip: "Iron: 469 / 679 XP"). Placing it makes the placer the owner, so Apprentices
  can be traded. Clicking a Transfer Tube booth with it stands it on the booth's floor. An empty Apprentice item (no
  data) won't place.
- **Filling / emptying:** a **Transfer Tube** booth can hold one player **or** one Apprentice. Player -> Apprentice:
  only what fits goes in, the rest stays with the player. Apprentice -> player: everything it holds. Apprentice ->
  Apprentice works too. The messages show the player's levels and the Apprentice's XP, e.g. "Steve (level 25 -> 18)
  gave 469 XP to Iron Apprentice (0 -> 469 / 679 XP)." A full Apprentice stops the transfer ("the Diamond Apprentice is full").
- **Code:** `enchanter/Apprentice` (the entity, `Kind`), `ApprenticeItem`, `ApprenticeWand` (the hit),
  `EnchantersTableMenu` (the wand recipe), `XpTransfer` (booths hold a player or an Apprentice). Drawn by
  `client/ApprenticeModel` + `ApprenticeRenderer` (about a block tall plus the hat). Textures: `tools/make_apprentice_entity.ps1`
  (the entity skin) and `tools/make_apprentice_items.ps1` (the wand and the Apprentice item icon).

## Numbers (change any of them)
- Enchanting cap for everyone else: **10** (`EnchantingLimit.MAX_LEVEL`).
- Transfer delay: **2 seconds** (`TransferTubeBlock.TRANSFER_DELAY`, 40 ticks).
- How much moves: **all** of it (`XpTransfer.run`).

## Guide
The **Enchanter's Guide** in `/guides` (Enchanters only, `client/EnchantersGuideScreen`): Workshop (Blocks: the three
blocks; Items: the Ench-Removal; Using the Table: removing an enchantment, shown as a table recipe) and Information
(Enchanting and Perks, as clickable boxes; no XP section). Short lines, no numbers (levels, costs, XP amounts).

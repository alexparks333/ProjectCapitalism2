# Librarian

A **Fun** job (`Job.LIBRARIAN`, added at the end of `Job`). The first Fun job after the Bartender. Books are theirs:
nobody else, not even the Enchanter, can enchant a book or combine Enchanted Books. So Enchanters buy books from
Librarians and put them on gear, and can sell XP to Librarians (through the Transfer Tube, which the Enchanter runs).

## Perks
| | Librarian | Everyone else (Enchanters too) |
|---|---|---|
| **Enchanting Table** | the only one who can enchant a **Book**: up to level **30**, no cap | a book gets **no options** (non-books work as usual: capped at 10, or 30 for Enchanters) |
| **Anvil** | the only one who can **combine Enchanted Books** (Enchanted Book + Enchanted Book) | gets nothing ("Only a Librarian can combine Enchanted Books.") |

Putting an Enchanted Book onto gear stays the Enchanter's (`Enchanter.md`); a Librarian who isn't an Enchanter can't.
Creative players aren't limited.

## Code
- `enchanter/EnchantingLimit.cost` (books: Librarian or nothing) through `mixin/EnchantmentMenuMixin` (wraps vanilla's
  `getEnchantmentCost` roll, so it knows the item).
- `enchanter/AnvilEnchanting` (book + book needs a Librarian).
- Guide: `client/LibrariansGuideScreen` (Workshop: Blocks, Items; Information: Books, Reading, Player Debuffs). The
  Rough / Delicate Book recipes are drawn like the Stitching Table itself (`HowToMake.stitching`: the ring of covers
  round the Paper, the thread, the book); slots with a few options switch every second (Rough / Delicate Book, the four
  Knowledge Piece qualities, Leather / Rabbit Hide). Icon: Lectern (guides, `/pick`);
  the stand-in NPC holds a Book (`npc/JobNpc`).

## Skill books (the Librarian's own thing)
Like Project Zomboid's skill books: Librarians make books anyone can read to learn something.
**Stitching Table -> blank book -> Librarian Table + Knowledge Piece (Knowledge Creator / the Whisperer) -> skill book -> read it.**

### Stitching Table (`librarian/StitchingTableBlock`, `StitchingTableBlockEntity`, `StitchingTableMenu`)
- **Recipe (Librarians only, `job_shaped`):** `SHS / LCL / PPP`: String, Shears, Leather, Crafting Table, any Planks.
  *(Placeholder, change it if you like.)*
- **Use:** Librarians only ("Only a Librarian can use the Stitching Table."). Breaking it drops everything.
- **Sewing:** with Paper, **a cover in every one of the 12 ring slots** (3 on each side) (Leather or Rabbit Hide, mixed is fine) and a
  thread, it sews; after `SEW_TICKS` = **18000 ticks (15 minutes)** one book is done and **the whole ring is used**:
  1 Paper, **1 from each of the 12 ring slots** and 1 thread. **Diamond -> Delicate Book, Iron Ingot -> Rough Book**
  (whichever thread is in when it finishes). Missing something: it waits and keeps its progress. Books stack to 16; it
  stops when the output is full (or holds the other kind of book).
- **Menu** (`client/StitchingTableScreen`, texture by `tools/make_librarian_textures.ps1`): the ring of 12 cover slots
  (3 a side) round the big Paper slot. (Tables placed when the ring had 16 slots are converted when they load: Paper,
  thread and books move over, the 4 extra covers are merged into the others.) While it sews, **thread is stitched round the Paper** along a dotted line as the book gets
  done (dashes with little cross-stitches; silver for Iron, glinting cyan for Diamond), with **the needle bobbing in and
  out** at the end of the thread. On the right: the thread slot (with a spool), a walnut-and-brass **plaque counting down
  the time left** (m:ss, the colon blinks while it sews, greyed out when paused) and a progress groove under it, then the
  finished books. Next to "Inventory": "Sewing a Rough / Delicate Book", or what's missing ("Needs Paper", "Fill the
  ring: 3 empty", "Needs thread", "Take the books out").
- **On the block** (`client/StitchingTableRenderer`, the table syncs what's on its mat to clients): while it sews, the
  book being made lies on the leather mat and **the needle (`capitalism:stitching_needle`, only drawn, not obtainable)
  works its way round the book's edge, stabbing down through it and pulling back up**, a stitch a second. Wisps of
  thread and sparkles float off it; you hear the needle pull through every second and a half and the thread snap tight
  now and then; when a book is done, a puff of green sparkles and a book thump. Idle: the finished books or the Paper on
  the mat, the needle lying beside them.
- **Hoppers:** Paper and covers in from the top, thread from the sides, books out of the bottom.
- **Look:** a spruce worktable with a leather mat (stitched border), a spool of red thread, and a drawer with thread
  hanging out.

### Librarian Table (`librarian/LibrarianTableBlock`, `LibrarianTableMenu`)
- **Recipe (Librarians only):** `FBI / SLS / PPP`: Feather, Book, Ink Sac, Bookshelf, Lectern, any Planks. *(Placeholder.)*
- **Use:** Librarians only. Like the smith tables: **big slot** = a Rough or Delicate Book, **small slot** = a
  **Knowledge Piece**, then the result. The result shows an "Unwritten Book" and is only **written when it's taken**
  (`Skill.roll`), so nobody can peek and swap. Faint letters drift across a page under the slots (no recipe hints).
- **Look:** a writing desk with bookshelf sides, a green felt top, an open green book, an inkwell and a quill.

### Knowledge Pieces (`librarian/KnowledgeQuality`, `KnowledgePieceItem`)
Not found in chests any more: they come from **the Knowledge Creator** (Librarians) and **the Whisperer** (`Town.md`).
Each has a **quality**, set by the Enchanted Book it was made from: the book's **power** = all its enchantment levels
added up, **+3 for each treasure enchantment** (Mending, Frost Walker...).

| Power | Piece | Persistent chance at the Librarian Table | Whisperer price |
|---|---|---|---|
| 1-2 | **Faded** (dull parchment) | x1 | 1 emerald |
| 3-5 | **Clear** (pale blue) | x1.5 | 2 emeralds |
| 6-9 | **Bright** (violet) | x2 | 3 emeralds |
| 10+ | **Radiant** (gold, shimmers) | x3 | 4 emeralds |

At the Librarian Table the piece's multiplier times Lucky Librarian's x2 (no cap): a Delicate Book with a Radiant piece
is 37.5% Persistent, 75% with Lucky Librarian. Faded is the plain item (no data); the others carry
`ModComponents.KNOWLEDGE_QUALITY` and pick their look by the CUSTOM_MODEL_DATA string (`items/knowledge_piece.json`).
Name and color by quality ("Bright Knowledge Piece"). Pieces of the same quality stack. All four are in the creative tab.
Never hidden in the guide (`ClientDiscoveries.foundNotMade`).

### Knowledge Creator (`librarian/KnowledgeCreatorBlock`, `KnowledgeCreatorBlockEntity`, `KnowledgeCreatorMenu`)
- **Recipe (Librarians only, `job_shaped`):** `BFB / WWW / III`: Book, Feather, Book / any Planks / Iron Ingots.
- **Use:** Librarians only. One **Enchanted Book** in; after **30 seconds** (`MAKE_TICKS` = 600) it's one Knowledge
  Piece of the book's quality, and the book is used up. It waits if the output holds another quality or is full.
  Hoppers: books in from the top and sides, pieces out of the bottom. Breaking it drops what's inside.
- **Menu** (`client/KnowledgeCreatorScreen`, texture by `tools/make_knowledge_textures.ps1`): dark deepslate with violet
  glyphs. The book on the left, the piece on the right, and between them a channel that fills with the quality's color,
  enchanting glyphs flowing along it and the time left under it. **Before it's done it shows what the book will make:**
  the piece's icon and "Power 7: Bright Piece (x2)", and four pips (Faded, Clear, Bright, Radiant) with the one it'll
  make lit.
- **Look:** a dark polished-deepslate pedestal with a strip of books on each side and a violet rune, a violet ring on
  top, amethyst on the corners, and a glass bell jar with a deepslate lid and an amethyst knob. While it works
  (`WORKING`, glows a little) the Enchanted Book floats in the jar, turning and shrinking, and the Knowledge Piece forms
  under it, growing (`client/KnowledgeCreatorRenderer`); glyphs swirl down into the jar. Done: an amethyst chime and a
  burst of sparkles. JSON by `tools/make_knowledge_assets.js`.

### Skill books (`librarian/Skill`, `SkillBookItem`, `SkillBook` component)
**Every book, what it does and its odds: `LibrarianBooks.md`** (23 lessons, 1 instant book, 14 traits). Keep it matching `Skill`.
- One item (`capitalism:skill_book`, stacks to 1) holding which `Skill` and Rough / Delicate. Named after the skill.
  **Every book has its own cover**: `items/skill_book.json` picks `item/skill_books/<skill>` by the CUSTOM_MODEL_DATA
  string (the skill's color and an emblem, drawn by `tools/make_librarian_textures.ps1`; models and lang by
  `tools/make_skill_book_models.js`). Lesson books show their level, a gold **I** (Rough) or **II** (Delicate), in the
  bottom-right corner (the second CUSTOM_MODEL_DATA string; numerals by `tools/make_book_numerals.ps1`; lesson books
  written before this have none). Trait books shimmer, have a gold name and a gold spine. Tooltip: "Rough Book" /
  "Delicate Book", "Persistent" (traits, "(rare)" for rare ones) or "Temporary: level I for 20 minutes" / "level II for
  40 minutes", what it does (`Skill.description`), and "Hold right-click to read it.". Creative search doesn't add the
  creative tab name to them (`mixin/CreativeModeInventoryScreenMixin`). Every book is in the creative tab for testing.
- **Anyone can read one:** hold right-click: **10 seconds for a Temporary book (lesson), 25 for a Persistent one (trait)**
  (`SkillBookItem.LESSON_READ_TICKS` / `TRAIT_READ_TICKS`). While you read, the book lies open in the middle of the
  screen (`client/ReadingHud`, GUI layer `capitalism:reading`): left page the book, its title, Rough / Delicate and
  Temporary / Persistent; right page what it teaches, written in as you read, then faint lines; under it "Reading... 7s"
  and a bar (green, gold for a trait). Pages turn now and then. Let go early and nothing happens. Done: it's used up.
- **Text:** `Skill.description(level)`: each description is written `Walk {10%|20%} faster.` and shows only the book's
  own level (Rough / level I: 10%, Delicate / level II: 20%) in the tooltip, the reading screen and Buffs/Debuffs.
- **Picking a book (`Skill.roll`):** first lesson or trait (**Rough 1%, Delicate 12.5%** trait; doubled for a Librarian with
  Lucky Librarian on), then which one by weight (**common 10, rare 4**: `Skill.rare`).
- **Lessons:** an effect (`ModEffects.BOOK_LESSONS`, own icon `mob_effect/book_<skill>.png`). **Rough: level I for 1
  Minecraft day (24000 ticks). Delicate: level II (double) for 2 days.** Bookworm: 50% longer. Gone on death; milk / a Totem (clearing every effect) leaves them (`mixin/LivingEntityMixin`). **Hidden
  from the vanilla effect icons** (no particles, not next to the inventory or on the HUD:
  `ClientModEvents.onRegisterClientExtensions`): they show in **Wellness > Buffs/Debuffs** (`client/WellnessScreen`).
- **Traits:** learned for good, **until you die: dying forgets every trait** (`Traits`, `TraitData` attachment, not
  copied on death; synced to its player so the client knows them too), just as dying loses the lessons. Attribute
  traits are re-applied as permanent modifiers at login / respawn (and ones for traits no longer known taken off). A trait you already know:
  "You already know X." and the book stays.
- **Instant books (`Skill.instant`):** A Fresh Start (diet, water and body heat back to the start, `WellnessEvents.freshStart`). An
  instant book is drawn with the lessons, does its thing once when read, tooltip "Instant".
- **How each works:** an attribute skill carries its modifier (`Skill.attribute`; level II doubles it); the rest are
  hooks asking `Skills.level(player, skill)` (0, 1, or 2 for a level II lesson): `SkillEffects` (Lumberjack's Log,
  Stonemason's Guide, Butchery Basics, Archery Drills, Steady Aim, Quiet Steps, Beekeeper's Notes, Fast Learner, Horse
  Whispering, Sure Footed on ice), `core/ModServerEvents` (Water Rationing, Camel), `economy/NpcPricing`
  (Merchant's Ledger), `player/BodyTemperature` (Winter Survival: the cold can't touch you), `player/DrinkEffects` +
  `occupation/JobEvents` (Night Owl, Iron Stomach), `SkillBookItem` (Bookworm), `LibrarianTableMenu` (Lucky Librarian),
  and mixins `FishingRodItemMixin` (Fisherman's Patience), `PlayerMixin` (Trail Rations), `PowderSnowBlockMixin` (Sure
  Footed), `FoodDataMixin` (First Aid Basics: every natural heal, fast or slow, heals 25% / 50% more), `LivingEntityMixin` (milk keeps lessons).
- Iron Constitution (+1 heart) was a test trait and is gone; `Traits.apply` takes its heart back off anyone who had it.

To add a book: a line in `Skill`, a cover in `tools/make_librarian_textures.ps1`, its id in
`tools/make_skill_book_models.js` (models + the effect's lang line), and a row in `LibrarianBooks.md`.

## Testing
- **Automated:** `gradlew runGameTestServer` runs `librarian/SkillBookTests` (and `player/FoodDrinkTests`, via `gametest/GameTests`) headless (its own world in
  `runs/gameTestServer`, then quits): 13 tests covering every book (attribute values at I / II, mining
  speed on logs / rock / dirt, NPC prices, thirst, Cold, phantoms / bees / mob sight, Hunger, arrow damage and aim,
  meat drops, XP, healing, horse speed, powder snow and ice, sprint hunger, fishing lure, reading times and effects,
  trait books, each book's text at both levels, the roll odds, Lucky Librarian at the table). Each check logs
  `[ModTest] PASS/FAIL`. Test players are real survival `ServerPlayer`s on a mock connection
  (`NetworkRegistry.configureMockConnection`, marked as loaded so they can be hurt). Structure:
  `data/capitalism/structure/empty.nbt` (`tools/make_test_structure.js`). Not covered: how things look and feel (the reading screen, covers, Buffs/Debuffs page).
- **In game:** `/devpanel` (client) shows a panel in the bottom left: how long the last block took to mine, how long
  the one you're looking at would take, your speed in blocks/second (walking / sprinting / riding...), the attributes
  books change (with the % from normal), and your lessons and traits. `client/DevPanel`.

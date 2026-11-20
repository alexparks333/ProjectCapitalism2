# Economy — Gold, the Banker, Locks & the Leaderboard

Money works like Project Capitalism 1: **it's real gold** you carry and store.

| Item | Worth |
|---|---|
| Gold Nugget | 1/9 gold |
| Gold Ingot | 1 gold |
| Gold Block | 9 gold |

---

## In-game: the General Guide

`/guides` -> **General Guide** (everyone sees it; `client/GeneralGuideScreen`). Three areas:
- **Rules:** the server's rules, numbered.
  - They come from a plain text file the admin edits, `config/capitalism-rules.txt`: one rule per line, and lines
    starting with `#` are notes.
  - It's made with the defaults if it's missing, and read again whenever someone logs in (`core/ServerRules`, sent by
    `network/RulesPacket`). An edit shows at the next login, no restart needed.
  - The defaults:
    1. No griefing or stealing.
    2. No hacked clients or X-ray mods.
    3. No AFK farming or fishing.
    4. No creating mob farms.
    5. Report bugs, don't exploit them.
- **Wiki:** short entries in six tabs:
  - **Gold:** money, Raw Gold, `/balance` and `/pay`, gold belongs to its owner, locked containers, Shopfronts,
    the Banker (and `/price`), the Gold Leaderboard (and `/toggle leaderboard`), the Death Tax (a player kill only
    counts in The Purge; pointing to Insurance).
  - **Insurance:** what Not-A-Scam Insurance does, plans and prices (always "see /insurance quotes": the prices live
    in the server config, so the guide never hard-codes them), buying and cancelling (next day start), the daily premium
    (can't pay: cancelled until you buy again), Blood Moons (no cover; Blood Moon Insurance covers the next one, and its price shows: the server sends it at login
    with the rules, `network/RulesPacket`, `client/ClientRules`),
    The Purge, `/insurance status` and `/insurance guide`.
  - **Town:** the safe zone, town NPCs, the Town Hall, the Bounty Clerk, the Fisherman (rare fish need an
    enchanted rod), Real Estate. Not the Whisperer or the Wishing Well: left out on purpose.
  - **Events:** the Blood Moon, The Purge, festivals.
  - **Jobs:** how jobs work (one Essential and one Fun to start, picked with `/pick`; changing is rarely allowed), "Wealth Is Power", repairs. No entry per
    job: players get their job's guide when they pick it.
  - **Wellness:** the Wellness screen, thirst, water purity, diet, gold bars, temperature (body heat: the ball, what
    chills and warms you, Freezing and Hot), bonus hearts, daily cravings. No ailments (they were removed). No bar bonus numbers.
  - Some things are left vague on purpose, to be discovered: the Blood Moon's secrets (no bosses, crests, shard uses
    or the Blood Merchant), the Whisperer and the Wishing Well (not in the guide at all). **The guide never
    says what Wishing Stars or the Changing Star do.**
- **Crafting:** **Blocks** (Shopfront, Purification Table, Mailbox) and **Items** (Purified Water Bucket, Purified Water,
  Compacted Iron and Diamond), each with how to make it.
- A long entry scrolls with the mouse wheel. `gradlew runPhoto -Pphoto=guide` photographs each area and logs any entry
  too long for its panel.
- All of it is hand-written in `GeneralGuideScreen.buildEntries`. Keep it matching this file, `Insurance.md`,
  `Town.md`, `Events.md`, `Jobs.md`, `Wellness.md`, `Cravings.md` and the recipes.
## Commands

| Command | What it does |
|---|---|
| `/balance` | How much gold you're carrying, plus all your storage (Ender Chest, Investment Chest, owned containers, Shopfront earnings: the leaderboard's count) |
| `/price` | What the Banker pays for the item in your hand, per unit ("4 Lapis Lazuli for 1 gold nugget") |
| `/toggle leaderboard` | Hide or show the Gold Leaderboard sidebar on your own screen (client side, remembered in `config/capitalism-client-leaderboard.txt`; a Blood Moon's kill board still shows) |
| `/pay <player> <amount>` | Hand gold from your inventory to an online player |
| `/pay death [gold]` | Pay your death tax: all of it, or part (`Insurance.md`) |
| `/deathtax` | What you owe Death, and to whom |
| `/insurance` | Not-A-Scam Insurance: `quotes`, `buy <plan>`, `cancel`, `status`, `guide` (`Insurance.md`) |

- `/pay` takes ingots first, then nuggets, then breaks blocks, and **gives you change** (pay 5 with only a Gold Block and you get 4 ingots back).
- You can't pay yourself or pay more than you're carrying.
- If your inventory is full, change or gold you receive **drops at your feet** (it never vanishes).

---

## The Banker
- **What it buys (version 6, about 370 items):** almost everything you pick up, cheaply: raw meat and fish (below cooked), plants, seeds and flowers, mob drops (Blaze Powder, ink, echo shards...), every wood, plank and sapling, even stone, dirt and sand (a few nuggets a stack), storage blocks (iron, coal, diamond... exactly 9 of the piece, never more), the rare finds (Nether Star 20 gold, Elytra 20, Heavy Core 20, Dragon Head 10, Heart of the Sea 5, Wither Skeleton Skull 5, Trident 3), smithing templates, music discs and pottery sherds. Not arrows or gold. The list is in `CapitalismConfig` (`BANK_PRICES_V5` plus `BANK_PRICE_CHANGES_V6` and `BANK_PRICES_ADDED_V6`).
- **Upgrading a server's list** (`economy/BankPriceUpgrade`): when the server starts, a list from an older version is brought up to date ONCE: a line still at the old default takes the new price, new items go on the end, and anything the server's admins changed (or already price) stays as it is. Nothing is reset. `bankPricesVersion` records that it's done. Change prices in game with `/admin panel` (Banker tab).
- **Version 6 changes:** Emerald 5 gold -> **1 nugget** (villagers hand them out for 32 sticks: at 5 gold a village of Fletchers printed money; at anything over a nugget it still would), Totem of Undying 5 gold -> 2 (raid farms), and rare things up: Echo Shard 2 -> 5 gold, Shulker Shell 4 -> 6, Turtle Scute 6 -> 9 nuggets, Lapis 1 for 3 -> 1 for 2.

**`/banker`** (operators) spawns a Banker villager where you stand, facing you: it stays put, can't be hurt and never
despawns. Or put a **name tag "Banker"** on any mob (a villager works great; it won't open
its trade screen). **No Banker can ever be killed** (no damage gets through, not even `/kill`; lightning doesn't make it a
witch). **`/banker remove`** (operators) takes away the nearest Banker within 6 blocks.

- **Right-click** holding an item: sell one (one lot, for things sold "N for M").
- **`/price`** while holding an item: what it's worth, per unit.
- **Shift + right-click**: sell the stack. Cheap things sell by the lot ("1 nugget for 3"): only whole lots sell, the
  leftover few stay in your hand.
- Payouts come as **blocks, then ingots, then nuggets**, so big sales don't flood your inventory.
- Prices are in **gold nuggets** (9 = 1 gold), in `capitalism-server.toml` → `economy.bankPricesV5`: `item=N` (N a
  piece) or `item=N/M` (N for M). Priced by how long a thing takes to get and how farmable it is: about 25-60 gold an
  hour for steady chopping, mining, farming or fighting, more for rare or risky things, less for what automatic farms
  pour out. Some: Log 1, Coal 1, Iron Ingot 3, Lapis 1 for 2, Redstone 1 for 3, Raw Gold 6, Emerald 1, Diamond 54
  (6 gold), Netherite Scrap 126 (14 gold), Netherite Ingot 540 (60 gold: 4 scrap + 4 gold),
  Gunpowder 3, Ender Pearl 9, Golden Apple 54 (6 gold). **Every crop is 1 nugget for 2** (wheat, carrots, potatoes,
  beetroot, melon slices, sugar cane; apples and pumpkins 1 each): a Farmer can grow a mountain, so the bank pays little on purpose
  and crops are worth more sold to a Cook or another player.
  (Project Capitalism 1 paid whole gold ingots: a stack of wheat was 64 gold.)
- **No money loops:** anything craftable sells for less than it's made from (Golden Apple, Raw Gold, Leather,
  Netherite) (the Name Tag, 1 paper + 1 nugget, pays 1 nugget for 2 and the Saddle 7: capped in code, `BankerEvents.CRAFTED_CAPS`, whatever the config says), Arrows aren't bought, and the bank never pays more for something than an NPC shop could sell it for, with
  every discount at once (`BankerEvents.prices`, `NpcPricing.lowestNpcPrice`). That's why the Pet shop's Wheat is 4 for
  6 nuggets.

---

## Gold belongs to its owner (`deathtax/GoldCollector`, `mixin/HopperItemsMixin`, `mixin/MobPickUpMixin`)

Gold changes hands by `/pay`, Shopfronts and NPC trade, not by being picked up off the ground. Any gold a player
**drops** is **locked to them**:
- **What's locked:** nuggets, ingots, gold blocks, Raw Gold, and any item holding gold (a shulker box, a bundle, a
  bag).
- **When it's locked:** whether they drop it by dying (everything that drops while they die), tossing it (Q, out of
  a menu), or as change that didn't fit in their inventory.
- **Only they can pick it up.** Vanilla's pickup target is set to them, and anyone else's pickup is refused. No
  hopper, hopper minecart or mob (fox, allay, villager, zombie...) takes it, except that a piglin may take a gold ingot, so bartering with piglins still works. It only merges with their own
  dropped items.
- **It despawns like any dropped item:** after 5 minutes on the ground (the same as everything else you drop when you die), and lava, fire, cactus and the void destroy it, as in vanilla. Gold you were carrying when you died is gone if you don't get back to it in time.
- **Look at it to see whose it is:** small text under the crosshair ("Dev's gold", `client/GoldOwnerHud`). It's named that, but the floating name tag is hidden (too big).
- **`/pay death` can collect it** (`Insurance.md` > Paying). A Purge killer gets only the death tax, never the
  victim's dropped gold.
- Gold dropped before this rule existed isn't locked.
## Container locks

**Anything that holds items is locked to whoever places it:** chests, barrels, shulker boxes, furnaces, hoppers, dispensers, brewing stands, and the mod's machines.

| Who | Can open | Can break |
|---|---|---|
| Owner | Yes | Yes |
| Other players | **No** | **No** |
| Admins | Yes | Yes |

- **Looking at a container** shows who owns it.
- **Owner: shift + right-click with an empty hand** to lock/unlock (a double chest locks/unlocks both halves together). Unlocked containers can be opened by anyone but **still belong to you** (and still count for your net worth).
- **Blocked tricks:**
  - Joining your chest onto someone else's chest to make a double chest.
  - Putting a hopper under someone else's container to drain it.
  - Blowing up locked containers (TNT, creepers): they survive.
  - Breaking or pushing the **top half** of a 2-block-tall container (Fermenter, Shopfront) to spill the bottom half.
- Containers placed **before** this update have no owner until they're broken and placed again.

---

## Shopfront (market stall)

A **2-block-tall** market stall: wooden counter with produce crates, striped awning, and a crooked hand-painted "Shop" sign. Needs 2 blocks of space. **Crafting:** `White Wool, Redstone Block, White Wool` / `Stick, Gold Block, Stick` / `Planks, Chest, Planks`. Also explained in `/guides` → General Guide.

**Owner** (right-click your own stall):
1. **Click a listing box with an item on your cursor** to copy it as the picture of what you're selling (enchantments and all). The item stays on your cursor; clicking with an empty cursor clears the listing.
2. Put the **real items** into **Storage** (18 slots, e.g. 250 porkchops). Each purchase takes matching items out of Storage; none left = **Sold out**. Only the **exact** item matches (same enchantments).
3. Set **Amount** (how many per purchase) with the arrows.
4. Set the **price** with the arrows next to each gold icon: **Nuggets**, **Ingots**, **Blocks**, in any combination.
5. Customers' gold is stored inside. Press **Collect** to take your earnings.
- Shift + right-click your own stall to see what customers see.
- **Admins only (testing):** shift + right-click with an empty hand flips ownership between you and "Nobody", so one person can test both the owner and the customer side.

**Customers** (right-click anyone else's stall): each listing shows the item, amount, price as gold icons, and how many are left. Press **Buy**. The gold comes out of your inventory (with change) and the item goes into it.

**Safe by design:**
- Only the owner (and admins) can break it, change it or collect. Explosions, hoppers, pistons, fire, the Wither and the Ender Dragon can't touch it (either half).
- If the owner changes an offer while you have the stall open, your purchase is refused ("This offer just changed").
- Everything is checked by the server: stock, price and your gold.
- Breaking it (owner only) drops **only the Storage items** and the uncollected earnings. Listing pictures are copies, not items: they can't be taken out and never drop, so nothing can be duplicated.
- Stock and uncollected earnings count toward the owner's **net worth**.
- **No tax:** the owner gets the full price of every sale.

---

## The Investment Chest (`economy/InvestmentChestBlock`, `InvestmentBank`, `InvestmentMenu`, `client/InvestmentChestScreen`)

The bank's gold-plated vault chest (dark iron straps, a silver combination dial that spins while it's open, a coin slot,
gold coins heaped inside), with a crooked deep green sign (bold glowing gold letters) hanging in front of two posts above it, into the next block up:
**INVESTMENT CHEST**, and a little tag dangling off its corner: *(totally safe!)*. **Not craftable:** an op in creative
puts it in the bank (`/give @s capitalism:investment_chest`); nothing breaks it in survival, explosions and pistons can't
move it. It holds nothing itself, so breaking it (op, creative) loses nobody's gold.

**Every player has their own vault**, the same from every Investment Chest (like an Ender Chest). Nobody ever sees
anyone else's.
- **Left, YOUR VAULT:** 3 slots, **Gold Ingots only** (3 stacks: **192 gold at most**). Shift-click works both ways.
- **Middle, the market:** today's rate on the arrow (green on a bull run, red and blinking in a crash), its mood, and
  how many days it has left.
- **Right, DAILY RETURNS:** what the vault earns a day as it is now (amounts shown short: "3.8 gold"): exactly what the
  next morning pays. Then the gold **ready** ("Ready: 7 / 64 gold") and **COLLECT** (into your inventory; what doesn't fit drops at your feet, locked to you).
- A stock ticker of the bank's very serious advice scrolls along the bottom; the little chart only ever goes up.

**The market:** the rate rerolls every **3 days**, the same for everyone (from the world seed: nobody can reroll it):
usually **4 to 7%**, about 1 stretch in 10 a **BULL RUN** (8 or 9%), about 1 in 10 a **CRASH** (2 or 3%). A new
stretch is told, in one short chat line, to each player with gold in a vault, only them ("[Investments] 📊 New rate: 8%
BULL RUN! (3 days)").
(`investmentMarket = false` in the server config: always `investmentDailyPercent`, 6%.)

**What it pays:** every morning (a new calendar day), the rate of the day that just ended on **whatever is in the vault
at that moment**, rounded down to the nugget: at 6%, **50 gold → 3 gold a day** and a full vault (192) → **11 gold
4 nuggets**; at 9%, a full vault makes 17 gold 2 nuggets, at 2% only 3 gold 7 nuggets. Each online investor gets one
short chat line, seen only by them ("[Investments] 📈 +3.8 gold (6%) ready to collect");
offline investors are paid too and find it waiting.
- Gold put in just before morning earns like any other (gold can only be in one vault when morning comes, so nothing
  can be doubled); an empty vault at morning earns nothing.
- **Once a day:** a skipped night or `/calendar set` forward pays once; setting the clock back pays nothing.
- Returns are new gold from the bank (a faucet, on purpose). Returns waiting don't earn more: collect them.
- **Ready holds 64 gold at most.** Once it's full the vault earns nothing ("READY IS FULL", and a red chat line each
  morning) until it's collected: an investor has to come back to the bank now and then.

**The money stays honest:**
- Vault gold and returns waiting **count in your net worth** (the leaderboard, `/balance`, the death tax). They're
  counted in the same recount as your inventory (`GoldLedger`), so moving gold in or out is never counted twice or lost.
- **`/pay`, Shopfronts and NPCs can't spend it** (they only take from your inventory), nor does `/pay death` collect it:
  take it out first.

---

## Gold Leaderboard (net worth)

The sidebar shows everyone's **total gold**, updated every 10 seconds:

**Net worth = gold on you + gold in your Ender Chest + your Investment Chest vault (and returns waiting) + gold in every container you own**

- **Raw Gold counts** at its smelted value: 1 Raw Gold = 1 gold, a Block of Raw Gold = 9 (`GoldMoney.rawValueOf`). It isn't money for `/pay` or Shopfronts until it's smelted, and `/balance`'s "carrying" figure is spendable gold only (Raw Gold shows in its storage figure).
- Gold inside **shulker boxes and bundles** counts too, and so does what's **on your back**: a worn Backpack's
  contents, and anything a pet carried in an Animal Bjorn is holding.
- "Every container you own" means every block that stores items that you put down: chests, barrels, the job
  stations (ovens, fermenters, tables...), Shopfronts (stock + earnings), Food Troughs and **Mailboxes** (mail
  others post into yours counts for you). No mod block or item keeps items anywhere the count can't see; plates only
  hold meals.
- **Not counted for anyone:** the town's own containers, the **Town Hall Mailbox** and the **Mayoral Chest** (and the **City Chests**, which hold what agents pay in for lots)
  (`Town.md` > The Town Hall). They aren't locked to whoever placed them, and whatever they hold is the town's.
- Containers in far-away (unloaded) areas keep their last count, so your net worth doesn't jump around.
- Players who log off keep their score.

**Why it can't double count** (Project Capitalism 1 doubled gold in double chests): every container is counted **once, by its exact position, and only its own slots**. A double chest is two containers of 27 slots each. Everything is counted in the same tick, so gold moving around can't be caught twice.

---

## Config (`capitalism-server.toml` → `[economy]`)

| Setting | Default | Meaning |
|---|---|---|
| `bankPricesV5` | see above | What the Banker pays in nuggets, `"item_id=N"` or `"item_id=N/M"` (the key name stays V5; the list inside is upgraded) |
| `bankPricesVersion` | set by the mod | Which version of the Banker's list the server has; leave it alone |
| `leaderboardEnabled` | `true` | Show the sidebar |
| `leaderboardIntervalTicks` | `200` | Update speed (200 = 10 sec) |
| `investmentMarket` | `true` | The Investment Chest's market: the rate rerolls every 3 days (4-7%, rarely 8-9% or 2-3%) |
| `investmentDailyPercent` | `6` | The Investment Chest's rate when the market is off, % of what's in the vault each morning |

---

## The admin panel (`core/AdminPanel`, `client/AdminPanelScreen`)

**`/admin panel`** (operators) opens the live admin panel. Three tabs, and a search box that filters each:
- **Settings:** every setting in `capitalism-server.toml` (all of `CapitalismConfig` but the Banker's list), by section.
  Click a value, type, Enter saves (Esc cancels); on/off settings flip with a click. Hover a row for what it does and
  its range. Out-of-range or non-numbers are refused. A change saves to the file at once and most take effect at once.
- **Banker:** every item the Banker buys and its price (`54` = 54 nuggets each, `1/2` = a nugget for 2, `0` = not
  bought). Under each name, what it really pays: red when an NPC shop's price or a crafting cap makes it less.
  **+ Held item** adds what's in your hand (at 0 until you price it).
- **Commands:** every operator command with what it does (hover for the whole line). Click one and it's typed into
  chat, up to the first thing to fill in.

Every change is logged on the server console and told to the admin who made it in chat. The client sends
`network/AdminSetPacket`; the server checks the sender is an operator, then sends the new figures back
(`network/AdminPanelPacket`). Keep the Commands tab's list (`AdminPanelScreen.COMMANDS`) matching the commands.

---

## For developers

| What | File |
|---|---|
| Counting, taking and giving gold | `economy/GoldMoney.java` |
| `/balance`, `/pay` | `economy/EconomyCommands.java` |
| Banker | `economy/BankerEvents.java` |
| Locks (place, open, break, explosions, hover) | `economy/ContainerLocks.java` |
| Owner stored on each container | `economy/ContainerOwner.java` (attachment in `core/ModAttachments.java`) |
| Saved ledger of containers + player gold | `economy/GoldLedger.java` |
| Leaderboard | `economy/GoldLeaderboard.java` |
| Investment Chest (vaults, mornings, menu) | `economy/InvestmentBank.java`, `InvestmentChestBlock.java`, `InvestmentMenu.java`, `client/InvestmentChestScreen.java`, `client/InvestmentChestRenderer.java` (texture: `tools/make_investment_chest.ps1`) |
| Shopfront | `economy/ShopfrontBlock.java`, `ShopfrontBlockEntity.java`, `ShopfrontMenu.java`, `client/ShopfrontScreen.java` |
| Death tax, Purgatory, `/pay death`, insurance | `deathtax/` (`Insurance.md`) |

Future systems that cost money (Shopfront, insurance, job changes) should use `GoldMoney.take()` / `GoldMoney.give()`.

**The death tax** (replacing Project Capitalism 1's Death Realm): dying owes 25% of all your gold (your net worth as the leaderboard counts it, Raw Gold included at 1 gold each),
less with Not-A-Scam Insurance, and until it's paid (`/pay death`) you're In Purgatory: no mining, no fighting. In The
Purge a player's kill sends that tax to the killer. Insurance premiums and the bank's share of the tax are gold sinks.
All of it: `Insurance.md` (`deathtax/`).

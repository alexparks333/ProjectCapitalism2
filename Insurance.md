# The Death Tax, Purgatory and Not-A-Scam Insurance

Code: `src/main/java/com/alexh/capitalism/deathtax/`:
- `DeathTax`: the tax, Purgatory, `/pay death`, `/deathtax`.
- `Insurance`: plans, premiums, Blood Moons, `/insurance`.
- `DeathTaxData`: a player's debts and policy (attachment `ModAttachments.DEATH_TAX`).
- `DeathTaxLedger`: server-wide records (saved data `capitalism:death_tax`).
- `DeathTaxTests`.

Also `GoldCollector` (collecting it when they pay, and tagging their gold on the ground).
Client: `client/PurgatoryHud` (the death tax board, and the skull by names), `client/InsuranceGuideScreen` (the terms
of service). The Purge is in `Events.md` (`event/Purge`). Project Capitalism 1 had a Death Realm with a toll; this
replaces it.

## The death tax
- **Pity:** with **10 gold or less** to your name (everything the tax counts) there's no death tax at all (`DeathTax.PITY_NUGGETS`): "Death takes pity on you". Applies in The Purge too.
- **Cap:** one death never costs more than **75 gold** (`DeathTax.MAX_TAX_NUGGETS`), whatever the rate and however much gold you have. Applies in The Purge too.
- **A Phoenix Feather** (only from the Traveling Merchant, `Town.md`) in your offhand when you die: no death tax that time (even in The Purge), and it burns away (`DeathTax.died`).

- **Plain PvP never brings the tax:** killed by another player (their hit, arrow or pet, or the last player to hurt you) outside The Purge, there's no death tax and no Purgatory (`DeathTax.byOtherPlayer`). In The Purge a player's kill is taxed and the tax goes to the killer (below). The killer gets only the tax, never the gold you drop: dropped gold is locked to its owner (`Economy.md` > Gold belongs to its owner).
- **When a player dies they owe Death 25% of ALL their gold:** their net worth exactly as the Gold Leaderboard counts
  it (`DeathTax.taxableGold` recounts with `GoldLeaderboard.recount` and reads `GoldLedger.netWorth`, so the two always
  agree):
  - Carried: the whole inventory, the cursor, shulker boxes and bundles in it, the bag on their back.
  - The Ender Chest.
  - Every container they own (`GoldLeaderboard.ledgerOwner`), Shopfront earnings included. Ones in unloaded chunks
    count at the ledger's last count. The town's chests (City Chests, the Mayoral Chest, the Town Hall Mailbox) are
    no one's.
  - **Raw Gold counts** at its smelted value: 1 Raw Gold = 1 gold, a Block of Raw Gold = 9 (`GoldMoney.rawValueOf`). It's not money for `/pay` until smelted.
  - It's measured the moment they die, before anything drops (`LivingDeathEvent`, lowest priority, skipped if
    something cancelled the death), and rounded down to whole nuggets (`DeathTax.tax`, exact integer maths).
  - Example: 32 gold on them and 32 in their own chest is 64 gold, so 16 owed.
- **Nothing is taken: it's a debt.** If the tax comes to 0 (no gold, or a 0% rate), nothing happens.
- **What happens to their things (checked in code and tests):**
  - Without `keepInventory` (the default), their inventory drops as in vanilla. The gold in it drops in full, as
    items, where they died, **locked to them**: only they can pick it up, no hopper or mob takes it, and it
    despawns after 5 minutes like any drop (`Economy.md` > Gold belongs to its owner).
  - With `keepInventory` they keep it.
  - The Ender Chest is never touched.
  - Either way the tax is worked out once, from the gold before the drop. The tax never removes or duplicates gold.
  - Worn bags and Bjorns behave as before (`Back.md`).
  - Their gold that drops is **tagged as theirs on the ground** (see Paying), so `/pay death` can collect it.
- **The debt is saved on the player.** It's kept through death, logouts and restarts.
- **Dying again while in debt adds** that death's tax (on the gold they have then, after insurance) to what they owe.
- **Messages:**
  - In chat as they die: the rate, why ("uninsured", "Silver plan", "Blood Moon, no insurers", "Blood Moon
    Insurance"), the gold it was taken on, the tax, and the total owed.
  - After respawning (or joining) while they owe, the **death tax board** (`PurgatoryHud`):
    - Only a slim black strip in the day boards' style at the top middle of the screen: "☠ Death Tax: 8 gold · /pay death".
      It slides in after a respawn or join, stays while they owe and goes when they've paid. (No big card: too much in the way.)
    - It's only drawn: it never takes the mouse or keys.
    - The full explanation lives in `/insurance guide`.
- Rates (server config `[insurance]`): `deathTaxPercent` 25, less with a plan, 0 with Blood Moon Insurance during a
  Blood Moon.

## Purgatory

While a player owes anything they're **In Purgatory**:
- **No breaking or mining blocks:**
  - Breaking is cancelled (`BreakBlockEvent`).
  - Digging makes no progress (`BreakSpeed` is cancelled on both sides, so there's no fake cracking).
  - A left click on a block does nothing.
- **No hurting anything:**
  - The attack key does nothing (`AttackEntityEvent`).
  - Any damage they're behind is cancelled: melee, arrows, thrown tridents, potions, TNT they lit, and their tamed
    pets' bites.
- **What still works:**
  - Walking, eating, drinking, opening chests, doors, buttons, beds.
  - Trading, Shopfronts, NPC shops, the Banker, fishing, `/pay`.
  - So they can go and raise the gold.
- **Monsters can still hurt them.**
- **Using the world to break blocks for them is allowed** (pistons, TNT they placed). That's left alone on purpose.
- **The job cost:** a Farmer can't harvest (that's breaking blocks) and a Butcher can't slaughter while In
  Purgatory. Both can still sell what they have, so the debt is never impossible to pay.
- **Seeing it:**
  - Everyone sees a grey **☠** before their name, above their head and in chat. `IN_PURGATORY` is synced to
    everyone who can see them, and the name is worked out again when it changes.
  - The player sees the death tax board (above).
- **Creative and spectator players aren't held back** (they can't die anyway).
- **Paying everything frees them,** with a chime and a message.

## Paying: `/pay death`

- `/pay death` pays the whole debt. **Death collects from all their gold within reach** (`GoldCollector`), in this
  order:
  1. **Their inventory:** the cursor, shulker boxes in it, the bag on their back (through its live slots, so nothing
     is duplicated).
  2. **Their Ender Chest, then every container they own in a loaded chunk** (locked or not), then their Shopfronts'
     uncollected earnings.
  3. **Their gold lying on the ground.** Gold and raw gold item entities are tagged with whose they are (and how many
     of the stack) when they drop: everything that appears while they're dying (their inventory, a spilled bag), and
     what they throw.
     - The tag stays for good: the item is locked to them (`Economy.md` > Gold belongs to its owner). Nobody else can
       pick it up, and it despawns after 5 minutes like any drop.
     - Only loaded entities can be reached. An emptied entity is discarded at once.
  - **Raw Gold never pays.** It counts toward their gold (so it's taxed), but Death won't take it: they smelt it into
    ingots first (furnaces work fine in Purgatory), then /pay death. If Raw Gold is all they have left, /pay death says so.
- **Within each place:** ingots first, then nuggets, then blocks. A broken block (or the last ingot)
  overshoots: the change goes back into their inventory, or drops at their feet if it's full.
- **Never touched:** anyone else's containers, the town's chests, other players' gold on the ground.
- **Unloaded chunks:** chunks are never loaded, and containers in unloaded ones are skipped.
- **Not enough within reach?** It takes what it can as a part payment and says so: "Paid 10 gold of 16 gold: 6 gold
  left; some of your gold is in unloaded chests" (or "that's all the gold Death could find"). Nothing within reach:
  nothing is paid.
- `/pay death <gold>` pays part (never more than is owed).
- `death` is a literal, so it's matched before any player name. Literals match exactly, so a player actually named
  Death is paid with `/pay Death <gold>` (capital D).
- **Where it goes:**
  - The bank's share leaves the economy (a gold sink).
  - A Purge killer's share goes to the killer: into their inventory (dropped at their feet if it's full), or kept in
    `DeathTaxLedger` and handed over at their next login.
- **Several debts:**
  - A player can owe the bank and several Purge killers at once, one debt per creditor, oldest first.
  - A part payment is shared between them by size. Each share is rounded down, and the leftover nuggets go one each to
    the oldest debts first, so the shares add up exactly.
  - Paying in full pays every debt exactly.
  - The chat shows the breakdown.
- `/deathtax`: your debts and your last death's tax.
- Ops:
  - `/deathtax <player>` shows theirs.
  - `/deathtax <player> forgive` wipes their debts. It's the only way besides paying, and an explicit admin command.
    Creditors get nothing for it.

## Not-A-Scam Insurance: `/insurance`

Flat company prices, **not** NPC prices: an exception to the `NpcPricing` rule (`CLAUDE.md`), like the Bounty fee.
Server config `[insurance]`:

| Plan | Gold a day | Death tax |
|---|---|---|
| (none) | - | 25% |
| Bronze | 1 | 20% |
| Silver | 3 | 15% |
| Gold | 7 | 10% |
| Premium | 20 | 5% |
| Blood Moon Insurance | 30 once | 0% during the next Blood Moon |

**Commands:**
- `/insurance` or `/insurance status`: your plan, whether you're covered today, tomorrow's change, your Blood Moon
  cover and the tax if you died now. (What you owe is `/deathtax`, not here.)
- `/insurance quotes`: the prices.
- `/insurance buy <bronze|silver|gold|premium>`: starts at the **next Calendar day start**, never today, and nothing is
  charged at purchase. Buying a different plan while insured switches the next morning; the old plan covers you until
  then. Buying your current plan while it's due to change keeps it (the change is off).
- `/insurance cancel`: your plan ends the next morning; you're covered until then. A plan that hasn't started yet is
  just dropped.
- `/insurance buy BloodMoonInsurance` (any capitalisation).
- `/insurance guide`: the terms of service (below).
- Everything tab-completes.

**Premiums:**
- At each day start, the day's premium is taken from the gold they carry: when the day starts if they're on, else
  when they log in that day (`Insurance.process`, also run before any death or `/insurance`).
- Days they're not on aren't charged. Nobody dies offline, so this can't be dodged: logging in charges for the day.
- **If they can't pay, the plan lapses:** it ends, there's no cover that day, a message says so, and no debt is created.
  They can buy again; it starts the next morning.
- Cover is for deaths while the premium for that day is paid.

**Blood Moons:**
- During an active Blood Moon the insurers hide and **insure nobody**: every death pays the full 25%.
- The exception is **Blood Moon Insurance:**
  - 30 gold once, and it covers every death (0%) during the **next Blood Moon that starts after it's bought**.
  - Bought during a Blood Moon, it's for the next one.
  - One at a time: a second purchase is refused while one is unused.
  - It's used up when that Blood Moon ends.
  - How it's tracked: `BloodMoon.start` counts Blood Moons (`DeathTaxLedger.moonsStarted`) and the insurance stores
    the number of the one it covers.
- **Announcements:** prefixed **[Not-A-Scam Insurance]** in teal. They're sent when a Blood Moon becomes known (the
  altar, or an admin's `/event bloodmoon delay`) and again when one begins: "You all will not be insured for this
  coming Blood Moon. Buy Blood Moon Insurance if you want it."
- Plans **do** cover Purge deaths (on a night that's a Blood Moon too, the Blood Moon's insurance rules apply).

**The guide** (`InsuranceGuideScreen`):
- A legal document on cream paper: the "NOT-A-SCAM INSURANCE CO." letterhead, "TERMS OF SERVICE", ten numbered
  clauses and the fine print.
- It covers: the tax, Purgatory, paying, the plans, when cover starts, premiums, the Blood Moon exclusion, Blood Moon
  Insurance, The Purge, and your account.
- It scrolls with the mouse wheel. The button reads "I agree (I have no choice)".
- Its numbers come from the server config. Keep its text matching this file.

## The Purge and the tax

In The Purge (`Events.md` > The Purge), when **another player** kills them (or last hurt them, if they fell or burned)
**outside the town**, that death's tax, after insurance, is owed to the **killer**:
- The killer is told.
- It counts as a Purge kill on the standings.
- When the victim pays, the gold goes to the killer.

Killed by a monster, by themselves, inside the town, or after dawn, the tax goes to the bank. No gold is created:
every coin a killer gets was collected from the victim's own gold.

**Standings** (the Bounty Clerk's Ledger, `Bounties.md`):
- **Purge kills:** counted at the kill.
- **Gold taken:** whole gold actually paid to them from Purge debts, counted as it's paid.

## Testing

`deathtax/DeathTaxTests`:
- **"death_tax":**
  - Rounding.
  - Carried + Ender Chest counted, a chest that isn't theirs not counted.
  - The carried gold drops in full and the Ender Chest keeps its gold.
  - The debt and the skull survive respawning.
  - Stacking on a second death.
  - Broke: nothing.
  - `keepInventory` keeps the gold and still taxes once.
- **"death_tax_collect":**
  - 32 on them + 32 in their own chest = 64 gold, so 16 owed.
  - Their dropped gold is tagged.
  - A City Chest and another player's chest aren't in their gold.
  - The order: inventory, then their chest, then their gold on the ground (exact counts at each step); Raw Gold never pays, kept to smelt.
  - Never another player's chest, the City Chest or another player's ground gold.
  - A gold block for 21 nuggets gives 60 back in change.
  - An unloaded chest counts at its last count, but paying takes only what's in reach and reports it.
  - Tossed gold is locked to its owner and named "Owner's gold". Nobody else can pick it up, it despawns after 5 minutes like any drop and
    a hopper can't take it (though it takes gold nobody dropped). Its owner picks it up.
  - A shulker box holding gold is locked; an empty one isn't.
- **"purgatory":**
  - No breaking.
  - No melee, thrown or potion damage, and none from their pet.
  - Monsters still hurt them.
  - Chests open and bread is eaten.
  - Paid up, they mine and fight again.
- **"pay_death":**
  - Short: Death takes what it can find.
  - Part payments and exact change.
  - Owing nothing takes nothing.
  - `/pay Death 3` pays a player named Death while `/pay death` pays the tax.
  - Shares by size (4/3/3).
  - Bank 30 + killer 60 paid in two goes: the killer gets exactly 60.
- **"insurance":**
  - Next-day start, a premium charged once a day, a death at 15%.
  - The plan survives death.
  - Switching (old plan covers today) and cancelling (covered today, nothing next day).
  - Lapse on an unpaid premium (no cover, no debt).
  - Cancelling a plan that hasn't started.
  - A bad plan name.
  - The default quotes.
- **"blood_moon_insurance":**
  - Plans don't cover a Blood Moon.
  - Bought during one: covers the next, not this one.
  - One at a time.
  - Covers every death that night.
  - Used up after.
  - Bought before one: covers it.
- **"purge":** see `Events.md`.

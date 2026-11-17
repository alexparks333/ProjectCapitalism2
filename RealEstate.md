# Real Estate Agent (Fun job)

The town center (`Town.md`) is protected: nobody can break or place blocks there. The city splits it into **lots**,
which only a **Real Estate Agent** can buy. An agent builds on what they own, and sells or leases lots to other
players, who can then build on them.
`Job.REAL_ESTATE_AGENT` (the last job). Code: `realestate/` (`Property` = one lot, `Properties` = every lot, saved with
the world, and the rules; `PropertyCommands`, `PropertySignBlock`, `SurveyStakeItem`, `RealEstateEvents`,
`RealEstateTests`), `town/TownProtection` (who can build where), `client/RealEstateGuideScreen`. Art:
`tools/make_real_estate_textures.ps1`.

## Lots (`Property`, `Properties`)
- A lot is a **named rectangle** of the Overworld, corners included, its **whole column** (sky to bedrock). Lots can't
  overlap. Names are one word (letters, numbers, `-`, `_`), not case-sensitive.
- **The city** owns a lot until an agent buys it. Then it has an **owner** (the agent, or a player the agent sold it to)
  and maybe a **tenant**.
- **Building:** in the town center only **ops**, and on a lot **its owner and its tenant**, can break or place blocks
  (`TownProtection.canBuildAt`). Outside the town center lots change nothing.
- Prices are gold (`economy/GoldMoney`): the city's price, the agent's asking price, the rent a day.

## Making lots (ops)
- The **Survey Stake** (`survey_stake`, creative menu / `/give`; ops only): **left-click** a block for one corner,
  **right-click** one for the other (the size shows), then **`/properties create <name>`**. While you hold it, the lots
  nearby show as **green particle boxes** and the corners you're marking as **white** ones (`RealEstateEvents`).
- To put it up for sale, put a **City Chest** on the lot (below) and, to show it, a **For Sale by the City** sign
  (`city_sale_sign`, ops only). Right-clicking that sign only tells people about the lot; nothing is bought there.
- **`/properties delete <name>`**.

## Buying a lot: the City Chest (agents)
**The City Chest is the only way to buy city land.** Agents never pay by command or from their pockets: they put the
things and the gold in the chest.
The **City Chest** (`city_chest`, `CityChestBlock`, `CityChestBlockEntity`, `CityChestMenu`, `client/CityChestScreen`):
a navy strongbox banded with gold. The city's price for a lot in **things and gold**.
- **Only an op in creative** can put one down, and only **on a lot**. It can't be broken in survival (an op in creative
  can remove it) and drops nothing.
- **The price (top row) can never be taken out** by anyone but an op setting it up in creative: not by clicks,
  shift-click, number keys or double-click gathering, not by hoppers or hopper minecarts (nothing goes in or out
  except by hand), not by explosions, withers or the dragon (`wither_immune`, `dragon_immune`). If an op breaks the
  chest only what an agent put in drops, never the price. **On completion every item in it is deleted first, then the
  lot becomes the agent's**; only extras over each wanted amount (and gold change) go back.
- **Setting it up:** the op (in creative) opens it and puts what the city wants in its **top row** (up to 9 stacks; the
  counts matter). **Gold there is the gold price** (50 Gold Ingots = 50 gold). There's no gold command.
- **What the agent sees:** the top row **ghosted** (slightly dimmed: it shows what's wanted, it isn't really in there)
  and, **right under each wanted thing, its own slot**: **red** until it holds enough, then **green**. A slot only
  takes the thing above it; a **gold slot takes any gold** (nuggets, ingots or blocks: change comes back).
- **Only a Real Estate Agent can open it** ("Only a Real Estate Agent can open the City Chest."). What they put in
  **stays** (it can take several trips).
- **The moment every slot is green**, what's wanted is used up (the gold goes to the city), anything else comes
  back to the agent (gold over the price as change), **the chest disappears** (a sparkle and a level-up sound) and **the
  lot is the agent's** (`Properties.claimFromCity`).

## Selling and leasing (agents)
- **Signs** (agent-only recipes, `capitalism:job_shaped`): **For Sale** = Red Dye over any sign, **For Lease** = Blue
  Dye over any sign. Only an agent can put one up, and only on a lot they own. Right-clicking one tells anyone the lot's
  size and price (or rent) and whom to talk to.
- **`/properties <name> price <gold>`**: the asking price on its For Sale sign. **`/properties <name> rent <gold>`**:
  the rent a day on its For Lease sign.
- **`/properties <name> sell <player>`**: the lot becomes the player's (they can build; the agent can't any more). The
  price is settled between them however they like (`/pay`, trade...). A leased lot can't be sold until its lease ends.
  A player who owns a lot but isn't an agent can build on it but can't sell or lease it.
- **`/properties <name> lease <player>`** (needs a rent): the player can build on it; the agent keeps it.
- **Rent:** every Minecraft day the tenant pays the rent from their gold, straight to the agent
  (`Properties.collectRent`, checked every 10 seconds; an offline tenant pays what's due when they log in; rent paid to
  an offline agent waits and is handed over at login). **A tenant who can't pay loses the lease** (both are told).
- **`/properties <name> evict`** (the owner) ends a lease.

## Everyone
- **`/properties list`**: every lot and what it's up to. **`/properties <name>`**: one lot's size, corners and status.
- `/property` works too.

## Guide
The **Real Estate Agent's Guide** in `/guides` (agents only): Workshop > Signs (both recipes); Information > Lots (City
Land, Buying a Lot, Building, Your Lots), Selling & Leasing (Pricing, Selling, Leasing, Rent, Ending a Lease), Player
Debuffs (No City Land, No Signs). Short lines, no numbers.

## Testing
`realestate/RealEstateTests` (`real_estate`, on a fresh set of lots, not the world's): making lots (size, names, no
overlaps, whole column), buying off the city (agents only, never from their pockets), who can
build, leasing (needs a rent, owner only, the tenant builds, can't sell a leased lot), the rent a day to the agent (once
a day), a tenant who can't pay loses the lease, ending a lease, selling (owner only, the buyer builds, the agent can't),
a non-agent owner can't lease it out, deleting; the City Chest (waits for every wanted thing and the gold, then gives
back the extras and the change; gold in the wanted row is a gold price paid in nuggets, ingots or a block with
change), only an agent can unlock a lot.

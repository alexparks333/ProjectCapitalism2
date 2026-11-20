# Bounties: the Bounty Clerk

Code: `src/main/java/com/alexh/capitalism/bounty/` (`BountyTask` the catalogue, `Bounties` the rules, `BountyEvents`
how tasks count, `BountyData` a player's bounties, `ShopIndex` the town's Shopfronts, `BountyTests`) and
`town/BountyClerk`, `BountyMenu`, `BountyCommands`, `client/BountyClerkScreen`, `BountyClerkModel`, `BountyClerkRenderer`
(skin by `tools/make_bounty_clerk.ps1`). The NPC is in `Town.md` > The Bounty Clerk.

Something to do when you're bored: the Bounty Clerk keeps one task posted for the whole server ("Kill 6 Zombies").
Take it for a small fee (2 nuggets for an Easy task up to 2 gold for a Heroic one); it never expires and pays gold when you turn it in. Tasks are a bonus, not a job: the
economy pays about 25-60 gold an hour (`Economy.md`); a task pays 5 to 55. The tasks are about fighting, mining, wood
and meat, fishing, the Nether, structures and the town's trade (niche ones like taming, milking and odd deliveries were
cut on 2026-11-19). Every task's pay is its base gold plus a tier bonus (`BountyTask.TIER_BONUS`): Easy +2, Medium +3,
Hard +4, Heroic +5.

## The rules

- **One task posted for everyone** (`BountyBoard`, saved with the world). Everyone sees the same one; **the moment
  someone takes it, a new one is posted** for everyone else (it's sent to anyone with the screen open), so no two
  players take the same task. A new one is never one of the **last 20 posted**.
- Posting: a tier by weight (**Easy 40%, Medium 30%, Hard 20%, Heroic 10%**), then any task of it; its amount rolled in
  its range, its reward in proportion ("Kill 4-7 Zombies": 4 pays 5 gold, 7 pays 7). A town task (the
  Shopfront ones) is only posted if someone on can do it; if it's posted and you can't do it (no Shopfront...), it stays up and
  your button says why.
- **One task at a time.** Taking the posted task **exchanges** it for yours: your old one is dropped, no refund (the
  screen asks first: "This replaces your current task: ..."). The fee goes by tier (`Bounties.FEES`): **Easy 2 nuggets, Medium 6 nuggets, Hard 1 gold, Heroic 2 gold** (a flat fee, not an
  NPC shop price, so not through `NpcPricing`: see `CLAUDE.md`). Creative players take them free.
- **One take per Calendar day**, even if you finish and turn in the first at once. The screen counts down to the next.
- **Tasks never expire.** **Abandon** drops yours (click twice; no refund).
- **Progress counts only from when you take it.**
- **Turn in** a finished task at the Clerk for its gold, with a stamp, a page turn and a flourish of particles.
  **Deliveries** count what's in your inventory and take the items at turn-in; a delivery always pays at least a gold
  more than the Banker would give for those items (`Bounties.reward`).
- **The race for three stars:** the **first 3 players on the server** to turn in **50** bounties each earn **one Fun
  Wishing Star**. A place is **reserved the moment you reach 50** (so someone who claims faster can't take it); claim it
  in the Ledger, with a fanfare and "🏅 X is the 2nd to complete 50 bounties and earns a Fun Wishing Star!". After 3,
  nobody else gets one. The winners (id and name, in order) are kept in `BountyBoard`.
- A player's task, the day they took one, how many they've turned in and the gold it earned are saved on them
  (`ModAttachments.BOUNTIES`), kept through death and synced to them, with a copy of the board (the posted task, the
  winners, their place) for their screen: on the client it's all `Bounties.of(player)`.

## The Clerk's screen

- **Today's Task:** the posted task pinned up as a notice: its title, its tier, the reward, the fee; **Take this task**
  (asks before replacing yours). If you can't: why (taken one today, with the countdown; not for you; no fee).
- **My Task:** your task with its tier colour, a progress bar, the reward, **Turn In** (when it's done) and **Abandon**.
- **Ledger:** bounties turned in, gold earned, "Reward at 50 completions" with "3 rewards left · you: 23 / 50" (the
  screen never says what the reward is; the fanfare and the announcement do, once it's claimed), your reward to claim,
  or "All 3 rewards have been claimed"; then the standings.
- Parchment text is dark ink with no shadow; the notice has a tier strip along its top and a red wax seal on its corner.
- **Standings** (on the Ledger): server-wide leaderboards, offline players included, one board per category
  (`bounty/Standings`, saved with the world: category -> player -> score, an extra number, name, and when they reached
  that score). Four boards so far: **Cravings satisfied** (`Cravings.md`), **Purge kills** and **Gold taken** (The Purge: `Insurance.md`, `Events.md`) and **Bounties** (score: bounties turned in; extra: gold earned; updated on
  every turn-in, seeded from a player's own data when they log in, and by an admin reset); ◀ ▶ switch between boards
  once there are more (to add one: a `Standings.Category` and a `set()` call where its score changes). The top 10 with rank, name, bounties and gold; gold,
  silver and bronze for the first three, ★ by the Fun-star winners, your own row picked out; outside the top 10, "You:
  #14, 9 bounties" underneath. On a tie, whoever reached the count first ranks higher. Sent when the screen opens and
  after every turn-in.
- The menu holds your inventory, hidden, so gold and deliveries on the screen stay up to date while it's open.
## How tasks count (`BountyEvents`)

(Some of these kinds are no longer used by any task after the 2026-11-19 trim, e.g. taming, shearing, mail; the code still
counts them, ready for new tasks.)

Kills (by the player or their arrows), "at night" kills, mining (not in creative; a break something else cancelled
doesn't count), harvesting (crops only when ripe), planting saplings, breeding, taming, shearing, milking, fishing (any
fish, a particular fish, treasure), rare catches (`fishing/RareCatchModifier`, so the Luck of the Sea rule holds),
uncorking a Message in a Bottle, cracking a Sunken Pirate Chest, smelting and cooking (taking the result out), brewing,
enchanting, getting something repaired at a town smith NPC (the anvil repairs nothing), trading with villagers, distance (walking, rowing, riding a horse, any travel;
teleports don't count), reaching a height, biomes visited in one day, a structure chest opened while its loot is still
unrolled (shipwreck, desert temple, ocean ruin, bastion), a vault opened with a key (trial or ominous), coming back from
the Nether, winning a raid (Hero of the Village), bosses (hurting it or being within 32 blocks when it dies counts),
Blood Moon Level II monsters, kills in one Blood Moon, surviving a whole Blood Moon, topping its kill board, sending
mail, buying from or selling through a Shopfront, buying a meal (the mod's food) from a Cook's Shopfront, feeding your
pet.

## The tasks (144 in all; amounts and gold are ranges, gold as paid: the base plus the tier's bonus)

### Easy (32 tasks)

| Task | Gold |
|---|---|
| Kill 4–7 Zombies | 5–7 |
| Kill 4–7 Skeletons | 5–7 |
| Kill 4–7 Spiders | 5–7 |
| Kill 2–4 Creepers | 5–7 |
| Kill 4–7 Drowned | 5–7 |
| Kill 4–7 Husks | 5–7 |
| Kill 4–7 Strays | 5–7 |
| Kill 3–6 Slimes | 5–7 |
| Kill 4–7 Cave Spiders | 5–7 |
| Kill 5–8 Skeletons at night | 5–7 |
| Mine 16–24 Coal Ore | 5–6 |
| Mine 12–20 Copper Ore | 5–6 |
| Mine 3–5 Iron Ore | 5–7 |
| Mine 48–80 Stone | 5–6 |
| Chop 24–40 logs | 5–6 |
| Chop 24–40 Oak logs | 5–6 |
| Chop 24–40 Birch logs | 5–6 |
| Chop 24–40 Spruce logs | 5–6 |
| Deliver 4–6 Raw Beef | 5–7 |
| Deliver 4–6 Raw Porkchops | 5–7 |
| Deliver 4–6 Raw Chicken | 5–7 |
| Deliver 4–6 Raw Mutton | 5–7 |
| Deliver 12–24 Bones | 5–6 |
| Deliver 24–40 String | 5–6 |
| Deliver 6–10 Leather | 5–7 |
| Harvest 24–40 Wheat | 5–6 |
| Harvest 24–40 Carrots | 5–6 |
| Harvest 24–40 Potatoes | 5–6 |
| Catch 8–12 fish | 5–7 |
| Catch 4–6 Salmon | 5–7 |
| Catch 6–10 Cod | 5–7 |
| Buy anything from a player's Shopfront *(only offered when possible)* | 5 |

### Medium (49 tasks)

| Task | Gold |
|---|---|
| Kill 8–12 Creepers | 10–13 |
| Kill 6–10 Spiders at night | 9–12 |
| Kill 5–8 Creepers at night | 10–13 |
| Kill 12–18 Zombies | 9–12 |
| Kill 12–18 Skeletons | 9–12 |
| Kill 8–12 Drowned | 9–11 |
| Kill 2–4 Witches | 11–15 |
| Kill 4–6 Pillagers | 11–14 |
| Kill 2–4 Phantoms | 11–14 |
| Kill 4–6 Guardians | 11–14 |
| Kill 2–4 Endermen | 10–13 |
| Visit the Nether and come back alive | 12 |
| Kill 4–7 Magma Cubes | 10–13 |
| Kill 4–7 Zombified Piglins | 9–11 |
| Kill 3–5 Hoglins | 11–14 |
| Kill 4–7 Piglins | 10–13 |
| Mine 20–32 Nether Quartz Ore | 9–12 |
| Mine 16–32 Glowstone | 10–13 |
| Mine 12–20 Nether Gold Ore | 9–11 |
| Chop 16–32 Crimson Stems | 10–13 |
| Chop 16–32 Warped Stems | 10–13 |
| Mine 8–12 Iron Ore | 9–12 |
| Mine 6–10 Gold Ore | 11–14 |
| Mine 10–14 Lapis Ore | 9–11 |
| Mine 12–20 Redstone Ore | 9–11 |
| Mine 32–48 Copper Ore | 9–11 |
| Mine 8–12 Obsidian | 11–14 |
| Reach Y -50 | 10 |
| Smelt 24–40 Iron Ingots | 10–13 |
| Smelt 8–16 Gold Ingots | 10–13 |
| Chop 24–40 Dark Oak logs | 9–12 |
| Chop 24–40 Jungle logs | 9–12 |
| Chop 24–40 Acacia logs | 9–12 |
| Chop 24–40 Mangrove logs | 10–13 |
| Chop 24–40 Cherry logs | 10–13 |
| Deliver 10–16 Raw Beef | 10–13 |
| Deliver 10–16 Raw Porkchops | 10–13 |
| Deliver 4–6 Raw Rabbit | 10–13 |
| Cook 12–20 meat | 9–11 |
| Cook 12–20 fish | 9–11 |
| Deliver 12–20 Gunpowder | 10–13 |
| Deliver 3–5 Ender Pearls | 10–13 |
| Deliver 32–64 Arrows | 9–11 |
| Catch 20–30 fish | 10–13 |
| Fish up 2–4 treasures | 11–15 |
| Travel 1,500–2,500 blocks | 9–12 |
| Enchant any item | 10 |
| Sell something from your Shopfront *(only offered when possible)* | 10 |
| Buy a meal from a Cook's Shopfront *(only offered when possible)* | 10 |

### Hard (40 tasks)

| Task | Gold |
|---|---|
| Kill 12–18 Endermen | 18–23 |
| Kill 5–8 Witches | 16–20 |
| Kill 10–15 Guardians | 16–20 |
| Kill 6–10 Phantoms | 16–20 |
| Kill 10–15 Pillagers | 16–20 |
| Kill 20–30 Creepers | 16–20 |
| Kill 2–4 Vindicators | 16–20 |
| Kill a Ravager | 20 |
| Kill an Evoker | 20 |
| Kill 2–4 Breezes | 16–20 |
| Kill 2–4 Shulkers | 17–21 |
| Kill a Blood Moon Level II monster | 16 |
| Survive a whole Blood Moon without dying | 19 |
| Get 20–30 kills in one Blood Moon | 18–23 |
| Kill 4–6 Blazes | 16–20 |
| Kill 4–6 Wither Skeletons | 17–21 |
| Kill 2–4 Ghasts | 16–21 |
| Kill 3–5 Piglin Brutes | 18–23 |
| Mine 2–3 Ancient Debris | 20–24 |
| Loot a Nether Fortress chest | 18 |
| Loot a Bastion chest | 22 |
| Deliver 6–10 Blaze Rods | 16–20 |
| Deliver 2–4 Ghast Tears | 18–24 |
| Mine 4–6 Diamond Ore | 18–23 |
| Mine 2–4 Emerald Ore | 17–22 |
| Mine 16–24 Gold Ore | 16–20 |
| Deliver 2–3 Diamonds | 18–24 |
| Deliver 2–3 Emeralds | 16–22 |
| Deliver 24–32 Raw Beef | 16–20 |
| Deliver 24–32 Raw Mutton | 16–20 |
| Chop 128–192 logs | 16–20 |
| Loot a Shipwreck chest | 16 |
| Loot a Desert Temple chest | 18 |
| Loot an Ocean Ruin chest | 16 |
| Loot a Stronghold chest | 20 |
| Loot a Jungle Temple chest | 18 |
| Loot a Woodland Mansion chest | 22 |
| Loot an Ancient City chest | 24 |
| Open a Trial Chamber Vault | 19 |
| Catch any rare fish | 18 |

### Heroic (23 tasks)

| Task | Gold |
|---|---|
| Help slay the Crimson Revenant | 50 |
| Help slay the Sanguine Witch | 50 |
| Help slay Gorefang | 50 |
| Help slay the Wither | 55 |
| Help slay the Ender Dragon | 55 |
| Slay a Warden | 55 |
| Slay an Elder Guardian | 40 |
| Win a Raid (Hero of the Village) | 45 |
| Top a Blood Moon's kill board | 45 |
| Kill 15–20 Wither Skeletons | 40–50 |
| Kill 40–50 Endermen | 40–50 |
| Kill 3 Ravagers | 45 |
| Kill 3 Evokers | 45 |
| Mine 6–8 Ancient Debris | 45–55 |
| Open an Ominous Vault | 40 |
| Catch an Epic or rarer fish | 40 |
| Loot an End City chest | 45 |
| Deliver 3 Wither Skeleton Skulls | 50 |
| Deliver a Trident | 40 |
| Deliver a Totem of Undying | 45 |
| Deliver a Heart of the Sea | 45 |
| Deliver 4 Shulker Shells | 45 |
| Deliver 4–6 Echo Shards | 40–50 |

## Admin

`/npc bountyclerk` puts him at your exact spot and facing (any other goes); `/npc bountyclerk remove` sends him away;
`/npc bountyclerk tasks <player> [reset]` shows (or wipes) a player's bounties; `/npc bountyclerk winners [reset]` lists
(or clears) the race's places. Never automatic.

## Testing

`bounty/BountyTests` ("bounties"): the catalogue (unique ids, 120+ tasks, deliveries beat the Banker, Hard pays 10-20),
the tier weights over 4,000 rolls, one task posted for everyone and a new one the moment it's taken (everyone sees the
new one; the board remembers the last 20), the 2-nugget fee for an Easy task and 2 gold for a Heroic one, a kill before taking doesn't count and after does, turn-in
pays, one take a day even after a turn-in, taking another exchanges it, a delivery takes exactly its items, abandon, and
the race: the first 3 to reach 50 have places reserved in order, claim a Fun Wishing Star once (claiming late doesn't
lose it), the 4th gets nothing, everyone's Ledger shows the winners; the standings (most first, ties to whoever got there first, the tie time only moving with the count, offline players kept and saved with the world, everyone's Ledger gets the top 10 and their rank).
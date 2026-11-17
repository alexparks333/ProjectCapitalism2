# Bounties: the Bounty Clerk

Code: `src/main/java/com/alexh/capitalism/bounty/` (`BountyTask` the catalogue, `Bounties` the rules, `BountyEvents`
how tasks count, `BountyData` a player's bounties, `ShopIndex` the town's Shopfronts, `BountyTests`) and
`town/BountyClerk`, `BountyMenu`, `BountyCommands`, `client/BountyClerkScreen`, `BountyClerkModel`, `BountyClerkRenderer`
(skin by `tools/make_bounty_clerk.ps1`). The NPC is in `Town.md` > The Bounty Clerk.

Something to do when you're bored: the Bounty Clerk keeps one task posted for the whole server ("Kill 6 Zombies").
Take it for 2 gold nuggets; it never expires and pays gold when you turn it in. Tasks are a bonus, not a job: the
economy pays about 25-60 gold an hour (`Economy.md`); a task pays 3 to 50.

## The rules

- **One task posted for everyone** (`BountyBoard`, saved with the world). Everyone sees the same one; **the moment
  someone takes it, a new one is posted** for everyone else (it's sent to anyone with the screen open), so no two
  players take the same task. A new one is never one of the **last 20 posted**.
- Posting: a tier by weight (**Easy 40%, Medium 30%, Hard 20%, Heroic 10%**), then any task of it; its amount rolled in
  its range, its reward in proportion ("Kill 4-7 Zombies": 4 pays 3 gold, 7 pays 5). A town task ("feed your pet", the
  Shopfront ones) is only posted if someone on can do it; if it's posted and you can't do it (no pet...), it stays up and
  your button says why.
- **One task at a time.** Taking the posted task **exchanges** it for yours: your old one is dropped, no refund (the
  screen asks first: "This replaces your current task: ..."). The fee is **exactly 2 gold nuggets** (a flat fee, not an
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

## The tasks (188 in all; amounts and gold are ranges)

### Easy (55 tasks)

| Task | Gold |
|---|---|
| Kill 4–7 Zombies | 3–5 |
| Kill 4–7 Skeletons | 3–5 |
| Kill 4–7 Spiders | 3–5 |
| Kill 2–4 Creepers | 3–5 |
| Kill 4–7 Drowned | 3–5 |
| Kill 4–7 Husks | 3–5 |
| Kill 4–7 Strays | 3–5 |
| Kill 3–6 Slimes | 3–5 |
| Kill 5–9 Silverfish | 3–4 |
| Kill 2–4 Zombie Villagers | 3–5 |
| Kill 4–7 Cave Spiders | 3–5 |
| Mine 16–24 Coal Ore | 3–4 |
| Mine 12–20 Copper Ore | 3–4 |
| Chop 24–40 logs | 3–4 |
| Mine 48–80 Stone | 3–4 |
| Dig 24–48 Sand | 3–4 |
| Dig 12–20 Clay | 3–4 |
| Dig 12–24 Gravel | 3–4 |
| Gather 16–32 Kelp | 3–4 |
| Gather 12–24 Sugar Cane | 3–4 |
| Harvest 24–40 Wheat | 3–4 |
| Harvest 24–40 Carrots | 3–4 |
| Harvest 24–40 Potatoes | 3–4 |
| Harvest 8–12 Pumpkins | 3–5 |
| Harvest 8–12 Melons | 3–5 |
| Plant 16–24 saplings | 3–4 |
| Breed 3–5 animals | 3–5 |
| Shear 4–8 sheep | 3–5 |
| Milk a cow 2–4 times | 3–4 |
| Deliver 10–16 Eggs | 3–4 |
| Catch 8–12 fish | 3–5 |
| Catch 4–6 Salmon | 3–5 |
| Deliver 12–20 Bread | 3–5 |
| Deliver 24–48 Torches | 3–4 |
| Deliver 6–10 Leather | 3–5 |
| Deliver 24–40 String | 3–4 |
| Deliver 12–24 Bones | 3–4 |
| Deliver 6–12 White Wool | 3–4 |
| Deliver 8–16 Apples | 3–5 |
| Walk 400–700 blocks | 3–5 |
| Row a boat 250–400 blocks | 3–5 |
| Send something by Mailbox | 3 |
| Buy anything from a player's Shopfront *(only offered when possible)* | 3 |
| Mine 3–5 Iron Ore | 3–5 |
| Dig 64–96 Dirt | 3–4 |
| Quarry 32–64 Andesite, Diorite or Granite | 3–4 |
| Harvest 24–40 Beetroots | 3–4 |
| Cut 24–48 Bamboo | 3–4 |
| Cut 8–16 Cactus | 3–4 |
| Catch 6–10 Cod | 3–5 |
| Kill 5–8 Skeletons at night | 3–5 |
| Smelt 16–32 Glass | 3–4 |
| Deliver 12–20 Baked Potatoes | 3–4 |
| Deliver 12–20 Feathers | 3–4 |
| Deliver 12–20 Charcoal | 3–4 |

### Medium (63 tasks)

| Task | Gold |
|---|---|
| Kill 8–12 Creepers | 7–10 |
| Kill 6–10 Spiders at night | 6–9 |
| Kill 2–4 Witches | 8–12 |
| Kill 8–12 Drowned | 6–8 |
| Kill 4–6 Pillagers | 8–11 |
| Kill 2–4 Phantoms | 8–11 |
| Kill 4–6 Guardians | 8–11 |
| Kill 4–7 Magma Cubes | 7–10 |
| Kill 2–4 Endermen | 7–10 |
| Kill 4–7 Zombified Piglins | 6–8 |
| Kill 3–5 Hoglins | 8–11 |
| Kill 4–6 Bogged | 7–9 |
| Mine 8–12 Iron Ore | 6–9 |
| Mine 6–10 Gold Ore | 8–11 |
| Mine 10–14 Lapis Ore | 6–8 |
| Mine 12–20 Redstone Ore | 6–8 |
| Mine 20–32 Nether Quartz Ore | 6–9 |
| Mine 48–80 Deepslate | 6–7 |
| Mine 8–12 Obsidian | 8–11 |
| Smelt 24–40 Iron Ingots | 7–10 |
| Cook 12–20 meat | 6–8 |
| Brew 3 potions | 8 |
| Enchant any item | 7 |
| Get something repaired at a town smith NPC | 6 |
| Tame a Wolf | 8 |
| Tame a Cat | 8 |
| Tame a Horse | 8 |
| Tame a Parrot | 10 |
| Breed 8–12 animals | 7–10 |
| Shear 16–24 sheep | 7–10 |
| Catch 20–30 fish | 7–10 |
| Catch a Pufferfish and a Tropical Fish | 8 |
| Fish up 2–4 treasures | 8–12 |
| Deliver 4–8 Honey Bottles | 7–10 |
| Deliver 2–4 Cakes | 7–11 |
| Deliver 3–5 Ender Pearls | 7–10 |
| Deliver 12–20 Gunpowder | 7–10 |
| Deliver 24–40 Glass | 6–8 |
| Deliver 12–20 Paper | 6–8 |
| Deliver 6–10 Books | 7–10 |
| Travel 1500–2500 blocks | 6–9 |
| Reach Y -50 | 7 |
| Climb above Y 200 | 7 |
| Ride a horse 400–600 blocks | 7–9 |
| Trade with villagers 3–5 times | 7–10 |
| Visit 4 different biomes in one day | 8 |
| Sell something from your Shopfront *(only offered when possible)* | 7 |
| Feed your pet 2–4 times *(only offered when possible)* | 6–8 |
| Buy a meal from a Cook's Shopfront *(only offered when possible)* | 7 |
| Kill 12–18 Zombies (any kind) | 6–9 |
| Kill 12–18 Skeletons (any kind) | 6–9 |
| Kill 4–7 Piglins | 7–10 |
| Kill 5–8 Creepers at night | 7–10 |
| Mine 32–48 Copper Ore | 6–8 |
| Mine 16–32 Glowstone | 7–10 |
| Mine 12–20 Nether Gold Ore | 6–8 |
| Tame a Donkey or Mule | 8 |
| Tame a Llama | 8 |
| Smelt 8–16 Gold Ingots | 7–10 |
| Cook 12–20 fish | 6–8 |
| Catch 2–3 Tropical Fish | 7–10 |
| Deliver 32–48 Bricks | 6–8 |
| Deliver 32–64 Arrows | 6–8 |

### Hard (44 tasks)

| Task | Gold |
|---|---|
| Kill 12–18 Endermen | 14–19 |
| Kill 4–6 Blazes | 12–16 |
| Kill 4–6 Wither Skeletons | 13–17 |
| Kill 2–4 Ghasts | 12–17 |
| Kill 2–4 Vindicators | 12–16 |
| Kill a Ravager | 16 |
| Kill an Evoker | 16 |
| Kill 2–4 Breezes | 12–16 |
| Kill 3–5 Piglin Brutes | 14–19 |
| Kill 2–4 Shulkers | 13–17 |
| Kill a Blood Moon Level II monster | 12 |
| Mine 4–6 Diamond Ore | 14–19 |
| Mine 2–4 Emerald Ore | 13–18 |
| Mine 2–3 Ancient Debris | 16–20 |
| Deliver 12–20 Amethyst Shards | 10–13 |
| Loot a Shipwreck chest | 12 |
| Loot a Desert Temple chest | 14 |
| Loot an Ocean Ruin chest | 12 |
| Loot a Bastion chest | 18 |
| Visit the Nether and come back alive | 12 |
| Open a Trial Chamber Vault | 15 |
| Catch any rare fish | 14 |
| Uncork a Message in a Bottle | 12 |
| Crack open a Sunken Pirate Chest | 15 |
| Deliver 6–10 Blaze Rods | 12–16 |
| Deliver 2–4 Ghast Tears | 14–20 |
| Deliver 2 Golden Apples | 15 |
| Deliver 8–12 Slime Balls | 10–13 |
| Deliver 3–5 Nautilus Shells | 12–17 |
| Survive a whole Blood Moon without dying | 15 |
| Get 20–30 kills in one Blood Moon | 14–19 |
| Kill 5–8 Witches | 12–16 |
| Kill 10–15 Guardians | 12–16 |
| Kill 6–10 Phantoms | 12–16 |
| Kill 10–15 Pillagers | 12–16 |
| Kill 20–30 Creepers | 12–16 |
| Mine 16–24 Gold Ore | 12–16 |
| Loot a Stronghold chest | 16 |
| Loot a Jungle Temple chest | 14 |
| Loot a Woodland Mansion chest | 18 |
| Loot a Nether Fortress chest | 14 |
| Loot an Ancient City chest | 20 |
| Deliver 2–3 Diamonds | 14–20 |
| Deliver 2–3 Emeralds | 12–18 |

### Heroic (26 tasks)

| Task | Gold |
|---|---|
| Help slay the Crimson Revenant | 45 |
| Help slay the Sanguine Witch | 45 |
| Help slay Gorefang | 45 |
| Help slay the Wither | 50 |
| Help slay the Ender Dragon | 50 |
| Slay a Warden | 50 |
| Slay an Elder Guardian | 35 |
| Win a Raid (Hero of the Village) | 40 |
| Open an Ominous Vault | 35 |
| Catch an Epic or rarer fish | 35 |
| Top a Blood Moon's kill board | 40 |
| Deliver a Trident | 35 |
| Deliver a Totem of Undying | 40 |
| Deliver 3 Wither Skeleton Skulls | 45 |
| Kill 15–20 Wither Skeletons | 35–45 |
| Kill 40–50 Endermen | 35–45 |
| Kill 3 Ravagers | 40 |
| Kill 3 Evokers | 40 |
| Mine 6–8 Ancient Debris | 40–50 |
| Loot an End City chest | 40 |
| Deliver a Heart of the Sea | 40 |
| Deliver 4 Shulker Shells | 40 |
| Deliver a Heavy Core | 45 |
| Deliver 4–6 Echo Shards | 35–45 |
| Deliver a Sniffer Egg | 35 |
| Deliver the Pigstep music disc | 40 |


## Admin

`/npc bountyclerk` puts him at your exact spot and facing (any other goes); `/npc bountyclerk remove` sends him away;
`/npc bountyclerk tasks <player> [reset]` shows (or wipes) a player's bounties; `/npc bountyclerk winners [reset]` lists
(or clears) the race's places. Never automatic.

## Testing

`bounty/BountyTests` ("bounties"): the catalogue (unique ids, 120+ tasks, deliveries beat the Banker, Hard pays 10-20),
the tier weights over 4,000 rolls, one task posted for everyone and a new one the moment it's taken (everyone sees the
new one; the board remembers the last 20), the 2-nugget fee, a kill before taking doesn't count and after does, turn-in
pays, one take a day even after a turn-in, taking another exchanges it, a delivery takes exactly its items, abandon, and
the race: the first 3 to reach 50 have places reserved in order, claim a Fun Wishing Star once (claiming late doesn't
lose it), the 4th gets nothing, everyone's Ledger shows the winners; the standings (most first, ties to whoever got there first, the tie time only moving with the count, offline players kept and saved with the world, everyone's Ledger gets the top 10 and their rank).
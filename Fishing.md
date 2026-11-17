# Fishing: rare catches and the Fisherman

Code: `src/main/java/com/alexh/capitalism/fishing/` (catches, prices, plaques, tests) and `town/Fisherman*` (the NPC and
his shop, see `Town.md` > The Fisherman). Textures: `tools/FishTex.cs` (icons and the plaque/hat textures),
`tools/make_fisherman.ps1` (the NPC).

## Rare catches

A cast with a fishing rod enchanted with **Luck of the Sea** can land a rare catch (`RareCatchModifier`, a global loot
modifier on the vanilla fishing table, `data/capitalism/loot_modifiers/rare_catch.json`). Each rare that lives where and
when the hook lands gets its chance, **rarest first**; the first that hits replaces the cast's whole loot.

### Luck of the Sea

- A rod **without** Luck of the Sea never lands a rare catch, however lucky the angler.
- Luck of the Sea **I**: the chances listed below; **II**: x1.5; **III**: x2 (`RareCatch.multiplier`). E.g. the
  Oarfish: 1 in 3,000 / 2,000 / 1,500.
- Other Luck (skill books like the Angler's Almanac, the Angler's Derby, the Luck effect) adds +15% a point on top, but
  only on a Luck of the Sea rod. The total is capped at x3.
- FishWatch's restriction overrides all of it (no rare catches at all).
- Item tooltips and the Collection's hover hints show the odds per level: "I 1 in 3,000 · II 1 in 2,000 · III 1 in
  1,500"; the Fisherman's Sell tab says rare fish only bite on a Luck of the Sea rod.

| Catch | Chance (1 in) | Where / when | Weight (lb) | Plaque | Server-wide |
|---|---|---|---|---|---|
| Giant Catfish | 60 | swamp or river, night | 30–90 | 1 wide | |
| Golden Koi | 80 | anywhere, day | 4–12 | 1 | |
| Message in a Bottle | 100 | ocean | — | — | |
| Pufferfish King | 120 | warm / lukewarm ocean | 8–20 | 1 | |
| Blobfish | 150 | deep ocean | 3–8 | 1 | |
| Swordfish | 200 | ocean, day | 150–600 | 2 | |
| Ocean Sunfish | 250 | ocean, day, not raining | 500–2000 | 2 | |
| Sunken Pirate Chest | 300 | ocean | — | — | |
| Electric Eel | 350 | river, thunderstorm | 20–45 | 1 | |
| Mako Shark | 500 | deep ocean | 150–400 | 2 | yes |
| Anglerfish | 600 | deep ocean, night | 10–40 | 1 | yes |
| Ghost Koi | 800 | anywhere, Blood Moon (`Events.md`) | 4–12 | 1 | yes |
| Kraken Tentacle | 1500 | deep ocean, thunderstorm | 80–250 | 2 | yes |
| Oarfish | 3000 | deep ocean, night | 100–600 | 3 | yes |
| Golden Leviathan Scale | 10000 | anywhere | 2–6 | 1 | yes, with a fanfare |

- A weighed catch is weighed within its range, middling weights likeliest (whole pounds; one decimal under 20 lb). It
  carries a `CatchInfo` component (weight, who caught it, the in-game day: `Calendar.md`), shown in the tooltip as
  `312 lb · caught by Alex · Summer 9, Year 2`. The bottle and the chest aren't weighed, so they stack (to 16).
- The Mako Shark and rarer are told to the whole server ("🎣 Alex landed a 312 lb Mako Shark!"). The Leviathan Scale
  gets a gold message, a toast sound and totem sparkles round the catcher.
- Landing the Pufferfish King poisons you for 5 seconds; the Electric Eel zaps you (3 damage, sparks).
- Rarity colours: Epic for 1 in 1000 or rarer, Rare for 1 in 300 or rarer, else Uncommon. They're in the creative
  tab after the Bottle of Blood, with the Soggy Note and the Pirate Hat.

## Tiers

Every catch has a tier (`CatchTier`, `RareCatch.tier()`): its name and a "◆ Tier" line in its tooltip are in the
tier's colour, with "Catch chance: 1 in 3,000" under it (plaques show it too). The rarer, the grander its plaque.

| Tier | Colour | Catches | Plaque |
|---|---|---|---|
| Uncommon | green | Giant Catfish, Golden Koi, Message in a Bottle | walnut, brass plate |
| Rare | blue | Pufferfish King, Blobfish, Swordfish, Ocean Sunfish, Sunken Pirate Chest | walnut, brass plate |
| Epic | purple | Electric Eel, Mako Shark, Anglerfish | dark polished wood, a carved border, silver rosettes and a silver-framed plate |
| Legendary | gold | Ghost Koi, Kraken Tentacle, Oarfish | ebony, gold filigree corners, a gold finial, a gold-framed black plate with gold lettering (the Ghost Koi's in Blood Moon crimson) |
| Mythic | red, bold, shimmering | Golden Leviathan Scale | obsidian in a heavy gold frame, black spikes, rubies, glowing runes and lettering, faint embers drifting off it |

## Message in a Bottle

Right-click to uncork: the glass bottle comes back with a **Soggy Note** for 1 to 5 gold (whole gold, written on
it: `NOTE_GOLD`). Only the Fisherman redeems it.

## Sunken Pirate Chest

Right-click to crack it open (`RareFindItems.PirateChest.loot`):
- always 2–6 gold (plus a few nuggets);
- 45%: 1–3 emeralds; 12%: a diamond; 10%: a spyglass; 25%: a nautilus shell;
- 50%: 1–3 raw **cod or salmon** that swam in;
- very rarely: **3%** the **Old Captain's Rod** (a fishing rod with Luck of the Sea III and Lure III, gold name), **4%** a **rare fish** (any rare catch but the chest and the bottle; it doesn't count as caught for the records) (`PirateChest.treasure`);
- 15%: the **Pirate Hat**, a black felt tricorn with gold braid, a skull and crossbones and a red feather. It's worn in
  the head slot (no armour points) and drawn on the player by `client/PirateHatLayer`.

## The Fisherman's prices

He buys **only** rare catches and Soggy Notes (ordinary fish go to the Cook). A catch pays its base gold for one of
middling weight, from 60% for the lightest of its kind to 140% for the heaviest (`FishMarket.valueOf`). He doesn't
buy unopened bottles or chests. A Soggy Note pays what's written on it.

Plaques: he mounts a catch on a wall plaque for **3 gold per block of width + a tenth of what the catch would sell
for** (the fair price), and since he's an NPC that goes through `NpcPricing.npcPrice` (his +50% markup, Emerald armour
discount, festival sales). The catch is used up.

| Catch | He pays: lightest | middling | heaviest | Plaque fair | Plaque, NPC price (no discounts) |
|---|---|---|---|---|---|
| Giant Catfish | 8 n | 1 g 5 n | 2 g 1 n | 3 g 1 n | 4 g 6 n |
| Golden Koi | 1 g 2 n | 2 g | 2 g 7 n | 3 g 1 n | 4 g 6 n |
| Pufferfish King | 1 g 7 n | 3 g | 4 g 2 n | 3 g 2 n | 4 g 8 n |
| Blobfish | 2 g 1 n | 3 g 5 n | 4 g 8 n | 3 g 3 n | 5 g |
| Swordfish | 3 g | 5 g | 7 g | 6 g 4 n | 9 g 6 n |
| Ocean Sunfish | 3 g 5 n | 6 g | 8 g 4 n | 6 g 5 n | 9 g 7 n |
| Electric Eel | 4 g 7 n | 8 g | 11 g 2 n | 3 g 7 n | 5 g 6 n |
| Mako Shark | 9 g | 15 g | 21 g | 7 g 4 n | 11 g 1 n |
| Anglerfish | 10 g 7 n | 18 g | 25 g 2 n | 4 g 7 n | 7 g 1 n |
| Ghost Koi | 15 g | 25 g | 35 g | 5 g 4 n | 8 g 2 n |
| Kraken Tentacle | 24 g | 40 g | 56 g | 10 g | 15 g |
| Oarfish | 36 g | 60 g | 84 g | 15 g | 22 g 4 n |
| Golden Leviathan Scale | 90 g | 150 g | 210 g | 18 g | 27 g |
| Soggy Note | 1–5 g (as written) | | | — | — |
(g gold, n nuggets, 9 to a gold; the plaque columns are for a middling catch. Priced for an economy of 25–60 gold an hour: a rare catch is a nice bonus, the Leviathan Scale a fortune. The plaque's 3 gold a block makes it cost more than a cheap catch is worth: it's a trophy, not an investment.)

## Plaques

A **Fish Plaque** (`FishPlaqueBlock`, `FishPlaqueBlockEntity`, item `FishPlaqueItem`) keeps the catch's kind
(`PLAQUE_CATCH`) and its `CatchInfo`; the tooltip names the catch, its story and how wide it is.
- It hangs on the side of a solid block (click a wall). 2-wide plaques (Swordfish, Ocean Sunfish, Mako, Kraken Tentacle) run one block
  to the right as you face them; the 3-wide Oarfish is centred on the block you clicked. It needs every block of its
  width free with solid wall behind, or it says so and doesn't go up. The extra blocks are part blocks
  (`decor/BloodPartBlock`, via its `Owner` interface).
- Breaking any part takes the whole plaque down and drops the one plaque, catch and story kept (its loot table copies
  the components from the block entity). It falls off if its wall goes.
- `client/FishPlaqueRenderer` draws a polished walnut board with a dark carved rim and gold trim, the catch mounted
  on it, and a brass nameplate with four screws engraved with the catch's name, weight, and `catcher · date`, each line
  shrunk to fit. The nameplate is a strip along the bottom of the board, the full width inside its rim, with two lines: the name big and bold, then weight · catcher · date. Its letters are full bright and the plate is drawn lamp-lit, so it reads at night; the catch is mounted in the space above it so nothing hangs in front of it. The catches are sculpted in 3D from boxes (`client/FishTrophies`, like the Blood decorations): a body
  of round slices along a bent spine (leaping or curved like a taxidermy trophy), separate angled fins and tail, eyes on
  both sides, the shark's and anglerfish's jaws open with teeth, the swordfish's bill, the oarfish's red crest and
  plumes, the tentacle curling out of the board with suckers, the Leviathan Scale a jagged black-gold shard with hooked barbs and a glowing slit eye, held in a spiked iron claw; the Pufferfish King furious, long-spiked, scarred and crowned. Each has its own pixel-art
  texture (`textures/entity/plaque/<id>.png`, 64 x 64). The Ghost Koi is see-through and glows; the anglerfish's lure,
  the eel's sparks and the scale's rune glow.

## Icons

16 x 16 pixel art in the vanilla fish style (`tools/FishTex.cs`, hand-drawn pixel maps, a small palette each): fish
lie diagonally, head bottom-left and tail top-right like cod and salmon, lit from the top left, outlined in a darker
shade of their own colour.

## The Collection

The Fisherman's third tab, **Collection** (`fishing/CatchCollection`, per player, kept through death, synced to that
player): all 15 rare catches (the bottle and the chest included). Ones you've **caught** yourself show in colour, named
in their tier's colour, with how many and your heaviest; the rest are black silhouettes named "???". Hovering any of
them shows a hint: its name (or "???"), tier, where and when it bites ("Deep ocean, at night", "Rivers, in a
thunderstorm", "Anywhere, only under a Blood Moon"...), its odds for each Luck of the Sea level and "Needs a rod with
Luck of the Sea"; a caught one adds how many and the heaviest. "Caught 7 / 15"
and a bar show your progress.
- Only catching counts: it's recorded when you land one (`RareCatchModifier.land`, the player holding the rod). Buying,
  being given, picking up or spawning one doesn't; selling or mounting one never takes it off.
- All 15 caught: a **Claim** button gives one **Essential Wishing Star** (the Blood Merchant's item), once per player
  ever, separate from the Blood Merchant's star, with a fanfare and a server-wide "🎣 X completed the Fisherman's
  Collection!". Then it says "Collection complete ✔".
- Admins: `/npc fisherman collection <player>` shows it, `... reset` wipes it (prize included). Never done
  automatically.

## AFK fishing (FishWatch)

`fishing/FishWatch` watches fishing for admins. **Nothing is automatic**: it scores and tells ops; only an op can
restrict anyone, and only an op can lift it. The player is never told.
- Tracked per player per session (a session ends after 10 minutes without a cast or at logout): casts, catches, the
  reaction from bite to reel-in (average and spread over the last 20), minutes since they last moved or turned their head
  (any yaw/pitch change) while still casting, the spacing of their casts, how much the bobber's spot varies, catches
  with a hopper or hopper minecart within 3 blocks of them, minutes without chat or opening a menu.
- The score (a real angler scores 0–20; 60 flags, 30 counts as suspicious):

| Sign | Points |
|---|---|
| 20+ catches reeled in on exactly the same tick after the bite | 50 |
| ...or within a spread under 1 tick | 40 |
| not moved or turned their head while casting, 10+ min / 20+ min | 30 / 40 |
| 20+ casts spaced like clockwork (spread under 2 ticks) | 20 |
| a hopper by them while catching (5+ catches) | 15 |
| the bobber on one spot for 20+ catches (all within 0.15 blocks) | 10 |
| 60+ min fishing without chat or a menu | 10 |

- Crossing 60: a line in the saved log (the last 100) and one message to online ops, at most once per player per 30
  minutes: "[FishWatch] X looks like AFK fishing (reels in 3 ticks after every bite, still 14 min). /fishlog X".
- Ops: `/fishlog` lists suspicious and flagged players (score, casts, catches, reaction ± spread, minutes still,
  reasons) and the latest log lines; `/fishlog <player>` the details; `/fishlog restrict <player>` /
  `/fishlog unrestrict <player>` the quiet countermeasure (while restricted, rare catches never roll for them; they get
  ordinary fish only; saved on the player, kept through logouts, restarts and death); `/fishlog clear <player>` clears
  their suspicion stats and nothing else.

## Testing

`fishing/FishingTests` ("fishing"): each catch's where/when, the rarest-first roll and Luck (a fixed random), weights in
range and the tooltip line, the price scaling, refusing cod and unopened chests, Soggy Note values, the plaque price
going through NpcPricing, uncorking a bottle, the pirate chest's loot, and a plaque's placement (blocked, then 3 wide),
its block entity, and breaking it into one plaque with its story. `fishing/FishingTests` ("fishing_collection"): landing a catch records it (count, heaviest), being given one doesn't, the prize can't be claimed incomplete and can be claimed once. ("fishwatch"): an inhuman pattern is flagged, a human-like one (varied reactions, looking around) isn't, flagging never restricts, restrict stops rare rolls and unrestrict brings them back, clear wipes the stats. `town/FishermanTests` covers his shop (`Town.md`).
`gradlew runPhoto -Pphoto=fish` shows every catch, the opened pirate chest with the hat worn, the Fisherman and the
plaques.

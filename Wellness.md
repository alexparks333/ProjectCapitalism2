# Wellness

Opened from the apple tab on the left of the survival inventory. Esc goes back to the inventory.

**Who controls what:**
| System | Controls | Bonus | Penalty |
|---|---|---|---|
| Diet | Food bar | Balanced: +1 drumstick (11th, gold slot) | Poor: −1 drumstick (the slot is simply gone, like a lost heart; gold can't fill it either) |
| Water Purity | Thirst bar | Excellent: +1 water drop (11th, gold slot) | Low: −1 water drop (the slot is simply gone; gold can't fill it either) |
| Wellbeing | Hearts | Balanced diet for long enough: +1 heart with Good water, +2 with Excellent water (gold) | none |
| Body heat | Hearts / thirst | Normal 50-80%: nothing | Freezing (<32%): lose health, faster the colder. Hot (>80%): thirst drains faster, faster the hotter |

**There are no ailments any more** (Poor Diet, Sick, Anxious, Tired, Bored, Lonely, Money Worries, Cold were all
removed on purpose, with `/ail`, the badges, the lost-heart penalty and the skill books The Socialite and A Fresh
Start). Don't bring them back. The cold and the heat are the body heat system below.

## Diet (Eco-style)
- Four food groups: Vegetables, Fruit, Protein, Carbs. Eating adds the food's hunger points to its group(s) (`player/DietValues.java`). Unlisted foods don't count.
- **Diet window:** the diet only remembers your last `dietWindowPoints` (130) diet points (1 hunger point eaten = 1 diet point, about 16 steaks). Past that, everything shrinks to make room. So one snack can only nudge the chart: a 2-point melon slice is about 1.5%. (Was 160; the user found that a little too slow.) (It used to fade 3% a minute, which left tiny totals where one melon slice swung 81% -> 54%.) `dietKeepPerMinute` is now 1.0 (no fading).
- **Food tooltips:** every food that counts shows its diet points, e.g. "Diet: +2 Fruit (~1% of your diet)" (`ClientModEvents.onItemTooltip`).
- **Hunger and thirst on tooltips:** every food shows how much it fills, e.g. "Hunger: +7 drumsticks", and every drink shows its water, e.g. "Thirst: +6 water drops" (Bartender drinks, Purified Water, water bottles). Values come from the item itself and the config, so they always match what you get.
- Note: because the ideal is 65% protein, eating only meat bottoms out around 61% (Okay), never Poor. Only-fruit or only-veg can reach Poor.
- **Ideal diet:** 10% Vegetables, 10% Fruit, 65% Protein, 15% Carbs (`DietData.IDEAL`).
- **Balanced head start:** the first time a player joins they get `dietStartPoints` (130, a full window) diet points split into the ideal mix. Once per player (saved as `seed`; bumping `DietData.SEED` re-seeds everyone once, which happened with the window change).
- **Meals before buffs:** diet effects only start once a player has eaten `dietMealsBeforeEffects` (20) meals that count toward the diet. This is silent: no badge, the diet card just shows the chart's status without a drumstick bonus until then. (Saved as `meals`.)
- Balance = how close your mix is to the ideal (100% = exactly the ideal, 0% = everything in veg or fruit). Compared as whole percentages, exactly as the screen shows.
  - Below `dietMinPoints` (12) total: no effect ("Eat something!").
  - **Balanced 86%+** (`dietBalancedScore` 0.86): +`dietBalancedFoodBonus` (2 = 1 drumstick). The food bar can go to 22.
  - **Okay 45–85%**: no effect.
  - **Poor 0–44%** (`dietPoorScore` 0.44): −`dietPoorFoodLoss` (2 = 1 drumstick). The food bar stops at 18.
  - **A food group at 0%** (once the diet counts): **Poor**, whatever the balance (`DietData.tier`). It also drags the wellbeing meter toward a lost heart.
- **Filling the bonus drumstick:** vanilla stops the food bar at 20 and only lets you eat below 20; we let you eat whenever you're below **your** top (22 with a Balanced diet; `mixin/PlayerMixin.canEat`, each side reading the top it knows), so once the bonus drumstick is used up you can eat again. Vanilla still stops the bar at 20 while eating, so the 11th drumstick fills from what the meal brings past 20 (e.g. steak at 16 → 22, anything at 20 → up to 22). We remember the bar when you start eating and top it up after. Gold drumsticks also top it up (gold always goes first, see Cook.md "Gourmet food and the golden food bar"). Your client also runs vanilla's eating, which would cut the bar shown on screen back to 20, so the thirst sync (every half second and right after eating) also sends the real food bar.
- Diet survives death.

## Water Purity
- One meter, 0–100%, shown as a glass tube on the far left of the Wellness screen. Bands: **Low 0–24, Medium 25–54, Good 55–89, Excellent 90–100**.
- Everyone starts at Good (`purityStart`, 70).
- Every drink pulls the meter toward that water's target. How hard depends on how much thirst it restores: a full bar (20 points) pulls `purityShift` (0.3) of the way.
  - Raw water (sips from lakes/rivers, plain water bottles): toward 0.
  - Purified Water: toward 84 (Good). It can get you back to Good, never to Excellent. If you're Excellent, it slowly brings you back down to Good.
  - **Spring Water** (Bartenders' Advanced Osmosis Machine; `Bartending.md` > Spring Water): toward 100, three times as hard as other water (two or three bottles: Good to Excellent).
  - Juices, liquors and cocktails don't change purity (for now).
- Rough feel with the defaults: from 70, about 8 raw water bottles (or ~17 sips) drops you to Low. From Low, about 4 Purified Waters gets you back to Good.
- **Excellent:** +`purityExcellentWaterBonus` (2 = 1 drop). The thirst bar can go to 22, and you can keep drinking until it's full. Gold water drops also top up the bonus drop.
- **Low:** −`purityLowWaterLoss` (2 = 1 drop). The thirst bar stops at 18. Drinking past that doesn't fill the hidden hydration buffer.
- Code: `player/WaterPurity.java` (bands + math), purity lives on `ThirstData` (saved as `purity`), bar caps set every tick in `ModServerEvents`.

## Body heat (temperature)
Like Rust's: a hidden 0-100% body heat that slides toward what the world around you is doing to it, worked out once
a second on the server (`player/BodyTemperature.java`, called from `WellnessEvents`). Saved (`WellbeingData`, `heat`);
reset to 70% on respawn. Creative/spectator: always 70%, no effects.
- **Zones:** Freezing below 32%, Cold 32-50%, Normal 50-80% (70% is just right), Hot (burning) above 80%.
- **What pulls it** (the target it slides toward; the coldest cold wins):
  - Rain falling on you: 25 (a roof keeps you dry). Water at night: 20. Water in a snowy place: **-5** (x2 speed). A snowy
    place at night or while it snows: **-5**. Powder snow: 0, fast. Never cold in the Nether. Body heat goes from -5 to 100.
  - **Warmth against the cold:** a fire, lit campfire, lit furnace / smoker / blast furnace, lava or magma within 4
    blocks (2 up/down), or a lit **Blood Crystal Fireplace within 10 blocks** (it warms a 21x21x21 base): back to
    70%. **A fire never makes you hot on its own.** A whole night slept in the **Four-Poster Crimson Bed** (Blood
    Merchant): 70% and dry on waking (`BodyTemperature.reset`).
  - **Leather:** each piece keeps a quarter of the cold off (`leatherCold`: 4 pieces = 70%, it slides slowly back to
    normal); in powder snow each piece only softens it 5%. Somewhere hot each piece adds a quarter of the way to 100%
    (`leatherHot`: 4 pieces = burning).
  - **Wet:** going into water in any leather soaks you (`WellbeingData.wet`, saved). Wet counts as still being in that
    water wherever you are (cold at night / in snowy places, x2 there; cooling somewhere hot), and wet leather keeps
    no cold off. Dry off: **5 seconds by a fire / fireplace** (`DRY_SECONDS`, the Nether dries you too).
  - **Molten Sockets** (`Sockets.md`, any armor piece, one is enough): **Ice** = you can't get hot (70% cap) but every
    cold is 10% colder; **Blaze Powder** = the cold can't reach you, but somewhere hot you burn (100%). Both: neither.
  - **Hand Warmer** (only from the Traveling Merchant) in your offhand: the cold can't reach you while it burns (20 minutes).
  - **Cook meals:** **Campfire Chili** (Warmed Up, 5 min): the cold can't reach you. **Frosty Melon Sorbet** (Cooled
    Off, 5 min): never above 70.
  - **Skill books:** **Winter Survival** (rare trait) / **Hot Pepper** (lesson): the cold can't touch you. **Ice In The Pants**
    (lesson) / **Used To Hell** (rare trait): never above 70%. **A Fresh Start** (instant): 70%, dry, diet and water
    back to the start.
  - **Heat:** the Nether 95%; a desert or badlands (`hotBiome`; savannas and other warm biomes don't count) in the daytime with
    no rain 88%. Water (or being wet) cools you there (65%). **A fire nearby when you're already somewhere hot:
    burning, 100%.**
  - Otherwise: 70%.
- **How fast it moves** (per second, `BodyTemperature.step`):
  - Cooling: from Hot 3 (out of burning in a few seconds), from Normal 1.33 (70% to Cold in ~15s), from Cold and below
    0.15 (Cold to Freezing in ~2 minutes, then on down to the target, e.g. icy water bottoms out at 20%). Powder snow:
    1.5 all the way (frozen in under half a minute). Water in a snowy place: all cooling x2 (`SNOWY_WATER_CHILL`: Cold in
    ~8s, Freezing ~1 minute later).
  - Warming: next to a heat source 2 (semi-fast: out of Freezing in ~6s, out of Cold ~9s later, then to 70%); without
    one 1 in the cold zones and 1.5 up to 70%. Above 70% (heading into Hot): 0.67 (~15s to Hot), then 0.3 deeper.
- **Freezing:** frosty hearts (vanilla's frozen ones, `PlayerHeartTypeEvent`) and half a heart lost every few seconds,
  **faster the colder you are**: every 15s at the edge (32%), every 3s at 0% (`frostInterval`). Freeze damage, never
  below 1 heart.
- **Hot:** you **sweat** (beads drip off the water drops, `client/ThirstHud.sweat`; the drain skips the hidden hydration buffer, `ThirstData.sweat`, so the bar drops straight away) and thirst drains **x3 past 80, up to x5 at 100** (`thirstMultiplier`, used in
  `ModServerEvents` instead of the old flat Nether / hot-biome multipliers).
- **The ball** (`client/TemperatureHud`, bottom right of the screen, after Tough As Nails' gauge) with the % in small
  text to its left: snowflake (Freezing), blue drop (Cold), green (Comfortable), flame (Hot). It shivers while
  Freezing. It shows in the inventory too, where hovering it (or the %) says "Temperature: Cold (43%)", what's acting
  on you ("The rain is soaking you.", "A fire is warming you.", "The fire makes it even hotter." ...) and what to do.
  The reasons are a bitmask (`BodyTemperature.R_*`) sent with the heat in `WellbeingSyncPacket`. Sprites
  `hud/temp_*.png` (drawn with PowerShell).
- Tests: `librarian/SkillBookTests` > `body_temperature` (the timings above, zones, frost damage and its floor, thirst)
  and `skill_winter_survival`.

## Wellbeing (hearts)
- **Bonus hearts ("thriving", all in effect after the grace period):**
  - **+1 heart:** **Balanced** diet (86%+) + **Good** water purity or better.
  - **+2 hearts:** **Balanced** diet (86%+) + **Excellent** water purity (90%+).
- **Wellbeing** (`player/WellbeingData.java`, saved, kept through death) is a timer in seconds, from -(debuff + grace) up to +(buff + grace) minutes:
  - Thriving (+1 or +2 level): +1 a second. Otherwise it drifts back toward 0 by 1 a second.
  - Once it reaches `wellbeingBuffMinutes` (10 min of thriving) you get the bonus for your current level: +1 heart, or +2 with Excellent water. Dropping from Excellent to Good water later takes you down to +1 (after the grace period).
  - It can keep going `wellbeingGraceMinutes` (5 min) past that line, so a topped-up player keeps the bonus a while after something slips.
  - `wellbeingHealth` (2 = 1 heart) is the size of one heart; the big bonus is double.
  - On top of that, the 3-minute grace period (below) applies when crossing any line.
- **Lost heart (neglect, `WellnessEvents.neglect`):** the meter can go below 0. Each second it drops by 1 for **a food
  group at 0%** (once the diet counts) **or Low water** (below Medium), and by 1 more for **body heat under 10 or over
  95** (freezing / burning). Neglect beats thriving: while anything drags it, it only goes down. At
  `-wellbeingDebuffMinutes` (5 min of one kind, 2.5 min of both) you lose **1 heart**; it bottoms out `wellbeingGraceMinutes`
  below that. Fix the cause and it drifts back up toward 0 (the heart comes back once it's above the line, after the
  grace period). The screen's bar has a red zone and just says "-1 heart" while it's gone: it **never says why** (on
  purpose: players have to watch their own diet, water and temperature; the guide only says neglect can cost a heart).
- The screen's Wellbeing line says what's next: "+1 heart in 6:12" / "+2 hearts in 0:40" (thriving), "+1 heart! Keep it up", "Need Balanced diet" / "Need Good water" / "Need diet & water", "Losing +1 heart in 4:59". While a change is waiting out the hidden grace period it just says what's coming ("+1 heart coming!", "Slipping to +1 heart...", "Losing +1 heart...").
- The old diet heart (`capitalism:diet_health`) is removed automatically from anyone who still has it.

## Grace period & fading (all three)
- **Grace period** (`wellnessGraceSeconds`, 180 = 3 minutes): the diet (food), purity (water) and wellbeing (hearts) bonuses/penalties only switch **on or off** after the new state has held for 3 minutes in a row. If it flips back before then, nothing changes (e.g. your diet drops to Okay, you eat a steak and it's Balanced again 2 minutes later: you keep the drumstick). Same when *becoming* Balanced/Excellent: you have to hold it 3 minutes before the bonus appears.
- **The grace period is hidden:** no countdowns anywhere. While a heart change is waiting, the Wellbeing line just says what's coming ("+1 heart coming!", "Losing +1 heart...", "Recovering...", "-1 heart coming..."). The purity card and diet badge show only what's in effect.
- **Fading** (`wellnessFadeSeconds`, 4): when a bonus is taken away (or a penalty starts), the extra heart / drumstick / drop drains away over ~4 seconds instead of vanishing. Gaining happens straight away once the grace period is over.
- **No "hit by air":** Minecraft plays the got-hit camera shake whenever your health goes down, even from a lower max health. When a heart fades, the server tells the client the new health first (`QuietHealthPacket`), so vanilla's health update isn't a "drop" and there's no shake or flash.
- The grace timers are saved (`WellbeingData`: `effective` / `pending`); the fading amounts aren't (after a relog they snap to what's in effect).

## The screen
- Far-left card: Water Purity tube with the band names beside it (your band is bright), band name and % underneath.
- Diet card: donut chart, balance % in the middle, a two-line badge (golden apple "Balanced! / +1 drumstick", apple "Okay diet", rotten flesh "Poor diet / -1 drumstick", bowl "Eat something!"). No meals badge and no hint text under it.
- Food Groups card: icon, share %, a colored bar with a white tick at that group's ideal share.
- How you're doing: real hearts / drumsticks / water drops like the HUD. Bonus slots (11th drumstick / drop) sit on a gold-tinted slot, the bonus heart is gold, and anything lost simply isn't there (9 drumsticks / drops).
- Wellbeing bar under that: neutral on the left, gold zone (+1 heart) on the right, and a white marker. The line above says what's next (see Wellbeing above for all the messages).
- Temperature card (bottom right, where Ailments used to be): the ball, the % (in the zone's colour) and the first thing acting on you ("The rain is soaking you."), or just "Comfortable". Hover it for the full story (the same as the HUD ball's hover).
- **(i) button** in the top right: **the main tooltip on the screen** (the only others: hovering a pie slice shows its name and %, e.g. "Fruit: 12% (ideal 10%)", and hovering the Temperature card). It covers the perfect diet (with example foods), the white ticks, that foods show their diet points, what Balanced/Poor do, water purity (what raises and lowers it, Excellent/Low), and what the bonus hearts need. It doesn't mention the grace period (that's hidden). All the other hover tooltips were removed.
- HUD: the 11th drop / drumstick appears at the left end of each bar on a gold slot; lost ones aren't drawn at all (the food bar is drawn by `ThirstHud.renderShortFood` instead of vanilla's then; the gold bars are only as long as the real bars, `ThirstData.goldFoodMax`).

### Buffs/Debuffs page
Three tabs at the top left of the screen: **Wellness** (everything above), **Buffs/Debuffs** and **Tasks** (below). Buffs/Debuffs: everything on your
character right now, in three cards (scroll a card if it's full; hover a row for what it does):
- **Traits:** skill book traits you've learned (for good). Book icon, gold name, "Trait" / "Rare trait".
- **Lessons:** skill book lessons running, longest first, with the time left ("Running Club II", "18:42 left"). Lessons
  are hidden from the vanilla effect icons (next to the inventory and on the HUD), so this is where you see them.
- **Effects:** every other effect (potions, meals, drinks): its icon, name and time, green for good, red for bad.
Code: `client/WellnessScreen` (`drawBuffsPage`). Skill books: `Librarian.md`, `LibrarianBooks.md`.

### Tasks page
A third tab, **Tasks**: your current **bounty** (title, tier, a progress bar, the reward; or "No task. Visit the Bounty
Clerk in town to take one." — `Bounties.md`) and **Today's Cravings** (`Cravings.md`): two cards, Food and Drink, each
with the item's icon, its name (with the doneness for a seared cut), what it does ("Fills your food bar to gold" /
"Fills your water bar to gold"), ✔ Satisfied or Not yet, and a countdown to the next morning's pair. Reads the player's
synced data (`Bounties.of(player)`, `Cravings.of(player)`). Code: `client/WellnessScreen` (`drawTasksPage`).

## Water & salt water
- **Drink from water with an empty hand:** right-click a fresh water *source* block. Each sip gives `waterSipRestore` (4 = 2 drops), max one sip a second, and you can't sip when your thirst bar is full.
- **Air bubbles** (underwater) are drawn one row higher than vanilla, above the water drops, so the two never overlap (`ClientModEvents.onLayerPre`).
- **Plain water bottles** (filled from any fresh water) restore `rawWaterRestore` (6 = 3 drops).
- **Unpurified water makes you sick sometimes:** sips AND plain water bottles have a `rawWaterSickChance` (50%) chance of giving Hunger for `rawWaterSickTicks` (200 = 10 seconds). Purified Water is always safe. Raw water also lowers your Water Purity.
- **Salt water:** Minecraft has no real salt water, so the biome decides: water in any ocean biome (including deep/warm/frozen) or a beach biome is salty. Rivers, lakes, ponds and swamps are fresh.
  - You can't sip salt water ("That's salt water! You can't drink it.").
  - You can't fill a glass bottle from it either.
  - Buckets still work, so ocean water can be taken to the Purification Table.
  - Known gap: bucket ocean water and pour it out in a non-ocean biome and it counts as fresh.
- Code: `player/WaterDrinking.java` (server + salt check), `ClientModEvents.onUseKey` (client right-click), `network/DrinkWaterPacket`.

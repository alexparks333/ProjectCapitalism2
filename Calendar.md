# The Calendar: days, seasons, years, festivals

A full calendar on top of Minecraft's day (`calendar/`). **One Minecraft day (20 minutes) is one calendar day.**

## The date (`calendar/Calendar`)
- Read from the **overworld's world clock** (Minecraft 26's clock system). Nothing is saved or sent: the server and
  every client work the date out from the same clock. The Nether and the End show the overworld's date.
- **Sleeping** moves the clock to the next morning, so it's the next day.
- **7-day weeks** (Monday to Sunday), **4 seasons of 28 days** (Spring, Summer, Fall, Winter: 4 weeks each),
  **years of 112 days**. Day 0 of a world is **Monday, Spring 1, Year 1**.
- **The time of day:** clock 0 is 6:00 am, 6000 noon, 12000 6:00 pm, 18000 midnight. It's shown in 10-minute steps
  ("9:40 am").
- **`/calendar`**: the date, the time and the next festival. **`/calendar festivals`**: the year's festivals.
  **`/calendar set <season> <day> [year]`** (operators): moves the clock to that date, keeping the time of day.
  For testing (operators): **`/calendar goto <festival>`** jumps to 7 am on that festival this year, **`/calendar next`** to
  7 am tomorrow, **`/calendar night`** to 8 pm today, **`/calendar midnight`** to 30 seconds before midnight (New Year's Eve fireworks).
  (Vanilla `/time set <number>` sets the clock to that absolute number, which resets the date. Use `/calendar set`.)

## Winter weather
- **It's winter everywhere in Winter:** whatever falls from the sky is drawn as **snow in every biome**, deserts and
  jungles too (`mixin/BiomeWinterMixin`, `client/WinterWeather`). It's only how it looks: on the server it's ordinary
  weather, so snow doesn't pile up across the world, fire is still put out, crops and fishing work as in rain.
- **It snows often in Winter:** each minute of clear weather there's a 15% chance it starts snowing for 3 to 8 minutes
  (`CalendarEvents`, while the weather is left to run, `advance_weather`).

## The clock (`client/ClockHud`)
Top right of the screen, at 85% size. It moves down below the status effect icons when there are any. It's a wooden panel:
- **A sky dial:** the sky's colour follows the time (dawn, day, dusk, night), the sun by day or the moon by night
  rides an arc across it, there are stars at night and hills along the bottom.
- **A paper card:** the day and date ("Mon. 14"), and the season with its icon (blossom, sun, leaf, snowflake).
- **Along the bottom:** the time and the weather (sun or moon, rain, storm).
- **On a festival day:** a ribbon underneath, in the festival's colour, names the festival.

## Mornings (`calendar/CalendarEvents`)
- **Every new day:** nothing is said in chat; everyone gets a small
  wooden card in the bottom right corner (`client/DayNoticeHud`, sent as `network/DayNoticePacket`): "Good morning!",
  the date, and a coloured strip for today's or tomorrow's festival. It slides in, stays 7 seconds and fades away.
- **The first day of a season:** the card's heading is the season's name and year, in the season's colour (no
  big title over the screen).
- **Festivals:** the card names today's festival, or tomorrow's as a heads-up; `/calendar festivals` says what each one brings.

## Festivals (`calendar/Festival`)

| Date | Festival | What happens all day |
|---|---|---|
| Spring 7 | Founders' Day | Every town NPC's shop is **20% off** |
| Spring 14 | Blossom Festival | Crops grow twice as fast |
| Summer 9 | Angler's Derby | Everyone has **+3 Luck** (fishing) |
| Summer 28 | Beach Festival | (Nothing yet: to be decided) |
| Fall 15 | Harvest Festival | **+50% XP** from everything |
| Fall 28 | Spooky Night | Monsters spawn in pumpkin masks; a treat for everyone (pumpkin pie, 4 cookies) |
| Winter 8 | Christmas | **Right-click the Grand Christmas Tree for a Christmas Present** (once each) |
| Winter 25 | Winter Fest | **It snows over the town** all day (whatever the biome) |
| Winter 28 | New Year's Eve | At **exactly midnight** the new year is rung in ("Happy New Year! Welcome to Year 2!" in chat) and **fireworks go up all over the town** until 1 am (8 to 11 a second for the first 10 seconds, then 3 to 6). The night rolls over to Spring 1, so nothing happens on Spring 1 itself |

- **Sales** go through `economy/NpcPricing` (`Festival.npcDiscount`), as every NPC price must.
- **Crops twice as fast:** a crop's growth check passes half the time it would have failed (`CropGrowEvent`).
- **XP:** every XP gain is ×1.5 (`PlayerXpEvent.XpChange`).
- **Fireworks:** launched from random spots all over the town (`town/Town`: the protected town center, from the server config), 3 to 6 a second, wherever its ground is loaded.
- **The Christmas present:** right-click a Grand Christmas Tree that's up (`Decorations.md`) on Christmas for a **Christmas Present** (once each that day). Right-click it to unwrap it: cookies, fireworks, snowballs, maybe a cake, a golden apple, emeralds, rarely a diamond. On other days the tree says when to come back.
- **The snow:** while you're in the town on Winter Fest, snowflakes fall all round you over the town (`client/FestivalSnow`; drawn, so it works in any biome).
- **Treats** (Spooky Night): into your inventory once that day per player (dropped if it's full).
- (The festivals can also stop monsters spawning after dark: no festival does now.)

Festival days also drive the seasonal decorations (`Decorations.md`): Spring decorations are up Spring 10 to 21, Summer 15 to 28, the harvest Fall 8 to 21, Halloween Fall 18 to 28, Christmas Winter 5 to 28. The rest of the year the town is bare.

## Testing
`calendar/CalendarTests` (`calendar`) checks:
- the date maths (weekdays, seasons and years rolling over), and the clock text;
- `/calendar set` keeping the time of day;
- the next festival, the Founders' Day sale, Harvest Festival XP, and the gift coming once a day.

Every game test starts on an ordinary day (Spring 2, noon: `gametest/GameTests`), so festivals don't change other
tests' prices.

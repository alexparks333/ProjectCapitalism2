# Cravings

Code: `src/main/java/com/alexh/capitalism/craving/` (`Cravings` the rules, the pools, the events and `/cravings`;
`CravingData` a player's cravings; `CravingTests`). Shown on the Wellness screen's **Tasks** page (`Wellness.md`).

Every morning (the Calendar day, the same day boundary as the Bounty Clerk) each player gets **their own** pair of
cravings: one **food** and one **drink**. They're mostly things only a job can make, so you buy them from Cooks,
Farmers and Bartenders, with a plain option only rarely (about 1 day in 50) so a craving is never impossible but Cooks and Bartenders get the business.

## Satisfying one

- Eating today's food (by hand, or off a Cook's plate) **fills the food bar and every golden drumstick**, even from
  empty (`ThirstData.fillFoodCompletely`). Drinking today's drink **fills the water bar and every golden drop**
  (`ThirstData.fillCompletely`). The item **still gives all its usual effects** (a cocktail's buffs, a meal's stats).
- A seared cut has to be at the **exact doneness** ("Medium Well Seared Beef Rib" takes Medium Well, nothing else).
- **1 point per craving, once each per day** (2 a day at most). A second one the same day is just food or drink.
- A chime, a level-up sound, sparkles and "✔ Craving satisfied!" in chat.
- Points go on the **"Cravings satisfied"** standings, kept by the town (the Bounty Clerk's Ledger, ◀ ▶ to its board;
  `bounty/Standings`, `Bounties.md` > Standings).
- No chat message about the day's pair (removed): players look in Wellness > Tasks.

## The pools (read from the code, so every craving is a real item)

**Food** (weights in brackets):
- Every **Cook meal** (3 each): the sushi (Salmon, Cod, Tropical and Puffer Nigiri; Cod, Kelp, Tropical, Rainbow,
  Puffer, Vegetable, Baked Salmon, Fruit, Steak, Potato, Sweet Bread and Ocean Sovereign Rolls), the Oven meals (Chicken
  Pot Pie, Loaded Baked Potato, Chicken and Rice, Lamb Chops, Seafood Pasta, Squid Ink Pasta, Seafood Chowder, Fiery
  Mushroom Bowl, Sweet Berry Tart, Stuffed Pumpkin, Golden Glazed Feast, Miner's Casserole, Garden Veggie Calzone, Nether
  Dragon Pot Pie, Honey Glazed Wing Roast, Sugar Rush Berry Crepe, Farmer's Harvest Loaf, Scholar's Brain Stew, the
  Beef, Chicken, Lamb and Fish Skewers) and the Cooking Table dishes that skip the Oven (Fruit Salad...). (`KitchenRecipes`
  SUSHI, OVEN results, COOKING results that aren't Oven inputs.)
- Every **Farmer's seared cut** at each safe doneness (1 each): the 15 cuts (beef chuck, brisket, round, rib; pork
  shoulder, belly, ham, loin; mutton shoulder, leg, rack; chicken leg quarter, breast; rabbit hind legs, saddle) at Rare,
  Medium Rare, Medium, Medium Well or Well Done (chicken: Well Done only; never Raw).
- **Plain** (1 each, rare on purpose): Bread, Baked Potato, Pumpkin Pie, Cooked Salmon.

**Drink:**
- Every **Bartender cocktail** (3 each): Glowberry Gold Rush, Nether Fireball Shot, Ender Tele-Tini, Pirate's High Seas
  Grog, Miner's Moonshine, Vampiric Mulled Wine, Overworld Mojito, Phantom's Nightcap, Zombie's Hangover Cure, Ender
  Twilight Fizz, Drunken Brawler's Stout, Classic Old Fashioned, Whiskey Sour, Watermelon Breeze, Sangria Pitcher, White
  Wine Spritzer, Tropical Kelp Cooler, Beetroot Velvet Sour, Beet Nightcap; a Glass of Red or White Wine (3 each).
- Every **liquor shot** (2 each).
- **Plain** (1 each, rare on purpose): Milk, Honey Bottle.

## Saved and synced

`ModAttachments.CRAVINGS` (kept through death, synced to its player): the day, today's food (item and doneness) and
drink, whether each is satisfied today, the total satisfied, and a reroll salt. On the client: `Cravings.of(player)`.
Today's pair is picked from the player's id, the day and the salt, so it stays the same all day.

## Admin

`/cravings <player>` shows their cravings; `/cravings <player> reroll` rolls a new pair for today. Nothing else ever
resets (only the daily roll).

## Testing

`craving/CravingTests` ("cravings"): every craving is a real item (30+ job foods, 15+ job drinks, plain ones too), never
a raw steak, today's pair stays put, a new pair each morning and each player their own, the craved food fills the food
bar and all the gold from empty, a second one the same day doesn't, the craved drink fills the water bar and all the gold
drops and its own effects still apply, 1 point each, the doneness must match, and the standings count them.
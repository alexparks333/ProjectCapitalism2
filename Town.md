# The Town

The server's central town (Overworld). Its areas are in the server config (`capitalism-server.toml`, section `town`;
corners are inclusive, either order). Code: `town/` (`Town` = the areas).

| Area | x | z | y |
|---|---|---|---|
| **Whisperer's streets** (`walkX1/Z1`, `walkX2/Z2`) | 557828 to 557917 | -849933 to -849826 | (he walks on the ground) |
| **Protected town center** (`protectX1/Z1`, `protectX2/Z2`, `protectMinY`) | 557796 to 557966 | -849948 to -849791 | **53 and up** (the town is at about y 63: everything above the surface and 10 blocks down) |

## Protected town center (`town/TownProtection`)
- Nobody can **break or place** blocks there (creative players included): "This is the town center: you can only build
  on a lot you own or lease." **Ops can** (permission level 2), so admins can still build, and so can a **lot's owner
  and tenant** on that lot (`RealEstate.md`: the town is split into lots the Real Estate Agents buy, sell and lease).
- **Explosions** (creepers, TNT, fireballs, the Wither) don't ruin it: its blocks stay, and so do item frames, paintings
  and armor stands. Mobs and players in the blast still get hurt.
- **Hostile mobs can't grief** there (`EntityMobGriefingEvent`): no Enderman takes a block, no zombie breaks a door, no
  Ravager tramples, no blaze or ghast fireball starts a fire. Villagers still farm and sheep still eat grass.
- Below y 53 it's ordinary ground (mining under the town is fine).
- **It's a safe zone, always** (the whole column, any height): no player hurts a player there, nor a player's pet or
  projectile, and nobody in it hurts anyone outside (`event/Purge.onIncomingDamage`; during The Purge too: `Events.md`).
- Not covered yet: fire a player starts spreading, pistons, water/lava flowing in.

## The Whisperer (`town/Whisperer`, `WhispererManager`, `WhispererStock`, `WhispererMenu`)
- **Looks:** an old, hunchbacked man in a long hooded grey-brown cloak with a faded violet trim; in the hood's shadow a
  wrinkled face (bushy white brows, deep-set pale eyes, a long hooked nose, a white moustache and beard hanging down),
  leaning on a gnarled cane (`client/WhispererModel`, skin by `tools/make_whisperer.ps1`).
  He hobbles (short steps, the cane planted with each one, a slow bob), murmurs now and then, and a few enchanting
  glyphs drift off him. "The Whisperer" over his head (dark purple).
- **Walks** the town's streets (the walking area), **nearly always on Stone Brick** (and mossy / cracked / chiseled
  stone bricks, their slabs and stairs): every few seconds he picks a nearby spot in town, a Stone Brick one if he can
  find one (off the streets only now and then). If he ever ends up outside the area he's put back on a path.
- **Can't be hurt or pushed**, never despawns. `WhispererManager` keeps **exactly one** in town: every 30 s, when the
  town is loaded, if there's none he appears on a Stone Brick street (extras are removed). `/summon capitalism:whisperer`
  works too.
- **Talking to him** (right-click, `client/WhispererScreen`): **he stops and looks at you until you close his menu**
  (or walk away / log out: `Whisperer.customer`). Two tabs:
- **Piece of Advice** (the first tab): pay **1 gold** for a riddle (`WhispererAdvice.TIPS`, right now 29 hints about what each Molten Socket gem does,
  never plain answers, in his voice, no numbers; never the same one twice in a row; more kinds get added over time). It's shown on a strip of parchment
  and said in chat so you keep it.
- **Knowledge** (the second tab) **sells Knowledge Pieces**: **5 per hour, shared by the whole server**
  (`WhispererStock`, saved with the world; first come first served). Each of the 5 is a random quality (Faded 40%,
  Clear 30%, Bright 20%, Radiant 10%), so a stock could be 2 Faded and 3 Radiant, or any mix. **Price in emeralds: Faded
  1, Clear 2, Bright 3, Radiant 4** (`KnowledgeQuality.emeralds`). He takes emeralds, not gold: he's not one of the job
  NPCs (`NpcPricing` is for those). The tab shows the 5 on a velvet cloth (sold ones crossed out), your emeralds,
  and a countdown to his restock ("Restocks in 42:17").

## The Blood Merchant (`town/BloodMerchant`, `BloodMerchantMenu`, `BloodMerchantShop`)
- **Looks:** a tall, gaunt vampire standing very straight: a deep black hood round a pale hollow-cheeked face, burning red
  eyes (they glow), two fangs over his lip, a high stiff collar flaring up behind his head (black outside, crimson
  inside, a gold edge), a burgundy doublet with a lace jabot and gold buttons, a blood-red gem at his throat, a long
  black cape lined with crimson to his heels, long pale hands steepled in front of him with black nails
  (`client/BloodMerchantModel`, skin by `tools/make_blood_merchant.ps1`). A faint red mist curls off him. "The Blood
  Merchant" over his head (blood red).
- **Where:** an admin puts him in his shop with **`/npc bloodmerchant`**: he stands exactly where you are, facing the
  way you face (any other Blood Merchant leaves, so there's one). **`/npc bloodmerchant remove`** sends him away
  (`town/BloodMerchantCommands`, ops). He stays on his spot (put back if he's moved), his body facing that way, his head
  following you. Can't be hurt or pushed, never despawns.
- **Only at night** (dusk 13000 to dawn 23000 on the clock): at dusk he appears in a swirl of smoke, red mist and the
  flutter of bats; at dawn he leaves the same way. By day he's not there at all: invisible, nothing to click or hit
  (his menu closes if it's open at dawn).
- **Talking to him** (right-click; he faces you), three tabs, your inventory along the bottom, your Blood Shards by the tabs:
  - **Curios:** click a card to pick it (gold outline), then **Buy** in the bar under the cards. First two **building
    blocks** at **1 Blood Shard each** (with a **Buy 64** button too): **Bloodstone Bricks** (big chiselled bricks of polished black
    stone, blood glowing at full brightness in the seams and cracks) and the **Blood Crystal Block** (see-through glowing blood crystal,
    light 10; textures by `tools/make_blood_blocks.ps1`). Then his one-of-a-kind Blood Moon decorations (`Decorations.md`)
    on velvet cards, drawn big with their price: Crimson Rug 50, Blood Moon Lantern 60, Blood Moon Banner 75, Crimson Candelabra 90, Bat Roost 100, Bat Gargoyle 125,
    Haunted Portrait 150, Vampire Coffin 200, Crimson Chandelier 200, Iron Maiden 225, Crimson Throne 250, Gothic
    Grandfather Clock 250, Secret Bookshelf Door 275, Four-Poster Crimson Bed 275, Blood Pipe Organ 300, Blood Moon Altar
    300, Blood Crystal Fireplace 325, Blood Fountain 350 (Blood Shards; two pages of cards, turned with the arrows by the
    tabs); the bosses' trophies take **300 Blood Shards and that
    boss's crest piece**: Revenant's Greatsword in Stone (Upper Left Revenant Crest), Sanguine Witch's Crystal Staff
    (Upper Right Sanguine Crest), Gorefang Skull Trophy (Lower Gorefang Crest). Anyone can buy as many as they like.
    What each decoration looks like and does (the big ones fill several blocks; the altar calls a Blood Moon, the
    fountain gives Bottles of Blood, the coffin and canopy bed are beds, the throne seats a Lord of the Manor...):
    `Decorations.md` > Blood Moon decorations.
    Last, the **Essential Wishing Star** for a **Blood Moon Crest**: **once per player, ever** (saved with the world:
    `blood_merchant_stars`); after that its card is crossed out.
  - **Mend the Crest:** the Blood Moon Crest drawn big and dim, broken in three, with a glowing socket where each piece
    goes (each takes only its own piece). Each piece you put in shows in its place, so the crest comes together; with
    all three in, **Mend the Crest** (50 Blood Shards) joins them into a Blood Moon Crest (it flares). Pieces left in
    go back to you when you close it.
- His prices are in Blood Shards (and crest pieces), not gold: he's not one of the job NPCs (`NpcPricing`). Creative
  players buy free. Photos: `gradlew runPhoto -Pphoto=merchant`.

## The Traveling Merchant (`town/TravelingMerchant`, `TravelingMerchantShop`, `TravelingMerchantMenu`, `TravelingMerchantCommands`)
- **Who:** a weary old traveller, stooped, with a long grey beard, a big red nose, a battered wide-brimmed hat with a
  red feather, a red scarf, a long patched coat, a pack and bedroll, a clay pipe (smoke curls off it now and then),
  leaning on a gnarled staff with a lantern; he grumbles like an old man (a Vindicator's mutter pitched down). Only his head turns. Beside him (on his left) stands his **covered wagon**, Oregon Trail style:
  a plank bed on four big spoked wheels, a patched canvas bonnet over hoops (open ends showing sacks and crates), the
  tongue down on the ground, a barrel lashed on, crates stacked behind, a lantern at the front (`client/
  TravelingMerchantModel`, texture by `tools/make_traveling_merchant.ps1`, the lanterns and pipe glow). He never moves.
- **Placing him:** `/npc travelingmerchant` (ops): exactly where you stand, facing your way, the wagon to his left.
  `/npc travelingmerchant remove`, `arrive` (his visit starts now: fresh wares), `leave` (today's visit ends),
  `status` (in town, or how many days until he's due).
- **When:** once every **8-14 days** (random, `MIN/MAX_DAYS_AWAY`), for **one whole day** (until dawn). The rest of the
  time he and his wagon are gone (invisible, can't be touched). **Nobody is told** when he rolls in or
  leaves (that's what the Traveler's Lantern is for). The schedule and today's wares are saved with the world.
- **His wares:** a fresh pick every visit from `POOL`, each on its own chance, **5-8 of them**, **one of each (a few things two), shared
  by everyone** (first come, first served; sold ones are stamped SOLD). Prices are **NPC prices** (`NpcPricing`: +50%
  on a fair price, Emerald sockets / Merchant's Ledger / festival sales off). With no discount:

  | Ware | Price | Chance a visit |
  |---|---|---|
  | Essential Wishing Star | 1400 gold | 10% |
  | Fun Wishing Star | 1000 gold | 10% |
  | **Purifying Waterskin** (only him) | 900 gold | 20% |
  | Pirate Hat | 400 gold | 25% |
  | **Wizard's Hat** (cosmetic, only him) | 150 gold | 10% |
  | **Welder's Mask** (cosmetic, only him) | 125 gold | 10% |
  | **Chef's Hat** (cosmetic, only him) | 100 gold | 10% |
  | **Farmer's Hat** (cosmetic, only him) | 100 gold | 10% |
  | **Traveler's Pipe** (cosmetic, only him) | 100 gold | 10% |
  | **Traveler's Bag** (only him) | 500 gold | 20% |
  | **Feeding Hat** (only him) | 200 gold | 25% |
  | Trident | 650 gold | 30% |
  | Heavy Core | 500 gold | 20% |
  | Changing Star | 450 gold | 10% |
  | **Smith's Chisel** (only him) | 400 gold | 25% |
  | Molten Socket | 300 gold | 35% |
  | Netherite Upgrade Template | 300 gold | 25% |
  | **Phoenix Feather** (only him) | 150 gold | 35% |
  | Netherite Ingot | 150 gold | 40% |
  | **Traveler's Lantern** (only him) | 50 gold | 30% |
  | An armor trim template (random: rare looks 100, others 55) | 55-100 gold | 40% |
  | **Traveler's Snow Globe** (only him) | 80 gold | 30% |
  | **Hand Warmer** (only him) | 40 gold | 45% |
  | Diamond Horse Armor | 60 gold | 40% |
  | 3 Ghast Tears | 45 gold | 50% |
  | A music disc (random, the Creeper-drop ones) | 40 gold | 30% |
  | Saddle | 30 gold | 40% |
  | 2 Rabbit's Feet | 30 gold | 50% |
  | Diamond (he brings **2**) | 5 gold each | 45% |
  | Emerald (he brings **2**) | 5 gold each | 45% |

- **Only from him** (`core/ModItems`, textures by `tools/make_traveler_items.ps1`; each tooltip says "Only from the
  Traveling Merchant."):
  - **Purifying Waterskin** (`town/BottomlessWaterskinItem`, id bottomless_waterskin): holds 16 sips (the bar under
    it). Right-click any water (lake, river, sea) to fill it right up: it comes out purified. Drink it like Purified
    Water (same thirst and purity). Empty: "It's empty. Fill it from any water." Never breaks.
  - **Traveler's Bag** (`designer/TravelersBag`, `Bag.TRAVELERS_BAG`; no Designer can make it, it isn't in the Designer
    Table or guide): worn on the back, 6 slots of anything; 1000+ blocks from the town center (or in another dimension)
    it opens out to 12. Coming back toward town it shrinks to 6: the first 6 keep their things, what was in 7-12 bursts
    out round you to pick up (done through the bag slots themselves, so nothing is lost or doubled).
  - **Cosmetic hats** (`client/CosmeticHatLayer`, palette by `tools/make_cosmetic_hats.ps1`): worn in the head slot, no
    armour and no effect, for roleplaying a job: a puffed white **Chef's Hat**, a steel **Welder's Mask** with a dark visor,
    a tall navy **Wizard's Hat** with gold stars, a straw **Farmer's Hat** with an ear of wheat, and the **Traveler's Pipe**
    (just a pipe hanging from the corner of your mouth, an ember in its bowl).
  - **Feeding Hat** (`designer/Hat.FEEDING_HAT`; not a Designer design): put it on a pet (its window, like any pet hat):
    that one pet is always full and never thirsty. A straw sun hat with a feed bowl and a carrot on top.
  - **Hand Warmer**: in your offhand, the cold can't reach you (`BodyTemperature`, after fires / Blaze sockets so it
    doesn't burn when something else already warms you). Burns down one second at a time while it works: 20 minutes in
    all, then "Your Hand Warmer has burnt out."
  - **Smith's Chisel**: right-click a socketed item with it (in your inventory, like a gem): the Molten Socket pops back
    out as an item (any gem in it comes back too), ready for a smith to set into something else. Used up
    (`Socketing.onStackedOn`).
  - **Phoenix Feather**: in your offhand when you die: no death tax that time (even in The Purge), and it burns away
    (`DeathTax.died`).
  - **Traveler's Snow Globe** (`town/TravelerBlocks.SnowGlobe`): a glass dome on a dark wooden base round a tiny snowy
    cabin and pine. Right-click it: the snow swirls (snowflakes, a chime).
  - **Traveler's Lantern** (`TravelerBlocks.Lantern`): an old rusty iron-and-brass road lantern with a handle. It lights
    up (light 15, a flame flickers) the day before he's due and while he's in town; dark otherwise. Checks every 5 s.
- The Banker's NPC price cap (`BankerEvents`) leaves him out on purpose: capping on his 5-gold diamonds would drag the
  bank's diamond price down for everyone; at most someone makes a few gold a visit.

- **The shop** (`client/TravelingMerchantScreen`): a wagon-wood frame round old parchment, his name on a canvas strip,
  a line of his talk, your gold and how long until he leaves; the wares on cloth cards (big icon, name, price in green
  if you can afford it). Pick one, then Buy. Hover for the item's tooltip and price.
- **Secret on purpose:** he isn't in any guide and nobody is told when he comes: players stumble across him (or own a Traveler's Lantern). Keep it that way.
- **Testing:** `town/TownTests` > `traveling_merchant`; photos `gradlew runPhoto -Pphoto=traveler`.

## The Fisherman (`town/Fisherman`, `FishermanMenu`, `FishermanCommands`)
- **Looks:** an old salt: a bright yellow oilskin coat to his thighs hanging open, black buckles down its edges, over
  dark green rubber waders that come up to his chest on braces (brass buttons) and a cream knitted jumper; a yellow
  sou'wester with a short brim tipped up in front and a broad one sloping down over his neck; a weathered, tanned face,
  bushy grey brows, a big bushy grey beard and white moustache, a briar pipe in the corner of his mouth (its ember glows,
  and a puff of smoke drifts up now and then); yellow sleeves, tanned hands, dark boots (`client/FishermanModel`, skin
  by `tools/make_fisherman.ps1`). Nothing like the Boat Master's navy captain's hat. "The Fisherman" over his head (sea
  blue). Now and then he grumbles like an old sailor.
- **Where:** an admin puts him on the docks with **`/npc fisherman`**: he stands exactly where you are, facing the way
  you face (any other Fisherman leaves, so there's one). **`/npc fisherman remove`** sends him away (ops, like the Blood
  Merchant's commands). He stays on his spot (put back if he's moved), his body facing that way, his head following you.
  Can't be hurt or pushed, never despawns.
- **Out day and night**: unlike the Blood Merchant he never leaves.
- **What he buys:** only the **rare catches** and **Soggy Notes** (`fishing/FishMarket`), never cod, salmon, other
  fish, junk or books (they won't even go in his slots; the Cook buys ordinary fish). A catch fetches more the heavier
  it is; a Soggy Note the gold written on it. The price table is in `Fishing.md`.
- **Talking to him** (right-click; he faces you), three tabs, your gold by the tabs, your inventory along the bottom:
  - **Sell:** a row of nine slots; each item's price shows under it (hover a slot, or something in your inventory, for
    the exact price), the whole offer in the bar below. **Sell** pays it all in gold and takes the items (a jingle of
    coins; the bar shines).
  - **Mount:** one slot on a wooden wall plaque (drawn as wide as the catch's plaque) for a catch that can be mounted;
    beside it the catch's weight, catcher and day, how many blocks wide the plaque is, and its price. **Mount** takes
    the gold and gives you the **plaque** (`FishMarket.plaqueFor`: it keeps the catch's kind and story; dropped at your
    feet if your inventory is full). Short of gold: a red message, and nothing happens. Creative players mount free.
  - **Collection:** your own Fisherman's Collection: every rare catch you've fished up yourself in colour (how many,
    the heaviest), the rest black silhouettes named "???", "Caught 7 / 15"; all 15 caught, **Claim** gives one Essential
    Wishing Star, once ever (`Fishing.md` > The Collection; `/npc fisherman collection <player> [reset]` for admins).
  - Anything left in the slots goes back to you when you close it.
- He pays for catches at the fair price, but mounting is buying from an NPC, so **plaque prices go through
  `NpcPricing`** (his markup, Emerald armour, the Merchant's Ledger, festival sales).
- Test: `town/FishermanTests` ("fisherman").

## The Bounty Clerk (`town/BountyClerk`, `BountyMenu`, `BountyCommands`)
- **Looks:** a neat, upright town official: a bottle-green frock coat with brass buttons and tails, a buff waistcoat, a
  white cravat, grey striped trousers and polished shoes; a lean clean-shaven face, neat side-parted chestnut hair,
  round gold spectacles, a white quill behind his ear, a thick red ledger held to his chest. Beside him stands a notice
  board under a little roof, pinned with bounty papers (`client/BountyClerkModel`, skin by `tools/make_bounty_clerk.ps1`).
  "The Bounty Clerk" over his head in ledger gold. He murmurs, his quill scratches, a page flutters now and then.
- **Where:** `/npc bountyclerk` puts him exactly where an admin stands, facing their way (any other Clerk goes);
  `/npc bountyclerk remove` sends him away (ops). He stays on his spot, out day and night, can't be hurt or pushed, never
  despawns.
- **What he does:** he keeps one bounty task posted for the whole server; whoever takes it (for **2 gold nuggets**, a
  flat fee: not an NPC shop price) gets it and a new one goes up for everyone else. One task per player at a time
  (taking another exchanges it, no refund), one take per Calendar day; tasks never expire and are turned in to him for
  gold. Three tabs: **Today's Task**, **My Task**, **Ledger** (the first 3 players to turn in 50 each earn a Fun
  Wishing Star). Everything is in `Bounties.md`.
- Admins: `/npc bountyclerk tasks <player> [reset]`, `/npc bountyclerk winners [reset]`.
- Test: `bounty/BountyTests` ("bounties").

## The Town Hall (`town/TownHall`)
- **The Mayoral Door** (`town/MayoralDoorBlock`): a grand mahogany-and-gold door (crafted: 5 Gold Ingots + an Emerald, door-shaped). **Only the Mayor** can open or close it ("Only the Mayor can open this door."). It works like a vanilla door: two side by side make a double door, and opening one swings its partner too. No redstone opens it, villagers can't, zombies can't break it down, pistons can't move it. Tests: `town/TownTests` > `event_switches`.
Where the town holds its meetings. There are three offices: **Mayor**, **Vice Mayor** and **Treasurer**. The town
roleplays a vote for the Mayor and the Vice Mayor, and they appoint a Treasurer; the server's admin makes it official
with a command. What officials do (hearing people out, changing the server's laws) is roleplay.
- **`/townhall`** (anyone): who holds each office, or "(vacant)".
- **`/townhall appoint <mayor|vice_mayor|treasurer> <player>`** (operators): gives the office. The whole server hears
  it, with a fanfare. One office each: appointing someone to another office leaves their old one empty, and an office
  that already has a holder passes to the new one.
- **`/townhall remove <office>`** (operators): empties it.
- **`/townhall meeting [what about]`** (an official or an operator): calls everyone to a Town Hall meeting, with a bell.
- **`/townannounce <message>`** (the Mayor, or an operator): an announcement shown bottom middle of everyone's screen
  (above the hotbar, gold), with a chime, and kept in chat ("[Town Hall] [Mayor] Name: ..."). Quotes round it are fine.
- **Titles:** an official's title shows before their name in chat and the player list, in the office's colour
  ("[Mayor] " gold, "[Vice Mayor] " blue, "[Treasurer] " green).
- Saved with the world (`town_hall` saved data).
- **The Mayoral Chest** (`town/MayoralChestBlock`, `MayoralChestBlockEntity`, `MayoralChestMenu`; looks:
  `client/MayoralChestModel`, `MayoralChestRenderer`, `MayoralChestItemRenderer`, `MayoralChestScreen`, texture by
  `tools/make_mayoral_chest.ps1`): the town's own chest, 54 slots (a double chest's worth) in one block. Dark mahogany
  banded with polished gold, gold corner caps and lock plate, the town crest on its front (white enamel, gold, crimson,
  like the Town Hall Mailbox), crimson velvet inside. It opens like a chest (the lid swings up, with a regal creak) for
  **the Mayor and the Vice Mayor**, whoever holds the office now (an office change passes the key on at once), and for
  an operator in creative. Anyone else, the Treasurer too, hears it locked: "Only the Mayor and Vice Mayor can open the
  Mayoral Chest." Not craftable: an operator puts it down from the creative tab. Nobody breaks it in survival (not
  even its officials); only an operator in creative can, and then what's inside drops. Explosions and pistons can't
  move it, and no hopper, hopper minecart or dropper puts anything in or takes anything out. It isn't locked to whoever
  placed it and counts in no one's net worth (`Economy.md`).

## Town votes (`town/TownVote`)
- **`/startvote <question>`** (the Mayor, or an operator): asks the town a yes-or-no question. Everyone sees it in chat
  with clickable **[Yes]** / **[No]** buttons and hears a bell; a bar along the top of the screen shows the question
  and the count as it goes (green while Yes leads, red while No does).
- **Voting:** type **yes** or **no** in chat (also y / n, aye / nay; it's taken as your vote, not said out loud),
  click a button, or **`/vote yes|no`**. One vote each; you can change it while it's open. **`/vote`** shows the question
  and the count.
- **`/endvote`** (the Mayor, or an operator): closes it and tells everyone the result (Yes wins, No wins or a tie, with
  the count).
- One vote at a time. People who join while it's open see it. A vote isn't kept over a server restart.

## Shop signs (`decor/ShopSignBlock`)
- **Town Hall Sign**, **Bank Sign**, **Boat Shop Sign**: Minecraft wooden boards (spruce planks in a dark oak log
  frame) 2 blocks wide and 1 tall for over a double door, the name standing out in chunky 3D block letters (a 5 x 7
  pixel font, each letter fattened and set on a dark coal outline): "TOWN / HALL" and "BANK" in gold, "BOAT /
  SHOP" in light blue. Real geometry with Minecraft's own textures.
- Put one on the wall above the **left** door (as you face it): it fills the block to the right too. Break either half
  and both go (one sign drops). No collision. Creative menu only (not craftable).
- Built by `tools/make_shop_signs.ps1` (the block models for both halves, and the item icons; add a sign there).
  Photos: `gradlew runPhoto -Pphoto=signs`.

## Testing
`town/TownTests` (with `gradlew runGameTestServer`): the protected area's edges (inside, the corner at y 53, y 52,
one block outside), breaking blocked in town but not under it or outside, both areas match the coordinates above, and
the Whisperer's stock (random qualities weighted right, buying costs the right emeralds, a sold piece is gone for
everyone, no emeralds = no sale, selling out, restocking every hour, the countdown), and advice (1 gold, a real tip, a
different one next time, none without gold).
`town/FishermanTests` ("fisherman"): Sell pays `FishMarket.valueOf` and takes the catch, cod and salmon don't go in a
sell slot, Mount takes the `NpcPricing` price and gives a plaque with the catch's kind and story, short of gold nothing
happens, and leftovers come back when the menu closes.
`town/MayoralChestTests` ("mayoral_chest"): 54 slots; the Mayor and Vice Mayor open it, the Treasurer and others
can't (nor keep the menu open); a new Mayor gets it and the old one and a removed Vice Mayor lose it; gold in it counts
for no one (even with a stray owner on it); hoppers under and over it move nothing; breaking it in survival fails;
pistons and explosions can't move it.

## Dev teleport
- `/tp town` (ops): teleports you to the ground at the middle of the protected town area (`town/TownHall`, `Town.teleportOnTop`). Overworld only.

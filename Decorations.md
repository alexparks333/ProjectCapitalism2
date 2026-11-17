# Seasonal decorations

Decorations for the town that **put themselves up when their holiday comes and pack themselves away after**
(`decor/`). You place them once. They stay where they are all year, so you don't have to take them down and put them
up again.

## When they're up (`decor/DecorKind`, `decor/Decorations`)

| Holiday | Days | Decorations |
|---|---|---|
| Christmas | Winter 5 to 28 | Grand Christmas Tree, String Light Hooks and Candy Cane Poles (and their lights), Present Pile, Candy Canes, Christmas Frost, Icicles |
| Halloween | Fall 18 to 28 (up to Spooky Night) | Spooky Scarecrow, Witch's Cauldron, Rounded Gravestone, Cross Gravestone, Old Tombstone, Pile of Bones, Ground Fog |
| The harvest | Fall 1 to 28 | Big Pumpkin Pile, Leaf Pile, Autumn Maple, Autumn Birch, Fallen Leaves |
| Summer | Summer 1 to 28 | Beach Towel, Beach Ball, Beach Umbrella, Beach Chair |
| Spring | Spring 1 to 28 | Egg Basket, Giant Painted Egg, Spring Bunny, Tulip Bed |

- **The calendar:** see `Calendar.md`.
- **`/decorations`:** what's up now, by holiday.
- **By hand (operators):** **`/decorations up`** puts every decoration up, **`/decorations down`** packs them all
  away, and **`/decorations auto`** follows the calendar again. Saved with the world. `up` is handy for trying them
  out after placing them.

## How they behave (`decor/SeasonalDecorBlock`, `SeasonalDecorBlockEntity`)
- **Nobody can break them**: unbreakable like bedrock, explosions and pistons leave them alone. Only an **operator in
  Creative** can take one away (it drops as its item), to move it.
- **One spot, one decoration per holiday:** you can place a decoration where a packed-away one of **another** holiday
  is (Christmas Frost on the ground, say, and a Big Pumpkin Pile there too). The new one goes in and keeps the old one
  there, packed away, with the way it faced and a holder's strands. When the season turns, whichever is in season takes
  the spot (with its sparkle), the other kept. If the one showing is taken away, the next kept one comes forward. (A
  spot keeps one per holiday: placing a second of the same holiday there needs the first taken away.)
- **Up:** drawn by `client/DecorRenderer`, solid where it stands, and some give light (the tree 12, the cauldron 9, the
  scarecrow 7, a hook 6).
- **Packed away:** nothing there. Nothing is drawn, there's nothing to bump into or click, and no light.
  **While you hold any decoration** (or the String Lights) you see a small marker where each packed-away one is, so
  you can find it and break it to move it.
- **Placed in season** it's up at once. **Placed out of season** it's packed away.
- **Going up and coming down:** each checks once a second whether it's due. Then it changes after its own short delay
  (up to 6 seconds, set by where it stands), so they go up one by one round the town.
  - **Going up:** a sparkle (snowflakes for Christmas, purple witch sparks for Halloween, orange leaves for the
    harvest, a warm glint for summer, cherry petals for spring) and a chime.
  - **Packing away:** a puff and a rustle.
- **On Christmas** (Winter 8) right-click the Grand Christmas Tree for a **Christmas Present**, once each (`Calendar.md`).
- **Up, they have a little life:** snowflakes drift round the tree, leaves fall off the piles, the cauldron bubbles and
  sparkles (and gurgles), and the scarecrow's crow caws now and then.

## The decorations (`client/DecorModels`, textures by `tools/BoatTex.cs`)
- **Grand Christmas Tree** (about 9 blocks tall):
  - 15 tiers of drooping fir branches, each a little different;
  - baubles on top of the branch tips (red, gold, blue, silver, purple);
  - lights in three sets that twinkle in turn;
  - gold tinsel spiralling up;
  - a turning gold star with a glowing heart;
  - a red velvet skirt and presents round the trunk.
- **String Light Hooks + String Lights**: place hooks round the town (on fence posts, walls, eaves).
  - **Stringing:** with the **String Lights**, click a holder (a hook or a **Candy Cane Pole**), then another up to 24
    blocks away. A strand of lights hangs between them, sagging. Keep clicking holder after holder to run them across the
    town. **Ctrl-click** (or sneak-click) a holder to take down every strand on it (its own and any strung to it) and start
    a new strand from it (`SeasonalDecorBlockEntity.clearAllAt`; Ctrl is read as the sprint key the server is sent).
  - **The strand:** a twisted pair of dark green wires. Every 0.4 blocks a teardrop bulb (red, green, blue, amber,
    pink in turn) hangs on a little socket, glowing, with a soft halo of its colour. They breathe gently, and the odd
    one sparkles.
- **Candy Cane Pole** (4 blocks tall): a giant red-and-white striped candy cane, its crook at the top, a green bow with a
  gold knot, a drift of snow at its foot. A light holder: strands hang from near its top.
- **Candy Canes**: three canes of different heights leaning together, crooks turned different ways, tied with a big red
  bow and a sprig of holly, in a little drift of snow.
- **Christmas Frost**: a thin layer of frost on top of a block, like a dusting of snow but frosty: sparse patches of
  white and icy blue with crystal veins and glints, the ground showing between, tiny sparkling crystals just above, the
  odd snowflake drifting up. Lies on top of a solid block (comes off if the block goes). A block model (4 patterns and
  turns, so a stretch of it doesn't repeat), nothing to bump into.
- **Icicles**: a fringe of icicles of different lengths hanging under a block (eaves, roofs, arches), see-through pale
  ice under a frosty cap, now and then a drip. Hangs from the underside of a solid block. A block model, nothing to
  bump into.
- **Present Pile** (about 2 × 2): ten wrapped presents, big ones underneath and smaller stacked on top, each turned its
  own way. The papers are red with white spots, blue with gold stars, green candy stripes, white with blue snowflakes,
  gold foil, purple with silver dots, and tartan. Each has gold, red or silver ribbon and a bow.
- **Spooky Scarecrow** (about 3 tall):
  - on a post with a crossbar, in a patched purple coat with tattered tails and a rope belt, straw out of the cuffs;
  - a pumpkin head with a carved face that glows, and a crooked witch's hat;
  - a crow on its arm looking about, little pumpkins at its feet;
  - it sways.
- **Witch's Cauldron**: an iron cauldron on three legs over a flickering fire, full of glowing green brew. Bubbles rise
  and grow, a skull bobs and turns in it, a broom leans on it, and candles stand round it.
- **Big Pumpkin Pile** (2 × 2, 2 high):
  - two hay bales with a third on top, tied with twine;
  - eleven pumpkins and gourds heaped round and on them (orange, white, a yellow squash, striped green gourds);
  - a sheaf of corn stalks rising behind, fallen leaves round it.
- **Leaf Pile** (2 × 2, 1 high): a lumpy heap of red, orange, gold and brown leaves, more scattered round it, and a
  rake leaning on it.
- **Fallen Leaves**: ground cover. A thin carpet of fallen autumn leaves on top of a block (red, orange, gold, brown,
  each with a darker midrib, the ground showing through here and there) with a few loose leaves lying just above.
  Place it block by block to cover the ground round trees and paths. A block model (2 patterns and 4 turns, so it
  doesn't repeat), lies on a solid block, nothing to bump into.
- **Autumn Maple** (about 6 tall): a spreading maple, thick dark trunk and roots, a round crown of red, orange and gold
  leaves; leaves drift down off it and lie round its foot.
- **Autumn Birch** (about 6.5 tall): a slender white birch with black marks, a tall oval crown of golden leaves, golden
  leaves falling and fallen round it.
- **Gravestones** (three kinds): a **Rounded Gravestone** (RIP, worn lines, a crack, moss), a **Cross Gravestone** (a
  Celtic cross on a two-step plinth) and an **Old Tombstone** (pointed gothic top, leaning back, skull and crossbones, a
  guttering candle that glows). Each has a grave mound in front.
- **Pile of Bones**: bones every which way, a ribcage, two skulls (one grinning out from the top).
- **Ground Fog** (about 4 × 4): dry ice fog. Soft white puffs well up near the middle, creep outward low along the
  ground, swell and thin out, then start again, each on its own clock, so it keeps rolling. Flat puffs lying on the
  ground make it thick from above, upright puffs turned to face you make it billow from the side. Each puff sits on the
  ground under it, so it follows steps and doesn't hang over drops. White by day, a pale ghostly grey at night (lit by
  hand in `DecorRenderer.fog`, texture `fog_puff.png`). Nothing to bump into.
- **Beach Towel** (2 long, flat): striped, its head end rolled for a pillow, sunglasses and flip-flops on it. Walk-through.
- **Beach Ball**: six slices (red, white, blue, yellow, white, green) with white caps, rocking gently.
- **Beach Umbrella** (about 3 tall): white pole in a mound of sand, a tilted red-and-white canopy with a fringe and a
  gold knob, swaying.
- **Beach Chair**: a low wooden folding chair, blue-and-white striped canvas, a glass of orange juice on the armrest.
- **Egg Basket**: a wicker basket with a handle, grass spilling over the rim, full of pastel painted eggs.
- **Giant Painted Egg** (over a block tall): pastel bands, zigzags and dots, in a nest of straw.
- **Spring Bunny**: a fluffy white bunny sitting up, ears pink inside (they twitch), a cotton tail, holding a yellow
  flower.
- **Tulip Bed** (2 × 2): dark earth bordered with grass, tulips (red, yellow, purple, white, pink) and daisies,
  swaying. Walk-through.

They're drawn from well beyond their block, so a big tree or a long strand doesn't pop out of view.

## Testing
`decor/DecorTests` (`decorations`) checks:
- which are up on which dates;
- placed out of season it's packed away: not solid, no light;
- a tree going up in winter and packing away in spring, staying where it is;
- `/decorations up` and `down`;
- that they change at different moments;
- string lights hooking hook to hook, and clearing a holder;
- frost and icicles needing a block, the stash (one decoration per holiday per spot) and that nobody breaks them;
- that summer and spring show only their own decorations, and fog, towels and tulip beds are walk-through.

`gradlew runPhoto` (`client/PhotoShoot`) photographs them all by day and night.

## Blood Moon decorations (`decor/BloodDecorKind`, `BloodDecorBlock`, `BloodDecorBlockEntity`, `client/BloodDecorModels(2)`)
Gothic vampire pieces, sold only by the Blood Merchant for Blood Shards (`Town.md` > The Blood Merchant). Unlike the
seasonal ones they are **always there**: anyone can break one (it drops itself) and place it anywhere; no crafting.
Each is drawn by `client/BloodDecorRenderer` from its own model and texture (`textures/entity/<id>.png` and a glowing
`<id>_glow.png`, painted by `tools/BloodDecorTex.cs`, run by `tools/make_boats.ps1`). As an item each one shows its
own **3D model** filling the slot (`client/BloodDecorItemRenderer`, a `minecraft:special` item model;
`models/item/blood_decor.json` sets the views, `blood_decor_flat.json` face-on for the skull trophy and portrait).
Flat pictures in the models (banner cloth, wings, painting, dial) are one-sided sheets a hair apart: entity render
types draw back faces, so a zero-thickness box flickers in stripes.

**Big pieces fill several blocks.** The block you place is the main one; **part blocks** (`decor/BloodPartBlock`,
`capitalism:blood_decor_part`, no item) fill the rest, each solid with its slice of the piece. Placing needs the whole
footprint free ("There isn't room for..." otherwise); breaking **any** block of it breaks the whole piece, which drops
**one** item (nothing in Creative); using a part uses the piece. The footprint runs to the **right** as you face the
piece's front, and back, and up:
- 2 wide x 2 tall: **Blood Pipe Organ**, **Blood Crystal Fireplace**; 2 wide: **Blood Moon Altar**; 2 x 2: **Blood Fountain**;
- 1 x 2 tall: **Grandfather Clock**, **Iron Maiden**, **Crimson Throne**, **Revenant's Greatsword**, **Sanguine Staff**;
- 2 long like a bed (`decor/BloodBedBlock`): **Vampire Coffin** (flat), **Four-Poster Crimson Bed** (also 2 tall);
- 1 x 2 like a door (`decor/BloodDoorBlock`): **Secret Bookshelf Door**;
- one block: lantern, candelabra, rug, banner, roost, gargoyle, portrait, chandelier, skull trophy.

**Looks and what they do:**
- **Blood Moon Lantern**: black iron cage, gold pointed arches, a stepped roof with spires and a gold crescent; inside,
  a glowing blood moon turns and breathes. Light 12.
- **Crimson Candelabra**: black iron on clawed feet, S-curled arms, five red candles with dripping wax and flickering
  flames, ruby drops. Light 12.
- **Crimson Throne**: black lacquered wood, tufted crimson velvet, gold trim, a pointed gothic back with a ruby crest
  and carved bat wings. **Right-click to sit** (`decor/DecorSeat`, an invisible seat; sneak to stand). While you sit,
  **"Lord of the Manor"** floats above your name (the seat's name tag; it goes when you stand, log out or the throne is
  broken), and **at night bats circle overhead** (drawn by the model, not real bats).
- **Vampire Coffin**: six-sided ebony coffin, gold cross, ruby, gold bat, pleated red satin inside with a velvet pillow
  and a rose. **Right-click** opens the lid; **right-click it open** to lie down in it; **sneak + right-click**
  (empty-handed) closes it. It's a bed (sets your spawn) you can use **by day too**: sleeping through the day takes
  you to **dusk** (`decor/BloodDecorEvents`); at night it's a normal bed. No sleeping through a Blood Moon.
- **Blood Fountain**: three gothic stone tiers, stone bat heads spitting arcing jets of glowing blood, ripples, drops,
  bubbles, a crowned blood moon on top, red mist, spray and bubbling. **The only place to fill a Bottle of Blood**:
  right-click with a glass bottle, **only during a Blood Moon** (otherwise "the basin only runs red under a Blood
  Moon"). A **gold nugget** (right-click with one, or tossed into the basin) is taken with a splash and a plink:
  **1 in 5** brings a Blood Shard bobbing back up.
- **Bottle of Blood** (`decor/BottleOfBloodItem`, an item): drunk **during a Blood Moon**, **Strength I and Speed I** till
  dawn (at most 10000 ticks), taken away when the Blood Moon ends, on login and on waking if none is on (only the
  blood's own effects: a potion's are left alone). Drunk any other time: **Nausea, Hunger and Weakness** for 2 minutes.
  Hands back the bottle.
- **Blood Moon Banner**: a tattered crimson banner hanging from an iron rod in front of a black iron pole, the Blood
  Moon crest (a glowing moon, bats, blood dripping), rippling.
- **Crimson Rug** (3 x 2, walk-through): crimson field, gold border, the moon medallion, gold tassels.
- **Revenant's Greatsword in Stone**: the Revenant's blackened greatsword driven into a cracked plinth, its red-hot line
  and cracks glowing, a chain and padlock across the plinth.
- **Gorefang Skull Trophy** (on a wall): Gorefang's skull with ram's horns and burning eye holes on a gold-edged plaque.
- **Sanguine Witch's Crystal Staff**: her bone staff in an ornate pedestal, a turning blood crystal with circling shards.
- **Bat Gargoyle**: a weathered, mossy stone gargoyle, carved wings with bone ribs, horns, snarl, glowing eyes.
- **Gothic Grandfather Clock**: a swinging pendulum behind gold glazing; its blood-moon dial's hands show the real
  time. Right-click: the time and how long until the next Blood Moon (if one is known). **No hourly chimes**: when a
  Blood Moon begins, **every clock tolls 13 times** (a deep bell heard about 32 blocks off). **Redstone: 15 at night,
  0 by day.**
- **Crimson Chandelier** (hangs under a block; falls if it goes): iron rings, twelve red candles, crystal drops; it
  sways. Right-click lights or snuffs it (light 15 lit). **Under a Blood Moon its candles gutter out** (smoke, no
  light) and come back after.
- **Blood Pipe Organ**: pipes, two manuals, carved wings, a blood-moon window. Right-click plays one of four haunting
  melodies (note-block sounds, red notes rising); right-click again stops it.
- **Secret Bookshelf Door**: a gothic bookshelf with one red book sticking out. **Only its owner can open it**: the
  owner of the property lot it stands on (`realestate/Properties`), or if it's on no lot, whoever placed it (not
  trusted players; Creative operators can). Others get "It won't budge." Open, it swings in on its hinge (nothing to
  bump into) and shuts itself after 5 seconds, or on another right-click.
- **Haunted Portrait** (on a wall): the Blood Merchant in an ornate gold frame; **his eyes follow the nearest player**,
  glow red at night, and it whispers at midnight.
- **Iron Maiden**: a **chest** (27 slots, dropped when broken; lockable like any container, `economy/ContainerLocks`):
  right-click opens it, and its **door swings open while anyone has it open**, shutting with a creak when the last
  closes. Inside it's **hollow**: velvet-lined, spikes pointing in from its back and its door, old bloodstains.
  **Sneak + right-click arms it** (door standing open, no menu): the first **mob** (never a player) to step in gets the
  door slammed on it: **8 damage**, held 3 seconds, then it lets go, disarmed. Sneak + right-click again disarms it.
- **Blood Moon Altar** (`decor/BloodAltar`): a carved stone altar, candles, a bowl of blood, a floating crystal. **Feed
  it Blood Shards by day** (right-click with them: it takes what's still needed) to **call a Blood Moon that night**.
  The pool and the cost are **server-wide**: any altar adds to it, each offering shows "87 / 150" and gives its giver a
  few seconds of blood-red sky. **The cost climbs each time this week: 150, 300, 450, 600...**, back to **150 every
  Monday morning**. When the pool reaches the cost a Blood Moon rises at dusk (13000), announced to everyone. **If
  it's short by dusk, everyone gets their shards back** (out of the altar if they're near it, else into their
  inventory; at their next login if they're away). No offerings at night, or while a Blood Moon is on or coming.
- **Bat Roost**: a twisted dead branch with bats hanging asleep by day; at night they flutter round it, squeaking.
- **Four-Poster Crimson Bed** (2 long, 2 tall): carved posts, a velvet canopy and curtains, gold tassels. A normal bed
  (sets your spawn); the curtains draw shut while someone sleeps in it. **Sleep through a whole night in it and your
  water is full, golden drops and all, and your body heat is back to just right (70%, dry)** (`player/ThirstData.fillCompletely`, `BodyTemperature.reset`).
- **Blood Crystal Fireplace** (2 wide, 2 tall): a gothic stone fireplace, a mantel with candles and a skull, a carved
  bat crest, blood crystals in the hearth. **Burns wood only**: right-click with logs (4 minutes each) or planks
  (1 minute), up to 30 minutes; it goes out when the wood's gone; empty-handed right-click puts it out or lights it
  again (only with wood in it). Lit: a crimson fire, crackling, light 13; it **warms the whole base**: body heat
  back to normal for anyone within 10 blocks, a 21x21x21 area (`BodyTemperature.FIREPLACE_RANGE`, `Wellness.md`), and anyone standing **in front of it** (within 3 blocks of the hearth) for 5 seconds gets
  **Regeneration I** for as long as they stay.

### Testing (Blood Moon decorations)
`decor/BloodDecorTests` (`blood_decorations`) checks: a big piece needs its whole footprint free, fills it with solid
part blocks and breaks whole from any part, dropping one item; the iron maiden keeps and drops its items, opens while
its chest is open, and its trap (a mob slammed and held, a player spared, let go after 3 seconds); the fireplace takes
wood not coal (log 4x a plank), heals in front of it after 5 seconds but not beside it, and goes out when burnt; the
altar's cost ladder (150, then 300), the Monday reset, refunds at dusk (out of the altar, into the inventory, kept for
an absent player) and no offerings by night; the fountain gives no blood without a Blood Moon and takes tossed gold
nuggets; the Bottle of Blood's effects taken after its night but a potion's left; the coffin usable by day and only
open; the bookshelf door's owner; the chandelier's light and its ceiling.
`gradlew runPhoto -Pphoto=blooddecor` photographs them all (and `-Pphoto=blooditems` their 3D items, `bloodbanner` the
banner close up in the dark).

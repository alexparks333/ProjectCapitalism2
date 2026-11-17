# Boats: the Boat Master and the Premium boats

## The Boat Master (`npc/BoatMaster`)
A town NPC at the docks. **Not a job's stand-in:** he's always there once an op sets his spot.
- **`/npc setspot boat_master`** (where you stand, facing your way), **`/npc removespot boat_master`**, shown in
  `/npc list` ("always open"). Uses the job NPCs' spots and checks (`npc/NpcManager`, `NpcSpots`).
- A villager in the fisherman's outfit with a navy captain's hat (`textures/entity/npc/boat_master.png`,
  `tools/make_npc_skins.ps1`), a little Pirate Ship across his arms, "Boat Master" in aqua. Can't be hurt, moved or
  killed, silent, never despawns (all `JobNpc`'s).
- **His shop** ("The Boat Master's Docks"; `NpcShops.BOAT_MASTER`; every price through `NpcPricing.npcPrice`):

| Tab | What | NPC price | Stock |
|---|---|---|---|
| Boats | Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove Boat | 3 nuggets | no limit |
| More Boats | Cherry, Pale Oak Boat, Bamboo Raft / Oak and Spruce Chest Boat, Bamboo Chest Raft | 3 / 6 nuggets | no limit |
| Premium | The Castaway's Tub **50 gold**, Speedboat **100**, Swamp Smuggler's Raft **150**, Yacht **300** (the best Premium) | | 2 each per restock |
| Legendary | River Queen Steamboat **350**, Nordic Longship **400**, Steampunk Paddlewheeler **450**, Pirate Ship **500**, The Wandering Teapot **550** | | **2 of each, ever**: they never restock |
| Legendary II | Spectral Galleon **666**, The Clockwork Submersible **750**, The Fairy Blossom **800**, The Leviathan's Wake **900** | | **2 of each, ever**: they never restock |

(The shop shows 7 rows a tab, so the legendaries take two tabs.) **The Legendary boats never restock**: only 2 of each will ever
be sold on the server, shared by everyone (`NpcShops.legendary`, `Offer.forever`; `NpcStock` keeps a tally of how many
have ever been sold, which the 20-minute restock doesn't clear). Boats, More Boats and Premium restock as usual.

NPC shops are keyed by a **shop key** (a job's id, or `boat_master`): `NpcShops.sections(String)`, `NpcStock`, `NpcShopMenu`.

## Premium boats (`boats/PremiumBoat`)
Luxuries for players with gold to burn: mostly about the look. All of them:
- **10% faster** than a boat (`PremiumBoat.SPEED_BOOST`: the forward push is 10% stronger, so the top speed is too).
  Otherwise they handle like a boat and are a **boat's size on the water** (they only look huge).
- **Locked to whoever puts it on the water**, like a chest: nobody else can get in, break it (hits or arrows) or tow it
  on a lead; admins can. The owner **shift + right-clicks it with an empty hand** to unlock it (anyone can ride) or lock
  it again. (Boats placed before the lock existed have no owner: break and place them again to lock them.)
- **Their own seats** (`seats()`); the first one in steers.
- **They light up what's round them** (`lights()`): invisible light blocks carried along at their lanterns, fires and
  windows, only put into empty air, taken away as it moves on, when it's broken or picked up, when its chunk unloads,
  and cleared on load if a crash left any behind. Soft (levels 5 to 10): a small pool of light round the lanterns and
  fire, not a floodlight. A new light goes down before the old one is cleared, so there's never a gap. **The boat itself
  isn't lit by them** (`PremiumBoatRenderer.getBlockLightLevel` takes its own brightest light, `maxOwnLight`, off the
  light on it, the same every frame, so it never flickers), so at night it stays dark and only its glowing parts shine: the full-bright glow pass on
  the model (`<boat>_glow.png`: lantern glass, lit windows, fire, gauges...). Other lights (torches) still light it.
- Spray and wake when they go fast. The item is one to a stack, Epic, **doesn't burn**; breaking the boat drops it.
- **Riding in first person, you see the boat ghosted** (see-through, `PremiumBoatRenderer`), so its masts, sails,
  decks and cabins don't block your view; everyone else, and you in third person, see it solid. (On the Pirate Ship,
  Spectral Galleon and Longship the masts in front of your seat are hidden too.)
- **Lava boats** (`boats/LavaBoat`: the **Spectral Galleon** and **the Leviathan's Wake**) sail on lava just as on water
  (NeoForge's `canBoatInFluid`) and don't burn. **Their riders never catch fire and take no fire or lava damage**
  (`BoatEvents`: their fire is put out every tick, before and after they tick, on both sides so the flames never show,
  and fire damage is cancelled; they also get Fire Resistance). They trail smoke and the odd lava pop instead of spray.
- **Flying kicks:** the server's "floating a vehicle too long" check can kick someone sailing one (it looks for air
  under the boat's small box). Our servers turn it off: `allow-flight=true` in `server.properties`.

| Boat | Seats | What's special |
|---|---|---|
| **Speedboat** | driver + passenger (bucket seats), 2 on the bench | Deep-V white hull, racing stripes, gold bolt; wraparound windshield; dash gauges glow; wheel turns; twin outboards whose props spin; lifts its bow as it planes; rooster tail of spray |
| **Swamp Smuggler's Raft** | 2 under the lean-to, 2 on the cross logs | Mossy logs lashed with vines, lily pads, patched tarp lean-to with swaying moss, contraband crates, moonshine, skull totem with an eerie green lantern, fishing rod, a frog; fireflies |
| **Nordic Longship** | helmsman, 4 on rowing benches, 1 at the prow | Clinker spruce hull, carved dragon prow (glowing eyes) and curled tail, painted round shields both sides (staggered like a shield wall), oars that row as you paddle, a red-and-white striped sail that billows, a campfire whose flames flicker (a thin wisp of smoke, the odd ember) |
| **Steampunk Paddlewheeler** | the pilot, 2 on the deck house roof | Oxblood and brass, two paddlewheels under sunburst boxes turning with speed; drive gear and pinion, flywheel, pumping pistons, a governor whose balls fly out; boiler with glowing firebox; twin stacks pour smoke and steam when driven |
| **Pirate Ship** | helm, 2 on the main deck, 1 on the forecastle | The galleon: 3 masts, tattered black sails (skull and crossbones, cutlasses) that billow, waving Jolly Roger, cannons, glowing stern gallery, turning wheel |
| **Spectral Galleon** | as the Pirate Ship | The galleon come back from the deep: drawn see-through, rotted and torn, glowing spectral cyan, soul-fire lanterns, a hooded ghost bobbing in the main top with a soul lantern; souls rise off it, mist trails it |
| **Yacht** | helm + 1 on the flybridge, 2 on the aft sofa, 2 **in the hot tub** | Superyacht: navy band, lit portholes, three decks with glowing window bands, flybridge helm, spinning radar, hot tub with glowing water and bubbles, tender on a davit, swim platform, flag |
| **The Castaway's Tub** | 1, on the composter | Half a giant barrel of stripped jungle wood with rusty hoops, half full of water; a composter captain's chair; a sad white banner on a crooked bamboo shoot; a wooden shovel paddle (paddles when you do); a message in a bottle bobbing; it wallows, drips and leaks |
| **The Wandering Teapot** | 3, inside the pot | A round porcelain teapot (blue flowers, gold bands) on a birch saucer; spout, handle, its lid tipped open, **a wide window in its front to see out**; brewing stands, a cake, cushions inside; a campfire glowing under it (gentle white steam from the spout and lid); a billowing plume of soft steam puffs and cobwebs from the spout for a sail |
| **River Queen Steamboat** | helm (between the texas and the stacks) + 2 front rail + 2 back rail, on the hurricane deck | In the style of the great theme-park riverboats: a white three-deck sternwheeler, its decks' fronts **rounded**; turned posts with gold capitals and **lacy gingerbread fretwork in every bay**; railings of turned balusters; **red, white and blue bunting fans** along the promenade; the main deck open forward, two black boilers on a brick bed with **glowing firebox doors**; the saloon's tall lit windows under gold arches; the texas cabin with the **pilothouse on its front** (lit all round, fretwork fringe, a gold crest and finial), the bell behind; **twin plain black stacks forward**, close together, with feathered gold crowns and a gold arch carrying a **star in a ring** between them; a big red sternwheel (turning with its speed, pitman arms pumping from the engine cylinders); a pennant on the jackstaff, the **Stars and Stripes** at the stern |
| **The Clockwork Submersible** | 1, in the middle of the glass dome (under the water) | Waxed oxidized copper, gold bands, iron trapdoors, lightning rods; conning tower with a periscope that sweeps; a big wooden propeller spinning in its shroud; bubbles churning up behind it all the time. **Its riders sit under the water in the clear glass dome** at a glowing redstone panel: they stay aboard under water and get Water Breathing while aboard (not Conduit Power: under water that works as Night Vision and would light everything, so the deep stays dark but for the flashlight). **F with an empty hand: its flashlight**, a horror-game torch: it points where its rider looks (up to 70° either way, 60° up or down) and lights a **cone** (20° either side, 32 blocks): 31 rays in rings, each lighting what it lands on (light 15 close up to 11 at full reach, the outer ring dimmer), stepping back past seagrass and kelp, and a few fainter lights down the middle of the beam; nothing round the boat itself, open water stays dark (`Submersible.beamLights`). It **doesn't flicker**: the light holds its aim until it turns more than 3° and where it casts from until the lamp moves most of a block, a light already beside a wanted spot stays put, and its own light blocks count as free spots (`sub_flashlight` checks bobbing and twitching move nothing). Seen from outside, a soft cone of light (a shell of 14 tapering glowing panels); from the dome a soft haze down the beam (the shell and the control panel and lamp are hidden from the rider's own view); silt drifts in it. Measured by the `sub_flashlight` test |
| **The Leviathan's Wake** | helm at the tail, 2 by the mast, 1 by the skull | A dark oak deck inside a sea monster's ribcage of bone ribs, blue soul lanterns swinging from their tips; its skull at the bow (glowing eyes), its tail behind; a vertebra mast with a fan of amethyst shards and purple glass that shimmers (and hums now and then); a crying obsidian heart that beats and weeps |
| **The Fairy Blossom** | helm on a stool behind the arbor, 2 on the loveseat under it | A slender leaf-shaped boat, pearl white blushing to lilac with gold filigree vines painted on its sides, **smothered in flowers**: garlands along the gunwales, swags of blossoms down its sides between roses, a vine curling over the bow like a fern frond with a **glowing bell flower** swaying from it, a smaller curl at the stern with a cascade of flowers; an **arbor** of silver birch arches over the loveseat with fairy lights (pink, mint, white) and **wisteria** hanging in long swaying clusters; for a mast a stem with two great leaves and a **lotus** (three rings of petals, a glowing heart and stamens, it nods); **four see-through iridescent wings** on glowing jewels (veined in gold), folded and breathing on the water, spread and fluttering in the air; petals drift off it, fireflies about it. **Space: it flutters** (`FairyBoat`): up 2.5 blocks, holds that height 2 seconds (steering and sailing on), then drifts gently back down; a short rest after landing. It runs on the driver's side, like a boat's movement; its wings spread whenever it's off the water, for everyone. No fall damage in a flutter |

## No clipping (`BoatKit.nudge`)
Where two boxes' faces lie in exactly the same place their textures fight (flicker). Every box grows by a hair (one of 11
sizes, up to 0.13 px), chosen so it differs from every box already made that shares a face plane with it, like colouring
a map; bars don't grow along their length; wheels' rims, spokes and gear discs alternate their widths; flat things that
overlap (shields, glass panes) are set a hair apart. `BoatTests` checks every model for faces still fighting and logs
any by name (a hairline spot or two deep in a joint is let through).

## Code and art
- Entities `boats/` (`PremiumBoat` + one class each), item `PremiumBoatItem`, tests `BoatTests` (`boats`: the shop,
  every price, the 10% speed, the lock, the light blocks, the lava boats and their riders, the flashlight landing on a wall, the Fairy Blossom's flutter, every model building, animating and not clipping, and
  animating via `client/PirateShipModelCheck`).
- Models `client/`: `PirateShipModel` (+ `GhostShipModel`, the same frame, see-through), and the rest built with
  `BoatKit` on `PremiumBoatModel`; all drawn by `PremiumBoatRenderer` (glow pass, speed / turn / first-person seat).
- Art: **`tools/make_boats.ps1`** runs `tools/BoatTex.cs` (the BoatKit boats: their shared zone layout must match
  `BoatKit`'s; each boat's hull-side picture, sails, shields and flag) and `tools/PirateShipTex.cs` (the Pirate Ship and
  the Spectral Galleon's ghost style; its zones must match `PirateShipModel`'s). Each boat: `textures/entity/<boat>.png`
  (1024 x 1024, 4x the model's 256 x 256 layout), `<boat>_glow.png`, `textures/item/<boat>.png` (32 x 32).

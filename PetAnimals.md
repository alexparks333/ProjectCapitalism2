# Pet Animals: every animal, its petting buff and what it eats

Every animal a Pet Trainer can train into a pet (`PetTrainer.md` has how training, claiming and caring work; the code is
`pets/PetTraits`). **Keep this matching `PetTraits` when a pet changes.**

## How the buffs work
- The **first time the owner pets their pet each Minecraft day** (right-click it with an empty hand), they get its
  buff. Petting it again that day only gives smiles. Sleeping through the night counts as a new day.
- **One pet buff at a time** (`pets/PetBuffs`): petting another pet for its first time that day takes the last pet's buff
  off and gives the new one, so you choose which buff you want (or wait for one to run out, then pet the next). Only
  the pet's part comes off: your own potions, food and drinks stay, even of the same kind, with their own time left
  (Minecraft keeps the strongest of a kind on and the weaker ones underneath, so a pet's Speed II beats your Speed I
  drink, and the drink is back when the pet buff ends or is replaced). Different effects all work together.
- Only the owner gets it (a pet has to be claimed with an Ownership Tag first).
- **Tiers:** everyday farm animals give a **Small** buff; animals that are tamed or harder to come by give a **Good**
  one; hard to tame, rare or far-away animals give a **Great** one (often two effects, and longer).
- The pet window's **Traits** tab (Ctrl + right-click your pet) shows its tier, its buff and what it eats (hover a
  food for its name).

## Feeding
- A pet eats **what it breeds with in vanilla** (by hand, or from a Food Trough within 5 blocks).
- Two animals can't be bred in vanilla, so they get pet food of their own: the **Polar Bear** eats Raw Cod and Raw
  Salmon, the **Parrot** eats seeds.
- The **Axolotl** eats plain Tropical Fish and the **Nautilus** plain Pufferfish (not a bucket of fish, which vanilla uses).
- Every pet drinks plain water (a Water Bucket or Bottle by hand, a Water Bowl, or standing in water).

## Can't be pets
Horses, donkeys and mules (skeleton and zombie horses too): they're riding animals. Llamas and camels are fine.

## Small buffs (everyday farm animals)
| Animal | Buff when petted (once a day) | Eats |
|---|---|---|
| Pig | Absorption I, 5 min | Carrot, Potato, Beetroot |
| Cow | Regeneration I, 1 min | Wheat |
| Sheep | Resistance I, 3 min | Wheat |
| Chicken | Slow Falling, 5 min | Wheat / Pumpkin / Melon / Beetroot / Torchflower Seeds, Pitcher Pod |
| Rabbit | Jump Boost I, 5 min | Dandelion, Carrot, Golden Carrot |

## Good buffs (tamed or harder to come by)
| Animal | Buff when petted (once a day) | Eats |
|---|---|---|
| Wolf | Strength I, 2.5 min | Any meat or fish, raw or cooked (Porkchop, Beef, Chicken, Mutton, Rabbit, Cod, Salmon, Tropical Fish, Pufferfish), Rabbit Stew, Rotten Flesh |
| Cat | Night Vision, 2.5 min | Raw Cod, Raw Salmon |
| Ocelot | Speed I, 5 min | Raw Cod, Raw Salmon |
| Parrot | Luck I, 5 min | Wheat / Pumpkin / Melon / Beetroot / Torchflower Seeds, Pitcher Pod *(pet food)* |
| Bee | Haste I, 5 min | Flowers (any small or tall flower, Flowering Azalea, Cherry Leaves, Spore Blossom, Pink Petals...) |
| Frog | Jump Boost II, 5 min | Slimeball |
| Turtle | Water Breathing, 5 min | Seagrass |
| Llama | Resistance I, 3 min | Hay Bale, Wheat |
| Camel | **Refill Thirst Bar** (instant: fills your thirst bar to the top) + **No Water Draining**, 3 min (your thirst bar doesn't go down; `core/ModEffects`, the stop is in `core/ModServerEvents`) | Cactus |
| Strider | Fire Resistance, 5 min | Warped Fungus |
| Armadillo | Resistance II, 5 min | Spider Eye |
| Axolotl | Conduit Power, 5 min | Tropical Fish |

## Great buffs (hard to tame, rare or far away)
| Animal | Buff when petted (once a day) | Eats |
|---|---|---|
| Panda | Health Boost I (+2 hearts), 3.5 min + Regeneration I, 4 min | Bamboo |
| Goat | Strength I, 5 min + Jump Boost II, 7.5 min | Wheat |
| Polar Bear | Strength I + Resistance I, 7.5 min | Raw Cod, Raw Salmon *(pet food)* |
| Sniffer | Luck II + Haste II, 7.5 min | Torchflower Seeds |
| Fox | Speed II, 4 min | Sweet Berries, Glow Berries |
| Mooshroom | Health Boost I, 7.5 min + Saturation (fills you up) | Wheat |
| Hoglin | Strength II, 7.5 min | Crimson Fungus |
| Trader Llama | Hero of the Village, 7.5 min | Hay Bale, Wheat |
| Happy Ghast | Slow Falling, 7.5 min + Regeneration I, 2 min | Snowball |
| Nautilus | Conduit Power, 7.5 min + Dolphin's Grace, 5 min | Pufferfish |

## Any other animal
Anything else that counts as an animal and isn't on these lists gives **Regeneration I for 1 minute** (Small) and eats
what it breeds with.

# Pet Trainer (Fun job)

Players bring animals home the normal Minecraft way (leads, food, boats... no new taming items). **First** the Pet
Trainer trains the animal into a **pet** with the **Training Stick**; **then** an **Ownership Tag** (only Pet Trainers make
them, anyone can use one: they sell them, e.g. with the pet) claims the pet for its new owner. A tag only works on a
trained pet. Code: `pets/` (`Pets` = the rules, `PetData` = the data,
`PetTraits` = each animal's buff and food, `OwnershipTagItem`, `TrainingStickItem`, `FeederSize`, `FoodTroughBlock`(+`Entity`,
`Menu`), `WaterBowlBlock`(+`Entity`, `Menu`)), `client/PetHud`, `client/PetScreen`, `client/FoodTroughScreen`,
`client/WaterBowlScreen`, `client/PetTrainersGuideScreen`, `network/PetActionPacket`, `RenamePetPacket`. **Every animal's
buff and food: `PetAnimals.md`.** `Job.PET_TRAINER`. Assets: `tools/make_pet_textures.ps1`,
`tools/make_pet_assets.js`.

## Items and blocks (job-only recipes, `capitalism:job_shaped` "pet_trainer")
The Medium (iron) shapes would match the vanilla Cauldron and Minecart, so every size has the Hay Bale / Bucket
in the middle. Sizes: Small is enough for one pet for a while, Medium for two or three, Large for about ten.

| Item | Recipe | What it does |
|---|---|---|
| **Ownership Tag** (`ownership_tag.json`) | Name Tag + Diamond, side by side | Anyone: right-click a **trained pet** that isn't anyone's and it's yours. Used up. On an untrained animal: "Only a trained pet can be claimed." (not used up). |
| **Small / Medium / Large Food Trough** (`<size>_food_trough.json`) | Upside-down pants with a Hay Bale in the middle: M gap M / M Hay M / M M M. M = Copper Ingot (Small), Iron Ingot (Medium), Compacted Iron (Large) | A low metal trough with **1 / 2 / 5 slots** of food (right-click: its window, laid out like a hopper; hoppers fill it too). Shows a heap of food when there's anything in it. |
| **Small / Medium / Large Water Bowl** (`<size>_water_bowl.json`) | The trough without the top corners, an empty Bucket in the middle: M Bucket M / M M M (the bucket is used up; a new bowl is empty). Same metals | A round metal bowl (with a lip round the top) holding **1,000 / 3,000 / 10,000 mL** of plain water (`FeederSize`). A Water Bucket pours in 1,000 mL (you keep the bucket), a Water Bottle 250. Right-click with anything else: its window shows the mL in a tank. |
| **Training Stick** (`training_stick.json`) | Lead, Fishing Rod, Diamond on top; two Sticks down the middle | Pet Trainers only: right-click an animal nobody owns to train it into a pet. **Costs 25 levels.** Lasts **5** trainings. |

## Training Feed (`pets/TrainingFeed`, made by Cooks)
**Cooks** craft it, **only Pet Trainers can use it** ("Only Pet Trainers can use Training Feed."; creative can) (`capitalism:job_shaped` "cook", `training_feed.json`): any seed (`#capitalism:seeds`) with any raw
meat or fish next to it (`#capitalism:raw_meat_or_fish`) = **6**; or a seed with **Rotten Meat** (rotted Meat Slabs
and cuts, `Farmer.md` > Freshness; `training_feed_from_rotten_meat.json`) = **6** too. Pet Trainers buy it from Cooks (or the NPC Cook:
**5 for a gold ingot**, `NPCs.md`). Right-click an animal **on the pet list** (`PetAnimals.md`) with it: one is used
and the animal **follows you for 10 seconds** (walks after you, pathfinding round things in the way, stops close by; gives up if you get more than 24 blocks away. Goal animals get a top-priority `FollowFeederGoal`, ahead of wandering and panicking; brain animals (goats, axolotls, frogs, camels, sniffers, armadillos, hoglins...) have their walk target set to you every tick), to help
bring animals home now that Pet Trainers can't use an Animal Bjorn. Not riding animals (they're not on the list),
not someone else's pet or tamed animal.

## Animal Bjorn (`pets/AnimalBjornItem`, `CarriedAnimal`, `client/BjornLayer`)
A baby carrier for animals, so people can take them on adventures. **Pet Trainers craft it** (String, gap, String /
Rabbit Hide, **Backpack**, Rabbit Hide / Rabbit Hide x3; `animal_bjorn.json`. The Backpack will come from the Designer,
see `Back.md`); **anyone but a Pet Trainer can use it**.
- **Carry:** right-click an animal with an empty Animal Bjorn: it climbs in (taken out of the world; everything about it is
  kept in the bjorn's `CARRIED_ANIMAL` component, its pet data, name and owner too). The bjorn is called "Animal Bjorn
  (Pet Pig)", and its icon shows an animal peeking out (`items/animal_bjorn.json` picks by the component).
- **Let out:** right-click the ground with a full one: it climbs out there, the very same animal.
- **Not by a Pet Trainer** ("Pet Trainers can't carry animals in an Animal Bjorn."; they can still make, sell, wear and empty
  one): they'd scoop up wild animals and carry them back to the shop. Creative players can.
- **Only your own pet** (claimed by you: "You can only carry your own pet in an Animal Bjorn."; creative can carry
  anything). **Not:** someone else's pet ("This animal belongs to X."), animals wider than a Polar Bear (Sniffer, Camel, Happy Ghast:
  "too big"), horses / donkeys / mules, or an animal with something riding it.
- **Wear it in the Back slot** (`Back.md`): a padded teal carrier with leather straps on your chest
  (`textures/entity/animal_bjorn.png`), and **the animal's head poking out of its top front, slowly looking around** (wandering within 40 degrees of straight ahead, never right round to the side; the pouch is shallow and snug so the head doesn't clip into it) (its
  own model's head part; an animal with no head part, like a bee, shows whole and shrunk), for everyone to see.
  With it on your back: **sneak + right-click an animal with an empty hand** to put it in, **sneak + right-click the
  ground with an empty hand** to let it out.
- **A carried pet still gets hungry and thirsty.** The bjorn remembers when it climbed in (`CarriedAnimal.since`, and its carrier's played time,
  `played`: only time they were online counts); when it climbs out it's caught up on that time (`Pets.timePassed`): its bars drop as they would have, and it loses half a
  heart for every 20 s a bar was empty (down to one heart, or to death if it may starve by then). It can't eat, drink or heal in there, and gives no
  daily buff. (A bjorn filled before this has no time: nothing passes.)

## Claimed pets (`Pets.claim`, step 2)
- Only a pet (trained first). Not one someone else has tamed (a wolf, a cat...).
- Owner (UUID + name) saved on the animal (`ModAttachments.PET`, saved once it's a pet, synced to everyone who can
  see it).
- **Other players can't hurt it** (`LivingIncomingDamageEvent`; mobs, fire and falls still can) **or do anything with
  it**: right-clicking it is blocked ("This animal belongs to X."): no leads, riding, milking, shearing, name tags,
  breeding. Exception: **feeding / watering a pet**.
  Creative players can do anything.
- Before it's claimed a pet is anyone's to handle (it's still in the Pet Trainer's shop).

## Pets (`Pets.train`, step 1)
- Any `Animal` nobody owns (pigs, cows, pandas, wolves...) **except riding animals**: horses, donkeys and mules
  (skeleton / zombie horses too; `Pets.trainable`: "Riding animals can't be trained into pets."). Llamas and camels can; costs the trainer 25 levels (creative: free); one use
  of the stick. A pet can't be trained twice. It **never despawns** from then on (`setPersistenceRequired`).
- It's named **"Pet <Animal>"** ("Pet Pig", "Pet Panda") unless it already had a name; the name shows over its head, drawn by `client/PetNames`: fully there within 10 blocks, fading out to nothing at 16 (`FADE_FROM`, `FADE_TO`), and never through walls (no see-through copy).
- **Docile:** a pet never picks a target (`LivingChangeTargetEvent`: polar bears, hoglins, wolves, bees, goats...) and
  never hurts anyone (damage it would deal is cancelled). A pet Hoglin never turns into a Zoglin in the Overworld.
- Otherwise it behaves like before, plus **hunger and thirst** (bars sized to the pet, below). The card shows its name, its
  hearts (the animal's own health, up to 15), drumsticks and water drops (filled from the right, emptying from the left like the hunger bar), and "Owner: X" or "No owner yet"
  (`client/PetHud`).
- **While its owner is offline an owned pet doesn't change at all** (no hunger, thirst, harm, eating or drinking:
  `Pets.ownerOnline`), so it can't starve while they're away. A pet nobody owns yet (in a Pet Trainer's shop) goes on.
- **Bars as big as the pet** (`Pets.barSize`, like its hearts): 2 points (one drumstick / drop) per heart of its own health
  (without a hat), at least 3 and at most 15 of each: a chicken 3, a pig 5, a polar bear 15. Saved and synced (`PetData.size`).
- **Hunger** and **thirst** drop the same points per second for every pet, whatever its bar's size: a point of hunger
  every 5 minutes, of thirst every 200 seconds. So a small bar empties sooner (a chicken's 6 points: 30 minutes of
  hunger, 20 of thirst) and a big one lasts longer (a polar bear's 30: 150 and 100). Each food still gives its own points.
  Healing (both bars at 75%+), walking to a trough (80% or less) and the room to eat / drink (10% / 20%) are shares of the bar.
- **Feeding:** right-click with its own food (`PetTraits.eats`: what it breeds with, plus pet food for the Polar Bear and
  Parrot, which vanilla gives none) while it's hungry: + the food's hunger value (4 for things like wheat or seeds). A
  bucket of fish leaves its Water Bucket. When full, food does the normal thing (breeding).
- **Water:** a Water Bucket fills it (you get the bucket back), a Water Bottle gives 8 (you get the glass back); standing
  in water it drinks 1 a second.
- **Health:** both bars 15+ heals half a heart every 10 s; either bar empty hurts half a heart **every 20 s**, but
  **not below one heart**, unless it may starve (`Pets.mayStarve`): with **both bars empty**, or one bar empty for **more
  than 5 minutes** (`STARVE_SECONDS`, `PetData.emptySeconds`), it keeps getting hurt and **can die**.

## Food Troughs and Water Bowls (`Pets.troughAndBowl`, once a second)
- A pet looks for a trough with **its own food** / a bowl with water within **5 blocks** (2 up or down), the nearest.
- Standing next to it (within 1.9 blocks): it eats one food a second (+ that food's hunger value) while there's room
  for a bite (hunger 18 or less), or drinks when thirst is 16 or less: just what it needs, **25 mL a thirst point** (a
  whole bar is 500 mL; a nearly empty bowl gives what's left). So it eats till it's full, and a full pet doesn't nibble
  every second. The bowl's water shows at a third / two thirds / full (`LEVEL`).
- Further away: it walks over once hunger is 16 or less / thirst 16 or less. The emptier bar goes first.
- Plain water works (no purification needed). A pet told to Stay Still doesn't go.

## The owner and their pet
- **Petting:** right-click your pet with an empty hand: it makes its happy sound; **the first pet of the day** (when you get its buff) also floats little patting hands up
  (`ModParticles.PETTING`) (not when it's saddled: then it's for
  riding).
- **Pet window** (`client/PetScreen`): **Ctrl + right-click** your pet (client side; nothing is sent for the click).
  - **Traits:** see below (its daily buff, what it eats).
  - **Status:** name, what it is, hearts (with numbers), hunger and thirst bars (with numbers).
  - **Actions** (no explanations, just the controls): **Stay Still** on / off (`PetActionPacket`; the server checks owner and distance 16): it doesn't move at
    all (no AI: no walking, turning, wandering, eating from troughs) until it's turned off. Saved.
  - **Name** (also on Actions): type a new name (up to 24 letters) and press Rename or Enter (`RenamePetPacket`,
    `Pets.rename`: owner only, within 16 blocks). Empty: back to "Pet <Animal>".
  - Closes if the pet goes away or dies.

## Hats (made by Designers, `designer/Hat`, `Designer.md`)
In the pet window's **Actions** tab, under the name: the **Hat** row. The hat it wears (click it to take it off: it goes
back to your inventory), or the Designer's hats you carry (click one to put it on: one is taken from your inventory).
Owner only, within 16 blocks (`PetHatPacket`, `Pets.wearHat`). Saved and synced with the pet (`PetData.hat`); a pet that
dies drops its hat.
- **Gucci Top Hat:** hunger drops at 70% speed (`Pets.hungerPerSecond`).
- **Burberry Santa Hat:** thirst drops at 70% speed (`Pets.thirstPerSecond`).
- **Louis Vuitton Baseball Cap:** starving hurts it every 40 s instead of 20 (`Pets.hurtSeconds`): it lasts twice as long.
- **Feeding Hat** (only from the Traveling Merchant, `Town.md`; not a Designer design): the pet wearing it is always full and never thirsty (`Pets.second` keeps both bars topped up). Only that pet.
- **Prada Fedora:** +2 hearts (a max-health modifier, removed with the hat), and both bars go to 24 instead of 20
  (`PetData.max`: 12 drumsticks and 12 drops on the card and in the window).
The bjorn's catch-up time (`Pets.timePassed`) uses the same rates.
- **Shown:** on the pet's card (looking at it, `client/PetHud`) a line at the bottom with the hat and what it does; and
  **on the animal's head** for everyone (`client/PetHatLayer`): a little model per hat (top hat, santa hat, cap with the
  peak to the front, fedora; textures `entity/pet_hat_*`), sitting snug on the top of the skull (sunk 0.6 of a pixel so it doesn't float) (the biggest box of its model's
  head), a little toward its front (heads that sink into a taller body, like a polar bear's, would swallow it), sized to it
  and turning with the head. **In an Animal Bjorn** it's on the head peeking out too (`BjornLayer`, `PetHatLayer.submitOn`). Animals whose model has no head part show no hat.

## Traits: a buff for the first pet of each day (`pets/PetTraits`)
The first time the owner pets their pet each Minecraft day (`Pets.petBuff`; **one pet buff at a time**, `PetBuffs`: a new
one takes the last one's place, only the pet's part, never your own effects; the overworld clock / 24000, so sleeping
moves it on; `PetData.pettedDay`), they get its buff. Everyday farm animals give small ones; harder to tame / rarer
animals better ones. The pet window's **Traits** tab shows the buff (tier, each effect's icon, level and time), whether
it's been petted today, and **what it eats**. **The full list of animals, buffs and foods is `PetAnimals.md`.**

## Guide (`/guides`, Pet Trainers only)
Workshop > Blocks (Small / Medium / Large Food Trough and Water Bowl) and Items (Ownership Tag, Training Stick, Animal
Bjorn), Information > Pets (1. Training, 2. Claiming, Looking After Pets, Troughs and Bowls, Petting, Pet Window, Safe
While Away, Adventures (for customers, with the sneak shortcuts), Carried Pets, Owned Animals), Perks (Pet Trainer Only;
No Carrying: the trainer's own limit) and Player Debuffs (others can't make tags, can't make or use the stick, can't
make troughs or bowls, can't make bjorns). No numbers in the guide.

## Testing
`pets/PetTests` (also `pet_training_feed`: one feed used, the animal follows; not a horse, not someone else's pet; the NPC sells 5 for 1 gold; axolotl / nautilus eat plain fish) (with `gradlew runGameTestServer`): claiming (a tag does nothing on an untrained animal, a trained pet is
nobody's yet, the tag claims it and is used up, nobody else can claim it, hurt it or lead it; the owner can; only the
owner can pet it or open its window), training (Pet Trainers only, needs 25 levels, costs 25 levels, horses / donkeys / mules can't be trained, llamas and camels can, "Pet Pig", nobody's
yet, never despawns, starts full, can't train twice, the stick breaks after 5), care (hunger
and thirst drop at the right rates, an owner who's offline: nothing changes, anyone can feed it its own food and water it, a starving pet is hurt but never dies,
a well-fed one heals), troughs and bowls (a bucket pours in 1000 mL and shows a third full, a pig won't eat wheat, it eats carrots only while
there's room, it drinks about 400 mL for 16 thirst, a nearly empty bowl gives what's left and shows empty, the sizes
have 1 / 2 / 5 slots and hold 1000 / 3000 / 10000 mL, a staying pet doesn't eat, the food heap shows and goes), actions (only the
owner can make it stay, Stay Still on and off, renaming: only the owner, trimmed, cut to 24 letters, empty = "Pet Pig";
petting; the first pet of the day gives a pig's Absorption, only once a day, again the next day, nobody else gets it;
every listed animal can be a pet, has a buff and has something to eat), the Animal Bjorn (`pet_bjorn`: the Back slot, a
pet climbs in and comes back out the same, 10 minutes in the bjorn makes it hungrier and thirstier, not someone else's pet, a Sniffer is too big, the sneak shortcuts with it on
your back, dying drops it), one buff at a time (`pet_one_buff`: a new pet buff takes the last one's place; your own
Strength potion, Speed drink and Night Vision stay with their own time; milk and running out forget it).

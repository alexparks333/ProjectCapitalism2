# Librarian Skill Books: every book, what it does, and the odds

Every skill book in the game (`Librarian.md` > Skill books has how the tables work; the code is `librarian/Skill`). Each
book has its own cover. **Keep this matching `Skill` when a book changes.**

## How a book is picked (two steps, plus the Blood Moon Books: see below)
1. **Lesson or trait?** A Rough Book is a trait **1%** of the time, a Delicate Book **12.5%** (it costs a
   Diamond). A Librarian with **Lucky Librarian** on doubles that: **2% / 25%**.
2. **Which one?** Within lessons, or within traits, by weight:
   **common 10, rare 4**.

**Knowledge Pieces multiply the trait chance too** (Librarian.md > Knowledge Pieces): Faded x1, Clear x1.5, Bright
x2, Radiant x3, and Lucky Librarian doubles that again (no cap). The tables below are for a Faded piece.

Each percentage below is both steps multiplied together: the real chance that one written book is that book. Each
column adds up to 100%.

## Lessons (timed) — 23 books (16 common, 7 rare), plus 1 instant book (common) drawn with them
Read it and you get an effect. **Rough Book: level I for 1 Minecraft day (20 minutes). Delicate Book: level II (the
numbers in brackets) for 2 days (40 minutes).** Gone on death, but milk and Totems don't clear them. Bookworm makes them last longer.

| Lesson                 | Tier   | Effect: level I (level II)                                               | Rough | Delicate | Rough + Lucky Librarian | Delicate + Lucky Librarian |
|------------------------|--------|--------------------------------------------------------------------------|-------|----------|-------------------------|----------------------------|
| Running Club           | common | Walk 10% faster (20%)                                                    | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Swimming Lessons       | common | Swim 33% faster (66%)                                                    | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Soft Landings          | common | 25% less fall damage (50%)                                               | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Holding Your Breath    | common | Air lasts about twice as long underwater (3x)                            | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| The Angler's Almanac   | common | +1 Luck: better fishing and loot (+2)                                    | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Butchery Basics        | common | 25% chance of extra meat from animals you kill (50%)                     | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Lumberjack's Log       | common | Chop logs 25% faster (50%)                                               | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Archery Drills         | common | Your arrows hit 15% harder (30%)                                         | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Horse Whispering       | common | Horses you ride run 15% faster (30%)                                     | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Quiet Steps            | common | Mobs notice you from 20% closer (40%)                                    | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Fisherman's Patience   | common | Fish bite faster, like Lure I (Lure II)                                  | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Stonemason's Guide     | common | Stone, deepslate and other rock break 25% faster (50%)                   | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Trail Rations          | common | Sprinting makes you 30% less hungry (60%)                                | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Beekeeper's Notes      | common | Bees never get angry at you (same)                                       | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Ice In The Pants       | common | You never get hot (same)                                                 | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Hot Pepper             | common | You never get cold (same)                                                | 5.00% | 4.42%    | 4.95%                   | 3.79%                      |
| Mining Monthly         | RARE   | Mine everything 15% faster (30%)                                         | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| Water Rationing        | RARE   | Thirst drains 25% slower (50%)                                           | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| First Aid Basics       | RARE   | Heal 25% faster on your own (50%)                                        | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| Lucky Librarian        | RARE   | Librarians only: books you write are twice as likely to be traits (same) | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| Merchant's Ledger      | RARE   | NPC shop prices 7.5% cheaper (15%)                                       | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| The Brawler's Handbook | RARE   | +1 attack damage (+2)                                                    | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |
| Reaching Further       | RARE   | Reach 1 block further when mining and building (2)                      | 2.00% | 1.77%    | 1.98%                   | 1.52%                      |

## Instant — 1 book (common, drawn with the lessons: same odds as a common lesson, 5.00% / 4.42% / 4.95% / 3.79%)
**A Fresh Start**: your body starts over. Diet back to the balanced head start, water full at the starting purity,
body heat 70% and dry (`WellnessEvents.freshStart`). Happens once, right away, when read; Rough and Delicate are the same.

## Traits (permanent) — 14 books (8 common, 6 rare)
Read it and it's yours until you die (dying forgets every trait). Each can be learned once (a book of a trait you already have
isn't used up). Trait books shimmer and have a gold name.

| Trait        | Tier   | Effect                                                     | Rough              | Delicate         | Rough + Lucky Librarian | Delicate + Lucky Librarian |
|--------------|--------|------------------------------------------------------------|--------------------|------------------|-------------------------|----------------------------|
| Iron Stomach | common | Raw food and Rotten Flesh never give you Hunger            | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Nimble       | common | Falls of up to 5 blocks don't hurt (normally 3)            | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Night Owl    | common | Phantoms leave you alone                                   | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Bookworm     | common | Lessons you read last 50% longer                           | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Steady Aim   | common | Bows and crossbows shoot dead straight (no spread)         | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Sure Footed  | common | Walk on Powder Snow without sinking, and 20% faster on ice | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Light Footed | common | Walk 5% faster                                             | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Deep Lungs   | common | Air lasts about 3 times as long underwater                 | 0.10% (1 in 1,040) | 1.20% (1 in 83)  | 0.19% (1 in 520)        | 2.40% (1 in 42)            |
| Born Miner   | RARE   | Mine everything 10% faster                                 | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |
| Lucky Star   | RARE   | +1 Luck: better fishing and loot                           | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |
| Fast Learner | RARE   | 25% more XP from XP orbs                                   | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |
| Camel        | RARE   | Thirst drains 25% slower, for good                         | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |
| Used To Hell | RARE   | Your body never goes above 70°: the heat can't touch you   | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |
| Winter Survival | RARE | The cold can't touch you: rain, snow and icy water never chill you | 0.04% (1 in 2,600) | 0.48% (1 in 208) | 0.08% (1 in 1,300)      | 0.96% (1 in 104)           |

## Blood Moon Books — 5 books (added 2026-11-19)
For the Blood Moon and its three bosses (`Events.md`). Read one and it lasts **one night** (11,000 ticks, about 9
minutes: read it at dusk and it lasts till dawn), **the same from a Rough or a Delicate Book** (no level II), not made
longer by Bookworm or Quick Study. Gone on death. A crimson spine on the cover, "Blood Moon Book: lasts one night" on the
tooltip and "Blood Moon" in red while reading. The effects are in `librarian/SkillEffects.onBloodMoonDamage` (arrows count
for whoever shot them).

| Book            | Effect                                              | Rough              | Delicate         | Rough + Lucky Librarian | Delicate + Lucky Librarian |
|-----------------|-----------------------------------------------------|--------------------|------------------|-------------------------|----------------------------|
| Revenant's Bane | Hit the Crimson Revenant 75% harder                 | 0.30% (1 in 337)   | 2.80% (1 in 36)  | 0.59% (1 in 170)        | 4.80% (1 in 21)            |
| Witch's Bane    | Hit the Sanguine Witch 75% harder                   | 0.30% (1 in 337)   | 2.80% (1 in 36)  | 0.59% (1 in 170)        | 4.80% (1 in 21)            |
| Wolfsbane       | Hit Gorefang 75% harder                             | 0.30% (1 in 337)   | 2.80% (1 in 36)  | 0.59% (1 in 170)        | 4.80% (1 in 21)            |
| Blood Fury      | Hit monsters 15% harder while a Blood Moon is up    | 0.30% (1 in 337)   | 2.80% (1 in 36)  | 0.59% (1 in 170)        | 4.80% (1 in 21)            |
| Blood Ward      | Take 10% less damage while a Blood Moon is up (a 5-heart hit takes 4.5) | 0.30% (1 in 337) | 2.80% (1 in 36) | 0.59% (1 in 170) | 4.80% (1 in 21) |

How they're picked: traits are rolled first (so their odds are exactly as before); a book that isn't a trait is then a
Blood Moon Book **1.5%** of the time from a Rough Book, **16%** from a Delicate Book (times the same multiplier as traits:
Knowledge Piece quality, Lucky Librarian), which comes to **1.49% / 14%** of all books (2.94% / 24% with Lucky
Librarian), a little less rare than traits; then one of the five, all alike. The lesson percentages above were worked out
before these were added: each lesson is now that much less likely (x0.985 Rough, x0.84 Delicate, x0.97 / x0.68 with
Lucky Librarian).

## In short
|                               | Rough            | Delicate        | Rough + Lucky Librarian | Delicate + Lucky Librarian |
|-------------------------------|------------------|-----------------|-------------------------|----------------------------|
| Any lesson                    | 97.5%            | 73.5%           | 95.1%                   | 51%                        |
| Any Blood Moon Book           | 1.49% (1 in 67)  | 14% (1 in 7)    | 2.94% (1 in 34)         | 24% (1 in 4)               |
| Any trait                     | 1% (1 in 100)    | 12.5% (1 in 8)  | 2% (1 in 50)            | 25% (1 in 4)               |
| Any rare trait                | 0.23% (1 in 433) | 2.88% (1 in 35) | 0.46% (1 in 217)        | 5.77% (1 in 17)            |

A Delicate Book is a little *less* likely to give any one lesson, because more of its chance goes to traits. Diamonds
buy trait chances, and stronger, longer lessons.

## Not picked
Iron Constitution (+1 heart, trait), Night Shift (lesson), Thick Skin and Green Thumb (traits).

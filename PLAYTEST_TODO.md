# Playtest TODO (2026-11-16)

Things noted while playing the test world. All done 2026-11-16 except the Getting Started text (item 9, the user writes it).

1. DONE **No bundles.** Taken out of all loot (`core/NoBundles` loot modifier); any bundle a player crafts, trades for or
   already has falls apart into Leather + String (contents come out). The vanilla recipe stays: turning it off with
   `neoforge:never` broke registry loading in 26.3.
2. DONE **Own food harder:** ordinary food fills 1/2; non-Farmers get 1 meat per animal.
3. DONE **Snowy biomes in the daytime pull body heat to 40** (nights / snowfall still -5).
4. DONE **Fast freezing (2x):** standing on any ice -> -5; snowy water -5 at night, 10 in the day, both 2x.
5. DONE **Sharpness / wear bar** moved up (just above the durability bar), a full pixel, black backing.
6. DONE **Wellbeing meter faster:** grace 3 min -> 1 min, lose the heart after 3 min of neglect (was 5), bonus heart
   after 5 min thriving (was 10).
7. DONE **Death keeps Water Purity** (`WellnessEvents.onClone`); full food, water, health on respawn; diet and the
   wellbeing meter (lost / bonus heart) already carried over.
8. DONE **Shovels and axes go dull** like pickaxes (half mining speed); the Toolsmith's whetstone is now the
   **Tool Whetstone** (pickaxes, shovels, axes, Butcher Knives). A dull axe only mines slower (damage unchanged).
9. DONE **Getting Started (Please Read)**: 8 pages stepped through with Back / Next (the user's text), first in the General Guide.
   Text lives in `GeneralGuideScreen.GETTING_STARTED`. Also: lava or open fire within 2 blocks heats you to 90.
10. DONE **Weaponsmith:** Bows Weaponsmith-only; Shields: plain iron for Weaponsmiths, Compacted Iron for others;
    Maces and Tridents socket at the Weaponsmith Table (sword gems); shields repaired at the Weaponsmith Table
    (planks); bows, crossbows and tridents repaired there with another of the same (what's left of it + 12%).
    The repair game uses each item's own picture (bow-shaped for bows); shields got a shield outline.
    (The +10% own-sword damage perk was added by mistake and removed.)
11. DONE **Wear limits at 75%:** sword edge (others 113-169, Weaponsmith 150-225, Grail 263), tools (iron 450),
    armor (iron 90). Swords already made keep the edge they rolled; tools and armor use the new limits right away.

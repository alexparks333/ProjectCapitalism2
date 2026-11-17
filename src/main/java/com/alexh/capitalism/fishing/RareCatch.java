package com.alexh.capitalism.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.function.Predicate;

// The rare catches (Fishing.md): once in a long while a cast brings up one of these instead of the usual fish
// (fishing/RareCatchModifier), each only where and when it lives. `chance` is one in that many casts (Luck raises it:
// chanceFor). Fish, the tentacle and the scale come up with a weight (`minLb` to `maxLb`), kept on the item with who
// caught it and when (CatchInfo); the heavier, the more the Fisherman pays (`gold` is what one of middling weight
// fetches: FishMarket). `plaque`: how many blocks wide its wall plaque is (0: it can't be mounted). `brag`: landing one
// is told to the whole server. The ids are the items' (capitalism:<id>): keep them.
public enum RareCatch {

    GIANT_CATFISH("giant_catfish", "Giant Catfish", 60, 30, 90, 1.5, 1, false,
        c -> (c.swamp() || c.river()) && !c.day()),
    GOLDEN_KOI("golden_koi", "Golden Koi", 80, 4, 12, 2, 1, false, c -> c.day()),
    MESSAGE_IN_A_BOTTLE("message_in_a_bottle", "Message in a Bottle", 100, 0, 0, 0, 0, false, c -> c.ocean()),
    PUFFERFISH_KING("pufferfish_king", "Pufferfish King", 120, 8, 20, 3, 1, false, c -> c.warmOcean()),
    BLOBFISH("blobfish", "Blobfish", 150, 3, 8, 3.5, 1, false, c -> c.deepOcean()),
    SWORDFISH("swordfish", "Swordfish", 200, 150, 600, 5, 2, false, c -> c.ocean() && c.day()),
    OCEAN_SUNFISH("ocean_sunfish", "Ocean Sunfish", 250, 500, 2000, 6, 2, false, c -> c.ocean() && c.day() && !c.raining()),
    SUNKEN_PIRATE_CHEST("sunken_pirate_chest", "Sunken Pirate Chest", 300, 0, 0, 0, 0, false, c -> c.ocean()),
    ELECTRIC_EEL("electric_eel", "Electric Eel", 350, 20, 45, 8, 1, false, c -> c.river() && c.thunder()),
    MAKO_SHARK("mako_shark", "Mako Shark", 500, 150, 400, 15, 2, true, c -> c.deepOcean()),
    ANGLERFISH("anglerfish", "Anglerfish", 600, 10, 40, 18, 1, true, c -> c.deepOcean() && !c.day()),
    GHOST_KOI("ghost_koi", "Ghost Koi", 800, 4, 12, 25, 1, true, c -> c.bloodMoon()),
    KRAKEN_TENTACLE("kraken_tentacle", "Kraken Tentacle", 1500, 80, 250, 40, 2, true, c -> c.deepOcean() && c.thunder()),
    OARFISH("oarfish", "Oarfish", 3000, 100, 600, 60, 3, true, c -> c.deepOcean() && !c.day()),
    GOLDEN_LEVIATHAN_SCALE("golden_leviathan_scale", "Golden Leviathan Scale", 10000, 2, 6, 150, 1, true, c -> true);

    public final String id;
    public final String title;
    public final int chance;
    public final float minLb, maxLb;
    // gold (9 nuggets each; halves allowed) for one of middling weight
    public final double gold;
    public final int plaque;
    public final boolean brag;
    private final Predicate<Where> lives;

    RareCatch(String id, String title, int chance, float minLb, float maxLb, double gold, int plaque, boolean brag, Predicate<Where> lives) {
        this.id = id;
        this.title = title;
        this.chance = chance;
        this.minLb = minLb;
        this.maxLb = maxLb;
        this.gold = gold;
        this.plaque = plaque;
        this.brag = brag;
        this.lives = lives;
    }

    public boolean weighed() {
        return this.maxLb > 0;
    }

    public boolean livesIn(Where where) {
        return this.lives.test(where);
    }

    // How much likelier than the listed chance a cast is (Fishing.md > Luck of the Sea). A rod WITHOUT Luck of the Sea
    // never lands a rare catch (0). Luck of the Sea I: the listed chance; II: x1.5; III: x2. Other Luck (skill books,
    // the Angler's Derby, the Luck effect) adds 15% a point on top, but only with Luck of the Sea; x3 at most.
    public static double multiplier(int luckOfTheSea, float otherLuck) {
        if (luckOfTheSea <= 0) return 0;
        double base = luckOfTheSea == 1 ? 1.0 : luckOfTheSea == 2 ? 1.5 : 2.0;
        return Math.min(3.0, base * (1.0 + 0.15 * Math.max(0.0F, otherLuck)));
    }

    // the chance of it on one cast (where it lives)
    public double chanceFor(int luckOfTheSea, float otherLuck) {
        return multiplier(luckOfTheSea, otherLuck) / this.chance;
    }

    // where and when it bites, for the Collection's hints
    public String hint() {
        return switch (this) {
            case GIANT_CATFISH -> "Swamps and rivers, at night";
            case GOLDEN_KOI -> "Anywhere, by day";
            case MESSAGE_IN_A_BOTTLE, SUNKEN_PIRATE_CHEST -> "Oceans";
            case PUFFERFISH_KING -> "Warm oceans";
            case BLOBFISH, MAKO_SHARK -> "Deep ocean";
            case SWORDFISH -> "Oceans, by day";
            case OCEAN_SUNFISH -> "Oceans, on a clear day";
            case ELECTRIC_EEL -> "Rivers, in a thunderstorm";
            case ANGLERFISH, OARFISH -> "Deep ocean, at night";
            case GHOST_KOI -> "Anywhere, only under a Blood Moon";
            case KRAKEN_TENTACLE -> "Deep ocean, in a thunderstorm";
            case GOLDEN_LEVIATHAN_SCALE -> "Anywhere, very rarely";
        };
    }

    // its tier (Fishing.md > Tiers)
    public CatchTier tier() {
        return switch (this) {
            case GIANT_CATFISH, GOLDEN_KOI, MESSAGE_IN_A_BOTTLE -> CatchTier.UNCOMMON;
            case PUFFERFISH_KING, BLOBFISH, SWORDFISH, OCEAN_SUNFISH, SUNKEN_PIRATE_CHEST -> CatchTier.RARE;
            case ELECTRIC_EEL, MAKO_SHARK, ANGLERFISH -> CatchTier.EPIC;
            case GHOST_KOI, KRAKEN_TENTACLE, OARFISH -> CatchTier.LEGENDARY;
            case GOLDEN_LEVIATHAN_SCALE -> CatchTier.MYTHIC;
        };
    }

    // "1 in 3,000" with Luck of the Sea at this level (no other Luck)
    public String oddsAt(int luckOfTheSea) {
        return String.format(java.util.Locale.ROOT, "1 in %,d", Math.round(1.0 / this.chanceFor(luckOfTheSea, 0)));
    }

    // the tooltips' chance lines: "Catch chance (Luck of the Sea):" / "I 1 in 3,000 · II 1 in 2,000 · III 1 in 1,500"
    public java.util.List<String> chanceLines() {
        return java.util.List.of("Catch chance (Luck of the Sea):",
            "I " + this.oddsAt(1) + " · II " + this.oddsAt(2) + " · III " + this.oddsAt(3));
    }

    public static RareCatch byId(String id) {
        for (RareCatch c : values()) if (c.id.equals(id)) return c;
        return null;
    }

    // Where and when a cast lands: its biome, the time of day, the weather, a Blood Moon
    public record Where(Holder<Biome> biome, boolean day, boolean raining, boolean thunder, boolean bloodMoon) {
        public static Where at(ServerLevel level, BlockPos pos) {
            long clock = com.alexh.capitalism.calendar.Calendar.clock(level);
            return new Where(level.getBiome(pos), com.alexh.capitalism.calendar.Calendar.isDay(clock), level.isRaining(), level.isThundering(),
                com.alexh.capitalism.event.BloodMoon.isActive());
        }

        public boolean ocean() {
            return this.biome.is(BiomeTags.IS_OCEAN);
        }

        public boolean deepOcean() {
            return this.biome.is(BiomeTags.IS_DEEP_OCEAN);
        }

        public boolean warmOcean() {
            return this.biome.is(Biomes.WARM_OCEAN) || this.biome.is(Biomes.LUKEWARM_OCEAN) || this.biome.is(Biomes.DEEP_LUKEWARM_OCEAN);
        }

        public boolean river() {
            return this.biome.is(BiomeTags.IS_RIVER);
        }

        public boolean swamp() {
            return this.biome.is(Biomes.SWAMP) || this.biome.is(Biomes.MANGROVE_SWAMP);
        }
    }
}

package com.alexh.capitalism.player;

import com.alexh.capitalism.core.CapitalismConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// A player's recent diet, like Eco: four food groups. Every meal adds points to its groups; only the last
// `dietWindowPoints` points are remembered, so the chart shows what you've been eating LATELY.
// Balance = how close the mix is to IDEAL (1.0 = exactly the ideal mix).
public class DietData {

    public static final int VEGETABLE = 0;
    public static final int FRUIT = 1;
    public static final int PROTEIN = 2;
    public static final int CARBS = 3;
    public static final int GROUPS = 4;
    public static final String[] NAMES = {"Vegetables", "Fruit", "Protein", "Carbs"};
    // The ideal (perfectly balanced) diet: 10% vegetables, 10% fruit, 65% protein, 15% carbs.
    public static final float[] IDEAL = {0.10f, 0.10f, 0.65f, 0.15f};

    public enum Tier { NOT_ENOUGH, POOR, OKAY, BALANCED }

    public static final MapCodec<DietData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.FLOAT.optionalFieldOf("vegetable", 0f).forGetter(d -> d.values[VEGETABLE]),
        Codec.FLOAT.optionalFieldOf("fruit", 0f).forGetter(d -> d.values[FRUIT]),
        Codec.FLOAT.optionalFieldOf("protein", 0f).forGetter(d -> d.values[PROTEIN]),
        Codec.FLOAT.optionalFieldOf("carbs", 0f).forGetter(d -> d.values[CARBS]),
        Codec.INT.optionalFieldOf("seed", 0).forGetter(d -> d.seed),
        Codec.INT.optionalFieldOf("meals", 0).forGetter(d -> d.meals)
    ).apply(inst, DietData::new));

    private final float[] values = new float[GROUPS];
    // Which head start the player has had (see startBalanced). Bumping SEED gives everyone a fresh one once.
    private static final int SEED = 2;
    private int seed;
    // Meals eaten that count toward the diet. Buffs/debuffs only start after enough of them (config),
    // so nobody gets the Balanced bonus just for having the head start.
    private int meals;

    public DietData() {
    }

    private DietData(float vegetable, float fruit, float protein, float carbs, int seed, int meals) {
        this.values[VEGETABLE] = vegetable;
        this.values[FRUIT] = fruit;
        this.values[PROTEIN] = protein;
        this.values[CARBS] = carbs;
        this.seed = seed;
        this.meals = meals;
    }

    public int meals() {
        return this.meals;
    }

    // Pretend a new player has been eating a balanced diet so far, so their first few meals don't swing the chart.
    // Returns true if it did anything (only once per player, ever).
    public boolean startBalanced(float totalPoints) {
        if (this.seed >= SEED) return false;
        this.seed = SEED;
        for (int i = 0; i < GROUPS; i++) {
            this.values[i] = totalPoints * IDEAL[i];
        }
        return true;
    }

    // A Fresh Start (skill book): back to the balanced head start a new player gets. Meals eaten still count.
    public void restart(float totalPoints) {
        for (int i = 0; i < GROUPS; i++) {
            this.values[i] = totalPoints * IDEAL[i];
        }
    }

    // Adds a meal: `points` (the food's hunger value) split across the groups by `shares` (they add up to 1).
    // The diet only remembers the last `window` points: past that, everything shrinks to make room, so one snack
    // can only nudge the chart (a 2-point melon slice is ~1.5% of a 130-point window).
    public void eat(float[] shares, float points, float window) {
        for (int i = 0; i < GROUPS; i++) {
            this.values[i] += shares[i] * points;
        }
        float total = total(this.values);
        if (window > 0 && total > window) {
            for (int i = 0; i < GROUPS; i++) {
                this.values[i] *= window / total;
            }
        }
        if (this.meals < 100000) this.meals++;
    }

    // Everything fades a little (called once a minute).
    public void decay(float keep) {
        for (int i = 0; i < GROUPS; i++) {
            this.values[i] *= keep;
            if (this.values[i] < 0.01f) this.values[i] = 0f;
        }
    }

    public float[] values() {
        return this.values.clone();
    }

    // ── Shared math (server for effects, client for the screen) ────────────

    public static float total(float[] values) {
        float total = 0;
        for (float v : values) total += v;
        return total;
    }

    // 1.0 when the groups match IDEAL exactly, 0.0 at the worst possible mix (everything in the smallest group).
    public static float balance(float[] values) {
        float total = total(values);
        if (total <= 0) return 0;
        float deviation = 0;
        float smallest = 1f;
        for (int i = 0; i < GROUPS; i++) {
            deviation += Math.abs(values[i] / total - IDEAL[i]);
            smallest = Math.min(smallest, IDEAL[i]);
        }
        float worst = 2f * (1f - smallest);
        return Math.max(0f, 1f - deviation / worst);
    }

    public static Tier tier(float[] values) {
        double minPoints = 12;
        double good = 0.86;
        double poor = 0.44;
        try {
            minPoints = CapitalismConfig.DIET_MIN_POINTS.get();
            good = CapitalismConfig.DIET_BALANCED_SCORE.get();
            poor = CapitalismConfig.DIET_POOR_SCORE.get();
        } catch (IllegalStateException notLoaded) {
            // defaults above
        }
        if (total(values) < minPoints) return Tier.NOT_ENOUGH;
        // a whole food group missing (0% on the chart) is a Poor diet, whatever the balance says (it also drags the
        // wellbeing meter toward a lost heart: WellnessEvents.neglect)
        for (float v : values) {
            if (Math.round(v / total(values) * 100) == 0) return Tier.POOR;
        }
        // Compare whole percentages, like the screen shows them (Poor 0-44%, Okay 45-85%, Balanced 86%+).
        long pct = Math.round(balance(values) * 100);
        if (pct >= Math.round(good * 100)) return Tier.BALANCED;
        if (pct <= Math.round(poor * 100)) return Tier.POOR;
        return Tier.OKAY;
    }
}

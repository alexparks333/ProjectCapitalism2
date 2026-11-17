package com.alexh.capitalism.player;

import com.alexh.capitalism.core.CapitalismConfig;

// Water Purity: one meter from 0 to 100 that shows how clean the water you've been drinking lately is.
// Every drink pulls the meter toward that water's target, bigger drinks pull harder (config purityShift):
//   - Raw water (sips from lakes/rivers, plain water bottles) pulls toward 0.
//   - Purified Water pulls toward the top of Good, so it can keep you at Good but never reach Excellent.
//   - Excellent water (Spring Water: Bartenders' Advanced Osmosis Machine) pulls toward 100,
//     three times as hard (two or three bottles take you from Good to Excellent).
// Everyone starts at Good (config purityStart). Low purity costs a water drop.
public final class WaterPurity {

    public enum Level { LOW, MEDIUM, GOOD, EXCELLENT }

    public enum Source {
        RAW(0f), PURIFIED(84f), EXCELLENT(100f);

        public final float target;

        Source(float target) {
            this.target = target;
        }
    }

    public static final String[] NAMES = {"Low", "Medium", "Good", "Excellent"};
    // Where each band starts (Low 0-24, Medium 25-54, Good 55-89, Excellent 90-100).
    public static final int MEDIUM_AT = 25;
    public static final int GOOD_AT = 55;
    public static final int EXCELLENT_AT = 90;
    public static final float EXCELLENT_PULL = 3f;

    private WaterPurity() {
    }

    public static Level level(float purity) {
        int pct = Math.round(purity);
        if (pct >= EXCELLENT_AT) return Level.EXCELLENT;
        if (pct >= GOOD_AT) return Level.GOOD;
        if (pct >= MEDIUM_AT) return Level.MEDIUM;
        return Level.LOW;
    }

    // `thirstPoints` = how much thirst the drink restores (a full bar is 20).
    public static float afterDrink(float purity, float thirstPoints, Source source) {
        float pull = thirstPoints / 20f * (float) CapitalismConfig.PURITY_SHIFT.get().doubleValue();
        // Spring Water pulls hard: two or three bottles take you from Good to Excellent
        if (source == Source.EXCELLENT) pull *= EXCELLENT_PULL;
        pull = Math.min(1f, pull);
        return Math.max(0f, Math.min(100f, purity + (source.target - purity) * pull));
    }
}

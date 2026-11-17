package com.alexh.capitalism.player;

// Client-side copy of the player's diet and wellbeing, for the Wellness screen. The server owns the real values.
public class ClientWellnessData {
    public static float[] diet = new float[DietData.GROUPS];
    public static int meals;
    public static int mealsNeeded = 20;

    // Wellbeing, all in seconds (see WellbeingData)
    public static int wellbeing;
    public static int wellbeingBuffAt = 600;
    public static int wellbeingMax = 900;
    public static int wellbeingDebuffAt = -300;
    public static int wellbeingMin = -600;
    // what is dragging the wellbeing meter down (WellnessEvents.N_*)
    public static int neglect;
    // Body heat 0-100 and what's acting on it (BodyTemperature), for the temperature ball
    public static float bodyHeat = BodyTemperature.NORMAL;
    public static int heatReasons;

    public static int temperatureZone() {
        return BodyTemperature.zone(bodyHeat);
    }

    // Grace period (seconds) and, per WellbeingData.DIET / PURITY / HEART: state right now, state in effect,
    // seconds they've been different.
    public static int grace = 180;
    public static int[] raw = {0, WaterPurity.Level.GOOD.ordinal(), WellbeingData.Effect.NONE.ordinal()};
    public static int[] effective = {0, WaterPurity.Level.GOOD.ordinal(), WellbeingData.Effect.NONE.ordinal()};
    public static int[] pending = new int[3];
    // Current (fading) max health bonus
    public static float healthMod;

    public static boolean effectsActive() {
        return meals >= mealsNeeded;
    }

    public static DietData.Tier dietTier() {
        return DietData.Tier.values()[Math.min(effective[WellbeingData.DIET], DietData.Tier.values().length - 1)];
    }

    public static WaterPurity.Level purityLevel() {
        return WaterPurity.Level.values()[Math.min(effective[WellbeingData.PURITY], 3)];
    }

    public static WellbeingData.Effect heart() {
        return WellbeingData.Effect.values()[Math.min(effective[WellbeingData.HEART], 3)];
    }

    // Seconds until a pending change happens, or -1 if nothing is changing.
    public static int changeIn(int state) {
        return raw[state] != effective[state] ? Math.max(0, grace - pending[state]) : -1;
    }
}

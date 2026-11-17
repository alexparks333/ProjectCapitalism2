package com.alexh.capitalism.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

// Everything that decides the Wellness bonuses, updated once a second by WellnessEvents.
//
// Wellbeing score: seconds of "being well", from -(debuff + grace) to +(buff + grace) minutes. Starts at 0 (neutral).
//  - Neglect pulls it down: -1 a second for each of (a food group at 0% or Low water) and (body heat under 10 or
//    over 95). Otherwise thriving (Balanced diet + Good or better water) pushes it up +1, or it drifts back to 0.
//  - At +buff minutes or more: +1 heart (+2 with Excellent water). At -debuff minutes or less: -1 heart.
//
// Grace period: diet (food), purity (water) and wellbeing (hearts) only switch after the new state has held for the
// whole grace period (config wellnessGraceSeconds). Flip back in time and nothing changes.
//
// Fading: when a bonus is taken away, the extra heart / drumstick / drop drains away over a few seconds instead of
// vanishing (and without the "you got hit" shake). Gaining happens right away (after the grace period).
//
// Body heat (BodyTemperature) lives here too.
public class WellbeingData {

    public static final int DIET = 0;
    public static final int PURITY = 1;
    public static final int HEART = 2;
    private static final int STATES = 3;

    public static final MapCodec<WellbeingData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT.optionalFieldOf("score", 0).forGetter(d -> d.score),
        Codec.INT.listOf().optionalFieldOf("effective", List.of()).forGetter(d -> toList(d.effective)),
        Codec.INT.listOf().optionalFieldOf("pending", List.of()).forGetter(d -> toList(d.pending)),
        Codec.FLOAT.optionalFieldOf("heat", BodyTemperature.NORMAL).forGetter(d -> d.bodyHeat),
        Codec.BOOL.optionalFieldOf("wet", false).forGetter(d -> d.wet)
    ).apply(inst, WellbeingData::new));

    // PENALTY = -1 heart (neglect, see tickScore).
    // BONUS = +1 heart (Good water), BIG_BONUS = +2 hearts (Excellent water). New values go at the end (saved as numbers).
    public enum Effect { PENALTY, NONE, BONUS, BIG_BONUS }

    private int score;
    // Per state (DIET = DietData.Tier ordinal, PURITY = WaterPurity.Level ordinal, HEART = Effect ordinal):
    // what's in effect, what it would be right now, and for how many seconds those have been different.
    // effective -1 = not set yet (takes the raw value straight away, so nobody waits after an update).
    private final int[] effective = {-1, -1, -1};
    private final int[] raw = {-1, -1, -1};
    private final int[] pending = new int[STATES];

    // Current (fading) amounts. Not saved: after a relog they snap to the target again.
    private float healthMod;
    private float foodCap = 20;
    private float thirstCap = 20;

    // Body heat, 0-100 (BodyTemperature): saved. What's acting on it right now (BodyTemperature.R_*) and seconds since
    // the last frost damage: not saved.
    public float bodyHeat = BodyTemperature.NORMAL;
    public int heatReasons;
    public int frostSeconds;
    // Soaked leather (BodyTemperature: like still being in the water until it dries by a fire): saved. Seconds spent
    // drying by a fire so far: not saved.
    public boolean wet;
    public int drySeconds;

    public WellbeingData() {
    }

    private WellbeingData(int score, List<Integer> effective, List<Integer> pending, float heat, boolean wet) {
        this.wet = wet;
        this.score = score;
        this.bodyHeat = heat;
        for (int i = 0; i < STATES; i++) {
            if (i < effective.size()) this.effective[i] = effective.get(i);
            if (i < pending.size()) this.pending[i] = pending.get(i);
        }
    }

    private static List<Integer> toList(int[] values) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int v : values) list.add(v);
        return list;
    }

    public int score() {
        return this.score;
    }

    // One second passes: `drag` (how many kinds of neglect, 0-2) pulls it down; otherwise thriving (Balanced diet and
    // Good or better water) pushes it up, or it drifts back to 0.
    public void tickScore(boolean thriving, int drag, int min, int max) {
        if (drag > 0) this.score -= drag;
        else if (thriving) this.score += 1;
        else if (this.score > 0) this.score -= 1;
        else if (this.score < 0) this.score += 1;
        this.score = Math.max(min, Math.min(max, this.score));
    }

    // `level` = WellnessEvents.thrivingLevel: 2 (Excellent water) makes the bonus two hearts. `debuffAt` is negative.
    public static Effect effect(int score, int buffAt, int debuffAt, int level) {
        if (score <= debuffAt) return Effect.PENALTY;
        if (score >= buffAt) return level >= 2 ? Effect.BIG_BONUS : Effect.BONUS;
        return Effect.NONE;
    }

    // One second passes for a state whose value right now is `now`. It only takes effect after `grace` seconds in a row.
    public void step(int state, int now, int grace) {
        this.raw[state] = now;
        if (this.effective[state] < 0 || now == this.effective[state]) {
            this.effective[state] = now;
            this.pending[state] = 0;
        } else if (++this.pending[state] >= grace) {
            this.effective[state] = now;
            this.pending[state] = 0;
        }
    }

    public int effective(int state) {
        return Math.max(0, this.effective[state]);
    }

    public int raw(int state) {
        return Math.max(0, this.raw[state]);
    }

    public int pending(int state) {
        return this.pending[state];
    }

    // Moves the fading amounts one second toward their targets: up instantly, down by `fadePerSecond`.
    public void fade(float healthTarget, float foodTarget, float thirstTarget, float healthPerSecond, float barPerSecond) {
        this.healthMod = approach(this.healthMod, healthTarget, healthPerSecond);
        this.foodCap = approach(this.foodCap, foodTarget, barPerSecond);
        this.thirstCap = approach(this.thirstCap, thirstTarget, barPerSecond);
    }

    private static float approach(float current, float target, float down) {
        return target >= current ? target : Math.max(target, current - down);
    }

    public float healthMod() {
        return this.healthMod;
    }

    public int foodCap() {
        return (int) Math.ceil(this.foodCap);
    }

    public int thirstCap() {
        return (int) Math.ceil(this.thirstCap);
    }
}

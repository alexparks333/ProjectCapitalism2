package com.alexh.capitalism.player;

import com.alexh.capitalism.core.CapitalismConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.food.FoodData;

// Per-player thirst, plus the golden bars from Chef/Bartender food and drinks.
// Golden bars sit on top of the normal ones and drain first, at exactly the normal rate.
public class ThirstData {

    public static final int MAX = 20;

    public static final MapCodec<ThirstData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.FLOAT.fieldOf("thirst").forGetter(d -> d.thirstF),
        Codec.FLOAT.optionalFieldOf("hydration", 0f).forGetter(d -> d.hydration),
        Codec.INT.fieldOf("damageTimer").forGetter(d -> d.damageTimer),
        Codec.FLOAT.optionalFieldOf("goldThirst", 0f).forGetter(d -> d.goldThirst),
        Codec.INT.optionalFieldOf("goldFood", 0).forGetter(d -> d.goldFood),
        Codec.FLOAT.optionalFieldOf("purity", -1f).forGetter(d -> d.purity)
    ).apply(inst, ThirstData::new));

    private float thirstF;
    // Hidden buffer like hunger's saturation: filled by drinking normal water past full, drained before thirst.
    private float hydration;
    private int damageTimer;
    // Golden water drops (0..20), drained before everything else.
    private float goldThirst;
    // Golden drumsticks (0..20), used up before the normal food bar goes down.
    private int goldFood;
    // Water Purity 0..100 (see WaterPurity). -1 = not set yet; ModServerEvents sets it to the config start value.
    private float purity;
    // Highest the thirst / food bars may go right now: 20, or 22 with Excellent purity / a Balanced diet, or 18 with
    // Low purity / a Poor diet. Set every tick. Not saved.
    public int thirstCap = MAX;
    public int foodCap = MAX;
    // Food bar when the player started eating (-1 = not eating). Not saved.
    public int foodBeforeEating = -1;
    // Saturation when the player started eating. Not saved.
    public float saturationBeforeEating;
    // Gold rush: ticks left of the quick heal from a gold-only meal (goldOnlyMeal). Not saved.
    public int goldRushTicks;
    public static final int GOLD_RUSH_TICKS = 30;
    public static final float GOLD_RUSH_SATURATION = 6.0F;
    // Game time of the last meal eaten off a plate (a short cooldown between servings). Not saved.
    public long lastPlateMeal;
    // Game time of the last Ender Leap (a short cooldown between leaps). Not saved.
    public long lastEnderLeap = Long.MIN_VALUE / 2;
    // player.tickCount of the last sip from a water source (WaterDrinking). Not saved.
    public int lastSipTick = Integer.MIN_VALUE / 2;

    public ThirstData() {
        this(20f, 0f, 0, 0f, 0, -1f);
    }

    private ThirstData(float thirstF, float hydration, int damageTimer, float goldThirst, int goldFood, float purity) {
        this.purity = purity;
        this.thirstF = thirstF;
        this.hydration = hydration;
        this.damageTimer = damageTimer;
        this.goldThirst = goldThirst;
        this.goldFood = goldFood;
    }

    // Gold drops go first, then the hidden hydration buffer, then the normal bar.
    public void drain(float amount) {
        if (goldThirst > 0f) {
            float fromGold = Math.min(goldThirst, amount);
            goldThirst -= fromGold;
            amount -= fromGold;
        }
        if (hydration > 0f) {
            float fromBuffer = Math.min(hydration, amount);
            hydration -= fromBuffer;
            amount -= fromBuffer;
        }
        thirstF = Math.max(0f, thirstF - amount);
    }

    // Sweating (Hot, player/BodyTemperature): gold drops still go first, but then the bar drops straight away; the hidden
    // hydration buffer doesn't hold it back (it sweats away at the same rate, alongside).
    public void sweat(float amount) {
        if (goldThirst > 0f) {
            float fromGold = Math.min(goldThirst, amount);
            goldThirst -= fromGold;
            amount -= fromGold;
        }
        hydration = Math.max(0f, hydration - amount);
        thirstF = Math.max(0f, thirstF - amount);
    }

    // Normal water: fills the bar, a little extra goes into the hidden hydration buffer.
    public void restore(float amount) {
        float total = thirstF + amount;
        thirstF = Math.min(thirstCap, Math.max(thirstF, total));
        // Only drinking past a truly full bar hydrates (a bar lowered by Low purity doesn't count; a bar raised by
        // Excellent purity has to fill its bonus drop first).
        float overflow = total - Math.max(MAX, thirstCap);
        if (overflow > 0f) {
            float maxHydration = (float) CapitalismConfig.THIRST_MAX_HYDRATION.get().doubleValue();
            hydration = Math.min(maxHydration, hydration + overflow);
        }
    }

    // Bartender drinks: fill the normal bar first, everything past full becomes gold drops (up to a full bar).
    public void restoreWithGold(float amount) {
        float total = thirstF + amount;
        thirstF = Math.min(thirstCap, Math.max(thirstF, total));
        float overflow = total - thirstF;
        if (overflow > 0f) {
            goldThirst = Math.min(goldThirstMax(), goldThirst + overflow);
        }
    }

    // Chef food: fill the normal food bar first, everything past full becomes gold drumsticks (up to a full bar).
    public void feedWithGold(FoodData food, int amount) {
        int total = food.getFoodLevel() + amount;
        int normal = Math.min(foodCap, total);
        food.setFoodLevel(normal);
        goldFood = Math.min(goldFoodMax(), goldFood + (total - normal));
    }

    // Called every tick: whenever vanilla hunger takes food points, pay them back out of the gold first.
    // Gold always goes first: while there are gold drumsticks, they keep the normal bar topped up to its current top
    // (20, 22 with a Balanced diet, 18 with a Poor one), so the normal drumsticks only start going down once the gold
    // is all gone. That covers vanilla hunger taking a point AND the bar getting bigger (the bonus drumstick).
    public void protectFoodWithGold(FoodData food) {
        int current = food.getFoodLevel();
        if (goldFood > goldFoodMax()) goldFood = goldFoodMax();
        if (goldFood > 0 && current < foodCap) {
            int topUp = Math.min(foodCap - current, goldFood);
            goldFood -= topUp;
            current += topUp;
            food.setFoodLevel(current);
        }
        // A Poor diet lowers the top of the food bar. Done here so the gold refund above doesn't fight it.
        if (current > foodCap) {
            current = foodCap;
            food.setFoodLevel(current);
        }
    }

    // Gourmet food eaten with the bar already full (it only made gold): vanilla's eating gave it saturation, which would
    // mean a long fast heal. Saturation only comes from filling the normal bar (like vanilla), so take that back, and
    // give a GOLD RUSH instead: 1.5 seconds of fast healing (vanilla heals fast with a full bar + saturation). Doesn't
    // stack: with saturation left from a normal meal, nothing changes. Cook.md.
    public void goldOnlyMeal(FoodData food, float saturationBefore) {
        food.setSaturation(saturationBefore);
        if (saturationBefore > 0f) return;
        food.setSaturation(GOLD_RUSH_SATURATION);
        goldRushTicks = GOLD_RUSH_TICKS;
    }

    // Every tick: when the gold rush runs out, its leftover saturation goes (not saturation from a meal since).
    public void tickGoldRush(FoodData food) {
        if (goldRushTicks <= 0 || --goldRushTicks > 0) return;
        if (food.getSaturationLevel() <= GOLD_RUSH_SATURATION) food.setSaturation(0f);
    }

    // After eating: vanilla stopped the food bar at 20, so put what didn't fit into the bonus drumstick (if any).
    public void fillBonusFood(FoodData food, int wanted) {
        if (foodCap <= MAX || wanted <= MAX || food.getFoodLevel() < MAX) return;
        int level = Math.min(foodCap, wanted);
        food.setFoodLevel(level);
    }

    // The gold bars are exactly as long as the normal bars: 10 icons, 11 with the bonus drumstick / drop, 9 when one is
    // lost (a Poor diet / Low water): gold never fills a lost slot.
    public int goldFoodMax() {
        return foodCap;
    }

    public float goldThirstMax() {
        return thirstCap;
    }

    // A full night's sleep in the Four-Poster Crimson Bed (decor/BloodDecorEvents): the water bar full, and every golden
    // drop too.
    public void fillCompletely() {
        thirstF = thirstCap;
        goldThirst = goldThirstMax();
    }

    // A Fresh Start (skill book): the water bar full and purity back to where everyone starts
    public void freshStart(float startPurity) {
        thirstF = Math.max(thirstF, thirstCap);
        damageTimer = 0;
        setPurity(startPurity);
    }

    // A craving satisfied (craving/Cravings): the food bar full, and every golden drumstick too, with saturation to match
    public void fillFoodCompletely(FoodData food) {
        food.setFoodLevel(foodCap);
        food.setSaturation(Math.max(food.getSaturationLevel(), Math.min(foodCap, MAX)));
        goldFood = goldFoodMax();
    }

    public int goldFood() {
        return goldFood;
    }

    public float goldThirst() {
        return goldThirst;
    }

    // Gourmet food: extra golden drumsticks (up to a full bar).
    public void addGoldFood(int points) {
        if (points > 0) this.goldFood = Math.min(goldFoodMax(), this.goldFood + points);
    }

    // Low purity lowers the top of the thirst bar; gold drops keep a short bar topped up (e.g. the bonus drop), so
    // normal drops only go down once the gold is gone.
    public void applyThirstCap() {
        if (thirstF > thirstCap) thirstF = thirstCap;
        if (goldThirst > goldThirstMax()) goldThirst = goldThirstMax();
        if (goldThirst > 0 && thirstF < thirstCap) {
            float topUp = Math.min(thirstCap - thirstF, goldThirst);
            goldThirst -= topUp;
            thirstF += topUp;
        }
    }

    public float getPurity() {
        return purity;
    }

    public void setPurity(float purity) {
        this.purity = Math.max(0f, Math.min(100f, purity));
    }

    // Round up so a bar at 19.99 still shows full instead of instantly dropping half a drop.
    public int getThirstInt() {
        return (int) Math.ceil(thirstF);
    }

    public int getGoldThirstInt() {
        return (int) Math.ceil(goldThirst);
    }

    public int getGoldFood() {
        return goldFood;
    }

    public float getThirstF() {
        return thirstF;
    }

    public float getHydration() {
        return hydration;
    }

    public void incrementDamageTimer() {
        damageTimer++;
    }

    public void resetDamageTimer() {
        damageTimer = 0;
    }

    public int getDamageTimer() {
        return damageTimer;
    }
}

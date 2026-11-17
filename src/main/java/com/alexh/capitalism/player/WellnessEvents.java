package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.network.DietSyncPacket;
import com.alexh.capitalism.network.QuietHealthPacket;
import com.alexh.capitalism.network.WellbeingSyncPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Wellness, step 1: the diet.
//  - Eating adds to the four food groups (DietValues says which).
//  - Once a minute every group fades a little, so only recent meals matter.
//  - Everyone starts with a balanced head start (config dietStartPoints, split into the ideal mix) the first time they join.
//  - Buffs/debuffs only start once they have eaten dietMealsBeforeEffects meals, so the head start alone gives nothing.
//  - Diet changes the food bar (Balanced +1 drumstick, Poor -1; done in ModServerEvents).
//  - Wellbeing: thriving (Balanced diet + Good water) earns +1 heart (+2 with Excellent water) (WellbeingData).
//  - Body heat (BodyTemperature) is moved here once a second too.
@EventBusSubscriber(modid = Capitalism.MODID)
public class WellnessEvents {

    // Old diet heart (diet now changes the food bar instead). Removed from anyone who still has it.
    private static final Identifier OLD_DIET_HEALTH = Identifier.fromNamespaceAndPath(Capitalism.MODID, "diet_health");
    private static final Identifier WELLBEING_HEALTH = Identifier.fromNamespaceAndPath(Capitalism.MODID, "wellbeing_health");

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        addToDiet(player, event.getItem());
        // any alcohol warms you: 2 minutes of Warmed Up (the cold can't reach you, BodyTemperature)
        if (com.alexh.capitalism.core.ModItems.isAlcoholic(event.getItem())) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.alexh.capitalism.core.ModEffects.WARMED_UP, ALCOHOL_WARMTH_TICKS));
        }
    }

    // A meal eaten some other way (off a Cook's plate): counts toward the diet.
    public static void recordMeal(ServerPlayer player, ItemStack stack) {
        addToDiet(player, stack);
    }

    private static void addToDiet(ServerPlayer player, ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        float[] shares = DietValues.sharesFor(stack);
        if (food == null || shares == null) return;

        DietData diet = player.getData(ModAttachments.DIET.get());
        diet.eat(shares, food.nutrition(), (float) CapitalismConfig.DIET_WINDOW_POINTS.get().doubleValue());
        sync(player, diet);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.tickCount % 1200 == 0) {
            DietData diet = player.getData(ModAttachments.DIET.get());
            diet.decay((float) CapitalismConfig.DIET_KEEP_PER_MINUTE.get().doubleValue());
            sync(player, diet);
        }
        if (player.tickCount % 20 == 0) {
            tickWellbeing(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DietData diet = player.getData(ModAttachments.DIET.get());
            if (diet.startBalanced((float) CapitalismConfig.DIET_START_POINTS.get().doubleValue())) {
                player.setData(ModAttachments.DIET.get(), diet);
            }
            sync(player, diet);
            applyHealthMod(player, player.getData(ModAttachments.WELLBEING.get()).healthMod());
        }
    }

    // The diet tier that counts right now: NOT_ENOUGH until the player has eaten enough meals.
    public static DietData.Tier activeTier(ServerPlayer player) {
        DietData diet = player.getData(ModAttachments.DIET.get());
        return diet.meals() >= CapitalismConfig.DIET_MEALS_BEFORE_EFFECTS.get() ? DietData.tier(diet.values()) : DietData.Tier.NOT_ENOUGH;
    }

    // ── Wellbeing (once a second) ──────────────────────────────────────────

    // How well the player is doing for the bonus hearts (all in effect, after the grace period):
    //  2 = Balanced diet + Excellent water (+2 hearts)
    //  1 = Balanced diet + Good water (+1 heart)
    //  0 = anything less
    public static int thrivingLevel(WellbeingData wellbeing) {
        if (wellbeing.effective(WellbeingData.DIET) != DietData.Tier.BALANCED.ordinal()) return 0;
        int water = wellbeing.effective(WellbeingData.PURITY);
        if (water == WaterPurity.Level.EXCELLENT.ordinal()) return 2;
        return water == WaterPurity.Level.GOOD.ordinal() ? 1 : 0;
    }

    // A Fresh Start (skill book): the whole body starts over. Diet back to the balanced head start, water full at the
    // starting purity, body heat 70% and dry.
    public static void freshStart(ServerPlayer player) {
        DietData diet = player.getData(ModAttachments.DIET.get());
        diet.restart((float) CapitalismConfig.DIET_START_POINTS.get().doubleValue());
        sync(player, diet);
        ThirstData thirst = player.getData(ModAttachments.THIRST.get());
        thirst.freshStart((float) CapitalismConfig.PURITY_START.get().doubleValue());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, com.alexh.capitalism.network.ThirstSyncPacket.of(thirst, player));
        BodyTemperature.reset(player.getData(ModAttachments.WELLBEING.get()));
    }

    // Dying is no way out of bad water: Water Purity carries over (the water bar itself comes back full, like food and
    // health; the diet and the wellbeing meter, lost or bonus heart included, carry over through copyOnDeath).
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        float purity = event.getOriginal().getData(ModAttachments.THIRST.get()).getPurity();
        if (purity >= 0f) event.getEntity().getData(ModAttachments.THIRST.get()).setPurity(purity);
    }

    // Dying (or coming back from the End) resets your body heat to just right.
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WellbeingData wellbeing = player.getData(ModAttachments.WELLBEING.get());
            BodyTemperature.reset(wellbeing);
        }
    }

    // Neglect: what's dragging the wellbeing meter down toward the lost heart (a bitmask, synced for the screen)
    public static final int N_FOOD_GROUP = 1, N_LOW_WATER = 2, N_FREEZING = 4, N_BURNING = 8;
    // a drink of any alcohol: Warmed Up for 2 minutes
    public static final int ALCOHOL_WARMTH_TICKS = 2 * 60 * 20;
    public static final float FREEZING_NEGLECT_BELOW = 10f, BURNING_NEGLECT_ABOVE = 95f;

    public static int neglect(ServerPlayer player, WellbeingData wellbeing) {
        int n = 0;
        // a food group at 0% (only once the diet counts: enough meals and points)
        DietData diet = player.getData(ModAttachments.DIET.get());
        float[] values = diet.values();
        float total = DietData.total(values);
        if (diet.meals() >= CapitalismConfig.DIET_MEALS_BEFORE_EFFECTS.get() && total >= CapitalismConfig.DIET_MIN_POINTS.get()) {
            for (float v : values) {
                if (Math.round(v / total * 100) == 0) n |= N_FOOD_GROUP;
            }
        }
        // water below Medium
        float purity = player.getData(ModAttachments.THIRST.get()).getPurity();
        if (purity >= 0f && WaterPurity.level(purity) == WaterPurity.Level.LOW) n |= N_LOW_WATER;
        if (wellbeing.bodyHeat < FREEZING_NEGLECT_BELOW) n |= N_FREEZING;
        if (wellbeing.bodyHeat > BURNING_NEGLECT_ABOVE) n |= N_BURNING;
        return n;
    }

    private static void tickWellbeing(ServerPlayer player) {
        int buffAt = CapitalismConfig.WELLBEING_BUFF_MINUTES.get() * 60;
        int max = buffAt + CapitalismConfig.WELLBEING_GRACE_MINUTES.get() * 60;
        int grace = CapitalismConfig.WELLNESS_GRACE_SECONDS.get();
        WellbeingData wellbeing = player.getData(ModAttachments.WELLBEING.get());

        // Body heat (BodyTemperature): slides toward what the world is doing to you; Freezing costs health
        BodyTemperature.tick(player, wellbeing);

        // Wellbeing score: neglect drags it down (toward the lost heart), thriving pushes it up, otherwise it drifts
        // back to the middle
        int debuffAt = -CapitalismConfig.WELLBEING_DEBUFF_MINUTES.get() * 60;
        int min = debuffAt - CapitalismConfig.WELLBEING_GRACE_MINUTES.get() * 60;
        int level = thrivingLevel(wellbeing);
        int neglect = neglect(player, wellbeing);
        int drag = ((neglect & (N_FOOD_GROUP | N_LOW_WATER)) != 0 ? 1 : 0) + ((neglect & (N_FREEZING | N_BURNING)) != 0 ? 1 : 0);
        wellbeing.tickScore(level > 0, drag, min, max);

        // Diet, purity and hearts only switch after holding for the grace period
        float purity = player.getData(ModAttachments.THIRST.get()).getPurity();
        wellbeing.step(WellbeingData.DIET, activeTier(player).ordinal(), grace);
        wellbeing.step(WellbeingData.PURITY, (purity < 0f ? WaterPurity.Level.GOOD : WaterPurity.level(purity)).ordinal(), grace);
        wellbeing.step(WellbeingData.HEART, WellbeingData.effect(wellbeing.score(), buffAt, debuffAt, level).ordinal(), grace);

        // What's in effect -> targets; removed bonuses fade away
        DietData.Tier diet = DietData.Tier.values()[wellbeing.effective(WellbeingData.DIET)];
        WaterPurity.Level water = WaterPurity.Level.values()[wellbeing.effective(WellbeingData.PURITY)];
        WellbeingData.Effect heart = WellbeingData.Effect.values()[wellbeing.effective(WellbeingData.HEART)];
        float hearts = (float) CapitalismConfig.WELLBEING_HEALTH.get().doubleValue();
        float healthTarget = switch (heart) {
            case BIG_BONUS -> hearts * 2;
            case BONUS -> hearts;
            case PENALTY -> -hearts;
            case NONE -> 0f;
        };
        float foodTarget = ThirstData.MAX + (diet == DietData.Tier.BALANCED ? CapitalismConfig.DIET_BALANCED_FOOD_BONUS.get()
            : diet == DietData.Tier.POOR ? -CapitalismConfig.DIET_POOR_FOOD_LOSS.get() : 0);
        float thirstTarget = ThirstData.MAX + (water == WaterPurity.Level.EXCELLENT ? CapitalismConfig.PURITY_EXCELLENT_WATER_BONUS.get()
            : water == WaterPurity.Level.LOW ? -CapitalismConfig.PURITY_LOW_WATER_LOSS.get() : 0);
        float perSecond = 2f / CapitalismConfig.WELLNESS_FADE_SECONDS.get();
        wellbeing.fade(healthTarget, foodTarget, thirstTarget, perSecond, perSecond);
        player.setData(ModAttachments.WELLBEING.get(), wellbeing);

        applyHealthMod(player, wellbeing.healthMod());
        int[] raw = new int[3];
        int[] effective = new int[3];
        int[] pending = new int[3];
        for (int i = 0; i < 3; i++) {
            raw[i] = wellbeing.raw(i);
            effective[i] = wellbeing.effective(i);
            pending[i] = wellbeing.pending(i);
        }
        PacketDistributor.sendToPlayer(player, new WellbeingSyncPacket(wellbeing.score(), buffAt, max, grace, debuffAt, min, neglect,
            raw, effective, pending, wellbeing.healthMod(), wellbeing.bodyHeat, wellbeing.heatReasons));
    }


    // Sets the Wellbeing max health modifier. If that leaves the player above their new max, their health is lowered
    // quietly: the client is told first, so it doesn't play the "you got hit" shake.
    private static void applyHealthMod(ServerPlayer player, float amount) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        AttributeModifier old = maxHealth.getModifier(OLD_DIET_HEALTH);
        AttributeModifier current = maxHealth.getModifier(WELLBEING_HEALTH);
        double oldAmount = (old != null ? old.amount() : 0) + (current != null ? current.amount() : 0);
        if (old == null && Math.abs(oldAmount - amount) < 0.001) return;

        float newMax = (float) (maxHealth.getValue() - oldAmount + amount);
        if (player.getHealth() > newMax) {
            PacketDistributor.sendToPlayer(player, new QuietHealthPacket(newMax));
            player.setHealth(newMax);
        }
        if (old != null) maxHealth.removeModifier(OLD_DIET_HEALTH);
        if (amount == 0) {
            if (current != null) maxHealth.removeModifier(WELLBEING_HEALTH);
        } else {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(WELLBEING_HEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void sync(ServerPlayer player, DietData diet) {
        float[] v = diet.values();
        PacketDistributor.sendToPlayer(player, new DietSyncPacket(v[0], v[1], v[2], v[3], diet.meals(), CapitalismConfig.DIET_MEALS_BEFORE_EFFECTS.get()));
    }
}

package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.network.ThirstSyncPacket;
import com.alexh.capitalism.occupation.GourmetFood;
import com.alexh.capitalism.player.ClientThirstData;
import com.alexh.capitalism.player.ThirstData;
import com.alexh.capitalism.player.WaterPurity;
import com.alexh.capitalism.player.WellbeingData;
import com.alexh.capitalism.player.WaterDrinking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Capitalism.MODID)
public class ModServerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ThirstData data = player.getData(ModAttachments.THIRST.get());

        // ── Water Purity moves the top of the thirst bar, the diet moves the top of the food bar ──
        // Excellent purity: +1 drop, Low: -1. Balanced diet: +1 drumstick, Poor: -1. Worked out once a second in
        // WellnessEvents (with the grace period and fading); here they're just applied.
        if (data.getPurity() < 0f) data.setPurity((float) CapitalismConfig.PURITY_START.get().doubleValue());
        WellbeingData wellbeing = player.getData(ModAttachments.WELLBEING.get());
        data.thirstCap = wellbeing.thirstCap();
        data.foodCap = wellbeing.foodCap();
        data.applyThirstCap();

        // Creative / spectator: thirst doesn't drain and never hurts (no darkness, slowness...)
        boolean exempt = player.isCreative() || player.isSpectator();

        // ── Drain ─────────────────────────────────────────────────────────
        float drain = (float) CapitalismConfig.THIRST_DRAIN_PER_TICK.get().doubleValue();
        if (player.isSprinting()) {
            drain *= (float) CapitalismConfig.THIRST_DRAIN_SPRINT_MULTIPLIER.get().doubleValue();
        }
        // Hot (body heat over 80, player/BodyTemperature): sweating, x3 to x5 the hotter you are
        float heatMultiplier = com.alexh.capitalism.player.BodyTemperature.thirstMultiplier(player.getData(ModAttachments.WELLBEING.get()).bodyHeat);
        drain *= heatMultiplier;
        // skill books: Water Rationing (a lesson) and Camel (a trait) slow it down
        drain *= com.alexh.capitalism.librarian.Skills.thirstDrainMultiplier(player);
        // a Camel pet's No Water Draining: none at all
        if (player.hasEffect(com.alexh.capitalism.core.ModEffects.NO_WATER_DRAINING)) drain = 0f;
        // sweating skips the hidden hydration buffer (it sweats away alongside), so the bar drops straight away
        if (!exempt) {
            if (heatMultiplier > 1f) data.sweat(drain);
            else data.drain(drain);
        }

        // ── Golden drumsticks: pay back any food vanilla hunger took ─────────
        data.protectFoodWithGold(player.getFoodData());
        data.tickGoldRush(player.getFoodData());

        int thirstLevel = exempt ? 20 : data.getThirstInt();

        // ── Thirst < 3: can't sprint + mining fatigue ─────────────────────
        if (thirstLevel < 3) {
            if (player.isSprinting()) player.setSprinting(false);
            if (thirstLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false));
            }
        }

        // ── Thirst = 0: slowness, weakness, darkness flashes, starvation ──
        if (thirstLevel <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            // Random darkness flash ~every 3 seconds on average
            if (player.getRandom().nextFloat() < 0.015f) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            }
            data.incrementDamageTimer();
            if (data.getDamageTimer() > 80) {
                player.hurt(player.level().damageSources().starve(), 1.0f);
                data.resetDamageTimer();
            }
        } else {
            data.resetDamageTimer();
        }

        // ── Sync to client every 10 ticks ────────────────────────────────
        if (player.level().getGameTime() % 10 == 0) {
            PacketDistributor.sendToPlayer(player, syncPacket(player, data));
        }
    }

    // Like food: you can't start drinking water while the thirst bar is full. (Drinking with only a little
    // missing is fine — the extra goes into the hidden hydration buffer.)
    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) return;
        // Remember the food bar before eating, so a big meal can overflow into a Balanced diet's bonus drumstick.
        if (player instanceof ServerPlayer serverPlayer && event.getItem().has(DataComponents.FOOD)) {
            ThirstData eating = serverPlayer.getData(ModAttachments.THIRST.get());
            eating.foodBeforeEating = serverPlayer.getFoodData().getFoodLevel();
            eating.saturationBeforeEating = serverPlayer.getFoodData().getSaturationLevel();
        }
        if (!isThirstDrink(event.getItem())) return;

        int thirst = player instanceof ServerPlayer serverPlayer
            ? serverPlayer.getData(ModAttachments.THIRST.get()).getThirstInt()
            : ClientThirstData.thirst;
        int cap = player instanceof ServerPlayer serverPlayer
            ? serverPlayer.getData(ModAttachments.THIRST.get()).thirstCap
            : ClientThirstData.thirstCap;
        if (thirst >= cap) {
            event.setCanceled(true);
        }
    }

    private static boolean isThirstDrink(ItemStack stack) {
        if (stack.is(ModItems.PURIFIED_WATER) || stack.is(ModItems.BOTTOMLESS_WATERSKIN) || stack.is(ModItems.SPRING_WATER)) return true;
        PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER);
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var item = event.getItem();

        // Vanilla stops the food bar at 20; with a Balanced diet the extra goes into the bonus drumstick.
        FoodProperties food = item.get(DataComponents.FOOD);
        ThirstData foodData = player.getData(ModAttachments.THIRST.get());
        if (food != null && foodData.foodBeforeEating >= 0) {
            int wanted = foodData.foodBeforeEating + food.nutrition();
            boolean wasFull = foodData.foodBeforeEating >= foodData.foodCap;
            foodData.fillBonusFood(player.getFoodData(), wanted);
            foodData.foodBeforeEating = -1;
            // Gourmet food: what didn't fit becomes gold drumsticks (half, since it's eaten by hand, not off a plate);
            // with the bar already full it's gold only: a gold rush, not saturation (ThirstData.goldOnlyMeal)
            if (KitchenRecipes.isGourmet(item)) {
                GourmetFood.addGold(player, wanted, GourmetFood.Serving.HAND);
                if (wasFull) foodData.goldOnlyMeal(player.getFoodData(), foodData.saturationBeforeEating);
            }
            PacketDistributor.sendToPlayer(player, syncPacket(player, foodData));
        }

        // Chef meals / Bartender drinks: fill the normal bars, extra becomes gold.
        GoldenBonus bonus = GoldenBonus.of(item);
        if (bonus != null) {
            ThirstData data = player.getData(ModAttachments.THIRST.get());
            if (bonus.water() > 0) data.restoreWithGold(bonus.water());
            if (bonus.food() > 0) data.feedWithGold(player.getFoodData(), bonus.food());
            // (alcoholic and other Bartender drinks never touch Water Purity: thirst and effects only)
            PacketDistributor.sendToPlayer(player, syncPacket(player, data));
            return;
        }

        PotionContents contents = item.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);

        float restore = 0f;
        boolean rawWater = false;
        WaterPurity.Source source = null;

        if (item.is(ModItems.SPRING_WATER)) {
            // Spring Water (Bartenders' Advanced Osmosis Machine): Excellent, pulls purity toward 100
            restore = (float) CapitalismConfig.THIRST_PURIFIED_WATER_RESTORE.get().doubleValue();
            source = WaterPurity.Source.EXCELLENT;
        } else if (item.is(ModItems.PURIFIED_WATER) || item.is(ModItems.BOTTOMLESS_WATERSKIN)) {
            restore = (float) CapitalismConfig.THIRST_PURIFIED_WATER_RESTORE.get().doubleValue();
            source = WaterPurity.Source.PURIFIED;
        } else if (contents != null && contents.is(Potions.WATER)) {
            // Plain water bottle = raw water
            restore = (float) CapitalismConfig.THIRST_RAW_WATER_RESTORE.get().doubleValue();
            rawWater = true;
            source = WaterPurity.Source.RAW;
        }

        if (restore <= 0f) return;

        ThirstData data = player.getData(ModAttachments.THIRST.get());
        data.restore(restore);
        data.setPurity(WaterPurity.afterDrink(data.getPurity(), restore, source));

        if (rawWater) {
            WaterDrinking.maybeGetSick(player);
        }

        PacketDistributor.sendToPlayer(player, syncPacket(player, data));
    }

    private static ThirstSyncPacket syncPacket(ServerPlayer player, ThirstData data) {
        return ThirstSyncPacket.of(data, player);
    }

    // Sync thirst when a player joins so HUD shows immediately
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ThirstData data = player.getData(ModAttachments.THIRST.get());
        PacketDistributor.sendToPlayer(player, syncPacket(player, data));
    }
}

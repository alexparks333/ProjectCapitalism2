package com.alexh.capitalism.boats;

import com.alexh.capitalism.Capitalism;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;

// Boat controls (Boats.md): riding the Clockwork Submersible, F (swap hands) with an empty hand turns its flashlight
// on or off instead of swapping hands. Riding a lava boat (LavaBoat), you never catch fire and take no fire or lava
// damage: your fire is put out every tick (before and after you tick, on both sides, so the flames never show) and
// any fire damage is cancelled.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BoatEvents {

    private BoatEvents() {
    }

    @SubscribeEvent
    public static void onTickPre(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        douse(event.getEntity());
    }

    @SubscribeEvent
    public static void onTickPost(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        douse(event.getEntity());
    }

    // aboard a lava boat: no fire on you
    static void douse(net.minecraft.world.entity.Entity entity) {
        if (!(entity.getVehicle() instanceof LavaBoat)) return;
        if (entity.getRemainingFireTicks() > 0) entity.clearFire();
        entity.setSharedFlagOnFire(false);
    }

    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity().getVehicle() instanceof LavaBoat && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (!(event.getEntity() instanceof Player player) || !(player.getVehicle() instanceof Submersible sub)) return;
        if (!player.getMainHandItem().isEmpty()) return;
        event.setCanceled(true);
        if (player.level().isClientSide()) return;
        sub.toggleFlashlight();
        player.level().playSound(null, sub.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6F, sub.flashlightOn() ? 1.2F : 0.8F);
        player.sendOverlayMessage(Component.literal(sub.flashlightOn() ? "Flashlight on" : "Flashlight off").withStyle(ChatFormatting.AQUA));
    }
}

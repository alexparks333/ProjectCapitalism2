package com.alexh.capitalism.designer;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.back.Back;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.town.Town;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The Traveler's Bag (Town.md > The Traveling Merchant: only he sells it, no Designer can make it; Bag.TRAVELERS_BAG):
// a magical bag worn on the back. Near town it has NEAR_SLOTS (6) slots of anything; once you're FAR_BLOCKS (1000) or
// more from the town center (or in another dimension) it opens out to FAR_SLOTS (12). Coming back toward town it shrinks
// back to 6: the first 6 keep what's in them, and whatever was in the other 6 bursts out around you (a puff, a pop) to be
// picked up. Whether it's open is on the bag itself (ModComponents.EXPANDED, synced to the client for the bag panel).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TravelersBag {

    public static final int NEAR_SLOTS = 6;
    public static final int FAR_SLOTS = 12;
    public static final int FAR_BLOCKS = 1000;

    private TravelersBag() {
    }

    public static boolean expanded(ItemStack bag) {
        return Boolean.TRUE.equals(bag.get(ModComponents.EXPANDED.get()));
    }

    // far enough from town for it to open out: another dimension, or FAR_BLOCKS from the town center (flat distance)
    public static boolean farFromTown(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return true;
        BlockPos center = Town.protectedArea().center(0);
        double dx = player.getX() - center.getX(), dz = player.getZ() - center.getZ();
        return dx * dx + dz * dz >= (double) FAR_BLOCKS * FAR_BLOCKS;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ItemStack worn = Back.peek(player);
        if (Bag.of(worn) != Bag.TRAVELERS_BAG) return;
        boolean far = farFromTown(player);
        if (far == expanded(worn)) return;
        if (far) {
            worn.set(ModComponents.EXPANDED.get(), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.7F);
            player.sendOverlayMessage(Component.literal("Your Traveler's Bag opens out: 12 slots out here.").withStyle(ChatFormatting.GOLD));
        } else {
            shrink(player, worn);
        }
        Back.set(player, worn);   // save it (and sync it to the client)
    }

    // back to NEAR_SLOTS: what's past them bursts out round the player
    public static void shrink(ServerPlayer player, ItemStack bag) {
        // through the inventory's own bag slots (their live container writes back into the bag), so nothing can be
        // left behind in it or doubled
        boolean burst = false;
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (!(slot instanceof BagSlot bagSlot) || bagSlot.index < NEAR_SLOTS) continue;
            ItemStack spill = bagSlot.getItem();
            if (spill.isEmpty()) continue;
            burst = true;
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 1.2, player.getZ(), spill.copy());
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            drop.setDeltaMovement(Math.cos(angle) * 0.25, 0.3, Math.sin(angle) * 0.25);
            drop.setPickUpDelay(20);
            player.level().addFreshEntity(drop);
            bagSlot.set(ItemStack.EMPTY);
        }
        bag.set(ModComponents.EXPANDED.get(), false);
        if (burst) {
            player.level().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
            player.level().playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.sendOverlayMessage(Component.literal("Your Traveler's Bag shrinks back to 6: what didn't fit bursts out!").withStyle(ChatFormatting.GOLD));
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.8F, 0.7F);
            player.sendOverlayMessage(Component.literal("Your Traveler's Bag shrinks back to 6 slots near town.").withStyle(ChatFormatting.GOLD));
        }
    }
}

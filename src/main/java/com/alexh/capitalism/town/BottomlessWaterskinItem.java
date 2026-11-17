package com.alexh.capitalism.town;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// The Purifying Waterskin (Town.md > The Traveling Merchant, only he sells it; id bottomless_waterskin): holds SIPS sips
// of water (the bar under it). Right-click any water (a lake, a river, even the sea) to fill it right up, and the water
// in it is purified straight away. Drink it like Purified Water (same thirst and purity: core/ModServerEvents treats it
// the same). Empty: it says so. Never breaks. Like any water, it can't be drunk with a full water bar.
public class BottomlessWaterskinItem extends Item {

    public static final int SIPS = 16;

    public BottomlessWaterskinItem(Properties properties) {
        super(properties);
    }

    public static int sipsLeft(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // looking at water: fill up (purified)
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            if (stack.getDamageValue() > 0) {
                stack.setDamageValue(0);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 0.8F);
                if (player instanceof ServerPlayer p) p.sendOverlayMessage(Component.literal("Filled: the water clears as it goes in."));
            }
            return InteractionResult.SUCCESS;
        }
        if (sipsLeft(stack) <= 0) {
            if (player instanceof ServerPlayer p) p.sendOverlayMessage(Component.literal("It's empty. Fill it from any water."));
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    // the drink happens (ModServerEvents.onItemUseFinish); the skin just loses a sip (and never breaks)
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player && player.isCreative())) stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
        return stack;
    }
}

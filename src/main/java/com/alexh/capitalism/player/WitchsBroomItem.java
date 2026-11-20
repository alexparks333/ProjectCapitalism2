package com.alexh.capitalism.player;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Witch's Broom (Town.md > The Blood Merchant > Spooky, only sold in the Halloween weeks): right-click and you float
// gently up for a moment (Levitation, RISE_TICKS), then drift slowly down (Slow Falling, FALL_TICKS), in a puff of
// witchy sparkles. Then it needs COOLDOWN_TICKS to recharge: for fun, not for flying. Never used up.
public class WitchsBroomItem extends Item {

    public static final int RISE_TICKS = 20 * 4;
    public static final int FALL_TICKS = 20 * 12;
    public static final int COOLDOWN_TICKS = 20 * 60;

    public WitchsBroomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            ride(player);
            server.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 0.3, player.getZ(), 30, 0.4, 0.2, 0.4, 0.05);
            server.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.3F);
            server.playSound(null, player.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    // up gently, then a slow drift down (the Slow Falling outlasts the rise, so the landing is soft)
    public static void ride(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, RISE_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, RISE_TICKS + FALL_TICKS, 0, false, false, true));
    }
}

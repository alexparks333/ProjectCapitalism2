package com.alexh.capitalism.decor;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.event.BloodMoon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A Bottle of Blood (Decorations.md > Blood Moon decorations): only the Blood Fountain gives it, and only under a Blood
// Moon (BloodDecorBlock.fillBottle). Drunk while the Blood Moon is up: Strength I and Speed I for the rest of that night
// (taken away the moment it ends: BloodDecorEvents). Drunk any other time: it turns your stomach (Nausea, Hunger and
// Weakness for two minutes). Hands back the glass bottle.
public class BottleOfBloodItem extends Item {

    // the player's tag while the blood's strength is in them (cleared, and the effects taken, when the Blood Moon ends)
    public static final String DRUNK = "capitalism_blood_drunk";
    public static final int SICK_TICKS = 2400;
    // the longest the blood's strength lasts (it never outlasts one night)
    public static final int MAX_TICKS = 10000;
    // when the blood's effects run out (game time): only effects matching it are the blood's to take away
    public static final String UNTIL = "capitalism_blood_until";

    // the blood's strength: Strength I and Speed I for `ticks`, tagged as the blood's
    public static void grant(ServerPlayer player, int ticks) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0));
        player.getPersistentData().putBoolean(DRUNK, true);
        player.getPersistentData().putLong(UNTIL, player.level().getGameTime() + ticks);
    }

    // Takes away the blood's Strength and Speed (the Blood Moon is over: BloodMoon.end, on login, on waking). Only the
    // blood's: an effect that isn't level I or doesn't run out when the blood's did (a potion's) is left alone.
    public static void stripBlood(ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.getBooleanOr(DRUNK, false)) return;
        long until = data.getLongOr(UNTIL, 0L);
        long now = player.level().getGameTime();
        for (var effect : java.util.List.of(MobEffects.STRENGTH, MobEffects.SPEED)) {
            MobEffectInstance e = player.getEffect(effect);
            if (e != null && e.getAmplifier() == 0 && Math.abs(now + e.getDuration() - until) <= 40) player.removeEffect(effect);
        }
        data.remove(DRUNK);
        data.remove(UNTIL);
    }

    public BottleOfBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) drink(player);
        return super.finishUsingItem(stack, level, user);
    }

    public static void drink(ServerPlayer player) {
        if (BloodMoon.isActive()) {
            // till dawn, never more than MAX_TICKS (and taken away the moment the Blood Moon ends: stripBlood)
            long timeOfDay = Math.floorMod(Calendar.clock(player.level()), Calendar.DAY_TICKS);
            grant(player, (int) Math.max(200, Math.min(MAX_TICKS, Math.floorMod(23000 - timeOfDay, Calendar.DAY_TICKS))));
            player.sendOverlayMessage(Component.literal("🩸 The blood burns in your veins: you feel the moon's strength.").withStyle(ChatFormatting.DARK_RED));
            player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.3F, 1.8F);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SICK_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, SICK_TICKS, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SICK_TICKS, 0));
            player.sendOverlayMessage(Component.literal("Ugh... cold blood, without the moon. You feel sick.").withStyle(ChatFormatting.GREEN));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 0.6F, 0.6F);
        }
    }
}

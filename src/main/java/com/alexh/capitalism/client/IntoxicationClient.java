package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// What Tipsy and Drunk feel like (player/Intoxication, Wellness.md > Intoxication), on your own screen only:
//  - Tipsy: the view rocks gently (a couple of degrees).
//  - Drunk: it rocks more and drifts side to side, and walking pulls you a little off to one side and back.
// Both fade in and out over a few seconds, so it never snaps.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class IntoxicationClient {

    private static final int FADE_TICKS = 100;

    private IntoxicationClient() {
    }

    // 0..1: how strong it is right now (fades in after drinking and out as it runs down), 0 = not at all
    private static float strength(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null) return 0;
        int left = instance.getDuration();
        return Math.min(1F, left / (float) FADE_TICKS);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        float tipsy = strength(player, ModEffects.TIPSY);
        float drunk = strength(player, ModEffects.DRUNK);
        if (tipsy <= 0 && drunk <= 0) return;
        float t = (player.tickCount + (float) event.getPartialTick()) / 20F;
        float roll = (float) Math.sin(t * 1.1) * (2.0F * tipsy + 5.0F * drunk);
        float yaw = (float) Math.sin(t * 0.7 + 1.3) * 3.0F * drunk;
        float pitch = (float) Math.sin(t * 0.9 + 0.4) * (0.6F * tipsy + 1.5F * drunk);
        event.setRoll(event.getRoll() + roll);
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
    }

    // Drunk: walking drifts a little to one side, then the other (players move on their own client)
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() || !player.isLocalPlayer()) return;
        float drunk = strength(player, ModEffects.DRUNK);
        if (drunk <= 0 || !player.onGround() || player.getAbilities().flying) return;
        if (player.zza == 0 && player.xxa == 0) return;
        float sway = (float) Math.sin(player.tickCount * 0.08) * 0.018F * drunk;
        player.moveRelative(Math.abs(sway), new Vec3(Math.signum(sway), 0, 0));
    }
}

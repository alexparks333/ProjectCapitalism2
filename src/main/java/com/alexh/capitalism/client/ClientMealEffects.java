package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.network.EnderLeapPacket;
import com.alexh.capitalism.player.MealEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// Ender Leap's keys: sneak + hold jump asks the server for a leap, at most once per cooldown.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class ClientMealEffects {

    private static int cooldown;

    private ClientMealEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cooldown > 0) cooldown--;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null || cooldown > 0) return;
        if (!mc.options.keyShift.isDown() || !mc.options.keyJump.isDown()) return;
        if (!player.hasEffect(ModEffects.ENDER_LEAP)) return;
        ClientPacketDistributor.sendToServer(EnderLeapPacket.INSTANCE);
        cooldown = MealEffects.ENDER_LEAP_COOLDOWN;
    }
}

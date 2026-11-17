package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// A Blood Moon on the client (event/BloodMoon sends BloodMoonPacket): the fog and the night air turn red; the sky and
// moon are mixin/SkyRendererMixin. Fades in over a few seconds as it begins, out as it ends, and only shows at night in
// the overworld.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public class BloodMoonClient {

    private static boolean active;
    private static float fade;

    public static void set(boolean on) {
        active = on;
    }

    // the Blood Moon Altar's vision (decor/BloodDecorBlock.offer, AltarVisionPacket): for this player alone the sky and fog
    // turn red for a while, by day or night
    private static int vision;
    private static float visionFade;

    public static void vision(int ticks) {
        vision = Math.max(vision, ticks);
    }

    // 0 (none) to 1 (full Blood Moon): faded in, and only by night in the overworld
    public static float strength() {
        Minecraft mc = Minecraft.getInstance();
        if ((fade <= 0.0F && visionFade <= 0.0F) || mc.level == null || mc.level.dimension() != Level.OVERWORLD) return 0.0F;
        long timeOfDay = Math.floorMod(Calendar.clock(mc.level), Calendar.DAY_TICKS);
        // fades in through dusk and out through the dawn
        float night = timeOfDay < 12000 || timeOfDay > 23500 ? 0.0F
            : timeOfDay < 13000 ? (timeOfDay - 12000) / 1000.0F
            : timeOfDay > 22500 ? (23500 - timeOfDay) / 1000.0F : 1.0F;
        return Math.max(fade * night, visionFade);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        float target = active ? 1.0F : 0.0F;
        if (fade < target) fade = Math.min(target, fade + 0.015F);
        else if (fade > target) fade = Math.max(target, fade - 0.015F);
        if (vision > 0) vision--;
        visionFade = vision > 0 ? Math.min(1.0F, visionFade + 0.04F) : Math.max(0.0F, visionFade - 0.02F);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        vision = 0;
        visionFade = 0.0F;
        fade = 0.0F;
    }

    // the fog, the colour of the air in the distance: blood red
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float s = strength();
        if (s <= 0.0F) return;
        float k = s * 0.75F;
        event.setRed(event.getRed() + (0.30F - event.getRed()) * k);
        event.setGreen(event.getGreen() + (0.02F - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (0.03F - event.getBlue()) * k);
    }
}

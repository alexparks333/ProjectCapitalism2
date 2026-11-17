package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Winter weather on this side (Calendar.md): once a tick, whether it's winter in the overworld you're in; while it is,
// mixin/BiomeWinterMixin draws whatever falls from the sky as snow, in every biome. (The server makes it snow often in
// winter: calendar/CalendarEvents.)
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class WinterWeather {

    private static volatile boolean winter;

    private WinterWeather() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        winter = mc.level != null && mc.level.dimension() == Level.OVERWORLD && Calendar.date(mc.level).season() == Calendar.Season.WINTER;
    }

    // should the sky's rain be drawn as snow now? (only asked on the render thread: the client's own drawing)
    public static boolean snowNow() {
        return winter && Minecraft.getInstance().isSameThread();
    }
}

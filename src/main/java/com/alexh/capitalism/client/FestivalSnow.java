package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Festival;
import com.alexh.capitalism.town.Town;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Winter Fest (calendar/Festival.WINTER_FEST, Calendar.md): all day it snows over the town, whatever the biome:
// snowflakes drifting down all round you while you're in the town (town/Town: the protected town center, from the
// server's config), only over the town itself.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class FestivalSnow {

    // snowflakes a tick, and how far round you they fall
    private static final int FLAKES = 30;
    private static final int RANGE = 24;

    private FestivalSnow() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.dimension() != Level.OVERWORLD) return;
        if (Festival.today(mc.level) != Festival.WINTER_FEST) return;
        Town.Area town = Town.protectedArea();
        if (!town.contains(mc.player.blockPosition())) return;
        RandomSource random = mc.level.getRandom();
        for (int i = 0; i < FLAKES; i++) {
            double x = mc.player.getX() + (random.nextDouble() - 0.5) * 2 * RANGE;
            double z = mc.player.getZ() + (random.nextDouble() - 0.5) * 2 * RANGE;
            if (!town.contains((int) Math.floor(x), (int) Math.floor(z))) continue;
            double y = mc.player.getY() + 4 + random.nextDouble() * 14;
            mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (random.nextDouble() - 0.5) * 0.04, -0.08 - random.nextDouble() * 0.06,
                (random.nextDouble() - 0.5) * 0.04);
        }
    }
}

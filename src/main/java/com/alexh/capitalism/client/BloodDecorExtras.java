package com.alexh.capitalism.client;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.decor.BloodDecorBlock;
import com.alexh.capitalism.decor.BloodDecorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The extra numbers some Blood Moon decorations are drawn with (BloodDecorModels.Anim x, y, z), worked out on the client
// each frame:
//  - the grandfather clock: its hour and minute hands (radians, from the in-game time: calendar/Calendar);
//  - the haunted portrait: where its eyes look (toward the nearest player, as a little sideways and up-down offset) and
//    whether it's night (its eyes glow);
//  - the bat roost and the throne: whether it's night (the roost's bats awake; bats circling over a seated lord).
final class BloodDecorExtras {

    private BloodDecorExtras() {
    }

    static void extract(BloodDecorBlockEntity decor, BloodDecorRenderer.State state, Vec3 camera, float partialTicks) {
        if (state.kind == null) return;
        Level level = decor.getLevel();
        long clock = Calendar.clock(level);
        switch (state.kind) {
            case GRANDFATHER_CLOCK -> {
                double minutes = (Math.floorMod(clock, Calendar.DAY_TICKS) * 1440.0 / Calendar.DAY_TICKS + 360.0) % 1440.0;
                state.x = (float) ((minutes % 720.0) / 720.0 * Math.PI * 2);
                state.y = (float) ((minutes % 60.0) / 60.0 * Math.PI * 2);
            }
            case HAUNTED_PORTRAIT -> {
                state.z = night(clock) ? 1.0F : 0.0F;
                Vec3 eyes = Vec3.atCenterOf(decor.getBlockPos()).add(0, 0.55, 0);
                Player nearest = level.getNearestPlayer(eyes.x, eyes.y, eyes.z, 24.0, false);
                if (nearest == null) return;
                Vec3 d = nearest.getEyePosition(partialTicks).subtract(eyes);
                double len = d.length();
                if (len < 1.0E-3) return;
                var block = decor.getBlockState();
                Direction front = block.hasProperty(BloodDecorBlock.FACING) ? block.getValue(BloodDecorBlock.FACING) : Direction.WEST;
                Direction side = front.getCounterClockWise();
                double across = (d.x * side.getStepX() + d.z * side.getStepZ()) / len;
                double up = d.y / len;
                state.x = (float) Math.max(-1, Math.min(1, across * 1.6));
                state.y = (float) Math.max(-1, Math.min(1, -up * 1.6));
            }
            case BAT_ROOST, CRIMSON_THRONE -> state.z = night(clock) ? 1.0F : 0.0F;
            default -> {
            }
        }
    }

    // 7 pm to 6 am
    static boolean night(long clock) {
        int m = Calendar.minuteOfDay(clock);
        return m >= 19 * 60 || m < 6 * 60;
    }

}

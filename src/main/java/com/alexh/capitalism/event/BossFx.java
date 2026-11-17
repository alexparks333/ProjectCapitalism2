package com.alexh.capitalism.event;

import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

// The Blood Moon bosses' attack visuals (Events.md > The bosses), so every attack has its own crisp, readable look.
// Built from trail particles (TrailParticleOption): each glides in a straight line to a target in an exact number of
// ticks, glows, then is gone. So rings, lines and waves stay sharp and in their own colour, instead of dust, which
// hangs about for seconds and smears into puddles. A "still" one (target = where it starts) is a glowing dot for that
// many ticks: redrawn each tick, a telegraph on the ground.
public final class BossFx {

    private BossFx() {
    }

    // a speck gliding from `from` to `to` in `ticks`
    public static void streak(ServerLevel level, Vec3 from, Vec3 to, int color, int ticks) {
        level.sendParticles(new TrailParticleOption(to, color, Math.max(1, ticks)), from.x, from.y, from.z, 1, 0, 0, 0, 0);
    }

    // a glowing dot that stays put for `ticks`
    public static void dot(ServerLevel level, Vec3 at, int color, int ticks) {
        streak(level, at, at, color, ticks);
    }

    // a still ring, flat, `up` above `centre` (redraw it every tick or two while it should show)
    public static void ring(ServerLevel level, Vec3 centre, double radius, int points, int color, int ticks, double up) {
        // dense enough to read as a solid line (a dot every ~0.22 blocks), and a second row inside on the big ones
        points = Math.max(points, (int) Math.ceil(radius * Math.PI * 2 / 0.22));
        if (radius >= 2.0) ringRow(level, centre, radius - 0.16, points, color, ticks, up);
        ringRow(level, centre, radius, points, color, ticks, up);
    }

    private static void ringRow(ServerLevel level, Vec3 centre, double radius, int points, int color, int ticks, double up) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            dot(level, new Vec3(centre.x + Math.cos(a) * radius, centre.y + up, centre.z + Math.sin(a) * radius), color, ticks);
        }
    }

    // a still line of dots
    public static void line(ServerLevel level, Vec3 from, Vec3 to, int points, int color, int ticks) {
        points = Math.max(points, (int) Math.ceil(from.distanceTo(to) / 0.22));
        for (int i = 0; i <= points; i++) dot(level, from.lerp(to, i / (double) points), color, ticks);
    }

    // a ring rolling outward along the ground from radius `from` to `to` in `ticks`: a wall, one row of points per
    // {height, colour} in `rows`. Sent once.
    public static void wave(ServerLevel level, Vec3 centre, double from, double to, int ticks, int points, double[][] rows) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            double cos = Math.cos(a), sin = Math.sin(a);
            for (double[] row : rows) {
                streak(level, new Vec3(centre.x + cos * from, centre.y + row[0], centre.z + sin * from),
                    new Vec3(centre.x + cos * to, centre.y + row[0], centre.z + sin * to), (int) row[1], ticks);
            }
        }
    }

    // spikes shooting up out of the ground in a circle of `radius`: n streaks, up to `height`, alternating two colours
    public static void spikes(ServerLevel level, RandomSource random, Vec3 at, double radius, int n, double height, int color, int tip) {
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * radius;
            Vec3 foot = new Vec3(at.x + Math.cos(a) * r, at.y + 0.05, at.z + Math.sin(a) * r);
            double h = height * (0.6 + random.nextDouble() * 0.4);
            streak(level, foot, foot.add(0, h, 0), i % 3 == 0 ? tip : color, 4 + random.nextInt(3));
        }
    }

    // specks flung out in every direction from `at`, `dist` blocks in `ticks`
    public static void burst(ServerLevel level, RandomSource random, Vec3 at, int n, double dist, int color, int ticks) {
        for (int i = 0; i < n; i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (dir.lengthSqr() < 1.0E-4) continue;
            streak(level, at, at.add(dir.normalize().scale(dist * (0.5 + random.nextDouble() * 0.5))), color, ticks);
        }
    }

    // specks flung out flat along the ground, a ring of debris
    public static void groundBurst(ServerLevel level, RandomSource random, Vec3 at, int n, double dist, int color, int ticks) {
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = dist * (0.5 + random.nextDouble() * 0.5);
            streak(level, at.add(0, 0.15, 0), at.add(Math.cos(a) * d, 0.15 + random.nextDouble() * 0.6, Math.sin(a) * d), color, ticks);
        }
    }
}

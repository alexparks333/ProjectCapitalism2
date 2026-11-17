package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Nordic Longship (Boats.md): a Viking dragon ship (client/LongshipModel). Seats six: the helmsman on his chest at
// the stern, four on the rowing benches, one at the prow. Its campfire smokes and crackles with sparks whether it's
// moving or not (a thin wisp, the odd ember); it throws spray off the dragon's chest when it's going fast.
public class Longship extends PremiumBoat {

    private static final float[][] SEATS = {
        {-27.0F, 0.0F, -2.7F},    // the helmsman, on his chest
        {-17.0F, -3.5F, -3.1F},   // rowing benches
        {-17.0F, 3.5F, -3.1F},
        {11.0F, -3.5F, -3.1F},
        {11.0F, 3.5F, -3.1F},
        {21.0F, 0.0F, -3.1F}};    // the prow
    // the campfire, in model pixels back from the middle
    private static final double FIRE_X = -7.0 / 16.0;

    public Longship(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.LONGSHIP.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-7.0F, 0.0F, -9.0F, 10}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw) * FIRE_X;
        double fz = Mth.cos(yaw) * FIRE_X;
        double y = this.getY() + 0.5;
        // a thin wisp of smoke, and now and then a tiny ember
        if (this.random.nextFloat() < 0.08F) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX() + fx, y + 0.1, this.getZ() + fz, 0.0, 0.02, 0.0);
        }
        if (this.random.nextFloat() < 0.02F) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX() + fx, y, this.getZ() + fz, 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            double bx = this.getX() + fx * 2.3 + fz * side * 0.2;
            double bz = this.getZ() + fz * 2.3 - fx * side * 0.2;
            this.level().addParticle(ParticleTypes.SPLASH, bx, y, bz, fz * side * 0.15, 0.1, -fx * side * 0.15);
        }
        if (this.random.nextFloat() < speed * 2.0) {
            this.level().addParticle(ParticleTypes.FISHING, this.getX() - fx * 2.3, y - 0.05, this.getZ() - fz * 2.3, 0.0, 0.0, 0.0);
        }
    }
}

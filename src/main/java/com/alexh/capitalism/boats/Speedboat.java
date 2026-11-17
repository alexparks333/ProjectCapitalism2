package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Speedboat (Boats.md): the Boat Master's 100 gold Premium boat. A Premium boat (10% faster than a boat, a boat's
// size on the water), smaller than the Pirate Ship: a sleek white deep-V runabout with racing stripes, a wraparound
// windshield, white leather seats, twin outboards whose propellers spin; it lifts its bow as it gets up to speed and
// throws a rooster tail of spray behind (client/SpeedboatModel, SpeedboatRenderer). Seats four: the driver (steers)
// and a passenger in bucket seats, two on the bench behind.
public class Speedboat extends PremiumBoat {

    private static final float[][] SEATS = {
        {-5.0F, -3.5F, -3.0F},   // driver, at the wheel (starboard)
        {-5.0F, 3.5F, -3.0F},    // passenger seat
        {-12.3F, -3.5F, -3.0F},  // the bench behind
        {-12.3F, 3.5F, -3.0F}};

    public Speedboat(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.SPEEDBOAT.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-17.0F, 0.0F, -17.0F, 6}, {15.0F, 0.0F, -9.0F, 5}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    // a rooster tail thrown up behind the twin outboards, spray peeling off the bow, a white wake
    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.25;
        for (int side = -1; side <= 1; side += 2) {
            double ex = this.getX() - fx * 1.5 + fz * side * 0.22;
            double ez = this.getZ() - fz * 1.5 - fx * side * 0.22;
            double up = 0.15 + speed * 0.6;
            this.level().addParticle(ParticleTypes.SPLASH, ex, y, ez, -fx * 0.2, up, -fz * 0.2);
            this.level().addParticle(ParticleTypes.FISHING, ex - fx * 0.4, y - 0.05, ez - fz * 0.4, 0.0, 0.0, 0.0);
            double bx = this.getX() + fx * 1.0 + fz * side * 0.45;
            double bz = this.getZ() + fz * 1.0 - fx * side * 0.45;
            this.level().addParticle(ParticleTypes.SPLASH, bx, y, bz, fz * side * 0.2, 0.08, -fx * side * 0.2);
        }
        if (this.random.nextFloat() < speed * 1.5) {
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() - fx * 1.8, y + 0.1, this.getZ() - fz * 1.8, -fx * 0.05, 0.03, -fz * 0.05);
        }
    }
}

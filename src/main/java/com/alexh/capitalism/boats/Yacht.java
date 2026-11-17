package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Yacht (Boats.md): the Boat Master's 700 gold Premium boat, the most luxurious of them (client/YachtModel).
// Seats six: the helm and a seat beside it up on the flybridge, two on the aft deck sofa, and two in the hot tub on
// the bow. Throws a wide bow wave and leaves a long white wake.
public class Yacht extends PremiumBoat {

    private static final float[][] SEATS = {
        {1.0F, -3.0F, -18.2F},    // the helm, on the flybridge
        {1.0F, 3.0F, -18.2F},
        {-28.5F, -4.0F, -5.6F},   // the aft deck sofa
        {-28.5F, 4.0F, -5.6F},
        {21.0F, -1.8F, -8.2F},    // in the hot tub
        {21.0F, 1.8F, -8.2F}};

    public Yacht(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.YACHT.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{0.0F, 0.0F, -23.0F, 7}, {-24.0F, 0.0F, -11.0F, 8}, {23.0F, 0.0F, -13.0F, 8}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            double bx = this.getX() + fx * 1.9 + fz * side * 0.4;
            double bz = this.getZ() + fz * 1.9 - fx * side * 0.4;
            this.level().addParticle(ParticleTypes.SPLASH, bx, y, bz, fz * side * 0.2, 0.12, -fx * side * 0.2);
            double sx = this.getX() - fx * 2.2 + fz * side * 0.5;
            double sz = this.getZ() - fz * 2.2 - fx * side * 0.5;
            this.level().addParticle(ParticleTypes.FISHING, sx, y - 0.05, sz, 0.0, 0.0, 0.0);
        }
        if (this.random.nextFloat() < 0.5) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - fx * 2.0, y - 0.3, this.getZ() - fz * 2.0, 0.0, 0.05, 0.0);
        }
    }
}

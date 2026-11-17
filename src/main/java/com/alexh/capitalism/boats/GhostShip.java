package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Spectral Galleon (Boats.md, client/GhostShipModel): the pirate galleon come back from the deep, see-through and
// glowing, with a ghost in its main top. Seats four, like the Pirate Ship. Souls drift up off it and its stern burns
// with soul fire whether it moves or not; when it sails it trails a pale mist.
public class GhostShip extends PremiumBoat implements LavaBoat {

    private static final float[][] SEATS = {
        {-20.0F, 0.0F, -7.0F},
        {3.0F, 4.0F, -1.0F},
        {3.0F, -4.0F, -1.0F},
        {22.0F, 0.0F, -5.0F}};

    public GhostShip(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.GHOST_SHIP.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-27.0F, 0.0F, -19.0F, 8}, {14.0F, 0.0F, -9.0F, 6}, {-3.0F, 0.0F, -52.0F, 6}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        // souls rising from anywhere along the hull
        if (this.random.nextFloat() < 0.25F) {
            double along = (this.random.nextDouble() - 0.5) * 3.2;
            double across = (this.random.nextDouble() - 0.5) * 1.3;
            this.level().addParticle(this.random.nextBoolean() ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL,
                this.getX() + fx * along + fz * across, this.getY() + 0.6, this.getZ() + fz * along - fx * across, 0.0, 0.03, 0.0);
        }
        // soul fire at the stern lantern
        if (this.random.nextFloat() < 0.3F) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() - fx * 1.8, this.getY() + 1.55, this.getZ() - fz * 1.8,
                0.0, 0.01, 0.0);
        }
    }

    // a pale mist trailing behind it
    @Override
    protected void spray(double speed, double fx, double fz) {
        if (this.isInLava()) {
            // on lava: smoke curling off the bow and the odd pop of lava in the wake
            double y = this.getY() + 0.3;
            for (int side = -1; side <= 1; side += 2) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + fx * 1.8 + fz * side * 0.3, y,
                    this.getZ() + fz * 1.8 - fx * side * 0.3, fz * side * 0.03, 0.05, -fx * side * 0.03);
            }
            if (this.random.nextFloat() < 0.15F) {
                this.level().addParticle(ParticleTypes.LAVA, this.getX() - fx * 1.8, y, this.getZ() - fz * 1.8, 0.0, 0.0, 0.0);
            }
            return;
        }
        double y = this.getY() + 0.35;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.WHITE_SMOKE, this.getX() + fx * 1.8 + fz * side * 0.3, y,
                this.getZ() + fz * 1.8 - fx * side * 0.3, fz * side * 0.05, 0.02, -fx * side * 0.05);
        }
        this.level().addParticle(ParticleTypes.SOUL, this.getX() - fx * 1.8, y + 0.2, this.getZ() - fz * 1.8, 0.0, 0.02, 0.0);
    }
}

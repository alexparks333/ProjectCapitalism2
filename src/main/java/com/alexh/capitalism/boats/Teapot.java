package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Wandering Teapot (Boats.md, client/TeapotModel): a great white porcelain teapot on a birch saucer, its lid
// tipped open, a plume of "steam" billowing from its spout for a sail. Seats three, sitting inside the pot among the
// brewing stands and cake (a window in the front to see out). A little campfire under it keeps it steaming: gentle
// white wisps from the spout and the open lid.
public class Teapot extends PremiumBoat {

    private static final float[][] SEATS = {
        {-3.0F, 0.0F, -3.0F},     // in the pot
        {2.5F, -3.0F, -3.0F},
        {2.5F, 3.0F, -3.0F}};
    private static final float[][] LIGHTS = {{0.0F, 0.0F, -18.0F, 6}};
    private static final double SPOUT_X = 20.0 / 16.0;
    private static final double SPOUT_Y = (22.0 + 6.0) / 16.0;

    public Teapot(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.TEAPOT.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

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
        // gentle wisps of white steam from the spout, now and then one from the open lid
        if (this.random.nextFloat() < 0.15F) {
            this.level().addParticle(ParticleTypes.WHITE_SMOKE, this.getX() + fx * SPOUT_X, this.getY() + SPOUT_Y,
                this.getZ() + fz * SPOUT_X, fx * 0.01, 0.02, fz * 0.01);
        }
        if (this.random.nextFloat() < 0.04F) {
            this.level().addParticle(ParticleTypes.WHITE_SMOKE, this.getX() + (this.random.nextDouble() - 0.5) * 0.5, this.getY() + 1.6,
                this.getZ() + (this.random.nextDouble() - 0.5) * 0.5, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.25;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 1.2 + fz * side * 0.9, y,
                this.getZ() + fz * 1.2 - fx * side * 0.9, fz * side * 0.12, 0.08, -fx * side * 0.12);
        }
    }
}

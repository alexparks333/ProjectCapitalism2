package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Steampunk Paddlewheeler (Boats.md, client/PaddlewheelerModel). Seats three: the pilot at the big wheel up on the
// pilothouse, two on the deck house roof in front of it. Its twin stacks puff a little smoke at rest and pour smoke
// and steam when it's driven (the whistle blows a jet of steam now and then); its wheels churn the water.
public class Paddlewheeler extends PremiumBoat {

    private static final float[][] SEATS = {
        {-19.0F, 0.0F, -12.6F},   // the pilot, at the wheel
        {-10.5F, -3.0F, -12.6F},  // the deck house roof
        {-10.5F, 3.0F, -12.6F}};
    // the stacks: forward of the middle, either side, their tops (blocks)
    private static final double STACK_X = 18.0 / 16.0;
    private static final double STACK_Z = 3.2 / 16.0;
    private static final double STACK_TOP = (42.0 + 6.0) / 16.0;

    public Paddlewheeler(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.PADDLEWHEELER.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-16.0F, 0.0F, -20.0F, 7}, {26.0F, 0.0F, -12.0F, 8}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        double speed = this.getDeltaMovement().horizontalDistance();
        boolean driven = this.getControllingPassenger() != null && speed > 0.03;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        for (int s = -1; s <= 1; s += 2) {
            if (this.random.nextFloat() < (driven ? 0.7F : 0.08F)) {
                double x = this.getX() + fx * STACK_X + fz * s * STACK_Z;
                double z = this.getZ() + fz * STACK_X - fx * s * STACK_Z;
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, this.getY() + STACK_TOP, z,
                    -fx * speed * 0.5, 0.05 + speed * 0.1, -fz * speed * 0.5);
            }
        }
        // the whistle: a jet of steam now and then
        if (driven && this.random.nextFloat() < 0.06F) {
            for (int i = 0; i < 6; i++) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getX() + fx * 1.0, this.getY() + 2.8, this.getZ() + fz * 1.0,
                    (this.random.nextDouble() - 0.5) * 0.05, 0.12, (this.random.nextDouble() - 0.5) * 0.05);
            }
        }
        // steam hissing off the engine
        if (driven && this.random.nextFloat() < 0.4F) {
            this.level().addParticle(ParticleTypes.WHITE_SMOKE, this.getX() + fx * 0.6, this.getY() + 0.9, this.getZ() + fz * 0.6, 0.0, 0.03, 0.0);
        }
    }

    // the wheels churning on each side, and the wake
    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            double wx = this.getX() + fz * side * 0.75;
            double wz = this.getZ() - fx * side * 0.75;
            this.level().addParticle(ParticleTypes.SPLASH, wx, y, wz, -fx * 0.1, 0.2, -fz * 0.1);
            this.level().addParticle(ParticleTypes.BUBBLE, wx - fx * 0.3, y - 0.2, wz - fz * 0.3, 0.0, 0.05, 0.0);
        }
        this.level().addParticle(ParticleTypes.FISHING, this.getX() - fx * 1.8, y - 0.05, this.getZ() - fz * 1.8, 0.0, 0.0, 0.0);
    }
}

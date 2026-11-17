package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The River Queen (Boats.md, client/SteamboatModel): a grand Mississippi sternwheeler. Seats five on the hurricane
// deck: the helm behind the wheel between the texas and the stacks, two at the front rail, two at the back. Its stacks smoke
// (more when it's driven), and the big sternwheel kicks the water up behind it: a little at rest, a lot under way.
public class Steamboat extends PremiumBoat {

    private static final float[][] SEATS = {
        {10.0F, 0.0F, -27.2F},    // the helm, before the texas, between it and the stacks
        {24.0F, -3.5F, -27.2F},   // the front rail
        {24.0F, 3.5F, -27.2F},
        {-25.0F, -3.5F, -27.2F},  // the back rail
        {-25.0F, 3.5F, -27.2F}};
    // the promenade lamps fore and aft, the pilothouse, the firebox doors
    private static final float[][] LIGHTS = {{22.0F, 0.0F, -31.0F, 7}, {-26.0F, 0.0F, -31.0F, 7}, {2.5F, 0.0F, -42.0F, 6}, {21.0F, 0.0F, -7.0F, 6}};
    private static final double STACK_X = 15.0 / 16.0;
    private static final double STACK_Z = 4.5 / 16.0;
    private static final double STACK_TOP = (68.0 + 6.0) / 16.0;
    private static final double WHEEL_X = -44.0 / 16.0;

    public Steamboat(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.STEAMBOAT.get());
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
        double speed = this.getDeltaMovement().horizontalDistance();
        boolean driven = this.getControllingPassenger() != null && speed > 0.03;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        // smoke from the stacks
        for (int s = -1; s <= 1; s += 2) {
            if (this.random.nextFloat() < (driven ? 0.6F : 0.1F)) {
                double x = this.getX() + fx * STACK_X + fz * s * STACK_Z;
                double z = this.getZ() + fz * STACK_X - fx * s * STACK_Z;
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, this.getY() + STACK_TOP, z,
                    -fx * speed * 0.5, 0.05 + speed * 0.1, -fz * speed * 0.5);
            }
        }
        // the sternwheel kicking up water: thrown up and back off the paddles
        double wx = this.getX() + fx * WHEEL_X;
        double wz = this.getZ() + fz * WHEEL_X;
        int drops = driven ? 6 : (this.random.nextFloat() < 0.3F ? 1 : 0);
        for (int i = 0; i < drops; i++) {
            double across = (this.random.nextDouble() - 0.5) * 0.9;
            double up = 0.15 + this.random.nextDouble() * (0.2 + speed * 0.8);
            this.level().addParticle(ParticleTypes.SPLASH, wx + fz * across, this.getY() + 0.4 + this.random.nextDouble() * 0.6,
                wz - fx * across, -fx * (0.1 + speed), up, -fz * (0.1 + speed));
        }
        if (driven && this.random.nextFloat() < 0.5F) {
            this.level().addParticle(ParticleTypes.FALLING_WATER, wx + (this.random.nextDouble() - 0.5) * 0.8, this.getY() + 1.1,
                wz + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.FISHING, wx - fx * 0.6, this.getY() + 0.3, wz - fz * 0.6, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 2.0 + fz * side * 0.5, y,
                this.getZ() + fz * 2.0 - fx * side * 0.5, fz * side * 0.15, 0.1, -fx * side * 0.15);
        }
    }
}

package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Swamp Smuggler's Raft (Boats.md, client/SwampRaftModel). Seats four: two under the lean-to, one on the middle
// cross log, one on the front one. Fireflies drift round it and swamp gas bubbles up beside it.
public class SwampRaft extends PremiumBoat {

    private static final float[][] SEATS = {
        {-11.0F, -3.5F, -4.0F},   // under the lean-to
        {-11.0F, 3.5F, -4.0F},
        {0.0F, -3.5F, -4.0F},     // the middle cross log
        {13.0F, 2.0F, -4.0F}};    // the front cross log

    public SwampRaft(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.SWAMP_RAFT.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-3.0F, -6.0F, -20.0F, 8}, {16.0F, -11.0F, -15.0F, 6}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        if (this.random.nextFloat() < 0.12F) {
            this.level().addParticle(ParticleTypes.FIREFLY, this.getX() + (this.random.nextDouble() - 0.5) * 3.0,
                this.getY() + 0.8 + this.random.nextDouble() * 1.4, this.getZ() + (this.random.nextDouble() - 0.5) * 3.0, 0.0, 0.0, 0.0);
        }
        if (this.isInWater() && this.random.nextFloat() < 0.05F) {
            this.level().addParticle(ParticleTypes.BUBBLE_POP, this.getX() + (this.random.nextDouble() - 0.5) * 2.6,
                this.getY() + 0.3, this.getZ() + (this.random.nextDouble() - 0.5) * 2.6, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.25;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 1.3 + fz * side * 0.6, y,
                this.getZ() + fz * 1.3 - fx * side * 0.6, fz * side * 0.08, 0.06, -fx * side * 0.08);
        }
    }
}

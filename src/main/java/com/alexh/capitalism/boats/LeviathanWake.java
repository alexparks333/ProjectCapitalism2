package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Leviathan's Wake (Boats.md, client/LeviathanModel): a dark oak deck built inside a sea monster's ribcage, sails
// of amethyst and tinted glass, a beating heart of crying obsidian, soul lanterns hanging from the ribs. Seats four.
// The heart weeps glowing tears and the sails shimmer with magic (particles); the amethyst hums now and then as it
// sails (a soft chime, only you hear it, on board or near).
public class LeviathanWake extends PremiumBoat implements LavaBoat {

    private static final float[][] SEATS = {
        {-20.0F, 0.0F, -1.7F},    // the helm, at the tail
        {-6.0F, -4.0F, -1.7F},    // either side of the mast
        {-6.0F, 4.0F, -1.7F},
        {17.0F, 0.0F, -1.7F}};    // up by the skull
    private static final float[][] LIGHTS = {{10.0F, 0.0F, -12.0F, 7}, {-18.0F, 0.0F, -30.0F, 5}, {18.0F, 0.0F, -30.0F, 5}};

    public LeviathanWake(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.LEVIATHAN.get());
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
        double hx = this.getX() + fx * (10.0 / 16.0);
        double hz = this.getZ() + fz * (10.0 / 16.0);
        // the heart weeps
        if (this.random.nextFloat() < 0.15F) {
            this.level().addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, hx + (this.random.nextDouble() - 0.5) * 0.3,
                this.getY() + 0.9, hz + (this.random.nextDouble() - 0.5) * 0.3, 0.0, 0.0, 0.0);
        }
        if (this.random.nextFloat() < 0.2F) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, hx, this.getY() + 1.0, hz,
                (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
        }
        // the sails shimmer
        if (this.random.nextFloat() < 0.25F) {
            double along = (this.random.nextDouble() - 0.5) * 2.0;
            this.level().addParticle(ParticleTypes.END_ROD, this.getX() + fx * along, this.getY() + 1.6 + this.random.nextDouble() * 1.4,
                this.getZ() + fz * along, 0.0, 0.01, 0.0);
        }
        // the hum
        if (this.getDeltaMovement().horizontalDistance() > 0.08 && this.random.nextFloat() < 0.02F) {
            this.level().playLocalSound(this.getX(), this.getY() + 2.0, this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL,
                0.6F, 0.6F + this.random.nextFloat() * 0.6F, false);
        }
    }

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
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 1.8 + fz * side * 0.3, y,
                this.getZ() + fz * 1.8 - fx * side * 0.3, fz * side * 0.15, 0.1, -fx * side * 0.15);
        }
        this.level().addParticle(ParticleTypes.SOUL, this.getX() - fx * 1.8, y + 0.2, this.getZ() - fz * 1.8, 0.0, 0.02, 0.0);
    }
}

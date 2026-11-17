package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Castaway's Tub (Boats.md, client/CastawayTubModel): half a giant barrel of stripped jungle wood, permanently
// half full of water, a composter for the captain's chair, a sad white banner on a crooked bamboo shoot, and a wooden
// shovel for a paddle (it paddles when you do). Seats one, on the composter. It drips and leaks.
public class CastawayTub extends PremiumBoat {

    private static final float[][] SEATS = {{-1.0F, 0.0F, -4.7F}};   // on the composter

    public CastawayTub(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.CASTAWAY_TUB.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        if (this.random.nextFloat() < 0.12F) {
            this.level().addParticle(ParticleTypes.DRIPPING_WATER, this.getX() + (this.random.nextDouble() - 0.5) * 1.2,
                this.getY() + 0.5, this.getZ() + (this.random.nextDouble() - 0.5) * 1.2, 0.0, 0.0, 0.0);
        }
        if (this.random.nextFloat() < 0.06F) {
            this.level().addParticle(ParticleTypes.BUBBLE_POP, this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                this.getY() + 0.45, this.getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 0.8, this.getY() + 0.25, this.getZ() + fz * 0.8, 0.0, 0.06, 0.0);
    }
}

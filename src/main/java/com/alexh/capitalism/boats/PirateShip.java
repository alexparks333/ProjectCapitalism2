package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Pirate Ship (Boats.md): the Boat Master's 500 gold Premium boat, a luxury for players with gold to burn. A
// Premium boat (10% faster than a boat, a boat's size on the water), seats four (the helm on the quarterdeck, two side
// by side on the main deck, one on the forecastle), throws spray off its bow, and looks like a full pirate galleon
// (client/PirateShipModel). Broken, it drops its item back.
public class PirateShip extends PremiumBoat {

    private static final float[][] SEATS = {
        {-20.0F, 0.0F, -7.0F},  // the helm, on the quarterdeck (behind the wheel)
        {3.0F, 4.0F, -1.0F},    // main deck, port
        {3.0F, -4.0F, -1.0F},   // main deck, starboard
        {22.0F, 0.0F, -5.0F}};  // the forecastle

    public PirateShip(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.PIRATE_SHIP.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    // its light on its surroundings: {forward, left, up, brightness} (PremiumBoat.lights)
    private static final float[][] LIGHTS = {{-27.0F, 0.0F, -19.0F, 9}, {14.0F, 0.0F, -9.0F, 7}};

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    // spray off the bow (about 1.8 blocks forward) to each side, a wake off the stern
    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        for (int side = -1; side <= 1; side += 2) {
            double bx = this.getX() + fx * 1.8 + fz * side * 0.25;
            double bz = this.getZ() + fz * 1.8 - fx * side * 0.25;
            this.level().addParticle(ParticleTypes.SPLASH, bx, y, bz, fz * side * 0.15, 0.1, -fx * side * 0.15);
            if (this.random.nextFloat() < speed * 2.0) {
                double sx = this.getX() - fx * 1.7 + fz * side * 0.4;
                double sz = this.getZ() - fz * 1.7 - fx * side * 0.4;
                this.level().addParticle(ParticleTypes.FISHING, sx, y - 0.05, sz, 0.0, 0.0, 0.0);
            }
        }
        if (this.random.nextFloat() < 0.3) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + fx * 1.6, y - 0.2, this.getZ() + fz * 1.6, 0.0, 0.05, 0.0);
        }
    }
}

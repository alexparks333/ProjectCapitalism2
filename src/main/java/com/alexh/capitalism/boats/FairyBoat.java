package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The Fairy Blossom (Boats.md, client/FairyBoatModel): a slender pearl-and-blush boat shaped like a leaf, smothered in
// flowers: garlands along its gunwales and swags of blossoms down its sides, a curling vine at the bow with a glowing
// bell flower swaying from it, an arbor of birch arches hung with wisteria and fairy lights over its seats, a great
// lotus blooming on a stem for a mast, and four glowing, see-through wings folded along its sides.
// Its FLUTTER: the driver presses jump (Space) on the water (or land) and it springs up FLIGHT_HEIGHT blocks, spreads
// its wings and holds that height for HOLD_TICKS (2 seconds), steering and sailing on as usual, then drifts gently
// back down. A short rest after it lands before it can flutter again.
// The boat moves on its driver's side (like any boat), so the flutter runs there: the height goes to the server and
// everyone else with its position. Its wings spread whenever it's off the water (wingSpread), on every side.
public class FairyBoat extends PremiumBoat {

    public static final double FLIGHT_HEIGHT = 2.5;
    public static final int HOLD_TICKS = 40;
    private static final int REST_TICKS = 15;
    private static final double FALL_SPEED = 0.11;

    private static final float[][] SEATS = {
        {-12.0F, 0.0F, -4.0F},    // the helm, on its stool behind the arbor
        {-2.0F, 2.6F, -3.8F},     // the loveseat under the arbor
        {-2.0F, -2.6F, -3.8F}};
    // the lotus on the mast, the bell flower at the bow, the fairy lights in the arbor
    private static final float[][] LIGHTS = {{12.0F, 0.0F, -33.0F, 8}, {27.0F, 0.0F, -12.0F, 7}, {-2.0F, 0.0F, -20.0F, 6}};

    private enum Flight { NONE, RISE, HOLD, FALL }

    private Flight flight = Flight.NONE;
    private double flightY;
    private int holdLeft;
    private int rest;
    private double fallSpeed;
    // how far its wings are spread (0 folded, 1 open: off the water), and last tick's (client, for the model)
    private float wingSpread;
    private float wingSpreadO;

    public FairyBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.FAIRY_BOAT.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    public float wingSpread(float partialTicks) {
        return Mth.lerp(partialTicks, this.wingSpreadO, this.wingSpread);
    }

    // in the middle of a flutter (the tests)
    public boolean fluttering() {
        return this.flight != Flight.NONE;
    }

    // starts a flutter, if it can (on the water or the ground, rested); true if it did
    public boolean flutter() {
        if (this.flight != Flight.NONE || this.rest > 0 || !(this.isInWater() || this.onGround())) return false;
        this.flight = Flight.RISE;
        this.flightY = this.getY() + FLIGHT_HEIGHT;
        if (this.level().isClientSide()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.6F, 1.4F, false);
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 0.8F, 1.3F, false);
            for (int i = 0; i < 18; i++) {
                double a = i * Math.PI * 2 / 18;
                this.level().addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.CHERRY_LEAVES, this.getX() + Math.cos(a) * 1.2, this.getY() + 0.3,
                    this.getZ() + Math.sin(a) * 1.2, Math.cos(a) * 0.08, 0.05, Math.sin(a) * 0.08);
            }
        }
        return true;
    }

    // Which way it moves up or down this tick in a flutter (its driver's side): up to its height, holding there with a
    // gentle bob, then drifting down until it's back on the water
    private double flightMotion(double current) {
        this.resetFallDistance();
        switch (this.flight) {
            case RISE -> {
                double gap = this.flightY - this.getY();
                if (gap < 0.04 || this.verticalCollision && current > 0) {
                    this.flight = Flight.HOLD;
                    this.holdLeft = HOLD_TICKS;
                    return gap * 0.5;
                }
                return Mth.clamp(gap * 0.3, 0.06, 0.45);
            }
            case HOLD -> {
                if (--this.holdLeft <= 0) {
                    this.flight = Flight.FALL;
                    this.fallSpeed = 0.0;
                }
                double bob = 0.05 * Mth.sin(this.tickCount * 0.25F);
                return (this.flightY + bob - this.getY()) * 0.5;
            }
            case FALL -> {
                // touching the water (or ground) again: the flutter's over, and from here it floats like any boat. (Not
                // one more push down: vanilla has just set it on the surface, and pushed under it, a boat sinks)
                if (this.isInWater() || this.onGround()) {
                    this.flight = Flight.NONE;
                    this.rest = REST_TICKS;
                    return current;
                }
                this.fallSpeed = Math.min(FALL_SPEED, this.fallSpeed + 0.008);
                return -this.fallSpeed;
            }
            default -> {
                return current;
            }
        }
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && this.flight != Flight.NONE && this.isLocalInstanceAuthoritative()) {
            double dy = this.flightMotion(delta.y);
            delta = new Vec3(delta.x, dy, delta.z);
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, dy, motion.z);
        }
        super.move(type, delta);
    }

    @Override
    public void tick() {
        // its driver pressed jump: flutter up (on the driver's side, where the boat moves)
        if (this.isLocalInstanceAuthoritative() && this.getControllingPassenger() instanceof LivingEntity driver && driver.isJumping()) {
            this.flutter();
        }
        if (this.rest > 0) this.rest--;
        super.tick();
        // back down on the water (or ground) from a fall: the flutter's over, a short rest
        if (this.flight == Flight.FALL && (this.isInWater() || this.onGround())) {
            this.flight = Flight.NONE;
            this.rest = REST_TICKS;
        }
        boolean aloft = !this.isInWater() && !this.onGround() || this.flight == Flight.RISE || this.flight == Flight.HOLD;
        this.wingSpreadO = this.wingSpread;
        this.wingSpread += ((aloft ? 1.0F : 0.0F) - this.wingSpread) * (aloft ? 0.25F : 0.08F);
        if (!this.level().isClientSide()) return;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        // petals drifting down off the arbor and the lotus; a firefly or two about it
        if (this.random.nextFloat() < 0.12F) {
            double along = (this.random.nextDouble() - 0.4) * 2.2;
            double across = (this.random.nextDouble() - 0.5) * 0.8;
            this.level().addParticle(ParticleTypes.CHERRY_LEAVES, this.getX() + fx * along + fz * across, this.getY() + 1.4 + this.random.nextDouble() * 0.6,
                this.getZ() + fz * along - fx * across, 0.0, 0.0, 0.0);
        }
        if (this.random.nextFloat() < 0.04F) {
            this.level().addParticle(ParticleTypes.FIREFLY, this.getX() + (this.random.nextDouble() - 0.5) * 3, this.getY() + 0.8 + this.random.nextDouble(),
                this.getZ() + (this.random.nextDouble() - 0.5) * 3, 0.0, 0.0, 0.0);
        }
        // in the air: a trail of sparkles and petals under it, glints off the wings
        if (aloft) {
            for (int i = 0; i < 2; i++) {
                double along = (this.random.nextDouble() - 0.5) * 2.0;
                double across = (this.random.nextDouble() - 0.5) * 1.2;
                this.level().addParticle(i == 0 ? ParticleTypes.END_ROD : ParticleTypes.FALLING_SPORE_BLOSSOM, this.getX() + fx * along + fz * across,
                    this.getY() - 0.1, this.getZ() + fz * along - fx * across, 0.0, -0.03, 0.0);
            }
            if (this.random.nextFloat() < 0.3F) {
                double side = this.random.nextBoolean() ? 1 : -1;
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX() + fz * side * 1.2, this.getY() + 0.5, this.getZ() - fx * side * 1.2, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.25;
        for (int side = -1; side <= 1; side += 2) {
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 1.3 + fz * side * 0.4, y,
                this.getZ() + fz * 1.3 - fx * side * 0.4, fz * side * 0.1, 0.08, -fx * side * 0.1);
        }
        if (this.random.nextFloat() < 0.4F) {
            this.level().addParticle(ParticleTypes.CHERRY_LEAVES, this.getX() - fx * 1.4, y + 0.1, this.getZ() - fz * 1.4, 0.0, 0.0, 0.0);
        }
    }

    // no fall damage, no breaking up on landing from a flutter
    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState onState, net.minecraft.core.BlockPos pos) {
        if (this.flight != Flight.NONE) {
            this.resetFallDistance();
            return;
        }
        super.checkFallDamage(ya, onGround, onState, pos);
    }
}

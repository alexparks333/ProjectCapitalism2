package com.alexh.capitalism.boats;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

// The Clockwork Submersible (Boats.md, client/SubmersibleModel): a waxed oxidized copper submarine riding half under
// the surface. Its riders sit down in the glass dome at the front, under the water: they aren't thrown out for being
// under water, and while aboard they can breathe (Water Breathing, no particles). A great wooden
// propeller turns at the stern, the periscope sweeps round, and bubbles churn up behind it all the time. Seats one, in
// the middle of the dome. Its rider presses F (swap hands) with an empty hand to turn its flashlight on or off: a beam
// shines a cone of light where they look, really lighting up what it falls on (beamLights).
public class Submersible extends PremiumBoat {

    private static final float[][] SEATS = {{12.0F, 0.0F, 25.0F}};   // in the middle of the dome, under the water
    private static final float[][] LIGHTS = {{-8.0F, 0.0F, -22.0F, 5}};
    // the flashlight: the lamp on the dome's nose (model pixels), how far round it turns to follow its rider's look
    // (degrees), how far its beam reaches (blocks) and how bright the spot is where it lands
    public static final float LAMP_X = 27.8F;
    public static final float LAMP_Y = 15.0F;
    public static final float MAX_YAW = 70.0F;
    public static final float MAX_PITCH = 60.0F;
    private static final double REACH = 32.0;
    // the cone: its half-angle (degrees); its rings of rays {share of the half-angle, how many}; how bright it lights
    // what it lands on, close up and at its reach
    public static final float CONE_DEG = 20.0F;
    private static final float[][] RINGS = {{0.0F, 1}, {0.35F, 6}, {0.7F, 10}, {1.0F, 14}};
    private static final double SPOT_NEAR = 15;
    private static final double SPOT_FAR = 11;
    // the light holds its aim until it turns more than this (degrees), and where it casts from until the lamp moves
    private static final float HOLD_DEG = 3.0F;
    private float heldYaw = Float.NaN;
    private float heldPitch = Float.NaN;
    private net.minecraft.core.BlockPos heldFrom;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> FLASHLIGHT =
        net.minecraft.network.syncher.SynchedEntityData.defineId(Submersible.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final double STERN_X = -26.0 / 16.0;

    public Submersible(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.SUBMERSIBLE.get());
    }

    @Override
    protected float[][] seats() {
        return SEATS;
    }

    @Override
    protected float[][] lights() {
        return LIGHTS;
    }

    // Where the flashlight points, relative to the boat: {yaw (+ to the right), pitch (+ down)} in degrees, following
    // its rider's look as far as the lamp turns (straight ahead with nobody aboard)
    // where the lamp points with nobody aboard (the tests; straight ahead if not set)
    private float[] fixedAim;

    public void aimLamp(float yaw, float pitch) {
        this.fixedAim = new float[]{yaw, pitch};
    }

    public float[] lampAim(float partialTicks) {
        Entity rider = this.getFirstPassenger();
        if (rider == null) return this.fixedAim != null ? this.fixedAim : new float[]{0.0F, 0.0F};
        float yaw = Mth.clamp(Mth.wrapDegrees(rider.getViewYRot(partialTicks) - this.getYRot(partialTicks)), -MAX_YAW, MAX_YAW);
        float pitch = Mth.clamp(rider.getViewXRot(partialTicks), -MAX_PITCH, MAX_PITCH);
        return new float[]{yaw, pitch};
    }

    // the beam: from the lamp, which way (a unit vector in the world)
    public net.minecraft.world.phys.Vec3 lampFrom() {
        return this.modelPoint(LAMP_X, 0.0F, LAMP_Y);
    }

    public net.minecraft.world.phys.Vec3 lampDirection(float partialTicks) {
        float[] aim = this.lampAim(partialTicks);
        return net.minecraft.world.phys.Vec3.directionFromRotation(aim[1], this.getYRot(partialTicks) + aim[0]);
    }

    // Where the beam lands: the water (or air) just in front of the first block the middle of the beam hits, within
    // its reach; null if it reaches nothing
    public net.minecraft.core.BlockPos beamSpot() {
        net.minecraft.world.phys.Vec3 from = this.lampFrom();
        net.minecraft.world.phys.Vec3 dir = this.lampDirection(1.0F);
        net.minecraft.world.phys.BlockHitResult hit = this.cast(from, dir);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return null;
        return this.placeable(hit.getLocation(), dir);
    }

    private net.minecraft.world.phys.BlockHitResult cast(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 dir) {
        return this.level().clip(new net.minecraft.world.level.ClipContext(from, from.add(dir.scale(REACH)),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
    }

    // a block a light can go in just before this point, coming along dir: water (a source) or air, stepping back past
    // seagrass, kelp and the like; null if there's none close
    private net.minecraft.core.BlockPos placeable(net.minecraft.world.phys.Vec3 at, net.minecraft.world.phys.Vec3 dir) {
        for (int i = 0; i < 5; i++) {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(at.subtract(dir.scale(0.3 + 0.6 * i)));
            net.minecraft.world.level.block.state.BlockState here = this.level().getBlockState(pos);
            if (here.isAir() || here.is(net.minecraft.world.level.block.Blocks.WATER) && here.getFluidState().isSource()
                || here.is(net.minecraft.world.level.block.Blocks.LIGHT)) return pos;
        }
        return null;
    }

    // The flashlight's light: a cone (CONE_DEG either side of where it points) of rays, rings of them round the middle
    // one. Where each lands on something (within REACH) it lights it, brighter the closer (SPOT_NEAR to SPOT_FAR), so
    // whatever's in the cone is lit and the rest stays dark; a few fainter lights down the middle of the beam light
    // the water it passes through (fish, kelp, seagrass). Each block once, at its brightest: {x, y, z, level}
    public java.util.List<int[]> beamLights() {
        java.util.Map<net.minecraft.core.BlockPos, Integer> lights = new java.util.LinkedHashMap<>();
        if (!this.flashlightOn()) return java.util.List.of();
        // steady: the light holds its aim until it turns more than HOLD_DEG, and casts from the middle of the lamp's block
        // until the lamp has moved most of a block, so the boat bobbing and the rider's head twitching don't keep moving the
        // light blocks about (that flickers)
        float[] aim = this.lampAim(1.0F);
        float yaw = this.getYRot() + aim[0];
        if (Float.isNaN(this.heldYaw) || Math.abs(Mth.wrapDegrees(yaw - this.heldYaw)) > HOLD_DEG) this.heldYaw = yaw;
        if (Float.isNaN(this.heldPitch) || Math.abs(aim[1] - this.heldPitch) > HOLD_DEG) this.heldPitch = aim[1];
        net.minecraft.world.phys.Vec3 lamp = this.lampFrom();
        if (this.heldFrom == null || lamp.distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(this.heldFrom)) > 0.85) this.heldFrom = net.minecraft.core.BlockPos.containing(lamp);
        net.minecraft.world.phys.Vec3 from = net.minecraft.world.phys.Vec3.atCenterOf(this.heldFrom);
        net.minecraft.world.phys.Vec3 dir = net.minecraft.world.phys.Vec3.directionFromRotation(this.heldPitch, this.heldYaw);
        net.minecraft.world.phys.Vec3 up = Math.abs(dir.y) > 0.95 ? new net.minecraft.world.phys.Vec3(1, 0, 0) : new net.minecraft.world.phys.Vec3(0, 1, 0);
        net.minecraft.world.phys.Vec3 right = dir.cross(up).normalize();
        net.minecraft.world.phys.Vec3 side = right.cross(dir).normalize();
        double centre = REACH;
        for (int ring = 0; ring < RINGS.length; ring++) {
            double a = Math.toRadians(CONE_DEG * RINGS[ring][0]);
            int n = (int) RINGS[ring][1];
            for (int k = 0; k < n; k++) {
                double phi = 2 * Math.PI * k / n + ring * 0.4;
                net.minecraft.world.phys.Vec3 ray = dir.scale(Math.cos(a))
                    .add(right.scale(Math.cos(phi) * Math.sin(a))).add(side.scale(Math.sin(phi) * Math.sin(a)));
                net.minecraft.world.phys.BlockHitResult hit = this.cast(from, ray);
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) continue;
                double d = hit.getLocation().distanceTo(from);
                if (ring == 0) centre = d;
                net.minecraft.core.BlockPos pos = this.placeable(hit.getLocation(), ray);
                if (pos == null) continue;
                // (the outer rings a little dimmer, so the cone fades off at its edge)
                int level = (int) Math.round(Mth.clamp(SPOT_NEAR - (SPOT_NEAR - SPOT_FAR) * d / REACH, SPOT_FAR, SPOT_NEAR) - 2.5 * RINGS[ring][0] * RINGS[ring][0]);
                lights.merge(pos, level, Math::max);
            }
        }
        // down the middle of the beam, short of where it lands
        for (double s = 3.0; s < Math.min(centre - 1.5, REACH); s += 4.0) {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(from.add(dir.scale(s)));
            net.minecraft.world.level.block.state.BlockState here = this.level().getBlockState(pos);
            // (its own light from last time counts as free: else it would put it there, then see it was in the way, take it
            // away, then put it back... flickering)
            if (!(here.isAir() || here.is(net.minecraft.world.level.block.Blocks.WATER) && here.getFluidState().isSource()
                || here.is(net.minecraft.world.level.block.Blocks.LIGHT))) continue;
            lights.merge(pos, Math.max(5, 9 - (int) (s / 6)), Math::max);
        }
        java.util.List<int[]> out = new java.util.ArrayList<>();
        lights.forEach((p, l) -> out.add(new int[]{p.getX(), p.getY(), p.getZ(), l}));
        return out;
    }

    // The flashlight lights the cone ahead of it, like a torch in the dark: nothing round the boat itself
    @Override
    protected void extraLights(java.util.List<net.minecraft.core.BlockPos> where, java.util.List<Integer> levels) {
        for (int[] l : this.beamLights()) {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(l[0], l[1], l[2]);
            if (where.contains(pos)) continue;
            where.add(pos);
            levels.add(l[3]);
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLASHLIGHT, false);
    }

    public boolean flashlightOn() {
        return this.entityData.get(FLASHLIGHT);
    }

    // its rider presses F (swap hands) with an empty hand: the flashlight on / off (boats/BoatEvents)
    public void toggleFlashlight() {
        this.entityData.set(FLASHLIGHT, !this.flashlightOn());
    }

    // (the light under it doesn't count against the light on it: only its own lamp's brightest)
    @Override
    public int maxOwnLight() {
        return 5;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("flashlight", this.flashlightOn());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(FLASHLIGHT, input.getBooleanOr("flashlight", false));
    }

    // its riders stay aboard under the water
    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean canBeRiddenUnderFluidType(net.neoforged.neoforge.fluids.FluidType type, Entity rider) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel) {
            // breathe down in the dome. (Not Conduit Power: under water it works as Night Vision, lighting everything up,
            // and the deep should be dark but for the flashlight)
            if (this.tickCount % 20 == 0) {
                for (Entity passenger : this.getPassengers()) {
                    if (passenger instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
                    }
                }
            }
            return;
        }
        // bubbles churning up behind it, always
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        for (int i = 0; i < 2; i++) {
            double across = (this.random.nextDouble() - 0.5) * 0.6;
            this.level().addParticle(ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + fx * STERN_X + fz * across, this.getY() - 0.6,
                this.getZ() + fz * STERN_X - fx * across, 0.0, 0.1, 0.0);
        }
        // the flashlight on: specks of silt drifting in its beam
        if (this.flashlightOn() && this.random.nextFloat() < 0.5F) {
            net.minecraft.world.phys.Vec3 dir = this.lampDirection(1.0F);
            net.minecraft.world.phys.Vec3 at = this.lampFrom().add(dir.scale(1.0 + this.random.nextDouble() * 5.0))
                .add((this.random.nextDouble() - 0.5) * 0.6, (this.random.nextDouble() - 0.5) * 0.6, (this.random.nextDouble() - 0.5) * 0.6);
            this.level().addParticle(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
        if (this.random.nextFloat() < 0.3F) {
            this.level().addParticle(ParticleTypes.BUBBLE_POP, this.getX() + fx * STERN_X, this.getY() + 0.35, this.getZ() + fz * STERN_X, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void spray(double speed, double fx, double fz) {
        double y = this.getY() + 0.3;
        this.level().addParticle(ParticleTypes.SPLASH, this.getX() + fx * 1.4, y, this.getZ() + fz * 1.4, 0.0, 0.08, 0.0);
        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - fx * 1.7, y - 0.5, this.getZ() - fz * 1.7, -fx * 0.1, 0.0, -fz * 0.1);
    }
}

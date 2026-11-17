package com.alexh.capitalism.boats;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

// A Premium boat (Boats.md: the Pirate Ship, the Speedboat...): handles like a boat and is a boat's size on the water,
// but SPEED_BOOST faster, with its own seats (seats(): where each passenger sits on its model; the first one steers)
// and spray when it's going fast (spray()).
// Locked to whoever puts it on the water, like a chest (economy/ContainerLocks): nobody else can get in or break it
// (admins can). The owner shift + right-clicks it with an empty hand to unlock it (anyone can ride) or lock it again.
// Its lanterns, fires and lit windows really light up what's round it: it carries invisible light blocks along
// (lights(): where, on its model, and how bright), put only into empty air and taken away again as it moves on, when
// it's broken or picked up, or when its chunk unloads (and any left behind by a crash are cleared when it loads).
public abstract class PremiumBoat extends Boat {

    // How much faster than a boat: the forward push is this much stronger, so its top speed is too
    public static final double SPEED_BOOST = 0.10;
    // A boat's forward push each tick while you hold forward (AbstractBoat.controlBoat)
    private static final double BOAT_PUSH = 0.04;
    // model y = 0 is this far above the boat's position (AbstractBoatRenderer)
    private static final double MODEL_ORIGIN = 0.375;

    private boolean forward;
    // the owner (their UUID as text, "" for nobody), their name, and whether it's locked to them
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> OWNER =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PremiumBoat.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> OWNER_NAME =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PremiumBoat.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> LOCKED =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PremiumBoat.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    protected PremiumBoat(EntityType<? extends Boat> type, Level level, Supplier<Item> dropItem) {
        super(type, level, dropItem);
    }

    // ── its light ──

    // the light blocks it has put down (server side), and any saved from before it was unloaded, to clear
    private final java.util.List<net.minecraft.core.BlockPos> placedLights = new java.util.ArrayList<>();

    // Where it gives light, in model pixels {forward, left, up (model y: up is negative), brightness 1-15}
    protected float[][] lights() {
        return new float[0][];
    }

    // where each of its lights is now
    private net.minecraft.core.BlockPos lightPos(float[] light) {
        return net.minecraft.core.BlockPos.containing(this.modelPoint(light[0], light[1], light[2]));
    }

    // a point on its model (model pixels: forward, left, model y with up negative) in the world, as it's turned now
    public Vec3 modelPoint(float forward, float left, float y) {
        return this.position().add(new Vec3(left / 16.0, MODEL_ORIGIN - y / 16.0, forward / 16.0).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
    }

    // Lights it puts down that aren't fixed to its model (the Submersible's flashlight: where its beam lands), added to
    // these lists (each block once, its brightness)
    protected void extraLights(java.util.List<net.minecraft.core.BlockPos> where, java.util.List<Integer> levels) {
    }

    // How much of the block light at this spot is its own (its lights fade a level a block, like any light): the
    // renderer leaves that out, so the ship itself stays dark at night and only its glowing parts shine
    public int ownLightAt(net.minecraft.core.BlockPos pos) {
        int own = 0;
        for (float[] light : this.lights()) {
            own = Math.max(own, (int) light[3] - pos.distManhattan(this.lightPos(light)));
        }
        return own;
    }

    // Puts its light where it is now, clearing where it was (every other tick)
    private void updateLights(net.minecraft.server.level.ServerLevel level) {
        java.util.List<net.minecraft.core.BlockPos> wanted = new java.util.ArrayList<>();
        java.util.List<Integer> levels = new java.util.ArrayList<>();
        if (this.isAlive()) {
            for (float[] light : this.lights()) {
                net.minecraft.core.BlockPos pos = this.lightPos(light);
                if (!wanted.contains(pos)) {
                    wanted.add(pos);
                    levels.add((int) light[3]);
                }
            }
            this.extraLights(wanted, levels);
            // sticky: where a light is wanted right next to one it already has (and that one isn't wanted itself),
            // keep that one instead of moving it a block, so small shifts don't keep re-lighting (flickering)
            for (int i = 0; i < wanted.size(); i++) {
                net.minecraft.core.BlockPos pos = wanted.get(i);
                if (this.placedLights.contains(pos)) continue;
                for (net.minecraft.core.BlockPos old : this.placedLights) {
                    if (old.distManhattan(pos) <= 1 && !wanted.contains(old)) {
                        wanted.set(i, old);
                        break;
                    }
                }
            }
        }
        // the new lights first, then the old ones go (so there's never a moment with none)
        for (int i = 0; i < wanted.size(); i++) {
            net.minecraft.core.BlockPos pos = wanted.get(i);
            if (this.placedLights.contains(pos) || !level.isLoaded(pos)) continue;
            net.minecraft.world.level.block.state.BlockState here = level.getBlockState(pos);
            boolean water = here.is(net.minecraft.world.level.block.Blocks.WATER) && here.getFluidState().isSource();
            if (!here.isAir() && !water) continue;
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, levels.get(i))
                .setValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED, water), 3);
            this.placedLights.add(pos);
        }
        for (java.util.Iterator<net.minecraft.core.BlockPos> it = this.placedLights.iterator(); it.hasNext(); ) {
            net.minecraft.core.BlockPos pos = it.next();
            if (wanted.contains(pos)) continue;
            clearLight(level, pos);
            it.remove();
        }
    }

    // takes a light away again: back to air, or back to water if it was put in water
    private static void clearLight(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!level.isLoaded(pos)) return;
        net.minecraft.world.level.block.state.BlockState here = level.getBlockState(pos);
        if (!here.is(net.minecraft.world.level.block.Blocks.LIGHT)) return;
        level.setBlock(pos, here.getValue(net.minecraft.world.level.block.LightBlock.WATERLOGGED)
            ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    // the brightest of its own lights (the renderer takes this much off the light on the boat itself)
    public int maxOwnLight() {
        int max = 0;
        for (float[] light : this.lights()) max = Math.max(max, (int) light[3]);
        return max;
    }

    // its light blocks, for the tests
    public java.util.List<net.minecraft.core.BlockPos> placedLights() {
        return java.util.List.copyOf(this.placedLights);
    }

    // updates its light now (the tests; it does this itself every other tick)
    public void refreshLights() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level) this.updateLights(level);
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (net.minecraft.core.BlockPos pos : this.placedLights) clearLight(level, pos);
            this.placedLights.clear();
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, "");
        builder.define(OWNER_NAME, "");
        builder.define(LOCKED, true);
    }

    // ── the lock ──

    public void setOwner(Player player) {
        this.entityData.set(OWNER, player.getUUID().toString());
        this.entityData.set(OWNER_NAME, player.getName().getString());
        this.entityData.set(LOCKED, true);
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().toString().equals(this.entityData.get(OWNER));
    }

    public boolean isLocked() {
        return this.entityData.get(LOCKED) && !this.entityData.get(OWNER).isEmpty();
    }

    public String ownerName() {
        return this.entityData.get(OWNER_NAME);
    }

    // May this player get in / break it?
    public boolean allows(Player player) {
        return !this.isLocked() || this.isOwnedBy(player)
            || player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand, Vec3 location) {
        // the owner, sneaking with an empty hand: lock / unlock
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide()) {
                boolean locked = !this.entityData.get(LOCKED);
                this.entityData.set(LOCKED, locked);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(locked ? "Locked: only you can ride it."
                    : "Unlocked: anyone can ride it now.").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (!this.allows(player)) {
            if (!this.level().isClientSide()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Locked: this is " + this.ownerName() + "'s "
                    + this.getName().getString() + ".").withStyle(net.minecraft.ChatFormatting.RED));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    // A lava boat (LavaBoat) floats on lava as on water: NeoForge asks every boat which fluids it can float in
    @Override
    public boolean canBoatInFluid(net.minecraft.world.level.material.FluidState fluid) {
        if (this instanceof LavaBoat && fluid.is(net.minecraft.tags.FluidTags.LAVA)) return true;
        return super.canBoatInFluid(fluid);
    }

    @Override
    public boolean canBoatInFluid(net.neoforged.neoforge.fluids.FluidType type) {
        if (this instanceof LavaBoat && type == net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value()) return true;
        return super.canBoatInFluid(type);
    }

    // nor tie a lead to it and tow it away
    @Override
    public boolean canBeLeashed() {
        return !this.isLocked();
    }

    // nobody but its owner (or an admin) can break it while it's locked: not by hitting it, nor with arrows
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (this.isLocked() && source.getEntity() instanceof Player player && !this.allows(player)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("You can't break " + this.ownerName() + "'s "
                + this.getName().getString() + ".").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        if (this.isLocked() && source.getEntity() != null && !(source.getEntity() instanceof Player)) return false;
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("owner", this.entityData.get(OWNER));
        output.putString("owner_name", this.entityData.get(OWNER_NAME));
        output.putBoolean("locked", this.entityData.get(LOCKED));
        // its light blocks, so they're cleared if it's loaded again without having taken them away (a crash)
        output.store("lights", net.minecraft.core.BlockPos.CODEC.listOf(), java.util.List.copyOf(this.placedLights));
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(OWNER, input.getStringOr("owner", ""));
        this.entityData.set(OWNER_NAME, input.getStringOr("owner_name", ""));
        this.entityData.set(LOCKED, input.getBooleanOr("locked", true));
        this.placedLights.clear();
        input.read("lights", net.minecraft.core.BlockPos.CODEC.listOf()).ifPresent(this.placedLights::addAll);
    }

    // Each seat in model pixels: forward (+ toward the bow), left (+ port), the seat's height (model y: up is negative)
    protected abstract float[][] seats();

    // Spray and wake, client side, when it's going faster than 0.12 blocks a tick
    protected abstract void spray(double speed, double fx, double fz);

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        super.setInput(left, right, up, down);
        this.forward = up && !down;
    }

    // The boat moves itself each tick by its motion (AbstractBoat.tick, on whoever steers it); with forward held, the
    // push is SPEED_BOOST stronger than a boat's. The extra stays in its motion, so its top speed is 10% higher.
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && this.forward && this.isLocalInstanceAuthoritative() && this.getControllingPassenger() instanceof Player) {
            Vec3 extra = extraPush(this.getYRot());
            delta = delta.add(extra);
            this.setDeltaMovement(this.getDeltaMovement().add(extra));
        }
        super.move(type, delta);
    }

    // The extra forward push for a boat facing this way (public for the tests)
    public static Vec3 extraPush(float yRot) {
        double push = BOAT_PUSH * SPEED_BOOST;
        return new Vec3(Mth.sin(-yRot * Mth.DEG_TO_RAD) * push, 0.0, Mth.cos(yRot * Mth.DEG_TO_RAD) * push);
    }

    @Override
    protected int getMaxPassengers() {
        return this.seats().length;
    }

    // How far forward (model pixels) passenger number `index` sits (the renderer hides what's in front of it)
    public float seatForward(int index) {
        float[][] seats = this.seats();
        return seats[Math.max(0, Math.min(seats.length - 1, index))][0];
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float[][] seats = this.seats();
        float[] seat = seats[Math.max(0, Math.min(seats.length - 1, this.getPassengers().indexOf(passenger)))];
        return new Vec3(seat[1] / 16.0, MODEL_ORIGIN - seat[2] / 16.0, seat[0] / 16.0).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof net.minecraft.server.level.ServerLevel level && this.tickCount % 2 == 0) this.updateLights(level);
        // a lava boat's riders don't burn while they're aboard
        if (this instanceof LavaBoat && !this.level().isClientSide() && this.tickCount % 20 == 0) {
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
                    living.clearFire();
                }
            }
        }
        if (!this.level().isClientSide() || !(this.isInWater() || (this instanceof LavaBoat && this.isInLava()))) return;
        double speed = this.getDeltaMovement().horizontalDistance();
        if (speed < 0.12) return;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        this.spray(speed, -Mth.sin(yaw), Mth.cos(yaw));
    }
}

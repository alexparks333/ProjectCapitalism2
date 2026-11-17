package com.alexh.capitalism.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

// An invisible seat for sitting on furniture (the Crimson Throne; any block can use it: sit()). The player rides it, sat
// where the seat is, facing the way the furniture faces; sneaking gets you up (vanilla's dismount) in front of it. It
// goes away as soon as nobody's on it or its block is gone. Drawn by nothing (client: NoopRenderer).
public class DecorSeat extends Entity {

    private BlockPos block = BlockPos.ZERO;
    private Direction front = Direction.NORTH;
    // whether its block is still the furniture (checked each second)
    private Predicate<net.minecraft.world.level.block.state.BlockState> stillThere = s -> !s.isAir();

    public DecorSeat(EntityType<? extends DecorSeat> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // Sits `player` on the furniture at `pos`: their seat at `seat` (where they sit, in the world), facing `front`. False if
    // someone is sitting there already.
    public static boolean sit(Level level, BlockPos pos, Player player, Vec3 seat, Direction front) {
        if (!(level instanceof ServerLevel server) || player.isPassenger() || player.isSleeping()) return false;
        for (DecorSeat other : server.getEntitiesOfClass(DecorSeat.class, new AABB(pos).inflate(0.5))) {
            if (other.block.equals(pos)) {
                if (other.isVehicle()) return false;
                other.discard();
            }
        }
        DecorSeat s = new DecorSeat(com.alexh.capitalism.core.ModEntities.DECOR_SEAT.get(), level);
        s.block = pos.immutable();
        s.front = front;
        net.minecraft.world.level.block.Block here = level.getBlockState(pos).getBlock();
        s.stillThere = state -> state.is(here);
        s.snapTo(seat.x, seat.y, seat.z, front.toYRot(), 0.0F);
        server.addFreshEntity(s);
        if (!player.startRiding(s, true, true)) {
            s.discard();
            return false;
        }
        player.setYRot(front.toYRot());
        player.setYHeadRot(front.toYRot());
        // the Crimson Throne: a title floats over its lord (this seat's name tag: the seat stands as tall as a sitter, so
        // it shows just above their own name), and the throne knows it's taken (OPEN: the bats circle at night)
        BlockState there = level.getBlockState(pos);
        if (there.getBlock() instanceof BloodDecorBlock b && b.kind() == BloodDecorKind.CRIMSON_THRONE) {
            s.setCustomName(Component.literal("👑 Lord of the Manor").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            s.setCustomNameVisible(true);
            if (there.hasProperty(BloodDecorBlock.OPEN)) level.setBlock(pos, there.setValue(BloodDecorBlock.OPEN, true), 3);
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (!this.isVehicle() || (this.tickCount % 20 == 0 && !this.stillThere.test(this.level().getBlockState(this.block)))) {
            this.ejectPassengers();
            this.discard();
            // the throne is free again
            BlockState there = this.level().getBlockState(this.block);
            if (there.getBlock() instanceof BloodDecorBlock && there.getValue(BloodDecorBlock.OPEN)) {
                this.level().setBlock(this.block, there.setValue(BloodDecorBlock.OPEN, false), 3);
            }
        }
    }

    // the rider sits right on the seat
    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position();
    }

    // getting up: in front of the furniture
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 out = Vec3.atBottomCenterOf(this.block.relative(this.front));
        return this.level().noCollision(passenger, passenger.getBoundingBox().move(out.subtract(passenger.position()))) ? out
            : Vec3.atBottomCenterOf(this.block.above());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    // never saved: a seat with nobody on it is gone
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}

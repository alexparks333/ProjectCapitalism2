package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A placed Meat Slab's timer and lean (Freshness): taken from the item when it's placed and given back to it when it's
// broken (the loot table copies them). It keeps rotting while it sits there: once its time is up the block goes rotten
// (MeatSlabBlock.ROTTEN), and breaking it then gives Rotten Meat. Set on ice (#minecraft:ice: Ice, Packed, Blue or
// Frosted Ice) it rots like in a Freezer, ICE_SLOWER times slower: when it goes onto the ice it notes when (iceSince)
// and how long it had left (iceLeft), and its time runs down from those, a sixth as fast, so it's exact (unloaded too)
// and the client only hears of it when it goes on or off the ice. Synced to the client for its look-at card
// (client/MeatSlabHud).
public class MeatSlabBlockEntity extends BlockEntity {

    public static final int ICE_SLOWER = ColdStorageBlockEntity.Kind.FREEZER.slower;

    private long rotsAt = -1;
    private int lean = Freshness.LEAN_MAX;
    // on ice since this tick (-1: not on ice), with this long left then
    private long iceSince = -1;
    private long iceLeft;
    private boolean synced;

    public MeatSlabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEAT_SLAB.get(), pos, state);
    }

    public int lean() {
        return this.lean;
    }

    public boolean onIce() {
        return this.iceSince >= 0;
    }

    public boolean dated() {
        return this.rotsAt >= 0;
    }

    // Ticks left at the normal rate
    public long left(long now) {
        if (this.rotsAt < 0) return -1;
        if (this.iceSince >= 0) return Math.max(0, this.iceLeft - Math.max(0, now - this.iceSince) / ICE_SLOWER);
        return Math.max(0, this.rotsAt - now);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MeatSlabBlockEntity entity) {
        if (state.getValue(MeatSlabBlock.ROTTEN) || level.getGameTime() % 20 != Math.floorMod(pos.hashCode(), 20)) return;
        entity.update(level, pos, state, level.getGameTime());
    }

    // Once a second: its timer if it had none, onto or off the ice, rotten when its time is up (public for the tests)
    public void update(ServerLevel level, BlockPos pos, BlockState state, long now) {
        MeatSlabBlockEntity entity = this;
        boolean changed = !entity.synced;
        if (entity.rotsAt < 0 && state.getBlock() instanceof MeatSlabBlock slab) {
            // placed from a slab with no timer yet
            entity.rotsAt = now + Freshness.shelfLife(slab.cut);
            entity.lean = Freshness.rollLean(level.getRandom());
            changed = true;
        }
        boolean ice = level.getBlockState(pos.below()).is(BlockTags.ICE);
        if (ice && entity.iceSince < 0) {
            entity.iceLeft = entity.rotsAt - now;
            entity.iceSince = now;
            changed = true;
        } else if (!ice && entity.iceSince >= 0) {
            entity.rotsAt = now + entity.left(now);
            entity.iceSince = -1;
            changed = true;
        }
        if (changed) {
            entity.synced = true;
            entity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (entity.left(now) <= 0) level.setBlock(pos, state.setValue(MeatSlabBlock.ROTTEN, true), 3);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.rotsAt = components.getOrDefault(ModComponents.ROTS_AT.get(), -1L);
        this.lean = components.getOrDefault(ModComponents.LEAN.get(), Freshness.LEAN_MAX);
        this.iceSince = -1;
    }

    // Back onto the slab when it's broken: the time it has left from now
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.rotsAt >= 0) {
            long now = this.level != null ? this.level.getGameTime() : 0;
            components.set(ModComponents.ROTS_AT.get(), this.level != null ? now + this.left(now) : this.rotsAt);
        }
        components.set(ModComponents.LEAN.get(), this.lean);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.rotsAt = input.getLongOr("rots_at", -1L);
        this.lean = input.getIntOr("lean", Freshness.LEAN_MAX);
        this.iceSince = input.getLongOr("ice_since", -1L);
        this.iceLeft = input.getLongOr("ice_left", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("rots_at", this.rotsAt);
        output.putInt("lean", this.lean);
        output.putLong("ice_since", this.iceSince);
        output.putLong("ice_left", this.iceLeft);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}

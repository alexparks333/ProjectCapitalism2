package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// What a Fish Plaque holds (fishing/FishPlaqueBlock): which catch, and its story (CatchInfo). Taken from the plaque item
// when it's placed and put back on it when it's broken (implicit components), sent to the client for
// client/FishPlaqueRenderer.
public class FishPlaqueBlockEntity extends BlockEntity {

    private String catchId = "";
    private @Nullable CatchInfo info;

    public FishPlaqueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISH_PLAQUE.get(), pos, state);
    }

    public @Nullable RareCatch kind() {
        return RareCatch.byId(this.catchId);
    }

    public @Nullable CatchInfo info() {
        return this.info;
    }

    public void set(RareCatch kind, @Nullable CatchInfo info) {
        this.catchId = kind.id;
        this.info = info;
        this.setChanged();
    }

    // the catch itself, as an item (for drawing it)
    public ItemStack catchStack() {
        RareCatch kind = this.kind();
        if (kind == null) return ItemStack.EMPTY;
        var item = com.alexh.capitalism.core.ModItems.RARE_CATCHES.get(kind);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.catchId = components.getOrDefault(ModComponents.PLAQUE_CATCH.get(), "");
        this.info = components.get(ModComponents.CATCH_INFO.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.catchId.isEmpty()) components.set(ModComponents.PLAQUE_CATCH.get(), this.catchId);
        if (this.info != null) components.set(ModComponents.CATCH_INFO.get(), this.info);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.catchId = input.getStringOr("catch", "");
        this.info = input.read("info", CatchInfo.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("catch", this.catchId);
        if (this.info != null) output.store("info", CatchInfo.CODEC, this.info);
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

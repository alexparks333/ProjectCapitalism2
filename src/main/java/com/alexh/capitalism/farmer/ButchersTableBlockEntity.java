package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The Butcher's Table keeps its Butcher Knife (ButcherKnifeItem) in its knife slot between uses, so it's there next
// time; breaking the table drops it. (The slab slot keeps nothing: a slab comes back when the menu closes.)
public class ButchersTableBlockEntity extends BlockEntity {

    private final SimpleContainer knife = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ButchersTableBlockEntity.this.setChanged();
        }
    };

    public ButchersTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUTCHERS_TABLE.get(), pos, state);
    }

    public SimpleContainer knife() {
        return this.knife;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.knife);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.knife.getItems().clear();
        ContainerHelper.loadAllItems(input, this.knife.getItems());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.knife.getItems());
    }
}

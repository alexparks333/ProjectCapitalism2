package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.menu.AssemblyTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Keeps the ingredient slots of a Sushi Table / Cooking Table (and the Sushi Table's output slot), so a half-made
// dish stays on the table when you walk away. The Cooking Table's result is worked out by the menu, like a crafting
// table's.
public class AssemblyTableBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> items;

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_TABLE.get(), pos, state);
        this.items = NonNullList.withSize(kind().containerSize(), ItemStack.EMPTY);
    }

    public AssemblyTableBlock.Kind kind() {
        return this.getBlockState().getBlock() instanceof AssemblyTableBlock table ? table.kind() : AssemblyTableBlock.Kind.COOKING;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + "." + kind().id);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return AssemblyTableMenu.create(kind(), containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return kind().containerSize();
    }
}

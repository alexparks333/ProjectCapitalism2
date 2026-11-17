package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.menu.BartendingTableMenu;
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

// Keeps the 5 ingredient slots and the glass bottles, so a half-made drink stays on the counter when you walk away.
// The finished drink is worked out by the menu, like a crafting table's result.
public class BartendingTableBlockEntity extends BaseContainerBlockEntity {

    // 5 ingredient slots + 1 slot for Empty Glass Bottles
    public static final int SLOT_GLASS = BartenderRecipe.SLOTS;
    public static final int SIZE = BartenderRecipe.SLOTS + 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public BartendingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARTENDING_TABLE.get(), pos, state);
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
        return Component.translatable("container." + Capitalism.MODID + ".bartending_table");
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
        return new BartendingTableMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }
}

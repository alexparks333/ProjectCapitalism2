package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// What's in a Food Trough (FoodTroughBlock): 1, 2 or 5 slots of anything by its size (a plain container, so hoppers
// fill it). A pet takes one
// of its own foods at a time (takeFoodFor).
public class FoodTroughBlockEntity extends BlockEntity implements Container, MenuProvider {

    private NonNullList<ItemStack> items;

    public FoodTroughBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_TROUGH.get(), pos, state);
        this.items = NonNullList.withSize(size(state).slots, ItemStack.EMPTY);
    }

    private static FeederSize size(BlockState state) {
        return state.getBlock() instanceof FoodTroughBlock trough ? trough.size : FeederSize.SMALL;
    }

    // Is there anything this animal eats in here?
    public boolean hasFoodFor(Animal animal) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty() && PetTraits.eats(animal, stack)) return true;
        }
        return false;
    }

    // One of this animal's foods out of the trough (a copy of one item), or EMPTY
    public ItemStack takeFoodFor(Animal animal) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty() && PetTraits.eats(animal, stack)) {
                ItemStack one = stack.copyWithCount(1);
                stack.shrink(1);
                // a bucket of fish leaves its Water Bucket behind
                if (one.getItem() instanceof net.minecraft.world.item.MobBucketItem && stack.isEmpty()) {
                    this.items.set(this.items.indexOf(stack), new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
                }
                this.setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    // Keeps the heap of food showing when there's anything in it
    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            boolean filled = !this.isEmpty();
            if (state.hasProperty(FoodTroughBlock.FILLED) && state.getValue(FoodTroughBlock.FILLED) != filled) {
                this.level.setBlock(this.worldPosition, state.setValue(FoodTroughBlock.FILLED, filled), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(size(this.getBlockState()).title + " Food Trough");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FoodTroughMenu(containerId, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(size(this.getBlockState()).slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, count);
        if (!out.isEmpty()) this.setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}

package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.menu.JuicerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// 6 fruit slots + 1 bucket slot. When they match a JuicerRecipe it crushes for JUICER_TICKS, then uses up
// one fruit from each slot and turns the bucket into juice in place.
public class JuicerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int FRUIT_SLOTS = 6;
    public static final int SLOT_BUCKET = 6;

    private static final int[] SLOTS_FOR_UP = {0, 1, 2, 3, 4, 5};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_BUCKET};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_BUCKET};

    private NonNullList<ItemStack> items = NonNullList.withSize(7, ItemStack.EMPTY);
    private int progress;
    private int totalTime;

    // Synced to the open menu: progress for the arrow, and which juice (ordinal + 1, 0 = none) for its color.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> progress;
                case 1 -> totalTime;
                case 2 -> {
                    JuiceType shown = currentJuice();
                    yield shown == null ? 0 : shown.ordinal() + 1;
                }
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0 -> progress = value;
                case 1 -> totalTime = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public JuicerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JUICER.get(), pos, state);
    }

    // The juice being made right now, or the one sitting finished in the bucket slot.
    private @Nullable JuiceType currentJuice() {
        JuiceType done = JuicerRecipe.juiceOf(this.items.get(SLOT_BUCKET));
        return done != null ? done : JuicerRecipe.find(this.items);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, JuicerBlockEntity entity) {
        JuiceType recipe = JuicerRecipe.find(entity.items);
        boolean canWork = recipe != null;

        if (!canWork) {
            if (entity.progress != 0) {
                entity.progress = 0;
                setChanged(level, pos, state);
            }
        } else {
            entity.totalTime = CapitalismConfig.JUICER_TICKS.get();
            entity.progress++;

            if (entity.progress >= entity.totalTime) {
                entity.progress = 0;
                for (int i = 0; i < FRUIT_SLOTS; i++) {
                    entity.items.get(i).shrink(1);
                }
                entity.items.set(SLOT_BUCKET, new ItemStack(recipe.result()));
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            } else if (entity.progress % 20 == 0) {
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25F, 1.4F);
            }

            setChanged(level, pos, state);
        }

        entity.updateBlockVisuals(level, pos, state, canWork);
    }

    // Gears spin while working; the juice window fills up in that juice's color and stays full when done.
    private void updateBlockVisuals(ServerLevel level, BlockPos pos, BlockState state, boolean working) {
        JuiceType done = JuicerRecipe.juiceOf(this.items.get(SLOT_BUCKET));
        JuiceType juice = state.getValue(JuicerBlock.JUICE);
        int fill;
        if (done != null) {
            fill = JuicerBlock.MAX_FILL;
            juice = done;
        } else if (working && this.totalTime > 0) {
            fill = this.progress * JuicerBlock.MAX_FILL / this.totalTime;
            juice = JuicerRecipe.find(this.items);
        } else {
            fill = 0;
        }

        BlockState newState = state.setValue(JuicerBlock.WORKING, working).setValue(JuicerBlock.FILL, fill).setValue(JuicerBlock.JUICE, juice);
        if (newState != state) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
        this.totalTime = input.getIntOr("total_time", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
        output.putInt("total_time", this.totalTime);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + ".juicer");
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
        return new JuicerMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == SLOT_BUCKET ? JuicerRecipe.isContainer(itemStack) : JuicerRecipe.isFruit(itemStack);
    }

    // Hoppers: fruit in from the top, one empty bucket at a time from the sides, finished juice out the bottom.
    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        // One item per slot, same as by hand
        if (!this.items.get(slot).isEmpty()) return false;
        return this.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == SLOT_BUCKET && JuicerRecipe.juiceOf(itemStack) != null;
    }
}

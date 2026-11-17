package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.menu.PurificationTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Holds a water bucket and slowly turns it into a purified water bucket, burning coal/charcoal as fuel.
public class PurificationTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;

    // Like a furnace: water in from the top, fuel from the sides, finished buckets out the bottom.
    private static final int[] SLOTS_FOR_UP = {SLOT_INPUT};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_FUEL};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    private int totalTime;
    // Buckets left on the coal/charcoal already burned.
    private int fuelUses;

    // Synced to the open menu so the client can draw the progress arrow and fuel flame.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> progress;
                case 1 -> totalTime;
                case 2 -> fuelUses;
                case 3 -> CapitalismConfig.PURIFICATION_USES_PER_FUEL.get();
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0 -> progress = value;
                case 1 -> totalTime = value;
                case 2 -> fuelUses = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PurificationTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURIFICATION_TABLE.get(), pos, state);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ItemTags.COALS);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PurificationTableBlockEntity entity) {
        boolean hasJob = entity.items.get(SLOT_INPUT).is(Items.WATER_BUCKET) && entity.items.get(SLOT_OUTPUT).isEmpty();

        // Burn a new piece of fuel only when a job is waiting and the last piece is spent.
        if (hasJob && entity.fuelUses <= 0 && isFuel(entity.items.get(SLOT_FUEL))) {
            entity.items.get(SLOT_FUEL).shrink(1);
            entity.fuelUses = CapitalismConfig.PURIFICATION_USES_PER_FUEL.get();
            setChanged(level, pos, state);
        }

        boolean canWork = hasJob && entity.fuelUses > 0;

        if (!canWork) {
            if (entity.progress != 0) {
                entity.progress = 0;
                setChanged(level, pos, state);
            }
        } else {
            entity.totalTime = CapitalismConfig.PURIFICATION_TICKS.get();
            entity.progress++;

            if (entity.progress >= entity.totalTime) {
                entity.progress = 0;
                entity.fuelUses--;
                entity.items.set(SLOT_INPUT, ItemStack.EMPTY);
                entity.items.set(SLOT_OUTPUT, new ItemStack(ModItems.PURIFIED_WATER_BUCKET.get()));
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.2F);
            }

            setChanged(level, pos, state);
        }

        entity.updateBlockVisuals(level, pos, state);
    }

    // Tank shows clean water once a purified bucket is waiting, dirty water while a water bucket is in,
    // and the loading bar fills in MAX_PROGRESS steps (full when done).
    private void updateBlockVisuals(ServerLevel level, BlockPos pos, BlockState state) {
        TankState water;
        int bar;
        if (!this.items.get(SLOT_OUTPUT).isEmpty()) {
            water = TankState.CLEAN;
            bar = PurificationTableBlock.MAX_PROGRESS;
        } else if (this.items.get(SLOT_INPUT).is(Items.WATER_BUCKET)) {
            water = TankState.DIRTY;
            bar = this.totalTime > 0 ? this.progress * PurificationTableBlock.MAX_PROGRESS / this.totalTime : 0;
        } else {
            water = TankState.EMPTY;
            bar = 0;
        }

        if (state.getValue(PurificationTableBlock.WATER) != water || state.getValue(PurificationTableBlock.PROGRESS) != bar) {
            level.setBlock(pos, state.setValue(PurificationTableBlock.WATER, water).setValue(PurificationTableBlock.PROGRESS, bar), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
        this.totalTime = input.getIntOr("total_time", 0);
        this.fuelUses = input.getIntOr("fuel_uses", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
        output.putInt("total_time", this.totalTime);
        output.putInt("fuel_uses", this.fuelUses);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + ".purification_table");
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
        return new PurificationTableMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return switch (slot) {
            case SLOT_INPUT -> itemStack.is(Items.WATER_BUCKET);
            case SLOT_FUEL -> isFuel(itemStack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case DOWN -> SLOTS_FOR_DOWN;
            case UP -> SLOTS_FOR_UP;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }
}

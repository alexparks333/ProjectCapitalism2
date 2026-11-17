package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The Lava Creator (any smith can make one, anyone can use it): fill its tank with water (a Water Bucket in its water
// slot, or right-click the block with one), put Blaze Powder in, and an empty Bucket in the bottom slot. Each Blaze
// Powder + WATER_PER_BUCKET of water becomes a Refined Bucket of Lava (2 buckets of fuel in a smith table) after
// BREW_TICKS. Hoppers: water buckets and Blaze Powder in from the top, empty buckets from the sides, Refined Buckets
// (and the water slot's empty buckets) out the bottom.
public class LavaCreatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOT_WATER = 0;
    public static final int SLOT_BLAZE = 1;
    public static final int SLOT_BUCKET = 2;
    public static final int WATER_CAPACITY = 4000;
    public static final int WATER_PER_BUCKET = 500;
    public static final int BREW_TICKS = 1200;

    private static final int[] SLOTS_FOR_UP = {SLOT_WATER, SLOT_BLAZE};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_BUCKET};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_BUCKET, SLOT_WATER};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int water;
    private int progress;

    // Synced to the open menu: water in the tank, progress
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> water;
                case 1 -> progress;
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0 -> water = value;
                case 1 -> progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public LavaCreatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_CREATOR.get(), pos, state);
    }

    // A Water Bucket poured in (from the slot or a right-click): true if it fit
    public boolean pourWater() {
        if (this.water + 1000 > WATER_CAPACITY) return false;
        this.water += 1000;
        this.setChanged();
        return true;
    }

    private boolean canBrew() {
        return this.items.get(SLOT_BLAZE).is(Items.BLAZE_POWDER) && this.water >= WATER_PER_BUCKET
            && this.items.get(SLOT_BUCKET).is(Items.BUCKET) && this.items.get(SLOT_BUCKET).getCount() == 1;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LavaCreatorBlockEntity creator) {
        // A Water Bucket in the water slot: into the tank, the empty bucket stays in the slot
        if (creator.items.get(SLOT_WATER).is(Items.WATER_BUCKET) && creator.pourWater()) {
            creator.items.set(SLOT_WATER, new ItemStack(Items.BUCKET));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        boolean working = creator.canBrew();
        if (!working) {
            if (creator.progress != 0) {
                creator.progress = 0;
                setChanged(level, pos, state);
            }
        } else {
            creator.progress++;
            if (creator.progress >= BREW_TICKS) {
                creator.progress = 0;
                creator.items.get(SLOT_BLAZE).shrink(1);
                creator.water -= WATER_PER_BUCKET;
                creator.items.set(SLOT_BUCKET, new ItemStack(ModItems.REFINED_LAVA_BUCKET.get()));
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 0.8F);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            setChanged(level, pos, state);
        }
        if (state.getValue(LavaCreatorBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(LavaCreatorBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.water = Math.clamp(input.getIntOr("water", 0), 0, WATER_CAPACITY);
        this.progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("water", this.water);
        output.putInt("progress", this.progress);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + ".lava_creator");
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
        return new LavaCreatorMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_WATER -> stack.is(Items.WATER_BUCKET);
            case SLOT_BLAZE -> stack.is(Items.BLAZE_POWDER);
            case SLOT_BUCKET -> stack.is(Items.BUCKET) && this.items.get(SLOT_BUCKET).isEmpty();
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
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    // Out the bottom: finished Refined Buckets, and the empty buckets the water slot leaves behind
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return (slot == SLOT_BUCKET && stack.is(ModItems.REFINED_LAVA_BUCKET.get())) || (slot == SLOT_WATER && stack.is(Items.BUCKET));
    }
}

package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.menu.FermenterMenu;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Lives in the Fermenter's bottom half. 6 fermentable slots + sugar + bucket. When they match a LiquorType
// it ferments for FERMENTER_TICKS (burning 1 sugar every FERMENTER_SUGAR_TICKS; out of sugar = paused),
// then uses one of each fermentable and fills the bucket.
public class FermenterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int INGREDIENT_SLOTS = 6;
    public static final int SLOT_SUGAR = 6;
    public static final int SLOT_BUCKET = 7;

    private static final int[] SLOTS_FOR_UP = {0, 1, 2, 3, 4, 5};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_SUGAR, SLOT_BUCKET};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_BUCKET};

    private NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
    private int progress;
    private int totalTime;
    // Sugar burns like furnace fuel: one piece lasts FERMENTER_SUGAR_TICKS of fermenting.
    private int sugarTime;
    private int sugarTimeTotal;

    // Synced to the open menu: progress, total time, which liquor (ordinal + 1, 0 = none) for its color, sugar left.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> progress;
                case 1 -> totalTime;
                case 2 -> {
                    LiquorType shown = currentLiquor();
                    yield shown == null ? 0 : shown.ordinal() + 1;
                }
                case 3 -> sugarTime;
                case 4 -> sugarTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0 -> progress = value;
                case 1 -> totalTime = value;
                case 3 -> sugarTime = value;
                case 4 -> sugarTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FERMENTER.get(), pos, state);
    }

    private @Nullable LiquorType currentLiquor() {
        LiquorType done = FermenterRecipe.liquorOf(this.items.get(SLOT_BUCKET));
        return done != null ? done : FermenterRecipe.find(this.items);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FermenterBlockEntity entity) {
        LiquorType recipe = FermenterRecipe.find(entity.items);

        // Burn a new sugar when the last one is used up and there is something to ferment.
        if (recipe != null && entity.sugarTime <= 0 && FermenterRecipe.isSugar(entity.items.get(SLOT_SUGAR))) {
            entity.items.get(SLOT_SUGAR).shrink(1);
            entity.sugarTime = entity.sugarTimeTotal = CapitalismConfig.FERMENTER_SUGAR_TICKS.get();
            setChanged(level, pos, state);
        }

        boolean canWork = recipe != null && entity.sugarTime > 0;

        if (recipe == null) {
            // Ingredients or bucket taken out: start over.
            if (entity.progress != 0) {
                entity.progress = 0;
                setChanged(level, pos, state);
            }
        } else if (canWork) {
            entity.totalTime = CapitalismConfig.FERMENTER_TICKS.get();
            entity.sugarTime--;
            entity.progress++;

            if (entity.progress >= entity.totalTime) {
                entity.progress = 0;
                for (int i = 0; i < INGREDIENT_SLOTS; i++) {
                    entity.items.get(i).shrink(1);
                }
                entity.items.set(SLOT_BUCKET, new ItemStack(recipe.result()));
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.7F);
            } else if (entity.progress % 100 == 0) {
                level.playSound(null, pos.above(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.2F);
            }

            setChanged(level, pos, state);
        }

        entity.updateBlockVisuals(level, pos, state, canWork);
    }

    // The glass jug up top bubbles while fermenting, fills with the liquor's color, and stays full when done.
    private void updateBlockVisuals(ServerLevel level, BlockPos pos, BlockState state, boolean working) {
        LiquorType done = FermenterRecipe.liquorOf(this.items.get(SLOT_BUCKET));
        LiquorType liquor = state.getValue(FermenterBlock.LIQUOR);
        int fill;
        if (done != null) {
            fill = FermenterBlock.MAX_FILL;
            liquor = done;
        } else if ((working || this.progress > 0) && this.totalTime > 0) {
            // Also while paused for sugar: keep the liquid, just stop bubbling.
            fill = 1 + this.progress * (FermenterBlock.MAX_FILL - 1) / this.totalTime;
            liquor = FermenterRecipe.find(this.items);
        } else {
            fill = 0;
        }

        BlockState newState = state.setValue(FermenterBlock.WORKING, working).setValue(FermenterBlock.FILL, fill).setValue(FermenterBlock.LIQUOR, liquor);
        if (newState != state) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            BlockPos upper = pos.above();
            BlockState upperState = level.getBlockState(upper);
            if (upperState.is(state.getBlock())) {
                level.setBlock(upper, newState.setValue(FermenterBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
        this.totalTime = input.getIntOr("total_time", 0);
        this.sugarTime = input.getIntOr("sugar_time", 0);
        this.sugarTimeTotal = input.getIntOr("sugar_time_total", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
        output.putInt("total_time", this.totalTime);
        output.putInt("sugar_time", this.sugarTime);
        output.putInt("sugar_time_total", this.sugarTimeTotal);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + ".fermenter");
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
        return new FermenterMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return switch (slot) {
            case SLOT_SUGAR -> FermenterRecipe.isSugar(itemStack);
            case SLOT_BUCKET -> FermenterRecipe.isBucket(itemStack);
            default -> FermenterRecipe.isFermentable(itemStack);
        };
    }

    // Hoppers: fermentables in the top, sugar and empty buckets from the sides, finished liquor out the bottom.
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
        // One fermentable per slot and one bucket, same as by hand; sugar can stack.
        if (slot != SLOT_SUGAR && !this.items.get(slot).isEmpty()) return false;
        return this.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == SLOT_BUCKET && FermenterRecipe.liquorOf(itemStack) != null;
    }
}

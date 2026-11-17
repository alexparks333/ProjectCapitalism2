package com.alexh.capitalism.block;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.menu.OvenMenu;
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

// The Cook's Oven: 3 rows, each an uncooked dish -> a Gourmet meal (KitchenRecipes.OVEN), all sharing one fuel slot.
// Rows bake at the same time while the fire burns; a row pauses if its output slot is full.
// Each meal bakes at its own pace (KitchenRecipes.OVEN) and a soft bell dings when one comes out.
// Overcooking: a finished meal left in its output slot while the fire burns turns into Burnt Food after that meal's
// burn time (about half its cook time; the whole stack), so Cooks have to take meals out in time.
public class OvenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int ROWS = 3;
    public static final int SLOT_FUEL = ROWS * 2;
    public static final int SIZE = SLOT_FUEL + 1;
    // synced numbers: fire time, fire length, then progress / total / overcook for each row
    public static final int DATA_COUNT = 2 + ROWS * 3;

    // input slot of row r = r, output slot of row r = ROWS + r
    private static final int[] SLOTS_FOR_UP = {0, 1, 2};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_FUEL};
    private static final int[] SLOTS_FOR_DOWN = {3, 4, 5};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private final int[] progress = new int[ROWS];
    private final int[] total = new int[ROWS];
    // ticks each row's finished meal has been sitting in the heat (it burns at its recipe's burnSeconds)
    private final int[] overcook = new int[ROWS];

    // Synced to the open menu: fire, then progress/total for each row.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            if (id == 0) return litTime;
            if (id == 1) return litDuration;
            int row = (id - 2) / 3;
            return switch ((id - 2) % 3) {
                case 0 -> progress[row];
                case 1 -> total[row];
                default -> overcook[row];
            };
        }

        @Override
        public void set(int id, int value) {
            if (id == 0) litTime = value;
            else if (id == 1) litDuration = value;
            else if ((id - 2) % 3 == 0) progress[(id - 2) / 3] = value;
            else if ((id - 2) % 3 == 1) total[(id - 2) / 3] = value;
            else overcook[(id - 2) / 3] = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OVEN.get(), pos, state);
    }

    // How long one of these burns in the Oven (0 = not fuel).
    public static int burnTicks(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 1600;
        if (stack.is(Items.COAL_BLOCK)) return 16000;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        if (stack.is(ItemTags.LOGS_THAT_BURN) || stack.is(ItemTags.PLANKS)) return 300;
        if (stack.is(Items.STICK)) return 100;
        return 0;
    }

    // How long a finished meal can sit in the heat before it burns (never, for something that isn't an Oven meal).
    public static int burnTicks(KitchenRecipes.@Nullable Baking recipe) {
        return recipe == null ? Integer.MAX_VALUE : recipe.burnTicks();
    }

    private boolean canBake(int row) {
        KitchenRecipes.Baking recipe = KitchenRecipes.baking(this.items.get(row));
        if (recipe == null) return false;
        ItemStack out = this.items.get(ROWS + row);
        return out.isEmpty() || (out.is(recipe.result().get()) && out.getCount() < out.getMaxStackSize());
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, OvenBlockEntity oven) {
        boolean wasLit = oven.litTime > 0;
        boolean anyWork = false;
        for (int r = 0; r < ROWS; r++) anyWork |= oven.canBake(r);

        // Light a new piece of fuel when there's something to bake
        if (oven.litTime <= 0 && anyWork) {
            ItemStack fuel = oven.items.get(SLOT_FUEL);
            int burn = burnTicks(fuel);
            if (burn > 0) {
                oven.litTime = burn;
                oven.litDuration = burn;
                fuel.shrink(1);
            }
        }

        boolean lit = oven.litTime > 0;
        for (int r = 0; r < ROWS; r++) {
            KitchenRecipes.Baking recipe = KitchenRecipes.baking(oven.items.get(r));
            if (lit && recipe != null && oven.canBake(r)) {
                oven.total[r] = recipe.cookTicks();
                if (++oven.progress[r] >= oven.total[r]) {
                    oven.progress[r] = 0;
                    oven.items.get(r).shrink(1);
                    ItemStack out = oven.items.get(ROWS + r);
                    if (out.isEmpty()) oven.items.set(ROWS + r, new ItemStack(recipe.result().get()));
                    else out.grow(1);
                    // the meal is done: a soft oven-timer ding
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.35F, 1.5F);
                }
            } else if (recipe == null) {
                oven.progress[r] = 0;
            }
        }
        // Overcooking: a finished meal left in the heat burns
        for (int r = 0; r < ROWS; r++) {
            ItemStack out = oven.items.get(ROWS + r);
            if (out.isEmpty() || out.is(ModItems.BURNT_FOOD.get())) {
                oven.overcook[r] = 0;
            } else if (lit && ++oven.overcook[r] >= burnTicks(KitchenRecipes.bakedInto(out))) {
                oven.overcook[r] = 0;
                oven.items.set(ROWS + r, new ItemStack(ModItems.BURNT_FOOD.get(), out.getCount()));
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
        }
        if (lit) oven.litTime--;

        boolean nowLit = oven.litTime > 0;
        if (wasLit != nowLit || lit) {
            setChanged(level, pos, state);
        }
        if (state.getValue(OvenBlock.LIT) != nowLit) {
            level.setBlock(pos, state.setValue(OvenBlock.LIT, nowLit), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.litTime = input.getIntOr("lit_time", 0);
        this.litDuration = input.getIntOr("lit_duration", 0);
        for (int r = 0; r < ROWS; r++) {
            this.progress[r] = input.getIntOr("progress_" + r, 0);
            this.total[r] = input.getIntOr("total_" + r, 0);
            this.overcook[r] = input.getIntOr("overcook_" + r, 0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("lit_time", this.litTime);
        output.putInt("lit_duration", this.litDuration);
        for (int r = 0; r < ROWS; r++) {
            output.putInt("progress_" + r, this.progress[r]);
            output.putInt("total_" + r, this.total[r]);
            output.putInt("overcook_" + r, this.overcook[r]);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Capitalism.MODID + ".oven");
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
        return new OvenMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) return burnTicks(stack) > 0;
        return slot < ROWS;
    }

    // Hoppers: dishes in from the top, fuel from the sides, meals out the bottom.
    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= ROWS && slot < SLOT_FUEL;
    }
}

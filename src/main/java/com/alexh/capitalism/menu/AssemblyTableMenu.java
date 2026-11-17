package com.alexh.capitalism.menu;

import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.block.AssemblyTableBlock;
import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

// Sushi Table (4 slots in a row) / Cooking Table (5 slots around a plate).
//  - Cooking Table: the dish appears in the result slot as soon as the ingredients match; taking it uses them up.
//  - Sushi Table: nothing happens until the Cook plays the ROLL timing game (the screen times it and then presses
//    BUTTON_ROLL for a good roll or BUTTON_SQUASH for one held too long); the result goes into the table's own
//    output slot, so it's never lost.
public class AssemblyTableMenu extends AbstractContainerMenu {

    // Sent by the screen when the ROLL is let go in the green zone (sushi!) or too late (squashed).
    public static final int BUTTON_ROLL = 0;
    public static final int BUTTON_SQUASH = 1;

    // Item positions; they match textures/gui/container/sushi_table.png and cooking_table.png
    public static final int[][] SUSHI_SLOTS = {{17, 26}, {35, 26}, {53, 26}, {71, 26}};
    public static final int[][] COOKING_SLOTS = {{52, 16}, {71, 30}, {64, 52}, {40, 52}, {33, 30}};  // clockwise from the top middle
    public static final int RESULT_X = 127;
    public static final int SUSHI_RESULT_Y = 26;
    public static final int COOKING_RESULT_Y = 35;

    private final AssemblyTableBlock.Kind kind;
    private final Container table;
    private final ResultContainer result = new ResultContainer();
    private final Player player;
    private final int resultSlot;
    private final int invStart;

    public static AssemblyTableMenu create(AssemblyTableBlock.Kind kind, int containerId, Inventory inventory, Container table) {
        MenuType<AssemblyTableMenu> type = kind == AssemblyTableBlock.Kind.SUSHI ? ModMenus.SUSHI_TABLE.get() : ModMenus.COOKING_TABLE.get();
        return new AssemblyTableMenu(type, kind, containerId, inventory, table);
    }

    // Client-side constructors (MenuType factories)
    public static AssemblyTableMenu sushi(int containerId, Inventory inventory) {
        AssemblyTableBlock.Kind kind = AssemblyTableBlock.Kind.SUSHI;
        return create(kind, containerId, inventory, new SimpleContainer(kind.containerSize()));
    }

    public static AssemblyTableMenu cooking(int containerId, Inventory inventory) {
        AssemblyTableBlock.Kind kind = AssemblyTableBlock.Kind.COOKING;
        return create(kind, containerId, inventory, new SimpleContainer(kind.containerSize()));
    }

    private AssemblyTableMenu(MenuType<AssemblyTableMenu> type, AssemblyTableBlock.Kind kind, int containerId, Inventory inventory, Container table) {
        super(type, containerId);
        checkContainerSize(table, kind.containerSize());
        this.kind = kind;
        this.table = table;
        this.player = inventory.player;

        int[][] positions = kind == AssemblyTableBlock.Kind.SUSHI ? SUSHI_SLOTS : COOKING_SLOTS;
        for (int i = 0; i < kind.slots; i++) {
            this.addSlot(new Slot(table, i, positions[i][0], positions[i][1]));
        }
        this.resultSlot = kind.slots;
        this.invStart = this.resultSlot + 1;
        if (kind.rolled) {
            // the table's own output slot: take-only
            this.addSlot(new Slot(table, kind.slots, RESULT_X, SUSHI_RESULT_Y) {
                @Override
                public void onTake(Player taker, ItemStack taken) {
                    Discoveries.discover(taker, taken);
                    super.onTake(taker, taken);
                }

                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return false;
                }
            });
        } else {
            this.addSlot(new Slot(this.result, 0, RESULT_X, COOKING_RESULT_Y) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return false;
                }

                @Override
                public void onTake(Player taker, ItemStack taken) {
                Discoveries.discover(taker, taken);
                    consumeIngredients(taker);
                    super.onTake(taker, taken);
                }
            });
        }
        this.addStandardInventorySlots(inventory, 8, 84);
        this.updateResult();
    }

    public AssemblyTableBlock.Kind kind() {
        return this.kind;
    }

    // Runs every tick on the server while the menu is open, so the Cooking Table's result always matches the slots.
    @Override
    public void broadcastChanges() {
        if (!this.player.level().isClientSide()) {
            this.updateResult();
        }
        super.broadcastChanges();
    }

    private void updateResult() {
        if (this.kind.rolled) return;
        KitchenRecipes.Assembly recipe = KitchenRecipes.find(this.kind.recipes(), this.table, this.kind.slots);
        ItemStack wanted = recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.result().get(), recipe.count());
        if (!ItemStack.matches(wanted, this.result.getItem(0))) {
            this.result.setItem(0, wanted);
        }
    }

    // Sushi Table: true when the ingredients make something and the output slot has room for it (the ROLL button
    // only works then). Works on both sides, since the slots are synced.
    public boolean canRoll() {
        if (!this.kind.rolled) return false;
        KitchenRecipes.Assembly recipe = KitchenRecipes.find(this.kind.recipes(), this.table, this.kind.slots);
        if (recipe == null) return false;
        ItemStack out = this.table.getItem(this.kind.slots);
        return out.isEmpty() || (out.is(recipe.result().get()) && out.getCount() + recipe.count() <= out.getMaxStackSize());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if ((id != BUTTON_ROLL && id != BUTTON_SQUASH) || !this.canRoll()) return false;
        KitchenRecipes.Assembly recipe = KitchenRecipes.find(this.kind.recipes(), this.table, this.kind.slots);
        ItemStack made = id == BUTTON_ROLL
            ? new ItemStack(recipe.result().get(), recipe.count())
            : new ItemStack(ModItems.SQUASHED_SUSHI.get(), recipe.count());
        consumeIngredients(player);
        ItemStack out = this.table.getItem(this.kind.slots);
        if (out.isEmpty()) {
            this.table.setItem(this.kind.slots, made);
        } else if (ItemStack.isSameItemSameComponents(out, made) && out.getCount() + made.getCount() <= out.getMaxStackSize()) {
            out.grow(made.getCount());
        } else if (!player.getInventory().add(made)) {
            // (only a squashed roll can end up here, when the output slot is holding good sushi)
            player.drop(made, false, Prediction.PREDICTED);
        }
        this.table.setChanged();
        return true;
    }

    private void consumeIngredients(Player taker) {
        for (int i = 0; i < this.kind.slots; i++) {
            ItemStack stack = this.table.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStackTemplate remainder = stack.getCraftingRemainder();
            stack.shrink(1);
            if (remainder != null) {
                ItemStack leftover = remainder.create();
                if (stack.isEmpty()) {
                    this.table.setItem(i, leftover);
                } else if (!taker.getInventory().add(leftover)) {
                    taker.drop(leftover, false, Prediction.PREDICTED);
                }
            }
        }
        this.table.setChanged();
        this.updateResult();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int invEnd = this.invStart + 36;
            if (slotIndex == this.resultSlot && !this.kind.rolled) {
                if (!this.moveItemStackTo(stack, this.invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, clicked);
                return clicked;
            } else if (slotIndex <= this.resultSlot) {
                if (!this.moveItemStackTo(stack, this.invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.kind.slots, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.table.stillValid(player);
    }
}

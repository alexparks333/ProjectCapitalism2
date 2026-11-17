package com.alexh.capitalism.menu;

import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.block.BartenderRecipe;
import com.alexh.capitalism.block.BartendingTableBlockEntity;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

// 5 ingredient slots in a row + a glass bottle slot; the drink appears in the result slot as soon as they
// match a recipe. Taking it uses one of each ingredient and the recipe's bottles (buckets come back empty).
public class BartendingTableMenu extends AbstractContainerMenu {

    // Positions match textures/gui/container/bartending_table.png
    public static final int INGREDIENT_X = 44;
    public static final int INGREDIENT_Y = 24;
    public static final int GLASS_X = 149;
    public static final int GLASS_Y = 24;
    public static final int RESULT_X = 80;
    public static final int RESULT_Y = 60;

    private static final int GLASS_SLOT = BartendingTableBlockEntity.SLOT_GLASS;
    private static final int RESULT_SLOT = GLASS_SLOT + 1;
    private static final int INV_START = RESULT_SLOT + 1;
    private static final int INV_END = INV_START + 36;

    private final Container table;
    private final ResultContainer result = new ResultContainer();
    private final Player player;

    public BartendingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BartendingTableBlockEntity.SIZE));
    }

    public BartendingTableMenu(int containerId, Inventory inventory, Container table) {
        super(ModMenus.BARTENDING_TABLE.get(), containerId);
        checkContainerSize(table, BartendingTableBlockEntity.SIZE);
        this.table = table;
        this.player = inventory.player;

        for (int i = 0; i < BartenderRecipe.SLOTS; i++) {
            this.addSlot(new Slot(table, i, INGREDIENT_X + i * 18, INGREDIENT_Y));
        }
        this.addSlot(new Slot(table, GLASS_SLOT, GLASS_X, GLASS_Y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.GLASS_BOTTLE);
            }
        });
        this.addSlot(new Slot(this.result, 0, RESULT_X, RESULT_Y) {
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
        this.addStandardInventorySlots(inventory, 8, 84);
        this.updateResult();
    }

    // Runs every tick on the server while the menu is open, so the drink preview always matches the slots.
    @Override
    public void broadcastChanges() {
        if (!this.player.level().isClientSide()) {
            this.updateResult();
        }
        super.broadcastChanges();
    }

    private void updateResult() {
        BartenderRecipe recipe = BartenderRecipe.find(this.table, GLASS_SLOT);
        ItemStack wanted = recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.result().get(), recipe.count());
        if (!ItemStack.matches(wanted, this.result.getItem(0))) {
            this.result.setItem(0, wanted);
        }
    }

    private void consumeIngredients(Player taker) {
        BartenderRecipe recipe = BartenderRecipe.find(this.table, GLASS_SLOT);
        if (recipe != null && recipe.bottles() > 0) {
            this.table.getItem(GLASS_SLOT).shrink(recipe.bottles());
        }

        for (int i = 0; i < BartenderRecipe.SLOTS; i++) {
            ItemStack stack = this.table.getItem(i);
            if (stack.isEmpty()) continue;

            ItemStackTemplate remainder = stack.getCraftingRemainder();
            stack.shrink(1);
            if (remainder != null) {
                ItemStack leftover = remainder.create();
                // Returned glass bottles go back into the glass slot, ready for the next drink.
                if (leftover.is(Items.GLASS_BOTTLE) && this.returnGlass(leftover)) continue;
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

    private boolean returnGlass(ItemStack bottle) {
        ItemStack glass = this.table.getItem(GLASS_SLOT);
        if (glass.isEmpty()) {
            this.table.setItem(GLASS_SLOT, bottle);
            return true;
        }
        if (glass.is(Items.GLASS_BOTTLE) && glass.getCount() + bottle.getCount() <= glass.getMaxStackSize()) {
            glass.grow(bottle.getCount());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, clicked);
                return clicked;
            } else if (slotIndex < RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.GLASS_BOTTLE)) {
                if (!this.moveItemStackTo(stack, GLASS_SLOT, GLASS_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, BartenderRecipe.SLOTS, false)) {
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

package com.alexh.capitalism.pets;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// A Food Trough's window (client/FoodTroughScreen, laid out like a hopper's): its 1, 2 or 5 food slots in a row in the
// middle, your inventory under them. The number of slots comes with the opening packet.
public class FoodTroughMenu extends AbstractContainerMenu {

    public static final int SLOT_Y = 20;
    private final Container container;
    private final int foodSlots;

    // Client side
    public FoodTroughMenu(int containerId, Inventory inventory, int foodSlots) {
        this(containerId, inventory, new SimpleContainer(foodSlots));
    }

    public FoodTroughMenu(int containerId, Inventory inventory, Container container) {
        super(ModMenus.FOOD_TROUGH.get(), containerId);
        this.container = container;
        this.foodSlots = container.getContainerSize();
        for (int i = 0; i < this.foodSlots; i++) this.addSlot(new Slot(container, i, slotX(this.foodSlots, i), SLOT_Y));
        this.addStandardInventorySlots(inventory, 8, 51);
    }

    // Where food slot i of n goes (its left edge, centered in the 176-wide window)
    public static int slotX(int slots, int i) {
        return 89 - slots * 9 + i * 18;
    }

    public int foodSlots() {
        return this.foodSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < this.foodSlots) {
            if (!this.moveItemStackTo(stack, this.foodSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, this.foodSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

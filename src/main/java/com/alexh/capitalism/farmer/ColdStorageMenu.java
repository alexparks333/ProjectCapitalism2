package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The Fridge's / Freezer's menu (client/ColdStorageScreen): three rows of nine like a chest, then the fuel slot with a
// gauge of how much of the burning fuel is left, then the inventory. Data: 0 burn left, 1 burn total, 2 the Kind.
public class ColdStorageMenu extends AbstractContainerMenu {

    public static final int ROWS_Y = 18;
    public static final int FUEL_X = 8;
    public static final int FUEL_Y = 78;
    public static final int INV_Y = 112;
    public static final int HEIGHT = 194;
    private static final int INV_START = ColdStorageBlockEntity.SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public ColdStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(ColdStorageBlockEntity.SIZE), new SimpleContainerData(3));
    }

    public ColdStorageMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.COLD_STORAGE.get(), containerId);
        checkContainerSize(container, ColdStorageBlockEntity.SIZE);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        for (int i = 0; i < ColdStorageBlockEntity.SLOTS; i++) {
            this.addSlot(new Slot(container, i, 8 + (i % 9) * 18, ROWS_Y + (i / 9) * 18));
        }
        this.addSlot(new Slot(container, ColdStorageBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ColdStorageMenu.this.kind().fuel(stack) > 0;
            }
        });
        this.addStandardInventorySlots(inventory, 8, INV_Y);
        this.addDataSlots(data);
        container.startOpen(inventory.player);
    }

    public ColdStorageBlockEntity.Kind kind() {
        int k = this.data.get(2);
        return k >= 0 && k < ColdStorageBlockEntity.Kind.values().length ? ColdStorageBlockEntity.Kind.values()[k] : ColdStorageBlockEntity.Kind.FRIDGE;
    }

    public int burnLeft() {
        return this.data.get(0);
    }

    public float burnShare() {
        int total = this.data.get(1);
        return total <= 0 ? 0 : Math.min(1, this.data.get(0) / (float) total);
    }

    // Shift-click: out into the inventory; from the inventory fuel to the fuel slot, anything else into the rows
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (this.kind().fuel(stack) > 0 && this.moveItemStackTo(stack, ColdStorageBlockEntity.FUEL, ColdStorageBlockEntity.FUEL + 1, false)) {
            // went to the fuel slot
        } else if (!this.moveItemStackTo(stack, 0, ColdStorageBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

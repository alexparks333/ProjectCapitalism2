package com.alexh.capitalism.menu;

import com.alexh.capitalism.block.PurificationTableBlockEntity;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PurificationTableMenu extends AbstractContainerMenu {

    private static final int INV_START = 3;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    // Client side: the real items and progress arrive from the server through the slots and data.
    public PurificationTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public PurificationTableMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.PURIFICATION_TABLE.get(), containerId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;

        // Same layout as a furnace: input top-left, fuel below it, output on the right.
        this.addSlot(new Slot(container, PurificationTableBlockEntity.SLOT_INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.WATER_BUCKET);
            }
        });
        this.addSlot(new Slot(container, PurificationTableBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return PurificationTableBlockEntity.isFuel(itemStack);
            }
        });
        this.addSlot(new Slot(container, PurificationTableBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public float getProgress() {
        int current = this.data.get(0);
        int total = this.data.get(1);
        return total != 0 && current != 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
    }

    public int getSecondsLeft() {
        int current = this.data.get(0);
        int total = this.data.get(1);
        return current == 0 ? 0 : Mth.ceil((total - current) / 20.0F);
    }

    public boolean hasOutput() {
        return this.getSlot(PurificationTableBlockEntity.SLOT_OUTPUT).hasItem();
    }

    public int getFuelUses() {
        return this.data.get(2);
    }

    public float getFuelLevel() {
        int max = this.data.get(3);
        return max <= 0 ? 0.0F : Mth.clamp((float) this.data.get(2) / max, 0.0F, 1.0F);
    }

    // Shift-click: table slots go to the inventory, water buckets and fuel go to their slots.
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < INV_START) {
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.WATER_BUCKET)) {
                if (!this.moveItemStackTo(stack, PurificationTableBlockEntity.SLOT_INPUT, PurificationTableBlockEntity.SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (PurificationTableBlockEntity.isFuel(stack)) {
                if (!this.moveItemStackTo(stack, PurificationTableBlockEntity.SLOT_FUEL, PurificationTableBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
        return this.container.stillValid(player);
    }
}

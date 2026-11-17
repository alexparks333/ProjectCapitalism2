package com.alexh.capitalism.enchanter;

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

// The Lapis Compressor's menu (client/LapisCompressorScreen): one Lapis slot, and how far the press is with the Lapis
// it's on (data 0, out of LapisCompressorBlockEntity.PRESS_TICKS). The XP comes out of the block's front.
public class LapisCompressorMenu extends AbstractContainerMenu {

    private static final int INV_START = 1;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    // Client side: the Lapis and the progress arrive from the server through the slot and the data
    public LapisCompressorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(1));
    }

    public LapisCompressorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.LAPIS_COMPRESSOR.get(), containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        this.addSlot(new Slot(container, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    // 0..1: how far the press is
    public float progress() {
        return Mth.clamp(this.data.get(0) / (float) LapisCompressorBlockEntity.PRESS_TICKS, 0.0F, 1.0F);
    }

    public boolean working() {
        return this.slots.get(0).hasItem() || this.data.get(0) > 0;
    }

    // Shift-click: the Lapis back to the inventory, or Lapis from the inventory into the slot
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (!stack.is(Items.LAPIS_LAZULI) || !this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

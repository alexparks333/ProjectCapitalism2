package com.alexh.capitalism.weaponsmith;

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

// The Lava Creator's menu: the water slot next to the tank (left), Blaze Powder on top, the bucket slot below it.
public class LavaCreatorMenu extends AbstractContainerMenu {

    private static final int INV_START = 3;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    // Client side: the real items, water and progress arrive from the server through the slots and data
    public LavaCreatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    public LavaCreatorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.LAVA_CREATOR.get(), containerId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.addSlot(new Slot(container, LavaCreatorBlockEntity.SLOT_WATER, 44, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }
        });
        this.addSlot(new Slot(container, LavaCreatorBlockEntity.SLOT_BLAZE, 98, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BLAZE_POWDER);
            }
        });
        this.addSlot(new Slot(container, LavaCreatorBlockEntity.SLOT_BUCKET, 98, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public int water() {
        return this.data.get(0);
    }

    public float progress() {
        return Mth.clamp(this.data.get(1) / (float) LavaCreatorBlockEntity.BREW_TICKS, 0.0F, 1.0F);
    }

    // Shift-click: Lava Creator slots go to the inventory; Water Buckets, Blaze Powder and empty Buckets to their slots
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            int target = stack.is(Items.WATER_BUCKET) ? LavaCreatorBlockEntity.SLOT_WATER
                : stack.is(Items.BLAZE_POWDER) ? LavaCreatorBlockEntity.SLOT_BLAZE
                : stack.is(Items.BUCKET) ? LavaCreatorBlockEntity.SLOT_BUCKET : -1;
            if (target < 0 || !this.moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
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

package com.alexh.capitalism.menu;

import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.block.OvenBlockEntity;
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

// 3 dish slots stacked on the left, each with an arrow to its meal slot, and a fuel slot under a flame.
public class OvenMenu extends AbstractContainerMenu {

    // Item positions; they match textures/gui/container/oven.png
    public static final int INPUT_X = 44;
    public static final int OUTPUT_X = 98;
    public static final int ROW_Y = 17;
    public static final int FUEL_X = 140;
    public static final int FUEL_Y = 53;

    private static final int INV_START = OvenBlockEntity.SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container oven;
    private final ContainerData data;

    public OvenMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(OvenBlockEntity.SIZE), new SimpleContainerData(OvenBlockEntity.DATA_COUNT));
    }

    public OvenMenu(int containerId, Inventory inventory, Container oven, ContainerData data) {
        super(ModMenus.OVEN.get(), containerId);
        checkContainerSize(oven, OvenBlockEntity.SIZE);
        checkContainerDataCount(data, OvenBlockEntity.DATA_COUNT);
        this.oven = oven;
        this.data = data;

        for (int r = 0; r < OvenBlockEntity.ROWS; r++) {
            this.addSlot(new Slot(oven, r, INPUT_X, ROW_Y + r * 18));
        }
        for (int r = 0; r < OvenBlockEntity.ROWS; r++) {
            this.addSlot(new Slot(oven, OvenBlockEntity.ROWS + r, OUTPUT_X, ROW_Y + r * 18) {
                @Override
                public void onTake(Player taker, ItemStack taken) {
                    Discoveries.discover(taker, taken);
                    super.onTake(taker, taken);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        this.addSlot(new Slot(oven, OvenBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OvenBlockEntity.burnTicks(stack) > 0;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public float litProgress() {
        int duration = this.data.get(1);
        return duration == 0 ? 0 : Mth.clamp(this.data.get(0) / (float) duration, 0, 1);
    }

    public float bakeProgress(int row) {
        int progress = this.data.get(2 + row * 3);
        int total = this.data.get(3 + row * 3);
        return total == 0 || progress == 0 ? 0 : Mth.clamp(progress / (float) total, 0, 1);
    }

    // How close a row's finished meal is to burning (0..1).
    public float burnProgress(int row) {
        int burn = OvenBlockEntity.burnTicks(KitchenRecipes.bakedInto(this.oven.getItem(OvenBlockEntity.ROWS + row)));
        return Mth.clamp(this.data.get(4 + row * 3) / (float) Math.max(1, burn), 0, 1);
    }

    // Shift-click: oven slots to the inventory; fuel to the fuel slot; dishes to the first free dish slot.
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
            } else if (OvenBlockEntity.burnTicks(stack) > 0 && KitchenRecipes.baking(stack) == null) {
                if (!this.moveItemStackTo(stack, OvenBlockEntity.SLOT_FUEL, OvenBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, OvenBlockEntity.ROWS, false)) {
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
        return this.oven.stillValid(player);
    }
}

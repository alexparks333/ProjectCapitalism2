package com.alexh.capitalism.menu;

import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.block.JuiceType;
import com.alexh.capitalism.block.JuicerBlockEntity;
import com.alexh.capitalism.block.JuicerRecipe;
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
import org.jspecify.annotations.Nullable;

public class JuicerMenu extends AbstractContainerMenu {

    // Positions match textures/gui/container/juicer.png
    public static final int FRUIT_X = 35;
    public static final int FRUIT_Y = 18;
    public static final int BUCKET_X = 80;
    public static final int BUCKET_Y = 60;

    private static final int INV_START = 7;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public JuicerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(7), new SimpleContainerData(3));
    }

    public JuicerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.JUICER.get(), containerId);
        checkContainerSize(container, 7);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;

        // 6 fruit slots across the top
        for (int i = 0; i < JuicerBlockEntity.FRUIT_SLOTS; i++) {
            this.addSlot(new Slot(container, i, FRUIT_X + i * 18, FRUIT_Y) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return JuicerRecipe.isFruit(itemStack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        // Big bucket slot: one empty bucket goes in, the juice bucket comes out of the same slot
        this.addSlot(new Slot(container, JuicerBlockEntity.SLOT_BUCKET, BUCKET_X, BUCKET_Y) {
            @Override
            public void onTake(Player taker, ItemStack taken) {
                Discoveries.discover(taker, taken);
                super.onTake(taker, taken);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return JuicerRecipe.isContainer(itemStack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
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

    // The juice being made or waiting in the bucket slot, or null.
    public @Nullable JuiceType getJuice() {
        int id = this.data.get(2);
        return id <= 0 || id > JuiceType.values().length ? null : JuiceType.values()[id - 1];
    }

    // Shift-click: juicer slots go to the inventory; a bucket goes to the big slot, fruit is dealt across the top.
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
            } else if (JuicerRecipe.isContainer(stack)) {
                if (!this.moveItemStackTo(stack, JuicerBlockEntity.SLOT_BUCKET, JuicerBlockEntity.SLOT_BUCKET + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (JuicerRecipe.isFruit(stack)) {
                if (!this.spreadAcrossFruitSlots(stack)) {
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

    // Put one fruit in each empty fruit slot.
    private boolean spreadAcrossFruitSlots(ItemStack stack) {
        boolean moved = false;
        while (!stack.isEmpty()) {
            boolean placedThisRound = false;
            for (int i = 0; i < JuicerBlockEntity.FRUIT_SLOTS && !stack.isEmpty(); i++) {
                Slot target = this.slots.get(i);
                ItemStack existing = target.getItem();
                if (existing.isEmpty()) {
                    target.setByPlayer(stack.split(1));
                    placedThisRound = true;
                } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < target.getMaxStackSize(existing)) {
                    existing.grow(1);
                    stack.shrink(1);
                    target.setChanged();
                    placedThisRound = true;
                }
            }
            if (!placedThisRound) break;
            moved = true;
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

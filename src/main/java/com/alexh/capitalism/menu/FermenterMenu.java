package com.alexh.capitalism.menu;

import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.block.FermenterBlockEntity;
import com.alexh.capitalism.block.FermenterRecipe;
import com.alexh.capitalism.block.LiquorType;
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

public class FermenterMenu extends AbstractContainerMenu {

    // Positions match textures/gui/container/fermenter.png
    public static final int INGREDIENT_X = 35;
    public static final int INGREDIENT_Y = 18;
    public static final int SUGAR_X = 25;
    public static final int SUGAR_Y = 49;
    public static final int BUCKET_X = 134;
    public static final int BUCKET_Y = 51;

    private static final int INV_START = 8;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public FermenterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(8), new SimpleContainerData(5));
    }

    public FermenterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.FERMENTER.get(), containerId);
        checkContainerSize(container, 8);
        checkContainerDataCount(data, 5);
        this.container = container;
        this.data = data;

        // 6 fermentable slots across the top, one item each
        for (int i = 0; i < FermenterBlockEntity.INGREDIENT_SLOTS; i++) {
            this.addSlot(new Slot(container, i, INGREDIENT_X + i * 18, INGREDIENT_Y) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return FermenterRecipe.isFermentable(itemStack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        // Sugar (the "fuel"): stacks, one burns every FERMENTER_SUGAR_TICKS while fermenting
        this.addSlot(new Slot(container, FermenterBlockEntity.SLOT_SUGAR, SUGAR_X, SUGAR_Y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return FermenterRecipe.isSugar(itemStack);
            }
        });
        // One empty bucket in, the liquor bucket comes out of the same slot
        this.addSlot(new Slot(container, FermenterBlockEntity.SLOT_BUCKET, BUCKET_X, BUCKET_Y) {
            @Override
            public void onTake(Player taker, ItemStack taken) {
                Discoveries.discover(taker, taken);
                super.onTake(taker, taken);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return FermenterRecipe.isBucket(itemStack);
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

    public @Nullable LiquorType getLiquor() {
        int id = this.data.get(2);
        return id <= 0 || id > LiquorType.values().length ? null : LiquorType.values()[id - 1];
    }

    // How much of the sugar currently burning is left, 0..1
    public float getSugarLevel() {
        int total = this.data.get(4);
        return total <= 0 ? 0.0F : Mth.clamp((float) this.data.get(3) / total, 0.0F, 1.0F);
    }

    public boolean hasFinishedLiquor() {
        return FermenterRecipe.liquorOf(this.getSlot(FermenterBlockEntity.SLOT_BUCKET).getItem()) != null;
    }

    // Shift-click: fermenter slots go to the inventory; sugar, buckets and fermentables go to their slots.
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
            } else if (FermenterRecipe.isSugar(stack)) {
                if (!this.moveItemStackTo(stack, FermenterBlockEntity.SLOT_SUGAR, FermenterBlockEntity.SLOT_SUGAR + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (FermenterRecipe.isBucket(stack)) {
                if (!this.moveItemStackTo(stack, FermenterBlockEntity.SLOT_BUCKET, FermenterBlockEntity.SLOT_BUCKET + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (FermenterRecipe.isFermentable(stack)) {
                if (!this.moveItemStackTo(stack, 0, FermenterBlockEntity.INGREDIENT_SLOTS, false)) {
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

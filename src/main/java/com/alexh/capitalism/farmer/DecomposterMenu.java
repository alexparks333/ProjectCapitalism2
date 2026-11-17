package com.alexh.capitalism.farmer;

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

import static com.alexh.capitalism.farmer.DecomposterBlockEntity.INPUTS;
import static com.alexh.capitalism.farmer.DecomposterBlockEntity.OUTPUT;
import static com.alexh.capitalism.farmer.DecomposterBlockEntity.SIZE;

// The Decomposter's menu (client/DecomposterScreen): the three ingredient slots on the left, the arrow filling as it
// rots down, the Fertilizer on the right. Positions match tools/make_farmer_textures.ps1.
public class DecomposterMenu extends AbstractContainerMenu {

    public static final int INPUT_X = 26;
    public static final int INPUT_Y = 35;
    public static final int INPUT_STEP = 20;
    public static final int OUTPUT_X = 134;
    public static final int OUTPUT_Y = 35;
    private static final int INV_START = SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public DecomposterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), new SimpleContainerData(1));
    }

    public DecomposterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.DECOMPOSTER.get(), containerId);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        for (int i = 0; i < INPUTS; i++) {
            this.addSlot(new Slot(container, i, INPUT_X + i * INPUT_STEP, INPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DecomposterBlockEntity.isIngredient(stack);
                }
            });
        }
        this.addSlot(new Slot(container, OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                com.alexh.capitalism.guide.Discoveries.discover(taker, stack);
                super.onTake(taker, stack);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public float progress() {
        return Mth.clamp(this.data.get(0) / (float) DecomposterBlockEntity.TICKS, 0.0F, 1.0F);
    }

    public int secondsLeft() {
        int done = this.data.get(0);
        return done == 0 ? 0 : Mth.ceil((DecomposterBlockEntity.TICKS - done) / 20.0F);
    }

    // Shift-click: out of the Decomposter into the inventory; from the inventory an ingredient to the first slot that
    // takes it (an empty one, or one already holding it)
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (DecomposterBlockEntity.isIngredient(stack)) {
            if (!this.moveItemStackTo(stack, 0, INPUTS, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}

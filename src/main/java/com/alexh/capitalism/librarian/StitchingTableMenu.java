package com.alexh.capitalism.librarian;

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

import static com.alexh.capitalism.librarian.StitchingTableBlockEntity.COVERS;
import static com.alexh.capitalism.librarian.StitchingTableBlockEntity.OUTPUT;
import static com.alexh.capitalism.librarian.StitchingTableBlockEntity.PAPER;
import static com.alexh.capitalism.librarian.StitchingTableBlockEntity.SIZE;
import static com.alexh.capitalism.librarian.StitchingTableBlockEntity.THREAD;

// The Stitching Table's menu (client/StitchingTableScreen, tools/make_librarian_textures.ps1): a ring of 12 cover
// slots (3 along each side) round the big Paper slot in the middle; on the right the thread slot (Iron Ingot or
// Diamond), the sewing progress (data 0, out of SEW_TICKS) and the finished book.
public class StitchingTableMenu extends AbstractContainerMenu {

    // the ring: the 5x5 box of slots at RING_X, RING_Y, without its corners and its middle 3x3 (where the Paper is)
    public static final int RING_X = 20;
    public static final int RING_Y = 28;
    public static final int PAPER_X = 57;
    public static final int PAPER_Y = 65;
    public static final int THREAD_X = 144;
    public static final int THREAD_Y = 38;
    public static final int OUTPUT_X = 144;
    public static final int OUTPUT_Y = 101;
    private static final int INV_START = SIZE;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public StitchingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE), new SimpleContainerData(1));
    }

    public StitchingTableMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.STITCHING_TABLE.get(), containerId);
        checkContainerSize(container, SIZE);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        // 3 along the top, 3 down the right, 3 along the bottom (right to left), 3 up the left: round the ring
        int[][] ring = new int[COVERS][];
        for (int i = 0; i < 3; i++) {
            ring[i] = new int[]{RING_X + 18 * (i + 1), RING_Y};
            ring[3 + i] = new int[]{RING_X + 18 * 4, RING_Y + 18 * (i + 1)};
            ring[6 + i] = new int[]{RING_X + 18 * (3 - i), RING_Y + 18 * 4};
            ring[9 + i] = new int[]{RING_X, RING_Y + 18 * (3 - i)};
        }
        for (int i = 0; i < COVERS; i++) {
            this.addSlot(new Slot(container, i, ring[i][0], ring[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return StitchingTableBlockEntity.isCover(stack);
                }
            });
        }
        this.addSlot(new Slot(container, PAPER, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.PAPER);
            }
        });
        this.addSlot(new Slot(container, THREAD, THREAD_X, THREAD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StitchingTableBlockEntity.isThread(stack);
            }
        });
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
        this.addStandardInventorySlots(inventory, 8, 140);
        this.addDataSlots(data);
    }

    // 0..1: how far the book being sewn is
    public float progress() {
        return Mth.clamp(this.data.get(0) / (float) StitchingTableBlockEntity.SEW_TICKS, 0.0F, 1.0F);
    }

    // Ticks left on the book being sewn
    public int ticksLeft() {
        return Math.max(0, StitchingTableBlockEntity.SEW_TICKS - this.data.get(0));
    }

    public boolean diamondThread() {
        return this.slots.get(THREAD).getItem().is(Items.DIAMOND);
    }

    public boolean hasThread() {
        return StitchingTableBlockEntity.isThread(this.slots.get(THREAD).getItem());
    }

    // What's stopping it sewing (shown on the screen), or null if it's sewing
    public @org.jspecify.annotations.Nullable String missing() {
        if (this.slots.get(PAPER).getItem().isEmpty()) return "Needs Paper";
        int empty = 0;
        for (int i = 0; i < COVERS; i++) {
            if (this.slots.get(i).getItem().isEmpty()) empty++;
        }
        if (empty > 0) return "Fill the ring: " + empty + " empty";
        if (!this.hasThread()) return "Needs thread";
        ItemStack out = this.slots.get(OUTPUT).getItem();
        if (!out.isEmpty() && (out.getCount() >= out.getMaxStackSize()
            || !out.is(this.diamondThread() ? com.alexh.capitalism.core.ModItems.DELICATE_BOOK.get() : com.alexh.capitalism.core.ModItems.ROUGH_BOOK.get()))) {
            return "Take the books out";
        }
        return null;
    }

    // Shift-click: out of the table into the inventory; from the inventory Paper to the middle, Leather / Rabbit Hide
    // round the ring, an Iron Ingot / Diamond to the thread slot
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.PAPER)) {
            if (!this.moveItemStackTo(stack, PAPER, PAPER + 1, false)) return ItemStack.EMPTY;
        } else if (StitchingTableBlockEntity.isCover(stack)) {
            if (!this.moveItemStackTo(stack, 0, COVERS, false)) return ItemStack.EMPTY;
        } else if (StitchingTableBlockEntity.isThread(stack)) {
            if (!this.moveItemStackTo(stack, THREAD, THREAD + 1, false)) return ItemStack.EMPTY;
        } else {
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

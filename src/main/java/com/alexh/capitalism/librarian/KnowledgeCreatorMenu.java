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

import static com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity.BOOK;
import static com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity.OUTPUT;

// The Knowledge Creator's menu (client/KnowledgeCreatorScreen): the Enchanted Book on the left, the Knowledge Piece on
// the right, and between them how far along it is (data 0, out of MAKE_TICKS). The screen shows what the book in it will
// make (its power and the piece's quality) before it's done.
public class KnowledgeCreatorMenu extends AbstractContainerMenu {

    public static final int BOOK_X = 35;
    public static final int BOOK_Y = 35;
    public static final int OUTPUT_X = 125;
    public static final int OUTPUT_Y = 35;
    private static final int INV_START = 2;
    private static final int INV_END = INV_START + 36;

    private final Container container;
    private final ContainerData data;

    public KnowledgeCreatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(1));
    }

    public KnowledgeCreatorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.KNOWLEDGE_CREATOR.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        this.addSlot(new Slot(container, BOOK, BOOK_X, BOOK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KnowledgeCreatorBlockEntity.isBook(stack);
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
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    public ItemStack book() {
        return this.slots.get(BOOK).getItem();
    }

    public float progress() {
        return Mth.clamp(this.data.get(0) / (float) KnowledgeCreatorBlockEntity.MAKE_TICKS, 0.0F, 1.0F);
    }

    public int ticksLeft() {
        return Math.max(0, KnowledgeCreatorBlockEntity.MAKE_TICKS - this.data.get(0));
    }

    // Can it make the book in it right now? (the output is empty or holds the same quality, with room)
    public boolean working() {
        ItemStack book = this.book();
        if (!KnowledgeCreatorBlockEntity.isBook(book)) return false;
        ItemStack out = this.slots.get(OUTPUT).getItem();
        if (out.isEmpty()) return true;
        ItemStack made = KnowledgeQuality.byPower(KnowledgeQuality.power(book)).stack(1);
        return ItemStack.isSameItemSameComponents(out, made) && out.getCount() < out.getMaxStackSize();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (!KnowledgeCreatorBlockEntity.isBook(stack) || !this.moveItemStackTo(stack, BOOK, BOOK + 1, false)) {
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

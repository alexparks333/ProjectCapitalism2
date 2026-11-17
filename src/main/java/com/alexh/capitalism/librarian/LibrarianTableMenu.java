package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.guide.Discoveries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The Librarian Table (LibrarianTableBlock, Librarian.md): a big slot (a Rough or Delicate Book), a small slot (a
// Knowledge Piece) and the result. The result shows an "Unwritten Book" until it's taken: only then is it written
// (Skill.roll), so nobody can peek and swap. The Knowledge Piece's quality (KnowledgeQuality) multiplies the chance
// of a Persistent book, and Lucky Librarian doubles it again. Taking it uses the book and the Knowledge Piece. Whatever is left in the
// slots goes back to you when you close it.
public class LibrarianTableMenu extends AbstractContainerMenu {

    public static final int BIG = 0;
    public static final int SMALL = 1;
    public static final int RESULT = 2;
    private static final int INV_START = 3;
    private static final int INV_END = INV_START + 36;

    private final ContainerLevelAccess access;
    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            LibrarianTableMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    public LibrarianTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public LibrarianTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.LIBRARIAN_TABLE.get(), containerId);
        this.access = access;
        this.addSlot(new Slot(this.inputs, BIG, 22, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBlankBook(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.inputs, SMALL, 62, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.KNOWLEDGE_PIECE.get());
            }
        });
        this.addSlot(new Slot(this.result, 0, 136, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack carried) {
                LibrarianTableMenu.this.take(taker, carried);
                super.onTake(taker, carried);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    public static boolean isBlankBook(ItemStack stack) {
        return stack.is(ModItems.ROUGH_BOOK.get()) || stack.is(ModItems.DELICATE_BOOK.get());
    }

    private boolean delicate() {
        return this.inputs.getItem(BIG).is(ModItems.DELICATE_BOOK.get());
    }

    private boolean ready() {
        return isBlankBook(this.inputs.getItem(BIG)) && this.inputs.getItem(SMALL).is(ModItems.KNOWLEDGE_PIECE.get());
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.result.setItem(0, this.ready() ? new ItemStack(ModItems.SKILL_BOOK.get()) : ItemStack.EMPTY);
    }

    // The book was taken: write it now (random), use up the inputs
    private void take(Player taker, ItemStack carried) {
        if (SkillBookItem.skill(carried) == null) {
            // Lucky Librarian (a lesson) doubles the trait chance, for Librarians
            boolean lucky = Skills.has(taker, Skill.LUCKY_LIBRARIAN)
                && com.alexh.capitalism.occupation.JobEvents.has(taker, com.alexh.capitalism.occupation.Job.LIBRARIAN);
            // and the Knowledge Piece's quality multiplies it (Faded x1 ... Radiant x3)
            KnowledgeQuality piece = KnowledgeQuality.of(this.inputs.getItem(SMALL));
            Skill skill = Skill.roll(taker.getRandom(), this.delicate(), piece.multiplier * (lucky ? 2.0F : 1.0F));
            SkillBookItem.write(carried, skill, this.delicate());
            if (taker instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.literal("You wrote " + skill.title + "!")
                    .withStyle(skill.trait ? ChatFormatting.GOLD : ChatFormatting.WHITE));
            }
            Discoveries.discover(taker, carried);
            this.access.execute((level, pos) -> {
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, pos, skill.trait ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_WORK_LIBRARIAN,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            });
        }
        this.inputs.removeItem(BIG, 1);
        this.inputs.removeItem(SMALL, 1);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.LIBRARIAN_TABLE.get());
    }

    // Shift-click: the result (written as it's taken) and the inputs go to the inventory; from the inventory a Knowledge
    // Piece goes in the small slot, a Rough / Delicate Book in the big one
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (slotIndex == RESULT && SkillBookItem.skill(stack) == null) {
            // write it before it moves; using up the inputs empties the result, so put the written book back
            this.take(player, stack);
            slot.set(stack);
        }
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.KNOWLEDGE_PIECE.get())) {
            if (!this.moveItemStackTo(stack, SMALL, SMALL + 1, false)) return ItemStack.EMPTY;
        } else if (!isBlankBook(stack) || !this.moveItemStackTo(stack, BIG, BIG + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        if (slotIndex != RESULT) slot.onTake(player, stack);
        return clicked;
    }
}

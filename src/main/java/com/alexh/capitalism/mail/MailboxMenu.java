package com.alexh.capitalism.mail;

import com.alexh.capitalism.core.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// The Mailbox's menu (client/MailboxScreen), two ways:
// - the OWNER: the 27 slots of mail, like a chest (take out, put in, shift-click).
// - anyone else (SENDER): only the mail slot, three slots; "Drop Mail In" posts what's in them into the box (it's
//   gone from the slot at once: they can't see or take the owner's mail). What doesn't fit (a full box) stays in the
//   slot and comes back when they close it. Data: 0 the free slots in the box.
public class MailboxMenu extends AbstractContainerMenu {

    public static final int MAIL_Y = 18;
    public static final int SLOT_Y = 34;
    public static final int SEND_SLOTS = 3;
    public static final int INV_Y = 84;
    public static final int HEIGHT = 166;

    private final boolean owner;
    private final int mailSlots;
    private final Container container;
    private final @Nullable MailboxBlockEntity mailbox;
    private final ContainerData data;


    private MailboxMenu(MenuType<?> type, int id, Inventory inventory, Container container, @Nullable MailboxBlockEntity mailbox,
                        ContainerData data, boolean owner) {
        super(type, id);
        this.owner = owner;
        this.container = container;
        this.mailbox = mailbox;
        this.data = data;
        this.mailSlots = container.getContainerSize();
        if (owner) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, MAIL_Y + row * 18));
            }
        } else {
            for (int i = 0; i < SEND_SLOTS; i++) this.addSlot(new Slot(container, i, 62 + i * 18, SLOT_Y));
        }
        this.addStandardInventorySlots(inventory, 8, INV_Y);
        this.addDataSlots(data);
    }

    // server
    public static MailboxMenu owner(int id, Inventory inventory, MailboxBlockEntity mailbox) {
        return new MailboxMenu(ModMenus.MAILBOX_OWNER.get(), id, inventory, mailbox, mailbox, new SimpleContainerData(1), true);
    }

    public static MailboxMenu sender(int id, Inventory inventory, MailboxBlockEntity mailbox, ContainerData data) {
        return new MailboxMenu(ModMenus.MAILBOX_SENDER.get(), id, inventory, new SimpleContainer(SEND_SLOTS), mailbox, data, false);
    }

    // client
    public static MailboxMenu ownerClient(int id, Inventory inventory) {
        return new MailboxMenu(ModMenus.MAILBOX_OWNER.get(), id, inventory, new SimpleContainer(MailboxBlockEntity.SIZE), null, new SimpleContainerData(1), true);
    }

    public static MailboxMenu senderClient(int id, Inventory inventory) {
        return new MailboxMenu(ModMenus.MAILBOX_SENDER.get(), id, inventory, new SimpleContainer(SEND_SLOTS), null, new SimpleContainerData(1), false);
    }

    public boolean isOwner() {
        return this.owner;
    }

    public int freeSlots() {
        return this.data.get(0);
    }

    // the "Drop Mail In" button (client/MailboxScreen): what's in the mail slot goes into the box
    public static final int SEND = 0;

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != SEND || this.owner || this.mailbox == null || !(player instanceof ServerPlayer sender)) return false;
        int sent = 0;
        boolean full = false;
        for (int i = 0; i < SEND_SLOTS; i++) {
            ItemStack stack = this.container.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = this.mailbox.deliver(stack);
            sent += stack.getCount() - rest.getCount();
            if (!rest.isEmpty()) full = true;
            this.container.setItem(i, rest);
        }
        if (sent > 0) {
            this.mailbox.notifyOwner(sender, sent);
            com.alexh.capitalism.bounty.BountyEvents.mailSent(sender);
            sender.level().playSound(null, this.mailbox.getBlockPos(), net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
            sender.sendOverlayMessage(Component.literal("✉ Mail sent to " + this.mailbox.recipient() + "."
                + (full ? " Some didn't fit: the mailbox is full." : "")));
        } else if (full) {
            sender.sendOverlayMessage(Component.literal("The mailbox is full."));
        }
        this.broadcastChanges();
        return true;
    }

    // what wasn't sent goes back to them
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.owner) this.clearContainer(player, this.container);
    }
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        int invStart = this.owner ? this.mailSlots : SEND_SLOTS;
        if (slotIndex < invStart) {
            if (!this.moveItemStackTo(stack, invStart, invStart + 36, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, invStart, false)) {
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
        return this.mailbox == null ? true : this.mailbox.stillValid(player);
    }
}

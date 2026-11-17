package com.alexh.capitalism.mail;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.designer.DesignerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

// The Mailbox's contents (27 slots, like a chest) and whose it is. The number of filled slots (letters) and the owner
// are sent to the clients, for the flag and the number the owner sees over it (client/MailboxRenderer).
public class MailboxBlockEntity extends DesignerBlockEntity {

    public static final int SIZE = 27;

    private @Nullable UUID owner;
    private String ownerName = "";
    private int letters;

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAILBOX.get(), pos, state, "Mailbox", SIZE);
    }

    // (clients) who may read it: the owner, or for the Town Hall Mailbox the Mayor and Vice Mayor when it last synced
    private final java.util.List<UUID> readers = new java.util.ArrayList<>();

    // the Town Hall Mailbox: the Mayor's and Vice Mayor's (whoever holds the office now), counted for no one
    public boolean town() {
        return this.getBlockState().is(com.alexh.capitalism.core.ModBlocks.TOWN_MAILBOX.get());
    }

    public boolean hasOwner() {
        return this.owner != null || this.town();
    }

    public boolean isOwner(Player player) {
        if (!this.town()) return player.getUUID().equals(this.owner);
        if (player.level().getServer() == null) return this.readers.contains(player.getUUID());
        var office = com.alexh.capitalism.town.TownHall.get(player.level().getServer()).officeOf(player.getUUID());
        return office == com.alexh.capitalism.town.TownHall.Office.MAYOR || office == com.alexh.capitalism.town.TownHall.Office.VICE_MAYOR;
    }

    // (clients) does this player see the count over it
    public boolean shownTo(UUID player) {
        return this.town() ? this.readers.contains(player) : player.equals(this.owner);
    }

    // the Mayor and the Vice Mayor (server)
    private java.util.List<com.alexh.capitalism.town.TownHall.Holder> officials() {
        java.util.List<com.alexh.capitalism.town.TownHall.Holder> out = new java.util.ArrayList<>();
        if (this.level == null || this.level.getServer() == null) return out;
        var hall = com.alexh.capitalism.town.TownHall.get(this.level.getServer());
        for (var office : new com.alexh.capitalism.town.TownHall.Office[]{com.alexh.capitalism.town.TownHall.Office.MAYOR, com.alexh.capitalism.town.TownHall.Office.VICE_MAYOR}) {
            var holder = hall.holder(office);
            if (holder != null) out.add(holder);
        }
        return out;
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public void setOwner(UUID owner, String name) {
        this.owner = owner;
        this.ownerName = name;
        // the gold in it counts toward its owner on the Gold Leaderboard (economy/GoldLeaderboard), like a chest
        if (this.level instanceof net.minecraft.server.level.ServerLevel server) {
            this.setData(com.alexh.capitalism.core.ModAttachments.CONTAINER_OWNER, new com.alexh.capitalism.economy.ContainerOwner(owner, name, true));
            com.alexh.capitalism.economy.GoldLedger.get(server.getServer()).setContainer(
                net.minecraft.core.GlobalPos.of(server.dimension(), this.worldPosition), owner, com.alexh.capitalism.economy.GoldMoney.deepCount(this));
        }
        this.setChanged();
        this.sync();
    }

    // how many slots have something in them
    public int letters() {
        return this.letters;
    }

    private int countLetters() {
        int n = 0;
        for (ItemStack stack : this.items) if (!stack.isEmpty()) n++;
        return n;
    }

    public int freeSlots() {
        return SIZE - this.countLetters();
    }

    // Puts mail in: onto matching stacks first, then into empty slots. Returns what didn't fit.
    public ItemStack deliver(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < SIZE && !rest.isEmpty(); i++) {
            ItemStack here = this.items.get(i);
            if (here.isEmpty() || !ItemStack.isSameItemSameComponents(here, rest)) continue;
            int move = Math.min(rest.getCount(), here.getMaxStackSize() - here.getCount());
            if (move <= 0) continue;
            here.grow(move);
            rest.shrink(move);
        }
        for (int i = 0; i < SIZE && !rest.isEmpty(); i++) {
            if (!this.items.get(i).isEmpty()) continue;
            int move = Math.min(rest.getCount(), rest.getMaxStackSize());
            this.items.set(i, rest.split(move));
        }
        if (rest.getCount() != stack.getCount()) this.setChanged();
        return rest;
    }

    // The owner gets the whole box; anyone else, the mail slot
    public void open(ServerPlayer player) {
        if (this.town()) this.sync();
        if (this.isOwner(player)) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> MailboxMenu.owner(id, inventory, this), Component.literal(this.town() ? "Town Hall Mail" : "Your Mailbox")));
        } else {
            ContainerData data = new ContainerData() {
                @Override
                public int get(int index) {
                    return MailboxBlockEntity.this.freeSlots();
                }

                @Override
                public void set(int index, int value) {
                }

                @Override
                public int getCount() {
                    return 1;
                }
            };
            String to = this.recipient();
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> MailboxMenu.sender(id, inventory, this, data), Component.literal("Mail for " + to)));
        }
    }

    // who it's for, in words
    public String recipient() {
        return this.town() ? "the Town Hall" : this.ownerName.isEmpty() ? "someone" : this.ownerName;
    }

    // someone left mail: tell the owner (the Mayor and Vice Mayor, for the Town Hall's) if they're on
    public void notifyOwner(ServerPlayer sender, int items) {
        if (this.level == null || this.level.getServer() == null) return;
        java.util.List<UUID> to = new java.util.ArrayList<>();
        if (this.town()) for (var holder : this.officials()) to.add(holder.id());
        else if (this.owner != null) to.add(this.owner);
        for (UUID id : to) {
            ServerPlayer owner = this.level.getServer().getPlayerList().getPlayer(id);
            if (owner == null || owner == sender) continue;
            owner.sendSystemMessage(Component.literal("✉ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(sender.getGameProfile().name()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" left " + (this.town() ? "the Town Hall" : "you") + " mail (" + items + (items == 1 ? " item" : " items") + "). Check "
                    + (this.town() ? "the Town Hall Mailbox." : "your mailbox.")).withStyle(ChatFormatting.WHITE)));
            owner.level().playSound(null, owner.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    // ── keeping the clients up to date (the flag, the number) ──

    @Override
    public void setChanged() {
        super.setChanged();
        int now = this.countLetters();
        if (now != this.letters) {
            this.letters = now;
            this.sync();
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.letters = input.getIntOr("letters", this.countLetters());
        this.readers.clear();
        input.read("readers", com.mojang.serialization.Codec.STRING.listOf()).ifPresent(ids -> ids.forEach(s -> this.readers.add(UUID.fromString(s))));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.putString("owner_name", this.ownerName);
        output.putInt("letters", this.countLetters());
    }

    // clients get the owner and the count, not the mail itself
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.owner != null) tag.store("owner", UUIDUtil.CODEC, this.owner);
        tag.putString("owner_name", this.ownerName);
        tag.putInt("letters", this.countLetters());
        if (this.town()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (var holder : this.officials()) list.add(net.minecraft.nbt.StringTag.valueOf(holder.id().toString()));
            tag.put("readers", list);
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // (menus are opened through open(), which picks the owner's or the sender's)
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return MailboxMenu.owner(id, inventory, this);
    }

    // mail goes in and out by hand only: no hoppers
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }
}

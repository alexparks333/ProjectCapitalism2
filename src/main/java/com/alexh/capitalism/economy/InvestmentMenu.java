package com.alexh.capitalism.economy;

import com.alexh.capitalism.core.ModMenus;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// The Investment Chest open (client/InvestmentChestScreen): on the left YOUR vault, three slots that take only Gold
// Ingots (your InvestmentBank account; nobody else's ever shows); an arrow; on the right what it pays each morning and
// the returns waiting, with COLLECT (button 0) to take them. Your inventory along the bottom.
// Data (16 bits a slot over the network, so each figure in nuggets is two: low 15 bits, then the rest): what's in it,
// returns waiting, all it ever earned; then today's market rate (%) and how many days it lasts (today included).
public class InvestmentMenu extends AbstractContainerMenu {

    public static final int COLLECT = 0;
    public static final int DATA = 8;
    // where things sit (client/InvestmentChestScreen draws round them)
    public static final int VAULT_X = 23;
    public static final int VAULT_Y = 46;
    public static final int INV_X = 50;
    public static final int INV_Y = 140;

    private final Container vault;
    private final ContainerData data;
    private final @Nullable InvestmentChestBlockEntity chest;

    // client
    public InvestmentMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(InvestmentBank.SLOTS) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return stack.is(Items.GOLD_INGOT);
            }
        }, new SimpleContainerData(DATA), null);
    }

    public InvestmentMenu(int id, Inventory inventory, Container vault, ContainerData data, @Nullable InvestmentChestBlockEntity chest) {
        super(ModMenus.INVESTMENT.get(), id);
        checkContainerSize(vault, InvestmentBank.SLOTS);
        checkContainerDataCount(data, DATA);
        this.vault = vault;
        this.data = data;
        this.chest = chest;
        for (int i = 0; i < InvestmentBank.SLOTS; i++) {
            this.addSlot(new Slot(vault, i, VAULT_X + i * 18, VAULT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.GOLD_INGOT);
                }
            });
        }
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
        this.addDataSlots(data);
        if (chest != null) chest.startOpen();
    }

    // the live figures for one account (server)
    public static ContainerData liveData(InvestmentBank.Account account, net.minecraft.server.MinecraftServer server) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long v = switch (index / 2) {
                    case 0 -> account.invested();
                    case 1 -> account.returns();
                    case 2 -> account.earned();
                    default -> 0;
                };
                if (index == 6) return InvestmentBank.rateOn(server, InvestmentBank.today(server));
                if (index == 7) return InvestmentBank.daysLeft(InvestmentBank.today(server));
                v = Math.max(0, Math.min(v, (1L << 30) - 1));
                return (int) (index % 2 == 0 ? v & 0x7FFF : v >> 15);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA;
            }
        };
    }

    private long figure(int i) {
        return (this.data.get(i * 2) & 0x7FFFL) | ((long) (this.data.get(i * 2 + 1) & 0x7FFF) << 15);
    }

    // (client and server) in nuggets
    public long invested() {
        return this.figure(0);
    }

    public long returns() {
        return this.figure(1);
    }

    public long earned() {
        return this.figure(2);
    }

    public int rate() {
        return this.data.get(6);
    }

    // days left at today's rate, today included
    public int daysLeft() {
        return this.data.get(7);
    }

    // a day's return at this rate (what InvestmentBank.dailyReturn pays)
    public long dailyOn(long nuggets) {
        return Math.max(0, Math.min(nuggets, InvestmentBank.MAX)) * this.rate() / 100;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != COLLECT || !(player instanceof ServerPlayer sp)) return false;
        InvestmentBank.Account account = InvestmentBank.get(sp.level().getServer()).account(sp.getUUID());
        if (account != this.vault) return false;
        long paid = account.collect(sp);
        if (paid <= 0) {
            sp.sendOverlayMessage(Component.literal("Nothing to collect yet. Money takes time! (One morning, to be exact.)").withStyle(ChatFormatting.GRAY));
            return true;
        }
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.8F);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.7F, 1.4F);
        sp.sendOverlayMessage(Component.literal("Cha-ching! +" + GoldMoney.format(paid)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int vault = InvestmentBank.SLOTS;
        if (index < vault) {
            if (!this.moveItemStackTo(stack, vault, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!stack.is(Items.GOLD_INGOT) || !this.moveItemStackTo(stack, 0, vault, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.chest == null) return true;
        return !this.chest.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.chest.getBlockPos())) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.chest != null) this.chest.stopOpen();
    }
}

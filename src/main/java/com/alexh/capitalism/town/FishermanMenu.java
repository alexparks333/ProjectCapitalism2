package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.fishing.FishMarket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// Talking to the Fisherman (client/FishermanScreen), two tabs:
//  - Sell: a row of SELL_SLOTS slots that take only what he buys (fishing/FishMarket.buys: the rare catches and Soggy
//    Notes, never cod, salmon or junk); SELL pays the whole offer in gold (GoldMoney.give) and takes them.
//  - Mount: one slot for a catch that can go on a wall plaque (FishMarket.mountable); MOUNT takes the plaque's price
//    (FishMarket.plaquePrice, through NpcPricing) and gives the plaque (FishMarket.plaqueFor) with the catch's story on it.
// Menu buttons: SELL, MOUNT, and TAB + which tab (the screen tells the server which tab shows, for shift-clicks).
// Anything left in the slots goes back to you when you close it. Your inventory is along the bottom.
public class FishermanMenu extends AbstractContainerMenu {

    public static final int SELL = 100;
    public static final int MOUNT = 101;
    public static final int CLAIM = 102;
    public static final int TAB = 200;
    public static final int SELL_SLOTS = 9;
    // the slot indexes: the sell row, then the mount slot, then your inventory
    public static final int MOUNT_SLOT = SELL_SLOTS;
    public static final int INV_START = SELL_SLOTS + 1;
    // where the slots sit in the screen
    public static final int SELL_X = 21;
    public static final int SELL_DX = 24;
    public static final int SELL_Y = 62;
    public static final int MOUNT_X = 52;
    public static final int MOUNT_Y = 86;
    public static final int INV_X = 44;
    public static final int INV_Y = 160;

    private final Container selling = new SimpleContainer(SELL_SLOTS);
    private final Container mounting = new SimpleContainer(1);
    private final @Nullable Fisherman fisherman;
    // (client) only the showing tab's slots are there; the server keeps them all and is told the tab for shift-clicks
    private final boolean client;
    public int tab;

    public FishermanMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, true);
    }

    public FishermanMenu(int containerId, Inventory inventory, @Nullable Fisherman fisherman) {
        this(containerId, inventory, fisherman, false);
    }

    private FishermanMenu(int containerId, Inventory inventory, @Nullable Fisherman fisherman, boolean client) {
        super(ModMenus.FISHERMAN.get(), containerId);
        this.fisherman = fisherman;
        this.client = client;
        for (int i = 0; i < SELL_SLOTS; i++) {
            this.addSlot(new Slot(this.selling, i, SELL_X + i * SELL_DX, SELL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FishMarket.buys(stack);
                }

                @Override
                public boolean isActive() {
                    return FishermanMenu.this.showing(0);
                }
            });
        }
        this.addSlot(new Slot(this.mounting, 0, MOUNT_X, MOUNT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FishMarket.mountable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return FishermanMenu.this.showing(1);
            }
        });
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
    }

    private boolean showing(int which) {
        return !this.client || this.tab == which;
    }

    public @Nullable Fisherman fisherman() {
        return this.fisherman;
    }

    // what's in the mount slot
    public ItemStack toMount() {
        return this.slots.get(MOUNT_SLOT).getItem();
    }

    // what he'd pay for everything in the sell row (nuggets)
    public long offer() {
        long total = 0;
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = this.selling.getItem(i);
            total += FishMarket.valueOf(stack) * stack.getCount();
        }
        return total;
    }

    private static void say(ServerPlayer player, String line, int color) {
        player.sendOverlayMessage(Component.literal(line).withStyle(s -> s.withColor(TextColor.fromRgb(color)).withItalic(true)));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= TAB && id < TAB + 3) {
            this.tab = id - TAB;
            return true;
        }
        if (!(player instanceof ServerPlayer customer)) return true;
        if (id == SELL) return this.sell(customer);
        if (id == MOUNT) return this.mount(customer);
        if (id == CLAIM) return com.alexh.capitalism.fishing.CatchCollection.claim(customer);
        return false;
    }

    // everything in the sell row, for its whole offer in gold
    private boolean sell(ServerPlayer customer) {
        long total = this.offer();
        if (total <= 0) {
            say(customer, "\"Show me something rare out o' the deep, and I'll pay ye for it.\"", Fisherman.COLOR);
            return false;
        }
        for (int i = 0; i < SELL_SLOTS; i++) this.selling.setItem(i, ItemStack.EMPTY);
        GoldMoney.give(customer, total);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.8F, 0.9F);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.9F, 1.6F);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 0.7F);
        say(customer, "\"A fine haul! " + GoldMoney.format(total) + ", fair and square.\"", Fisherman.COLOR);
        this.broadcastChanges();
        return true;
    }

    // the catch in the mount slot onto a wall plaque, for its price
    private boolean mount(ServerPlayer customer) {
        ItemStack fish = this.toMount();
        if (fish.isEmpty() || !FishMarket.mountable(fish)) {
            say(customer, "\"Bring me a catch worth hangin' on a wall.\"", Fisherman.COLOR);
            return false;
        }
        long price = FishMarket.plaquePrice(fish, customer);
        if (!customer.isCreative() && !GoldMoney.take(customer, price)) {
            say(customer, "\"That plaque's " + GoldMoney.format(price) + ", matey. Come back with the gold.\"", 0xFF5050);
            return false;
        }
        ItemStack plaque = FishMarket.plaqueFor(fish);
        this.mounting.setItem(0, ItemStack.EMPTY);
        give(customer, plaque);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 1.0F, 0.8F);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 1.0F, 0.9F);
        customer.level().playSound(null, customer.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 0.7F);
        say(customer, "\"There she is. A beauty for yer wall.\"", Fisherman.COLOR);
        this.broadcastChanges();
        return true;
    }

    // shift-click: from your inventory into the showing tab's slot(s); out of a slot back to your inventory
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (this.tab == 1) {
            if (!FishMarket.mountable(stack) || this.slots.get(MOUNT_SLOT).hasItem()) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, MOUNT_SLOT, MOUNT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            if (!FishMarket.buys(stack)) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(stack, 0, SELL_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.selling);
        this.clearContainer(player, this.mounting);
        if (this.fisherman != null && this.fisherman.customer() == player) this.fisherman.setCustomer(null);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.fisherman == null || (this.fisherman.isAlive() && this.fisherman.distanceToSqr(player) < 8 * 8);
    }
}

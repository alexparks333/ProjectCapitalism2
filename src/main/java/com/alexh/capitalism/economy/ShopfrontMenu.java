package com.alexh.capitalism.economy;

import com.alexh.capitalism.core.ModMenus;
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

// Shopfront menu in two versions:
//  - Owner:    4 listing pictures (click with an item to copy it), Storage (the real stock), and per listing:
//              amount per purchase + price in nuggets / ingots / blocks (◀ ▶ buttons). Collect earnings.
//  - Customer: the 4 listings (look only) with Buy buttons.
// Buttons go through clickMenuButton, so the server checks and does everything.
public class ShopfrontMenu extends AbstractContainerMenu {

    // Synced numbers: per listing [amount, nuggets, ingots, blocks, sales in stock, version], then earnings.
    // Each value is split over two data slots because menus sync data as shorts.
    public static final int VALUES_PER_LISTING = ShopfrontBlockEntity.FIELDS + 2;
    private static final int VALUES = ShopfrontBlockEntity.LISTINGS * VALUES_PER_LISTING + 1;
    public static final int DATA_COUNT = VALUES * 2;

    // Button ids
    public static final int BUTTON_BUY = 0;       // + listing + LISTINGS * version seen (customer)
    public static final int BUTTON_SET_ITEM = 0;  // + listing: copy the cursor item as the listing picture (owner)
    public static final int BUTTON_COLLECT = 50;  // (owner)
    public static final int BUTTON_ADJUST = 100;  // + (listing * FIELDS + field) * 2 + (0 = down, 1 = up) (owner)

    // Layout (matches ShopfrontScreen)
    public static final int WIDTH = 280;
    public static final int OWNER_HEIGHT = 254;
    public static final int CUSTOMER_HEIGHT = 220;
    public static final int SLOT_X = 10;
    public static final int OWNER_LISTING_Y = 32;
    public static final int OWNER_ROW = 20;
    public static final int OWNER_SLOT_OFFSET = 1;
    public static final int STORAGE_X = 59;
    public static final int STORAGE_Y = 124;
    public static final int CUSTOMER_LISTING_Y = 32;
    public static final int CUSTOMER_ROW = 22;
    public static final int CUSTOMER_SLOT_OFFSET = 2;
    public static final int OWNER_INV_Y = 174;
    public static final int CUSTOMER_INV_Y = 138;
    public static final int INV_X = 59;

    private final boolean owner;
    private final Container listings;
    private final Container storage;
    private final ContainerData data;
    private final @Nullable ShopfrontBlockEntity shop;
    private final int invStart;

    // Client side
    public static ShopfrontMenu ownerClient(int id, Inventory inventory) {
        return new ShopfrontMenu(ModMenus.SHOPFRONT_OWNER.get(), id, inventory, null,
            new SimpleContainer(ShopfrontBlockEntity.LISTINGS), new SimpleContainer(ShopfrontBlockEntity.STORAGE_SLOTS), new SimpleContainerData(DATA_COUNT), true);
    }

    public static ShopfrontMenu customerClient(int id, Inventory inventory) {
        return new ShopfrontMenu(ModMenus.SHOPFRONT_CUSTOMER.get(), id, inventory, null,
            new SimpleContainer(ShopfrontBlockEntity.LISTINGS), new SimpleContainer(0), new SimpleContainerData(DATA_COUNT), false);
    }

    // Server side: the owner also gets the Storage; both see the listing pictures.
    public static ShopfrontMenu owner(int id, Inventory inventory, ShopfrontBlockEntity shop) {
        return new ShopfrontMenu(ModMenus.SHOPFRONT_OWNER.get(), id, inventory, shop, shop.displayView(), shop, shop.dataAccess(), true);
    }

    public static ShopfrontMenu customer(int id, Inventory inventory, ShopfrontBlockEntity shop) {
        return new ShopfrontMenu(ModMenus.SHOPFRONT_CUSTOMER.get(), id, inventory, shop, shop.displayView(), new SimpleContainer(0), shop.dataAccess(), false);
    }

    private ShopfrontMenu(MenuType<?> type, int id, Inventory inventory, @Nullable ShopfrontBlockEntity shop,
                          Container listings, Container storage, ContainerData data, boolean owner) {
        super(type, id);
        checkContainerDataCount(data, DATA_COUNT);
        this.owner = owner;
        this.listings = listings;
        this.storage = storage;
        this.data = data;
        this.shop = shop;

        // Listing pictures: look only (the owner sets them by clicking, which the screen turns into a button)
        int listingY = owner ? OWNER_LISTING_Y + OWNER_SLOT_OFFSET : CUSTOMER_LISTING_Y + CUSTOMER_SLOT_OFFSET;
        int row = owner ? OWNER_ROW : CUSTOMER_ROW;
        for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
            this.addSlot(new Slot(listings, i, SLOT_X, listingY + i * row) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        // Storage: the real stock (owner only)
        if (owner) {
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 9; c++) {
                    this.addSlot(new Slot(storage, c + r * 9, STORAGE_X + c * 18, STORAGE_Y + r * 18));
                }
            }
        }

        this.invStart = this.slots.size();
        this.addStandardInventorySlots(inventory, INV_X, owner ? OWNER_INV_Y : CUSTOMER_INV_Y);
        this.addDataSlots(data);
    }

    public boolean isOwner() {
        return this.owner;
    }

    // ── Synced numbers ─────────────────────────────────────────────────────

    public static int valueIndex(int dataIndex) {
        return dataIndex / 2;
    }

    public static int packHalf(int value, int dataIndex) {
        return dataIndex % 2 == 0 ? value & 0x7FFF : (value >> 15) & 0x7FFF;
    }

    private int value(int valueIndex) {
        return (this.data.get(valueIndex * 2) & 0x7FFF) | ((this.data.get(valueIndex * 2 + 1) & 0x7FFF) << 15);
    }

    public int field(int listing, int field) {
        return value(listing * VALUES_PER_LISTING + field);
    }

    public int salesInStock(int listing) {
        return value(listing * VALUES_PER_LISTING + ShopfrontBlockEntity.FIELDS);
    }

    public int version(int listing) {
        return value(listing * VALUES_PER_LISTING + ShopfrontBlockEntity.FIELDS + 1);
    }

    public int earnings() {
        return value(ShopfrontBlockEntity.LISTINGS * VALUES_PER_LISTING);
    }

    public long priceNuggets(int listing) {
        return field(listing, ShopfrontBlockEntity.FIELD_NUGGETS) * (long) GoldMoney.NUGGET
            + field(listing, ShopfrontBlockEntity.FIELD_INGOTS) * (long) GoldMoney.INGOT
            + field(listing, ShopfrontBlockEntity.FIELD_BLOCKS) * (long) GoldMoney.BLOCK;
    }

    public ItemStack listingItem(int listing) {
        return this.listings.getItem(listing);
    }

    // ── Buttons (run on the server) ────────────────────────────────────────

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.shop == null || !(player instanceof ServerPlayer serverPlayer)) return true;

        if (!this.owner) {
            int code = id - BUTTON_BUY;
            if (code >= 0) {
                this.shop.buy(serverPlayer, code % ShopfrontBlockEntity.LISTINGS, code / ShopfrontBlockEntity.LISTINGS);
            }
            return true;
        }

        // Owner actions: double-check it really is the owner.
        if (!player.getUUID().equals(this.shop.ownerId())) return false;
        if (id >= BUTTON_SET_ITEM && id < BUTTON_SET_ITEM + ShopfrontBlockEntity.LISTINGS) {
            this.shop.setTemplate(id - BUTTON_SET_ITEM, this.getCarried());
        } else if (id == BUTTON_COLLECT) {
            this.shop.collect(serverPlayer);
        } else if (id >= BUTTON_ADJUST && id < BUTTON_ADJUST + ShopfrontBlockEntity.LISTINGS * ShopfrontBlockEntity.FIELDS * 2) {
            int code = id - BUTTON_ADJUST;
            int listingField = code / 2;
            this.shop.adjustField(listingField / ShopfrontBlockEntity.FIELDS, listingField % ShopfrontBlockEntity.FIELDS, code % 2 == 0 ? -1 : 1);
        }
        return true;
    }

    // ── Shift-click (owner): Storage <-> inventory ─────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!this.owner || slotIndex < ShopfrontBlockEntity.LISTINGS) return ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < this.invStart) {
            if (!this.moveItemStackTo(stack, this.invStart, this.invStart + 36, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, ShopfrontBlockEntity.LISTINGS, this.invStart, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.shop == null || this.shop.stillValid(player);
    }
}

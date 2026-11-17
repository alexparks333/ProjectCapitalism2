package com.alexh.capitalism.client;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.ShopfrontBlockEntity;
import com.alexh.capitalism.economy.ShopfrontMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

// Market-stall style screen for the Shopfront.
//  Owner:    left = the 4 listings: click a box with an item to copy it as the picture (each row shows amount + price).
//            right = edit panel for the selected listing: Amount, Nuggets, Ingots, Blocks with ◀ ▶.
//            below = Storage, the real stock that purchases come out of.
//  Customer: one row per listing (item, amount, price, stock) with a Buy button.
// Every button is a menu button, so the server checks and does the work.
public class ShopfrontScreen extends AbstractContainerScreen<ShopfrontMenu> {

    // Warm wooden stall with a striped awning
    private static final int WOOD = 0xFF7A5234;
    private static final int WOOD_DARK = 0xFF4E3220;
    private static final int TRIM_LIGHT = 0xFFE8C88A;
    private static final int TRIM_DARK = 0xFF3A2414;
    private static final int STRIPE_RED = 0xFFD8453C;
    private static final int STRIPE_WHITE = 0xFFF4ECDC;
    private static final int SLOT = 0xFF2A1A10;
    private static final int SLOT_EDGE = 0xFFB8905C;
    private static final int SELECTED = 0xFFFFD24A;
    private static final int PRICE = 0xFFFFD24A;
    private static final int TEXT = 0xFFFFF4E0;
    private static final int TEXT_DIM = 0xFFD8C0A0;
    private static final int SOLD_OUT = 0xFFFF8A80;

    private static final ItemStack NUGGET = new ItemStack(Items.GOLD_NUGGET);
    private static final ItemStack INGOT = new ItemStack(Items.GOLD_INGOT);
    private static final ItemStack BLOCK = new ItemStack(Items.GOLD_BLOCK);

    // Owner layout
    private static final int LIST_RIGHT = 150;       // listings column ends here
    private static final int PANEL_X = 156;          // edit panel starts here
    private static final int PANEL_Y = ShopfrontMenu.OWNER_LISTING_Y;
    private static final int EDIT_ROW_Y = PANEL_Y + 12;   // first edit row
    private static final int EDIT_ROW = 16;
    private static final int ARROW_LEFT_X = PANEL_X + 30;
    private static final int NUMBER_CENTER_X = PANEL_X + 62;
    private static final int ARROW_RIGHT_X = PANEL_X + 82;
    private static final int[] EDIT_FIELDS = {
        ShopfrontBlockEntity.FIELD_AMOUNT, ShopfrontBlockEntity.FIELD_NUGGETS, ShopfrontBlockEntity.FIELD_INGOTS, ShopfrontBlockEntity.FIELD_BLOCKS
    };

    private final List<Button> buyButtons = new ArrayList<>();
    private int selected = 0;

    public ShopfrontScreen(ShopfrontMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ShopfrontMenu.WIDTH, menu.isOwner() ? ShopfrontMenu.OWNER_HEIGHT : ShopfrontMenu.CUSTOMER_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 20;
        this.inventoryLabelX = ShopfrontMenu.INV_X;
        this.inventoryLabelY = (this.menu.isOwner() ? ShopfrontMenu.OWNER_INV_Y : ShopfrontMenu.CUSTOMER_INV_Y) - 11;
        this.buyButtons.clear();

        if (this.menu.isOwner()) {
            // One ◀ ▶ pair per edit row; they act on whichever listing is selected.
            for (int row = 0; row < EDIT_FIELDS.length; row++) {
                int field = EDIT_FIELDS[row];
                int y = this.topPos + EDIT_ROW_Y + row * EDIT_ROW;
                this.addRenderableWidget(Button.builder(Component.literal("<"), b -> adjust(field, 0))
                    .bounds(this.leftPos + ARROW_LEFT_X, y, 14, 14).build());
                this.addRenderableWidget(Button.builder(Component.literal(">"), b -> adjust(field, 1))
                    .bounds(this.leftPos + ARROW_RIGHT_X, y, 14, 14).build());
            }
            this.addRenderableWidget(Button.builder(Component.literal("Collect"), b -> press(ShopfrontMenu.BUTTON_COLLECT))
                .bounds(this.leftPos + this.imageWidth - 62, this.topPos + 17, 54, 14).build());
        } else {
            for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
                int listing = i;
                int y = this.topPos + ShopfrontMenu.CUSTOMER_LISTING_Y + i * ShopfrontMenu.CUSTOMER_ROW + 1;
                this.buyButtons.add(this.addRenderableWidget(Button.builder(Component.literal("Buy"),
                        b -> press(ShopfrontMenu.BUTTON_BUY + listing + ShopfrontBlockEntity.LISTINGS * this.menu.version(listing)))
                    .bounds(this.leftPos + this.imageWidth - 44, y, 36, 18).build()));
            }
        }
    }

    private void adjust(int field, int direction) {
        press(ShopfrontMenu.BUTTON_ADJUST + (this.selected * ShopfrontBlockEntity.FIELDS + field) * 2 + direction);
    }

    private void press(int buttonId) {
        if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < this.buyButtons.size(); i++) {
            this.buyButtons.get(i).active = !this.menu.listingItem(i).isEmpty() && this.menu.priceNuggets(i) > 0 && this.menu.salesInStock(i) > 0;
        }
    }

    // Owner: clicking a listing row selects it for editing (clicking its slot also selects it).
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.menu.isOwner()) {
            for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
                int y = this.topPos + ShopfrontMenu.OWNER_LISTING_Y + i * ShopfrontMenu.OWNER_ROW;
                if (event.x() >= this.leftPos + 6 && event.x() < this.leftPos + LIST_RIGHT && event.y() >= y && event.y() < y + ShopfrontMenu.OWNER_ROW - 2) {
                    this.selected = i;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        // Listing boxes are pictures: the owner clicks one with an item on the cursor to copy it (it stays on the
        // cursor; an empty cursor clears the listing). Customers can't click them.
        if (slot != null && slotId >= 0 && slotId < ShopfrontBlockEntity.LISTINGS) {
            if (this.menu.isOwner()) {
                this.selected = slotId;
                press(ShopfrontMenu.BUTTON_SET_ITEM + slotId);
            }
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int l = this.leftPos;
        int t = this.topPos;
        int w = this.imageWidth;
        graphics.fill(l - 2, t - 2, l + w + 2, t + this.imageHeight + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + w + 1, t + this.imageHeight + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + w, t + this.imageHeight, WOOD);

        // Striped awning with a scalloped edge
        for (int x = 0; x < w; x += 12) {
            int color = (x / 12) % 2 == 0 ? STRIPE_RED : STRIPE_WHITE;
            graphics.fill(l + x, t, l + Math.min(x + 12, w), t + 12, color);
            graphics.fill(l + x + 2, t + 12, l + Math.min(x + 10, w), t + 14, color);
        }

        boolean owner = this.menu.isOwner();
        int listingY = owner ? ShopfrontMenu.OWNER_LISTING_Y : ShopfrontMenu.CUSTOMER_LISTING_Y;
        int row = owner ? ShopfrontMenu.OWNER_ROW : ShopfrontMenu.CUSTOMER_ROW;
        int rowRight = owner ? LIST_RIGHT : w - 6;
        for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
            int y = t + listingY + i * row;
            if (owner && i == this.selected) {
                graphics.fill(l + 5, y - 1, l + rowRight + 1, y + row - 1, SELECTED);
            }
            graphics.fill(l + 6, y, l + rowRight, y + row - 2, WOOD_DARK);
        }
        if (owner) {
            graphics.fill(l + PANEL_X, t + PANEL_Y, l + w - 6, t + PANEL_Y + 4 * ShopfrontMenu.OWNER_ROW - 2, WOOD_DARK);
        }

        for (Slot slot : this.menu.slots) {
            int x = l + slot.x - 1;
            int y = t + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_DIM, false);
        if (this.menu.isOwner()) {
            ownerLabels(graphics);
        } else {
            customerLabels(graphics);
        }
    }

    private void ownerLabels(GuiGraphicsExtractor graphics) {
        String earnings = "Earnings: " + GoldMoney.format(this.menu.earnings());
        graphics.text(this.font, earnings, this.imageWidth - 66 - this.font.width(earnings), 20, PRICE, false);

        // Listings: slot, then "x16" and the price as gold icons
        for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
            int y = ShopfrontMenu.OWNER_LISTING_Y + i * ShopfrontMenu.OWNER_ROW;
            ItemStack item = this.menu.listingItem(i);
            if (item.isEmpty()) {
                graphics.text(this.font, "Click with an item", 32, y + 5, TEXT_DIM, false);
                continue;
            }
            graphics.text(this.font, "x" + this.menu.field(i, ShopfrontBlockEntity.FIELD_AMOUNT), 32, y + 5, TEXT, false);
            drawPrice(graphics, i, 56, y + 1, false);
        }

        // Edit panel for the selected listing
        int x = PANEL_X + 6;
        graphics.text(this.font, "Listing " + (this.selected + 1), x, PANEL_Y + 3, PRICE, false);
        graphics.text(this.font, "Amt", x, EDIT_ROW_Y + 3, TEXT, false);
        ItemStack[] icons = {NUGGET, INGOT, BLOCK};
        for (int row = 0; row < EDIT_FIELDS.length; row++) {
            int y = EDIT_ROW_Y + row * EDIT_ROW;
            if (row > 0) {
                graphics.item(icons[row - 1], x + 2, y - 1);
            }
            String value = String.valueOf(this.menu.field(this.selected, EDIT_FIELDS[row]));
            graphics.text(this.font, value, NUMBER_CENTER_X - this.font.width(value) / 2, y + 3, row == 0 ? TEXT : PRICE, false);
        }
        int sales = this.menu.salesInStock(this.selected);
        boolean hasItem = !this.menu.listingItem(this.selected).isEmpty();
        String stock = !hasItem ? "" : sales > 0 ? sales + " left" : "Sold out";
        graphics.text(this.font, stock, this.imageWidth - 10 - this.font.width(stock), PANEL_Y + 3, sales > 0 ? TEXT_DIM : SOLD_OUT, false);

        graphics.text(this.font, "Storage", ShopfrontMenu.STORAGE_X, ShopfrontMenu.STORAGE_Y - 10, TEXT_DIM, false);
    }

    private void customerLabels(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < ShopfrontBlockEntity.LISTINGS; i++) {
            int y = ShopfrontMenu.CUSTOMER_LISTING_Y + i * ShopfrontMenu.CUSTOMER_ROW;
            ItemStack item = this.menu.listingItem(i);
            if (item.isEmpty() || this.menu.priceNuggets(i) <= 0) {
                graphics.text(this.font, "-", 32, y + 6, TEXT_DIM, false);
                continue;
            }
            int amount = this.menu.field(i, ShopfrontBlockEntity.FIELD_AMOUNT);
            graphics.text(this.font, this.font.plainSubstrByWidth(amount + "x " + item.getHoverName().getString(), 102), 32, y + 2, TEXT, false);
            int sales = this.menu.salesInStock(i);
            graphics.text(this.font, sales > 0 ? sales + " left" : "Sold out", 32, y + 11, sales > 0 ? TEXT_DIM : SOLD_OUT, false);
            drawPrice(graphics, i, 140, y + 2, true);
        }
    }

    // Price as gold icons with counts, biggest first, skipping parts that are 0.
    private void drawPrice(GuiGraphicsExtractor graphics, int listing, int x, int y, boolean showNotForSale) {
        int[] counts = {
            this.menu.field(listing, ShopfrontBlockEntity.FIELD_BLOCKS),
            this.menu.field(listing, ShopfrontBlockEntity.FIELD_INGOTS),
            this.menu.field(listing, ShopfrontBlockEntity.FIELD_NUGGETS)
        };
        ItemStack[] icons = {BLOCK, INGOT, NUGGET};
        boolean any = false;
        for (int k = 0; k < counts.length; k++) {
            if (counts[k] <= 0) continue;
            any = true;
            graphics.item(icons[k], x, y);
            String n = String.valueOf(counts[k]);
            graphics.text(this.font, n, x + 17, y + 4, PRICE, false);
            x += 17 + this.font.width(n) + 5;
        }
        if (!any) {
            graphics.text(this.font, showNotForSale ? "Not for sale" : "Set a price", x, y + 4, TEXT_DIM, false);
        }
    }
}

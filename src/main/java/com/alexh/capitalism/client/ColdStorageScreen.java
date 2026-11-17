package com.alexh.capitalism.client;

import com.alexh.capitalism.farmer.ColdStorageBlockEntity;
import com.alexh.capitalism.farmer.ColdStorageMenu;
import com.alexh.capitalism.farmer.Freshness;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The Fridge / Freezer (farmer/ColdStorageMenu), drawn here: three rows of slots like a chest, then the fuel slot (a
// ghost of coal or ice when it's empty) with a gauge of the fuel burning and how much slower meat rots in it. The
// Fridge is plain white enamel; the Freezer is frozen all over: an ice-blue panel crusted with frost round its edges,
// icicles hanging along the top and under the rows, snowflakes drifting down behind the slots, frosted slots.
public class ColdStorageScreen extends AbstractContainerScreen<ColdStorageMenu> {

    private static final int ENAMEL = 0xFFE8EEF2;
    private static final int ENAMEL_EDGE = 0xFF8A98A4;
    private static final int INSIDE = 0xFFC4D8E4;
    private static final int TEXT = 0xFF34414C;
    private static final int SLOT_EDGE = 0xFF5A6A78;
    private static final int SLOT_LIP = 0xFFF6FAFC;
    private static final int SLOT_INSIDE = 0xFF9EB2C0;

    public ColdStorageScreen(ColdStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, ColdStorageMenu.HEIGHT);
        this.inventoryLabelY = ColdStorageMenu.INV_Y - 11;
    }

    // the Freezer's colours
    private static final int ICE_EDGE = 0xFF3E6E9C;
    private static final int FROST = 0xFFF0FAFF;
    private static final int ICICLE = 0xFFDCF2FF;
    private static final int ICICLE_TIP = 0xFF8EC4EC;
    private static final int ICE_TEXT = 0xFF1E3E5E;
    private static final int ICE_SLOT_EDGE = 0xFF46769E;
    private static final int ICE_SLOT_INSIDE = 0xFF9CC8E8;

    private static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    private void icicles(GuiGraphicsExtractor graphics, int x0, int x1, int y, int seed) {
        for (int x = x0; x < x1; x += 3 + hash(x, seed) % 3) {
            int len = 2 + hash(x, seed + 1) % 6;
            for (int k = 0; k < len; k++) {
                int width = k < len / 2 ? 2 : 1;
                graphics.fill(x, y + k, x + width, y + k + 1, k == len - 1 ? ICICLE_TIP : ICICLE);
            }
        }
    }

    private void freezerPanel(GuiGraphicsExtractor graphics, int x0, int y0, int w, int h) {
        // ice blue, lighter at the top
        for (int y = 0; y < h; y += 2) {
            float t = y / (float) h;
            int r = (int) (204 - 44 * t);
            int g = (int) (232 - 30 * t);
            int b = (int) (250 - 14 * t);
            graphics.fill(x0, y0 + y, x0 + w, y0 + Math.min(h, y + 2), 0xFF000000 | (r << 16) | (g << 8) | b);
        }
        // frost crusted round the edges, in blotches
        for (int i = 0; i < w; i += 2) {
            int top = 2 + hash(i, 7) % 4;
            int bottom = 2 + hash(i, 9) % 4;
            graphics.fill(x0 + i, y0, x0 + i + 2, y0 + top, FROST);
            graphics.fill(x0 + i, y0 + h - bottom, x0 + i + 2, y0 + h, FROST);
        }
        for (int j = 0; j < h; j += 2) {
            int left = 2 + hash(j, 11) % 4;
            int right = 2 + hash(j, 13) % 4;
            graphics.fill(x0, y0 + j, x0 + left, y0 + j + 2, FROST);
            graphics.fill(x0 + w - right, y0 + j, x0 + w, y0 + j + 2, FROST);
        }
        // the outline, then snow drifting down (behind the slots)
        graphics.fill(x0, y0, x0 + w, y0 + 1, ICE_EDGE);
        graphics.fill(x0, y0 + h - 1, x0 + w, y0 + h, ICE_EDGE);
        graphics.fill(x0, y0, x0 + 1, y0 + h, ICE_EDGE);
        graphics.fill(x0 + w - 1, y0, x0 + w, y0 + h, ICE_EDGE);
        long now = net.minecraft.util.Util.getMillis();
        for (int i = 0; i < 26; i++) {
            int speed = 12 + hash(i, 3) % 14;
            int x = x0 + 4 + hash(i, 1) % (w - 8) + (int) (Math.sin(now / 700.0 + i) * 3);
            int y = y0 + (int) ((hash(i, 2) % h + now * speed / 1000) % h);
            graphics.fill(x, y, x + 1 + (i % 3 == 0 ? 1 : 0), y + 1 + (i % 3 == 0 ? 1 : 0), 0xD0FFFFFF);
        }
        this.icicles(graphics, x0 + 3, x0 + w - 3, y0 + 1, 17);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        boolean freezer = this.menu.kind() == ColdStorageBlockEntity.Kind.FREEZER;
        if (freezer) {
            this.freezerPanel(graphics, x0, y0, w, h);
            this.freezerSlots(graphics, x0, y0);
            return;
        }
        graphics.fill(x0, y0, x0 + w, y0 + h, ENAMEL_EDGE);
        graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, ENAMEL);
        // the frosty inside round the storage rows, with a bit of frost along its top
        graphics.fill(x0 + 5, y0 + 15, x0 + w - 5, y0 + 75, freezer ? 0xFFB8D4E8 : INSIDE);
        for (int x = x0 + 6; x < x0 + w - 6; x += 3) graphics.fill(x, y0 + 15, x + 2, y0 + 16 + (x % 7 == 0 ? 2 : 1), 0xFFF4FAFF);
        for (int i = 0; i < ColdStorageBlockEntity.SLOTS; i++) {
            DesignerDraw.slot(graphics, x0 + 7 + (i % 9) * 18, y0 + ColdStorageMenu.ROWS_Y - 1 + (i / 9) * 18, 18, SLOT_EDGE, SLOT_LIP, SLOT_INSIDE);
        }
        // the fuel slot and its gauge
        int fx = x0 + ColdStorageMenu.FUEL_X;
        int fy = y0 + ColdStorageMenu.FUEL_Y;
        DesignerDraw.slot(graphics, fx - 1, fy - 1, 18, SLOT_EDGE, freezer ? 0xFF8EC8F0 : 0xFF6A6A6A, SLOT_INSIDE);
        if (this.menu.getSlot(ColdStorageBlockEntity.FUEL).getItem().isEmpty()) {
            DesignerDraw.ghost(graphics, new ItemStack(freezer ? Items.ICE : Items.COAL), fx, fy, SLOT_INSIDE);
        }
        int gx = fx + 22;
        int gy = fy + 5;
        graphics.fill(gx, gy, gx + 50, gy + 6, 0xFF4A5864);
        int filled = Math.round(48 * this.menu.burnShare());
        if (filled > 0) graphics.fill(gx + 1, gy + 1, gx + 1 + filled, gy + 5, freezer ? 0xFF7AD0FF : 0xFFE08A3A);
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? ColdStorageMenu.INV_Y - 1 + (i / 9) * 18 : ColdStorageMenu.INV_Y + 57);
            DesignerDraw.slot(graphics, x, y, 18, SLOT_EDGE, SLOT_LIP, 0xFFB4C2CC);
        }
    }

    // The Freezer's slots: frosted, with icicles hanging under the storage rows, and the fuel slot with its gauge
    private void freezerSlots(GuiGraphicsExtractor graphics, int x0, int y0) {
        graphics.fill(x0 + 5, y0 + 15, x0 + this.imageWidth - 5, y0 + 75, 0x50FFFFFF);
        for (int i = 0; i < ColdStorageBlockEntity.SLOTS; i++) {
            DesignerDraw.slot(graphics, x0 + 7 + (i % 9) * 18, y0 + ColdStorageMenu.ROWS_Y - 1 + (i / 9) * 18, 18, ICE_SLOT_EDGE, FROST, ICE_SLOT_INSIDE);
        }
        this.icicles(graphics, x0 + 7, x0 + this.imageWidth - 7, y0 + ColdStorageMenu.ROWS_Y - 1 + 3 * 18, 23);
        int fx = x0 + ColdStorageMenu.FUEL_X;
        int fy = y0 + ColdStorageMenu.FUEL_Y;
        DesignerDraw.slot(graphics, fx - 1, fy - 1, 18, ICE_SLOT_EDGE, 0xFF8EC8F0, ICE_SLOT_INSIDE);
        if (this.menu.getSlot(ColdStorageBlockEntity.FUEL).getItem().isEmpty()) {
            DesignerDraw.ghost(graphics, new ItemStack(Items.ICE), fx, fy, ICE_SLOT_INSIDE);
        }
        int gx = fx + 22;
        int gy = fy + 5;
        graphics.fill(gx, gy, gx + 50, gy + 6, ICE_EDGE);
        int filled = Math.round(48 * this.menu.burnShare());
        if (filled > 0) {
            graphics.fill(gx + 1, gy + 1, gx + 1 + filled, gy + 5, 0xFF7AD0FF);
            graphics.fill(gx + 1, gy + 1, gx + 1 + filled, gy + 2, 0xFFD8F4FF);
        }
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? ColdStorageMenu.INV_Y - 1 + (i / 9) * 18 : ColdStorageMenu.INV_Y + 57);
            DesignerDraw.slot(graphics, x, y, 18, ICE_SLOT_EDGE, FROST, 0xFFB4D6EE);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        boolean freezer = this.menu.kind() == ColdStorageBlockEntity.Kind.FREEZER;
        int text = freezer ? ICE_TEXT : TEXT;
        graphics.text(this.font, this.title, 8, 5, text, false);
        ColdStorageBlockEntity.Kind kind = this.menu.kind();
        String speed = "Meat lasts " + kind.slower + "x";
        graphics.text(this.font, speed, this.imageWidth - 8 - this.font.width(speed), 5, 0xFF2A6A9A, false);
        int left = this.menu.burnLeft();
        // burning (with meat in it to keep), idle (fuel but nothing to keep cold), or out of fuel
        boolean meat = false;
        for (int i = 0; i < ColdStorageBlockEntity.SLOTS; i++) meat |= Freshness.rots(this.menu.getSlot(i).getItem());
        boolean hasFuel = left > 0 || !this.menu.getSlot(ColdStorageBlockEntity.FUEL).getItem().isEmpty();
        String fuel;
        int color;
        if (!hasFuel) {
            fuel = kind == ColdStorageBlockEntity.Kind.FREEZER ? "Add Ice" : "Add Coal";
            color = 0xFFA04030;
        } else if (!meat) {
            fuel = "Idle: no meat";
            color = 0xFF6A7A88;
        } else {
            fuel = left > 0 ? "Burning " + Freshness.clock(left) : "Starting...";
            color = text;
        }
        graphics.text(this.font, fuel, ColdStorageMenu.FUEL_X + 76, ColdStorageMenu.FUEL_Y + 4, color, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, text, false);
    }
}

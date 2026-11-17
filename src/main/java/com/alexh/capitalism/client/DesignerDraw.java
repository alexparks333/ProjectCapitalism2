package com.alexh.capitalism.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

// Little drawing pieces the Designer's two screens share (SewingMachineScreen, DesignerTableScreen): slot frames in
// their colors, ghost items (what goes in an empty slot), a gold selection frame and dashed thread.
final class DesignerDraw {

    private DesignerDraw() {
    }

    // A sunken slot frame (its top-left corner is one pixel up and left of the item): dark edge, light bottom-right lip
    static void slot(GuiGraphicsExtractor graphics, int x, int y, int size, int edge, int lip, int inside) {
        graphics.fill(x, y, x + size, y + size, edge);
        graphics.fill(x + 1, y + 1, x + size, y + size, lip);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, inside);
    }

    // What goes in an empty slot: the item, faded into the slot's color
    static void ghost(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int inside) {
        graphics.item(stack, x, y);
        graphics.nextStratum();
        graphics.fill(x, y, x + 16, y + 16, 0xB0000000 | (inside & 0xFFFFFF));
    }

    // A gold frame round a box (selected things), with a brighter top-left
    static void goldFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x - 1, y - 1, x + w + 1, y, 0xFFF6DC8E);
        graphics.fill(x - 1, y - 1, x, y + h + 1, 0xFFF6DC8E);
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFB08A34);
        graphics.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFFB08A34);
    }

    // The line above the inventory: what's wrong (in bold orange with a shadow, on a dark plate across the whole line, in
    // place of the "Inventory" label so they never overlap), or else "Inventory" with a short green note on the right
    static void status(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component inventory,
                       @org.jspecify.annotations.Nullable String problem, String ok, int width, int y, int label) {
        if (problem != null) {
            graphics.fill(5, y - 2, width - 5, y + 10, 0xF01A0E08);
            graphics.fill(5, y - 2, width - 5, y - 1, 0xFF8E6E26);
            graphics.fill(5, y + 9, width - 5, y + 10, 0xFF8E6E26);
            graphics.centeredText(font, "⚠ " + problem, width / 2, y, 0xFFFFA040);
            return;
        }
        graphics.text(font, inventory, 8, y, label, false);
        if (!ok.isEmpty()) graphics.text(font, ok, width - 8 - font.width(ok), y, 0xFF9CF08A, true);
    }

    // A dashed line of thread from one point to another (straight, 1 pixel)
    static void thread(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, int offset) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            if (((i + offset) / 2) % 2 == 1) continue;
            int x = x0 + Math.round((x1 - x0) * (i / (float) Math.max(1, steps)));
            int y = y0 + Math.round((y1 - y0) * (i / (float) Math.max(1, steps)));
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}

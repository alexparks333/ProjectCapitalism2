package com.alexh.capitalism.client;

import com.alexh.capitalism.designer.Bag;
import com.alexh.capitalism.designer.BagSlot;
import com.alexh.capitalism.designer.Bags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// The bag on your back, next to the survival inventory (mixin/InventoryScreenMixin): a stitched leather pouch hanging
// off the inventory's right side with the bag's name on a little brass plate and its slots (designer/BagSlot). Empty
// slots show a faint picture of what goes in them.
public final class BagPanel {

    private static final int LEATHER = 0xFF7A4A2A;
    private static final int LEATHER_DARK = 0xFF5A341C;
    private static final int LEATHER_LIGHT = 0xFF94603A;
    private static final int EDGE = 0xFF2E1A0E;
    private static final int STITCH = 0xFFF0DCB0;
    private static final int BRASS = 0xFFE2B85A;
    private static final int BRASS_DARK = 0xFF9A7428;
    private static final int TEXT = 0xFF3A2410;

    private BagPanel() {
    }

    // how many slots the worn bag has right now (the Traveler's Bag: 6, or 12 far from town)
    private static int liveSlots(Bag bag) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? bag.slots : bag.slots(com.alexh.capitalism.back.Back.peek(player));
    }

    // How tall the pouch is for this bag
    public static int height(Bag bag) {
        int rows = (liveSlots(bag) + BagSlot.COLUMNS - 1) / BagSlot.COLUMNS;
        return BagSlot.FIRST_Y - BagSlot.PANEL_Y + rows * 18 + 6;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Bag bag = Bags.worn(player);
        if (bag == null) return;
        int x0 = leftPos + BagSlot.PANEL_X;
        int y0 = topPos + BagSlot.PANEL_Y;
        int w = BagSlot.PANEL_WIDTH;
        int h = height(bag);

        // the leather: dark edge, the hide with a lighter top and a few grain flecks, a darker band at the bottom
        graphics.fill(x0 + 1, y0, x0 + w - 1, y0 + h, EDGE);
        graphics.fill(x0, y0 + 1, x0 + w, y0 + h - 1, EDGE);
        graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, LEATHER);
        graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + 3, LEATHER_LIGHT);
        graphics.fill(x0 + 1, y0 + h - 4, x0 + w - 1, y0 + h - 1, LEATHER_DARK);
        for (int i = 0; i < h * w / 90; i++) {
            int gx = x0 + 3 + (i * 37) % (w - 6);
            int gy = y0 + 5 + (i * 23) % (h - 10);
            graphics.fill(gx, gy, gx + 1, gy + 1, LEATHER_DARK);
        }
        // the stitching all round, a dash every other pixel
        for (int x = x0 + 3; x < x0 + w - 3; x += 3) {
            graphics.fill(x, y0 + 3, x + 2, y0 + 4, STITCH);
            graphics.fill(x, y0 + h - 4, x + 2, y0 + h - 3, STITCH);
        }
        for (int y = y0 + 6; y < y0 + h - 5; y += 3) {
            graphics.fill(x0 + 3, y, x0 + 4, y + 2, STITCH);
            graphics.fill(x0 + w - 4, y, x0 + w - 3, y + 2, STITCH);
        }
        // the strap it hangs from, over to the inventory
        graphics.fill(x0 - 5, y0 + 8, x0 + 1, y0 + 12, LEATHER_DARK);
        graphics.fill(x0 - 5, y0 + 9, x0 + 1, y0 + 10, LEATHER_LIGHT);
        graphics.fill(x0 - 3, y0 + 7, x0 - 1, y0 + 13, BRASS_DARK);

        // the brass name plate
        String name = bag.title;
        float scale = Math.min(1.0F, (w - 14) / (float) font.width(name));
        int plateW = Math.round(font.width(name) * scale) + 8;
        int px = x0 + (w - plateW) / 2;
        graphics.fill(px, y0 + 6, px + plateW, y0 + 16, BRASS_DARK);
        graphics.fill(px + 1, y0 + 7, px + plateW - 1, y0 + 15, BRASS);
        graphics.fill(px + 1, y0 + 7, px + plateW - 1, y0 + 8, 0xFFF6DC8E);
        graphics.pose().pushMatrix();
        graphics.pose().translate(px + 4, y0 + 8 + (1 - scale) * 3);
        graphics.pose().scale(scale, scale);
        graphics.text(font, name, 0, 0, TEXT, false);
        graphics.pose().popMatrix();

        // the slots, with a faint picture of what goes in them while empty; each group of slots a slightly different shade
        for (int i = 0; i < liveSlots(bag); i++) {
            Bag.Holds holds = bag.holds(i);
            ItemStack hint = holds == null ? ItemStack.EMPTY : holds.hint();
            int group = holds == null ? 0 : groupOf(bag, i);
            int sx = leftPos + BagSlot.FIRST_X + (i % BagSlot.COLUMNS) * 18 - 1;
            int sy = topPos + BagSlot.FIRST_Y + (i / BagSlot.COLUMNS) * 18 - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, EDGE);
            graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, LEATHER_LIGHT);
            int inside = group % 2 == 0 ? 0xFF4A2C18 : 0xFF2E3A2A;
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, inside);
            if (!hint.isEmpty() && player.inventoryMenu.slots.size() > 47 + i && !player.inventoryMenu.slots.get(47 + i).hasItem()) {
                graphics.item(hint, sx + 1, sy + 1);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xC0000000 | (inside & 0xFFFFFF));
            }
        }
    }

    // Which of the bag's groups this slot is in (0, 1, 2...)
    private static int groupOf(Bag bag, int slot) {
        int at = 0;
        for (int g = 0; g < bag.groups.size(); g++) {
            at += bag.groups.get(g).count();
            if (slot < at) return g;
        }
        return 0;
    }

    // How far right the potion effects move so they don't sit on the pouch (0 without a bag)
    public static int effectsShift() {
        Player player = Minecraft.getInstance().player;
        return player != null && Bags.worn(player) != null ? BagSlot.PANEL_WIDTH + 6 : 0;
    }
}

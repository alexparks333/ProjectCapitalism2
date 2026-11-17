package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.librarian.StitchingTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

// The Stitching Table (textures/gui/container/stitching_table.png, tools/make_librarian_textures.ps1): a leather mat
// with the ring of cover slots round the big Paper slot. While it sews, thread is stitched round the Paper along the
// dotted line (silver for Iron, glinting cyan for Diamond) as the book gets done, with the needle bobbing in and out at
// the end of it. On the right: the thread slot (with a spool), a walnut plaque counting down the time left, a progress
// groove, and the finished books. What's missing shows next to "Inventory" (and the countdown greys out).
public class StitchingTableScreen extends AbstractContainerScreen<StitchingTableMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/stitching_table.png");
    private static final int TITLE = 0xFFFFE8C0;
    private static final int LABEL = 0xFFE0C8A0;
    private static final int WARN = 0xFFFFB070;
    private static final int BRASS = 0xFFF2D27A;
    private static final int DIM = 0xFF8A7458;
    // the stitch path: a square round the Paper slot
    private static final int PATH_X0 = 48;
    private static final int PATH_Y0 = 56;
    private static final int PATH_SIDE = 33;
    private static final int PATH_LENGTH = PATH_SIDE * 4;

    public StitchingTableScreen(StitchingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = 129;
    }

    // A point d pixels along the stitch path (clockwise from the top left), and which way is "into the Paper" there
    private static int[] along(int d) {
        d = Math.floorMod(d, PATH_LENGTH);
        int side = d / PATH_SIDE;
        int t = d % PATH_SIDE;
        return switch (side) {
            case 0 -> new int[]{PATH_X0 + t, PATH_Y0, 0, 1};
            case 1 -> new int[]{PATH_X0 + PATH_SIDE, PATH_Y0 + t, -1, 0};
            case 2 -> new int[]{PATH_X0 + PATH_SIDE - t, PATH_Y0 + PATH_SIDE, 0, -1};
            default -> new int[]{PATH_X0, PATH_Y0 + PATH_SIDE - t, 1, 0};
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        boolean sewing = this.menu.missing() == null;
        float progress = this.menu.progress();
        long now = Util.getMillis();
        boolean diamond = this.menu.diamondThread();

        // the thread sewn so far: dashes along the path, with a cross-stitch every few pixels
        int sewn = Math.round(PATH_LENGTH * progress);
        for (int d = 0; d < sewn; d++) {
            int[] p = along(d);
            if (d % 4 == 3) continue;
            int color = diamond ? ((d + now / 120) % 9 == 0 ? 0xFFE8FFFF : 0xFF5FE0D8) : 0xFFD8DCE4;
            graphics.fill(xo + p[0], yo + p[1], xo + p[0] + 1, yo + p[1] + 1, color);
            if (d % 8 == 1) {
                // a little slanted stitch across the line, into the paper and out
                graphics.fill(xo + p[0] + p[2], yo + p[1] + p[3], xo + p[0] + p[2] + 1, yo + p[1] + p[3] + 1, color);
                graphics.fill(xo + p[0] - p[2], yo + p[1] - p[3], xo + p[0] - p[2] + 1, yo + p[1] - p[3] + 1, color);
            }
        }

        // the needle at the end of the thread, bobbing in and out of the Paper while it sews
        if (sewing || progress > 0) {
            int[] tip = along(sewn);
            int bob = sewing ? (int) Math.round(Math.sin(now / 160.0) * 2.5) : 0;
            int tx = xo + tip[0] + tip[2] * bob;
            int ty = yo + tip[1] + tip[3] * bob;
            // the body leans back out of the paper, at 45 degrees
            int ox = -tip[2] + (tip[3] != 0 ? 1 : 0);
            int oy = -tip[3] + (tip[2] != 0 ? -1 : 0);
            for (int i = 0; i < 7; i++) {
                int shade = i == 0 ? 0xFFFFFFFF : i == 6 ? 0xFF40404A : 0xFFC8CCD6;
                graphics.fill(tx + ox * i, ty + oy * i, tx + ox * i + 1, ty + oy * i + 1, shade);
            }
            // a strand from the needle's eye back to the last stitch
            int ex = tx + ox * 6;
            int ey = ty + oy * 6;
            graphics.fill(Math.min(ex, xo + tip[0]), Math.min(ey, yo + tip[1]), Math.min(ex, xo + tip[0]) + 1, Math.max(ey, yo + tip[1]) + 1,
                diamond ? 0xAA5FE0D8 : 0xAAD8DCE4);
        }

        // the countdown on the plaque, and the groove filling up
        int ticks = this.menu.ticksLeft();
        int seconds = (ticks + 19) / 20;
        String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        int timeColor = sewing ? BRASS : DIM;
        graphics.centeredText(this.font, time, xo + 148, yo + 68, timeColor);
        int fill = Math.round(44 * progress);
        if (fill > 0) graphics.fill(xo + 126, yo + 87, xo + 126 + fill, yo + 90, diamond ? 0xFF5FE0D8 : 0xFFD8DCE4);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, "Thread", 132, 27, LABEL, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, LABEL, false);
        String missing = this.menu.missing();
        String status = missing != null ? missing : this.menu.diamondThread() ? "Sewing a Delicate Book" : "Sewing a Rough Book";
        graphics.text(this.font, status, this.imageWidth - 8 - this.font.width(status), this.inventoryLabelY, missing != null ? WARN : 0xFFB8F0A8, false);
    }
}

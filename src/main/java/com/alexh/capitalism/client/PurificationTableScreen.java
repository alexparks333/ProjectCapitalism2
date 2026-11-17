package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.menu.PurificationTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class PurificationTableScreen extends AbstractContainerScreen<PurificationTableMenu> {

    // Slate stone panel: filter canister, water slot, fuel slot, arrow, output slot and a water gauge.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/purification_table.png");
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");

    // Right arrow (24x16 at 79,34): each row's filled span, painted water-blue left to right as it works.
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_W = 24;
    private static final int[][] ARROW_ROWS = buildArrowRows();

    // Inside of the glass gauge on the right, filled from the bottom.
    private static final int GAUGE_X0 = 150;
    private static final int GAUGE_Y0 = 22;
    private static final int GAUGE_X1 = 158;
    private static final int GAUGE_Y1 = 70;

    private static final int WATER = 0xFF4696E1;
    private static final int WATER_LIGHT = 0xFF96D2FA;
    private static final int TITLE_COLOR = 0xFFE4EEF6;
    private static final int LABEL_COLOR = 0xFF1E2A36;

    public PurificationTableScreen(PurificationTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private static int[][] buildArrowRows() {
        int[][] rows = new int[16][];
        for (int r = 0; r < 16; r++) {
            int end = 23 - (int) Math.ceil(Math.abs(r - 7.5));
            int start = r >= 5 && r <= 10 ? 0 : 15;
            rows[r] = new int[]{start, end};
        }
        return rows;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Flame height = buckets left on the current piece of fuel.
        if (this.menu.getFuelUses() > 0) {
            int flameHeight = Mth.ceil(this.menu.getFuelLevel() * 13.0F) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - flameHeight, xo + 56, yo + 36 + 14 - flameHeight, 14, flameHeight);
        }

        // Arrow fills with water from left to right.
        int filledCols = Mth.ceil(this.menu.getProgress() * ARROW_W);
        for (int r = 0; r < ARROW_ROWS.length; r++) {
            int x0 = ARROW_ROWS[r][0];
            int x1 = Math.min(ARROW_ROWS[r][1] + 1, filledCols);
            if (x1 > x0) {
                graphics.fill(xo + ARROW_X + x0, yo + ARROW_Y + r, xo + ARROW_X + x1, yo + ARROW_Y + r + 1, r == 5 ? WATER_LIGHT : WATER);
            }
        }

        // Gauge fills from the bottom; full while a purified bucket is waiting.
        float level = this.menu.hasOutput() ? 1.0F : this.menu.getProgress();
        int height = Mth.ceil(level * (GAUGE_Y1 - GAUGE_Y0));
        if (height > 0) {
            int top = yo + GAUGE_Y1 - height;
            graphics.fill(xo + GAUGE_X0, top, xo + GAUGE_X1, yo + GAUGE_Y1, WATER);
            graphics.fill(xo + GAUGE_X0 + 1, top, xo + GAUGE_X0 + 2, yo + GAUGE_Y1, WATER_LIGHT);
            graphics.fill(xo + GAUGE_X0, top, xo + GAUGE_X1, top + 1, WATER_LIGHT);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        int secondsLeft = this.menu.getSecondsLeft();
        if (secondsLeft > 0) {
            Component timer = Component.literal(secondsLeft + "s");
            graphics.text(this.font, timer, 91 - this.font.width(timer) / 2, 54, LABEL_COLOR, false);
        }
    }
}

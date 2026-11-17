package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.menu.FermenterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

public class FermenterScreen extends AbstractContainerScreen<FermenterMenu> {

    // Cellar panel: 6 fermentable slots, a sugar slot, a big glass vat in the middle, and the bucket slot.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/fermenter.png");

    // Inside of the glass vat, filled from the bottom
    private static final int VAT_X0 = 72;
    private static final int VAT_Y0 = 41;
    private static final int VAT_X1 = 105;
    private static final int VAT_Y1 = 78;

    // Sugar bar under the sugar slot
    private static final int SUGAR_BAR_X = 25;
    private static final int SUGAR_BAR_Y = 67;
    private static final int SUGAR_BAR_W = 16;

    private static final int TITLE_COLOR = 0xFFF4E4C0;
    private static final int LABEL_COLOR = 0xFFF0DCB4;
    private static final int DEFAULT_LIQUOR = 0xC0782A;

    public FermenterScreen(FermenterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
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

        // Sugar bar under the sugar slot: how long the sugar currently burning will last
        int sugarWidth = Mth.ceil(this.menu.getSugarLevel() * SUGAR_BAR_W);
        if (sugarWidth > 0) {
            graphics.fill(xo + SUGAR_BAR_X, yo + SUGAR_BAR_Y, xo + SUGAR_BAR_X + sugarWidth, yo + SUGAR_BAR_Y + 2, 0xFFF4F0E8);
            graphics.fill(xo + SUGAR_BAR_X, yo + SUGAR_BAR_Y + 1, xo + SUGAR_BAR_X + sugarWidth, yo + SUGAR_BAR_Y + 2, 0xFFC8C0B4);
        }

        LiquorType liquor = this.menu.getLiquor();
        int rgb = liquor == null ? DEFAULT_LIQUOR : liquor.color();
        int color = 0xFF000000 | rgb;
        int light = 0xFF000000 | shift(rgb, 50);
        int dark = 0xFF000000 | shift(rgb, -40);

        boolean done = this.menu.hasFinishedLiquor();
        float progress = this.menu.getProgress();
        float level = done ? 1.0F : progress;
        int height = Mth.ceil(level * (VAT_Y1 - VAT_Y0));
        if (height <= 0) return;

        int top = yo + VAT_Y1 - height;
        graphics.fill(xo + VAT_X0, top, xo + VAT_X1, yo + VAT_Y1, color);
        graphics.fill(xo + VAT_X0, yo + VAT_Y1 - 3, xo + VAT_X1, yo + VAT_Y1, dark);
        graphics.fill(xo + VAT_X0, top, xo + VAT_X1, top + 1, light);
        graphics.fill(xo + VAT_X0 + 2, top, xo + VAT_X0 + 3, yo + VAT_Y1, light);

        // Bubbles rise through the liquid while it's fermenting.
        if (progress > 0.0F && height > 4) {
            long time = Util.getMillis() / 60;
            for (int i = 0; i < 7; i++) {
                int column = 5 + (i * 11) % (VAT_X1 - VAT_X0 - 8);
                int travel = height - 3;
                int rise = (int) ((time + i * 17L) % travel);
                int bx = xo + VAT_X0 + column + ((rise / 4 + i) % 2);
                int by = yo + VAT_Y1 - 2 - rise;
                graphics.fill(bx, by, bx + 2, by + 2, light);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        int secondsLeft = this.menu.getSecondsLeft();
        if (secondsLeft > 0) {
            String time = String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60);
            graphics.text(this.font, Component.literal(time), 142 - this.font.width(time) / 2, 74, LABEL_COLOR, false);
        }
    }

    private static int shift(int rgb, int amount) {
        int r = Mth.clamp(((rgb >> 16) & 0xFF) + amount, 0, 255);
        int g = Mth.clamp(((rgb >> 8) & 0xFF) + amount, 0, 255);
        int b = Mth.clamp((rgb & 0xFF) + amount, 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}

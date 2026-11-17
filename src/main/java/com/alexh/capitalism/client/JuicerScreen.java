package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.JuiceType;
import com.alexh.capitalism.block.JuicerBlockEntity;
import com.alexh.capitalism.block.JuicerRecipe;
import com.alexh.capitalism.menu.JuicerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class JuicerScreen extends AbstractContainerScreen<JuicerMenu> {

    // Bamboo machine panel: FRUITS slots, copper pipes into a down arrow, the bucket slot, and a juice gauge.
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/juicer.png");

    // Down arrow (12x13). Each row's filled span, so it can be painted in the juice's color.
    private static final int ARROW_X = 82;
    private static final int ARROW_Y = 42;
    private static final int[][] ARROW_ROWS = {
        {4, 7}, {4, 7}, {4, 7}, {4, 7}, {4, 7}, {4, 7}, {4, 7}, {0, 11}, {1, 10}, {2, 9}, {3, 8}, {4, 7}, {5, 6}
    };

    // Inside of the glass juice gauge on the right, filled from the bottom.
    private static final int GAUGE_X0 = 152;
    private static final int GAUGE_Y0 = 44;
    private static final int GAUGE_X1 = 160;
    private static final int GAUGE_Y1 = 78;

    private static final int LABEL_COLOR = 0xFF4A3A14;
    private static final int DEFAULT_JUICE = 0xE24654;

    public JuicerScreen(JuicerMenu menu, Inventory inventory, Component title) {
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

        JuiceType juice = this.menu.getJuice();
        int rgb = juice == null ? DEFAULT_JUICE : juice.color();
        int color = 0xFF000000 | rgb;
        int highlight = 0xFF000000 | lighten(rgb);

        // Arrow fills from the top down while juicing.
        int arrowRows = Mth.ceil(this.menu.getProgress() * ARROW_ROWS.length);
        for (int r = 0; r < arrowRows; r++) {
            int x0 = xo + ARROW_X + ARROW_ROWS[r][0];
            int x1 = xo + ARROW_X + ARROW_ROWS[r][1] + 1;
            graphics.fill(x0, yo + ARROW_Y + r, x1, yo + ARROW_Y + r + 1, color);
        }

        // Gauge fills from the bottom; full while a finished juice bucket is waiting.
        boolean done = JuicerRecipe.juiceOf(this.menu.getSlot(JuicerBlockEntity.SLOT_BUCKET).getItem()) != null;
        float level = done ? 1.0F : this.menu.getProgress();
        int height = Mth.ceil(level * (GAUGE_Y1 - GAUGE_Y0));
        if (height > 0) {
            int top = yo + GAUGE_Y1 - height;
            graphics.fill(xo + GAUGE_X0, top, xo + GAUGE_X1, yo + GAUGE_Y1, color);
            graphics.fill(xo + GAUGE_X0 + 1, top, xo + GAUGE_X0 + 2, yo + GAUGE_Y1, highlight);
            graphics.fill(xo + GAUGE_X0, top, xo + GAUGE_X1, top + 1, highlight);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFF4E8B8, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        int secondsLeft = this.menu.getSecondsLeft();
        if (secondsLeft > 0) {
            graphics.text(this.font, Component.literal(secondsLeft + "s"), ARROW_X + 17, ARROW_Y + 3, LABEL_COLOR, false);
        }
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60);
        int g = Math.min(255, ((rgb >> 8) & 0xFF) + 60);
        int b = Math.min(255, (rgb & 0xFF) + 60);
        return (r << 16) | (g << 8) | b;
    }
}

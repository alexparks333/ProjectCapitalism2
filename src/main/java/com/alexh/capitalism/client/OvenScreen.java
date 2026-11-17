package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.OvenBlockEntity;
import com.alexh.capitalism.menu.OvenMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

// Brick oven panel: 3 dish slots on the left, an arrow per row filling as it bakes, the meal slots, and the fire.
// A finished meal about to burn makes its slot pulse red.
public class OvenScreen extends AbstractContainerScreen<OvenMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/oven.png");
    // Vanilla furnace pieces: the flame and the arrow fill
    private static final Identifier LIT = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public static final int ARROW_X = 68;
    public static final int FLAME_X = 141;
    public static final int FLAME_Y = 36;

    private static final int TITLE_COLOR = 0xFFFFE2B8;
    private static final int LABEL_COLOR = 0xFFF0D8B8;

    public OvenScreen(OvenMenu menu, Inventory inventory, Component title) {
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

        if (this.menu.isLit()) {
            int h = Mth.ceil(this.menu.litProgress() * 13.0F) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT, 14, 14, 0, 14 - h, xo + FLAME_X, yo + FLAME_Y + 14 - h, 14, h);
        }
        for (int r = 0; r < OvenBlockEntity.ROWS; r++) {
            // A finished meal getting close to burning: its slot glows red, pulsing faster as time runs out
            float burn = this.menu.burnProgress(r);
            if (burn > 0.4F) {
                double speed = 4 + burn * 14;
                float pulse = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 1000.0 * speed);
                int alpha = (int) (60 + 150 * pulse * burn);
                int sx = xo + OvenMenu.OUTPUT_X;
                int sy = yo + OvenMenu.ROW_Y + r * 18;
                graphics.fill(sx, sy, sx + 16, sy + 16, (alpha << 24) | 0xE02810);
            }

            int w = Mth.ceil(this.menu.bakeProgress(r) * 24.0F);
            if (w > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, xo + ARROW_X, yo + OvenMenu.ROW_Y + r * 18, w, 16);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }
}

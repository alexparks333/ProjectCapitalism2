package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.farmer.DecomposterBlockEntity;
import com.alexh.capitalism.farmer.DecomposterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// The Decomposter (farmer/DecomposterMenu): nailed planks and a compost soil bed (tools/make_farmer_textures.ps1),
// the three ingredient slots in a crate, an arrow that fills with green as it rots down, the Fertilizer slot in a
// leafy wreath. A panel on its right shows the recipes (DecomposterBlockEntity.Recipe); hover an item there for its name.
public class DecomposterScreen extends AbstractContainerScreen<DecomposterMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/decomposter.png");
    // the arrow's inside (filled left to right)
    private static final int ARROW_X0 = 92;
    private static final int ARROW_X1 = 122;
    private static final int ARROW_Y0 = 39;
    private static final int ARROW_Y1 = 47;
    private static final int LABEL = 0xFF3A2A14;
    private static final int TITLE = 0xFFF2E6C0;

    public DecomposterScreen(DecomposterMenu menu, Inventory inventory, Component title) {
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
        int filled = Mth.ceil(this.menu.progress() * (ARROW_X1 - ARROW_X0));
        if (filled > 0) {
            graphics.fill(xo + ARROW_X0, yo + ARROW_Y0, xo + ARROW_X0 + filled, yo + ARROW_Y1, 0xFF5FA83A);
            graphics.fill(xo + ARROW_X0, yo + ARROW_Y0, xo + ARROW_X0 + filled, yo + ARROW_Y0 + 1, 0xFF8FD46A);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL, false);
        int seconds = this.menu.secondsLeft();
        if (seconds > 0) graphics.text(this.font, Component.literal(seconds + "s"), ARROW_X0 + 8, ARROW_Y1 + 4, TITLE, false);
    }

    // The recipe panel to the right of the menu: per recipe a row of its three ingredients, an arrow, what it makes
    private static final int PANEL_GAP = 4;
    private static final int PANEL_W = 100;
    private static final int ROW_Y = 26;
    private static final int ROW_H = 22;

    private void recipes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x0 = this.leftPos + this.imageWidth + PANEL_GAP;
        int y0 = this.topPos;
        DecomposterBlockEntity.Recipe[] recipes = DecomposterBlockEntity.Recipe.values();
        int h = ROW_Y + recipes.length * ROW_H + 2;
        graphics.fill(x0, y0, x0 + PANEL_W, y0 + h, 0xFF3C2814);
        graphics.fill(x0 + 1, y0 + 1, x0 + PANEL_W - 1, y0 + h - 1, 0xFFB08A58);
        graphics.fill(x0 + 3, y0 + 3, x0 + PANEL_W - 3, y0 + 14, 0xFF46321E);
        graphics.centeredText(this.font, Component.literal("Recipes"), x0 + PANEL_W / 2, y0 + 5, TITLE);
        graphics.text(this.font, Component.literal("Any order"), x0 + 6, y0 + 16, LABEL, false);
        ItemStack hovered = ItemStack.EMPTY;
        for (int r = 0; r < recipes.length; r++) {
            int y = y0 + ROW_Y + r * ROW_H;
            List<ItemStack> row = recipes[r].ingredients.stream().map(ItemStack::new).toList();
            for (int i = 0; i < row.size(); i++) {
                int x = x0 + 5 + i * 18;
                graphics.fill(x, y, x + 18, y + 18, 0xFF7A5A34);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8E6C40);
                graphics.item(row.get(i), x + 1, y + 1);
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) hovered = row.get(i);
            }
            graphics.text(this.font, Component.literal("→"), x0 + 62, y + 5, LABEL, false);
            int x = x0 + 76;
            ItemStack made = recipes[r].result();
            graphics.fill(x, y, x + 18, y + 18, 0xFF4E7A2C);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8E6C40);
            graphics.item(made, x + 1, y + 1);
            graphics.itemDecorations(this.font, made, x + 1, y + 1);
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) hovered = made;
        }
        if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.recipes(graphics, mouseX, mouseY);
    }
}

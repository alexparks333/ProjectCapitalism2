package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.librarian.KnowledgeCreatorMenu;
import com.alexh.capitalism.librarian.KnowledgeQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

// The Knowledge Creator (textures/gui/container/knowledge_creator.png, tools/make_librarian_textures.ps1): dark
// deepslate with violet glyphs. The Enchanted Book on the left, the Knowledge Piece on the right, and between them a
// channel that fills with the quality's color as it works, with the time left above it and enchanting glyphs flowing
// along it. Under them, what the book will make before you start: "Power 7: Bright Knowledge Piece (x2)", with the
// piece's icon and the four qualities as pips (the one it'll make lit up).
public class KnowledgeCreatorScreen extends AbstractContainerScreen<KnowledgeCreatorMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/knowledge_creator.png");
    private static final Style GLYPHS = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt")));
    private static final int TITLE = 0xFFE6D8FF;
    private static final int LABEL = 0xFFA898C8;
    private static final int DIM = 0xFF6E6290;
    // the channel between the slots
    private static final int CHANNEL_X0 = 62;
    private static final int CHANNEL_X1 = 116;
    private static final int CHANNEL_Y = 42;

    public KnowledgeCreatorScreen(KnowledgeCreatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        ItemStack book = this.menu.book();
        int power = KnowledgeQuality.power(book);
        KnowledgeQuality quality = power > 0 ? KnowledgeQuality.byPower(power) : null;
        boolean working = this.menu.working();
        float progress = this.menu.progress();
        long now = Util.getMillis();
        int color = quality == null ? 0x6E6290 : quality.color;

        // the channel fills with the quality's color
        int fill = Math.round((CHANNEL_X1 - CHANNEL_X0) * progress);
        if (fill > 0) graphics.fill(xo + CHANNEL_X0, yo + CHANNEL_Y, xo + CHANNEL_X0 + fill, yo + CHANNEL_Y + 4, 0xFF000000 | color);
        // glyphs drifting along it while it works
        if (working) {
            for (int i = 0; i < 4; i++) {
                double t = ((now / 1000.0) * 0.6 + i * 0.25) % 1.0;
                int x = xo + CHANNEL_X0 + (int) (t * (CHANNEL_X1 - CHANNEL_X0 - 6));
                int alpha = (int) (220 * Math.sin(Math.PI * t));
                char rune = "abcdefghijklmnopqrstuvwxyz".charAt((int) ((i * 7 + now / 300) % 26));
                graphics.text(this.font, Component.literal(String.valueOf(rune)).withStyle(GLYPHS), x, yo + CHANNEL_Y - 10,
                    (alpha << 24) | color, false);
            }
        }
        // the time left, over the channel
        if (quality != null) {
            int seconds = (this.menu.ticksLeft() + 19) / 20;
            String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            graphics.centeredText(this.font, time, xo + (CHANNEL_X0 + CHANNEL_X1) / 2, yo + CHANNEL_Y + 8, working ? 0xFFF2D27A : DIM);
        }

        // what it will make
        int previewY = yo + 60;
        if (quality == null) {
            graphics.centeredText(this.font, "Put in an Enchanted Book", xo + this.imageWidth / 2, previewY, DIM);
        } else {
            graphics.item(quality.stack(1), xo + 12, previewY - 4);
            String line = "Power " + power + ": " + quality.title + " Piece (x" + (quality.multiplier == (int) quality.multiplier
                ? String.valueOf((int) quality.multiplier) : String.valueOf(quality.multiplier)) + ")";
            graphics.text(this.font, line, xo + 32, previewY, 0xFF000000 | quality.color, true);
        }
        // the four qualities as pips, the one it'll make lit
        for (KnowledgeQuality q : KnowledgeQuality.values()) {
            int px = xo + 134 + q.ordinal() * 9;
            boolean lit = q == quality;
            graphics.fill(px, yo + 8, px + 6, yo + 14, lit ? 0xFF000000 | q.color : 0xFF2A2140);
            graphics.fill(px, yo + 8, px + 6, yo + 9, lit ? 0xFFFFFFFF : 0xFF4A3E66);
        }
        if (mouseX >= xo + 134 && mouseX < xo + 170 && mouseY >= yo + 8 && mouseY < yo + 14) {
            graphics.setTooltipForNextFrame(this.font, Component.literal("Faded, Clear, Bright, Radiant: the more enchantment levels, the better"), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, LABEL, false);
    }
}

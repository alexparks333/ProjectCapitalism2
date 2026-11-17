package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.librarian.LibrarianTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

// The Librarian Table (textures/gui/container/librarian_table.png, tools/make_librarian_textures.ps1): a green felt
// desk with the big book slot, the small Knowledge Piece slot and the result across the top, like the smith tables.
// Under them, faint letters drift across an empty page (just for looks: no recipe hints).
public class LibrarianTableScreen extends AbstractContainerScreen<LibrarianTableMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/librarian_table.png");
    private static final Style GLYPH_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt")));
    private static final String RUNES = "abcdefghijklmnopqrstuvwxyz";
    private static final int TITLE = 0xFFF2EAD6;
    private static final int LABEL = 0xFFB8D8C0;
    private static final int INK = 0x3A2A1A;
    // the page under the slots
    private static final int PAGE_X = 30;
    private static final int PAGE_Y = 50;
    private static final int PAGE_W = 116;

    public LibrarianTableScreen(LibrarianTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        // letters being written across the page, fading in and out
        long now = Util.getMillis();
        for (int i = 0; i < 14; i++) {
            double t = ((now / 1000.0) * 0.35 + i * 0.37) % 1.0;
            int alpha = (int) (200 * Math.sin(Math.PI * t));
            if (alpha < 20) continue;
            int x = xo + PAGE_X + 4 + (i * 29 + 7) % (PAGE_W - 10);
            int y = yo + PAGE_Y + 2 + (i % 2) * 8;
            char rune = RUNES.charAt((int) ((i * 7 + now / 900) % RUNES.length()));
            graphics.text(this.font, Component.literal(String.valueOf(rune)).withStyle(GLYPH_STYLE), x, y, (alpha << 24) | INK, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, LABEL, false);
    }
}

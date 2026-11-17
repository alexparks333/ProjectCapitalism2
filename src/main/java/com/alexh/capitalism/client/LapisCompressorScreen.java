package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.enchanter.LapisCompressorBlockEntity;
import com.alexh.capitalism.enchanter.LapisCompressorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

// The Lapis Compressor (textures/gui/container/lapis_compressor.png, drawn by tools/make_lapis_compressor_gui.ps1):
// the Lapis slot on the left, the press in the middle (the steel head comes down and squashes a lapis chunk as the
// press goes on) and the XP chute on the right, where an XP orb glows brighter until it pops out of the block's front.
public class LapisCompressorScreen extends AbstractContainerScreen<LapisCompressorMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/lapis_compressor.png");

    // the press chamber (inside), as drawn in the texture
    private static final int CH_X0 = 73;
    private static final int CH_X1 = 104;
    private static final int CH_Y0 = 19;
    private static final int CH_Y1 = 59;
    // the XP orb in the chute
    private static final int ORB_X = 128;
    private static final int ORB_Y = 37;

    public LapisCompressorScreen(LapisCompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        float p = this.menu.progress();
        boolean working = this.menu.working();
        // the lapis chunk on the floor of the press, squashed flatter as the press goes on
        if (working) {
            int chunkH = Math.max(2, Math.round(12 * (1 - p * 0.75F)));
            int chunkW = Math.round(16 + 8 * p);
            int cx = (CH_X0 + CH_X1) / 2;
            int x0 = xo + cx - chunkW / 2;
            int y1 = yo + CH_Y1;
            graphics.fill(x0, y1 - chunkH, x0 + chunkW, y1, 0xFF1E3CA0);
            graphics.fill(x0, y1 - chunkH, x0 + chunkW, y1 - chunkH + 1, 0xFF5A8CF0);
            graphics.fill(x0 + 3, y1 - chunkH + 2, x0 + 5, y1 - chunkH + 3, 0xFF9CC0FF);
            graphics.fill(x0 + chunkW - 6, y1 - 3, x0 + chunkW - 4, y1 - 2, 0xFF9CC0FF);
        }
        // the press head: a rod and a heavy steel plate, coming down to meet the chunk
        int chunkTop = working ? CH_Y1 - Math.max(2, Math.round(12 * (1 - p * 0.75F))) : CH_Y1 - 12;
        int headBottom = Math.round(Mth.lerp(working ? p : 0, CH_Y0 + 12, chunkTop));
        int cx = xo + (CH_X0 + CH_X1) / 2;
        graphics.fill(cx - 2, yo + CH_Y0, cx + 2, yo + headBottom - 5, 0xFF9AA2AE);
        graphics.fill(cx - 2, yo + CH_Y0, cx - 1, yo + headBottom - 5, 0xFFD0D6DE);
        graphics.fill(xo + CH_X0 + 3, yo + headBottom - 6, xo + CH_X1 - 3, yo + headBottom, 0xFF5C626C);
        graphics.fill(xo + CH_X0 + 3, yo + headBottom - 6, xo + CH_X1 - 3, yo + headBottom - 5, 0xFFB4BAC4);
        graphics.fill(xo + CH_X0 + 3, yo + headBottom - 1, xo + CH_X1 - 3, yo + headBottom, 0xFF2A2E34);

        // the XP orb in the chute: grows and brightens until it's made
        if (working) {
            int r = 2 + Math.round(4 * p);
            int glow = Math.round(120 + 135 * p);
            int color = 0xFF000000 | (Math.round(glow * 0.75F) << 16) | (glow << 8) | 0x30;
            int ox = xo + ORB_X;
            int oy = yo + ORB_Y;
            graphics.fill(ox - r, oy - r + 1, ox + r, oy + r - 1, color);
            graphics.fill(ox - r + 1, oy - r, ox + r - 1, oy + r, color);
            graphics.fill(ox - r + 1, oy - r + 1, ox - r + 3, oy - r + 2, 0xFFF4FFD0);
        }
        String rate = "+" + LapisCompressorBlockEntity.XP_PER_LAPIS + " XP";
        graphics.text(this.font, rate, xo + ORB_X - this.font.width(rate) / 2, yo + 56, 0xFF9CFF6A, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0xFFE4E8F0, true);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFB8C0CC, false);
        String hint = this.menu.working() ? "XP comes out the front" : "Put Lapis in";
        graphics.text(this.font, hint, this.imageWidth - 8 - this.font.width(hint), this.inventoryLabelY, 0xFF8FA8E0, false);
    }
}

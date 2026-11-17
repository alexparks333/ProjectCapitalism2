package com.alexh.capitalism.mixin;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.enchanter.NpcEnchanting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The Enchanter NPC's enchanting (enchanter/NpcEnchanting) looks like a shop, not a table: the same enchanting screen
// (so it all works as usual), dressed as a market stall like the other NPC shops (client/NpcShopScreen): a striped
// awning over it, oak posts down its sides, a warm wood tint over the panel, and a price tag to the right of each
// option with its gold (gold if you can pay, red if you can't). Told apart from a real table by its title.
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {

    private EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void capitalism$shopLook(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!NpcEnchanting.TITLE.equals(this.getTitle().getString())) return;
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        // a warm wood tint over the panel (the items are drawn after, untinted)
        graphics.fill(x0, y0, x0 + w, y0 + h, 0x38A06A30);
        // oak posts down both sides
        post(graphics, x0 - 7, y0 - 4, y0 + h);
        post(graphics, x0 + w + 1, y0 - 4, y0 + h);
        // the striped awning, with a scalloped edge
        int ax0 = x0 - 9;
        int ax1 = x0 + w + 9;
        int top = y0 - 18;
        for (int x = ax0; x < ax1; x++) {
            boolean red = Math.floorMod((x - ax0) / 10, 2) == 0;
            for (int y = top; y < y0 - 4; y++) {
                int shade = y - top <= 1 ? 18 : y >= y0 - 6 ? -20 : 0;
                graphics.fill(x, y, x + 1, y + 1, red ? rgb(196 + shade, 52 + shade, 46 + shade) : rgb(240 + shade, 226 + shade, 190 + shade));
            }
        }
        for (int s = 0; ax0 + s * 10 < ax1; s++) {
            boolean red = s % 2 == 0;
            float cx = ax0 + s * 10 + 4.5F;
            for (int dy = 0; dy < 4; dy++) {
                int half = (int) Math.sqrt(Math.max(0, 25 - Math.pow((dy + 1) * 1.6, 2)));
                int y = y0 - 4 + dy;
                graphics.fill(Math.max(ax0, (int) (cx - half)), y, Math.min(ax1, (int) (cx + half) + 1), y + 1,
                    red ? rgb(170, 40, 36) : rgb(214, 200, 166));
            }
        }
        graphics.fill(ax0, top - 1, ax1, top, rgb(46, 30, 18));
        // the price tags, right of each option (the options are at y0 + 14 + 19i, 19 tall)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long carried = GoldMoney.carried(mc.player);
        int tx = x0 + w + 10;
        for (int i = 0; i < 3; i++) {
            int cost = this.menu.costs[i];
            if (cost <= 0) continue;
            long price = NpcEnchanting.price(cost, mc.player);
            String text = GoldMoney.format(price);
            int tw = this.font.width(text) + 16;
            int ty = y0 + 14 + 19 * i + 1;
            // a little paper tag on a string
            graphics.fill(tx - 3, ty + 8, tx, ty + 9, rgb(90, 60, 34));
            graphics.fill(tx, ty, tx + tw, ty + 17, rgb(70, 44, 24));
            graphics.fill(tx + 1, ty + 1, tx + tw - 1, ty + 16, rgb(246, 230, 196));
            graphics.fill(tx + 1, ty + 15, tx + tw - 1, ty + 16, rgb(214, 192, 150));
            // a gold coin
            graphics.fill(tx + 4, ty + 5, tx + 10, ty + 12, rgb(150, 100, 20));
            graphics.fill(tx + 5, ty + 4, tx + 9, ty + 13, rgb(150, 100, 20));
            graphics.fill(tx + 5, ty + 5, tx + 9, ty + 12, rgb(250, 206, 60));
            graphics.fill(tx + 6, ty + 6, tx + 7, ty + 8, rgb(255, 246, 180));
            boolean afford = carried >= price || mc.player.isCreative();
            graphics.text(this.font, text, tx + 13, ty + 5, afford ? rgb(150, 96, 10) : rgb(200, 40, 40), false);
        }
    }

    private static void post(GuiGraphicsExtractor graphics, int x, int y0, int y1) {
        graphics.fill(x, y0, x + 6, y1, rgb(46, 30, 18));
        graphics.fill(x + 1, y0, x + 5, y1, rgb(150, 104, 60));
        graphics.fill(x + 1, y0, x + 2, y1, rgb(178, 130, 80));
        for (int y = y0 + 5; y < y1; y += 9) graphics.fill(x + 1, y, x + 5, y + 1, rgb(110, 72, 40));
    }

    private static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

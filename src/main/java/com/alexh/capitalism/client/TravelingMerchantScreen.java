package com.alexh.capitalism.client;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.town.TravelingMerchantMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

// The Traveling Merchant's wares (town/TravelingMerchantMenu), drawn in code: a weathered wagon-wood frame with iron
// corner brackets around a sheet of old parchment, a canvas strip across the top with his name, and a line of his
// talk. His wares lie on cloth cards, four to a row, each drawn big with its name and price in gold; sold ones are
// stamped SOLD. Pick one, then Buy (it's a lot of gold: no buying by accident). Hover a card for the item's own
// tooltip and the price. Up top: how much gold you carry and how long until he leaves at dawn.
public class TravelingMerchantScreen extends AbstractContainerScreen<TravelingMerchantMenu> {

    private static final int W = 256;
    private static final int H = 196;
    private static final int CARD_W = 56;
    private static final int CARD_H = 60;
    private static final int GAP = 4;
    private static final int GRID_X = 10;
    private static final int GRID_Y = 44;
    private static final int INK = 0xFF3A2614;
    private static final int INK_DIM = 0xFF7A6040;
    private static final int GOLD = 0xFFE0A830;
    private static final int GOOD = 0xFF2E7A28;
    private static final int BAD = 0xFFA02818;
    private static final String[] LINES = {
        "\"Rare things, from far-off roads. One of each, friend.\"",
        "\"I won't be here long. The road always calls.\"",
        "\"Haggling? Ha! You won't find these anywhere else.\"",
        "\"Every one of these nearly cost me my life. Worth every coin.\""};

    private int selected = -1;
    private final String line;

    public TravelingMerchantScreen(TravelingMerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.line = LINES[(int) (Math.abs(System.nanoTime() / 1_000_000_000L) % LINES.length)];
    }

    private int cardX(int i) {
        return this.leftPos + GRID_X + (i % 4) * (CARD_W + GAP) + 2;
    }

    private int cardY(int i) {
        return this.topPos + GRID_Y + (i / 4) * (CARD_H + GAP);
    }

    private int cardAt(double mx, double my) {
        for (int i = 0; i < this.menu.wares().size(); i++) {
            if (mx >= cardX(i) && mx < cardX(i) + CARD_W && my >= cardY(i) && my < cardY(i) + CARD_H) return i;
        }
        return -1;
    }

    private int buyX() {
        return this.leftPos + W - 66;
    }

    private int buyY() {
        return this.topPos + H - 26;
    }

    private boolean overBuy(double mx, double my) {
        return mx >= buyX() && mx < buyX() + 56 && my >= buyY() && my < buyY() + 18;
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0F, 2.0F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void smallItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.5F, 0.5F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void smallText(GuiGraphicsExtractor g, String text, int cx, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(0.75F, 0.75F);
        this.flat(g, text, 0, 0, color);
        g.pose().popMatrix();
    }

    // text centred on cx, with no drop shadow (it smears on the light parchment)
    private void flat(GuiGraphicsExtractor g, String text, int cx, int y, int color) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private void flat(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int l = this.leftPos, t = this.topPos;
        // the wagon-wood frame: dark planks with a lighter edge, iron brackets at the corners
        g.fill(l - 4, t - 4, l + W + 4, t + H + 4, 0xFF1E140C);
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, 0xFF6A4628);
        for (int y = t - 3; y < t + H + 3; y += 6) g.fill(l - 3, y, l + W + 3, y + 1, 0x40000000);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, 0xFF2A1C10);
        // the parchment, a little stained toward the edges
        g.fill(l, t, l + W, t + H, 0xFFE8D6AE);
        g.fill(l, t, l + W, t + 3, 0xFFD4BE90);
        g.fill(l, t + H - 3, l + W, t + H, 0xFFD4BE90);
        g.fill(l, t, l + 3, t + H, 0xFFD8C498);
        g.fill(l + W - 3, t, l + W, t + H, 0xFFD8C498);
        for (int[] c : new int[][]{{l - 4, t - 4}, {l + W - 6, t - 4}, {l - 4, t + H - 6}, {l + W - 6, t + H - 6}}) {
            g.fill(c[0], c[1], c[0] + 10, c[1] + 10, 0xFF3A3A40);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 9, c[1] + 9, 0xFF6A6A72);
            g.fill(c[0] + 4, c[1] + 4, c[0] + 6, c[1] + 6, 0xFF2A2A30);
        }
        // the canvas strip across the top, with his name
        g.fill(l + 6, t + 5, l + W - 6, t + 21, 0xFFB8A888);
        g.fill(l + 7, t + 6, l + W - 7, t + 20, 0xFFF2EBD8);
        for (int x = l + 20; x < l + W - 10; x += 24) g.fill(x, t + 6, x + 1, t + 20, 0x30604020);
        this.flat(g, Component.literal("The Traveling Merchant").withStyle(ChatFormatting.BOLD), l + W / 2, t + 9, 0xFF5A3418);
        // his talk
        g.pose().pushMatrix();
        g.pose().translate(l + W / 2, t + 26);
        g.pose().scale(0.85F, 0.85F);
        this.flat(g, Component.literal(this.line).withStyle(ChatFormatting.ITALIC), 0, 0, INK_DIM);
        g.pose().popMatrix();
        // your gold, and when he leaves
        long carried = GoldMoney.carried(this.minecraft.player);
        this.smallItem(g, new ItemStack(Items.GOLD_INGOT), l + 8, t + 36);
        g.text(this.font, GoldMoney.format(carried), l + 18, t + 35, INK, false);
        int secs = this.menu.secondsLeft();
        String leaves = "Leaves at dawn: " + (secs / 60) + ":" + String.format("%02d", secs % 60);
        g.text(this.font, leaves, l + W - 8 - this.font.width(leaves), t + 35, INK_DIM, false);

        List<TravelingMerchantMenu.Ware> wares = this.menu.wares();
        int hovered = this.cardAt(mouseX, mouseY);
        if (wares.isEmpty()) this.flat(g, "\"Sold out of everything, friend.\"", l + W / 2, t + 90, INK_DIM);
        for (int i = 0; i < wares.size(); i++) {
            TravelingMerchantMenu.Ware ware = wares.get(i);
            int x = cardX(i), y = cardY(i);
            boolean sold = this.menu.sold(i);
            boolean picked = i == this.selected;
            // a cloth card: a dark edge (gold when picked, lighter when hovered)
            g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, picked ? GOLD : i == hovered && !sold ? 0xFF8A6438 : 0xFF5A4028);
            g.fill(x, y, x + CARD_W, y + CARD_H, sold ? 0xFFB8A888 : picked ? 0xFFFFF4D8 : 0xFFF6E8C8);
            g.fill(x, y, x + CARD_W, y + 2, sold ? 0xFF9A8A6A : 0xFF8A3A24);
            this.bigItem(g, ware.stack(), x + CARD_W / 2 - 16, y + 3);
            String name = ware.stack().getHoverName().getString();
            if (ware.stack().getCount() > 1) name = ware.stack().getCount() + "x " + name;
            // the whole name, on up to two small lines
            List<String> rows = this.wrap(name, (int) ((CARD_W - 4) / 0.75F));
            for (int r = 0; r < Math.min(2, rows.size()); r++) this.smallText(g, rows.get(r), x + CARD_W / 2, y + (rows.size() == 1 ? 40 : 36) + r * 7, INK);
            boolean afford = this.minecraft.player.isCreative() || carried >= ware.price();
            this.smallText(g, GoldMoney.format(ware.price()), x + CARD_W / 2, y + 51, sold ? INK_DIM : afford ? GOOD : BAD);
            // more than one of it: how many are left, in the corner
            if (!sold && this.menu.left(i) > 1) this.smallText(g, this.menu.left(i) + " left", x + CARD_W - 12, y + 4, INK_DIM);
            if (sold) {
                // a red SOLD stamp across it
                g.fill(x + 4, y + 18, x + CARD_W - 4, y + 30, 0xC0A02818);
                this.flat(g, Component.literal("SOLD").withStyle(ChatFormatting.BOLD), x + CARD_W / 2, y + 20, 0xFFFFE8D0);
            }
        }
        this.drawBuyBar(g, mouseX, mouseY, carried);
        if (hovered >= 0) this.tooltip(g, wares.get(hovered), hovered, carried, mouseX, mouseY);
    }

    // words onto lines no wider than `width` (font pixels); a word too long on its own is cut
    private List<String> wrap(String text, int width) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = row.isEmpty() ? word : row + " " + word;
            if (this.font.width(next) > width && !row.isEmpty()) {
                rows.add(row.toString());
                row = new StringBuilder(word);
            } else {
                row = new StringBuilder(next);
            }
        }
        rows.add(row.toString());
        if (rows.size() > 2) rows.set(1, this.font.plainSubstrByWidth(rows.get(1) + " " + rows.get(2), width - 4) + "..");
        for (int i = 0; i < rows.size(); i++) rows.set(i, this.font.plainSubstrByWidth(rows.get(i), width));
        return rows;
    }

    private void drawBuyBar(GuiGraphicsExtractor g, int mouseX, int mouseY, long carried) {
        int l = this.leftPos, y = this.buyY();
        g.fill(l + 6, y - 3, l + W - 6, y + 21, 0xFF8A6A44);
        g.fill(l + 7, y - 2, l + W - 7, y + 20, 0xFFF2E4C2);
        if (this.selected < 0 || this.selected >= this.menu.wares().size()) {
            g.text(this.font, Component.literal("Pick something, then press Buy.").withStyle(ChatFormatting.ITALIC), l + 12, y + 5, INK_DIM, false);
            return;
        }
        TravelingMerchantMenu.Ware ware = this.menu.wares().get(this.selected);
        boolean sold = this.menu.sold(this.selected);
        g.item(ware.stack(), l + 10, y + 1);
        String text = ware.stack().getHoverName().getString() + ": " + GoldMoney.format(ware.price());
        g.text(this.font, this.font.plainSubstrByWidth(text, buyX() - (l + 30) - 4), l + 30, y + 5, INK, false);
        boolean ok = !sold && (this.minecraft.player.isCreative() || carried >= ware.price());
        boolean over = this.overBuy(mouseX, mouseY);
        g.fill(buyX(), y, buyX() + 56, y + 18, ok ? (over ? 0xFFFFD870 : GOLD) : 0xFF8A7A60);
        g.fill(buyX() + 1, y + 1, buyX() + 55, y + 17, ok ? (over ? 0xFF7A4A1C : 0xFF5A3414) : 0xFFB0A080);
        this.flat(g, sold ? "Sold" : "Buy", buyX() + 28, y + 5, ok ? 0xFFFFF0D0 : 0xFF6A5A40);
    }

    private void tooltip(GuiGraphicsExtractor g, TravelingMerchantMenu.Ware ware, int index, long carried, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, ware.stack()));
        if (this.menu.sold(index)) {
            lines.add(Component.literal("Sold: someone got here first.").withStyle(ChatFormatting.DARK_RED));
        } else {
            boolean afford = this.minecraft.player.isCreative() || carried >= ware.price();
            lines.add(Component.literal("Price: " + GoldMoney.format(ware.price())).withStyle(afford ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add(Component.literal((this.menu.left(index) > 1 ? this.menu.left(index) + " left." : "Only one.") + " Click to pick, then Buy.").withStyle(ChatFormatting.GRAY));
        }
        g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, ItemStack.EMPTY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        // drawn in extractBackground
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            if (this.selected >= 0 && this.overBuy(event.x(), event.y())) {
                if (!this.menu.sold(this.selected)) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.selected);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            int card = this.cardAt(event.x(), event.y());
            if (card >= 0) {
                this.selected = card;
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

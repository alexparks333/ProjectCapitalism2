package com.alexh.capitalism.client;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.librarian.KnowledgeQuality;
import com.alexh.capitalism.town.WhispererAdvice;
import com.alexh.capitalism.town.WhispererMenu;
import com.alexh.capitalism.town.WhispererStock;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Talking to the Whisperer (town/WhispererMenu), drawn in code: a dark cloak-colored panel with two tabs.
//  - Piece of Advice (first): his last tip on a strip of parchment, and an "Ask" button (1 gold).
//  - Knowledge: his Knowledge Pieces on a dark velvet cloth, each on a mat in its quality's color with its price in
//    emeralds (sold ones crossed out). Click one to buy it.
// The bottom line shows what you have to pay with (gold or emeralds) and, on Knowledge, a countdown to his restock.
public class WhispererScreen extends AbstractContainerScreen<WhispererMenu> {

    private static final int W = 232;
    private static final int H = 132;
    private static final int CARD = 32;
    private static final int GAP = 8;
    private static final int CONTENT_Y = 36;
    private static final String[] TABS = {"Piece of Advice", "Knowledge"};
    private static final int TEXT = 0xFFD8C8F0;
    private static final int DIM = 0xFF8A7AA8;

    // 0 = advice (first), 1 = knowledge
    private int tab;

    public WhispererScreen(WhispererMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    // ── layout ──

    private int tabX(int i) {
        int x = this.leftPos + 8;
        for (int k = 0; k < i; k++) x += this.font.width(TABS[k]) + 16;
        return x;
    }

    private int tabW(int i) {
        return this.font.width(TABS[i]) + 12;
    }

    private int cardX(int i) {
        int total = WhispererStock.SIZE * CARD + (WhispererStock.SIZE - 1) * GAP;
        return this.leftPos + (W - total) / 2 + i * (CARD + GAP);
    }

    private int cardAt(double mouseX, double mouseY) {
        for (int i = 0; i < WhispererStock.SIZE; i++) {
            if (mouseX >= cardX(i) && mouseX < cardX(i) + CARD && mouseY >= this.topPos + CONTENT_Y && mouseY < this.topPos + CONTENT_Y + CARD + 14) return i;
        }
        return -1;
    }

    // the "Ask" button on the advice tab
    private int askX() {
        return this.leftPos + W / 2 - 50;
    }

    private int askY() {
        return this.topPos + H - 44;
    }

    private boolean overAsk(double mouseX, double mouseY) {
        return mouseX >= askX() && mouseX < askX() + 100 && mouseY >= askY() && mouseY < askY() + 18;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int l = this.leftPos;
        int t = this.topPos;
        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, 0xFF0C0810);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, 0xFF5A4A6E);
        graphics.fill(l, t, l + W, t + H, 0xFF1E1626);

        // tabs
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i);
            boolean active = i == this.tab;
            boolean hover = mouseX >= x && mouseX < x + tabW(i) && mouseY >= t + 18 && mouseY < t + 32;
            graphics.fill(x, t + 18, x + tabW(i), t + 32, active ? 0xFFB8A0D8 : hover ? 0xFF6E5A8E : 0xFF3A2C4C);
            graphics.fill(x + 1, t + 19, x + tabW(i) - 1, t + 31, active ? 0xFF2E1F3E : 0xFF1E1626);
            graphics.text(this.font, TABS[i], x + 6, t + 21, active ? TEXT : DIM, false);
        }
        graphics.fill(l + 6, t + 32, l + W - 6, t + 33, 0xFFB8A0D8);

        if (this.tab == 0) this.drawAdvice(graphics, mouseX, mouseY);
        else this.drawKnowledge(graphics, mouseX, mouseY);
    }

    // ── Piece of Advice ──

    private void drawAdvice(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        // a strip of old parchment with the tip in faded ink
        int px0 = l + 12;
        int px1 = l + W - 12;
        int py0 = t + CONTENT_Y + 2;
        int py1 = t + H - 50;
        graphics.fill(px0, py0, px1, py1, 0xFFD8C8A0);
        graphics.fill(px0 + 1, py0 + 1, px1 - 1, py1 - 1, 0xFFE8DCB8);
        String advice = this.menu.advice();
        Component text = advice == null
            ? Component.literal("\"Gold for a whisper? I know many things...\"").withStyle(ChatFormatting.ITALIC)
            : Component.literal("\"" + advice + "\"").withStyle(ChatFormatting.ITALIC);
        List<FormattedCharSequence> lines = this.font.split(text, px1 - px0 - 12);
        int y = (py0 + py1) / 2 - lines.size() * 5;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, (px0 + px1) / 2 - this.font.width(line) / 2, y, advice == null ? 0xFF7A6A58 : 0xFF3A2A1A, false);
            y += 10;
        }

        // the Ask button: a gold ingot and the price
        boolean hover = this.overAsk(mouseX, mouseY);
        long gold = GoldMoney.carried(this.minecraft.player);
        boolean canPay = gold >= WhispererAdvice.PRICE_NUGGETS || this.minecraft.player.isCreative();
        graphics.fill(askX(), askY(), askX() + 100, askY() + 18, hover ? 0xFFF2D27A : 0xFF8A6A2A);
        graphics.fill(askX() + 1, askY() + 1, askX() + 99, askY() + 17, hover ? 0xFF4A3466 : 0xFF2E1F3E);
        graphics.item(new ItemStack(Items.GOLD_INGOT), askX() + 3, askY() + 1);
        graphics.text(this.font, advice == null ? "Ask: 1 gold" : "Ask again: 1 gold", askX() + 22, askY() + 5, canPay ? 0xFFF2D27A : 0xFFFF8078, false);

        String have = "You have " + GoldMoney.format(gold);
        graphics.text(this.font, have, l + 10, t + H - 16, DIM, false);
    }

    // ── Knowledge ──

    private void drawKnowledge(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        graphics.fill(l + 6, t + CONTENT_Y - 2, l + W - 6, t + H - 26, 0xFF2E1F3E);
        int hovered = this.cardAt(mouseX, mouseY);
        int have = WhispererMenu.emeralds(this.minecraft.player);
        for (int i = 0; i < WhispererStock.SIZE; i++) {
            KnowledgeQuality quality = this.menu.offer(i);
            int x = cardX(i);
            int y = t + CONTENT_Y + 2;
            int matColor = quality == null ? 0xFF16101C : 0xFF000000 | darken(quality.color);
            graphics.fill(x, y, x + CARD, y + CARD, i == hovered && quality != null ? 0xFFE8D8FF : 0xFF0C0810);
            graphics.fill(x + 1, y + 1, x + CARD - 1, y + CARD - 1, matColor);
            if (quality == null) {
                for (int k = 4; k < CARD - 4; k++) {
                    graphics.fill(x + k, y + k, x + k + 1, y + k + 1, 0xFF5A4A6E);
                    graphics.fill(x + CARD - 1 - k, y + k, x + CARD - k, y + k + 1, 0xFF5A4A6E);
                }
                graphics.centeredText(this.font, "Sold", x + CARD / 2, y + CARD + 4, 0xFF6E6290);
            } else {
                graphics.item(quality.stack(1), x + 8, y + 8);
                graphics.item(new ItemStack(Items.EMERALD), x + 1, y + CARD + 1);
                graphics.text(this.font, "x" + quality.emeralds(), x + 18, y + CARD + 5, have >= quality.emeralds() ? 0xFF8AF09A : 0xFFFF8078, true);
            }
        }
        graphics.centeredText(this.font, Component.literal("\"Knowledge... for a price.\"").withStyle(ChatFormatting.ITALIC), l + W / 2, t + H - 38, 0xFF7A6A98);

        // bottom line: your emeralds on the left, the restock countdown on the right
        graphics.item(new ItemStack(Items.EMERALD), l + 6, t + H - 21);
        graphics.text(this.font, "x" + have, l + 24, t + H - 16, TEXT, false);
        int seconds = this.menu.secondsUntilRestock();
        String restock = String.format(Locale.ROOT, "Restocks in %d:%02d", seconds / 60, seconds % 60);
        graphics.text(this.font, restock, l + W - 10 - this.font.width(restock), t + H - 16, DIM, false);

        if (hovered >= 0) {
            KnowledgeQuality quality = this.menu.offer(hovered);
            List<Component> lines = new ArrayList<>();
            if (quality == null) {
                lines.add(Component.literal("Already sold").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal(quality.title + " Knowledge Piece").withStyle(style -> style.withColor(quality.color).withBold(true)));
                lines.add(Component.literal("Writes better books at the Librarian Table").withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("Click to buy for " + quality.emeralds() + (quality.emeralds() == 1 ? " emerald" : " emeralds"))
                    .withStyle(have >= quality.emeralds() ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.ITALIC), W / 2, 6, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            for (int i = 0; i < TABS.length; i++) {
                if (event.x() >= tabX(i) && event.x() < tabX(i) + tabW(i) && event.y() >= this.topPos + 18 && event.y() < this.topPos + 32) {
                    if (this.tab != i) {
                        this.tab = i;
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    }
                    return true;
                }
            }
            if (this.tab == 0 && this.overAsk(event.x(), event.y())) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, WhispererMenu.ADVICE);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            int card = this.tab == 1 ? this.cardAt(event.x(), event.y()) : -1;
            if (card >= 0 && this.menu.offer(card) != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, card);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static int darken(int rgb) {
        return (((rgb >> 16) & 0xFF) * 2 / 5 << 16) | (((rgb >> 8) & 0xFF) * 2 / 5 << 8) | ((rgb & 0xFF) * 2 / 5);
    }
}

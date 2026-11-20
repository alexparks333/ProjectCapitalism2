package com.alexh.capitalism.client;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.economy.InvestmentBank;
import com.alexh.capitalism.economy.InvestmentMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

// The Investment Chest open (economy/InvestmentMenu), a bank's brochure come to life: deep green felt in a gold frame.
//  - Left, YOUR VAULT: three slots for Gold Ingots (3 stacks: 192 gold at most), what's in it, and a little chart whose
//    line only ever goes up.
//  - A fat gold arrow (bobbing) with the rate on it.
//  - Right, DAILY RETURNS: what the vault pays each morning as it is now, what tomorrow morning will actually pay (on the
//    least it held since this morning: hover for why), the returns waiting, and COLLECT.
//  - A stock ticker scrolling along under it all, full of the bank's very serious financial advice.
public class InvestmentChestScreen extends AbstractContainerScreen<InvestmentMenu> {

    static final int W = 262, H = 222;
    private static final int GOLD = 0xFFE8C04A, GOLD_DARK = 0xFF8A6A1C, FELT = 0xFF14361F, FELT_DARK = 0xFF0C2414,
        PANEL = 0xFF0A1E10, TEXT = 0xFFF4EBD0, DIM = 0xFF9DB8A2, UP = 0xFF6CF08A, CREAM = 0xFFF2E6C4;
    // the vault on the left, the market in the gap (ARROW_X its middle), the returns on the right (wider: its numbers)
    private static final int LEFT_X = 8, PANEL_W = 82, ARROW_X = 117, RIGHT_X = 144, RIGHT_W = 110, PANEL_Y = 28, PANEL_H = 88;
    private static final int COLLECT_W = 70, COLLECT_X = RIGHT_X + (RIGHT_W - COLLECT_W) / 2, COLLECT_Y = 98, COLLECT_H = 15;
    private static final String TICKER = "GOLD: still shiny   •   "
        + "Past performance guarantees future results*   •   Our analysts (a pigeon) rate this chest: BUY   •   "
        + "Line goes up ↗   •   Diversify! (Into three slots.)   •   *Not a guarantee. Mostly a guarantee.   •   "
        + "MATTRESS ▼ -100%   •   ";

    private int ticks;

    public InvestmentChestScreen(InvestmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventoryLabelX = InvestmentMenu.INV_X;
        this.inventoryLabelY = InvestmentMenu.INV_Y - 10;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int l = this.leftPos, t = this.topPos;
        float time = this.ticks + a;
        // the frame: a double gold edge, green felt, gold corner brackets
        g.fill(l - 4, t - 4, l + W + 4, t + H + 4, 0xFF050A06);
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, GOLD_DARK);
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, GOLD);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, GOLD_DARK);
        g.fill(l, t, l + W, t + H, FELT);
        for (int y = 0; y < H; y += 2) g.fill(l, t + y, l + W, t + y + 1, FELT_DARK & 0x30FFFFFF);
        g.fill(l + 6, t + 25, l + W - 6, t + 26, GOLD_DARK);

        // ── YOUR VAULT ──
        this.panel(g, l + LEFT_X, t + PANEL_Y, PANEL_W);
        g.centeredText(this.font, Component.literal("YOUR VAULT"), l + LEFT_X + PANEL_W / 2, t + PANEL_Y + 4, GOLD);
        for (int i = 0; i < InvestmentBank.SLOTS; i++) {
            int sx = l + InvestmentMenu.VAULT_X + i * 18, sy = t + InvestmentMenu.VAULT_Y;
            g.fill(sx - 2, sy - 2, sx + 18, sy + 18, GOLD_DARK);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A1206);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF3A2A0C);
            if (this.menu.getSlot(i).getItem().isEmpty()) {
                // a faint ingot in an empty slot: this is where the gold goes
                g.item(new ItemStack(Items.GOLD_INGOT), sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, 0xB03A2A0C);
            }
        }
        long in = this.menu.invested();
        g.centeredText(this.font, GoldMoney.format(in), l + LEFT_X + PANEL_W / 2, t + 67, in > 0 ? TEXT : DIM);
        g.centeredText(this.font, "of " + GoldMoney.toGold(InvestmentBank.MAX) + " max", l + LEFT_X + PANEL_W / 2, t + 77, DIM);
        this.chart(g, l + LEFT_X + 6, t + 88, PANEL_W - 12, 24, time);

        // ── the arrow ──
        int ax = l + ARROW_X - 15, ay = t + 57 + Math.round(Mth.sin(time * 0.15F) * 1.5F);
        // today's market: the rate in its mood's colour (a crash blinks), its mood, how long it lasts
        InvestmentBank.Mood mood = InvestmentBank.Mood.of(this.menu.rate());
        boolean blink = mood == InvestmentBank.Mood.CRASH && (this.ticks / 10) % 2 == 0;
        int cx = l + ARROW_X;
        g.centeredText(this.font, Component.literal(this.menu.rate() + "%").withStyle(ChatFormatting.BOLD), cx, t + 33, blink ? 0xFFFFFFFF : 0xFF000000 | mood.color);
        g.centeredText(this.font, "a day", cx, t + 43, DIM);
        g.centeredText(this.font, mood.title, cx, t + 78, 0xFF000000 | mood.color);
        int left = Math.max(1, this.menu.daysLeft());
        g.centeredText(this.font, left + (left == 1 ? " day" : " days"), cx, t + 89, DIM);
        g.centeredText(this.font, "left", cx, t + 98, DIM);
        g.fill(ax, ay, ax + 22, ay + 8, GOLD_DARK);
        g.fill(ax + 1, ay + 1, ax + 22, ay + 7, GOLD);
        for (int k = 0; k < 8; k++) {
            g.fill(ax + 22 + k, ay - 5 + k, ax + 23 + k, ay + 13 - k, k == 0 || k == 7 ? GOLD_DARK : GOLD);
        }
        g.fill(ax + 2, ay + 2, ax + 20, ay + 3, 0xFFFFF2B0);

        // ── DAILY RETURNS ──
        this.panel(g, l + RIGHT_X, t + PANEL_Y, RIGHT_W);
        int rx = l + RIGHT_X + RIGHT_W / 2;
        g.centeredText(this.font, Component.literal("DAILY RETURNS"), rx, t + PANEL_Y + 4, GOLD);
        // what the vault earns a day as it is now, at today's rate
        long perDay = this.menu.dailyOn(in);
        long waiting = this.menu.returns();
        boolean full = waiting >= InvestmentBank.READY_MAX;
        if (full) {
            // Ready is full: it earns nothing until it's collected
            boolean flash = (this.ticks / 10) % 2 == 0;
            g.centeredText(this.font, Component.literal("READY IS FULL").withStyle(ChatFormatting.BOLD), rx, t + 44, flash ? 0xFFFF6A5A : 0xFFFFB0A0);
            g.centeredText(this.font, "collect to earn again", rx, t + 55, 0xFFFF9A8A);
        } else if (in > 0) {
            g.centeredText(this.font, Component.literal("+" + shortGold(perDay) + " a day").withStyle(ChatFormatting.BOLD), rx, t + 44, UP);
            g.centeredText(this.font, "paid every morning", rx, t + 55, DIM);
        } else {
            g.centeredText(this.font, "Put gold in to earn!", rx, t + 44, DIM);
            g.centeredText(this.font, "paid every morning", rx, t + 55, DIM);
        }
        g.fill(l + RIGHT_X + 8, t + 68, l + RIGHT_X + RIGHT_W - 8, t + 69, 0xFF1E3A26);
        // what's been paid and not taken yet (it holds 64 gold at most)
        g.centeredText(this.font, "Ready: " + shortGold(waiting).replace(" gold", "") + " / " + GoldMoney.toGold(InvestmentBank.READY_MAX) + " gold",
            rx, t + 80, full ? 0xFFFF6A5A : waiting > 0 ? GOLD : DIM);
        boolean can = waiting > 0, over = this.overCollect(mouseX, mouseY);
        int bx = l + COLLECT_X, by = t + COLLECT_Y;
        g.fill(bx, by, bx + COLLECT_W, by + COLLECT_H, can ? (over ? 0xFFFFF0A0 : GOLD) : 0xFF3A4A3E);
        g.fill(bx + 1, by + 1, bx + COLLECT_W - 1, by + COLLECT_H - 1, can ? (over ? 0xFF2E7A40 : 0xFF1F5A2E) : 0xFF16261A);
        g.centeredText(this.font, Component.literal("COLLECT").withStyle(ChatFormatting.BOLD), bx + COLLECT_W / 2, by + 4, can ? CREAM : 0xFF5A6A5E);

        // ── the ticker ──
        int ty = t + 119;
        g.fill(l + 6, ty, l + W - 6, ty + 10, 0xFF050A06);
        g.enableScissor(l + 7, ty, l + W - 7, ty + 10);
        String rate = this.menu.rate() + ".00%";
        boolean crash = mood == InvestmentBank.Mood.CRASH;
        String ticker = "CAPITALISM INDEX " + (crash ? "▼ " : "▲ +") + rate + "   •   " + TICKER
            + "BANK " + (crash ? "▼ (temporarily) " : "▲ +") + rate + "   •   "
            + (crash ? "DON'T PANIC   •   " : mood == InvestmentBank.Mood.BULL ? "BULL RUN!!   •   " : "");
        int width = this.font.width(ticker);
        int off = (int) (time * 0.8F) % width;
        for (int x = l + 7 - off; x < l + W; x += width) g.text(this.font, ticker, x, ty + 1, 0xFFFFD86A, false);
        g.disableScissor();

        // the inventory's slots
        for (Slot slot : this.menu.slots) {
            if (slot.index < InvestmentBank.SLOTS) continue;
            g.fill(l + slot.x - 1, t + slot.y - 1, l + slot.x + 17, t + slot.y + 17, 0xFF06140A);
            g.fill(l + slot.x, t + slot.y, l + slot.x + 16, t + slot.y + 16, 0xFF24452E);
        }
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x - 1, y - 1, x + w + 1, y + PANEL_H + 1, GOLD_DARK);
        g.fill(x, y, x + w, y + PANEL_H, PANEL);
    }

    static String shortGold(long nuggets) {
        return InvestmentBank.shortGold(nuggets);
    }

    // a tiny stock chart: the line wobbles, but it only ever goes up (the left of the chart is always lower)
    private void chart(GuiGraphicsExtractor g, int x, int y, int w, int h, float time) {
        g.fill(x, y, x + w, y + h, 0xFF06120A);
        for (int gx = x + 7; gx < x + w; gx += 7) g.fill(gx, y, gx + 1, y + h, 0xFF0E2214);
        int prev = -1;
        for (int i = 0; i < w; i++) {
            float k = i + time * 0.25F;
            float wobble = Mth.sin(k * 0.55F) * 2.0F + Mth.sin(k * 0.21F + 1.3F) * 1.5F;
            int v = Mth.clamp(Math.round(h - 4 - i * (h - 8) / (float) w + wobble), 1, h - 2);
            g.fill(x + i, y + v + 1, x + i + 1, y + h, 0x4036C060);
            int from = prev < 0 ? v : Math.min(prev, v), to = prev < 0 ? v : Math.max(prev, v);
            g.fill(x + i, y + from, x + i + 1, y + to + 1, UP);
            prev = v;
        }
    }

    private boolean overCollect(double mx, double my) {
        int bx = this.leftPos + COLLECT_X, by = this.topPos + COLLECT_Y;
        return mx >= bx && mx < bx + COLLECT_W && my >= by && my < by + COLLECT_H;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), W / 2, 6, GOLD);
        g.centeredText(this.font, Component.literal("Your gold. Your vault. Nobody else's.").withStyle(ChatFormatting.ITALIC), W / 2, 15, DIM);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, DIM, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int l = this.leftPos, t = this.topPos;
        List<FormattedCharSequence> tip = new ArrayList<>();
        if (mouseX >= l + LEFT_X && mouseX < l + LEFT_X + PANEL_W && mouseY >= t + 62 && mouseY < t + PANEL_Y + PANEL_H) {
            line(tip, "Your vault", GOLD);
            line(tip, "Gold Ingots only, 3 stacks at most.", TEXT);
            line(tip, "It counts in your net worth, but", TEXT);
            line(tip, "/pay and shops can't spend it:", TEXT);
            line(tip, "take it out first.", TEXT);
        }
        if (!tip.isEmpty()) g.setTooltipForNextFrame(this.font, tip, mouseX, mouseY);
    }

    private static void line(List<FormattedCharSequence> tip, String s, int color) {
        tip.add(Component.literal(s).withColor(color & 0xFFFFFF).getVisualOrderText());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.overCollect(event.x(), event.y()) && this.minecraft.gameMode != null) {
            if (this.menu.returns() > 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, InvestmentMenu.COLLECT);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

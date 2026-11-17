package com.alexh.capitalism.client;

import com.alexh.capitalism.bounty.Bounties;
import com.alexh.capitalism.bounty.BountyData;
import com.alexh.capitalism.bounty.BountyTask;
import com.alexh.capitalism.bounty.Standings;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.town.BountyClerk;
import com.alexh.capitalism.town.BountyMenu;
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

import java.util.List;

// Talking to the Bounty Clerk (town/BountyMenu, the rules in bounty/Bounties), drawn in code: a dark oak panel with a
// brass edge and rivets, a parchment page inside it, three tabs, your gold by the tabs.
//  - Today's Task: the task posted for everyone, pinned up as a notice (its title, its tier, the reward, the 2-nugget
//    fee) and "Take this task" (asking first if it would replace yours); why you can't, if you can't; once you've taken
//    one today, how long till you can take another.
//  - My Task: your one task, its tier, a progress bar, the reward, Turn In (when it's done) and Abandon (click twice).
//  - Ledger: how many you've turned in, the gold it's earned you, the race for the 3 Fun Wishing Stars (the first 3 to
//    turn in 50), your progress, your star to claim.
public class BountyClerkScreen extends AbstractContainerScreen<BountyMenu> {

    private static final int W = 260;
    private static final int H = 222;
    private static final String[] TABS = {"Today's Task", "My Task", "Ledger"};
    private static final int INK = 0xFF3A2414;
    private static final int INK_DIM = 0xFF7A6040;
    private static final int PAGE = 0xFFEADCB8;
    private static final int PAGE_DARK = 0xFFD8C69C;
    private static final int OAK = 0xFF3A2414;
    private static final int BRASS = 0xFFD8B060;
    private static final int GOOD = 0xFF2E8A3A;
    private static final int BAD = 0xFFA83028;

    private int tab;
    private int ticks;
    // Abandon and an exchange ask twice: when the first click was (or -1)
    private int abandonArmed = -1;
    private int abandonAt;
    private int exchangeArmed = -1;

    public BountyClerkScreen(BountyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
        if (this.abandonArmed >= 0 && this.ticks - this.abandonAt > 60) this.abandonArmed = -1;
        if (this.exchangeArmed >= 0 && this.ticks - this.exchangeArmed > 80) this.exchangeArmed = -1;
    }

    // (dev: client/PhotoShoot) show this tab
    public void showTab(int i) {
        this.tab = i;
    }

    // (dev: client/PhotoShoot) show this standings board on the Ledger
    public void showBoard(int i) {
        this.board = i;
    }

    private BountyData data() {
        return Bounties.of(this.minecraft.player);
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

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // Today: the Take button
    private int takeX() {
        return this.leftPos + W / 2 - 60;
    }

    private int takeY() {
        return this.topPos + 160;
    }

    // My Task: its row
    private int rowY() {
        return this.topPos + 70;
    }

    private int turnInX() {
        return this.leftPos + W / 2 - 78;
    }

    private int abandonX() {
        return this.leftPos + W / 2 + 8;
    }

    private int claimX() {
        return this.leftPos + W - 116;
    }

    private int claimY() {
        return this.topPos + 56;
    }

    // ── drawing ──

    // parchment text: dark ink, crisp, centred, no shadow
    private void ink(GuiGraphicsExtractor g, String text, int cx, int y, int colour) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, colour, false);
    }

    private void ink(GuiGraphicsExtractor g, Component text, int cx, int y, int colour) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, colour, false);
    }

    private void ink(GuiGraphicsExtractor g, FormattedCharSequence text, int cx, int y, int colour) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, colour, false);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean ok, boolean over) {
        g.fill(x, y, x + w, y + h, ok ? (over ? 0xFFFFE8A0 : BRASS) : 0xFF8A7A60);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, ok ? (over ? 0xFF6A3E1E : 0xFF4E2C14) : 0xFF6A5E4A);
        g.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, ok ? 0xFFF4E6C0 : 0xFFB0A488);
    }

    private void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double frac, int colour) {
        g.fill(x, y, x + w, y + h, 0xFF5A4630);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFCDBB92);
        int fill = (int) Math.round((w - 2) * Math.max(0, Math.min(1, frac)));
        if (fill > 0) g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, colour);
    }

    private void tierBadge(GuiGraphicsExtractor g, BountyTask.Tier tier, int x, int y) {
        int w = this.font.width(tier.title) + 8;
        g.fill(x, y, x + w, y + 11, 0xFF000000 | tier.color);
        g.fill(x + 1, y + 1, x + w - 1, y + 10, 0xFF2A1A10);
        g.text(this.font, tier.title, x + 4, y + 2, 0xFF000000 | tier.color, false);
    }

    // a round red wax seal, centred on (x, y): darker rim, a stamped ring, a highlight
    private void seal(GuiGraphicsExtractor g, int x, int y) {
        int r = 7;
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x - half, y + dy, x + half + 1, y + dy + 1, 0xFF8A1414);
        }
        for (int dy = -r + 1; dy <= r - 1; dy++) {
            int half = (int) Math.round(Math.sqrt((r - 1) * (r - 1) - dy * dy));
            g.fill(x - half, y + dy, x + half + 1, y + dy + 1, 0xFFC02828);
        }
        for (int dy = -3; dy <= 3; dy++) {
            int half = (int) Math.round(Math.sqrt(9 - dy * dy));
            g.fill(x - half, y + dy, x - half + 1, y + dy + 1, 0xFF8A1414);
            g.fill(x + half, y + dy, x + half + 1, y + dy + 1, 0xFF8A1414);
        }
        g.fill(x - 3, y - 5, x - 1, y - 4, 0xFFE86060);
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int l = this.leftPos;
        int t = this.topPos;
        // a dark oak panel, a brass edge, a parchment page inside
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, 0xFF100804);
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, BRASS);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, 0xFF5A3A20);
        g.fill(l, t, l + W, t + H, OAK);
        for (int k = 0; k < 6; k++) g.fill(l, t + 4 + k * 33, l + W, t + 5 + k * 33, 0x22000000);   // the grain
        g.fill(l + 5, t + 34, l + W - 5, t + H - 5, PAGE_DARK);
        g.fill(l + 6, t + 35, l + W - 6, t + H - 6, PAGE);
        for (int y = t + 50; y < t + H - 8; y += 12) g.fill(l + 10, y, l + W - 10, y + 1, 0x14503820);   // faint ruled lines
        for (int[] c : new int[][]{{l + 3, t + 3}, {l + W - 6, t + 3}, {l + 3, t + H - 6}, {l + W - 6, t + H - 6}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, BRASS);
            g.fill(c[0], c[1], c[0] + 1, c[1] + 1, 0xFFFFF0C0);
        }
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i);
            boolean active = i == this.tab;
            boolean hover = over(mouseX, mouseY, x, t + 18, tabW(i), 14);
            g.fill(x, t + 18, x + tabW(i), t + 34, active ? BRASS : hover ? 0xFF8A6438 : 0xFF5A3A20);
            g.fill(x + 1, t + 19, x + tabW(i) - 1, t + 34, active ? PAGE : 0xFF2A1A10);
            g.text(this.font, TABS[i], x + 6, t + 22, active ? INK : 0xFFC8B088, false);
        }
        // your gold
        String have = GoldMoney.format(GoldMoney.carried(this.minecraft.player));
        int hx = l + W - 10 - this.font.width(have);
        g.item(new ItemStack(Items.GOLD_INGOT), hx - 17, t + 1);
        g.text(this.font, have, hx, t + 5, 0xFFF4E6C0, false);
        if (this.tab == 0) this.drawToday(g, mouseX, mouseY);
        else if (this.tab == 1) this.drawTasks(g, mouseX, mouseY);
        else this.drawLedger(g, mouseX, mouseY);
    }

    // ── Today's Task ──

    private static String countdown(long ticks) {
        long secs = ticks / 20;
        return secs >= 60 ? (secs / 60) + " min " + (secs % 60) + " s" : secs + " s";
    }

    private void drawToday(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        BountyData data = this.data();
        BountyData.Open offer = data.offer();
        if (offer == null || offer.task() == null) {
            this.ink(g, "The Clerk is pinning up a new notice...", l + W / 2, t + 80, INK_DIM);
            return;
        }
        BountyTask task = offer.task();
        // the notice: a pinned card, a tier-coloured strip along its top, a red wax seal on its top-right corner
        int cx = l + 34, cy = t + 44, cw = W - 68, ch = 96;
        g.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFFB8A57E);
        g.fill(cx, cy, cx + cw, cy + ch, 0xFFFBF5E2);
        g.fill(cx, cy, cx + cw, cy + 3, 0xFF000000 | task.tier.color);
        g.fill(cx + cw / 2 - 1, cy - 3, cx + cw / 2 + 2, cy + 1, 0xFF8A8A96);   // the pin
        this.seal(g, cx + cw - 4, cy + 4);
        this.ink(g, Component.literal("BOUNTY").withStyle(ChatFormatting.BOLD), cx + cw / 2, cy + 9, 0xFF7A2014);
        g.fill(cx + 16, cy + 20, cx + cw - 16, cy + 21, 0x40503820);
        List<FormattedCharSequence> lines = this.font.split(Component.literal(offer.title()).withStyle(ChatFormatting.BOLD), cw - 20);
        int y = cy + 27;
        for (FormattedCharSequence line : lines) {
            this.ink(g, line, cx + cw / 2, y, INK);
            y += 11;
        }
        this.tierBadge(g, task.tier, cx + cw / 2 - (this.font.width(task.tier.title) + 8) / 2, y + 2);
        g.fill(cx + 16, cy + ch - 30, cx + cw - 16, cy + ch - 29, 0x40503820);
        this.ink(g, "Reward: " + GoldMoney.format(offer.reward()), cx + cw / 2, cy + ch - 24, GOOD);
        this.ink(g, "Fee " + Bounties.FEE + " gold nuggets · no time limit", cx + cw / 2, cy + ch - 12, INK_DIM);        // can they take it?
        long today = Bounties.today(this.minecraft.level);
        boolean takenToday = data.takenDay() == today;
        boolean canDo = !task.conditional() || this.possibleForMe(task);
        boolean gold = this.minecraft.player.isCreative() || GoldMoney.carried(this.minecraft.player) >= Bounties.FEE;
        String label = takenToday ? "Taken one today" : !canDo ? "Not for you" : this.exchangeArmed >= 0 ? "Yes, exchange it" : "Take this task";
        boolean ok = !takenToday && canDo && gold;
        this.button(g, takeX(), takeY() - 10, 120, 20, label, ok, over(mouseX, mouseY, takeX(), takeY() - 10, 120, 20));
        String note = null;
        int noteColour = INK_DIM;
        if (takenToday) note = "You can take another in " + countdown(Bounties.ticksToTomorrow(this.minecraft.level)) + ".";
        else if (!canDo) { note = this.whyNot(task); noteColour = BAD; }
        else if (!gold) { note = "The fee is " + Bounties.FEE + " gold nuggets."; noteColour = BAD; }
        else if (this.exchangeArmed >= 0 && data.task() != null) { note = "This replaces your current task: " + data.task().title() + " (no refund)."; noteColour = BAD; }
        else if (data.task() != null) note = "Taking it replaces your task: " + data.task().title() + ".";
        if (note != null) {
            int ny = takeY() + 14;
            for (FormattedCharSequence line : this.font.split(Component.literal(note), W - 30)) {
                this.ink(g, line, l + W / 2, ny, noteColour);
                ny += 10;
            }
        }
    }

    // town tasks this player can't do (the client's best guess; the server decides): no pet, no Shopfront...
    private boolean possibleForMe(BountyTask task) {
        return switch (task.kind) {
            case PET_FEED -> this.minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                this.minecraft.player.getBoundingBox().inflate(128), a -> com.alexh.capitalism.pets.Pets.claimed(a)
                    && com.alexh.capitalism.pets.Pets.isOwner(a, this.minecraft.player)).size() > 0;
            default -> true;
        };
    }

    private String whyNot(BountyTask task) {
        return switch (task.kind) {
            case PET_FEED -> "You need a pet of your own for this one.";
            case SHOP_SELL -> "You need a Shopfront of your own for this one.";
            case SHOP_BUY -> "There's no other player's Shopfront to buy from.";
            case COOK_BUY -> "There's no Cook's Shopfront to buy from.";
            default -> "You can't take this one.";
        };
    }

    // ── My Task ──

    private void drawTasks(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        BountyData data = this.data();
        BountyData.Open o = data.task();
        if (o == null || o.task() == null) {
            this.ink(g, Component.literal("No task. Take the posted one on the first tab.").withStyle(ChatFormatting.ITALIC), l + W / 2, t + 90, INK_DIM);
            return;
        }
        BountyTask task = o.task();
        int y = rowY();
        g.fill(l + 12, y - 20, l + W - 12, y + 52, 0xFFF2E6C6);
        g.fill(l + 12, y - 20, l + 15, y + 52, 0xFF000000 | task.tier.color);
        // the title always fits the card: bold if there's room, plain if not, two lines if it's still too long
        int room = W - 44;
        Component bold = Component.literal(o.title()).withStyle(ChatFormatting.BOLD);
        if (this.font.width(bold) <= room) {
            this.ink(g, bold, l + W / 2, y - 12, INK);
        } else if (this.font.width(o.title()) <= room) {
            this.ink(g, o.title(), l + W / 2, y - 12, INK);
        } else {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(o.title()), room);
            for (int i = 0; i < Math.min(2, lines.size()); i++) this.ink(g, lines.get(i), l + W / 2, y - 16 + i * 9, INK);
        }
        this.tierBadge(g, task.tier, l + W / 2 - (this.font.width(task.tier.title) + 8) / 2, y + 2);
        int p = Bounties.progress(this.minecraft.player, o);
        boolean done = p >= o.amount();
        this.bar(g, l + 30, y + 18, W - 60, 10, p / (double) o.amount(), done ? 0xFF4CAF50 : 0xFF000000 | task.tier.color);
        this.ink(g, p + " / " + o.amount() + (task.kind == BountyTask.Kind.DELIVER ? " in your bags" : ""), l + W / 2, y + 32, INK_DIM);
        this.ink(g, "Reward: " + GoldMoney.format(o.reward()), l + W / 2, y + 42, GOOD);
        this.button(g, turnInX(), t + 140, 70, 20, "Turn In", done, over(mouseX, mouseY, turnInX(), t + 140, 70, 20));
        boolean armed = this.abandonArmed >= 0;
        this.button(g, abandonX(), t + 140, 70, 20, armed ? "Really?" : "Abandon", true, over(mouseX, mouseY, abandonX(), t + 140, 70, 20));
        if (armed) this.ink(g, "Click again to give it up (no refund).", l + W / 2, t + H - 18, BAD);
    }

    // ── Ledger ──

    private static final int[] MEDALS = {0xFFC8961E, 0xFF8C8C98, 0xFF9A5A2A};

    // which standings board shows (cycled with the arrows)
    private int board;

    private int arrowY() {
        return this.topPos + 83;
    }

    private void drawLedger(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        BountyData data = this.data();
        // your page: turned in, gold earned
        g.text(this.font, "Turned in: " + data.completed(), l + 14, t + 39, INK, false);
        String earned = "Gold earned: " + GoldMoney.format(data.earned());
        g.text(this.font, earned, l + W - 14 - this.font.width(earned), t + 39, GOOD, false);
        // the milestone: a reward for the first 3 to turn in 50 (what it is isn't said till it's claimed)
        List<String> winners = data.winners();
        g.fill(l + 10, t + 50, l + W - 10, t + 79, 0xFFF2E6C6);
        g.text(this.font, Component.literal("Reward at " + Bounties.MILESTONE + " completions").withStyle(ChatFormatting.BOLD), l + 14, t + 53, INK, false);
        if (data.place() >= 0 && !data.claimed()) {
            g.text(this.font, "You're " + Bounties.ordinal(data.place() + 1) + "!", l + 14, t + 66, 0xFF8A5A10, false);
            this.button(g, claimX(), claimY() + 6, 100, 16, "Claim your reward", true, over(mouseX, mouseY, claimX(), claimY() + 6, 100, 16));
        } else if (data.place() >= 0) {
            g.text(this.font, "You were " + Bounties.ordinal(data.place() + 1) + ": your reward is claimed ✔", l + 14, t + 66, GOOD, false);
        } else if (winners.size() >= 3) {
            g.text(this.font, "All 3 rewards have been claimed.", l + 14, t + 66, INK_DIM, false);
        } else {
            int left = 3 - winners.size();
            g.text(this.font, left + (left == 1 ? " reward left" : " rewards left") + " · you: " + Math.min(data.completed(), Bounties.MILESTONE) + " / " + Bounties.MILESTONE,
                l + 14, t + 66, INK, false);
            this.bar(g, l + W - 90, t + 66, 76, 8, data.completed() / (double) Bounties.MILESTONE, 0xFFD8A030);
        }
        // the standings: one board at a time (◀ ▶ to switch), the top 10, medals for the first three, ★ by the
        // milestone's winners, your row picked out
        List<BountyData.Board> boards = data.boards();
        if (boards.isEmpty()) return;
        this.board = Math.floorMod(this.board, boards.size());
        BountyData.Board b = boards.get(this.board);
        Standings.Category cat = category(b.category());
        int y = arrowY();
        boolean many = boards.size() > 1;
        this.button(g, l + 12, y - 2, 12, 12, "◀", many, many && over(mouseX, mouseY, l + 12, y - 2, 12, 12));
        this.button(g, l + 26, y - 2, 12, 12, "▶", many, many && over(mouseX, mouseY, l + 26, y - 2, 12, 12));
        g.text(this.font, Component.literal(cat == null ? b.category() : cat.title).withStyle(ChatFormatting.BOLD), l + 42, y, INK, false);
        if (cat != null) {
            // (the column's label, unless the board's title already says it: "Cravings satisfied")
            if (!cat.title.toLowerCase().contains(cat.unit.toLowerCase())) g.text(this.font, cat.unit, l + W - 74 - this.font.width(cat.unit), y, INK_DIM, false);
            if (!cat.extra.isEmpty()) g.text(this.font, cat.extra, l + W - 14 - this.font.width(cat.extra), y, INK_DIM, false);
        }
        y += 12;
        if (b.top().isEmpty()) g.text(this.font, Component.literal("Nobody's on this board yet.").withStyle(ChatFormatting.ITALIC), l + 14, y + 4, INK_DIM, false);
        for (int i = 0; i < b.top().size(); i++) {
            BountyData.Row r = b.top().get(i);
            int ry = y + i * 10;
            if (r.me()) g.fill(l + 10, ry - 1, l + W - 10, ry + 9, 0x50D8A030);
            int medal = i < 3 ? MEDALS[i] : INK_DIM;
            g.text(this.font, "#" + (i + 1), l + 14, ry, medal, false);
            g.text(this.font, this.font.plainSubstrByWidth(r.name(), 110) + (r.star() ? " ★" : ""), l + 40, ry, i < 3 ? medal : INK, false);
            String score = String.valueOf(r.score());
            g.text(this.font, score, l + W - 74 - this.font.width(score), ry, INK, false);
            if (cat != null && cat == Standings.Category.BOUNTIES) {
                String gold = GoldMoney.toGold(r.extra()) + "g";
                g.text(this.font, gold, l + W - 14 - this.font.width(gold), ry, GOOD, false);
            }
        }
        if (b.rank() > 10) {
            int ry = y + 10 * 10 + 2;
            g.fill(l + 10, ry - 1, l + W - 10, ry + 9, 0x50D8A030);
            g.text(this.font, "You: #" + b.rank() + ", " + b.score() + (cat == Standings.Category.BOUNTIES ? (b.score() == 1 ? " bounty" : " bounties") : cat != null ? " " + cat.unit : ""), l + 14, ry, INK, false);
        }
    }

    private static Standings.Category category(String id) {
        for (Standings.Category c : Standings.Category.values()) if (c.id.equals(id)) return c;
        return null;
    }
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.ITALIC), W / 2, 5, 0xFF000000 | BountyClerk.COLOR);
    }

    private void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void send(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            double mx = event.x(), my = event.y();
            for (int i = 0; i < TABS.length; i++) {
                if (over(mx, my, tabX(i), this.topPos + 18, tabW(i), 16)) {
                    if (this.tab != i) {
                        this.tab = i;
                        this.abandonArmed = -1;
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    }
                    return true;
                }
            }
            BountyData data = this.data();
            if (this.tab == 0 && over(mx, my, takeX(), takeY() - 10, 120, 20)) {
                // replacing a task asks first
                if (data.task() != null && this.exchangeArmed < 0) this.exchangeArmed = this.ticks;
                else {
                    send(BountyMenu.TAKE);
                    this.exchangeArmed = -1;
                }
                click();
                return true;
            }
            if (this.tab == 1 && data.task() != null) {
                if (over(mx, my, turnInX(), this.topPos + 140, 70, 20)) {
                    if (Bounties.done(this.minecraft.player, data.task())) send(BountyMenu.TURN_IN);
                    click();
                    return true;
                }
                if (over(mx, my, abandonX(), this.topPos + 140, 70, 20)) {
                    if (this.abandonArmed >= 0) {
                        send(BountyMenu.ABANDON);
                        this.abandonArmed = -1;
                    } else {
                        this.abandonArmed = 0;
                        this.abandonAt = this.ticks;
                    }
                    click();
                    return true;
                }
            }
            if (this.tab == 2 && data.boards().size() > 1) {
                if (over(mx, my, this.leftPos + 12, arrowY() - 2, 12, 12)) { this.board--; click(); return true; }
                if (over(mx, my, this.leftPos + 26, arrowY() - 2, 12, 12)) { this.board++; click(); return true; }
            }            if (this.tab == 2 && data.place() >= 0 && !data.claimed() && over(mx, my, claimX(), claimY() + 6, 100, 16)) {
                send(BountyMenu.CLAIM);
                click();
                return true;
            }
        }        return super.mouseClicked(event, doubleClick);
    }
}

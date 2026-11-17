package com.alexh.capitalism.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

// /insurance guide: Not-A-Scam Insurance's Terms of Service (Insurance.md), a legal document on cream paper: the
// company letterhead, numbered clauses, the fine print at the bottom. Scrolls with the mouse wheel. The numbers come
// from the server (network/OpenInsuranceGuidePacket): each plan's price and rate, the uninsured rate, Blood Moon
// Insurance's price. Keep the clauses matching Insurance.md and the code.
public class InsuranceGuideScreen extends Screen {

    private static final int W = 300;
    private static final int PAPER = 0xFFF4ECD8;
    private static final int PAPER_EDGE = 0xFFC8B890;
    private static final int INK = 0xFF2A2420;
    private static final int INK_DIM = 0xFF6A6058;
    private static final int TEAL = 0xFF1E8C80;
    private static final String[] PLANS = {"Bronze", "Silver", "Gold", "Premium"};

    private final List<Integer> v;
    private final List<Line> lines = new ArrayList<>();
    private int scroll;
    private int contentHeight;

    private record Line(FormattedCharSequence text, int color, int height, boolean small) {
    }

    public InsuranceGuideScreen(List<Integer> values) {
        super(Component.literal("Not-A-Scam Insurance: Terms of Service"));
        this.v = values;
    }

    public static void open(List<Integer> values) {
        Minecraft.getInstance().gui.setScreen(new InsuranceGuideScreen(values));
    }

    private int value(int i) {
        return i < this.v.size() ? this.v.get(i) : 0;
    }

    private int top() {
        return 20;
    }

    private int bottom() {
        return this.height - 34;
    }

    // the paper's text area: below the letterhead
    private int textTop() {
        return this.top() + 50;
    }

    @Override
    protected void init() {
        this.lines.clear();
        int base = value(8), bm = value(9);
        int wrap = W - 30;
        StringBuilder plans = new StringBuilder();
        for (int i = 0; i < 4; i++) plans.append(i == 0 ? "" : "; ").append(PLANS[i]).append(": ").append(value(i * 2)).append(" gold a day, death tax ").append(value(i * 2 + 1)).append("%");
        clause(1, "The Death Tax", "When you die you owe Death " + base + "% of ALL your gold, counted just like the Gold Leaderboard: what you carry "
            + "(your bag too), your Ender Chest, every container you own and your Shopfront earnings (a chest somewhere unloaded at its last count). "
            + "Measured the moment you die, before anything drops, rounded down to whole nuggets. Raw Gold counts too (1 gold each, what it smelts into). Nothing is taken from you "
            + "then: it's a debt. Your things drop as they always do (or stay, with keepInventory), but any gold you drop stays yours: "
            + "only you can pick it up, and like anything dropped it despawns after 5 minutes. Die again while you owe and the new tax is "
            + "added to the old.", wrap);
        clause(2, "Purgatory", "While you owe anything you are In Purgatory. You can't break or mine blocks and you can't hurt anything: "
            + "not with your fists, arrows, tridents, potions, TNT or pets. You can walk, eat, drink, open chests, use doors and beds, trade, use shops "
            + "and sell to the Banker, so go and raise that gold. Monsters can still hurt you. Everyone sees a skull by your name.", wrap);
        clause(3, "Paying Death", "/pay death pays it all. Death collects in this order: your inventory and bag; your Ender Chest and your own "
            + "containers (Shopfront earnings too); your own gold lying on the ground (your death drops, what you threw, until someone else picks "
            + "it up). Raw Gold counts as your gold but never pays: smelt it into ingots in a furnace first (you can in Purgatory). Blocks are broken "
            + "and the change goes in your pocket. Death never touches anyone else's things or the town's chests, and can't reach chests in "
            + "unloaded places: then it takes what it can and tells you what's left. /pay death <gold> pays part. Paid in full, you're free. "
            + "What the bank is owed leaves the world for good: Death doesn't spend. (To pay a player named Death, type /pay Death <gold>.)", wrap);
        clause(4, "Our Plans", plans + ". Uninsured: " + base + "%. /insurance quotes shows today's prices; /insurance buy <plan> signs you up.", wrap);
        clause(5, "When Cover Starts", "A plan starts at the next day start (the next morning), never the day you buy it. Switching plans happens the next "
            + "morning too, and your old plan covers you until then. /insurance cancel ends your plan the next morning; you're covered until then.", wrap);
        clause(6, "Premiums", "Each morning (or when you first log on that day) the day's premium is taken from the gold you carry. Days you aren't on "
            + "aren't charged. Can't pay? Your plan lapses: it ends, and you have no cover that day. No debt: we aren't monsters. Buy again "
            + "and it starts the next morning.", wrap);
        clause(7, "The Blood Moon Exclusion", "During a Blood Moon our staff hide in the cellar. Nobody is insured, not even you, valued customer: "
            + "every death during a Blood Moon pays the full " + base + "%.", wrap);
        clause(8, "Blood Moon Insurance", bm + " gold, once (/insurance buy BloodMoonInsurance). Every death during the next Blood Moon that starts "
            + "after you buy it costs 0%. Bought during a Blood Moon, it covers the next one, not this one. One at a time; it's used up when "
            + "that Blood Moon ends. We announce every Blood Moon we hear of.", wrap);
        clause(9, "The Purge", "On a Purge night (dusk to dawn, announced a day ahead) PvP is on everywhere but the town, which is a safe zone. "
            + "If another player kills you outside the town, your death tax (after your plan's discount) is owed to them, and what you pay goes "
            + "to them, even if they're away. Killed by a monster, or in the town, it's the bank's as usual. Our plans do cover Purge deaths. The killer gets only the tax: the gold you drop stays yours. On any other night, a death at another player's hands is none of Death's business: no tax, no Purgatory.", wrap);
        clause(10, "Your Account", "/insurance status shows your plan, tomorrow's change, your Blood Moon cover, the tax if you died now, and what you owe.", wrap);
        this.lines.add(new Line(FormattedCharSequence.EMPTY, INK, 8, false));
        for (FormattedCharSequence f : this.font.split(Component.literal("Not-A-Scam Insurance is not a scam. Cover void where prohibited by dragons. "
            + "Premiums are non-refundable, non-negotiable and nonsense. Our cellar is not available for public use. Reading these terms may cause "
            + "confidence. By dying you agree to all of the above."), (int) ((W - 30) / 0.75F))) {
            this.lines.add(new Line(f, INK_DIM, 8, true));
        }
        this.contentHeight = 0;
        for (Line line : this.lines) this.contentHeight += line.height;
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        this.addRenderableWidget(Button.builder(Component.literal("I agree (I have no choice)"), b -> this.onClose())
            .bounds(this.width / 2 - 75, this.height - 26, 150, 18).build());
    }

    private void clause(int n, String head, String body, int wrap) {
        MutableComponent title = Component.literal(n + ". " + head.toUpperCase()).withStyle(ChatFormatting.BOLD);
        for (FormattedCharSequence f : this.font.split(title, wrap)) this.lines.add(new Line(f, INK, 11, false));
        for (FormattedCharSequence f : this.font.split(Component.literal(body), wrap)) this.lines.add(new Line(f, INK, 10, false));
        this.lines.add(new Line(FormattedCharSequence.EMPTY, INK, 6, false));
    }

    // (dev: client/PhotoShoot) scroll to the end
    public void scrollToEnd() {
        this.scroll = this.maxScroll();
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (this.bottom() - this.textTop() - 6));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Mth.clamp(this.scroll - (int) (scrollY * 20), 0, this.maxScroll());
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int l = (this.width - W) / 2, t = this.top(), b = this.bottom();
        g.fill(0, 0, this.width, this.height, 0xA0101010);
        g.fill(l - 2, t - 2, l + W + 2, b + 2, PAPER_EDGE);
        g.fill(l, t, l + W, b, PAPER);
        // the letterhead
        Component name = Component.literal("NOT-A-SCAM INSURANCE CO.").withStyle(ChatFormatting.BOLD);
        g.text(this.font, name, l + (W - this.font.width(name)) / 2, t + 7, TEAL, false);
        String motto = "\"Trust us.\"  Est. whenever. Licensed by nobody.";
        g.text(this.font, motto, l + (W - this.font.width(motto)) / 2, t + 19, INK_DIM, false);
        g.fill(l + 10, t + 31, l + W - 10, t + 32, TEAL);
        Component tos = Component.literal("TERMS OF SERVICE").withStyle(ChatFormatting.BOLD);
        g.text(this.font, tos, l + (W - this.font.width(tos)) / 2, t + 36, INK, false);
        // the clauses (only the lines inside the page)
        int y = this.textTop() - this.scroll;
        int clipTop = this.textTop(), clipBottom = b - 4;
        for (Line line : this.lines) {
            if (y >= clipTop && y + line.height <= clipBottom) {
                if (line.small) {
                    g.pose().pushMatrix();
                    g.pose().translate(l + 15, y);
                    g.pose().scale(0.75F, 0.75F);
                    g.text(this.font, line.text, 0, 0, line.color, false);
                    g.pose().popMatrix();
                } else g.text(this.font, line.text, l + 15, y, line.color, false);
            }
            y += line.height;
        }
        // the scroll bar
        int max = this.maxScroll();
        if (max > 0) {
            int track = clipBottom - clipTop;
            int knob = Math.max(16, track * track / (track + max));
            int ky = clipTop + (track - knob) * this.scroll / max;
            g.fill(l + W - 6, clipTop, l + W - 3, clipBottom, 0x30000000);
            g.fill(l + W - 6, ky, l + W - 3, ky + knob, TEAL);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
    }
}

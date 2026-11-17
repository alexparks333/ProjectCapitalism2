package com.alexh.capitalism.client;

import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.fishing.CatchInfo;
import com.alexh.capitalism.fishing.FishMarket;
import com.alexh.capitalism.fishing.RareCatch;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.town.FishermanMenu;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

// Talking to the Fisherman (town/FishermanMenu), drawn in code: a deep-sea navy panel with a teal edge and brass
// rivets, two tabs, your gold by the tabs, your inventory along the bottom.
//  - Sell: a row of nine slots (only rare catches and Soggy Notes go in), each one's price under it; the whole offer
//    in a bar below with the Sell button (the bar shines when he pays). Hover a slot for its exact price.
//  - Mount: a wooden wall plaque (as wide as the catch's plaque) with the catch's slot in the middle; beside it the
//    catch's name, its weight, catcher and day (CatchInfo), how wide the plaque is, its price and the Mount button.
//  - Collection: every rare catch, the ones you've fished up yourself in colour (name in its tier's colour, how many,
//    the heaviest), the rest black silhouettes named "???"; "Caught 7 / 15"; all caught, a Claim button for the prize
//    (fishing/CatchCollection), then "Collection complete ✔".
public class FishermanScreen extends AbstractContainerScreen<FishermanMenu> {

    private static final int W = 250;
    private static final int H = 250;
    private static final String[] TABS = {"Sell", "Mount", "Collection"};
    private static final int TEXT = 0xFFDCEFF0;
    private static final int DIM = 0xFF7FA4AC;
    private static final int TEAL = 0xFF3AA8B0;
    private static final int BRASS = 0xFFD8B060;
    private static final int GOOD = 0xFF90F0A0;
    private static final int BAD = 0xFFFF8078;

    private int tab;
    // (the offer bar shining after a sale: when it was, in ticks of this screen)
    private int soldFlash = -100;
    private int ticks;

    public FishermanScreen(FishermanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventoryLabelY = FishermanMenu.INV_Y - 11;
        this.inventoryLabelX = FishermanMenu.INV_X;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    // (dev: client/PhotoShoot) show this tab
    public void showTab(int i) {
        this.tab = i;
        this.menu.tab = i;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FishermanMenu.TAB + i);
        }
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

    // the Sell button, at the right of the offer bar
    private int sellX() {
        return this.leftPos + W - 60;
    }

    private int sellY() {
        return this.topPos + 104;
    }

    private boolean overSell(double mx, double my) {
        return mx >= sellX() && mx < sellX() + 50 && my >= sellY() && my < sellY() + 18;
    }

    private int mountX() {
        return this.leftPos + 128;
    }

    private int mountY() {
        return this.topPos + 124;
    }

    private boolean overMount(double mx, double my) {
        return mx >= mountX() && mx < mountX() + 92 && my >= mountY() && my < mountY() + 20;
    }

    // "60g", "2.5g", "12k" or "4n": short enough to sit under a slot (the bar and the tooltip give it exactly)
    private static String compact(long nuggets) {
        if (nuggets < 9) return nuggets + "n";
        long gold = nuggets / 9;
        if (gold >= 10000) return (gold / 1000) + "k";
        if (gold < 10 && nuggets % 9 != 0) return String.format(java.util.Locale.ROOT, "%.1fg", nuggets / 9.0);
        return gold + "g";
    }

    // ── drawing ──

    private void slotFrame(GuiGraphicsExtractor g, int x, int y, boolean wanted) {
        g.fill(x - 2, y - 2, x + 18, y + 18, wanted ? TEAL : 0xFF1E4A54);
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF06121A);
        g.fill(x, y, x + 16, y + 16, 0xFF143440);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean ok, boolean over) {
        g.fill(x, y, x + w, y + h, ok ? (over ? 0xFFFFE8A0 : BRASS) : 0xFF34444A);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, ok ? (over ? 0xFF1E6A74 : 0xFF14505A) : 0xFF141E22);
        g.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, ok ? TEXT : 0xFF5A6A70);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int l = this.leftPos;
        int t = this.topPos;
        // a navy panel with a brass edge and a teal inner line
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, 0xFF02080C);
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, BRASS);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, 0xFF1E5A66);
        g.fill(l, t, l + W, t + H, 0xFF0C1C26);
        // a few faint waves rolling along the bottom, under your inventory
        for (int k = 0; k < 3; k++) {
            int wy = t + H - 14 + k * 4;
            for (int x = l + 6; x < l + W - 6; x++) {
                int dy = (int) Math.round(Math.sin((x + k * 9 + this.ticks * 0.3) * 0.18) * 1.2);
                g.fill(x, wy + dy, x + 1, wy + dy + 1, 0x1A6AC8D8);
            }
        }
        // brass rivets in the corners
        for (int[] c : new int[][]{{l + 3, t + 3}, {l + W - 6, t + 3}, {l + 3, t + H - 6}, {l + W - 6, t + H - 6}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, BRASS);
            g.fill(c[0], c[1], c[0] + 1, c[1] + 1, 0xFFFFF0C0);
        }
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i);
            boolean active = i == this.tab;
            boolean hover = mouseX >= x && mouseX < x + tabW(i) && mouseY >= t + 18 && mouseY < t + 32;
            g.fill(x, t + 18, x + tabW(i), t + 32, active ? BRASS : hover ? 0xFF2E7A86 : 0xFF1A3E48);
            g.fill(x + 1, t + 19, x + tabW(i) - 1, t + 31, active ? 0xFF14404A : 0xFF0A1C24);
            g.text(this.font, TABS[i], x + 6, t + 21, active ? TEXT : DIM, false);
        }
        g.fill(l + 6, t + 32, l + W - 6, t + 33, BRASS);
        // your gold, up by the tabs
        String have = GoldMoney.format(GoldMoney.carried(this.minecraft.player));
        int hx = l + W - 10 - this.font.width(have);
        g.item(new ItemStack(Items.GOLD_INGOT), hx - 17, t + 16);
        g.text(this.font, have, hx, t + 21, TEXT, false);
        // the inventory's tray
        g.fill(l + FishermanMenu.INV_X - 2, t + FishermanMenu.INV_Y - 2, l + FishermanMenu.INV_X + 164, t + FishermanMenu.INV_Y + 76, 0xFF12303A);
        for (Slot slot : this.menu.slots) {
            if (slot.container instanceof Inventory) {
                g.fill(l + slot.x - 1, t + slot.y - 1, l + slot.x + 17, t + slot.y + 17, 0xFF06121A);
                g.fill(l + slot.x, t + slot.y, l + slot.x + 16, t + slot.y + 16, 0xFF1C4450);
            }
        }
        if (this.tab == 0) this.drawSell(g, mouseX, mouseY);
        else if (this.tab == 1) this.drawMount(g, mouseX, mouseY);
        else this.drawCollection(g, mouseX, mouseY);
    }

    // ── Sell ──

    private void drawSell(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        List<FormattedCharSequence> help = this.font.split(Component.literal(
            "Rare catches and Soggy Notes, I'll pay good gold for. They only bite on a rod with Luck of the Sea. Cod and the like, take to the Cook.").withStyle(ChatFormatting.ITALIC), W - 20);
        int y = t + 38;
        for (FormattedCharSequence line : help) {
            g.text(this.font, line, l + 10, y, 0xFF9CC8D0, false);
            y += 10;
        }
        for (int i = 0; i < FishermanMenu.SELL_SLOTS; i++) {
            int x = l + FishermanMenu.SELL_X + i * FishermanMenu.SELL_DX, sy = t + FishermanMenu.SELL_Y;
            ItemStack stack = this.menu.slots.get(i).getItem();
            this.slotFrame(g, x, sy, !stack.isEmpty());
            if (stack.isEmpty()) continue;
            // its price under it, small
            String price = compact(FishMarket.valueOf(stack) * stack.getCount());
            g.pose().pushMatrix();
            g.pose().translate(x + 8, sy + 21);
            g.pose().scale(0.75F, 0.75F);
            g.centeredText(this.font, price, 0, 0, BRASS);
            g.pose().popMatrix();
        }
        // the offer bar: the whole offer and the Sell button; shining for a moment after a sale
        long offer = this.menu.offer();
        int by = t + 100;
        float flash = Math.max(0.0F, 1.0F - (this.ticks - this.soldFlash) / 25.0F);
        g.fill(l + 6, by, l + W - 6, by + 26, 0xFF06121A);
        g.fill(l + 7, by + 1, l + W - 7, by + 25, 0xFF10303A);
        if (flash > 0) g.fill(l + 7, by + 1, l + W - 7, by + 25, ((int) (flash * 140) << 24) | 0xFFE080);
        g.item(new ItemStack(Items.GOLD_INGOT), l + 11, by + 5);
        g.text(this.font, "Offer:", l + 30, by + 4, DIM, false);
        g.text(this.font, offer > 0 ? GoldMoney.format(offer) : "nothing yet", l + 30, by + 14, offer > 0 ? GOOD : DIM, false);
        this.button(g, sellX(), sellY(), 50, 18, "Sell", offer > 0, this.overSell(mouseX, mouseY));
        // the hovered slot's exact price
        Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.index < FishermanMenu.SELL_SLOTS && !(hovered.container instanceof Inventory) && hovered.hasItem()) {
            ItemStack stack = hovered.getItem();
            String line = stack.getHoverName().getString() + ": " + GoldMoney.format(FishMarket.valueOf(stack) * stack.getCount());
            g.text(this.font, this.font.plainSubstrByWidth(line, W - 20), l + 10, t + 130, TEXT, false);
        } else if (hovered != null && hovered.container instanceof Inventory && hovered.hasItem()) {
            ItemStack stack = hovered.getItem();
            boolean buys = FishMarket.buys(stack);
            String line = buys ? "He'd pay " + GoldMoney.format(FishMarket.valueOf(stack) * stack.getCount()) + " for that." : "He doesn't buy that.";
            g.text(this.font, line, l + 10, t + 130, buys ? GOOD : DIM, false);
        }
    }

    // ── Mount ──

    private void drawMount(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        ItemStack fish = this.menu.toMount();
        RareCatch kind = FishMarket.catchOf(fish);
        int blocks = kind != null && kind.plaque > 0 ? kind.plaque : 1;
        // the wooden plaque, as wide as the catch's, with the slot in its middle
        int cx = l + FishermanMenu.MOUNT_X + 8, cy = t + FishermanMenu.MOUNT_Y + 8;
        int half = 14 + 12 * blocks;
        g.fill(cx - half - 1, cy - 25, cx + half + 1, cy + 25, 0xFF1E0E06);
        g.fill(cx - half, cy - 24, cx + half, cy + 24, 0xFF6A4224);
        for (int k = 0; k < 6; k++) g.fill(cx - half + 2, cy - 20 + k * 8, cx + half - 2, cy - 19 + k * 8, 0xFF5A361C);
        g.fill(cx - half + 3, cy - 21, cx + half - 3, cy + 21, 0x30000000);
        for (int[] s : new int[][]{{cx - half + 3, cy - 21}, {cx + half - 5, cy - 21}, {cx - half + 3, cy + 19}, {cx + half - 5, cy + 19}}) {
            g.fill(s[0], s[1], s[0] + 2, s[1] + 2, BRASS);
        }
        // a little brass nameplate under the slot
        g.fill(cx - 16, cy + 13, cx + 16, cy + 19, 0xFFB08A40);
        g.fill(cx - 15, cy + 14, cx + 15, cy + 18, BRASS);
        int sx = l + FishermanMenu.MOUNT_X, sy = t + FishermanMenu.MOUNT_Y;
        g.fill(sx - 2, sy - 2, sx + 18, sy + 18, fish.isEmpty() ? (int) (0x80 + 0x60 * Math.sin(this.ticks * 0.15)) << 24 | 0x3AA8B0 : TEAL);
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF06121A);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF143440);

        int rx = mountX();
        int width = W - (rx - l) - 8;
        if (fish.isEmpty()) {
            List<FormattedCharSequence> help = this.font.split(Component.literal(
                "Lay a big catch there, and I'll mount her on a plaque for yer wall.").withStyle(ChatFormatting.ITALIC), width);
            int y = t + 42;
            for (FormattedCharSequence line : help) {
                g.text(this.font, line, rx, y, 0xFF9CC8D0, false);
                y += 10;
            }
            this.button(g, mountX(), mountY(), 92, 20, "Mount", false, false);
            return;
        }
        // the catch: its name, its story, the plaque's width, the price
        int y = t + 40;
        g.text(this.font, this.font.plainSubstrByWidth(fish.getHoverName().getString(), width), rx, y, 0xFF70E0E8, false);
        y += 12;
        CatchInfo info = fish.get(ModComponents.CATCH_INFO.get());
        if (info != null) {
            for (FormattedCharSequence line : this.font.split(Component.literal(info.line()), width)) {
                g.text(this.font, line, rx, y, DIM, false);
                y += 10;
            }
        }
        g.text(this.font, "Plaque: " + blocks + (blocks == 1 ? " block wide" : " blocks wide"), rx, t + 86, TEXT, false);
        long price = FishMarket.plaquePrice(fish, this.minecraft.player);
        long carried = GoldMoney.carried(this.minecraft.player);
        boolean free = this.minecraft.player.isCreative();
        boolean can = free || carried >= price;
        g.item(new ItemStack(Items.GOLD_INGOT), rx - 2, t + 96);
        g.text(this.font, GoldMoney.format(price), rx + 16, t + 100, can ? GOOD : BAD, false);
        if (!can) g.text(this.font, "You're short.", rx + 16, t + 110, BAD, false);
        this.button(g, mountX(), mountY(), 92, 20, "Mount", can, this.overMount(mouseX, mouseY));
    }

    // ── Collection ──

    private static final int CELL_W = 47, CELL_H = 32, COLS = 5;

    private int claimX() {
        return this.leftPos + W - 66;
    }

    private int claimY() {
        return this.topPos + 35;
    }

    private boolean overClaim(double mx, double my) {
        return mx >= claimX() && mx < claimX() + 58 && my >= claimY() && my < claimY() + 14;
    }

    private com.alexh.capitalism.fishing.CatchCollection collection() {
        return this.minecraft.player.getData(com.alexh.capitalism.core.ModAttachments.CATCH_COLLECTION.get());
    }

    private void small(GuiGraphicsExtractor g, Component text, int cx, int y, float scale, int colour) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(scale, scale);
        g.centeredText(this.font, text, 0, 0, colour);
        g.pose().popMatrix();
    }

    private void drawCollection(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        var c = this.collection();
        int total = com.alexh.capitalism.fishing.CatchCollection.total();
        g.text(this.font, "Caught " + c.count() + " / " + total, l + 10, t + 38, c.complete() ? GOOD : TEXT, false);
        // progress bar under it
        g.fill(l + 10, t + 48, l + 110, t + 50, 0xFF06121A);
        g.fill(l + 10, t + 48, l + 10 + 100 * c.count() / total, t + 50, c.complete() ? GOOD : TEAL);
        if (c.complete() && !c.claimed()) {
            this.button(g, claimX(), claimY(), 58, 14, "Claim", true, this.overClaim(mouseX, mouseY));
        } else if (c.claimed()) {
            String done = "Collection complete ✔";
            g.text(this.font, done, l + W - 10 - this.font.width(done), t + 38, GOOD, false);
        } else {
            String hint = "Catch all " + total + "!";
            g.text(this.font, hint, l + W - 10 - this.font.width(hint), t + 38, DIM, false);
        }
        RareCatch[] all = RareCatch.values();
        for (int i = 0; i < all.length; i++) {
            RareCatch kind = all[i];
            int x = l + 7 + (i % COLS) * CELL_W, y = t + 53 + (i / COLS) * CELL_H;
            var entry = c.get(kind);
            boolean caught = entry != null;
            g.fill(x, y, x + CELL_W - 2, y + CELL_H - 2, caught ? 0xFF143440 : 0xFF0A1820);
            if (caught) g.fill(x, y, x + CELL_W - 2, y + 1, 0xFF000000 | kind.tier().color);
            ItemStack icon = new ItemStack(com.alexh.capitalism.core.ModItems.RARE_CATCHES.get(kind).get());
            int ix = x + (CELL_W - 2) / 2 - 8, iy = y + 2;
            if (caught) g.item(icon, ix, iy);
            else Silhouettes.draw(g, icon, ix, iy);
            int cx = x + (CELL_W - 2) / 2;
            if (caught) {
                this.small(g, Component.literal(kind.title), cx, y + 19, 0.5F, 0xFF000000 | kind.tier().color);
                String stats = "x" + entry.count() + (entry.heaviest() > 0 ? " · " + new CatchInfo(entry.heaviest(), "", "").weightText() : "");
                this.small(g, Component.literal(stats), cx, y + 24, 0.5F, DIM);
            } else {
                this.small(g, Component.literal("???"), cx, y + 20, 0.6F, 0xFF4A6A72);
            }
            // hovering it: what it is (or "???"), its tier, where and when it bites, its odds, what you need
            if (mouseX >= x && mouseX < x + CELL_W - 2 && mouseY >= y && mouseY < y + CELL_H - 2) {
                List<Component> tip = new java.util.ArrayList<>();
                tip.add(caught ? kind.tier().paint(Component.literal(kind.title)) : Component.literal("???").withStyle(ChatFormatting.GRAY));
                tip.add(kind.tier().line());
                tip.add(Component.literal(kind.hint()).withStyle(ChatFormatting.AQUA));
                for (String line : kind.chanceLines()) tip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
                tip.add(Component.literal("Needs a rod with Luck of the Sea").withStyle(ChatFormatting.DARK_AQUA));
                if (caught) {
                    tip.add(Component.literal("Caught " + entry.count() + (entry.heaviest() > 0 ? " · heaviest " + new CatchInfo(entry.heaviest(), "", "").weightText() : ""))
                        .withStyle(ChatFormatting.GRAY));
                }
                g.setTooltipForNextFrame(this.font, tip, java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }
    // the sell and mount slots are framed by drawSell / drawMount
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.ITALIC), W / 2, 6, 0xFF000000 | com.alexh.capitalism.town.Fisherman.COLOR);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            for (int i = 0; i < TABS.length; i++) {
                if (event.x() >= tabX(i) && event.x() < tabX(i) + tabW(i) && event.y() >= this.topPos + 18 && event.y() < this.topPos + 32) {
                    if (this.tab != i) {
                        this.showTab(i);
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    }
                    return true;
                }
            }
            if (this.tab == 0 && this.overSell(event.x(), event.y())) {
                if (this.menu.offer() > 0) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FishermanMenu.SELL);
                    this.soldFlash = this.ticks;
                }
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (this.tab == 2 && this.overClaim(event.x(), event.y())) {
                var c = this.collection();
                if (c.complete() && !c.claimed()) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FishermanMenu.CLAIM);
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
            if (this.tab == 1 && this.overMount(event.x(), event.y())) {
                if (!this.menu.toMount().isEmpty()) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FishermanMenu.MOUNT);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

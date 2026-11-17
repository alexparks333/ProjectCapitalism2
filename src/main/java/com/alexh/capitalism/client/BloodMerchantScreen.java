package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.town.BloodMerchantMenu;
import com.alexh.capitalism.town.BloodMerchantShop;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Talking to the Blood Merchant (town/BloodMerchantMenu), drawn in code: a black-and-crimson panel with gold edging,
// two tabs, your inventory along the bottom.
//  - Curios: his wares on velvet cards, two rows of six, each drawn big with its price under it (Blood Shards; the
//    bosses' trophies also show their crest piece; the Essential Wishing Star shows the Blood Moon Crest, crossed out
//    once you've had yours). Hover for what it is and its whole price, click to buy.
//  - Mend the Crest: the Blood Moon Crest drawn big and dim, broken in three; each piece you put in its socket shows in
//    its place, so the crest comes together as you fill it; "Mend" (50 Blood Shards) joins them, and the crest flares.
public class BloodMerchantScreen extends AbstractContainerScreen<BloodMerchantMenu> {

    private static final int W = 250;
    private static final int H = 250;
    private static final String[] TABS = {"Curios", "Mend the Crest"};
    private static final int TEXT = 0xFFF0D0D0;
    private static final int DIM = 0xFFA07080;
    private static final int GOLD = 0xFFD8A848;
    private static final int CARD_W = 36;
    private static final int CARD_H = 46;
    private static final int CARD_GAP = 4;
    // the big broken crest on Mend the Crest (96 x 96: the 32 x 32 textures three times over)
    public static final int CREST_X = 19;
    public static final int CREST_Y = 39;
    private static final Identifier WHOLE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/item/blood_moon_crest.png");
    private static final Identifier[] PIECES = {
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/item/revenant_crest.png"),
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/item/sanguine_crest.png"),
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/item/gorefang_crest.png")};
    // each piece's picture is moved to its middle (tools/make_blood_crest.ps1): this many texture pixels, put back here
    private static final int[][] PIECE_SHIFT = {{5, 3}, {-5, 3}, {0, -6}};

    private int tab;
    // Curios: which page of PER_PAGE cards
    private int page;
    // Curios: the card picked (buy it with the Buy button), or -1
    private int selected = -1;
    private static final int PER_PAGE = 12;
    // (the crest flaring after a mend: when it was, in ticks of this screen)
    private int mendFlash = -100;
    private int ticks;

    public BloodMerchantScreen(BloodMerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventoryLabelY = BloodMerchantMenu.INV_Y - 11;
        this.inventoryLabelX = BloodMerchantMenu.INV_X;
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
        int total = 6 * CARD_W + 5 * CARD_GAP;
        return this.leftPos + (W - total) / 2 + (i % PER_PAGE % 6) * (CARD_W + CARD_GAP);
    }

    private int cardY(int i) {
        return this.topPos + 40 + (i % PER_PAGE / 6) * (CARD_H + 6);
    }

    private int cardAt(double mx, double my) {
        for (int i = this.page * PER_PAGE; i < Math.min(BloodMerchantShop.OFFERS.size(), (this.page + 1) * PER_PAGE); i++) {
            if (mx >= cardX(i) && mx < cardX(i) + CARD_W && my >= cardY(i) && my < cardY(i) + CARD_H) return i;
        }
        return -1;
    }

    private int mendX() {
        return this.leftPos + 140;
    }

    private int mendY() {
        return this.topPos + 112;
    }

    private boolean overMend(double mx, double my) {
        return mx >= mendX() && mx < mendX() + 92 && my >= mendY() && my < mendY() + 20;
    }

    // ── drawing ──

    private void smallItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.5F, 0.5F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0F, 2.0F);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int l = this.leftPos;
        int t = this.topPos;
        // a black panel with a gold edge and a crimson inner line
        g.fill(l - 3, t - 3, l + W + 3, t + H + 3, 0xFF050204);
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, GOLD);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, 0xFF3A0810);
        g.fill(l, t, l + W, t + H, 0xFF140A0E);
        // gold corner flourishes
        for (int[] c : new int[][]{{l + 2, t + 2}, {l + W - 8, t + 2}, {l + 2, t + H - 8}, {l + W - 8, t + H - 8}}) {
            g.fill(c[0], c[1], c[0] + 6, c[1] + 1, GOLD);
            g.fill(c[0], c[1], c[0] + 1, c[1] + 6, GOLD);
            g.fill(c[0] + 5, c[1], c[0] + 6, c[1] + 6, GOLD);
            g.fill(c[0], c[1] + 5, c[0] + 6, c[1] + 6, GOLD);
        }
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i);
            boolean active = i == this.tab;
            boolean hover = mouseX >= x && mouseX < x + tabW(i) && mouseY >= t + 18 && mouseY < t + 32;
            g.fill(x, t + 18, x + tabW(i), t + 32, active ? GOLD : hover ? 0xFF8A3040 : 0xFF4A1820);
            g.fill(x + 1, t + 19, x + tabW(i) - 1, t + 31, active ? 0xFF3A0C16 : 0xFF1C0C12);
            g.text(this.font, TABS[i], x + 6, t + 21, active ? TEXT : DIM, false);
        }
        g.fill(l + 6, t + 32, l + W - 6, t + 33, GOLD);
        // the inventory's tray
        g.fill(l + BloodMerchantMenu.INV_X - 2, t + BloodMerchantMenu.INV_Y - 2, l + BloodMerchantMenu.INV_X + 164, t + BloodMerchantMenu.INV_Y + 76, 0xFF2A1218);
        for (Slot slot : this.menu.slots) {
            if (slot.index >= 0 && slot.container instanceof Inventory) {
                g.fill(l + slot.x - 1, t + slot.y - 1, l + slot.x + 17, t + slot.y + 17, 0xFF0A0406);
                g.fill(l + slot.x, t + slot.y, l + slot.x + 16, t + slot.y + 16, 0xFF3E1A22);
            }
        }
        if (this.tab == 0) this.drawCurios(g, mouseX, mouseY);
        else this.drawMend(g, mouseX, mouseY);
    }

    // ── Curios ──

    private void drawCurios(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int shards = BloodMerchantShop.shards(this.minecraft.player);
        int hovered = this.cardAt(mouseX, mouseY);
        List<BloodMerchantShop.Offer> offers = BloodMerchantShop.OFFERS;
        for (int i = this.page * PER_PAGE; i < Math.min(offers.size(), (this.page + 1) * PER_PAGE); i++) {
            BloodMerchantShop.Offer offer = offers.get(i);
            int x = cardX(i), y = cardY(i);
            boolean there = offer.exists();
            boolean done = offer.star() && this.menu.hadStar();
            // a velvet card, gold-edged when hovered (the star's card trimmed in gold)
            boolean picked = i == this.selected;
            if (picked) g.fill(x - 2, y - 2, x + CARD_W + 2, y + CARD_H + 2, 0xFFFFE090);
            g.fill(x, y, x + CARD_W, y + CARD_H, picked ? 0xFFFFF0C0 : i == hovered && there && !done ? 0xFFF0C060 : offer.star() ? 0xFF8A6A20 : 0xFF0A0406);
            g.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, offer.star() ? 0xFF2E1430 : 0xFF3A0C18);
            g.fill(x + 1, y + 1, x + CARD_W - 1, y + 3, offer.star() ? 0xFF4A2850 : 0xFF581424);
            if (!there) {
                g.centeredText(this.font, "?", x + CARD_W / 2, y + 14, DIM);
                continue;
            }
            this.bigItem(g, new ItemStack(offer.item()), x + 2, y + 2);
            // the price
            int py = y + CARD_H - 11;
            if (offer.star()) {
                this.smallItem(g, new ItemStack(ModItems.BLOOD_MOON_CREST.get()), x + 3, py);
                boolean has = BloodMerchantShop.count(this.minecraft.player, ModItems.BLOOD_MOON_CREST.get()) > 0;
                g.text(this.font, done ? "Had" : "x1", x + 13, py, done ? DIM : has ? 0xFF90F0A0 : 0xFFFF8078, false);
            } else {
                this.smallItem(g, new ItemStack(ModItems.BLOOD_SHARD.get()), x + 2, py);
                g.text(this.font, String.valueOf(offer.shards()), x + 11, py, shards >= offer.shards() ? 0xFF90F0A0 : 0xFFFF8078, false);
                if (offer.piece() != null) this.smallItem(g, new ItemStack(offer.piece().get()), x + CARD_W - 10, py);
            }
            if (done) {
                for (int k = 3; k < CARD_W - 3; k++) {
                    g.fill(x + k, y + 3 + k * 30 / CARD_W, x + k + 1, y + 4 + k * 30 / CARD_W, 0xFFA03040);
                }
            }
        }
        // your shards, up by the tabs
        String have = "x" + shards;
        int hx = this.leftPos + W - 10 - this.font.width(have);
        g.item(new ItemStack(ModItems.BLOOD_SHARD.get()), hx - 17, this.topPos + 16);
        g.text(this.font, have, hx, this.topPos + 21, TEXT, false);
        // the pages: < 1/2 >
        if (this.pages() > 1) {
            for (int side = 0; side < 2; side++) {
                int ax = this.arrowX(side), ay = this.topPos + 19;
                boolean can = side == 0 ? this.page > 0 : this.page < this.pages() - 1;
                boolean over = mouseX >= ax && mouseX < ax + 12 && mouseY >= ay && mouseY < ay + 12;
                g.fill(ax, ay, ax + 12, ay + 12, can ? (over ? 0xFFFFE090 : GOLD) : 0xFF4A3030);
                g.fill(ax + 1, ay + 1, ax + 11, ay + 11, can ? 0xFF3A0C16 : 0xFF1C1014);
                g.centeredText(this.font, side == 0 ? "<" : ">", ax + 6, ay + 2, can ? TEXT : 0xFF6A5058);
            }
            g.centeredText(this.font, (this.page + 1) + "/" + this.pages(), (this.arrowX(0) + this.arrowX(1) + 12) / 2, this.topPos + 21, DIM);
        }
        this.drawBuyBar(g, mouseX, mouseY, shards);
        if (hovered >= 0) this.curioTooltip(g, offers.get(hovered), shards, mouseX, mouseY);
    }

    private boolean isBlock(BloodMerchantShop.Offer o) {
        return o.shards() == 1 && o.piece() == null && !o.star();
    }

    // the Buy buttons under the cards: 0 Buy, 1 Buy 64 (building blocks)
    private int buyX(int which) {
        return this.leftPos + W - 54 - which * 54;
    }

    private int buyY() {
        return this.topPos + 142;
    }

    private boolean overBuy(int which, double mx, double my) {
        return mx >= buyX(which) && mx < buyX(which) + 48 && my >= buyY() && my < buyY() + 16;
    }

    // what you picked, its price, and the Buy button(s)
    private void drawBuyBar(GuiGraphicsExtractor g, int mouseX, int mouseY, int shards) {
        int l = this.leftPos, y = this.buyY();
        g.fill(l + 6, y - 2, l + W - 6, y + 18, 0xFF0A0406);
        g.fill(l + 7, y - 1, l + W - 7, y + 17, 0xFF2A0C14);
        if (this.selected < 0) {
            g.text(this.font, Component.literal("Pick something, then press Buy.").withStyle(ChatFormatting.ITALIC), l + 12, y + 4, DIM, false);
            return;
        }
        BloodMerchantShop.Offer o = BloodMerchantShop.OFFERS.get(this.selected);
        ItemStack stack = new ItemStack(o.item());
        g.item(stack, l + 9, y);
        Item piece = o.piece() == null ? null : o.piece().get();
        boolean can = o.star() ? !this.menu.hadStar() && BloodMerchantShop.count(this.minecraft.player, ModItems.BLOOD_MOON_CREST.get()) > 0
            : shards >= o.shards() && (piece == null || BloodMerchantShop.count(this.minecraft.player, piece) > 0);
        if (this.minecraft.player.isCreative()) can = !o.star() || !this.menu.hadStar();
        String name = stack.getHoverName().getString();
        int room = this.buyX(this.isBlock(o) ? 1 : 0) - (l + 28) - 4;
        g.text(this.font, this.font.plainSubstrByWidth(name, room), l + 28, y + 4, TEXT, false);
        for (int which = 0; which < (this.isBlock(o) ? 2 : 1); which++) {
            int bx = this.buyX(which);
            boolean over = this.overBuy(which, mouseX, mouseY);
            boolean ok = can && (which == 0 || shards >= 64 || this.minecraft.player.isCreative());
            g.fill(bx, y, bx + 48, y + 16, ok ? (over ? 0xFFFFE090 : GOLD) : 0xFF4A3030);
            g.fill(bx + 1, y + 1, bx + 47, y + 15, ok ? (over ? 0xFF6A1424 : 0xFF4A0C18) : 0xFF1C1014);
            g.centeredText(this.font, which == 0 ? "Buy" : "Buy 64", bx + 24, y + 4, ok ? TEXT : 0xFF6A5058);
        }
    }

    private int pages() {
        return (BloodMerchantShop.OFFERS.size() + PER_PAGE - 1) / PER_PAGE;
    }

    // the page arrows, between the tabs and your shards: 0 back, 1 on
    private int arrowX(int side) {
        return this.tabX(TABS.length - 1) + this.tabW(TABS.length - 1) + 10 + side * 40;
    }

    private void curioTooltip(GuiGraphicsExtractor g, BloodMerchantShop.Offer offer, int shards, int mouseX, int mouseY) {
        if (!offer.exists()) return;
        List<Component> lines = new ArrayList<>();
        ItemStack stack = new ItemStack(offer.item());
        lines.add(stack.getHoverName().copy().withStyle(s -> s.withColor(0xFF5060).withBold(true)));
        if (offer.star()) {
            lines.add(Component.literal("One more Essential job slot (wish on the Wishing Well).").withStyle(ChatFormatting.LIGHT_PURPLE));
            if (this.menu.hadStar()) {
                lines.add(Component.literal("You've had yours: one per player.").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                boolean has = BloodMerchantShop.count(this.minecraft.player, ModItems.BLOOD_MOON_CREST.get()) > 0;
                lines.add(Component.literal("Costs a Blood Moon Crest. One per player.").withStyle(has ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        } else {
            boolean block = offer.shards() == 1 && offer.piece() == null;
            lines.add(Component.literal(block ? "A Blood Moon building block." : "A Blood Moon decoration: one of a kind.").withStyle(ChatFormatting.GRAY));
            if (block) lines.add(Component.literal("Buy 64: a stack for 64 Blood Shards.").withStyle(ChatFormatting.GOLD));
            Item piece = offer.piece() == null ? null : offer.piece().get();
            boolean can = shards >= offer.shards() && (piece == null || BloodMerchantShop.count(this.minecraft.player, piece) > 0);
            String price = offer.shards() + " Blood Shards" + (piece == null ? "" : " + " + new ItemStack(piece).getHoverName().getString());
            lines.add(Component.literal("Click to pick, then Buy: " + price).withStyle(can ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        g.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, ItemStack.EMPTY);
    }

    // ── Mend the Crest ──

    private void drawMend(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        int cx = l + CREST_X, cy = t + CREST_Y;
        // a dark velvet ground behind the crest
        g.fill(cx - 4, cy - 4, cx + 100, cy + 100, 0xFF0A0406);
        g.fill(cx - 3, cy - 3, cx + 99, cy + 99, 0xFF24080E);
        float flash = Math.max(0.0F, 1.0F - (this.ticks - this.mendFlash) / 30.0F);
        // the crest's ghost: dim, so you see where each piece goes
        g.blit(RenderPipelines.GUI_TEXTURED, WHOLE, cx, cy, 0.0F, 0.0F, 96, 96, 32, 32, 32, 32, 0x40FFFFFF);
        // the pieces you've put in, each in its place
        for (int i = 0; i < 3; i++) {
            if (!this.menu.piecePlaced(i)) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, PIECES[i], cx - PIECE_SHIFT[i][0] * 3, cy - PIECE_SHIFT[i][1] * 3, 0.0F, 0.0F, 96, 96, 32, 32, 32, 32);
        }
        if (flash > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, WHOLE, cx, cy, 0.0F, 0.0F, 96, 96, 32, 32, 32, 32, ((int) (flash * 255) << 24) | 0xFFFFFF);
            g.fill(cx - 3, cy - 3, cx + 99, cy + 99, ((int) (flash * 90) << 24) | 0xFF3040);
        }
        // the sockets: a glowing ring where each piece goes, pulsing while it's empty
        for (int i = 0; i < 3; i++) {
            int sx = l + BloodMerchantMenu.PIECE_SLOTS[i][0], sy = t + BloodMerchantMenu.PIECE_SLOTS[i][1];
            if (this.menu.piecePlaced(i)) continue;
            int pulse = (int) (120 + 100 * Math.sin((this.ticks + i * 7) * 0.2));
            int c = (pulse << 24) | 0xE04050;
            g.fill(sx - 2, sy - 2, sx + 18, sy - 1, c);
            g.fill(sx - 2, sy + 17, sx + 18, sy + 18, c);
            g.fill(sx - 2, sy - 1, sx - 1, sy + 17, c);
            g.fill(sx + 17, sy - 1, sx + 18, sy + 17, c);
            g.fill(sx, sy, sx + 16, sy + 16, 0x90100408);
            // the piece it wants, faint
            g.pose().pushMatrix();
            g.item(new ItemStack(BloodMerchantMenu.piece(i).get()), sx, sy);
            g.pose().popMatrix();
            g.fill(sx, sy, sx + 16, sy + 16, 0xA0140810);
        }
        // the right side: what to do, the price, the button
        int rx = l + 128;
        List<FormattedCharSequence> help = this.font.split(Component.literal("Fit each piece of the crest into its place, and I will make it whole.").withStyle(ChatFormatting.ITALIC), W - 136);
        int y = t + 42;
        for (FormattedCharSequence line : help) {
            g.text(this.font, line, rx, y, 0xFFD0A0A8, false);
            y += 10;
        }
        int shards = BloodMerchantShop.shards(this.minecraft.player);
        boolean free = this.minecraft.player.isCreative();
        g.item(new ItemStack(ModItems.BLOOD_SHARD.get()), rx, t + 88);
        g.text(this.font, BloodMerchantShop.MEND_SHARDS + " Blood Shards", rx + 18, t + 92, shards >= BloodMerchantShop.MEND_SHARDS || free ? 0xFF90F0A0 : 0xFFFF8078, false);
        boolean ready = this.menu.allPlaced() && (shards >= BloodMerchantShop.MEND_SHARDS || free);
        boolean hover = this.overMend(mouseX, mouseY);
        g.fill(mendX(), mendY(), mendX() + 92, mendY() + 20, ready ? (hover ? 0xFFFFE090 : GOLD) : 0xFF4A3030);
        g.fill(mendX() + 1, mendY() + 1, mendX() + 91, mendY() + 19, ready ? (hover ? 0xFF6A1424 : 0xFF4A0C18) : 0xFF1C1014);
        g.centeredText(this.font, "Mend the Crest", mendX() + 46, mendY() + 6, ready ? TEXT : 0xFF6A5058);
        g.text(this.font, "You have " + shards, rx + 18, t + 102, DIM, false);
    }

    // the piece sockets are drawn by drawMend (the piece shows in its place in the crest), not as plain slots
    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        if (slot.index < 3 && !(slot.container instanceof Inventory)) return;
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.ITALIC), W / 2, 6, 0xFFE04050);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            for (int i = 0; i < TABS.length; i++) {
                if (event.x() >= tabX(i) && event.x() < tabX(i) + tabW(i) && event.y() >= this.topPos + 18 && event.y() < this.topPos + 32) {
                    if (this.tab != i) {
                        this.tab = i;
                        this.menu.tab = i;
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    }
                    return true;
                }
            }
            if (this.tab == 0) {
                for (int side = 0; side < 2 && this.pages() > 1; side++) {
                    int ax = this.arrowX(side), ay = this.topPos + 19;
                    if (event.x() >= ax && event.x() < ax + 12 && event.y() >= ay && event.y() < ay + 12) {
                        int to = Math.max(0, Math.min(this.pages() - 1, this.page + (side == 0 ? -1 : 1)));
                        if (to != this.page) {
                            this.page = to;
                            this.selected = -1;
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                        }
                        return true;
                    }
                }
                // the Buy buttons for what's picked
                if (this.selected >= 0) {
                    for (int which = 0; which < 2; which++) {
                        if (which == 1 && !this.isBlock(BloodMerchantShop.OFFERS.get(this.selected))) continue;
                        if (this.overBuy(which, event.x(), event.y())) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, which == 1 ? BloodMerchantMenu.BULK + this.selected : this.selected);
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            return true;
                        }
                    }
                }
                // a card: pick it (buy with the button)
                int card = this.cardAt(event.x(), event.y());
                if (card >= 0 && BloodMerchantShop.OFFERS.get(card).exists()) {
                    this.selected = card;
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2F));
                    return true;
                }
            } else if (this.overMend(event.x(), event.y())) {
                if (this.menu.allPlaced()) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, BloodMerchantMenu.MEND);
                    if (BloodMerchantShop.shards(this.minecraft.player) >= BloodMerchantShop.MEND_SHARDS || this.minecraft.player.isCreative()) this.mendFlash = this.ticks;
                }
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}

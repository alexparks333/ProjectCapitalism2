package com.alexh.capitalism.client;

import com.alexh.capitalism.mail.MailboxMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// The Mailbox (mail/MailboxMenu), drawn as an airmail envelope: cream paper inside a border of red and blue diagonal
// stripes, a stamp in the top right corner with a wavy postmark over it. The owner sees their mail in three rows like a
// chest ("3 letters"); anyone else sees the mail slot: three slots under "Drop mail in the slot", with how much room is
// left (or that it's full).
public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {

    private static final int PAPER = 0xFFF2E8D0;
    private static final int PAPER_DARK = 0xFFE2D4B4;
    private static final int RED = 0xFFC8343C;
    private static final int BLUE = 0xFF2E4E9E;
    private static final int INK = 0xFF3A3040;
    private static final int INK_SOFT = 0xFF7A6A70;
    private static final int STAMP = 0xFF4E8A5A;

    public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, MailboxMenu.HEIGHT);
        this.inventoryLabelY = MailboxMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        // the airmail border: diagonal red and blue stripes
        for (int y = 0; y < h; y++) {
            if (y < 4 || y >= h - 4) stripes(graphics, x0, y0, y, 0, w);
            else {
                stripes(graphics, x0, y0, y, 0, 4);
                stripes(graphics, x0, y0, y, w - 4, w);
            }
        }
        graphics.fill(x0 + 4, y0 + 4, x0 + w - 4, y0 + h - 4, PAPER);
        graphics.fill(x0 + 4, y0 + 4, x0 + w - 4, y0 + 5, PAPER_DARK);
        // the stamp, top right, and the postmark's wavy lines across it
        int sx = x0 + w - 26;
        int sy = y0 + 6;
        graphics.fill(sx, sy, sx + 16, sy + 12, 0xFFFFFFFF);
        graphics.fill(sx + 1, sy + 1, sx + 15, sy + 11, STAMP);
        graphics.fill(sx + 5, sy + 4, sx + 11, sy + 8, 0xFFF0E0A0);
        graphics.fill(sx + 5, sy + 4, sx + 11, sy + 5, 0xFFC8A040);
        for (int x = sx - 10; x < sx + 12; x++) {
            int y = sy + 5 + (int) Math.round(Math.sin(x * 0.6) * 1.5);
            graphics.fill(x, y, x + 1, y + 1, 0x904A4050);
            graphics.fill(x, y + 3, x + 1, y + 4, 0x904A4050);
        }
        int slotEdge = 0xFF6E5A40;
        if (this.menu.isOwner()) {
            for (int i = 0; i < 27; i++) {
                DesignerDraw.slot(graphics, x0 + 7 + (i % 9) * 18, y0 + MailboxMenu.MAIL_Y - 1 + (i / 9) * 18, 18, 0xFF9C8462, slotEdge, 0xFFB8A07C);
            }
        } else {
            // the mail slot: a dark opening with the three slots in it
            int mx = x0 + 61 - 4;
            int my = y0 + MailboxMenu.SLOT_Y - 1 - 3;
            graphics.fill(mx, my, mx + MailboxMenu.SEND_SLOTS * 18 + 8, my + 24, INK);
            for (int i = 0; i < MailboxMenu.SEND_SLOTS; i++) {
                DesignerDraw.slot(graphics, x0 + 61 + i * 18, y0 + MailboxMenu.SLOT_Y - 1, 18, 0xFF2A2430, 0xFF5A5060, 0xFF4A4250);
            }
        }
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? MailboxMenu.INV_Y - 1 + (i / 9) * 18 : MailboxMenu.INV_Y + 57);
            DesignerDraw.slot(graphics, x, y, 18, 0xFF9C8462, slotEdge, 0xFFB8A07C);
        }
    }

    // one row of the striped border from x to x2: diagonal bands 6 wide, red and blue, drawn as runs
    private static void stripes(GuiGraphicsExtractor graphics, int x0, int y0, int y, int x, int x2) {
        while (x < x2) {
            int phase = Math.floorMod(x - y, 12);
            int run = Math.min(x2 - x, 6 - phase % 6);
            graphics.fill(x0 + x, y0 + y, x0 + x + run, y0 + y + 1, phase < 6 ? RED : BLUE);
            x += run;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, INK, false);
        if (!this.menu.isOwner()) {
            int free = this.menu.freeSlots();
            String room = free <= 0 ? "This mailbox is full." : free + " slot" + (free == 1 ? "" : "s") + " of room left";
            graphics.centeredText(this.font, Component.literal(room), this.imageWidth / 2, 21, free <= 0 ? RED : INK_SOFT);
            // the Drop Mail In button: red like an airmail stripe, grey while the slot is empty
            boolean ready = this.hasMail();
            boolean hover = ready && this.overButton(xm, ym);
            int bx = BUTTON_X, by = BUTTON_Y;
            graphics.fill(bx, by, bx + BUTTON_W, by + BUTTON_H, INK);
            graphics.fill(bx + 1, by + 1, bx + BUTTON_W - 1, by + BUTTON_H - 1, !ready ? 0xFF9A9090 : hover ? 0xFFE04850 : RED);
            graphics.fill(bx + 1, by + 1, bx + BUTTON_W - 1, by + 2, !ready ? 0xFFB0A8A8 : 0xFFF07880);
            graphics.centeredText(this.font, Component.literal("✉ Drop Mail In"), bx + BUTTON_W / 2, by + 3, !ready ? 0xFFDDD6D6 : 0xFFFFFFFF);
        }
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, INK, false);
    }

    private static final int BUTTON_W = 90;
    private static final int BUTTON_H = 14;
    private static final int BUTTON_X = (176 - BUTTON_W) / 2;
    private static final int BUTTON_Y = MailboxMenu.SLOT_Y + 22;

    private boolean hasMail() {
        for (int i = 0; i < MailboxMenu.SEND_SLOTS; i++) if (!this.menu.getSlot(i).getItem().isEmpty()) return true;
        return false;
    }

    private boolean overButton(double x, double y) {
        double bx = this.leftPos + BUTTON_X, by = this.topPos + BUTTON_Y;
        return x >= bx && x < bx + BUTTON_W && y >= by && y < by + BUTTON_H;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (!this.menu.isOwner() && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                && this.minecraft.gameMode != null && this.overButton(event.x(), event.y())) {
            if (this.hasMail()) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MailboxMenu.SEND);
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}

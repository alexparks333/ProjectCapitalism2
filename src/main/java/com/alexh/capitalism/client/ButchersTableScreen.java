package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.farmer.ButcherKnifeItem;
import com.alexh.capitalism.farmer.ButchersTableMenu;
import com.alexh.capitalism.farmer.Freshness;
import com.alexh.capitalism.farmer.MeatCut;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// The Butcher's Table (farmer/ButchersTableMenu), drawn here: a dark wood frame round an end-grain chopping board with
// the slab's slot on it, and under it what the slab gives: the cut if it's cut well, plain meat if it's botched.
// "Start Cutting" takes the slab and starts the butchery game (ButcheryGame), drawn over everything until it ends.
public class ButchersTableScreen extends AbstractContainerScreen<ButchersTableMenu> {

    private static final int FRAME = 0xFF2A1A0E;
    private static final int FRAME_LIGHT = 0xFF5A3A20;
    private static final int PANEL = 0xFF3E2814;
    private static final int TITLE = 0xFFF6E2B0;
    private static final int LABEL = 0xFFD8C69A;
    private static final int SLOT_EDGE = 0xFF1A0E06;
    private static final int SLOT_LIP = 0xFF6A4A2A;
    private static final int SLOT_INSIDE = 0xFF2A1A0E;

    private @Nullable Button cutButton;
    private @Nullable ButcheryGame game;

    public ButchersTableScreen(ButchersTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        this.cutButton = this.addRenderableWidget(Button.builder(Component.literal("Cut Slab"), b -> this.start())
            .bounds(this.leftPos + 104, this.topPos + 44, 64, 16).build());
        this.cutButton.setTooltip(Tooltip.create(Component.literal("Trim the fat, scan for bones, then make two cuts. Botch it and you get plain meat. Uses one cut of the knife's sharpness.")));
        this.updateButton();
    }

    private void start() {
        MeatCut cut = MeatCut.ofSlab(this.menu.slab());
        if (cut == null || this.game != null || this.minecraft.gameMode == null || ButcherKnifeItem.cutsLeft(this.menu.knife()) <= 0) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ButchersTableMenu.CUT);
        this.game = new ButcheryGame(cut, Freshness.yield(cut, this.menu.slab()), Freshness.lean(this.menu.slab()));
    }

    private void updateButton() {
        if (this.cutButton == null) return;
        this.cutButton.visible = this.game == null;
        this.cutButton.active = MeatCut.ofSlab(this.menu.slab()) != null && ButcherKnifeItem.cutsLeft(this.menu.knife()) > 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.game != null && this.game.isOver()) {
            if (this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                    this.game.succeeded() ? ButchersTableMenu.SUCCESS : ButchersTableMenu.FAILED);
            }
            this.game = null;
        }
        this.updateButton();
    }

    // ── drawing ──

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        // the frame
        graphics.fill(x0, y0, x0 + w, y0 + h, FRAME);
        graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, FRAME_LIGHT);
        graphics.fill(x0 + 3, y0 + 3, x0 + w - 3, y0 + h - 3, PANEL);
        for (int y = y0 + 6; y < y0 + h - 4; y += 5) graphics.fill(x0 + 3, y, x0 + w - 3, y + 1, 0x18000000);
        // the chopping board
        int bx0 = x0 + 8;
        int by0 = y0 + 18;
        int bx1 = x0 + 98;
        int by1 = y0 + 70;
        graphics.fill(bx0 - 1, by0 - 1, bx1 + 1, by1 + 1, 0xFF1A0E06);
        for (int ty = by0, row = 0; ty < by1; ty += 9, row++) {
            for (int tx = bx0, col = 0; tx < bx1; tx += 9, col++) {
                graphics.fill(tx, ty, Math.min(bx1, tx + 9), Math.min(by1, ty + 9), (row + col) % 2 == 0 ? 0xFFA67646 : 0xFF98683C);
                if (tx + 6 < bx1 && ty + 6 < by1) graphics.fill(tx + 3, ty + 3, tx + 6, ty + 6, (row + col) % 2 == 0 ? 0xFF946234 : 0xFF8A5A30);
            }
        }
        // the knife's slot, in the board's corner (the knife is what lets you cut)
        int kx = x0 + ButchersTableMenu.KNIFE_X;
        int ky = y0 + ButchersTableMenu.KNIFE_Y;
        DesignerDraw.slot(graphics, kx - 1, ky - 1, 18, SLOT_EDGE, 0xFF9AA4B0, SLOT_INSIDE);
        if (this.menu.knife().isEmpty()) DesignerDraw.ghost(graphics, new ItemStack(ModItems.IRON_BUTCHER_KNIFE.get()), kx, ky, SLOT_INSIDE);
        // the slab's slot, on the board
        int sx = x0 + ButchersTableMenu.SLAB_X;
        int sy = y0 + ButchersTableMenu.SLAB_Y;
        DesignerDraw.slot(graphics, sx - 5, sy - 5, 26, SLOT_EDGE, 0xFFE2BC5C, SLOT_INSIDE);
        if (this.menu.slab().isEmpty()) DesignerDraw.ghost(graphics, new ItemStack(MeatCut.BEEF_RIB.slab()), sx, sy, SLOT_INSIDE);
        // the inventory's slots
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? 83 + (i / 9) * 18 : 141);
            DesignerDraw.slot(graphics, x, y, 18, SLOT_EDGE, SLOT_LIP, SLOT_INSIDE);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 6, TITLE, true);
        // the knife: how sharp it still is
        ItemStack knife = this.menu.knife();
        int kx = ButchersTableMenu.KNIFE_X + 20;
        if (knife.isEmpty()) {
            graphics.text(this.font, "Knife", kx, ButchersTableMenu.KNIFE_Y + 4, 0xFFE0A080, false);
        } else {
            int left = ButcherKnifeItem.cutsLeft(knife);
            graphics.text(this.font, left + "/" + ButcherKnifeItem.maxCuts(knife), kx, ButchersTableMenu.KNIFE_Y + 4,
                left <= 2 ? 0xFFFF7A5A : 0xFFBFD8F0, false);
        }
        MeatCut cut = MeatCut.ofSlab(this.menu.slab());
        if (cut != null && knife.isEmpty()) {
            graphics.text(this.font, "Needs a", 104, 18, 0xFFE0A080, false);
            graphics.text(this.font, "Butcher Knife", 104, 29, 0xFFE0A080, false);
        } else if (cut != null) {
            int count = Freshness.yield(cut, this.menu.slab());
            ItemStack good = cut.result(true).copyWithCount(count);
            ItemStack bad = cut.result(false).copyWithCount(count);
            graphics.text(this.font, "Clean: " + good.getCount() + "x", 104, 18, 0xFF9CF08A, false);
            graphics.item(good, 148, 14);
            graphics.text(this.font, "Botched: " + bad.getCount() + "x", 104, 31, 0xFFE0A080, false);
            graphics.item(bad, 156, 27);
        } else {
            graphics.text(this.font, "Put in a", 104, 18, LABEL, false);
            graphics.text(this.font, "Meat Slab", 104, 29, LABEL, false);
        }
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, LABEL, false);
    }

    // ── the butchery game: drawn over everything, takes every click ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.game == null) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
            return;
        }
        this.extractContents(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.game.draw(graphics, this.font, this.width, this.height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.game == null) return super.mouseClicked(event, doubleClick);
        this.game.press(event.x(), event.y(), event.button());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.game == null) return super.mouseReleased(event);
        this.game.release(event.button());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.game == null) return super.mouseDragged(event, dx, dy);
        this.game.drag(event.x(), event.y(), event.button());
        return true;
    }

    // Esc gives up (plain meat); Space / Enter ends the scan; every other key is ignored while cutting
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.game == null) return super.keyPressed(event);
        if (event.isEscape()) {
            if (this.game.playing()) this.game.giveUp();
            return true;
        }
        this.game.key(event.key());
        return true;
    }
}

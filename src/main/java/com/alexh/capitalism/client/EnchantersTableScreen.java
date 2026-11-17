package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.enchanter.EnchantRemoval;
import com.alexh.capitalism.enchanter.EnchantersTableMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.List;

// The Enchanter's Table (textures/gui/container/enchanters_table.png, drawn by tools/make_enchanters_table_gui.ps1):
// a night-sky panel with the big item slot, the small slot and the result across the top, and under them the item's
// enchantments as glowing rows, like the Enchanting Table's offers: name on the left (curses in red), the XP cost on the
// right (green, red if you're short). Click one to pick it (a menu button); scroll if there are more than ROWS.
// With nothing to list, enchanting runes drift through the empty space (no recipe hints: players find those out).
public class EnchantersTableScreen extends AbstractContainerScreen<EnchantersTableMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/enchanters_table.png");

    private static final int ROWS = 5;
    private static final int ROW_X = 10;
    private static final int ROW_Y = 51;
    private static final int ROW_W = 156;
    private static final int ROW_H = 14;
    private static final int ROW_STEP = 15;

    private static final int TITLE = 0xFFE6D8FF;
    private static final int NAME = 0xFFEDE4FF;
    private static final int CURSE = 0xFFFF7070;
    private static final int COST = 0xFF80FF20;
    private static final int SHORT = 0xFFFF5050;
    private static final Style GLYPH_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt")));
    private static final String RUNES = "abcdefghijklmnopqrstuvwxyz";
    private static final int GLYPHS = 18;
    private static final int HINT = 0xFFB9A6E0;
    private static final int GLYPH_COLOR = 0xC9A4FF;

    private int scroll;

    public EnchantersTableScreen(EnchantersTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = 129;
    }

    // Nothing to list: enchanting runes (the Enchanting Table's glyph font) drift up through the empty space, fading in
    // and out and changing as they go. Just for looks: no recipes here, players find those out.
    private void drawIdleGlyphs(GuiGraphicsExtractor graphics, int xo, int yo) {
        long now = Util.getMillis();
        int top = yo + ROW_Y;
        int height = ROWS * ROW_STEP - 8;
        for (int i = 0; i < GLYPHS; i++) {
            double speed = 0.006 + (i % 5) * 0.002;
            double rise = (now * speed + i * 53) % height;
            int alpha = (int) (255 * Math.sin(Math.PI * rise / height));
            if (alpha < 24) continue;
            int x = xo + ROW_X + 4 + (i * 37 + 11) % (ROW_W - 12);
            int y = top + height - (int) rise;
            char rune = RUNES.charAt((int) ((i * 13 + now / 700) % RUNES.length()));
            graphics.text(this.font, Component.literal(String.valueOf(rune)).withStyle(GLYPH_STYLE), x, y, (alpha << 24) | GLYPH_COLOR, false);
        }
    }

    private int rowAt(double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos - ROW_X;
        double my = mouseY - this.topPos - ROW_Y;
        if (mx < 0 || mx >= ROW_W || my < 0) return -1;
        int row = (int) (my / ROW_STEP);
        if (row >= ROWS || my - row * ROW_STEP >= ROW_H) return -1;
        int index = this.scroll + row;
        return index < this.menu.entries().size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.minecraft.gameMode != null) {
            int index = this.rowAt(event.x(), event.y());
            if (index >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int size = this.menu.entries().size();
        if (size > ROWS && scrollY != 0) {
            this.scroll = Math.clamp(this.scroll - (int) Math.signum(scrollY), 0, size - ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        List<EnchantRemoval.Entry> entries = this.menu.entries();
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, entries.size() - ROWS));
        if (entries.isEmpty()) {
            this.drawIdleGlyphs(graphics, xo, yo);
            return;
        }
        Player player = this.minecraft.player;
        int hovered = this.rowAt(mouseX, mouseY);
        for (int row = 0; row < ROWS && this.scroll + row < entries.size(); row++) {
            int index = this.scroll + row;
            EnchantRemoval.Entry entry = entries.get(index);
            int x = xo + ROW_X;
            int y = yo + ROW_Y + row * ROW_STEP;
            boolean picked = index == this.menu.selected();
            // the row: a dark violet bar, brighter when hovered, glowing when picked
            int fillColor = picked ? 0xFF6B3FA8 : index == hovered ? 0xFF46306A : 0xFF2C1F45;
            int edgeTop = picked ? 0xFFC9A4FF : 0xFF5C4888;
            int edgeBottom = picked ? 0xFF3A1E66 : 0xFF140C22;
            graphics.fill(x, y, x + ROW_W, y + ROW_H, fillColor);
            graphics.fill(x, y, x + ROW_W, y + 1, edgeTop);
            graphics.fill(x, y, x + 1, y + ROW_H, edgeTop);
            graphics.fill(x, y + ROW_H - 1, x + ROW_W, y + ROW_H, edgeBottom);
            graphics.fill(x + ROW_W - 1, y, x + ROW_W, y + ROW_H, edgeBottom);
            String cost = String.valueOf(entry.cost());
            int costWidth = this.font.width(cost);
            String name = this.font.plainSubstrByWidth(entry.name().getString(), ROW_W - costWidth - 14);
            graphics.text(this.font, name, x + 4, y + 3, entry.curse() ? CURSE : NAME, true);
            boolean affordable = player != null && EnchantRemoval.canAfford(player, entry);
            graphics.text(this.font, cost, x + ROW_W - costWidth - 4, y + 3, affordable ? COST : SHORT, true);
        }
        // more below / above (unless the status line is there)
        if (entries.size() > ROWS && this.status().isEmpty()) {
            String more = (this.scroll + 1) + "-" + Math.min(entries.size(), this.scroll + ROWS) + " of " + entries.size();
            graphics.text(this.font, more, xo + this.imageWidth - 8 - this.font.width(more), yo + 129, HINT, false);
        }
        if (hovered >= 0) {
            EnchantRemoval.Entry entry = entries.get(hovered);
            graphics.setTooltipForNextFrame(this.font, Component.literal("Burn off ").append(entry.name())
                .append(" for " + entry.cost() + (entry.cost() == 1 ? " level" : " levels")), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, TITLE, true);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, HINT, false);
        String status = this.status();
        if (!status.isEmpty()) {
            graphics.text(this.font, status, this.imageWidth - 8 - this.font.width(status), this.inventoryLabelY, SHORT, false);
        }
    }

    // What's still missing (shown right of the "Inventory" label)
    private String status() {
        if (this.menu.entries().isEmpty()) return "";
        if (!this.menu.hasRemover()) return "Needs Ench-Removal";
        return this.menu.selected() < 0 ? "Pick one above" : "";
    }
}

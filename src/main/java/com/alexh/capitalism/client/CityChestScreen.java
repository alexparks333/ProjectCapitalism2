package com.alexh.capitalism.client;

import com.alexh.capitalism.realestate.CityChestBlockEntity;
import com.alexh.capitalism.realestate.CityChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

// The City Chest (realestate/CityChestMenu), drawn here: navy and gold. On top "The city wants": the wanted row,
// ghosted (it isn't really in there); right under each wanted thing the slot to put it in, red until it's filled,
// then green; then the inventory. Setting it up (an op in creative) says so, and the wanted row takes items.
public class CityChestScreen extends AbstractContainerScreen<CityChestMenu> {

    private static final int NAVY = 0xFF1C2448;
    private static final int NAVY_DARK = 0xFF10162E;
    private static final int GOLD = 0xFFE8C060;
    private static final int TEXT = 0xFFF0E6C8;
    private static final int RED = 0xFFD04848;
    private static final int GREEN = 0xFF5ACB5A;
    // laid over the wanted row so it looks ghosted
    private static final int GHOST = 0x7810162E;

    public CityChestScreen(CityChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, CityChestMenu.HEIGHT);
        this.inventoryLabelY = CityChestMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        graphics.fill(x0, y0, x0 + w, y0 + h, GOLD);
        graphics.fill(x0 + 2, y0 + 2, x0 + w - 2, y0 + h - 2, NAVY);
        graphics.fill(x0 + 5, y0 + CityChestMenu.WANTED_Y - 4, x0 + w - 5, y0 + CityChestMenu.DEPOSIT_Y + 20, NAVY_DARK);
        for (int i = 0; i < CityChestBlockEntity.WANTED; i++) {
            int x = x0 + 7 + i * 18;
            ItemStack want = this.menu.getSlot(i).getItem();
            ItemStack have = this.menu.getSlot(CityChestBlockEntity.WANTED + i).getItem();
            DesignerDraw.slot(graphics, x, y0 + CityChestMenu.WANTED_Y - 1, 18, 0xFF0A0F22, 0xFF3A4270, 0xFF1A2040);
            int edge = want.isEmpty() ? 0xFF2A3058 : CityChestBlockEntity.covers(want, have) ? GREEN : RED;
            DesignerDraw.slot(graphics, x, y0 + CityChestMenu.DEPOSIT_Y - 1, 18, 0xFF060A18, edge, 0xFF2A3460);
        }
        for (int i = 0; i < 36; i++) {
            int x = x0 + 7 + (i % 9) * 18;
            int y = y0 + (i < 27 ? CityChestMenu.INV_Y - 1 + (i / 9) * 18 : CityChestMenu.INV_Y + 57);
            DesignerDraw.slot(graphics, x, y, 18, 0xFF060A18, 0xFF4A5488, 0xFF2A3460);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 6, GOLD, false);
        graphics.text(this.font, this.menu.setup() ? "Setting up: what the city wants" : "The city wants:", 8, 17, TEXT, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, TEXT, false);
        // ghost the wanted row, over the items drawn in it
        graphics.nextStratum();
        for (int i = 0; i < CityChestBlockEntity.WANTED; i++) {
            int x = 8 + i * 18;
            graphics.fill(x, CityChestMenu.WANTED_Y, x + 16, CityChestMenu.WANTED_Y + 16, GHOST);
        }
    }
}

package com.alexh.capitalism.client;

import com.alexh.capitalism.pets.FoodTroughMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// A Food Trough's window (pets/FoodTroughMenu): laid out like a hopper's, drawn in code in the vanilla grey: its 1, 2 or
// 5 food slots in a row in the middle, your inventory below.
public class FoodTroughScreen extends AbstractContainerScreen<FoodTroughMenu> {

    static final int PANEL = 0xFFC6C6C6;
    static final int LIGHT = 0xFFFFFFFF;
    static final int SHADOW = 0xFF555555;
    static final int SLOT = 0xFF8B8B8B;
    static final int SLOT_DARK = 0xFF373737;

    public FoodTroughScreen(FoodTroughMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        for (int i = 0; i < this.menu.foodSlots(); i++) slot(graphics, this.leftPos + FoodTroughMenu.slotX(this.menu.foodSlots(), i) - 1, this.topPos + FoodTroughMenu.SLOT_Y - 1);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slot(graphics, this.leftPos + 7 + col * 18, this.topPos + 50 + row * 18);
        }
        for (int col = 0; col < 9; col++) slot(graphics, this.leftPos + 7 + col * 18, this.topPos + 108);
    }

    // A raised vanilla-style window
    static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        graphics.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, LIGHT);
        graphics.fill(x + 3, y + 3, x + w - 1, y + h - 1, SHADOW);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    // A sunken 18x18 slot
    static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, LIGHT);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }
}

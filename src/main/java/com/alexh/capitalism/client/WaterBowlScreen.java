package com.alexh.capitalism.client;

import com.alexh.capitalism.pets.WaterBowlMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

// A Water Bowl's window (pets/WaterBowlMenu): a tank that's as full as the bowl, with a gentle ripple on top, and the
// water in mL under it ("1,250 / 3,000 mL"). Water goes in with a bucket or bottle on the bowl itself.
public class WaterBowlScreen extends AbstractContainerScreen<WaterBowlMenu> {

    private static final int TANK_X0 = 14;
    private static final int TANK_X1 = 162;
    private static final int TANK_Y0 = 22;
    private static final int TANK_Y1 = 52;

    public WaterBowlScreen(WaterBowlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 80);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        FoodTroughScreen.panel(graphics, x, y, this.imageWidth, this.imageHeight);
        // the tank, sunk in like a slot
        graphics.fill(x + TANK_X0 - 1, y + TANK_Y0 - 1, x + TANK_X1 + 1, y + TANK_Y1 + 1, FoodTroughScreen.SLOT_DARK);
        graphics.fill(x + TANK_X0, y + TANK_Y0, x + TANK_X1 + 1, y + TANK_Y1 + 1, FoodTroughScreen.LIGHT);
        graphics.fill(x + TANK_X0, y + TANK_Y0, x + TANK_X1, y + TANK_Y1, 0xFF4A4A52);
        // the water, from the bottom up, with a ripple along its top
        float full = Math.min(1f, this.menu.water() / (float) this.menu.capacity());
        int height = Math.round((TANK_Y1 - TANK_Y0) * full);
        if (height > 0) {
            int top = y + TANK_Y1 - height;
            graphics.fill(x + TANK_X0, top, x + TANK_X1, y + TANK_Y1, 0xFF2E62C8);
            graphics.fill(x + TANK_X0, top, x + TANK_X1, Math.min(y + TANK_Y1, top + 3), 0xFF4A86E8);
            long now = Util.getMillis();
            for (int px = x + TANK_X0; px < x + TANK_X1; px += 2) {
                int wave = (int) Math.round(Math.sin((px + now / 120.0) * 0.35) * 1.0);
                if (wave > 0) graphics.fill(px, top, px + 2, top + 1, 0xFF8AB8F8);
            }
        }
        // marks every 1000 mL (a bucket)
        for (int ml = 1000; ml < this.menu.capacity(); ml += 1000) {
            int my = y + TANK_Y1 - Math.round((TANK_Y1 - TANK_Y0) * ml / (float) this.menu.capacity());
            graphics.fill(x + TANK_X1 - 6, my, x + TANK_X1, my + 1, 0x80FFFFFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, 8, 7, 0xFF404040, false);
        String amount = String.format(Locale.ROOT, "%,d / %,d mL", this.menu.water(), this.menu.capacity());
        graphics.text(this.font, amount, (this.imageWidth - this.font.width(amount)) / 2, TANK_Y1 + 8, 0xFF404040, false);
    }
}

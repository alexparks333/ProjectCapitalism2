package com.alexh.capitalism.client;

import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// The Mayoral Chest open: the six-row chest in a frame of the town's colours (crimson velvet, a polished gold edge,
// white enamel rivets), its title "Mayoral Chest" in bold crimson.
public class MayoralChestScreen extends ContainerScreen {

    private static final int GOLD = 0xFFE0B848;
    private static final int GOLD_DARK = 0xFF9A7420;
    private static final int CRIMSON = 0xFF7A1020;
    private static final int ENAMEL = 0xFFF4F0E8;

    public MayoralChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int l = this.leftPos, t = this.topPos, r = l + this.imageWidth, b = t + this.imageHeight;
        // the regal frame round it: crimson velvet, a gold edge, enamel rivets in the corners
        g.fill(l - 6, t - 6, r + 6, b + 6, GOLD_DARK);
        g.fill(l - 5, t - 5, r + 5, b + 5, GOLD);
        g.fill(l - 4, t - 4, r + 4, b + 4, CRIMSON);
        for (int[] c : new int[][]{{l - 4, t - 4}, {r + 1, t - 4}, {l - 4, b + 1}, {r + 1, b + 1}}) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, ENAMEL);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 2, c[1] + 2, GOLD);
        }
        super.extractBackground(g, mouseX, mouseY, a);
    }
}

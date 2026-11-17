package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.weaponsmith.LavaCreatorBlockEntity;
import com.alexh.capitalism.weaponsmith.LavaCreatorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// The Lava Creator: the smith tables' dark molten look (textures/gui/container/lava_creator.png, drawn by
// tools/make_lava_creator_gui.ps1). A water tank on the left (hover it for how much), the water slot beside it, Blaze
// Powder on top, the bucket below, and a channel between them that fills with lava as it brews.
public class LavaCreatorScreen extends AbstractContainerScreen<LavaCreatorMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/gui/container/lava_creator.png");

    private static final int TITLE_COLOR = 0xFFF2D9B8;
    private static final int LABEL_COLOR = 0xFFD8C0A0;
    // the water tank's inside, in the texture
    private static final int TANK_X = 20;
    private static final int TANK_Y = 19;
    private static final int TANK_W = 16;
    private static final int TANK_H = 55;
    // the channel from the Blaze Powder down to the bucket
    private static final int DRIP_X = 103;
    private static final int DRIP_Y = 41;
    private static final int DRIP_W = 6;
    private static final int DRIP_H = 14;

    public LavaCreatorScreen(LavaCreatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // water, from the bottom up, with a lighter surface line and a slow shimmer
        int water = this.menu.water();
        int height = Math.round((float) TANK_H * water / LavaCreatorBlockEntity.WATER_CAPACITY);
        if (height > 0) {
            int top = yo + TANK_Y + TANK_H - height;
            graphics.fill(xo + TANK_X, top, xo + TANK_X + TANK_W, yo + TANK_Y + TANK_H, 0xFF2C5FD0);
            graphics.fill(xo + TANK_X, top, xo + TANK_X + TANK_W, top + 1, 0xFF7FB0FF);
            int shimmer = (int) (Util.getMillis() / 120 % TANK_W);
            graphics.fill(xo + TANK_X + shimmer, top, xo + TANK_X + shimmer + 2, top + 1, 0xFFD8ECFF);
        }

        // the lava channel fills from the top as it brews
        float progress = this.menu.progress();
        if (progress > 0) {
            int drip = Math.round(DRIP_H * progress);
            graphics.fill(xo + DRIP_X, yo + DRIP_Y, xo + DRIP_X + DRIP_W, yo + DRIP_Y + drip, 0xFFFF8A2A);
            graphics.fill(xo + DRIP_X + 1, yo + DRIP_Y, xo + DRIP_X + 3, yo + DRIP_Y + drip, 0xFFFFC870);
        }

        // hover help
        if (inside(mouseX, mouseY, xo + TANK_X, yo + TANK_Y, TANK_W, TANK_H)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                Component.literal("Water: " + buckets(water) + " / " + buckets(LavaCreatorBlockEntity.WATER_CAPACITY) + " buckets"),
                Component.literal("Each Blaze Powder uses half a bucket").withStyle(ChatFormatting.GRAY)), mouseX, mouseY, ItemStack.EMPTY);
        }
        this.slotHint(graphics, LavaCreatorBlockEntity.SLOT_WATER, "Water Bucket (or right-click the block with one)", mouseX, mouseY);
        this.slotHint(graphics, LavaCreatorBlockEntity.SLOT_BLAZE, "Blaze Powder", mouseX, mouseY);
        this.slotHint(graphics, LavaCreatorBlockEntity.SLOT_BUCKET, "An empty Bucket: becomes a Refined Bucket of Lava", mouseX, mouseY);
    }

    private void slotHint(GuiGraphicsExtractor graphics, int index, String text, int mouseX, int mouseY) {
        Slot slot = this.menu.getSlot(index);
        if (slot.hasItem() || !this.menu.getCarried().isEmpty()) return;
        if (inside(mouseX, mouseY, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(text)), mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // 2500 -> "2.5"
    private static String buckets(int millibuckets) {
        float buckets = millibuckets / 1000.0F;
        return buckets == Math.floor(buckets) ? String.valueOf((int) buckets) : String.format(java.util.Locale.ROOT, "%.1f", buckets);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }
}

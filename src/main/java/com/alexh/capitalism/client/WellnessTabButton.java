package com.alexh.capitalism.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The "Wellness" tab stuck to the left side of the survival inventory. Follows the inventory when the recipe book
// pushes it sideways.
public class WellnessTabButton extends AbstractButton {

    private static final int SIZE = 24;
    private static final ItemStack ICON = new ItemStack(Items.APPLE);

    private final AbstractContainerScreen<?> screen;

    public WellnessTabButton(AbstractContainerScreen<?> screen) {
        super(screen.getLeftPos() - SIZE + 2, screen.getTopPos() + 8, SIZE, SIZE, Component.literal("Wellness"));
        this.screen = screen;
        this.setTooltip(Tooltip.create(Component.literal("Wellness")));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        Minecraft.getInstance().gui.setScreen(new WellnessScreen());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Stay attached to the inventory's left edge.
        this.setX(this.screen.getLeftPos() - SIZE + 2);
        this.setY(this.screen.getTopPos() + 8);

        int x = this.getX();
        int y = this.getY();
        boolean hovered = this.isHoveredOrFocused();
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + SIZE, y + SIZE - 1, hovered ? 0xFFE0E0E0 : 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + SIZE, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + SIZE - 1, 0xFFFFFFFF);
        graphics.item(ICON, x + 5, y + 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}

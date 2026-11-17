package com.alexh.capitalism.mixin;

import com.alexh.capitalism.back.BackSlot;
import com.alexh.capitalism.client.BackSlotFrame;
import com.alexh.capitalism.client.BagPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The inventory's picture has no slot where the Back slot is (back/BackSlot): draw one on it. And the bag on your back
// hangs off its right side (client/BagPanel, designer/BagSlot).
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void capitalism$backSlotFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        BackSlotFrame.draw(graphics, screen.capitalism$leftPos() + BackSlot.X - 1, screen.capitalism$topPos() + BackSlot.Y - 1);
        BagPanel.draw(graphics, Minecraft.getInstance().font, screen.capitalism$leftPos(), screen.capitalism$topPos());
    }
}

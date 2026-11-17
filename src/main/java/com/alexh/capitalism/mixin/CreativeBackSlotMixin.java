package com.alexh.capitalism.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The creative inventory tab puts every inventory slot it doesn't know in the main grid, so the Back slot (index 46,
// back/BackSlot) would sit on top of a hotbar slot. Put it left of the offhand slot instead, with a slot frame drawn there.
// The bag slots after it (designer/BagSlot) are left out of the creative inventory (moved far off the screen): use the
// survival inventory for them.
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeBackSlotMixin {

    private static final String SLOT_WRAPPER = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";
    private static final int BACK_INDEX = 46;

    private static final int X = 16;
    private static final int Y = 20;
    // the bag slots (designer/BagSlot, after the Back slot) don't fit here: far off the screen
    private static final int HIDDEN = -10000;

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void capitalism$backSlotFrame(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!((CreativeModeInventoryScreen) (Object) this).isInventoryOpen()) return;
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        com.alexh.capitalism.client.BackSlotFrame.draw(graphics, screen.capitalism$leftPos() + X - 1, screen.capitalism$topPos() + Y - 1);
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = SLOT_WRAPPER), index = 2)
    private int capitalism$backSlotX(Slot target, int index, int x, int y) {
        return index == BACK_INDEX ? X : index > BACK_INDEX ? HIDDEN : x;
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = SLOT_WRAPPER), index = 3)
    private int capitalism$backSlotY(Slot target, int index, int x, int y) {
        return index == BACK_INDEX ? Y : index > BACK_INDEX ? HIDDEN : y;
    }
}

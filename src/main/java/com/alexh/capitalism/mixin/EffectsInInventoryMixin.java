package com.alexh.capitalism.mixin;

import com.alexh.capitalism.client.BagPanel;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The potion effects listed right of the survival inventory move over to make room for the bag pouch hanging there
// (client/BagPanel), both where it checks for room and where it draws them. Both work out where the effects start from
// the inventory picture's width (leftPos + imageWidth + 2), so that width reads wider by the pouch while a bag is worn.
// (It used to change a local variable found by name, optionally; that could miss without a word, and the pouch covered
// the effects.)
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {

    @ModifyExpressionValue(method = {"canSeeEffects", "extractRenderState"}, at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;imageWidth:I"))
    private int capitalism$besideBag(int imageWidth) {
        return imageWidth + BagPanel.effectsShift();
    }
}

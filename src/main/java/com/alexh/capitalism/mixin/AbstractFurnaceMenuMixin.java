package com.alexh.capitalism.mixin;

import com.alexh.capitalism.occupation.CookMadeFood;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Shift-clicking a Furnace / Smoker / Blast Furnace result moves the food into your inventory BEFORE vanilla's "made by"
// hook runs (on what's left behind). So a Cook's food is marked Cook-made here, while it's still in the result slot
// (occupation/CookMadeFood). A normal click goes through mixin/ItemMixin.
@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin {

    private static final int RESULT_SLOT = 2;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void capitalism$cookMadeOnShiftClick(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != RESULT_SLOT) return;
        ItemStack result = ((AbstractContainerMenu) (Object) this).slots.get(RESULT_SLOT).getItem();
        CookMadeFood.madeBy(result, player);
    }
}

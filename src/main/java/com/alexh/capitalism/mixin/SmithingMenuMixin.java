package com.alexh.capitalism.mixin;

import com.alexh.capitalism.weaponsmith.WeaponsmithRules;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Smithing Table: Netherite swords and tools are job-only (see WeaponsmithRules.checkSmithing).
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void capitalism$weaponsmithOnly(CallbackInfo ci) {
        ItemCombinerMenuAccessor menu = (ItemCombinerMenuAccessor) this;
        WeaponsmithRules.checkSmithing(menu.capitalism$getPlayer(), menu.capitalism$getResultSlots());
    }
}

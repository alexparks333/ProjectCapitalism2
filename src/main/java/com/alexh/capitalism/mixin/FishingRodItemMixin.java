package com.alexh.capitalism.mixin;

import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Fisherman's Patience (a skill book lesson, librarian/Skill): fish bite faster, like Lure I (level II: Lure II). Lure
// takes 5 seconds off the wait per level; this adds the same on top of the rod's own Lure.
@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFishingTimeReduction(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)F"))
    private float capitalism$patience(float seconds, @Local(argsOnly = true) Player player) {
        return seconds + 5.0F * Skills.level(player, Skill.FISHERMANS_PATIENCE);
    }
}

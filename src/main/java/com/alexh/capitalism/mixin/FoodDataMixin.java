package com.alexh.capitalism.mixin;

import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// First Aid Basics (a skill book lesson, librarian/Skill): natural healing (from a full food bar, fast while there's
// saturation and slow after) heals 25% more each time (level II: 50%), for the same hunger.
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void capitalism$firstAid(ServerPlayer player, float amount) {
        int level = Skills.level(player, Skill.FIRST_AID_BASICS);
        player.heal(level > 0 ? amount * (1.0F + 0.25F * level) : amount);
    }
}

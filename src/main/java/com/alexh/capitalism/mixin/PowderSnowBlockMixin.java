package com.alexh.capitalism.mixin;

import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Sure Footed (a skill book trait, librarian/Skill): walk on Powder Snow like you had leather boots on. Both sides (the
// client moves the player; the trait is synced, librarian/TraitData).
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void capitalism$sureFooted(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Skills.has(entity, Skill.SURE_FOOTED)) cir.setReturnValue(true);
    }
}

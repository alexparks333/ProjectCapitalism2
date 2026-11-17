package com.alexh.capitalism.mixin;

import com.alexh.capitalism.librarian.Skills;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Skill book lessons (librarian/Skill) are something you learned, not a potion: clearing every effect (drinking milk,
// a Totem of Undying) leaves them alone. They still end when they run out or you die.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Redirect(method = "removeAllEffects", at = @At(value = "INVOKE",
        target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean capitalism$keepLessons(LivingEntity entity, MobEffectInstance effect) {
        return Skills.lessonOf(effect) != null || EventHooks.onEffectRemoved(entity, effect);
    }
}

package com.alexh.capitalism.mixin;

import com.alexh.capitalism.weaponsmith.Socketing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Client only: an Echo Shard in your helmet's socket (weaponsmith/Socketing) makes hostile mobs near you glow (the
// outline you see through walls), on your screen only.
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public @Nullable LocalPlayer player;

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void capitalism$echoSight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = this.player;
        if (self == null || !(entity instanceof Enemy) || !(entity instanceof LivingEntity living) || !living.isAlive()) return;
        if (Socketing.gem(self.getItemBySlot(EquipmentSlot.HEAD)) != Socketing.Gem.ECHO) return;
        if (entity.distanceToSqr(self) <= Socketing.ECHO_RANGE * Socketing.ECHO_RANGE) cir.setReturnValue(true);
    }
}

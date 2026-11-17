package com.alexh.capitalism.mixin;

import com.alexh.capitalism.core.TougherMobs;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A Boss blaze shoots 1.5x as fast (NPCs.md > Mob levels, core/TougherMobs): a blaze heats up, throws its three fireballs and cools off, all counted by its attack
// goal's ticks, so for a Boss the goal runs one extra step every other tick, and the whole cycle goes 1.5x as fast.
@Mixin(targets = "net.minecraft.world.entity.monster.Blaze$BlazeAttackGoal")
public abstract class BlazeAttackMixin {

    @Shadow @Final private Blaze blaze;

    @Unique private boolean capitalism$extraStep;

    @Inject(method = "tick", at = @At("TAIL"))
    private void capitalism$bossShootsFaster(CallbackInfo ci) {
        if (this.capitalism$extraStep || this.blaze.tickCount % 2 != 0 || TougherMobs.levelOf(this.blaze) != TougherMobs.Level.BOSS) return;
        this.capitalism$extraStep = true;
        try {
            ((Goal) (Object) this).tick();
        } finally {
            this.capitalism$extraStep = false;
        }
    }
}

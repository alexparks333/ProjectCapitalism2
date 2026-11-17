package com.alexh.capitalism.mixin;

import com.alexh.capitalism.core.TougherMobs;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A Boss skeleton shoots 1.5x as fast (NPCs.md > Mob levels, core/TougherMobs). A shot is a 20-tick bow draw and then a
// wait (the attack interval: 20 ticks on Hard, 40 otherwise, more for some kinds), so the wait is cut until the whole
// draw-and-wait takes 2/3 as long. Set wherever the skeleton picks its weapon goal (spawning, loading, a new weapon), and
// TougherMobs.setLevel asks for it again once the level is given.
@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {

    @Shadow @Final private RangedBowAttackGoal<AbstractSkeleton> bowGoal;

    @Shadow protected abstract int getHardAttackInterval();

    @Shadow protected abstract int getAttackInterval();

    @Inject(method = "reassessWeaponGoal", at = @At("TAIL"))
    private void capitalism$bossShootsFaster(CallbackInfo ci) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (self.level().isClientSide() || TougherMobs.levelOf(self) != TougherMobs.Level.BOSS) return;
        if (!(self.getMainHandItem().getItem() instanceof BowItem)) return;
        int interval = self.level().getDifficulty() == Difficulty.HARD ? this.getHardAttackInterval() : this.getAttackInterval();
        int faster = Math.max(1, Math.round((interval + 20) / 1.5F - 20));
        this.bowGoal.setMinAttackInterval(faster);
    }
}

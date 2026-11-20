package com.alexh.capitalism.mixin;

import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Trail Rations (a skill book lesson, librarian/Skill): while sprinting you get hungry 30% slower (level II: 60%).
// And eating with room in a Balanced diet's bonus drumstick (canEat).
@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float capitalism$trailRations(float amount) {
        Player self = (Player) (Object) this;
        if (!self.isSprinting()) return amount;
        // Second Wind (a Trail Mix Bar): sprinting costs no hunger at all
        if (self.hasEffect(com.alexh.capitalism.core.ModEffects.SECOND_WIND)) return 0.0F;
        int level = Skills.level(self, Skill.TRAIL_RATIONS);
        return level > 0 ? amount * (1.0F - 0.3F * level) : amount;
    }

    // A Balanced diet makes the food bar 22 (an 11th drumstick, Wellness.md), but vanilla only lets you eat below 20, so
    // once the bonus drumstick was gone you sat at 20 of 22 and couldn't eat. You can eat whenever you're below your own
    // top. Both sides ask (your client decides whether to start eating), so each reads the top it knows: the server's
    // ThirstData, the client's copy synced to it (player/ClientThirstData). The bonus drumstick then fills from the meal
    // (ThirstData.fillBonusFood).
    @org.spongepowered.asm.mixin.injection.Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void capitalism$roomInBonusDrumstick(boolean canAlwaysEat, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Player self = (Player) (Object) this;
        int cap = self instanceof net.minecraft.server.level.ServerPlayer
            ? self.getData(com.alexh.capitalism.core.ModAttachments.THIRST.get()).foodCap
            : com.alexh.capitalism.player.ClientThirstData.foodCap;
        if (self.getFoodData().getFoodLevel() < cap) cir.setReturnValue(true);
    }
}

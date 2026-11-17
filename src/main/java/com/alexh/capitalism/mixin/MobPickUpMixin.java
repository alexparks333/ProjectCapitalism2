package com.alexh.capitalism.mixin;

import com.alexh.capitalism.deathtax.GoldCollector;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// A player's dropped gold is theirs alone (deathtax/GoldCollector, Economy.md > Gold belongs to its owner): no mob picks
// it up (foxes, allays, villagers, zombies... they all pick things up through Mob.aiStep). Except piglin bartering:
// a piglin may take a gold ingot, so trading with piglins still works.
@Mixin(Mob.class)
public abstract class MobPickUpMixin {

    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;pickUpItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/item/ItemEntity;)V"))
    private boolean capitalism$notOwnedGold(Mob mob, ServerLevel level, ItemEntity item) {
        if (mob instanceof net.minecraft.world.entity.monster.piglin.Piglin && item.getItem().is(net.minecraft.world.item.Items.GOLD_INGOT)) return true;
        return !GoldCollector.isLocked(item);
    }
}
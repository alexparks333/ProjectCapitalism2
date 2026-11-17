package com.alexh.capitalism.mixin;

import com.alexh.capitalism.deathtax.GoldCollector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// A player's dropped gold is theirs alone (deathtax/GoldCollector): hoppers and hopper minecarts (both suck items up
// through getItemsAtAndAbove) don't see it.
@Mixin(HopperBlockEntity.class)
public abstract class HopperItemsMixin {

    @Inject(method = "getItemsAtAndAbove", at = @At("RETURN"), cancellable = true)
    private static void capitalism$notOwnedGold(Level level, Hopper hopper, CallbackInfoReturnable<List<ItemEntity>> cir) {
        List<ItemEntity> items = cir.getReturnValue();
        if (items.stream().anyMatch(GoldCollector::isLocked)) cir.setReturnValue(items.stream().filter(e -> !GoldCollector.isLocked(e)).toList());
    }
}
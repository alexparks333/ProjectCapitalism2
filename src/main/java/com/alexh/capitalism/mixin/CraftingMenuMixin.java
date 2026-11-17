package com.alexh.capitalism.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Vanilla works out the crafting result without saying who's crafting. This tells our job recipes
// (occupation/JobShapedRecipe) who it is, for the crafting table and the 2x2 inventory grid.
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void capitalism$setCrafter(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
                                             ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
        CommonHooks.setCraftingPlayer(player);
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void capitalism$clearCrafter(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
                                               ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
        CommonHooks.setCraftingPlayer(null);
    }
}

package com.alexh.capitalism.mixin;

import com.alexh.capitalism.occupation.CookMadeFood;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// A Cook's Furnace / Smoker / Blast Furnace cooks Gourmet food: the furnace works out each result once per tick
// (recipe.assemble) and uses that same item both to check it can stack onto the output and to make it, so it's turned
// Cook-made right there (occupation/CookMadeFood.furnaceResult) and is Gourmet while it's still in the output slot.
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/AbstractCookingRecipe;assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack capitalism$cookMade(ItemStack result, ServerLevel level, BlockPos pos, BlockState state,
                                                 AbstractFurnaceBlockEntity furnace) {
        return CookMadeFood.furnaceResult(result, furnace, level);
    }
}

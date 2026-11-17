package com.alexh.capitalism.weaponsmith;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RepairItemRecipe;

// Vanilla's "two damaged items in the crafting grid" repair (replaces data/minecraft/recipe/repair_item.json):
// switched off for everyone. Repairs only happen at the smith tables (see WeaponRepairs).
public class JobRepairItemRecipe extends RepairItemRecipe {

    public static final JobRepairItemRecipe INSTANCE = new JobRepairItemRecipe();
    public static final RecipeSerializer<JobRepairItemRecipe> SERIALIZER = new RecipeSerializer<>(
        MapCodec.unit(INSTANCE), StreamCodec.<RegistryFriendlyByteBuf, JobRepairItemRecipe>unit(INSTANCE));

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<RepairItemRecipe> getSerializer() {
        return (RecipeSerializer) SERIALIZER;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return ItemStack.EMPTY;
    }
}

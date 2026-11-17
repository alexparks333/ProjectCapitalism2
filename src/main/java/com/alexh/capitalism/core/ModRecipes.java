package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.occupation.JobImbueRecipe;
import com.alexh.capitalism.occupation.JobShapedRecipe;
import com.alexh.capitalism.weaponsmith.JobRepairItemRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Capitalism.MODID);

    // "type": "capitalism:job_shaped": a shaped recipe that only a job can make (or that gives that job more)
    public static final Supplier<RecipeSerializer<JobShapedRecipe>> JOB_SHAPED =
        SERIALIZERS.register("job_shaped", () -> JobShapedRecipe.SERIALIZER);

    // "type": "capitalism:job_imbue": Tipped Arrows, double for a Weaponsmith
    public static final Supplier<RecipeSerializer<JobImbueRecipe>> JOB_IMBUE =
        SERIALIZERS.register("job_imbue", () -> JobImbueRecipe.SERIALIZER);

    // replaces vanilla repair_item: two damaged weapons only combine for a Weaponsmith
    public static final Supplier<RecipeSerializer<JobRepairItemRecipe>> REPAIR_ITEM =
        SERIALIZERS.register("repair_item", () -> JobRepairItemRecipe.SERIALIZER);
}

package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.weaponsmith.SwordLootModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Capitalism.MODID);

    // "type": "capitalism:forge_swords": swords in generated loot get their stats rolled
    public static final Supplier<MapCodec<SwordLootModifier>> FORGE_SWORDS =
        SERIALIZERS.register("forge_swords", () -> SwordLootModifier.CODEC);

    // "type": "capitalism:farm_harvest": only Farmers get full crop harvests, fertilized farmland gives more
    public static final Supplier<MapCodec<com.alexh.capitalism.farmer.FarmHarvestModifier>> FARM_HARVEST =
        SERIALIZERS.register("farm_harvest", () -> com.alexh.capitalism.farmer.FarmHarvestModifier.CODEC);

    // "type": "capitalism:rare_catch": the rare fishing catches (fishing/RareCatchModifier, Fishing.md)
    public static final Supplier<MapCodec<com.alexh.capitalism.fishing.RareCatchModifier>> RARE_CATCH =
        SERIALIZERS.register("rare_catch", () -> com.alexh.capitalism.fishing.RareCatchModifier.CODEC);
}

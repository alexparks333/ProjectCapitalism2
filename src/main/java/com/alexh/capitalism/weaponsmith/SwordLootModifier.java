package com.alexh.capitalism.weaponsmith;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Optional;

// Swords and tools found in loot (chests, barrels, mob drops, fishing...) get their stats as the loot is generated, so
// they're already rolled when you open the chest. Everyone-else ranges (see SwordForging, ToolForging).
// Turned on by data/capitalism/loot_modifiers/forge_swords.json; the codec is registered in core/ModLootModifiers.
public class SwordLootModifier extends LootModifier {

    public static final MapCodec<SwordLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> codecStart(inst).apply(inst, SwordLootModifier::new));

    public SwordLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (ItemStack stack : generatedLoot) {
            SwordForging.roll(stack, null, context.getRandom());
            ToolForging.roll(stack, null, context.getRandom());
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

package com.alexh.capitalism.fishing;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModComponents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jspecify.annotations.Nullable;

// The rare catches (Fishing.md): every cast that brings something up (the fishing loot table, only it) first rolls for
// a rare catch where the hook landed (RareCatch.livesIn), rarest first; one that comes up REPLACES the ordinary catch.
// The catch gets its weight, catcher and day (CatchInfo). Some do something as they're landed: the Pufferfish King
// poisons you a little, the Electric Eel zaps you; the biggest are told to the whole server.
// Turned on by data/capitalism/loot_modifiers/rare_catch.json; the codec is registered in core/ModLootModifiers.
public class RareCatchModifier extends LootModifier {

    public static final MapCodec<RareCatchModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, RareCatchModifier::new));

    public RareCatchModifier(java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!context.getQueriedLootTableId().equals(BuiltInLootTables.FISHING.identifier())) return loot;
        Entity hook = context.getOptional(LootContextParams.THIS_ENTITY);
        if (!(hook instanceof FishingHook fishing) || !(fishing.getPlayerOwner() instanceof ServerPlayer player)) return loot;
        ServerLevel level = context.getLevel();
        Vec3 at = context.getOptional(LootContextParams.ORIGIN);
        BlockPos pos = at == null ? fishing.blockPosition() : BlockPos.containing(at);
        // Luck of the Sea on the rod opens the rare catches up (Fishing.md > Luck of the Sea); the rest of the cast's
        // Luck (vanilla adds the enchantment's level to the player's Luck) counts on top
        var tool = context.getOptional(LootContextParams.TOOL);
        int sea = luckOfTheSea(level, tool instanceof ItemStack rod ? rod : player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem ? player.getMainHandItem() : player.getOffhandItem());
        RareCatch rare = rollFor(player, RareCatch.Where.at(level, pos), sea, Math.max(0.0F, context.getLuck() - sea), level.getRandom());
        if (rare == null) return loot;
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        out.add(land(level, player, rare, level.getRandom()));
        return out;
    }

    // a rare catch for this player's cast, or null; never for someone an op has restricted (FishWatch: silently, they
    // just get ordinary fish)
    public static @Nullable RareCatch rollFor(ServerPlayer player, RareCatch.Where where, int luckOfTheSea, float otherLuck, RandomSource random) {
        return FishWatch.restricted(player) ? null : roll(where, luckOfTheSea, otherLuck, random);
    }

    // a rare catch on this cast, or null: each that lives here gets its chance, rarest first
    // the rod's Luck of the Sea level (0: none)
    public static int luckOfTheSea(ServerLevel level, ItemStack rod) {
        var holder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .get(net.minecraft.world.item.enchantment.Enchantments.LUCK_OF_THE_SEA);
        return holder.map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h, rod)).orElse(0);
    }

    public static @Nullable RareCatch roll(RareCatch.Where where, int luckOfTheSea, float otherLuck, RandomSource random) {
        if (luckOfTheSea <= 0) return null;
        RareCatch[] all = RareCatch.values();
        for (int i = all.length - 1; i >= 0; i--) {
            RareCatch c = all[i];
            if (c.livesIn(where) && random.nextDouble() < c.chanceFor(luckOfTheSea, otherLuck)) return c;
        }
        return null;
    }

    // the catch as an item: weighed, named, dated; and what landing it does
    public static ItemStack land(ServerLevel level, ServerPlayer player, RareCatch rare, RandomSource random) {
        ItemStack stack = make(rare, player.getGameProfile().name(), CatchInfo.dateText(Calendar.date(level)), random);
        CatchInfo info = stack.get(ModComponents.CATCH_INFO.get());
        // into their collection: they caught it (only here, never for one bought, given or spawned)
        CatchCollection.record(player, rare, info == null ? 0 : info.weight());
        com.alexh.capitalism.bounty.BountyEvents.rareCaught(player, rare);
        switch (rare) {
            case PUFFERFISH_KING -> {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                player.sendOverlayMessage(Component.literal("The Pufferfish King puffs up and pricks you!").withStyle(ChatFormatting.GREEN));
            }
            case ELECTRIC_EEL -> {
                player.hurtServer(level, level.damageSources().lightningBolt(), 3.0F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
                player.sendOverlayMessage(Component.literal("⚡ The Electric Eel zaps you!").withStyle(ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
        if (rare.brag) brag(level, player, rare, info);
        else player.sendOverlayMessage(Component.literal("🎣 A rare catch: " + (info != null && info.weight() > 0 ? info.weightText() + " " : "") + rare.title + "!")
            .withStyle(ChatFormatting.AQUA));
        return stack;
    }

    // the item for a catch, weighed (somewhere in its range, middling weights likeliest)
    public static ItemStack make(RareCatch rare, String catcher, String date, RandomSource random) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, rare.id)));
        // the bottle and the chest aren't weighed or named (so they still stack)
        if (!rare.weighed()) return stack;
        double t = (random.nextDouble() + random.nextDouble()) / 2;
        float weight = (float) (rare.minLb + (rare.maxLb - rare.minLb) * t);
        weight = weight >= 20 ? Math.round(weight) : Math.round(weight * 10) / 10.0F;
        stack.set(ModComponents.CATCH_INFO.get(), new CatchInfo(weight, catcher, date));
        return stack;
    }

    // the biggest catches, told to everyone (the scale with a fanfare)
    static void brag(ServerLevel level, ServerPlayer player, RareCatch rare, @Nullable CatchInfo info) {
        boolean scale = rare == RareCatch.GOLDEN_LEVIATHAN_SCALE;
        String what = (info != null && info.weight() > 0 && !scale ? info.weightText() + " " : "a ") + rare.title;
        Component line = scale
            ? Component.literal("🌊✨ " + player.getGameProfile().name() + " hauled up a GOLDEN LEVIATHAN SCALE from the deep! ✨🌊")
                .withStyle(s -> s.withColor(TextColor.fromRgb(0xFFD040)).withBold(true))
            : Component.literal("🎣 " + player.getGameProfile().name() + " landed " + what + "!").withStyle(s -> s.withColor(TextColor.fromRgb(0x40C8E0)));
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(line);
            if (scale) p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        if (scale) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 0.8, 0.6, 0.3);
        }
    }
}

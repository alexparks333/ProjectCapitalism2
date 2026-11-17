package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.network.ThirstSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

// Drinking straight from water with an empty hand, and salt water.
//  - Right-click a fresh water source with an empty hand to take a sip (config waterSipRestore).
//  - Unpurified water (sips and plain water bottles) has a chance to give Hunger (config rawWaterSick*).
//    It also lowers your Water Purity (see WaterPurity).
//  - Salt water = water in an ocean or beach biome. You can't drink it or fill a glass bottle from it.
//    (Buckets still work, so it can be taken to the Purification Table.)
@EventBusSubscriber(modid = Capitalism.MODID)
public final class WaterDrinking {

    private static final int SIP_COOLDOWN_TICKS = 20;

    private WaterDrinking() {
    }

    // Minecraft has no salt water, so the biome decides: oceans (deep, warm, frozen...) and beaches are salty.
    // Rivers, lakes, ponds and swamps are fresh.
    public static boolean isSaltWater(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
    }

    // The water source the player is looking at, within reach, or null.
    public static @Nullable BlockPos lookedAtWater(Player player) {
        double reach = player.blockInteractionRange();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1f).scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        return player.level().getFluidState(pos).is(FluidTags.WATER) ? pos : null;
    }

    // Called from DrinkWaterPacket.
    public static void drinkFromSource(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty() || player.isSpectator()) return;
        BlockPos pos = lookedAtWater(player);
        if (pos == null) return;
        if (isSaltWater(player.level(), pos)) {
            player.sendOverlayMessage(Component.literal("That's salt water! You can't drink it."));
            return;
        }

        ThirstData data = player.getData(ModAttachments.THIRST.get());
        if (data.getThirstInt() >= data.thirstCap && !player.isCreative()) return;
        if (player.tickCount - data.lastSipTick < SIP_COOLDOWN_TICKS && data.lastSipTick <= player.tickCount) return;
        data.lastSipTick = player.tickCount;

        float sip = (float) CapitalismConfig.WATER_SIP_RESTORE.get().doubleValue();
        data.restore(sip);
        data.setPurity(WaterPurity.afterDrink(data.getPurity(), sip, WaterPurity.Source.RAW));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.5f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        maybeGetSick(player);
        PacketDistributor.sendToPlayer(player, ThirstSyncPacket.of(data, player));
    }

    // Unpurified water: a chance of Hunger for a while.
    public static void maybeGetSick(ServerPlayer player) {
        if (player.getRandom().nextFloat() < CapitalismConfig.RAW_WATER_SICK_CHANCE.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, CapitalismConfig.RAW_WATER_SICK_TICKS.get(), 0));
        }
    }

    // No filling glass bottles from the ocean.
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(Items.GLASS_BOTTLE)) return;
        Player player = event.getEntity();
        BlockPos pos = lookedAtWater(player);
        if (pos == null || !isSaltWater(player.level(), pos)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("That's salt water! Purify a bucket of it instead."));
        }
    }
}

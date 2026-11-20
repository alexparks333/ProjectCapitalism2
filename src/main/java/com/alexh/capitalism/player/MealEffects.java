package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.block.RiceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Special effects from Gourmet meals that vanilla doesn't have.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class MealEffects {

    // Vanilla lava swimming: each tick adds 0.02 of push in the direction you're moving, then halves your speed, so
    // top speed is twice the push. Adding 0.04 more triples the push, so Lava Swimmer is 3x as fast.
    private static final float LAVA_SWIM_EXTRA_PUSH = 0.04F;

    // Ender Leap: up to 8 blocks straight ahead, once a second, for half a hunger point (4 exhaustion = 1 point).
    public static final int ENDER_LEAP_COOLDOWN = 20;
    private static final double ENDER_LEAP_DISTANCE = 8;
    private static final float ENDER_LEAP_EXHAUSTION = 2.0F;

    // Green Thumb Aura: once a second, every crop / sapling / berry bush within 5 blocks (2 up or down) has a 1 in 4
    // chance to get a free bone meal. A wheat field next to you is ripe in about 10 seconds.
    private static final int GREEN_THUMB_RADIUS = 5;
    private static final float GREEN_THUMB_CHANCE = 0.25F;

    private MealEffects() {
    }

    // ── The second round of Specialty meals (Cook.md > Meal effects) ──
    // Timber (Lumberjack Flapjacks): an axe chops logs 25% faster
    @SubscribeEvent
    public static void onBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.TIMBER) || !event.getState().is(net.minecraft.tags.BlockTags.LOGS)) return;
        if (!player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES)) return;
        event.setNewSpeed(event.getNewSpeed() * 1.25F);
    }

    // Night Watch (Night Watch Hotpot): hostile mobs only notice you from half as far away
    @SubscribeEvent
    public static void onVisibility(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && player.hasEffect(ModEffects.NIGHT_WATCH)
            && event.getLookingEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            event.modifyVisibility(0.5);
        }
    }

    // Comfort Soup: cures the sickness from raw water (Hunger) and warms you back up to normal if you're cold
    @SubscribeEvent
    public static void onEat(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!event.getItem().is(com.alexh.capitalism.core.ModItems.COMFORT_SOUP.get())) return;
        player.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        WellbeingData wellbeing = player.getData(com.alexh.capitalism.core.ModAttachments.WELLBEING.get());
        if (wellbeing.bodyHeat < BodyTemperature.NORMAL) wellbeing.bodyHeat = BodyTemperature.NORMAL;
    }

    // Players move on their own client, so the boost runs there (only for your own player).
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() || !player.isLocalPlayer()) return;
        if (!player.isInLava() || player.getAbilities().flying || !player.hasEffect(ModEffects.LAVA_SWIMMER)) return;
        player.moveRelative(LAVA_SWIM_EXTRA_PUSH, new Vec3(player.xxa, 0, player.zza));
    }

    @SubscribeEvent
    public static void onServerPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (player.hasEffect(ModEffects.GREEN_THUMB) && !player.isSpectator()) greenThumb(player);
    }

    private static void greenThumb(ServerPlayer player) {
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-GREEN_THUMB_RADIUS, -2, -GREEN_THUMB_RADIUS),
            center.offset(GREEN_THUMB_RADIUS, 2, GREEN_THUMB_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!isPlant(state) || !(state.getBlock() instanceof BonemealableBlock plant)) continue;
            if (random.nextFloat() >= GREEN_THUMB_CHANCE) continue;
            if (!plant.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) continue;
            BlockPos at = pos.immutable();
            plant.performBonemeal(level, random, at, state, BonemealSource.INTERACTION);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
        }
    }

    // Only farm plants: crops, saplings, stems, berry bushes, cocoa, rice. (Not grass, which would sprout flowers.)
    private static boolean isPlant(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || block instanceof StemBlock
            || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock || block instanceof RiceBlock;
    }

    // Scholar's Focus: double the experience from every mob you kill.
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        Player killer = event.getAttackingPlayer();
        if (killer != null && killer.hasEffect(ModEffects.SCHOLARS_FOCUS)) event.setDroppedExperience(event.getDroppedExperience() * 2);
    }

    // Sneak + jump with Ender Leap: blink forward (the way you're facing, level), like a short Ender Pearl. Stops
    // before walls, can hop up one block, and lands you safely (no fall damage from the leap).
    public static void enderLeap(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.ENDER_LEAP) || player.isSpectator() || player.isPassenger()) return;
        ThirstData data = player.getData(ModAttachments.THIRST.get());
        long now = player.level().getGameTime();
        if (now - data.lastEnderLeap < ENDER_LEAP_COOLDOWN - 2) return;
        if (!player.isCreative() && player.getFoodData().getFoodLevel() <= 0) return;

        ServerLevel level = player.level();
        Vec3 dir = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(ENDER_LEAP_DISTANCE)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double max = hit.getType() == HitResult.Type.MISS ? ENDER_LEAP_DISTANCE : hit.getLocation().distanceTo(eye) - 0.6;

        for (double d = max; d >= 1; d -= 0.5) {
            for (int up = 0; up <= 1; up++) {
                AABB box = player.getBoundingBox().move(dir.x * d, up, dir.z * d);
                if (!level.noCollision(player, box)) continue;
                Vec3 from = player.position();
                Vec3 to = from.add(dir.x * d, up, dir.z * d);
                portalPuff(level, from);
                player.teleportTo(to.x, to.y, to.z);
                player.resetFallDistance();
                portalPuff(level, to);
                level.playSound(null, to.x, to.y, to.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.2F);
                if (!player.isCreative()) player.causeFoodExhaustion(ENDER_LEAP_EXHAUSTION);
                data.lastEnderLeap = now;
                return;
            }
        }
    }

    private static void portalPuff(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 24, 0.3, 0.6, 0.3, 0.2);
    }
}

package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.network.TreasureSightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import com.mojang.logging.LogUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The special parts of the Bartender's cocktails (the plain potion effects come from the drink itself, ModItems):
//  - Glowberry Gold Rush: Luminescent Aura, an invisible light block that follows your head.
//  - Nether Fireball Shot: Magma Trail, flames where you walked (5 s) that burn hostile mobs and other players.
//  - Ender Tele-Tini: Phase Step, back to where you stood 10 seconds ago.
//  - Pirate's High Seas Grog: Treasure Sight, buried-treasure / shipwreck chests within 48 blocks glow (found while
//    still unopened, then tagged so they keep glowing after opening, until broken)
//    through walls (the glow itself is client/TreasureSightClient).
//  - Miner's Moonshine: Fortune Boost, 1 in 3 chance per ore drop of one extra.
//  - Vampiric Mulled Wine: Life Siphon, melee hits on hostile mobs heal you for 25% of the damage.
//  - Overworld Mojito: clears every bad effect.
//  - Phantom's Nightcap: resets "time since rest" (no phantoms), and phantoms can't target you.
//  - Zombie's Hangover Cure: Iron Stomach, no Hunger effect (and Cooks can eat raw food, see JobEvents).
//  - Ender Twilight Fizz: Spatial Dislocation, a hit of 3+ hearts (or one that would kill you) teleports you up to 8
//    blocks away like a Chorus Fruit; a killing blow is cancelled. Once every 2 seconds at most.
//  - Drunken Brawler's Stout: Berserker's Rage, no knockback (the effect's attribute, ModEffects); Strength II and
//    Nausea come with the drink.
//  - Dolphin's Grace (Pirate's High Seas Grog): skimming the water's surface doesn't get you kicked for flying.
// All of it is server-side and kept in memory only (nothing saved).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class DrinkEffects {

    private static final int PHASE_STEP_SECONDS = 10;
    private static final int TRAIL_TICKS = 100;
    private static final double TRAIL_SPACING = 0.7;
    private static final float TRAIL_DAMAGE = 1.0F;
    private static final int TREASURE_RADIUS_CHUNKS = 3;
    private static final float FORTUNE_CHANCE = 1F / 3F;
    private static final float SIPHON_SHARE = 0.25F;

    private record Spot(ResourceKey<Level> dimension, Vec3 pos) {
    }

    private record Light(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Ember(Vec3 pos, long until) {
    }

    // where each player stood, once a second, for the last 10 seconds (oldest first)
    private static final Map<UUID, Deque<Spot>> HISTORY = new HashMap<>();
    private static final Map<UUID, Light> LIGHTS = new HashMap<>();
    private static final Map<UUID, Deque<Ember>> TRAILS = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> TREASURE = new HashMap<>();

    private DrinkEffects() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        forgiveSkimming(player);
        if (player.tickCount % 20 == 0) rememberSpot(player);
        if (player.tickCount % 2 == 0) updateLight(player);
        if (player.tickCount % 4 == 0) updateTrail(player, now);
        if (player.tickCount % 40 == 0) updateTreasure(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        removeLight(player);
        UUID id = player.getUUID();
        HISTORY.remove(id);
        TRAILS.remove(id);
        TREASURE.remove(id);
    }

    // ── Dolphin's Grace skimming ───────────────────────────────────────────
    // Sprinting along the surface with Dolphin's Grace (Pirate's High Seas Grog) bounces you a little above the
    // water. Vanilla's anti-fly check sees only air under your feet for those moments, and after 4 seconds of it the
    // server kicks you for "flying". So while you have Dolphin's Grace and there's water right below you (within 3
    // blocks), the server's floating counter is reset every tick. Real flying anywhere else is still caught.

    private static final int SKIM_DEPTH = 3;
    private static Field aboveGroundTicks;
    private static boolean aboveGroundTicksBroken;

    private static void forgiveSkimming(ServerPlayer player) {
        if (aboveGroundTicksBroken || !player.hasEffect(MobEffects.DOLPHINS_GRACE)) return;
        BlockPos feet = player.blockPosition();
        boolean waterBelow = false;
        for (int down = 0; down <= SKIM_DEPTH && !waterBelow; down++) {
            waterBelow = player.level().getFluidState(feet.below(down)).is(FluidTags.WATER);
        }
        if (!waterBelow) return;
        try {
            if (aboveGroundTicks == null) {
                aboveGroundTicks = ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                aboveGroundTicks.setAccessible(true);
            }
            aboveGroundTicks.setInt(player.connection, 0);
        } catch (ReflectiveOperationException | RuntimeException broken) {
            aboveGroundTicksBroken = true;
            LogUtils.getLogger().warn("[{}] Can't stop Dolphin's Grace fly kicks: {}", Capitalism.MODID, broken.toString());
        }
    }

    // ── What happens the moment you finish a drink ─────────────────────────

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack drink = event.getItem();
        if (drink.is(ModItems.ENDER_TELE_TINI.get())) {
            phaseStep(player);
        } else if (drink.is(ModItems.OVERWORLD_MOJITO.get())) {
            for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) player.removeEffect(effect.getEffect());
            }
        } else if (drink.is(ModItems.PHANTOMS_NIGHTCAP.get())) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        } else if (drink.is(ModItems.ZOMBIES_HANGOVER_CURE.get())) {
            player.removeEffect(MobEffects.HUNGER);
        }
    }

    // ── Phase Step (Ender Tele-Tini) ───────────────────────────────────────

    private static void rememberSpot(ServerPlayer player) {
        Deque<Spot> history = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        history.addLast(new Spot(player.level().dimension(), player.position()));
        while (history.size() > PHASE_STEP_SECONDS + 1) history.removeFirst();
    }

    private static void phaseStep(ServerPlayer player) {
        Deque<Spot> history = HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) return;
        Spot back = history.peekFirst();
        if (!back.dimension().equals(player.level().dimension())) return;
        ServerLevel level = player.level();
        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 32, 0.3, 0.6, 0.3, 0.3);
        player.teleportTo(back.pos().x, back.pos().y, back.pos().z);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, back.pos().x, back.pos().y + 1, back.pos().z, 32, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, back.pos().x, back.pos().y, back.pos().z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        history.clear();
    }

    // ── Luminescent Aura (Glowberry Gold Rush) ─────────────────────────────

    private static void updateLight(ServerPlayer player) {
        Light old = LIGHTS.get(player.getUUID());
        Light wanted = null;
        // Luminescent Aura, or a Blaze Powder-socketed sword / pickaxe in hand (weaponsmith/SocketEffects)
        if ((player.hasEffect(ModEffects.LUMINESCENT_AURA) || com.alexh.capitalism.weaponsmith.SocketEffects.holdsLight(player)) && !player.isSpectator()) {
            BlockPos spot = lightSpot(player.level(), BlockPos.containing(player.getEyePosition()));
            if (spot == null) spot = lightSpot(player.level(), player.blockPosition());
            if (spot != null) wanted = new Light(player.level().dimension(), spot);
            else if (old != null && old.dimension().equals(player.level().dimension())) wanted = old;   // nowhere free: keep the old one
        }
        if (wanted != null && wanted.equals(old)) return;
        removeLight(player);
        if (wanted == null) return;
        ServerLevel level = player.level();
        boolean water = level.getFluidState(wanted.pos()).is(Fluids.WATER);
        level.setBlock(wanted.pos(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15)
            .setValue(BlockStateProperties.WATERLOGGED, water), Block.UPDATE_ALL);
        LIGHTS.put(player.getUUID(), wanted);
    }

    // Air, or a still water source (a waterlogged light).
    private static @Nullable BlockPos lightSpot(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return pos;
        if (state.is(Blocks.WATER) && level.getFluidState(pos).isSource()) return pos;
        return null;
    }

    private static void removeLight(ServerPlayer player) {
        Light light = LIGHTS.remove(player.getUUID());
        if (light == null) return;
        ServerLevel level = player.level().getServer().getLevel(light.dimension());
        if (level == null) return;
        BlockState state = level.getBlockState(light.pos());
        if (!state.is(Blocks.LIGHT)) return;   // something else is there now
        level.setBlock(light.pos(), state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState()
            : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ── Magma Trail (Nether Fireball Shot) ─────────────────────────────────

    private static void updateTrail(ServerPlayer player, long now) {
        Deque<Ember> trail = TRAILS.get(player.getUUID());
        if (player.hasEffect(ModEffects.MAGMA_TRAIL) && player.onGround() && !player.isSpectator()) {
            if (trail == null) {
                trail = new ArrayDeque<>();
                TRAILS.put(player.getUUID(), trail);
            }
            Ember last = trail.peekLast();
            if (last == null || last.pos().distanceTo(player.position()) > TRAIL_SPACING) {
                trail.addLast(new Ember(player.position(), now + TRAIL_TICKS));
            }
        }
        if (trail == null) return;
        while (!trail.isEmpty() && trail.peekFirst().until() < now) trail.removeFirst();
        if (trail.isEmpty()) {
            TRAILS.remove(player.getUUID());
            return;
        }
        ServerLevel level = player.level();
        // (Checked every update, i.e. every 4 ticks. It used to wait for the world clock to hit a multiple of 12,
        // which could never line up with the player's 4-tick updates, so nothing ever burned.)
        for (Ember ember : trail) {
            Vec3 p = ember.pos();
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.1, p.z, 1, 0.2, 0.02, 0.2, 0.005);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(0.6, 0.5, 0.6),
                target -> burnsInTrail(target, player))) {
                victim.igniteForSeconds(4);
                victim.hurtServer(level, level.damageSources().inFire(), TRAIL_DAMAGE);
            }
        }
    }

    // Hostile mobs and other players (not the one leaving the trail, not creative / spectators).
    private static boolean burnsInTrail(LivingEntity target, ServerPlayer owner) {
        if (target == owner) return false;
        if (target instanceof Player other) return !other.isCreative() && !other.isSpectator();
        return target instanceof Enemy;
    }

    // ── Treasure Sight (Pirate's High Seas Grog) ───────────────────────────

    private static void updateTreasure(ServerPlayer player) {
        List<BlockPos> found = player.hasEffect(ModEffects.TREASURE_SIGHT) ? findTreasure(player) : List.of();
        List<BlockPos> sent = TREASURE.getOrDefault(player.getUUID(), List.of());
        if (found.equals(sent)) return;
        if (found.isEmpty()) TREASURE.remove(player.getUUID());
        else TREASURE.put(player.getUUID(), found);
        PacketDistributor.sendToPlayer(player, new TreasureSightPacket(found));
    }

    // Unopened loot chests (or barrels) that are part of a shipwreck or buried treasure, in loaded chunks nearby.
    private static List<BlockPos> findTreasure(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> found = new ArrayList<>();
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        for (int x = cx - TREASURE_RADIUS_CHUNKS; x <= cx + TREASURE_RADIUS_CHUNKS; x++) {
            for (int z = cz - TREASURE_RADIUS_CHUNKS; z <= cz + TREASURE_RADIUS_CHUNKS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) continue;
                    BlockPos pos = blockEntity.getBlockPos();
                    // Already known treasure: keeps glowing after it's opened, until the chest is broken (the tag goes
                    // with the block entity).
                    if (blockEntity.getData(ModAttachments.TREASURE_CHEST.get())) {
                        found.add(pos.immutable());
                        continue;
                    }
                    if (container.getLootTable() == null) continue;   // opened before anyone saw it with the effect
                    if (level.structureManager().getStructureWithPieceAt(pos, StructureTags.SHIPWRECK).isValid()
                        || level.structureManager().getStructureWithPieceAt(pos, StructureTags.ON_TREASURE_MAPS).isValid()) {
                        blockEntity.setData(ModAttachments.TREASURE_CHEST.get(), true);
                        blockEntity.setChanged();
                        found.add(pos.immutable());
                    }
                }
            }
        }
        return found;
    }

    // ── Fortune Boost (Miner's Moonshine) ──────────────────────────────────

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player) || !player.hasEffect(ModEffects.FORTUNE_BOOST)) return;
        if (!event.getState().is(Tags.Blocks.ORES)) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(event.getState().getBlock().asItem())) continue;   // silk touch: the ore block itself
            if (event.getLevel().getRandom().nextFloat() < FORTUNE_CHANCE) {
                ItemStack more = stack.copy();
                more.grow(1);
                drop.setItem(more);
            }
        }
    }

    // ── Life Siphon (Vampiric Mulled Wine) ─────────────────────────────────

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Enemy)) return;
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Player player) || event.getSource().getEntity() != player) return;   // melee only
        if (!player.hasEffect(ModEffects.LIFE_SIPHON) || event.getHealthDamage() <= 0) return;
        player.heal(event.getHealthDamage() * SIPHON_SHARE);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, event.getEntity().getX(), event.getEntity().getY(0.6), event.getEntity().getZ(),
                3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    // ── Spatial Dislocation (Ender Twilight Fizz) ──────────────────────────

    private static final float HEAVY_HIT = 6.0F;
    private static final int DISLOCATION_COOLDOWN = 40;
    private static final Map<UUID, Long> LAST_DISLOCATION = new HashMap<>();

    @SubscribeEvent
    public static void onIncomingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasEffect(ModEffects.SPATIAL_DISLOCATION)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;   // the void, /kill
        float damage = event.getNewDamage();
        boolean fatal = damage >= player.getHealth();
        if (!fatal && damage < HEAVY_HIT) return;
        long now = player.level().getGameTime();
        Long last = LAST_DISLOCATION.get(player.getUUID());
        if (last != null && now - last < DISLOCATION_COOLDOWN) return;
        if (!dislocate(player)) return;
        LAST_DISLOCATION.put(player.getUUID(), now);
        if (fatal) event.setNewDamage(0);
    }

    // Like eating a Chorus Fruit: up to 16 tries at a safe random spot within 8 blocks.
    private static boolean dislocate(ServerPlayer player) {
        ServerLevel level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < 16; i++) {
            double tx = x + (player.getRandom().nextDouble() - 0.5) * 16;
            double ty = Math.clamp(y + (player.getRandom().nextInt(16) - 8), level.getMinY(), level.getMinY() + level.getHeight() - 1);
            double tz = z + (player.getRandom().nextDouble() - 0.5) * 16;
            if (player.randomTeleport(tx, ty, tz, true, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) {
                level.playSound(null, x, y, z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.resetFallDistance();
                return true;
            }
        }
        return false;
    }

    // ── Phantom Camouflage (Phantom's Nightcap) ────────────────────────────

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Phantom && event.getNewAboutToBeSetTarget() instanceof Player player
            && (player.hasEffect(ModEffects.PHANTOM_CAMOUFLAGE) || com.alexh.capitalism.librarian.Skills.has(player, com.alexh.capitalism.librarian.Skill.NIGHT_OWL))) {
            event.setCanceled(true);
        }
    }

    // ── Iron Stomach (Zombie's Hangover Cure) ──────────────────────────────

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (effect.is(MobEffects.HUNGER) && (event.getEntity().hasEffect(ModEffects.IRON_STOMACH)
            || com.alexh.capitalism.librarian.Skills.has(event.getEntity(), com.alexh.capitalism.librarian.Skill.IRON_STOMACH))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}

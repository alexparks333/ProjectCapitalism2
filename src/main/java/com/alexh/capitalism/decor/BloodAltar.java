package com.alexh.capitalism.decor;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.event.BloodMoon;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// The Blood Moon Altar (Decorations.md > Blood Moon decorations, Events.md): players call a Blood Moon down by feeding
// the altars Blood Shards. Server-wide: every altar adds to the same day's pool, and the cost climbs with every Blood Moon
// called this week: 150 shards for the first, then 300, 450, 600... (COST more each time), back to 150 every Monday
// morning. Offerings are only taken by day (and not while a Blood Moon is on or already coming); each one shows the
// pool ("87 / 150") and gives its giver a few seconds of blood-red sky (client/BloodMoonClient.vision). When the pool
// reaches the cost a Blood Moon rises at dusk that night (BloodMoon.scheduleTonight), announced to everyone. If it
// isn't reached by dusk, everyone gets their shards back: spat out of the altar they gave them to if they're near it,
// else into their inventory (dropped at their feet if it's full), or at their next login if they're away.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BloodAltar {

    public static final int COST = 150;
    // a giver this near their altar at dusk gets their shards back out of it
    static final double NEAR = 24.0;
    // the red sky each offering shows its giver (ticks)
    static final int VISION = 100;

    private BloodAltar() {
    }

    public static final class Data extends SavedData {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("pool", 0).forGetter(d -> d.pool),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("given", Map.of()).forGetter(d -> d.given),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC).optionalFieldOf("where", Map.of()).forGetter(d -> d.where),
            Codec.INT.optionalFieldOf("uses", 0).forGetter(d -> d.uses),
            Codec.LONG.optionalFieldOf("reset_day", -1L).forGetter(d -> d.resetDay),
            Codec.LONG.optionalFieldOf("pool_day", -1L).forGetter(d -> d.poolDay),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("owed", Map.of()).forGetter(d -> d.owed)
        ).apply(i, Data::new));
        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_altar"), Data::new, CODEC);

        // today's pool, who gave how much and at which altar
        int pool;
        final Map<UUID, Integer> given = new LinkedHashMap<>();
        final Map<UUID, BlockPos> where = new HashMap<>();
        // Blood Moons called this week; the Monday it last went back to the start; the day the pool belongs to
        int uses;
        long resetDay = -1;
        long poolDay = -1;
        // shards to hand back to givers who were away at dusk
        final Map<UUID, Integer> owed = new HashMap<>();

        public Data() {
        }

        private Data(int pool, Map<UUID, Integer> given, Map<UUID, BlockPos> where, int uses, long resetDay, long poolDay, Map<UUID, Integer> owed) {
            this.pool = pool;
            this.given.putAll(given);
            this.where.putAll(where);
            this.uses = uses;
            this.resetDay = resetDay;
            this.poolDay = poolDay;
            this.owed.putAll(owed);
        }

        // what the next Blood Moon costs
        public int cost() {
            return COST * (this.uses + 1);
        }

        public int pool() {
            return this.pool;
        }

        public int uses() {
            return this.uses;
        }

        public int owedTo(UUID player) {
            return this.owed.getOrDefault(player, 0);
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
    }

    static long timeOfDay(ServerLevel level) {
        return Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
    }

    // Offers shards from `stack` at the altar at `pos`: as many as the cost still needs. How many were taken.
    public static int offer(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        MinecraftServer server = level.getServer();
        Data data = data(server);
        settle(server);
        String refusal = BloodMoon.isActive() ? "The Blood Moon is already upon us."
            : BloodMoon.ticksUntil(server) >= 0 ? "A Blood Moon already rises tonight."
            : !Calendar.isDay(Calendar.clock(level)) ? "The altar only takes offerings by day." : null;
        if (refusal != null) {
            player.sendOverlayMessage(Component.literal(refusal).withStyle(ChatFormatting.DARK_RED));
            return 0;
        }
        int take = Math.min(stack.getCount(), data.cost() - data.pool);
        if (take <= 0) return 0;
        stack.consume(take, player);
        data.pool += take;
        data.poolDay = Calendar.date(level).index();
        data.given.merge(player.getUUID(), take, Integer::sum);
        data.where.put(player.getUUID(), pos.immutable());
        data.setDirty();
        // the giver's own glimpse of the blood moon, the shards burning away
        PacketDistributor.sendToPlayer(player, new com.alexh.capitalism.network.AltarVisionPacket(VISION));
        level.sendParticles(new DustParticleOptions(0xE01020, 1.4F), pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 20 + take / 4, 0.5, 0.4, 0.5, 0.02);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (data.pool >= data.cost()) call(server, player);
        else player.sendOverlayMessage(progress(data));
        return take;
    }

    static Component progress(Data data) {
        return Component.literal("🩸 " + data.pool + " / " + data.cost() + " Blood Shards offered").withStyle(ChatFormatting.DARK_RED)
            .append(Component.literal("  (given back at dusk if it isn't reached)").withStyle(ChatFormatting.GRAY));
    }

    // what the altar shows when it's right-clicked empty-handed
    public static Component status(ServerLevel level) {
        MinecraftServer server = level.getServer();
        Data data = data(server);
        if (BloodMoon.isActive()) return Component.literal("The Blood Moon is upon us.").withStyle(ChatFormatting.DARK_RED);
        if (BloodMoon.ticksUntil(server) >= 0) return Component.literal("A Blood Moon rises tonight.").withStyle(ChatFormatting.DARK_RED);
        if (!Calendar.isDay(Calendar.clock(level))) return Component.literal("The altar sleeps by night. Bring Blood Shards by day.").withStyle(ChatFormatting.GRAY);
        return Component.literal("🩸 " + data.pool + " / " + data.cost() + " Blood Shards to call a Blood Moon tonight.").withStyle(ChatFormatting.DARK_RED);
    }

    // the pool is full: a Blood Moon tonight, told to everyone; the next one this week costs more
    static void call(MinecraftServer server, ServerPlayer by) {
        Data data = data(server);
        if (!BloodMoon.scheduleTonight(server)) return;
        int paid = data.pool;
        data.uses++;
        data.pool = 0;
        data.given.clear();
        data.where.clear();
        data.setDirty();
        TextColor red = TextColor.fromRgb(0xFF3030);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("🩸 The altar drinks its fill of " + paid + " Blood Shards... ").withStyle(s -> s.withColor(red).withBold(true))
                .append(Component.literal(by.getGameProfile().name() + " and the others have called it down: the moon will rise RED tonight!")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(0xE8B0B0)).withBold(false))));
            PacketDistributor.sendToPlayer(p, new com.alexh.capitalism.network.DayNoticePacket("The Altar Answers", 0xFF3030,
                "A Blood Moon rises tonight", "Next one this week: " + data.cost() + " shards", 0xE8B0B0));
            p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.5F, 0.6F);
        }
    }

    // ── every second: Monday morning's reset, dusk's refunds ──

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7) return;
        settle(server);
    }

    // Monday morning: back to the first price. Dusk (or a pool left over from an earlier day): everyone's shards back.
    public static void settle(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Data data = data(server);
        Calendar.Date today = Calendar.date(level);
        if (today.weekday() == Calendar.Weekday.MONDAY && data.resetDay != today.index()) {
            data.uses = 0;
            data.resetDay = today.index();
            data.setDirty();
        }
        if (data.pool > 0 && (data.poolDay != today.index() || timeOfDay(level) >= 13000)) refundAll(server);
    }

    // the day's offering fell short: each giver gets theirs back
    public static void refundAll(MinecraftServer server) {
        Data data = data(server);
        for (Map.Entry<UUID, Integer> e : data.given.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            BlockPos altar = data.where.get(e.getKey());
            if (player == null) {
                data.owed.merge(e.getKey(), e.getValue(), Integer::sum);
                continue;
            }
            giveBack(player, e.getValue(), altar);
            player.sendSystemMessage(Component.literal("🩸 The altar wasn't fed enough by dusk: it gives back your " + e.getValue() + " Blood Shards.")
                .withStyle(ChatFormatting.DARK_RED));
        }
        data.pool = 0;
        data.given.clear();
        data.where.clear();
        data.setDirty();
    }

    // shards back to a player: out of the altar if they're near it, else into their inventory (or at their feet)
    static void giveBack(ServerPlayer player, int count, BlockPos altar) {
        ServerLevel level = player.level();
        boolean near = altar != null && level == level.getServer().overworld() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(altar)) < NEAR * NEAR;
        while (count > 0) {
            int n = Math.min(64, count);
            count -= n;
            ItemStack shards = new ItemStack(ModItems.BLOOD_SHARD.get(), n);
            if (near) {
                ItemEntity spat = new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, shards,
                    (level.getRandom().nextDouble() - 0.5) * 0.2, 0.3, (level.getRandom().nextDouble() - 0.5) * 0.2);
                spat.setPickUpDelay(10);
                level.addFreshEntity(spat);
            } else if (!player.getInventory().add(shards)) {
                player.drop(shards, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        if (near) {
            level.playSound(null, altar, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.SMOKE, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
        }
    }

    // back from being away: any shards owed
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Data data = data(player.level().getServer());
        Integer owed = data.owed.remove(player.getUUID());
        if (owed == null || owed <= 0) return;
        data.setDirty();
        giveBack(player, owed, null);
        player.sendSystemMessage(Component.literal("🩸 While you were away the altar gave back your " + owed + " Blood Shards.").withStyle(ChatFormatting.DARK_RED));
    }
}

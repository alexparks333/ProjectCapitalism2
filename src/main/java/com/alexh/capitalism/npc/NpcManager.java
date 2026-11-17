package com.alexh.capitalism.npc;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

// Job NPCs fill in for missing players (NPCs.md). Every CHECK_TICKS (30 s), for every spot an admin set:
//  - no player with that job online: its NPC should be standing at the spot;
//  - a player with that job is online: the NPC stays LEAVE_AFTER_TICKS (10 minutes) from when they came online, then
//    leaves; it comes back once the last of them logs out. EXCEPT on the days it comes to town anyway: each job's NPC
//    has its own 20% chance every day (inTownToday) of staying all day, competing with the players.
// Only spots in loaded chunks are touched (an NPC in an unloaded chunk is dealt with when the chunk loads, and the NPC
// also checks itself, see JobNpc.tick). Admin commands: /npc setspot <job>, /npc removespot <job>, /npc list.
// The Boat Master (BoatMaster, Boats.md) uses the same spots and commands (/npc setspot boat_master) but is always there.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class NpcManager {

    // 30 seconds: NPCs don't need to react faster (setspot checks straight away)
    private static final int CHECK_TICKS = 600;
    public static final int LEAVE_AFTER_TICKS = 10 * 60 * 20;
    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    // When a player with each job came online (server tick); not saved: a restart starts the 10 minutes over
    private static final Map<Job, Long> ONLINE_SINCE = new EnumMap<>(Job.class);

    private NpcManager() {
    }

    // ── Who should be standing where ──

    private static boolean jobOnline(MinecraftServer server, Job job) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (JobEvents.has(player, job)) return true;
        }
        return false;
    }

    // Should this job's NPC be at its spot right now? Always when no one with the job is online (or for the first
    // LEAVE_AFTER_TICKS after they came on); otherwise only on the days it came to town anyway (inTownToday).
    private static boolean shouldBePresent(MinecraftServer server, Job job) {
        Long since = ONLINE_SINCE.get(job);
        return since == null || server.getTickCount() - since < LEAVE_AFTER_TICKS || inTownToday(server, job);
    }

    // Each job's NPC has its own DAILY_CHANCE (20%) of coming to town for the whole day even while players with its
    // job are online: a bit of competition for them. The same answer all day (seeded by the world, the day and the job).
    public static final float DAILY_CHANCE = 0.2F;

    public static boolean inTownToday(MinecraftServer server, Job job) {
        long day = com.alexh.capitalism.calendar.Calendar.date(server.overworld()).index();
        return inTownOn(server.overworld().getSeed(), day, job);
    }

    public static boolean inTownOn(long worldSeed, long day, Job job) {
        long seed = worldSeed ^ (day * 0x9E3779B97F4A7C15L) ^ ((long) (job.ordinal() + 1) * 0xC2B2AE3D27D4EB4FL);
        return new java.util.Random(seed).nextFloat() < DAILY_CHANCE;
    }

    // Asked by each NPC now and then: is it still the one for its job's spot?
    static boolean isCurrent(JobNpc npc) {
        MinecraftServer server = npc.level().getServer();
        Job job = npc.job();
        if (server == null || job == null) return false;
        NpcSpots.Spot spot = NpcSpots.get(server).get(job.id());
        return spot != null && spot.npc().map(npc.getUUID()::equals).orElse(false) && shouldBePresent(server, job);
    }

    // The Boat Master (BoatMaster): is he the one for his spot? (He's always there while it's set.)
    static boolean isCurrentBoatMaster(BoatMaster npc) {
        MinecraftServer server = npc.level().getServer();
        if (server == null) return false;
        NpcSpots.Spot spot = NpcSpots.get(server).get(NpcShops.BOAT_MASTER);
        return spot != null && spot.npc().map(npc.getUUID()::equals).orElse(false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS == 0) checkAll(server);
    }

    private static void checkAll(MinecraftServer server) {
        for (Job job : Job.values()) {
            if (jobOnline(server, job)) {
                ONLINE_SINCE.putIfAbsent(job, (long) server.getTickCount());
            } else {
                ONLINE_SINCE.remove(job);
            }
        }
        NpcSpots spots = NpcSpots.get(server);
        for (NpcSpots.Spot spot : spots.list()) {
            if (NpcShops.BOAT_MASTER.equals(spot.job())) {
                checkBoatMaster(server, spots, spot);
                continue;
            }
            Job job = Job.byId(spot.job());
            ServerLevel level = server.getLevel(spot.pos().dimension());
            if (job == null || level == null || !level.isLoaded(spot.pos().pos())) continue;
            @Nullable JobNpc current = spot.npc().map(level::getEntity).filter(e -> e instanceof JobNpc).map(e -> (JobNpc) e).orElse(null);
            if (shouldBePresent(server, job)) {
                if (current == null || !current.isAlive()) spawn(level, spots, spot, job);
            } else if (current != null) {
                current.leave();
                spots.setNpc(job.id(), null);
            }
        }
    }

    // The Boat Master: always at his spot (in a loaded chunk)
    private static void checkBoatMaster(MinecraftServer server, NpcSpots spots, NpcSpots.Spot spot) {
        ServerLevel level = server.getLevel(spot.pos().dimension());
        if (level == null || !level.isLoaded(spot.pos().pos())) return;
        Entity current = spot.npc().map(level::getEntity).orElse(null);
        if (current instanceof BoatMaster npc && npc.isAlive()) return;
        BlockPos pos = spot.pos().pos();
        for (BoatMaster stray : level.getEntitiesOfClass(BoatMaster.class, new AABB(pos).inflate(8))) stray.discard();
        BoatMaster npc = ModEntities.BOAT_MASTER.get().create(level, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.setUp();
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, spot.yaw(), 0.0F);
        npc.setYHeadRot(spot.yaw());
        npc.setYBodyRot(spot.yaw());
        level.addFreshEntity(npc);
        spots.setNpc(NpcShops.BOAT_MASTER, npc.getUUID());
    }

    private static void spawn(ServerLevel level, NpcSpots spots, NpcSpots.Spot spot, Job job) {
        BlockPos pos = spot.pos().pos();
        // no stray copies of this job's NPC around the spot
        for (JobNpc stray : level.getEntitiesOfClass(JobNpc.class, new AABB(pos).inflate(8), npc -> npc.job() == job)) {
            stray.discard();
        }
        JobNpc npc = ModEntities.JOB_NPC.get().create(level, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.setJob(job);
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, spot.yaw(), 0.0F);
        npc.setYHeadRot(spot.yaw());
        npc.setYBodyRot(spot.yaw());
        level.addFreshEntity(npc);
        spots.setNpc(job.id(), npc.getUUID());
    }

    // ── Admin commands ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("setspot").then(Commands.argument("job", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                    java.util.stream.Stream.concat(Arrays.stream(Job.values()).map(Job::id), java.util.stream.Stream.of(NpcShops.BOAT_MASTER)), builder))
                .executes(NpcManager::setSpot)))
            .then(Commands.literal("removespot").then(Commands.argument("job", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                    NpcSpots.get(context.getSource().getServer()).list().stream().map(NpcSpots.Spot::job), builder))
                .executes(NpcManager::removeSpot)))
            .then(Commands.literal("list").executes(NpcManager::list)));
    }

    private static @Nullable Job jobArgument(CommandContext<CommandSourceStack> context) {
        Job job = Job.byId(StringArgumentType.getString(context, "job"));
        if (job == null) context.getSource().sendFailure(Component.literal("No job called \"" + StringArgumentType.getString(context, "job") + "\"."));
        return job;
    }

    // Where you stand, facing the way you face
    private static int setSpot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (NpcShops.BOAT_MASTER.equals(StringArgumentType.getString(context, "job"))) {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            NpcSpots spots = NpcSpots.get(server);
            removeNpc(server, spots.get(NpcShops.BOAT_MASTER));
            spots.set(NpcShops.BOAT_MASTER, GlobalPos.of(player.level().dimension(), player.blockPosition()), player.getYRot());
            checkAll(server);
            context.getSource().sendSuccess(() -> Component.literal("The Boat Master will stand here (always open).").withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        Job job = jobArgument(context);
        if (job == null) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        NpcSpots spots = NpcSpots.get(server);
        removeNpc(server, spots.get(job.id()));
        spots.set(job.id(), GlobalPos.of(player.level().dimension(), player.blockPosition()), player.getYRot());
        checkAll(server);
        context.getSource().sendSuccess(() -> Component.literal("The " + job.title + " NPC will stand here (it shows up whenever no "
            + job.title + " is online).").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeSpot(CommandContext<CommandSourceStack> context) {
        if (NpcShops.BOAT_MASTER.equals(StringArgumentType.getString(context, "job"))) {
            MinecraftServer server = context.getSource().getServer();
            NpcSpots.Spot removed = NpcSpots.get(server).remove(NpcShops.BOAT_MASTER);
            if (removed == null) {
                context.getSource().sendFailure(Component.literal("The Boat Master has no spot."));
                return 0;
            }
            removeNpc(server, removed);
            context.getSource().sendSuccess(() -> Component.literal("Removed the Boat Master and his spot.").withStyle(ChatFormatting.YELLOW), true);
            return 1;
        }
        Job job = jobArgument(context);
        if (job == null) return 0;
        MinecraftServer server = context.getSource().getServer();
        NpcSpots.Spot removed = NpcSpots.get(server).remove(job.id());
        if (removed == null) {
            context.getSource().sendFailure(Component.literal("The " + job.title + " has no NPC spot."));
            return 0;
        }
        removeNpc(server, removed);
        context.getSource().sendSuccess(() -> Component.literal("Removed the " + job.title + " NPC and its spot.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        var all = NpcSpots.get(server).list();
        if (all.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No NPC spots yet. Stand somewhere and use /npc setspot <job>."), false);
            return 0;
        }
        for (NpcSpots.Spot spot : all) {
            BlockPos pos = spot.pos().pos();
            if (NpcShops.BOAT_MASTER.equals(spot.job())) {
                context.getSource().sendSuccess(() -> Component.literal("Boat Master: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                    + " in " + spot.pos().dimension().identifier() + ", always open"), false);
                continue;
            }
            Job job = Job.byId(spot.job());
            if (job == null) continue;
            String status = !jobOnline(server, job) ? "open (no " + job.title + " online)"
                : inTownToday(server, job) ? "open (in town today, a " + job.title + " is online too)"
                : shouldBePresent(server, job) ? "leaving soon (a " + job.title + " is online)" : "away (a " + job.title + " is online)";
            context.getSource().sendSuccess(() -> Component.literal(job.title + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " in " + spot.pos().dimension().identifier() + ", " + status), false);
        }
        return all.size();
    }

    private static void removeNpc(MinecraftServer server, NpcSpots.@Nullable Spot spot) {
        if (spot == null || spot.npc().isEmpty()) return;
        ServerLevel level = server.getLevel(spot.pos().dimension());
        UUID id = spot.npc().get();
        Entity entity = level == null ? null : level.getEntity(id);
        if (entity instanceof JobNpc npc) npc.leave(); // (the Boat Master is a JobNpc too)
    }
}

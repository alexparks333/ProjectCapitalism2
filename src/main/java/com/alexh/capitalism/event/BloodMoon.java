package com.alexh.capitalism.event;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.TougherMobs;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.network.BloodMoonPacket;
import com.alexh.capitalism.network.DayNoticePacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// The Blood Moon (Events.md): a night where the monsters are far worse, everyone's kills are counted, and a boss (one of
// three, Boss: CrimsonRevenant, Gorefang, SanguineWitch, all BloodMoonBoss) hides out past the town for the brave to hunt.
// - Started by an op: /event bloodmoon start (now: if it's day it turns to nightfall), /event bloodmoon delay <time>
//   (in that long; again to push it back), /event bloodmoon end (now). It ends by itself at dawn.
// - Monsters that spawn in the overworld during it: 20% Level I, 60% Level II, 20% Boss (core/TougherMobs), and every
//   monster gives double XP. Nobody can sleep through it. Dying is dying.
// - Points for each kill (Level I 1, Level II 2, Boss 4, the Revenant 35) on the sidebar ("Blood Moon Kills").
// - At dawn the top hunter gets 25 gold (handed over at their next login if they're off). The Revenant's killer gets
//   its 15 gold drop. Monsters drop Blood Shards.
// - Looks: a red moon and a red sky and fog (client/BloodMoonClient, mixin/SkyRendererMixin), a red bar at the top.
@EventBusSubscriber(modid = Capitalism.MODID)
public class BloodMoon {

    public static final int POINTS_ONE = 1;
    public static final int POINTS_TWO = 2;
    public static final int POINTS_BOSS = 4;
    public static final int POINTS_REVENANT = 35;
    public static final int WINNER_GOLD = 25;
    public static final double LEVEL_TWO_CHANCE = 0.60;
    public static final double BOSS_CHANCE = 0.20;
    // the Revenant hides this far out from the town's middle
    static final int HIDE_MIN = 150;
    static final int HIDE_MAX = 400;
    // the red pillar over it shows within this far; its heartbeat within this
    static final double PILLAR_RANGE = 100.0;
    static final double HEARTBEAT_RANGE = 56.0;
    static final int RED = 0xC81E1E;

    private static final String OBJECTIVE = "blood_moon";
    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    // on the server thread: is one on right now (mirrors the saved data, for the quick checks)
    private static volatile boolean active;
    private static @Nullable ServerBossEvent bar;

    public static boolean isActive() {
        return active;
    }

    // (game tests: deathtax/DeathTaxTests) a Blood Moon on or off as far as the rest of the mod can tell (the state,
    // counted for Blood Moon Insurance), without its boss, sky, sidebar or messages
    public static void testSetActive(MinecraftServer server, boolean on) {
        Data data = data(server);
        data.active = on;
        data.setDirty();
        active = on;
        if (on) com.alexh.capitalism.deathtax.Insurance.bloodMoonStarted(server, false);
    }

    // The Blood Moon Altar's offering is complete (decor/BloodAltar): a Blood Moon at dusk tonight (13000 on the clock;
    // at once if it's already past). False if one is on or already coming.
    // calls off a Blood Moon that's coming (the altar's tests)
    public static void unschedule(MinecraftServer server) {
        Data data = data(server);
        data.startsAt = -1;
        data.setDirty();
    }

    public static boolean scheduleTonight(MinecraftServer server) {
        Data data = data(server);
        if (data.active || data.startsAt >= 0) return false;
        long timeOfDay = Math.floorMod(Calendar.clock(server.overworld()), Calendar.DAY_TICKS);
        long wait = timeOfDay < 13000 ? 13000 - timeOfDay : 0;
        data.startsAt = server.overworld().getGameTime() + wait;
        data.setDirty();
        return true;
    }

    // ticks until a scheduled (delayed) Blood Moon starts, or -1 if none is known (the grandfather clock asks:
    // decor/BloodDecorBlock.clockReading)
    public static long ticksUntil(MinecraftServer server) {
        Data data = data(server);
        return data.startsAt < 0 ? -1 : Math.max(0, data.startsAt - server.overworld().getGameTime());
    }

    // a Blood Moon night in this level (the overworld)?
    public static boolean isActive(Level level) {
        return active && level.dimension() == Level.OVERWORLD;
    }

    // ── the saved state ──

    public static final class Data extends SavedData {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(d -> d.active),
            Codec.LONG.optionalFieldOf("starts_at", -1L).forGetter(d -> d.startsAt),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("points", Map.of()).forGetter(d -> d.points),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(d -> d.names),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("owed", Map.of()).forGetter(d -> d.owed),
            UUIDUtil.STRING_CODEC.optionalFieldOf("revenant").forGetter(d -> Optional.ofNullable(d.revenant)),
            BlockPos.CODEC.optionalFieldOf("revenant_pos").forGetter(d -> Optional.ofNullable(d.revenantPos)),
            Codec.BOOL.optionalFieldOf("revenant_found", false).forGetter(d -> d.found),
            Codec.BOOL.optionalFieldOf("revenant_down", false).forGetter(d -> d.down),
            Codec.STRING.optionalFieldOf("boss", "revenant").forGetter(d -> d.boss)
        ).apply(i, Data::new));
        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_moon"), Data::new, CODEC);

        boolean active;
        long startsAt = -1;
        final Map<UUID, Integer> points = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        // gold (nuggets) owed to winners who were off at dawn
        final Map<UUID, Long> owed = new HashMap<>();
        @Nullable UUID revenant;
        @Nullable BlockPos revenantPos;
        boolean found;
        boolean down;
        // which boss tonight (Boss.id)
        String boss = "revenant";

        public Data() {
        }

        private Data(boolean active, long startsAt, Map<UUID, Integer> points, Map<UUID, String> names, Map<UUID, Long> owed,
                     Optional<UUID> revenant, Optional<BlockPos> revenantPos, boolean found, boolean down, String boss) {
            this.boss = boss;
            this.active = active;
            this.startsAt = startsAt;
            this.points.putAll(points);
            this.names.putAll(names);
            this.owed.putAll(owed);
            this.revenant = revenant.orElse(null);
            this.revenantPos = revenantPos.orElse(null);
            this.found = found;
            this.down = down;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        active = data(event.getServer()).active;
        bar = null;
    }

    // ── /event ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("event").requires(Commands.hasPermission(OPS))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"bloodmoon"}, b))
                .executes(c -> status(c))
                .then(Commands.literal("start").executes(c -> command(c, "start", 0))
                    .then(Commands.argument("boss", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Boss.values()).map(x -> x.id), b))
                        .executes(c -> command(c, "start", 0))))
                .then(Commands.literal("end").executes(c -> command(c, "end", 0)))
                .then(Commands.literal("tp").executes(BloodMoon::tpToRevenant))
                .then(Commands.literal("delay").then(Commands.argument("time", TimeArgument.time(1))
                    .executes(c -> command(c, "delay", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "time")))))));
    }

    private static boolean known(CommandContext<CommandSourceStack> c) {
        if (StringArgumentType.getString(c, "type").equalsIgnoreCase("bloodmoon")) return true;
        c.getSource().sendFailure(Component.literal("Unknown event. Events: bloodmoon, purge"));
        return false;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        if (!known(c)) return 0;
        MinecraftServer server = c.getSource().getServer();
        Data data = data(server);
        String text = data.active ? "A Blood Moon is on now." : data.startsAt >= 0
            ? "A Blood Moon starts in " + duration(data.startsAt - server.overworld().getGameTime()) + "."
            : "No Blood Moon is coming. /event bloodmoon start | end | delay <time>";
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int command(CommandContext<CommandSourceStack> c, String what, int ticks) {
        if (!known(c)) return 0;
        MinecraftServer server = c.getSource().getServer();
        Data data = data(server);
        switch (what) {
            case "start" -> {
                if (data.active) {
                    c.getSource().sendFailure(Component.literal("A Blood Moon is already on."));
                    return 0;
                }
                Boss forced = null;
                try {
                    String id = StringArgumentType.getString(c, "boss");
                    forced = Boss.byId(id);
                    if (forced == null) {
                        c.getSource().sendFailure(Component.literal("Unknown boss. Bosses: revenant, gorefang, witch"));
                        return 0;
                    }
                } catch (IllegalArgumentException noBoss) {
                    // none named: a random one
                }
                start(server, forced);
                c.getSource().sendSuccess(() -> Component.literal("The Blood Moon has begun: " + boss(data).name + " is out there."), true);
            }
            case "end" -> {
                if (!data.active && data.startsAt < 0) {
                    c.getSource().sendFailure(Component.literal("No Blood Moon is on or coming."));
                    return 0;
                }
                if (data.active) end(server, false);
                else {
                    data.startsAt = -1;
                    data.setDirty();
                }
                c.getSource().sendSuccess(() -> Component.literal("The Blood Moon was called off."), true);
            }
            default -> {
                if (data.active) {
                    c.getSource().sendFailure(Component.literal("A Blood Moon is on now: /event bloodmoon end first."));
                    return 0;
                }
                long now = server.overworld().getGameTime();
                boolean pushed = data.startsAt >= 0;
                data.startsAt = (pushed ? data.startsAt : now) + ticks;
                data.setDirty();
                String when = duration(data.startsAt - now);
                c.getSource().sendSuccess(() -> Component.literal((pushed ? "Pushed back: the" : "The") + " Blood Moon starts in " + when + "."), true);
            }
        }
        return 1;
    }

    // (dev) /event bloodmoon tp: to 6 blocks from tonight's Revenant (wherever it is, loaded or not)
    private static int tpToRevenant(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!known(c)) return 0;
        ServerPlayer player = c.getSource().getPlayerOrException();
        MinecraftServer server = c.getSource().getServer();
        Data data = data(server);
        BlockPos at = data.revenant != null && server.overworld().getEntity(data.revenant) instanceof BloodMoonBoss r
            ? r.blockPosition() : data.revenantPos;
        if (!data.active || data.down || at == null) {
            c.getSource().sendFailure(Component.literal(!data.active ? "No Blood Moon is on." : "The boss isn't out there."));
            return 0;
        }
        if (!com.alexh.capitalism.town.Town.teleportOnTop(player, at.getX() + 6, at.getZ())) {
            c.getSource().sendFailure(Component.literal("Only from the overworld."));
            return 0;
        }
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 2, 0));
        c.getSource().sendSuccess(() -> Component.literal("To " + boss(data).name + " (" + at.getX() + ", " + at.getY() + ", " + at.getZ() + ")."), false);
        return 1;
    }

    // ticks as "2m 30s" / "1d 4m" (a Minecraft day is 20 minutes)
    static String duration(long ticks) {
        long s = Math.max(0, ticks) / 20;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        StringBuilder out = new StringBuilder();
        if (h > 0) out.append(h).append("h ");
        if (m > 0 || h > 0) out.append(m).append("m ");
        out.append(sec).append("s");
        return out.toString();
    }

    // ── start and end ──

    public static void start(MinecraftServer server) {
        start(server, null);
    }

    // forced: that boss tonight (else one of the three at random)
    public static void start(MinecraftServer server, @Nullable Boss forced) {
        Data data = data(server);
        ServerLevel level = server.overworld();
        data.active = true;
        data.startsAt = -1;
        data.points.clear();
        data.names.clear();
        data.revenant = null;
        data.revenantPos = null;
        data.found = false;
        data.boss = (forced != null ? forced : Boss.values()[level.getRandom().nextInt(Boss.values().length)]).id;
        data.down = false;
        data.setDirty();
        active = true;
        // it's a night: if it's day, the sun goes down now
        long clock = Calendar.clock(level);
        long timeOfDay = Math.floorMod(clock, Calendar.DAY_TICKS);
        if (timeOfDay < 12500 || timeOfDay >= 22500) {
            long day = Math.floorDiv(clock, Calendar.DAY_TICKS) + (timeOfDay >= 22500 ? 1 : 0);
            Calendar.setClock(level, day * Calendar.DAY_TICKS + 13000);
        }
        // the sidebar shows the kills
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective old = scoreboard.getObjective(OBJECTIVE);
        if (old != null) scoreboard.removeObjective(old);
        Objective objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
            Component.literal("🩸 Blood Moon Kills").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF4040)).withBold(true)),
            ObjectiveCriteria.RenderType.INTEGER, true, null);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        // the Revenant goes into hiding
        spawnRevenant(server);
        // tell everyone: the card, chat, three tolls of a bell
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new BloodMoonPacket(true));
            PacketDistributor.sendToPlayer(player, new DayNoticePacket("Blood Moon", 0xFF3030, "The moon rises red tonight...",
                "Hunt " + boss(data).name, RED));
            player.sendSystemMessage(Component.literal("🩸 The moon rises red... ").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF4040)).withBold(true))
                .append(Component.literal("Monsters are stronger tonight and every kill counts. " + boss(data).title + " hides somewhere out past the town: find it.")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(0xE8B0B0)).withBold(false))));
        }
        tolls = 3;
        tollAt = server.getTickCount();
        // the insurers go into hiding (deathtax/Insurance: counts it for Blood Moon Insurance, tells everyone)
        com.alexh.capitalism.deathtax.Insurance.bloodMoonStarted(server, true);
        com.alexh.capitalism.bounty.BountyEvents.bloodMoonStarted(server);
    }

    public static void end(MinecraftServer server, boolean dawn) {
        Data data = data(server);
        data.active = false;
        active = false;
        // a Bottle of Blood's strength goes with it (decor/BottleOfBloodItem)
        for (ServerPlayer p : server.getPlayerList().getPlayers()) com.alexh.capitalism.decor.BottleOfBloodItem.stripBlood(p);
        // the Revenant, if it's still about, sinks away (if its chunk isn't loaded, it goes when it next is)
        if (data.revenant != null) {
            Entity revenant = server.overworld().getEntity(data.revenant);
            if (revenant instanceof BloodMoonBoss crimson && crimson.isAlive()) crimson.vanish();
        }
        // the results
        List<Map.Entry<UUID, Integer>> ranked = new ArrayList<>(data.points.entrySet());
        ranked.sort((a, b) -> b.getValue() - a.getValue());
        MutableComponent results = Component.literal("🩸 The Blood Moon is over" + (dawn ? ": the sun rises." : ".")).withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5050)).withBold(true));
        if (ranked.isEmpty()) {
            results.append(Component.literal(" Nobody hunted tonight.").withStyle(ChatFormatting.GRAY));
        } else {
            String[] places = {"1st", "2nd", "3rd"};
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                var e = ranked.get(i);
                results.append(Component.literal("\n  " + places[i] + "  ").withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY))
                    .append(Component.literal(data.names.getOrDefault(e.getKey(), "?")).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  " + e.getValue() + " point" + (e.getValue() == 1 ? "" : "s")).withStyle(ChatFormatting.RED));
            }
            var top = ranked.get(0);
            String winner = data.names.getOrDefault(top.getKey(), "?");
            results.append(Component.literal("\n  " + winner + " wins " + WINNER_GOLD + " gold!").withStyle(ChatFormatting.GOLD));
            ServerPlayer online = server.getPlayerList().getPlayer(top.getKey());
            long prize = (long) WINNER_GOLD * GoldMoney.INGOT;
            if (online != null) GoldMoney.give(online, prize);
            else data.owed.merge(top.getKey(), prize, Long::sum);
        }
        com.alexh.capitalism.bounty.BountyEvents.bloodMoonEnded(server, dawn, ranked.isEmpty() ? null : ranked.get(0).getKey());
        if (!data.down && data.revenant != null) results.append(Component.literal("\n  " + boss(data).title + " escaped into the earth.").withStyle(ChatFormatting.DARK_RED));
        String head = ranked.isEmpty() ? "Nobody hunted" : data.names.getOrDefault(ranked.get(0).getKey(), "?") + " wins " + WINNER_GOLD + " gold";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new BloodMoonPacket(false));
            player.sendSystemMessage(results);
            PacketDistributor.sendToPlayer(player, new DayNoticePacket("The Blood Moon is over", 0xFF5050, head,
                data.down ? boss(data).title + " fell" : boss(data).title + " escaped", data.down ? 0xFFD040 : 0x904040));
        }
        data.revenant = null;
        data.revenantPos = null;
        data.setDirty();
        // the sidebar goes back to the Gold Leaderboard (economy/GoldLeaderboard puts it back)
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE);
        if (objective != null) scoreboard.removeObjective(objective);
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    // ── each tick ──

    private static int tolls;
    private static int tollAt;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        Data data = data(server);
        if (!data.active) {
            if (data.startsAt >= 0 && level.getGameTime() >= data.startsAt) {
                start(server);   // (The Purge too? Both, the same night)
            }
            return;
        }
        // the bell tolls three times as it begins
        if (tolls > 0 && server.getTickCount() >= tollAt) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1.6F, 0.5F);
            }
            tolls--;
            tollAt = server.getTickCount() + 40;
        }
        long timeOfDay = Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
        // dawn
        if (timeOfDay >= 23000 || timeOfDay < 12000) {
            end(server, true);
            return;
        }
        // the red bar at the top: how much of the night is left
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), barTitle(data), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setDarkenScreen(true);
        }
        if (server.getTickCount() % 20 == 0) {
            bar.setName(barTitle(data));
            bar.setProgress(Math.max(0.0F, Math.min(1.0F, (23000 - timeOfDay) / 10000.0F)));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() == level) bar.addPlayer(player);
                else bar.removePlayer(player);
            }
        }
        // the clues to where the Revenant is: a red pillar into the sky (seen from up to 100 blocks), a heartbeat
        BlockPos at = data.revenantPos;
        if (at != null && !data.down) {
            if (server.getTickCount() % 3 == 0) pillar(level, at);
            if (server.getTickCount() % 28 == 0) {
                for (ServerPlayer player : level.players()) {
                    if (player.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) < HEARTBEAT_RANGE * HEARTBEAT_RANGE) {
                        level.playSound(null, at, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.5F, 0.6F);
                        break;
                    }
                }
            }
        }
    }

    private static Component barTitle(Data data) {
        String text = data.down ? "🩸 Blood Moon — " + boss(data).name + " has fallen"
            : data.found ? "🩸 Blood Moon — " + boss(data).name + " has been found!"
            : "🩸 Blood Moon — " + boss(data).name + " hides out past the town";
        return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(0xFF6060)));
    }

    // a column of red dust from the Revenant up into the sky, for the players near enough to see it
    private static void pillar(ServerLevel level, BlockPos at) {
        DustParticleOptions dust = new DustParticleOptions(0xD01818, 3.0F);
        DustParticleOptions core = new DustParticleOptions(0xFF5050, 1.6F);
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - (at.getX() + 0.5), dz = player.getZ() - (at.getZ() + 0.5);
            if (dx * dx + dz * dz > PILLAR_RANGE * PILLAR_RANGE) continue;
            for (int i = 0; i < 14; i++) {
                double y = at.getY() + 2 + level.getRandom().nextDouble() * 90;
                level.sendParticles(player, dust, true, true, at.getX() + 0.5, y, at.getZ() + 0.5, 1, 0.25, 0.4, 0.25, 0.0);
            }
            level.sendParticles(player, core, true, true, at.getX() + 0.5, at.getY() + 2 + level.getRandom().nextDouble() * 60, at.getZ() + 0.5,
                2, 0.05, 2.0, 0.05, 0.0);
        }
    }

    // ── the boss: one of three, picked at random each Blood Moon ──

    public enum Boss {
        REVENANT("revenant", "the Crimson Revenant", "The Crimson Revenant", () -> ModEntities.CRIMSON_REVENANT.get()),
        GOREFANG("gorefang", "Gorefang, the Blood Wolf", "Gorefang", () -> ModEntities.GOREFANG.get()),
        WITCH("witch", "the Sanguine Witch", "The Sanguine Witch", () -> ModEntities.SANGUINE_WITCH.get());

        public final String id;
        // in a sentence ("Hunt the Crimson Revenant"), and to start one
        public final String name;
        public final String title;
        final java.util.function.Supplier<net.minecraft.world.entity.EntityType<? extends BloodMoonBoss>> entity;

        Boss(String id, String name, String title, java.util.function.Supplier<net.minecraft.world.entity.EntityType<? extends BloodMoonBoss>> entity) {
            this.id = id;
            this.name = name;
            this.title = title;
            this.entity = entity;
        }

        static @Nullable Boss byId(String id) {
            for (Boss b : values()) if (b.id.equalsIgnoreCase(id)) return b;
            return null;
        }
    }

    static Boss boss(Data data) {
        Boss b = Boss.byId(data.boss);
        return b == null ? Boss.REVENANT : b;
    }

    static void spawnRevenant(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Data data = data(server);
        var area = com.alexh.capitalism.town.Town.protectedArea();
        int cx = (area.minX() + area.maxX()) / 2, cz = (area.minZ() + area.maxZ()) / 2;
        var random = level.getRandom();
        BlockPos spot = null;
        for (int attempt = 0; attempt < 16 && spot == null; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = HIDE_MIN + random.nextDouble() * (HIDE_MAX - HIDE_MIN);
            int x = cx + (int) Math.round(Math.cos(angle) * dist), z = cz + (int) Math.round(Math.sin(angle) * dist);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            if (!level.getFluidState(ground).isEmpty() || !level.getBlockState(ground).isSolid()) continue;
            spot = new BlockPos(x, y, z);
        }
        if (spot == null) return;
        BloodMoonBoss revenant = boss(data).entity.get().create(level, EntitySpawnReason.EVENT);
        if (revenant == null) return;
        revenant.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        revenant.setPersistenceRequired();
        level.addFreshEntity(revenant);
        data.revenant = revenant.getUUID();
        data.revenantPos = spot;
        data.setDirty();
    }

    // (photos, tests) this Revenant is tonight's: it stays, and its clues follow it
    public static void adopt(MinecraftServer server, BloodMoonBoss revenant) {
        Data data = data(server);
        if (data.revenant != null && server.overworld().getEntity(data.revenant) instanceof BloodMoonBoss old && old != revenant) old.discard();
        data.revenant = revenant.getUUID();
        data.revenantPos = revenant.blockPosition();
        for (Boss b : Boss.values()) if (b.entity.get() == revenant.getType()) data.boss = b.id;
        data.setDirty();
    }

    // the Revenant moved (it keeps its clues over it)
    static void revenantAt(MinecraftServer server, BloodMoonBoss revenant) {
        Data data = data(server);
        if (!revenant.getUUID().equals(data.revenant)) return;
        BlockPos pos = revenant.blockPosition();
        if (!pos.equals(data.revenantPos)) {
            data.revenantPos = pos;
            data.setDirty();
        }
    }

    static boolean isTheRevenant(MinecraftServer server, BloodMoonBoss revenant) {
        Data data = data(server);
        return data.active && revenant.getUUID().equals(data.revenant);
    }

    static void found(MinecraftServer server, ServerPlayer by) {
        Data data = data(server);
        if (data.found) return;
        data.found = true;
        data.setDirty();
        Component text = Component.literal("🩸 ").append(Component.literal(by.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" has found " + boss(data).name + "!").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5050)).withBold(true)));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(text);
            player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8F, 0.8F);
        }
    }

    // ── kills, XP, drops ──

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        BloodMoonBoss.onThrallDeath(dead);
        if (!isActive(level) || !(dead instanceof Enemy)) return;
        MinecraftServer server = level.getServer();
        Data data = data(server);
        Entity killer = event.getSource().getEntity();
        if (dead instanceof BloodMoonBoss revenant && revenant.getUUID().equals(data.revenant)) {
            data.down = true;
            data.setDirty();
            String who = killer instanceof ServerPlayer p ? p.getGameProfile().name() : "Something";
            Component text = Component.literal("🩸 ").append(Component.literal(who).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" has slain " + boss(data).name + "!").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5050)).withBold(true)));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(text);
                player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.9F, 0.8F);
            }
        }
        if (!(killer instanceof ServerPlayer player)) return;
        int points = dead instanceof BloodMoonBoss ? POINTS_REVENANT : switch (levelOrOne(dead)) {
            case BOSS -> POINTS_BOSS;
            case TWO -> POINTS_TWO;
            case ONE -> POINTS_ONE;
        };
        addPoints(server, player, points);
    }

    private static TougherMobs.Level levelOrOne(Entity entity) {
        TougherMobs.Level level = TougherMobs.levelOf(entity);
        return level == null ? TougherMobs.Level.ONE : level;
    }

    public static void addPoints(MinecraftServer server, ServerPlayer player, int points) {
        Data data = data(server);
        int total = data.points.merge(player.getUUID(), points, Integer::sum);
        data.names.put(player.getUUID(), player.getGameProfile().name());
        data.setDirty();
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
        if (objective != null) server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getGameProfile().name()), objective).set(total);
        player.sendOverlayMessage(Component.literal("+" + points + " 🩸  (" + total + ")").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF6060))));
    }

    // Blood Shards: Level I 15%, Level II 35%, Boss 1-2 every time (the Revenant drops its own)
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!isActive(dead.level()) || !(dead instanceof Enemy) || dead instanceof BloodMoonBoss) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;
        var random = dead.getRandom();
        int shards = switch (levelOrOne(dead)) {
            case BOSS -> 1 + random.nextInt(2);
            case TWO -> random.nextFloat() < 0.35F ? 1 : 0;
            case ONE -> random.nextFloat() < 0.15F ? 1 : 0;
        };
        if (shards > 0) {
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(ModItems.BLOOD_SHARD.get(), shards)));
        }
    }

    // the monsters are all on one side: none goes for a boss, a boss goes for none of them
    @SubscribeEvent
    public static void onTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        LivingEntity who = event.getEntity();
        if (target == null) return;
        if (target instanceof BloodMoonBoss && BloodMoonBoss.isMonsterSide(who) || who instanceof BloodMoonBoss && BloodMoonBoss.isMonsterSide(target)) {
            event.setCanceled(true);
        }
    }

    // nobody sleeps through it
    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        if (isActive(event.getLevel())) {
            event.setProblem(new Player.BedSleepingProblem(Component.literal("You can't sleep through a Blood Moon.").withStyle(ChatFormatting.RED)));
        }
    }

    // logging in: the red sky and bar if it's on, any prize they were owed
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Data data = data(player.level().getServer());
        PacketDistributor.sendToPlayer(player, new BloodMoonPacket(data.active));
        Long owed = data.owed.remove(player.getUUID());
        if (owed != null && owed > 0) {
            data.setDirty();
            GoldMoney.give(player, owed);
            player.sendSystemMessage(Component.literal("🩸 Your Blood Moon prize: " + GoldMoney.format(owed) + ".").withStyle(ChatFormatting.GOLD));
        }
    }
}

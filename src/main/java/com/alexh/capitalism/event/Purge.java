package com.alexh.capitalism.event;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.network.DayNoticePacket;
import com.alexh.capitalism.town.Town;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

// The Purge (Events.md): a rare night with no laws.
//  - Each morning there's a small chance (config events.purgeChancePerDay) it's announced for the NEXT night, with
//    sirens: "The Purge begins at dusk tomorrow. No laws until dawn." Reminders that evening, the next morning and just
//    before dusk. Never the same night as a Blood Moon: it isn't announced while one is on or coming, the Blood Moon
//    Altar takes no offerings on a Purge day, and any other Blood Moon due on a Purge night waits a night (BloodMoon).
//  - Dusk (13000) to dawn (23000), a red boss bar counting the night down; nobody sleeps through it.
//  - PvP is on everywhere (the vanilla `pvp` game rule, which is all that normally decides PvP on this server, is set
//    true for the night and put back to what it was at dawn), except the protected town (Town.protectedArea, the
//    whole column, overworld): a safe zone, no player hurts a player there (nor their pets).
//  - Killed by another player outside the town: the victim's death tax is owed to the killer (deathtax/DeathTax).
//  - /event purge [start | schedule | end] (ops).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Purge {

    public static final int RED = 0xD01010;
    static final int DUSK = 13000;
    static final int DAWN = 23000;
    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private static volatile boolean active;
    private static @Nullable ServerBossEvent bar;
    private static int sirens;
    private static int sirenAt;

    private Purge() {
    }

    public static boolean isActive() {
        return active;
    }

    // ── the saved state ──

    public static final class Data extends SavedData {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(d -> d.active),
            Codec.LONG.optionalFieldOf("night", -1L).forGetter(d -> d.night),
            Codec.BOOL.optionalFieldOf("saved_pvp").forGetter(d -> Optional.ofNullable(d.savedPvp)),
            Codec.LONG.optionalFieldOf("last_rolled", Long.MIN_VALUE).forGetter(d -> d.lastRolled),
            Codec.INT.optionalFieldOf("reminded", 0).forGetter(d -> d.reminded),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(d -> d.kills),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(d -> d.names)
        ).apply(i, Data::new));
        static final SavedDataType<Data> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "purge"), Data::new, CODEC);

        boolean active;
        // the day (Calendar index) whose night it's announced for, -1 = none
        long night = -1;
        // the pvp game rule before it began (put back at dawn)
        @Nullable Boolean savedPvp;
        // the last day the morning roll was made
        long lastRolled = Long.MIN_VALUE;
        // reminders given for the announced night (bits: 1 that evening, 2 that morning, 4 just before dusk)
        int reminded;
        // tonight's kills
        final Map<UUID, Integer> kills = new HashMap<>();
        final Map<UUID, String> names = new HashMap<>();

        public Data() {
        }

        private Data(boolean active, long night, Optional<Boolean> savedPvp, long lastRolled, int reminded, Map<UUID, Integer> kills, Map<UUID, String> names) {
            this.active = active;
            this.night = night;
            this.savedPvp = savedPvp.orElse(null);
            this.lastRolled = lastRolled;
            this.reminded = reminded;
            this.kills.putAll(kills);
            this.names.putAll(names);
        }

        public long night() {
            return this.night;
        }

        public @Nullable Boolean savedPvp() {
            return this.savedPvp;
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

    static long today(MinecraftServer server) {
        return Calendar.date(server.overworld()).index();
    }

    static long timeOfDay(MinecraftServer server) {
        return Math.floorMod(Calendar.clock(server.overworld()), Calendar.DAY_TICKS);
    }

    // Is tonight the Purge's (on now, or announced for today)? (A Blood Moon can share the night.)
    public static boolean claimsTonight(MinecraftServer server) {
        Data data = data(server);
        return data.active || data.night == today(server);
    }

    // ── the safe zone, and who's owed a victim's death tax ──

    // in the protected town (the whole column, overworld only)
    public static boolean inSafeZone(Entity entity) {
        return entity.level().dimension() == Level.OVERWORLD && Town.protectedArea().contains(entity.blockPosition());
    }

    // The player owed this death's tax: in The Purge, the player who killed them (or last hurt them, if they fell or
    // burned), not themselves, and only outside the town (null = the bank).
    public static @Nullable ServerPlayer killerFor(ServerPlayer victim, DamageSource source) {
        if (!active) return null;
        ServerPlayer killer = null;
        if (source.getEntity() instanceof ServerPlayer p) killer = p;
        else if (victim.getKillCredit() instanceof ServerPlayer p) killer = p;
        if (killer == null || killer.getUUID().equals(victim.getUUID()) || inSafeZone(victim)) return null;
        return killer;
    }

    // (deathtax/DeathTax) tonight's tally, for dawn's results
    public static void countKill(MinecraftServer server, ServerPlayer killer) {
        Data data = data(server);
        data.kills.merge(killer.getUUID(), 1, Integer::sum);
        data.names.put(killer.getUUID(), killer.getGameProfile().name());
        data.setDirty();
    }

    // The town is a safe zone, always (Town.md): no player hurts a player there (nor a player's pet or projectile), and
    // nobody in it hurts anyone outside. Purge night or not.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity attacker = event.getSource().getEntity();
        Player behind = attacker instanceof Player p ? p : attacker instanceof TamableAnimal pet && pet.getOwner() instanceof Player owner ? owner : null;
        if (behind == null || behind == victim) return;
        if (inSafeZone(victim) || inSafeZone(attacker)) {
            event.setCanceled(true);
            if (behind instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.literal("The town is a safe zone: no fighting here.").withStyle(ChatFormatting.GREEN));
        }
    }

    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        if (active) event.setProblem(new Player.BedSleepingProblem(Component.literal("Nobody sleeps through The Purge.").withStyle(ChatFormatting.RED)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Data data = data(player.level().getServer());
        if (data.active) player.sendSystemMessage(line("The Purge is on: no laws until dawn. The town is a safe zone."));
        else if (data.night >= 0) player.sendSystemMessage(line(data.night == today(player.level().getServer())
            ? "The Purge begins at dusk tonight. No laws until dawn." : "The Purge begins at dusk tomorrow. No laws until dawn."));
    }

    // ── announcing, starting and ending ──

    // could a Purge be announced for this night? Not while one is on or announced, nor with a Blood Moon on or coming
    static boolean canAnnounce(MinecraftServer server) {
        Data data = data(server);
        // (a Blood Moon doesn't stop it: the two can share a night)
        return !data.active && data.night < 0;
    }

    // announce it for tomorrow night (false if it can't be)
    public static boolean schedule(MinecraftServer server) {
        if (!canAnnounce(server)) return false;
        Data data = data(server);
        data.night = today(server) + 1;
        data.reminded = 0;
        data.setDirty();
        broadcast(server, "The Purge begins at dusk tomorrow. No laws until dawn.", true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new DayNoticePacket("The Purge", RED, "Begins at dusk tomorrow", "No laws until dawn", 0xFF6060));
        }
        return true;
    }

    public static boolean start(MinecraftServer server) {
        Data data = data(server);
        if (data.active) return false;
        ServerLevel level = server.overworld();
        data.active = true;
        data.night = -1;
        data.reminded = 0;
        data.kills.clear();
        data.names.clear();
        data.savedPvp = level.getGameRules().get(GameRules.PVP);
        level.getGameRules().set(GameRules.PVP, true, server);
        data.setDirty();
        active = true;
        // it's a night: if it's day (an admin's /event purge start), the sun goes down now
        long clock = Calendar.clock(level);
        long tod = Math.floorMod(clock, Calendar.DAY_TICKS);
        if (tod < DUSK - 500 || tod >= DAWN) {
            long day = Math.floorDiv(clock, Calendar.DAY_TICKS) + (tod >= DAWN ? 1 : 0);
            Calendar.setClock(level, day * Calendar.DAY_TICKS + DUSK);
        }
        broadcast(server, "THE PURGE HAS BEGUN. No laws until dawn. PvP is on everywhere but the town, the only safe zone. "
            + "Killed by a player out there? Your death tax goes to them.", true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new DayNoticePacket("The Purge", RED, "No laws until dawn", "The town is a safe zone", 0x60E060));
        }
        return true;
    }

    public static void end(MinecraftServer server, boolean dawn) {
        Data data = data(server);
        ServerLevel level = server.overworld();
        if (data.savedPvp != null) level.getGameRules().set(GameRules.PVP, data.savedPvp, server);
        data.savedPvp = null;
        data.active = false;
        active = false;
        List<Map.Entry<UUID, Integer>> ranked = new ArrayList<>(data.kills.entrySet());
        ranked.sort((a, b) -> b.getValue() - a.getValue());
        MutableComponent results = Component.literal("☠ The Purge is over" + (dawn ? ": the sun rises" : "") + ". Laws are back.")
            .withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5050)).withBold(true));
        if (ranked.isEmpty()) results.append(Component.literal(" Nobody was purged.").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            var e = ranked.get(i);
            results.append(Component.literal("\n  " + data.names.getOrDefault(e.getKey(), "?") + "  " + e.getValue() + " kill" + (e.getValue() == 1 ? "" : "s"))
                .withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        }
        data.setDirty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(results);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.MASTER, 1.0F, 1.2F);
        }
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // the sirens wail a few times as it's announced and as it begins
        if (sirens > 0 && server.getTickCount() >= sirenAt) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.MASTER, 1.4F, sirens % 2 == 0 ? 0.6F : 0.75F);
            }
            sirens--;
            sirenAt = server.getTickCount() + 60;
        }
        if (server.getTickCount() % 20 != 0) return;
        Data data = data(server);
        long day = today(server), tod = timeOfDay(server);
        if (data.active) {
            if (tod >= DAWN || tod < DUSK - 1000) {
                end(server, true);
                return;
            }
            if (bar == null) {
                bar = new ServerBossEvent(UUID.randomUUID(), barTitle(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
                bar.setDarkenScreen(true);
            }
            bar.setProgress(Math.max(0.0F, Math.min(1.0F, (DAWN - tod) / (float) (DAWN - DUSK))));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) bar.addPlayer(player);
            return;
        }
        // (when a natural Purge comes: event/EventSchedule, every 10-12 days)
        if (data.night < 0) return;
        if (day > data.night || (day == data.night && tod >= DAWN)) {
            // missed (the server was off all night)
            data.night = -1;
            data.setDirty();
        } else if (day == data.night && tod >= DUSK) {
            start(server);   // (a Blood Moon too? Both, the same night)
        } else if (day == data.night - 1 && tod >= DUSK && (data.reminded & 1) == 0) {
            remind(server, data, 1, "Reminder: The Purge begins at dusk tomorrow. No laws until dawn.");
        } else if (day == data.night && tod < DUSK - 1200 && (data.reminded & 2) == 0) {
            remind(server, data, 2, "The Purge begins at dusk tonight. No laws until dawn. The town is the only safe zone.");
        } else if (day == data.night && tod >= DUSK - 1200 && (data.reminded & 4) == 0) {
            remind(server, data, 4, "The Purge begins in a minute. Get to the town if you want to be safe.");
        }
    }

    private static void remind(MinecraftServer server, Data data, int bit, String text) {
        data.reminded |= bit;
        data.setDirty();
        broadcast(server, text, bit == 4);
    }

    private static Component barTitle() {
        return Component.literal("☠ THE PURGE — no laws until dawn").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF4040)).withBold(true));
    }

    private static MutableComponent line(String text) {
        return Component.literal("☠ ").withStyle(s -> s.withColor(TextColor.fromRgb(RED)).withBold(true))
            .append(Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(0xFF7070)).withBold(false)));
    }

    static void broadcast(MinecraftServer server, String text, boolean siren) {
        Component message = line(text);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.sendSystemMessage(message);
        if (siren) {
            sirens = 3;
            sirenAt = server.getTickCount();
        }
    }

    // calls off an announced Purge (quietly: game tests)
    public static void unschedule(MinecraftServer server) {
        Data data = data(server);
        data.night = -1;
        data.setDirty();
    }

    // ── /event purge [start | schedule | end] ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // (a literal: matched before /event's "type" word, which is the Blood Moon's)
        event.getDispatcher().register(Commands.literal("event").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("purge")
                .executes(Purge::status)
                .then(Commands.literal("start").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    if (data(server).active) return fail(c, "The Purge is already on.");
                    start(server);
                    c.getSource().sendSuccess(() -> Component.literal("The Purge has begun."), true);
                    return 1;
                }))
                .then(Commands.literal("schedule").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    Data data = data(server);
                    if (data.active || data.night >= 0) return fail(c, data.active ? "The Purge is on now." : "The Purge is already announced.");
                    if (!schedule(server)) return fail(c, "A Blood Moon is on or coming: no Purge can be announced now.");
                    c.getSource().sendSuccess(() -> Component.literal("The Purge is announced for tomorrow night."), true);
                    return 1;
                }))
                .then(Commands.literal("end").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    Data data = data(server);
                    if (data.active) end(server, false);
                    else if (data.night >= 0) {
                        data.night = -1;
                        data.setDirty();
                        broadcast(server, "The Purge is called off.", false);
                    } else return fail(c, "No Purge is on or coming.");
                    c.getSource().sendSuccess(() -> Component.literal("The Purge was called off."), true);
                    return 1;
                }))));
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer server = c.getSource().getServer();
        Data data = data(server);
        String text = data.active ? "The Purge is on now." : data.night >= 0
            ? "The Purge is announced for " + (data.night == today(server) ? "tonight." : "tomorrow night.")
            : "No Purge is coming. /event purge start | schedule | end";
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }
}

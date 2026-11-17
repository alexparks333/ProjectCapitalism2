package com.alexh.capitalism.fishing;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// FishWatch (Fishing.md > AFK fishing): spots AFK fishing for admins. Nothing here acts on its own: it only scores and
// tells ops; only an op can quietly restrict someone (/fishlog restrict), and only an op can lift it.
//  - Per player, in memory: casts and catches; the reaction from bite to reel-in (its average and spread); how long
//    since they last moved or turned their head while still casting; how regular their casting is; how much the
//    bobber's spot varies; catches with a hopper by them (the catch going somewhere else); how long since chat or a menu.
//  - A suspicion score from those with the reasons. Lenient: a real angler never gets near the line (see score()).
//  - Crossing the line: logged (saved, the last 100) and told to online ops, at most once per player per 30 minutes.
//    The player is never told.
//  - Restricted (ModAttachments.FISH_RESTRICTED, saved, survives logouts and restarts): no rare catches, only ordinary
//    fish (RareCatchModifier). Silent.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class FishWatch {

    public static final int FLAG = 60, SUSPICIOUS = 30;
    static final long ALERT_EVERY = 20L * 60 * 30;
    static final long SESSION_GAP = 20L * 60 * 10;
    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    // one player's fishing session
    public static final class Stats {
        public String name = "";
        public int casts, catches, hopperCatches;
        final Deque<Integer> reactions = new ArrayDeque<>();
        final Deque<Long> castTicks = new ArrayDeque<>();
        final Deque<Vec3> spots = new ArrayDeque<>();
        long biteTick = -1, lastCastTick = -1, lastMoveTick, lastTalkTick, sessionStart;
        float yaw, pitch;
        Vec3 pos = Vec3.ZERO;
        boolean wasFishing, biting;
        long lastAlert = Long.MIN_VALUE / 2;
        public int score;
        public final List<String> reasons = new ArrayList<>();

        public double averageReaction() {
            return this.reactions.stream().mapToInt(Integer::intValue).average().orElse(0);
        }

        public double reactionSpread() {
            return spread(this.reactions.stream().mapToDouble(Integer::doubleValue).toArray());
        }

        public double minutesStill(long now) {
            return (now - this.lastMoveTick) / 1200.0;
        }
    }

    private static final Map<UUID, Stats> STATS = new HashMap<>();

    private FishWatch() {
    }

    public static Stats stats(ServerPlayer player) {
        Stats s = STATS.computeIfAbsent(player.getUUID(), id -> new Stats());
        s.name = player.getGameProfile().name();
        return s;
    }

    static double spread(double[] v) {
        if (v.length < 2) return 0;
        double mean = 0;
        for (double d : v) mean += d;
        mean /= v.length;
        double var = 0;
        for (double d : v) var += (d - mean) * (d - mean);
        return Math.sqrt(var / v.length);
    }

    private static <T> void keep(Deque<T> q, T item, int max) {
        q.addLast(item);
        while (q.size() > max) q.removeFirst();
    }

    // ── what happened (called by the events below; the game tests call them directly) ──

    public static void cast(ServerPlayer player, long now) {
        Stats s = stats(player);
        if (s.lastCastTick < 0 || now - s.lastCastTick > SESSION_GAP) {
            // a new session
            s.casts = 0;
            s.catches = 0;
            s.hopperCatches = 0;
            s.reactions.clear();
            s.castTicks.clear();
            s.spots.clear();
            s.sessionStart = now;
            if (s.lastTalkTick < now - SESSION_GAP) s.lastTalkTick = now;
            if (s.lastMoveTick == 0) s.lastMoveTick = now;
        }
        s.casts++;
        s.lastCastTick = now;
        s.biteTick = -1;
        keep(s.castTicks, now, 40);
    }

    public static void bite(ServerPlayer player, long now) {
        stats(player).biteTick = now;
    }

    public static void caught(ServerPlayer player, long now, Vec3 hook, boolean hopperNear) {
        Stats s = stats(player);
        s.catches++;
        if (s.biteTick >= 0) keep(s.reactions, (int) (now - s.biteTick), 40);
        s.biteTick = -1;
        keep(s.spots, hook, 30);
        if (hopperNear) s.hopperCatches++;
        evaluate(player, now);
    }

    // they moved or turned their head
    public static void moved(ServerPlayer player, long now) {
        stats(player).lastMoveTick = now;
    }

    // they chatted or opened a menu
    public static void talked(ServerPlayer player, long now) {
        stats(player).lastTalkTick = now;
    }

    // ── the score ──

    // Each sign adds to the score (a real angler scores 0 to 20; the line is 60):
    //  - 20+ catches reeled in on exactly the same tick after the bite: 50; or within a spread under 1 tick: 40
    //  - not moved or turned their head while casting for 10+ minutes: 30; 20+ minutes: 40
    //  - 20+ casts spaced like clockwork (spread under 2 ticks): 20
    //  - the bobber on the same spot for 20+ catches (all within 0.15 blocks): 10
    //  - 5+ catches with a hopper (or hopper minecart) within 3 blocks of them: 15
    //  - 60+ minutes fishing without chat or a menu: 10
    public static void score(Stats s, long now) {
        s.score = 0;
        s.reasons.clear();
        if (s.reactions.size() >= 20) {
            List<Integer> last = new ArrayList<>(s.reactions).subList(Math.max(0, s.reactions.size() - 20), s.reactions.size());
            boolean same = last.stream().distinct().count() == 1;
            double sd = spread(last.stream().mapToDouble(Integer::doubleValue).toArray());
            if (same) add(s, 50, "reels in " + last.get(0) + " ticks after every bite");
            else if (sd < 1.0) add(s, 40, String.format(Locale.ROOT, "reaction never varies (%.1f ± %.2f ticks)", s.averageReaction(), sd));
        }
        boolean casting = s.lastCastTick >= 0 && now - s.lastCastTick < 1200;
        double still = s.minutesStill(now);
        if (casting && still >= 20) add(s, 40, String.format(Locale.ROOT, "still %.0f min", still));
        else if (casting && still >= 10) add(s, 30, String.format(Locale.ROOT, "still %.0f min", still));
        if (s.castTicks.size() >= 21) {
            Long[] t = s.castTicks.toArray(new Long[0]);
            double[] gaps = new double[t.length - 1];
            for (int i = 1; i < t.length; i++) gaps[i - 1] = t[i] - t[i - 1];
            if (spread(gaps) < 2.0) add(s, 20, "casts like clockwork");
        }
        if (s.spots.size() >= 20) {
            Vec3 mean = Vec3.ZERO;
            for (Vec3 v : s.spots) mean = mean.add(v);
            mean = mean.scale(1.0 / s.spots.size());
            double far = 0;
            for (Vec3 v : s.spots) far = Math.max(far, v.distanceTo(mean));
            if (far < 0.15) add(s, 10, "bobber on the same spot");
        }
        if (s.hopperCatches >= 5) add(s, 15, "a hopper by them takes the catches");
        double quiet = (now - Math.max(s.lastTalkTick, s.sessionStart)) / 1200.0;
        if (casting && quiet >= 60) add(s, 10, String.format(Locale.ROOT, "%.0f min without chat or menus", quiet));
    }

    private static void add(Stats s, int points, String why) {
        s.score += points;
        s.reasons.add(why);
    }

    public static boolean flagged(Stats s) {
        return s.score >= FLAG;
    }

    // scored after each catch: crossing the line is logged and told to ops (once per 30 minutes per player)
    static void evaluate(ServerPlayer player, long now) {
        Stats s = stats(player);
        score(s, now);
        if (!flagged(s) || now - s.lastAlert < ALERT_EVERY) return;
        s.lastAlert = now;
        String line = "[FishWatch] " + s.name + " looks like AFK fishing (" + String.join(", ", s.reasons) + "). /fishlog " + s.name;
        MinecraftServer server = player.level().getServer();
        Log.get(server).add(String.format(Locale.ROOT, "%s: %s, score %d: %s", CatchInfo.dateText(com.alexh.capitalism.calendar.Calendar.date(player.level())), s.name, s.score, String.join(", ", s.reasons)));
        Capitalism.LOGGER.info(line);
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (op != player && op.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                op.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    // ── the quiet countermeasure (ops only) ──

    public static boolean restricted(ServerPlayer player) {
        return player.getData(ModAttachments.FISH_RESTRICTED.get());
    }

    public static void restrict(ServerPlayer player, boolean on) {
        player.setData(ModAttachments.FISH_RESTRICTED.get(), on);
    }

    public static void clear(ServerPlayer player) {
        STATS.remove(player.getUUID());
    }

    // ── watching the game ──

    private static Field nibble;

    private static boolean biting(FishingHook hook) {
        try {
            if (nibble == null) {
                nibble = FishingHook.class.getDeclaredField("nibble");
                nibble.setAccessible(true);
            }
            return nibble.getInt(hook) > 0;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getServer().getTickCount();
        Stats s = STATS.get(player.getUUID());
        FishingHook hook = player.fishing;
        if (s == null && hook == null) return;
        if (s == null) s = stats(player);
        // moving or turning (any change of yaw or pitch counts)
        if (player.getYRot() != s.yaw || player.getXRot() != s.pitch || player.position().distanceToSqr(s.pos) > 0.0004) {
            s.yaw = player.getYRot();
            s.pitch = player.getXRot();
            s.pos = player.position();
            s.lastMoveTick = now;
        }
        if (hook != null && !s.wasFishing) cast(player, now);
        s.wasFishing = hook != null;
        boolean bitingNow = hook != null && biting(hook);
        if (bitingNow && !s.biting) bite(player, now);
        s.biting = bitingNow;
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getServer().getTickCount();
        caught(player, now, event.getHookEntity().position(), hopperNear(player));
    }

    static boolean hopperNear(ServerPlayer player) {
        BlockPos at = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-3, -3, -3), at.offset(3, 2, 3))) {
            if (player.level().getBlockState(p).getBlock() instanceof HopperBlock) return true;
        }
        return !player.level().getEntitiesOfClass(MinecartHopper.class, new AABB(at).inflate(3)).isEmpty();
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        talked(event.getPlayer(), event.getPlayer().level().getServer().getTickCount());
    }

    @SubscribeEvent
    public static void onMenu(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) talked(player, player.level().getServer().getTickCount());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // a session ends with the login; the score is gone, the saved log and a restriction aren't
        Stats s = STATS.get(event.getEntity().getUUID());
        if (s != null && s.score < SUSPICIOUS) STATS.remove(event.getEntity().getUUID());
    }

    // ── the saved log (the last 100 flags) ──

    public static final class Log extends SavedData {
        private static final Codec<Log> CODEC = Codec.STRING.listOf().xmap(Log::new, log -> List.copyOf(log.lines));
        static final SavedDataType<Log> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "fishwatch_log"), Log::new, CODEC);
        private final List<String> lines = new ArrayList<>();

        public Log() {
        }

        private Log(List<String> lines) {
            this.lines.addAll(lines);
        }

        static Log get(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }

        void add(String line) {
            this.lines.add(line);
            while (this.lines.size() > 100) this.lines.remove(0);
            this.setDirty();
        }
    }

    // ── /fishlog (ops) ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fishlog").requires(Commands.hasPermission(OPS))
            .executes(FishWatch::list)
            .then(Commands.literal("restrict").then(Commands.argument("player", EntityArgument.player()).executes(c -> setRestricted(c, true))))
            .then(Commands.literal("unrestrict").then(Commands.argument("player", EntityArgument.player()).executes(c -> setRestricted(c, false))))
            .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(FishWatch::clearCmd)))
            .then(Commands.argument("player", EntityArgument.player()).executes(FishWatch::details)));
    }

    private static String summary(Stats s, long now) {
        return String.format(Locale.ROOT, "%s: score %d, %d casts, %d catches, reaction %.1f ± %.1f ticks, still %.0f min%s",
            s.name, s.score, s.casts, s.catches, s.averageReaction(), s.reactionSpread(), s.minutesStill(now),
            s.reasons.isEmpty() ? "" : " (" + String.join(", ", s.reasons) + ")");
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        long now = context.getSource().getServer().getTickCount();
        List<String> out = new ArrayList<>();
        for (Stats s : STATS.values()) {
            score(s, now);
            if (s.score >= SUSPICIOUS) out.add((flagged(s) ? "FLAGGED " : "suspicious ") + summary(s, now));
        }
        List<String> log = Log.get(context.getSource().getServer()).lines;
        StringBuilder sb = new StringBuilder("[FishWatch] ");
        sb.append(out.isEmpty() ? "nobody looks like AFK fishing right now." : String.join("\n  ", out));
        if (!log.isEmpty()) {
            sb.append("\nRecent flags:");
            for (String l : log.subList(Math.max(0, log.size() - 5), log.size())) sb.append("\n  ").append(l);
        }
        context.getSource().sendSuccess(() -> Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD), false);
        return out.size();
    }

    private static int details(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long now = context.getSource().getServer().getTickCount();
        Stats s = stats(target);
        score(s, now);
        double gapSpread = 0;
        if (s.castTicks.size() >= 2) {
            Long[] t = s.castTicks.toArray(new Long[0]);
            double[] gaps = new double[t.length - 1];
            for (int i = 1; i < t.length; i++) gaps[i - 1] = t[i] - t[i - 1];
            gapSpread = spread(gaps);
        }
        String text = "[FishWatch] " + summary(s, now)
            + String.format(Locale.ROOT, "\n  session %.0f min, %.0f min since chat or a menu, cast spacing ± %.1f ticks, %d catches by a hopper",
                (now - s.sessionStart) / 1200.0, (now - s.lastTalkTick) / 1200.0, gapSpread, s.hopperCatches)
            + "\n  " + (restricted(target) ? "RESTRICTED (no rare catches)" : "not restricted");
        context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GOLD), false);
        return s.score;
    }

    private static int setRestricted(CommandContext<CommandSourceStack> context, boolean on) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        restrict(target, on);
        context.getSource().sendSuccess(() -> Component.literal("[FishWatch] " + target.getGameProfile().name()
            + (on ? " is now restricted: no rare catches (they aren't told)." : " is no longer restricted.")).withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int clearCmd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        clear(target);
        context.getSource().sendSuccess(() -> Component.literal("[FishWatch] " + target.getGameProfile().name() + "'s suspicion stats were cleared.")
            .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }
}

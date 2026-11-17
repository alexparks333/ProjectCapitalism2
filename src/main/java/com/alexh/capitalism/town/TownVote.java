package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Town votes (Town.md > Town votes): the Mayor (or an operator) asks the town a yes-or-no question with /startvote.
// Everyone sees it in chat with [Yes] / [No] buttons, and a bar along the top of the screen shows the question and the
// count as it goes. Vote by typing yes or no in chat (that's taken as your vote, not said out loud), clicking a button
// or /vote yes|no. One vote each; you can change it while the vote is open. /endvote closes it and tells everyone how
// it went. One vote at a time; it isn't kept over a server restart.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TownVote {

    private static String question;
    private static UUID askedBy;
    private static Component askedByName;
    private static final Map<UUID, Boolean> votes = new HashMap<>();
    private static ServerBossEvent bar;

    private TownVote() {
    }

    public static boolean open() {
        return question != null;
    }

    public static int yes() {
        return (int) votes.values().stream().filter(v -> v).count();
    }

    public static int no() {
        return votes.size() - yes();
    }

    // ── starting, voting, ending (also used by the tests) ──

    public static void start(MinecraftServer server, String q, UUID by) {
        question = q;
        askedBy = by;
        ServerPlayer asker = by == null ? null : server.getPlayerList().getPlayer(by);
        TownHall.Office office = asker == null ? null : TownHall.get(server).officeOf(by);
        askedByName = asker == null ? Component.literal("The town")
            : office != null ? TownHall.title(office).append(Component.literal(asker.getGameProfile().name()).withStyle(ChatFormatting.WHITE))
            : Component.literal(asker.getGameProfile().name()).withStyle(ChatFormatting.WHITE);
        votes.clear();
        bar = new ServerBossEvent(UUID.randomUUID(), barText(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        refresh();
    }

    // records a vote; returns false if there's no vote open
    public static boolean vote(ServerPlayer player, boolean yes) {
        if (!open()) return false;
        Boolean before = votes.put(player.getUUID(), yes);
        refresh();
        if (before == null || before != yes) {
            player.sendSystemMessage(tag().append(Component.literal(before == null ? "You voted " : "You changed your vote to ").withStyle(ChatFormatting.GRAY))
                .append(answer(yes)).append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
        } else {
            player.sendSystemMessage(tag().append(Component.literal("You already voted ").withStyle(ChatFormatting.GRAY)).append(answer(yes))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
        }
        return true;
    }

    public static void end(MinecraftServer server) {
        if (!open()) return;
        int y = yes();
        int n = no();
        MutableComponent result = y > n ? Component.literal("YES wins").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            : n > y ? Component.literal("NO wins").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            : Component.literal("It's a tie").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(tag().append(Component.literal("The vote is closed: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\"" + question + "\"").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)), false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("  ").append(result)
            .append(Component.literal("  (Yes " + y + ", No " + n + ", " + votes.size() + (votes.size() == 1 ? " vote)" : " votes)")).withStyle(ChatFormatting.GRAY)), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        clear();
    }

    private static void clear() {
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        question = null;
        askedBy = null;
        askedByName = null;
        votes.clear();
    }

    // the bar: the question and the count, filled by the share of yes votes (half-way when nobody has voted)
    private static void refresh() {
        if (bar == null) return;
        bar.setName(barText());
        int total = votes.size();
        bar.setProgress(total == 0 ? 0.5F : yes() / (float) total);
        bar.setColor(total == 0 ? BossEvent.BossBarColor.YELLOW : yes() > no() ? BossEvent.BossBarColor.GREEN
            : no() > yes() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
    }

    private static Component barText() {
        return Component.literal("Town vote: ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(question).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("   Yes " + yes()).withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("No " + no()).withStyle(ChatFormatting.RED));
    }

    // ── chat: a plain "yes" or "no" while a vote is open is a vote ──

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (!open()) return;
        String text = event.getRawText().trim().toLowerCase().replaceAll("[.!]+$", "");
        Boolean yes = switch (text) {
            case "yes", "y", "yea", "aye" -> true;
            case "no", "n", "nay" -> false;
            default -> null;
        };
        if (yes == null) return;
        event.setCanceled(true);
        ServerPlayer player = event.getPlayer();
        player.level().getServer().execute(() -> vote(player, yes));
    }

    // people who join while a vote is open see the bar and the question
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (open() && bar != null && event.getEntity() instanceof ServerPlayer p) {
            bar.addPlayer(p);
            p.sendSystemMessage(ask());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clear();
    }

    // ── commands ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("startvote")
            .then(Commands.argument("question", StringArgumentType.greedyString())
                .executes(c -> startCommand(c.getSource(), StringArgumentType.getString(c, "question")))));
        event.getDispatcher().register(Commands.literal("endvote").executes(c -> endCommand(c.getSource())));
        event.getDispatcher().register(Commands.literal("vote")
            .executes(c -> status(c.getSource()))
            .then(Commands.literal("yes").executes(c -> voteCommand(c.getSource(), true)))
            .then(Commands.literal("no").executes(c -> voteCommand(c.getSource(), false))));
    }

    // the Mayor or an operator
    private static boolean mayMind(CommandSourceStack source) {
        if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        ServerPlayer p = source.getPlayer();
        return p != null && TownHall.get(source.getServer()).officeOf(p.getUUID()) == TownHall.Office.MAYOR;
    }

    private static int startCommand(CommandSourceStack source, String q) {
        if (!mayMind(source)) {
            source.sendFailure(Component.literal("Only the Mayor can start a town vote."));
            return 0;
        }
        if (open()) {
            source.sendFailure(Component.literal("A vote is already open (\"" + question + "\"). /endvote closes it."));
            return 0;
        }
        String text = q.trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1).trim();
        if (text.isEmpty()) return 0;
        MinecraftServer server = source.getServer();
        ServerPlayer by = source.getPlayer();
        start(server, text, by == null ? null : by.getUUID());
        server.getPlayerList().broadcastSystemMessage(ask(), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return 1;
    }

    // the question in chat, who asked, and the buttons
    private static MutableComponent ask() {
        Component who = askedByName != null ? askedByName : Component.literal("The town").withStyle(ChatFormatting.WHITE);
        return tag().append(who).append(Component.literal(" asks the town to vote:\n").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("  \"" + question + "\"\n").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
            .append(Component.literal("  "))
            .append(button("[Yes]", ChatFormatting.GREEN, "/vote yes"))
            .append(Component.literal("  "))
            .append(button("[No]", ChatFormatting.RED, "/vote no"))
            .append(Component.literal("  or type yes / no in chat").withStyle(ChatFormatting.GRAY));
    }

    private static MutableComponent button(String text, ChatFormatting color, String command) {
        return Component.literal(text).withStyle(s -> s.withColor(color).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(command))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Vote " + text.substring(1, text.length() - 1)))));
    }

    private static int endCommand(CommandSourceStack source) {
        if (!mayMind(source)) {
            source.sendFailure(Component.literal("Only the Mayor can close a town vote."));
            return 0;
        }
        if (!open()) {
            source.sendFailure(Component.literal("There's no vote open."));
            return 0;
        }
        end(source.getServer());
        return 1;
    }

    private static int voteCommand(CommandSourceStack source, boolean yes) {
        ServerPlayer p = source.getPlayer();
        if (p == null) return 0;
        if (!vote(p, yes)) {
            source.sendFailure(Component.literal("There's no vote open."));
            return 0;
        }
        return 1;
    }

    private static int status(CommandSourceStack source) {
        if (!open()) {
            source.sendSuccess(() -> tag().append(Component.literal("There's no vote open.").withStyle(ChatFormatting.GRAY)), false);
            return 1;
        }
        source.sendSuccess(TownVote::ask, false);
        source.sendSuccess(() -> Component.literal("  So far: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("Yes " + yes()).withStyle(ChatFormatting.GREEN)).append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("No " + no()).withStyle(ChatFormatting.RED)), false);
        return 1;
    }

    private static MutableComponent answer(boolean yes) {
        return yes ? Component.literal("Yes").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            : Component.literal("No").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    private static MutableComponent tag() {
        return Component.literal("[Town Vote] ").withStyle(ChatFormatting.GOLD);
    }
}

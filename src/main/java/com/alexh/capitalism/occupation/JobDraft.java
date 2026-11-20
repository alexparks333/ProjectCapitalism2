package com.alexh.capitalism.occupation;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.network.DraftStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The job drafts (Jobs.md > The job draft): how the server starts. There's an Essential draft and a Fun draft, each its
// own list. The admin adds players one by one (/admin draft essential add <player>), then starts it; players pick one at
// a time in that order. More players can be added to the end while it runs. Once anyone is on a draft's list, nobody
// picks that kind of job outside the draft until it's over (or stopped). No cap: any number can take the same job.
// Kept in memory only: a server restart ends both.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class JobDraft {

    public record Entry(UUID id, String name) {
    }

    // One draft: its list, whether it's started, who is up.
    private static final class Draft {
        final List<Entry> order = new ArrayList<>();
        boolean started;
        int index;

        Entry current() {
            return started && index < order.size() ? order.get(index) : null;
        }
    }

    private static final Map<Job.Category, Draft> DRAFTS = new EnumMap<>(Job.Category.class);

    static {
        for (Job.Category category : Job.Category.values()) DRAFTS.put(category, new Draft());
    }

    private JobDraft() {
    }

    private static Draft draft(Job.Category category) {
        return DRAFTS.get(category);
    }

    // a list exists (being built or running): this kind of job is only picked through the draft
    public static boolean active(Job.Category category) {
        return !draft(category).order.isEmpty();
    }

    public static boolean started(Job.Category category) {
        return draft(category).started;
    }

    public static boolean anyActive() {
        for (Job.Category category : Job.Category.values()) if (active(category)) return true;
        return false;
    }

    public static boolean isTurn(ServerPlayer player, Job.Category category) {
        Entry e = draft(category).current();
        return e != null && e.id().equals(player.getUUID());
    }

    public static String currentName(Job.Category category) {
        Entry e = draft(category).current();
        return e == null ? "" : e.name();
    }

    public static List<String> orderNames(Job.Category category) {
        List<String> names = new ArrayList<>();
        for (Entry e : draft(category).order) names.add(e.name());
        return names;
    }

    public static int position(Job.Category category) {
        return draft(category).index;
    }

    private static String what(Job.Category category) {
        return "the " + category.title + " draft";
    }

    // Adds a player to the end of the list. Returns an error, or null.
    public static String add(MinecraftServer server, Job.Category category, ServerPlayer player) {
        Draft d = draft(category);
        for (Entry e : d.order) {
            if (e.id().equals(player.getUUID())) return player.getGameProfile().name() + " is already on " + what(category) + ".";
        }
        boolean waiting = d.started && d.current() == null;
        d.order.add(new Entry(player.getUUID(), player.getGameProfile().name()));
        server.getPlayerList().broadcastSystemMessage(Component.literal("📜 " + player.getGameProfile().name() + " joins " + what(category)
            + " (#" + d.order.size() + ").").withStyle(ChatFormatting.GOLD), false);
        if (!d.started) {
            player.sendSystemMessage(Component.literal("You're #" + d.order.size() + " to pick a " + category.title
                + " job. Wait for your turn!").withStyle(ChatFormatting.GOLD));
        }
        if (waiting) settle(server, category);
        else syncAll(server);
        return null;
    }

    // Takes a player off the list (an admin changing their mind). If it was their turn, the next player is up.
    public static String remove(MinecraftServer server, Job.Category category, String name) {
        Draft d = draft(category);
        for (int i = 0; i < d.order.size(); i++) {
            if (!d.order.get(i).name().equalsIgnoreCase(name)) continue;
            d.order.remove(i);
            if (i < d.index) d.index--;
            server.getPlayerList().broadcastSystemMessage(Component.literal(name + " was taken off " + what(category) + ".").withStyle(ChatFormatting.GRAY), false);
            if (d.order.isEmpty()) end(server, category, false);
            else if (d.started && i == d.index) settle(server, category);
            else syncAll(server);
            return null;
        }
        return name + " isn't on " + what(category) + ".";
    }

    public static String start(MinecraftServer server, Job.Category category) {
        Draft d = draft(category);
        if (d.order.isEmpty()) return "Nobody is on " + what(category) + " yet: /admin draft " + category.name().toLowerCase() + " add <player>";
        if (d.started) return what(category) + " has already started.";
        d.started = true;
        d.index = 0;
        server.getPlayerList().broadcastSystemMessage(Component.literal("📜 The " + category.title + " job draft begins! Order: "
            + String.join(", ", orderNames(category)) + ".").withStyle(ChatFormatting.GOLD), false);
        settle(server, category);
        return null;
    }

    public static void stop(MinecraftServer server, Job.Category category) {
        end(server, category, false);
        server.getPlayerList().broadcastSystemMessage(Component.literal("The " + category.title + " job draft was stopped: anyone can /pick a "
            + category.title + " job now.").withStyle(ChatFormatting.GOLD), false);
    }

    private static void end(MinecraftServer server, Job.Category category, boolean announce) {
        Draft d = draft(category);
        d.order.clear();
        d.started = false;
        d.index = 0;
        if (announce) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("📜 The " + category.title
                + " job draft is over! Anyone with a free " + category.title + " slot can /pick now.").withStyle(ChatFormatting.GOLD), false);
        }
        syncAll(server);
    }

    // The current picker is passed over (offline, AFK...). They can pick freely once the draft is over.
    public static String skip(MinecraftServer server, Job.Category category) {
        Draft d = draft(category);
        if (d.current() == null) return "Nobody is picking in " + what(category) + " right now.";
        server.getPlayerList().broadcastSystemMessage(Component.literal(currentName(category) + " was skipped in " + what(category) + ".")
            .withStyle(ChatFormatting.GRAY), false);
        d.index++;
        settle(server, category);
        return null;
    }

    // Why this player can't pick this job right now because of a draft, or null if they can.
    public static String blocked(ServerPlayer player, Job job) {
        Job.Category category = job.category;
        Draft d = draft(category);
        if (d.order.isEmpty()) return null;
        // an admin reset their jobs (/admin jobs reset): no draft in the way
        if (player.getData(ModAttachments.JOBS.get()).freePick()) return null;
        if (!d.started) return "The " + category.title + " job draft hasn't started yet: wait for your turn.";
        if (isTurn(player, category)) return null;
        String picker = currentName(category);
        return picker.isEmpty() ? "The " + category.title + " job draft is on: wait for your turn."
            : "The " + category.title + " job draft is on: it's " + picker + "'s turn to pick. Wait for yours!";
    }

    // Called after a pick went through: if it was their draft turn, the next player is up.
    public static void picked(ServerPlayer player, Job job) {
        if (!isTurn(player, job.category)) return;
        MinecraftServer server = player.level().getServer();
        server.getPlayerList().broadcastSystemMessage(Component.literal(player.getGameProfile().name() + " picked " + job.title + ".")
            .withStyle(ChatFormatting.YELLOW), false);
        draft(job.category).index++;
        settle(server, job.category);
    }

    // Moves on past anyone with no free slot of this kind; when everyone on the list has had their turn the draft is
    // over; otherwise tells everyone whose turn it is.
    private static void settle(MinecraftServer server, Job.Category category) {
        Draft d = draft(category);
        while (d.started) {
            if (d.index >= d.order.size()) {
                end(server, category, true);
                return;
            }
            Entry e = d.order.get(d.index);
            ServerPlayer p = server.getPlayerList().getPlayer(e.id());
            // already holds a job of this kind with no slot left: nothing to pick, next
            if (p != null && !p.getData(ModAttachments.JOBS.get()).hasFreeSlot(category)) {
                d.index++;
                continue;
            }
            server.getPlayerList().broadcastSystemMessage(Component.literal("➤ It's " + e.name() + "'s turn to pick a " + category.title + " job ("
                + (d.index + 1) + " of " + d.order.size() + ")."
                + (p == null ? " (They're offline: an admin can /admin draft " + category.name().toLowerCase() + " skip.)" : ""))
                .withStyle(ChatFormatting.AQUA), false);
            if (p != null) callUp(p, category);
            break;
        }
        syncAll(server);
    }

    private static void callUp(ServerPlayer p, Job.Category category) {
        p.sendSystemMessage(Component.literal("Your turn! Pick your " + category.title + " job.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        PacketDistributor.sendToPlayer(p, new com.alexh.capitalism.network.OpenJobPickPacket());
    }

    // What the job screen says about this draft for this player ("" = no draft), and whether it greys that column out.
    private static String line(ServerPlayer p, Job.Category category) {
        Draft d = draft(category);
        if (d.order.isEmpty()) return "";
        if (!d.started) return category.title + " draft: waiting to start.";
        if (isTurn(p, category)) return category.title + " draft: your turn! Pick your " + category.title + " job.";
        return category.title + " draft: " + currentName(category) + " is picking. Wait for your turn.";
    }

    public static void sync(ServerPlayer p) {
        Job.Category ess = Job.Category.ESSENTIAL, fun = Job.Category.FUN;
        // reset by an admin: the drafts don't apply to them
        if (p.getData(ModAttachments.JOBS.get()).freePick()) {
            PacketDistributor.sendToPlayer(p, new DraftStatePacket("", false, "", false));
            return;
        }
        PacketDistributor.sendToPlayer(p, new DraftStatePacket(
            line(p, ess), active(ess) && !isTurn(p, ess),
            line(p, fun), active(fun) && !isTurn(p, fun)));
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    // Called from JobEvents.onLogin while a draft has a list.
    public static void onLogin(ServerPlayer player) {
        sync(player);
        if (player.getData(ModAttachments.JOBS.get()).freePick()) {
            player.sendSystemMessage(Component.literal("You have a job slot to fill: type /pick.").withStyle(ChatFormatting.GOLD));
            return;
        }
        for (Job.Category category : Job.Category.values()) {
            if (!active(category)) continue;
            if (isTurn(player, category)) {
                if (player.getData(ModAttachments.JOBS.get()).hasFreeSlot(category)) callUp(player, category);
                else {
                    draft(category).index++;
                    settle(player.level().getServer(), category);
                }
            } else {
                player.sendSystemMessage(Component.literal(line(player, category)).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    // Clears everything (tests, server stop)
    public static void reset() {
        for (Draft d : DRAFTS.values()) {
            d.order.clear();
            d.started = false;
            d.index = 0;
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        reset();
    }
}

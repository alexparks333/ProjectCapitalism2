package com.alexh.capitalism.bounty;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.BankerEvents;
import com.alexh.capitalism.economy.GoldMoney;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.function.Predicate;

// The Bounty Clerk's rules (Bounties.md):
//  - one task posted for the whole server (BountyBoard): everyone sees the same one; the moment someone takes it, a new
//    one is posted (Easy 40, Medium 30, Hard 20, Heroic 10; never one of the last 20 posted). A town task someone can't
//    do ("feed your pet" with no pet) stays posted; they just can't take it;
//  - a player holds ONE task at a time: taking the posted one exchanges it for theirs (no refund), for a flat fee of 2
//    gold nuggets (not an NPC shop price: CLAUDE.md); one take per Calendar day, even if they finish it at once;
//  - tasks never expire; progress counts from when it's taken (BountyEvents); a delivery counts what's in their
//    inventory, takes it at turn-in and always pays more than the Banker would; abandon drops it (no refund);
//  - turn-ins pay in gold; the first 3 players on the server to turn in 50 each get one Fun Wishing Star (their place
//    is reserved the moment they reach 50; they claim it in the Ledger).
@EventBusSubscriber(modid = com.alexh.capitalism.Capitalism.MODID)
public final class Bounties {

    // what taking a task costs, in gold nuggets, by tier: Easy 2 nuggets, Medium 6, Hard 1 gold, Heroic 2 gold (a flat fee,
    // not a shop price)
    public static final int[] FEES = {2, 6, 9, 18};

    public static int fee(BountyTask task) {
        return task == null ? FEES[0] : FEES[task.tier.ordinal()];
    }
    public static final int MILESTONE = 50;

    private Bounties() {
    }

    public static BountyData of(Player player) {
        return player.getData(ModAttachments.BOUNTIES.get());
    }

    static void save(ServerPlayer player, BountyData data) {
        player.setData(ModAttachments.BOUNTIES.get(), data);   // saves it and sends it to them
    }

    public static long today(Level level) {
        return Calendar.date(level).index();
    }

    // ticks until tomorrow (the next take)
    public static long ticksToTomorrow(Level level) {
        return Calendar.DAY_TICKS - Math.floorMod(Calendar.clock(level), Calendar.DAY_TICKS);
    }

    // ── the board ──

    // the task posted now (posting one if there's none yet)
    public static BountyData.Open posted(MinecraftServer server) {
        BountyBoard board = BountyBoard.get(server);
        if (board.posted == null || board.posted.task() == null) board.post(board.roll(server, server.overworld().getRandom()));
        return board.posted;
    }

    // copy the board onto a player's data for their screen: the posted task, the winners, their place
    static void copyBoard(ServerPlayer player, BountyData data) {
        MinecraftServer server = player.level().getServer();
        BountyBoard board = BountyBoard.get(server);
        data.offer = posted(server);
        data.winners.clear();
        for (BountyBoard.Place p : board.places) data.winners.add(p.name());
        data.place = board.placeOf(player.getUUID());
        data.claimed = data.place >= 0 && board.places.get(data.place).claimed();
        // the standings, every board: the top 10, their rank (★ by the bounty milestone's winners)
        java.util.Set<java.util.UUID> stars = new java.util.HashSet<>();
        for (BountyBoard.Place p : board.places) stars.add(p.id());
        Standings standings = Standings.get(server);
        data.boards.clear();
        for (Standings.Category category : Standings.Category.values()) {
            List<Standings.Entry> ranking = standings.ranking(category);
            List<BountyData.Row> top = new java.util.ArrayList<>();
            int rank = 0;
            long score = 0;
            for (int i = 0; i < ranking.size(); i++) {
                Standings.Entry e = ranking.get(i);
                boolean me = e.id().equals(player.getUUID());
                if (me) {
                    rank = i + 1;
                    score = e.score();
                }
                boolean star = category == Standings.Category.BOUNTIES && stars.contains(e.id());
                if (i < 10) top.add(new BountyData.Row(e.name(), e.score(), e.extra(), star, me));
            }
            data.boards.add(new BountyData.Board(category.id, top, rank, score));
        }
    }

    // note a player's bounties on the standings (now, for breaking ties)
    static void stand(ServerPlayer player, BountyData data) {
        MinecraftServer server = player.level().getServer();
        Standings.get(server).set(Standings.Category.BOUNTIES, player.getUUID(), player.getGameProfile().name(), data.completed, data.earned,
            server.overworld().getGameTime());
    }

    // send the board to everyone on (it changed)
    public static void syncBoard(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            BountyData data = of(p);
            copyBoard(p, data);
            save(p, data);
        }
    }

    public static void syncBoard(ServerPlayer player) {
        BountyData data = of(player);
        copyBoard(player, data);
        save(player, data);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stand(player, of(player));   // seeded from their own data, so progress from before shows
            syncBoard(player);
        }
    }

    // what the Banker would pay for `count` of this item (nuggets)
    public static long bankValue(Item item, int count) {
        var price = BankerEvents.prices().get(item);
        if (price == null || price.nuggets() <= 0 || price.per() <= 0) return 0;
        return (long) (count / price.per()) * price.nuggets();
    }

    // what turning it in pays (nuggets): its gold for that amount; a delivery at least a gold more than the Banker gives
    // for its items
    public static long reward(BountyTask task, int amount) {
        long pay = task.goldFor(amount);
        if (task.kind == BountyTask.Kind.DELIVER) pay = Math.max(pay, bankValue(task.item(), amount) + GoldMoney.INGOT);
        return pay;
    }

    // (game tests, dev) post this task, this many
    public static void setPosted(MinecraftServer server, String id, int amount) {
        BountyTask task = BountyTask.byId(id);
        BountyBoard.get(server).post(new BountyData.Open(id, amount, reward(task, amount), 0));
        syncBoard(server);
    }

    // ── taking, turning in ──

    public static boolean takenToday(Player player) {
        return of(player).takenDay == today(player.level());
    }

    public enum TakeResult { OK, ALREADY_TODAY, CANT_DO_IT, NO_GOLD }

    // pay the fee and take the posted task (it replaces theirs, no refund); a new one goes up for everyone else
    public static TakeResult take(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        BountyData data = of(player);
        long day = today(player.level());
        if (data.takenDay == day) return TakeResult.ALREADY_TODAY;
        BountyData.Open posted = posted(server);
        if (!posted.task().availableTo(player)) return TakeResult.CANT_DO_IT;
        if (!player.isCreative() && !GoldMoney.take(player, fee(posted.task()))) return TakeResult.NO_GOLD;
        data = of(player);
        data.open.clear();
        data.open.add(posted.withProgress(0));
        data.takenDay = day;
        save(player, data);
        BountyBoard board = BountyBoard.get(server);
        board.post(board.roll(server, player.getRandom()));
        syncBoard(server);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return TakeResult.OK;
    }

    public static boolean abandon(ServerPlayer player) {
        BountyData data = of(player);
        if (data.open.isEmpty()) return false;
        data.open.clear();
        save(player, data);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    // how far along it is (a delivery: what's in their inventory now)
    public static int progress(Player player, BountyData.Open open) {
        BountyTask task = open.task();
        if (task == null) return 0;
        if (task.kind == BountyTask.Kind.DELIVER) return Math.min(open.amount(), player.getInventory().countItem(task.item()));
        return Math.min(open.amount(), open.progress());
    }

    public static boolean done(Player player, BountyData.Open open) {
        return open.task() != null && progress(player, open) >= open.amount();
    }

    // count `amount` towards their task, if this matches it (the events call this)
    public static void count(ServerPlayer player, Predicate<BountyTask> matches, int amount) {
        BountyData data = of(player);
        boolean changed = false;
        for (int i = 0; i < data.open.size(); i++) {
            BountyData.Open o = data.open.get(i);
            BountyTask task = o.task();
            if (task == null || task.kind == BountyTask.Kind.DELIVER || o.progress() >= o.amount() || !matches.test(task)) continue;
            data.open.set(i, o.withProgress(Math.min(o.amount(), o.progress() + amount)));
            changed = true;
            if (o.progress() + amount >= o.amount()) {
                player.sendSystemMessage(Component.literal("📜 Bounty done: " + o.title() + ". Turn it in at the Bounty Clerk.").withStyle(ChatFormatting.GOLD));
            }
        }
        if (changed) save(player, data);
    }

    // set their task's progress, if this matches it (a count that can go back down: biomes today, kills this Blood Moon)
    public static void setProgress(ServerPlayer player, Predicate<BountyTask> matches, int value) {
        BountyData data = of(player);
        boolean changed = false;
        for (int i = 0; i < data.open.size(); i++) {
            BountyData.Open o = data.open.get(i);
            BountyTask task = o.task();
            if (task == null || o.progress() >= o.amount() || !matches.test(task) || o.progress() == Math.min(o.amount(), value)) continue;
            data.open.set(i, o.withProgress(Math.min(o.amount(), value)));
            changed = true;
            if (value >= o.amount()) {
                player.sendSystemMessage(Component.literal("📜 Bounty done: " + o.title() + ". Turn it in at the Bounty Clerk.").withStyle(ChatFormatting.GOLD));
            }
        }
        if (changed) save(player, data);
    }

    // does this player hold an unfinished task of this kind?
    public static boolean wants(Player player, BountyTask.Kind kind) {
        for (BountyData.Open o : of(player).open) if (o.task() != null && o.task().kind == kind && o.progress() < o.amount()) return true;
        return false;
    }

    // turn in their finished task: a delivery hands over its items; the reward is paid with a stamp and a flourish
    public static boolean turnIn(ServerPlayer player, Entity clerk) {
        BountyData data = of(player);
        if (data.open.isEmpty()) return false;
        BountyData.Open open = data.open.get(0);
        BountyTask task = open.task();
        if (task == null || !done(player, open)) return false;
        if (task.kind == BountyTask.Kind.DELIVER) takeItems(player, task.item(), open.amount());
        long pay = open.reward();
        GoldMoney.give(player, pay);
        data.open.clear();
        data.completed++;
        data.earned += pay;
        save(player, data);
        stand(player, data);
        var level = player.level();
        Entity at = clerk != null ? clerk : player;
        level.playSound(null, at.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.NEUTRAL, 1.0F, 0.8F);
        level.playSound(null, at.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.25F, 1.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX(), at.getY() + 1.6, at.getZ(), 14, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.WAX_ON, at.getX(), at.getY() + 1.2, at.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        player.sendSystemMessage(Component.literal("📜 Bounty paid: " + open.title() + " — " + GoldMoney.format(pay)
            + " (" + data.completed + " turned in)").withStyle(ChatFormatting.GOLD));
        syncBoard(level.getServer());   // the standings moved
        // reaching 50: a place among the first 3 is reserved now, whenever they claim it
        if (data.completed >= MILESTONE) {
            BountyBoard board = BountyBoard.get(level.getServer());
            int before = board.places.size();
            int place = board.reserve(player);
            if (place >= 0 && board.places.size() > before) {
                player.sendSystemMessage(Component.literal("🏅 " + MILESTONE + " bounties! You're " + ordinal(place + 1)
                    + ": claim your reward in the Clerk's Ledger.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                syncBoard(level.getServer());
            }
        }
        return true;
    }

    private static void takeItems(ServerPlayer player, Item item, int count) {
        var inv = player.getInventory();
        int left = count;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) continue;
            int n = Math.min(left, s.getCount());
            s.shrink(n);
            left -= n;
        }
    }

    public static String ordinal(int n) {
        return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
    }

    // the prize: a reserved place, not claimed yet -> one Fun Wishing Star, a fanfare, everyone told
    public static boolean claim(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        BountyBoard board = BountyBoard.get(server);
        int place = board.placeOf(player.getUUID());
        if (place < 0 || board.places.get(place).claimed()) return false;
        board.markClaimed(place);
        ItemStack star = new ItemStack(ModItems.FUN_WISHING_STAR.get());
        if (!player.getInventory().add(star)) player.drop(star, false, net.minecraft.util.Prediction.SERVER_ONLY);
        var level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 0.8, 0.6, 0.3);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.5, 0.6, 0.5, 0.15);
        server.getPlayerList().broadcastSystemMessage(Component.literal("🏅 " + player.getGameProfile().name() + " is the " + ordinal(place + 1)
            + " to complete " + MILESTONE + " bounties and earns a Fun Wishing Star!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        syncBoard(server);
        return true;
    }

    // (admin, by hand only) wipe one player's bounties; clear the milestone places
    public static void reset(ServerPlayer player) {
        BountyData fresh = new BountyData();
        stand(player, fresh);
        copyBoard(player, fresh);
        save(player, fresh);
    }

    public static void resetWinners(MinecraftServer server) {
        BountyBoard.get(server).resetPlaces();
        syncBoard(server);
    }

    // (dev: client/PhotoShoot) a made-up player on the standings
    public static void devStanding(MinecraftServer server, String name, int completed, long earned) {
        Standings.get(server).set(Standings.Category.BOUNTIES, java.util.UUID.nameUUIDFromBytes(name.getBytes()), name, completed, earned, server.overworld().getGameTime());
    }

    // (dev: client/PhotoShoot) pretend they've just reached the milestone: reserve their place if one's left
    public static void devReserve(ServerPlayer player) {
        BountyBoard.get(player.level().getServer()).reserve(player);
        syncBoard(player.level().getServer());
    }

    // (dev: client/PhotoShoot, game tests) pretend they've turned in this many
    public static void devSetCompleted(ServerPlayer player, int completed, long earned) {
        BountyData data = of(player);
        data.completed = completed;
        data.earned = earned;
        save(player, data);
        stand(player, data);
        syncBoard(player.level().getServer());
    }
}

package com.alexh.capitalism.bounty;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// The Bounty Clerk's rules (Bounties.md): one task posted for everyone, a new one the moment someone takes it, never one
// of the last 20 posted; one task per player (taking exchanges it); one take a day, even after a turn-in; exactly 2
// nuggets; progress only after taking it; a turn-in pays; a delivery takes its items and pays more than the Banker;
// abandon; the tier weights; only the first 3 to reach 50 get a Fun Wishing Star (their place reserved on reaching 50).
public final class BountyTests {

    static {
        GameTests.register("bounties", BountyTests::bounties);
    }

    private BountyTests() {
    }

    public static void init() {
    }

    private static void nextDay(ServerLevel level) {
        Calendar.setClock(level, Calendar.clock(level) + Calendar.DAY_TICKS);
    }

    private static void killZombie(GameTests.Checks c, ServerPlayer player) {
        ServerLevel level = c.level();
        Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        Vec3 at = c.at(1.5, 1, 1.5);
        zombie.snapTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(zombie);
        zombie.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
    }

    // take the posted task (setting it first), a day after the last
    private static Bounties.TakeResult takeTask(ServerPlayer player, String id, int amount) {
        Bounties.setPosted(player.level().getServer(), id, amount);
        return Bounties.take(player);
    }

    // turn in a "Deliver 1 Bread"-sized task right away (for counting turn-ins)
    private static void quickTurnIn(ServerLevel level, ServerPlayer player) {
        nextDay(level);
        GoldMoney.give(player, 9);
        takeTask(player, "deliver_bread", 12);
        player.getInventory().add(new ItemStack(Items.BREAD, 12));
        Bounties.turnIn(player, null);
    }

    private static void bounties(GameTests.Checks c) {
        ServerLevel level = c.level();
        MinecraftServer server = level.getServer();
        ServerPlayer player = c.player(), other = c.player();
        Bounties.reset(player);
        Bounties.reset(other);
        Bounties.resetWinners(server);
        player.getInventory().clearContent();

        // ── the catalogue ──
        Set<String> ids = new HashSet<>();
        boolean unique = true;
        for (BountyTask t : BountyTask.all()) unique &= ids.add(t.id);
        c.check(unique && BountyTask.all().size() >= 120, "about 125 tasks, every id different (" + BountyTask.all().size() + ")");
        boolean deliveriesPay = true;
        for (BountyTask t : BountyTask.all()) {
            if (t.kind != BountyTask.Kind.DELIVER) continue;
            for (int amount : new int[]{t.min, t.max}) deliveriesPay &= Bounties.reward(t, amount) > Bounties.bankValue(t.item(), amount);
        }
        c.check(deliveriesPay, "every delivery pays more than the Banker gives for its items");
        c.check(BountyTask.all().stream().filter(t -> t.tier == BountyTask.Tier.HARD).allMatch(t -> t.goldFor(t.min) >= 10 * 9 && t.goldFor(t.max) <= 20 * 9),
            "Hard tasks pay 10 to 20 gold");

        // ── the tier weights hold over many rolls (Easy 40, Medium 30, Hard 20, Heroic 10) ──
        Map<BountyTask.Tier, Integer> rolls = new EnumMap<>(BountyTask.Tier.class);
        RandomSource random = RandomSource.create(11L);
        for (int i = 0; i < 4000; i++) rolls.merge(BountyBoard.rollTier(random), 1, Integer::sum);
        boolean weights = true;
        for (BountyTask.Tier t : BountyTask.Tier.values()) weights &= Math.abs(rolls.getOrDefault(t, 0) / 4000.0 - t.weight / 100.0) < 0.03;
        c.check(weights, "tiers roll about 40 / 30 / 20 / 10 " + rolls);

        // ── one shared posted task, a new one the moment it's taken ──
        BountyData.Open posted = Bounties.posted(server);
        c.check(posted.equals(Bounties.posted(server)) && Bounties.of(other).offer() != null, "one task is posted for everyone");
        c.check(!BountyTask.byId("feed_pet").availableTo(player), "with no pet, \"feed your pet\" can't be taken");

        // ── progress only after taking it; the fee; a new task posted ──
        killZombie(c, player);
        GoldMoney.give(player, 9);
        long before = GoldMoney.carried(player);
        c.check(takeTask(player, "kill_zombies", 2) == Bounties.TakeResult.OK && GoldMoney.carried(player) == before - 2, "taking it costs exactly 2 gold nuggets");
        c.check(Bounties.of(player).task() != null && Bounties.of(player).task().progress() == 0, "a zombie killed before taking it doesn't count");
        BountyData.Open next = Bounties.posted(server);
        c.check(!next.id().equals("kill_zombies") && next.equals(Bounties.of(other).offer()),
            "taking it posts a new task, and everyone else sees the new one (" + next.title() + ")");
        c.check(BountyBoard.get(server).history().contains("kill_zombies") && BountyBoard.get(server).history().contains(next.id()),
            "the board remembers what it posted (never again among the last 20)");

        // ── one take a day, even after turning it in ──
        killZombie(c, player);
        killZombie(c, player);
        BountyData.Open open = Bounties.of(player).task();
        long had = GoldMoney.carried(player);
        c.check(Bounties.done(player, open) && Bounties.turnIn(player, null) && GoldMoney.carried(player) == had + open.reward()
            && Bounties.of(player).completed() == 1 && Bounties.of(player).task() == null, "turning it in pays its reward (" + GoldMoney.format(open.reward()) + ")");
        c.check(takeTask(player, "kill_skeletons", 4) == Bounties.TakeResult.ALREADY_TODAY && Bounties.of(player).task() == null,
            "one take a day, even after turning the first in");

        // ── one task at a time: taking another exchanges it ──
        nextDay(level);
        takeTask(player, "mine_stone", 50);
        Bounties.count(player, t -> t.id.equals("mine_stone"), 10);
        nextDay(level);
        c.check(takeTask(player, "kill_skeletons", 4) == Bounties.TakeResult.OK && Bounties.of(player).open().size() == 1
            && Bounties.of(player).task().id().equals("kill_skeletons"), "one task at a time: taking the posted one exchanges it");

        // ── a delivery takes its items; abandon ──
        nextDay(level);
        takeTask(player, "deliver_bread", 12);
        player.getInventory().add(new ItemStack(Items.BREAD, 14));
        c.check(Bounties.turnIn(player, null) && player.getInventory().countItem(Items.BREAD) == 2, "turning in a delivery takes exactly its 12 bread");
        nextDay(level);
        takeTask(player, "kill_skeletons", 4);
        c.check(Bounties.abandon(player) && Bounties.of(player).task() == null, "abandoning drops the task (no refund)");

        // ── the race: only the first 3 to reach 50 get a Fun Wishing Star; the place is reserved on reaching 50 ──
        ServerPlayer[] racers = {c.player(), c.player(), c.player(), c.player()};
        for (ServerPlayer r : racers) {
            Bounties.reset(r);
            r.getInventory().clearContent();
        }
        for (int i = 0; i < 4; i++) {
            Bounties.devSetCompleted(racers[i], Bounties.MILESTONE - 1, 0);
            quickTurnIn(level, racers[i]);
        }
        var places = BountyBoard.get(server).places();
        c.check(places.size() == 3 && places.get(0).id().equals(racers[0].getUUID()) && places.get(2).id().equals(racers[2].getUUID()),
            "the first 3 to reach 50 have their places, in order, reserved before anyone claims");
        boolean third = Bounties.claim(racers[2]), first = Bounties.claim(racers[0]), fourth = Bounties.claim(racers[3]), again = Bounties.claim(racers[0]);
        c.check(third && first && !again && racers[2].getInventory().countItem(ModItems.FUN_WISHING_STAR.get()) == 1
            && racers[0].getInventory().countItem(ModItems.FUN_WISHING_STAR.get()) == 1, "a reserved place claims one Fun Wishing Star, once (claiming late doesn't lose it)");
        c.check(!fourth && racers[3].getInventory().countItem(ModItems.FUN_WISHING_STAR.get()) == 0
            && racers[3].getInventory().countItem(ModItems.ESSENTIAL_WISHING_STAR.get()) == 0, "the 4th to reach 50 gets nothing");
        c.check(Bounties.of(racers[3]).winners().size() == 3 && Bounties.of(racers[1]).place() == 1, "everyone's Ledger shows the winners and their own place");
        // ── the standings: most first, on a tie whoever got there first; offline players stay on it, and it's saved ──
        Standings standings = Standings.get(server);
        Standings.Category B = Standings.Category.BOUNTIES;
        java.util.UUID ann = java.util.UUID.randomUUID(), bo = java.util.UUID.randomUUID(), cy = java.util.UUID.randomUUID();
        standings.set(B, ann, "Ann", 5, 90, 100);
        standings.set(B, bo, "Bo", 5, 80, 50);
        standings.set(B, cy, "Cy", 7, 120, 300);
        java.util.List<java.util.UUID> order = standings.ranking(B).stream().map(Standings.Entry::id).filter(id -> id.equals(ann) || id.equals(bo) || id.equals(cy)).toList();
        c.check(order.equals(java.util.List.of(cy, bo, ann)), "standings: most turned in first; on a tie, whoever reached it first (Cy 7, Bo 5 earlier, Ann 5)");
        standings.set(B, ann, "Ann", 5, 90, 999);
        c.check(standings.ranking(B).stream().filter(s -> s.id().equals(ann)).findFirst().get().reachedAt() == 100, "the tie-break time only moves when the score does");
        var saved = Standings.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, standings).getOrThrow();
        Standings loaded = Standings.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, saved).getOrThrow();
        c.check(loaded.ranking(B).stream().anyMatch(s -> s.id().equals(bo) && s.score() == 5 && s.name().equals("Bo")),
            "offline players stay on the standings, and they're saved with the world");
        Bounties.syncBoard(racers[0]);
        BountyData.Board shown = Bounties.of(racers[0]).board(B.id);
        c.check(shown != null && shown.top().stream().anyMatch(r -> r.name().equals("Cy")) && shown.rank() > 0
            && shown.top().stream().anyMatch(r -> r.me() && r.star()), "everyone's Ledger gets each board's top 10 (offline players too), their rank, the ★ winners");
        standings.remove(B, ann);
        standings.remove(B, bo);
        standings.remove(B, cy);        Bounties.resetWinners(server);
        for (ServerPlayer r : racers) Bounties.reset(r);
        Bounties.reset(player);
        Bounties.reset(other);
    }
}

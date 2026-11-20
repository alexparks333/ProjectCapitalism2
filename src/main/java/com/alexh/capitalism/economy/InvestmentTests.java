package com.alexh.capitalism.economy;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

// The Investment Chest (Economy.md > The Investment Chest): each player's own vault, Gold Ingots only; its gold counts
// in net worth exactly once; /pay can't spend it; each morning pays the rate on the LEAST it held since the last
// morning (so a quick deposit before morning earns nothing), once a day; returns collect into the inventory without
// changing net worth; offline players get paid too.
public final class InvestmentTests {

    static {
        GameTests.register("investment_chest", InvestmentTests::investments);
    }

    private InvestmentTests() {
    }

    public static void init() {
    }

    private static void investments(GameTests.Checks c) {
        MinecraftServer server = c.level().getServer();
        InvestmentBank bank = InvestmentBank.get(server);
        GoldLedger ledger = GoldLedger.get(server);
        ServerPlayer alice = c.player(), bob = c.player();
        alice.getInventory().clearContent();
        bob.getInventory().clearContent();
        alice.getEnderChestInventory().clearContent();


        // ── the rate: a market, 4-7% mostly, rarely a bull run (8-9%) or a crash (2-3%), held for 3 days ──
        c.check(InvestmentBank.dailyReturn(GoldMoney.fromGold(50), 6) == GoldMoney.fromGold(3), "50 gold at 6% earns 3 gold a day");
        c.check(InvestmentBank.dailyReturn(InvestmentBank.MAX, 6) == 103 && InvestmentBank.MAX == GoldMoney.fromGold(192),
            "3 full stacks (192 gold) at 6% earn 11 gold 4 nuggets a day");
        c.check(InvestmentBank.dailyReturn(GoldMoney.fromGold(50), 9) == 40 && InvestmentBank.dailyReturn(GoldMoney.fromGold(50), 2) == 9,
            "50 gold on a 9% bull run: 4 gold 4 nuggets; on a 2% crash: 1 gold");
        long seed = server.overworld().getSeed();
        int lows = 0, highs = 0, odd = 0, wobbly = 0;
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (long stretch = 0; stretch < 3000; stretch++) {
            int r = InvestmentBank.marketRate(seed, stretch * 3);
            seen.add(r);
            if (r < 2 || r > 9) odd++;
            if (r <= 3) lows++;
            if (r >= 8) highs++;
            if (InvestmentBank.marketRate(seed, stretch * 3 + 1) != r || InvestmentBank.marketRate(seed, stretch * 3 + 2) != r) wobbly++;
        }
        c.check(odd == 0 && seen.size() == 8, "every rate is 2 to 9%, and all of them happen (" + seen + ")");
        c.check(wobbly == 0, "a rate holds for its 3 days");
        c.check(lows > 200 && lows < 400 && highs > 200 && highs < 400,
            "crashes and bull runs are rare, about 1 stretch in 10 each (" + lows + " and " + highs + " of 3000)");
        c.check(InvestmentBank.daysLeft(30) == 3 && InvestmentBank.daysLeft(31) == 2 && InvestmentBank.daysLeft(32) == 1,
            "the screen counts the days left at a rate");

        // ── what goes in, whose it is ──
        InvestmentBank.Account a = bank.account(alice.getUUID());
        a.clearContent();
        a.returns = 0;
        InvestmentBank.Account b = bank.account(bob.getUUID());
        c.check(a.getContainerSize() == 3 && a != b, "everyone has their own 3-slot vault");
        c.check(a.canPlaceItem(0, new ItemStack(Items.GOLD_INGOT)) && !a.canPlaceItem(0, new ItemStack(Items.GOLD_BLOCK))
            && !a.canPlaceItem(0, new ItemStack(Items.GOLD_NUGGET)) && !a.canPlaceItem(0, new ItemStack(Items.DIAMOND)),
            "it takes Gold Ingots only");
        InvestmentMenu menu = new InvestmentMenu(0, alice.getInventory(), a, InvestmentMenu.liveData(a, server), null);
        c.check(!menu.getSlot(0).mayPlace(new ItemStack(Items.GOLD_BLOCK)) && menu.getSlot(0).mayPlace(new ItemStack(Items.GOLD_INGOT)),
            "...in the menu too");
        alice.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        menu.quickMoveStack(alice, InvestmentBank.SLOTS + 27);   // hotbar slot 0
        c.check(a.isEmpty(), "shift-clicking a diamond doesn't put it in");
        alice.getInventory().clearContent();

        // ── net worth: counted once, before and after putting it in ──
        alice.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 50));
        GoldLeaderboard.recount(server, ledger);
        long before = ledger.netWorth(alice.getUUID());
        menu.quickMoveStack(alice, InvestmentBank.SLOTS + 27);
        c.check(a.invested() == GoldMoney.fromGold(50) && GoldMoney.carried(alice) == 0, "shift-click puts the 50 ingots in the vault");
        GoldLeaderboard.recount(server, ledger);
        c.check(before == GoldMoney.fromGold(50) && ledger.netWorth(alice.getUUID()) == before,
            "net worth counts it once, in or out: " + GoldMoney.format(before) + " -> " + GoldMoney.format(ledger.netWorth(alice.getUUID())));
        c.check(ledger.netWorth(bob.getUUID()) == 0, "it's nobody else's");

        // ── /pay (and every shop) spends only the inventory ──
        c.check(!GoldMoney.take(alice, GoldMoney.fromGold(1)) && a.invested() == GoldMoney.fromGold(50),
            "with all her gold invested, she can't pay 1 gold: the vault isn't touched");

        // ── the mornings: the rate of the day that just ended, on what's in the vault (what the screen promised) ──
        long today = Calendar.date(Calendar.clock(server.overworld())).index();
        bank.resetDay(today);
        long fifty = GoldMoney.fromGold(50);
        long first = InvestmentBank.dailyReturn(fifty, InvestmentBank.rateOn(server, today));
        long promised = new InvestmentMenu(2, bob.getInventory(), a, InvestmentMenu.liveData(a, server), null).dailyOn(a.invested());
        bank.checkMorning(server, today + 1);
        c.check(a.returns() == first && first > 0 && promised == first, "the next morning pays what the screen said: "
            + InvestmentBank.rateOn(server, today) + "% on 50 gold, +" + GoldMoney.format(a.returns()));
        bank.checkMorning(server, today + 1);
        c.check(a.returns() == first, "only once a morning");
        // taken out before morning: nothing; back in: paid again
        ItemStack out = a.removeItemNoUpdate(0);
        a.setChanged();
        bank.checkMorning(server, today + 2);
        c.check(a.returns() == first, "an empty vault earns nothing that morning");
        a.setItem(0, out);
        long second = InvestmentBank.dailyReturn(fifty, InvestmentBank.rateOn(server, today + 2));
        bank.checkMorning(server, today + 3);
        c.check(a.returns() == first + second, "gold back in: +" + GoldMoney.format(second) + " the next morning");
        bank.checkMorning(server, today + 1);
        c.check(a.returns() == first + second, "the clock going back (/calendar set) pays nothing");
        bank.resetDay(today);

        // ── Ready holds 64 gold at most: then nothing until it's collected ──
        long keep = a.returns;
        a.returns = InvestmentBank.READY_MAX - 2;
        bank.checkMorning(server, today + 1);
        c.check(a.returns() == InvestmentBank.READY_MAX, "a morning tops Ready up to 64 gold, no further (" + GoldMoney.format(a.returns()) + ")");
        bank.checkMorning(server, today + 2);
        c.check(a.returns() == InvestmentBank.READY_MAX, "with Ready full, the vault earns nothing");
        c.check(!GoldMoney.take(alice, 1) && a.returns() == InvestmentBank.READY_MAX && a.invested() == fifty,
            "/pay can't spend Ready gold or vault gold: only what's in her inventory");
        bank.resetDay(today);
        a.returns = keep;

        // ── the screen's figures survive the trip (16 bits a slot) ──
        a.returns = 40000;
        SimpleContainerData copy = new SimpleContainerData(InvestmentMenu.DATA);
        var live = InvestmentMenu.liveData(a, server);
        for (int i = 0; i < InvestmentMenu.DATA; i++) copy.set(i, (short) live.get(i));
        InvestmentMenu client = new InvestmentMenu(1, bob.getInventory(), new net.minecraft.world.SimpleContainer(3), copy, null);
        c.check(client.returns() == 40000 && client.invested() == GoldMoney.fromGold(50) && client.rate() == InvestmentBank.rateOn(server, InvestmentBank.today(server)) && client.daysLeft() == InvestmentBank.daysLeft(InvestmentBank.today(server)),
            "the screen reads big numbers right (" + client.returns() + ")");
        a.returns = GoldMoney.fromGold(6);

        // ── collecting ──
        GoldLeaderboard.recount(server, ledger);
        long worth = ledger.netWorth(alice.getUUID());
        menu.clickMenuButton(alice, InvestmentMenu.COLLECT);
        GoldLeaderboard.recount(server, ledger);
        c.check(a.returns() == 0 && GoldMoney.carried(alice) == GoldMoney.fromGold(6) && a.invested() == GoldMoney.fromGold(50),
            "COLLECT puts the 6 gold in her inventory, the vault keeps its 50");
        c.check(worth == GoldMoney.fromGold(56) && ledger.netWorth(alice.getUUID()) == worth,
            "net worth counts the returns once, waiting or collected (56 gold)");
        c.check(GoldMoney.take(alice, GoldMoney.fromGold(6)) && a.invested() == GoldMoney.fromGold(50), "collected gold can be paid; the vault still can't");

        // ── someone offline gets paid too, and it counts for them ──
        UUID away = UUID.randomUUID();
        InvestmentBank.Account o = bank.account(away);
        o.setItem(0, new ItemStack(Items.GOLD_INGOT, 64));
        o.setItem(1, new ItemStack(Items.GOLD_INGOT, 64));
        o.setItem(2, new ItemStack(Items.GOLD_INGOT, 64));
        bank.checkMorning(server, today + 1);
        GoldLeaderboard.recount(server, ledger);
        long full = InvestmentBank.dailyReturn(InvestmentBank.MAX, InvestmentBank.rateOn(server, today));
        c.check(o.returns() == full && full > 0 && ledger.netWorth(away) == InvestmentBank.MAX + full,
            "an offline player's full vault earns its rate and counts for them: " + GoldMoney.format(ledger.netWorth(away)));
        bank.resetDay(today);

        // (the Banker: a Name Tag, 1 paper + 1 nugget, can't be sold for a gold any more)
        var tag = BankerEvents.prices().get(Items.NAME_TAG);
        var saddle = BankerEvents.prices().get(Items.SADDLE);
        c.check(tag != null && (long) tag.nuggets() * 2 <= tag.per() && saddle != null && saddle.nuggets() <= 7 * saddle.per(),
            "the Banker pays at most a nugget for 2 Name Tags and 7 nuggets for a Saddle (less than they're made of)");

        // tidy up
        a.clearContent();
        o.clearContent();
        o.returns = 0;
        GoldLeaderboard.recount(server, ledger);
    }
}

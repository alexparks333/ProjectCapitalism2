package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.CapitalismConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The Investment Chest's accounts (Economy.md > The Investment Chest), saved with the world. Every player has ONE
// account, the same from every Investment Chest (like an Ender Chest): SLOTS slots that take only Gold Ingots (3 stacks:
// 192 gold at most), and their returns waiting to be collected.
//
// How the money stays honest:
//  - The gold in it is real gold, taken out of the world while it's in there. It counts in its owner's net worth (the
//    leaderboard, /balance, the death tax: GoldLedger, snapshotted in the same recount as their inventory, so moving gold
//    in or out never counts it twice). Returns waiting count too.
//  - /pay, Shopfronts and NPCs only ever take gold from the inventory (GoldMoney.take), so they can't touch it. Nor can
//    /pay death (deathtax/GoldCollector only looks at the inventory, the Ender Chest and owned containers): take it out
//    first.
//  - Every morning (a new calendar day) the bank pays each account the rate on whatever is in it at that moment: what the
//    screen says it earns a day is what it gets. (Gold can only be in one vault when morning comes, so dropping it in
//    just before can't double anything.)
//  - The returns are new gold from the bank (a faucet, on purpose), paid as a number of nuggets the owner collects (the
//    Collect button: GoldMoney.give, what doesn't fit drops at their feet, locked to them). They never earn more.
//  - One payout a day, whatever happens: a skipped night or /calendar set forward pays once; going back pays nothing.
@EventBusSubscriber(modid = Capitalism.MODID)
public class InvestmentBank extends SavedData {

    public static final int SLOTS = 3;
    // the most it holds, in nuggets (3 stacks of ingots)
    public static final long MAX = (long) SLOTS * 64 * GoldMoney.INGOT;
    // the most that waits in Ready (64 gold): once it's full the vault earns nothing until it's collected
    public static final long READY_MAX = 64L * GoldMoney.INGOT;

    // ── one player's account ──
    public static final class Account extends SimpleContainer {
        private InvestmentBank bank;
        long returns;   // nuggets paid out, waiting to be collected
        long earned;    // everything it has ever paid out (for the screen's bragging line)

        Account(InvestmentBank bank) {
            super(SLOTS);
            this.bank = bank;
        }

        // gold in it, in nuggets
        public long invested() {
            return GoldMoney.count(this);
        }

        public long returns() {
            return this.returns;
        }

        public long earned() {
            return this.earned;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(Items.GOLD_INGOT);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (this.bank != null) this.bank.setDirty();
        }

        // the returns into their inventory (or at their feet). Returns what was paid.
        public long collect(ServerPlayer player) {
            long paid = this.returns;
            if (paid <= 0) return 0;
            this.returns = 0;
            this.bank.setDirty();
            GoldMoney.give(player, paid);
            return paid;
        }

        // the morning: yesterday's rate on what's in it now, up to Ready's limit (full: nothing until it's collected)
        long payDay(int rate) {
            long pay = Math.min(dailyReturn(this.invested(), rate), Math.max(0, READY_MAX - this.returns));
            this.returns += pay;
            this.earned += pay;
            return pay;
        }
    }

    private record Row(UUID id, List<Integer> ingots, long returns, long earned) {
        static final Codec<Row> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Row::id),
            Codec.INT.listOf().fieldOf("ingots").forGetter(Row::ingots),
            Codec.LONG.fieldOf("returns").forGetter(Row::returns),
            Codec.LONG.optionalFieldOf("earned", 0L).forGetter(Row::earned)
        ).apply(inst, Row::new));
    }

    private static final Codec<InvestmentBank> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Row.CODEC.listOf().fieldOf("accounts").forGetter(InvestmentBank::rows),
        Codec.LONG.fieldOf("lastDay").forGetter(b -> b.lastDay)
    ).apply(inst, InvestmentBank::new));

    public static final SavedDataType<InvestmentBank> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "investments"), InvestmentBank::new, CODEC);

    private final Map<UUID, Account> accounts = new HashMap<>();
    // the calendar day last paid for (-1: not yet: the first day it sees isn't paid, it only starts counting)
    private long lastDay = -1;

    public InvestmentBank() {
    }

    private InvestmentBank(List<Row> rows, long lastDay) {
        this.lastDay = lastDay;
        for (Row row : rows) {
            Account account = new Account(null);
            for (int i = 0; i < SLOTS && i < row.ingots().size(); i++) {
                int n = Math.max(0, Math.min(64, row.ingots().get(i)));
                if (n > 0) account.setItem(i, new ItemStack(Items.GOLD_INGOT, n));
            }
            account.returns = Math.max(0, row.returns());
            account.earned = Math.max(0, row.earned());
            account.bank = this;
            this.accounts.put(row.id(), account);
        }
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        this.accounts.forEach((id, a) -> {
            List<Integer> ingots = new ArrayList<>();
            for (int i = 0; i < SLOTS; i++) ingots.add(a.getItem(i).is(Items.GOLD_INGOT) ? a.getItem(i).getCount() : 0);
            rows.add(new Row(id, ingots, a.returns, a.earned));
        });
        return rows;
    }

    public static InvestmentBank get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // their account (made, empty, the first time)
    public Account account(UUID id) {
        return this.accounts.computeIfAbsent(id, k -> {
            this.setDirty();
            return new Account(this);
        });
    }

    public Set<UUID> investors() {
        return this.accounts.keySet();
    }

    // what an account is worth to its owner, in nuggets: the gold in it plus the returns waiting
    public long worth(UUID id) {
        Account a = this.accounts.get(id);
        return a == null ? 0 : a.invested() + a.returns;
    }

    // ── the rate: a little stock market ──
    // The rate rerolls every MARKET_DAYS days, the same for everyone (from the world seed and which stretch of days it
    // is, so nothing needs saving and it can't be rerolled by anyone): usually 4 to 7 %, on rare stretches a BULL RUN
    // (8 or 9 %) or a CRASH (2 or 3 %). (With investmentMarket off in the config: always investmentDailyPercent.)
    public static final int MARKET_DAYS = 3;
    public static final float RARE_HIGH = 0.10F, RARE_LOW = 0.10F;

    public enum Mood {
        CRASH("CRASH!", 0xFF6A5A), STEADY("Steady", 0xE8D080), BULL("BULL RUN!", 0x6CF08A);

        public final String title;
        public final int color;

        Mood(String title, int color) {
            this.title = title;
            this.color = color;
        }

        public static Mood of(int rate) {
            return rate <= 3 ? CRASH : rate >= 8 ? BULL : STEADY;
        }
    }

    // the market's rate for a calendar day in this world, in whole percent
    public static int marketRate(long worldSeed, long day) {
        long stretch = Math.floorDiv(day, MARKET_DAYS);
        java.util.Random r = new java.util.Random(worldSeed * 0x5DEECE66DL + stretch * 0x9E3779B97F4A7C15L + 0x1E57L);
        r.nextInt();
        float roll = r.nextFloat();
        if (roll < RARE_LOW) return 2 + r.nextInt(2);
        if (roll < RARE_LOW + RARE_HIGH) return 8 + r.nextInt(2);
        return 4 + r.nextInt(4);
    }

    // days left at today's rate, today included (3, 2 or 1)
    public static int daysLeft(long day) {
        return MARKET_DAYS - (int) Math.floorMod(day, MARKET_DAYS);
    }

    public static int rateOn(MinecraftServer server, long day) {
        try {
            if (!CapitalismConfig.INVESTMENT_MARKET.get()) return CapitalismConfig.INVESTMENT_DAILY_PERCENT.get();
        } catch (IllegalStateException notLoaded) {
            // (configs not loaded: the market it is)
        }
        return marketRate(server.overworld().getSeed(), day);
    }

    public static long today(MinecraftServer server) {
        return Calendar.date(Calendar.clock(server.overworld())).index();
    }

    // a day's return on `nuggets` held all day at `rate` %, rounded down to the nugget (50 gold at 6% -> 3 gold)
    public static long dailyReturn(long nuggets, int rate) {
        return Math.max(0, Math.min(nuggets, MAX)) * rate / 100;
    }

    // ── the morning ──

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        get(server).checkMorning(server, today(server));
    }

    // pays once when the calendar day moves forward; returns how many accounts were paid
    public int checkMorning(MinecraftServer server, long today) {
        if (this.lastDay < 0 || today < this.lastDay) {
            this.lastDay = today;
            this.setDirty();
            return 0;
        }
        if (today == this.lastDay) return 0;
        this.lastDay = today;
        this.setDirty();
        return this.payEveryone(server, today);
    }

    // every account's morning: paid at the rate of the day that just ended (yesterday's market); a new stretch's rate
    // is announced to everyone online who has a vault
    public int payEveryone(MinecraftServer server, long today) {
        int paid = 0;
        int rate = rateOn(server, today - 1);
        int next = rateOn(server, today);
        boolean newStretch = daysLeft(today) == MARKET_DAYS;
        for (Map.Entry<UUID, Account> e : this.accounts.entrySet()) {
            long pay = e.getValue().payDay(rate);
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null) {
                if (pay > 0) paid++;
                continue;
            }
            if (pay > 0) {
                paid++;
                player.sendSystemMessage(morningLine(pay, rate, today));
            } else if (e.getValue().returns() >= READY_MAX && e.getValue().invested() > 0) {
                player.sendSystemMessage(fullLine());
            }
            if (newStretch && (pay > 0 || e.getValue().invested() > 0)) player.sendSystemMessage(marketLine(next));
        }
        this.setDirty();
        return paid;
    }

    // the morning's chat line, to the vault's owner only: one short line
    static Component morningLine(long pay, int rate, long day) {
        return Component.literal("[Investments] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("📈 +" + shortGold(pay)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
            .append(Component.literal(" (" + rate + "%) ready to collect").withStyle(ChatFormatting.GRAY));
    }

    // Ready is full: nothing earned until they collect
    static Component fullLine() {
        return Component.literal("[Investments] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("Ready is full (" + GoldMoney.toGold(READY_MAX) + " gold): collect it to keep earning").withStyle(ChatFormatting.RED));
    }

    // a new stretch of the market, to each investor only: its rate for the next MARKET_DAYS days
    static Component marketLine(int rate) {
        Mood mood = Mood.of(rate);
        return Component.literal("[Investments] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("📊 New rate: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(rate + "% " + mood.title).withStyle(s -> s.withColor(mood.color).withBold(true)))
            .append(Component.literal(" (" + MARKET_DAYS + " days)").withStyle(ChatFormatting.GRAY));
    }

    // gold, short: "3.8 gold", "12 gold"
    public static String shortGold(long nuggets) {
        if (nuggets % GoldMoney.INGOT == 0) return GoldMoney.toGold(nuggets) + " gold";
        return String.format(java.util.Locale.ROOT, "%.1f gold", nuggets / (double) GoldMoney.INGOT);
    }

    // for tests: start counting days from scratch
    public void resetDay(long today) {
        this.lastDay = today;
        this.setDirty();
    }
}

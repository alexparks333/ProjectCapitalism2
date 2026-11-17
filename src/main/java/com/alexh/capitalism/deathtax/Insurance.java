package com.alexh.capitalism.deathtax;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.event.BloodMoon;
import com.alexh.capitalism.network.OpenInsuranceGuidePacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

// Not-A-Scam Insurance (Insurance.md): /insurance quotes | buy <plan> | cancel | status | guide.
//  - Four plans, flat company prices (server config `insurance`, NOT NpcPricing: CLAUDE.md), each lowering the death tax.
//    A plan bought (or switched, or cancelled) today takes effect at the next Calendar day start.
//  - Each day start (or when they log in that day) the day's premium comes out of their gold; can't pay = the plan
//    lapses (ends, no cover that day, no debt). Only days they're on are charged (nobody dies offline).
//  - During a Blood Moon nobody is insured (the insurers hide), except with Blood Moon Insurance: a one-time buy that
//    covers every death (0%) during the next Blood Moon that STARTS after it's bought, used up when that one ends.
//  - "[Not-A-Scam Insurance]" tells everyone when a Blood Moon is coming and when it begins.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Insurance {

    public static final int COLOR = 0x3FD0C0;

    public enum Tier {
        BRONZE("bronze", "Bronze", 0xCD8A3A, () -> CapitalismConfig.INSURANCE_BRONZE_PRICE.get(), () -> CapitalismConfig.INSURANCE_BRONZE_RATE.get()),
        SILVER("silver", "Silver", 0xC8CCD4, () -> CapitalismConfig.INSURANCE_SILVER_PRICE.get(), () -> CapitalismConfig.INSURANCE_SILVER_RATE.get()),
        GOLD("gold", "Gold", 0xF0C040, () -> CapitalismConfig.INSURANCE_GOLD_PRICE.get(), () -> CapitalismConfig.INSURANCE_GOLD_RATE.get()),
        PREMIUM("premium", "Premium", 0xB070F0, () -> CapitalismConfig.INSURANCE_PREMIUM_PRICE.get(), () -> CapitalismConfig.INSURANCE_PREMIUM_RATE.get());

        public final String id;
        public final String title;
        public final int color;
        private final Supplier<Integer> price;
        private final Supplier<Integer> rate;

        Tier(String id, String title, int color, Supplier<Integer> price, Supplier<Integer> rate) {
            this.id = id;
            this.title = title;
            this.color = color;
            this.price = price;
            this.rate = rate;
        }

        // whole gold a day
        public int price() {
            return this.price.get();
        }

        // the death tax with this plan, percent
        public int rate() {
            return this.rate.get();
        }

        public static @Nullable Tier byId(String id) {
            for (Tier t : values()) if (t.id.equalsIgnoreCase(id)) return t;
            return null;
        }
    }

    public static final String BLOOD_MOON_PLAN = "BloodMoonInsurance";

    private Insurance() {
    }

    public static int baseRate() {
        return CapitalismConfig.DEATH_TAX_PERCENT.get();
    }

    public static int bloodMoonPrice() {
        return CapitalismConfig.BLOOD_MOON_INSURANCE_PRICE.get();
    }

    static long today(MinecraftServer server) {
        return Calendar.date(server.overworld()).index();
    }

    // ── the day's business: a waiting change, then the premium ──

    // Once a Calendar day per player (at the day start if they're on, else when they log in): a plan bought, switched
    // or cancelled for today takes effect, then today's premium is charged (can't pay: the plan lapses). Safe to call
    // any time; the rest of the day it does nothing.
    public static void process(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        DeathTaxData d = DeathTax.data(player);
        long today = today(server);
        if (d.processedDay == today) return;
        d.processedDay = today;
        if (!d.pending.isEmpty() && today >= d.pendingDay) {
            if (d.pending.equals("cancel")) {
                if (!d.tier.isEmpty()) tell(player, Component.literal("Your " + title(d.tier) + " plan has ended, as you asked. We'll miss your money."));
                d.tier = "";
            } else {
                boolean switching = !d.tier.isEmpty();
                d.tier = d.pending;
                tell(player, Component.literal(switching ? "You're on the " + title(d.tier) + " plan from today." : "Your " + title(d.tier) + " plan starts today."));
            }
            d.pending = "";
            d.pendingDay = -1;
        }
        Tier tier = Tier.byId(d.tier);
        if (tier == null) d.tier = "";
        else {
            long cost = GoldMoney.fromGold(tier.price());
            if (GoldMoney.take(player, cost)) {
                d.coveredDay = today;
                tell(player, Component.literal("Today's " + tier.title + " premium: " + GoldMoney.format(cost) + ". You're covered today (" + tier.rate() + "% death tax)."));
            } else {
                d.tier = "";
                tellBad(player, "You couldn't pay today's " + tier.title + " premium (" + GoldMoney.format(cost) + "), so your plan has lapsed: no cover today. "
                    + "No hard feelings, and no debt. /insurance buy to start again tomorrow.");
            }
        }
        // spent Blood Moon Insurance goes
        if (d.bloodMoonCover >= 0 && !bloodMoonCoverUnused(server, d.bloodMoonCover)) d.bloodMoonCover = -1;
        DeathTax.save(player, d);
    }

    // insured today (by a plan whose premium was paid this morning)?
    public static @Nullable Tier coveredBy(ServerPlayer player) {
        DeathTaxData d = DeathTax.data(player);
        Tier tier = Tier.byId(d.tier);
        return tier != null && d.coveredDay == today(player.level().getServer()) ? tier : null;
    }

    // Blood Moon Insurance for this Blood Moon (by number) not yet used: the one it covers hasn't started, or is on now
    static boolean bloodMoonCoverUnused(MinecraftServer server, long cover) {
        long started = DeathTaxLedger.get(server).moonsStarted;
        return cover > started || (cover == started && BloodMoon.isActive());
    }

    // The death tax rate if they died right now, and why
    public record Rate(int percent, String why) {
    }

    public static Rate rateFor(ServerPlayer player) {
        process(player);
        MinecraftServer server = player.level().getServer();
        DeathTaxData d = DeathTax.data(player);
        if (BloodMoon.isActive()) {
            if (d.bloodMoonCover >= 0 && d.bloodMoonCover == DeathTaxLedger.get(server).moonsStarted) return new Rate(0, "Blood Moon Insurance");
            return new Rate(baseRate(), "Blood Moon, no insurers");
        }
        Tier tier = coveredBy(player);
        if (tier != null) return new Rate(tier.rate(), tier.title + " plan");
        return new Rate(baseRate(), "uninsured");
    }

    // ── Blood Moons ──

    // (event/BloodMoon.start) one has begun: count it (Blood Moon Insurance covers one by its number) and tell everyone
    public static void bloodMoonStarted(MinecraftServer server, boolean announce) {
        DeathTaxLedger ledger = DeathTaxLedger.get(server);
        ledger.moonsStarted++;
        ledger.setDirty();
        if (announce) announce(server, "You all will not be insured for this coming Blood Moon. Buy Blood Moon Insurance if you want it.");
    }

    public static void announce(MinecraftServer server, String text) {
        Component line = prefix().append(Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(0xBFF0EA))));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(line);
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 0.7F, 1.4F);
        }
    }

    private static boolean moonKnown;

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        // (a restart doesn't announce again one that was already known)
        moonKnown = BloodMoon.ticksUntil(event.getServer()) >= 0;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        // a Blood Moon has been called (the altar, or an admin's delay): warn everyone
        boolean known = BloodMoon.ticksUntil(server) >= 0;
        if (known && !moonKnown) announce(server, "You all will not be insured for this coming Blood Moon. Buy Blood Moon Insurance if you want it.");
        moonKnown = known;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) process(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) process(player);
    }

    // ── /insurance ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("insurance")
            .executes(c -> status(c.getSource().getPlayerOrException()))
            .then(Commands.literal("quotes").executes(c -> quotes(c.getSource().getPlayerOrException())))
            .then(Commands.literal("status").executes(c -> status(c.getSource().getPlayerOrException())))
            .then(Commands.literal("cancel").executes(c -> cancel(c.getSource().getPlayerOrException())))
            .then(Commands.literal("guide").executes(c -> guide(c.getSource().getPlayerOrException())))
            .then(Commands.literal("buy").then(Commands.argument("plan", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"bronze", "silver", "gold", "premium", BLOOD_MOON_PLAN}, b))
                .executes(Insurance::buy))));
    }

    static int quotes(ServerPlayer player) {
        MutableComponent text = prefix().append(Component.literal("Today's quotes (charged each morning, starting tomorrow):").withStyle(ChatFormatting.WHITE));
        for (Tier t : Tier.values()) {
            text.append(Component.literal("\n  " + t.title).withStyle(s -> s.withColor(TextColor.fromRgb(t.color)).withBold(true)))
                .append(Component.literal("  " + t.price() + " gold a day, death tax " + t.rate() + "%").withStyle(ChatFormatting.GRAY));
        }
        text.append(Component.literal("\n  No plan: death tax " + baseRate() + "%").withStyle(ChatFormatting.DARK_GRAY));
        text.append(Component.literal("\n  Blood Moon Insurance").withStyle(s -> s.withColor(TextColor.fromRgb(0xFF5050)).withBold(true)))
            .append(Component.literal("  " + bloodMoonPrice() + " gold once: 0% for deaths during the next Blood Moon (plans don't cover Blood Moons)").withStyle(ChatFormatting.GRAY));
        text.append(Component.literal("\n  /insurance buy <plan>, /insurance guide for the fine print").withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(text);
        return 1;
    }

    private static int buy(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return buy(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "plan"));
    }

    public static int buy(ServerPlayer player, String plan) {
        process(player);
        String what = plan.toLowerCase(Locale.ROOT);
        if (what.equals(BLOOD_MOON_PLAN.toLowerCase(Locale.ROOT))) return buyBloodMoon(player);
        Tier tier = Tier.byId(what);
        if (tier == null) {
            tellBad(player, "We don't sell \"" + plan + "\". Plans: bronze, silver, gold, premium, " + BLOOD_MOON_PLAN + ".");
            return 0;
        }
        DeathTaxData d = DeathTax.data(player);
        long tomorrow = today(player.level().getServer()) + 1;
        if (tier.id.equals(d.tier)) {
            if (d.pending.isEmpty()) {
                tellBad(player, "You already have the " + tier.title + " plan.");
                return 0;
            }
            d.pending = "";
            d.pendingDay = -1;
            tell(player, Component.literal("You'll stay on the " + tier.title + " plan: tomorrow's change is off."));
        } else if (tier.id.equals(d.pending)) {
            tellBad(player, "Your " + tier.title + " plan already starts tomorrow morning.");
            return 0;
        } else {
            boolean switching = !d.tier.isEmpty();
            d.pending = tier.id;
            d.pendingDay = tomorrow;
            tell(player, Component.literal((switching ? "You'll switch from " + title(d.tier) + " to " + tier.title + " tomorrow morning"
                : "Your " + tier.title + " plan starts tomorrow morning") + ": " + tier.price() + " gold each morning, death tax " + tier.rate() + "%."
                + (switching ? " Until then your " + title(d.tier) + " plan still covers you." : " Nothing is charged today.")));
        }
        DeathTax.save(player, d);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.1F);
        return 1;
    }

    private static int buyBloodMoon(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        DeathTaxData d = DeathTax.data(player);
        if (d.bloodMoonCover >= 0 && bloodMoonCoverUnused(server, d.bloodMoonCover)) {
            tellBad(player, "You already hold Blood Moon Insurance that hasn't been used. One at a time, please.");
            return 0;
        }
        long cost = GoldMoney.fromGold(bloodMoonPrice());
        long carried = GoldMoney.carried(player);
        if (carried < cost || !GoldMoney.take(player, cost)) {
            tellBad(player, "Blood Moon Insurance costs " + GoldMoney.format(cost) + "; you have " + GoldMoney.format(carried) + ".");
            return 0;
        }
        d.bloodMoonCover = DeathTaxLedger.get(server).moonsStarted + 1;
        DeathTax.save(player, d);
        tell(player, Component.literal("Blood Moon Insurance bought (" + GoldMoney.format(cost) + "): no death tax for any death during the "
            + (BloodMoon.isActive() ? "NEXT Blood Moon. Not this one: you can't insure a house that's already on fire." : "next Blood Moon.")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 0.9F);
        return 1;
    }

    public static int cancel(ServerPlayer player) {
        process(player);
        DeathTaxData d = DeathTax.data(player);
        if (d.tier.isEmpty()) {
            if (d.pending.isEmpty() || d.pending.equals("cancel")) {
                tellBad(player, "You don't have a plan to cancel.");
                return 0;
            }
            String was = title(d.pending);
            d.pending = "";
            d.pendingDay = -1;
            DeathTax.save(player, d);
            tell(player, Component.literal("Your " + was + " plan won't start. Nothing was charged."));
            return 1;
        }
        if (d.pending.equals("cancel")) {
            tellBad(player, "Your plan already ends tomorrow morning.");
            return 0;
        }
        d.pending = "cancel";
        d.pendingDay = today(player.level().getServer()) + 1;
        DeathTax.save(player, d);
        tell(player, Component.literal("Your " + title(d.tier) + " plan ends tomorrow morning. Until then you're still covered."));
        return 1;
    }

    public static int status(ServerPlayer player) {
        process(player);
        MinecraftServer server = player.level().getServer();
        DeathTaxData d = DeathTax.data(player);
        Tier tier = Tier.byId(d.tier);
        MutableComponent text = prefix().append(Component.literal("Your policy").withStyle(ChatFormatting.WHITE));
        if (tier == null) text.append(line("Plan: none (death tax " + baseRate() + "%)"));
        else text.append(line("Plan: " + tier.title + " (" + tier.price() + " gold a day, death tax " + tier.rate() + "%), "
            + (coveredBy(player) != null ? "covered today" : "not covered today")));
        if (d.pending.equals("cancel")) text.append(line("Ends tomorrow morning (cancelled)"));
        else if (!d.pending.isEmpty()) text.append(line((tier == null ? "Starts" : "Switches to " + title(d.pending)) + " tomorrow morning"
            + (tier == null ? ": " + title(d.pending) : "")));
        boolean bm = d.bloodMoonCover >= 0 && bloodMoonCoverUnused(server, d.bloodMoonCover);
        text.append(line("Blood Moon Insurance: " + (!bm ? "none (plans don't cover Blood Moons)"
            : d.bloodMoonCover == DeathTaxLedger.get(server).moonsStarted ? "covering this Blood Moon" : "covers the next Blood Moon")));
        Rate rate = rateFor(player);
        text.append(line("If you died now: " + rate.percent() + "% death tax (" + rate.why() + ")"));
        player.sendSystemMessage(text);
        return 1;
    }

    static int guide(ServerPlayer player) {
        int[] values = new int[Tier.values().length * 2 + 2];
        int i = 0;
        for (Tier t : Tier.values()) {
            values[i++] = t.price();
            values[i++] = t.rate();
        }
        values[i++] = baseRate();
        values[i] = bloodMoonPrice();
        PacketDistributor.sendToPlayer(player, new OpenInsuranceGuidePacket(java.util.Arrays.stream(values).boxed().toList()));
        return 1;
    }

    // ── chat ──

    static MutableComponent prefix() {
        return Component.literal("[Not-A-Scam Insurance] ").withStyle(s -> s.withColor(TextColor.fromRgb(COLOR)).withBold(true));
    }

    private static MutableComponent line(String s) {
        return Component.literal("\n  " + s).withStyle(ChatFormatting.GRAY);
    }

    static void tell(ServerPlayer player, Component text) {
        player.sendSystemMessage(prefix().append(text.copy().withStyle(s -> s.withColor(TextColor.fromRgb(0xBFF0EA)).withBold(false))));
    }

    static void tellBad(ServerPlayer player, String text) {
        player.sendSystemMessage(prefix().append(Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(0xFF8080)).withBold(false))));
    }

    static String title(String id) {
        Tier t = Tier.byId(id);
        return t == null ? id : t.title;
    }
}

package com.alexh.capitalism.core;

import com.alexh.capitalism.economy.BankerEvents;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

// The admin panel (core/AdminPanel) and the Banker's version 6 prices (CapitalismConfig, economy/BankPriceUpgrade):
// settings change live, within their range, and are put back; Banker prices change; an old price list is brought up
// to date without losing a server's own prices; every price names a real item; no storage block pays more than its 9
// pieces; the emerald money loop is closed.
public final class AdminPanelTests {

    static {
        GameTests.register("admin_panel", AdminPanelTests::panel);
    }

    private AdminPanelTests() {
    }

    public static void init() {
    }

    private static void panel(GameTests.Checks c) {
        // ── the Banker's list ──
        c.check(CapitalismConfig.BANK_PRICES_VERSION.get() == CapitalismConfig.BANK_PRICES_LATEST, "the server's Banker list is up to date (version 6)");
        int bad = 0;
        StringBuilder which = new StringBuilder();
        for (String line : CapitalismConfig.BANK_PRICES.get()) {
            int split = line.lastIndexOf('=');
            Identifier id = split > 0 ? Identifier.tryParse(line.substring(0, split)) : null;
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || !line.substring(split + 1).matches("\\d+(/\\d+)?")) {
                bad++;
                which.append(' ').append(line);
            }
        }
        c.check(bad == 0, "every Banker price names a real item and a real price (" + bad + ":" + which + ")");
        Map<Item, BankerEvents.Price> prices = BankerEvents.prices();
        c.check(prices.size() > 300, "the Banker buys a lot more now (" + prices.size() + " items)");
        c.check(eachPays(prices, Items.EMERALD) <= 1, "an Emerald pays a nugget at most: villagers give them away for 32 sticks");
        c.check(eachPays(prices, Items.ECHO_SHARD) >= 45 && eachPays(prices, Items.SHULKER_SHELL) >= 54,
            "rare finds pay more: Echo Shard 5 gold, Shulker Shell 6");
        Item[][] blocks = {{Items.IRON_BLOCK, Items.IRON_INGOT}, {Items.COAL_BLOCK, Items.COAL}, {Items.DIAMOND_BLOCK, Items.DIAMOND},
            {Items.EMERALD_BLOCK, Items.EMERALD}, {Items.LAPIS_BLOCK, Items.LAPIS_LAZULI}, {Items.REDSTONE_BLOCK, Items.REDSTONE},
            {Items.RAW_IRON_BLOCK, Items.RAW_IRON}, {Items.RAW_GOLD_BLOCK, Items.RAW_GOLD},
            {Items.HAY_BLOCK, Items.WHEAT}, {Items.MELON, Items.MELON_SLICE}, {Items.SLIME_BLOCK, Items.SLIME_BALL},
            {Items.DRIED_KELP_BLOCK, Items.DRIED_KELP}, {Items.NETHERITE_BLOCK, Items.NETHERITE_INGOT}};
        int loops = 0;
        for (Item[] pair : blocks) if (eachPays(prices, pair[0]) > 9 * eachPays(prices, pair[1]) + 1e-9) loops++;
        c.check(loops == 0, "no storage block pays more than its 9 pieces");

        // ── bringing an old list up to date ──
        List<String> old = List.of("minecraft:emerald=45", "minecraft:echo_shard=20", "minecraft:diamond=60", "minecraft:nether_star=500");
        List<String> now = CapitalismConfig.upgradeBankPrices(old);
        c.check(now.get(0).equals("minecraft:emerald=1"), "a price still at the old default takes the new one");
        c.check(now.get(1).equals("minecraft:echo_shard=20") && now.get(2).equals("minecraft:diamond=60"),
            "prices the server changed stay as they are");
        c.check(now.contains("minecraft:nether_star=500") && now.stream().filter(l -> l.startsWith("minecraft:nether_star=")).count() == 1,
            "an item the server already prices isn't added again");
        c.check(now.contains("minecraft:heart_of_the_sea=45") && now.size() == old.size() + CapitalismConfig.BANK_PRICES_ADDED_V6.size() - 1,
            "the new items are added on the end");
        c.check(CapitalismConfig.upgradeBankPrices(now).equals(now), "running it again changes nothing");

        // ── the panel: settings ──
        List<String> rows = AdminPanel.settings();
        c.check(rows.stream().anyMatch(r -> r.contains(AdminPanel.SEP + "economy.investmentDailyPercent" + AdminPanel.SEP))
                && rows.stream().noneMatch(r -> r.contains("bankPricesVersion") || r.contains("bankPricesV5")),
            "the Settings tab has every setting (" + rows.size() + ") but the Banker's list and its version");
        int rate = CapitalismConfig.INVESTMENT_DAILY_PERCENT.get();
        int other = rate == 7 ? 8 : 7;
        c.check(AdminPanel.apply(null, "economy.investmentDailyPercent", String.valueOf(other)) == null
            && CapitalismConfig.INVESTMENT_DAILY_PERCENT.get() == other, "a setting changes live (" + rate + " -> " + other + ")");
        c.check(AdminPanel.apply(null, "economy.investmentDailyPercent", "500") != null
            && AdminPanel.apply(null, "economy.investmentDailyPercent", "lots") != null
            && CapitalismConfig.INVESTMENT_DAILY_PERCENT.get() == other, "out of range or not a number: refused, nothing changes");
        AdminPanel.apply(null, "economy.investmentDailyPercent", String.valueOf(rate));
        c.check(CapitalismConfig.INVESTMENT_DAILY_PERCENT.get() == rate, "...and put back");
        boolean market = CapitalismConfig.INVESTMENT_MARKET.get();
        AdminPanel.apply(null, "economy.investmentMarket", String.valueOf(!market));
        boolean flipped = CapitalismConfig.INVESTMENT_MARKET.get() == !market;
        AdminPanel.apply(null, "economy.investmentMarket", String.valueOf(market));
        c.check(flipped && CapitalismConfig.INVESTMENT_MARKET.get() == market, "an on/off setting flips (and back)");
        double drain = CapitalismConfig.THIRST_DRAIN_PER_TICK.get();
        c.check(AdminPanel.apply(null, "thirst.drainPerTick", "0.001") == null && CapitalismConfig.THIRST_DRAIN_PER_TICK.get() == 0.001,
            "decimal settings too");
        AdminPanel.apply(null, "thirst.drainPerTick", String.valueOf(drain));
        c.check(AdminPanel.apply(null, "economy.bankPricesV5", "x") != null && AdminPanel.apply(null, "nope.nothing", "1") != null,
            "the list itself and made-up names can't be set");

        // ── the panel: Banker prices ──
        String diamond = rawPrice("minecraft:diamond");
        c.check(AdminPanel.apply(null, AdminPanel.BANK + "minecraft:diamond", "60") == null && eachPays(BankerEvents.prices(), Items.DIAMOND) == 60,
            "a Banker price changes live");
        c.check(AdminPanel.apply(null, AdminPanel.BANK + "minecraft:diamond", "five") != null
            && AdminPanel.apply(null, AdminPanel.BANK + "minecraft:diamond", "1/0") != null
            && AdminPanel.apply(null, AdminPanel.BANK + "minecraft:not_a_thing", "1") != null, "bad prices and made-up items are refused");
        AdminPanel.apply(null, AdminPanel.BANK + "minecraft:diamond", diamond);
        c.check(diamond.equals(rawPrice("minecraft:diamond")), "...and put back (" + diamond + ")");
        c.check(AdminPanel.banker().size() >= prices.size(),
            "the Banker tab lists every item");
    }

    // nuggets one of the item brings (0 if not bought)
    private static double eachPays(Map<Item, BankerEvents.Price> prices, Item item) {
        BankerEvents.Price p = prices.get(item);
        return p == null ? 0 : (double) p.nuggets() / p.per();
    }

    private static String rawPrice(String id) {
        for (String line : CapitalismConfig.BANK_PRICES.get()) if (line.startsWith(id + "=")) return line.substring(id.length() + 1);
        return "0";
    }
}

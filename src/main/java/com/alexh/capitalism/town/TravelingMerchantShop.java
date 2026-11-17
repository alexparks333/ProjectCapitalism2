package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.GoldMoney;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// The Traveling Merchant (Town.md > The Traveling Merchant): his wagon rolls into town once every 8-14 days (random) and
// stays one whole day, with a handful of rare, expensive treasures. Every visit he brings a fresh pick from POOL (each
// offer has its own chance; 5-8 of them), one of each, shared by everyone: first come, first served.
// Prices go through NpcPricing like every NPC (the +50% markup, Emerald sockets, the Merchant's Ledger, festival sales),
// so `fair` here is the FAIR price: a Molten Socket's 200 gold fair = 300 gold from him.
// The schedule and today's stock are saved with the world (Visits).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TravelingMerchantShop {

    public static final int MIN_DAYS_AWAY = 8, MAX_DAYS_AWAY = 14;
    public static final int MIN_OFFERS = 5, MAX_OFFERS = 8;

    // key: what it is (saved); make: the item; fair: the fair price in nuggets; chance: of being in his wagon on a visit;
    // stock: how many he brings (most things: one)
    public record Offer(String key, Supplier<ItemStack> make, long fair, float chance, int stock) {
        Offer(String key, Supplier<ItemStack> make, long fair, float chance) {
            this(key, make, fair, chance, 1);
        }
    }

    private static Supplier<ItemStack> of(Supplier<? extends net.minecraft.world.level.ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    // his price to a buyer with no discount, in gold: the fair price is two thirds of it (NpcPricing adds 50%)
    private static long sells(long goldPrice) {
        return GoldMoney.fromGold(goldPrice) * 2 / 3;
    }

    // armor trim templates: one picked at random each visit ("trim:silence"); the rarer looks cost more
    private static final List<String> TRIMS = List.of("coast", "dune", "wild", "sentry", "tide", "shaper", "host", "raiser",
        "wayfinder", "flow", "bolt", "silence", "eye", "spire", "vex", "ward", "rib", "snout");
    private static final Set<String> RARE_TRIMS = Set.of("silence", "eye", "spire", "vex", "ward", "rib", "snout");

    public static final List<Offer> POOL = List.of(
        new Offer("essential_wishing_star", of(ModItems.ESSENTIAL_WISHING_STAR::get, 1), sells(1400), 0.10F),
        new Offer("fun_wishing_star", of(ModItems.FUN_WISHING_STAR::get, 1), sells(1000), 0.10F),
        // ── only he sells these ──
        new Offer("bottomless_waterskin", of(ModItems.BOTTOMLESS_WATERSKIN::get, 1), sells(900), 0.20F),
        new Offer("trident", of(() -> Items.TRIDENT, 1), sells(650), 0.30F),
        new Offer("pirate_hat", of(ModItems.PIRATE_HAT::get, 1), sells(400), 0.25F),
        new Offer("wizards_hat", of(ModItems.WIZARDS_HAT::get, 1), sells(150), 0.10F),
        new Offer("welders_mask", of(ModItems.WELDERS_MASK::get, 1), sells(125), 0.10F),
        new Offer("chefs_hat", of(ModItems.CHEFS_HAT::get, 1), sells(100), 0.10F),
        new Offer("farmers_hat", of(ModItems.FARMERS_HAT::get, 1), sells(100), 0.10F),
        new Offer("travelers_pipe", of(ModItems.TRAVELERS_PIPE::get, 1), sells(100), 0.10F),
        new Offer("travelers_bag", of(ModItems.TRAVELERS_BAG::get, 1), sells(500), 0.20F),
        new Offer("feeding_hat", of(ModItems.FEEDING_HAT::get, 1), sells(200), 0.25F),
        new Offer("heavy_core", of(() -> Items.HEAVY_CORE, 1), sells(500), 0.20F),
        new Offer("changing_star", of(ModItems.CHANGING_STAR::get, 1), sells(450), 0.10F),
        new Offer("smiths_chisel", of(ModItems.SMITHS_CHISEL::get, 1), sells(400), 0.25F),
        new Offer("molten_socket", of(ModItems.MOLTEN_SOCKET::get, 1), sells(300), 0.35F),
        new Offer("netherite_upgrade", of(() -> Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1), sells(300), 0.25F),
        new Offer("phoenix_feather", of(ModItems.PHOENIX_FEATHER::get, 1), sells(150), 0.35F),
        new Offer("netherite_ingot", of(() -> Items.NETHERITE_INGOT, 1), sells(150), 0.40F),
        new Offer("travelers_lantern", of(ModItems.TRAVELERS_LANTERN::get, 1), sells(50), 0.30F),
        new Offer("trim", () -> ItemStack.EMPTY, sells(55), 0.40F),
        new Offer("travelers_snow_globe", of(ModItems.TRAVELERS_SNOW_GLOBE::get, 1), sells(80), 0.30F),
        new Offer("hand_warmer", of(ModItems.HAND_WARMER::get, 1), sells(40), 0.45F),
        new Offer("diamond_horse_armor", of(() -> Items.DIAMOND_HORSE_ARMOR, 1), sells(60), 0.40F),
        new Offer("ghast_tears", of(() -> Items.GHAST_TEAR, 3), sells(45), 0.50F),
        new Offer("disc", () -> ItemStack.EMPTY, sells(40), 0.30F),
        new Offer("saddle", of(() -> Items.SADDLE, 1), sells(30), 0.40F),
        new Offer("rabbit_feet", of(() -> Items.RABBIT_FOOT, 2), sells(30), 0.50F),
        new Offer("diamond", of(() -> Items.DIAMOND, 1), sells(5), 0.45F, 2),
        new Offer("emerald", of(() -> Items.EMERALD, 1), sells(5), 0.45F, 2));

    private TravelingMerchantShop() {
    }

    public static @Nullable Offer offer(String key) {
        String base = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
        for (Offer offer : POOL) {
            if (offer.key().equals(base)) return offer;
        }
        return null;
    }

    // The item a stock key stands for ("trim:silence" = a Silence trim template, "disc:minecraft:music_disc_cat")
    public static ItemStack stackFor(String key) {
        if (key.startsWith("trim:")) return item("minecraft:" + key.substring(5) + "_armor_trim_smithing_template");
        if (key.startsWith("disc:")) return item(key.substring(5));
        Offer offer = offer(key);
        return offer == null ? ItemStack.EMPTY : offer.make().get();
    }

    private static ItemStack item(String id) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static long fairFor(String key) {
        if (key.startsWith("trim:")) return sells(RARE_TRIMS.contains(key.substring(5)) ? 100 : 55);
        Offer offer = offer(key);
        return offer == null ? 0 : offer.fair();
    }

    public static int stockFor(String key) {
        Offer offer = offer(key);
        return offer == null ? 1 : offer.stock();
    }

    // What he packs for a visit: each offer on its own chance, then topped up / trimmed to MIN-MAX_OFFERS
    public static List<String> rollStock(RandomSource random) {
        List<Offer> picked = new ArrayList<>();
        List<Offer> rest = new ArrayList<>();
        for (Offer offer : POOL) {
            if (random.nextFloat() < offer.chance()) picked.add(offer);
            else rest.add(offer);
        }
        while (picked.size() < MIN_OFFERS && !rest.isEmpty()) picked.add(rest.remove(random.nextInt(rest.size())));
        while (picked.size() > MAX_OFFERS) picked.remove(random.nextInt(picked.size()));
        // keep the pool's order (the priciest wonders first)
        picked.sort(java.util.Comparator.comparingInt(POOL::indexOf));
        List<String> keys = new ArrayList<>();
        for (Offer offer : picked) {
            if (offer.key().equals("trim")) {
                keys.add("trim:" + TRIMS.get(random.nextInt(TRIMS.size())));
            } else if (offer.key().equals("disc")) {
                var discs = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.ItemTags.CREEPER_DROP_MUSIC_DISCS);
                List<net.minecraft.world.item.Item> list = new ArrayList<>();
                discs.forEach(h -> list.add(h.value()));
                keys.add("disc:" + (list.isEmpty() ? "minecraft:music_disc_cat"
                    : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(list.get(random.nextInt(list.size()))).toString()));
            } else {
                keys.add(offer.key());
            }
        }
        return keys;
    }

    public static int daysAway(RandomSource random) {
        return MIN_DAYS_AWAY + random.nextInt(MAX_DAYS_AWAY - MIN_DAYS_AWAY + 1);
    }

    // ── when he's here ──

    public static long today(MinecraftServer server) {
        return Calendar.date(server.overworld()).index();
    }

    public static boolean inTown(MinecraftServer server) {
        return Visits.get(server).visitDay == today(server);
    }

    // a new visit starting today: fresh stock, the next one booked, and everyone told
    public static void arrive(MinecraftServer server) {
        Visits visits = Visits.get(server);
        RandomSource random = server.overworld().getRandom();
        long day = today(server);
        visits.visitDay = day;
        visits.nextDay = day + daysAway(random);
        visits.stock = new ArrayList<>(rollStock(random));
        visits.sold = new ArrayList<>();
        visits.toldLeaving = false;
        visits.setDirty();
    }

    // gone early (an admin)
    public static void leave(MinecraftServer server) {
        Visits visits = Visits.get(server);
        visits.visitDay = -1;
        visits.toldLeaving = true;
        visits.setDirty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 200 != 0) return;
        Visits visits = Visits.get(server);
        long day = today(server);
        if (visits.nextDay < 0) {
            visits.nextDay = day + daysAway(server.overworld().getRandom());
            visits.setDirty();
        }
        if (visits.visitDay >= 0 && day > visits.visitDay && !visits.toldLeaving) {
            visits.toldLeaving = true;
            visits.setDirty();
        }
        if (day >= visits.nextDay && visits.visitDay != day) arrive(server);
    }

    // ── buying ──

    public static List<String> stock(MinecraftServer server) {
        return List.copyOf(Visits.get(server).stock);
    }

    // how many of stock item `index` he has left this visit
    public static int left(MinecraftServer server, int index) {
        Visits visits = Visits.get(server);
        if (index < 0 || index >= visits.stock.size()) return 0;
        int bought = 0;
        for (int i : visits.sold) if (i == index) bought++;
        return Math.max(0, stockFor(visits.stock.get(index)) - bought);
    }

    // all of it gone
    public static boolean sold(MinecraftServer server, int index) {
        return left(server, index) <= 0;
    }

    // one more of stock item `index` bought
    public static void markSold(MinecraftServer server, int index) {
        Visits visits = Visits.get(server);
        visits.sold.add(index);
        visits.setDirty();
    }

    // bit i = stock item i is sold out
    public static int soldMask(MinecraftServer server) {
        int mask = 0;
        for (int i = 0; i < Visits.get(server).stock.size(); i++) if (sold(server, i)) mask |= 1 << i;
        return mask;
    }

    public static long nextDay(MinecraftServer server) {
        return Visits.get(server).nextDay;
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    // ── saved with the world ──

    public static final class Visits extends SavedData {
        private static final Codec<Visits> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.optionalFieldOf("next_day", -1L).forGetter(v -> v.nextDay),
            Codec.LONG.optionalFieldOf("visit_day", -1L).forGetter(v -> v.visitDay),
            Codec.STRING.listOf().optionalFieldOf("stock", List.of()).forGetter(v -> v.stock),
            Codec.INT.listOf().optionalFieldOf("sold", List.of()).forGetter(v -> new ArrayList<>(v.sold)),
            Codec.BOOL.optionalFieldOf("told_leaving", true).forGetter(v -> v.toldLeaving)
        ).apply(inst, Visits::new));
        public static final SavedDataType<Visits> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "traveling_merchant"), Visits::new, CODEC);

        long nextDay = -1;
        long visitDay = -1;
        List<String> stock = new ArrayList<>();
        // one entry per purchase: the index of the stock item bought
        List<Integer> sold = new ArrayList<>();
        boolean toldLeaving = true;

        public Visits() {
        }

        private Visits(long nextDay, long visitDay, List<String> stock, List<Integer> sold, boolean toldLeaving) {
            this.nextDay = nextDay;
            this.visitDay = visitDay;
            this.stock = new ArrayList<>(stock);
            this.sold = new ArrayList<>(sold);
            this.toldLeaving = toldLeaving;
        }

        public static Visits get(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

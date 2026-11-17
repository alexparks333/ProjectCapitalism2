package com.alexh.capitalism.npc;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// How many of each NPC offer are left (NpcShops.Offer.maxStock), shared by every player on the server and saved with
// the world, so buying out an NPC leaves nothing for anyone until the restock. Everything restocks every RESTOCK_TICKS
// (20 minutes of game time, so a server restart doesn't refill it early). Offers with no limit always show as plenty.
// Offers that never restock (Offer.forever: the Legendary boats) keep a tally of how many have ever been sold (sold,
// by shop and item), which the restock doesn't clear: once maxStock are sold, they're gone for good.
public class NpcStock extends SavedData {

    public static final int RESTOCK_TICKS = 20 * 60 * 20;
    public static final int UNLIMITED = Short.MAX_VALUE;

    private static final Codec<NpcStock> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()).fieldOf("left").forGetter(stock -> stock.left),
        Codec.LONG.fieldOf("restocked").forGetter(stock -> stock.restockedAt),
        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("sold", Map.of()).forGetter(stock -> stock.sold)
    ).apply(inst, NpcStock::new));

    public static final SavedDataType<NpcStock> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "npc_stock"), NpcStock::new, CODEC);

    // shop key (a job's id, or NpcShops.BOAT_MASTER) -> how many are left of each of its offers (in NpcShops.all order)
    private final Map<String, List<Integer>> left = new HashMap<>();
    private long restockedAt = -1;
    // "shop|item" -> how many of a never-restocking offer have ever been sold
    private final Map<String, Integer> sold = new HashMap<>();

    public NpcStock() {
    }

    private NpcStock(Map<String, List<Integer>> left, long restockedAt, Map<String, Integer> sold) {
        this.sold.putAll(sold);
        left.forEach((job, counts) -> this.left.put(job, new ArrayList<>(counts)));
        this.restockedAt = restockedAt;
    }

    public static NpcStock get(MinecraftServer server) {
        NpcStock stock = server.getDataStorage().computeIfAbsent(TYPE);
        stock.restockIfDue(server);
        return stock;
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private void restockIfDue(MinecraftServer server) {
        long now = now(server);
        if (this.restockedAt < 0 || now - this.restockedAt >= RESTOCK_TICKS || now < this.restockedAt) {
            this.left.clear();
            this.restockedAt = now;
            this.setDirty();
        }
    }

    // restocks now (the tests)
    public void restockNow() {
        this.left.clear();
        this.setDirty();
    }

    public long ticksUntilRestock(MinecraftServer server) {
        return Math.max(0, RESTOCK_TICKS - (now(server) - this.restockedAt));
    }

    // This job's counts, starting full (and starting over if its offers changed)
    private List<Integer> counts(String shop) {
        List<NpcShops.Offer> offers = NpcShops.all(shop);
        List<Integer> counts = this.left.get(shop);
        if (counts == null || counts.size() != offers.size()) {
            counts = new ArrayList<>();
            for (NpcShops.Offer offer : offers) {
                if (offer.maxStock() <= 0) counts.add(UNLIMITED);
                else if (offer.forever()) counts.add(Math.max(0, offer.maxStock() - this.sold.getOrDefault(soldKey(shop, offer), 0)));
                else counts.add(offer.maxStock());
            }
            this.left.put(shop, counts);
            this.setDirty();
        }
        return counts;
    }

    public int left(String shop, int index) {
        List<Integer> counts = this.counts(shop);
        return index < 0 || index >= counts.size() ? 0 : counts.get(index);
    }

    // Takes up to `wanted`; returns how many it could
    public int take(String shop, int index, int wanted) {
        List<Integer> counts = this.counts(shop);
        if (index < 0 || index >= counts.size()) return 0;
        int have = counts.get(index);
        if (have >= UNLIMITED) return wanted;
        int taken = Math.min(have, wanted);
        counts.set(index, have - taken);
        NpcShops.Offer offer = NpcShops.all(shop).get(index);
        if (offer.forever() && taken > 0) this.sold.merge(soldKey(shop, offer), taken, Integer::sum);
        this.setDirty();
        return taken;
    }

    private static String soldKey(String shop, NpcShops.Offer offer) {
        return shop + "|" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.item().get());
    }

    // how many of a never-restocking offer have ever been sold (the tests)
    public int soldForever(String shop, NpcShops.Offer offer) {
        return this.sold.getOrDefault(soldKey(shop, offer), 0);
    }
}

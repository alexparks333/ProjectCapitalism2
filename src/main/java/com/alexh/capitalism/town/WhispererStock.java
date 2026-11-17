package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.librarian.KnowledgeQuality;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

// What the Whisperer has left to sell (Town.md): SIZE Knowledge Pieces, each a random quality, shared by every player
// on the server and saved with the world. Every RESTOCK_TICKS (an hour of game time) he has a fresh SIZE. Faded is the
// most common, Radiant the rarest (WEIGHTS). Each costs its quality's emeralds (Faded 1 ... Radiant 4).
public class WhispererStock extends SavedData {

    public static final int SIZE = 5;
    public static final int RESTOCK_TICKS = 60 * 60 * 20;
    public static final int SOLD = -1;
    // how likely each quality is, Faded to Radiant
    private static final int[] WEIGHTS = {40, 30, 20, 10};

    private static final Codec<WhispererStock> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.listOf().fieldOf("stock").forGetter(stock -> stock.stock),
        Codec.LONG.fieldOf("restocked").forGetter(stock -> stock.restockedAt)
    ).apply(inst, WhispererStock::new));

    public static final SavedDataType<WhispererStock> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "whisperer_stock"), WhispererStock::new, CODEC);

    // each piece's quality (KnowledgeQuality ordinal), or SOLD
    private final List<Integer> stock = new ArrayList<>();
    private long restockedAt = -1;

    public WhispererStock() {
    }

    private WhispererStock(List<Integer> stock, long restockedAt) {
        this.stock.addAll(stock);
        this.restockedAt = restockedAt;
    }

    public static WhispererStock get(MinecraftServer server) {
        WhispererStock stock = server.getDataStorage().computeIfAbsent(TYPE);
        stock.restockIfDue(server);
        return stock;
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private void restockIfDue(MinecraftServer server) {
        long now = now(server);
        if (this.restockedAt < 0 || now - this.restockedAt >= RESTOCK_TICKS || now < this.restockedAt || this.stock.size() != SIZE) {
            this.restock(server.overworld().getRandom(), now);
        }
    }

    // A fresh SIZE pieces (also used by the tests)
    public void restock(RandomSource random, long now) {
        this.stock.clear();
        for (int i = 0; i < SIZE; i++) this.stock.add(rollQuality(random).ordinal());
        this.restockedAt = now;
        this.setDirty();
    }

    public static KnowledgeQuality rollQuality(RandomSource random) {
        int total = 0;
        for (int weight : WEIGHTS) total += weight;
        int pick = random.nextInt(total);
        for (int i = 0; i < WEIGHTS.length; i++) {
            pick -= WEIGHTS[i];
            if (pick < 0) return KnowledgeQuality.byOrdinal(i);
        }
        return KnowledgeQuality.FADED;
    }

    // This piece's quality ordinal, or SOLD
    public int get(int index) {
        return index >= 0 && index < this.stock.size() ? this.stock.get(index) : SOLD;
    }

    public void markSold(int index) {
        if (index >= 0 && index < this.stock.size()) {
            this.stock.set(index, SOLD);
            this.setDirty();
        }
    }

    public long ticksUntilRestock(MinecraftServer server) {
        return Math.max(0, RESTOCK_TICKS - (now(server) - this.restockedAt));
    }
}

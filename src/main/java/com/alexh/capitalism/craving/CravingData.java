package com.alexh.capitalism.craving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// A player's cravings (Cravings.md; ModAttachments.CRAVINGS: saved, kept through death, synced to that player): the
// day they're for, today's food (an item id, and for a seared cut its doneness, else -1) and drink, whether each has
// been satisfied today, how many they've satisfied in all (the "Cravings satisfied" standings), and a salt an admin's
// reroll bumps. Read on the client with Cravings.of(player).
public class CravingData {

    public static final MapCodec<CravingData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.LONG.optionalFieldOf("day", -1L).forGetter(d -> d.day),
        Codec.STRING.optionalFieldOf("food", "").forGetter(d -> d.food),
        Codec.INT.optionalFieldOf("food_done", -1).forGetter(d -> d.foodDone),
        Codec.STRING.optionalFieldOf("drink", "").forGetter(d -> d.drink),
        Codec.BOOL.optionalFieldOf("ate", false).forGetter(d -> d.ate),
        Codec.BOOL.optionalFieldOf("drank", false).forGetter(d -> d.drank),
        Codec.INT.optionalFieldOf("total", 0).forGetter(d -> d.total),
        Codec.INT.optionalFieldOf("salt", 0).forGetter(d -> d.salt)
    ).apply(i, CravingData::new));

    public static final StreamCodec<ByteBuf, CravingData> STREAM_CODEC = StreamCodec.of(
        (buf, d) -> {
            ByteBufCodecs.VAR_LONG.encode(buf, d.day);
            ByteBufCodecs.STRING_UTF8.encode(buf, d.food);
            ByteBufCodecs.VAR_INT.encode(buf, d.foodDone + 1);
            ByteBufCodecs.STRING_UTF8.encode(buf, d.drink);
            ByteBufCodecs.BOOL.encode(buf, d.ate);
            ByteBufCodecs.BOOL.encode(buf, d.drank);
            ByteBufCodecs.VAR_INT.encode(buf, d.total);
            ByteBufCodecs.VAR_INT.encode(buf, d.salt);
        },
        buf -> new CravingData(ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf) - 1,
            ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf)));

    long day = -1;
    String food = "";
    int foodDone = -1;
    String drink = "";
    boolean ate, drank;
    int total;
    int salt;

    public CravingData() {
    }

    private CravingData(long day, String food, int foodDone, String drink, boolean ate, boolean drank, int total, int salt) {
        this.day = day;
        this.food = food;
        this.foodDone = foodDone;
        this.drink = drink;
        this.ate = ate;
        this.drank = drank;
        this.total = total;
        this.salt = salt;
    }

    public long day() {
        return this.day;
    }

    // today's food craving: an item id ("capitalism:salmon_roll"), and for a seared cut its doneness (Sear.Doneness
    // ordinal), else -1
    public String food() {
        return this.food;
    }

    public int foodDone() {
        return this.foodDone;
    }

    public String drink() {
        return this.drink;
    }

    // satisfied today?
    public boolean ate() {
        return this.ate;
    }

    public boolean drank() {
        return this.drank;
    }

    // cravings satisfied, all told
    public int total() {
        return this.total;
    }
}

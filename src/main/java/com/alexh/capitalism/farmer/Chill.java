package com.alexh.capitalism.farmer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Meat being kept in a running Fridge or Freezer (ColdStorageBlockEntity puts it on, ModComponents.CHILL): it rots
// `slower` times slower, and at game tick `since` it had `left` ticks left at the normal rate. Only for showing it
// (the tooltip counts down smoothly from it, where the real timer, ROTS_AT, is pushed back once a second). Taken off as
// soon as the meat is out of cold storage or the fuel runs out (FreshnessEvents, ColdStorageBlockEntity).
public record Chill(int slower, long since, long left) {

    public static final Codec<Chill> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.fieldOf("slower").forGetter(Chill::slower),
        Codec.LONG.fieldOf("since").forGetter(Chill::since),
        Codec.LONG.fieldOf("left").forGetter(Chill::left)
    ).apply(inst, Chill::new));

    public static final StreamCodec<ByteBuf, Chill> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, Chill::slower,
        ByteBufCodecs.VAR_LONG, Chill::since,
        ByteBufCodecs.VAR_LONG, Chill::left,
        Chill::new);

    // Ticks left at the normal rate, now (it goes down `slower` times slower than the clock)
    public long leftAtNormalRate(long now) {
        return Math.max(0, this.left - Math.max(0, now - this.since) / this.slower);
    }

    // It's up to date (put on in the last few seconds): an old one is from meat that's been taken out
    public boolean current(long now) {
        return now - this.since < 60;
    }
}

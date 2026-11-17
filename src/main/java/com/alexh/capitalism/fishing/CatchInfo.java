package com.alexh.capitalism.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

// A rare catch's story, kept on the item (ModComponents.CATCH_INFO) and on its wall plaque: how much it weighed (lb;
// 0: not weighed, the bottle and the chest), who landed it, and the day (the calendar's: "Summer 9, Year 2").
public record CatchInfo(float weight, String catcher, String date) {

    public static final Codec<CatchInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("weight").forGetter(CatchInfo::weight),
        Codec.STRING.fieldOf("catcher").forGetter(CatchInfo::catcher),
        Codec.STRING.fieldOf("date").forGetter(CatchInfo::date)
    ).apply(i, CatchInfo::new));

    public static final StreamCodec<ByteBuf, CatchInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, CatchInfo::weight,
        ByteBufCodecs.STRING_UTF8, CatchInfo::catcher,
        ByteBufCodecs.STRING_UTF8, CatchInfo::date,
        CatchInfo::new);

    // "312 lb" ("4.6 lb" for the little ones)
    public String weightText() {
        return this.weight >= 20 ? Math.round(this.weight) + " lb" : String.format(Locale.ROOT, "%.1f lb", this.weight);
    }

    // "312 lb · caught by Alex · Summer 9, Year 2"
    public String line() {
        return (this.weight > 0 ? this.weightText() + " · " : "") + "caught by " + this.catcher + " · " + this.date;
    }

    // "Summer 9, Year 2" for a calendar date
    public static String dateText(com.alexh.capitalism.calendar.Calendar.Date date) {
        return date.season().title + " " + date.day() + ", Year " + date.year();
    }
}

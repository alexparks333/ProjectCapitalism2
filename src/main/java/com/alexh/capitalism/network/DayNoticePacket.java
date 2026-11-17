package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the new day's little card, bottom right (client/DayNoticeHud, Calendar.md): a heading ("Good
// morning!", or the new season's name), the date, and a festival line (empty: none), each with its colour.
public record DayNoticePacket(String head, int headColor, String date, String festival, int festivalColor) implements CustomPacketPayload {

    public static final Type<DayNoticePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "day_notice"));
    public static final StreamCodec<ByteBuf, DayNoticePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DayNoticePacket::head,
        ByteBufCodecs.INT, DayNoticePacket::headColor,
        ByteBufCodecs.STRING_UTF8, DayNoticePacket::date,
        ByteBufCodecs.STRING_UTF8, DayNoticePacket::festival,
        ByteBufCodecs.INT, DayNoticePacket::festivalColor,
        DayNoticePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

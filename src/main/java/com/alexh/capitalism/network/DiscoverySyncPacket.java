package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client: discovered items for the guides. `replace` = this is the whole list (login, /discoveries);
// otherwise these are new ones to add.
public record DiscoverySyncPacket(List<Identifier> items, boolean replace) implements CustomPacketPayload {

    public static final Type<DiscoverySyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "discovery_sync"));
    public static final StreamCodec<ByteBuf, DiscoverySyncPacket> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), DiscoverySyncPacket::items,
        ByteBufCodecs.BOOL, DiscoverySyncPacket::replace,
        DiscoverySyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

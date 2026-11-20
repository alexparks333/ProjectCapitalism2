package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Client -> server, from the admin panel's Save (client/AdminPanelScreen): every change waiting to be saved, keys[i]
// to values[i] (settings, Banker prices), or the one "add the held item" (core/AdminPanel.apply). Operators only; the
// server checks.
public record AdminSetPacket(List<String> keys, List<String> values) implements CustomPacketPayload {

    public static final Type<AdminSetPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "admin_set"));
    public static final StreamCodec<ByteBuf, AdminSetPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(1024)), AdminSetPacket::keys,
        ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(1024)), AdminSetPacket::values,
        AdminSetPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

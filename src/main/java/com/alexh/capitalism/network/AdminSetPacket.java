package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the admin panel (client/AdminPanelScreen): change a setting, a Banker price, or add the held
// item to the Banker's list (core/AdminPanel.apply). Operators only; the server checks.
public record AdminSetPacket(String key, String value) implements CustomPacketPayload {

    public static final Type<AdminSetPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "admin_set"));
    public static final StreamCodec<ByteBuf, AdminSetPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(256), AdminSetPacket::key,
        ByteBufCodecs.stringUtf8(256), AdminSetPacket::value,
        AdminSetPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

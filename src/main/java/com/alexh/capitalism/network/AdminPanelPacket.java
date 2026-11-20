package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client (operators): the admin panel's figures (core/AdminPanel.settings and .banker). open: /admin panel
// (open the screen); otherwise just new figures after a change (client/AdminPanelScreen updates if it's open).
public record AdminPanelPacket(List<String> settings, List<String> banker, boolean open) implements CustomPacketPayload {

    public static final Type<AdminPanelPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "admin_panel"));
    public static final StreamCodec<ByteBuf, AdminPanelPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AdminPanelPacket::settings,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AdminPanelPacket::banker,
        ByteBufCodecs.BOOL, AdminPanelPacket::open,
        AdminPanelPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

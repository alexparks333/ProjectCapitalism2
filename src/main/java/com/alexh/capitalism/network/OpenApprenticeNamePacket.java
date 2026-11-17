package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the owner right-clicked their Apprentice with the Apprentice Wand, open the naming box
// (client/ApprenticeNameScreen) for this entity, filled with its current name ("" = none).
public record OpenApprenticeNamePacket(int entityId, String name) implements CustomPacketPayload {

    public static final Type<OpenApprenticeNamePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "open_apprentice_name"));
    public static final StreamCodec<ByteBuf, OpenApprenticeNamePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, OpenApprenticeNamePacket::entityId,
        ByteBufCodecs.STRING_UTF8, OpenApprenticeNamePacket::name,
        OpenApprenticeNamePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

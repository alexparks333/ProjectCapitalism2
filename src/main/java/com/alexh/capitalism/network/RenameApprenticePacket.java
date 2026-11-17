package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the naming box: give this Apprentice this name ("" = back to "Iron Apprentice"). The server
// checks it's the owner, nearby and holding the wand (enchanter/Apprentice.rename).
public record RenameApprenticePacket(int entityId, String name) implements CustomPacketPayload {

    public static final Type<RenameApprenticePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "rename_apprentice"));
    public static final StreamCodec<ByteBuf, RenameApprenticePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RenameApprenticePacket::entityId,
        ByteBufCodecs.stringUtf8(64), RenameApprenticePacket::name,
        RenameApprenticePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

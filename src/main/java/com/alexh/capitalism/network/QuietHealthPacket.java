package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client, sent right before the server lowers the player's health because a bonus heart faded away.
// The client lowers its health first, so when vanilla's health update arrives it isn't a "drop" and the screen
// doesn't shake like the player got hit by nothing.
public record QuietHealthPacket(float health) implements CustomPacketPayload {

    public static final Type<QuietHealthPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "quiet_health"));
    public static final StreamCodec<ByteBuf, QuietHealthPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, QuietHealthPacket::health,
        QuietHealthPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

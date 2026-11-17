package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: "I'm sneaking and holding jump" (Ender Leap). The server checks the effect, cooldown and hunger.
public record EnderLeapPacket() implements CustomPacketPayload {

    public static final EnderLeapPacket INSTANCE = new EnderLeapPacket();
    public static final Type<EnderLeapPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "ender_leap"));
    public static final StreamCodec<ByteBuf, EnderLeapPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

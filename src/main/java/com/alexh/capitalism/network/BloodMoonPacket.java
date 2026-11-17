package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: a Blood Moon began or ended (event/BloodMoon): the client turns the sky, moon and fog red
// (client/BloodMoonClient).
public record BloodMoonPacket(boolean active) implements CustomPacketPayload {

    public static final Type<BloodMoonPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_moon"));
    public static final StreamCodec<ByteBuf, BloodMoonPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, BloodMoonPacket::active,
        BloodMoonPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

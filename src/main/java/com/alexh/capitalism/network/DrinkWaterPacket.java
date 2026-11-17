package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server: "I right-clicked water with an empty hand". The server checks everything itself.
public record DrinkWaterPacket() implements CustomPacketPayload {

    public static final DrinkWaterPacket INSTANCE = new DrinkWaterPacket();
    public static final Type<DrinkWaterPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "drink_water"));
    public static final StreamCodec<ByteBuf, DrinkWaterPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

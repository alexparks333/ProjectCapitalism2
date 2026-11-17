package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the dev /pick screen: take or drop this job (Job ordinal). Ops only; the server checks.
public record PickJobPacket(int job) implements CustomPacketPayload {

    public static final Type<PickJobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pick_job"));
    public static final StreamCodec<ByteBuf, PickJobPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PickJobPacket::job,
        PickJobPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

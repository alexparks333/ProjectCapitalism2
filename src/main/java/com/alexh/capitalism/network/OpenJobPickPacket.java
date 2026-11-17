package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: open the job screen (/pick), e.g. right after a Changing Star goes into the Wishing Well
// (occupation/WishingWellBlock), so the player can pick which job to change straight away.
public record OpenJobPickPacket() implements CustomPacketPayload {

    public static final Type<OpenJobPickPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "open_job_pick"));
    public static final StreamCodec<ByteBuf, OpenJobPickPacket> STREAM_CODEC = StreamCodec.unit(new OpenJobPickPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

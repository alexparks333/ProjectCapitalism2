package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the player's jobs as a bitmask (see Job.bit), so the client can stop things early (e.g. a Cook
// starting to eat raw meat) and show them.
public record JobSyncPacket(int jobs, int essentialSlots, int funSlots, boolean changing) implements CustomPacketPayload {

    public static final Type<JobSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "job_sync"));
    public static final StreamCodec<ByteBuf, JobSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, JobSyncPacket::jobs,
        ByteBufCodecs.VAR_INT, JobSyncPacket::essentialSlots,
        ByteBufCodecs.VAR_INT, JobSyncPacket::funSlots,
        ByteBufCodecs.BOOL, JobSyncPacket::changing,
        JobSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

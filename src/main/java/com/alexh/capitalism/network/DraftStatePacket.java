package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the job drafts (occupation/JobDraft) as the job screen shows them. Per kind: the line to show
// ("" = no draft) and whether that column is out of reach for this player right now.
public record DraftStatePacket(String essentialLine, boolean essentialLocked, String funLine, boolean funLocked) implements CustomPacketPayload {

    public static final Type<DraftStatePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "draft_state"));
    public static final StreamCodec<ByteBuf, DraftStatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DraftStatePacket::essentialLine,
        ByteBufCodecs.BOOL, DraftStatePacket::essentialLocked,
        ByteBufCodecs.STRING_UTF8, DraftStatePacket::funLine,
        ByteBufCodecs.BOOL, DraftStatePacket::funLocked,
        DraftStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

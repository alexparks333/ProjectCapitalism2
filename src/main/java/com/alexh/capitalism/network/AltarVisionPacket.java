package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: a Blood Shard was offered at the Blood Moon Altar (decor/BloodDecorBlock.offer): for this player
// alone the sky, moon and fog turn blood red for `ticks` (client/BloodMoonClient.vision).
public record AltarVisionPacket(int ticks) implements CustomPacketPayload {

    public static final Type<AltarVisionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "altar_vision"));
    public static final StreamCodec<ByteBuf, AltarVisionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AltarVisionPacket::ticks,
        AltarVisionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

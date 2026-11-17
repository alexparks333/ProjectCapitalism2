package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client: /insurance guide, open Not-A-Scam Insurance's terms of service (client/InsuranceGuideScreen) with
// the server's numbers: each plan's price and rate (Bronze, Silver, Gold, Premium), then the uninsured death tax and
// Blood Moon Insurance's price (deathtax/Insurance.guide).
public record OpenInsuranceGuidePacket(List<Integer> values) implements CustomPacketPayload {

    public static final Type<OpenInsuranceGuidePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "open_insurance_guide"));
    public static final StreamCodec<ByteBuf, OpenInsuranceGuidePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), OpenInsuranceGuidePacket::values,
        OpenInsuranceGuidePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

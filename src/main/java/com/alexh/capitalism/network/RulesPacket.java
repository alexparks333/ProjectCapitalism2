package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client, on login: the server's rules (core/ServerRules, config/capitalism-rules.txt) for the General Guide's
// Rules page, and the Blood Moon Insurance price (server config) for its Insurance tab (client/ClientRules).
public record RulesPacket(List<String> rules, int bloodMoonInsurancePrice) implements CustomPacketPayload {

    public static final Type<RulesPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "rules"));
    public static final StreamCodec<ByteBuf, RulesPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(2048).apply(ByteBufCodecs.list(200)), RulesPacket::rules,
        ByteBufCodecs.VAR_INT, RulesPacket::bloodMoonInsurancePrice,
        RulesPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
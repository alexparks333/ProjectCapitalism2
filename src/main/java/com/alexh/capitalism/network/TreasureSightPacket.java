package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client: the treasure chests Treasure Sight shows right now (empty = none / the effect ended).
public record TreasureSightPacket(List<BlockPos> chests) implements CustomPacketPayload {

    public static final Type<TreasureSightPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "treasure_sight"));
    public static final StreamCodec<ByteBuf, TreasureSightPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), TreasureSightPacket::chests,
        TreasureSightPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

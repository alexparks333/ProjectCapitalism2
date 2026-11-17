package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.player.ThirstData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

// Server -> client: thirst, the golden water and golden food bars, Water Purity, and how high the thirst/food
// bars may go right now (lowered by Low purity / a Poor diet), for the HUD and the Wellness screen.
// Also the real food bar: when you eat, the client runs vanilla's eating too, which stops the bar at 20 and would
// hide a Balanced diet's bonus drumstick (the server keeps it, but vanilla only resends food when it changes).
public record ThirstSyncPacket(int thirst, int goldThirst, int goldFood, float purity, int thirstCap, int foodCap, int food) implements CustomPacketPayload {

    public static final Type<ThirstSyncPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "thirst_sync")
    );

    public static final StreamCodec<ByteBuf, ThirstSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, ThirstSyncPacket::thirst,
        ByteBufCodecs.INT, ThirstSyncPacket::goldThirst,
        ByteBufCodecs.INT, ThirstSyncPacket::goldFood,
        ByteBufCodecs.FLOAT, ThirstSyncPacket::purity,
        ByteBufCodecs.VAR_INT, ThirstSyncPacket::thirstCap,
        ByteBufCodecs.VAR_INT, ThirstSyncPacket::foodCap,
        ByteBufCodecs.VAR_INT, ThirstSyncPacket::food,
        ThirstSyncPacket::new
    );

    public static ThirstSyncPacket of(ThirstData data, Player player) {
        return new ThirstSyncPacket(data.getThirstInt(), data.getGoldThirstInt(), data.getGoldFood(), data.getPurity(), data.thirstCap, data.foodCap, player.getFoodData().getFoodLevel());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

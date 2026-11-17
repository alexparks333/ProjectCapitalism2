package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client: the player's four diet groups, plus meals eaten / meals needed before buffs start,
// for the Wellness screen.
public record DietSyncPacket(float vegetable, float fruit, float protein, float carbs, int meals, int mealsNeeded) implements CustomPacketPayload {

    public static final Type<DietSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "diet_sync"));

    public static final StreamCodec<ByteBuf, DietSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, DietSyncPacket::vegetable,
        ByteBufCodecs.FLOAT, DietSyncPacket::fruit,
        ByteBufCodecs.FLOAT, DietSyncPacket::protein,
        ByteBufCodecs.FLOAT, DietSyncPacket::carbs,
        ByteBufCodecs.VAR_INT, DietSyncPacket::meals,
        ByteBufCodecs.VAR_INT, DietSyncPacket::mealsNeeded,
        DietSyncPacket::new
    );

    public float[] values() {
        return new float[]{this.vegetable, this.fruit, this.protein, this.carbs};
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the pet window's Actions tab (client/PetScreen): do this to that pet (Pets.ACTION_STAY...).
// The server checks it's the owner and close by (pets/Pets.action).
public record PetActionPacket(int entityId, int action) implements CustomPacketPayload {

    public static final Type<PetActionPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_action"));
    public static final StreamCodec<ByteBuf, PetActionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PetActionPacket::entityId,
        ByteBufCodecs.VAR_INT, PetActionPacket::action,
        PetActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

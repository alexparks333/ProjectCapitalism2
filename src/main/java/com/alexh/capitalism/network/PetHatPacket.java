package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the pet window's Actions tab (client/PetScreen): put this hat (designer/Hat, by its place) on
// the pet from the player's inventory, or take it off (-1). The server checks it's the owner and close by
// (pets/Pets.wearHat).
public record PetHatPacket(int entityId, int hat) implements CustomPacketPayload {

    public static final Type<PetHatPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_hat"));
    public static final StreamCodec<ByteBuf, PetHatPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PetHatPacket::entityId,
        ByteBufCodecs.VAR_INT, PetHatPacket::hat,
        PetHatPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

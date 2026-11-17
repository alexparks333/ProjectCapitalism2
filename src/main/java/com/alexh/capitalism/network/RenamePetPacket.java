package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.pets.Pets;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Client -> server, from the pet window's Actions tab (client/PetScreen): give this pet this name ("" = back to "Pet
// Pig"). The server checks it's the owner and close by (pets/Pets.rename).
public record RenamePetPacket(int entityId, String name) implements CustomPacketPayload {

    public static final Type<RenamePetPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "rename_pet"));
    public static final StreamCodec<ByteBuf, RenamePetPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RenamePetPacket::entityId,
        ByteBufCodecs.stringUtf8(Pets.MAX_NAME * 4), RenamePetPacket::name,
        RenamePetPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

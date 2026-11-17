package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

// Server -> client: they've just respawned owing death tax: show the death tax board big for a few seconds
// (client/PurgatoryHud.showBig; it reads what it shows from their synced DeathTaxData, the rest here is for the log). Their last death:
// the gold they had (nuggets), the rate and why, the tax; everything they owe now, and the breakdown ("Death: 3 gold",
// "Sam: 10 gold"). (deathtax/DeathTax.onRespawn)
public record DeathTaxPopupPacket(long gold, int rate, String why, long tax, long owed, List<String> breakdown) implements CustomPacketPayload {

    public static final Type<DeathTaxPopupPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "death_tax_popup"));
    public static final StreamCodec<ByteBuf, DeathTaxPopupPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, DeathTaxPopupPacket::gold,
        ByteBufCodecs.VAR_INT, DeathTaxPopupPacket::rate,
        ByteBufCodecs.STRING_UTF8, DeathTaxPopupPacket::why,
        ByteBufCodecs.VAR_LONG, DeathTaxPopupPacket::tax,
        ByteBufCodecs.VAR_LONG, DeathTaxPopupPacket::owed,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DeathTaxPopupPacket::breakdown,
        DeathTaxPopupPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.alexh.capitalism.network;

import com.alexh.capitalism.Capitalism;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server -> client (every second), for the Wellness screen and the temperature ball:
//  - Wellbeing score, where the bonus heart starts and the top of the bar, where the lost heart starts and the bottom
//    of the bar (seconds, negative), and what is dragging it down (WellnessEvents.N_*)
//  - grace period length (seconds)
//  - for diet / purity / hearts: the state right now, the state in effect, and seconds they've been different
//  - the current (fading) max health bonus
//  - body heat (0-100) and what's acting on it (player/BodyTemperature.R_*)
public record WellbeingSyncPacket(int score, int buffAt, int max, int grace, int debuffAt, int min, int neglect,
                                  int[] raw, int[] effective, int[] pending, float healthMod,
                                  float bodyHeat, int heatReasons) implements CustomPacketPayload {

    public static final Type<WellbeingSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "wellbeing_sync"));

    public static final StreamCodec<ByteBuf, WellbeingSyncPacket> STREAM_CODEC = StreamCodec.of(WellbeingSyncPacket::write, WellbeingSyncPacket::read);

    private static void write(ByteBuf buf, WellbeingSyncPacket p) {
        for (int v : new int[]{p.score, p.buffAt, p.max, p.grace, p.debuffAt, p.min, p.neglect}) ByteBufCodecs.VAR_INT.encode(buf, v);
        for (int i = 0; i < 3; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, p.raw[i]);
            ByteBufCodecs.VAR_INT.encode(buf, p.effective[i]);
            ByteBufCodecs.VAR_INT.encode(buf, p.pending[i]);
        }
        ByteBufCodecs.FLOAT.encode(buf, p.healthMod);
        ByteBufCodecs.FLOAT.encode(buf, p.bodyHeat);
        ByteBufCodecs.VAR_INT.encode(buf, p.heatReasons);
    }

    private static WellbeingSyncPacket read(ByteBuf buf) {
        int[] head = new int[7];
        for (int i = 0; i < head.length; i++) head[i] = ByteBufCodecs.VAR_INT.decode(buf);
        int[] raw = new int[3];
        int[] effective = new int[3];
        int[] pending = new int[3];
        for (int i = 0; i < 3; i++) {
            raw[i] = ByteBufCodecs.VAR_INT.decode(buf);
            effective[i] = ByteBufCodecs.VAR_INT.decode(buf);
            pending[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        float healthMod = ByteBufCodecs.FLOAT.decode(buf);
        float bodyHeat = ByteBufCodecs.FLOAT.decode(buf);
        int heatReasons = ByteBufCodecs.VAR_INT.decode(buf);
        return new WellbeingSyncPacket(head[0], head[1], head[2], head[3], head[4], head[5], head[6], raw, effective, pending, healthMod, bodyHeat, heatReasons);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

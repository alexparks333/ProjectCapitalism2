package com.alexh.capitalism.deathtax;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The death tax's server-wide records (Insurance.md): gold paid toward Purge debts for creditors who were offline
// (handed over when they next log in), every player's Purge kills and gold taken (the Ledger's standings), their names,
// and how many Blood Moons have started (Blood Moon Insurance covers one by its number).
public final class DeathTaxLedger extends SavedData {

    static final Codec<DeathTaxLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("owed", Map.of()).forGetter(d -> d.owed),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("gold_taken", Map.of()).forGetter(d -> d.goldTaken),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("purge_kills", Map.of()).forGetter(d -> d.purgeKills),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(d -> d.names),
        Codec.LONG.optionalFieldOf("moons_started", 0L).forGetter(d -> d.moonsStarted)
    ).apply(i, DeathTaxLedger::new));
    static final SavedDataType<DeathTaxLedger> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "death_tax"), DeathTaxLedger::new, CODEC);

    // nuggets waiting for an offline creditor
    final Map<UUID, Long> owed = new HashMap<>();
    final Map<UUID, Long> goldTaken = new HashMap<>();
    final Map<UUID, Integer> purgeKills = new HashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    long moonsStarted;

    public DeathTaxLedger() {
    }

    private DeathTaxLedger(Map<UUID, Long> owed, Map<UUID, Long> goldTaken, Map<UUID, Integer> purgeKills, Map<UUID, String> names, long moonsStarted) {
        this.owed.putAll(owed);
        this.goldTaken.putAll(goldTaken);
        this.purgeKills.putAll(purgeKills);
        this.names.putAll(names);
        this.moonsStarted = moonsStarted;
    }

    public static DeathTaxLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public long owedTo(UUID id) {
        return this.owed.getOrDefault(id, 0L);
    }

    public long goldTaken(UUID id) {
        return this.goldTaken.getOrDefault(id, 0L);
    }

    public int purgeKills(UUID id) {
        return this.purgeKills.getOrDefault(id, 0);
    }

    public long moonsStarted() {
        return this.moonsStarted;
    }
}

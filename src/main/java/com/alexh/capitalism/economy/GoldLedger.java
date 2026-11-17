package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Saved with the world: every owned container (by position) with its owner and the gold last counted inside it,
// plus each player's last-known carried gold. Net worth = carried + all their containers.
//
// Why it can't double count: each container is ONE entry keyed by its exact position, and each entry only ever
// holds that block's own slots (a double chest is two entries of 27 slots each, never 54 + 54).
// Containers in unloaded chunks keep their last count, which stays correct because nothing changes there.
public class GoldLedger extends SavedData {

    public record ContainerEntry(UUID owner, long nuggets) {
        static final Codec<ContainerEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(ContainerEntry::owner),
            Codec.LONG.fieldOf("nuggets").forGetter(ContainerEntry::nuggets)
        ).apply(inst, ContainerEntry::new));
    }

    public record PlayerEntry(String name, long carriedNuggets) {
        static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(PlayerEntry::name),
            Codec.LONG.fieldOf("carried").forGetter(PlayerEntry::carriedNuggets)
        ).apply(inst, PlayerEntry::new));
    }

    private record ContainerRow(GlobalPos pos, ContainerEntry entry) {
        static final Codec<ContainerRow> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(ContainerRow::pos),
            ContainerEntry.CODEC.fieldOf("entry").forGetter(ContainerRow::entry)
        ).apply(inst, ContainerRow::new));
    }

    private record PlayerRow(UUID id, PlayerEntry entry) {
        static final Codec<PlayerRow> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(PlayerRow::id),
            PlayerEntry.CODEC.fieldOf("entry").forGetter(PlayerRow::entry)
        ).apply(inst, PlayerRow::new));
    }

    private static final Codec<GoldLedger> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ContainerRow.CODEC.listOf().fieldOf("containers").forGetter(GoldLedger::containerRows),
        PlayerRow.CODEC.listOf().fieldOf("players").forGetter(GoldLedger::playerRows)
    ).apply(inst, GoldLedger::new));

    public static final SavedDataType<GoldLedger> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "gold_ledger"), GoldLedger::new, CODEC);

    private final Map<GlobalPos, ContainerEntry> containers = new HashMap<>();
    private final Map<UUID, PlayerEntry> players = new HashMap<>();

    public GoldLedger() {
    }

    private GoldLedger(List<ContainerRow> containerRows, List<PlayerRow> playerRows) {
        containerRows.forEach(row -> this.containers.put(row.pos(), row.entry()));
        playerRows.forEach(row -> this.players.put(row.id(), row.entry()));
    }

    public static GoldLedger get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<ContainerRow> containerRows() {
        List<ContainerRow> rows = new ArrayList<>();
        this.containers.forEach((pos, entry) -> rows.add(new ContainerRow(pos, entry)));
        return rows;
    }

    private List<PlayerRow> playerRows() {
        List<PlayerRow> rows = new ArrayList<>();
        this.players.forEach((id, entry) -> rows.add(new PlayerRow(id, entry)));
        return rows;
    }

    // ── Containers ─────────────────────────────────────────────────────────

    public Set<GlobalPos> containerPositions() {
        return new HashSet<>(this.containers.keySet());
    }

    public ContainerEntry container(GlobalPos pos) {
        return this.containers.get(pos);
    }

    public void setContainer(GlobalPos pos, UUID owner, long nuggets) {
        ContainerEntry updated = new ContainerEntry(owner, nuggets);
        if (!updated.equals(this.containers.put(pos, updated))) {
            this.setDirty();
        }
    }

    public void removeContainer(GlobalPos pos) {
        if (this.containers.remove(pos) != null) {
            this.setDirty();
        }
    }

    // ── Players ────────────────────────────────────────────────────────────

    public void setPlayer(UUID id, String name, long carriedNuggets) {
        PlayerEntry updated = new PlayerEntry(name, carriedNuggets);
        if (!updated.equals(this.players.put(id, updated))) {
            this.setDirty();
        }
    }

    // Everyone with carried gold on record or at least one owned container.
    public Set<UUID> knownPlayers() {
        Set<UUID> ids = new HashSet<>(this.players.keySet());
        this.containers.values().forEach(entry -> ids.add(entry.owner()));
        return ids;
    }

    public String nameOf(UUID id) {
        PlayerEntry entry = this.players.get(id);
        return entry == null ? null : entry.name();
    }

    // Carried gold (last known) + gold in every container they own, in nuggets.
    public long netWorth(UUID id) {
        PlayerEntry entry = this.players.get(id);
        long total = entry == null ? 0 : entry.carriedNuggets();
        for (ContainerEntry container : this.containers.values()) {
            if (container.owner().equals(id)) total += container.nuggets();
        }
        return total;
    }
}

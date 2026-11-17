package com.alexh.capitalism.npc;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Where each job's NPC stands (set by admins with /npc setspot <job>), saved with the world, and which NPC entity is
// the current one there (so stray copies can be cleaned up).
public class NpcSpots extends SavedData {

    public record Spot(String job, GlobalPos pos, float yaw, Optional<UUID> npc) {
        static final Codec<Spot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("job").forGetter(Spot::job),
            GlobalPos.CODEC.fieldOf("pos").forGetter(Spot::pos),
            Codec.FLOAT.fieldOf("yaw").forGetter(Spot::yaw),
            UUIDUtil.CODEC.optionalFieldOf("npc").forGetter(Spot::npc)
        ).apply(inst, Spot::new));

        Spot withNpc(@Nullable UUID id) {
            return new Spot(this.job, this.pos, this.yaw, Optional.ofNullable(id));
        }
    }

    private static final Codec<NpcSpots> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Spot.CODEC.listOf().fieldOf("spots").forGetter(NpcSpots::list)
    ).apply(inst, NpcSpots::new));

    public static final SavedDataType<NpcSpots> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "npc_spots"), NpcSpots::new, CODEC);

    private final Map<String, Spot> spots = new HashMap<>();

    public NpcSpots() {
    }

    private NpcSpots(List<Spot> list) {
        list.forEach(spot -> this.spots.put(spot.job(), spot));
    }

    public static NpcSpots get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Spot> list() {
        return new ArrayList<>(this.spots.values());
    }

    public @Nullable Spot get(String job) {
        return this.spots.get(job);
    }

    public void set(String job, GlobalPos pos, float yaw) {
        this.spots.put(job, new Spot(job, pos, yaw, Optional.empty()));
        this.setDirty();
    }

    public @Nullable Spot remove(String job) {
        Spot removed = this.spots.remove(job);
        if (removed != null) this.setDirty();
        return removed;
    }

    public void setNpc(String job, @Nullable UUID id) {
        Spot spot = this.spots.get(job);
        if (spot == null) return;
        this.spots.put(job, spot.withNpc(id));
        this.setDirty();
    }
}

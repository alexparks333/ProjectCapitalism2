package com.alexh.capitalism.librarian;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// The traits a player has learned from skill books (ModAttachments.TRAITS, kept through death, synced to that player so
// the client knows them too: Wellness > Buffs & Debuffs, Sure Footed's powder snow...). Saved by id.
public class TraitData {

    public static final MapCodec<TraitData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(TraitData::ids)
    ).apply(inst, TraitData::new));

    public static final StreamCodec<ByteBuf, TraitData> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(TraitData::new, TraitData::ids);

    private final Set<Skill> traits = EnumSet.noneOf(Skill.class);

    public TraitData() {
    }

    private TraitData(List<String> ids) {
        for (String id : ids) {
            Skill skill = Skill.byId(id);
            if (skill != null && skill.trait) this.traits.add(skill);
        }
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Skill skill : this.traits) ids.add(skill.id());
        return ids;
    }

    public Set<Skill> traits() {
        return this.traits;
    }

    public boolean has(Skill skill) {
        return this.traits.contains(skill);
    }

    // false if it was already known
    public boolean learn(Skill skill) {
        return this.traits.add(skill);
    }
}

package com.alexh.capitalism.librarian;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

// What a written skill book holds (ModComponents.SKILL_BOOK): which Skill, and whether it was written in a Delicate
// Book (stronger, longer) or a Rough one.
public record SkillBook(String skill, boolean delicate) {

    public static final Codec<SkillBook> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("skill").forGetter(SkillBook::skill),
        Codec.BOOL.optionalFieldOf("delicate", false).forGetter(SkillBook::delicate)
    ).apply(inst, SkillBook::new));

    public static final StreamCodec<ByteBuf, SkillBook> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SkillBook::skill,
        ByteBufCodecs.BOOL, SkillBook::delicate,
        SkillBook::new
    );

    public @Nullable Skill resolve() {
        return Skill.byId(this.skill);
    }
}

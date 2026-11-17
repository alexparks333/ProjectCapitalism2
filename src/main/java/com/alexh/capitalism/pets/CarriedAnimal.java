package com.alexh.capitalism.pets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

// The animal in an Animal Bjorn (ModComponents.CARRIED_ANIMAL, AnimalBjornItem): what kind it is, everything about it
// (the entity's saved data, its pet data and name included), its name to show, and when it climbed in (the game time, and the
// carrier's played time then: a pet gets as hungry and thirsty as it would have while its carrier was on,
// AnimalBjornItem.letOut). Synced to clients so the carrier on
// someone's chest can show it (client/BjornLayer).
public record CarriedAnimal(Identifier type, CompoundTag data, String name, long since, long played) {

    public static final Codec<CarriedAnimal> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Identifier.CODEC.fieldOf("type").forGetter(CarriedAnimal::type),
        CompoundTag.CODEC.fieldOf("data").forGetter(CarriedAnimal::data),
        Codec.STRING.optionalFieldOf("name", "").forGetter(CarriedAnimal::name),
        Codec.LONG.optionalFieldOf("since", 0L).forGetter(CarriedAnimal::since),
        Codec.LONG.optionalFieldOf("played", 0L).forGetter(CarriedAnimal::played)
    ).apply(inst, CarriedAnimal::new));

    public static final StreamCodec<ByteBuf, CarriedAnimal> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, CarriedAnimal::type,
        ByteBufCodecs.TRUSTED_COMPOUND_TAG, CarriedAnimal::data,
        ByteBufCodecs.STRING_UTF8, CarriedAnimal::name,
        ByteBufCodecs.VAR_LONG, CarriedAnimal::since,
        ByteBufCodecs.VAR_LONG, CarriedAnimal::played,
        CarriedAnimal::new);
}

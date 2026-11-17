package com.alexh.capitalism.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// Who owns a container block (chest, barrel, furnace, shulker box, our machines...), stored on its block entity.
// `locked` = only the owner (and admins) can open or break it. Unlocked containers are still theirs for net worth.
public record ContainerOwner(UUID owner, String name, boolean locked) {

    public static final ContainerOwner NONE = new ContainerOwner(new UUID(0L, 0L), "", false);

    public static final MapCodec<ContainerOwner> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        UUIDUtil.CODEC.fieldOf("owner").forGetter(ContainerOwner::owner),
        Codec.STRING.fieldOf("name").forGetter(ContainerOwner::name),
        Codec.BOOL.fieldOf("locked").forGetter(ContainerOwner::locked)
    ).apply(inst, ContainerOwner::new));

    public boolean isOwnedBy(UUID player) {
        return this.owner.equals(player);
    }

    public ContainerOwner withLocked(boolean locked) {
        return new ContainerOwner(this.owner, this.name, locked);
    }
}

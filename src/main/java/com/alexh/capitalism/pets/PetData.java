package com.alexh.capitalism.pets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

// An animal's owner and, once a Pet Trainer has trained it, its hunger, thirst and whether it's told to stay still (ModAttachments.PET, saved with the
// animal and synced to everyone who can see it, for client/PetHud). Pets.java does the rest. Both bars are 0..20 like a
// player's.
public class PetData {

    public static final float MAX = 20f;

    public static final MapCodec<PetData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(d -> Optional.ofNullable(d.owner)),
        Codec.STRING.optionalFieldOf("ownerName", "").forGetter(d -> d.ownerName),
        Codec.BOOL.optionalFieldOf("pet", false).forGetter(d -> d.pet),
        Codec.FLOAT.optionalFieldOf("hunger", MAX).forGetter(d -> d.hunger),
        Codec.FLOAT.optionalFieldOf("thirst", MAX).forGetter(d -> d.thirst),
        Codec.BOOL.optionalFieldOf("stay", false).forGetter(d -> d.stay),
        Codec.LONG.optionalFieldOf("pettedDay", -1L).forGetter(d -> d.pettedDay),
        Codec.INT.optionalFieldOf("emptySeconds", 0).forGetter(d -> d.emptySeconds),
        Codec.INT.optionalFieldOf("hat", -1).forGetter(d -> d.hat),
        Codec.FLOAT.optionalFieldOf("size", MAX).forGetter(d -> d.size)
    ).apply(inst, (owner, name, pet, hunger, thirst, stay, day, empty, hat, size) -> {
        PetData data = new PetData(owner.orElse(null), name, pet, hunger, thirst, stay, day);
        data.emptySeconds = empty;
        data.hat = hat;
        data.size = size;
        return data;
    }));

    public static final StreamCodec<ByteBuf, PetData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), d -> Optional.ofNullable(d.owner),
        ByteBufCodecs.STRING_UTF8, d -> d.ownerName,
        ByteBufCodecs.BOOL, d -> d.pet,
        ByteBufCodecs.FLOAT, d -> d.hunger,
        ByteBufCodecs.FLOAT, d -> d.thirst,
        ByteBufCodecs.BOOL, d -> d.stay,
        ByteBufCodecs.VAR_LONG, d -> d.pettedDay,
        ByteBufCodecs.VAR_INT, d -> d.hat,
        ByteBufCodecs.FLOAT, d -> d.size,
        (owner, name, pet, hunger, thirst, stay, day, hat, size) -> {
            PetData data = new PetData(owner.orElse(null), name, pet, hunger, thirst, stay, day);
            data.hat = hat;
            data.size = size;
            return data;
        });

    private @Nullable UUID owner;
    private String ownerName;
    private boolean pet;
    private float hunger;
    private float thirst;
    // Stay Still (the pet window's Actions tab): it doesn't move at all (no AI) until it's let go
    private boolean stay;
    // The Minecraft day its owner last got its buff for petting it (PetTraits), -1 = never
    private long pettedDay;
    // How long (seconds) its food or water bar has been empty (Pets: after STARVE_SECONDS it can die). Saved, not synced.
    private int emptySeconds;
    // The Designer's hat it wears (designer/Hat, by its place; -1 none). Saved and synced.
    private int hat = -1;
    // How big its bars are without a hat (Pets.barSize: bigger animals, bigger stomachs). Saved and synced.
    private float size = MAX;

    public PetData() {
        this(null, "", false, MAX, MAX, false, -1L);
    }

    private PetData(@Nullable UUID owner, String ownerName, boolean pet, float hunger, float thirst, boolean stay, long pettedDay) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.pet = pet;
        this.hunger = hunger;
        this.thirst = thirst;
        this.stay = stay;
        this.pettedDay = pettedDay;
    }

    public int emptySeconds() {
        return this.emptySeconds;
    }

    public void setEmptySeconds(int seconds) {
        this.emptySeconds = seconds;
    }

    public long pettedDay() {
        return this.pettedDay;
    }

    public void setPettedDay(long day) {
        this.pettedDay = day;
    }

    public boolean claimed() {
        return this.owner != null;
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public boolean isOwner(UUID player) {
        return player.equals(this.owner);
    }

    public void claim(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public boolean pet() {
        return this.pet;
    }

    // Trained: a pet from now on, starting full
    public void train() {
        this.pet = true;
        this.hunger = this.max();
        this.thirst = this.max();
    }

    public com.alexh.capitalism.designer.@Nullable Hat hat() {
        return com.alexh.capitalism.designer.Hat.byIndex(this.hat);
    }

    public void setHat(com.alexh.capitalism.designer.@Nullable Hat hat) {
        this.hat = hat == null ? -1 : hat.ordinal();
        this.hunger = Math.min(this.hunger, this.max());
        this.thirst = Math.min(this.thirst, this.max());
    }

    // How full its bars can get: its size, or more with a Prada Fedora
    public float max() {
        return this.hat() == com.alexh.capitalism.designer.Hat.PRADA_FEDORA ? this.size + com.alexh.capitalism.designer.Hat.FEDORA_BARS : this.size;
    }

    public float size() {
        return this.size;
    }

    public void setSize(float size) {
        this.size = size;
        this.hunger = Math.min(this.hunger, this.max());
        this.thirst = Math.min(this.thirst, this.max());
    }

    public float hunger() {
        return this.hunger;
    }

    public float thirst() {
        return this.thirst;
    }

    public void setHunger(float hunger) {
        this.hunger = Math.clamp(hunger, 0f, this.max());
    }

    public boolean stay() {
        return this.stay;
    }

    public void setStay(boolean stay) {
        this.stay = stay;
    }

    public void setThirst(float thirst) {
        this.thirst = Math.clamp(thirst, 0f, this.max());
    }
}

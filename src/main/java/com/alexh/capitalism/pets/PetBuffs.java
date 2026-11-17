package com.alexh.capitalism.pets;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// One pet buff at a time (PetAnimals.md > How the buffs work): petting another pet for its first time that day takes off
// the last pet's buff and gives the new one. Only the PET's part is taken off: a player's own effects (potions, food,
// drinks, beacons...) of the same kind are put back as they would have been without the pet.
//
// Minecraft keeps one effect per kind (the strongest, with weaker longer ones hidden under it), so once a potion and a
// pet give the same effect you can't tell them apart. So while a pet buff is on we remember, for each of its kinds,
// the pet's own part ("pet") and every other part ("others": what the player had when they petted, and anything given
// since), and count both down with the player's effects. Taking the pet buff off = remove that kind, then give the
// others back. Saved with the player (ModAttachments.PET_BUFF), dropped once the pet buff has run out.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class PetBuffs {

    public static final class State {
        public static final MapCodec<State> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MobEffectInstance.CODEC.listOf().fieldOf("pet").forGetter(s -> s.pet),
            MobEffectInstance.CODEC.listOf().fieldOf("others").forGetter(s -> s.others)
        ).apply(i, State::new));

        private final List<MobEffectInstance> pet;
        private final List<MobEffectInstance> others;

        public State() {
            this(List.of(), List.of());
        }

        private State(List<MobEffectInstance> pet, List<MobEffectInstance> others) {
            this.pet = new ArrayList<>(pet);
            this.others = new ArrayList<>(others);
        }

        public boolean active() {
            return !this.pet.isEmpty();
        }

        private boolean tracks(Holder<MobEffect> effect) {
            return this.pet.stream().anyMatch(e -> e.is(effect));
        }
    }

    // Set while we add or remove effects ourselves, so the events below don't count them as the player's own
    private static boolean ourselves;

    private PetBuffs() {
    }

    // The pet buff the player has on now (empty: none)
    public static List<MobEffectInstance> current(Player player) {
        State state = player.getExistingDataOrNull(ModAttachments.PET_BUFF.get());
        return state == null ? List.of() : List.copyOf(state.pet);
    }

    // Takes off the last pet buff (only the pet's part) and gives this one
    public static void give(Player player, List<MobEffectInstance> buff) {
        takeOff(player);
        State state = new State();
        ourselves = true;
        try {
            for (MobEffectInstance effect : buff) {
                MobEffectInstance had = player.getEffect(effect.getEffect());
                if (had != null && !state.tracks(effect.getEffect())) state.others.addAll(layers(had));
                state.pet.add(new MobEffectInstance(effect));
                player.addEffect(new MobEffectInstance(effect));
            }
        } finally {
            ourselves = false;
        }
        player.setData(ModAttachments.PET_BUFF.get(), state);
    }

    // Removes the pet's part of the buff, and gives back everything else of those kinds
    public static void takeOff(Player player) {
        State state = player.getExistingDataOrNull(ModAttachments.PET_BUFF.get());
        player.removeData(ModAttachments.PET_BUFF.get());
        if (state == null || !state.active()) return;
        Set<Holder<MobEffect>> kinds = new LinkedHashSet<>();
        for (MobEffectInstance effect : state.pet) kinds.add(effect.getEffect());
        ourselves = true;
        try {
            for (Holder<MobEffect> kind : kinds) {
                player.removeEffect(kind);
                for (MobEffectInstance other : state.others) {
                    if (other.is(kind)) player.addEffect(new MobEffectInstance(other));
                }
            }
        } finally {
            ourselves = false;
        }
    }

    // One effect and the weaker ones hidden under it, each on its own (their times count down together, so each keeps
    // its own time left). The hidden ones aren't public, so they're read from the saved form.
    private static List<MobEffectInstance> layers(MobEffectInstance top) {
        List<MobEffectInstance> list = new ArrayList<>();
        CompoundTag tag = (CompoundTag) MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, top).getOrThrow();
        while (tag != null) {
            boolean particles = tag.getBooleanOr("show_particles", true);
            list.add(new MobEffectInstance(top.getEffect(), tag.getIntOr("duration", 0), tag.getByteOr("amplifier", (byte) 0) & 0xFF,
                tag.getBooleanOr("ambient", false), particles, tag.getBooleanOr("show_icon", particles)));
            tag = tag.getCompound("hidden_effect").orElse(null);
        }
        return list;
    }

    // Counts the remembered parts down with the player's own effects (Post: after they've ticked)
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) tick(event.getEntity(), 1);
    }

    public static void tick(Player player, int ticks) {
        State state = player.getExistingDataOrNull(ModAttachments.PET_BUFF.get());
        if (state == null) return;
        countDown(state.pet, ticks);
        state.others.removeIf(other -> !state.tracks(other.getEffect()));
        countDown(state.others, ticks);
        if (!state.active()) player.removeData(ModAttachments.PET_BUFF.get());
    }

    private static void countDown(List<MobEffectInstance> list, int ticks) {
        list.replaceAll(e -> e.isInfiniteDuration() ? e
            : new MobEffectInstance(e.getEffect(), e.getDuration() - ticks, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
        list.removeIf(e -> !e.isInfiniteDuration() && e.getDuration() <= 0);
    }

    // Something else gave the player an effect of a kind the pet buff has: remember it, so it comes back if the pet buff
    // is taken off
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (ourselves || !(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        State state = player.getExistingDataOrNull(ModAttachments.PET_BUFF.get());
        MobEffectInstance added = event.getEffectInstance();
        if (state != null && state.tracks(added.getEffect())) state.others.addAll(layers(added));
    }

    // Something else took an effect away (milk, a cure...): that kind is gone, pet part and all
    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (ourselves || !(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        State state = player.getExistingDataOrNull(ModAttachments.PET_BUFF.get());
        if (state == null) return;
        state.pet.removeIf(e -> e.is(event.getEffect()));
        state.others.removeIf(e -> e.is(event.getEffect()));
        if (!state.active()) player.removeData(ModAttachments.PET_BUFF.get());
    }
}

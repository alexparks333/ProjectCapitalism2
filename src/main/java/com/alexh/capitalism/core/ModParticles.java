package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Our own particles (textures in textures/particle/, listed in particles/<name>.json; client side in ClientModEvents).
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Capitalism.MODID);

    // A little hand, patting, that floats up like a heart: the first pet of the day, when its owner gets its buff
    // (pets/Pets.petBuff)
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PETTING = PARTICLE_TYPES.register("petting", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}

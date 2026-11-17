package com.alexh.capitalism.boats;

// A Premium boat that sails on lava as on water (Boats.md: the Spectral Galleon, the Leviathan's Wake): the boat's
// fluid checks (NeoForge's canBoatInFluid: PremiumBoat) count lava too, it doesn't burn (its entity type is fire immune), and its
// riders have Fire Resistance while they're aboard (PremiumBoat.tick).
public interface LavaBoat {
}

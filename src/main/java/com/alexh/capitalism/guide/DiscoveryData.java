package com.alexh.capitalism.guide;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Every item a player has made at a machine at least once. In the Cook's and Bartender's Guides, recipes for things
// you haven't made yet show as black silhouettes with "???" (old Smash Bros style). Kept through death.
public class DiscoveryData {

    public static final MapCodec<DiscoveryData> CODEC = Identifier.CODEC.listOf()
        .xmap(DiscoveryData::new, data -> List.copyOf(data.items))
        .fieldOf("items");

    private final Set<Identifier> items = new LinkedHashSet<>();

    public DiscoveryData() {
    }

    private DiscoveryData(List<Identifier> items) {
        this.items.addAll(items);
    }

    // true if it's new
    public boolean add(Identifier item) {
        return this.items.add(item);
    }

    public Set<Identifier> items() {
        return this.items;
    }

    public void clear() {
        this.items.clear();
    }
}

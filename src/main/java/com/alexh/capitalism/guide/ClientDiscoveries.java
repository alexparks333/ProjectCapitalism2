package com.alexh.capitalism.guide;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The client's copy of DiscoveryData, for drawing the guides.
public final class ClientDiscoveries {

    private static final Set<Identifier> ITEMS = new HashSet<>();

    private ClientDiscoveries() {
    }

    public static void apply(List<Identifier> items, boolean replace) {
        if (replace) ITEMS.clear();
        ITEMS.addAll(items);
    }

    public static boolean known(ItemStack stack) {
        return stack.isEmpty() || foundNotMade(stack) || ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    // Things you find rather than make (nobody "discovers" them by making one): never hidden in the guides
    private static boolean foundNotMade(ItemStack stack) {
        return stack.is(ModItems.KNOWLEDGE_PIECE.get()) || stack.is(ModItems.MOLTEN_SOCKET.get());
    }
}

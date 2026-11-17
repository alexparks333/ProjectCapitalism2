package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// The Enchanter's perk: only they enchant past level 10 at an Enchanting Table (up to vanilla's 30, on anything:
// tools, weapons, armor). Everyone else's three options are capped at MAX_LEVEL (mixin/EnchantmentMenuMixin).
// Books are the Librarian's (Librarian.md): only Librarians enchant a book (up to 30, no cap); for anyone else a book
// gets no options at all, Enchanters included. Creative players aren't limited.
public final class EnchantingLimit {

    public static final int MAX_LEVEL = 10;

    private EnchantingLimit() {
    }

    public static boolean limited(@Nullable Player player) {
        return player != null && !player.isCreative() && !JobEvents.has(player, Job.ENCHANTER);
    }

    public static int cap(@Nullable Player player, int cost) {
        return limited(player) ? Math.min(cost, MAX_LEVEL) : cost;
    }

    public static boolean enchantsBooks(@Nullable Player player) {
        return player == null || player.isCreative() || JobEvents.has(player, Job.LIBRARIAN);
    }

    // One of the Enchanting Table's three costs for this player and item: 0 = no option
    public static int cost(@Nullable Player player, ItemStack stack, int cost) {
        if (stack.is(Items.BOOK)) return enchantsBooks(player) ? cost : 0;
        return cap(player, cost);
    }
}

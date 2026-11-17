package com.alexh.capitalism.client;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

// How a block or item is made, for the guides' build pages:
//  - a 3x3 crafting grid (left to right, top to bottom; keep it matching data/capitalism/recipe/),
//  - a short row of icons with a note, for things that aren't crafted,
//  - a two-slot table recipe (table: the big slot and the small slot, drawn like the smith tables / Enchanter's Table /
//    Librarian Table, with the result after an arrow),
//  - a Stitching Table recipe (ring: the ring of covers round the big Paper slot, the thread, then the result, drawn
//    like the table's own menu).
// A slot can cycle through a few items, one a second (options: e.g. a Rough Book, then a Delicate Book), and the result
// can cycle with it (results). Every slot cycles on the same clock, so the options line up with the results.
public record HowToMake(ItemStack @Nullable [] grid, List<ItemStack> steps, String note, boolean table, boolean ring,
                        List<List<ItemStack>> options, List<ItemStack> results) {

    public static HowToMake crafting(ItemStack... nine) {
        return new HowToMake(nine, List.of(), "", false, false, List.of(), List.of());
    }

    // A 3x3 grid where slots cycle through a few items (e.g. charcoal, then prismarine shards); one list per slot
    @SafeVarargs
    public static HowToMake crafting(List<ItemStack>... nine) {
        ItemStack[] first = new ItemStack[9];
        for (int i = 0; i < 9; i++) first[i] = nine[i].isEmpty() ? ItemStack.EMPTY : nine[i].getFirst();
        return new HowToMake(first, List.of(), "", false, false, List.of(nine), List.of());
    }

    // What grid slot i shows right now
    public ItemStack gridAt(int i, long cycle) {
        if (this.grid == null) return ItemStack.EMPTY;
        if (this.options.size() != 9 || this.options.get(i).isEmpty()) return this.grid[i];
        List<ItemStack> choices = this.options.get(i);
        return choices.get((int) (cycle % choices.size()));
    }

    public static HowToMake steps(String note, ItemStack... icons) {
        return new HowToMake(null, List.of(icons), note, false, false, List.of(), List.of());
    }

    // Big slot + small slot at a table (the note says which table)
    public static HowToMake table(String note, ItemStack big, ItemStack small) {
        return new HowToMake(null, List.of(big, small), note, true, false, List.of(), List.of());
    }

    // Big slot + small slot at a table, where the slots (and the result) cycle through a few items
    public static HowToMake table(String note, List<ItemStack> bigs, List<ItemStack> smalls, List<ItemStack> results) {
        return new HowToMake(null, List.of(bigs.getFirst(), smalls.getFirst()), note, true, false, List.of(bigs, smalls), results);
    }

    // The Stitching Table: a cover in every ring slot, Paper in the middle, the thread (cycling through threads, with the
    // book each one makes as the result)
    public static HowToMake stitching(String note, List<ItemStack> covers, ItemStack paper, List<ItemStack> threads, List<ItemStack> results) {
        return new HowToMake(null, List.of(covers.getFirst(), paper, threads.getFirst()), note, false, true,
            List.of(covers, List.of(paper), threads), results);
    }

    // What step i shows right now (cycle = seconds on the clock)
    public ItemStack shown(int i, long cycle) {
        if (i >= this.options.size()) return this.steps.get(i);
        List<ItemStack> choices = this.options.get(i);
        return choices.get((int) (cycle % choices.size()));
    }

    // The result right now, or null to show the entry's own icon
    public @Nullable ItemStack result(long cycle) {
        return this.results.isEmpty() ? null : this.results.get((int) (cycle % this.results.size()));
    }
}

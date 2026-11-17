package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.economy.NpcPricing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// What the Fisherman (town/Fisherman, Fishing.md > The Fisherman) pays and charges. He buys only the rare catches and
// Soggy Notes, nothing else (the Cook buys ordinary fish).
//  - A catch: its RareCatch.gold for one of middling weight, from 60% for the lightest to 140% for the heaviest.
//  - A Soggy Note: the gold written on it.
//  - Mounting a catch on a wall plaque: 3 gold a block of its width plus a tenth of what the catch would sell for (the
//    fair price), and he's an NPC, so it goes through NpcPricing (his markup, Emerald armour, festival sales).
// All in gold nuggets (9 to a gold).
public final class FishMarket {

    public static final int PLAQUE_GOLD_PER_BLOCK = 3;

    private FishMarket() {
    }

    public static RareCatch catchOf(ItemStack stack) {
        return stack.getItem() instanceof RareCatchItem item ? item.kind() : null;
    }

    // how heavy it is for its kind, 0 (lightest) to 1 (heaviest); 0.5 if it has no weight on it
    public static double heft(RareCatch kind, ItemStack stack) {
        CatchInfo info = stack.get(ModComponents.CATCH_INFO.get());
        if (info == null || !kind.weighed() || kind.maxLb <= kind.minLb) return 0.5;
        return Math.max(0.0, Math.min(1.0, (info.weight() - kind.minLb) / (kind.maxLb - kind.minLb)));
    }

    // what he pays for one of these (nuggets), or 0 if he doesn't buy it
    public static long valueOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ModItems.SOGGY_NOTE.get())) {
            Integer gold = stack.get(ModComponents.NOTE_GOLD.get());
            return gold == null ? 0 : gold;
        }
        RareCatch kind = catchOf(stack);
        if (kind == null || kind.gold <= 0) return 0;
        return Math.round(kind.gold * 9.0 * (0.6 + 0.8 * heft(kind, stack)));
    }

    public static boolean buys(ItemStack stack) {
        return valueOf(stack) > 0;
    }

    public static boolean mountable(ItemStack stack) {
        RareCatch kind = catchOf(stack);
        return kind != null && kind.plaque > 0;
    }

    // the plaque's fair price (nuggets), before NpcPricing
    public static long plaqueFair(ItemStack stack) {
        RareCatch kind = catchOf(stack);
        if (kind == null || kind.plaque <= 0) return 0;
        long catchValue = Math.round(kind.gold * 9.0 * (0.6 + 0.8 * heft(kind, stack)));
        return PLAQUE_GOLD_PER_BLOCK * 9L * kind.plaque + catchValue / 10;
    }

    // what `buyer` pays him to mount it
    public static long plaquePrice(ItemStack stack, Player buyer) {
        long fair = plaqueFair(stack);
        return fair <= 0 ? 0 : NpcPricing.npcPrice(fair, buyer);
    }

    // the plaque made from a catch: it keeps what the catch was, its weight, catcher and day
    public static ItemStack plaqueFor(ItemStack stack) {
        RareCatch kind = catchOf(stack);
        if (kind == null || kind.plaque <= 0) return ItemStack.EMPTY;
        ItemStack plaque = new ItemStack(ModItems.FISH_PLAQUE.get());
        plaque.set(ModComponents.PLAQUE_CATCH.get(), kind.id);
        CatchInfo info = stack.get(ModComponents.CATCH_INFO.get());
        if (info != null) plaque.set(ModComponents.CATCH_INFO.get(), info);
        return plaque;
    }
}

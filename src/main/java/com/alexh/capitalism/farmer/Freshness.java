package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// Meat that rots (Farmer.md > Freshness): Meat Slabs and the fine cuts (MeatCut). Each one carries the game tick it
// rots at (ModComponents.ROTS_AT) and turns into Rotten Meat when that comes. A slab's shelf life goes by its rarity
// (SHELF_LIFE: 15 / 20 / 25 minutes; chicken 10). Cutting a slab up gives the cuts the time it had left plus a quarter
// (CUT_BONUS), all on one timer, so a slab's cuts stack together but not with another slab's. A slab also rolls how
// lean it is (ModComponents.LEAN, 60-100%): under 85% it gives one less meat, under 70% two less. Fridges and
// Freezers (ColdStorageBlockEntity) push the time back while they run. Plain (botched) meat doesn't rot.
// Times are game ticks (Level.getGameTime: they stop when the server does). Checked every second where the items are:
// players' inventories and the containers they have open, dropped items, cold storage, and placed slabs
// (FreshnessEvents, MeatSlabBlockEntity); anything else is caught the moment someone opens it.
public final class Freshness {

    public static final int MINUTE = 20 * 60;
    public static final float CUT_BONUS = 1.25F;
    public static final int LEAN_MIN = 60;
    public static final int LEAN_MAX = 100;
    // Dry-aging: a fine cut kept in a running Fridge (not a Freezer) ages: Dry-Aged after DRY_AGED of it, Prime
    // Dry-Aged after PRIME_AGED (ModComponents.AGING, ticks aged; it keeps rotting the whole time, just slower)
    public static final int DRY_AGED = 8 * MINUTE;
    public static final int PRIME_AGED = 16 * MINUTE;

    // The game time on the client, for drawing the freshness bars (MeatItems); set by the client (ClientModEvents),
    // -1 when there's no world
    public static java.util.function.LongSupplier clientClock = () -> -1L;

    private Freshness() {
    }

    // The most time a piece of this can have: a slab's shelf life, a cut's plus a quarter
    public static long fullLife(ItemStack stack) {
        MeatCut cut = cutOf(stack);
        if (cut == null) return 1;
        return MeatCut.ofSlab(stack) != null ? shelfLife(cut) : Math.round(shelfLife(cut) * (double) CUT_BONUS);
    }

    // How long a fresh slab of this cut lasts, in ticks
    public static int shelfLife(MeatCut cut) {
        if (cut.animal == MeatCut.Animal.CHICKEN) return 10 * MINUTE;
        return switch (cut.rarity) {
            case 0 -> 15 * MINUTE;
            case 1 -> 20 * MINUTE;
            default -> 25 * MINUTE;
        };
    }

    public static boolean rots(ItemStack stack) {
        return MeatCut.ofSlab(stack) != null || MeatCut.isCut(stack);
    }

    private static @Nullable MeatCut cutOf(ItemStack stack) {
        MeatCut slab = MeatCut.ofSlab(stack);
        if (slab != null) return slab;
        for (MeatCut cut : MeatCut.values()) {
            if (stack.is(cut.cut())) return cut;
        }
        return null;
    }

    // ── lean ──

    public static int rollLean(RandomSource random) {
        return LEAN_MIN + random.nextInt(LEAN_MAX - LEAN_MIN + 1);
    }

    public static int lean(ItemStack slab) {
        return slab.getOrDefault(ModComponents.LEAN.get(), LEAN_MAX);
    }

    // Meat lost to fat: one under 85% lean, two under 70%
    public static int leanPenalty(int lean) {
        return lean < 70 ? 2 : lean < 85 ? 1 : 0;
    }

    // What this slab gives when it's cut up (clean or botched)
    public static int yield(MeatCut cut, ItemStack slab) {
        return Math.max(1, cut.yield - leanPenalty(lean(slab)));
    }

    // ── time ──

    // A fresh slab of this cut, dropped now
    public static ItemStack newSlab(MeatCut cut, long now, RandomSource random) {
        ItemStack slab = new ItemStack(cut.slab());
        slab.set(ModComponents.ROTS_AT.get(), now + shelfLife(cut));
        slab.set(ModComponents.LEAN.get(), rollLean(random));
        return slab;
    }

    // Gives something that rots its timer if it hasn't got one (one from the creative menu, say): true if it did
    public static boolean stamp(ItemStack stack, long now, RandomSource random) {
        if (stack.isEmpty() || !rots(stack) || stack.has(ModComponents.ROTS_AT.get())) return false;
        MeatCut cut = cutOf(stack);
        if (cut == null) return false;
        boolean slab = MeatCut.ofSlab(stack) != null;
        stack.set(ModComponents.ROTS_AT.get(), now + (slab ? shelfLife(cut) : Math.round(shelfLife(cut) * CUT_BONUS)));
        if (slab && !stack.has(ModComponents.LEAN.get())) stack.set(ModComponents.LEAN.get(), rollLean(random));
        return true;
    }

    // The tick it rots at, or -1 if it has no timer (yet)
    public static long rotsAt(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ROTS_AT.get(), -1L);
    }

    public static long ticksLeft(ItemStack stack, long now) {
        long at = rotsAt(stack);
        return at < 0 ? -1 : Math.max(0, at - now);
    }

    public static boolean isRotten(ItemStack stack, long now) {
        long at = rotsAt(stack);
        return at >= 0 && rots(stack) && now >= at;
    }

    // The cuts from a slab cut up now that had this long left: that plus a quarter, on one timer
    public static long cutRotsAt(long now, long slabLeft) {
        return now + Math.round(slabLeft * (double) CUT_BONUS);
    }

    // What it turns into: Rotten Meat, as much as the cuts would have been (a whole slab: its cut's yield)
    public static ItemStack rotted(ItemStack stack) {
        MeatCut slab = MeatCut.ofSlab(stack);
        int count = slab != null ? slab.yield * stack.getCount() : stack.getCount();
        return new ItemStack(ModItems.ROTTEN_MEAT.get(), Math.min(count, 64));
    }

    // Brings one stack up to date: its timer if it had none, or Rotten Meat if its time is up. Returns the stack to
    // put back (the same one if nothing but the timer changed), or null if nothing changed at all
    public static @Nullable ItemStack update(ItemStack stack, long now, RandomSource random) {
        if (stack.isEmpty() || !rots(stack)) return null;
        if (stamp(stack, now, random)) return stack;
        return isRotten(stack, now) ? rotted(stack) : null;
    }

    public static int aging(ItemStack stack) {
        return stack.getOrDefault(ModComponents.AGING.get(), 0);
    }

    // 0 not aged, 1 Dry-Aged, 2 Prime Dry-Aged
    public static int agedLevel(ItemStack stack) {
        int aging = aging(stack);
        return aging >= PRIME_AGED ? 2 : aging >= DRY_AGED ? 1 : 0;
    }

    public static boolean canAge(ItemStack stack) {
        return stack.getItem() instanceof MeatItems.Cut;
    }

    // "12:34" (minutes and seconds) of so many ticks
    public static String clock(long ticks) {
        long seconds = Math.max(0, ticks) / 20;
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
}

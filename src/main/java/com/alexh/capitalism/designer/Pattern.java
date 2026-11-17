package com.alexh.capitalism.designer;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

// The Patches the Sewing Machine sews (Designer.md > Sewing Machine), in three tiers (the machine's tabs): Generic
// (easy), Fancy (medium) and Luxury (hard, and only with Diamond Sewing Needles). A pattern is a few materials, sewn on
// one at a time with the needle game (client/SewingGame): how hard the seam is comes from the tier, how it winds from
// the material (Style). New patterns go at the END (the saved pattern and the machine's buttons count in this order).
public enum Pattern {
    // ── Generic ──
    LEATHER_PATCH(Tier.GENERIC, ModItems.LEATHER_PATCH, () -> Items.LEATHER, () -> Items.LEATHER, () -> Items.STRING),
    STRAP_PATCH(Tier.GENERIC, ModItems.STRAP_PATCH, () -> Items.LEATHER, () -> Items.STRING, () -> Items.IRON_NUGGET),
    PANTRY_PATCH(Tier.GENERIC, ModItems.PANTRY_PATCH, () -> Items.LEATHER, () -> Items.WHEAT, () -> Items.HONEYCOMB, () -> Items.STRING),
    SEED_POCKET_PATCH(Tier.GENERIC, ModItems.SEED_POCKET_PATCH, () -> Items.LEATHER, () -> Items.WHEAT_SEEDS, () -> Items.BONE_MEAL, () -> Items.STRING),
    // ── Fancy ──
    VELVET_PATCH(Tier.FANCY, ModItems.VELVET_PATCH, AnyWool.ANY, () -> Items.LEATHER, () -> Items.STRING),
    EMBROIDERED_PATCH(Tier.FANCY, ModItems.EMBROIDERED_PATCH, () -> Items.LEATHER, () -> Items.STRING, () -> Items.STRING, () -> Items.GOLD_NUGGET),
    GOLD_TRIM_PATCH(Tier.FANCY, ModItems.GOLD_TRIM_PATCH, () -> Items.LEATHER, () -> Items.GOLD_INGOT, () -> Items.STRING),
    SILK_PATCH(Tier.FANCY, ModItems.SILK_PATCH, () -> Items.STRING, () -> Items.STRING, AnyWool.ANY, () -> Items.STRING),
    // ── Luxury ──
    GUCCI_PATCH(Tier.LUXURY, ModItems.GUCCI_PATCH, () -> Items.LEATHER, () -> Items.WOOL.green(), () -> Items.WOOL.red(), () -> ModItems.COMPACTED_DIAMOND.get()),
    BURBERRY_PATCH(Tier.LUXURY, ModItems.BURBERRY_PATCH, () -> Items.EMERALD, () -> Items.WOOL.brown(), () -> Items.WOOL.black(), () -> Items.EMERALD),
    LOUIS_VUITTON_PATCH(Tier.LUXURY, ModItems.LOUIS_VUITTON_PATCH, () -> Items.EMERALD, () -> Items.WOOL.brown(), () -> Items.GOLD_INGOT, () -> Items.EMERALD),
    PRADA_PATCH(Tier.LUXURY, ModItems.PRADA_PATCH, () -> Items.LEATHER, () -> Items.WOOL.black(), () -> ModItems.COMPACTED_IRON.get(), () -> ModItems.COMPACTED_DIAMOND.get()),
    // ── Utility ── (patches that do something: the Utility bags)
    FROZEN_PATCH(Tier.UTILITY, ModItems.FROZEN_PATCH, () -> Items.WOOL.blue(), () -> Items.ICE, () -> Items.SNOWBALL, () -> Items.IRON_INGOT);

    // the most materials a pattern has (the machine's row of material slots)
    public static final int MAX_STITCHES = 4;

    // How hard a tier's seams are (0 easy .. 1 hard: narrower, faster, longer)
    public enum Tier {
        GENERIC("Generic", 0.2F, 1.25F, 1.0F),
        FANCY("Fancy", 0.55F, 1.2F, 1.0F),
        LUXURY("Luxury", 0.9F, 1.12F, 1.12F),
        // medium-hard: between Fancy and Luxury
        UTILITY("Utility", 0.72F, 1.15F, 1.05F);

        public final String title;
        public final float difficulty;
        // how fast the cloth feeds, on top of the difficulty (Generic seams run quicker so they aren't dull, Fancy and Luxury a tad quicker)
        public final float speed;
        // how wide the seam is, on top of the difficulty (Luxury gets a touch more room so it's hard but fair)
        public final float width;

        Tier(String title, float difficulty, float speed, float width) {
            this.title = title;
            this.difficulty = difficulty;
            this.speed = speed;
            this.width = width;
        }
    }

    // How a material's seam winds (client/SewingGame): string sways wide and steady, leather wanders, metal runs
    // straight then turns sharply, gems are narrow and jittery, ice cracks in sharp zig-zags and freezes narrow in places
    public enum Style {
        WAVY, WANDERING, RIGID, JITTERY, ICY;

        public static Style of(Item item) {
            ItemStack stack = new ItemStack(item);
            if (item == Items.STRING || stack.is(net.minecraft.tags.ItemTags.WOOL)) return WAVY;
            if (item == Items.IRON_NUGGET || item == Items.IRON_INGOT || item == Items.GOLD_NUGGET || item == Items.GOLD_INGOT
                || item == ModItems.COMPACTED_IRON.get()) return RIGID;
            if (item == Items.DIAMOND || item == Items.EMERALD || item == ModItems.COMPACTED_DIAMOND.get()) return JITTERY;
            if (item == Items.ICE || item == Items.PACKED_ICE || item == Items.BLUE_ICE || item == Items.SNOWBALL || item == Items.SNOW_BLOCK) return ICY;
            return WANDERING;
        }
    }

    // "Any color of wool" in a pattern (the Fancy patches); shown as White Wool
    public static final class AnyWool {
        public static final Supplier<Item> ANY = () -> Items.WOOL.white();
    }

    // One material (anyWool: any color of wool will do)
    public record Stitch(Supplier<Item> item, boolean anyWool) {
        public ItemStack stack() {
            return new ItemStack(this.item.get());
        }

        public boolean matches(ItemStack stack) {
            if (this.anyWool) return stack.is(net.minecraft.tags.ItemTags.WOOL);
            // any ice will do where a pattern asks for Ice (Packed and Blue Ice too)
            if (this.item.get() == Items.ICE) return stack.is(Items.ICE) || stack.is(Items.PACKED_ICE) || stack.is(Items.BLUE_ICE);
            return stack.is(this.item.get());
        }

        public String name() {
            if (this.item.get() == Items.ICE) return "Ice (any)";
            return this.anyWool ? "Wool (any color)" : this.stack().getHoverName().getString();
        }

        public Style style() {
            return Style.of(this.item.get());
        }
    }

    public final Tier tier;
    private final Supplier<? extends Item> patch;
    public final List<Stitch> stitches;

    @SafeVarargs
    Pattern(Tier tier, Supplier<? extends Item> patch, Supplier<Item>... materials) {
        this.tier = tier;
        this.patch = patch;
        this.stitches = Arrays.stream(materials).map(m -> new Stitch(m, m == AnyWool.ANY)).toList();
    }

    public Item patch() {
        return this.patch.get();
    }

    // Generic patches are sewn with Iron Sewing Needles, Fancy and Luxury ones with Diamond needles (only that kind)
    public Item needle() {
        return this.tier == Tier.GENERIC ? ModItems.IRON_SEWING_NEEDLE.get() : ModItems.DIAMOND_SEWING_NEEDLE.get();
    }

    public boolean takesNeedle(ItemStack needle) {
        return needle.is(this.needle());
    }

    public static List<Pattern> inTier(Tier tier) {
        return Arrays.stream(values()).filter(p -> p.tier == tier).toList();
    }

    public static Pattern byIndex(int index) {
        Pattern[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}

package com.alexh.capitalism.designer;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

// The Designer's bags (Designer.md > Bags): worn on the back (back/Back), each adds a few slots (BagSlot, shown in a
// panel next to the inventory), in groups that each take their own kind of thing (Holds). Made at the Designer Table from
// Leather and a row of Patches (up to 6). New bags go at the END (the Designer Table counts its choices in this order,
// Designs).
public enum Bag implements Design {
    // ── Standard ──
    BACKPACK("Backpack", Line.STANDARD, ModItems.BACKPACK, List.of(group(2, Holds.ANYTHING)),
        List.of(ModItems.LEATHER_PATCH, ModItems.LEATHER_PATCH, ModItems.STRAP_PATCH, ModItems.STRAP_PATCH)),
    FOOD_SATCHEL("Food Satchel", Line.STANDARD, ModItems.FOOD_SATCHEL, List.of(group(4, Holds.FOOD)),
        List.of(ModItems.LEATHER_PATCH, ModItems.LEATHER_PATCH, ModItems.STRAP_PATCH, ModItems.PANTRY_PATCH, ModItems.PANTRY_PATCH)),
    SEED_SATCHEL("Seed Satchel", Line.STANDARD, ModItems.SEED_SATCHEL, List.of(group(10, Holds.SEEDS)),
        List.of(ModItems.LEATHER_PATCH, ModItems.LEATHER_PATCH, ModItems.STRAP_PATCH, ModItems.STRAP_PATCH,
            ModItems.SEED_POCKET_PATCH, ModItems.SEED_POCKET_PATCH)),
    // ── Fancy ──
    FANCY_BACKPACK("Fancy Backpack", Line.FANCY, ModItems.FANCY_BACKPACK, List.of(group(3, Holds.ANYTHING)),
        List.of(ModItems.VELVET_PATCH, ModItems.VELVET_PATCH, ModItems.STRAP_PATCH, ModItems.STRAP_PATCH, ModItems.EMBROIDERED_PATCH)),
    FANCY_FOOD_SATCHEL("Fancy Food Satchel", Line.FANCY, ModItems.FANCY_FOOD_SATCHEL, List.of(group(6, Holds.FOOD_OR_WATER)),
        List.of(ModItems.VELVET_PATCH, ModItems.SILK_PATCH, ModItems.PANTRY_PATCH, ModItems.PANTRY_PATCH, ModItems.GOLD_TRIM_PATCH)),
    FANCY_SEED_SATCHEL("Fancy Seed Satchel", Line.FANCY, ModItems.FANCY_SEED_SATCHEL, List.of(group(15, Holds.SEEDS_OR_FLOWERS)),
        List.of(ModItems.SILK_PATCH, ModItems.SILK_PATCH, ModItems.SEED_POCKET_PATCH, ModItems.SEED_POCKET_PATCH,
            ModItems.EMBROIDERED_PATCH, ModItems.GOLD_TRIM_PATCH)),
    UNIFORM_BACKPACK("Uniform Backpack", Line.FANCY, ModItems.UNIFORM_BACKPACK, List.of(group(2, Holds.ANYTHING), group(2, Holds.FOOD_OR_WATER)),
        List.of(ModItems.EMBROIDERED_PATCH, ModItems.EMBROIDERED_PATCH, ModItems.STRAP_PATCH, ModItems.PANTRY_PATCH, ModItems.GOLD_TRIM_PATCH)),
    // ── Luxury ──
    GUCCI_BAG("Gucci Bag", Line.LUXURY, ModItems.GUCCI_BAG, List.of(group(8, Holds.ANYTHING)),
        List.of(ModItems.GUCCI_PATCH, ModItems.GUCCI_PATCH, ModItems.GUCCI_PATCH, ModItems.GOLD_TRIM_PATCH, ModItems.STRAP_PATCH, ModItems.SILK_PATCH)),
    BURBERRY_BAG("Burberry Bag", Line.LUXURY, ModItems.BURBERRY_BAG, List.of(group(8, Holds.ANYTHING), group(3, Holds.FOOD_OR_WATER)),
        List.of(ModItems.BURBERRY_PATCH, ModItems.BURBERRY_PATCH, ModItems.BURBERRY_PATCH, ModItems.GOLD_TRIM_PATCH, ModItems.STRAP_PATCH, ModItems.PANTRY_PATCH)),
    LOUIS_VUITTON_BAG("Louis Vuitton Bag", Line.LUXURY, ModItems.LOUIS_VUITTON_BAG,
        List.of(group(4, Holds.FOOD_OR_WATER), group(6, Holds.ANYTHING), group(4, Holds.WEAPON_OR_TOOL)),
        List.of(ModItems.LOUIS_VUITTON_PATCH, ModItems.LOUIS_VUITTON_PATCH, ModItems.LOUIS_VUITTON_PATCH, ModItems.GOLD_TRIM_PATCH,
            ModItems.STRAP_PATCH, ModItems.EMBROIDERED_PATCH)),
    PRADA_BAG("Prada Bag", Line.LUXURY, ModItems.PRADA_BAG, List.of(group(10, Holds.GEMS_OR_ORES), group(2, Holds.ANYTHING), group(2, Holds.FOOD)),
        List.of(ModItems.PRADA_PATCH, ModItems.PRADA_PATCH, ModItems.PRADA_PATCH, ModItems.GOLD_TRIM_PATCH, ModItems.STRAP_PATCH, ModItems.VELVET_PATCH)),
    // ── Utility ── (bags that do something)
    // keeps food like a Freezer: meat in it rots 6x slower, no fuel needed (farmer/FreshnessEvents)
    FREEZER_SLING("Freezer Sling", Line.UTILITY, ModItems.FREEZER_SLING, List.of(group(10, Holds.FOOD_OR_MEAT)),
        List.of(ModItems.SILK_PATCH, ModItems.FROZEN_PATCH, ModItems.FROZEN_PATCH, ModItems.SILK_PATCH)),
    // the Traveler's Bag: NOT a Designer's design (only the Traveling Merchant sells it, so it's left out of Design.all):
    // 6 slots of anything, opening out to 12 far from town (designer/TravelersBag)
    TRAVELERS_BAG("Traveler's Bag", Line.UTILITY, ModItems.TRAVELERS_BAG, List.of(group(TravelersBag.FAR_SLOTS, Holds.ANYTHING)), List.of());

    // How many times slower meat rots in a bag (the Freezer Sling: like a Freezer), 1 for an ordinary bag
    public int slower() {
        return this == FREEZER_SLING ? com.alexh.capitalism.farmer.ColdStorageBlockEntity.Kind.FREEZER.slower : 1;
    }

    // the most slots any bag has (the inventory always has this many bag slots; a bag uses the first `slots` of them)
    public static final int MAX_SLOTS = 16;
    // the most patches a design takes (the Designer Table's row)
    public static final int MAX_PATCHES = 6;

    // (in a class of their own: an enum's constants can't use its static fields)
    public static final class Tags {
        public static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Capitalism.MODID, "seeds"));
    }

    // What a group of slots takes, its name (the slots' tooltip) and the faint picture in its empty slots
    public enum Holds {
        ANYTHING("Anything", stack -> true, () -> ItemStack.EMPTY),
        FOOD("Food", stack -> stack.has(DataComponents.FOOD), () -> new ItemStack(Items.APPLE)),
        FOOD_OR_WATER("Food or water", stack -> stack.has(DataComponents.FOOD) || stack.has(DataComponents.CONSUMABLE), () -> new ItemStack(Items.BREAD)),
        SEEDS("Seeds", stack -> stack.is(Tags.SEEDS), () -> new ItemStack(Items.WHEAT_SEEDS)),
        SEEDS_OR_FLOWERS("Seeds or flowers", stack -> stack.is(Tags.SEEDS) || stack.is(net.minecraft.tags.BlockItemTags.FLOWERS.item()), () -> new ItemStack(Items.POPPY)),
        WEAPON_OR_TOOL("Weapon or tool", stack -> stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON)
            || stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT) || stack.is(Items.MACE) || stack.is(Items.SHEARS)
            || stack.is(Items.FISHING_ROD) || stack.is(Items.FLINT_AND_STEEL), () -> new ItemStack(Items.IRON_SWORD)),
        GEMS_OR_ORES("Gems or ores", stack -> stack.is(net.neoforged.neoforge.common.Tags.Items.GEMS) || stack.is(net.neoforged.neoforge.common.Tags.Items.ORES)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS) || stack.is(net.neoforged.neoforge.common.Tags.Items.INGOTS)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.NUGGETS) || stack.is(Items.COAL) || stack.is(Items.CHARCOAL)
            || stack.is(Items.REDSTONE) || stack.is(Items.NETHERITE_SCRAP), () -> new ItemStack(Items.DIAMOND)),
        // food, and meat that rots (Meat Slabs too: they aren't eaten as they are)
        FOOD_OR_MEAT("Food or meat", stack -> stack.has(DataComponents.FOOD) || com.alexh.capitalism.farmer.Freshness.rots(stack), () -> new ItemStack(Items.BEEF));

        public final String title;
        private final Predicate<ItemStack> test;
        private final Supplier<ItemStack> hint;

        Holds(String title, Predicate<ItemStack> test, Supplier<ItemStack> hint) {
            this.title = title;
            this.test = test;
            this.hint = hint;
        }

        public ItemStack hint() {
            return this.hint.get();
        }
    }

    // A few slots that all take the same kind of thing
    public record Group(int count, Holds holds) {
    }

    private static Group group(int count, Holds holds) {
        return new Group(count, holds);
    }

    public final String title;
    public final Line line;
    public final int slots;
    public final List<Group> groups;
    private final Supplier<? extends Item> item;
    private final List<Supplier<? extends Item>> patches;

    Bag(String title, Line line, Supplier<? extends Item> item, List<Group> groups, List<Supplier<? extends Item>> patches) {
        this.title = title;
        this.line = line;
        this.item = item;
        this.groups = groups;
        this.slots = groups.stream().mapToInt(Group::count).sum();
        this.patches = patches;
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Line line() {
        return this.line;
    }

    @Override
    public Item item() {
        return this.item.get();
    }

    // The patches it's made of, in the order they go in the Designer Table's row
    @Override
    public List<Item> patches() {
        return this.patches.stream().map(p -> (Item) p.get()).toList();
    }

    // How many slots it has right now: the Traveler's Bag only opens out to all of them far from town (TravelersBag)
    public int slots(ItemStack bag) {
        return this == TRAVELERS_BAG && !TravelersBag.expanded(bag) ? TravelersBag.NEAR_SLOTS : this.slots;
    }

    // What this slot of the bag takes (null past its last slot)
    public @Nullable Holds holds(int slot) {
        int at = 0;
        for (Group group : this.groups) {
            if (slot < at + group.count()) return group.holds();
            at += group.count();
        }
        return null;
    }

    // Short description of what it holds: "Anything", or for mixed bags "8 anything, 3 food or water"
    public String holdsText() {
        if (this.groups.size() == 1) return this.groups.getFirst().holds().title;
        List<String> parts = new ArrayList<>();
        for (Group group : this.groups) parts.add(group.count() + " " + group.holds().title.toLowerCase(java.util.Locale.ROOT));
        return String.join(", ", parts);
    }

    // Can this go in that slot of the bag? Never another bag, a shulker box or a bundle (no bags in bags for endless space)
    public boolean fits(int slot, ItemStack stack) {
        Holds holds = this.holds(slot);
        if (holds == null || stack.isEmpty() || of(stack) != null || stack.is(ItemTags.SHULKER_BOXES) || stack.is(ItemTags.BUNDLES)
            || stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.BUNDLE_CONTENTS)) return false;
        return holds.test.test(stack);
    }

    // Does it fit anywhere in the bag (shift-clicking into it)?
    public boolean fitsAnywhere(ItemStack stack) {
        for (int i = 0; i < this.slots; i++) {
            if (this.fits(i, stack)) return true;
        }
        return false;
    }

    public static @Nullable Bag of(ItemStack stack) {
        for (Bag bag : values()) {
            if (stack.is(bag.item())) return bag;
        }
        return null;
    }
}

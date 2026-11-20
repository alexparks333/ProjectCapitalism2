package com.alexh.capitalism.block;

import com.alexh.capitalism.occupation.CookMadeFood;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Recipes for the Cook's three stations:
//  - Sushi Table: up to 4 ingredients -> sushi, rolled with the ROLL button. Cooked Rice always goes in the FIRST
//    slot, and Dried Kelp (nori) in the SECOND slot when the roll has it; the rest can go in any order.
//  - Cooking Table: up to 5 ingredients -> an uncooked dish, made on the spot. Slots go clockwise from the top
//    middle, and dishes use exact slots (ordered = 5).
//  - Oven: one uncooked dish -> a Gourmet meal after `cookSeconds` (with a soft ding). Left in the hot Oven for
//    `burnSeconds` more, it turns into Burnt Food. Burning takes about half the cook time.
// Every recipe is listed in Cook.md. Sushi food values and diet groups are in ModItems / DietValues.
public final class KitchenRecipes {

    // The first `ordered` ingredients must be in the first slots, in that order; the rest can be anywhere in the
    // remaining slots. ordered = 0 means fully shapeless.
    public record Assembly(Supplier<? extends Item> result, int count, int ordered, List<Supplier<? extends Item>> ingredients) {

        @SafeVarargs
        public Assembly(Supplier<? extends Item> result, int count, int ordered, Supplier<? extends Item>... ingredients) {
            this(result, count, ordered, List.of(ingredients));
        }

        public boolean matches(Container slots, int slotCount) {
            for (int i = 0; i < this.ordered; i++) {
                if (!slots.getItem(i).is(this.ingredients.get(i).get())) return false;
            }
            List<Item> needed = new ArrayList<>();
            for (int i = this.ordered; i < this.ingredients.size(); i++) needed.add(this.ingredients.get(i).get());
            for (int i = this.ordered; i < slotCount; i++) {
                ItemStack stack = slots.getItem(i);
                if (stack.isEmpty()) continue;
                if (!needed.remove(stack.getItem())) return false;
            }
            return needed.isEmpty();
        }
    }

    public record Baking(Supplier<? extends Item> input, Supplier<? extends Item> result, int cookSeconds, int burnSeconds) {

        public int cookTicks() {
            return this.cookSeconds * 20;
        }

        public int burnTicks() {
            return this.burnSeconds * 20;
        }
    }

    public static final List<Assembly> SUSHI = new ArrayList<>();
    public static final List<Assembly> COOKING = new ArrayList<>();
    public static final List<Baking> OVEN = new ArrayList<>();

    static {
        Supplier<Item> rice = ModItems.COOKED_RICE::get;
        Supplier<Item> kelp = () -> Items.DRIED_KELP;

        // ── Sushi (Cooked Rice first, Dried Kelp second) ──
        SUSHI.add(new Assembly(ModItems.SALMON_NIGIRI, 1, 1, rice, () -> Items.SALMON));
        SUSHI.add(new Assembly(ModItems.COD_NIGIRI, 1, 1, rice, () -> Items.COD));
        SUSHI.add(new Assembly(ModItems.TROPICAL_NIGIRI, 1, 1, rice, () -> Items.TROPICAL_FISH));
        SUSHI.add(new Assembly(ModItems.PUFFER_NIGIRI, 1, 1, rice, () -> Items.PUFFERFISH));
        SUSHI.add(new Assembly(ModItems.COD_ROLL, 1, 2, rice, kelp, () -> Items.COD));
        SUSHI.add(new Assembly(ModItems.KELP_ROLL, 1, 2, rice, kelp, () -> Items.CARROT));
        SUSHI.add(new Assembly(ModItems.TROPICAL_ROLL, 1, 2, rice, kelp, () -> Items.TROPICAL_FISH, () -> Items.SWEET_BERRIES));
        SUSHI.add(new Assembly(ModItems.RAINBOW_ROLL, 1, 2, rice, kelp, () -> Items.SALMON, () -> Items.COD));
        SUSHI.add(new Assembly(ModItems.PUFFER_ROLL, 1, 2, rice, kelp, () -> Items.PUFFERFISH, () -> Items.CARROT));
        SUSHI.add(new Assembly(ModItems.VEGETABLE_ROLL, 1, 2, rice, kelp, () -> Items.BROWN_MUSHROOM, () -> Items.CARROT));
        SUSHI.add(new Assembly(ModItems.BAKED_SALMON_ROLL, 1, 2, rice, kelp, () -> Items.COOKED_SALMON, () -> Items.COOKED_SALMON));
        SUSHI.add(new Assembly(ModItems.FRUIT_ROLL, 1, 2, rice, kelp, () -> Items.SWEET_BERRIES, () -> Items.MELON_SLICE));
        SUSHI.add(new Assembly(ModItems.STEAK_ROLL, 1, 2, rice, kelp, () -> Items.COOKED_BEEF, () -> Items.COOKED_BEEF));
        SUSHI.add(new Assembly(ModItems.POTATO_ROLL, 1, 2, rice, kelp, () -> Items.BAKED_POTATO, () -> Items.BAKED_POTATO));
        SUSHI.add(new Assembly(ModItems.SWEET_BREAD_ROLL, 1, 1, rice, () -> Items.COCOA_BEANS, () -> Items.BREAD, () -> Items.BREAD));
        SUSHI.add(new Assembly(ModItems.OCEAN_SOVEREIGN_ROLL, 1, 2, rice, kelp, () -> Items.TURTLE_SCUTE, () -> Items.TROPICAL_FISH));

        // ── Cooking Table (clockwise from the top middle) -> Oven ──
        COOKING.add(new Assembly(ModItems.UNCOOKED_CHICKEN_POT_PIE, 1, 5,
            () -> Items.CHICKEN, () -> Items.CHICKEN, () -> Items.CARROT, () -> Items.BREAD, () -> Items.POTATO));
        OVEN.add(new Baking(ModItems.UNCOOKED_CHICKEN_POT_PIE, ModItems.CHICKEN_POT_PIE, 15, 8));

        // Milk leaves its bucket behind in the slot.
        COOKING.add(new Assembly(ModItems.UNCOOKED_LOADED_BAKED_POTATO, 1, 3, () -> Items.POTATO, () -> Items.BEEF, () -> Items.MILK_BUCKET));
        OVEN.add(new Baking(ModItems.UNCOOKED_LOADED_BAKED_POTATO, ModItems.LOADED_BAKED_POTATO, 12, 6));

        COOKING.add(new Assembly(ModItems.UNCOOKED_CHICKEN_AND_RICE, 1, 4, () -> Items.CHICKEN, () -> Items.CHICKEN, rice, rice));
        OVEN.add(new Baking(ModItems.UNCOOKED_CHICKEN_AND_RICE, ModItems.CHICKEN_AND_RICE, 15, 8));

        COOKING.add(new Assembly(ModItems.UNCOOKED_LAMB_CHOPS, 1, 5,
            () -> Items.MUTTON, () -> Items.BONE, () -> Items.MUTTON, () -> Items.BONE, () -> Items.POTATO));
        OVEN.add(new Baking(ModItems.UNCOOKED_LAMB_CHOPS, ModItems.LAMB_CHOPS, 18, 9));

        COOKING.add(new Assembly(ModItems.UNCOOKED_SEAFOOD_PASTA, 1, 5,
            () -> Items.SALMON, () -> Items.COD, () -> Items.WHEAT, () -> Items.WHEAT, () -> Items.WHEAT));
        OVEN.add(new Baking(ModItems.UNCOOKED_SEAFOOD_PASTA, ModItems.SEAFOOD_PASTA, 14, 7));

        COOKING.add(new Assembly(ModItems.UNCOOKED_SQUID_INK_PASTA, 1, 5,
            () -> Items.SALMON, () -> Items.INK_SAC, () -> Items.WHEAT, () -> Items.WHEAT, () -> Items.WHEAT));
        OVEN.add(new Baking(ModItems.UNCOOKED_SQUID_INK_PASTA, ModItems.SQUID_INK_PASTA, 14, 7));

        Supplier<Item> bowl = () -> Items.BOWL;
        COOKING.add(new Assembly(ModItems.UNCOOKED_SEAFOOD_CHOWDER, 1, 5,
            () -> Items.COD, () -> Items.SALMON, () -> Items.KELP, () -> Items.MILK_BUCKET, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_SEAFOOD_CHOWDER, ModItems.SEAFOOD_CHOWDER, 14, 7));

        COOKING.add(new Assembly(ModItems.UNCOOKED_FIERY_MUSHROOM_BOWL, 1, 5,
            () -> Items.MAGMA_CREAM, () -> Items.PORKCHOP, () -> Items.CRIMSON_FUNGUS, () -> Items.NETHER_WART, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_FIERY_MUSHROOM_BOWL, ModItems.FIERY_MUSHROOM_BOWL, 16, 8));

        COOKING.add(new Assembly(ModItems.UNCOOKED_SWEET_BERRY_TART, 1, 5,
            () -> Items.SWEET_BERRIES, () -> Items.SWEET_BERRIES, () -> Items.SUGAR, () -> Items.WHEAT, () -> Items.EGG));
        OVEN.add(new Baking(ModItems.UNCOOKED_SWEET_BERRY_TART, ModItems.SWEET_BERRY_TART, 12, 6));

        COOKING.add(new Assembly(ModItems.UNCOOKED_STUFFED_PUMPKIN, 1, 5,
            () -> Items.PUMPKIN, () -> Items.COOKED_MUTTON, () -> Items.CARROT, () -> Items.POTATO, () -> Items.BREAD));
        OVEN.add(new Baking(ModItems.UNCOOKED_STUFFED_PUMPKIN, ModItems.STUFFED_PUMPKIN, 20, 10));

        // Honey leaves its glass bottle behind in the slot.
        COOKING.add(new Assembly(ModItems.UNCOOKED_GOLDEN_GLAZED_FEAST, 1, 5,
            () -> Items.GOLDEN_CARROT, () -> Items.BEEF, () -> Items.HONEY_BOTTLE, () -> Items.BROWN_MUSHROOM, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_GOLDEN_GLAZED_FEAST, ModItems.GOLDEN_GLAZED_FEAST, 22, 11));

        COOKING.add(new Assembly(ModItems.UNCOOKED_MINERS_CASSEROLE, 1, 5,
            () -> Items.GLOW_BERRIES, () -> Items.COOKED_PORKCHOP, () -> Items.BAKED_POTATO, () -> Items.BROWN_MUSHROOM, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_MINERS_CASSEROLE, ModItems.MINERS_CASSEROLE, 18, 9));

        COOKING.add(new Assembly(ModItems.UNCOOKED_GARDEN_VEGGIE_CALZONE, 1, 5,
            () -> Items.BREAD, () -> Items.RED_MUSHROOM, () -> Items.CARROT, () -> Items.POTATO, () -> Items.BEETROOT));
        OVEN.add(new Baking(ModItems.UNCOOKED_GARDEN_VEGGIE_CALZONE, ModItems.GARDEN_VEGGIE_CALZONE, 15, 8));

        COOKING.add(new Assembly(ModItems.UNCOOKED_NETHER_DRAGON_POT_PIE, 1, 5,
            () -> Items.CHORUS_FRUIT, () -> Items.BLAZE_POWDER, () -> Items.PORKCHOP, () -> Items.PUMPKIN, () -> Items.BREAD));
        OVEN.add(new Baking(ModItems.UNCOOKED_NETHER_DRAGON_POT_PIE, ModItems.NETHER_DRAGON_POT_PIE, 18, 9));

        // Honey leaves its glass bottle behind in the slot.
        COOKING.add(new Assembly(ModItems.UNCOOKED_HONEY_GLAZED_WING_ROAST, 1, 5,
            () -> Items.HONEY_BOTTLE, () -> Items.CHICKEN, () -> Items.CARROT, () -> Items.SWEET_BERRIES, () -> Items.FEATHER));
        OVEN.add(new Baking(ModItems.UNCOOKED_HONEY_GLAZED_WING_ROAST, ModItems.HONEY_GLAZED_WING_ROAST, 15, 8));

        // Milk leaves its bucket behind in the slot.
        COOKING.add(new Assembly(ModItems.UNCOOKED_SUGAR_RUSH_BERRY_CREPE, 1, 5,
            () -> Items.WHEAT, () -> Items.EGG, () -> Items.MILK_BUCKET, () -> Items.SUGAR, () -> Items.SWEET_BERRIES));
        OVEN.add(new Baking(ModItems.UNCOOKED_SUGAR_RUSH_BERRY_CREPE, ModItems.SUGAR_RUSH_BERRY_CREPE, 10, 5));

        COOKING.add(new Assembly(ModItems.UNCOOKED_FARMERS_HARVEST_LOAF, 1, 5,
            () -> Items.WHEAT, () -> Items.CARROT, () -> Items.POTATO, () -> Items.BEETROOT, () -> Items.BONE_MEAL));
        OVEN.add(new Baking(ModItems.UNCOOKED_FARMERS_HARVEST_LOAF, ModItems.FARMERS_HARVEST_LOAF, 16, 8));

        COOKING.add(new Assembly(ModItems.UNCOOKED_SCHOLARS_BRAIN_STEW, 1, 5,
            () -> Items.SLIME_BALL, () -> Items.SPIDER_EYE, () -> Items.BONE_MEAL, () -> Items.SUGAR, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_SCHOLARS_BRAIN_STEW, ModItems.SCHOLARS_BRAIN_STEW, 16, 8));

        COOKING.add(new Assembly(ModItems.UNCOOKED_CAMPFIRE_CHILI, 1, 5,
            () -> Items.BEEF, () -> Items.BEETROOT, () -> Items.BEETROOT, () -> Items.BROWN_MUSHROOM, bowl));
        // simmered extra long to get it super hot: the longest bake of any dish
        OVEN.add(new Baking(ModItems.UNCOOKED_CAMPFIRE_CHILI, ModItems.CAMPFIRE_CHILI, 45, 15));

        // Skewers: meat, apple, meat, stick, stick
        Supplier<Item> stick = () -> Items.STICK;
        Supplier<Item> apple = () -> Items.APPLE;
        COOKING.add(new Assembly(ModItems.UNCOOKED_BEEF_SKEWER, 1, 5, () -> Items.BEEF, apple, () -> Items.BEEF, stick, stick));
        OVEN.add(new Baking(ModItems.UNCOOKED_BEEF_SKEWER, ModItems.BEEF_SKEWER, 12, 6));
        COOKING.add(new Assembly(ModItems.UNCOOKED_CHICKEN_SKEWER, 1, 5, () -> Items.CHICKEN, apple, () -> Items.CHICKEN, stick, stick));
        OVEN.add(new Baking(ModItems.UNCOOKED_CHICKEN_SKEWER, ModItems.CHICKEN_SKEWER, 12, 6));
        COOKING.add(new Assembly(ModItems.UNCOOKED_LAMB_SKEWER, 1, 5, () -> Items.MUTTON, apple, () -> Items.MUTTON, stick, stick));
        OVEN.add(new Baking(ModItems.UNCOOKED_LAMB_SKEWER, ModItems.LAMB_SKEWER, 12, 6));
        COOKING.add(new Assembly(ModItems.UNCOOKED_FISH_SKEWER, 1, 5, () -> Items.COD, apple, () -> Items.COD, stick, stick));
        OVEN.add(new Baking(ModItems.UNCOOKED_FISH_SKEWER, ModItems.FISH_SKEWER, 10, 5));

        // ── The second round of Oven meals: most need a Farmer's fine cut (farmer/MeatCut) ──
        Supplier<Item> potato = () -> Items.POTATO;
        Supplier<Item> carrot = () -> Items.CARROT;
        COOKING.add(new Assembly(ModItems.UNCOOKED_PULLED_PORK_SANDWICH, 1, 4,
            cut(com.alexh.capitalism.farmer.MeatCut.PORK_SHOULDER), () -> Items.BREAD, () -> Items.BEETROOT, () -> Items.SUGAR));
        OVEN.add(new Baking(ModItems.UNCOOKED_PULLED_PORK_SANDWICH, ModItems.PULLED_PORK_SANDWICH, 14, 7));
        COOKING.add(new Assembly(ModItems.UNCOOKED_BRISKET_AND_POTATOES, 1, 4,
            cut(com.alexh.capitalism.farmer.MeatCut.BEEF_BRISKET), potato, potato, carrot));
        OVEN.add(new Baking(ModItems.UNCOOKED_BRISKET_AND_POTATOES, ModItems.BRISKET_AND_POTATOES, 18, 9));
        COOKING.add(new Assembly(ModItems.UNCOOKED_SHEPHERDS_PIE, 1, 5,
            cut(com.alexh.capitalism.farmer.MeatCut.MUTTON_SHOULDER), potato, carrot, () -> Items.BREAD, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_SHEPHERDS_PIE, ModItems.SHEPHERDS_PIE, 18, 9));
        COOKING.add(new Assembly(ModItems.UNCOOKED_HAM_AND_EGG_BREAKFAST, 1, 4,
            cut(com.alexh.capitalism.farmer.MeatCut.PORK_HAM), () -> Items.EGG, () -> Items.EGG, () -> Items.BREAD));
        OVEN.add(new Baking(ModItems.UNCOOKED_HAM_AND_EGG_BREAKFAST, ModItems.HAM_AND_EGG_BREAKFAST, 12, 6));
        COOKING.add(new Assembly(ModItems.UNCOOKED_HUNTERS_RABBIT_PIE, 1, 5,
            cut(com.alexh.capitalism.farmer.MeatCut.RABBIT_SADDLE), carrot, potato, () -> Items.WHEAT, () -> Items.BROWN_MUSHROOM));
        OVEN.add(new Baking(ModItems.UNCOOKED_HUNTERS_RABBIT_PIE, ModItems.HUNTERS_RABBIT_PIE, 16, 8));
        COOKING.add(new Assembly(ModItems.UNCOOKED_FISH_TACOS, 1, 4,
            () -> Items.COD, () -> Items.WHEAT, carrot, () -> Items.SWEET_BERRIES));
        OVEN.add(new Baking(ModItems.UNCOOKED_FISH_TACOS, ModItems.FISH_TACOS, 10, 5));
        // Milk leaves its bucket behind in the slot.
        COOKING.add(new Assembly(ModItems.UNCOOKED_MUSHROOM_RISOTTO, 1, 5,
            rice, () -> Items.BROWN_MUSHROOM, () -> Items.RED_MUSHROOM, () -> Items.MILK_BUCKET, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_MUSHROOM_RISOTTO, ModItems.MUSHROOM_RISOTTO, 14, 7));
        COOKING.add(new Assembly(ModItems.UNCOOKED_APPLE_CRUMBLE, 1, 5,
            apple, apple, () -> Items.SUGAR, () -> Items.WHEAT, () -> Items.EGG));
        OVEN.add(new Baking(ModItems.UNCOOKED_APPLE_CRUMBLE, ModItems.APPLE_CRUMBLE, 12, 6));

        // ── The second round of Specialty meals (each helps a job) ──
        COOKING.add(new Assembly(ModItems.UNCOOKED_IRONWORKERS_STEW, 1, 5,
            cut(com.alexh.capitalism.farmer.MeatCut.BEEF_CHUCK), () -> Items.IRON_NUGGET, potato, carrot, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_IRONWORKERS_STEW, ModItems.IRONWORKERS_STEW, 18, 9));
        // Milk leaves its bucket, Honey its bottle
        COOKING.add(new Assembly(ModItems.UNCOOKED_LUMBERJACK_FLAPJACKS, 1, 5,
            () -> Items.WHEAT, () -> Items.EGG, () -> Items.MILK_BUCKET, () -> Items.HONEY_BOTTLE, () -> Items.SUGAR));
        OVEN.add(new Baking(ModItems.UNCOOKED_LUMBERJACK_FLAPJACKS, ModItems.LUMBERJACK_FLAPJACKS, 12, 6));
        COOKING.add(new Assembly(ModItems.UNCOOKED_BRICK_OVEN_PIZZA, 1, 5,
            () -> Items.BREAD, cut(com.alexh.capitalism.farmer.MeatCut.PORK_HAM), () -> Items.BEETROOT, () -> Items.BROWN_MUSHROOM, () -> Items.MILK_BUCKET));
        OVEN.add(new Baking(ModItems.UNCOOKED_BRICK_OVEN_PIZZA, ModItems.BRICK_OVEN_PIZZA, 15, 8));
        COOKING.add(new Assembly(ModItems.UNCOOKED_PRIME_RIB_DINNER, 1, 5,
            cut(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB), () -> Items.BAKED_POTATO, carrot, () -> Items.GOLDEN_CARROT, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_PRIME_RIB_DINNER, ModItems.PRIME_RIB_DINNER, 22, 11));
        COOKING.add(new Assembly(ModItems.UNCOOKED_ANGLERS_FISH_PIE, 1, 5,
            () -> Items.SALMON, () -> Items.COD, potato, () -> Items.EGG, () -> Items.BREAD));
        OVEN.add(new Baking(ModItems.UNCOOKED_ANGLERS_FISH_PIE, ModItems.ANGLERS_FISH_PIE, 15, 8));
        COOKING.add(new Assembly(ModItems.UNCOOKED_COMFORT_SOUP, 1, 5,
            cut(com.alexh.capitalism.farmer.MeatCut.CHICKEN_BREAST), carrot, potato, rice, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_COMFORT_SOUP, ModItems.COMFORT_SOUP, 14, 7));
        COOKING.add(new Assembly(ModItems.UNCOOKED_TRAIL_MIX_BAR, 1, 5,
            () -> Items.SWEET_BERRIES, () -> Items.COCOA_BEANS, () -> Items.WHEAT, () -> Items.HONEY_BOTTLE, () -> Items.SUGAR));
        OVEN.add(new Baking(ModItems.UNCOOKED_TRAIL_MIX_BAR, ModItems.TRAIL_MIX_BAR, 10, 5));
        COOKING.add(new Assembly(ModItems.UNCOOKED_NIGHT_WATCH_HOTPOT, 1, 5,
            rice, cut(com.alexh.capitalism.farmer.MeatCut.PORK_LOIN), () -> Items.BROWN_MUSHROOM, carrot, bowl));
        OVEN.add(new Baking(ModItems.UNCOOKED_NIGHT_WATCH_HOTPOT, ModItems.NIGHT_WATCH_HOTPOT, 16, 8));

        // ── Cooking Table only (no Oven) ──
        COOKING.add(new Assembly(ModItems.FRUIT_SALAD, 1, 4, () -> Items.MELON_SLICE, () -> Items.APPLE, () -> Items.MELON_SLICE, () -> Items.WHEAT));
        COOKING.add(new Assembly(ModItems.FROSTY_MELON_SORBET, 1, 5, () -> Items.MELON_SLICE, () -> Items.SNOWBALL, () -> Items.SUGAR, () -> Items.SWEET_BERRIES, bowl));
    }

    private KitchenRecipes() {
    }

    // a Farmer's raw fine cut (Raw Beef Brisket...), only made at the Butcher's Table
    private static Supplier<Item> cut(com.alexh.capitalism.farmer.MeatCut cut) {
        return () -> ModItems.MEAT_CUTS.get(cut).get();
    }

    public static @Nullable Assembly find(List<Assembly> recipes, Container slots, int slotCount) {
        for (Assembly recipe : recipes) {
            if (recipe.matches(slots, slotCount)) return recipe;
        }
        return null;
    }

    public static @Nullable Baking baking(ItemStack input) {
        if (input.isEmpty()) return null;
        for (Baking recipe : OVEN) {
            if (input.is(recipe.input().get())) return recipe;
        }
        return null;
    }

    private static boolean isOvenInput(ItemStack stack) {
        for (Baking recipe : OVEN) if (stack.is(recipe.input().get())) return true;
        return false;
    }

    // The Oven recipe that makes this meal (to know how long it takes to burn), or null.
    public static @Nullable Baking bakedInto(ItemStack meal) {
        if (meal.isEmpty()) return null;
        for (Baking recipe : OVEN) {
            if (meal.is(recipe.result().get())) return recipe;
        }
        return null;
    }

    // Gourmet = anything a Cook makes at their stations (sushi, Oven meals, Cooking Table dishes that skip the Oven), and
// vanilla food a Cook made (occupation/CookMadeFood: cooked meat, bread...).
    // Shown on the tooltip. Uncooked dishes (Cooking Table results that go in the Oven) aren't.
    public static boolean isGourmet(ItemStack stack) {
        if (CookMadeFood.isCookMade(stack)) return true; // vanilla food a Cook made
        for (Assembly recipe : COOKING) if (stack.is(recipe.result().get()) && !isOvenInput(stack)) return true;
        for (Assembly recipe : SUSHI) if (stack.is(recipe.result().get())) return true;
        for (Baking recipe : OVEN) if (stack.is(recipe.result().get())) return true;
        return false;
    }
}

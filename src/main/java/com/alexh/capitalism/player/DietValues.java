package com.alexh.capitalism.player;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Which food groups each food counts toward, as shares that add up to 1: {vegetable, fruit, protein, carbs}.
// A meal adds (its hunger points x share) to each group. Foods not listed here don't change your diet.
// Add foods (e.g. Chef meals later) with one line each.
public final class DietValues {

    private static final float[] VEGETABLE = {1, 0, 0, 0};
    private static final float[] FRUIT = {0, 1, 0, 0};
    private static final float[] PROTEIN = {0, 0, 1, 0};
    private static final float[] CARBS = {0, 0, 0, 1};

    private static @Nullable Map<Item, float[]> table;

    private DietValues() {
    }

    public static float @Nullable [] sharesFor(ItemStack stack) {
        return table().get(stack.getItem());
    }

    private static Map<Item, float[]> table() {
        if (table != null) return table;
        Map<Item, float[]> t = new HashMap<>();

        // Vegetables
        for (Item i : new Item[]{Items.CARROT, Items.GOLDEN_CARROT, Items.BEETROOT, Items.BEETROOT_SOUP, Items.DRIED_KELP,
            Items.MUSHROOM_STEW, Items.SUSPICIOUS_STEW}) t.put(i, VEGETABLE);

        // Fruit
        for (Item i : new Item[]{Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.MELON_SLICE,
            Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.CHORUS_FRUIT}) t.put(i, FRUIT);

        // Protein
        for (Item i : new Item[]{Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.MUTTON,
            Items.COOKED_MUTTON, Items.CHICKEN, Items.COOKED_CHICKEN, Items.RABBIT, Items.COOKED_RABBIT, Items.COD,
            Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH,
            Items.ROTTEN_FLESH, Items.SPIDER_EYE}) t.put(i, PROTEIN);

        // Carbs
        for (Item i : new Item[]{Items.BREAD, Items.COOKIE, Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO,
            Items.HONEY_BOTTLE}) t.put(i, CARBS);

        // Sushi (Gourmet): rice is carbs, fish and meat protein, kelp (nori) a pinch of vegetables
        t.put(ModItems.SALMON_NIGIRI.get(), new float[]{0, 0, 0.6f, 0.4f});
        t.put(ModItems.COD_NIGIRI.get(), new float[]{0, 0, 0.6f, 0.4f});
        t.put(ModItems.TROPICAL_NIGIRI.get(), new float[]{0, 0, 0.6f, 0.4f});
        t.put(ModItems.PUFFER_NIGIRI.get(), new float[]{0, 0, 0.6f, 0.4f});
        t.put(ModItems.COD_ROLL.get(), new float[]{0.1f, 0, 0.5f, 0.4f});
        t.put(ModItems.KELP_ROLL.get(), new float[]{0.5f, 0, 0, 0.5f});
        t.put(ModItems.TROPICAL_ROLL.get(), new float[]{0.1f, 0.25f, 0.35f, 0.3f});
        t.put(ModItems.RAINBOW_ROLL.get(), new float[]{0.1f, 0, 0.6f, 0.3f});
        t.put(ModItems.PUFFER_ROLL.get(), new float[]{0.25f, 0, 0.4f, 0.35f});
        t.put(ModItems.VEGETABLE_ROLL.get(), new float[]{0.6f, 0, 0, 0.4f});
        t.put(ModItems.BAKED_SALMON_ROLL.get(), new float[]{0.05f, 0, 0.7f, 0.25f});
        t.put(ModItems.FRUIT_ROLL.get(), new float[]{0.05f, 0.5f, 0, 0.45f});
        t.put(ModItems.STEAK_ROLL.get(), new float[]{0.05f, 0, 0.75f, 0.2f});
        t.put(ModItems.POTATO_ROLL.get(), new float[]{0.1f, 0, 0, 0.9f});
        t.put(ModItems.SWEET_BREAD_ROLL.get(), new float[]{0, 0.1f, 0, 0.9f});
        t.put(ModItems.OCEAN_SOVEREIGN_ROLL.get(), new float[]{0.1f, 0, 0.5f, 0.4f});

        // Oven meals (Gourmet)
        t.put(ModItems.CHICKEN_POT_PIE.get(), new float[]{0.25f, 0, 0.4f, 0.35f});
        t.put(ModItems.LOADED_BAKED_POTATO.get(), new float[]{0, 0, 0.45f, 0.55f});
        t.put(ModItems.CHICKEN_AND_RICE.get(), new float[]{0, 0, 0.55f, 0.45f});
        t.put(ModItems.LAMB_CHOPS.get(), new float[]{0, 0, 0.75f, 0.25f});
        t.put(ModItems.SEAFOOD_PASTA.get(), new float[]{0, 0, 0.45f, 0.55f});
        t.put(ModItems.SQUID_INK_PASTA.get(), new float[]{0, 0, 0.4f, 0.6f});
        t.put(ModItems.FRUIT_SALAD.get(), new float[]{0, 0.75f, 0, 0.25f});
        t.put(ModItems.SEAFOOD_CHOWDER.get(), new float[]{0.1f, 0, 0.6f, 0.3f});
        t.put(ModItems.FIERY_MUSHROOM_BOWL.get(), new float[]{0.4f, 0, 0.5f, 0.1f});
        t.put(ModItems.SWEET_BERRY_TART.get(), new float[]{0, 0.5f, 0.1f, 0.4f});
        t.put(ModItems.STUFFED_PUMPKIN.get(), new float[]{0.35f, 0, 0.35f, 0.3f});
        t.put(ModItems.GOLDEN_GLAZED_FEAST.get(), new float[]{0.25f, 0, 0.5f, 0.25f});
        t.put(ModItems.MINERS_CASSEROLE.get(), new float[]{0.1f, 0.1f, 0.45f, 0.35f});
        t.put(ModItems.GARDEN_VEGGIE_CALZONE.get(), new float[]{0.6f, 0, 0, 0.4f});
        t.put(ModItems.NETHER_DRAGON_POT_PIE.get(), new float[]{0.2f, 0.1f, 0.4f, 0.3f});
        t.put(ModItems.HONEY_GLAZED_WING_ROAST.get(), new float[]{0.15f, 0.15f, 0.6f, 0.1f});
        t.put(ModItems.BEEF_SKEWER.get(), new float[]{0, 0.2f, 0.8f, 0});
        t.put(ModItems.CHICKEN_SKEWER.get(), new float[]{0, 0.2f, 0.8f, 0});
        t.put(ModItems.LAMB_SKEWER.get(), new float[]{0, 0.2f, 0.8f, 0});
        t.put(ModItems.FISH_SKEWER.get(), new float[]{0, 0.2f, 0.8f, 0});
        t.put(ModItems.SUGAR_RUSH_BERRY_CREPE.get(), new float[]{0, 0.35f, 0.15f, 0.5f});
        t.put(ModItems.FARMERS_HARVEST_LOAF.get(), new float[]{0.5f, 0, 0, 0.5f});
        t.put(ModItems.SCHOLARS_BRAIN_STEW.get(), new float[]{0, 0, 0.6f, 0.4f});
        t.put(ModItems.CAMPFIRE_CHILI.get(), new float[]{0.35f, 0, 0.55f, 0.1f});
        t.put(ModItems.FROSTY_MELON_SORBET.get(), new float[]{0, 0.75f, 0, 0.25f});
        t.put(ModItems.PULLED_PORK_SANDWICH.get(), new float[]{0.1f, 0, 0.55f, 0.35f});
        t.put(ModItems.BRISKET_AND_POTATOES.get(), new float[]{0.15f, 0, 0.5f, 0.35f});
        t.put(ModItems.SHEPHERDS_PIE.get(), new float[]{0.2f, 0, 0.45f, 0.35f});
        t.put(ModItems.HAM_AND_EGG_BREAKFAST.get(), new float[]{0, 0, 0.65f, 0.35f});
        t.put(ModItems.HUNTERS_RABBIT_PIE.get(), new float[]{0.3f, 0, 0.4f, 0.3f});
        t.put(ModItems.FISH_TACOS.get(), new float[]{0.15f, 0.15f, 0.4f, 0.3f});
        t.put(ModItems.MUSHROOM_RISOTTO.get(), new float[]{0.35f, 0, 0.1f, 0.55f});
        t.put(ModItems.APPLE_CRUMBLE.get(), new float[]{0, 0.55f, 0.05f, 0.4f});
        t.put(ModItems.IRONWORKERS_STEW.get(), new float[]{0.2f, 0, 0.5f, 0.3f});
        t.put(ModItems.LUMBERJACK_FLAPJACKS.get(), new float[]{0, 0, 0.15f, 0.85f});
        t.put(ModItems.BRICK_OVEN_PIZZA.get(), new float[]{0.25f, 0, 0.35f, 0.4f});
        t.put(ModItems.PRIME_RIB_DINNER.get(), new float[]{0.2f, 0, 0.55f, 0.25f});
        t.put(ModItems.ANGLERS_FISH_PIE.get(), new float[]{0.05f, 0, 0.55f, 0.4f});
        t.put(ModItems.COMFORT_SOUP.get(), new float[]{0.3f, 0, 0.4f, 0.3f});
        t.put(ModItems.TRAIL_MIX_BAR.get(), new float[]{0, 0.4f, 0, 0.6f});
        t.put(ModItems.NIGHT_WATCH_HOTPOT.get(), new float[]{0.25f, 0, 0.4f, 0.35f});

        // Mixed meals
        t.put(Items.PUMPKIN_PIE, new float[]{0, 0.5f, 0, 0.5f});
        t.put(Items.RABBIT_STEW, new float[]{0.4f, 0, 0.4f, 0.2f});

        table = t;
        return t;
    }
}

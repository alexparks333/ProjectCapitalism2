package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// A Bartending Table recipe: 1-5 ingredients in any order in the 5 slots, plus `bottles` Empty Glass Bottles
// from the glass slot, makes `count` of `result`. Every drink made uses one glass bottle, so drinks have
// bottles == count; not enough bottles = can't make it. Every recipe is listed in Bartending.md.
public record BartenderRecipe(Kind kind, Supplier<? extends Item> result, int count, int bottles, List<Supplier<? extends Item>> ingredients) {

    // INGREDIENT = made to go into drinks (shots, bitters). DRINK = a finished drink (wine glasses, cocktails).
    // Only used to sort them into tabs in the Bartender's Guide.
    public enum Kind { INGREDIENT, DRINK }

    public static final int SLOTS = 5;

    @SafeVarargs
    public BartenderRecipe(Kind kind, Supplier<? extends Item> result, int count, int bottles, Supplier<? extends Item>... ingredients) {
        this(kind, result, count, bottles, List.of(ingredients));
    }

    public static final List<BartenderRecipe> RECIPES = buildRecipes();

    private static List<BartenderRecipe> buildRecipes() {
        List<BartenderRecipe> recipes = new ArrayList<>();

        // Pour a bucket of liquor into 4 shots: one glass bottle per shot (the empty bucket comes back)
        for (LiquorType liquor : LiquorType.values()) {
            recipes.add(new BartenderRecipe(Kind.INGREDIENT, ModItems.SHOTS.get(liquor), 4, 4, ModItems.LIQUOR_BUCKETS.get(liquor)));
        }

        // Pour a bucket of juice (or Simple Syrup) into 6 glasses: one glass bottle each (the empty bucket comes back)
        for (JuiceType juice : JuiceType.values()) {
            recipes.add(new BartenderRecipe(Kind.INGREDIENT, ModItems.JUICES.get(juice), 6, 6, ModItems.JUICE_BUCKETS.get(juice)));
        }

        // Spring Water (from the Advanced Osmosis Machine): a bucket into 6 bottles (the bucket comes back)
        recipes.add(new BartenderRecipe(Kind.INGREDIENT, ModItems.SPRING_WATER, 6, 6, ModItems.SPRING_WATER_BUCKET));

        // Bitters: purified water, cocoa beans and sugar -> 4 bitters, one glass bottle each
        recipes.add(new BartenderRecipe(Kind.INGREDIENT, ModItems.BITTERS, 4, 4,
            ModItems.PURIFIED_WATER, () -> Items.COCOA_BEANS, () -> Items.SUGAR));

        // Wine by the glass: 3 shots of wine + 1 glass bottle
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.GLASS_OF_RED_WINE, 1, 1,
            ModItems.SHOTS.get(LiquorType.RED_WINE), ModItems.SHOTS.get(LiquorType.RED_WINE), ModItems.SHOTS.get(LiquorType.RED_WINE)));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.GLASS_OF_WHITE_WINE, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHITE_WINE), ModItems.SHOTS.get(LiquorType.WHITE_WINE), ModItems.SHOTS.get(LiquorType.WHITE_WINE)));

        // ── Cocktails (1 glass bottle each) ──
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.BEET_NIGHTCAP, 1, 1,
            ModItems.JUICES.get(JuiceType.BEETROOT),
            ModItems.JUICES.get(JuiceType.BEETROOT),
            ModItems.JUICES.get(JuiceType.SIMPLE_SYRUP),
            ModItems.BITTERS,
            ModItems.SHOTS.get(LiquorType.TEQUILA)));

        Supplier<Item> bitters = ModItems.BITTERS;
        Supplier<Item> syrup = ModItems.JUICES.get(JuiceType.SIMPLE_SYRUP);
        Supplier<Item> sugar = () -> Items.SUGAR;
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.GLOWBERRY_GOLD_RUSH, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHISKEY), ModItems.JUICES.get(JuiceType.GLOW_BERRY), syrup, () -> Items.HONEY_BOTTLE, sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.NETHER_FIREBALL_SHOT, 1, 1,
            ModItems.SHOTS.get(LiquorType.TEQUILA), bitters, () -> Items.BLAZE_POWDER, () -> Items.NETHER_WART, () -> Items.MAGMA_CREAM));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.ENDER_TELE_TINI, 1, 1,
            ModItems.SHOTS.get(LiquorType.VODKA), ModItems.JUICES.get(JuiceType.CHORUS_FRUIT), bitters, () -> Items.ENDER_PEARL, syrup));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.PIRATES_HIGH_SEAS_GROG, 1, 1,
            ModItems.SHOTS.get(LiquorType.RUM), ModItems.JUICES.get(JuiceType.KELP), () -> Items.CACTUS, sugar, () -> Items.WATER_BUCKET));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.MINERS_MOONSHINE, 1, 1,
            ModItems.SHOTS.get(LiquorType.VODKA), ModItems.JUICES.get(JuiceType.BEETROOT), ModItems.JUICES.get(JuiceType.CARROT), sugar, () -> Items.GOLD_INGOT));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.VAMPIRIC_MULLED_WINE, 1, 1,
            ModItems.SHOTS.get(LiquorType.RED_WINE), bitters, ModItems.JUICES.get(JuiceType.APPLE), sugar, () -> Items.SWEET_BERRIES));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.OVERWORLD_MOJITO, 1, 1,
            ModItems.SHOTS.get(LiquorType.RUM), ModItems.JUICES.get(JuiceType.WATERMELON), syrup, sugar, () -> Items.OAK_LEAVES));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.PHANTOMS_NIGHTCAP, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHISKEY), ModItems.JUICES.get(JuiceType.APPLE), () -> Items.PHANTOM_MEMBRANE, syrup, bitters));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.ZOMBIES_HANGOVER_CURE, 1, 1,
            ModItems.SHOTS.get(LiquorType.VODKA), ModItems.JUICES.get(JuiceType.CARROT), ModItems.JUICES.get(JuiceType.BEETROOT), () -> Items.ROTTEN_FLESH, () -> Items.MILK_BUCKET));

        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.ENDER_TWILIGHT_FIZZ, 1, 1,
            ModItems.SHOTS.get(LiquorType.VODKA), ModItems.SHOTS.get(LiquorType.WHITE_WINE), ModItems.JUICES.get(JuiceType.CHORUS_FRUIT),
            ModItems.JUICES.get(JuiceType.GLOW_BERRY), sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.DRUNKEN_BRAWLERS_STOUT, 1, 1,
            ModItems.SHOTS.get(LiquorType.TEQUILA), ModItems.SHOTS.get(LiquorType.WHISKEY), ModItems.SHOTS.get(LiquorType.RUM), bitters, () -> Items.LEATHER));

        // ── Classic cocktails (no effects) ──
        // Snowball = the ice
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.CLASSIC_OLD_FASHIONED, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHISKEY), bitters, syrup, () -> Items.SWEET_BERRIES, () -> Items.SNOWBALL));
        // Apple Juice = the sour, Egg = the foam
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.WHISKEY_SOUR, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHISKEY), ModItems.JUICES.get(JuiceType.APPLE), syrup, () -> Items.EGG, bitters));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.WATERMELON_BREEZE, 1, 1,
            ModItems.SHOTS.get(LiquorType.VODKA), ModItems.JUICES.get(JuiceType.WATERMELON), ModItems.JUICES.get(JuiceType.APPLE), syrup, sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.SANGRIA_PITCHER, 1, 1,
            ModItems.SHOTS.get(LiquorType.RED_WINE), ModItems.JUICES.get(JuiceType.APPLE), ModItems.JUICES.get(JuiceType.WATERMELON), () -> Items.SWEET_BERRIES, sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.WHITE_WINE_SPRITZER, 1, 1,
            ModItems.SHOTS.get(LiquorType.WHITE_WINE), ModItems.JUICES.get(JuiceType.APPLE), syrup, () -> Items.WATER_BUCKET, sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.TROPICAL_KELP_COOLER, 1, 1,
            ModItems.SHOTS.get(LiquorType.RUM), ModItems.JUICES.get(JuiceType.KELP), ModItems.JUICES.get(JuiceType.WATERMELON), syrup, sugar));
        recipes.add(new BartenderRecipe(Kind.DRINK, ModItems.BEETROOT_VELVET_SOUR, 1, 1,
            ModItems.SHOTS.get(LiquorType.TEQUILA), ModItems.JUICES.get(JuiceType.BEETROOT), bitters, syrup, () -> Items.EGG));

        return List.copyOf(recipes);
    }

    // True when the non-empty ingredient slots hold exactly this recipe's ingredients, in any order,
    // and the glass slot has enough bottles.
    public boolean matches(Container slots, int glassSlot) {
        if (slots.getItem(glassSlot).getCount() < bottles) return false;

        List<Item> needed = new ArrayList<>();
        for (Supplier<? extends Item> ingredient : ingredients) {
            needed.add(ingredient.get());
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = slots.getItem(i);
            if (stack.isEmpty()) continue;
            if (!needed.remove(stack.getItem())) return false;
        }
        return needed.isEmpty();
    }

    public static @Nullable BartenderRecipe find(Container slots, int glassSlot) {
        for (BartenderRecipe recipe : RECIPES) {
            if (recipe.matches(slots, glassSlot)) return recipe;
        }
        return null;
    }
}

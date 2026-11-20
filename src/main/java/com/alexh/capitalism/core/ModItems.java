package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.JuiceType;
import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.enchanter.ApprenticeItem;
import com.alexh.capitalism.item.BartendersGuideItem;
import com.alexh.capitalism.item.PlateItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;

@EventBusSubscriber(modid = Capitalism.MODID)
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Capitalism.MODID);

    // Drinkable like a potion, hands back the glass bottle (also when used as an ingredient). Thirst restore is in ModServerEvents.
    public static final DeferredItem<Item> PURIFIED_WATER = ITEMS.registerSimpleItem("purified_water", props -> props
        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
        .usingConvertsTo(Items.GLASS_BOTTLE)
        .craftRemainder(Items.GLASS_BOTTLE)
        .stacksTo(16));

    // Not drinkable or placeable; only a crafting ingredient. Leaves the empty bucket behind when crafted.
    public static final DeferredItem<Item> PURIFIED_WATER_BUCKET = ITEMS.registerSimpleItem("purified_water_bucket", props -> props
        .craftRemainder(Items.BUCKET)
        .stacksTo(1));
    // Spring Water (Bartenders: the Advanced Osmosis Machine, block/OsmosisMachineBlock): Excellent water, pulls purity
    // toward 100. A bucket of it (poured into 6 bottles at the Bartending Table), and the bottles.
    public static final DeferredItem<Item> SPRING_WATER_BUCKET = ITEMS.registerSimpleItem("spring_water_bucket", props -> props
        .craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SPRING_WATER = ITEMS.registerSimpleItem("spring_water", props -> props
        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
        .usingConvertsTo(Items.GLASS_BOTTLE)
        .craftRemainder(Items.GLASS_BOTTLE)
        .stacksTo(16).rarity(Rarity.UNCOMMON));
    // the machine's filters: a bucket each use (Osmosis: 2, Advanced: 4)
    public static final DeferredItem<Item> OSMOSIS_FILTER = ITEMS.registerSimpleItem("osmosis_filter", props -> props.durability(2));
    public static final DeferredItem<Item> ADVANCED_OSMOSIS_FILTER = ITEMS.registerSimpleItem("advanced_osmosis_filter", props -> props.durability(4).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> PURIFICATION_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.PURIFICATION_TABLE);

    // One bucket per juice (ingredients for the Bartender later); each leaves the empty bucket behind when crafted.
    public static final Map<JuiceType, DeferredItem<Item>> JUICE_BUCKETS = registerJuiceBuckets();

    private static Map<JuiceType, DeferredItem<Item>> registerJuiceBuckets() {
        Map<JuiceType, DeferredItem<Item>> buckets = new EnumMap<>(JuiceType.class);
        for (JuiceType juice : JuiceType.values()) {
            buckets.put(juice, ITEMS.registerSimpleItem(juice.itemId(), props -> props
                .craftRemainder(Items.BUCKET)
                .stacksTo(1)));
        }
        return buckets;
    }

    public static final DeferredItem<BlockItem> JUICER = ITEMS.registerSimpleBlockItem(ModBlocks.JUICER);

    public static final DeferredItem<BlockItem> BARTENDING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.BARTENDING_TABLE);

    // Rice Seeds: plant them in shallow water. The ripe plant gives Rice (the grains you cook with) and more seeds.
    public static final DeferredItem<BlockItem> RICE_SEEDS = ITEMS.registerItem("rice_seeds",
        props -> new BlockItem(ModBlocks.RICE.get(), props.useItemDescriptionPrefix()));
    public static final DeferredItem<Item> RICE = ITEMS.registerSimpleItem("rice", props -> props);
    // Rice cooked in a Furnace, Smoker or the Cook's Oven. The rice that goes into sushi.
    public static final DeferredItem<Item> COOKED_RICE = ITEMS.registerSimpleItem("cooked_rice", props -> props);

    // ── Sushi (Gourmet; made at the Sushi Table by Cooks) ──
    // Hunger points (2 = one drumstick) and saturation: rolls with two cooked mains are the big meals. Normal food only
    // fills 2/3 (FoodNerf); these are full strength and keep you full longer. Diet groups: DietValues.
    // Edible even with a full bar: what doesn't fit becomes golden drumsticks (see GourmetFood).
    public static final DeferredItem<Item> SALMON_NIGIRI = gourmet("salmon_nigiri", 5, 0.6F);
    public static final DeferredItem<Item> COD_NIGIRI = gourmet("cod_nigiri", 5, 0.6F);
    public static final DeferredItem<Item> TROPICAL_NIGIRI = gourmet("tropical_nigiri", 4, 0.5F);
    public static final DeferredItem<Item> PUFFER_NIGIRI = gourmet("puffer_nigiri", 4, 0.6F);
    public static final DeferredItem<Item> COD_ROLL = gourmet("cod_roll", 6, 0.6F);
    public static final DeferredItem<Item> KELP_ROLL = gourmet("kelp_roll", 5, 0.5F);
    public static final DeferredItem<Item> TROPICAL_ROLL = gourmet("tropical_roll", 7, 0.6F);
    public static final DeferredItem<Item> RAINBOW_ROLL = gourmet("rainbow_roll", 8, 0.7F);
    public static final DeferredItem<Item> PUFFER_ROLL = gourmet("puffer_roll", 7, 0.7F);
    public static final DeferredItem<Item> VEGETABLE_ROLL = gourmet("vegetable_roll", 6, 0.6F);
    public static final DeferredItem<Item> BAKED_SALMON_ROLL = gourmet("baked_salmon_roll", 12, 0.8F);
    public static final DeferredItem<Item> FRUIT_ROLL = gourmet("fruit_roll", 6, 0.4F);
    public static final DeferredItem<Item> STEAK_ROLL = gourmet("steak_roll", 14, 0.8F);
    public static final DeferredItem<Item> POTATO_ROLL = gourmet("potato_roll", 11, 0.6F);
    public static final DeferredItem<Item> SWEET_BREAD_ROLL = gourmet("sweet_bread_roll", 10, 0.5F);
    // Dolphin's Grace for 1:00 and Water Breathing for 1:30: fast swimming, no running out of air
    public static final DeferredItem<Item> OCEAN_SOVEREIGN_ROLL = gourmet("ocean_sovereign_roll", 9, 0.7F, null, () -> List.of(
        new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60 * 20), new MobEffectInstance(MobEffects.WATER_BREATHING, 90 * 20)));

    // What a sushi roll turns into if the ROLL is held too long. Can't be eaten.
    public static final DeferredItem<Item> SQUASHED_SUSHI = ITEMS.registerSimpleItem("squashed_sushi", props -> props);

    // Gourmet food (sushi, Cooking Table / Oven meals): edible even when full, stacks to 16.
    private static DeferredItem<Item> gourmet(String id, int hunger, float saturation) {
        return ITEMS.registerSimpleItem(id, props -> props.stacksTo(16).food(new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation).alwaysEdible().build()));
    }

    // Gourmet food that leaves something behind when eaten (a bowl, a skewer's stick; null = nothing) and/or gives
    // effects when eaten.
    private static DeferredItem<Item> gourmet(String id, int hunger, float saturation, @Nullable Item leftover, Supplier<List<MobEffectInstance>> effects) {
        return ITEMS.registerSimpleItem(id, props -> {
            Consumable.Builder consumable = Consumables.defaultFood();
            List<MobEffectInstance> list = effects.get();
            if (!list.isEmpty()) consumable.onConsume(new ApplyStatusEffectsConsumeEffect(list));
            props.stacksTo(16).food(new FoodProperties.Builder().nutrition(hunger).saturationModifier(saturation).alwaysEdible().build(), consumable.build());
            if (leftover != null) props.usingConvertsTo(leftover);
            return props;
        });
    }

    // Oven meals: put together at the Cooking Table (uncooked, can't be eaten), then baked in the Oven (Gourmet).
    public static final DeferredItem<Item> UNCOOKED_CHICKEN_POT_PIE = ITEMS.registerSimpleItem("uncooked_chicken_pot_pie", props -> props);
    public static final DeferredItem<Item> CHICKEN_POT_PIE = gourmet("chicken_pot_pie", 14, 0.8F);
    public static final DeferredItem<Item> UNCOOKED_LOADED_BAKED_POTATO = ITEMS.registerSimpleItem("uncooked_loaded_baked_potato", props -> props);
    public static final DeferredItem<Item> LOADED_BAKED_POTATO = gourmet("loaded_baked_potato", 10, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_CHICKEN_AND_RICE = ITEMS.registerSimpleItem("uncooked_chicken_and_rice", props -> props);
    public static final DeferredItem<Item> CHICKEN_AND_RICE = gourmet("chicken_and_rice", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_LAMB_CHOPS = ITEMS.registerSimpleItem("uncooked_lamb_chops", props -> props);
    public static final DeferredItem<Item> LAMB_CHOPS = gourmet("lamb_chops", 14, 0.8F);
    public static final DeferredItem<Item> UNCOOKED_SEAFOOD_PASTA = ITEMS.registerSimpleItem("uncooked_seafood_pasta", props -> props);
    public static final DeferredItem<Item> SEAFOOD_PASTA = gourmet("seafood_pasta", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_SQUID_INK_PASTA = ITEMS.registerSimpleItem("uncooked_squid_ink_pasta", props -> props);
    public static final DeferredItem<Item> SQUID_INK_PASTA = gourmet("squid_ink_pasta", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_SEAFOOD_CHOWDER = ITEMS.registerSimpleItem("uncooked_seafood_chowder", props -> props);
    public static final DeferredItem<Item> SEAFOOD_CHOWDER = gourmet("seafood_chowder", 12, 0.7F, Items.BOWL, List::of);
    public static final DeferredItem<Item> UNCOOKED_FIERY_MUSHROOM_BOWL = ITEMS.registerSimpleItem("uncooked_fiery_mushroom_bowl", props -> props);
    // Fire Resistance for 4 minutes, and 3x lava swimming for the first minute
    public static final DeferredItem<Item> FIERY_MUSHROOM_BOWL = gourmet("fiery_mushroom_bowl", 10, 0.6F, Items.BOWL, () -> List.of(
        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4 * 60 * 20), new MobEffectInstance(ModEffects.LAVA_SWIMMER, 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_SWEET_BERRY_TART = ITEMS.registerSimpleItem("uncooked_sweet_berry_tart", props -> props);
    public static final DeferredItem<Item> SWEET_BERRY_TART = gourmet("sweet_berry_tart", 8, 0.5F);
    public static final DeferredItem<Item> UNCOOKED_STUFFED_PUMPKIN = ITEMS.registerSimpleItem("uncooked_stuffed_pumpkin", props -> props);
    public static final DeferredItem<Item> STUFFED_PUMPKIN = gourmet("stuffed_pumpkin", 16, 0.8F);
    public static final DeferredItem<Item> UNCOOKED_GOLDEN_GLAZED_FEAST = ITEMS.registerSimpleItem("uncooked_golden_glazed_feast", props -> props);
    public static final DeferredItem<Item> GOLDEN_GLAZED_FEAST = gourmet("golden_glazed_feast", 24, 1.0F, Items.BOWL, List::of);
    public static final DeferredItem<Item> UNCOOKED_MINERS_CASSEROLE = ITEMS.registerSimpleItem("uncooked_miners_casserole", props -> props);
    // Night Vision and Haste for 2 minutes
    public static final DeferredItem<Item> MINERS_CASSEROLE = gourmet("miners_casserole", 14, 0.8F, Items.BOWL, () -> List.of(
        new MobEffectInstance(MobEffects.NIGHT_VISION, 2 * 60 * 20), new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_GARDEN_VEGGIE_CALZONE = ITEMS.registerSimpleItem("uncooked_garden_veggie_calzone", props -> props);
    public static final DeferredItem<Item> GARDEN_VEGGIE_CALZONE = gourmet("garden_veggie_calzone", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_NETHER_DRAGON_POT_PIE = ITEMS.registerSimpleItem("uncooked_nether_dragon_pot_pie", props -> props);
    // Fire Resistance and Ender Leap for 3 minutes
    public static final DeferredItem<Item> NETHER_DRAGON_POT_PIE = gourmet("nether_dragon_pot_pie", 14, 0.8F, null, () -> List.of(
        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3 * 60 * 20), new MobEffectInstance(ModEffects.ENDER_LEAP, 3 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_HONEY_GLAZED_WING_ROAST = ITEMS.registerSimpleItem("uncooked_honey_glazed_wing_roast", props -> props);
    public static final DeferredItem<Item> HONEY_GLAZED_WING_ROAST = gourmet("honey_glazed_wing_roast", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_SUGAR_RUSH_BERRY_CREPE = ITEMS.registerSimpleItem("uncooked_sugar_rush_berry_crepe", props -> props);
    // Speed III and Jump Boost II for 45 seconds
    public static final DeferredItem<Item> SUGAR_RUSH_BERRY_CREPE = gourmet("sugar_rush_berry_crepe", 8, 0.5F, null, () -> List.of(
        new MobEffectInstance(MobEffects.SPEED, 45 * 20, 2), new MobEffectInstance(MobEffects.JUMP_BOOST, 45 * 20, 1)));
    public static final DeferredItem<Item> UNCOOKED_FARMERS_HARVEST_LOAF = ITEMS.registerSimpleItem("uncooked_farmers_harvest_loaf", props -> props);
    // Green Thumb Aura for 1 minute: nearby crops and saplings grow fast
    public static final DeferredItem<Item> FARMERS_HARVEST_LOAF = gourmet("farmers_harvest_loaf", 12, 0.7F, null, () -> List.of(
        new MobEffectInstance(ModEffects.GREEN_THUMB, 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_SCHOLARS_BRAIN_STEW = ITEMS.registerSimpleItem("uncooked_scholars_brain_stew", props -> props);
    // Scholar's Focus for 3 minutes: double experience from mob kills
    public static final DeferredItem<Item> SCHOLARS_BRAIN_STEW = gourmet("scholars_brain_stew", 8, 0.6F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.SCHOLARS_FOCUS, 3 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_CAMPFIRE_CHILI = ITEMS.registerSimpleItem("uncooked_campfire_chili", props -> props);
    // Warmed Up for 5 minutes: the cold can't reach you (player/BodyTemperature)
    public static final DeferredItem<Item> CAMPFIRE_CHILI = gourmet("campfire_chili", 12, 0.7F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.WARMED_UP, 5 * 60 * 20)));
    // Not baked (Cooking Table only). Cooled Off for 5 minutes: you can't get hot (player/BodyTemperature)
    public static final DeferredItem<Item> FROSTY_MELON_SORBET = gourmet("frosty_melon_sorbet", 6, 0.4F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.COOLED_OFF, 5 * 60 * 20)));
    // Skewers give their stick back when eaten
    public static final DeferredItem<Item> UNCOOKED_BEEF_SKEWER = ITEMS.registerSimpleItem("uncooked_beef_skewer", props -> props);
    public static final DeferredItem<Item> BEEF_SKEWER = gourmet("beef_skewer", 12, 0.8F, Items.STICK, List::of);
    public static final DeferredItem<Item> UNCOOKED_CHICKEN_SKEWER = ITEMS.registerSimpleItem("uncooked_chicken_skewer", props -> props);
    public static final DeferredItem<Item> CHICKEN_SKEWER = gourmet("chicken_skewer", 10, 0.7F, Items.STICK, List::of);
    public static final DeferredItem<Item> UNCOOKED_LAMB_SKEWER = ITEMS.registerSimpleItem("uncooked_lamb_skewer", props -> props);
    public static final DeferredItem<Item> LAMB_SKEWER = gourmet("lamb_skewer", 11, 0.8F, Items.STICK, List::of);
    public static final DeferredItem<Item> UNCOOKED_FISH_SKEWER = ITEMS.registerSimpleItem("uncooked_fish_skewer", props -> props);
    public static final DeferredItem<Item> FISH_SKEWER = gourmet("fish_skewer", 10, 0.7F, Items.STICK, List::of);
    // Not baked: made straight at the Cooking Table, already Gourmet.
    public static final DeferredItem<Item> FRUIT_SALAD = gourmet("fruit_salad", 8, 0.5F);
    // The second round of Oven meals; most need a Farmer's fine cut (textures: tools/make_meals.ps1)
    public static final DeferredItem<Item> UNCOOKED_PULLED_PORK_SANDWICH = ITEMS.registerSimpleItem("uncooked_pulled_pork_sandwich", props -> props);
    public static final DeferredItem<Item> PULLED_PORK_SANDWICH = gourmet("pulled_pork_sandwich", 12, 0.75F);
    public static final DeferredItem<Item> UNCOOKED_BRISKET_AND_POTATOES = ITEMS.registerSimpleItem("uncooked_brisket_and_potatoes", props -> props);
    public static final DeferredItem<Item> BRISKET_AND_POTATOES = gourmet("brisket_and_potatoes", 14, 0.8F);
    public static final DeferredItem<Item> UNCOOKED_SHEPHERDS_PIE = ITEMS.registerSimpleItem("uncooked_shepherds_pie", props -> props);
    public static final DeferredItem<Item> SHEPHERDS_PIE = gourmet("shepherds_pie", 14, 0.8F, Items.BOWL, List::of);
    public static final DeferredItem<Item> UNCOOKED_HAM_AND_EGG_BREAKFAST = ITEMS.registerSimpleItem("uncooked_ham_and_egg_breakfast", props -> props);
    public static final DeferredItem<Item> HAM_AND_EGG_BREAKFAST = gourmet("ham_and_egg_breakfast", 12, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_HUNTERS_RABBIT_PIE = ITEMS.registerSimpleItem("uncooked_hunters_rabbit_pie", props -> props);
    public static final DeferredItem<Item> HUNTERS_RABBIT_PIE = gourmet("hunters_rabbit_pie", 13, 0.8F);
    public static final DeferredItem<Item> UNCOOKED_FISH_TACOS = ITEMS.registerSimpleItem("uncooked_fish_tacos", props -> props);
    public static final DeferredItem<Item> FISH_TACOS = gourmet("fish_tacos", 10, 0.7F);
    public static final DeferredItem<Item> UNCOOKED_MUSHROOM_RISOTTO = ITEMS.registerSimpleItem("uncooked_mushroom_risotto", props -> props);
    public static final DeferredItem<Item> MUSHROOM_RISOTTO = gourmet("mushroom_risotto", 11, 0.7F, Items.BOWL, List::of);
    public static final DeferredItem<Item> UNCOOKED_APPLE_CRUMBLE = ITEMS.registerSimpleItem("uncooked_apple_crumble", props -> props);
    public static final DeferredItem<Item> APPLE_CRUMBLE = gourmet("apple_crumble", 9, 0.5F);
    // The second round of Specialty meals: each helps a job (ModEffects; the extras are in player/MealEffects)
    public static final DeferredItem<Item> UNCOOKED_IRONWORKERS_STEW = ITEMS.registerSimpleItem("uncooked_ironworkers_stew", props -> props);
    public static final DeferredItem<Item> IRONWORKERS_STEW = gourmet("ironworkers_stew", 14, 0.8F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.TEMPERED, 5 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_LUMBERJACK_FLAPJACKS = ITEMS.registerSimpleItem("uncooked_lumberjack_flapjacks", props -> props);
    public static final DeferredItem<Item> LUMBERJACK_FLAPJACKS = gourmet("lumberjack_flapjacks", 10, 0.6F, null, () -> List.of(
        new MobEffectInstance(ModEffects.TIMBER, 5 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_BRICK_OVEN_PIZZA = ITEMS.registerSimpleItem("uncooked_brick_oven_pizza", props -> props);
    public static final DeferredItem<Item> BRICK_OVEN_PIZZA = gourmet("brick_oven_pizza", 12, 0.7F, null, () -> List.of(
        new MobEffectInstance(ModEffects.LONG_REACH, 5 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_PRIME_RIB_DINNER = ITEMS.registerSimpleItem("uncooked_prime_rib_dinner", props -> props);
    public static final DeferredItem<Item> PRIME_RIB_DINNER = gourmet("prime_rib_dinner", 16, 0.9F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.HEARTY, 5 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_ANGLERS_FISH_PIE = ITEMS.registerSimpleItem("uncooked_anglers_fish_pie", props -> props);
    public static final DeferredItem<Item> ANGLERS_FISH_PIE = gourmet("anglers_fish_pie", 12, 0.7F, null, () -> List.of(
        new MobEffectInstance(ModEffects.QUICK_BITE, 3 * 60 * 20)));
    // also cures Hunger and warms you back up to normal (player/MealEffects)
    public static final DeferredItem<Item> UNCOOKED_COMFORT_SOUP = ITEMS.registerSimpleItem("uncooked_comfort_soup", props -> props);
    public static final DeferredItem<Item> COMFORT_SOUP = gourmet("comfort_soup", 10, 0.7F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.COMFORTED, 3 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_TRAIL_MIX_BAR = ITEMS.registerSimpleItem("uncooked_trail_mix_bar", props -> props);
    public static final DeferredItem<Item> TRAIL_MIX_BAR = gourmet("trail_mix_bar", 6, 0.5F, null, () -> List.of(
        new MobEffectInstance(ModEffects.SECOND_WIND, 5 * 60 * 20)));
    public static final DeferredItem<Item> UNCOOKED_NIGHT_WATCH_HOTPOT = ITEMS.registerSimpleItem("uncooked_night_watch_hotpot", props -> props);
    public static final DeferredItem<Item> NIGHT_WATCH_HOTPOT = gourmet("night_watch_hotpot", 12, 0.7F, Items.BOWL, () -> List.of(
        new MobEffectInstance(ModEffects.NIGHT_WATCH, 5 * 60 * 20)));

    // What an overcooked Gourmet meal turns into if it's left in the Oven too long. Edible in a pinch: half a drumstick,
    // and no diet points (it isn't in DietValues, so it doesn't change your diet at all).
    public static final DeferredItem<Item> BURNT_FOOD = ITEMS.registerSimpleItem("burnt_food", props -> props.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));

    // Plates (crafting table: 5 iron ingots in a minecart shape + 1 iron nugget in the gap; same with gold). Only Cooks
    // can place, break or serve on them.
    public static final DeferredItem<PlateItem> SILVER_PLATE = ITEMS.registerItem("silver_plate",
        props -> new PlateItem(ModBlocks.SILVER_PLATE.get(), props));
    public static final DeferredItem<PlateItem> GOLDEN_PLATE = ITEMS.registerItem("golden_plate",
        props -> new PlateItem(ModBlocks.GOLDEN_PLATE.get(), props));

    // What a plate turns into once someone has eaten off it: can't be placed, wash it in a Sink.
    public static final DeferredItem<Item> DIRTY_SILVER_PLATE = ITEMS.registerSimpleItem("dirty_silver_plate", props -> props);
    public static final DeferredItem<Item> DIRTY_GOLDEN_PLATE = ITEMS.registerSimpleItem("dirty_golden_plate", props -> props);
    public static final DeferredItem<BlockItem> SINK = ITEMS.registerSimpleBlockItem(ModBlocks.SINK);
    // Butcher Shop Ice: ice that never melts, to show meat on (Farmer.md)
    public static final DeferredItem<BlockItem> BUTCHER_SHOP_ICE = ITEMS.registerSimpleBlockItem(ModBlocks.BUTCHER_SHOP_ICE);

    // The Cook's kitchen
    public static final DeferredItem<BlockItem> SUSHI_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.SUSHI_TABLE);
    public static final DeferredItem<BlockItem> COOKING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.COOKING_TABLE);
    public static final DeferredItem<BlockItem> OVEN = ITEMS.registerSimpleBlockItem(ModBlocks.OVEN);

    // The Weaponsmith's forge
    public static final DeferredItem<BlockItem> WEAPONSMITH_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.WEAPONSMITH_TABLE);
    public static final DeferredItem<BlockItem> WHETSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.WHETSTONE);
    public static final DeferredItem<BlockItem> LAVA_CREATOR = ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_CREATOR);
    public static final DeferredItem<BlockItem> OSMOSIS_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.OSMOSIS_MACHINE);
    // From the Lava Creator: 2 buckets of fuel in a smith table
    public static final DeferredItem<Item> REFINED_LAVA_BUCKET = ITEMS.registerSimpleItem("refined_lava_bucket", props -> props.stacksTo(1));
    public static final DeferredItem<BlockItem> PICKAXE_WHETSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.PICKAXE_WHETSTONE);
    public static final DeferredItem<BlockItem> ARMOR_WHETSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.ARMOR_WHETSTONE);
    public static final DeferredItem<BlockItem> TRANSFER_TUBE = ITEMS.registerSimpleBlockItem(ModBlocks.TRANSFER_TUBE);
    public static final DeferredItem<BlockItem> WISHING_WELL = ITEMS.registerItem("wishing_well",
        props -> new BlockItem(ModBlocks.WISHING_WELL.get(), props), props -> props.stacksTo(1).rarity(Rarity.EPIC).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> MAILBOX = ITEMS.registerItem("mailbox",
        props -> new BlockItem(ModBlocks.MAILBOX.get(), props), props -> props.useBlockDescriptionPrefix());
    // Blood Shards (event/BloodMoon, Events.md): monsters drop them during a Blood Moon; what they buy is still to come
    public static final DeferredItem<Item> BLOOD_SHARD = ITEMS.registerItem("blood_shard", Item::new, props -> props.rarity(Rarity.UNCOMMON));
    // Bottle of Blood (decor/BottleOfBloodItem): filled at the Blood Fountain under a Blood Moon; drink it then for
    // Strength and Speed till dawn, any other time and it makes you sick. Hands back the glass bottle.
    public static final DeferredItem<com.alexh.capitalism.decor.BottleOfBloodItem> BOTTLE_OF_BLOOD = ITEMS.registerItem("bottle_of_blood",
        com.alexh.capitalism.decor.BottleOfBloodItem::new, props -> props
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .craftRemainder(Items.GLASS_BOTTLE)
            .stacksTo(16)
            .rarity(Rarity.UNCOMMON));
    // The rare fishing catches (fishing/RareCatch, Fishing.md): one item each, weighed, named and dated (CatchInfo).
    // The Message in a Bottle and the Sunken Pirate Chest are opened with a right-click.
    public static final java.util.Map<com.alexh.capitalism.fishing.RareCatch, DeferredItem<? extends Item>> RARE_CATCHES = rareCatches();
    // what comes out of them: a Soggy Note (only the Fisherman redeems it), the Pirate Hat (a cosmetic hat)
    public static final DeferredItem<Item> SOGGY_NOTE = ITEMS.registerItem("soggy_note",
        com.alexh.capitalism.fishing.RareFindItems.SoggyNote::new, props -> props.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PIRATE_HAT = ITEMS.registerItem("pirate_hat",
        com.alexh.capitalism.fishing.RareFindItems.PirateHat::new, props -> props.stacksTo(1).rarity(Rarity.RARE)
            // an equipment asset with no layers (assets/capitalism/equipment/pirate_hat.json): so the game treats it as worn
            // headgear (not an item stuck on the head) and client/PirateHatLayer draws the 3D hat
            .component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
                .setAsset(net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, "pirate_hat")))
                .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER).build()));
    // cosmetic hats for roleplaying a job (only from the Traveling Merchant): worn in the head slot, no armour, no effect;
    // drawn by client/CosmeticHatLayer (the same empty equipment asset as the Pirate Hat)
    public static final DeferredItem<Item> CHEFS_HAT = cosmeticHat("chefs_hat");
    public static final DeferredItem<Item> WELDERS_MASK = cosmeticHat("welders_mask");
    public static final DeferredItem<Item> WIZARDS_HAT = cosmeticHat("wizards_hat");
    public static final DeferredItem<Item> FARMERS_HAT = cosmeticHat("farmers_hat");
    public static final DeferredItem<Item> TRAVELERS_PIPE = cosmeticHat("travelers_pipe");
    // the Blood Merchant's Ghost Sheet (Halloween): a cosmetic like these, but hostile mobs spot you from 25% less far (player/SpookyGear)
    public static final DeferredItem<Item> GHOST_SHEET = cosmeticHat("ghost_sheet");
    // the Blood Merchant's Witch's Broom (Halloween): right-click to float up for a moment, then drift down (player/SpookyGear)
    public static final DeferredItem<Item> WITCHS_BROOM = ITEMS.registerItem("witchs_broom", com.alexh.capitalism.player.WitchsBroomItem::new,
        props -> props.stacksTo(1).rarity(Rarity.RARE));

    private static DeferredItem<Item> cosmeticHat(String id) {
        return ITEMS.registerSimpleItem(id, props -> props.stacksTo(1).rarity(Rarity.RARE)
            .component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
                .setAsset(net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, "pirate_hat")))
                .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER).build()));
    }

    // a rare catch mounted on a wall plaque by the Fisherman (fishing/FishPlaqueBlock)
    public static final DeferredItem<BlockItem> FISH_PLAQUE = ITEMS.registerItem("fish_plaque",
        props -> new com.alexh.capitalism.fishing.FishPlaqueItem(ModBlocks.FISH_PLAQUE.get(), props),
        props -> props.stacksTo(1).rarity(Rarity.RARE).useBlockDescriptionPrefix());

    private static java.util.Map<com.alexh.capitalism.fishing.RareCatch, DeferredItem<? extends Item>> rareCatches() {
        java.util.Map<com.alexh.capitalism.fishing.RareCatch, DeferredItem<? extends Item>> out = new java.util.EnumMap<>(com.alexh.capitalism.fishing.RareCatch.class);
        for (com.alexh.capitalism.fishing.RareCatch c : com.alexh.capitalism.fishing.RareCatch.values()) {
            Rarity rarity = c.chance >= 1000 ? Rarity.EPIC : c.chance >= 300 ? Rarity.RARE : Rarity.UNCOMMON;
            DeferredItem<? extends Item> item = switch (c) {
                case MESSAGE_IN_A_BOTTLE -> ITEMS.registerItem(c.id, com.alexh.capitalism.fishing.RareFindItems.MessageInABottle::new,
                    props -> props.stacksTo(16).rarity(rarity));
                case SUNKEN_PIRATE_CHEST -> ITEMS.registerItem(c.id, com.alexh.capitalism.fishing.RareFindItems.PirateChest::new,
                    props -> props.stacksTo(16).rarity(rarity));
                default -> ITEMS.registerItem(c.id, props -> new com.alexh.capitalism.fishing.RareCatchItem(c, props),
                    props -> props.stacksTo(1).rarity(rarity));
            };
            out.put(c, item);
        }
        return out;
    }
    // The Blood Moon Crest (Events.md > Trophies): each boss drops its third of it; the three are
    // joined (for 50 Blood Shards) at the Blood Merchant (town/BloodMerchant), who takes it for an Essential Wishing Star (once per player)
    public static final DeferredItem<Item> REVENANT_CREST = ITEMS.registerItem("revenant_crest", Item::new, props -> props.rarity(Rarity.EPIC).stacksTo(16));
    public static final DeferredItem<Item> SANGUINE_CREST = ITEMS.registerItem("sanguine_crest", Item::new, props -> props.rarity(Rarity.EPIC).stacksTo(16));
    public static final DeferredItem<Item> GOREFANG_CREST = ITEMS.registerItem("gorefang_crest", Item::new, props -> props.rarity(Rarity.EPIC).stacksTo(16));
    public static final DeferredItem<Item> BLOOD_MOON_CREST = ITEMS.registerItem("blood_moon_crest", Item::new,
        props -> props.rarity(Rarity.EPIC).stacksTo(1).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<BlockItem> TOWN_HALL_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.TOWN_HALL_SIGN);
    public static final DeferredItem<BlockItem> BANK_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.BANK_SIGN);
    public static final DeferredItem<BlockItem> BOAT_SHOP_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.BOAT_SHOP_SIGN);
    public static final DeferredItem<BlockItem> TOWN_MAILBOX = ITEMS.registerItem("town_mailbox",
        props -> new BlockItem(ModBlocks.TOWN_MAILBOX.get(), props), props -> props.stacksTo(1).rarity(Rarity.RARE).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> MAYORAL_CHEST = ITEMS.registerItem("mayoral_chest",
        props -> new BlockItem(ModBlocks.MAYORAL_CHEST.get(), props), props -> props.stacksTo(1).rarity(Rarity.EPIC).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> INVESTMENT_CHEST = ITEMS.registerItem("investment_chest",
        props -> new BlockItem(ModBlocks.INVESTMENT_CHEST.get(), props), props -> props.stacksTo(1).rarity(Rarity.EPIC).useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> ENCHANTERS_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ENCHANTERS_TABLE);
    // Made at the Enchanter's Table (Book + Blaze Rod); used there to burn one enchantment off an item (enchanter/EnchantRemoval)
    public static final DeferredItem<Item> ENCH_REMOVAL = ITEMS.registerSimpleItem("ench_removal", props -> props.stacksTo(16).fireResistant());
    // Made at the Enchanter's Table (Bamboo + Potion of Poison): turns an armor stand in a full armor set into an
    // Apprentice (enchanter/ApprenticeWand). 4 uses.
    public static final DeferredItem<Item> APPRENTICE_WAND = ITEMS.registerSimpleItem("apprentice_wand", props -> props.durability(4));
    // A picked-up Apprentice, holding its armor and XP (enchanter/ApprenticeItem). Only made by picking one up.
    public static final DeferredItem<ApprenticeItem> APPRENTICE = ITEMS.registerItem("apprentice", ApprenticeItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<BlockItem> LAPIS_COMPRESSOR = ITEMS.registerSimpleBlockItem(ModBlocks.LAPIS_COMPRESSOR);
    // ── Librarian (Librarian.md) ──
    public static final DeferredItem<BlockItem> STITCHING_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.STITCHING_TABLE);
    public static final DeferredItem<BlockItem> LIBRARIAN_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.LIBRARIAN_TABLE);
    public static final DeferredItem<BlockItem> KNOWLEDGE_CREATOR = ITEMS.registerSimpleBlockItem(ModBlocks.KNOWLEDGE_CREATOR);
    // Sewn at the Stitching Table: Iron Ingot thread makes a Rough Book, Diamond thread a Delicate Book. Written into
    // skill books at the Librarian Table.
    public static final DeferredItem<Item> ROUGH_BOOK = ITEMS.registerSimpleItem("rough_book", props -> props.stacksTo(16));
    public static final DeferredItem<Item> DELICATE_BOOK = ITEMS.registerSimpleItem("delicate_book", props -> props.stacksTo(16));
    // Only drawn: the needle bobbing on a Stitching Table while it sews (client/StitchingTableRenderer). Not obtainable.
    public static final DeferredItem<Item> STITCHING_NEEDLE = ITEMS.registerSimpleItem("stitching_needle", props -> props);
    // What a skill book is written from (with a Rough / Delicate Book). Made in the Knowledge Creator from an Enchanted
    // Book, or bought from the Whisperer. Faded / Clear / Bright / Radiant (librarian/KnowledgeQuality).
    public static final DeferredItem<com.alexh.capitalism.librarian.KnowledgePieceItem> KNOWLEDGE_PIECE = ITEMS.registerItem("knowledge_piece",
        com.alexh.capitalism.librarian.KnowledgePieceItem::new, props -> props.stacksTo(64));
    // A written skill book: read it to learn something (librarian/SkillBookItem). Every one is in the creative tab for testing.
    public static final DeferredItem<com.alexh.capitalism.librarian.SkillBookItem> SKILL_BOOK = ITEMS.registerItem("skill_book",
        com.alexh.capitalism.librarian.SkillBookItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<BlockItem> TOOLSMITH_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.TOOLSMITH_TABLE);
    // Pet Trainer (pets/Pets): the Ownership Tag claims an animal (anyone can use one), the Training Stick trains a claimed
    // animal into a pet (Pet Trainers only, 5 uses)
    public static final DeferredItem<com.alexh.capitalism.pets.OwnershipTagItem> OWNERSHIP_TAG = ITEMS.registerItem("ownership_tag",
        com.alexh.capitalism.pets.OwnershipTagItem::new, props -> props.stacksTo(64));
    // The Animal Bjorn: carries an animal, worn in the Back slot (pets/AnimalBjornItem, back/Back)
    public static final DeferredItem<com.alexh.capitalism.pets.AnimalBjornItem> ANIMAL_BJORN = ITEMS.registerItem("animal_bjorn",
        com.alexh.capitalism.pets.AnimalBjornItem::new, props -> props.stacksTo(1));
    // Training Feed (pets/TrainingFeed): Cooks make it (seeds + raw meat or fish = 6); an animal on the pet list fed one
    // follows you for a few seconds
    public static final DeferredItem<Item> TRAINING_FEED = ITEMS.registerSimpleItem("training_feed", props -> props.stacksTo(64));
    // ── The Designer (designer/, Designer.md): bags worn on the back (designer/Bag; the Backpack also goes into the
    // Animal Bjorn), the Patches they're made of (sewn at the Sewing Machine), the Iron and Diamond Sewing Needles (one breaks
    // for every slip) and the two blocks ──
    public static final DeferredItem<Item> BACKPACK = ITEMS.registerSimpleItem("backpack", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FOOD_SATCHEL = ITEMS.registerSimpleItem("food_satchel", props -> props.stacksTo(1));
    public static final DeferredItem<Item> SEED_SATCHEL = ITEMS.registerSimpleItem("seed_satchel", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FANCY_BACKPACK = ITEMS.registerSimpleItem("fancy_backpack", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FANCY_FOOD_SATCHEL = ITEMS.registerSimpleItem("fancy_food_satchel", props -> props.stacksTo(1));
    public static final DeferredItem<Item> FANCY_SEED_SATCHEL = ITEMS.registerSimpleItem("fancy_seed_satchel", props -> props.stacksTo(1));
    public static final DeferredItem<Item> UNIFORM_BACKPACK = ITEMS.registerSimpleItem("uniform_backpack", props -> props.stacksTo(1));
    public static final DeferredItem<Item> GUCCI_BAG = ITEMS.registerSimpleItem("gucci_bag", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<Item> BURBERRY_BAG = ITEMS.registerSimpleItem("burberry_bag", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<Item> LOUIS_VUITTON_BAG = ITEMS.registerSimpleItem("louis_vuitton_bag", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<Item> PRADA_BAG = ITEMS.registerSimpleItem("prada_bag", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    // pet hats (designer/Hat): worn by a pet, set in its window
    // only the Traveling Merchant sells it: the pet wearing it is always full and never thirsty (designer/Hat, pets/Pets)
    public static final DeferredItem<Item> FEEDING_HAT = ITEMS.registerSimpleItem("feeding_hat", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<Item> GUCCI_TOP_HAT = ITEMS.registerSimpleItem("gucci_top_hat", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> BURBERRY_SANTA_HAT = ITEMS.registerSimpleItem("burberry_santa_hat", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> LOUIS_VUITTON_CAP = ITEMS.registerSimpleItem("louis_vuitton_cap", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> PRADA_FEDORA = ITEMS.registerSimpleItem("prada_fedora", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> LEATHER_PATCH = ITEMS.registerSimpleItem("leather_patch", props -> props);
    public static final DeferredItem<Item> STRAP_PATCH = ITEMS.registerSimpleItem("strap_patch", props -> props);
    public static final DeferredItem<Item> PANTRY_PATCH = ITEMS.registerSimpleItem("pantry_patch", props -> props);
    public static final DeferredItem<Item> SEED_POCKET_PATCH = ITEMS.registerSimpleItem("seed_pocket_patch", props -> props);
    public static final DeferredItem<Item> VELVET_PATCH = ITEMS.registerSimpleItem("velvet_patch", props -> props);
    public static final DeferredItem<Item> EMBROIDERED_PATCH = ITEMS.registerSimpleItem("embroidered_patch", props -> props);
    public static final DeferredItem<Item> GOLD_TRIM_PATCH = ITEMS.registerSimpleItem("gold_trim_patch", props -> props);
    public static final DeferredItem<Item> SILK_PATCH = ITEMS.registerSimpleItem("silk_patch", props -> props);
    public static final DeferredItem<Item> GUCCI_PATCH = ITEMS.registerSimpleItem("gucci_patch", props -> props.rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> BURBERRY_PATCH = ITEMS.registerSimpleItem("burberry_patch", props -> props.rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> LOUIS_VUITTON_PATCH = ITEMS.registerSimpleItem("louis_vuitton_patch", props -> props.rarity(net.minecraft.world.item.Rarity.RARE));
    public static final DeferredItem<Item> PRADA_PATCH = ITEMS.registerSimpleItem("prada_patch", props -> props.rarity(net.minecraft.world.item.Rarity.RARE));
    // Utility patches and bags (designer/Pattern.Tier.UTILITY, Bag.Line.UTILITY)
    public static final DeferredItem<Item> FROZEN_PATCH = ITEMS.registerSimpleItem("frozen_patch", props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final DeferredItem<Item> FREEZER_SLING = ITEMS.registerSimpleItem("freezer_sling", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    // the Traveler's Bag: only the Traveling Merchant sells it (no Designer can make it); 6 slots, 12 far from town (designer/TravelersBag)
    public static final DeferredItem<Item> TRAVELERS_BAG = ITEMS.registerSimpleItem("travelers_bag", props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    // sewing needles: one breaks for every slip in the needle game (Luxury patches need Diamond ones)
    public static final DeferredItem<Item> IRON_SEWING_NEEDLE = ITEMS.registerSimpleItem("iron_sewing_needle", props -> props.stacksTo(64));
    public static final DeferredItem<Item> DIAMOND_SEWING_NEEDLE = ITEMS.registerSimpleItem("diamond_sewing_needle", props -> props.stacksTo(64));
    public static final DeferredItem<BlockItem> DESIGNER_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.DESIGNER_TABLE);
    public static final DeferredItem<BlockItem> SEWING_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.SEWING_MACHINE);
    // The Farmer: the Decomposter, its Fertilizers (Yield and Super Yield: more crops; Fast: faster growth), the Scythes (cut a 3x2 / 5x3 patch)
    public static final DeferredItem<BlockItem> DECOMPOSTER = ITEMS.registerSimpleBlockItem(ModBlocks.DECOMPOSTER);
    public static final DeferredItem<com.alexh.capitalism.farmer.FertilizerItem> FERTILIZER = ITEMS.registerItem("fertilizer",
        props -> new com.alexh.capitalism.farmer.FertilizerItem(props, com.alexh.capitalism.farmer.FertilizerItem.Kind.YIELD));
    public static final DeferredItem<com.alexh.capitalism.farmer.FertilizerItem> SUPER_FERTILIZER = ITEMS.registerItem("super_fertilizer",
        props -> new com.alexh.capitalism.farmer.FertilizerItem(props.rarity(net.minecraft.world.item.Rarity.UNCOMMON), com.alexh.capitalism.farmer.FertilizerItem.Kind.SUPER_YIELD));
    public static final DeferredItem<com.alexh.capitalism.farmer.FertilizerItem> FAST_FERTILIZER = ITEMS.registerItem("fast_fertilizer",
        props -> new com.alexh.capitalism.farmer.FertilizerItem(props, com.alexh.capitalism.farmer.FertilizerItem.Kind.FAST));
    public static final DeferredItem<com.alexh.capitalism.farmer.ScytheItem> SCYTHE = ITEMS.registerItem("scythe",
        com.alexh.capitalism.farmer.ScytheItem::iron, props -> props.durability(com.alexh.capitalism.farmer.ScytheItem.USES)
            .repairable(net.minecraft.tags.ItemTags.IRON_TOOL_MATERIALS));   // repaired at the Toolsmith Table only
    public static final DeferredItem<com.alexh.capitalism.farmer.ScytheItem> DIAMOND_SCYTHE = ITEMS.registerItem("diamond_scythe",
        com.alexh.capitalism.farmer.ScytheItem::diamond, props -> props.durability(com.alexh.capitalism.farmer.ScytheItem.DIAMOND_USES)
            .repairable(net.minecraft.tags.ItemTags.DIAMOND_TOOL_MATERIALS));
    // what meat turns into when its time is up (farmer/Freshness): pet food and fertilizer, not people food
    public static final DeferredItem<Item> ROTTEN_MEAT = ITEMS.registerSimpleItem("rotten_meat");
    // Real Estate (realestate/): the Survey Stake (ops mark out lots) and the signs
    public static final DeferredItem<com.alexh.capitalism.realestate.SurveyStakeItem> SURVEY_STAKE = ITEMS.registerItem("survey_stake",
        com.alexh.capitalism.realestate.SurveyStakeItem::new, props -> props.rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<BlockItem> CITY_SALE_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.CITY_SALE_SIGN,
        props -> props.rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<BlockItem> FOR_SALE_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.FOR_SALE_SIGN);
    public static final DeferredItem<BlockItem> CITY_CHEST = ITEMS.registerSimpleBlockItem(ModBlocks.CITY_CHEST,
        props -> props.rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredItem<BlockItem> FOR_LEASE_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.FOR_LEASE_SIGN);
    // The Premium boats (boats/PremiumBoat, Boats.md): the Boat Master's luxuries. One to a stack, they don't burn
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> PIRATE_SHIP = premiumBoat("pirate_ship",
        () -> ModEntities.PIRATE_SHIP.get(), "A pirate galleon, cannons and all", "Seats four.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> SPEEDBOAT = premiumBoat("speedboat",
        () -> ModEntities.SPEEDBOAT.get(), "A sleek runabout with twin outboards", "Seats four.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> YACHT = premiumBoat("yacht",
        () -> ModEntities.YACHT.get(), "A superyacht, hot tub included", "Seats six.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> LONGSHIP = premiumBoat("longship",
        () -> ModEntities.LONGSHIP.get(), "A Viking dragon ship", "Seats six.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> PADDLEWHEELER = premiumBoat("paddlewheeler",
        () -> ModEntities.PADDLEWHEELER.get(), "Brass, gears and steam", "Seats three.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> GHOST_SHIP = premiumBoat("ghost_ship",
        () -> ModEntities.GHOST_SHIP.get(), "A galleon come back from the deep", "Seats four.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> SWAMP_RAFT = premiumBoat("swamp_raft",
        () -> ModEntities.SWAMP_RAFT.get(), "A smuggler's raft from the bayou", "Seats four.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> STEAMBOAT = premiumBoat("steamboat",
        () -> ModEntities.STEAMBOAT.get(), "A grand riverboat with a big red sternwheel", "Seats five.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> LEVIATHAN = premiumBoat("leviathan",
        () -> ModEntities.LEVIATHAN.get(), "A deck built inside a sea monster's bones", "Seats four.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> TEAPOT = premiumBoat("teapot",
        () -> ModEntities.TEAPOT.get(), "Tea time on the water", "Seats three, in the pot.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> SUBMERSIBLE = premiumBoat("submersible",
        () -> ModEntities.SUBMERSIBLE.get(), "Sit under the waves and see clearly", "Seats one, in the dome. F: flashlight.");
    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> CASTAWAY_TUB = premiumBoat("castaway_tub",
        () -> ModEntities.CASTAWAY_TUB.get(), "Half a barrel, half full of water", "Seats one.");

    public static final DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> FAIRY_BOAT = premiumBoat("fairy_boat",
        () -> ModEntities.FAIRY_BOAT.get(), "A flower-covered boat with fairy wings", "Seats three. Space: flutter up into the air.");

    // The seasonal decorations (decor/, Decorations.md) and the String Lights to hang between the hooks
    public static final DeferredItem<BlockItem> CHRISTMAS_TREE = ITEMS.registerSimpleBlockItem(ModBlocks.CHRISTMAS_TREE, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> STRING_LIGHT_HOOK = ITEMS.registerSimpleBlockItem(ModBlocks.STRING_LIGHT_HOOK);
    public static final DeferredItem<BlockItem> PRESENT_PILE = ITEMS.registerSimpleBlockItem(ModBlocks.PRESENT_PILE);
    public static final DeferredItem<BlockItem> CANDY_CANE_POLE = ITEMS.registerSimpleBlockItem(ModBlocks.CANDY_CANE_POLE);
    public static final DeferredItem<BlockItem> CANDY_CANES = ITEMS.registerSimpleBlockItem(ModBlocks.CANDY_CANES);
    public static final DeferredItem<BlockItem> CHRISTMAS_FROST = ITEMS.registerSimpleBlockItem(ModBlocks.CHRISTMAS_FROST);
    public static final DeferredItem<BlockItem> ICICLES = ITEMS.registerSimpleBlockItem(ModBlocks.ICICLES);
    public static final DeferredItem<BlockItem> SPOOKY_SCARECROW = ITEMS.registerSimpleBlockItem(ModBlocks.SPOOKY_SCARECROW, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> WITCHS_CAULDRON = ITEMS.registerSimpleBlockItem(ModBlocks.WITCHS_CAULDRON, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> GRAVESTONE_ROUND = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVESTONE_ROUND);
    public static final DeferredItem<BlockItem> GRAVESTONE_CROSS = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVESTONE_CROSS);
    public static final DeferredItem<BlockItem> GRAVESTONE_TOMB = ITEMS.registerSimpleBlockItem(ModBlocks.GRAVESTONE_TOMB);
    public static final DeferredItem<BlockItem> BONE_PILE = ITEMS.registerSimpleBlockItem(ModBlocks.BONE_PILE);
    public static final DeferredItem<BlockItem> GROUND_FOG = ITEMS.registerSimpleBlockItem(ModBlocks.GROUND_FOG);
    public static final DeferredItem<BlockItem> AUTUMN_MAPLE = ITEMS.registerSimpleBlockItem(ModBlocks.AUTUMN_MAPLE);
    public static final DeferredItem<BlockItem> AUTUMN_BIRCH = ITEMS.registerSimpleBlockItem(ModBlocks.AUTUMN_BIRCH);
    public static final DeferredItem<BlockItem> FALLEN_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.FALLEN_LEAVES);
    public static final DeferredItem<BlockItem> BEACH_TOWEL = ITEMS.registerSimpleBlockItem(ModBlocks.BEACH_TOWEL);
    public static final DeferredItem<BlockItem> BEACH_BALL = ITEMS.registerSimpleBlockItem(ModBlocks.BEACH_BALL);
    public static final DeferredItem<BlockItem> BEACH_UMBRELLA = ITEMS.registerSimpleBlockItem(ModBlocks.BEACH_UMBRELLA);
    public static final DeferredItem<BlockItem> BEACH_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.BEACH_CHAIR);
    public static final DeferredItem<BlockItem> EGG_BASKET = ITEMS.registerSimpleBlockItem(ModBlocks.EGG_BASKET);
    public static final DeferredItem<BlockItem> GIANT_EGG = ITEMS.registerSimpleBlockItem(ModBlocks.GIANT_EGG);
    public static final DeferredItem<BlockItem> SPRING_BUNNY = ITEMS.registerSimpleBlockItem(ModBlocks.SPRING_BUNNY);
    public static final DeferredItem<BlockItem> TULIP_BED = ITEMS.registerSimpleBlockItem(ModBlocks.TULIP_BED);
    public static final DeferredItem<BlockItem> PUMPKIN_PILE = ITEMS.registerSimpleBlockItem(ModBlocks.PUMPKIN_PILE);
    public static final DeferredItem<BlockItem> LEAF_PILE = ITEMS.registerSimpleBlockItem(ModBlocks.LEAF_PILE);
    // The Blood Moon decorations (decor/BloodDecorKind, Decorations.md): the Night Merchant's, for Blood Shards
    public static final DeferredItem<BlockItem> BLOOD_MOON_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_MOON_LANTERN, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRIMSON_CANDELABRA = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_CANDELABRA, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRIMSON_THRONE = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_THRONE, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> VAMPIRE_COFFIN = ITEMS.registerSimpleBlockItem(ModBlocks.VAMPIRE_COFFIN, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> BLOOD_FOUNTAIN = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_FOUNTAIN, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BLOOD_MOON_BANNER = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_MOON_BANNER, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRIMSON_RUG = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_RUG, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> REVENANT_GREATSWORD_STATUE = ITEMS.registerSimpleBlockItem(ModBlocks.REVENANT_GREATSWORD_STATUE, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> GOREFANG_SKULL_TROPHY = ITEMS.registerSimpleBlockItem(ModBlocks.GOREFANG_SKULL_TROPHY, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> SANGUINE_STAFF_PEDESTAL = ITEMS.registerSimpleBlockItem(ModBlocks.SANGUINE_STAFF_PEDESTAL, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BAT_GARGOYLE = ITEMS.registerSimpleBlockItem(ModBlocks.BAT_GARGOYLE, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> GRANDFATHER_CLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GRANDFATHER_CLOCK, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> CRIMSON_CHANDELIER = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_CHANDELIER, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BLOOD_PIPE_ORGAN = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_PIPE_ORGAN, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> SECRET_BOOKSHELF_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.SECRET_BOOKSHELF_DOOR, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> HAUNTED_PORTRAIT = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_PORTRAIT, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> IRON_MAIDEN = ITEMS.registerSimpleBlockItem(ModBlocks.IRON_MAIDEN, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BLOOD_MOON_ALTAR = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_MOON_ALTAR, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BAT_ROOST = ITEMS.registerSimpleBlockItem(ModBlocks.BAT_ROOST, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CRIMSON_CANOPY_BED = ITEMS.registerSimpleBlockItem(ModBlocks.CRIMSON_CANOPY_BED, props -> props.rarity(Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<BlockItem> BLOOD_CRYSTAL_FIREPLACE = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_CRYSTAL_FIREPLACE, props -> props.rarity(Rarity.EPIC));
    // the Halloween ones (the Blood Merchant's Spooky tab, Town.md), and the two spooky things to wear and ride
    public static final DeferredItem<BlockItem> GRINNING_JACK_O_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.GRINNING_JACK_O_LANTERN, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> HAUNTED_TOMBSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.HAUNTED_TOMBSTONE, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> POSSESSED_SCARECROW = ITEMS.registerSimpleBlockItem(ModBlocks.POSSESSED_SCARECROW, props -> props.rarity(Rarity.RARE));
    // Blood Moon building blocks (the Blood Merchant sells them for 1 Blood Shard each)
    public static final DeferredItem<BlockItem> BLOODSTONE_BRICKS = ITEMS.registerSimpleBlockItem(ModBlocks.BLOODSTONE_BRICKS);
    public static final DeferredItem<BlockItem> BLOOD_CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_CRYSTAL_BLOCK);
    // Wishing Stars and the Changing Star (Jobs.md): a job slot from the Wishing Well; changing your jobs (admins hand it out)
    public static final DeferredItem<com.alexh.capitalism.occupation.WishingStarItem> FUN_WISHING_STAR = ITEMS.registerItem("fun_wishing_star",
        props -> new com.alexh.capitalism.occupation.WishingStarItem(com.alexh.capitalism.occupation.Job.Category.FUN, props),
        props -> props.stacksTo(16).rarity(Rarity.RARE).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<com.alexh.capitalism.occupation.WishingStarItem> ESSENTIAL_WISHING_STAR = ITEMS.registerItem("essential_wishing_star",
        props -> new com.alexh.capitalism.occupation.WishingStarItem(com.alexh.capitalism.occupation.Job.Category.ESSENTIAL, props),
        props -> props.stacksTo(16).rarity(Rarity.EPIC).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<com.alexh.capitalism.occupation.ChangingStar> CHANGING_STAR = ITEMS.registerItem("changing_star",
        com.alexh.capitalism.occupation.ChangingStar::new,
        props -> props.stacksTo(16).rarity(Rarity.EPIC).component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<com.alexh.capitalism.decor.ChristmasPresentItem> CHRISTMAS_PRESENT = ITEMS.registerItem("christmas_present",
        com.alexh.capitalism.decor.ChristmasPresentItem::new, props -> props.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<com.alexh.capitalism.decor.StringLightsItem> STRING_LIGHTS = ITEMS.registerItem("string_lights",
        com.alexh.capitalism.decor.StringLightsItem::new, props -> props.stacksTo(1));

    private static DeferredItem<com.alexh.capitalism.boats.PremiumBoatItem> premiumBoat(String name,
            Supplier<? extends net.minecraft.world.entity.EntityType<? extends com.alexh.capitalism.boats.PremiumBoat>> type, String what, String seats) {
        return ITEMS.registerItem(name, props -> new com.alexh.capitalism.boats.PremiumBoatItem(type, props),
            props -> props.stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                    net.minecraft.network.chat.Component.literal(what)
                        .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GOLD).withItalic(false)),
                    net.minecraft.network.chat.Component.literal("10% faster than a boat. " + seats)
                        .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false)),
                    net.minecraft.network.chat.Component.literal("Locked to you: sneak + right-click to share it.")
                        .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.DARK_GRAY).withItalic(false))))));
    }
    // the Grill (farmer/GrillBlock)
    public static final DeferredItem<BlockItem> GRILL = ITEMS.registerSimpleBlockItem(ModBlocks.GRILL);
    // the Fridge and Freezer (farmer/ColdStorageBlock)
    public static final DeferredItem<BlockItem> FRIDGE = ITEMS.registerSimpleBlockItem(ModBlocks.FRIDGE);
    public static final DeferredItem<BlockItem> FREEZER = ITEMS.registerSimpleBlockItem(ModBlocks.FREEZER);
    // the Butcher Knives (farmer/ButcherKnifeItem): Toolsmiths make them, Farmers cut slabs up with them
    public static final DeferredItem<com.alexh.capitalism.farmer.ButcherKnifeItem> IRON_BUTCHER_KNIFE = ITEMS.registerItem("iron_butcher_knife",
        com.alexh.capitalism.farmer.ButcherKnifeItem::iron);
    public static final DeferredItem<com.alexh.capitalism.farmer.ButcherKnifeItem> DIAMOND_BUTCHER_KNIFE = ITEMS.registerItem("diamond_butcher_knife",
        com.alexh.capitalism.farmer.ButcherKnifeItem::diamond, props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    // Butchering (farmer/MeatCut): the Butcher's Table, the Meat Slabs Farmers get from animals, and the cuts a slab
    // gives when it's cut up well (raw: eaten like the plain meat; Cooks will cook them)
    public static final DeferredItem<BlockItem> BUTCHERS_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.BUTCHERS_TABLE);
    public static final Map<com.alexh.capitalism.farmer.MeatCut, DeferredItem<com.alexh.capitalism.farmer.MeatItems.Slab>> MEAT_SLABS = new EnumMap<>(com.alexh.capitalism.farmer.MeatCut.class);
    public static final Map<com.alexh.capitalism.farmer.MeatCut, DeferredItem<com.alexh.capitalism.farmer.MeatItems.Cut>> MEAT_CUTS = new EnumMap<>(com.alexh.capitalism.farmer.MeatCut.class);
    // a fine cut seared on the Grill (farmer/Sear: its food value is set per steak)
    public static final Map<com.alexh.capitalism.farmer.MeatCut, DeferredItem<Item>> SEARED_CUTS = new EnumMap<>(com.alexh.capitalism.farmer.MeatCut.class);
    static {
        for (com.alexh.capitalism.farmer.MeatCut cut : com.alexh.capitalism.farmer.MeatCut.values()) {
            MEAT_SLABS.put(cut, ITEMS.registerItem(cut.id + "_slab",
                props -> new com.alexh.capitalism.farmer.MeatItems.Slab(ModBlocks.MEAT_SLABS.get(cut).get(), props.useBlockDescriptionPrefix())));
            MEAT_CUTS.put(cut, ITEMS.registerItem("raw_" + cut.id, com.alexh.capitalism.farmer.MeatItems.Cut::new, props -> switch (cut.animal) {
                case COW -> props.food(net.minecraft.world.food.Foods.BEEF);
                case PIG -> props.food(net.minecraft.world.food.Foods.PORKCHOP);
                case SHEEP -> props.food(net.minecraft.world.food.Foods.MUTTON);
                case CHICKEN -> props.food(net.minecraft.world.food.Foods.CHICKEN, Consumables.CHICKEN);
                case RABBIT -> props.food(net.minecraft.world.food.Foods.RABBIT);
            }));
            SEARED_CUTS.put(cut, ITEMS.registerSimpleItem("seared_" + cut.id, props -> props.stacksTo(16)
                .food(new net.minecraft.world.food.FoodProperties(8, 12.8F, true))));
        }
    }
    public static final DeferredItem<com.alexh.capitalism.pets.TrainingStickItem> TRAINING_STICK = ITEMS.registerItem("training_stick",
        com.alexh.capitalism.pets.TrainingStickItem::new, props -> props.durability(com.alexh.capitalism.pets.TrainingStickItem.USES));
    public static final DeferredItem<BlockItem> SMALL_FOOD_TROUGH = ITEMS.registerSimpleBlockItem(ModBlocks.SMALL_FOOD_TROUGH);
    public static final DeferredItem<BlockItem> SMALL_WATER_BOWL = ITEMS.registerSimpleBlockItem(ModBlocks.SMALL_WATER_BOWL);
    public static final DeferredItem<BlockItem> MEDIUM_FOOD_TROUGH = ITEMS.registerSimpleBlockItem(ModBlocks.MEDIUM_FOOD_TROUGH);
    public static final DeferredItem<BlockItem> MEDIUM_WATER_BOWL = ITEMS.registerSimpleBlockItem(ModBlocks.MEDIUM_WATER_BOWL);
    public static final DeferredItem<BlockItem> LARGE_FOOD_TROUGH = ITEMS.registerSimpleBlockItem(ModBlocks.LARGE_FOOD_TROUGH);
    public static final DeferredItem<BlockItem> LARGE_WATER_BOWL = ITEMS.registerSimpleBlockItem(ModBlocks.LARGE_WATER_BOWL);
    public static final DeferredItem<BlockItem> ARMORSMITH_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ARMORSMITH_TABLE);
    // Compacted metals: 2 of the material side by side. What everyone but a Weaponsmith / Armorsmith needs
    // for iron and diamond swords, pickaxes and armor.
    public static final DeferredItem<Item> COMPACTED_DIAMOND = ITEMS.registerSimpleItem("compacted_diamond", props -> props);
    // 2 Iron Ingots side by side.
    public static final DeferredItem<Item> COMPACTED_IRON = ITEMS.registerSimpleItem("compacted_iron", props -> props);
    // Molten Socket: a rare mob drop (1%); a smith sets it into a sword, pickaxe, axe or armor piece for 30 XP levels
    // (weaponsmith/Socketing). Purple name, survives lava.
    public static final DeferredItem<Item> MOLTEN_SOCKET = ITEMS.registerSimpleItem("molten_socket",
        props -> props.rarity(Rarity.EPIC).fireResistant());
    // ── only the Traveling Merchant sells these (Town.md > The Traveling Merchant) ──
    // the Purifying Waterskin: 16 sips, refill from any water and it's purified (town/BottomlessWaterskinItem, core/ModServerEvents)
    public static final DeferredItem<com.alexh.capitalism.town.BottomlessWaterskinItem> BOTTOMLESS_WATERSKIN = ITEMS.registerItem("bottomless_waterskin",
        com.alexh.capitalism.town.BottomlessWaterskinItem::new, props -> props.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK).durability(com.alexh.capitalism.town.BottomlessWaterskinItem.SIPS).rarity(Rarity.EPIC));
    // in your offhand the cold can't reach you; burns out after HAND_WARMER_SECONDS of keeping you warm (player/BodyTemperature)
    public static final int HAND_WARMER_SECONDS = 20 * 60;
    public static final DeferredItem<Item> HAND_WARMER = ITEMS.registerSimpleItem("hand_warmer", props -> props.durability(HAND_WARMER_SECONDS).rarity(Rarity.RARE));
    // right-click a socketed item with it (in your inventory): the Molten Socket comes back out (gem too); used up (weaponsmith/Socketing)
    public static final DeferredItem<Item> SMITHS_CHISEL = ITEMS.registerSimpleItem("smiths_chisel", props -> props.stacksTo(1).rarity(Rarity.EPIC));
    // in your offhand when you die: no death tax that time; it burns away (deathtax/DeathTax)
    public static final DeferredItem<Item> PHOENIX_FEATHER = ITEMS.registerSimpleItem("phoenix_feather", props -> props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<net.minecraft.world.item.DoubleHighBlockItem> MAYORAL_DOOR = ITEMS.registerItem("mayoral_door",
        props -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.MAYORAL_DOOR.get(), props), props -> props.rarity(Rarity.RARE));
    // only the Mayor can throw these (event/EventSwitchBlock); nobody can craft them
    public static final DeferredItem<BlockItem> PURGE_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.PURGE_SWITCH, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> BLOOD_MOON_SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.BLOOD_MOON_SWITCH, props -> props.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> TRAVELERS_SNOW_GLOBE = ITEMS.registerSimpleBlockItem(ModBlocks.TRAVELERS_SNOW_GLOBE, props -> props.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> TRAVELERS_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.TRAVELERS_LANTERN, props -> props.stacksTo(16).rarity(Rarity.RARE));
    // Salvage Hammer (smiths only, iron T): breaks gear down into 1 of its material at a smith table, 35 uses (weaponsmith/Salvaging)
    public static final DeferredItem<Item> SALVAGE_HAMMER = ITEMS.registerSimpleItem("salvage_hammer", props -> props.durability(35));
    // Reroll Tokens (Toolsmith Table: Name Tag + the gem): a sword or tool of that material gets new stats (weaponsmith/Rerolling)
    public static final DeferredItem<Item> IRON_REROLL_TOKEN = ITEMS.registerSimpleItem("iron_reroll_token", props -> props);
    public static final DeferredItem<Item> DIAMOND_REROLL_TOKEN = ITEMS.registerSimpleItem("diamond_reroll_token", props -> props);
    public static final DeferredItem<Item> NETHERITE_REROLL_TOKEN = ITEMS.registerSimpleItem("netherite_reroll_token", props -> props);

    // Placing it takes two blocks of space; the Fermenter handles the top half itself.
    public static final DeferredItem<BlockItem> FERMENTER = ITEMS.registerSimpleBlockItem(ModBlocks.FERMENTER);

    public static final DeferredItem<BlockItem> SHOPFRONT = ITEMS.registerSimpleBlockItem(ModBlocks.SHOPFRONT);

    // In-game guide to the Juicer, Fermenter and Bartending Table. Pages are generated from the recipes.
    public static final DeferredItem<BartendersGuideItem> BARTENDERS_GUIDE = ITEMS.registerItem("bartenders_guide", BartendersGuideItem::new, props -> props.stacksTo(1));

    // One bucket per liquor, made in the Fermenter; each leaves the empty bucket behind when used.
    public static final Map<LiquorType, DeferredItem<Item>> LIQUOR_BUCKETS = registerLiquorBuckets();

    private static Map<LiquorType, DeferredItem<Item>> registerLiquorBuckets() {
        Map<LiquorType, DeferredItem<Item>> buckets = new EnumMap<>(LiquorType.class);
        for (LiquorType liquor : LiquorType.values()) {
            buckets.put(liquor, ITEMS.registerSimpleItem(liquor.itemId(), props -> props
                .craftRemainder(Items.BUCKET)
                .stacksTo(1)));
        }
        return buckets;
    }

    // 4 shots are poured from one liquor bucket at the Bartending Table (one glass bottle each). Drinking one hands the glass back.
    public static final Map<LiquorType, DeferredItem<Item>> SHOTS = registerShots();

    private static Map<LiquorType, DeferredItem<Item>> registerShots() {
        Map<LiquorType, DeferredItem<Item>> shots = new EnumMap<>(LiquorType.class);
        for (LiquorType liquor : LiquorType.values()) {
            shots.put(liquor, ITEMS.registerSimpleItem(liquor.getSerializedName() + "_shot", props -> props
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(Items.GLASS_BOTTLE)
                .craftRemainder(Items.GLASS_BOTTLE)
                .stacksTo(16)));
        }
        return shots;
    }

    // 6 juices are poured from one juice bucket at the Bartending Table (one glass bottle each), into a glass pitcher.
    // Simple Syrup pours the same way into a syrup dispenser. None of them can be drunk: they're ingredients for
    // drinks, and their glass goes back into the glass slot when they're used.
    public static final Map<JuiceType, DeferredItem<Item>> JUICES = registerJuices();

    private static Map<JuiceType, DeferredItem<Item>> registerJuices() {
        Map<JuiceType, DeferredItem<Item>> juices = new EnumMap<>(JuiceType.class);
        for (JuiceType juice : JuiceType.values()) {
            String id = juice == JuiceType.SIMPLE_SYRUP ? "simple_syrup" : juice.getSerializedName() + "_juice";
            juices.put(juice, ITEMS.registerSimpleItem(id, props -> props.craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));
        }
        return juices;
    }

    // A dash of flavor for cocktails; made at the Bartending Table (4 at a time, one glass bottle each).
    // Its bottle goes back into the glass slot when it's used in a drink.
    public static final DeferredItem<Item> BITTERS = ITEMS.registerSimpleItem("bitters", props -> props
        .craftRemainder(Items.GLASS_BOTTLE)
        .stacksTo(16));

    // ── Cocktails: drinkable, hand back the glass bottle. Effects come later. ──
    public static final DeferredItem<Item> BEET_NIGHTCAP = ITEMS.registerSimpleItem("beet_nightcap", props -> props
        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
        .usingConvertsTo(Items.GLASS_BOTTLE)
        .stacksTo(16));

    // ── Cocktails with special effects (Bartending Table, 1 glass bottle each). Effects listed in Bartending.md;
    // the custom parts (teleport, light, trail...) are in player/DrinkEffects. ──
    // Luminescent Aura 2:00
    public static final DeferredItem<Item> GLOWBERRY_GOLD_RUSH = cocktail("glowberry_gold_rush", () -> List.of(
        new MobEffectInstance(ModEffects.LUMINESCENT_AURA, 2 * 60 * 20)));
    // Fire Resistance + Magma Trail 1:30
    public static final DeferredItem<Item> NETHER_FIREBALL_SHOT = cocktail("nether_fireball_shot", () -> List.of(
        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 90 * 20), new MobEffectInstance(ModEffects.MAGMA_TRAIL, 90 * 20)));
    // Phase Step: back to where you stood 10 seconds ago (DrinkEffects), then Slow Falling for 10 seconds
    public static final DeferredItem<Item> ENDER_TELE_TINI = cocktail("ender_tele_tini", () -> List.of(
        new MobEffectInstance(MobEffects.SLOW_FALLING, 10 * 20)));
    // Dolphin's Grace 1:00 + Treasure Sight 2:30
    public static final DeferredItem<Item> PIRATES_HIGH_SEAS_GROG = cocktail("pirates_high_seas_grog", () -> List.of(
        new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60 * 20), new MobEffectInstance(ModEffects.TREASURE_SIGHT, 150 * 20)));
    // Haste II + Fortune Boost 2:00
    public static final DeferredItem<Item> MINERS_MOONSHINE = cocktail("miners_moonshine", () -> List.of(
        new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20, 1), new MobEffectInstance(ModEffects.FORTUNE_BOOST, 2 * 60 * 20)));
    // Life Siphon 1:00
    public static final DeferredItem<Item> VAMPIRIC_MULLED_WINE = cocktail("vampiric_mulled_wine", () -> List.of(
        new MobEffectInstance(ModEffects.LIFE_SIPHON, 60 * 20)));
    // Clears every bad effect (DrinkEffects), then Speed II for 45 seconds
    public static final DeferredItem<Item> OVERWORLD_MOJITO = cocktail("overworld_mojito", () -> List.of(
        new MobEffectInstance(MobEffects.SPEED, 45 * 20, 1)));
    // Resets "time since rest" (DrinkEffects) + Phantom Camouflage 3:00
    public static final DeferredItem<Item> PHANTOMS_NIGHTCAP = cocktail("phantoms_nightcap", () -> List.of(
        new MobEffectInstance(ModEffects.PHANTOM_CAMOUFLAGE, 3 * 60 * 20)));
    // Iron Stomach + Regeneration II 1:30 (and cures Hunger right away)
    public static final DeferredItem<Item> ZOMBIES_HANGOVER_CURE = cocktail("zombies_hangover_cure", () -> List.of(
        new MobEffectInstance(ModEffects.IRON_STOMACH, 90 * 20), new MobEffectInstance(MobEffects.REGENERATION, 90 * 20, 1)));

    // Spatial Dislocation 1:30
    public static final DeferredItem<Item> ENDER_TWILIGHT_FIZZ = cocktail("ender_twilight_fizz", () -> List.of(
        new MobEffectInstance(ModEffects.SPATIAL_DISLOCATION, 90 * 20)));
    // Berserker's Rage 1:00: Strength II, no knockback, and Nausea the whole time
    public static final DeferredItem<Item> DRUNKEN_BRAWLERS_STOUT = cocktail("drunken_brawlers_stout", () -> List.of(
        new MobEffectInstance(ModEffects.BERSERKERS_RAGE, 60 * 20), new MobEffectInstance(MobEffects.STRENGTH, 60 * 20, 1),
        new MobEffectInstance(MobEffects.NAUSEA, 60 * 20)));

    // The second round of Specialty cocktails: each helps a job (ModEffects; textures: tools/make_cocktails.ps1)
    public static final DeferredItem<Item> FORGEFIRE_OLD_FASHIONED = cocktail("forgefire_old_fashioned", () -> List.of(
        new MobEffectInstance(ModEffects.STEADY_FORGE, 5 * 60 * 20)));
    public static final DeferredItem<Item> ANGLERS_SUNRISE = cocktail("anglers_sunrise", () -> List.of(
        new MobEffectInstance(ModEffects.LUCKY_LINE, 5 * 60 * 20)));
    public static final DeferredItem<Item> BUTCHERS_BLOODY_MARY = cocktail("butchers_bloody_mary", () -> List.of(
        new MobEffectInstance(ModEffects.STEADY_HANDS, 5 * 60 * 20)));
    // Warmed Up for 8 minutes (Campfire Chili gives 5): the cold can't reach you
    public static final DeferredItem<Item> HOT_TODDY = cocktail("hot_toddy", () -> List.of(
        new MobEffectInstance(ModEffects.WARMED_UP, 8 * 60 * 20)));
    public static final DeferredItem<Item> MIDNIGHT_OIL_MARTINI = cocktail("midnight_oil_martini", () -> List.of(
        new MobEffectInstance(ModEffects.QUICK_STUDY, 3 * 60 * 20)));
    public static final DeferredItem<Item> HAGGLERS_HIGHBALL = cocktail("hagglers_highball", () -> List.of(
        new MobEffectInstance(ModEffects.SILVER_TONGUE, 5 * 60 * 20)));
    public static final DeferredItem<Item> DESERT_ROSE = cocktail("desert_rose", () -> List.of(
        new MobEffectInstance(ModEffects.QUENCHED, 4 * 60 * 20)));
    public static final DeferredItem<Item> MOONSTALKER_SANGRIA = cocktail("moonstalker_sangria", () -> List.of(
        new MobEffectInstance(ModEffects.MOONSTALKER, 5 * 60 * 20)));

    // ── Classic cocktails: no effects, just water (and gold water) like every cocktail ──
    public static final DeferredItem<Item> CLASSIC_OLD_FASHIONED = cocktail("classic_old_fashioned", List::of);
    public static final DeferredItem<Item> WHISKEY_SOUR = cocktail("whiskey_sour", List::of);
    public static final DeferredItem<Item> WATERMELON_BREEZE = cocktail("watermelon_breeze", List::of);
    public static final DeferredItem<Item> SANGRIA_PITCHER = cocktail("sangria_pitcher", List::of);
    public static final DeferredItem<Item> WHITE_WINE_SPRITZER = cocktail("white_wine_spritzer", List::of);
    public static final DeferredItem<Item> TROPICAL_KELP_COOLER = cocktail("tropical_kelp_cooler", List::of);
    public static final DeferredItem<Item> BEETROOT_VELVET_SOUR = cocktail("beetroot_velvet_sour", List::of);
    // the second round of classics (textures: tools/make_cocktails.ps1)
    public static final DeferredItem<Item> MARGARITA = cocktail("margarita", List::of);
    public static final DeferredItem<Item> BERRY_DAIQUIRI = cocktail("berry_daiquiri", List::of);
    public static final DeferredItem<Item> COSMOPOLITAN = cocktail("cosmopolitan", List::of);
    public static final DeferredItem<Item> TEQUILA_SUNRISE = cocktail("tequila_sunrise", List::of);
    public static final DeferredItem<Item> MOSCOW_MULE = cocktail("moscow_mule", List::of);
    public static final DeferredItem<Item> RUM_PUNCH = cocktail("rum_punch", List::of);
    public static final DeferredItem<Item> ROSE_SPRITZ = cocktail("rose_spritz", List::of);
    public static final DeferredItem<Item> PUMPKIN_SPICE_FLIP = cocktail("pumpkin_spice_flip", List::of);

    public static final List<DeferredItem<Item>> COCKTAILS = List.of(GLOWBERRY_GOLD_RUSH, NETHER_FIREBALL_SHOT, ENDER_TELE_TINI,
        PIRATES_HIGH_SEAS_GROG, MINERS_MOONSHINE, VAMPIRIC_MULLED_WINE, OVERWORLD_MOJITO, PHANTOMS_NIGHTCAP, ZOMBIES_HANGOVER_CURE, ENDER_TWILIGHT_FIZZ, DRUNKEN_BRAWLERS_STOUT,
        CLASSIC_OLD_FASHIONED, WHISKEY_SOUR, WATERMELON_BREEZE, SANGRIA_PITCHER, WHITE_WINE_SPRITZER, TROPICAL_KELP_COOLER, BEETROOT_VELVET_SOUR,
        MARGARITA, BERRY_DAIQUIRI, COSMOPOLITAN, TEQUILA_SUNRISE, MOSCOW_MULE, RUM_PUNCH, ROSE_SPRITZ, PUMPKIN_SPICE_FLIP,
        FORGEFIRE_OLD_FASHIONED, ANGLERS_SUNRISE, BUTCHERS_BLOODY_MARY, HOT_TODDY, MIDNIGHT_OIL_MARTINI, HAGGLERS_HIGHBALL, DESERT_ROSE,
        MOONSTALKER_SANGRIA);

    private static Consumable drinkWith(List<MobEffectInstance> effects) {
        Consumable.Builder drink = Consumables.defaultDrink();
        if (!effects.isEmpty()) drink.onConsume(new ApplyStatusEffectsConsumeEffect(effects));
        return drink.build();
    }

    // A drinkable cocktail that hands its glass back and gives these effects.
    private static DeferredItem<Item> cocktail(String id, Supplier<List<MobEffectInstance>> effects) {
        return ITEMS.registerSimpleItem(id, props -> props
            .component(DataComponents.CONSUMABLE, drinkWith(effects.get()))
            .usingConvertsTo(Items.GLASS_BOTTLE)
            .stacksTo(16));
    }

    // 3 wine shots poured into a glass at the Bartending Table.
    public static final DeferredItem<Item> GLASS_OF_RED_WINE = ITEMS.registerSimpleItem("glass_of_red_wine", props -> props
        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
        .usingConvertsTo(Items.GLASS_BOTTLE)
        .stacksTo(16));
    public static final DeferredItem<Item> GLASS_OF_WHITE_WINE = ITEMS.registerSimpleItem("glass_of_white_wine", props -> props
        .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
        .usingConvertsTo(Items.GLASS_BOTTLE)
        .stacksTo(16));

    // Liquor shots, wine glasses and cocktails.
    public static boolean isAlcoholic(ItemStack stack) {
        for (DeferredItem<Item> shot : SHOTS.values()) {
            if (stack.is(shot)) return true;
        }
        for (DeferredItem<Item> cocktail : COCKTAILS) {
            if (stack.is(cocktail)) return true;
        }
        return stack.is(GLASS_OF_RED_WINE) || stack.is(GLASS_OF_WHITE_WINE) || stack.is(BEET_NIGHTCAP);
    }

    @SubscribeEvent
    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(PURIFIED_WATER);
            JUICE_BUCKETS.values().forEach(event::accept);
            LIQUOR_BUCKETS.values().forEach(event::accept);
            SHOTS.values().forEach(event::accept);
            JUICES.values().forEach(event::accept);
            event.accept(BEET_NIGHTCAP);
            event.accept(GLASS_OF_RED_WINE);
            event.accept(GLASS_OF_WHITE_WINE);
            COCKTAILS.forEach(event::accept);
            for (DeferredItem<Item> sushi : List.of(SALMON_NIGIRI, COD_NIGIRI, TROPICAL_NIGIRI, PUFFER_NIGIRI, COD_ROLL, KELP_ROLL, TROPICAL_ROLL, RAINBOW_ROLL, PUFFER_ROLL,
                VEGETABLE_ROLL, BAKED_SALMON_ROLL, FRUIT_ROLL, STEAK_ROLL, POTATO_ROLL, SWEET_BREAD_ROLL, OCEAN_SOVEREIGN_ROLL)) {
                event.accept(sushi);
            }
            for (DeferredItem<Item> meal : List.of(CHICKEN_POT_PIE, LOADED_BAKED_POTATO, CHICKEN_AND_RICE, LAMB_CHOPS, SEAFOOD_PASTA, SQUID_INK_PASTA, FRUIT_SALAD,
                SEAFOOD_CHOWDER, FIERY_MUSHROOM_BOWL, SWEET_BERRY_TART, STUFFED_PUMPKIN, GOLDEN_GLAZED_FEAST, MINERS_CASSEROLE, GARDEN_VEGGIE_CALZONE,
                NETHER_DRAGON_POT_PIE, HONEY_GLAZED_WING_ROAST, BEEF_SKEWER, CHICKEN_SKEWER, LAMB_SKEWER, FISH_SKEWER,
                SUGAR_RUSH_BERRY_CREPE, FARMERS_HARVEST_LOAF, SCHOLARS_BRAIN_STEW, CAMPFIRE_CHILI, FROSTY_MELON_SORBET,
                PULLED_PORK_SANDWICH, BRISKET_AND_POTATOES, SHEPHERDS_PIE, HAM_AND_EGG_BREAKFAST, HUNTERS_RABBIT_PIE, FISH_TACOS,
                MUSHROOM_RISOTTO, APPLE_CRUMBLE, IRONWORKERS_STEW, LUMBERJACK_FLAPJACKS, BRICK_OVEN_PIZZA, PRIME_RIB_DINNER,
                ANGLERS_FISH_PIE, COMFORT_SOUP, TRAIL_MIX_BAR, NIGHT_WATCH_HOTPOT)) {
                event.accept(meal);
            }
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PURIFIED_WATER_BUCKET);
            event.accept(BITTERS);
            event.accept(RICE_SEEDS);
            event.accept(RICE);
            event.accept(COOKED_RICE);
            for (DeferredItem<Item> dish : List.of(UNCOOKED_CHICKEN_POT_PIE, UNCOOKED_LOADED_BAKED_POTATO, UNCOOKED_CHICKEN_AND_RICE, UNCOOKED_LAMB_CHOPS, UNCOOKED_SEAFOOD_PASTA, UNCOOKED_SQUID_INK_PASTA,
                UNCOOKED_SEAFOOD_CHOWDER, UNCOOKED_FIERY_MUSHROOM_BOWL, UNCOOKED_SWEET_BERRY_TART, UNCOOKED_STUFFED_PUMPKIN, UNCOOKED_GOLDEN_GLAZED_FEAST,
                UNCOOKED_MINERS_CASSEROLE, UNCOOKED_GARDEN_VEGGIE_CALZONE,
                UNCOOKED_NETHER_DRAGON_POT_PIE, UNCOOKED_HONEY_GLAZED_WING_ROAST, UNCOOKED_BEEF_SKEWER, UNCOOKED_CHICKEN_SKEWER, UNCOOKED_LAMB_SKEWER, UNCOOKED_FISH_SKEWER,
                UNCOOKED_SUGAR_RUSH_BERRY_CREPE, UNCOOKED_FARMERS_HARVEST_LOAF, UNCOOKED_SCHOLARS_BRAIN_STEW, UNCOOKED_CAMPFIRE_CHILI,
                UNCOOKED_PULLED_PORK_SANDWICH, UNCOOKED_BRISKET_AND_POTATOES, UNCOOKED_SHEPHERDS_PIE, UNCOOKED_HAM_AND_EGG_BREAKFAST,
                UNCOOKED_HUNTERS_RABBIT_PIE, UNCOOKED_FISH_TACOS, UNCOOKED_MUSHROOM_RISOTTO, UNCOOKED_APPLE_CRUMBLE,
                UNCOOKED_IRONWORKERS_STEW, UNCOOKED_LUMBERJACK_FLAPJACKS, UNCOOKED_BRICK_OVEN_PIZZA, UNCOOKED_PRIME_RIB_DINNER,
                UNCOOKED_ANGLERS_FISH_PIE, UNCOOKED_COMFORT_SOUP, UNCOOKED_TRAIL_MIX_BAR, UNCOOKED_NIGHT_WATCH_HOTPOT)) {
                event.accept(dish);
            }
            event.accept(BURNT_FOOD);
            event.accept(DIRTY_SILVER_PLATE);
            event.accept(DIRTY_GOLDEN_PLATE);
            event.accept(SQUASHED_SUSHI);
            event.accept(COMPACTED_DIAMOND);
            event.accept(COMPACTED_IRON);
            event.accept(SALVAGE_HAMMER);
            event.accept(MOLTEN_SOCKET);
            for (var traveler : List.of(BOTTOMLESS_WATERSKIN, HAND_WARMER, SMITHS_CHISEL, PHOENIX_FEATHER, TRAVELERS_SNOW_GLOBE, TRAVELERS_LANTERN)) event.accept(traveler.get());
            event.accept(MAYORAL_DOOR);
            event.accept(PURGE_SWITCH);
            event.accept(BLOOD_MOON_SWITCH);
            event.accept(IRON_REROLL_TOKEN);
            event.accept(DIAMOND_REROLL_TOKEN);
            event.accept(NETHERITE_REROLL_TOKEN);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(PURIFICATION_TABLE);
            event.accept(JUICER);
            event.accept(BARTENDING_TABLE);
            event.accept(FERMENTER);
            event.accept(SHOPFRONT);
            event.accept(SUSHI_TABLE);
            event.accept(COOKING_TABLE);
            event.accept(OVEN);
            event.accept(SILVER_PLATE);
            event.accept(GOLDEN_PLATE);
            event.accept(SINK);
            event.accept(BUTCHER_SHOP_ICE);
            event.accept(WEAPONSMITH_TABLE);
            event.accept(WHETSTONE);
            event.accept(LAVA_CREATOR);
            event.accept(OSMOSIS_MACHINE);
            event.accept(OSMOSIS_FILTER);
            event.accept(ADVANCED_OSMOSIS_FILTER);
            event.accept(SPRING_WATER_BUCKET);
            event.accept(SPRING_WATER);
            event.accept(REFINED_LAVA_BUCKET);
            event.accept(PICKAXE_WHETSTONE);
            event.accept(ARMOR_WHETSTONE);
            event.accept(TOOLSMITH_TABLE);
            event.accept(TRANSFER_TUBE);
            event.accept(WISHING_WELL);
            event.accept(MAILBOX);
            event.accept(TOWN_MAILBOX);
            event.accept(MAYORAL_CHEST);
            event.accept(INVESTMENT_CHEST);
            event.accept(TOWN_HALL_SIGN);
            event.accept(BANK_SIGN);
            event.accept(BOAT_SHOP_SIGN);
            event.accept(BLOOD_SHARD);
            event.accept(REVENANT_CREST);
            event.accept(SANGUINE_CREST);
            event.accept(GOREFANG_CREST);
            event.accept(BLOOD_MOON_CREST);
            for (var bloodDecor : List.of(BLOOD_MOON_LANTERN, CRIMSON_CANDELABRA, CRIMSON_THRONE, VAMPIRE_COFFIN, BLOOD_FOUNTAIN, BLOOD_MOON_BANNER,
                CRIMSON_RUG, REVENANT_GREATSWORD_STATUE, GOREFANG_SKULL_TROPHY, SANGUINE_STAFF_PEDESTAL, BAT_GARGOYLE, GRANDFATHER_CLOCK,
                CRIMSON_CHANDELIER, BLOOD_PIPE_ORGAN, SECRET_BOOKSHELF_DOOR, HAUNTED_PORTRAIT, IRON_MAIDEN, BLOOD_MOON_ALTAR, BAT_ROOST,
                CRIMSON_CANOPY_BED, BLOOD_CRYSTAL_FIREPLACE, GRINNING_JACK_O_LANTERN, HAUNTED_TOMBSTONE, POSSESSED_SCARECROW)) event.accept(bloodDecor);
            event.accept(GHOST_SHEET);
            event.accept(WITCHS_BROOM);
            event.accept(BOTTLE_OF_BLOOD);
            // the rare fishing catches (Fishing.md), and what comes of them
            for (var rare : RARE_CATCHES.values()) event.accept(rare.get());
            event.accept(SOGGY_NOTE);
            event.accept(PIRATE_HAT);
            event.accept(BLOODSTONE_BRICKS);
            event.accept(BLOOD_CRYSTAL_BLOCK);
            event.accept(FUN_WISHING_STAR);
            event.accept(ESSENTIAL_WISHING_STAR);
            event.accept(CHANGING_STAR);
            event.accept(ENCHANTERS_TABLE);
            event.accept(ENCH_REMOVAL);
            event.accept(APPRENTICE_WAND);
            event.accept(LAPIS_COMPRESSOR);
            event.accept(STITCHING_TABLE);
            event.accept(LIBRARIAN_TABLE);
            event.accept(KNOWLEDGE_CREATOR);
            event.accept(ROUGH_BOOK);
            event.accept(DELICATE_BOOK);
            for (com.alexh.capitalism.librarian.KnowledgeQuality quality : com.alexh.capitalism.librarian.KnowledgeQuality.values()) {
                event.accept(quality.stack(1));
            }
            // every skill book, for testing (Rough, and Delicate = level II for lessons); in play they're only written at random
            for (com.alexh.capitalism.librarian.Skill skill : com.alexh.capitalism.librarian.Skill.values()) {
                event.accept(com.alexh.capitalism.librarian.SkillBookItem.write(new ItemStack(SKILL_BOOK.get()), skill, false));
                if (!skill.trait && !skill.instant) event.accept(com.alexh.capitalism.librarian.SkillBookItem.write(new ItemStack(SKILL_BOOK.get()), skill, true));
            }
            event.accept(ARMORSMITH_TABLE);
            event.accept(OWNERSHIP_TAG);
            event.accept(TRAINING_STICK);
            event.accept(BACKPACK);
            event.accept(FOOD_SATCHEL);
            event.accept(SEED_SATCHEL);
            event.accept(FANCY_BACKPACK);
            event.accept(FANCY_FOOD_SATCHEL);
            event.accept(FANCY_SEED_SATCHEL);
            event.accept(UNIFORM_BACKPACK);
            event.accept(GUCCI_BAG);
            event.accept(BURBERRY_BAG);
            event.accept(LOUIS_VUITTON_BAG);
            event.accept(PRADA_BAG);
            event.accept(GUCCI_TOP_HAT);
            event.accept(BURBERRY_SANTA_HAT);
            event.accept(LOUIS_VUITTON_CAP);
            event.accept(PRADA_FEDORA);
            event.accept(LEATHER_PATCH);
            event.accept(STRAP_PATCH);
            event.accept(PANTRY_PATCH);
            event.accept(SEED_POCKET_PATCH);
            event.accept(VELVET_PATCH);
            event.accept(EMBROIDERED_PATCH);
            event.accept(GOLD_TRIM_PATCH);
            event.accept(SILK_PATCH);
            event.accept(GUCCI_PATCH);
            event.accept(BURBERRY_PATCH);
            event.accept(LOUIS_VUITTON_PATCH);
            event.accept(PRADA_PATCH);
            event.accept(FROZEN_PATCH);
            event.accept(FREEZER_SLING);
            event.accept(TRAVELERS_BAG);
            event.accept(IRON_SEWING_NEEDLE);
            event.accept(DIAMOND_SEWING_NEEDLE);
            event.accept(DESIGNER_TABLE);
            event.accept(SEWING_MACHINE);
            event.accept(DECOMPOSTER);
            event.accept(FERTILIZER);
            event.accept(SUPER_FERTILIZER);
            event.accept(FAST_FERTILIZER);
            event.accept(SCYTHE);
            event.accept(DIAMOND_SCYTHE);
            event.accept(BUTCHERS_TABLE);
            event.accept(IRON_BUTCHER_KNIFE);
            event.accept(DIAMOND_BUTCHER_KNIFE);
            event.accept(FRIDGE);
            event.accept(FREEZER);
            event.accept(ROTTEN_MEAT);
            event.accept(GRILL);
            event.accept(SURVEY_STAKE);
            event.accept(CITY_SALE_SIGN);
            event.accept(CITY_CHEST);
            event.accept(FOR_SALE_SIGN);
            event.accept(FOR_LEASE_SIGN);
            for (var boat : List.of(CASTAWAY_TUB, SPEEDBOAT, SWAMP_RAFT, YACHT, LONGSHIP, PADDLEWHEELER, PIRATE_SHIP, TEAPOT, STEAMBOAT,
                GHOST_SHIP, SUBMERSIBLE, FAIRY_BOAT, LEVIATHAN)) event.accept(boat);
            for (var decor : List.of(CHRISTMAS_TREE, STRING_LIGHT_HOOK, STRING_LIGHTS, PRESENT_PILE, CANDY_CANE_POLE, CANDY_CANES, CHRISTMAS_FROST, ICICLES, CHRISTMAS_PRESENT, SPOOKY_SCARECROW, WITCHS_CAULDRON, GRAVESTONE_ROUND, GRAVESTONE_CROSS, GRAVESTONE_TOMB, BONE_PILE, GROUND_FOG, PUMPKIN_PILE, LEAF_PILE,
                AUTUMN_MAPLE, AUTUMN_BIRCH, FALLEN_LEAVES, BEACH_TOWEL, BEACH_BALL, BEACH_UMBRELLA, BEACH_CHAIR, EGG_BASKET, GIANT_EGG, SPRING_BUNNY, TULIP_BED)) event.accept(decor);
            for (var seared : SEARED_CUTS.values()) event.accept(seared);
            for (var slab : MEAT_SLABS.values()) event.accept(slab);
            for (var cut : MEAT_CUTS.values()) event.accept(cut);
            event.accept(TRAINING_FEED);
            event.accept(ANIMAL_BJORN);
            event.accept(SMALL_FOOD_TROUGH);
            event.accept(MEDIUM_FOOD_TROUGH);
            event.accept(LARGE_FOOD_TROUGH);
            event.accept(SMALL_WATER_BOWL);
            event.accept(MEDIUM_WATER_BOWL);
            event.accept(LARGE_WATER_BOWL);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BARTENDERS_GUIDE);
        }
    }
}

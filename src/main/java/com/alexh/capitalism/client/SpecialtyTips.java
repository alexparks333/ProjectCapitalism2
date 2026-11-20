package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// A one-line "what it does" on the tooltip of every Specialty drink and meal (Bartending.md > Cocktail effects,
// Cook.md > Meal effects). Short on purpose: no numbers, just the gist. New specialty food or drink: add a line here.
public final class SpecialtyTips {

    private static final Map<Supplier<? extends Item>, String> TIPS = new LinkedHashMap<>();

    static {
        // Bartender's Specialty Drinks
        TIPS.put(ModItems.GLOWBERRY_GOLD_RUSH, "A light follows you around.");
        TIPS.put(ModItems.NETHER_FIREBALL_SHOT, "Fireproof; your footsteps burn enemies.");
        TIPS.put(ModItems.ENDER_TELE_TINI, "Zaps you back to where you were 5s ago.");
        TIPS.put(ModItems.PIRATES_HIGH_SEAS_GROG, "Swim fast; nearby sunken treasure glows.");
        TIPS.put(ModItems.MINERS_MOONSHINE, "Mine faster; ores may drop extra.");
        TIPS.put(ModItems.VAMPIRIC_MULLED_WINE, "Hitting mobs heals you.");
        TIPS.put(ModItems.OVERWORLD_MOJITO, "Clears bad effects; run faster.");
        TIPS.put(ModItems.PHANTOMS_NIGHTCAP, "Counts as sleep; phantoms ignore you.");
        TIPS.put(ModItems.ZOMBIES_HANGOVER_CURE, "Cures hunger sickness; eat raw food safely.");
        TIPS.put(ModItems.ENDER_TWILIGHT_FIZZ, "Big hits teleport you away instead.");
        TIPS.put(ModItems.DRUNKEN_BRAWLERS_STOUT, "Hit harder, no knockback, but woozy.");
        TIPS.put(ModItems.FORGEFIRE_OLD_FASHIONED, "Repair dents stay hot longer.");
        TIPS.put(ModItems.ANGLERS_SUNRISE, "Rare fish bite more often.");
        TIPS.put(ModItems.BUTCHERS_BLOODY_MARY, "An easier hand at the butcher's table.");
        TIPS.put(ModItems.HOT_TODDY, "Keeps the cold away, a long while.");
        TIPS.put(ModItems.MIDNIGHT_OIL_MARTINI, "Read books faster; lessons last longer.");
        TIPS.put(ModItems.HAGGLERS_HIGHBALL, "NPC shops give you a discount.");
        TIPS.put(ModItems.DESERT_ROSE, "You get thirsty half as fast.");
        TIPS.put(ModItems.MOONSTALKER_SANGRIA, "Hit night monsters and bosses harder.");
        // Cook's Specialty Food / Sushi
        TIPS.put(ModItems.FIERY_MUSHROOM_BOWL, "Fireproof; swim fast through lava.");
        TIPS.put(ModItems.MINERS_CASSEROLE, "See in the dark; mine faster.");
        TIPS.put(ModItems.NETHER_DRAGON_POT_PIE, "Fireproof; sneak + jump to blink ahead.");
        TIPS.put(ModItems.OCEAN_SOVEREIGN_ROLL, "Swim fast; breathe underwater.");
        TIPS.put(ModItems.SUGAR_RUSH_BERRY_CREPE, "Run fast, jump high.");
        TIPS.put(ModItems.FARMERS_HARVEST_LOAF, "Crops around you grow super fast.");
        TIPS.put(ModItems.SCHOLARS_BRAIN_STEW, "Mobs you kill drop double XP.");
        TIPS.put(ModItems.CAMPFIRE_CHILI, "Keeps the cold away.");
        TIPS.put(ModItems.FROSTY_MELON_SORBET, "Keeps you from overheating.");
        TIPS.put(ModItems.IRONWORKERS_STEW, "Your gear wears down half as fast.");
        TIPS.put(ModItems.LUMBERJACK_FLAPJACKS, "Axes chop wood faster.");
        TIPS.put(ModItems.BRICK_OVEN_PIZZA, "Reach further when building.");
        TIPS.put(ModItems.PRIME_RIB_DINNER, "One extra heart for a while.");
        TIPS.put(ModItems.ANGLERS_FISH_PIE, "Fish bite faster.");
        TIPS.put(ModItems.COMFORT_SOUP, "Cures sickness; perfectly comfortable a while.");
        TIPS.put(ModItems.TRAIL_MIX_BAR, "Sprinting costs no hunger.");
        TIPS.put(ModItems.NIGHT_WATCH_HOTPOT, "Monsters notice you from less far.");
    }

    private SpecialtyTips() {
    }

    // the line for this item, or null (the Cook's and Bartender's Guides show it on the item's page too)
    public static String tip(ItemStack stack) {
        for (Map.Entry<Supplier<? extends Item>, String> tip : TIPS.entrySet()) {
            if (stack.is(tip.getKey().get())) return tip.getValue();
        }
        return null;
    }

    public static void add(ItemStack stack, List<Component> tooltip) {
        for (Map.Entry<Supplier<? extends Item>, String> tip : TIPS.entrySet()) {
            if (stack.is(tip.getKey().get())) {
                tooltip.add(Component.literal("✦ " + tip.getValue()).withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
        }
    }
}

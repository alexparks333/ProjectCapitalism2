package com.alexh.capitalism.core;

import com.alexh.capitalism.block.LiquorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

// Chef meals and Bartender drinks that can give golden bars. Consuming one adds `food` and `water` points
// (2 points = 1 icon): they fill the normal bar first, and anything past full turns into gold (up to a full bar).
// These can be eaten/drunk even when your bars are full. Every item is listed in Bartending.md.
public record GoldenBonus(Supplier<? extends Item> item, int food, int water) {

    public static final List<GoldenBonus> ALL = List.of(
        // Spirit shots: 2 water drops
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.TEQUILA), 0, 4),
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.VODKA), 0, 4),
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.RUM), 0, 4),
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.WHISKEY), 0, 4),
        // Wine: 1 drop a shot, 6 drops a glass
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.RED_WINE), 0, 2),
        new GoldenBonus(ModItems.SHOTS.get(LiquorType.WHITE_WINE), 0, 2),
        new GoldenBonus(ModItems.GLASS_OF_RED_WINE, 0, 12),
        new GoldenBonus(ModItems.GLASS_OF_WHITE_WINE, 0, 12),
        // Cocktails
        new GoldenBonus(ModItems.BEET_NIGHTCAP, 0, 12),
        new GoldenBonus(ModItems.GLOWBERRY_GOLD_RUSH, 0, 12),
        new GoldenBonus(ModItems.NETHER_FIREBALL_SHOT, 0, 12),
        new GoldenBonus(ModItems.ENDER_TELE_TINI, 0, 12),
        new GoldenBonus(ModItems.PIRATES_HIGH_SEAS_GROG, 0, 12),
        new GoldenBonus(ModItems.MINERS_MOONSHINE, 0, 12),
        new GoldenBonus(ModItems.VAMPIRIC_MULLED_WINE, 0, 12),
        new GoldenBonus(ModItems.OVERWORLD_MOJITO, 0, 12),
        new GoldenBonus(ModItems.PHANTOMS_NIGHTCAP, 0, 12),
        new GoldenBonus(ModItems.ZOMBIES_HANGOVER_CURE, 0, 12),
        new GoldenBonus(ModItems.ENDER_TWILIGHT_FIZZ, 0, 12),
        new GoldenBonus(ModItems.DRUNKEN_BRAWLERS_STOUT, 0, 12),
        new GoldenBonus(ModItems.CLASSIC_OLD_FASHIONED, 0, 12),
        new GoldenBonus(ModItems.WHISKEY_SOUR, 0, 12),
        new GoldenBonus(ModItems.WATERMELON_BREEZE, 0, 12),
        new GoldenBonus(ModItems.SANGRIA_PITCHER, 0, 12),
        new GoldenBonus(ModItems.WHITE_WINE_SPRITZER, 0, 12),
        new GoldenBonus(ModItems.TROPICAL_KELP_COOLER, 0, 12),
        new GoldenBonus(ModItems.BEETROOT_VELVET_SOUR, 0, 12),
        new GoldenBonus(ModItems.MARGARITA, 0, 12),
        new GoldenBonus(ModItems.BERRY_DAIQUIRI, 0, 12),
        new GoldenBonus(ModItems.COSMOPOLITAN, 0, 12),
        new GoldenBonus(ModItems.TEQUILA_SUNRISE, 0, 12),
        new GoldenBonus(ModItems.MOSCOW_MULE, 0, 12),
        new GoldenBonus(ModItems.RUM_PUNCH, 0, 12),
        new GoldenBonus(ModItems.ROSE_SPRITZ, 0, 12),
        new GoldenBonus(ModItems.PUMPKIN_SPICE_FLIP, 0, 12),
        new GoldenBonus(ModItems.FORGEFIRE_OLD_FASHIONED, 0, 12),
        new GoldenBonus(ModItems.ANGLERS_SUNRISE, 0, 12),
        new GoldenBonus(ModItems.BUTCHERS_BLOODY_MARY, 0, 12),
        new GoldenBonus(ModItems.HOT_TODDY, 0, 12),
        new GoldenBonus(ModItems.MIDNIGHT_OIL_MARTINI, 0, 12),
        new GoldenBonus(ModItems.HAGGLERS_HIGHBALL, 0, 12),
        new GoldenBonus(ModItems.DESERT_ROSE, 0, 12),
        new GoldenBonus(ModItems.MOONSTALKER_SANGRIA, 0, 12)
    );


    public static @Nullable GoldenBonus of(ItemStack stack) {
        for (GoldenBonus bonus : ALL) {
            if (stack.is(bonus.item().get())) return bonus;
        }
        return null;
    }
}

package com.alexh.capitalism.occupation;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

// Server-wide passive debuff: ordinary (non-Gourmet) food only fills 1/2 as much, rounded up to the nearest
// hunger point (half a drumstick). Saturation shrinks by the same amount. Gourmet food made by a Cook will carry its
// own full-strength food values, so it isn't affected; so does vanilla food a Cook makes (CookMadeFood, fullFood()).
//
// Every vanilla food and its new hunger points are listed here (vanilla values are in net.minecraft.world.food.Foods);
// change a number to tune that food.
@EventBusSubscriber(modid = Capitalism.MODID)
public class FoodNerf {

    // vanilla food -> {vanilla hunger points, new hunger points}
    private static Map<Item, int[]> table() {
        Map<Item, int[]> t = new LinkedHashMap<>();
        t.put(Items.APPLE, new int[]{4, 2});
        t.put(Items.BAKED_POTATO, new int[]{5, 3});
        t.put(Items.BEEF, new int[]{3, 2});
        t.put(Items.BEETROOT, new int[]{1, 1});
        t.put(Items.BEETROOT_SOUP, new int[]{6, 3});
        t.put(Items.BREAD, new int[]{5, 3});
        t.put(Items.CARROT, new int[]{3, 2});
        t.put(Items.CHICKEN, new int[]{2, 1});
        t.put(Items.CHORUS_FRUIT, new int[]{4, 2});
        t.put(Items.COD, new int[]{2, 1});
        t.put(Items.COOKED_BEEF, new int[]{8, 4});
        t.put(Items.COOKED_CHICKEN, new int[]{6, 3});
        t.put(Items.COOKED_COD, new int[]{5, 3});
        t.put(Items.COOKED_MUTTON, new int[]{6, 3});
        t.put(Items.COOKED_PORKCHOP, new int[]{8, 4});
        t.put(Items.COOKED_RABBIT, new int[]{5, 3});
        t.put(Items.COOKED_SALMON, new int[]{6, 3});
        t.put(Items.COOKIE, new int[]{2, 1});
        t.put(Items.DRIED_KELP, new int[]{1, 1});
        t.put(Items.ENCHANTED_GOLDEN_APPLE, new int[]{4, 2});
        t.put(Items.GLOW_BERRIES, new int[]{2, 1});
        t.put(Items.GOLDEN_APPLE, new int[]{4, 2});
        t.put(Items.GOLDEN_CARROT, new int[]{6, 3});
        t.put(Items.HONEY_BOTTLE, new int[]{6, 3});
        t.put(Items.MELON_SLICE, new int[]{2, 1});
        t.put(Items.MUSHROOM_STEW, new int[]{6, 3});
        t.put(Items.MUTTON, new int[]{2, 1});
        t.put(Items.POISONOUS_POTATO, new int[]{2, 1});
        t.put(Items.PORKCHOP, new int[]{3, 2});
        t.put(Items.POTATO, new int[]{1, 1});
        t.put(Items.PUFFERFISH, new int[]{1, 1});
        t.put(Items.PUMPKIN_PIE, new int[]{8, 4});
        t.put(Items.RABBIT, new int[]{3, 2});
        t.put(Items.RABBIT_STEW, new int[]{10, 5});
        t.put(Items.ROTTEN_FLESH, new int[]{4, 2});
        t.put(Items.SALMON, new int[]{2, 1});
        t.put(Items.SPIDER_EYE, new int[]{2, 1});
        t.put(Items.SUSPICIOUS_STEW, new int[]{6, 3});
        t.put(Items.SWEET_BERRIES, new int[]{2, 1});
        t.put(Items.TROPICAL_FISH, new int[]{1, 1});
        return t;
    }

    // Every debuffed vanilla food (the ones that can be Cook-made)
    public static java.util.Set<Item> foods() {
        return table().keySet();
    }

    // A vanilla food's full (un-nerfed) food values, for Cook-made food (CookMadeFood); null if it isn't a nerfed food
    public static @Nullable FoodProperties fullFood(Item item) {
        int[] points = table().get(item);
        FoodProperties nerfed = item.components().get(DataComponents.FOOD);
        if (points == null || nerfed == null || points[1] <= 0) return null;
        float scale = points[1] / (float) points[0];
        return new FoodProperties(points[0], nerfed.saturation() / scale, nerfed.canAlwaysEat());
    }

    @SubscribeEvent
    public static void onModifyComponents(ModifyDefaultComponentsEvent event) {
        table().forEach((item, points) -> event.modify(item, (components, context, it) -> {
            FoodProperties food = components.get(DataComponents.FOOD);
            if (food == null || food.nutrition() <= 0) return;
            float scale = points[1] / (float) points[0];
            components.set(DataComponents.FOOD, new FoodProperties(points[1], food.saturation() * scale, food.canAlwaysEat()));
        }));
    }
}

package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

// Every liquor the Fermenter can make: what ferments into it, its color, and the bucket it fills.
// To add one: add a line here, a lang entry, and an items/<id>.json (copy tequila_bucket.json, change the color).
public enum LiquorType implements StringRepresentable {
    TEQUILA("tequila", "tequila_bucket", 0xF2E2A0, () -> Items.CACTUS),
    VODKA("vodka", "vodka_bucket", 0xE4EEF4, () -> Items.POTATO),
    RUM("rum", "rum_bucket", 0xB8742C, () -> Items.SUGAR_CANE),
    RED_WINE("red_wine", "red_wine_bucket", 0x7A1030, () -> Items.SWEET_BERRIES),
    WHITE_WINE("white_wine", "white_wine_bucket", 0xF0E27A, () -> Items.GLOW_BERRIES),
    WHISKEY("whiskey", "whiskey_bucket", 0xC0782A, () -> Items.WHEAT);

    private final String name;
    private final String itemId;
    private final int color;
    private final Supplier<Item> ingredient;

    LiquorType(String name, String itemId, int color, Supplier<Item> ingredient) {
        this.name = name;
        this.itemId = itemId;
        this.color = color;
        this.ingredient = ingredient;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String itemId() {
        return this.itemId;
    }

    // RGB, no alpha
    public int color() {
        return this.color;
    }

    public Item ingredient() {
        return this.ingredient.get();
    }

    public Item result() {
        return ModItems.LIQUOR_BUCKETS.get(this).get();
    }
}

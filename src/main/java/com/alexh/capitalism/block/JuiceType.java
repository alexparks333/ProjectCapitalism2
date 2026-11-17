package com.alexh.capitalism.block;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

// Every juice the Juicer can make: which fruit goes in, what color it shows, and the bucket it fills.
// To add a juice: add a line here, a lang entry, and an items/<id>.json (see watermelon_juice_bucket.json).
public enum JuiceType implements StringRepresentable {
    WATERMELON("watermelon", "watermelon_juice_bucket", 0xE24654, () -> Items.MELON_SLICE),
    APPLE("apple", "apple_juice_bucket", 0xE8C45A, () -> Items.APPLE),
    CARROT("carrot", "carrot_juice_bucket", 0xF08A28, () -> Items.CARROT),
    BEETROOT("beetroot", "beetroot_juice_bucket", 0x961E3C, () -> Items.BEETROOT),
    CHORUS_FRUIT("chorus_fruit", "chorus_fruit_juice_bucket", 0xB07CC8, () -> Items.CHORUS_FRUIT),
    KELP("kelp", "kelp_juice_bucket", 0x5A8C3C, () -> Items.KELP),
    SWEET_BERRY("sweet_berry", "sweet_berry_juice_bucket", 0xC8243C, () -> Items.SWEET_BERRIES),
    GLOW_BERRY("glow_berry", "glow_berry_juice_bucket", 0xFFB43C, () -> Items.GLOW_BERRIES),
    GOLDEN_APPLE("golden_apple", "golden_apple_juice_bucket", 0xFFD83C, () -> Items.GOLDEN_APPLE),
    SIMPLE_SYRUP("simple_syrup", "simple_syrup_bucket", 0xEEDDA8, () -> Items.SUGAR_CANE);

    private final String name;
    private final String itemId;
    private final int color;
    private final Supplier<Item> fruit;

    JuiceType(String name, String itemId, int color, Supplier<Item> fruit) {
        this.name = name;
        this.itemId = itemId;
        this.color = color;
        this.fruit = fruit;
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

    public Item fruit() {
        return this.fruit.get();
    }

    public Item result() {
        return ModItems.JUICE_BUCKETS.get(this).get();
    }
}

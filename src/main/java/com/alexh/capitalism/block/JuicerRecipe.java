package com.alexh.capitalism.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Juicer rules: the same fruit in all 6 top slots + one empty bucket in the big slot.
// The bucket turns into that fruit's juice in place. The juices themselves live in JuiceType.
public final class JuicerRecipe {

    private JuicerRecipe() {
    }

    public static @Nullable JuiceType find(NonNullList<ItemStack> items) {
        ItemStack bucket = items.get(JuicerBlockEntity.SLOT_BUCKET);
        if (!isContainer(bucket) || bucket.getCount() != 1) return null;

        for (JuiceType juice : JuiceType.values()) {
            boolean allMatch = true;
            for (int i = 0; i < JuicerBlockEntity.FRUIT_SLOTS; i++) {
                if (!items.get(i).is(juice.fruit())) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) return juice;
        }
        return null;
    }

    public static boolean isFruit(ItemStack stack) {
        for (JuiceType juice : JuiceType.values()) {
            if (stack.is(juice.fruit())) return true;
        }
        return false;
    }

    public static boolean isContainer(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    // Which juice a finished bucket holds, or null if it isn't a juice bucket.
    public static @Nullable JuiceType juiceOf(ItemStack stack) {
        for (JuiceType juice : JuiceType.values()) {
            if (stack.is(juice.result())) return juice;
        }
        return null;
    }
}

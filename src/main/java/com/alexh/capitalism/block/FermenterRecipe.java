package com.alexh.capitalism.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Fermenter rules: the same fermentable in all 6 slots, sugar in the sugar slot, one empty bucket.
// After the wait the bucket becomes that liquor. The liquors themselves live in LiquorType.
public final class FermenterRecipe {

    private FermenterRecipe() {
    }

    public static @Nullable LiquorType find(NonNullList<ItemStack> items) {
        ItemStack bucket = items.get(FermenterBlockEntity.SLOT_BUCKET);
        if (!bucket.is(Items.BUCKET) || bucket.getCount() != 1) return null;

        for (LiquorType liquor : LiquorType.values()) {
            boolean allMatch = true;
            for (int i = 0; i < FermenterBlockEntity.INGREDIENT_SLOTS; i++) {
                if (!items.get(i).is(liquor.ingredient())) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) return liquor;
        }
        return null;
    }

    public static boolean isFermentable(ItemStack stack) {
        for (LiquorType liquor : LiquorType.values()) {
            if (stack.is(liquor.ingredient())) return true;
        }
        return false;
    }

    public static boolean isSugar(ItemStack stack) {
        return stack.is(Items.SUGAR);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    // Which liquor a finished bucket holds, or null.
    public static @Nullable LiquorType liquorOf(ItemStack stack) {
        for (LiquorType liquor : LiquorType.values()) {
            if (stack.is(liquor.result())) return liquor;
        }
        return null;
    }
}

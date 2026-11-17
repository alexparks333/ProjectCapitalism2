package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

// What the Weaponsmith Table makes: the item in the big slot + an ingredient in the small slot -> result.
// Every recipe burns some of the table's lava (SmithTableBlockEntity), in millibuckets (1000 = 1 Lava Bucket).
//  - Sword Whetstone (the "whetstone" block): Grindstone + Compacted Iron, 250 lava.
// Repairs (a damaged weapon + its material) aren't recipes: they're the Forge button (SmithTableMenu, the dent game).
public final class WeaponsmithTableRecipes {

    // big slot item + small slot item -> result, burning lavaCost
    public record Recipe(Item big, Item small, Item result, int lavaCost) {
    }

    private WeaponsmithTableRecipes() {
    }

    public static List<Recipe> recipes() {
        List<Recipe> recipes = new java.util.ArrayList<>();
        recipes.add(new Recipe(Items.GRINDSTONE, ModItems.COMPACTED_IRON.get(), ModItems.WHETSTONE.get(), 250));
        recipes.addAll(ToolsmithTableRecipes.rerollTokens());
        return recipes;
    }

    // Weapons only a Weaponsmith can repair (see WeaponRepairs), and what the Weaponsmith Table forges back into shape
    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.CROSSBOW) || stack.is(Items.BOW)
            || stack.is(Items.TRIDENT) || stack.is(Items.MACE) || stack.is(ItemTags.SPEARS);
    }

    // The recipe these two slots make, out of a table's recipes (see SmithTable.recipes)
    public static @Nullable Recipe find(List<Recipe> recipes, ItemStack big, ItemStack small) {
        for (Recipe recipe : recipes) {
            if (big.is(recipe.big()) && small.is(recipe.small())) return recipe;
        }
        return null;
    }
}

package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

// What the Toolsmith Table makes: big slot + small slot, each recipe burns some lava (see WeaponsmithTableRecipes.Recipe).
//  - Tool Whetstone: Grindstone + Compacted Iron, 250 lava (sharpens dull pickaxes, see Wear).
//  - Reroll Tokens (rerollTokens(): every smith table makes them, see SmithTable.recipes).
public final class ToolsmithTableRecipes {

    private ToolsmithTableRecipes() {
    }

    public static List<WeaponsmithTableRecipes.Recipe> recipes() {
        List<WeaponsmithTableRecipes.Recipe> recipes = new ArrayList<>();
        recipes.add(new WeaponsmithTableRecipes.Recipe(Items.GRINDSTONE, ModItems.COMPACTED_IRON.get(), ModItems.PICKAXE_WHETSTONE.get(), 250));
        recipes.addAll(rerollTokens());
        return recipes;
    }

    // Reroll Tokens: Name Tag + Iron Ingot / Diamond / Netherite Ingot, 100 lava each. Any smith makes them at their own
    // table (used at the smith tables to give a sword or tool new stats, see Rerolling).
    public static List<WeaponsmithTableRecipes.Recipe> rerollTokens() {
        return List.of(
            new WeaponsmithTableRecipes.Recipe(Items.NAME_TAG, Items.IRON_INGOT, ModItems.IRON_REROLL_TOKEN.get(), 100),
            new WeaponsmithTableRecipes.Recipe(Items.NAME_TAG, Items.DIAMOND, ModItems.DIAMOND_REROLL_TOKEN.get(), 100),
            new WeaponsmithTableRecipes.Recipe(Items.NAME_TAG, Items.NETHERITE_INGOT, ModItems.NETHERITE_REROLL_TOKEN.get(), 100));
    }
}

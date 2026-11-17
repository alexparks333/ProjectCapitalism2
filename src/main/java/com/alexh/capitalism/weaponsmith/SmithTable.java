package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

// The smith tables: same block, lava tank and 2-slot menu (SmithTableBlock / SmithTableBlockEntity / SmithTableMenu),
// each with its own job, name, recipes and what it repairs with the dent game (DentForging).
public enum SmithTable {
    WEAPONSMITH(Job.WEAPONSMITH, "Weaponsmith Table"),
    TOOLSMITH(Job.TOOLSMITH, "Toolsmith Table"),
    ARMORSMITH(Job.ARMORSMITH, "Armorsmith Table");

    public final Job job;
    public final String title;

    SmithTable(Job job, String title) {
        this.job = job;
        this.title = title;
    }

    public List<WeaponsmithTableRecipes.Recipe> recipes() {
        return switch (this) {
            case WEAPONSMITH -> WeaponsmithTableRecipes.recipes();
            case TOOLSMITH -> ToolsmithTableRecipes.recipes();
            // Armor Whetstone: Grindstone + Compacted Iron (hammers the dents out of armor, see Wear)
            case ARMORSMITH -> {
                List<WeaponsmithTableRecipes.Recipe> recipes = new java.util.ArrayList<>();
                recipes.add(new WeaponsmithTableRecipes.Recipe(Items.GRINDSTONE, ModItems.COMPACTED_IRON.get(),
                    ModItems.ARMOR_WHETSTONE.get(), 250));
                recipes.addAll(ToolsmithTableRecipes.rerollTokens());
                yield recipes;
            }
        };
    }

    // What the Forge button (the dent game) repairs here. Axes are both weapons and tools: either table. Shields: the
    // Weaponsmith or the Armorsmith.
    public boolean repairs(ItemStack stack) {
        return switch (this) {
            case WEAPONSMITH -> WeaponsmithTableRecipes.isWeapon(stack) || stack.is(net.minecraft.world.item.Items.SHIELD);
            case TOOLSMITH -> stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(ItemTags.AXES);
            case ARMORSMITH -> stack.is(net.minecraft.world.item.Items.SHIELD) || stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR)
                || stack.is(ItemTags.FOOT_ARMOR);
        };
    }

    public Block block() {
        return switch (this) {
            case WEAPONSMITH -> ModBlocks.WEAPONSMITH_TABLE.get();
            case TOOLSMITH -> ModBlocks.TOOLSMITH_TABLE.get();
            case ARMORSMITH -> ModBlocks.ARMORSMITH_TABLE.get();
        };
    }

    public MenuType<SmithTableMenu> menuType() {
        return switch (this) {
            case WEAPONSMITH -> ModMenus.WEAPONSMITH_TABLE.get();
            case TOOLSMITH -> ModMenus.TOOLSMITH_TABLE.get();
            case ARMORSMITH -> ModMenus.ARMORSMITH_TABLE.get();
        };
    }
}

package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.List;

// Salvaging at a smith table (any of the three): gear in the big slot + a Salvage Hammer in the small slot shows the
// Salvage button (where Forge is). Pressing it breaks the gear down into 1 of its material (a gold sword -> 1 Gold Ingot,
// a diamond pickaxe -> 1 Diamond...), for a little lava and 1 use of the hammer (35 uses). The material is what repairs
// it (an anvil's repair material); leather, wood and stone gear can't be salvaged.
// The Salvage Hammer can only be crafted by smiths (data/capitalism/recipe/salvage_hammer.json, job_shaped).
public final class Salvaging {

    public static final int SALVAGE_LAVA = 100;
    public static final int HAMMER_USES = 35;

    // What a piece of gear can give back, best first (netherite gear only takes Netherite Ingots, so no overlap)
    private static final List<Item> MATERIALS = List.of(Items.NETHERITE_INGOT, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT,
        Items.COPPER_INGOT);

    // Why the Salvage button can or can't be used (synced to the client as its ordinal)
    public enum State {
        NONE,       // not a salvage: no button
        READY,
        NEEDS_LAVA
    }

    private Salvaging() {
    }

    public static boolean isHammer(ItemStack stack) {
        return stack.is(ModItems.SALVAGE_HAMMER.get());
    }

    // The 1 material this gear gives back, or empty if it can't be salvaged
    public static ItemStack result(ItemStack gear) {
        if (gear.isEmpty() || isHammer(gear)) return ItemStack.EMPTY;
        for (Item material : MATERIALS) {
            ItemStack one = new ItemStack(material);
            if (gear.isValidRepairItem(one)) return one;
        }
        return ItemStack.EMPTY;
    }

    public static State state(ItemStack gear, ItemStack hammer, int lava) {
        if (!isHammer(hammer) || result(gear).isEmpty()) return State.NONE;
        return lava < SALVAGE_LAVA ? State.NEEDS_LAVA : State.READY;
    }

    // One use of the hammer; returns false if that broke it
    public static boolean useHammer(ItemStack hammer) {
        int damage = hammer.getDamageValue() + 1;
        if (damage >= hammer.getMaxDamage()) {
            hammer.shrink(1);
            return false;
        }
        hammer.setDamageValue(damage);
        return true;
    }

    public static @Nullable String describe(ItemStack gear) {
        ItemStack back = result(gear);
        return back.isEmpty() ? null : "1 " + back.getHoverName().getString();
    }
}

package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Reroll Tokens: give a sword or tool brand new stats. Made at the Toolsmith Table (Name Tag + Iron Ingot / Diamond /
// Netherite Ingot, see ToolsmithTableRecipes). Used at a smith table: the item in the big slot + the token of ITS
// material in the small slot (Diamond Token for diamond gear...), for a little lava, no game, no XP.
//  - Weaponsmith Table: swords (Sharpness and Edge, SwordForging).
//  - Toolsmith Table: pickaxes, shovels and axes (Efficiency, ToolForging).
// The result slot shows the item with its stats wiped ("?/max" in the tooltip); the new stats are rolled when it's
// taken out, by whoever takes it (the smith, so the smith's ranges and Grail chance). Everything else (enchantments,
// name, durability, hits since sharpening) stays.
public final class Rerolling {

    public static final int REROLL_LAVA = 100;

    public enum Tier {
        IRON(Items.IRON_INGOT),
        DIAMOND(Items.DIAMOND),
        NETHERITE(Items.NETHERITE_INGOT);

        public final Item material;

        Tier(Item material) {
            this.material = material;
        }

        public Item token() {
            return switch (this) {
                case IRON -> ModItems.IRON_REROLL_TOKEN.get();
                case DIAMOND -> ModItems.DIAMOND_REROLL_TOKEN.get();
                case NETHERITE -> ModItems.NETHERITE_REROLL_TOKEN.get();
            };
        }
    }

    private Rerolling() {
    }

    public static @Nullable Tier tokenTier(ItemStack token) {
        for (Tier tier : Tier.values()) {
            if (token.is(tier.token())) return tier;
        }
        return null;
    }

    // The item's material, going by what repairs it (iron gear takes Iron Ingots...). Wood, stone, gold, copper: none.
    public static @Nullable Tier itemTier(ItemStack item) {
        for (Tier tier : Tier.values()) {
            if (item.isValidRepairItem(new ItemStack(tier.material))) return tier;
        }
        return null;
    }

    public static boolean canReroll(SmithTable kind, ItemStack item, ItemStack token) {
        boolean hasStats = switch (kind) {
            case WEAPONSMITH -> Sharpness.isSword(item);
            case TOOLSMITH -> ToolForging.isTool(item);
            case ARMORSMITH -> false;
        };
        Tier tier = tokenTier(token);
        return hasStats && tier != null && tier == itemTier(item);
    }

    // What the result slot shows: the same item with its stats wiped (so nothing can be peeked at)
    public static ItemStack preview(ItemStack item) {
        ItemStack copy = item.copyWithCount(1);
        copy.remove(ModComponents.SWORD_SHARPNESS.get());
        copy.remove(ModComponents.SWORD_EDGE.get());
        copy.remove(ModComponents.TOOL_EFFICIENCY.get());
        return copy;
    }

    // Taken out of the result slot: roll its new stats (server only; does nothing if it already has them)
    public static void roll(ItemStack stack, Player taker) {
        if (taker.level().isClientSide()) return;
        SwordForging.roll(stack, taker, taker.getRandom());
        ToolForging.roll(stack, taker, taker.getRandom());
    }
}

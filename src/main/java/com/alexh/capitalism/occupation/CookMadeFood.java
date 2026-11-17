package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.economy.ContainerLocks;
import com.alexh.capitalism.economy.ContainerOwner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

// Vanilla food made by a Cook is Gourmet: it escapes the food debuff (FoodNerf) and gets its full vanilla hunger and
// saturation back, and counts as Gourmet everywhere (tooltip star, gold drumsticks, plates; KitchenRecipes.isGourmet).
// "Made" = cooked in a Cook's own Furnace / Smoker / Blast Furnace (Gourmet right in the output slot,
// mixin/AbstractFurnaceBlockEntityMixin), taken out of any furnace by a Cook (click or shift-click,
// mixin/AbstractFurnaceMenuMixin), or crafted by a Cook (bread, pumpkin pie, stews, cookies...; mixin/ItemMixin).
// "Cook" = a player who really has the Cook job.
// It's marked on the stack (ModComponents.COOK_MADE), so it doesn't stack with ordinary food of the same kind.
public final class CookMadeFood {

    private static final int OUTPUT_SLOT = 2;

    private CookMadeFood() {
    }

    public static boolean isCookMade(ItemStack stack) {
        return stack.getOrDefault(ModComponents.COOK_MADE.get(), false);
    }

    // A player made this stack: if they're a Cook and it's a (debuffed) vanilla food, it's Cook-made
    public static void madeBy(ItemStack stack, Player player) {
        if (player.level().isClientSide() || !JobEvents.has(player, Job.COOK)) return;
        makeCookMade(stack);
    }

    // What a furnace makes this tick (mixin/AbstractFurnaceBlockEntityMixin): Cook-made if the furnace belongs to a Cook
    // (whoever placed it, if they're online). It always matches what's already in the output slot, though, so the furnace
    // can keep stacking its output and never stalls (e.g. ordinary porkchops already there: it keeps making ordinary ones
    // until they're taken out; a Cook taking them out still makes them Cook-made).
    public static ItemStack furnaceResult(ItemStack result, AbstractFurnaceBlockEntity furnace, ServerLevel level) {
        if (FoodNerf.fullFood(result.getItem()) == null) return result;
        ItemStack output = furnace.getItem(OUTPUT_SLOT);
        boolean cookMade;
        if (!output.isEmpty() && output.is(result.getItem())) {
            cookMade = isCookMade(output);
        } else {
            ContainerOwner owner = ContainerLocks.ownerOf(furnace);
            ServerPlayer player = owner == null || owner.equals(ContainerOwner.NONE) ? null
                : level.getServer().getPlayerList().getPlayer(owner.owner());
            cookMade = player != null && JobEvents.has(player, Job.COOK);
        }
        if (cookMade) makeCookMade(result);
        return result;
    }

    // Full strength + the Cook-made mark (does nothing for food that isn't debuffed, or that's already Cook-made)
    public static void makeCookMade(ItemStack stack) {
        if (stack.isEmpty() || isCookMade(stack)) return;
        FoodProperties full = FoodNerf.fullFood(stack.getItem());
        if (full == null) return;
        stack.set(DataComponents.FOOD, full);
        stack.set(ModComponents.COOK_MADE.get(), true);
    }
}

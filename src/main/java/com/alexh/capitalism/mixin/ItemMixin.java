package com.alexh.capitalism.mixin;

import com.alexh.capitalism.occupation.CookMadeFood;
import com.alexh.capitalism.weaponsmith.SwordForging;
import com.alexh.capitalism.weaponsmith.ToolForging;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Swords and tools get their stats when they're crafted (weaponsmith/SwordForging, weaponsmith/ToolForging). Both run on the real item before it
// leaves the result slot (click, shift-click or drop), so it's rolled exactly once and can't be re-rolled by
// shuffling the crafting grid. Server only: the client never rolls its own numbers.
@Mixin(Item.class)
public abstract class ItemMixin {

    // A player crafted it (crafting table, inventory grid, smithing table...): Weaponsmiths forge better swords,
    // Toolsmiths better tools.
    @Inject(method = "onCraftedBy", at = @At("HEAD"))
    private void capitalism$forgeSword(ItemStack stack, Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        SwordForging.roll(stack, player, player.getRandom());
        ToolForging.roll(stack, player, player.getRandom());
        // vanilla food a Cook makes (crafted, or taken out of a furnace) is full-strength Gourmet
        CookMadeFood.madeBy(stack, player);
    }

    // A Crafter block made it (no player): counts as "everyone else". Players already rolled in onCraftedBy above.
    @Inject(method = "onCraftedPostProcess", at = @At("HEAD"))
    private void capitalism$forgeSwordBySystem(ItemStack stack, Level level, CallbackInfo ci) {
        if (level.isClientSide()) return;
        SwordForging.roll(stack, null, level.getRandom());
        ToolForging.roll(stack, null, level.getRandom());
    }
}

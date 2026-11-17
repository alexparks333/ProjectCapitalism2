package com.alexh.capitalism.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Reaches the Smithing Table's player and result slot (protected in vanilla).
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("player")
    Player capitalism$getPlayer();

    @Accessor("resultSlots")
    ResultContainer capitalism$getResultSlots();
}

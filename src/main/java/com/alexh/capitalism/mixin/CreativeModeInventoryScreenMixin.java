package com.alexh.capitalism.mixin;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Creative search adds the creative tab an item is in to its tooltip ("Functional Blocks"). Skill books
// (librarian/SkillBookItem) are only in a tab for testing, so their tooltips leave it out.
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void capitalism$noTabName(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (!stack.is(ModItems.SKILL_BOOK.get())) return;
        Set<String> tabs = new HashSet<>();
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) tabs.add(tab.getDisplayName().getString());
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        for (int i = lines.size() - 1; i >= 1; i--) {
            if (tabs.contains(lines.get(i).getString())) lines.remove(i);
        }
        cir.setReturnValue(lines);
    }
}

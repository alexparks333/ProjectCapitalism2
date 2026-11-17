package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

// A Fish Plaque as an item (fishing/FishPlaqueBlock, Fishing.md > Plaques): its tooltip names the catch mounted on it
// and keeps its story, and says how wide a wall it needs.
public class FishPlaqueItem extends BlockItem {

    public FishPlaqueItem(Block block, Properties properties) {
        super(block, properties);
    }

    // "Fish Plaque" in its catch's tier colour
    @Override
    public Component getName(ItemStack stack) {
        RareCatch kind = RareCatch.byId(stack.getOrDefault(ModComponents.PLAQUE_CATCH.get(), ""));
        return kind == null ? super.getName(stack) : kind.tier().paint(super.getName(stack).copy());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        RareCatch kind = RareCatch.byId(stack.getOrDefault(ModComponents.PLAQUE_CATCH.get(), ""));
        if (kind == null) {
            builder.accept(Component.literal("Empty: the Fisherman mounts a rare catch on one").withStyle(ChatFormatting.GRAY));
            return;
        }
        builder.accept(kind.tier().paint(Component.literal(kind.title)));
        builder.accept(kind.tier().line());
        for (String line : kind.chanceLines()) builder.accept(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        CatchInfo info = stack.get(ModComponents.CATCH_INFO.get());
        if (info != null) builder.accept(Component.literal(info.line()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("Hangs on a wall, " + kind.plaque + (kind.plaque == 1 ? " block" : " blocks") + " wide")
            .withStyle(ChatFormatting.DARK_AQUA));
    }
}

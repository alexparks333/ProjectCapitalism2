package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

// A rare catch (fishing/RareCatch, Fishing.md): its tooltip tells its weight, who caught it and when ("312 lb · caught by
// Alex · Summer 9, Year 2"). The Fisherman buys it (more the heavier it is) or mounts it on a wall plaque. The Message in
// a Bottle and the Sunken Pirate Chest are their own items (opened with a right-click) but count as rare catches too.
public class RareCatchItem extends Item {

    private final RareCatch kind;

    public RareCatchItem(RareCatch kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public RareCatch kind() {
        return this.kind;
    }

    // its name in its tier's colour
    @Override
    public Component getName(ItemStack stack) {
        return this.kind.tier().paint(super.getName(stack).copy());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(this.kind.tier().line());
        for (String line : this.kind.chanceLines()) builder.accept(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        CatchInfo info = stack.get(ModComponents.CATCH_INFO.get());
        if (info != null) builder.accept(Component.literal(info.line()).withStyle(ChatFormatting.GRAY));
        if (this.kind.gold > 0) builder.accept(Component.literal("🎣 A rare catch: the Fisherman buys it, or mounts it on a plaque").withStyle(ChatFormatting.DARK_AQUA));
    }
}

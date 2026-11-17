package com.alexh.capitalism.occupation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

// A Wishing Star (Jobs.md > The Wishing Well): Essential or Fun. Wished on the Wishing Well (right-click the well with
// it, WishingWellBlock) it gives one more job slot of its kind, then /pick fills it. The Essential one is far rarer.
public class WishingStarItem extends Item {

    public final Job.Category category;

    public WishingStarItem(Job.Category category, Properties properties) {
        super(properties);
        this.category = category;
    }

    // used anywhere else: a hint
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.literal("Wish on the Wishing Well with it (right-click the well).").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }
}

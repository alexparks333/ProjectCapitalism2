package com.alexh.capitalism.item;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

// Silver / Golden Plate item: only Cooks can set plates down.
public class PlateItem extends BlockItem {

    public PlateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isCreative() && !JobEvents.has(player, Job.COOK)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.literal("Only Cooks can set out plates."));
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}

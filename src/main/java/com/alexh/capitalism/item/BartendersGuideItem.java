package com.alexh.capitalism.item;

import com.alexh.capitalism.client.BartendersGuideScreen;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

// Right-click to open the recipe viewer. It is built from the live recipe lists, so it is always up to date.
// Only Bartenders can read it (same rule as /guides).
public class BartendersGuideItem extends Item {

    public BartendersGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!JobEvents.has(player, Job.BARTENDER) && !player.isCreative()) {
            if (level.isClientSide()) player.sendOverlayMessage(Component.literal("Only Bartenders can make sense of this book."));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            BartendersGuideScreen.open();
        }
        return InteractionResult.SUCCESS;
    }
}

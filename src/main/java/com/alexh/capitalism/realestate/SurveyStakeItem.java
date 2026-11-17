package com.alexh.capitalism.realestate;

import com.alexh.capitalism.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The Survey Stake (RealEstate.md), for ops marking out lots: left-click a block for one corner (RealEstateEvents),
// right-click a block for the other, then /properties create <name>. While it's held the corners show as a box of
// particles, and the lots already made show too (RealEstateEvents).
public class SurveyStakeItem extends Item {

    // each op's corners so far (server only, forgotten on restart)
    private static final Map<UUID, BlockPos[]> CORNERS = new HashMap<>();

    public SurveyStakeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static BlockPos[] corners(ServerPlayer player) {
        return CORNERS.computeIfAbsent(player.getUUID(), id -> new BlockPos[2]);
    }

    public static void mark(ServerPlayer player, int corner, BlockPos pos) {
        if (!TownProtection.canBuild(player)) {
            player.sendOverlayMessage(Component.literal("Only an op can survey lots."));
            return;
        }
        corners(player)[corner] = pos.immutable();
        BlockPos[] c = corners(player);
        String size = c[0] != null && c[1] != null
            ? "  (" + (Math.abs(c[0].getX() - c[1].getX()) + 1) + " x " + (Math.abs(c[0].getZ() - c[1].getZ()) + 1) + ")" : "";
        player.sendOverlayMessage(Component.literal("Corner " + (corner + 1) + ": " + pos.getX() + ", " + pos.getZ() + size));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) mark(player, 1, context.getClickedPos());
        return InteractionResult.SUCCESS;
    }
}

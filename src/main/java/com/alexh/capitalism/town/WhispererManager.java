package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

// Keeps exactly one Whisperer in town (Town.md). Every CHECK_TICKS (30 s), when the town is loaded (its corners and
// middle): no Whisperer around -> one appears on a Stone Brick street; more than one -> the extras leave.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class WhispererManager {

    private static final int CHECK_TICKS = 600;

    private WhispererManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_TICKS != 0) return;
        ServerLevel level = event.getServer().overworld();
        Town.Area area = Town.walkArea();
        BlockPos middle = area.center(64);
        for (BlockPos corner : List.of(middle, new BlockPos(area.minX(), 64, area.minZ()), new BlockPos(area.maxX(), 64, area.maxZ()))) {
            if (!level.isLoaded(corner)) return;
        }
        List<Whisperer> found = level.getEntities(ModEntities.WHISPERER.get(), area.box(level.getMinY(), level.getMaxY()).inflate(32), w -> true);
        for (int i = 1; i < found.size(); i++) found.get(i).discard();
        if (!found.isEmpty()) return;
        BlockPos spot = Whisperer.pathSpot(level, level.getRandom(), middle, Math.max(area.maxX() - area.minX(), area.maxZ() - area.minZ()));
        if (spot == null) return;
        Whisperer whisperer = Whisperer.create(level, spot);
        level.addFreshEntity(whisperer);
    }
}

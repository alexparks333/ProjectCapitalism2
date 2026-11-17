package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

// Gold Leaderboard (net worth) on the sidebar. Every few seconds, in one tick:
//  1. Each online player's personal gold (inventory, cursor, ender chest) is recorded.
//  2. Every owned container in a loaded chunk is recounted (its own slots only). Containers that were
//     broken or lost their owner are dropped from the ledger. Unloaded ones keep their last count.
//  3. Each known player's score = personal gold + all their containers, in whole gold.
// Everything is counted in one tick, so gold moving between places can never be counted twice.
@EventBusSubscriber(modid = Capitalism.MODID)
public class GoldLeaderboard {

    private static final String OBJECTIVE = "gold_lb";

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CapitalismConfig.LEADERBOARD_INTERVAL_TICKS.get() != 0) return;

        GoldLedger ledger = GoldLedger.get(server);
        recount(server, ledger);

        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE);
        // a Blood Moon has the sidebar for its kills (event/BloodMoon)
        if (com.alexh.capitalism.event.BloodMoon.isActive()) return;
        if (!CapitalismConfig.LEADERBOARD_ENABLED.get()) {
            if (objective != null && scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) == objective) {
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            }
            return;
        }
        if (objective == null) {
            objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
                Component.literal("Gold Leaderboard").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        if (scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) != objective) {
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        }

        for (UUID id : ledger.knownPlayers()) {
            String name = ledger.nameOf(id);
            if (name == null || name.isEmpty()) continue;
            long gold = GoldMoney.toGold(ledger.netWorth(id));
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective).set((int) Math.min(gold, Integer.MAX_VALUE));
        }
    }

    // Whose gold this container is, for net worth (and the death tax: deathtax/GoldCollector takes only from these), or
    // null: no owner, or the town's (the Mayoral Chest and the Town Hall Mailbox never get one; a City Chest holds the
    // town's gold, what agents pay in for a lot, RealEstate.md: it counts for no one, not the op who placed it)
    public static @org.jspecify.annotations.Nullable ContainerOwner ledgerOwner(@org.jspecify.annotations.Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof com.alexh.capitalism.realestate.CityChestBlockEntity) return null;
        return ContainerLocks.ownerOf(blockEntity);
    }

    // Brings the ledger up to date with everything that's currently loaded.
    public static void recount(MinecraftServer server, GoldLedger ledger) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ledger.setPlayer(player.getUUID(), player.getGameProfile().name(), GoldMoney.personalNetWorth(player));
        }

        for (GlobalPos pos : ledger.containerPositions()) {
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) {
                ledger.removeContainer(pos);
                continue;
            }
            // Never load a chunk just to count it; unloaded containers keep their last count.
            if (!level.isLoaded(pos.pos())) continue;

            // The owner stored on the block itself is the truth; the ledger just remembers it.
            BlockEntity blockEntity = level.getBlockEntity(pos.pos());
            ContainerOwner owner = ledgerOwner(blockEntity);
            if (owner == null) {
                ledger.removeContainer(pos);
                continue;
            }
            long gold = GoldMoney.deepCount((Container) blockEntity);
            if (blockEntity instanceof ShopfrontBlockEntity shop) {
                gold += shop.earnings();
            }
            ledger.setContainer(pos, owner.owner(), gold);
        }
    }
}

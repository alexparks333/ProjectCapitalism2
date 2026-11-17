package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

// The protected town center (Town.isProtected): nobody can break or place blocks there (creative players included),
// except ops, and on a lot of town land its owner and its tenant (realestate/Properties, RealEstate.md). Explosions
// leave its blocks alone. Below TOWN_PROTECT_MIN_Y it's ordinary ground.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TownProtection {

    private TownProtection() {
    }

    // An op: builds anywhere
    public static boolean canBuild(Player player) {
        return player instanceof ServerPlayer serverPlayer && serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    // Can this player build at this spot of the town center: an op, or the owner or tenant of the lot it's on
    public static boolean canBuildAt(Player player, Level level, BlockPos pos) {
        return canBuild(player) || com.alexh.capitalism.realestate.Properties.canBuild(player, level, pos);
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || !Town.isProtected(level, event.getPos())) return;
        Player player = event.getPlayer();
        if (canBuildAt(player, level, event.getPos())) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("This is the town center: you can only build on a lot you own or lease."));
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || !Town.isProtected(level, event.getPos())) return;
        if (!(event.getEntity() instanceof Player player) || canBuildAt(player, level, event.getPos())) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("This is the town center: you can only build on a lot you own or lease."));
        }
    }

    // Explosions (creepers, TNT, fireballs, the Wither) don't ruin the town: its blocks stay, and so do the things hung or
    // stood about in it (item frames, paintings, armor stands). Mobs and players in the blast still get hurt.
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> Town.isProtected(event.getLevel(), pos));
        event.getAffectedEntities().removeIf(e -> (e instanceof net.minecraft.world.entity.decoration.HangingEntity
            || e instanceof net.minecraft.world.entity.decoration.ArmorStand) && Town.isProtected(event.getLevel(), e.blockPosition()));
    }

    // And hostile mobs don't grief in it: no Enderman takes a block, no zombie breaks a door, no Ravager tramples, no
    // blaze or ghast fireball starts a fire, no Wither breaks through. (Only hostile mobs: villagers still farm, sheep
    // still eat grass.)
    @SubscribeEvent
    public static void onMobGriefing(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy && Town.isProtected(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }
}

package com.alexh.capitalism.realestate;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Real estate's running parts (RealEstate.md): the rent every Minecraft day (Properties.collectRent; checked every 10
// seconds, so a tenant who logs in pays what's due), rent paid to someone while they were away handed over when they
// log in, and the Survey Stake: left-click marks its first corner, and while it's held the lots show as boxes of
// particles (the corners being marked too).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class RealEstateEvents {

    private RealEstateEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 200 != 0) return;
        Properties.get(server).collectRent(server, Properties.today(server));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Properties.get(player.level().getServer()).payOwed(player);
    }

    // the Survey Stake's left-click: the first corner (and no breaking the block)
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getItemStack().is(ModItems.SURVEY_STAKE.get())) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            SurveyStakeItem.mark(player, 0, event.getPos());
        }
    }

    // Holding the Survey Stake: the lots nearby as green boxes, the corners being marked as a white one
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        if (!player.getMainHandItem().is(ModItems.SURVEY_STAKE.get()) || player.level().dimension() != Level.OVERWORLD) return;
        ServerLevel level = player.level();
        int y = player.getBlockY() + 1;
        for (Property p : Properties.get(level.getServer()).all()) {
            if (Math.abs(p.minX - player.getX()) > 96 && Math.abs(p.maxX - player.getX()) > 96) continue;
            if (Math.abs(p.minZ - player.getZ()) > 96 && Math.abs(p.maxZ - player.getZ()) > 96) continue;
            outline(player, level, p.minX, p.minZ, p.maxX, p.maxZ, y, ParticleTypes.HAPPY_VILLAGER);
        }
        BlockPos[] c = SurveyStakeItem.corners(player);
        if (c[0] != null && c[1] != null) outline(player, level, c[0].getX(), c[0].getZ(), c[1].getX(), c[1].getZ(), y, ParticleTypes.END_ROD);
        else if (c[0] != null) outline(player, level, c[0].getX(), c[0].getZ(), c[0].getX(), c[0].getZ(), y, ParticleTypes.END_ROD);
    }

    // A box round the edge of these blocks (every block along the edge), shown to this player only
    private static void outline(ServerPlayer player, ServerLevel level, int x1, int z1, int x2, int z2, int y, ParticleOptions particle) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2) + 1;
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2) + 1;
        int step = Math.max(1, Math.max(maxX - minX, maxZ - minZ) / 60);
        for (int x = minX; x <= maxX; x += step) {
            level.sendParticles(player, particle, true, false, x, y, minZ, 1, 0, 0, 0, 0);
            level.sendParticles(player, particle, true, false, x, y, maxZ, 1, 0, 0, 0, 0);
        }
        for (int z = minZ; z <= maxZ; z += step) {
            level.sendParticles(player, particle, true, false, minX, y, z, 1, 0, 0, 0, 0);
            level.sendParticles(player, particle, true, false, maxX, y, z, 1, 0, 0, 0, 0);
        }
    }
}

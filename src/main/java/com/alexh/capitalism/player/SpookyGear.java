package com.alexh.capitalism.player;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// The Blood Merchant's Halloween gear (Town.md > The Blood Merchant > Spooky):
//  - the Ghost Sheet (a cosmetic worn on the head, drawn by client/CosmeticHatLayer): hostile mobs spot you from
//    SHEET_VISIBILITY of the usual distance, and at night you leave a faint trail of wisps while you move;
//  - the Witch's Broom: player/WitchsBroomItem.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class SpookyGear {

    public static final double SHEET_VISIBILITY = 0.75;

    private SpookyGear() {
    }

    public static boolean wearingSheet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GHOST_SHEET.get());
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && wearingSheet(player) && event.getLookingEntity() instanceof Enemy) {
            event.modifyVisibility(SHEET_VISIBILITY);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 4 != 0 || !wearingSheet(player)) return;
        if (player.isInvisible() || player.isSpectator()) return;
        ServerLevel level = player.level();
        if (!com.alexh.capitalism.decor.BloodDecorBlockEntity.isNight(level)) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() < 0.0004) return;
        level.sendParticles(ParticleTypes.WHITE_ASH, player.getX(), player.getY() + 0.4 + player.getRandom().nextDouble() * 0.8, player.getZ(),
            3, 0.2, 0.2, 0.2, 0.0);
        if (player.getRandom().nextFloat() < 0.25F) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.6, player.getZ(), 1, 0.15, 0.3, 0.15, 0.005);
        }
    }
}

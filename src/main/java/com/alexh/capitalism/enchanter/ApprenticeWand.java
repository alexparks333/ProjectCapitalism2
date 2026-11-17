package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.guide.Discoveries;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

// The Apprentice Wand (Enchanter's Table: Bamboo + Potion of Poison, 4 uses): an Enchanter hits an armor stand wearing a
// full set of one kind of armor with it, and the stand becomes an Apprentice (same armor, same spot, owned by them).
// Anything in the stand's hands drops. The armor stand itself is used up.
@EventBusSubscriber(modid = Capitalism.MODID)
public class ApprenticeWand {

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ArmorStand stand) || stand instanceof Apprentice || stand.isMarker()) return;
        ItemStack wand = event.getEntity().getMainHandItem();
        if (!wand.is(ModItems.APPRENTICE_WAND.get())) return;
        // never knock the stand over with the wand
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;

        if (!player.isCreative() && !JobEvents.has(player, Job.ENCHANTER)) {
            player.sendOverlayMessage(Component.literal("Only Enchanters can use the Apprentice Wand."));
            return;
        }
        Apprentice.Kind kind = Apprentice.Kind.of(stand);
        if (kind == null) {
            player.sendOverlayMessage(Component.literal("It needs a full set of one kind of armor."));
            level.playSound(null, stand.getX(), stand.getY(), stand.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return;
        }

        Apprentice apprentice = new Apprentice(ModEntities.APPRENTICE.get(), level);
        apprentice.snapTo(stand.getX(), stand.getY(), stand.getZ(), stand.getYRot(), 0.0F);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            apprentice.setItemSlot(slot, stand.getItemBySlot(slot).copy());
        }
        for (EquipmentSlot hand : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack held = stand.getItemBySlot(hand);
            if (!held.isEmpty()) stand.spawnAtLocation(level, held.copy());
        }
        apprentice.setup(kind, 0, player.getUUID());
        stand.discard();
        level.addFreshEntity(apprentice);
        Discoveries.discover(player, apprentice.toItem());

        double x = apprentice.getX();
        double y = apprentice.getY() + 1.0;
        double z = apprentice.getZ();
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 60, 0.4, 0.8, 0.4, 1.0);
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, x, y, z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.9F, 1.2F);
        level.playSound(null, x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendOverlayMessage(Component.literal("A " + kind.title + " Apprentice! It can hold " + kind.capacity() + " XP."));
        if (!player.isCreative()) wand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
}

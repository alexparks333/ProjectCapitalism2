package com.alexh.capitalism.decor;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.event.BloodMoon;
import com.alexh.capitalism.network.ThirstSyncPacket;
import com.alexh.capitalism.player.ThirstData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

// The Blood Moon decorations' doings that aren't any one block's (Decorations.md > Blood Moon decorations):
//  - everyone asleep and someone's in a Vampire Coffin by day: the sleep skips to dusk, not the next morning;
//  - sleeping right through the night in the Four-Poster Crimson Bed fills your water, golden drops and all;
//  - a Bottle of Blood's strength is taken away once the Blood Moon is over: when it ends (BloodMoon.end), and also on
//    logging in, on waking, and every few seconds if somehow none of those caught it (BottleOfBloodItem.stripBlood).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BloodDecorEvents {

    private BloodDecorEvents() {
    }

    // what someone's sleeping in (null: not one of ours)
    static @Nullable BloodDecorKind sleptIn(ServerPlayer player) {
        BlockPos bed = player.getSleepingPos().orElse(null);
        if (bed == null) return null;
        BlockState state = player.level().getBlockState(bed);
        return state.getBlock() instanceof BloodBedBlock b ? b.kind() : null;
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long clock = Calendar.clock(level);
        long timeOfDay = Math.floorMod(clock, Calendar.DAY_TICKS);
        boolean coffin = false;
        for (ServerPlayer p : level.players()) coffin |= p.isSleeping() && sleptIn(p) == BloodDecorKind.VAMPIRE_COFFIN;
        if (coffin && timeOfDay < 12000) {
            // a vampire's day's sleep ends at dusk
            event.setAdjustment(new ClockAdjustment.Absolute(clock - timeOfDay + 13000));
            return;
        }
        // a whole night slept through in the canopy bed (it skips to morning): water full, golden drops and all, body heat 70%
        for (ServerPlayer p : level.players()) {
            if (p.isSleeping() && sleptIn(p) == BloodDecorKind.CRIMSON_CANOPY_BED) refill(p);
        }
    }

    // the water bar full, every golden drop too, body heat back to just right (70%, dry), and told to the player
    public static void refill(ServerPlayer player) {
        ThirstData thirst = player.getData(ModAttachments.THIRST.get());
        thirst.fillCompletely();
        com.alexh.capitalism.player.BodyTemperature.reset(player.getData(ModAttachments.WELLBEING.get()));
        PacketDistributor.sendToPlayer(player, ThirstSyncPacket.of(thirst, player));
        player.sendSystemMessage(Component.literal("You wake refreshed in the crimson bed: water full, golden drops and all, and your body warm and dry.")
            .withStyle(ChatFormatting.GOLD));
    }

    // ── the Bottle of Blood never outlasts its night ──

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !BloodMoon.isActive()) BottleOfBloodItem.stripBlood(p);
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !BloodMoon.isActive()) BottleOfBloodItem.stripBlood(p);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 != 13 || BloodMoon.isActive()) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) BottleOfBloodItem.stripBlood(p);
    }
}

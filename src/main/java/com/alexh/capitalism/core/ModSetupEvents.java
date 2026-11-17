package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.network.DietSyncPacket;
import com.alexh.capitalism.network.DrinkWaterPacket;
import com.alexh.capitalism.network.TreasureSightPacket;
import com.alexh.capitalism.network.DiscoverySyncPacket;
import com.alexh.capitalism.guide.ClientDiscoveries;
import com.alexh.capitalism.network.EnderLeapPacket;
import com.alexh.capitalism.player.MealEffects;
import com.alexh.capitalism.network.JobSyncPacket;
import com.alexh.capitalism.network.PickJobPacket;
import com.alexh.capitalism.network.OpenApprenticeNamePacket;
import com.alexh.capitalism.network.RenameApprenticePacket;
import com.alexh.capitalism.enchanter.Apprentice;
import com.alexh.capitalism.occupation.JobCommands;
import com.alexh.capitalism.occupation.ClientJobData;
import com.alexh.capitalism.network.QuietHealthPacket;
import com.alexh.capitalism.network.ThirstSyncPacket;
import com.alexh.capitalism.network.WellbeingSyncPacket;
import com.alexh.capitalism.player.ClientWellnessData;
import com.alexh.capitalism.player.ClientThirstData;
import com.alexh.capitalism.player.WaterDrinking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Capitalism.MODID)
public class ModSetupEvents {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(Capitalism.MODID);
        registrar.playToClient(
            DietSyncPacket.TYPE,
            DietSyncPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                ClientWellnessData.diet = packet.values();
                ClientWellnessData.meals = packet.meals();
                ClientWellnessData.mealsNeeded = packet.mealsNeeded();
            })
        );
        registrar.playToClient(
            WellbeingSyncPacket.TYPE,
            WellbeingSyncPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                ClientWellnessData.wellbeing = packet.score();
                ClientWellnessData.wellbeingBuffAt = packet.buffAt();
                ClientWellnessData.wellbeingMax = packet.max();
                ClientWellnessData.wellbeingDebuffAt = packet.debuffAt();
                ClientWellnessData.wellbeingMin = packet.min();
                ClientWellnessData.neglect = packet.neglect();
                ClientWellnessData.bodyHeat = packet.bodyHeat();
                ClientWellnessData.heatReasons = packet.heatReasons();
                ClientWellnessData.grace = packet.grace();
                ClientWellnessData.raw = packet.raw();
                ClientWellnessData.effective = packet.effective();
                ClientWellnessData.pending = packet.pending();
                ClientWellnessData.healthMod = packet.healthMod();
            })
        );
        registrar.playToClient(
            QuietHealthPacket.TYPE,
            QuietHealthPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                // lower health before vanilla does, so it doesn't look like a hit
                if (context.player().getHealth() > packet.health()) context.player().setHealth(packet.health());
            })
        );
        registrar.playToClient(
            JobSyncPacket.TYPE,
            JobSyncPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                ClientJobData.jobs = packet.jobs();
                ClientJobData.essentialSlots = packet.essentialSlots();
                ClientJobData.funSlots = packet.funSlots();
                ClientJobData.changing = packet.changing();
            })
        );
        registrar.playToServer(
            PickJobPacket.TYPE,
            PickJobPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) JobCommands.togglePicked(player, packet.job());
            })
        );
        registrar.playToServer(
            DrinkWaterPacket.TYPE,
            DrinkWaterPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) WaterDrinking.drinkFromSource(player);
            })
        );
        registrar.playToServer(
            com.alexh.capitalism.network.RenamePetPacket.TYPE,
            com.alexh.capitalism.network.RenamePetPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) com.alexh.capitalism.pets.Pets.rename(player, packet.entityId(), packet.name());
            })
        );
        registrar.playToServer(
            com.alexh.capitalism.network.PetHatPacket.TYPE,
            com.alexh.capitalism.network.PetHatPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.alexh.capitalism.pets.Pets.wearHat(player, packet.entityId(), com.alexh.capitalism.designer.Hat.byIndex(packet.hat()));
                }
            })
        );
        registrar.playToServer(
            com.alexh.capitalism.network.PetActionPacket.TYPE,
            com.alexh.capitalism.network.PetActionPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) com.alexh.capitalism.pets.Pets.action(player, packet.entityId(), packet.action());
            })
        );
        registrar.playToServer(
            EnderLeapPacket.TYPE,
            EnderLeapPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) MealEffects.enderLeap(player);
            })
        );
        registrar.playToClient(
            DiscoverySyncPacket.TYPE,
            DiscoverySyncPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> ClientDiscoveries.apply(packet.items(), packet.replace()))
        );
        registrar.playToClient(
            com.alexh.capitalism.network.BloodMoonPacket.TYPE,
            com.alexh.capitalism.network.BloodMoonPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.BloodMoonClient.set(packet.active()))
        );
        registrar.playToClient(
            com.alexh.capitalism.network.AltarVisionPacket.TYPE,
            com.alexh.capitalism.network.AltarVisionPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.BloodMoonClient.vision(packet.ticks()))
        );
        registrar.playToClient(
            com.alexh.capitalism.network.DayNoticePacket.TYPE,
            com.alexh.capitalism.network.DayNoticePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.DayNoticeHud.show(packet))
        );
        registrar.playToClient(
            com.alexh.capitalism.network.OpenJobPickPacket.TYPE,
            com.alexh.capitalism.network.OpenJobPickPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(com.alexh.capitalism.client.JobPickScreen::open)
        );
        registrar.playToClient(
            com.alexh.capitalism.network.DeathTaxPopupPacket.TYPE,
            com.alexh.capitalism.network.DeathTaxPopupPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(com.alexh.capitalism.client.PurgatoryHud::showBig)
        );
        registrar.playToClient(
            com.alexh.capitalism.network.OpenInsuranceGuidePacket.TYPE,
            com.alexh.capitalism.network.OpenInsuranceGuidePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.InsuranceGuideScreen.open(packet.values()))
        );
        registrar.playToClient(
            com.alexh.capitalism.network.RulesPacket.TYPE,
            com.alexh.capitalism.network.RulesPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                com.alexh.capitalism.client.ClientRules.rules = java.util.List.copyOf(packet.rules());
                com.alexh.capitalism.client.ClientRules.bloodMoonInsurancePrice = packet.bloodMoonInsurancePrice();
            })
        );
        registrar.playToClient(
            TreasureSightPacket.TYPE,
            TreasureSightPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.TreasureSightClient.apply(packet.chests()))
        );
        registrar.playToClient(
            OpenApprenticeNamePacket.TYPE,
            OpenApprenticeNamePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> com.alexh.capitalism.client.ApprenticeNameScreen.open(packet.entityId(), packet.name()))
        );
        registrar.playToServer(
            RenameApprenticePacket.TYPE,
            RenameApprenticePacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(packet.entityId()) instanceof Apprentice apprentice) {
                    apprentice.rename(player, packet.name());
                }
            })
        );
        registrar.playToClient(
            ThirstSyncPacket.TYPE,
            ThirstSyncPacket.STREAM_CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                ClientThirstData.thirst = packet.thirst();
                ClientThirstData.goldThirst = packet.goldThirst();
                ClientThirstData.goldFood = packet.goldFood();
                ClientThirstData.purity = packet.purity();
                ClientThirstData.thirstCap = packet.thirstCap();
                ClientThirstData.foodCap = packet.foodCap();
                context.player().getFoodData().setFoodLevel(packet.food());
            })
        );
    }
}

package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.enchanter.Apprentice;
import com.alexh.capitalism.npc.JobNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Capitalism.MODID)
public class ModEntities {

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Capitalism.MODID);

    // A job's stand-in shopkeeper (npc/NpcManager): villager-sized
    public static final DeferredHolder<EntityType<?>, EntityType<JobNpc>> JOB_NPC = ENTITIES.registerEntityType("job_npc",
        JobNpc::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));

    // An Enchanter's Apprentice (enchanter/Apprentice): a little figure that stores XP (about a block tall, plus its hat)
    public static final DeferredHolder<EntityType<?>, EntityType<Apprentice>> APPRENTICE = ENTITIES.registerEntityType("apprentice",
        Apprentice::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.5F).eyeHeight(0.75F).clientTrackingRange(10));

    // The Whisperer (town/Whisperer): a hunched old figure who wanders the town and sells Knowledge Pieces
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.town.Whisperer>> WHISPERER = ENTITIES.registerEntityType("whisperer",
        com.alexh.capitalism.town.Whisperer::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.6F).eyeHeight(1.3F).clientTrackingRange(10));
    // The Traveling Merchant (town/TravelingMerchant): an old traveller and his wagon, in town one day every 8-14
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.town.TravelingMerchant>> TRAVELING_MERCHANT = ENTITIES.registerEntityType("traveling_merchant",
        com.alexh.capitalism.town.TravelingMerchant::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.9F).eyeHeight(1.6F).clientTrackingRange(10));
    // The Blood Merchant (town/BloodMerchant): a hooded vampire an admin puts in his shop; out only at night
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.town.BloodMerchant>> BLOOD_MERCHANT = ENTITIES.registerEntityType("blood_merchant",
        com.alexh.capitalism.town.BloodMerchant::new, MobCategory.MISC, builder -> builder.sized(0.6F, 2.0F).eyeHeight(1.75F).clientTrackingRange(10));
    // The Fisherman (town/Fisherman): an old salt an admin puts on the docks; buys rare catches, mounts them on plaques
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.town.Fisherman>> FISHERMAN = ENTITIES.registerEntityType("fisherman",
        com.alexh.capitalism.town.Fisherman::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
    // The Bounty Clerk (town/BountyClerk): a town official an admin puts by a notice board; hands out bounty tasks
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.town.BountyClerk>> BOUNTY_CLERK = ENTITIES.registerEntityType("bounty_clerk",
        com.alexh.capitalism.town.BountyClerk::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
    // An invisible seat on furniture (decor/DecorSeat: the Crimson Throne)
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.decor.DecorSeat>> DECOR_SEAT = ENTITIES.registerEntityType("decor_seat",
        com.alexh.capitalism.decor.DecorSeat::new, MobCategory.MISC, builder -> builder.sized(0.2F, 1.4F).noSummon().clientTrackingRange(10));

    // The Boat Master (npc/BoatMaster): the docks' shopkeeper, villager-sized like the job NPCs
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.npc.BoatMaster>> BOAT_MASTER = ENTITIES.registerEntityType("boat_master",
        com.alexh.capitalism.npc.BoatMaster::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
    // The Butcher (npc/Butcher): sells raw fine cuts, villager-sized like the job NPCs
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.npc.Butcher>> BUTCHER = ENTITIES.registerEntityType("butcher",
        com.alexh.capitalism.npc.Butcher::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));

    // The Premium boats (boats/PremiumBoat, Boats.md): each a boat's size on the water (they only look huge), seen
    // from further away
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.PirateShip>> PIRATE_SHIP =
        premiumBoat("pirate_ship", com.alexh.capitalism.boats.PirateShip::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Speedboat>> SPEEDBOAT =
        premiumBoat("speedboat", com.alexh.capitalism.boats.Speedboat::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Yacht>> YACHT =
        premiumBoat("yacht", com.alexh.capitalism.boats.Yacht::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Longship>> LONGSHIP =
        premiumBoat("longship", com.alexh.capitalism.boats.Longship::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Paddlewheeler>> PADDLEWHEELER =
        premiumBoat("paddlewheeler", com.alexh.capitalism.boats.Paddlewheeler::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.GhostShip>> GHOST_SHIP =
        lavaBoat("ghost_ship", com.alexh.capitalism.boats.GhostShip::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.SwampRaft>> SWAMP_RAFT =
        premiumBoat("swamp_raft", com.alexh.capitalism.boats.SwampRaft::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Steamboat>> STEAMBOAT =
        premiumBoat("steamboat", com.alexh.capitalism.boats.Steamboat::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.LeviathanWake>> LEVIATHAN =
        lavaBoat("leviathan", com.alexh.capitalism.boats.LeviathanWake::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Teapot>> TEAPOT =
        premiumBoat("teapot", com.alexh.capitalism.boats.Teapot::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.Submersible>> SUBMERSIBLE =
        premiumBoat("submersible", com.alexh.capitalism.boats.Submersible::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.FairyBoat>> FAIRY_BOAT =
        premiumBoat("fairy_boat", com.alexh.capitalism.boats.FairyBoat::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.boats.CastawayTub>> CASTAWAY_TUB =
        premiumBoat("castaway_tub", com.alexh.capitalism.boats.CastawayTub::new);

    // The Crimson Revenant (event/CrimsonRevenant, Events.md): the Blood Moon's boss (its SCALE attribute makes it 1.75x this)
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.event.CrimsonRevenant>> CRIMSON_REVENANT = ENTITIES.registerEntityType("crimson_revenant",
        com.alexh.capitalism.event.CrimsonRevenant::new, MobCategory.MONSTER, builder -> builder.sized(1.25F, 2.4F).eyeHeight(2.0F).fireImmune().clientTrackingRange(16));
    // Gorefang, the Blood Wolf and the Sanguine Witch (event/Gorefang, event/SanguineWitch): the other two Blood Moon bosses
    // (SCALE 1.9x and 1.5x these)
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.event.Gorefang>> GOREFANG = ENTITIES.registerEntityType("gorefang",
        com.alexh.capitalism.event.Gorefang::new, MobCategory.MONSTER, builder -> builder.sized(1.7F, 1.75F).eyeHeight(1.4F).fireImmune().clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<com.alexh.capitalism.event.SanguineWitch>> SANGUINE_WITCH = ENTITIES.registerEntityType("sanguine_witch",
        com.alexh.capitalism.event.SanguineWitch::new, MobCategory.MONSTER, builder -> builder.sized(0.9F, 2.2F).eyeHeight(1.95F).fireImmune().clientTrackingRange(16));

    private static <T extends com.alexh.capitalism.boats.PremiumBoat> DeferredHolder<EntityType<?>, EntityType<T>> premiumBoat(
            String name, EntityType.EntityFactory<T> factory) {
        return ENTITIES.registerEntityType(name, factory, MobCategory.MISC,
            builder -> builder.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(12));
    }

    // a Premium boat that sails on lava too (boats/LavaBoat): it doesn't burn
    private static <T extends com.alexh.capitalism.boats.PremiumBoat> DeferredHolder<EntityType<?>, EntityType<T>> lavaBoat(
            String name, EntityType.EntityFactory<T> factory) {
        return ENTITIES.registerEntityType(name, factory, MobCategory.MISC,
            builder -> builder.noLootTable().fireImmune().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(12));
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BOAT_MASTER.get(), JobNpc.createAttributes().build());
        event.put(BUTCHER.get(), JobNpc.createAttributes().build());
        event.put(JOB_NPC.get(), JobNpc.createAttributes().build());
        event.put(APPRENTICE.get(), ArmorStand.createAttributes().build());
        event.put(WHISPERER.get(), com.alexh.capitalism.town.Whisperer.createAttributes().build());
        event.put(BLOOD_MERCHANT.get(), com.alexh.capitalism.town.BloodMerchant.createAttributes().build());
        event.put(TRAVELING_MERCHANT.get(), com.alexh.capitalism.town.TravelingMerchant.createAttributes().build());
        event.put(FISHERMAN.get(), com.alexh.capitalism.town.Fisherman.createAttributes().build());
        event.put(BOUNTY_CLERK.get(), com.alexh.capitalism.town.BountyClerk.createAttributes().build());
        event.put(CRIMSON_REVENANT.get(), com.alexh.capitalism.event.CrimsonRevenant.createAttributes().build());
        event.put(GOREFANG.get(), com.alexh.capitalism.event.Gorefang.createAttributes().build());
        event.put(SANGUINE_WITCH.get(), com.alexh.capitalism.event.SanguineWitch.createAttributes().build());
    }
}

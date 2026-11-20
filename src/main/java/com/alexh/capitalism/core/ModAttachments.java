package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.ContainerOwner;
import com.alexh.capitalism.guide.DiscoveryData;
import com.alexh.capitalism.librarian.TraitData;
import com.alexh.capitalism.occupation.JobData;
import com.alexh.capitalism.player.DietData;
import com.alexh.capitalism.player.ThirstData;
import com.alexh.capitalism.player.WellbeingData;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Capitalism.MODID);

    // Owner + lock of a container block (chests, barrels, furnaces, shulker boxes, our machines...)
    public static final Supplier<AttachmentType<ContainerOwner>> CONTAINER_OWNER =
        ATTACHMENT_TYPES.register("container_owner", () ->
            AttachmentType.builder(() -> ContainerOwner.NONE)
                .serialize(ContainerOwner.CODEC)
                .build());

    // Recent diet (four food groups). Kept through death.
    public static final Supplier<AttachmentType<DietData>> DIET =
        ATTACHMENT_TYPES.register("diet", () ->
            AttachmentType.builder(DietData::new)
                .serialize(DietData.CODEC)
                .copyOnDeath()
                .build());

    // The jobs a player holds (up to 3). Kept through death.
    public static final Supplier<AttachmentType<JobData>> JOBS =
        ATTACHMENT_TYPES.register("jobs", () ->
            AttachmentType.builder(JobData::new)
                .serialize(JobData.CODEC)
                .copyOnDeath()
                .build());

    // Wellbeing timer (Balanced diet + Good water -> bonus heart) and body heat (player/BodyTemperature). Kept through death (body heat resets on respawn).
    public static final Supplier<AttachmentType<WellbeingData>> WELLBEING =
        ATTACHMENT_TYPES.register("wellbeing", () ->
            AttachmentType.builder(WellbeingData::new)
                .serialize(WellbeingData.CODEC)
                .copyOnDeath()
                .build());

    // On a chest (block entity): Treasure Sight found it in a shipwreck / buried treasure, so it keeps glowing for
    // Treasure Sight even after it's opened. Gone when the chest is broken.
    public static final Supplier<AttachmentType<Boolean>> TREASURE_CHEST =
        ATTACHMENT_TYPES.register("treasure_chest", () ->
            AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL.fieldOf("treasure"))
                .build());

    // Items made at least once (unlocks their guide entries). Kept through death.
    public static final Supplier<AttachmentType<DiscoveryData>> DISCOVERIES =
        ATTACHMENT_TYPES.register("discoveries", () ->
            AttachmentType.builder(DiscoveryData::new)
                .serialize(DiscoveryData.CODEC)
                .copyOnDeath()
                .build());

    // The rare catches a player has fished up themselves (fishing/CatchCollection, the Fisherman's Collection tab), and
    // whether they've claimed its prize. Kept through death; synced to that player.
    public static final Supplier<AttachmentType<com.alexh.capitalism.fishing.CatchCollection>> CATCH_COLLECTION =
        ATTACHMENT_TYPES.register("catch_collection", () ->
            AttachmentType.builder(com.alexh.capitalism.fishing.CatchCollection::new)
                .serialize(com.alexh.capitalism.fishing.CatchCollection.CODEC)
                .sync((holder, player) -> holder == player, com.alexh.capitalism.fishing.CatchCollection.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // A player's bounties from the Bounty Clerk (bounty/BountyData): open tasks and their progress, the day they last took
    // one, how many they've turned in, the 50-bounty prize. Kept through death; synced to that player.
    public static final Supplier<AttachmentType<com.alexh.capitalism.bounty.BountyData>> BOUNTIES =
        ATTACHMENT_TYPES.register("bounties", () ->
            AttachmentType.builder(com.alexh.capitalism.bounty.BountyData::new)
                .serialize(com.alexh.capitalism.bounty.BountyData.CODEC)
                .sync((holder, player) -> holder == player, com.alexh.capitalism.bounty.BountyData.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // A player's cravings (craving/CravingData): today's food and drink, whether satisfied, how many in all. Kept
    // through death; synced to that player.
    public static final Supplier<AttachmentType<com.alexh.capitalism.craving.CravingData>> CRAVINGS =
        ATTACHMENT_TYPES.register("cravings", () ->
            AttachmentType.builder(com.alexh.capitalism.craving.CravingData::new)
                .serialize(com.alexh.capitalism.craving.CravingData.CODEC)
                .sync((holder, player) -> holder == player, com.alexh.capitalism.craving.CravingData.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // A player's drinks today (player/Intoxication, Wellness.md > Intoxication). Saved, kept through death, synced to
    // that player (the Wellness screen).
    public static final Supplier<AttachmentType<com.alexh.capitalism.player.Intoxication.Data>> INTOXICATION =
        ATTACHMENT_TYPES.register("intoxication", () ->
            AttachmentType.builder(() -> new com.alexh.capitalism.player.Intoxication.Data())
                .serialize(com.alexh.capitalism.player.Intoxication.Data.CODEC)
                .sync((holder, player) -> holder == player, com.alexh.capitalism.player.Intoxication.Data.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // A player's death tax, debts and Not-A-Scam Insurance (deathtax/DeathTaxData, Insurance.md). Saved, kept through
    // death (dying again adds to the debt), synced to that player (the Purgatory HUD, the respawn popup).
    public static final Supplier<AttachmentType<com.alexh.capitalism.deathtax.DeathTaxData>> DEATH_TAX =
        ATTACHMENT_TYPES.register("death_tax", () ->
            AttachmentType.builder(com.alexh.capitalism.deathtax.DeathTaxData::new)
                .serialize(com.alexh.capitalism.deathtax.DeathTaxData.CODEC)
                .sync((holder, player) -> holder == player, com.alexh.capitalism.deathtax.DeathTaxData.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // Gold (or raw gold) lying on the ground that came from this player: their death drops, what they threw
    // (deathtax/GoldCollector.Drop: whose, and how many of the stack are theirs). /pay death can take it. Saved with the item;
    // gone when someone else picks it up.
    public static final Supplier<AttachmentType<com.alexh.capitalism.deathtax.GoldCollector.Drop>> GOLD_DROPPED_BY =
        ATTACHMENT_TYPES.register("gold_dropped_by", () ->
            AttachmentType.builder(() -> com.alexh.capitalism.deathtax.GoldCollector.Drop.NONE)
                .serialize(com.alexh.capitalism.deathtax.GoldCollector.Drop.CODEC.fieldOf("drop"), drop -> drop.count() > 0)
                .build());

    // In Purgatory (owes death tax: deathtax/DeathTax.refresh keeps it in step with DEATH_TAX). Synced to everyone who
    // can see them, so their name shows a skull.
    public static final Supplier<AttachmentType<Boolean>> IN_PURGATORY =
        ATTACHMENT_TYPES.register("in_purgatory", () ->
            AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL.fieldOf("in_purgatory"))
                .sync(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                .copyOnDeath()
                .build());

    // FishWatch's quiet countermeasure (fishing/FishWatch, /fishlog restrict): no rare catches for this player. Only
    // ever set or lifted by an op; saved, kept through death.
    public static final Supplier<AttachmentType<Boolean>> FISH_RESTRICTED =
        ATTACHMENT_TYPES.register("fish_restricted", () ->
            AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL.fieldOf("restricted"))
                .copyOnDeath()
                .build());

    // A pet's hunger and thirst, and its owner (pets/Pets). Saved only once it's a pet; synced to everyone who
    // can see the animal (client/PetHud).
    public static final Supplier<AttachmentType<com.alexh.capitalism.pets.PetData>> PET =
        ATTACHMENT_TYPES.register("pet", () ->
            AttachmentType.builder(com.alexh.capitalism.pets.PetData::new)
                .serialize(com.alexh.capitalism.pets.PetData.CODEC, data -> data.pet() || data.claimed())
                .sync(com.alexh.capitalism.pets.PetData.STREAM_CODEC)
                .build());

    // A player's pet buff right now, the pet's part and the rest of those effects (pets/PetBuffs: one pet buff at a time).
    // Saved while one is on; not kept through death (effects aren't either).
    public static final Supplier<AttachmentType<com.alexh.capitalism.pets.PetBuffs.State>> PET_BUFF =
        ATTACHMENT_TYPES.register("pet_buff", () ->
            AttachmentType.builder(com.alexh.capitalism.pets.PetBuffs.State::new)
                .serialize(com.alexh.capitalism.pets.PetBuffs.State.CODEC, com.alexh.capitalism.pets.PetBuffs.State::active)
                .build());

    // What's in a player's Back slot (back/Back): an Animal Bjorn, later backpacks. Synced to everyone who can see them
    // (shown on them). Kept through death only with keepInventory (otherwise Back drops it when they die).
    public static final Supplier<AttachmentType<com.alexh.capitalism.back.Back.Worn>> BACK =
        ATTACHMENT_TYPES.register("back", () ->
            AttachmentType.builder(() -> new com.alexh.capitalism.back.Back.Worn())
                .serialize(com.alexh.capitalism.back.Back.Worn.CODEC.fieldOf("stack"), worn -> !worn.stack().isEmpty())
                .sync(com.alexh.capitalism.back.Back.Worn.STREAM_CODEC)
                .copyOnDeath()
                .build());

    // Traits learned from skill books (librarian/Traits). Kept through death.
    public static final Supplier<AttachmentType<TraitData>> TRAITS =
        ATTACHMENT_TYPES.register("traits", () ->
            AttachmentType.builder(TraitData::new)
                .serialize(TraitData.CODEC)
                .sync((holder, player) -> holder == player, TraitData.STREAM_CODEC)
                // not copied on death: dying forgets every trait (librarian/Traits)
                .build());

    public static final Supplier<AttachmentType<ThirstData>> THIRST =
        ATTACHMENT_TYPES.register("thirst", () ->
            AttachmentType.builder(ThirstData::new)
                .serialize(ThirstData.CODEC)
                .build());

    // On a chunk: its fertilized farmland and how many Farmer harvests each spot has left (farmer/FertilizedSoil)
    public static final Supplier<AttachmentType<com.alexh.capitalism.farmer.FertilizedSoil>> FERTILIZED_SOIL =
        ATTACHMENT_TYPES.register("fertilized_soil", () ->
            AttachmentType.builder(com.alexh.capitalism.farmer.FertilizedSoil::new)
                .serialize(com.alexh.capitalism.farmer.FertilizedSoil.CODEC)
                .build());
}

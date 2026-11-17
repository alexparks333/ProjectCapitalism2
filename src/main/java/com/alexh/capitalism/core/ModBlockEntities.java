package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.AssemblyTableBlockEntity;
import com.alexh.capitalism.block.BartendingTableBlockEntity;
import com.alexh.capitalism.block.OvenBlockEntity;
import com.alexh.capitalism.block.PlateBlockEntity;
import com.alexh.capitalism.block.FermenterBlockEntity;
import com.alexh.capitalism.economy.ShopfrontBlockEntity;
import com.alexh.capitalism.block.JuicerBlockEntity;
import com.alexh.capitalism.block.PurificationTableBlockEntity;
import com.alexh.capitalism.enchanter.LapisCompressorBlockEntity;
import com.alexh.capitalism.weaponsmith.LavaCreatorBlockEntity;
import com.alexh.capitalism.weaponsmith.SmithTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Capitalism.MODID);

    public static final Supplier<BlockEntityType<PurificationTableBlockEntity>> PURIFICATION_TABLE =
        BLOCK_ENTITIES.register("purification_table", () ->
            new BlockEntityType<>(PurificationTableBlockEntity::new, ModBlocks.PURIFICATION_TABLE.get()));

    public static final Supplier<BlockEntityType<JuicerBlockEntity>> JUICER =
        BLOCK_ENTITIES.register("juicer", () ->
            new BlockEntityType<>(JuicerBlockEntity::new, ModBlocks.JUICER.get()));

    public static final Supplier<BlockEntityType<BartendingTableBlockEntity>> BARTENDING_TABLE =
        BLOCK_ENTITIES.register("bartending_table", () ->
            new BlockEntityType<>(BartendingTableBlockEntity::new, ModBlocks.BARTENDING_TABLE.get()));

    // Sushi Table and Cooking Table share one block entity (the block says which it is)
    public static final Supplier<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE =
        BLOCK_ENTITIES.register("assembly_table", () ->
            new BlockEntityType<>(AssemblyTableBlockEntity::new, ModBlocks.SUSHI_TABLE.get(), ModBlocks.COOKING_TABLE.get()));

    public static final Supplier<BlockEntityType<PlateBlockEntity>> PLATE =
        BLOCK_ENTITIES.register("plate", () ->
            new BlockEntityType<>(PlateBlockEntity::new, ModBlocks.SILVER_PLATE.get(), ModBlocks.GOLDEN_PLATE.get()));

    public static final Supplier<BlockEntityType<OvenBlockEntity>> OVEN =
        BLOCK_ENTITIES.register("oven", () ->
            new BlockEntityType<>(OvenBlockEntity::new, ModBlocks.OVEN.get()));

    // The Lava Creator's water tank and brewing
    // The Lapis Compressor: Lapis in, XP out
    public static final Supplier<BlockEntityType<LapisCompressorBlockEntity>> LAPIS_COMPRESSOR =
        BLOCK_ENTITIES.register("lapis_compressor", () ->
            new BlockEntityType<>(LapisCompressorBlockEntity::new, ModBlocks.LAPIS_COMPRESSOR.get()));

    // The Stitching Table: sews blank books
    public static final Supplier<BlockEntityType<com.alexh.capitalism.librarian.StitchingTableBlockEntity>> STITCHING_TABLE =
        BLOCK_ENTITIES.register("stitching_table", () ->
            new BlockEntityType<>(com.alexh.capitalism.librarian.StitchingTableBlockEntity::new, ModBlocks.STITCHING_TABLE.get()));

    // The Food Trough: food for pets
    public static final Supplier<BlockEntityType<com.alexh.capitalism.pets.FoodTroughBlockEntity>> FOOD_TROUGH =
        BLOCK_ENTITIES.register("food_trough", () ->
            new BlockEntityType<>(com.alexh.capitalism.pets.FoodTroughBlockEntity::new,
                ModBlocks.SMALL_FOOD_TROUGH.get(), ModBlocks.MEDIUM_FOOD_TROUGH.get(), ModBlocks.LARGE_FOOD_TROUGH.get()));

    // The Water Bowl: the water (mL) pets drink
    public static final Supplier<BlockEntityType<com.alexh.capitalism.pets.WaterBowlBlockEntity>> WATER_BOWL =
        BLOCK_ENTITIES.register("water_bowl", () ->
            new BlockEntityType<>(com.alexh.capitalism.pets.WaterBowlBlockEntity::new,
                ModBlocks.SMALL_WATER_BOWL.get(), ModBlocks.MEDIUM_WATER_BOWL.get(), ModBlocks.LARGE_WATER_BOWL.get()));

    // The Knowledge Creator: Enchanted Books into Knowledge Pieces
    public static final Supplier<BlockEntityType<com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity>> KNOWLEDGE_CREATOR =
        BLOCK_ENTITIES.register("knowledge_creator", () ->
            new BlockEntityType<>(com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity::new, ModBlocks.KNOWLEDGE_CREATOR.get()));

    public static final Supplier<BlockEntityType<com.alexh.capitalism.block.OsmosisMachineBlock.Machine>> OSMOSIS_MACHINE =
        BLOCK_ENTITIES.register("advanced_osmosis_machine", () ->
            new BlockEntityType<>(com.alexh.capitalism.block.OsmosisMachineBlock.Machine::new, ModBlocks.OSMOSIS_MACHINE.get()));

    public static final Supplier<BlockEntityType<LavaCreatorBlockEntity>> LAVA_CREATOR =
        BLOCK_ENTITIES.register("lava_creator", () ->
            new BlockEntityType<>(LavaCreatorBlockEntity::new, ModBlocks.LAVA_CREATOR.get()));

    // The smith tables' lava tank (Weaponsmith, Toolsmith and Armorsmith Tables)
    public static final Supplier<BlockEntityType<SmithTableBlockEntity>> WEAPONSMITH_TABLE =
        BLOCK_ENTITIES.register("weaponsmith_table", () ->
            new BlockEntityType<>(SmithTableBlockEntity::new, ModBlocks.WEAPONSMITH_TABLE.get(), ModBlocks.TOOLSMITH_TABLE.get(),
                ModBlocks.ARMORSMITH_TABLE.get()));

    public static final Supplier<BlockEntityType<FermenterBlockEntity>> FERMENTER =
        BLOCK_ENTITIES.register("fermenter", () ->
            new BlockEntityType<>(FermenterBlockEntity::new, ModBlocks.FERMENTER.get()));

    public static final Supplier<BlockEntityType<ShopfrontBlockEntity>> SHOPFRONT =
        BLOCK_ENTITIES.register("shopfront", () ->
            new BlockEntityType<>(ShopfrontBlockEntity::new, ModBlocks.SHOPFRONT.get()));

    // The Designer: the Designer Table (bags) and the Sewing Machine (Patches)
    public static final Supplier<BlockEntityType<com.alexh.capitalism.designer.DesignerTableBlockEntity>> DESIGNER_TABLE =
        BLOCK_ENTITIES.register("designer_table", () ->
            new BlockEntityType<>(com.alexh.capitalism.designer.DesignerTableBlockEntity::new, ModBlocks.DESIGNER_TABLE.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.designer.SewingMachineBlockEntity>> SEWING_MACHINE =
        BLOCK_ENTITIES.register("sewing_machine", () ->
            new BlockEntityType<>(com.alexh.capitalism.designer.SewingMachineBlockEntity::new, ModBlocks.SEWING_MACHINE.get()));

    // The Farmer: the Decomposter
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.DecomposterBlockEntity>> DECOMPOSTER =
        BLOCK_ENTITIES.register("decomposter", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.DecomposterBlockEntity::new, ModBlocks.DECOMPOSTER.get()));
    // the Fridge and Freezer, and placed Meat Slabs (their rot timers)
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.ColdStorageBlockEntity>> FRIDGE =
        BLOCK_ENTITIES.register("fridge", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.ColdStorageBlockEntity::new, ModBlocks.FRIDGE.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.ColdStorageBlockEntity>> FREEZER =
        BLOCK_ENTITIES.register("freezer", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.ColdStorageBlockEntity::new, ModBlocks.FREEZER.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.realestate.CityChestBlockEntity>> CITY_CHEST =
        BLOCK_ENTITIES.register("city_chest", () ->
            new BlockEntityType<>(com.alexh.capitalism.realestate.CityChestBlockEntity::new, ModBlocks.CITY_CHEST.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.GrillBlockEntity>> GRILL =
        BLOCK_ENTITIES.register("grill", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.GrillBlockEntity::new, ModBlocks.GRILL.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.ButchersTableBlockEntity>> BUTCHERS_TABLE =
        BLOCK_ENTITIES.register("butchers_table", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.ButchersTableBlockEntity::new, ModBlocks.BUTCHERS_TABLE.get()));
    public static final Supplier<BlockEntityType<com.alexh.capitalism.farmer.MeatSlabBlockEntity>> MEAT_SLAB =
        BLOCK_ENTITIES.register("meat_slab", () ->
            new BlockEntityType<>(com.alexh.capitalism.farmer.MeatSlabBlockEntity::new,
                ModBlocks.MEAT_SLABS.values().stream().map(b -> (net.minecraft.world.level.block.Block) b.get()).collect(java.util.stream.Collectors.toSet())));

    // The Wishing Well's core (it draws the whole well: client/WishingWellRenderer)
    public static final Supplier<BlockEntityType<com.alexh.capitalism.occupation.WishingWellBlock.WishingWellBlockEntity>> WISHING_WELL =
        BLOCK_ENTITIES.register("wishing_well", () ->
            new BlockEntityType<>(com.alexh.capitalism.occupation.WishingWellBlock.WishingWellBlockEntity::new, ModBlocks.WISHING_WELL.get()));

    // The Mailbox (mail/MailboxBlockEntity): its mail and its owner
    public static final Supplier<BlockEntityType<com.alexh.capitalism.mail.MailboxBlockEntity>> MAILBOX =
        BLOCK_ENTITIES.register("mailbox", () ->
            new BlockEntityType<>(com.alexh.capitalism.mail.MailboxBlockEntity::new, ModBlocks.MAILBOX.get(), ModBlocks.TOWN_MAILBOX.get()));

    // The Mayoral Chest (town/MayoralChestBlockEntity): 54 slots, the Mayor's and Vice Mayor's
    public static final Supplier<BlockEntityType<com.alexh.capitalism.town.MayoralChestBlockEntity>> MAYORAL_CHEST =
        BLOCK_ENTITIES.register("mayoral_chest", () ->
            new BlockEntityType<>(com.alexh.capitalism.town.MayoralChestBlockEntity::new, ModBlocks.MAYORAL_CHEST.get()));

    // The seasonal decorations: putting themselves up and away, a light hook's strands (decor/SeasonalDecorBlockEntity)
    public static final Supplier<BlockEntityType<com.alexh.capitalism.decor.SeasonalDecorBlockEntity>> SEASONAL_DECOR =
        BLOCK_ENTITIES.register("seasonal_decor", () ->
            new BlockEntityType<>(com.alexh.capitalism.decor.SeasonalDecorBlockEntity::new, ModBlocks.CHRISTMAS_TREE.get(), ModBlocks.STRING_LIGHT_HOOK.get(),
                ModBlocks.SPOOKY_SCARECROW.get(), ModBlocks.WITCHS_CAULDRON.get(), ModBlocks.PUMPKIN_PILE.get(), ModBlocks.LEAF_PILE.get(),
                ModBlocks.PRESENT_PILE.get(), ModBlocks.CANDY_CANE_POLE.get(), ModBlocks.CANDY_CANES.get(),
                ModBlocks.CHRISTMAS_FROST.get(), ModBlocks.ICICLES.get(), ModBlocks.GRAVESTONE_ROUND.get(), ModBlocks.GRAVESTONE_CROSS.get(),
                ModBlocks.GRAVESTONE_TOMB.get(), ModBlocks.BONE_PILE.get(), ModBlocks.GROUND_FOG.get(), ModBlocks.AUTUMN_MAPLE.get(), ModBlocks.AUTUMN_BIRCH.get(), ModBlocks.FALLEN_LEAVES.get(),
                ModBlocks.BEACH_TOWEL.get(), ModBlocks.BEACH_BALL.get(), ModBlocks.BEACH_UMBRELLA.get(), ModBlocks.BEACH_CHAIR.get(),
                ModBlocks.EGG_BASKET.get(), ModBlocks.GIANT_EGG.get(), ModBlocks.SPRING_BUNNY.get(), ModBlocks.TULIP_BED.get()));

    // The Blood Moon decorations (decor/BloodDecorBlockEntity): just so client/BloodDecorRenderer can draw them
    // A fish plaque: the catch on it (fishing/FishPlaqueBlockEntity), drawn by client/FishPlaqueRenderer
    public static final Supplier<BlockEntityType<com.alexh.capitalism.fishing.FishPlaqueBlockEntity>> FISH_PLAQUE =
        BLOCK_ENTITIES.register("fish_plaque", () ->
            new BlockEntityType<>(com.alexh.capitalism.fishing.FishPlaqueBlockEntity::new, ModBlocks.FISH_PLAQUE.get()));

    public static final Supplier<BlockEntityType<com.alexh.capitalism.decor.BloodDecorBlockEntity>> BLOOD_DECOR =
        BLOCK_ENTITIES.register("blood_decor", () ->
            new BlockEntityType<>(com.alexh.capitalism.decor.BloodDecorBlockEntity::new, ModBlocks.BLOOD_MOON_LANTERN.get(),
                ModBlocks.CRIMSON_CANDELABRA.get(), ModBlocks.CRIMSON_THRONE.get(), ModBlocks.VAMPIRE_COFFIN.get(), ModBlocks.BLOOD_FOUNTAIN.get(),
                ModBlocks.BLOOD_MOON_BANNER.get(), ModBlocks.CRIMSON_RUG.get(), ModBlocks.REVENANT_GREATSWORD_STATUE.get(),
                ModBlocks.GOREFANG_SKULL_TROPHY.get(), ModBlocks.SANGUINE_STAFF_PEDESTAL.get(), ModBlocks.BAT_GARGOYLE.get(),
                ModBlocks.GRANDFATHER_CLOCK.get(), ModBlocks.CRIMSON_CHANDELIER.get(), ModBlocks.BLOOD_PIPE_ORGAN.get(), ModBlocks.SECRET_BOOKSHELF_DOOR.get(),
                ModBlocks.HAUNTED_PORTRAIT.get(), ModBlocks.IRON_MAIDEN.get(), ModBlocks.BLOOD_MOON_ALTAR.get(), ModBlocks.BAT_ROOST.get(),
                ModBlocks.CRIMSON_CANOPY_BED.get(), ModBlocks.BLOOD_CRYSTAL_FIREPLACE.get()));
}

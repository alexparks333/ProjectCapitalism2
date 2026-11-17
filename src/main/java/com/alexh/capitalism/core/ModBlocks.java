package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.AssemblyTableBlock;
import com.alexh.capitalism.block.BartendingTableBlock;
import com.alexh.capitalism.block.OvenBlock;
import com.alexh.capitalism.block.PlateBlock;
import com.alexh.capitalism.occupation.GourmetFood;
import com.alexh.capitalism.block.RiceBlock;
import com.alexh.capitalism.block.SinkBlock;
import com.alexh.capitalism.block.FermenterBlock;
import com.alexh.capitalism.economy.ShopfrontBlock;
import com.alexh.capitalism.block.JuicerBlock;
import com.alexh.capitalism.block.PurificationTableBlock;
import com.alexh.capitalism.weaponsmith.SmithTableBlock;
import com.alexh.capitalism.weaponsmith.LavaCreatorBlock;
import com.alexh.capitalism.weaponsmith.SmithTable;
import com.alexh.capitalism.weaponsmith.WhetstoneBlock;
import com.alexh.capitalism.enchanter.TransferTubeBlock;
import com.alexh.capitalism.enchanter.EnchantersTableBlock;
import com.alexh.capitalism.enchanter.LapisCompressorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Capitalism.MODID);

    public static final DeferredBlock<PurificationTableBlock> PURIFICATION_TABLE = BLOCKS.registerBlock("purification_table",
        PurificationTableBlock::new, props -> props
            .mapColor(MapColor.STONE)
            .requiresCorrectToolForDrops()
            .strength(3.5F)
            .sound(SoundType.STONE)
            .noOcclusion()
            // Soft glow while the loading bar is lit
            .lightLevel(state -> state.getValue(PurificationTableBlock.PROGRESS) > 0 ? 4 : 0));

    public static final DeferredBlock<JuicerBlock> JUICER = BLOCKS.registerBlock("juicer",
        JuicerBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            // The juice window glows softly once there is juice in it
            .lightLevel(state -> state.getValue(JuicerBlock.FILL) > 0 ? 3 : 0));

    public static final DeferredBlock<BartendingTableBlock> BARTENDING_TABLE = BLOCKS.registerBlock("bartending_table",
        BartendingTableBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    // Rice: planted in shallow water, found wild in swamps, mangroves, rivers and jungles
    public static final DeferredBlock<RiceBlock> RICE = BLOCKS.registerBlock("rice",
        RiceBlock::new, props -> props
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .randomTicks()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.POPPED));

    // Plates: set on any block by a Cook, who serves Gourmet food on them; anyone can eat off them
    public static final DeferredBlock<PlateBlock> SILVER_PLATE = BLOCKS.registerBlock("silver_plate",
        props -> new PlateBlock(GourmetFood.Serving.SILVER_PLATE, props), props -> props
            .mapColor(MapColor.METAL)
            .strength(1.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<PlateBlock> GOLDEN_PLATE = BLOCKS.registerBlock("golden_plate",
        props -> new PlateBlock(GourmetFood.Serving.GOLDEN_PLATE, props), props -> props
            .mapColor(MapColor.GOLD)
            .strength(1.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.POPPED));

    // Sink: washes Dirty Plates
    public static final DeferredBlock<SinkBlock> SINK = BLOCKS.registerBlock("sink",
        SinkBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    // â”€â”€ The Cook's kitchen (Cooks only, see JobEvents) â”€â”€
    public static final DeferredBlock<AssemblyTableBlock> SUSHI_TABLE = BLOCKS.registerBlock("sushi_table",
        props -> new AssemblyTableBlock(AssemblyTableBlock.Kind.SUSHI, props), props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.BAMBOO_WOOD)
            .noOcclusion());

    public static final DeferredBlock<AssemblyTableBlock> COOKING_TABLE = BLOCKS.registerBlock("cooking_table",
        props -> new AssemblyTableBlock(AssemblyTableBlock.Kind.COOKING, props), props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<OvenBlock> OVEN = BLOCKS.registerBlock("oven",
        OvenBlock::new, props -> props
            .mapColor(MapColor.COLOR_RED)
            .strength(3.5F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE)
            .noOcclusion()
            .lightLevel(state -> state.getValue(OvenBlock.LIT) ? 13 : 0));

    public static final DeferredBlock<FermenterBlock> FERMENTER = BLOCKS.registerBlock("fermenter",
        FermenterBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            // Two blocks tall: pistons must not split it (that would spill the contents)
            .pushReaction(PushReaction.IMMOVEABLE)
            // The liquid in the glass jug glows softly
            .lightLevel(state -> state.getValue(FermenterBlock.FILL) > 0 ? 3 : 0));

    // â”€â”€ The Weaponsmith's forge (Weaponsmiths only, see JobEvents) â”€â”€
    public static final DeferredBlock<SmithTableBlock> WEAPONSMITH_TABLE = BLOCKS.registerBlock("weaponsmith_table",
        props -> new SmithTableBlock(SmithTable.WEAPONSMITH, props), props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    // â”€â”€ The Toolsmith's table (Toolsmiths only, see JobEvents) â”€â”€
    public static final DeferredBlock<SmithTableBlock> TOOLSMITH_TABLE = BLOCKS.registerBlock("toolsmith_table",
        props -> new SmithTableBlock(SmithTable.TOOLSMITH, props), props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    // â”€â”€ The Armorsmith's table (Armorsmiths only, see JobEvents) â”€â”€
    public static final DeferredBlock<SmithTableBlock> ARMORSMITH_TABLE = BLOCKS.registerBlock("armorsmith_table",
        props -> new SmithTableBlock(SmithTable.ARMORSMITH, props), props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());

    public static final DeferredBlock<WhetstoneBlock> WHETSTONE = BLOCKS.registerBlock("whetstone",
        props -> new WhetstoneBlock(WhetstoneBlock.Kind.SWORD, props), props -> props
            .mapColor(MapColor.STONE)
            .strength(1.5F)
            .sound(SoundType.STONE)
            .noOcclusion());
    // The Mayoral Door (town/MayoralDoorBlock): a double-door-able door only the Mayor can open
    public static final DeferredBlock<com.alexh.capitalism.town.MayoralDoorBlock> MAYORAL_DOOR = BLOCKS.registerBlock("mayoral_door",
        com.alexh.capitalism.town.MayoralDoorBlock::new, props -> props.mapColor(MapColor.GOLD).strength(5.0F, 1200.0F).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    // The Purge Switch and the Blood Moon Switch (event/EventSwitchBlock): only the Mayor can throw them; nobody can craft them
    public static final DeferredBlock<com.alexh.capitalism.event.EventSwitchBlock> PURGE_SWITCH = BLOCKS.registerBlock("purge_switch",
        props -> new com.alexh.capitalism.event.EventSwitchBlock(com.alexh.capitalism.event.EventSwitchBlock.Kind.PURGE, props),
        props -> props.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.event.EventSwitchBlock> BLOOD_MOON_SWITCH = BLOCKS.registerBlock("blood_moon_switch",
        props -> new com.alexh.capitalism.event.EventSwitchBlock(com.alexh.capitalism.event.EventSwitchBlock.Kind.BLOOD_MOON, props),
        props -> props.mapColor(MapColor.COLOR_RED).strength(3.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion());
    // The Traveling Merchant's decorations (town/TravelerBlocks): a snow globe, and a lantern that lights when he's coming
    public static final DeferredBlock<com.alexh.capitalism.town.TravelerBlocks.SnowGlobe> TRAVELERS_SNOW_GLOBE = BLOCKS.registerBlock("travelers_snow_globe",
        com.alexh.capitalism.town.TravelerBlocks.SnowGlobe::new, props -> props.mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<com.alexh.capitalism.town.TravelerBlocks.Lantern> TRAVELERS_LANTERN = BLOCKS.registerBlock("travelers_lantern",
        com.alexh.capitalism.town.TravelerBlocks.Lantern::new, props -> props.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.LANTERN).noOcclusion()
            .pushReaction(PushReaction.POPPED).lightLevel(state -> state.getValue(com.alexh.capitalism.town.TravelerBlocks.Lantern.LIT) ? 15 : 0));
    // The Advanced Osmosis Machine (block/OsmosisMachineBlock): Bartenders make Spring Water (Excellent) in it
    public static final DeferredBlock<com.alexh.capitalism.block.OsmosisMachineBlock> OSMOSIS_MACHINE = BLOCKS.registerBlock("advanced_osmosis_machine",
        com.alexh.capitalism.block.OsmosisMachineBlock::new, props -> props.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(com.alexh.capitalism.block.OsmosisMachineBlock.LIT) ? 7 : 0));
    // The Lava Creator: Blaze Powder + water -> Refined Buckets of Lava (any smith can make one)
    public static final DeferredBlock<LavaCreatorBlock> LAVA_CREATOR = BLOCKS.registerBlock("lava_creator",
        LavaCreatorBlock::new, props -> props
            .mapColor(MapColor.COLOR_BLACK)
            .strength(3.0F)
            .sound(SoundType.NETHER_BRICKS)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(LavaCreatorBlock.LIT) ? 10 : 3));
    // Toolsmiths make it: sharpens dull pickaxes
    public static final DeferredBlock<WhetstoneBlock> PICKAXE_WHETSTONE = BLOCKS.registerBlock("pickaxe_whetstone",
        props -> new WhetstoneBlock(WhetstoneBlock.Kind.PICKAXE, props), props -> props
            .mapColor(MapColor.STONE)
            .strength(1.5F)
            .sound(SoundType.STONE)
            .noOcclusion());
    // The Enchanter's Transfer Tube (3x2): moves XP from one player to another (enchanter/TransferTubeBlock)
    // The Wishing Well (occupation/WishingWellBlock, Jobs.md): 21 parts, unbreakable but for an admin in Creative
    public static final DeferredBlock<com.alexh.capitalism.occupation.WishingWellBlock> WISHING_WELL = BLOCKS.registerBlock("wishing_well",
        com.alexh.capitalism.occupation.WishingWellBlock::new, props -> props
            .mapColor(MapColor.STONE)
            .strength(-1.0F, 3600000.0F)
            .sound(net.minecraft.world.level.block.SoundType.STONE)
            .noOcclusion()
            .lightLevel(state -> state.getValue(com.alexh.capitalism.occupation.WishingWellBlock.PART) == 0 ? 7 : 0)
            .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
    public static final DeferredBlock<TransferTubeBlock> TRANSFER_TUBE = BLOCKS.registerBlock("transfer_tube",
        TransferTubeBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion());
    // The Enchanter's Table: takes unwanted enchantments off items (enchanter/EnchantersTableBlock)
    public static final DeferredBlock<EnchantersTableBlock> ENCHANTERS_TABLE = BLOCKS.registerBlock("enchanters_table",
        EnchantersTableBlock::new, props -> props
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .lightLevel(state -> 6));
    // The Lapis Compressor: presses Lapis Lazuli into XP orbs (enchanter/LapisCompressorBlock)
    public static final DeferredBlock<LapisCompressorBlock> LAPIS_COMPRESSOR = BLOCKS.registerBlock("lapis_compressor",
        LapisCompressorBlock::new, props -> props
            .mapColor(MapColor.LAPIS)
            .strength(3.5F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(LapisCompressorBlock.LIT) ? 9 : 0));
    // The Stitching Table: Librarians sew Paper and Leather into blank books (librarian/StitchingTableBlock)
    public static final DeferredBlock<com.alexh.capitalism.librarian.StitchingTableBlock> STITCHING_TABLE = BLOCKS.registerBlock("stitching_table",
        com.alexh.capitalism.librarian.StitchingTableBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());
    // The Designer (designer/): the Designer Table (bags from Leather + Patches) and the Sewing Machine (Patches, the
    // needle game)
    public static final DeferredBlock<com.alexh.capitalism.designer.DesignerTableBlock> DESIGNER_TABLE = BLOCKS.registerBlock("designer_table",
        com.alexh.capitalism.designer.DesignerTableBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.designer.SewingMachineBlock> SEWING_MACHINE = BLOCKS.registerBlock("sewing_machine",
        com.alexh.capitalism.designer.SewingMachineBlock::new, props -> props
            .mapColor(MapColor.METAL)
            .strength(3.0F)
            .sound(SoundType.METAL)
            .noOcclusion());
    // The Farmer (farmer/): the Decomposter (the Fertilizers), Fertilized Farmland (farmland showing its fertilizer;
    // no item: it drops dirt like farmland)
    public static final DeferredBlock<com.alexh.capitalism.farmer.DecomposterBlock> DECOMPOSTER = BLOCKS.registerBlock("decomposter",
        com.alexh.capitalism.farmer.DecomposterBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.0F)
            .noOcclusion()
            .sound(SoundType.WOOD));
    public static final DeferredBlock<com.alexh.capitalism.farmer.ButchersTableBlock> BUTCHERS_TABLE = BLOCKS.registerBlock("butchers_table",
        com.alexh.capitalism.farmer.ButchersTableBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD));
    // the Meat Slabs, placeable (farmer/MeatSlabBlock): soft and squishy, broken by hand in a moment
    public static final java.util.Map<com.alexh.capitalism.farmer.MeatCut, DeferredBlock<com.alexh.capitalism.farmer.MeatSlabBlock>> MEAT_SLABS =
        new java.util.EnumMap<>(com.alexh.capitalism.farmer.MeatCut.class);
    static {
        for (com.alexh.capitalism.farmer.MeatCut cut : com.alexh.capitalism.farmer.MeatCut.values()) {
            MEAT_SLABS.put(cut, BLOCKS.registerBlock(cut.id + "_slab", props -> new com.alexh.capitalism.farmer.MeatSlabBlock(cut, props), props -> props
                .mapColor(cut.animal == com.alexh.capitalism.farmer.MeatCut.Animal.CHICKEN ? MapColor.TERRACOTTA_WHITE
                    : cut.animal == com.alexh.capitalism.farmer.MeatCut.Animal.PIG ? MapColor.TERRACOTTA_PINK : MapColor.TERRACOTTA_RED)
                .strength(0.4F)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED)));
        }
    }
    // the real estate signs (realestate/PropertySignBlock)
    public static final DeferredBlock<com.alexh.capitalism.realestate.PropertySignBlock> CITY_SALE_SIGN = BLOCKS.registerBlock("city_sale_sign",
        props -> new com.alexh.capitalism.realestate.PropertySignBlock(com.alexh.capitalism.realestate.PropertySignBlock.Kind.CITY, props),
        props -> props.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.realestate.PropertySignBlock> FOR_SALE_SIGN = BLOCKS.registerBlock("for_sale_sign",
        props -> new com.alexh.capitalism.realestate.PropertySignBlock(com.alexh.capitalism.realestate.PropertySignBlock.Kind.SALE, props),
        props -> props.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.realestate.PropertySignBlock> FOR_LEASE_SIGN = BLOCKS.registerBlock("for_lease_sign",
        props -> new com.alexh.capitalism.realestate.PropertySignBlock(com.alexh.capitalism.realestate.PropertySignBlock.Kind.LEASE, props),
        props -> props.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    // the City Chest (realestate/CityChestBlock): what an agent pays in to unlock a lot; ops only, drops nothing
    public static final DeferredBlock<com.alexh.capitalism.realestate.CityChestBlock> CITY_CHEST = BLOCKS.registerBlock("city_chest",
        com.alexh.capitalism.realestate.CityChestBlock::new,
        props -> props.mapColor(MapColor.COLOR_BLUE).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noLootTable());
    // the Mailbox (mail/MailboxBlock, Mailbox.md): anyone can make one; its owner gets their mail out of it
    public static final DeferredBlock<com.alexh.capitalism.mail.MailboxBlock> MAILBOX = BLOCKS.registerBlock("mailbox",
        com.alexh.capitalism.mail.MailboxBlock::new,
        props -> props.mapColor(MapColor.WOOD).strength(2.0F, 6.0F).sound(SoundType.WOOD).noOcclusion());
    // shop signs for over a double door (decor/ShopSignBlock, Town.md > Shop signs): 2 x 1 painted boards
    public static final DeferredBlock<com.alexh.capitalism.decor.ShopSignBlock> TOWN_HALL_SIGN = shopSign("town_hall_sign");
    public static final DeferredBlock<com.alexh.capitalism.decor.ShopSignBlock> BANK_SIGN = shopSign("bank_sign");
    public static final DeferredBlock<com.alexh.capitalism.decor.ShopSignBlock> BOAT_SHOP_SIGN = shopSign("boat_shop_sign");

    private static DeferredBlock<com.alexh.capitalism.decor.ShopSignBlock> shopSign(String id) {
        return BLOCKS.registerBlock(id, com.alexh.capitalism.decor.ShopSignBlock::new,
            props -> props.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noCollision().noOcclusion());
    }
    // the Town Hall Mailbox (Mailbox.md): white and gold, for the Mayor and Vice Mayor; ops put it down, nothing breaks it
    public static final DeferredBlock<com.alexh.capitalism.mail.MailboxBlock> TOWN_MAILBOX = BLOCKS.registerBlock("town_mailbox",
        com.alexh.capitalism.mail.MailboxBlock::new,
        props -> props.mapColor(MapColor.QUARTZ).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noOcclusion().noLootTable());
    // the Mayoral Chest (town/MayoralChestBlock): the Mayor's and Vice Mayor's; only an op in creative places or breaks it
    public static final DeferredBlock<com.alexh.capitalism.town.MayoralChestBlock> MAYORAL_CHEST = BLOCKS.registerBlock("mayoral_chest",
        com.alexh.capitalism.town.MayoralChestBlock::new,
        props -> props.mapColor(MapColor.COLOR_RED).strength(-1.0F, 3600000.0F).sound(SoundType.WOOD).noOcclusion().noLootTable()
            .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
    // the Grill (farmer/GrillBlock): Cooks sear fine cuts on it
    public static final DeferredBlock<com.alexh.capitalism.farmer.GrillBlock> GRILL = BLOCKS.registerBlock("grill",
        com.alexh.capitalism.farmer.GrillBlock::new, props -> props
            .mapColor(MapColor.COLOR_BLACK).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
            .lightLevel(state -> state.getValue(com.alexh.capitalism.farmer.GrillBlock.LIT) ? 10 : 0));
    // the Fridge and Freezer (farmer/ColdStorageBlock): keep meat fresh for longer
    public static final DeferredBlock<com.alexh.capitalism.farmer.ColdStorageBlock> FRIDGE = BLOCKS.registerBlock("fridge",
        props -> new com.alexh.capitalism.farmer.ColdStorageBlock(com.alexh.capitalism.farmer.ColdStorageBlockEntity.Kind.FRIDGE, props), props -> props
            .mapColor(MapColor.SNOW).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<com.alexh.capitalism.farmer.ColdStorageBlock> FREEZER = BLOCKS.registerBlock("freezer",
        props -> new com.alexh.capitalism.farmer.ColdStorageBlock(com.alexh.capitalism.farmer.ColdStorageBlockEntity.Kind.FREEZER, props), props -> props
            .mapColor(MapColor.SNOW).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredBlock<com.alexh.capitalism.farmer.FertilizedFarmlandBlock> FERTILIZED_FARMLAND = BLOCKS.registerBlock("fertilized_farmland",
        com.alexh.capitalism.farmer.FertilizedFarmlandBlock::new, props -> props
            .mapColor(MapColor.DIRT)
            .randomTicks()
            .strength(0.6F)
            .sound(SoundType.GRAVEL)
            .isSuffocating((state, level, pos) -> true));
    // Pet Trainer (pets/): the Food Troughs and Water Bowls pets eat and drink from, Small (copper), Medium (iron), Large
    // (Compacted Iron)
    public static final DeferredBlock<com.alexh.capitalism.pets.FoodTroughBlock> SMALL_FOOD_TROUGH = BLOCKS.registerBlock("small_food_trough",
        props -> new com.alexh.capitalism.pets.FoodTroughBlock(props, com.alexh.capitalism.pets.FeederSize.SMALL), props -> props
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.pets.WaterBowlBlock> SMALL_WATER_BOWL = BLOCKS.registerBlock("small_water_bowl",
        props -> new com.alexh.capitalism.pets.WaterBowlBlock(props, com.alexh.capitalism.pets.FeederSize.SMALL), props -> props
            .mapColor(MapColor.COLOR_ORANGE).strength(3.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.pets.FoodTroughBlock> MEDIUM_FOOD_TROUGH = BLOCKS.registerBlock("medium_food_trough",
        props -> new com.alexh.capitalism.pets.FoodTroughBlock(props, com.alexh.capitalism.pets.FeederSize.MEDIUM), props -> props
            .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.pets.WaterBowlBlock> MEDIUM_WATER_BOWL = BLOCKS.registerBlock("medium_water_bowl",
        props -> new com.alexh.capitalism.pets.WaterBowlBlock(props, com.alexh.capitalism.pets.FeederSize.MEDIUM), props -> props
            .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.pets.FoodTroughBlock> LARGE_FOOD_TROUGH = BLOCKS.registerBlock("large_food_trough",
        props -> new com.alexh.capitalism.pets.FoodTroughBlock(props, com.alexh.capitalism.pets.FeederSize.LARGE), props -> props
            .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<com.alexh.capitalism.pets.WaterBowlBlock> LARGE_WATER_BOWL = BLOCKS.registerBlock("large_water_bowl",
        props -> new com.alexh.capitalism.pets.WaterBowlBlock(props, com.alexh.capitalism.pets.FeederSize.LARGE), props -> props
            .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    // The Knowledge Creator: Librarians turn Enchanted Books into Knowledge Pieces (librarian/KnowledgeCreatorBlock)
    public static final DeferredBlock<com.alexh.capitalism.librarian.KnowledgeCreatorBlock> KNOWLEDGE_CREATOR = BLOCKS.registerBlock("knowledge_creator",
        com.alexh.capitalism.librarian.KnowledgeCreatorBlock::new, props -> props
            .mapColor(MapColor.DEEPSLATE)
            .strength(3.0F)
            .sound(SoundType.DEEPSLATE)
            .noOcclusion()
            .lightLevel(state -> state.getValue(com.alexh.capitalism.librarian.KnowledgeCreatorBlock.WORKING) ? 7 : 2));
    // The Librarian Table: Librarians write skill books (librarian/LibrarianTableBlock)
    public static final DeferredBlock<com.alexh.capitalism.librarian.LibrarianTableBlock> LIBRARIAN_TABLE = BLOCKS.registerBlock("librarian_table",
        com.alexh.capitalism.librarian.LibrarianTableBlock::new, props -> props
            .mapColor(MapColor.COLOR_GREEN)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion());
    // Armorsmiths make it: hammers the dents out of armor
    public static final DeferredBlock<WhetstoneBlock> ARMOR_WHETSTONE = BLOCKS.registerBlock("armor_whetstone",
        props -> new WhetstoneBlock(WhetstoneBlock.Kind.ARMOR, props), props -> props
            .mapColor(MapColor.STONE)
            .strength(1.5F)
            .sound(SoundType.STONE)
            .noOcclusion());

    // Player shop (2-tall market stall). Tough, and only its owner can break it (see ContainerLocks).
    public static final DeferredBlock<ShopfrontBlock> SHOPFRONT = BLOCKS.registerBlock("shopfront",
        ShopfrontBlock::new, props -> props
            .mapColor(MapColor.WOOD)
            .strength(5.0F, 1200.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.IMMOVEABLE));

    // The seasonal decorations (decor/, Decorations.md): placed once, up in their holiday and packed away the rest of
    // the year (decor/SeasonalDecorBlock). Their light only while they're up.
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> CHRISTMAS_TREE =
        decor("christmas_tree", com.alexh.capitalism.decor.DecorKind.CHRISTMAS_TREE, Block.box(5, 0, 5, 11, 16, 11), 12, SoundType.GRASS, MapColor.PLANT);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> STRING_LIGHT_HOOK =
        decor("string_light_hook", com.alexh.capitalism.decor.DecorKind.STRING_LIGHTS, Block.box(6, 6, 6, 10, 10, 10), 6, SoundType.LANTERN, MapColor.METAL);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> PRESENT_PILE =
        decor("present_pile", com.alexh.capitalism.decor.DecorKind.PRESENT_PILE, Block.box(0, 0, 0, 16, 12, 16), 0, SoundType.WOOL, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> CANDY_CANE_POLE =
        decor("candy_cane_pole", com.alexh.capitalism.decor.DecorKind.CANDY_CANE_POLE, Block.box(6.5, 0, 6.5, 9.5, 16, 9.5), 0, SoundType.STONE, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> CANDY_CANES =
        decor("candy_canes", com.alexh.capitalism.decor.DecorKind.CANDY_CANES, Block.box(4, 0, 4, 12, 12, 12), 0, SoundType.STONE, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> CHRISTMAS_FROST =
        decor("christmas_frost", com.alexh.capitalism.decor.DecorKind.CHRISTMAS_FROST, Block.box(0, 0, 0, 16, 1, 16), 0, SoundType.SNOW, MapColor.SNOW);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> ICICLES =
        decor("icicles", com.alexh.capitalism.decor.DecorKind.ICICLES, Block.box(1, 3, 1, 15, 16, 15), 0, SoundType.GLASS, MapColor.ICE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> SPOOKY_SCARECROW =
        decor("spooky_scarecrow", com.alexh.capitalism.decor.DecorKind.SCARECROW, Block.box(6, 0, 6, 10, 16, 10), 7, SoundType.WOOD, MapColor.WOOD);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> WITCHS_CAULDRON =
        decor("witchs_cauldron", com.alexh.capitalism.decor.DecorKind.WITCHS_CAULDRON, Block.box(1, 0, 1, 15, 13, 15), 9, SoundType.METAL, MapColor.METAL);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> GRAVESTONE_ROUND =
        decor("gravestone_round", com.alexh.capitalism.decor.DecorKind.GRAVESTONE_ROUND, Block.box(5, 0, 2, 11, 16, 14), 0, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> GRAVESTONE_CROSS =
        decor("gravestone_cross", com.alexh.capitalism.decor.DecorKind.GRAVESTONE_CROSS, Block.box(5, 0, 5, 11, 16, 11), 0, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> GRAVESTONE_TOMB =
        decor("gravestone_tomb", com.alexh.capitalism.decor.DecorKind.GRAVESTONE_TOMB, Block.box(5, 0, 2, 11, 16, 14), 5, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> BONE_PILE =
        decor("bone_pile", com.alexh.capitalism.decor.DecorKind.BONE_PILE, Block.box(1, 0, 1, 15, 6, 15), 0, SoundType.BONE_BLOCK, MapColor.SAND);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> GROUND_FOG =
        decor("ground_fog", com.alexh.capitalism.decor.DecorKind.GROUND_FOG, Block.box(0, 0, 0, 16, 2, 16), 0, SoundType.WOOL, MapColor.SNOW);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> AUTUMN_MAPLE =
        decor("autumn_maple", com.alexh.capitalism.decor.DecorKind.AUTUMN_MAPLE, Block.box(5, 0, 5, 11, 16, 11), 0, SoundType.WOOD, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> AUTUMN_BIRCH =
        decor("autumn_birch", com.alexh.capitalism.decor.DecorKind.AUTUMN_BIRCH, Block.box(6, 0, 6, 10, 16, 10), 0, SoundType.WOOD, MapColor.COLOR_YELLOW);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> FALLEN_LEAVES =
        decor("fallen_leaves", com.alexh.capitalism.decor.DecorKind.FALLEN_LEAVES, Block.box(0, 0, 0, 16, 1, 16), 0, SoundType.AZALEA_LEAVES, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> BEACH_TOWEL =
        decor("beach_towel", com.alexh.capitalism.decor.DecorKind.BEACH_TOWEL, Block.box(0, 0, 0, 16, 1, 16), 0, SoundType.WOOL, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> BEACH_BALL =
        decor("beach_ball", com.alexh.capitalism.decor.DecorKind.BEACH_BALL, Block.box(3, 0, 3, 13, 10, 13), 0, SoundType.WOOL, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> BEACH_UMBRELLA =
        decor("beach_umbrella", com.alexh.capitalism.decor.DecorKind.BEACH_UMBRELLA, Block.box(7, 0, 7, 9, 16, 9), 0, SoundType.WOOL, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> BEACH_CHAIR =
        decor("beach_chair", com.alexh.capitalism.decor.DecorKind.BEACH_CHAIR, Block.box(1, 0, 2, 15, 10, 14), 0, SoundType.WOOD, MapColor.COLOR_BLUE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> EGG_BASKET =
        decor("egg_basket", com.alexh.capitalism.decor.DecorKind.EGG_BASKET, Block.box(3, 0, 3, 13, 8, 13), 0, SoundType.WOOD, MapColor.COLOR_YELLOW);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> GIANT_EGG =
        decor("giant_egg", com.alexh.capitalism.decor.DecorKind.GIANT_EGG, Block.box(2, 0, 2, 14, 16, 14), 0, SoundType.STONE, MapColor.COLOR_PINK);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> SPRING_BUNNY =
        decor("spring_bunny", com.alexh.capitalism.decor.DecorKind.SPRING_BUNNY, Block.box(3, 0, 3, 13, 14, 13), 0, SoundType.WOOL, MapColor.SNOW);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> TULIP_BED =
        decor("tulip_bed", com.alexh.capitalism.decor.DecorKind.TULIP_BED, Block.box(0, 0, 0, 16, 8, 16), 0, SoundType.GRASS, MapColor.PLANT);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> PUMPKIN_PILE =
        decor("pumpkin_pile", com.alexh.capitalism.decor.DecorKind.PUMPKIN_PILE, Block.box(0, 0, 0, 16, 16, 16), 0, SoundType.WOOD, MapColor.COLOR_ORANGE);
    public static final DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> LEAF_PILE =
        decor("leaf_pile", com.alexh.capitalism.decor.DecorKind.LEAF_PILE, Block.box(0, 0, 0, 16, 16, 16), 0, SoundType.AZALEA_LEAVES, MapColor.COLOR_RED);

    private static DeferredBlock<com.alexh.capitalism.decor.SeasonalDecorBlock> decor(String name, com.alexh.capitalism.decor.DecorKind kind,
            net.minecraft.world.phys.shapes.VoxelShape shape, int light, SoundType sound, MapColor color) {
        return BLOCKS.registerBlock(name, props -> new com.alexh.capitalism.decor.SeasonalDecorBlock(kind, shape, light, props), props -> props
            .mapColor(color)
            // nobody breaks them (operators in Creative can: decor/Decorations), not explosions, not pistons
            .strength(-1.0F, 3600000.0F)
            .pushReaction(PushReaction.IMMOVEABLE)
            .sound(sound)
            .noOcclusion()
            .dynamicShape()
            .lightLevel(state -> state.getValue(com.alexh.capitalism.decor.SeasonalDecorBlock.UP) ? light : 0));
    }

    // The Blood Moon decorations (decor/BloodDecorKind, Decorations.md): always there, anyone can break one (it drops
    // itself) and place it anywhere. Sold by the Night Merchant for Blood Shards (the shop finds them by these ids).
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_MOON_LANTERN =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_MOON_LANTERN, SoundType.LANTERN, MapColor.COLOR_BLACK);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> CRIMSON_CANDELABRA =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.CRIMSON_CANDELABRA, SoundType.METAL, MapColor.COLOR_BLACK);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> CRIMSON_THRONE =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.CRIMSON_THRONE, SoundType.WOOD, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodBedBlock> VAMPIRE_COFFIN =
        bloodBed(com.alexh.capitalism.decor.BloodDecorKind.VAMPIRE_COFFIN, SoundType.WOOD, MapColor.COLOR_BLACK);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_FOUNTAIN =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_FOUNTAIN, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_MOON_BANNER =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_MOON_BANNER, SoundType.WOOL, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> CRIMSON_RUG =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.CRIMSON_RUG, SoundType.WOOL, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> REVENANT_GREATSWORD_STATUE =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.REVENANT_GREATSWORD_STATUE, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> GOREFANG_SKULL_TROPHY =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.GOREFANG_SKULL_TROPHY, SoundType.BONE_BLOCK, MapColor.SAND);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> SANGUINE_STAFF_PEDESTAL =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.SANGUINE_STAFF_PEDESTAL, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BAT_GARGOYLE =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BAT_GARGOYLE, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> GRANDFATHER_CLOCK =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.GRANDFATHER_CLOCK, SoundType.WOOD, MapColor.COLOR_BROWN);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> CRIMSON_CHANDELIER =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.CRIMSON_CHANDELIER, SoundType.CHAIN, MapColor.COLOR_BLACK);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_PIPE_ORGAN =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_PIPE_ORGAN, SoundType.WOOD, MapColor.COLOR_BROWN);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDoorBlock> SECRET_BOOKSHELF_DOOR =
        BLOCKS.registerBlock("secret_bookshelf_door", props -> new com.alexh.capitalism.decor.BloodDoorBlock(com.alexh.capitalism.decor.BloodDecorKind.SECRET_BOOKSHELF_DOOR, props),
            props -> props.mapColor(MapColor.WOOD).strength(1.5F, 6.0F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> HAUNTED_PORTRAIT =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.HAUNTED_PORTRAIT, SoundType.WOOD, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> IRON_MAIDEN =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.IRON_MAIDEN, SoundType.METAL, MapColor.METAL);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_MOON_ALTAR =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_MOON_ALTAR, SoundType.STONE, MapColor.STONE);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BAT_ROOST =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BAT_ROOST, SoundType.WOOD, MapColor.COLOR_BLACK);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodBedBlock> CRIMSON_CANOPY_BED =
        bloodBed(com.alexh.capitalism.decor.BloodDecorKind.CRIMSON_CANOPY_BED, SoundType.WOOD, MapColor.COLOR_RED);
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> BLOOD_CRYSTAL_FIREPLACE =
        bloodDecor(com.alexh.capitalism.decor.BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE, SoundType.STONE, MapColor.STONE);
    // A mounted rare catch on a wall (fishing/FishPlaqueBlock, Fishing.md > Plaques): 1 to 3 blocks wide
    public static final DeferredBlock<com.alexh.capitalism.fishing.FishPlaqueBlock> FISH_PLAQUE = BLOCKS.registerBlock("fish_plaque",
        com.alexh.capitalism.fishing.FishPlaqueBlock::new, props -> props.mapColor(MapColor.WOOD).strength(1.0F, 3.0F).sound(SoundType.WOOD)
            .noOcclusion().pushReaction(PushReaction.IMMOVEABLE));

    // the extra blocks the big Blood Moon decorations fill (decor/BloodPartBlock: no item, they drop nothing themselves)
    public static final DeferredBlock<com.alexh.capitalism.decor.BloodPartBlock> BLOOD_DECOR_PART = BLOCKS.registerBlock("blood_decor_part",
        com.alexh.capitalism.decor.BloodPartBlock::new, props -> props.mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.WOOD)
            .noOcclusion().noLootTable().dynamicShape().pushReaction(PushReaction.IMMOVEABLE));

    // Blood Moon building blocks (Town.md > The Blood Merchant: 1 Blood Shard each): dark bloodstone bricks with red
    // veins, and a glowing see-through blood crystal block
    public static final DeferredBlock<Block> BLOODSTONE_BRICKS = BLOCKS.registerBlock("bloodstone_bricks", Block::new, props -> props
        .mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> BLOOD_CRYSTAL_BLOCK = BLOCKS.registerBlock("blood_crystal_block",
        net.minecraft.world.level.block.TransparentBlock::new, props -> props
            .mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.AMETHYST).lightLevel(state -> 10).noOcclusion()
            .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false));

    // the beds (the coffin, the canopy bed): two blocks, like a bed
    private static DeferredBlock<com.alexh.capitalism.decor.BloodBedBlock> bloodBed(com.alexh.capitalism.decor.BloodDecorKind kind,
            SoundType sound, MapColor color) {
        return BLOCKS.registerBlock(kind.id, props -> new com.alexh.capitalism.decor.BloodBedBlock(kind, props), props -> props
            .mapColor(color)
            .strength(1.5F, 6.0F)
            .sound(sound)
            .noOcclusion()
            .pushReaction(PushReaction.POPPED));
    }

    private static DeferredBlock<com.alexh.capitalism.decor.BloodDecorBlock> bloodDecor(com.alexh.capitalism.decor.BloodDecorKind kind,
            SoundType sound, MapColor color) {
        return BLOCKS.registerBlock(kind.id, props -> new com.alexh.capitalism.decor.BloodDecorBlock(kind, props), props -> props
            .mapColor(color)
            .strength(1.5F, 6.0F)
            .sound(sound)
            .noOcclusion()
            .pushReaction(PushReaction.IMMOVEABLE)
            .lightLevel(state -> kind.lights && !state.getValue(com.alexh.capitalism.decor.BloodDecorBlock.LIT) ? 0 : kind.light));
    }
}

package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.JobGuideScreen.Entry.info;
import static com.alexh.capitalism.client.JobGuideScreen.Entry.made;

// The Farmer's Guide (Farmers only, JobGuideScreen's layout in soil brown and wheat gold):
//  - Workshop: Blocks (the Decomposter), Items (the Yield, Super Yield and Fast Fertilizers, the Scythes). Every recipe lives here.
//  - Information: Harvests, Fertilizer, Butchering, and Player Debuffs, as clickable info boxes.
// Keep every line short, and no numbers: players find those out. Hand-written: keep it matching Farmer.md and farmer/.
public class FarmersGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF3A2A18, 0xFF22180C, 0xFFE8C860, 0xFF6A5020, 0xFF140E06, 0xFF6A5838,
        0xFFF6E08E, 0xFFF4EEDC, 0xFFC4B490, 0xFFE8D48A);

    private static final Tab BLOCKS = new Tab("Workshop", "Blocks", new ItemStack(ModItems.DECOMPOSTER.get()));
    private static final Tab ITEMS = new Tab("Workshop", "Items", new ItemStack(ModItems.FERTILIZER.get()));
    private static final Tab HARVESTS = new Tab("Information", "Harvests", new ItemStack(Items.WHEAT));
    private static final Tab FERTILIZING = new Tab("Information", "Fertilizer", new ItemStack(ModItems.SUPER_FERTILIZER.get()));
    private static final Tab BUTCHERING = new Tab("Information", "Butchering", new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB.slab()));
    private static final Tab FRESHNESS = new Tab("Information", "Freshness", new ItemStack(Items.CLOCK));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public FarmersGuideScreen(@Nullable Screen parent) {
        super("Farmer's Guide", THEME, List.of(BLOCKS, ITEMS, HARVESTS, FERTILIZING, BUTCHERING, FRESHNESS, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
        ItemStack dirt = new ItemStack(Items.DIRT);
        ItemStack decomposter = new ItemStack(ModItems.DECOMPOSTER.get());
        ItemStack fertilizer = new ItemStack(ModItems.FERTILIZER.get());
        ItemStack superFertilizer = new ItemStack(ModItems.SUPER_FERTILIZER.get());
        ItemStack fastFertilizer = new ItemStack(ModItems.FAST_FERTILIZER.get());
        ItemStack sugar = new ItemStack(Items.SUGAR);
        ItemStack scythe = new ItemStack(ModItems.SCYTHE.get());
        ItemStack diamondScythe = new ItemStack(ModItems.DIAMOND_SCYTHE.get());
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack slab = new ItemStack(Items.OAK_SLAB);
        ItemStack table = new ItemStack(ModItems.BUTCHERS_TABLE.get());
        ItemStack ribSlab = new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB.slab());
        ItemStack rib = new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB.cut());
        ItemStack chuckSlab = new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_CHUCK.slab());
        ItemStack hamSlab = new ItemStack(com.alexh.capitalism.farmer.MeatCut.PORK_HAM.slab());
        ItemStack rackSlab = new ItemStack(com.alexh.capitalism.farmer.MeatCut.MUTTON_RACK.slab());

        List<Entry> list = new ArrayList<>();

        // ── Workshop: Blocks ──
        list.add(made(BLOCKS, decomposter, "Decomposter",
            HowToMake.crafting(planks, iron, planks, planks, new ItemStack(Items.COMPOSTER), planks, planks, planks, planks),
            "Like a furnace with no fuel.", "Put in three ingredients and wait."));
        list.add(made(BLOCKS, table, "Butcher's Table",
            HowToMake.crafting(iron, e, e, slab, slab, slab, log, e, log),
            "Cut Meat Slabs up into fine cuts."));
        ItemStack chest = new ItemStack(Items.CHEST);
        ItemStack fridge = new ItemStack(com.alexh.capitalism.core.ModItems.FRIDGE.get());
        ItemStack freezer = new ItemStack(com.alexh.capitalism.core.ModItems.FREEZER.get());
        list.add(made(BLOCKS, fridge, "Fridge",
            HowToMake.crafting(iron, iron, iron, iron, chest, iron, iron, redstone, iron),
            "Meat keeps longer in it", "while it burns coal or charcoal."));
        list.add(made(BLOCKS, freezer, "Freezer",
            HowToMake.crafting(iron, iron, iron, iron, chest, iron, iron, new ItemStack(Items.PACKED_ICE), iron),
            "Meat keeps much longer in it", "while it melts any ice or snowballs.", "Packed, Blue and Butcher Shop Ice", "last longest."));

        // ── Workshop: Items ──
        list.add(made(ITEMS, fertilizer, "Yield Fertilizer",
            HowToMake.steps("Rotted down in the Decomposter.", boneMeal, flesh, dirt),
            "Use it on farmland.", "Its next harvests give more crops."));
        list.add(made(ITEMS, superFertilizer, "Super Yield Fertilizer",
            HowToMake.steps("Rotted down in the Decomposter.", boneMeal, redstone, redstone),
            "Like Yield Fertilizer, but it gives", "more crops and lasts longer."));
        list.add(made(ITEMS, fastFertilizer, "Fast Fertilizer",
            HowToMake.steps("Rotted down in the Decomposter.", boneMeal, sugar, dirt),
            "Use it on farmland.", "Crops on it grow faster."));
        // Butcher Shop Ice: 4 Ice in a 2x2, anyone can make it
        ItemStack butcherIce = new ItemStack(ModItems.BUTCHER_SHOP_ICE.get());
        ItemStack ice = new ItemStack(Items.ICE);
        ItemStack none = ItemStack.EMPTY;
        list.add(made(ITEMS, butcherIce, "Butcher Shop Ice",
            HowToMake.crafting(ice, ice, none, ice, ice, none, none, none, none),
            "Ice that never melts.", "Show your meat on it in the shop:", "it keeps like on any ice."));
        // Scythes and Butcher Knives are made by Toolsmiths (their guide has the recipes): Farmers buy them
        list.add(info(ITEMS, scythe, "Scythe", List.of(scythe),
            "Cut one ripe crop and it cuts", "the ripe crops around it too.", "Buy one from a Toolsmith."));
        list.add(info(ITEMS, diamondScythe, "Diamond Scythe", List.of(diamondScythe),
            "The same patch as the Scythe,", "but it lasts far longer.", "Buy one from a Toolsmith."));
        ItemStack ironKnife = new ItemStack(com.alexh.capitalism.core.ModItems.IRON_BUTCHER_KNIFE.get());
        ItemStack diamondKnife = new ItemStack(com.alexh.capitalism.core.ModItems.DIAMOND_BUTCHER_KNIFE.get());
        list.add(info(ITEMS, ironKnife, "Iron Butcher Knife", List.of(ironKnife),
            "Goes on the Butcher's Table.", "Cuts a few slabs before it's spent.", "Buy from a Toolsmith or their NPC."));
        list.add(info(ITEMS, diamondKnife, "Diamond Butcher Knife", List.of(diamondKnife),
            "A cleaver that cuts many more", "slabs before it's spent.", "Buy one from a Toolsmith."));

        // ── Information: Harvests ──
        list.add(info(HARVESTS, new ItemStack(Items.WHEAT), "Full Harvests", List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.CARROT), new ItemStack(Items.POTATO)),
            "Only you get the full harvest", "from ripe crops, and the full", "meat from animals."));
        list.add(info(HARVESTS, new ItemStack(Items.WHEAT_SEEDS), "Everyone Else", List.of(new ItemStack(Items.WHEAT_SEEDS)),
            "Others get one crop and one seed back.", "Water, pistons and villagers too."));
        list.add(info(HARVESTS, scythe, "The Scythes", List.of(scythe, diamondScythe),
            "Unripe crops are left standing.", "Anyone can use one."));

        // ── Information: Fertilizer ──
        list.add(info(FERTILIZING, new ItemStack(Items.FARMLAND), "Fertilizing", List.of(fertilizer, superFertilizer, fastFertilizer),
            "Right-click farmland, or a crop on it.", "It stays fertilized for a few harvests."));
        list.add(info(FERTILIZING, new ItemStack(Items.BARRIER), "One at a Time", List.of(fertilizer, superFertilizer, fastFertilizer),
            "Farmland takes one Fertilizer of any kind.", "Use it up before adding another."));
        list.add(info(FERTILIZING, new ItemStack(Items.DIRT), "Don't Trample It", List.of(),
            "Fertilized land is speckled so you can spot it.", "Trample it to dirt and the fertilizer is lost."));
        list.add(info(FERTILIZING, new ItemStack(Items.MELON), "Melons and Pumpkins", List.of(new ItemStack(Items.MELON), new ItemStack(Items.PUMPKIN)),
            "Fertilize the farmland under the stem."));
        list.add(info(FERTILIZING, new ItemStack(Items.BARRIER), "Only Your Harvests", List.of(),
            "Only your harvests get the extra crops.", "Others' harvests don't use it up."));

        // ── Information: Butchering ──
        ItemStack knife = new ItemStack(com.alexh.capitalism.core.ModItems.IRON_BUTCHER_KNIFE.get());
        ItemStack diamondKnifeIcon = new ItemStack(com.alexh.capitalism.core.ModItems.DIAMOND_BUTCHER_KNIFE.get());
        list.add(info(BUTCHERING, ribSlab, "Meat Slabs", List.of(chuckSlab, hamSlab, rackSlab, ribSlab),
            "Kill grown animals with a diamond or", "netherite sword or axe. Sometimes", "they drop a Meat Slab too:", "a big chunk of the animal."));
        list.add(info(BUTCHERING, new ItemStack(Items.RABBIT_FOOT), "Luck", List.of(),
            "Luck brings slabs more often,", "and the rarer, bigger ones too."));
        list.add(info(BUTCHERING, chuckSlab, "Showing Them Off", List.of(chuckSlab, ribSlab),
            "Set a slab down to show it off.", "Look at it to see how lean", "it is and how long it keeps."));
        list.add(info(BUTCHERING, knife, "Butcher Knives", List.of(knife, diamondKnifeIcon),
            "Every slab needs a knife on the table.", "Each slab dulls it, and it breaks", "when it's spent. The knife stays", "in the table between uses."));
        list.add(info(BUTCHERING, new ItemStack(ModItems.PICKAXE_WHETSTONE.get()), "Sharpening", List.of(knife, diamondKnifeIcon),
            "A Tool Whetstone sharpens", "a Butcher Knife like new.", "Toolsmiths make them."));
        list.add(info(BUTCHERING, table, "Cutting a Slab", List.of(ribSlab, rib),
            "Put a slab and a knife on the table", "and press Cut Slab. Three steps:", "trim, scan, cut."));
        list.add(info(BUTCHERING, new ItemStack(Items.SHEARS), "1: Trim the Fat", List.of(),
            "Drag the cleaver over the pale fat", "until every bit is gone. No rush,", "but spare the meat: cut too much", "and it's botched."));
        list.add(info(BUTCHERING, new ItemStack(Items.SPYGLASS), "2: Scan for Bones", List.of(),
            "Hold to look through the scanner.", "Bones glow and it ticks faster near", "one. Right-click to pin a spot.", "The battery is short!"));
        list.add(info(BUTCHERING, new ItemStack(Items.IRON_AXE), "3: Make Two Cuts", List.of(),
            "From the meat's top edge down out", "the bottom, in one go. Bones curve", "and hide: touch one and it's botched."));
        list.add(info(BUTCHERING, new ItemStack(Items.BONE), "Botched", List.of(new ItemStack(Items.BEEF)),
            "Too much meat lost, a bone hit,", "or out of time: you get plain", "raw meat instead."));
        list.add(info(BUTCHERING, rib, "Fine Cuts", List.of(rib),
            "A clean job gives the cut itself,", "one piece at a time.", "Cooks will want these."));

        // ── Information: Freshness ──
        ItemStack rotten = new ItemStack(com.alexh.capitalism.core.ModItems.ROTTEN_MEAT.get());
        ItemStack fridgeIcon = new ItemStack(com.alexh.capitalism.core.ModItems.FRIDGE.get());
        ItemStack freezerIcon = new ItemStack(com.alexh.capitalism.core.ModItems.FREEZER.get());
        list.add(info(FRESHNESS, new ItemStack(Items.CLOCK), "Meat Rots", List.of(ribSlab, rib),
            "Slabs and fine cuts rot, wherever", "they are. Each piece keeps its", "own time, so they don't stack."));
        list.add(info(FRESHNESS, rib, "The Freshness Bar", List.of(rib),
            "The bar under a piece shows how", "fresh it is: green, then yellow,", "then red. Blue while it's chilled."));
        list.add(info(FRESHNESS, table, "Cutting Buys Time", List.of(ribSlab, rib),
            "Cutting a slab up gives its cuts", "a little more time than it had left."));
        list.add(info(FRESHNESS, chuckSlab, "Lean", List.of(chuckSlab),
            "A fatty slab gives less meat", "and has more fat to trim.", "A lean one is quick work."));
        list.add(info(FRESHNESS, fridgeIcon, "The Fridge", List.of(fridgeIcon, new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL)),
            "Meat keeps longer in it while it", "burns coal or charcoal. It only", "burns while there's meat inside."));
        list.add(info(FRESHNESS, freezerIcon, "The Freezer", List.of(freezerIcon, new ItemStack(Items.ICE), new ItemStack(Items.PACKED_ICE), new ItemStack(Items.BLUE_ICE), new ItemStack(Items.SNOWBALL)),
            "Meat keeps much longer in it", "while it melts any ice or snowballs.", "Packed, Blue and Butcher Shop Ice", "last longest."));
        list.add(info(FRESHNESS, rib, "Dry-Aging", List.of(rib, fridgeIcon),
            "Fine cuts left in a running Fridge", "age: Dry-Aged, then Prime. Cooks", "sear better steaks from them."));
        list.add(info(FRESHNESS, new ItemStack(Items.PACKED_ICE), "On Ice", List.of(new ItemStack(Items.ICE), new ItemStack(Items.PACKED_ICE),
                new ItemStack(Items.BLUE_ICE), new ItemStack(ModItems.BUTCHER_SHOP_ICE.get())),
            "Set a slab down on ice and", "it keeps like in a Freezer.", "Plain ice can melt;", "Butcher Shop Ice never does."));
        list.add(info(FRESHNESS, new ItemStack(com.alexh.capitalism.core.ModItems.FREEZER_SLING.get()), "Freezer Sling", List.of(new ItemStack(com.alexh.capitalism.core.ModItems.FREEZER_SLING.get())),
            "A Designer's bag that keeps meat", "like a Freezer on your back."));
        list.add(info(FRESHNESS, rotten, "Rotten Meat", List.of(rotten),
            "What meat turns into. Not for", "eating, but Cooks make pet food", "with it, and it rots down into", "fertilizer in the Decomposter."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, new ItemStack(Items.WHEAT_SEEDS), "Small Harvests", List.of(),
            "Others only get one crop", "and one seed back from a ripe crop."));
        list.add(info(DEBUFFS, new ItemStack(Items.BEEF), "Less Meat", List.of(),
            "Others only get one piece", "of meat from an animal."));
        list.add(info(DEBUFFS, decomposter, "No Decomposter", List.of(),
            "Others can't make or use the Decomposter."));
        list.add(info(DEBUFFS, fertilizer, "No Fertilizer", List.of(),
            "Others can't use any Fertilizer."));
        list.add(info(DEBUFFS, table, "No Butchering", List.of(),
            "Others get no Meat Slabs", "and can't use the Butcher's Table."));
        return list;
    }
}

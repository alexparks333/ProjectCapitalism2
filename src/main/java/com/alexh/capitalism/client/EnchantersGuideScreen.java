package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.JobGuideScreen.Entry.info;
import static com.alexh.capitalism.client.JobGuideScreen.Entry.made;

// The Enchanter's Guide (Enchanters only, JobGuideScreen's layout in night violet):
//  - Workshop: Blocks (Enchanter's Table, Transfer Tube, Lapis Compressor), Items (Ench-Removal, Apprentice Wand,
//    Apprentice) and Using the Table (removing an enchantment, shown like a table recipe). Every recipe lives here.
//  - Information: Enchanting and Perks, as clickable info boxes. No recipes.
// Keep every line short, and no numbers (levels, costs, XP amounts): players find those out.
// Hand-written: keep it matching Enchanter.md, the enchanter package and the recipes.
public class EnchantersGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF2A2140, 0xFF161026, 0xFFA77BEA, 0xFF4A2E7A, 0xFF120C1E, 0xFF6E6290,
        0xFFC9A4FF, 0xFFEDE4FF, 0xFFA898C8, 0xFFD6B8FF);

    private static final Tab BLOCKS = new Tab("Workshop", "Blocks", new ItemStack(ModItems.ENCHANTERS_TABLE.get()));
    private static final Tab ITEMS = new Tab("Workshop", "Items", new ItemStack(ModItems.ENCH_REMOVAL.get()));
    private static final Tab USING = new Tab("Workshop", "Using the Table", new ItemStack(ModItems.ENCHANTERS_TABLE.get()));
    private static final Tab ENCHANTING = new Tab("Information", "Enchanting", new ItemStack(Items.ENCHANTING_TABLE));
    private static final Tab PERKS = new Tab("Information", "Perks", new ItemStack(Items.ENCHANTED_BOOK));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public EnchantersGuideScreen(@Nullable Screen parent) {
        super("Enchanter's Guide", THEME, List.of(BLOCKS, ITEMS, USING, ENCHANTING, PERKS, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack enchantersTable = new ItemStack(ModItems.ENCHANTERS_TABLE.get());
        ItemStack tube = new ItemStack(ModItems.TRANSFER_TUBE.get());
        ItemStack compressor = new ItemStack(ModItems.LAPIS_COMPRESSOR.get());
        ItemStack removal = new ItemStack(ModItems.ENCH_REMOVAL.get());
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack candle = new ItemStack(Items.CANDLE);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack glass = new ItemStack(Items.GLASS);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack lapisBlock = new ItemStack(Items.LAPIS_BLOCK);
        ItemStack compactedIron = new ItemStack(ModItems.COMPACTED_IRON.get());
        ItemStack enchantedSword = new ItemStack(Items.DIAMOND_SWORD);

        List<Entry> list = new ArrayList<>();

        // ── Workshop: Blocks ──
        list.add(made(BLOCKS, enchantersTable, "Enchanter's Table",
            HowToMake.crafting(candle, book, candle, book, compactedIron, book, planks, planks, planks),
            "Removes enchantments. Enchanters only."));
        list.add(made(BLOCKS, tube, "Transfer Tube",
            HowToMake.crafting(glass, lapisBlock, glass, pearl, new ItemStack(Items.ENCHANTING_TABLE), pearl, obsidian, obsidian, obsidian),
            "Moves XP between players and Apprentices.", "Anyone can stand in a booth."));
        list.add(made(BLOCKS, compressor, "Lapis Compressor",
            HowToMake.crafting(iron, new ItemStack(Items.PISTON), iron, lapisBlock, compactedIron, lapisBlock, iron, iron, iron),
            "Turns Lapis into XP.", "Anyone can use it."));

        // ── Workshop: Items ──
        list.add(made(ITEMS, removal, "Ench-Removal",
            HowToMake.table("At the Enchanter's Table.", book, new ItemStack(Items.BLAZE_ROD)),
            "Burns one enchantment off an item."));
        ItemStack wand = new ItemStack(ModItems.APPRENTICE_WAND.get());
        list.add(made(ITEMS, wand, "Apprentice Wand",
            HowToMake.table("At the Enchanter's Table.", new ItemStack(Items.BAMBOO), PotionContents.createItemStack(Items.POTION, Potions.POISON)),
            "Turns an armor stand into an Apprentice.", "Wears out."));
        ItemStack apprentice = new ItemStack(ModItems.APPRENTICE.get());
        list.add(made(ITEMS, apprentice, "Apprentice",
            HowToMake.steps("Hit an armor stand in a full set of armor with the wand.", new ItemStack(Items.ARMOR_STAND), wand),
            "Stores XP. Fill or empty it in a Transfer Tube.", "Better armor holds more.", "Right-click it with the wand to name it."));

        // ── Workshop: Using the Table ──
        list.add(made(USING, enchantedSword, "Removing Enchantments",
            HowToMake.table("At the Enchanter's Table.", enchantedSword, removal),
            "Pick an enchantment: it's gone for good.", "Curses too. Costs some levels."));

        // ── Information: Enchanting ──
        list.add(info(ENCHANTING, new ItemStack(Items.ENCHANTING_TABLE), "High Level Enchanting", List.of(new ItemStack(Items.ENCHANTING_TABLE)),
            "Only you can enchant high level enchantments.", "Others can only enchant at a low level.",
            "You can't enchant books, only Librarians can."));
        list.add(info(ENCHANTING, new ItemStack(Items.ANVIL), "The Anvil", List.of(new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.ANVIL)),
            "Only you can put enchantments onto items and gear.", "You can't combine books, only Librarians can."));

        // ── Information: Perks ──
        list.add(info(PERKS, new ItemStack(Items.ENCHANTED_BOOK), "Enchanter Only", List.of(new ItemStack(Items.ENCHANTING_TABLE), new ItemStack(Items.ANVIL), enchantersTable),
            "Only you can enchant at high levels, put enchantments onto gear,", "and take enchantments off at the Enchanter's Table."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, new ItemStack(Items.ENCHANTING_TABLE), "Capped Levels", List.of(),
            "Others can only enchant at a low level."));
        list.add(info(DEBUFFS, new ItemStack(Items.ANVIL), "No Anvil Enchanting", List.of(),
            "Others can't use an Anvil to put enchantments onto gear."));
        list.add(info(DEBUFFS, enchantersTable, "No Removing", List.of(),
            "Others can't use the Enchanter's Table to take enchantments off."));
        list.add(info(DEBUFFS, tube, "No Tube", List.of(),
            "Others can't build or run a Transfer Tube.", "Others can still stand in its booth."));
        list.add(info(DEBUFFS, compressor, "No Compressor", List.of(),
            "Others can't craft a Lapis Compressor,", "but others can still use one."));
        list.add(info(DEBUFFS, wand, "No Apprentices", List.of(),
            "Others can't make, name or pick up Apprentices."));
        return list;
    }
}

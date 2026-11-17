package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.JobGuideScreen.Entry.info;
import static com.alexh.capitalism.client.JobGuideScreen.Entry.made;

// The Toolsmith's Guide (Toolsmiths only, JobGuideScreen's layout in light oak and copper). Every Toolsmith Table recipe
// is in Forge (Blocks, Items, and Using the Table: repairs, rerolling, salvaging, sockets). Recipes is crafting only;
// Information has no recipes. Keep every line short.
// Hand-written: keep it matching Toolsmith.md, weaponsmith/ToolForging, Wear, ToolsmithTableRecipes and the recipes.
public class ToolsmithsGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF3A3226, 0xFF221C14, 0xFFD89A4A, 0xFF6A4418, 0xFF181410, 0xFF7A6E5E,
        0xFFF0A850, 0xFFF2E8DA, 0xFFB4A690, 0xFFFFC266);

    private static final Tab BLOCKS = new Tab("Forge", "Blocks", new ItemStack(ModItems.TOOLSMITH_TABLE.get()));
    private static final Tab ITEMS = new Tab("Forge", "Items", new ItemStack(ModItems.DIAMOND_REROLL_TOKEN.get()));
    private static final Tab USING = new Tab("Forge", "Using the Table", new ItemStack(ModItems.TOOLSMITH_TABLE.get()));
    private static final Tab CRAFTING = new Tab("Recipes", "Crafting", new ItemStack(Items.CRAFTING_TABLE));
    private static final Tab TOOLS = new Tab("Information", "Your Tools", new ItemStack(Items.DIAMOND_PICKAXE));
    private static final Tab PERKS = new Tab("Information", "Perks", new ItemStack(Items.NETHERITE_PICKAXE));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public ToolsmithsGuideScreen(@Nullable Screen parent) {
        super("Toolsmith's Guide", THEME, List.of(BLOCKS, ITEMS, USING, CRAFTING, TOOLS, PERKS, DEBUFFS), entries(), parent);
    }

    private static void addTools(List<Entry> list, ItemStack metal, String name, Item pickaxe, Item shovel, Item hoe) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack stick = new ItemStack(Items.STICK);
        String others = "Others: Compacted " + name + " instead.";
        list.add(made(CRAFTING, new ItemStack(pickaxe), name + " Pickaxe", HowToMake.crafting(metal, metal, metal, e, stick, e, e, stick, e), others));
        list.add(made(CRAFTING, new ItemStack(shovel), name + " Shovel", HowToMake.crafting(e, metal, e, e, stick, e, e, stick, e), others));
        list.add(made(CRAFTING, new ItemStack(hoe), name + " Hoe", HowToMake.crafting(metal, metal, e, e, stick, e, e, stick, e), others));
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack table = new ItemStack(ModItems.TOOLSMITH_TABLE.get());
        ItemStack whetstone = new ItemStack(ModItems.PICKAXE_WHETSTONE.get());
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack compactedIron = new ItemStack(ModItems.COMPACTED_IRON.get());
        ItemStack nameTag = new ItemStack(Items.NAME_TAG);
        HowToMake whetstoneRecipe = HowToMake.table("At the Toolsmith Table.",
            new ItemStack(Items.GRINDSTONE), compactedIron);

        List<Entry> list = new ArrayList<>();

        // ── Forge: Blocks ──
        list.add(made(BLOCKS, table, "Toolsmith Table",
            HowToMake.crafting(iron, new ItemStack(Items.IRON_PICKAXE), iron, planks, new ItemStack(Items.SMITHING_TABLE), planks, planks, e, planks),
            "Toolsmiths only. Runs on Lava Buckets."));
        list.add(made(BLOCKS, whetstone, "Pickaxe Whetstone", whetstoneRecipe,
            "Sharpens a pickaxe. Needs a Water Bucket.", "Breaks after one use."));

        // ── Forge: Items ──
        ItemStack hammer = new ItemStack(ModItems.SALVAGE_HAMMER.get());
        ItemStack socket = new ItemStack(ModItems.MOLTEN_SOCKET.get());
        ItemStack token = new ItemStack(ModItems.IRON_REROLL_TOKEN.get());
        list.add(made(ITEMS, compactedIron, "Compacted Iron", HowToMake.crafting(iron, iron, e, e, e, e, e, e, e),
            "Others need these for iron tools."));
        list.add(made(ITEMS, new ItemStack(ModItems.COMPACTED_DIAMOND.get()), "Compacted Diamond", HowToMake.crafting(diamond, diamond, e, e, e, e, e, e, e),
            "Others need these for diamond tools."));
        list.add(made(ITEMS, new ItemStack(ModItems.IRON_REROLL_TOKEN.get()), "Iron Reroll Token",
            HowToMake.table("At the Toolsmith Table.", nameTag, iron), "Rerolls iron gear."));
        list.add(made(ITEMS, new ItemStack(ModItems.DIAMOND_REROLL_TOKEN.get()), "Diamond Reroll Token",
            HowToMake.table("At the Toolsmith Table.", nameTag, diamond), "Rerolls diamond gear."));
        list.add(made(ITEMS, new ItemStack(ModItems.NETHERITE_REROLL_TOKEN.get()), "Netherite Reroll Token",
            HowToMake.table("At the Toolsmith Table.", nameTag, new ItemStack(Items.NETHERITE_INGOT)), "Rerolls netherite gear."));
        list.add(info(ITEMS, socket, "Molten Socket", List.of(), "Rare drop from hard mobs and bosses."));

        // ── Forge: Using the Table ──
        String atTable = "At the Toolsmith Table.";
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.setDamageValue(damagedPickaxe.getMaxDamage() / 2);
        list.add(made(USING, damagedPickaxe, "Forging Repairs", HowToMake.table(atTable, damagedPickaxe, iron),
            "Click the glowing dents before they cool.", "Costs some XP."));
        list.add(made(USING, token, "Rerolling", HowToMake.table(atTable, new ItemStack(Items.IRON_PICKAXE), token),
            "New Efficiency."));
        list.add(made(USING, hammer, "Salvaging", HowToMake.table(atTable, new ItemStack(Items.GOLDEN_PICKAXE), hammer),
            "Gear back into 1 of its material."));
        list.add(made(USING, socket, "Molten Sockets", HowToMake.table(atTable, new ItemStack(Items.DIAMOND_PICKAXE), socket),
            "Adds a gem socket to a pickaxe or axe.", "Try different gems."));

        // ── Recipes: Crafting ──
        addTools(list, iron, "Iron", Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE);
        addTools(list, diamond, "Diamond", Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE);
        ItemStack stick = new ItemStack(Items.STICK);
        list.add(made(CRAFTING, new ItemStack(Items.DIAMOND_AXE), "Iron & Diamond Axes", HowToMake.crafting(diamond, diamond, e, diamond, stick, e, e, stick, e),
            "Others: Compacted instead."));
        list.add(info(CRAFTING, new ItemStack(Items.NETHERITE_PICKAXE), "Netherite Tools", List.of(new ItemStack(Items.NETHERITE_PICKAXE),
                new ItemStack(Items.NETHERITE_SHOVEL), new ItemStack(Items.NETHERITE_HOE)),
            "Smithing Table. Toolsmiths only."));
        list.add(made(CRAFTING, hammer, "Salvage Hammer", HowToMake.crafting(iron, iron, iron, e, iron, e, e, iron, e),
            "Smiths only. Wears out."));

        // ── Information: Your Tools ──
        list.add(info(TOOLS, new ItemStack(Items.IRON_PICKAXE), "Efficiency", List.of(new ItemStack(Items.IRON_PICKAXE)),
            "Efficiency is how fast a tool mines.", "Tools you make come out faster."));
        list.add(info(TOOLS, new ItemStack(Items.STONE_PICKAXE), "Dull Pickaxes", List.of(new ItemStack(Items.IRON_PICKAXE), whetstone),
            "Mining wears a pickaxe down. A dull one mines at half speed.", "Sharpen it at a Pickaxe Whetstone."));

        // ── Information: Perks ──
        list.add(info(PERKS, new ItemStack(Items.NETHERITE_PICKAXE), "Toolsmith Only", List.of(new ItemStack(Items.NETHERITE_PICKAXE), whetstone),
            "Only you can make Netherite tools and Pickaxe Whetstones."));
        list.add(info(PERKS, diamond, "Cheaper Tools", List.of(iron, diamond),
            "You make tools with plain ingots and diamonds.", "Others need twice as much."));
        list.add(info(PERKS, table, "Repairs", List.of(table),
            "Only you can repair tools, at your Toolsmith Table."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, new ItemStack(Items.WOODEN_PICKAXE), "Slower Tools", List.of(),
            "Tools others make mine slower."));
        list.add(info(DEBUFFS, compactedIron, "Double Cost", List.of(),
            "Others need Compacted metal (twice as much)", "for iron and diamond tools."));
        list.add(info(DEBUFFS, new ItemStack(Items.IRON_PICKAXE), "No Repairs", List.of(),
            "Others can't repair tools."));
        list.add(info(DEBUFFS, new ItemStack(Items.NETHERITE_PICKAXE), "No Netherite", List.of(),
            "Others can't make Netherite Pickaxes, Shovels or Hoes."));
        list.add(info(DEBUFFS, table, "Table Locked", List.of(),
            "Others can't use the Toolsmith Table", "or make Pickaxe Whetstones."));
        return list;
    }
}

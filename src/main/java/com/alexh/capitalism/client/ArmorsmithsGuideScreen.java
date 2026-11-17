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

// The Armorsmith's Guide (Armorsmiths only, JobGuideScreen's layout in crimson and steel). Every Armorsmith Table recipe
// is in Forge (Blocks, Items, and Using the Table: repairs, salvaging, sockets). Recipes is crafting only; Information has
// no recipes. Keep every line short, and don't give away numbers or exactly what gems do: players find out by testing.
// Hand-written: keep it matching Armorsmith.md, Sockets.md, weaponsmith/Wear and the recipes.
public class ArmorsmithsGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF34262A, 0xFF1C1416, 0xFFC8506A, 0xFF5A1E2A, 0xFF140E10, 0xFF7A6A6E,
        0xFFE8708A, 0xFFF2E6E8, 0xFFB49CA2, 0xFFFF9AB0);

    private static final Tab BLOCKS = new Tab("Forge", "Blocks", new ItemStack(ModItems.ARMORSMITH_TABLE.get()));
    private static final Tab ITEMS = new Tab("Forge", "Items", new ItemStack(ModItems.COMPACTED_IRON.get()));
    private static final Tab USING = new Tab("Forge", "Using the Table", new ItemStack(ModItems.ARMORSMITH_TABLE.get()));
    private static final Tab CRAFTING = new Tab("Recipes", "Crafting", new ItemStack(Items.CRAFTING_TABLE));
    private static final Tab ARMOR = new Tab("Information", "Armor & Gems", new ItemStack(Items.DIAMOND_CHESTPLATE));
    private static final Tab PERKS = new Tab("Information", "Perks", new ItemStack(Items.IRON_HELMET));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public ArmorsmithsGuideScreen(@Nullable Screen parent) {
        super("Armorsmith's Guide", THEME, List.of(BLOCKS, ITEMS, USING, CRAFTING, ARMOR, PERKS, DEBUFFS), entries(), parent);
    }

    private static void addArmor(List<Entry> list, ItemStack m, String name, Item helmet, Item chest, Item legs, Item boots) {
        ItemStack e = ItemStack.EMPTY;
        String others = "Others: Compacted " + name + " instead.";
        list.add(made(CRAFTING, new ItemStack(helmet), name + " Helmet", HowToMake.crafting(m, m, m, m, e, m, e, e, e), others));
        list.add(made(CRAFTING, new ItemStack(chest), name + " Chestplate", HowToMake.crafting(m, e, m, m, m, m, m, m, m), others));
        list.add(made(CRAFTING, new ItemStack(legs), name + " Leggings", HowToMake.crafting(m, m, m, m, e, m, m, e, m), others));
        list.add(made(CRAFTING, new ItemStack(boots), name + " Boots", HowToMake.crafting(e, e, e, m, e, m, m, e, m), others));
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack table = new ItemStack(ModItems.ARMORSMITH_TABLE.get());
        ItemStack whetstone = new ItemStack(ModItems.ARMOR_WHETSTONE.get());
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack compactedIron = new ItemStack(ModItems.COMPACTED_IRON.get());
        ItemStack socket = new ItemStack(ModItems.MOLTEN_SOCKET.get());
        ItemStack hammer = new ItemStack(ModItems.SALVAGE_HAMMER.get());
        ItemStack nameTag = new ItemStack(Items.NAME_TAG);
        HowToMake whetstoneRecipe = HowToMake.table("At the Armorsmith Table.",
            new ItemStack(Items.GRINDSTONE), compactedIron);

        List<Entry> list = new ArrayList<>();

        // ── Forge: Blocks ──
        list.add(made(BLOCKS, table, "Armorsmith Table",
            HowToMake.crafting(iron, new ItemStack(Items.IRON_CHESTPLATE), iron, planks, new ItemStack(Items.SMITHING_TABLE), planks, planks, e, planks),
            "Armorsmiths only. Runs on Lava Buckets."));
        list.add(made(BLOCKS, whetstone, "Armor Whetstone", whetstoneRecipe,
            "Fixes dented armor. Needs a Water Bucket.", "Breaks after one use."));

        // ── Forge: Items ──
        list.add(made(ITEMS, compactedIron, "Compacted Iron", HowToMake.crafting(iron, iron, e, e, e, e, e, e, e),
            "Others need these for iron armor."));
        list.add(made(ITEMS, new ItemStack(ModItems.COMPACTED_DIAMOND.get()), "Compacted Diamond", HowToMake.crafting(diamond, diamond, e, e, e, e, e, e, e),
            "Others need these for diamond armor."));
        list.add(made(ITEMS, new ItemStack(ModItems.DIAMOND_REROLL_TOKEN.get()), "Reroll Tokens",
            HowToMake.table("At the Armorsmith Table.", nameTag, diamond),
            "Name Tag + Iron, Diamond or Netherite.", "Rerolls a sword or tool."));
        list.add(info(ITEMS, socket, "Molten Socket", List.of(), "Rare drop from hard mobs and bosses."));

        // ── Forge: Using the Table ──
        String atTable = "At the Armorsmith Table.";
        ItemStack damagedChest = new ItemStack(Items.IRON_CHESTPLATE);
        damagedChest.setDamageValue(damagedChest.getMaxDamage() / 2);
        list.add(made(USING, damagedChest, "Forging Repairs", HowToMake.table(atTable, damagedChest, iron),
            "Click the glowing dents before they cool.", "Costs some XP."));
        list.add(made(USING, hammer, "Salvaging", HowToMake.table(atTable, new ItemStack(Items.GOLDEN_CHESTPLATE), hammer),
            "Gear back into 1 of its material."));
        list.add(made(USING, socket, "Molten Sockets", HowToMake.table(atTable, new ItemStack(Items.DIAMOND_HELMET), socket),
            "Adds a gem socket. Try different gems."));

        // ── Recipes: Crafting ──
        addArmor(list, iron, "Iron", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        addArmor(list, diamond, "Diamond", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        list.add(made(CRAFTING, hammer, "Salvage Hammer", HowToMake.crafting(iron, iron, iron, e, iron, e, e, iron, e),
            "Smiths only. Wears out."));

        // ── Information: Armor & Gems ──
        list.add(info(ARMOR, new ItemStack(Items.IRON_CHESTPLATE), "Dented Armor", List.of(new ItemStack(Items.IRON_CHESTPLATE), whetstone),
            "Getting hit dents your armor, and dented armor protects less.", "Fix it at an Armor Whetstone."));
        list.add(info(ARMOR, new ItemStack(Items.LAPIS_LAZULI), "Gems", List.of(new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.REDSTONE),
                new ItemStack(Items.EMERALD), new ItemStack(Items.PRISMARINE_SHARD), new ItemStack(Items.ECHO_SHARD)),
            "Hold a gem and right-click armor that has a socket.", "Each gem does something different. Find out!"));

        // ── Information: Perks ──
        list.add(info(PERKS, whetstone, "Armorsmith Only", List.of(table, whetstone),
            "Only you can make Armor Whetstones and add sockets to armor."));
        list.add(info(PERKS, diamond, "Cheaper Armor", List.of(iron, diamond),
            "You make armor with plain ingots and diamonds.", "Others need twice as much."));
        list.add(info(PERKS, table, "Repairs", List.of(table),
            "Only you can repair armor, at your Armorsmith Table."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, compactedIron, "Double Cost", List.of(),
            "Others need Compacted metal (twice as much)", "for iron and diamond armor."));
        list.add(info(DEBUFFS, new ItemStack(Items.IRON_CHESTPLATE), "No Repairs", List.of(),
            "Others can't repair armor."));
        list.add(info(DEBUFFS, socket, "No Sockets", List.of(),
            "Others can't socket armor."));
        list.add(info(DEBUFFS, table, "Table Locked", List.of(),
            "Others can't use the Armorsmith Table", "or make Armor Whetstones."));
        return list;
    }
}

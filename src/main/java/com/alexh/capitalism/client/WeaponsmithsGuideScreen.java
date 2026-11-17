package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.weaponsmith.Sharpness;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// The Weaponsmith's Guide (Weaponsmiths only, JobGuideScreen's layout in dark gunmetal and ember orange):
//  - Forge: Blocks (Weaponsmith Table, Whetstone), Items (Compacted Diamond and Iron, Reroll Tokens, Molten Socket) and
//    Using the Table (repairs, rerolling, salvaging, sockets, shown like table recipes). Every Weaponsmith Table recipe
//    lives in Forge. Our things show as black silhouettes until you've made one.
//  - Recipes: Crafting only (what Weaponsmiths craft differently, the Salvage Hammer).
//  - Information: Sharpness and Perks, as clickable info boxes. No recipes here.
// Keep every line short, and no stat ranges, percentages or exact gem effects: players find those out by testing.
// Hand-written: keep it matching data/minecraft/recipe/, data/capitalism/recipe/, weaponsmith/WeaponsmithTableRecipes,
// weaponsmith/SwordForging and Weaponsmith.md.
public class WeaponsmithsGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF34302E, 0xFF1E1B1A, 0xFFE07A2A, 0xFF6A3010, 0xFF141212, 0xFF7A7270,
        0xFFFF9A3A, 0xFFF0E6DC, 0xFFB0A498, 0xFFFFB050);

    private static final Tab BLOCKS = new Tab("Forge", "Blocks", new ItemStack(ModItems.WEAPONSMITH_TABLE.get()));
    private static final Tab ITEMS = new Tab("Forge", "Items", new ItemStack(ModItems.COMPACTED_DIAMOND.get()));
    private static final Tab USING = new Tab("Forge", "Using the Table", new ItemStack(ModItems.WEAPONSMITH_TABLE.get()));
    private static final Tab CRAFTING = new Tab("Recipes", "Crafting", new ItemStack(Items.CRAFTING_TABLE));
    private static final Tab SHARPNESS = new Tab("Information", "Sharpness", new ItemStack(ModItems.WHETSTONE.get()));
    private static final Tab PERKS = new Tab("Information", "Perks", new ItemStack(Items.CROSSBOW));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public WeaponsmithsGuideScreen(@Nullable Screen parent) {
        super("Weaponsmith's Guide", THEME, List.of(BLOCKS, ITEMS, USING, CRAFTING, SHARPNESS, PERKS, DEBUFFS), buildEntries(), parent);
    }

    // "Iron Sword" + "Iron Axe" entries: Weaponsmiths use the plain material, everyone else the Compacted one.
    private static void addSwordAndAxe(List<Entry> list, ItemStack metal, String name, String plural, Item sword, Item axe, ItemStack stick) {
        ItemStack e = ItemStack.EMPTY;
        list.add(new Entry(CRAFTING, new ItemStack(sword), name + " Sword",
            HowToMake.crafting(e, metal, e, e, metal, e, e, stick, e),
            List.of(), List.of("Others: Compacted " + name + " instead.")));
        list.add(new Entry(CRAFTING, new ItemStack(axe), name + " Axe",
            HowToMake.crafting(metal, metal, e, metal, stick, e, e, stick, e),
            List.of(), List.of("Others: Compacted " + name + " instead.")));
    }

    private static List<Entry> buildEntries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack table = new ItemStack(ModItems.WEAPONSMITH_TABLE.get());
        ItemStack whetstone = new ItemStack(ModItems.WHETSTONE.get());
        ItemStack compacted = new ItemStack(ModItems.COMPACTED_DIAMOND.get());
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack dullSword = new ItemStack(Items.IRON_SWORD);
        dullSword.set(ModComponents.SWORD_HITS.get(), Sharpness.maxHits());
        ItemStack compactedIron = new ItemStack(ModItems.COMPACTED_IRON.get());
        HowToMake whetstoneRecipe = HowToMake.table("At the Weaponsmith Table.",
            new ItemStack(Items.GRINDSTONE), compactedIron);

        List<Entry> list = new ArrayList<>();

        // ── Forge: Blocks ──
        list.add(new Entry(BLOCKS, table, "Weaponsmith Table",
            HowToMake.crafting(iron, new ItemStack(Items.IRON_SWORD), iron, planks, new ItemStack(Items.SMITHING_TABLE), planks, planks, e, planks),
            List.of(), List.of("Weaponsmiths only. Runs on Lava Buckets.")));
        list.add(new Entry(BLOCKS, whetstone, "Sword Whetstone", whetstoneRecipe,
            List.of(), List.of("Sharpens a sword. Needs a Water Bucket.", "Breaks after one use.")));

        // ── Forge: Items ──
        ItemStack hammer = new ItemStack(ModItems.SALVAGE_HAMMER.get());
        ItemStack socket = new ItemStack(ModItems.MOLTEN_SOCKET.get());
        ItemStack token = new ItemStack(ModItems.DIAMOND_REROLL_TOKEN.get());
        list.add(new Entry(ITEMS, compacted, "Compacted Diamond",
            HowToMake.crafting(diamond, diamond, e, e, e, e, e, e, e),
            List.of(), List.of("Others need these for diamond weapons.")));
        list.add(new Entry(ITEMS, compactedIron, "Compacted Iron",
            HowToMake.crafting(iron, iron, e, e, e, e, e, e, e),
            List.of(), List.of("Others need these for iron weapons.")));
        list.add(new Entry(ITEMS, token, "Reroll Tokens",
            HowToMake.table("At the Weaponsmith Table.", new ItemStack(Items.NAME_TAG), diamond),
            List.of(), List.of("Name Tag + Iron, Diamond or Netherite.", "Rerolls a sword's stats.")));
        list.add(new Entry(ITEMS, socket, "Molten Socket", null, List.of(), List.of(
            "Rare drop from hard mobs and bosses.")));

        // ── Forge: Using the Table ──
        String atTable = "At the Weaponsmith Table.";
        ItemStack damagedSword = new ItemStack(Items.IRON_SWORD);
        damagedSword.setDamageValue(damagedSword.getMaxDamage() / 2);
        list.add(new Entry(USING, damagedSword, "Forging Repairs", HowToMake.table(atTable, damagedSword, iron), List.of(), List.of(
            "Click the glowing dents before they cool.", "Costs some XP.")));
        list.add(new Entry(USING, token, "Rerolling", HowToMake.table(atTable, new ItemStack(Items.DIAMOND_SWORD), token), List.of(), List.of(
            "New Sharpness and Edge.")));
        list.add(new Entry(USING, hammer, "Salvaging", HowToMake.table(atTable, new ItemStack(Items.GOLDEN_SWORD), hammer), List.of(), List.of(
            "Gear back into 1 of its material.")));
        list.add(new Entry(USING, socket, "Molten Sockets", HowToMake.table(atTable, new ItemStack(Items.DIAMOND_SWORD), socket), List.of(), List.of(
            "Adds a gem socket. Try different gems.")));

        // ── Recipes: Crafting ──
        addSwordAndAxe(list, iron, "Iron", "Iron Ingots", Items.IRON_SWORD, Items.IRON_AXE, stick);
        addSwordAndAxe(list, diamond, "Diamond", "Diamonds", Items.DIAMOND_SWORD, Items.DIAMOND_AXE, stick);
        list.add(new Entry(CRAFTING, new ItemStack(Items.CROSSBOW), "Crossbow",
            HowToMake.crafting(stick, iron, stick, new ItemStack(Items.STRING), new ItemStack(Items.TRIPWIRE_HOOK), new ItemStack(Items.STRING), e, stick, e),
            List.of(), List.of("Weaponsmiths only.")));
        list.add(new Entry(CRAFTING, new ItemStack(Items.ARROW, 4), "Arrows",
            HowToMake.crafting(e, new ItemStack(Items.FLINT), e, e, stick, e, e, new ItemStack(Items.FEATHER), e),
            List.of(), List.of("You: 4. Others: 2.")));
        list.add(new Entry(CRAFTING, hammer, "Salvage Hammer",
            HowToMake.crafting(iron, iron, iron, e, iron, e, e, iron, e),
            List.of(), List.of("Smiths only. Wears out.")));

        // ── Information: Sharpness ──
        list.add(new Entry(SHARPNESS, new ItemStack(Items.IRON_SWORD), "Forged Swords", null, List.of(new ItemStack(Items.IRON_SWORD)), List.of(
            "Sharpness is how hard a sword hits.", "Edge is how long it stays sharp.")));
        list.add(new Entry(SHARPNESS, dullSword, "Dull Swords", null, List.of(dullSword), List.of(
            "A sword with no edge left does half damage.", "Sharpen it at a Sword Whetstone.")));
        list.add(new Entry(SHARPNESS, whetstone, "Sharpening", null, List.of(whetstone, new ItemStack(Items.IRON_SWORD)), List.of(
            "Hold the sword and right-click the Whetstone,", "with a Water Bucket in your inventory.", "Each Whetstone sharpens once, then breaks.")));

        // ── Information: Perks ──
        list.add(new Entry(PERKS, new ItemStack(Items.CROSSBOW), "Weaponsmith Only", null,
            List.of(new ItemStack(Items.CROSSBOW), new ItemStack(Items.NETHERITE_SWORD), whetstone), List.of(
            "Only you can make Crossbows, Netherite Swords", "and Sword Whetstones.")));
        list.add(new Entry(PERKS, new ItemStack(Items.DIAMOND_SWORD), "Better Swords", null, List.of(new ItemStack(Items.DIAMOND_SWORD)), List.of(
            "Swords you make hit harder and stay sharp longer.")));
        list.add(new Entry(PERKS, diamond, "Cheaper Weapons", null, List.of(iron, diamond), List.of(
            "You make weapons with plain ingots and diamonds.", "Others need twice as much.")));
        list.add(new Entry(PERKS, table, "Repairs", null, List.of(table), List.of(
            "Only you can repair weapons, at your Weaponsmith Table.")));
        list.add(new Entry(PERKS, new ItemStack(Items.ARROW), "More Arrows", null, List.of(new ItemStack(Items.ARROW, 4)), List.of(
            "You get twice the arrows from each craft.")));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(Entry.info(DEBUFFS, new ItemStack(Items.WOODEN_SWORD), "Weaker Swords", List.of(),
            "Swords others make hit softer and go dull sooner."));
        list.add(Entry.info(DEBUFFS, compactedIron, "Double Cost", List.of(),
            "Others need Compacted metal (twice as much)", "for iron and diamond weapons."));
        list.add(Entry.info(DEBUFFS, new ItemStack(Items.IRON_SWORD), "No Repairs", List.of(),
            "Others can't repair weapons."));
        list.add(Entry.info(DEBUFFS, new ItemStack(Items.CROSSBOW), "No Crossbows", List.of(),
            "Others can't make Crossbows or Netherite Swords."));
        list.add(Entry.info(DEBUFFS, new ItemStack(Items.ARROW), "Fewer Arrows", List.of(),
            "Others get half the arrows from each craft."));
        list.add(Entry.info(DEBUFFS, table, "Table Locked", List.of(),
            "Others can't use the Weaponsmith Table", "or make Sword Whetstones."));
        return list;
    }

}

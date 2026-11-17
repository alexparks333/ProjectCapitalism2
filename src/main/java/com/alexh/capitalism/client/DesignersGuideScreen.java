package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.designer.Bag;
import com.alexh.capitalism.designer.Pattern;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.JobGuideScreen.Entry.info;
import static com.alexh.capitalism.client.JobGuideScreen.Entry.made;

// The Designer's Guide (Designers only, JobGuideScreen's layout in plum velvet and gold):
//  - Workshop: Blocks (Designer Table, Sewing Machine), Items (the needle, the Patches: what each is sewn from), Bags
//    (what each takes). Every recipe lives here.
//  - Information: Sewing (the needle game), Bags (wearing one, what spills) and Player Debuffs, as clickable info boxes.
// Keep every line short, and no numbers (slots, needle wear): players find those out. Hand-written: keep it matching
// Designer.md and designer/Bag, designer/Pattern.
public class DesignersGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF3A1E30, 0xFF22101C, 0xFFE8C062, 0xFF6A4420, 0xFF180A14, 0xFF6E4A60,
        0xFFF6DC8E, 0xFFF4E8EE, 0xFFC0A8B6, 0xFFE8D08A);

    private static final Tab BLOCKS = new Tab("Workshop", "Blocks", new ItemStack(ModItems.SEWING_MACHINE.get()));
    private static final Tab ITEMS = new Tab("Workshop", "Items", new ItemStack(ModItems.LEATHER_PATCH.get()));
    private static final Tab BAGS = new Tab("Workshop", "Bags", new ItemStack(ModItems.BACKPACK.get()));
    private static final Tab HATS = new Tab("Workshop", "Pet Hats", new ItemStack(ModItems.GUCCI_TOP_HAT.get()));
    private static final Tab SEWING = new Tab("Information", "Sewing", new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get()));
    private static final Tab WEARING = new Tab("Information", "Wearing Bags", new ItemStack(ModItems.SEED_SATCHEL.get()));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public DesignersGuideScreen(@Nullable Screen parent) {
        super("Designer's Guide", THEME, List.of(BLOCKS, ITEMS, BAGS, HATS, SEWING, WEARING, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack string = new ItemStack(Items.STRING);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack slab = new ItemStack(Items.OAK_SLAB);
        ItemStack table = new ItemStack(ModItems.DESIGNER_TABLE.get());
        ItemStack machine = new ItemStack(ModItems.SEWING_MACHINE.get());
        ItemStack needle = new ItemStack(ModItems.DIAMOND_SEWING_NEEDLE.get());

        List<Entry> list = new ArrayList<>();

        // ── Workshop: Blocks ──
        list.add(made(BLOCKS, table, "Designer Table",
            HowToMake.crafting(leather, new ItemStack(Items.SHEARS), leather, planks, new ItemStack(Items.CRAFTING_TABLE), planks, planks, e, planks),
            "Pick a bag, lay its Patches in a row,", "add Leather and needles, then Stitch.", "Stitching uses up needles."));
        list.add(made(BLOCKS, machine, "Sewing Machine",
            HowToMake.crafting(iron, iron, iron, iron, new ItemStack(Items.REDSTONE), string, slab, slab, slab),
            "Pick a pattern, then sew its materials", "on one at a time."));

        // ── Workshop: Items ──
        ItemStack ironNeedle = new ItemStack(ModItems.IRON_SEWING_NEEDLE.get());
        list.add(made(ITEMS, ironNeedle, "Iron Sewing Needle",
            HowToMake.crafting(e, e, iron, e, new ItemStack(Items.STICK), e, string, e, e),
            "For Generic patches,", "and stitching Standard bags.", "Each slip breaks one."));
        list.add(made(ITEMS, needle, "Diamond Sewing Needle",
            HowToMake.crafting(e, e, new ItemStack(Items.DIAMOND), e, new ItemStack(Items.STICK), e, string, e, e),
            "For Fancy, Luxury and Utility patches,", "and stitching every bag but Standard.", "Each slip breaks one."));
        for (Pattern pattern : Pattern.values()) {
            ItemStack[] materials = pattern.stitches.stream().map(Pattern.Stitch::stack).toArray(ItemStack[]::new);
            list.add(made(ITEMS, new ItemStack(pattern.patch()), new ItemStack(pattern.patch()).getHoverName().getString(),
                HowToMake.steps("Sewn on in this order at the Sewing Machine.", materials),
                pattern.tier.title + " patch."));
        }

        // ── Workshop: Bags ──
        for (Bag bag : Bag.values()) {
            if (bag == Bag.TRAVELERS_BAG) continue;   // not a design: only the Traveling Merchant sells it
            ItemStack[] patches = bag.patches().stream().map(ItemStack::new).toArray(ItemStack[]::new);
            String holds = String.join(", ", bag.groups.stream().map(g -> g.holds().title.toLowerCase(java.util.Locale.ROOT)).toList());
            list.add(made(BAGS, new ItemStack(bag.item()), bag.title,
                HowToMake.steps("These Patches and a Leather, at the Designer Table.", patches),
                bag.line.title + ". Holds: " + holds + ".", "Worn on your back."));
        }

        // ── Workshop: Hats ──
        for (com.alexh.capitalism.designer.Hat hat : com.alexh.capitalism.designer.Hat.values()) {
            if (hat == com.alexh.capitalism.designer.Hat.FEEDING_HAT) continue;   // not a design: only the Traveling Merchant sells it
            ItemStack[] patches = hat.patches().stream().map(ItemStack::new).toArray(ItemStack[]::new);
            list.add(made(HATS, new ItemStack(hat.item()), hat.title,
                HowToMake.steps("These Patches and a Leather, at the Designer Table.", patches),
                "For a pet: " + hat.does.toLowerCase(java.util.Locale.ROOT) + ".", "Put it on in the pet's window (Actions)."));
        }

        // the Utility bag that does something
        list.add(info(WEARING, new ItemStack(ModItems.FREEZER_SLING.get()), "The Freezer Sling", List.of(new ItemStack(ModItems.FREEZER_SLING.get())),
            "Holds food and meat, and keeps it", "like a Freezer: meat rots much", "slower in it. No fuel needed."));
        // ── Information: Sewing ──
        list.add(info(SEWING, machine, "1. Pick a Pattern", List.of(new ItemStack(ModItems.LEATHER_PATCH.get())),
            "Click a pattern at the Sewing Machine.", "Its materials show faintly in the slots below."));
        list.add(info(SEWING, needle, "2. Sew", List.of(needle),
            "Press Sew. The seam comes down the cloth.", "Move the mouse left and right only.", "Keep the needle inside the chalk lines."));
        list.add(info(SEWING, new ItemStack(Items.ICE), "Icy Seams", List.of(new ItemStack(Items.ICE), new ItemStack(Items.SNOWBALL)),
            "Ice cracks in sharp zig-zags, and", "where it freezes the seam pinches", "narrow. Watch for the blue."));
        list.add(info(SEWING, new ItemStack(Items.STRING), "Slips", List.of(),
            "Touching a chalk line breaks a needle.", "Run out of needles and", "that material is lost."));
        list.add(info(SEWING, new ItemStack(ModItems.GUCCI_PATCH.get()), "Tiers", List.of(new ItemStack(ModItems.LEATHER_PATCH.get()), new ItemStack(ModItems.VELVET_PATCH.get()), new ItemStack(ModItems.GUCCI_PATCH.get())),
            "Generic patches are easy, Fancy ones", "harder, Luxury ones hardest.", "Generic takes Iron needles,", "Fancy and Luxury take Diamond."));
        list.add(info(SEWING, new ItemStack(Items.STRING), "Every Material Sews Its Own Way", List.of(new ItemStack(Items.STRING), leather, iron, new ItemStack(Items.DIAMOND)),
            "String sways side to side, leather wanders,", "iron jumps in sharp turns,", "diamond twitches every which way."));
        list.add(info(SEWING, table, "3. Stitch the Bag", List.of(table),
            "Lay the Patches in the row in order,", "add Leather and needles, press Stitch.", "Standard bags use Iron needles,", "the rest use Diamond ones."));

        // ── Information: Wearing Bags ──
        list.add(info(WEARING, new ItemStack(ModItems.BACKPACK.get()), "Your Back", List.of(new ItemStack(ModItems.BACKPACK.get())),
            "Wear a bag in the Back slot.", "Its slots hang beside your inventory."));
        list.add(info(WEARING, new ItemStack(ModItems.FOOD_SATCHEL.get()), "Picky Bags", List.of(new ItemStack(Items.APPLE), new ItemStack(Items.WHEAT_SEEDS)),
            "Satchels only take what they're for."));
        list.add(info(WEARING, new ItemStack(Items.CHEST), "Taking It Off", List.of(),
            "A bag only holds things while it's worn.", "Take it off and everything spills out."));
        list.add(info(WEARING, new ItemStack(Items.SKELETON_SKULL), "Dying", List.of(),
            "What was in the bag drops,", "but the bag itself is destroyed."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, machine, "No Sewing", List.of(),
            "Others can't make or use the Sewing Machine", "or the Designer Table."));
        list.add(info(DEBUFFS, needle, "No Needles", List.of(),
            "Others can't make Sewing Needles."));
        list.add(info(DEBUFFS, new ItemStack(ModItems.BACKPACK.get()), "No Bags", List.of(),
            "Others can't make bags.", "Anyone can still wear one."));
        return list;
    }
}

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

// The Librarian's Guide (Librarians only, JobGuideScreen's layout in dark oak and library green):
//  - Workshop: Blocks (Stitching Table, Librarian Table), Items (Rough / Delicate Book, Knowledge Piece, Skill Book)
//    and Using the Table. Every recipe lives here.
//  - Information: Books (enchanting and combining them), Reading, and Player Debuffs, as clickable info boxes.
// Keep every line short, and no numbers: players find those out (what each book teaches, how long, the odds).
// Hand-written: keep it matching Librarian.md and the enchanter package (EnchantingLimit, AnvilEnchanting).
public class LibrariansGuideScreen extends JobGuideScreen {

    private static final Theme THEME = new Theme(0xFF2E2A22, 0xFF1A1712, 0xFF6FBF8A, 0xFF2A5A3A, 0xFF14120E, 0xFF6E6A5E,
        0xFFA8E0B8, 0xFFF2EAD6, 0xFFB8AE98, 0xFFE8D08A);

    private static final Tab BLOCKS = new Tab("Workshop", "Blocks", new ItemStack(ModItems.LIBRARIAN_TABLE.get()));
    private static final Tab ITEMS = new Tab("Workshop", "Items", new ItemStack(ModItems.ROUGH_BOOK.get()));
    private static final Tab BOOKS = new Tab("Information", "Books", new ItemStack(Items.ENCHANTED_BOOK));
    private static final Tab READING = new Tab("Information", "Reading", new ItemStack(ModItems.DELICATE_BOOK.get()));
    private static final Tab DEBUFFS = new Tab("Information", "Player Debuffs", new ItemStack(Items.BARRIER));

    public LibrariansGuideScreen(@Nullable Screen parent) {
        super("Librarian's Guide", THEME, List.of(BLOCKS, ITEMS, BOOKS, READING, DEBUFFS), entries(), parent);
    }

    private static List<Entry> entries() {
        ItemStack table = new ItemStack(Items.ENCHANTING_TABLE);
        ItemStack anvil = new ItemStack(Items.ANVIL);
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        ItemStack stitching = new ItemStack(ModItems.STITCHING_TABLE.get());
        ItemStack librarianTable = new ItemStack(ModItems.LIBRARIAN_TABLE.get());
        ItemStack rough = new ItemStack(ModItems.ROUGH_BOOK.get());
        ItemStack delicate = new ItemStack(ModItems.DELICATE_BOOK.get());
        ItemStack piece = new ItemStack(ModItems.KNOWLEDGE_PIECE.get());
        ItemStack skillBook = new ItemStack(ModItems.SKILL_BOOK.get());
        ItemStack creator = new ItemStack(ModItems.KNOWLEDGE_CREATOR.get());
        ItemStack paper = new ItemStack(Items.PAPER);
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack hide = new ItemStack(Items.RABBIT_HIDE);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        // the four Knowledge Piece qualities, shown in turn
        List<ItemStack> pieces = new java.util.ArrayList<>();
        for (com.alexh.capitalism.librarian.KnowledgeQuality quality : com.alexh.capitalism.librarian.KnowledgeQuality.values()) pieces.add(quality.stack(1));

        List<Entry> list = new ArrayList<>();

        // ── Workshop: Blocks ──
        list.add(made(BLOCKS, stitching, "Stitching Table",
            HowToMake.crafting(new ItemStack(Items.STRING), new ItemStack(Items.SHEARS), new ItemStack(Items.STRING),
                leather, new ItemStack(Items.CRAFTING_TABLE), leather, planks, planks, planks),
            "Sews blank books. Slowly.", "Librarians only."));
        list.add(made(BLOCKS, creator, "Knowledge Creator",
            HowToMake.crafting(book, new ItemStack(Items.FEATHER), book, planks, planks, planks,
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)),
            "Turns Enchanted Books into Knowledge Pieces.", "Librarians only."));
        list.add(made(BLOCKS, librarianTable, "Librarian Table",
            HowToMake.crafting(new ItemStack(Items.FEATHER), book, new ItemStack(Items.INK_SAC),
                new ItemStack(Items.BOOKSHELF), new ItemStack(Items.LECTERN), new ItemStack(Items.BOOKSHELF), planks, planks, planks),
            "Writes skill books.", "Librarians only."));

        // ── Workshop: Items ──
        list.add(made(ITEMS, rough, "Rough Book",
            HowToMake.stitching("At the Stitching Table, with Iron thread.", List.of(leather, hide), paper, List.of(iron), List.of(rough)),
            "A plain blank book."));
        list.add(made(ITEMS, delicate, "Delicate Book",
            HowToMake.stitching("At the Stitching Table, with Diamond thread.", List.of(leather, hide), paper, List.of(diamond), List.of(delicate)),
            "A fine blank book. Better books."));
        list.add(made(ITEMS, piece, "Knowledge Piece",
            HowToMake.steps("Made in the Knowledge Creator from an Enchanted Book. The Whisperer sells some too.", enchantedBook, creator),
            "What a book is written from.", "Faded, Clear, Bright or Radiant."));
        list.add(made(ITEMS, skillBook, "Skill Book",
            HowToMake.table("At the Librarian Table.", List.of(rough, delicate), pieces, List.of(skillBook)),
            "Anyone can read it.", "You never know what you'll write."));

        // ── Information: Books ──
        list.add(info(BOOKS, table, "Enchanting Books", List.of(book, table),
            "Only you can enchant books.", "Not even Enchanters can."));
        list.add(info(BOOKS, anvil, "Combining Books", List.of(enchantedBook, anvil),
            "Only you can combine Enchanted Books at an Anvil.", "Putting them onto gear is the Enchanter's job."));

        // ── Information: Reading ──
        list.add(info(READING, skillBook, "Reading", List.of(skillBook),
            "Hold right-click to read a Skill Book.", "Most teach you something for a while."));
        list.add(info(READING, new ItemStack(Items.ENCHANTED_BOOK), "Rare Books", List.of(),
            "A shimmering book is Persistent.", "What it teaches stays with you", "until you die."));
        list.add(info(READING, new ItemStack(Items.REDSTONE), "Blood Moon Books", List.of(),
            "A book with a crimson spine is a", "Blood Moon Book: read it before", "the moon turns red. It lasts", "one night."));

        // ── Information: Player Debuffs (what everyone without the job can't do) ──
        list.add(info(DEBUFFS, table, "No Book Enchanting", List.of(),
            "Others can't enchant books, not even Enchanters."));
        list.add(info(DEBUFFS, anvil, "No Combining", List.of(),
            "Others can't combine Enchanted Books."));
        list.add(info(DEBUFFS, stitching, "No Sewing", List.of(),
            "Others can't use a Stitching Table."));
        list.add(info(DEBUFFS, creator, "No Knowledge", List.of(),
            "Others can't use a Knowledge Creator.", "Others can buy Knowledge Pieces from the Whisperer."));
        list.add(info(DEBUFFS, librarianTable, "No Writing", List.of(),
            "Others can't write Skill Books at a Librarian Table.", "Anyone can read them."));
        return list;
    }
}

package com.alexh.capitalism.client;

import com.alexh.capitalism.block.BartenderRecipe;
import com.alexh.capitalism.block.JuiceType;
import com.alexh.capitalism.block.LiquorType;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.GoldenBonus;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.guide.ClientDiscoveries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// The Bartender's Guide, in two sections:
//  - Recipes: a recipe viewer like the vanilla recipe book (tabs: Juicer, Fermenter, Ingredients, Drinks, and
//    Specialty Drinks for the ones with effects; icon grid, click for the recipe).
//  - Blocks: the Bartender's machines, how to craft them and what they do (black silhouettes until crafted).
//    Built from JuiceType, LiquorType, BartenderRecipe and GoldenBonus, so it's always current.
//    Things you haven't made yet are black silhouettes named "???" (the recipe still shows); making one once
//    unlocks it (guide/Discoveries).
//  - Information: a short step-by-step tutorial for new bartenders (INFO_PAGES below).
public class BartendersGuideScreen extends Screen {

    private static final int W = 280;
    private static final int H = 200;
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int CELL = 20;

    // Layout (relative to the panel's top-left)
    private static final int SECTION_Y = 18;
    private static final int MACHINE_TAB_Y = 36;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 62;
    private static final int DETAIL_X = 116;
    private static final int DETAIL_Y = 62;

    // Colors (dark mahogany bar, brass trim)
    private static final int PANEL = 0xFF5A3420;
    private static final int PANEL_DARK = 0xFF3A1E10;
    private static final int TRIM_LIGHT = 0xFFD4A040;
    private static final int TRIM_DARK = 0xFF7A5418;
    private static final int SLOT = 0xFF2A140A;
    private static final int SLOT_EDGE = 0xFF9A6A44;
    private static final int SELECTED = 0xFFE8C060;
    private static final int TEXT = 0xFFF2E2C0;
    private static final int TEXT_DIM = 0xFFBFA888;
    private static final int GOLD = 0xFFFFD24A;
    private static final int WATER = 0xFF7CC8FF;
    private static final int LOCKED_SLOT = 0xFF8A5E3E;

    private enum Section {
        BAR("Bar"), RECIPES("Recipes"), INFORMATION("Information");

        final String title;

        Section(String title) {
            this.title = title;
        }
    }

    private enum Machine {
        JUICER("Juicer"), FERMENTER("Fermenter"), BARTENDING_TABLE("Bartending Table");

        final String title;

        Machine(String title) {
            this.title = title;
        }

        ItemStack icon() {
            return new ItemStack(switch (this) {
                case JUICER -> ModItems.JUICER.get();
                case FERMENTER -> ModItems.FERMENTER.get();
                case BARTENDING_TABLE -> ModItems.BARTENDING_TABLE.get();
            });
        }
    }

    // Tabs. Bar: Blocks (the machines) and Ingredients (everything you make to go INTO drinks: juice and liquor
    // buckets, shots, juices, Simple Syrup, Bitters). Recipes: Drinks (plain) and Specialty Drinks (with effects).
    private enum Tab {
        BLOCKS("Blocks", Section.BAR), ACCESSORIES("Ingredients", Section.BAR),
        DRINKS("Drinks", Section.RECIPES), SPECIALTY("Specialty Drinks", Section.RECIPES),
        TOPICS("Bartending", Section.INFORMATION), DEBUFFS("Player Debuffs", Section.INFORMATION);

        final String title;
        final Section section;

        Tab(String title, Section section) {
            this.title = title;
            this.section = section;
        }

        static List<Tab> in(Section section) {
            return java.util.Arrays.stream(values()).filter(tab -> tab.section == section).toList();
        }

        ItemStack icon() {
            return new ItemStack(switch (this) {
                case BLOCKS -> ModItems.JUICER.get();
                case ACCESSORIES -> ModItems.SHOTS.get(LiquorType.TEQUILA).get();
                case DRINKS -> ModItems.CLASSIC_OLD_FASHIONED.get();
                case SPECIALTY -> ModItems.GLOWBERRY_GOLD_RUSH.get();
                case TOPICS -> Items.GLASS_BOTTLE;
                case DEBUFFS -> Items.BARRIER;
            });
        }
    }

    // One recipe as shown on the right: ingredients (with counts), optional fuel/glass, result, time, drops.
    private record Entry(Tab tab, Machine machine, ItemStack result, List<ItemStack> ingredients, @Nullable ItemStack extra,
                         @Nullable String extraLabel, List<String> info, @Nullable GoldenBonus bonus) {
    }

    // A Bartender machine: how to craft it and a few lines about it (shown once you've crafted one).
    private record BlockEntry(ItemStack stack, HowToMake how, List<String> lines) {
    }

    // One Information topic: a clickable box with its title, and a line or two (text only: pictures of drinks would
    // give away the ones you haven't made yet).
    private record Topic(Tab tab, String title, List<String> lines) {
    }

    private final List<Entry> entries = buildEntries();
    private final List<Topic> topics = buildTopics();
    private final List<BlockEntry> blocks = buildBlocks();
    private @Nullable BlockEntry selectedBlock;
    private Section section = Section.BAR;
    // the open tab in each section
    private Tab barTab = Tab.BLOCKS;
    private Tab recipeTab = Tab.DRINKS;
    private Tab infoTab = Tab.TOPICS;
    private int page = 0;
    private @Nullable Topic selectedTopic;
    private @Nullable Entry selected;

    private int left;
    private int top;
    private @Nullable ItemStack hovered;
    private boolean hoveredLocked;
    // Where Esc goes back to (the /guides screen), or null to just close.
    private final @Nullable Screen parent;

    public BartendersGuideScreen() {
        this(null);
    }

    public BartendersGuideScreen(@Nullable Screen parent) {
        super(Component.literal("Bartender's Guide"));
        this.parent = parent;
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new BartendersGuideScreen());
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.gui.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
        if (this.selected == null) {
            this.selected = firstOf(tab());
        }
        if (this.selectedBlock == null) this.selectedBlock = this.blocks.get(0);
        if (this.selectedTopic == null && !topicsIn(this.infoTab).isEmpty()) this.selectedTopic = topicsIn(this.infoTab).get(0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Recipes ────────────────────────────────────────────────────────────

    private static List<Entry> buildEntries() {
        List<Entry> list = new ArrayList<>();
        String juiceTime = time(CapitalismConfig.JUICER_TICKS, 600);
        for (JuiceType juice : JuiceType.values()) {
            list.add(new Entry(Tab.ACCESSORIES, Machine.JUICER, new ItemStack(juice.result()),
                List.of(new ItemStack(juice.fruit(), 6), new ItemStack(Items.BUCKET)),
                null, null, List.of("Time: " + juiceTime), null));
        }
        String fermentTime = time(CapitalismConfig.FERMENTER_TICKS, 12000);
        String sugarTime = time(CapitalismConfig.FERMENTER_SUGAR_TICKS, 1800);
        for (LiquorType liquor : LiquorType.values()) {
            list.add(new Entry(Tab.ACCESSORIES, Machine.FERMENTER, new ItemStack(liquor.result()),
                List.of(new ItemStack(liquor.ingredient(), 6), new ItemStack(Items.BUCKET)),
                new ItemStack(Items.SUGAR), "Fuel", List.of("Time: " + fermentTime, "1 sugar per " + sugarTime), null));
        }
        for (BartenderRecipe recipe : BartenderRecipe.RECIPES) {
            Map<Item, Integer> counts = new LinkedHashMap<>();
            for (Supplier<? extends Item> ingredient : recipe.ingredients()) {
                counts.merge(ingredient.get(), 1, Integer::sum);
            }
            List<ItemStack> ingredients = new ArrayList<>();
            counts.forEach((item, n) -> ingredients.add(new ItemStack(item, n)));
            ItemStack glass = recipe.bottles() > 0 ? new ItemStack(Items.GLASS_BOTTLE, recipe.bottles()) : null;
            ItemStack result = new ItemStack(recipe.result().get(), recipe.count());
            // Drinks with effects go in Specialty Drinks, plain ones in Drinks
            Tab tab = recipe.kind() != BartenderRecipe.Kind.DRINK ? Tab.ACCESSORIES
                : CooksGuideScreen.hasEffects(result) ? Tab.SPECIALTY : Tab.DRINKS;
            list.add(new Entry(tab, Machine.BARTENDING_TABLE, result, ingredients, glass,
                glass == null ? null : "Glass Bottles", List.of(), GoldenBonus.of(result)));
        }
        return list;
    }

    // The open tab of the open section.
    private Tab tab() {
        return switch (this.section) {
            case RECIPES -> this.recipeTab;
            case INFORMATION -> this.infoTab;
            case BAR -> this.barTab;
        };
    }

    private void setTab(Tab tab) {
        this.section = tab.section;
        switch (tab.section) {
            case RECIPES -> this.recipeTab = tab;
            case INFORMATION -> {
                this.infoTab = tab;
                this.selectedTopic = topicsIn(tab).isEmpty() ? null : topicsIn(tab).get(0);
            }
            case BAR -> this.barTab = tab;
        }
    }

    private List<Topic> topicsIn(Tab tab) {
        return this.topics.stream().filter(topic -> topic.tab() == tab).toList();
    }

    private List<Entry> inTab(Tab tab) {
        return this.entries.stream().filter(e -> e.tab() == tab).toList();
    }

    private @Nullable Entry firstOf(Tab tab) {
        List<Entry> list = inTab(tab);
        return list.isEmpty() ? null : list.get(0);
    }

    // The recipe that makes this item, if the guide has one (so clicking an ingredient can jump to it).
    private @Nullable Entry recipeFor(ItemStack stack) {
        for (Entry entry : this.entries) {
            if (ItemStack.isSameItem(entry.result(), stack)) return entry;
        }
        return null;
    }

    private int pageCount() {
        return Math.max(1, (inTab(tab()).size() + COLS * ROWS - 1) / (COLS * ROWS));
    }

    // ── Blocks ─────────────────────────────────────────────────────────────
    // Crafting grids match data/capitalism/recipe/ (left to right, top to bottom).

    private static List<BlockEntry> buildBlocks() {
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack glass = new ItemStack(Items.GLASS);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack wool = new ItemStack(Items.WOOL.black());
        List<BlockEntry> list = new ArrayList<>();
        list.add(new BlockEntry(new ItemStack(ModItems.JUICER.get()),
            HowToMake.crafting(copper, new ItemStack(Items.HOPPER), copper, planks, new ItemStack(Items.GRINDSTONE), planks, copper, planks, copper),
            List.of("6 fruit + a bucket = a bucket of juice.", "Anyone can use it.")));
        list.add(new BlockEntry(new ItemStack(ModItems.FERMENTER.get()),
            HowToMake.crafting(glass, glass, glass, iron, new ItemStack(Items.BARREL), iron, planks, planks, planks),
            List.of("6 crops + sugar + a bucket = liquor.", "2 blocks tall. Bartenders only.")));
        list.add(new BlockEntry(new ItemStack(ModItems.BARTENDING_TABLE.get()),
            HowToMake.crafting(wool, wool, wool, iron, iron, iron, planks, planks, planks),
            List.of("Pour shots and mix drinks", "(glass bottles on the right).", "Bartenders only.")));
        ItemStack charcoal = new ItemStack(Items.CHARCOAL);
        ItemStack paper = new ItemStack(Items.PAPER);
        list.add(new BlockEntry(new ItemStack(ModItems.OSMOSIS_MACHINE.get()),
            HowToMake.crafting(iron, glass, iron, glass, new ItemStack(Items.CAULDRON), glass, iron, new ItemStack(Items.BUCKET), iron),
            List.of("A filter, then a Purified Water bucket:", "Spring Water. Bartenders only.")));
        list.add(new BlockEntry(new ItemStack(ModItems.OSMOSIS_FILTER.get()),
            filterGrid(List.of(charcoal, new ItemStack(Items.PRISMARINE_SHARD)), paper),
            List.of("Lasts 2 buckets.")));
        ItemStack crystals = new ItemStack(Items.PRISMARINE_CRYSTALS);
        list.add(new BlockEntry(new ItemStack(ModItems.ADVANCED_OSMOSIS_FILTER.get()),
            HowToMake.crafting(crystals, crystals, crystals, paper, paper, paper, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
            List.of("Lasts 4 buckets.")));
        return list;
    }

    // top row cycles through the filter materials, paper below
    private static HowToMake filterGrid(List<ItemStack> top, ItemStack paper) {
        List<ItemStack> p = List.of(paper);
        return HowToMake.crafting(top, top, top, p, p, p, List.of(), List.of(), List.of());
    }

    // ── Information ────────────────────────────────────────────────────────
    // A line or two per topic. No times, numbers or effects: players find those out.

    private static List<Topic> buildTopics() {
        List<Topic> list = new ArrayList<>();
        list.add(new Topic(Tab.TOPICS, "Your Machines", List.of("Juice the fruit, ferment the juice,", "then mix the drink.")));
        list.add(new Topic(Tab.TOPICS, "Glass Bottles", List.of("Every drink needs a glass bottle.", "You get the bottle back after drinking.")));
        list.add(new Topic(Tab.TOPICS, "Golden Drops", List.of("Your drinks can fill the water bar", "past full, as golden drops.")));
        list.add(new Topic(Tab.TOPICS, "Hoppers", List.of("Hoppers can put things into your machines", "and take them out.")));
        list.add(new Topic(Tab.DEBUFFS, "Locked Machines", List.of("Others can't use the Fermenter", "or the Bartending Table.")));
        return list;
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.hovered = null;
        this.hoveredLocked = false;
        int l = this.left;
        int t = this.top;

        // Panel with brass trim
        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + W, t + H, PANEL);
        graphics.centeredText(this.font, this.title, l + W / 2, t + 6, GOLD);

        // Section buttons: Bar | Recipes | Information
        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            int x = l + 8 + i * 76;
            boolean active = sections[i] == this.section;
            graphics.fill(x, t + SECTION_Y, x + 72, t + SECTION_Y + 14, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, t + SECTION_Y + 1, x + 71, t + SECTION_Y + 13, active ? PANEL_DARK : SLOT);
            graphics.centeredText(this.font, sections[i].title, x + 36, t + SECTION_Y + 3, active ? GOLD : TEXT_DIM);
        }

        drawTabs(graphics, mouseX, mouseY);
        if (this.section == Section.INFORMATION) drawInformation(graphics, mouseX, mouseY);
        else if (tab() == Tab.BLOCKS) drawBlocks(graphics, mouseX, mouseY);
        else drawRecipes(graphics, mouseX, mouseY);

        if (this.hoveredLocked) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal("???"),
                Component.literal("Make it once to unlock").withStyle(s -> s.withColor(TEXT_DIM))), mouseX, mouseY, ItemStack.EMPTY);
        } else if (this.hovered != null && !this.hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, this.hovered, mouseX, mouseY);
        }
    }

    // The open section's tabs (icons) and the open tab's name.
    private void drawTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;
        List<Tab> tabs = Tab.in(this.section);
        for (int i = 0; i < tabs.size(); i++) {
            int x = l + 8 + i * 30;
            int y = t + MACHINE_TAB_Y;
            boolean active = tabs.get(i) == tab();
            graphics.fill(x, y, x + 26, y + 22, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + 25, y + 21, active ? PANEL_DARK : SLOT);
            graphics.item(tabs.get(i).icon(), x + 5, y + 3);
            if (inside(mouseX, mouseY, x, y, 26, 22)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(tabs.get(i).title)), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        int tx = l + 8 + tabs.size() * 30 + 6;
        graphics.text(this.font, tab().title, tx, t + MACHINE_TAB_Y + (tab() == Tab.BLOCKS ? 2 : 7), TEXT, false);
        if (tab() == Tab.BLOCKS) graphics.text(this.font, "Craft one to unlock it!", tx, t + MACHINE_TAB_Y + 12, TEXT_DIM, false);
    }

    private void drawRecipes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;

        // Grid of things to make
        List<Entry> list = inTab(tab());
        int start = this.page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + GRID_X + (i % COLS) * CELL;
            int y = t + GRID_Y + (i / COLS) * CELL;
            int index = start + i;
            Entry entry = index < list.size() ? list.get(index) : null;
            if (entry == null) {
                slot(graphics, x, y, false);
            } else {
                lockedSlot(graphics, entry.result(), x, y, entry == this.selected, mouseX, mouseY);
            }
        }
        if (pageCount() > 1) {
            int ny = t + GRID_Y + ROWS * CELL + 2;
            graphics.text(this.font, "<", l + GRID_X + 2, ny, this.page > 0 ? TEXT : TEXT_DIM, false);
            graphics.centeredText(this.font, (this.page + 1) + "/" + pageCount(), l + GRID_X + COLS * CELL / 2, ny, TEXT);
            graphics.text(this.font, ">", l + GRID_X + COLS * CELL - 8, ny, this.page < pageCount() - 1 ? TEXT : TEXT_DIM, false);
        }

        // Recipe panel
        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selected != null) {
            drawRecipe(graphics, this.selected, mouseX, mouseY);
        }
    }

    private void drawRecipe(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
        int x = this.left + DETAIL_X + 6;
        int y = this.top + DETAIL_Y + 6;

        // Name and machine
        graphics.text(this.font, known(entry.result()) ? shortName(entry.result()) : "???", x, y, GOLD, false);
        graphics.item(entry.machine().icon(), x, y + 10);
        graphics.text(this.font, entry.machine().title, x + 20, y + 14, TEXT_DIM, false);

        // Ingredients -> result, on one row
        graphics.text(this.font, "Put in:", x, y + 32, TEXT, false);
        int rowY = y + 42;
        for (int i = 0; i < entry.ingredients().size(); i++) {
            itemSlot(graphics, entry.ingredients().get(i), x + i * 20, rowY, mouseX, mouseY);
        }
        int resultX = x + Math.max(entry.ingredients().size(), 2) * 20 + 12;
        graphics.text(this.font, "->", resultX - 11, rowY + 5, TEXT, false);
        lockedSlot(graphics, entry.result(), resultX, rowY, false, mouseX, mouseY);

        // Fuel or glass bottles
        int nextY = rowY + 24;
        if (entry.extra() != null) {
            itemSlot(graphics, entry.extra(), x, nextY, mouseX, mouseY);
            graphics.text(this.font, entry.extraLabel(), x + 22, nextY + 5, TEXT_DIM, false);
            nextY += 24;
        }

        // Time and water drops
        for (String line : entry.info()) {
            graphics.text(this.font, line, x, nextY, TEXT_DIM, false);
            nextY += 10;
        }
        if (!known(entry.result())) {
            graphics.text(this.font, "Make it once to unlock", x, nextY, TEXT_DIM, false);
            graphics.text(this.font, "its name and info!", x, nextY + 10, TEXT_DIM, false);
        } else if (entry.bonus() != null) {
            if (entry.bonus().water() > 0) {
                graphics.text(this.font, "+" + drops(entry.bonus().water()) + " water drops", x, nextY, WATER, false);
                nextY += 10;
            }
            if (entry.bonus().food() > 0) {
                graphics.text(this.font, "+" + drops(entry.bonus().food()) + " food", x, nextY, GOLD, false);
            }
        }
    }

    private void drawBlocks(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + GRID_X + (i % COLS) * CELL;
            int y = t + GRID_Y + (i / COLS) * CELL;
            BlockEntry block = i < this.blocks.size() ? this.blocks.get(i) : null;
            if (block == null) slot(graphics, x, y, false);
            else lockedSlot(graphics, block.stack(), x, y, block == this.selectedBlock, mouseX, mouseY);
        }

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selectedBlock == null) return;
        int x = l + DETAIL_X + 6;
        int y = t + DETAIL_Y + 6;
        ItemStack stack = this.selectedBlock.stack();
        boolean known = known(stack);
        graphics.text(this.font, known ? stack.getHoverName().getString() : "???", x, y, GOLD, false);
        graphics.text(this.font, "How to make:", x, y + 12, TEXT_DIM, false);
        int gy = y + 23;
        for (int i = 0; i < 9; i++) {
            itemSlot(graphics, this.selectedBlock.how().gridAt(i, net.minecraft.util.Util.getMillis() / 1000L), x + (i % 3) * 18, gy + (i / 3) * 18, mouseX, mouseY);
        }
        graphics.text(this.font, "->", x + 59, gy + 23, TEXT, false);
        lockedSlot(graphics, stack, x + 74, gy + 18, false, mouseX, mouseY);
        int ty = gy + 60;
        if (!known) {
            graphics.text(this.font, "Craft it once to unlock", x, ty, TEXT_DIM, false);
            graphics.text(this.font, "its name and info!", x, ty + 10, TEXT_DIM, false);
            return;
        }
        for (String line : this.selectedBlock.lines()) {
            for (FormattedCharSequence wrapped : this.font.split(Component.literal(line), W - DETAIL_X - 18)) {
                graphics.text(this.font, wrapped, x, ty, TEXT, false);
                ty += 10;
            }
        }
    }

    // Information: a column of clickable topic boxes on the left (like the other guides), the topic on the right.
    private static final int BOX_W = DETAIL_X - GRID_X - 6;
    private static final int BOX_H = 16;
    private static final int BOX_STEP = BOX_H + 3;

    private void drawInformation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;
        List<Topic> list = topicsIn(this.infoTab);
        for (int i = 0; i < list.size(); i++) {
            Topic topic = list.get(i);
            int x = l + GRID_X;
            int y = t + GRID_Y + i * BOX_STEP;
            boolean selected = topic == this.selectedTopic;
            boolean hover = inside(mouseX, mouseY, x, y, BOX_W, BOX_H);
            graphics.fill(x, y, x + BOX_W, y + BOX_H, selected ? SELECTED : hover ? TRIM_LIGHT : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + BOX_W - 1, y + BOX_H - 1, selected ? PANEL_DARK : SLOT);
            graphics.text(this.font, topic.title(), x + 5, y + 4, selected ? GOLD : TEXT, false);
        }

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selectedTopic == null) return;
        int x = l + DETAIL_X + 6;
        int ty = t + DETAIL_Y + 6;
        graphics.text(this.font, this.selectedTopic.title(), x, ty, GOLD, false);
        ty += 14;
        for (String line : this.selectedTopic.lines()) {
            for (FormattedCharSequence wrapped : this.font.split(Component.literal(line), W - DETAIL_X - 18)) {
                graphics.text(this.font, wrapped, x, ty, TEXT, false);
                ty += 10;
            }
            ty += 2;
        }
    }

    private void itemSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        boolean clickable = this.section != Section.INFORMATION && tab() != Tab.BLOCKS && recipeFor(stack) != null
            && !ItemStack.isSameItem(stack, this.selected == null ? ItemStack.EMPTY : this.selected.result());
        slot(graphics, x, y, false);
        if (clickable) {
            graphics.outline(x, y, 18, 18, SELECTED);
        }
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        if (inside(mouseX, mouseY, x, y, 18, 18)) {
            this.hovered = stack;
        }
    }

    // A result slot: the item if you've made it, otherwise its black silhouette on a lighter tile.
    private void lockedSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, boolean selected, int mouseX, int mouseY) {
        if (known(stack)) {
            slot(graphics, x, y, selected);
            graphics.item(stack, x + 1, y + 1);
            graphics.itemDecorations(this.font, stack, x + 1, y + 1);
            if (inside(mouseX, mouseY, x, y, 18, 18)) this.hovered = stack;
            return;
        }
        graphics.fill(x, y, x + 18, y + 18, selected ? SELECTED : SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, LOCKED_SLOT);
        Silhouettes.draw(graphics, stack, x + 1, y + 1);
        graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        if (inside(mouseX, mouseY, x, y, 18, 18)) this.hoveredLocked = true;
    }

    private static boolean known(ItemStack stack) {
        return ClientDiscoveries.known(stack);
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
        graphics.fill(x, y, x + 18, y + 18, selected ? SELECTED : SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    // ── Clicking ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int l = this.left;
        int t = this.top;

        // Section buttons
        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            if (inside(mx, my, l + 8 + i * 76, t + SECTION_Y, 72, 14)) {
                this.section = sections[i];
                // keep the details panel on something from the tab that's now open
                if (this.section != Section.INFORMATION && tab() != Tab.BLOCKS
                    && (this.selected == null || this.selected.tab() != tab())) {
                    this.selected = firstOf(tab());
                    this.page = 0;
                }
                return true;
            }
        }

        // Tabs (every section has them)
        List<Tab> tabs = Tab.in(this.section);
        for (int i = 0; i < tabs.size(); i++) {
            if (inside(mx, my, l + 8 + i * 30, t + MACHINE_TAB_Y, 26, 22)) {
                setTab(tabs.get(i));
                this.page = 0;
                if (tabs.get(i) != Tab.BLOCKS && this.section != Section.INFORMATION) this.selected = firstOf(tabs.get(i));
                return true;
            }
        }

        if (this.section == Section.BAR && tab() == Tab.BLOCKS) {
            for (int i = 0; i < this.blocks.size(); i++) {
                if (inside(mx, my, l + GRID_X + (i % COLS) * CELL, t + GRID_Y + (i / COLS) * CELL, 18, 18)) {
                    this.selectedBlock = this.blocks.get(i);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (this.section == Section.INFORMATION) {
            List<Topic> list = topicsIn(this.infoTab);
            for (int i = 0; i < list.size(); i++) {
                if (inside(mx, my, l + GRID_X, t + GRID_Y + i * BOX_STEP, BOX_W, BOX_H)) {
                    this.selectedTopic = list.get(i);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Grid
        List<Entry> list = inTab(tab());
        for (int i = 0; i < COLS * ROWS; i++) {
            int index = this.page * COLS * ROWS + i;
            if (index >= list.size()) break;
            if (inside(mx, my, l + GRID_X + (i % COLS) * CELL, t + GRID_Y + (i / COLS) * CELL, 18, 18)) {
                this.selected = list.get(index);
                return true;
            }
        }

        // Grid page arrows
        int ny = t + GRID_Y + ROWS * CELL + 2;
        if (inside(mx, my, l + GRID_X, ny - 2, 12, 12) && this.page > 0) {
            this.page--;
            return true;
        }
        if (inside(mx, my, l + GRID_X + COLS * CELL - 10, ny - 2, 12, 12) && this.page < pageCount() - 1) {
            this.page++;
            return true;
        }

        // Clicking an ingredient you can make jumps to its recipe
        if (this.selected != null) {
            int x = l + DETAIL_X + 6;
            int rowY = t + DETAIL_Y + 6 + 42;
            for (int i = 0; i < this.selected.ingredients().size(); i++) {
                if (inside(mx, my, x + i * 20, rowY, 18, 18)) {
                    Entry target = recipeFor(this.selected.ingredients().get(i));
                    if (target != null) {
                        setTab(target.tab());
                        this.selected = target;
                        this.page = inTab(tab()).indexOf(target) / (COLS * ROWS);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // Long names lose the "Bucket o' " part so they fit the panel.
    private String shortName(ItemStack stack) {
        String name = stack.getHoverName().getString();
        return this.font.width(name) > W - DETAIL_X - 18 ? name.replace("Bucket o' ", "") : name;
    }

    // 2 points = 1 icon
    private static String drops(int points) {
        return points % 2 == 0 ? String.valueOf(points / 2) : (points / 2) + ".5";
    }

    // Machine times come from the server config, so the guide matches if they're changed.
    private static String time(ModConfigSpec.IntValue ticks, int fallback) {
        int value;
        try {
            value = ticks.get();
        } catch (IllegalStateException notLoaded) {
            value = fallback;
        }
        int seconds = value / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        if (minutes == 0) return seconds + " sec";
        return seconds == 0 ? minutes + " min" : minutes + " min " + seconds + " sec";
    }
}

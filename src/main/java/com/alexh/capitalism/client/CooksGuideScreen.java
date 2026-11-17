package com.alexh.capitalism.client;

import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.guide.ClientDiscoveries;
import com.alexh.capitalism.menu.AssemblyTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// The Cook's Guide, in two sections:
//  - Kitchen: everything a Cook works with, in two tabs (Blocks: the stations; Accessories: plates, rice...):
//    how to make it, and what it does.
//    Black silhouettes until you've made one.
//  - Recipes: tabs for Sushi, Specialty Sushi, Food (Cooking Table dishes) and Specialty Food (the ones that give
//    effects), an icon grid, click a dish for its recipe.
//    Built straight from KitchenRecipes, so new dishes show up on their own. Dishes you haven't made yet are black
//    silhouettes named "???" (the recipe still shows); making one once unlocks it (guide/Discoveries).
public class CooksGuideScreen extends Screen {

    private static final int W = 280;
    private static final int H = 216;
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int CELL = 20;

    // Layout (relative to the panel's top-left)
    private static final int SECTION_Y = 18;
    private static final int TAB_Y = 36;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 62;
    private static final int DETAIL_X = 116;
    private static final int DETAIL_Y = 62;

    // Colors (warm spruce kitchen wood, copper-pot trim, cream text)
    private static final int PANEL = 0xFF6A4628;
    private static final int PANEL_DARK = 0xFF3C2614;
    private static final int TRIM_LIGHT = 0xFFE09A5A;
    private static final int TRIM_DARK = 0xFF8A4E22;
    private static final int SLOT = 0xFF2C1A0C;
    private static final int SLOT_EDGE = 0xFFA0744A;
    private static final int SELECTED = 0xFFF0B070;
    private static final int TEXT = 0xFFF6E8CC;
    private static final int TEXT_DIM = 0xFFC8AC88;
    private static final int GOLD = 0xFFFFD24A;
    private static final int EFFECT = 0xFF9CE0FF;
    private static final int WARN = 0xFFFF8A6A;
    private static final int LOCKED_SLOT = 0xFF8C6A48;

    private enum Section {
        KITCHEN("Kitchen"), RECIPES("Recipes"), INFORMATION("Information");

        final String title;

        Section(String title) {
            this.title = title;
        }
    }

    private enum Tab {
        SUSHI("Sushi"), SPECIALTY_SUSHI("Specialty Sushi"), FOOD("Food"), SPECIALTY("Specialty Food");

        final String title;

        Tab(String title) {
            this.title = title;
        }

        ItemStack icon() {
            return new ItemStack(switch (this) {
                case SUSHI -> ModItems.SALMON_NIGIRI.get();
                case SPECIALTY_SUSHI -> ModItems.OCEAN_SOVEREIGN_ROLL.get();
                case FOOD -> ModItems.CHICKEN_POT_PIE.get();
                case SPECIALTY -> ModItems.FIERY_MUSHROOM_BOWL.get();
            });
        }
    }

    // The Kitchen's two tabs: the stations you place, and everything else you handle.
    private enum KitchenTab {
        BLOCKS("Blocks"), ACCESSORIES("Accessories");

        final String title;

        KitchenTab(String title) {
            this.title = title;
        }

        ItemStack icon() {
            return new ItemStack(this == BLOCKS ? ModItems.OVEN.get() : ModItems.SILVER_PLATE.get());
        }
    }

    // A Kitchen item: how it's made and a few short lines about it (shown once you've made one).
    private record KitchenItem(KitchenTab tab, ItemStack stack, HowToMake how, List<String> lines) {
    }

    // A dish: the table recipe, plus the Oven step if it's baked (null for sushi and dishes like Fruit Salad).
    private record Dish(Tab tab, boolean sushi, KitchenRecipes.Assembly recipe, ItemStack made, KitchenRecipes.@Nullable Baking baking, ItemStack meal) {
    }

    // An Information topic: a clickable box with its title and a line or two.
    private record Topic(String title, List<String> lines) {
    }

    // Information has one tab for now: what everyone without the Cook job can't do.
    private static final List<Topic> DEBUFFS = List.of(
        new Topic("Weaker Food", List.of("Food others cook fills you up less.")),
        new Topic("Locked Kitchen", List.of("Others can't use the Sushi Table,", "Cooking Table or Oven.")),
        new Topic("No Serving", List.of("Others can't place plates", "or serve meals on them.")));
    private static final int BOX_W = DETAIL_X - GRID_X - 6;
    private static final int BOX_H = 16;
    private static final int BOX_STEP = BOX_H + 3;
    private Topic selectedTopic = DEBUFFS.get(0);

    private final List<KitchenItem> allKitchen = buildKitchen();
    private KitchenTab kitchenTab = KitchenTab.BLOCKS;
    private final List<Dish> dishes = buildDishes();
    private Section section = Section.KITCHEN;
    private Tab tab = Tab.SUSHI;
    private int page = 0;
    private int kitchenPage = 0;
    private @Nullable Dish selected;
    private @Nullable KitchenItem selectedItem;

    private int left;
    private int top;
    private @Nullable ItemStack hovered;
    private boolean hoveredLocked;
    // Where Esc goes back to (the /guides screen), or null to just close.
    private final @Nullable Screen parent;

    public CooksGuideScreen() {
        this(null);
    }

    public CooksGuideScreen(@Nullable Screen parent) {
        super(Component.literal("Cook's Guide"));
        this.parent = parent;
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new CooksGuideScreen());
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
        if (this.selected == null) this.selected = firstOf(this.tab);
        if (this.selectedItem == null && !kitchen().isEmpty()) this.selectedItem = kitchen().get(0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Content ────────────────────────────────────────────────────────────
    // Kitchen lines: short, the panel wraps them. Keep these in sync with Cook.md.

    private static List<KitchenItem> buildKitchen() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack kelp = new ItemStack(Items.DRIED_KELP);
        ItemStack cod = new ItemStack(Items.COD);
        ItemStack bambooPlanks = new ItemStack(Items.BAMBOO_PLANKS);
        ItemStack bamboo = new ItemStack(Items.BAMBOO);
        ItemStack slab = new ItemStack(Items.SMOOTH_STONE_SLAB);
        ItemStack smooth = new ItemStack(Items.SMOOTH_STONE);
        ItemStack bricks = new ItemStack(Items.BRICKS);
        ItemStack ironNugget = new ItemStack(Items.IRON_NUGGET);
        ItemStack goldNugget = new ItemStack(Items.GOLD_NUGGET);
        ItemStack silverPlate = new ItemStack(ModItems.SILVER_PLATE.get());

        List<KitchenItem> list = new ArrayList<>();
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.SUSHI_TABLE.get()),
            HowToMake.crafting(kelp, cod, kelp, bambooPlanks, bambooPlanks, bambooPlanks, bamboo, e, bamboo), List.of(
            "Rolls sushi: hold ROLL, let go in the green.",
            "Rice in slot 1, Dried Kelp in slot 2.")));
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.COOKING_TABLE.get()),
            HowToMake.crafting(slab, slab, slab, planks, new ItemStack(Items.CRAFTING_TABLE), planks, planks, planks, planks), List.of(
            "Puts dishes together. Each ingredient has",
            "its own slot, clockwise from the top.")));
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.OVEN.get()),
            HowToMake.crafting(bricks, bricks, bricks, bricks, new ItemStack(Items.FURNACE), bricks, bricks, iron, bricks), List.of(
            "Bakes 3 dishes at once with fuel. Dings",
            "when done. Take meals out or they burn!")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, silverPlate,
            HowToMake.crafting(iron, ironNugget, iron, iron, iron, iron, e, e, e), List.of(
            "Serve one meal on it. Eating off it gives",
            "full gold. Only Cooks place and serve.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.GOLDEN_PLATE.get()),
            HowToMake.crafting(gold, goldNugget, gold, gold, gold, gold, e, e, e), List.of(
            "Like the Silver Plate, but meals give",
            "1.5x gold.")));
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.SINK.get()),
            HowToMake.crafting(e, iron, e, smooth, new ItemStack(Items.CAULDRON), smooth, e, e, e), List.of(
            "Right-click it with a Dirty Plate",
            "to wash it clean.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.DIRTY_SILVER_PLATE.get()),
            HowToMake.steps("Someone eats off a plate; pick it up with an empty hand.", silverPlate), List.of(
            "Wash it in the Sink before serving again.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.DIRTY_GOLDEN_PLATE.get()),
            HowToMake.steps("Someone eats off a Golden Plate; pick it up with an empty hand.", new ItemStack(ModItems.GOLDEN_PLATE.get())), List.of(
            "Wash it in the Sink before serving again.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.RICE_SEEDS.get()),
            HowToMake.steps("Found wild in shallow water: swamps, rivers, jungles.", new ItemStack(Items.WATER_BUCKET)), List.of(
            "Plant in 1-deep water. Ripe rice gives",
            "Rice and more seeds.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.COOKED_RICE.get()),
            HowToMake.steps("Smelt Rice in a Furnace or Smoker (not the Oven).",
                new ItemStack(ModItems.RICE.get()), new ItemStack(Items.FURNACE), new ItemStack(Items.SMOKER)), List.of(
            "Every sushi starts with it.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.BURNT_FOOD.get()),
            HowToMake.steps("Leave a finished meal in a lit Oven too long.", new ItemStack(ModItems.OVEN.get())), List.of(
            "Edible in a pinch: half a drumstick,",
            "no diet points.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.SQUASHED_SUSHI.get()),
            HowToMake.steps("Let go of ROLL too early or too late.", new ItemStack(ModItems.SUSHI_TABLE.get())), List.of(
            "Can't be eaten.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.TRAINING_FEED.get()),
            HowToMake.crafting(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.BEEF), e, e, e, e, e, e, e), List.of(
            "Any seeds next to any raw meat or fish.",
            "Only Pet Trainers can feed it to animals.")));
        // Fresh meat (farmer/Freshness): the Fridge and Freezer (Cooks and Farmers make them), Rotten Meat, fine cuts
        ItemStack chest = new ItemStack(Items.CHEST);
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.FRIDGE.get()),
            HowToMake.crafting(iron, iron, iron, iron, chest, iron, iron, new ItemStack(Items.REDSTONE), iron), List.of(
            "Fine cuts rot. They keep longer in it",
            "while it burns coal or charcoal.")));
        list.add(new KitchenItem(KitchenTab.BLOCKS, new ItemStack(ModItems.FREEZER.get()),
            HowToMake.crafting(iron, iron, iron, iron, chest, iron, iron, new ItemStack(Items.PACKED_ICE), iron), List.of(
            "Keeps meat much longer while it melts",
            "ice or snowballs. Ice lasts far longer.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB.cut()),
            HowToMake.steps("Farmers cut them from Meat Slabs at the Butcher's Table.", new ItemStack(ModItems.BUTCHERS_TABLE.get())), List.of(
            "Fine cuts of meat. Each piece rots in",
            "time: the bar under it shows how fresh.")));
        ItemStack grill = new ItemStack(ModItems.GRILL.get());
        list.add(new KitchenItem(KitchenTab.BLOCKS, grill,
            HowToMake.crafting(new ItemStack(Items.IRON_BARS), new ItemStack(Items.IRON_BARS), new ItemStack(Items.IRON_BARS),
                iron, new ItemStack(Items.CAMPFIRE), iron, iron, e, iron), List.of(
            "Sear up to three cuts at once: flip",
            "each in time or it burns! Pull at MR.")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.SEARED_CUTS.get(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB).get()),
            HowToMake.steps("Sear a fine cut on the Grill, with coal for heat.", grill), List.of(
            "Medium Rare fills most (chicken: Well",
            "Done, or it makes you ill!).")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(com.alexh.capitalism.farmer.MeatCut.BEEF_CHUCK.cut()),
            HowToMake.steps("Leave a fine cut in a running Fridge (not a Freezer).", new ItemStack(ModItems.FRIDGE.get())), List.of(
            "It dry-ages: Dry-Aged, then Prime. Aged",
            "steaks fill more. It still rots, slower!")));
        list.add(new KitchenItem(KitchenTab.ACCESSORIES, new ItemStack(ModItems.ROTTEN_MEAT.get()),
            HowToMake.steps("Meat left too long turns into it.", new ItemStack(Items.CLOCK)), List.of(
            "Not for eating. Seeds next to it",
            "make Training Feed too.")));
        return list;
    }

    private static List<Dish> buildDishes() {
        List<Dish> list = new ArrayList<>();
        for (KitchenRecipes.Assembly recipe : KitchenRecipes.SUSHI) {
            ItemStack made = new ItemStack(recipe.result().get(), recipe.count());
            list.add(new Dish(hasEffects(made) ? Tab.SPECIALTY_SUSHI : Tab.SUSHI, true, recipe, made, null, made));
        }
        for (KitchenRecipes.Assembly recipe : KitchenRecipes.COOKING) {
            ItemStack made = new ItemStack(recipe.result().get(), recipe.count());
            KitchenRecipes.Baking baking = KitchenRecipes.baking(made);
            ItemStack meal = baking == null ? made : new ItemStack(baking.result().get());
            list.add(new Dish(hasEffects(meal) ? Tab.SPECIALTY : Tab.FOOD, false, recipe, made, baking, meal));
        }
        return list;
    }

    // The Kitchen items on the open tab
    private List<KitchenItem> kitchen() {
        return this.allKitchen.stream().filter(k -> k.tab() == this.kitchenTab).toList();
    }

    private List<Dish> inTab(Tab tab) {
        return this.dishes.stream().filter(d -> d.tab() == tab).toList();
    }

    private @Nullable Dish firstOf(Tab tab) {
        List<Dish> list = inTab(tab);
        return list.isEmpty() ? null : list.get(0);
    }

    private int pageCount(int size) {
        return Math.max(1, (size + COLS * ROWS - 1) / (COLS * ROWS));
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.hovered = null;
        this.hoveredLocked = false;
        int l = this.left;
        int t = this.top;

        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + W, t + H, PANEL);
        graphics.centeredText(this.font, this.title, l + W / 2, t + 6, GOLD);

        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            int x = sectionX(i);
            int w = sectionW(i);
            boolean active = sections[i] == this.section;
            graphics.fill(x, t + SECTION_Y, x + w, t + SECTION_Y + 14, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, t + SECTION_Y + 1, x + w - 1, t + SECTION_Y + 13, active ? PANEL_DARK : SLOT);
            graphics.centeredText(this.font, sections[i].title, x + w / 2, t + SECTION_Y + 3, active ? GOLD : TEXT_DIM);
        }

        switch (this.section) {
            case KITCHEN -> drawKitchen(graphics, mouseX, mouseY);
            case RECIPES -> drawRecipes(graphics, mouseX, mouseY);
            case INFORMATION -> drawInformation(graphics, mouseX, mouseY);
        }

        if (this.hoveredLocked) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal("???"),
                Component.literal("Make it once to unlock").withStyle(s -> s.withColor(TEXT_DIM))), mouseX, mouseY, ItemStack.EMPTY);
        } else if (this.hovered != null && !this.hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, this.hovered, mouseX, mouseY);
        }
    }

    private int sectionX(int i) {
        return this.left + 8 + i * 76;
    }

    private int sectionW(int i) {
        return 72;
    }

    // Information: the Player Debuffs tab, a column of clickable topic boxes on the left, the topic on the right.
    private void drawInformation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;
        int tx = l + 8;
        int ty = t + TAB_Y;
        graphics.fill(tx, ty, tx + 26, ty + 22, SELECTED);
        graphics.fill(tx + 1, ty + 1, tx + 25, ty + 21, PANEL_DARK);
        graphics.item(new ItemStack(Items.BARRIER), tx + 5, ty + 3);
        graphics.text(this.font, "Player Debuffs", tx + 36, ty + 7, TEXT, false);

        for (int i = 0; i < DEBUFFS.size(); i++) {
            Topic topic = DEBUFFS.get(i);
            int x = l + GRID_X;
            int y = t + GRID_Y + i * BOX_STEP;
            boolean selected = topic == this.selectedTopic;
            boolean hover = inside(mouseX, mouseY, x, y, BOX_W, BOX_H);
            graphics.fill(x, y, x + BOX_W, y + BOX_H, selected ? SELECTED : hover ? TRIM_LIGHT : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + BOX_W - 1, y + BOX_H - 1, selected ? PANEL_DARK : SLOT);
            graphics.text(this.font, topic.title(), x + 5, y + 4, selected ? GOLD : TEXT, false);
        }

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        int x = l + DETAIL_X + 6;
        int y = t + DETAIL_Y + 6;
        graphics.text(this.font, this.selectedTopic.title(), x, y, GOLD, false);
        y += 14;
        for (String line : this.selectedTopic.lines()) {
            for (FormattedCharSequence wrapped : this.font.split(Component.literal(line), W - DETAIL_X - 18)) {
                graphics.text(this.font, wrapped, x, y, TEXT, false);
                y += 10;
            }
            y += 2;
        }
    }

    private void drawKitchen(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;
        KitchenTab[] tabs = KitchenTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int x = l + 8 + i * 30;
            int y = t + TAB_Y;
            boolean active = tabs[i] == this.kitchenTab;
            graphics.fill(x, y, x + 26, y + 22, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + 25, y + 21, active ? PANEL_DARK : SLOT);
            graphics.item(tabs[i].icon(), x + 5, y + 3);
            if (inside(mouseX, mouseY, x, y, 26, 22)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(tabs[i].title)), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        graphics.text(this.font, this.kitchenTab.title, l + 8 + tabs.length * 30 + 6, t + TAB_Y + 2, TEXT, false);
        graphics.text(this.font, "Make one to unlock it!", l + 8 + tabs.length * 30 + 6, t + TAB_Y + 12, TEXT_DIM, false);

        int start = this.kitchenPage * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + GRID_X + (i % COLS) * CELL;
            int y = t + GRID_Y + (i / COLS) * CELL;
            int index = start + i;
            KitchenItem item = index < kitchen().size() ? kitchen().get(index) : null;
            if (item == null) slot(graphics, x, y, false);
            else lockedSlot(graphics, item.stack(), x, y, item == this.selectedItem, mouseX, mouseY);
        }
        drawGridPages(graphics, this.kitchenPage, pageCount(kitchen().size()));

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selectedItem == null) return;
        int x = l + DETAIL_X + 6;
        int y = t + DETAIL_Y + 6;
        ItemStack stack = this.selectedItem.stack();
        lockedSlot(graphics, stack, x, y, false, mouseX, mouseY);
        graphics.text(this.font, known(stack) ? stack.getHoverName() : Component.literal("???"), x + 22, y + 5, GOLD, false);

        int ty = drawHowToMake(graphics, this.selectedItem.how(), stack, x, y + 24, mouseX, mouseY);
        if (!known(stack)) {
            graphics.text(this.font, "Make it once to unlock", x, ty, TEXT_DIM, false);
            graphics.text(this.font, "its name and info!", x, ty + 10, TEXT_DIM, false);
            return;
        }
        for (String line : this.selectedItem.lines()) {
            for (FormattedCharSequence wrapped : this.font.split(Component.literal(line), W - DETAIL_X - 18)) {
                graphics.text(this.font, wrapped, x, ty, TEXT, false);
                ty += 10;
            }
        }
    }

    // "How to make": a 3x3 crafting grid -> the item, or a row of icons with a short note. Returns the y below it.
    private int drawHowToMake(GuiGraphicsExtractor graphics, HowToMake how, ItemStack result, int x, int y, int mouseX, int mouseY) {
        graphics.text(this.font, "How to make:", x, y, TEXT_DIM, false);
        y += 11;
        if (how.grid() != null) {
            for (int i = 0; i < 9; i++) {
                itemSlot(graphics, how.grid()[i], x + (i % 3) * 18, y + (i / 3) * 18, mouseX, mouseY);
            }
            graphics.text(this.font, "->", x + 59, y + 23, TEXT, false);
            lockedSlot(graphics, result, x + 74, y + 18, false, mouseX, mouseY);
            return y + 60;
        }
        for (int i = 0; i < how.steps().size(); i++) {
            itemSlot(graphics, how.steps().get(i), x + i * 20, y, mouseX, mouseY);
        }
        int ty = y + 22;
        for (FormattedCharSequence wrapped : this.font.split(Component.literal(how.note()), W - DETAIL_X - 18)) {
            graphics.text(this.font, wrapped, x, ty, TEXT, false);
            ty += 10;
        }
        return ty + 4;
    }

    private void drawRecipes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left;
        int t = this.top;

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int x = l + 8 + i * 30;
            int y = t + TAB_Y;
            boolean active = tabs[i] == this.tab;
            graphics.fill(x, y, x + 26, y + 22, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + 25, y + 21, active ? PANEL_DARK : SLOT);
            graphics.item(tabs[i].icon(), x + 5, y + 3);
            if (inside(mouseX, mouseY, x, y, 26, 22)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(tabs[i].title)), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        List<Dish> list = inTab(this.tab);
        graphics.text(this.font, this.tab.title + " (" + list.size() + ")", l + 8 + tabs.length * 30 + 6, t + TAB_Y + 7, TEXT, false);

        int start = this.page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + GRID_X + (i % COLS) * CELL;
            int y = t + GRID_Y + (i / COLS) * CELL;
            int index = start + i;
            Dish dish = index < list.size() ? list.get(index) : null;
            if (dish == null) {
                slot(graphics, x, y, false);
            } else {
                lockedSlot(graphics, dish.meal(), x, y, dish == this.selected, mouseX, mouseY);
            }
        }
        drawGridPages(graphics, this.page, pageCount(list.size()));

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selected != null) {
            if (this.selected.sushi()) drawSushi(graphics, this.selected, mouseX, mouseY);
            else drawCooking(graphics, this.selected, mouseX, mouseY);
        }
    }

    private void drawGridPages(GuiGraphicsExtractor graphics, int current, int count) {
        if (count <= 1) return;
        int l = this.left;
        int ny = this.top + GRID_Y + ROWS * CELL + 2;
        graphics.text(this.font, "<", l + GRID_X + 2, ny, current > 0 ? TEXT : TEXT_DIM, false);
        graphics.centeredText(this.font, (current + 1) + "/" + count, l + GRID_X + COLS * CELL / 2, ny, TEXT);
        graphics.text(this.font, ">", l + GRID_X + COLS * CELL - 8, ny, current < count - 1 ? TEXT : TEXT_DIM, false);
    }

    // Sushi: the slots in order (rice first, kelp second, the rest any order), ROLL, the roll.
    private void drawSushi(GuiGraphicsExtractor graphics, Dish dish, int mouseX, int mouseY) {
        int x = this.left + DETAIL_X + 6;
        int y = this.top + DETAIL_Y + 6;
        graphics.text(this.font, known(dish.meal()) ? dish.meal().getHoverName() : Component.literal("???"), x, y, GOLD, false);

        List<Supplier<? extends Item>> ingredients = dish.recipe().ingredients();
        int rowY = y + 24;
        for (int i = 0; i < ingredients.size(); i++) {
            int sx = x + i * 20;
            if (i < dish.recipe().ordered()) {
                graphics.centeredText(this.font, String.valueOf(i + 1), sx + 9, rowY - 10, TEXT_DIM);
            }
            itemSlot(graphics, new ItemStack(ingredients.get(i).get()), sx, rowY, mouseX, mouseY);
        }
        int resultX = x + 4 * 20 + 14;
        graphics.text(this.font, "->", resultX - 12, rowY + 5, TEXT, false);
        lockedSlot(graphics, dish.made(), resultX, rowY, false, mouseX, mouseY);

        int ty = rowY + 24;
        int loose = ingredients.size() - dish.recipe().ordered();
        if (loose > 1) {
            graphics.text(this.font, "Numbered slots first; the", x, ty, TEXT_DIM, false);
            graphics.text(this.font, "rest in any order.", x, ty + 10, TEXT_DIM, false);
            ty += 22;
        } else {
            graphics.text(this.font, "Slots in this order.", x, ty, TEXT_DIM, false);
            ty += 12;
        }
        graphics.text(this.font, "Hold ROLL, let go in green!", x, ty, TEXT, false);
        drawFood(graphics, dish.meal(), x, ty + 12);
    }

    // Cooking Table: the plate with every ingredient in its slot, then the uncooked dish -> Oven -> meal.
    private void drawCooking(GuiGraphicsExtractor graphics, Dish dish, int mouseX, int mouseY) {
        int x = this.left + DETAIL_X + 6;
        int y = this.top + DETAIL_Y + 6;
        graphics.text(this.font, known(dish.meal()) ? dish.meal().getHoverName() : Component.literal("???"), x, y, GOLD, false);

        // Mini plate: the table's 5 slot spots, shifted to start at (x, y + 12)
        int px = x;
        int py = y + 12;
        int[][] spots = AssemblyTableMenu.COOKING_SLOTS;
        drawPlate(graphics, px + 28, py + 28);
        List<Supplier<? extends Item>> ingredients = dish.recipe().ingredients();
        for (int i = 0; i < spots.length; i++) {
            int sx = px + spots[i][0] - 33;
            int sy = py + spots[i][1] - 16;
            ItemStack stack = i < ingredients.size() ? new ItemStack(ingredients.get(i).get()) : ItemStack.EMPTY;
            itemSlot(graphics, stack, sx, sy, mouseX, mouseY);
        }

        // Makes -> Oven -> meal (or straight to the meal)
        int rx = x + 64;
        int ry = y + 26;
        lockedSlot(graphics, dish.made(), rx, ry, false, mouseX, mouseY);
        if (dish.baking() != null) {
            graphics.text(this.font, ">", rx + 20, ry + 5, TEXT, false);
            graphics.item(new ItemStack(ModItems.OVEN.get()), rx + 27, ry + 1);
            if (inside(mouseX, mouseY, rx + 27, ry + 1, 16, 16)) this.hovered = new ItemStack(ModItems.OVEN.get());
            graphics.text(this.font, ">", rx + 45, ry + 5, TEXT, false);
            lockedSlot(graphics, dish.meal(), rx + 52, ry, false, mouseX, mouseY);
            graphics.text(this.font, "Bake " + dish.baking().cookSeconds() + "s", rx, ry + 22, TEXT, false);
            graphics.text(this.font, "Burns " + dish.baking().burnSeconds() + "s later", rx, ry + 32, WARN, false);
        } else {
            graphics.text(this.font, "Ready to eat!", rx, ry + 22, TEXT, false);
            graphics.text(this.font, "(no Oven)", rx, ry + 32, TEXT_DIM, false);
        }

        drawFood(graphics, dish.meal(), x, py + 60);
    }

    // The Cooking Table's round plate (same colors and rings as textures/gui/container/cooking_table.png, made by
    // scratchpad make_kitchen_gui.ps1), centered on (cx, cy).
    private static void drawPlate(GuiGraphicsExtractor graphics, int cx, int cy) {
        for (int dx = -29; dx <= 29; dx++) {
            for (int dy = -29; dy <= 29; dy++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 28.5) continue;
                int color = 0xFFEEECE4;
                if (d > 26.5) color = 0xFF96928A;                       // outer rim shadow
                else if (d > 24.5) color = 0xFFFAFAF6;                  // rim highlight
                else if (d > 17 && d < 18.2) color = 0xFFD6D2C8;        // inner ring
                if (dx + dy < -20 && d < 24) color = 0xFFFCFCFA;        // shine
                graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }

    // Food or drink that gives effects when you have it (shown in the Specialty tabs).
    static boolean hasEffects(ItemStack stack) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) return false;
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyStatusEffectsConsumeEffect statuses && !statuses.effects().isEmpty()) return true;
        }
        return false;
    }

    // Hunger and effects of the finished meal.
    private void drawFood(GuiGraphicsExtractor graphics, ItemStack meal, int x, int y) {
        if (!known(meal)) {
            graphics.text(this.font, "Make it once to unlock", x, y, TEXT_DIM, false);
            graphics.text(this.font, "its name and info!", x, y + 10, TEXT_DIM, false);
            return;
        }
        FoodProperties food = meal.get(DataComponents.FOOD);
        if (food != null) {
            graphics.text(this.font, "+" + drumsticks(food.nutrition()) + " drumsticks", x, y, GOLD, false);
            y += 10;
        }
        Consumable consumable = meal.get(DataComponents.CONSUMABLE);
        if (consumable == null) return;
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect statuses)) continue;
            for (MobEffectInstance instance : statuses.effects()) {
                String name = instance.getEffect().value().getDisplayName().getString();
                if (instance.getAmplifier() > 0) name += " " + Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)).getString();
                graphics.text(this.font, name + " " + duration(instance.getDuration()), x, y, EFFECT, false);
                y += 10;
            }
        }
    }

    private void itemSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        slot(graphics, x, y, false);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        if (inside(mouseX, mouseY, x, y, 18, 18)) this.hovered = stack;
    }

    // A dish (or uncooked dish) slot: the item if you've made it, otherwise its black silhouette on a lighter tile.
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

        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            if (inside(mx, my, sectionX(i), t + SECTION_Y, sectionW(i), 14)) {
                this.section = sections[i];
                return true;
            }
        }

        if (this.section == Section.KITCHEN) {
            KitchenTab[] tabs = KitchenTab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (inside(mx, my, l + 8 + i * 30, t + TAB_Y, 26, 22)) {
                    this.kitchenTab = tabs[i];
                    this.kitchenPage = 0;
                    this.selectedItem = kitchen().isEmpty() ? null : kitchen().get(0);
                    return true;
                }
            }
        }

        if (this.section == Section.RECIPES) {
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (inside(mx, my, l + 8 + i * 30, t + TAB_Y, 26, 22)) {
                    this.tab = tabs[i];
                    this.page = 0;
                    this.selected = firstOf(this.tab);
                    return true;
                }
            }
        }

        if (this.section == Section.INFORMATION) {
            for (int i = 0; i < DEBUFFS.size(); i++) {
                if (inside(mx, my, l + GRID_X, t + GRID_Y + i * BOX_STEP, BOX_W, BOX_H)) {
                    this.selectedTopic = DEBUFFS.get(i);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Grid
        int size = this.section == Section.KITCHEN ? kitchen().size() : inTab(this.tab).size();
        int current = this.section == Section.KITCHEN ? this.kitchenPage : this.page;
        for (int i = 0; i < COLS * ROWS; i++) {
            int index = current * COLS * ROWS + i;
            if (index >= size) break;
            if (inside(mx, my, l + GRID_X + (i % COLS) * CELL, t + GRID_Y + (i / COLS) * CELL, 18, 18)) {
                if (this.section == Section.KITCHEN) this.selectedItem = kitchen().get(index);
                else this.selected = inTab(this.tab).get(index);
                return true;
            }
        }

        // Grid page arrows
        int ny = t + GRID_Y + ROWS * CELL + 2;
        int count = pageCount(size);
        int step = 0;
        if (inside(mx, my, l + GRID_X, ny - 2, 12, 12) && current > 0) step = -1;
        if (inside(mx, my, l + GRID_X + COLS * CELL - 10, ny - 2, 12, 12) && current < count - 1) step = 1;
        if (step != 0) {
            if (this.section == Section.KITCHEN) this.kitchenPage += step;
            else this.page += step;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // 2 points = 1 drumstick
    private static String drumsticks(int points) {
        return points % 2 == 0 ? String.valueOf(points / 2) : (points / 2) + ".5";
    }

    // ticks -> "4:00"
    private static String duration(int ticks) {
        int seconds = ticks / 20;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}

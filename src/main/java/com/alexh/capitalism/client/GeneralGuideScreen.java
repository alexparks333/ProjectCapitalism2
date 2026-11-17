package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

// The General Guide (everyone can read it, Economy.md > In-game: the General Guide), three areas:
//  - Rules: the server's rules, from config/capitalism-rules.txt (core/ServerRules, sent at login: ClientRules), numbered.
//  - Wiki: Gold, Insurance, Town, Events, Jobs, Wellness: word tabs, short entries listed by title on the left, the entry on the right.
//  - Crafting: Blocks (Shopfront, Purification Table) and Items (Purified Water, Compacted Iron / Diamond), with how to make them.
// Everything here is hand-written: keep the crafting grids matching data/capitalism/recipe/ and the wiki matching
// Economy.md, Insurance.md, Town.md, Events.md, Jobs.md, Wellness.md and Cravings.md. Some things are left to be found
// on purpose (the Blood Moon's secrets, the Whisperer, the Wishing Well and what Wishing Stars do: never say): keep them
// vague. Insurance prices live in the server config: never write them here (point to /insurance quotes).
public class GeneralGuideScreen extends Screen {

    private static final int W = 320;
    private static final int H = 236;
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int CELL = 20;

    private static final int SECTION_Y = 18;
    private static final int TAB_Y = 36;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 62;
    private static final int DETAIL_X = 116;
    private static final int DETAIL_Y = 62;
    // the Wiki: its tabs are words (as wide as their name) and its entries a list of titles
    private static final int ROW_H = 11;
    private static final int LIST_ROWS = 15;
    private static final int LIST_W = DETAIL_X - GRID_X - 4;

    // Colors (green banker's felt, gold trim)
    private static final int PANEL = 0xFF2E4636;
    private static final int PANEL_DARK = 0xFF1A2C20;
    private static final int TRIM_LIGHT = 0xFFD4A040;
    private static final int TRIM_DARK = 0xFF7A5418;
    private static final int SLOT = 0xFF122018;
    private static final int SLOT_EDGE = 0xFF6E8A6A;
    private static final int SELECTED = 0xFFE8C060;
    private static final int TEXT = 0xFFEEF2DC;
    private static final int TEXT_DIM = 0xFFA8BC9C;
    private static final int GOLD = 0xFFFFD24A;

    private enum Section {
        START("Getting Started"), RULES("Rules"), WIKI("Wiki"), CRAFTING("Crafting");

        final String title;

        Section(String title) {
            this.title = title;
        }
    }

    private enum Tab {
        GOLD("Gold", Section.WIKI), INSURANCE("Insurance", Section.WIKI), TOWN("Town", Section.WIKI), EVENTS("Events", Section.WIKI), JOBS("Jobs", Section.WIKI),
        WELLNESS("Wellness", Section.WIKI),
        BLOCKS("Blocks", Section.CRAFTING), ITEMS("Items", Section.CRAFTING);

        final String title;
        final Section section;

        Tab(String title, Section section) {
            this.title = title;
            this.section = section;
        }

        static List<Tab> in(Section section) {
            return Arrays.stream(values()).filter(tab -> tab.section == section).toList();
        }

        ItemStack icon() {
            return switch (this) {
                case BLOCKS -> new ItemStack(ModItems.SHOPFRONT.get());
                case ITEMS -> new ItemStack(ModItems.PURIFIED_WATER.get());
                case GOLD -> new ItemStack(Items.GOLD_INGOT);
                case INSURANCE -> new ItemStack(Items.PAPER);
                case TOWN -> new ItemStack(Items.BELL);
                case EVENTS -> new ItemStack(Items.REDSTONE);
                case WELLNESS -> new ItemStack(Items.APPLE);
                case JOBS -> new ItemStack(ModItems.OVEN.get());
            };
        }
    }

    // One entry: its icon in the grid; either a "how to make" (Crafting) or a row of pictures (Topics); a few lines.
    private record Entry(Tab tab, ItemStack icon, String title, @Nullable HowToMake how, List<ItemStack> pictures, List<String> lines) {
    }

    private final List<Entry> entries = buildEntries();
    private final @Nullable Screen parent;
    private Section section = Section.START;
    private Tab craftingTab = Tab.BLOCKS;
    private Tab wikiTab = Tab.GOLD;
    // how far the entry (or the rules) is scrolled, if it's longer than its panel
    private int scroll;
    private int page = 0;
    private @Nullable Entry selected;
    private int left;
    private int top;
    private @Nullable ItemStack hovered;

    public GeneralGuideScreen(@Nullable Screen parent) {
        super(Component.literal("General Guide"));
        this.parent = parent;
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
        if (this.selected == null && tab() != null) this.selected = firstOf(tab());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Content ────────────────────────────────────────────────────────────

    private static List<Entry> buildEntries() {
        ItemStack e = ItemStack.EMPTY;
        ItemStack shop = new ItemStack(ModItems.SHOPFRONT.get());
        ItemStack table = new ItemStack(ModItems.PURIFICATION_TABLE.get());
        ItemStack purifiedBucket = new ItemStack(ModItems.PURIFIED_WATER_BUCKET.get());
        ItemStack purified = new ItemStack(ModItems.PURIFIED_WATER.get());
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack coal = new ItemStack(Items.COAL);
        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack wool = new ItemStack(Items.WOOL.white());
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS);
        ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);
        ItemStack ingot = new ItemStack(Items.GOLD_INGOT);
        ItemStack goldBlock = new ItemStack(Items.GOLD_BLOCK);
        ItemStack waterBottle = new ItemStack(Items.POTION);
        waterBottle.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));

        List<Entry> list = new ArrayList<>();

        // ── Crafting: Blocks ──
        list.add(new Entry(Tab.BLOCKS, shop, "Shopfront",
            HowToMake.crafting(wool, new ItemStack(Items.REDSTONE_BLOCK), wool, stick, goldBlock, stick, planks, new ItemStack(Items.CHEST), planks),
            List.of(), List.of("Your own market stall: sell items for gold, even while you're away.", "2 blocks tall. Any planks work.")));
        list.add(new Entry(Tab.BLOCKS, table, "Purification Table",
            HowToMake.crafting(iron, coal, iron, iron, new ItemStack(Items.CAULDRON), iron, stone, stone, stone),
            List.of(), List.of("Turns a Water Bucket into Purified Water.", "Burns coal or charcoal: 1 piece = 2 buckets.", "Anyone can use it.")));
        ItemStack fence = new ItemStack(Items.OAK_FENCE);
        list.add(new Entry(Tab.BLOCKS, new ItemStack(ModItems.MAILBOX.get()), "Mailbox",
            HowToMake.crafting(e, new ItemStack(Items.CHEST), e, e, fence, e, e, fence, e),
            List.of(), List.of("Put one outside your home so other players can leave you things.", "Only you can open it and take the mail out. Its red flag goes up when there's mail.", "Any wooden fence works.")));

        // ── Crafting: Items ──
        list.add(new Entry(Tab.ITEMS, purifiedBucket, "Purified Water Bucket",
            HowToMake.table("Made in the Purification Table.", new ItemStack(Items.WATER_BUCKET), coal),
            List.of(), List.of("Can't be drunk: pour it into bottles.")));
        list.add(new Entry(Tab.ITEMS, purified, "Purified Water",
            HowToMake.crafting(bottle, bottle, bottle, bottle, purifiedBucket, bottle, bottle, bottle, bottle),
            List.of(), List.of("Makes 8. Clean drinking water: +7 water drops,", "and it brings your Water Purity up to Good.")));
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        list.add(new Entry(Tab.ITEMS, new ItemStack(ModItems.COMPACTED_IRON.get()), "Compacted Iron",
            HowToMake.crafting(iron, iron, e, e, e, e, e, e, e),
            List.of(), List.of("2 Iron Ingots side by side, in any crafting grid.",
                "Without the right smith job, iron swords, axes, tools and armor need these instead of Iron Ingots (double the iron).",
                "Smiths also repair double with them.")));
        list.add(new Entry(Tab.ITEMS, new ItemStack(ModItems.COMPACTED_DIAMOND.get()), "Compacted Diamond",
            HowToMake.crafting(diamond, diamond, e, e, e, e, e, e, e),
            List.of(), List.of("2 Diamonds side by side, in any crafting grid.",
                "Without the right smith job, diamond swords, axes, tools and armor need these instead of Diamonds (double the diamonds).",
                "Or buy from a smith: it's cheaper!")));

        ItemStack rawGold = new ItemStack(Items.RAW_GOLD);
        ItemStack skull = new ItemStack(Items.SKELETON_SKULL);
        ItemStack bell = new ItemStack(Items.BELL);
        ItemStack clock = new ItemStack(Items.CLOCK);

        // ── Wiki: Gold ── (Economy.md, Insurance.md)
        list.add(new Entry(Tab.GOLD, ingot, "Gold Is Money", null, List.of(nugget, ingot, goldBlock), List.of(
            "Everything is paid in real gold: nuggets, ingots and blocks.",
            "9 nuggets = 1 ingot, 9 ingots = 1 block.")));
        list.add(new Entry(Tab.GOLD, rawGold, "Raw Gold", null, List.of(rawGold, new ItemStack(Items.FURNACE), ingot), List.of(
            "Raw Gold counts toward your wealth (1 gold each), but it can't be spent.",
            "Smelt it into ingots first.")));
        list.add(new Entry(Tab.GOLD, new ItemStack(Items.WRITABLE_BOOK), "/balance and /pay", null, List.of(ingot), List.of(
            "/balance: the gold you carry, plus all your storage.",
            "/pay <player> <amount>: give gold to an online player. Change is given automatically.")));
        list.add(new Entry(Tab.GOLD, goldBlock, "Gold Belongs to Its Owner", null, List.of(ingot), List.of(
            "Gold changes hands by /pay, Shopfronts and shops, never off the ground.",
            "Gold you drop is yours alone: nobody else can pick it up, and no hopper or mob takes it.",
            "Like any drop, it despawns after a while.")));
        list.add(new Entry(Tab.GOLD, new ItemStack(Items.CHEST), "Locked Containers", null, List.of(new ItemStack(Items.CHEST), new ItemStack(Items.FURNACE)), List.of(
            "Chests, barrels, furnaces and the like are locked to whoever placed them.",
            "Look at one to see its owner.",
            "Owner: shift + right-click with an empty hand to lock or unlock.")));
        list.add(new Entry(Tab.GOLD, shop, "Shopfronts", null, List.of(shop, new ItemStack(Items.COOKED_PORKCHOP, 64)), List.of(
            "Your own market stall, open even while you're away.",
            "Click a listing box with an item, put the real items in Storage, set the amount and price.",
            "Buyers' gold waits inside: press Collect.")));
        list.add(new Entry(Tab.GOLD, new ItemStack(Items.VILLAGER_SPAWN_EGG), "The Banker", null, List.of(new ItemStack(Items.WHEAT), ingot), List.of(
            "The Banker buys your goods for gold.",
            "Right-click him holding an item to sell one; shift + right-click sells the stack.",
            "Hold an item and type /price to see what it's worth.",
            "Crops sell cheap: a Cook pays better.")));
        list.add(new Entry(Tab.GOLD, new ItemStack(Items.GOLDEN_HELMET), "Gold Leaderboard", null, List.of(ingot), List.of(
            "The sidebar ranks everyone's net worth:",
            "gold on you, your Ender Chest, every container you own and your Shopfront earnings. Raw Gold counts too.",
            "/toggle leaderboard hides it from your screen (type it again to bring it back).")));
        list.add(new Entry(Tab.GOLD, skull, "Death Tax", null, List.of(skull), List.of(
            "When you die you owe Death a share of all your gold (25% uninsured).",
            "Until it's paid you're In Purgatory: no mining, no fighting. Pay with /pay death.",
            "With 10 gold or less to your name, Death takes pity: no tax.", "One death never costs more than 75 gold.",
            "Getting killed by a player doesn't trigger the death tax (unless in The Purge).",
            "Lower your death tax with insurance: see the Insurance tab.")));

        // ── Wiki: Insurance ── (Insurance.md; the prices live in the server config, so they're never hard-coded: the Blood Moon Insurance price comes from the server at login, ClientRules)
        ItemStack paper = new ItemStack(Items.PAPER);
        list.add(new Entry(Tab.INSURANCE, paper, "Not-A-Scam Insurance", null, List.of(paper), List.of(
            "Pay a little each day and owe less death tax when you die.",
            "It's not a scam. It says so in the name.")));
        list.add(new Entry(Tab.INSURANCE, new ItemStack(Items.WRITABLE_BOOK), "Plans and Prices", null, List.of(), List.of(
            "/insurance quotes shows today's plans: what each costs a day, and the death tax it leaves you.",
            "The pricier the plan, the less you owe.")));
        list.add(new Entry(Tab.INSURANCE, new ItemStack(Items.CLOCK), "Buying and Cancelling", null, List.of(), List.of(
            "/insurance buy <plan> starts at the next day start, never today. Switching plans works the same way.",
            "/insurance cancel ends it at the next day start; you're covered until then.")));
        list.add(new Entry(Tab.INSURANCE, ingot, "The Daily Premium", null, List.of(ingot), List.of(
            "Each morning the day's premium comes out of the gold you carry.",
            "Can't pay? Your plan is cancelled, with no debt, until you buy again.")));
        list.add(new Entry(Tab.INSURANCE, new ItemStack(Items.REDSTONE), "Blood Moons", null, List.of(), List.of(
            "During a Blood Moon no company insures anyone: plans don't cover it.",
            "Blood Moon Insurance is a one-time buy that covers the NEXT Blood Moon to start (/insurance buy BloodMoonInsurance).",
            ClientRules.bloodMoonInsurancePrice >= 0 ? "Price: " + ClientRules.bloodMoonInsurancePrice + " gold, once." : "Price: see /insurance quotes.")));
        list.add(new Entry(Tab.INSURANCE, new ItemStack(Items.IRON_SWORD), "The Purge", null, List.of(), List.of(
            "Plans do cover Purge deaths.",
            "Killed by a player in The Purge, what you owe (after your plan) goes to your killer when you pay.")));
        list.add(new Entry(Tab.INSURANCE, new ItemStack(Items.BOOK), "Status and Terms", null, List.of(), List.of(
            "/insurance status: your plan, tomorrow's change, your Blood Moon cover and what you owe.",
            "/insurance guide: the full terms.")));

        // ── Wiki: Town ── (Town.md, NPCs.md, RealEstate.md)
        list.add(new Entry(Tab.TOWN, new ItemStack(Items.SHIELD), "Safe Zone", null, List.of(new ItemStack(Items.SHIELD)), List.of(
            "The protected town center is a safe zone: no player can ever hurt another there.",
            "Nobody builds or breaks blocks there, except on a lot they own or lease.")));
        list.add(new Entry(Tab.TOWN, new ItemStack(ModItems.OVEN.get()), "Town NPCs", null, List.of(new ItemStack(ModItems.OVEN.get())), List.of(
            "When nobody with a specific job is online, a town NPC stands in, so goods are always for sale.",
            "Some days they come to town anyway, and sell alongside the real players.",
            "NPCs charge extra: real players come first.")));
        list.add(new Entry(Tab.TOWN, bell, "The Town Hall", null, List.of(bell, new ItemStack(ModItems.MAYORAL_CHEST.get())), List.of(
            "The town has a Mayor, a Vice Mayor and a Treasurer: /townhall shows who.",
            "Officials call meetings; the Mayor makes announcements and puts questions to a vote.",
            "The Mayor and Vice Mayor keep the Mayoral Chest, and the Town Hall Mailbox takes the town's mail.")));
        list.add(new Entry(Tab.TOWN, new ItemStack(Items.MAP), "The Bounty Clerk", null, List.of(), List.of(
            "Posts a task each day. Finish it for a reward,",
            "and see how you stand in his Ledger.")));
        list.add(new Entry(Tab.TOWN, new ItemStack(Items.COD), "The Fisherman", null, List.of(new ItemStack(Items.SALMON)), List.of(
            "Buys your catch.",
            "The sea holds rarer things than cod, for the patient.",
            "Enchanted rods are the only way.")));
        list.add(new Entry(Tab.TOWN, new ItemStack(ModItems.SURVEY_STAKE.get()), "Real Estate", null, List.of(), List.of(
            "The town is split into lots. Real Estate Agents buy them, then sell or lease them.",
            "A lot's owner or tenant can build on it.")));

        // ── Wiki: Events ── (Events.md, Calendar.md)
        list.add(new Entry(Tab.EVENTS, new ItemStack(Items.REDSTONE), "The Blood Moon", null, List.of(), List.of(
            "Some nights the moon rises red. A bell tolls, monsters grow stronger, every kill is counted and nobody sleeps till dawn.",
            "Something stalks the Blood Moon... and those who survive may find it worth their while.")));
        list.add(new Entry(Tab.EVENTS, new ItemStack(Items.IRON_SWORD), "The Purge", null, List.of(), List.of(
            "Announced a day ahead, with sirens. From dusk to dawn there are no laws: PvP everywhere but the town.",
            "Killed by a player out there? Your death tax goes to them.")));
        list.add(new Entry(Tab.EVENTS, clock, "Festivals", null, List.of(clock), List.of(
            "Some days on the calendar are festivals.",
            "The clock at the top right names them.")));

        // ── Wiki: Jobs ── (Jobs.md and each job's notes)
        list.add(new Entry(Tab.JOBS, new ItemStack(Items.IRON_PICKAXE), "Jobs", null, List.of(), List.of(
            "You get one Essential job and one Fun job to start.",
            "Pick intentionally: changing jobs is rarely allowed.",
            "Pick your starting jobs with /pick.")));
        list.add(new Entry(Tab.JOBS, new ItemStack(Items.GOLD_BLOCK), "Wealth Is Power", null, List.of(), List.of(
            "Every job has its perks and its downsides, so players need each other.",
            "Here money is the measure of a person: the more you earn, the more powerful you are.")));
        list.add(new Entry(Tab.JOBS, new ItemStack(Items.ANVIL), "Repairs", null, List.of(new ItemStack(Items.ANVIL)), List.of(
            "Only smiths repair gear, at their tables.",
            "The Anvil doesn't repair anything.")));

        // ── Wiki: Wellness ── (Wellness.md, Cook.md, Bartending.md, Cravings.md)
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.APPLE), "The Wellness Screen", null, List.of(new ItemStack(Items.APPLE)), List.of(
            "Open your inventory and click the apple tab on the left.",
            "Your diet, water, hearts and temperature, your buffs, and today's tasks.")));
        list.add(new Entry(Tab.WELLNESS, waterBottle, "Thirst", null, List.of(waterBottle), List.of(
            "The blue drops above your food bar go down over time, faster when sprinting or when you're Hot.",
            "Running dry hurts. Right-click a body of water with an empty hand to sip.",
            "This is unpurified water. (You can't drink salty ocean water.)")));
        list.add(new Entry(Tab.WELLNESS, purified, "Water Purity", null, List.of(waterBottle, purified), List.of(
            "Raw water can make you sick and drags your purity down; Purified Water brings it up.",
            "Only a Bartender's Spring Water brings it up to Excellent. Alcoholic drinks don't change it.")));
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.COOKED_BEEF), "Diet", null,
            List.of(new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.BREAD), new ItemStack(Items.CARROT), new ItemStack(Items.APPLE)), List.of(
            "Eat a mix: mostly protein, some carbs, veg and fruit. Food tooltips show what each adds.")));
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.GOLDEN_CARROT), "Gold Bars", null, List.of(new ItemStack(ModItems.SILVER_PLATE.get()), new ItemStack(ModItems.GOLDEN_PLATE.get())), List.of(
            "A Cook's Gourmet food overfills into golden drumsticks; a Bartender's drinks into golden drops.",
            "They sit on top of your bars and drain first, so you stay full longer.",
            "Meals eaten off a plate give the most.")));
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.SNOWBALL), "Temperature", null, List.of(new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.CAMPFIRE)), List.of(
            "The ball in the bottom right is your body heat. Hover it in your inventory to see what's warming or chilling you.",
            "Green is just right. Blue: you're Cold. A snowflake: you're Freezing and losing health, faster the colder you get.",
            "A flame: you're Hot and get thirsty faster, faster the hotter you get.",
            "Rain, cold water at night and snowy nights chill you. Snowy water chills twice as fast. Stay out long enough and Cold turns to Freezing.",
            "A fire, a lit furnace or a Fireplace in your base warms you back up. So does a night in the Crimson Bed.",
            "Each leather piece keeps some cold off; a full set keeps it all off, but bakes you somewhere hot.",
            "Swim in leather and it gets soaked: you stay wet until you dry off by a fire.",
            "The desert sun and the Nether heat you up. Water cools you off; a fire there only makes it worse.",
            "A Cook's Campfire Chili keeps you warm; Frosty Melon Sorbet keeps you cool.")));
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.GOLDEN_APPLE), "Bonus Hearts", null, List.of(), List.of(
            "A Balanced diet and Good water for a while: +1 heart.",
            "With Excellent water: +2.",
            "Neglect yourself and you can lose a heart.")));
        list.add(new Entry(Tab.WELLNESS, new ItemStack(Items.CAKE), "Daily Cravings", null, List.of(), List.of(
            "Every morning you crave your own food and drink.",
            "Satisfy one and that bar fills right up to gold, with a point on the Cravings standings.",
            "See Wellness > Tasks.")));        return list;
    }

    private @Nullable Tab tab() {
        return switch (this.section) {
            case START, RULES -> null;
            case WIKI -> this.wikiTab;
            case CRAFTING -> this.craftingTab;
        };
    }

    private List<Entry> inTab(@Nullable Tab tab) {
        return this.entries.stream().filter(entry -> entry.tab() == tab).toList();
    }

    private @Nullable Entry firstOf(@Nullable Tab tab) {
        List<Entry> list = inTab(tab);
        return list.isEmpty() ? null : list.get(0);
    }

    // entries on one page: the Crafting grid, or the Wiki list (one row less if it needs the page arrows)
    private int perPage() {
        if (this.section != Section.WIKI) return COLS * ROWS;
        return inTab(tab()).size() <= LIST_ROWS ? LIST_ROWS : LIST_ROWS - 1;
    }

    private int pageCount() {
        return Math.max(1, (inTab(tab()).size() + perPage() - 1) / perPage());
    }

    // where the page arrows sit: under the grid, or in the list's last row
    private int pagerY() {
        return this.top + GRID_Y + (this.section == Section.WIKI ? (LIST_ROWS - 1) * ROW_H + 2 : ROWS * CELL + 2);
    }

    private int pagerW() {
        return this.section == Section.WIKI ? LIST_W : COLS * CELL;
    }

    // a Wiki tab's left edge and width (words, side by side)
    private int wikiTabX(List<Tab> tabs, int i) {
        int x = this.left + 8;
        for (int j = 0; j < i; j++) x += wikiTabW(tabs.get(j)) + 3;
        return x;
    }

    private int wikiTabW(Tab tab) {
        return this.font.width(tab.title) + 12;
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    // Getting Started and Rules are plain text pages (no tabs, no entry grid)
    private boolean textPage() {
        return this.section == Section.START || this.section == Section.RULES;
    }

    // The section buttons along the top: Getting Started is wider (its name is long), the rest share what's left
    private static final int START_W = 112, SECTION_GAP = 4;

    private int sectionW(int i) {
        if (i == 0) return START_W;
        int rest = Section.values().length - 1;
        return (W - 16 - START_W - SECTION_GAP * rest) / rest;
    }

    private int sectionX(int i) {
        int x = this.left + 8;
        for (int j = 0; j < i; j++) x += sectionW(j) + SECTION_GAP;
        return x;
    }

    // ── Getting Started (Please Read): the first thing anyone sees, a few pages stepped through with Back / Next.
    // The admin's own words. A line starting "- " is a bullet, "-> " a smaller bullet under the one before, "1) " a
    // numbered point; icons (if any) are shown under the title with their names.
    private record StartPage(String title, List<String> lines, List<Supplier<ItemStack>> icons, List<Supplier<StartRecipe>> recipes) {
        StartPage(String title, List<String> lines, List<Supplier<ItemStack>> icons) {
            this(title, lines, icons, List.of());
        }
    }

    // a crafting recipe shown on a Getting Started page: its name above, the 3x3 grid, an arrow and what it makes
    private record StartRecipe(String name, ItemStack[] grid, ItemStack result) {
    }

    private static StartRecipe compacted(net.minecraft.world.item.Item material, ItemStack result) {
        ItemStack m = new ItemStack(material), e = ItemStack.EMPTY;
        return new StartRecipe(result.getHoverName().getString(), new ItemStack[] {m, m, e, e, e, e, e, e, e}, result);
    }

    private static StartRecipe purificationTable() {
        ItemStack i = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT), c = new ItemStack(net.minecraft.world.item.Items.COAL),
            u = new ItemStack(net.minecraft.world.item.Items.CAULDRON), s = new ItemStack(net.minecraft.world.item.Items.STONE);
        ItemStack table = new ItemStack(ModItems.PURIFICATION_TABLE.get());
        return new StartRecipe(table.getHoverName().getString(), new ItemStack[] {i, c, i, i, u, i, s, s, s}, table);
    }

    // 8 glass bottles around a Purified Water Bucket = 8 Purified Water
    private static StartRecipe purifiedWater() {
        ItemStack b = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE), bucket = new ItemStack(ModItems.PURIFIED_WATER_BUCKET.get());
        ItemStack water = new ItemStack(ModItems.PURIFIED_WATER.get(), 8);
        return new StartRecipe(water.getHoverName().getString(), new ItemStack[] {b, b, b, b, bucket, b, b, b, b}, water);
    }

    private static final int RECIPE_W = 54 + 18 + 18, RECIPE_H = 10 + 54;

    private void drawStartRecipe(GuiGraphicsExtractor graphics, StartRecipe recipe, int x, int y, int mouseX, int mouseY) {
        graphics.text(this.font, recipe.name(), x, y, TEXT, false);
        int gy = y + 10;
        for (int i = 0; i < 9; i++) itemSlot(graphics, recipe.grid()[i], x + (i % 3) * 18, gy + (i / 3) * 18, mouseX, mouseY);
        graphics.text(this.font, "->", x + 57, gy + 23, TEXT, false);
        itemSlot(graphics, recipe.result(), x + 72, gy + 18, mouseX, mouseY);
    }

    private static final List<StartPage> GETTING_STARTED = List.of(
        new StartPage("Welcome To Project Capitalism 2", List.of(
            "- THIS MOD is not made for the faint of heart. IT IS HARD. I tried to make Minecraft more difficult and teamwork oriented. Do not bitch and whine about dying or the rules.",
            "- Use gold as currency. Please use it and don't hoard it. It will significantly help your adventure.",
            "- PvP is allowed... but allies are recommended.",
            "- The Main Town is a safe place."), List.of()),
        new StartPage("Take It Slow", List.of(
            "- Take it super slow. Focus on getting food, a house and Purified Water first.",
            "- Then go for tools and armor.",
            "- MOBS are way harder in this mode.",
            "- Then go for a big cave adventure. (Trust me, stock up for an adventure.)"), List.of()),
        new StartPage("Need To Know Items!", List.of(
            "- Compacted Diamond and Compacted Iron are how you make tools, weapons and armor.",
            "- Regular vanilla crafts don't work for pickaxes, swords and other tools. (Unless your job says otherwise.)"),
            List.of(), List.of(() -> compacted(net.minecraft.world.item.Items.DIAMOND, new ItemStack(ModItems.COMPACTED_DIAMOND.get())),
                () -> compacted(net.minecraft.world.item.Items.IRON_INGOT, new ItemStack(ModItems.COMPACTED_IRON.get())))),
        new StartPage("Water", List.of(
            "- Water is like food. When it gets low you can't run, and you get hurt.",
            "- You can drink from your hand in streams, rivers and ponds (source water blocks).",
            "- Purify water: put a Bucket of Water and coal in a Purification Table, then craft the Purified Water Bucket into 8 bottles."),
            List.of(), List.of(GeneralGuideScreen::purificationTable, GeneralGuideScreen::purifiedWater)),
        new StartPage("Temperature", List.of(
            "- The bottom right of your screen is your Body Temperature.",
            "-> Try to stay around Normal (70 degrees, green).",
            "-> Cold or hot biomes, being in water, and rain or snow affect it.",
            "- Be near a fire to warm up... or jump in water to cool down."), List.of()),
        new StartPage("Player Debuffs", List.of(
            "- All players (except some jobs) have these debuffs:",
            "1) It takes more resources to make tools and armor.",
            "2) Harvesting crops gives you at most 1 grown crop and 1 seed.",
            "3) Killing animals gives you 1 meat.",
            "4) Food not cooked by a Cook is weaker."), List.of()),
        new StartPage("Use People's Abilities", List.of(
            "- Each job has strengths... Use their shops as a source to survive easier! If you do not use other people's services, it WILL BE TOUGH."), List.of()),
        new StartPage("Character Tab", List.of(
            "- In your inventory, click the apple tab on the left side. This is your character's statistics and wellbeing."),
            List.of(() -> new ItemStack(net.minecraft.world.item.Items.APPLE))));

    private int startPage;

    // the Back / Next buttons and the page count along the bottom of the panel
    private static final int START_BUTTON_W = 50, START_BUTTON_H = 14;

    private int startButtonY() {
        return panelBottom() - START_BUTTON_H - 4;
    }

    private int startBackX() {
        return this.left + 16;
    }

    private int startNextX() {
        return this.left + W - 16 - START_BUTTON_W;
    }

    private void drawStart(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left, x0 = l + 8, top = panelTop(), bottom = panelBottom();
        graphics.fill(x0, top, l + W - 8, bottom, PANEL_DARK);
        StartPage page = GETTING_STARTED.get(this.startPage);
        int x = x0 + 8, wrap = W - 36;
        graphics.text(this.font, page.title(), x, top + 6, GOLD, false);
        int y = top + 20;
        // the icons, with their names
        if (!page.icons().isEmpty()) {
            int ix = x;
            for (Supplier<ItemStack> icon : page.icons()) {
                ItemStack stack = icon.get();
                itemSlot(graphics, stack, ix, y, mouseX, mouseY);
                String name = stack.getHoverName().getString();
                graphics.text(this.font, name, ix + 21, y + 5, TEXT, false);
                ix += 21 + this.font.width(name) + 12;
            }
            y += 24;
        }
        // the recipes, side by side
        if (!page.recipes().isEmpty()) {
            int rx = x;
            for (Supplier<StartRecipe> recipe : page.recipes()) {
                drawStartRecipe(graphics, recipe.get(), rx, y, mouseX, mouseY);
                rx += RECIPE_W + 30;
            }
            y += RECIPE_H + 8;
        }
        List<Line> lines = new ArrayList<>();
        for (String raw : page.lines()) {
            int base;
            String marker;
            String text;
            if (raw.startsWith("-> ")) {
                base = 12;
                marker = "- ";
                text = raw.substring(3);
            } else if (raw.startsWith("- ")) {
                base = 0;
                marker = "• ";
                text = raw.substring(2);
            } else if (raw.length() > 2 && Character.isDigit(raw.charAt(0)) && raw.charAt(1) == ')') {
                base = 12;
                marker = raw.substring(0, 3);
                text = raw.substring(3);
            } else {
                base = 0;
                marker = "";
                text = raw;
            }
            int indent = base + this.font.width(marker);
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(text), wrap - indent);
            for (int j = 0; j < wrapped.size(); j++) {
                if (j == 0 && !marker.isEmpty()) lines.add(new Line(FormattedCharSequence.forward(marker, net.minecraft.network.chat.Style.EMPTY), -1 - base, GOLD, 0));
                lines.add(new Line(wrapped.get(j), indent, TEXT, j == wrapped.size() - 1 ? 13 : 10));
            }
        }
        drawLines(graphics, lines, x, y, startButtonY() - 4);
        // Back / page n of N / Next
        int by = startButtonY();
        boolean canBack = this.startPage > 0, canNext = this.startPage < GETTING_STARTED.size() - 1;
        startButton(graphics, startBackX(), by, "< Back", canBack, mouseX, mouseY);
        startButton(graphics, startNextX(), by, "Next >", canNext, mouseX, mouseY);
        graphics.centeredText(this.font, "Page " + (this.startPage + 1) + " of " + GETTING_STARTED.size(), l + W / 2, by + 3, TEXT_DIM);
    }

    private void startButton(GuiGraphicsExtractor graphics, int x, int y, String label, boolean enabled, int mouseX, int mouseY) {
        boolean over = enabled && inside(mouseX, mouseY, x, y, START_BUTTON_W, START_BUTTON_H);
        graphics.fill(x, y, x + START_BUTTON_W, y + START_BUTTON_H, over ? SELECTED : SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + START_BUTTON_W - 1, y + START_BUTTON_H - 1, SLOT);
        graphics.centeredText(this.font, label, x + START_BUTTON_W / 2, y + 3, !enabled ? 0xFF5A5A5A : over ? GOLD : TEXT);
    }

    // a click on Back / Next
    private boolean clickStart(int mx, int my) {
        int by = startButtonY();
        if (this.startPage > 0 && inside(mx, my, startBackX(), by, START_BUTTON_W, START_BUTTON_H)) {
            this.startPage--;
        } else if (this.startPage < GETTING_STARTED.size() - 1 && inside(mx, my, startNextX(), by, START_BUTTON_W, START_BUTTON_H)) {
            this.startPage++;
        } else {
            return false;
        }
        this.scroll = 0;
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
        return true;
    }

    // the panel the entry (or the rules) is drawn in: its top and bottom on screen
    private int panelTop() {
        return this.top + (textPage() ? TAB_Y : DETAIL_Y);
    }

    private int panelBottom() {
        return this.top + H - 6;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.hovered = null;
        int l = this.left;
        int t = this.top;

        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + W, t + H, PANEL);
        graphics.centeredText(this.font, this.title, l + W / 2, t + 6, GOLD);

        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            int x = sectionX(i), sw = sectionW(i);
            boolean active = sections[i] == this.section;
            graphics.fill(x, t + SECTION_Y, x + sw, t + SECTION_Y + 14, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, t + SECTION_Y + 1, x + sw - 1, t + SECTION_Y + 13, active ? PANEL_DARK : SLOT);
            graphics.centeredText(this.font, sections[i].title, x + sw / 2, t + SECTION_Y + 3, active ? GOLD : TEXT_DIM);
        }

        if (this.section == Section.START) {
            drawStart(graphics, mouseX, mouseY);
            return;
        }
        if (this.section == Section.RULES) {
            drawRules(graphics);
            return;
        }

        // tabs
        List<Tab> tabs = Tab.in(this.section);
        Tab current = tab();
        if (this.section == Section.WIKI) {
            drawWiki(graphics, tabs, current, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < tabs.size(); i++) {
            int x = l + 8 + i * 30;
            int y = t + TAB_Y;
            boolean active = tabs.get(i) == current;
            graphics.fill(x, y, x + 26, y + 22, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + 25, y + 21, active ? PANEL_DARK : SLOT);
            graphics.item(tabs.get(i).icon(), x + 5, y + 3);
            if (inside(mouseX, mouseY, x, y, 26, 22)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(tabs.get(i).title)), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        if (current != null) graphics.text(this.font, current.title, l + 8 + tabs.size() * 30 + 6, t + TAB_Y + 7, TEXT, false);

        // grid
        List<Entry> list = inTab(current);
        int start = this.page * COLS * ROWS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + GRID_X + (i % COLS) * CELL;
            int y = t + GRID_Y + (i / COLS) * CELL;
            int index = start + i;
            Entry entry = index < list.size() ? list.get(index) : null;
            slot(graphics, x, y, entry != null && entry == this.selected);
            if (entry == null) continue;
            graphics.item(entry.icon(), x + 1, y + 1);
            if (inside(mouseX, mouseY, x, y, 18, 18)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(entry.title())), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        drawPager(graphics);
        drawDetails(graphics, mouseX, mouseY);
    }

    private void drawPager(GuiGraphicsExtractor graphics) {
        if (pageCount() <= 1) return;
        int l = this.left, ny = pagerY();
        graphics.text(this.font, "<", l + GRID_X + 2, ny, this.page > 0 ? TEXT : TEXT_DIM, false);
        graphics.centeredText(this.font, (this.page + 1) + "/" + pageCount(), l + GRID_X + pagerW() / 2, ny, TEXT);
        graphics.text(this.font, ">", l + GRID_X + pagerW() - 8, ny, this.page < pageCount() - 1 ? TEXT : TEXT_DIM, false);
    }

    private void drawDetails(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int l = this.left, t = this.top;
        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, PANEL_DARK);
        if (this.selected != null) drawEntry(graphics, this.selected, mouseX, mouseY);
        if (this.hovered != null && !this.hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, this.hovered, mouseX, mouseY);
        }
    }

    // the Wiki: a row of word tabs, a list of entry titles on the left, the entry on the right
    private void drawWiki(GuiGraphicsExtractor graphics, List<Tab> tabs, @Nullable Tab current, int mouseX, int mouseY) {
        int l = this.left, t = this.top;
        for (int i = 0; i < tabs.size(); i++) {
            int x = wikiTabX(tabs, i), w = wikiTabW(tabs.get(i)), y = t + TAB_Y;
            boolean active = tabs.get(i) == current;
            boolean over = inside(mouseX, mouseY, x, y, w, 16);
            graphics.fill(x, y, x + w, y + 16, active ? SELECTED : SLOT_EDGE);
            graphics.fill(x + 1, y + 1, x + w - 1, y + 15, active ? PANEL_DARK : SLOT);
            graphics.centeredText(this.font, tabs.get(i).title, x + w / 2, y + 4, active ? GOLD : over ? TEXT : TEXT_DIM);
        }
        int x0 = l + GRID_X;
        graphics.fill(x0, t + GRID_Y, x0 + LIST_W, t + H - 6, PANEL_DARK);
        List<Entry> list = inTab(current);
        int start = this.page * perPage();
        for (int i = 0; i < perPage() && start + i < list.size(); i++) {
            Entry entry = list.get(start + i);
            int y = t + GRID_Y + i * ROW_H;
            boolean active = entry == this.selected;
            boolean over = inside(mouseX, mouseY, x0, y, LIST_W, ROW_H);
            if (active) {
                graphics.fill(x0, y, x0 + LIST_W, y + ROW_H, SELECTED);
                graphics.fill(x0 + 1, y + 1, x0 + LIST_W - 1, y + ROW_H - 1, SLOT);
            } else if (over) {
                graphics.fill(x0, y, x0 + LIST_W, y + ROW_H, SLOT);
            }
            String title = entry.title();
            if (this.font.width(title) > LIST_W - 8) title = this.font.plainSubstrByWidth(title, LIST_W - 14) + "...";
            graphics.text(this.font, title, x0 + 4, y + 2, active ? GOLD : over ? TEXT : TEXT_DIM, false);
        }
        drawPager(graphics);
        drawDetails(graphics, mouseX, mouseY);
    }

    // the rules, numbered, in one wide panel (scrolls if the admin wrote a lot)
    private void drawRules(GuiGraphicsExtractor graphics) {
        int l = this.left, x0 = l + 8, top = panelTop(), bottom = panelBottom();
        graphics.fill(x0, top, l + W - 8, bottom, PANEL_DARK);
        int x = x0 + 8, wrap = W - 36;
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(FormattedCharSequence.forward("Server Rules (set by the admins)", net.minecraft.network.chat.Style.EMPTY), 0, GOLD, 14));
        List<String> rules = ClientRules.rules;
        for (int i = 0; i < rules.size(); i++) {
            String number = (i + 1) + ". ";
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(rules.get(i)), wrap - this.font.width(number));
            for (int j = 0; j < wrapped.size(); j++) {
                if (j == 0) lines.add(new Line(FormattedCharSequence.forward(number, net.minecraft.network.chat.Style.EMPTY), -1, GOLD, 0));
                lines.add(new Line(wrapped.get(j), this.font.width(number), TEXT, j == wrapped.size() - 1 ? 13 : 10));
            }
        }
        drawLines(graphics, lines, x, top + 6, bottom - 4);
    }

    // a line of text: indented this far (-1: drawn at the left with the next line, a rule's number), its colour, and the
    // space after it
    private record Line(FormattedCharSequence text, int indent, int color, int after) {
    }

    private int contentHeight;

    // draws the lines from y, scrolled, only those inside [y, bottom); remembers how tall they are (for scrolling)
    private void drawLines(GuiGraphicsExtractor graphics, List<Line> lines, int x, int y, int bottom) {
        int total = 0;
        for (Line line : lines) total += line.after;
        // (the last line needs only its own height, not the gap after it)
        if (!lines.isEmpty()) total += 9 - lines.get(lines.size() - 1).after;
        this.contentHeight = total;
        int room = bottom - y;
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, total - room));
        int ly = y - this.scroll;
        for (Line line : lines) {
            // (indent -1 - n: a marker drawn n pixels in, with the next line)
            int indent = line.indent < 0 ? -1 - line.indent : line.indent;
            if (ly >= y && ly + 9 <= bottom) graphics.text(this.font, line.text, x + indent, ly, line.color, false);
            ly += line.after;
        }
        if (total > room) {
            String more = this.scroll < total - room ? "v" : "^";
            graphics.text(this.font, more, this.left + W - 16, bottom - 9, TEXT_DIM, false);
        }
    }

    private void drawEntry(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
        int x = this.left + DETAIL_X + 6;
        int y = this.top + DETAIL_Y + 6;
        int wrap = W - DETAIL_X - 18;
        graphics.text(this.font, entry.title(), x, y, GOLD, false);
        int ty = y + 14;
        HowToMake how = entry.how();
        if (how != null) {
            graphics.text(this.font, "How to make:", x, ty, TEXT_DIM, false);
            ty += 11;
            if (how.grid() != null) {
                for (int i = 0; i < 9; i++) itemSlot(graphics, how.gridAt(i, net.minecraft.util.Util.getMillis() / 1000L), x + (i % 3) * 18, ty + (i / 3) * 18, mouseX, mouseY);
                graphics.text(this.font, "->", x + 59, ty + 23, TEXT, false);
                itemSlot(graphics, entry.icon(), x + 74, ty + 18, mouseX, mouseY);
                ty += 60;
            } else {
                if (how.table()) {
                    // like the smith guides: what goes in, an arrow and the result; the note says which table
                    for (int i = 0; i < how.steps().size(); i++) itemSlot(graphics, how.steps().get(i), x + i * 20, ty, mouseX, mouseY);
                    int ax = x + how.steps().size() * 20;
                    graphics.text(this.font, "->", ax + 2, ty + 5, TEXT, false);
                    itemSlot(graphics, entry.icon(), ax + 16, ty, mouseX, mouseY);
                } else {
                    for (int i = 0; i < how.steps().size(); i++) itemSlot(graphics, how.steps().get(i), x + i * 20, ty, mouseX, mouseY);
                }
                ty += 22;
                for (FormattedCharSequence line : this.font.split(Component.literal(how.note()), wrap)) {
                    graphics.text(this.font, line, x, ty, TEXT_DIM, false);
                    ty += 10;
                }
                ty += 4;
            }
        } else if (!entry.pictures().isEmpty()) {
            for (int i = 0; i < entry.pictures().size(); i++) itemSlot(graphics, entry.pictures().get(i), x + i * 20, ty, mouseX, mouseY);
            ty += 24;
        }
        List<Line> lines = new ArrayList<>();
        for (String text : entry.lines()) {
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(text), wrap);
            for (int j = 0; j < wrapped.size(); j++) lines.add(new Line(wrapped.get(j), 0, TEXT, j == wrapped.size() - 1 ? 12 : 10));
        }
        drawLines(graphics, lines, x, ty, panelBottom() - 3);
    }

    // (dev: client/PhotoShoot) the wiki entries too long for their panel (they'd need scrolling): "title (height / room)"
    public List<String> overflows() {
        List<String> out = new ArrayList<>();
        int wrap = W - DETAIL_X - 18;
        int room = (H - 9) - (DETAIL_Y + 6);
        for (Entry entry : this.entries) {
            if (entry.how() != null) continue;
            int h = 14 + (entry.pictures().isEmpty() ? 0 : 24);
            for (String text : entry.lines()) h += this.font.split(Component.literal(text), wrap).size() * 10 + 2;
            if (h > room) out.add(entry.title() + " (" + h + " / " + room + ")");
        }
        return out;
    }

    // (dev: client/PhotoShoot) show this area and tab (null: the area's first), and this entry of it
    public void show(String section, @Nullable String tab, int entry) {
        this.section = Section.valueOf(section);
        if (this.section == Section.START) this.startPage = Math.min(entry, GETTING_STARTED.size() - 1);
        if (tab != null) {
            if (this.section == Section.WIKI) this.wikiTab = Tab.valueOf(tab);
            else if (this.section == Section.CRAFTING) this.craftingTab = Tab.valueOf(tab);
        }
        this.page = 0;
        this.scroll = 0;
        List<Entry> list = inTab(tab());
        this.selected = list.isEmpty() ? null : list.get(Math.min(entry, list.size() - 1));
    }

    // (dev) how many entries this area's tab has
    public int entryCount() {
        return inTab(tab()).size();
    }

    private void itemSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        slot(graphics, x, y, false);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        if (inside(mouseX, mouseY, x, y, 18, 18)) this.hovered = stack;
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
        graphics.fill(x, y, x + 18, y + 18, selected ? SELECTED : SLOT_EDGE);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    // ── Clicking and scrolling ─────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Math.max(0, this.scroll - (int) (scrollY * 12));
        return true;
    }

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
                this.page = 0;
                this.scroll = 0;
                this.selected = firstOf(tab());
                return true;
            }
        }
        if (this.section == Section.START && clickStart(mx, my)) return true;
        if (textPage()) return super.mouseClicked(event, doubleClick);

        List<Tab> tabs = Tab.in(this.section);
        boolean wiki = this.section == Section.WIKI;
        for (int i = 0; i < tabs.size(); i++) {
            boolean hit = wiki ? inside(mx, my, wikiTabX(tabs, i), t + TAB_Y, wikiTabW(tabs.get(i)), 16)
                : inside(mx, my, l + 8 + i * 30, t + TAB_Y, 26, 22);
            if (hit) {
                if (this.section == Section.WIKI) this.wikiTab = tabs.get(i);
                else this.craftingTab = tabs.get(i);
                this.page = 0;
                this.scroll = 0;
                this.selected = firstOf(tab());
                return true;
            }
        }

        List<Entry> list = inTab(tab());
        for (int i = 0; i < perPage(); i++) {
            int index = this.page * perPage() + i;
            if (index >= list.size()) break;
            boolean hit = wiki ? inside(mx, my, l + GRID_X, t + GRID_Y + i * ROW_H, LIST_W, ROW_H)
                : inside(mx, my, l + GRID_X + (i % COLS) * CELL, t + GRID_Y + (i / COLS) * CELL, 18, 18);
            if (hit) {
                this.selected = list.get(index);
                this.scroll = 0;
                return true;
            }
        }

        int ny = pagerY();
        if (inside(mx, my, l + GRID_X, ny - 2, 12, 12) && this.page > 0) {
            this.page--;
            return true;
        }
        if (inside(mx, my, l + GRID_X + pagerW() - 10, ny - 2, 12, 12) && this.page < pageCount() - 1) {
            this.page++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
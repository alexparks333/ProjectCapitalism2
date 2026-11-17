package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.player.ClientThirstData;
import com.alexh.capitalism.player.ClientWellnessData;
import com.alexh.capitalism.player.DietData;
import com.alexh.capitalism.player.WaterPurity;
import com.alexh.capitalism.player.WellbeingData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.SkillBookItem;
import com.alexh.capitalism.librarian.Skills;
import com.alexh.capitalism.librarian.Traits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import java.util.Comparator;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// The Wellness screen (opened from the tab next to the inventory).
//  Far left:    Water Purity tube (Low / Medium / Good / Excellent).
//  Left card:   Diet donut chart (Vegetables / Fruit / Protein / Carbs, like Eco), balance in the middle,
//               and a badge saying what your diet is doing for you.
//  Right cards: food groups with icons and bars; how you're doing as real hearts / drumsticks / water drops
//               (bonus slots gold, lost ones blacked out) plus the Wellbeing bar; temperature.
// A second page (tabs at the top left), Buffs & Debuffs: everything on you right now, in three cards: Traits (from
// skill books, for good), Lessons (skill book effects, with the time left: they're hidden from the vanilla effect
// icons) and Effects (every other effect, good green / bad red). Hover a row for what it does.
public class WellnessScreen extends Screen {

    private static final int W = 380;
    private static final int H = 240;

    // Soft leafy palette
    private static final int BG_TOP = 0xFF3F6B3C;
    private static final int BG_BOTTOM = 0xFF22391F;
    private static final int FRAME_LIGHT = 0xFFB6E39A;
    private static final int FRAME_DARK = 0xFF1A2B17;
    private static final int CARD = 0xFF172415;
    private static final int CARD_EDGE = 0xFF3E6A38;
    private static final int TEXT = 0xFFF1F8E9;
    private static final int TEXT_DIM = 0xFFA9C29F;
    private static final int GOOD = 0xFF8AF09A;
    private static final int OKAY = 0xFFFFD86A;
    private static final int BAD = 0xFFFF8078;
    private static final int RING_OUTLINE = 0xFF0C140B;
    private static final int RING_EMPTY = 0xFF33452F;
    private static final int BAR_BG = 0xFF0E170D;
    private static final int GOLD = 0xFFFFD24A;
    // Water Purity bands: Low, Medium, Good, Excellent
    private static final int[] PURITY_COLORS = {0xFFE0605A, 0xFFE8A64E, 0xFF4FB8F0, 0xFF9CF4FF};

    // Vegetables, Fruit, Protein, Carbs
    private static final int[] GROUP_COLORS = {0xFF5DCB63, 0xFFF0607A, 0xFFE8944E, 0xFFF2D35E};

    // A food group's color (RGB), also used by food tooltips.
    public static int groupColor(int group) {
        return GROUP_COLORS[group] & 0xFFFFFF;
    }
    private static final ItemStack[] GROUP_ICONS = {
        new ItemStack(Items.CARROT), new ItemStack(Items.APPLE), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.BREAD)
    };

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier HEART_GOLD_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
    private static final Identifier HEART_GOLD_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_GOLD_FULL = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/food_full_gold");
    private static final Identifier FOOD_GOLD_HALF = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/food_half_gold");
    private static final Identifier WATER_EMPTY = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_empty");
    private static final Identifier WATER_FULL = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full");
    private static final Identifier WATER_HALF = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half");
    private static final Identifier WATER_GOLD_FULL = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_full_gold");
    private static final Identifier WATER_GOLD_HALF = Identifier.fromNamespaceAndPath(Capitalism.MODID, "hud/thirst_half_gold");

    private static final int PIE_RADIUS = 44;
    private static final int PIE_HOLE = 24;

    private static final int ROW_H = 22;
    private static final int ROWS = 8;

    private int left;
    private int top;
    // 0 = Wellness, 1 = Buffs & Debuffs, 2 = Tasks
    private int page;
    // scroll of the three Buffs & Debuffs cards (Traits, Lessons, Effects)
    private final int[] scroll = new int[3];
    // The pie slice or Temperature card under the mouse this frame (the only tooltips besides the (i)).
    private @Nullable List<Component> hoverTooltip;

    public WellnessScreen() {
        super(Component.literal("Wellness"));
    }

    // Esc goes back to the inventory, where the tab is.
    @Override
    public void onClose() {
        if (this.minecraft.player != null) {
            this.minecraft.gui.setScreen(new InventoryScreen(this.minecraft.player));
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int l = this.left;
        int t = this.top;

        // Frame + soft gradient background
        rounded(graphics, l - 2, t - 2, l + W + 2, t + H + 2, FRAME_DARK);
        rounded(graphics, l - 1, t - 1, l + W + 1, t + H + 1, FRAME_LIGHT);
        graphics.fillGradient(l, t, l + W, t + H, BG_TOP, BG_BOTTOM);
        graphics.fill(l + 1, t + 1, l + W - 1, t + 2, 0x40FFFFFF);

        // Title with an apple
        int titleWidth = this.font.width(this.title) + 20;
        // (on the right: the page tabs fill the left and would run into it in the middle)
        graphics.item(new ItemStack(Items.APPLE), l + W - titleWidth - 8, t + 3);
        graphics.text(this.font, this.title, l + W - titleWidth + 12, t + 8, TEXT, true);

        drawPageTabs(graphics, mouseX, mouseY);
        if (this.page == 2) {
            this.hoverTooltip = null;
            drawTasksPage(graphics, mouseX, mouseY);
            if (this.hoverTooltip != null) {
                graphics.setComponentTooltipForNextFrame(this.font, this.hoverTooltip, mouseX, mouseY, ItemStack.EMPTY);
            }
            return;
        }
        if (this.page == 1) {
            this.hoverTooltip = null;
            drawBuffsPage(graphics, mouseX, mouseY);
            if (this.hoverTooltip != null) {
                graphics.setComponentTooltipForNextFrame(this.font, this.hoverTooltip, mouseX, mouseY, ItemStack.EMPTY);
            }
            return;
        }

        float[] diet = ClientWellnessData.diet;
        float total = DietData.total(diet);
        float balance = DietData.balance(diet);
        // No buffs or debuffs until the player has eaten enough meals (the head start alone gives nothing).
        // The diet in effect (server-side: after the 20 meals and the grace period)
        // (Before the first meals nothing is in effect yet; then just show how the chart looks.)
        DietData.Tier tier = ClientWellnessData.effectsActive() ? ClientWellnessData.dietTier() : DietData.tier(diet);

        drawPurityCard(graphics, l + 8, t + 26, l + 80, t + H - 8);
        this.hoverTooltip = null;
        drawDietCard(graphics, l + 86, t + 26, l + 224, t + H - 8, diet, total, balance, tier, mouseX, mouseY);
        drawGroupsCard(graphics, l + 230, t + 26, l + W - 8, t + 104, diet, total);
        drawStatusCard(graphics, l + 230, t + 110, l + W - 8, t + 184);
        drawTemperatureCard(graphics, l + 230, t + 190, l + W - 8, t + H - 8, mouseX, mouseY);

        // Tooltips: the (i) explains everything; hovering a pie slice names it, hovering Temperature explains it.
        List<Component> info = drawInfoButton(graphics, l + W - 19, t + 6, mouseX, mouseY);
        if (info != null) {
            graphics.setComponentTooltipForNextFrame(this.font, info, mouseX, mouseY, ItemStack.EMPTY);
        } else if (this.hoverTooltip != null) {
            graphics.setComponentTooltipForNextFrame(this.font, this.hoverTooltip, mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    // A little round "i" in the top right. Hovering it explains the diet, water and bonus hearts.
    private @Nullable List<Component> drawInfoButton(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, 13, 13);
        int ring = hover ? 0xFFFFFFFF : FRAME_LIGHT;
        int fill = hover ? 0xFF4F8A48 : 0xFF2C4A28;
        // a 13x13 circle-ish badge
        graphics.fill(x + 4, y, x + 9, y + 13, ring);
        graphics.fill(x + 2, y + 1, x + 11, y + 12, ring);
        graphics.fill(x + 1, y + 2, x + 12, y + 11, ring);
        graphics.fill(x, y + 4, x + 13, y + 9, ring);
        graphics.fill(x + 4, y + 1, x + 9, y + 12, fill);
        graphics.fill(x + 2, y + 2, x + 11, y + 11, fill);
        graphics.fill(x + 1, y + 4, x + 12, y + 9, fill);
        // the "i"
        graphics.fill(x + 6, y + 3, x + 7, y + 4, 0xFFFFFFFF);
        graphics.fill(x + 6, y + 5, x + 7, y + 10, 0xFFFFFFFF);
        graphics.fill(x + 5, y + 5, x + 6, y + 6, 0xFFFFFFFF);
        graphics.fill(x + 5, y + 9, x + 8, y + 10, 0xFFFFFFFF);
        if (!hover) return null;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("The Perfect Diet").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.literal("65% Protein").withStyle(style -> style.withColor(GROUP_COLORS[DietData.PROTEIN] & 0xFFFFFF))
            .append(Component.literal(" - meat & fish").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal("15% Carbs").withStyle(style -> style.withColor(GROUP_COLORS[DietData.CARBS] & 0xFFFFFF))
            .append(Component.literal(" - bread, potatoes, cookies").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal("10% Vegetables").withStyle(style -> style.withColor(GROUP_COLORS[DietData.VEGETABLE] & 0xFFFFFF))
            .append(Component.literal(" - carrots, beetroot, kelp").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal("10% Fruit").withStyle(style -> style.withColor(GROUP_COLORS[DietData.FRUIT] & 0xFFFFFF))
            .append(Component.literal(" - apples, melon, berries").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal("White ticks = where each group should be.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Hover a food to see its diet points.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Diet: ").withStyle(ChatFormatting.GREEN)
            .append(Component.literal("Balanced " + pct(CapitalismConfig.DIET_BALANCED_SCORE, 86) + "%+ = +" + drumsticks(CapitalismConfig.DIET_BALANCED_FOOD_BONUS.get())
                + ", Poor " + pct(CapitalismConfig.DIET_POOR_SCORE, 44) + "% or less = -" + drumsticks(CapitalismConfig.DIET_POOR_FOOD_LOSS.get())).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.empty());
        lines.add(Component.literal("Water Purity").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        lines.add(Component.literal("Purified Water keeps you at Good. Lakes and").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("water bottles drag it down. Bartenders sell better.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Water: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal("Excellent " + WaterPurity.EXCELLENT_AT + "%+ = +" + drops(CapitalismConfig.PURITY_EXCELLENT_WATER_BONUS.get())
                + ", Low = -" + drops(CapitalismConfig.PURITY_LOW_WATER_LOSS.get())).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.empty());
        lines.add(Component.literal("Bonus Hearts").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.literal(hearts(CapitalismConfig.WELLBEING_HEALTH, "+") + ": ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("Balanced diet + Good water").withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(hearts(configValue(CapitalismConfig.WELLBEING_HEALTH, 2.0) * 2, "+") + ": ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("the same, with Excellent water").withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Keep it up and the marker fills the gold zone.").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    // ── Pages: Wellness | Buffs/Debuffs ────────────────────────────────────

    private static final String[] PAGES = {"Wellness", "Buffs/Debuffs", "Tasks"};

    private int tabX(int page) {
        int x = this.left + 8;
        for (int i = 0; i < page; i++) x += this.font.width(PAGES[i]) + 14;
        return x;
    }

    private int tabW(int page) {
        return this.font.width(PAGES[page]) + 10;
    }

    private void drawPageTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < PAGES.length; i++) {
            int x = tabX(i);
            int y = this.top + 6;
            boolean active = i == this.page;
            boolean hover = inside(mouseX, mouseY, x, y, tabW(i), 14);
            rounded(graphics, x, y, x + tabW(i), y + 14, active ? FRAME_LIGHT : hover ? 0xFF6E9A66 : CARD_EDGE);
            rounded(graphics, x + 1, y + 1, x + tabW(i) - 1, y + 13, active ? 0xFF2C4A28 : CARD);
            graphics.text(this.font, PAGES[i], x + 5, y + 3, active ? TEXT : TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < PAGES.length; i++) {
                if (inside((int) event.x(), (int) event.y(), tabX(i), this.top + 6, tabW(i), 14)) {
                    if (this.page != i) {
                        this.page = i;
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.page == 1 && scrollY != 0) {
            for (int card = 0; card < 3; card++) {
                if (inside((int) mouseX, (int) mouseY, cardX0(card), this.top + 26, cardX1(card) - cardX0(card), H - 34)) {
                    this.scroll[card] -= (int) Math.signum(scrollY);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── Tasks: your bounty, today's cravings ────────────────────────────────

    // (dev: client/PhotoShoot) show this page
    public void showPage(int page) {
        this.page = page;
    }

    private void drawTasksPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Player player = this.minecraft.player;
        if (player == null) return;
        int l = this.left, t = this.top;
        // the bounty card
        int x0 = l + 8, y0 = t + 26, x1 = l + W - 8, y1 = t + 92;
        rounded(graphics, x0, y0, x1, y1, CARD_EDGE);
        rounded(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, CARD);
        graphics.text(this.font, Component.literal("Your Bounty").withStyle(ChatFormatting.BOLD), x0 + 8, y0 + 6, TEXT, false);
        var bounties = com.alexh.capitalism.bounty.Bounties.of(player);
        var task = bounties.task();
        if (task == null || task.task() == null) {
            graphics.text(this.font, "No task. Visit the Bounty Clerk in town to take one.", x0 + 8, y0 + 24, TEXT_DIM, false);
            graphics.text(this.font, "Turned in: " + bounties.completed(), x0 + 8, y0 + 40, TEXT_DIM, false);
        } else {
            var bt = task.task();
            graphics.fill(x0 + 1, y0 + 1, x0 + 4, y1 - 1, 0xFF000000 | bt.tier.color);
            graphics.text(this.font, task.title(), x0 + 8, y0 + 20, TEXT, false);
            String tier = bt.tier.title;
            graphics.text(this.font, tier, x1 - 8 - this.font.width(tier), y0 + 6, 0xFF000000 | bt.tier.color, false);
            int p = com.alexh.capitalism.bounty.Bounties.progress(player, task);
            boolean done = p >= task.amount();
            int bx0 = x0 + 8, bx1 = x1 - 8, by = y0 + 34;
            graphics.fill(bx0, by, bx1, by + 8, 0xFF1A2A18);
            graphics.fill(bx0 + 1, by + 1, bx0 + 1 + (int) ((bx1 - bx0 - 2) * Math.min(1.0, p / (double) task.amount())), by + 7, done ? 0xFF6BD06B : 0xFF000000 | bt.tier.color);
            graphics.text(this.font, p + " / " + task.amount() + (done ? "  Done! Turn it in at the Bounty Clerk." : ""), x0 + 8, y0 + 46, done ? 0xFF8CF08C : TEXT_DIM, false);
            String reward = "Reward: " + com.alexh.capitalism.economy.GoldMoney.format(task.reward());
            graphics.text(this.font, reward, x1 - 8 - this.font.width(reward), y0 + 46, 0xFFF0D060, false);
        }
        // today's cravings: two cards
        var cravings = com.alexh.capitalism.craving.Cravings.of(player);
        int cy0 = t + 100, cy1 = t + H - 8;
        graphics.text(this.font, Component.literal("Today's Cravings").withStyle(ChatFormatting.BOLD), l + 10, cy0, TEXT, false);
        long ticks = com.alexh.capitalism.bounty.Bounties.ticksToTomorrow(this.minecraft.level);
        long secs = ticks / 20;
        String next = "New ones in " + (secs / 60) + " min " + (secs % 60) + " s";
        graphics.text(this.font, next, l + W - 10 - this.font.width(next), cy0, TEXT_DIM, false);
        int mid = l + W / 2;
        craving(graphics, l + 8, cy0 + 12, mid - 4, cy1, "Food", cravings.food(), cravings.foodDone(), cravings.ate(), "Fills your food bar to gold", mouseX, mouseY);
        craving(graphics, mid + 4, cy0 + 12, l + W - 8, cy1, "Drink", cravings.drink(), -1, cravings.drank(), "Fills your water bar to gold", mouseX, mouseY);
    }

    private void craving(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, String kind, String item, int doneness, boolean satisfied,
                         String does, int mouseX, int mouseY) {
        rounded(graphics, x0, y0, x1, y1, satisfied ? 0xFF6BD06B : CARD_EDGE);
        rounded(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, CARD);
        graphics.text(this.font, kind, x0 + 8, y0 + 6, TEXT_DIM, false);
        if (item.isEmpty()) {
            graphics.text(this.font, "...", x0 + 8, y0 + 24, TEXT_DIM, false);
            return;
        }
        ItemStack icon = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(item)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x0 + 8, y0 + 18);
        graphics.pose().scale(2.0F, 2.0F);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();
        Component name = com.alexh.capitalism.craving.Cravings.name(item, doneness);
        int tx = x0 + 46, tw = x1 - tx - 8;
        int ty = y0 + 20;
        for (var line : this.font.split(name.copy().withStyle(ChatFormatting.BOLD), tw)) {
            graphics.text(this.font, line, tx, ty, 0xFFFFF0B0, false);
            ty += 10;
        }
        for (var line : this.font.split(Component.literal(does), tw)) {
            graphics.text(this.font, line, tx, ty + 2, TEXT_DIM, false);
            ty += 10;
        }
        String state = satisfied ? "✔ Satisfied" : "Not yet";
        graphics.text(this.font, state, x0 + 8, y1 - 14, satisfied ? 0xFF8CF08C : TEXT_DIM, false);
        if (inside(mouseX, mouseY, x0 + 8, y0 + 18, 32, 32)) {
            List<Component> tip = new ArrayList<>();
            tip.add(name.copy().withStyle(ChatFormatting.GOLD));
            tip.add(Component.literal(does + ", once today.").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Its usual effects still apply.").withStyle(ChatFormatting.GRAY));
            this.hoverTooltip = tip;
        }
    }

    // ── Buffs & Debuffs ────────────────────────────────────────────────────
    // One line in a Buffs & Debuffs card: an item or effect icon, a name, a dim line
    // under it, and what hovering it says
    private record Row(@Nullable ItemStack item, @Nullable Identifier sprite, String name, int color, String sub, List<Component> tooltip) {
    }

    private int cardX0(int card) {
        return this.left + 8 + card * 122;
    }

    private int cardX1(int card) {
        return card == 2 ? this.left + W - 8 : cardX0(card) + 116;
    }

    private void drawBuffsPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Player player = this.minecraft.player;
        if (player == null) return;
        drawRowCard(graphics, 0, "Traits", traitRows(player), "None yet.", "Rare books teach these.", mouseX, mouseY);
        drawRowCard(graphics, 1, "Lessons", lessonRows(player), "None right now.", "Read a skill book.", mouseX, mouseY);
        drawRowCard(graphics, 2, "Effects", effectRows(player), "Nothing else.", "", mouseX, mouseY);
    }

    private static ItemStack book(Skill skill, boolean delicate) {
        return SkillBookItem.write(new ItemStack(ModItems.SKILL_BOOK.get()), skill, delicate);
    }

    private static List<Row> traitRows(Player player) {
        List<Row> rows = new ArrayList<>();
        for (Skill skill : Traits.of(player).traits()) {
            rows.add(new Row(book(skill, false), null, skill.title, GOLD, skill.rare ? "Rare trait" : "Trait",
                List.of(Component.literal(skill.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal(skill.description(1)).withStyle(ChatFormatting.GRAY),
                    Component.literal("Yours for good.").withStyle(ChatFormatting.GREEN))));
        }
        return rows;
    }

    private List<Row> lessonRows(Player player) {
        List<MobEffectInstance> lessons = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (Skills.lessonOf(effect) != null) lessons.add(effect);
        }
        lessons.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        List<Row> rows = new ArrayList<>();
        for (MobEffectInstance effect : lessons) {
            Skill skill = Skills.lessonOf(effect);
            boolean two = effect.getAmplifier() > 0;
            String left = duration(effect);
            rows.add(new Row(book(skill, two), null, skill.title + (two ? " II" : ""), GOOD, left + " left",
                List.of(Component.literal(skill.title + (two ? " II" : "")).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal(skill.description(two ? 2 : 1)).withStyle(ChatFormatting.GRAY),
                    Component.literal(left + " left").withStyle(ChatFormatting.WHITE))));
        }
        return rows;
    }

    private List<Row> effectRows(Player player) {
        List<Row> rows = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (Skills.lessonOf(effect) != null) continue;
            MobEffectCategory category = effect.getEffect().value().getCategory();
            int color = category == MobEffectCategory.HARMFUL ? BAD : category == MobEffectCategory.BENEFICIAL ? GOOD : TEXT;
            Component name = effect.getEffect().value().getDisplayName().copy();
            if (effect.getAmplifier() > 0) {
                name = Component.empty().append(name).append(" ").append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
            }
            String left = duration(effect);
            Component title = name.copy().withStyle(style -> style.withColor(color & 0xFFFFFF).withBold(true));
            rows.add(new Row(null, Hud.getMobEffectSprite(effect.getEffect()), name.getString(), color, left,
                List.of(title,
                    Component.literal(category == MobEffectCategory.HARMFUL ? "Debuff" : "Buff").withStyle(ChatFormatting.GRAY),
                    Component.literal(left).withStyle(ChatFormatting.WHITE))));
        }
        return rows;
    }

    private String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "Forever";
        float tickRate = this.minecraft.level == null ? 20.0F : this.minecraft.level.tickRateManager().tickrate();
        return MobEffectUtil.formatDuration(effect, 1.0F, tickRate).getString();
    }

    private void drawRowCard(GuiGraphicsExtractor graphics, int card, String title, List<Row> rows, String empty, String emptyHint,
                             int mouseX, int mouseY) {
        int x0 = cardX0(card);
        int x1 = cardX1(card);
        int y0 = this.top + 26;
        int y1 = this.top + H - 8;
        card(graphics, x0, y0, x1, y1);
        graphics.text(this.font, rows.isEmpty() ? title : title + " (" + rows.size() + ")", x0 + 6, y0 + 6, TEXT, true);
        if (rows.isEmpty()) {
            graphics.text(this.font, empty, x0 + 6, y0 + 22, TEXT_DIM, false);
            if (!emptyHint.isEmpty()) graphics.text(this.font, this.font.plainSubstrByWidth(emptyHint, x1 - x0 - 10), x0 + 6, y0 + 33, TEXT_DIM, false);
            return;
        }
        this.scroll[card] = Math.max(0, Math.min(this.scroll[card], rows.size() - ROWS));
        int textW = x1 - x0 - 30;
        for (int i = 0; i < ROWS && this.scroll[card] + i < rows.size(); i++) {
            Row row = rows.get(this.scroll[card] + i);
            int y = y0 + 19 + i * ROW_H;
            boolean hover = inside(mouseX, mouseY, x0 + 2, y - 1, x1 - x0 - 4, ROW_H - 1);
            if (hover) {
                graphics.fill(x0 + 2, y - 1, x1 - 2, y + ROW_H - 2, 0x30FFFFFF);
                this.hoverTooltip = row.tooltip();
            }
            if (row.item() != null) {
                graphics.item(row.item(), x0 + 5, y + 1);
            } else if (row.sprite() != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, row.sprite(), x0 + 4, y, 18, 18);
            }
            graphics.text(this.font, this.font.plainSubstrByWidth(row.name(), textW), x0 + 25, y + 1, row.color(), false);
            graphics.text(this.font, this.font.plainSubstrByWidth(row.sub(), textW), x0 + 25, y + 10, TEXT_DIM, false);
        }
        if (rows.size() > ROWS) {
            String more = (this.scroll[card] + 1) + "-" + Math.min(rows.size(), this.scroll[card] + ROWS) + " of " + rows.size();
            graphics.text(this.font, more, x1 - 6 - this.font.width(more), y0 + 6, TEXT_DIM, false);
        }
    }

    // ── Cards ──────────────────────────────────────────────────────────────

    // Water Purity: a glass tube filled up to your purity, over the four colored bands, with the bands named beside it.
    private void drawPurityCard(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        card(graphics, x0, y0, x1, y1);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WATER_FULL, x0 + 6, y0 + 5, 7, 9);
        graphics.text(this.font, "Purity", x0 + 16, y0 + 6, TEXT, true);

        float purity = ClientThirstData.purity;
        WaterPurity.Level now = WaterPurity.level(purity);
        // what counts (after the grace period)
        WaterPurity.Level level = ClientWellnessData.purityLevel();
        int color = PURITY_COLORS[level.ordinal()];

        int tx0 = x0 + 7;
        int tx1 = x0 + 19;
        int ty0 = y0 + 20;
        int ty1 = y1 - 30;
        int height = ty1 - ty0;
        int[] starts = {0, WaterPurity.MEDIUM_AT, WaterPurity.GOOD_AT, WaterPurity.EXCELLENT_AT, 100};

        // tube outline, then the four bands (dim), then the water (bright) up to your purity
        rounded(graphics, tx0 - 2, ty0 - 2, tx1 + 2, ty1 + 2, RING_OUTLINE);
        for (int band = 0; band < 4; band++) {
            int bandTop = ty1 - height * starts[band + 1] / 100;
            int bandBottom = ty1 - height * starts[band] / 100;
            graphics.fill(tx0, bandTop, tx1, bandBottom, (PURITY_COLORS[band] & 0x00FFFFFF) | 0x40000000);
            if (band > 0) graphics.fill(tx0, bandBottom, tx1, bandBottom + 1, 0x60000000);

            // band name beside the tube, the band you're in is bright
            int labelY = (bandTop + bandBottom) / 2 - 4;
            boolean current = band == now.ordinal();
            int labelColor = current ? PURITY_COLORS[band] : (PURITY_COLORS[band] & 0x00FFFFFF) | 0x90000000;
            graphics.text(this.font, WaterPurity.NAMES[band], tx1 + 5, labelY, labelColor, current);
        }
        int waterTop = ty1 - Math.round(height * purity / 100f);
        if (waterTop < ty1) {
            graphics.fillGradient(tx0, waterTop, tx1, ty1, brighten(color), color);
            graphics.fill(tx0 + 1, waterTop, tx0 + 3, ty1, 0x40FFFFFF);
        }
        // surface line
        graphics.fill(tx0 - 3, waterTop, tx1 + 3, waterTop + 1, 0xFFFFFFFF);
        // glass shine
        graphics.fill(tx1 - 3, ty0 + 2, tx1 - 2, ty1 - 2, 0x30FFFFFF);

        // (the grace period before a new level counts is hidden: no countdown)
        graphics.centeredText(this.font, WaterPurity.NAMES[level.ordinal()], (x0 + x1) / 2, y1 - 24, color);
        graphics.centeredText(this.font, Math.round(purity) + "%", (x0 + x1) / 2, y1 - 13, TEXT_DIM);
    }

    private void drawDietCard(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float[] diet, float total, float balance, DietData.Tier tier, int mouseX, int mouseY) {
        card(graphics, x0, y0, x1, y1);
        graphics.text(this.font, "Diet", x0 + 6, y0 + 6, TEXT, true);

        int cx = (x0 + x1) / 2;
        int cy = y0 + 78;
        drawDonut(graphics, cx, cy, PIE_RADIUS + 2, PIE_HOLE - 2, null, 0, RING_OUTLINE);
        drawDonut(graphics, cx, cy, PIE_RADIUS, PIE_HOLE, diet, total, RING_EMPTY);
        int slice = sliceAt(mouseX - cx, mouseY - cy, diet, total);
        if (slice >= 0) {
            int share = Math.round(diet[slice] / total * 100);
            this.hoverTooltip = List.of(Component.literal(DietData.NAMES[slice]).withStyle(style -> style.withColor(groupColor(slice)))
                .append(Component.literal(": " + share + "% (ideal " + Math.round(DietData.IDEAL[slice] * 100) + "%)").withStyle(ChatFormatting.GRAY)));
        }

        String pct = total > 0 ? Math.round(balance * 100) + "%" : "-";
        graphics.centeredText(this.font, pct, cx, cy - 8, TEXT);
        graphics.centeredText(this.font, "balance", cx, cy + 3, TEXT_DIM);

        // Status badge
        ItemStack icon;
        String status;
        String detail;
        int color;
        switch (tier) {
            case BALANCED -> {
                icon = new ItemStack(Items.GOLDEN_APPLE);
                status = "Balanced!";
                detail = "+" + drumsticks(CapitalismConfig.DIET_BALANCED_FOOD_BONUS.get());
                color = GOOD;
            }
            case POOR -> {
                icon = new ItemStack(Items.ROTTEN_FLESH);
                status = "Poor diet";
                detail = "-" + drumsticks(CapitalismConfig.DIET_POOR_FOOD_LOSS.get());
                color = BAD;
            }
            case OKAY -> {
                icon = new ItemStack(Items.APPLE);
                status = "Okay diet";
                detail = "No bonus yet";
                color = OKAY;
            }
            default -> {
                icon = new ItemStack(Items.BOWL);
                status = "Eat something!";
                detail = "";
                color = TEXT_DIM;
            }
        }
        // Before the first meals (config dietMealsBeforeEffects) the diet has no effect yet, silently: show the
        // status without promising a drumstick.
        if (!ClientWellnessData.effectsActive()) detail = "";
        int badgeY = y0 + 138;
        rounded(graphics, x0 + 6, badgeY, x1 - 6, badgeY + 22, (color & 0x00FFFFFF) | 0x33000000);
        graphics.fill(x0 + 6, badgeY, x0 + 8, badgeY + 22, color);
        graphics.item(icon, x0 + 11, badgeY + 3);
        if (detail.isEmpty()) {
            graphics.text(this.font, status, x0 + 30, badgeY + 7, color, true);
        } else {
            graphics.text(this.font, status, x0 + 30, badgeY + 3, color, true);
            graphics.text(this.font, detail, x0 + 30, badgeY + 12, TEXT_DIM, false);
        }

    }

    private void drawGroupsCard(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float[] diet, float total) {
        card(graphics, x0, y0, x1, y1);
        graphics.text(this.font, "Food Groups", x0 + 6, y0 + 6, TEXT, true);
        int barX0 = x0 + 26;
        int barX1 = x1 - 6;
        for (int i = 0; i < DietData.GROUPS; i++) {
            int y = y0 + 18 + i * 14;
            graphics.item(GROUP_ICONS[i], x0 + 5, y - 3);
            float share = total > 0 ? diet[i] / total : 0;
            String pct = Math.round(share * 100) + "%";
            graphics.text(this.font, DietData.NAMES[i], barX0, y - 2, TEXT, false);
            graphics.text(this.font, pct, barX1 - this.font.width(pct), y - 2, TEXT_DIM, false);
            // bar with a white tick at this group's ideal share
            graphics.fill(barX0, y + 7, barX1, y + 10, BAR_BG);
            int filled = Math.round((barX1 - barX0) * share);
            if (filled > 0) {
                graphics.fill(barX0, y + 7, barX0 + filled, y + 10, GROUP_COLORS[i]);
                graphics.fill(barX0, y + 7, barX0 + filled, y + 8, 0x55FFFFFF);
            }
            int ideal = barX0 + Math.round((barX1 - barX0) * DietData.IDEAL[i]);
            graphics.fill(ideal, y + 6, ideal + 1, y + 11, 0xCCFFFFFF);
        }
    }

    private void drawStatusCard(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        card(graphics, x0, y0, x1, y1);
        graphics.text(this.font, "How you're doing", x0 + 6, y0 + 6, TEXT, true);
        Player player = this.minecraft.player;
        if (player == null) return;

        int rowX = x0 + 8;
        drawHearts(graphics, player, rowX, y0 + 18);
        drawBar(graphics, rowX, y0 + 29, player.getFoodData().getFoodLevel(), ClientThirstData.goldFood, ClientThirstData.foodCap, 9,
            FOOD_EMPTY, FOOD_FULL, FOOD_HALF, FOOD_GOLD_FULL, FOOD_GOLD_HALF);
        drawBar(graphics, rowX, y0 + 40, ClientThirstData.thirst, ClientThirstData.goldThirst, ClientThirstData.thirstCap, 7,
            WATER_EMPTY, WATER_FULL, WATER_HALF, WATER_GOLD_FULL, WATER_GOLD_HALF);

        drawWellbeing(graphics, x0 + 6, y0 + 52, x1 - 6);
    }

    // Wellbeing: a status line and a bar from the middle (left) to the bonus heart (gold zone, right).
    private void drawWellbeing(GuiGraphicsExtractor graphics, int x0, int y, int x1) {
        int score = ClientWellnessData.wellbeing;
        int max = Math.max(1, ClientWellnessData.wellbeingMax);
        int buffAt = ClientWellnessData.wellbeingBuffAt;
        String heart = hearts(CapitalismConfig.WELLBEING_HEALTH, "+");

        // Bonus hearts need both at once: Balanced diet + Good water (+1), or Excellent water (+2).
        boolean dietOk = ClientWellnessData.dietTier() == DietData.Tier.BALANCED;
        WaterPurity.Level water = ClientWellnessData.purityLevel();
        boolean waterGood = water == WaterPurity.Level.GOOD || water == WaterPurity.Level.EXCELLENT;
        boolean waterExcellent = water == WaterPurity.Level.EXCELLENT;
        int level = dietOk ? (waterExcellent ? 2 : waterGood ? 1 : 0) : 0;
        double points = configValue(CapitalismConfig.WELLBEING_HEALTH, 2.0);
        String bigHeart = hearts(points * 2, "+");
        String nextBonus = level == 2 ? bigHeart : heart;
        String text;
        int color;
        int grace = ClientWellnessData.grace;
        int changeIn = ClientWellnessData.changeIn(WellbeingData.HEART);
        WellbeingData.Effect now = WellbeingData.Effect.values()[Math.min(ClientWellnessData.raw[WellbeingData.HEART], 3)];
        WellbeingData.Effect inEffect = ClientWellnessData.heart();
        // the lost heart: never says why (players have to watch themselves)
        int debuffAt = ClientWellnessData.wellbeingDebuffAt;
        if (inEffect == WellbeingData.Effect.PENALTY) {
            text = "-1 heart";
            color = BAD;
        } else if (changeIn >= 0) {
            // crossed a line, waiting out the (hidden) grace period: say what's coming, not when
            if (now.ordinal() > inEffect.ordinal()) {
                text = now == WellbeingData.Effect.BIG_BONUS ? bigHeart + " coming!" : heart + " coming!";
                color = GOLD;
            } else if (inEffect == WellbeingData.Effect.BIG_BONUS) {
                text = "Slipping to " + heart + "...";
                color = OKAY;
            } else {
                text = "Losing " + heart + "...";
                color = OKAY;
            }
        } else if (inEffect == WellbeingData.Effect.BIG_BONUS || inEffect == WellbeingData.Effect.BONUS) {
            String have = inEffect == WellbeingData.Effect.BIG_BONUS ? bigHeart : heart;
            if (level > 0) {
                text = have + "! Keep it up";
                color = GOLD;
            } else {
                // not thriving any more: drifting back toward the middle
                text = "Losing " + have + " in " + time(score - buffAt + 1 + grace);
                color = OKAY;
            }
        } else if (level > 0) {
            text = nextBonus + " in " + time(buffAt - score + grace);
            color = GOOD;
        } else {
            // the diet and/or water aren't good enough for a bonus heart
            text = !dietOk && !waterGood ? "Need diet & water" : !dietOk ? "Need Balanced diet" : "Need Good water";
            color = TEXT_DIM;
        }
        graphics.text(this.font, text, x0, y, color, true);

        // the bar: red (lost heart) | neutral | gold (bonus heart), from the bottom of the score to the top
        int min = Math.min(-1, ClientWellnessData.wellbeingMin);
        int span = max - min;
        int barY = y + 11;
        int width = x1 - x0;
        int debuffX = x0 + (int) ((long) width * (debuffAt - min) / span);
        int buffX = x0 + (int) ((long) width * (buffAt - min) / span);
        graphics.fill(x0 - 1, barY - 1, x1 + 1, barY + 5, RING_OUTLINE);
        graphics.fill(x0, barY, debuffX, barY + 4, 0xFF7A2A24);
        graphics.fill(debuffX, barY, buffX, barY + 4, 0xFF34472F);
        graphics.fill(buffX, barY, x1, barY + 4, 0xFF8A6D1E);
        int markerX = x0 + (int) ((long) width * (Math.max(min, Math.min(max, score)) - min) / span);
        graphics.fill(markerX - 1, barY - 2, markerX + 1, barY + 6, 0xFFFFFFFF);
    }

    // Temperature (player/BodyTemperature): the ball, how you feel and the %, and what's doing it. Hover it for the
    // whole story (the same as the HUD ball's hover in the inventory).
    private void drawTemperatureCard(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        card(graphics, x0, y0, x1, y1);
        float heat = ClientWellnessData.bodyHeat;
        int zone = ClientWellnessData.temperatureZone();
        graphics.text(this.font, "Temperature", x0 + 6, y0 + 6, TEXT, true);
        String pct = Math.round(heat) + "°";
        graphics.text(this.font, pct, x1 - 6 - this.font.width(pct), y0 + 6, TemperatureHud.color(zone), true);
        int y = y0 + 18;
        TemperatureHud.drawBall(graphics, x0 + 5, y, zone);
        List<String> reasons = TemperatureHud.reasons(ClientWellnessData.heatReasons);
        String line = reasons.isEmpty() ? TemperatureHud.name(zone) : reasons.get(0);
        // the whole line, never cut off: full size if it fits, otherwise smaller text over up to two lines
        int color = reasons.isEmpty() ? TemperatureHud.color(zone) : TEXT_DIM;
        int avail = x1 - x0 - 30;
        if (this.font.width(line) <= avail) {
            graphics.text(this.font, line, x0 + 25, y + 4, color, false);
        } else {
            float s = 0.75f;
            int room = (int) (avail / s);
            List<String> rows = new java.util.ArrayList<>();
            StringBuilder row = new StringBuilder();
            for (String word : line.split(" ")) {
                String next = row.isEmpty() ? word : row + " " + word;
                if (this.font.width(next) > room && !row.isEmpty()) {
                    rows.add(row.toString());
                    row = new StringBuilder(word);
                } else {
                    row = new StringBuilder(next);
                }
            }
            rows.add(row.toString());
            if (rows.size() > 2) s = 0.6f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x0 + 25, y + 8 - rows.size() * 4.5f * s / 0.75f);
            graphics.pose().scale(s, s);
            for (int i = 0; i < rows.size(); i++) graphics.text(this.font, rows.get(i), 0, i * 10, color, false);
            graphics.pose().popMatrix();
        }
        if (inside(mouseX, mouseY, x0, y0, x1 - x0, y1 - y0)) this.hoverTooltip = TemperatureHud.tooltip();
    }

    private static String time(int seconds) {
        seconds = Math.max(0, seconds);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // Hearts like the HUD. The Wellbeing bonus heart is gold.
    private void drawHearts(GuiGraphicsExtractor graphics, Player player, int x, int y) {
        // the actual (fading) bonus / penalty
        double delta = ClientWellnessData.healthMod;
        float max = player.getMaxHealth();
        float health = player.getHealth();
        int hearts = (int) Math.ceil(max / 2.0);
        int baseHearts = (int) Math.ceil((max - Math.max(delta, 0)) / 2.0);
        for (int i = 0; i < hearts; i++) {
            int hx = x + i * 8;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, hx, y, 9, 9);
            float left = health - i * 2;
            boolean bonus = i >= baseHearts;
            if (left >= 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bonus ? HEART_GOLD_FULL : HEART_FULL, hx, y, 9, 9);
            } else if (left >= 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bonus ? HEART_GOLD_HALF : HEART_HALF, hx, y, 9, 9);
            }
        }
    }

    // A HUD-style row of 10 icons with gold drawn over them from the right. Icons at or past `cap` are blacked out;
    // with cap above 20 there's an 11th (bonus) icon on a gold slot.
    private static void drawBar(GuiGraphicsExtractor graphics, int x, int y, int value, int gold, int cap, int width,
                                Identifier empty, Identifier full, Identifier half, Identifier goldFull, Identifier goldHalf) {
        // as many icons as the bar holds: 11 with a bonus, 9 when one is lost (it just isn't there, like a lost heart)
        int icons = (cap + 1) / 2;
        for (int i = 0; i < icons; i++) {
            int ix = x + i * 8;
            int left = value - i * 2;
            if (i >= 10) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, ix, y, width, 9, ThirstHud.BONUS_TINT);
                if (left >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full, ix, y, width, 9);
                else if (left == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half, ix, y, width, 9);
                int goldBonus = gold - (icons - 1 - i) * 2;
                if (goldBonus >= 2) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, goldFull, ix, y, width, 9);
                else if (goldBonus == 1) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, goldHalf, ix, y, width, 9);
                continue;
            }
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, ix, y, width, 9);
            if (left >= 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, full, ix, y, width, 9);
            } else if (left == 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half, ix, y, width, 9);
            }
            int goldLeft = gold - (icons - 1 - i) * 2;
            if (goldLeft >= 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, goldFull, ix, y, width, 9);
            } else if (goldLeft == 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, goldHalf, ix, y, width, 9);
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static void card(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        rounded(graphics, x0, y0, x1, y1, CARD_EDGE);
        rounded(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, CARD);
        graphics.fill(x0 + 2, y0 + 1, x1 - 2, y0 + 2, 0x30FFFFFF);
    }

    // A rectangle with its corner pixels cut off.
    private static void rounded(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static double configValue(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return fallback;
        }
    }

    // A config fraction (0-1) as a whole percentage.
    private static int pct(ModConfigSpec.DoubleValue value, int fallback) {
        return (int) Math.round(configValue(value, fallback / 100.0) * 100);
    }

    private static String drumsticks(int points) {
        return count(points, "drumstick");
    }

    private static String drops(int points) {
        return count(points, "water drop");
    }

    // 2 points = 1 icon, like the HUD.
    private static String count(int points, String name) {
        double n = points / 2.0;
        String s = n == Math.floor(n) ? String.valueOf((int) n) : String.valueOf(n);
        return s + " " + name + (n == 1 ? "" : "s");
    }

    private static String hearts(ModConfigSpec.DoubleValue value, String sign) {
        return hearts(configValue(value, 2.0), sign);
    }

    // `points` of health as "+1 heart" / "-1.5 hearts"
    private static String hearts(double points, String sign) {
        double hearts = points / 2.0;
        String n = hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
        return sign + n + (hearts == 1 ? " heart" : " hearts");
    }

    // A donut chart: each pixel's angle (clockwise from the top) picks its food group. `values == null` draws a
    // single-color ring (used for the outline). Drawn in horizontal runs.
    private static void drawDonut(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int hole, float @Nullable [] values, float total, int emptyColor) {
        float[] cumulative = new float[DietData.GROUPS];
        if (values != null) {
            float running = 0;
            for (int i = 0; i < DietData.GROUPS; i++) {
                running += total > 0 ? values[i] / total : 0;
                cumulative[i] = running;
            }
        }
        int r2 = radius * radius;
        int h2 = hole * hole;
        for (int dy = -radius; dy <= radius; dy++) {
            int runStart = 0;
            int runColor = 0;
            for (int dx = -radius; dx <= radius + 1; dx++) {
                int d2 = dx * dx + dy * dy;
                int color = 0;
                if (dx <= radius && d2 <= r2 && d2 >= h2) {
                    if (values == null || total <= 0) {
                        color = emptyColor;
                    } else {
                        double angle = Math.atan2(dx + 0.5, -(dy + 0.5));
                        if (angle < 0) angle += Math.PI * 2;
                        float fraction = (float) (angle / (Math.PI * 2));
                        color = GROUP_COLORS[DietData.GROUPS - 1];
                        for (int i = 0; i < DietData.GROUPS; i++) {
                            if (fraction < cumulative[i]) {
                                color = GROUP_COLORS[i];
                                break;
                            }
                        }
                        // a soft highlight on the top-left of the ring
                        if (dx + dy < -radius * 0.6 && d2 > (r2 + h2) / 2) {
                            color = brighten(color);
                        }
                    }
                }
                if (color != runColor) {
                    if (runColor != 0) {
                        graphics.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    }
                    runStart = dx;
                    runColor = color;
                }
            }
        }
    }

    // Which food group's slice is at (dx, dy) from the donut's center, or -1 if that's not on the ring. Same angle
    // rule as drawDonut: clockwise from the top.
    private static int sliceAt(int dx, int dy, float[] values, float total) {
        int d2 = dx * dx + dy * dy;
        if (total <= 0 || d2 > PIE_RADIUS * PIE_RADIUS || d2 < PIE_HOLE * PIE_HOLE) return -1;
        double angle = Math.atan2(dx + 0.5, -(dy + 0.5));
        if (angle < 0) angle += Math.PI * 2;
        float fraction = (float) (angle / (Math.PI * 2));
        float running = 0;
        for (int i = 0; i < DietData.GROUPS; i++) {
            running += values[i] / total;
            if (fraction < running) return i;
        }
        return DietData.GROUPS - 1;
    }

    private static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 28);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 28);
        int b = Math.min(255, (argb & 0xFF) + 28);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}

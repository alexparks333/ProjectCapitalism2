package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.guide.ClientDiscoveries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The layout every smith / Enchanter guide shares (the same as the Weaponsmith's Guide): section buttons along the
// top, icon tabs under them, a grid of entries on the left and the picked entry on the right (a crafting grid or a
// row of steps, pictures and a few lines). Our own blocks and items stay black silhouettes until you've made one.
// The "Information" section isn't recipes: its entries are a column of clickable title boxes instead of the icon grid,
// and their pictures are drawn without slots.
// A guide is just its colors (Theme), its tabs and its entries: see ToolsmithsGuideScreen, ArmorsmithsGuideScreen,
// EnchantersGuideScreen.
public class JobGuideScreen extends Screen {

    private static final int W = 280;
    private static final int H = 216;
    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int CELL = 20;

    private static final int SECTION_Y = 18;
    private static final int TAB_Y = 36;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 62;
    private static final int DETAIL_X = 116;
    private static final int DETAIL_Y = 62;

    // The Information section isn't recipes: a column of clickable boxes (one per topic) instead of the icon grid
    private static final String INFORMATION = "Information";
    private static final int BOX_W = DETAIL_X - GRID_X - 6;
    private static final int BOX_H = 16;
    private static final int BOX_STEP = BOX_H + 3;
    private static final int BOXES = 7;

    public record Theme(int panel, int panelDark, int trimLight, int trimDark, int slot, int slotEdge, int selected,
                        int text, int textDim, int gold) {
    }

    // A tab: an icon button inside a section (sections are made from the tabs, in order)
    public record Tab(String section, String title, ItemStack icon) {
    }

    // One entry: its icon in the grid; either a "how to make" or a row of pictures; a few lines.
    public record Entry(Tab tab, ItemStack icon, String title, @Nullable HowToMake how, List<ItemStack> pictures, List<String> lines) {
        public static Entry made(Tab tab, ItemStack icon, String title, HowToMake how, String... lines) {
            return new Entry(tab, icon, title, how, List.of(), List.of(lines));
        }

        public static Entry info(Tab tab, ItemStack icon, String title, List<ItemStack> pictures, String... lines) {
            return new Entry(tab, icon, title, null, pictures, List.of(lines));
        }
    }

    private final Theme theme;
    private final List<Tab> allTabs;
    private final List<String> sections = new ArrayList<>();
    private final List<Entry> entries;
    private final @Nullable Screen parent;
    private final Map<String, Tab> tabs = new HashMap<>();
    private String section;
    private int page = 0;
    private @Nullable Entry selected;
    private int left;
    private int top;
    private @Nullable ItemStack hovered;

    protected JobGuideScreen(String title, Theme theme, List<Tab> tabs, List<Entry> entries, @Nullable Screen parent) {
        super(Component.literal(title));
        this.theme = theme;
        this.allTabs = tabs;
        this.entries = entries;
        this.parent = parent;
        for (Tab tab : tabs) {
            if (!this.sections.contains(tab.section())) {
                this.sections.add(tab.section());
                this.tabs.put(tab.section(), tab);
            }
        }
        this.section = this.sections.getFirst();
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
        if (this.selected == null) this.selected = firstOf(tab());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Our blocks and items stay black silhouettes until you've made one
    // (Information topics never are: they're not things you make.)
    private static boolean locked(Entry entry) {
        if (INFORMATION.equals(entry.tab().section())) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(entry.icon().getItem());
        return id.getNamespace().equals(Capitalism.MODID) && !ClientDiscoveries.known(entry.icon());
    }

    private static String title(Entry entry) {
        return locked(entry) ? "???" : entry.title();
    }

    private Tab tab() {
        return this.tabs.get(this.section);
    }

    private List<Tab> tabsIn(String section) {
        return this.allTabs.stream().filter(tab -> tab.section().equals(section)).toList();
    }

    private List<Entry> inTab(Tab tab) {
        return this.entries.stream().filter(entry -> entry.tab() == tab).toList();
    }

    private @Nullable Entry firstOf(Tab tab) {
        List<Entry> list = inTab(tab);
        return list.isEmpty() ? null : list.getFirst();
    }

    private boolean infoMode() {
        return INFORMATION.equals(this.section);
    }

    private int perPage() {
        return infoMode() ? BOXES : COLS * ROWS;
    }

    private int pageCount() {
        return Math.max(1, (inTab(tab()).size() + perPage() - 1) / perPage());
    }

    // Where entry slot i of this page is, and how big: [x, y, w, h] (relative to the panel)
    private int[] cell(int i) {
        if (infoMode()) return new int[]{GRID_X, GRID_Y + i * BOX_STEP, BOX_W, BOX_H};
        return new int[]{GRID_X + (i % COLS) * CELL, GRID_Y + (i / COLS) * CELL, 18, 18};
    }

    private int navY() {
        return GRID_Y + (infoMode() ? BOXES * BOX_STEP : ROWS * CELL) + 2;
    }

    private int navW() {
        return infoMode() ? BOX_W : COLS * CELL;
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        Theme c = this.theme;
        this.hovered = null;
        int l = this.left;
        int t = this.top;

        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, c.trimDark());
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, c.trimLight());
        graphics.fill(l, t, l + W, t + H, c.panel());
        graphics.centeredText(this.font, this.title, l + W / 2, t + 6, c.gold());

        for (int i = 0; i < this.sections.size(); i++) {
            int x = l + 8 + i * 76;
            boolean active = this.sections.get(i).equals(this.section);
            graphics.fill(x, t + SECTION_Y, x + 72, t + SECTION_Y + 14, active ? c.selected() : c.slotEdge());
            graphics.fill(x + 1, t + SECTION_Y + 1, x + 71, t + SECTION_Y + 13, active ? c.panelDark() : c.slot());
            graphics.centeredText(this.font, this.sections.get(i), x + 36, t + SECTION_Y + 3, active ? c.gold() : c.textDim());
        }

        List<Tab> tabs = tabsIn(this.section);
        for (int i = 0; i < tabs.size(); i++) {
            int x = l + 8 + i * 30;
            int y = t + TAB_Y;
            boolean active = tabs.get(i) == tab();
            graphics.fill(x, y, x + 26, y + 22, active ? c.selected() : c.slotEdge());
            graphics.fill(x + 1, y + 1, x + 25, y + 21, active ? c.panelDark() : c.slot());
            graphics.item(tabs.get(i).icon(), x + 5, y + 3);
            if (inside(mouseX, mouseY, x, y, 26, 22)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(tabs.get(i).title())), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        graphics.text(this.font, tab().title(), l + 8 + tabs.size() * 30 + 6, t + TAB_Y + 7, c.text(), false);

        List<Entry> list = inTab(tab());
        int start = this.page * perPage();
        for (int i = 0; i < perPage(); i++) {
            int index = start + i;
            Entry entry = index < list.size() ? list.get(index) : null;
            int[] cell = cell(i);
            int x = l + cell[0];
            int y = t + cell[1];
            if (infoMode()) {
                if (entry != null) infoBox(graphics, entry, x, y, entry == this.selected, inside(mouseX, mouseY, x, y, BOX_W, BOX_H));
                continue;
            }
            slot(graphics, x, y, entry != null && entry == this.selected);
            if (entry == null) continue;
            if (locked(entry)) Silhouettes.draw(graphics, entry.icon(), x + 1, y + 1);
            else graphics.item(entry.icon(), x + 1, y + 1);
            if (inside(mouseX, mouseY, x, y, 18, 18)) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(title(entry))), mouseX, mouseY, ItemStack.EMPTY);
            }
        }
        if (pageCount() > 1) {
            int ny = t + navY();
            graphics.text(this.font, "<", l + GRID_X + 2, ny, this.page > 0 ? c.text() : c.textDim(), false);
            graphics.centeredText(this.font, (this.page + 1) + "/" + pageCount(), l + GRID_X + navW() / 2, ny, c.text());
            graphics.text(this.font, ">", l + GRID_X + navW() - 8, ny, this.page < pageCount() - 1 ? c.text() : c.textDim(), false);
        }

        graphics.fill(l + DETAIL_X, t + DETAIL_Y, l + W - 6, t + H - 6, c.panelDark());
        if (this.selected != null) drawEntry(graphics, this.selected, mouseX, mouseY);

        if (this.hovered != null && !this.hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, this.hovered, mouseX, mouseY);
        }
    }

    private void drawEntry(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
        Theme c = this.theme;
        int x = this.left + DETAIL_X + 6;
        int y = this.top + DETAIL_Y + 6;
        int wrap = W - DETAIL_X - 18;
        boolean locked = locked(entry);
        graphics.text(this.font, title(entry), x, y, c.gold(), false);
        int ty = y + 14;
        HowToMake how = entry.how();
        if (how != null) {
            graphics.text(this.font, "How to make:", x, ty, c.textDim(), false);
            ty += 11;
            if (how.grid() != null) {
                for (int i = 0; i < 9; i++) itemSlot(graphics, how.gridAt(i, net.minecraft.util.Util.getMillis() / 1000L), x + (i % 3) * 18, ty + (i / 3) * 18, mouseX, mouseY);
                graphics.text(this.font, "->", x + 59, ty + 23, c.text(), false);
                if (locked) {
                    slot(graphics, x + 74, ty + 18, false);
                    Silhouettes.draw(graphics, entry.icon(), x + 75, ty + 19);
                } else {
                    itemSlot(graphics, entry.icon(), x + 74, ty + 18, mouseX, mouseY);
                }
                ty += 60;
            } else if (how.ring()) {
                ty = ringRecipe(graphics, how, entry, locked, x, ty, mouseX, mouseY);
                for (FormattedCharSequence line : this.font.split(Component.literal(how.note()), wrap)) {
                    graphics.text(this.font, line, x, ty, c.textDim(), false);
                    ty += 10;
                }
                ty += 4;
            } else if (how.table()) {
                ty = tableRecipe(graphics, how, entry, locked, x, ty, mouseX, mouseY);
                for (FormattedCharSequence line : this.font.split(Component.literal(how.note()), wrap)) {
                    graphics.text(this.font, line, x, ty, c.textDim(), false);
                    ty += 10;
                }
                ty += 4;
            } else {
                for (int i = 0; i < how.steps().size(); i++) itemSlot(graphics, how.shown(i, cycle()), x + i * 20, ty, mouseX, mouseY);
                ty += 22;
                for (FormattedCharSequence line : this.font.split(Component.literal(how.note()), wrap)) {
                    graphics.text(this.font, line, x, ty, c.textDim(), false);
                    ty += 10;
                }
                ty += 4;
            }
        } else if (!entry.pictures().isEmpty()) {
            // just the items, no slots: information isn't a recipe
            for (int i = 0; i < entry.pictures().size(); i++) {
                ItemStack picture = entry.pictures().get(i);
                int px = x + i * 18;
                // our things you haven't made yet stay silhouettes here too, so the pictures don't give them away
                if (BuiltInRegistries.ITEM.getKey(picture.getItem()).getNamespace().equals(Capitalism.MODID) && !ClientDiscoveries.known(picture)) {
                    Silhouettes.draw(graphics, picture, px, ty);
                } else {
                    graphics.item(picture, px, ty);
                    if (inside(mouseX, mouseY, px, ty, 16, 16)) this.hovered = picture;
                }
            }
            ty += 22;
        }
        for (String text : locked ? List.of("Make it once to unlock its name and info!") : entry.lines()) {
            for (FormattedCharSequence line : this.font.split(Component.literal(text), wrap)) {
                graphics.text(this.font, line, x, ty, c.text(), false);
                ty += 10;
            }
            ty += 2;
        }
    }

    // A table recipe, like the table's own menu: the big slot (26x26), the small slot, an arrow and the result.
    // Returns the y under it.
    private int tableRecipe(GuiGraphicsExtractor graphics, HowToMake how, Entry entry, boolean locked, int x, int y, int mouseX, int mouseY) {
        Theme c = this.theme;
        graphics.fill(x, y, x + 26, y + 26, c.selected());
        graphics.fill(x + 1, y + 1, x + 25, y + 25, c.slot());
        long cycle = cycle();
        ItemStack big = how.shown(0, cycle);
        graphics.item(big, x + 5, y + 5);
        if (inside(mouseX, mouseY, x, y, 26, 26)) this.hovered = big;
        itemSlot(graphics, how.shown(1, cycle), x + 32, y + 4, mouseX, mouseY);
        graphics.text(this.font, "->", x + 56, y + 9, c.text(), false);
        resultSlot(graphics, how, entry, locked, x + 70, y + 4, mouseX, mouseY);
        return y + 30;
    }

    // Seconds on the clock: slots with a few options show the next one each second
    private static long cycle() {
        return net.minecraft.util.Util.getMillis() / 1000L;
    }

    // The result slot: the cycling result if the recipe has one, else the entry's own icon (a silhouette while locked)
    private void resultSlot(GuiGraphicsExtractor graphics, HowToMake how, Entry entry, boolean locked, int x, int y, int mouseX, int mouseY) {
        ItemStack result = how.result(cycle());
        if (result == null) result = entry.icon();
        if (locked) {
            slot(graphics, x, y, false);
            Silhouettes.draw(graphics, result, x + 1, y + 1);
        } else {
            itemSlot(graphics, result, x, y, mouseX, mouseY);
        }
    }

    // A Stitching Table recipe, like the table's own menu (shrunk to fit): a cover in each of the 12 ring slots (3 a
    // side) round the big Paper slot, then the thread, an arrow and the book. Returns the y under it.
    private static final float RING_SCALE = 0.8F;

    private int ringRecipe(GuiGraphicsExtractor graphics, HowToMake how, Entry entry, boolean locked, int x, int y, int mouseX, int mouseY) {
        Theme c = this.theme;
        long cycle = cycle();
        ItemStack cover = how.shown(0, cycle);
        ItemStack paper = how.shown(1, cycle);
        ItemStack thread = how.shown(2, cycle);
        // where the mouse is, in the shrunk drawing's own pixels
        double lx = (mouseX - x) / RING_SCALE;
        double ly = (mouseY - y) / RING_SCALE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(RING_SCALE, RING_SCALE);
        // the mat under the ring
        graphics.fill(-2, -2, 92, 92, c.panelDark());
        for (int i = 0; i < 3; i++) {
            int[][] spots = {{18 * (i + 1), 0}, {72, 18 * (i + 1)}, {18 * (i + 1), 72}, {0, 18 * (i + 1)}};
            for (int[] spot : spots) {
                slot(graphics, spot[0], spot[1], false);
                graphics.item(cover, spot[0] + 1, spot[1] + 1);
                if (lx >= spot[0] && lx < spot[0] + 18 && ly >= spot[1] && ly < spot[1] + 18) this.hovered = cover;
            }
        }
        // the big Paper slot in the middle
        graphics.fill(32, 32, 58, 58, c.selected());
        graphics.fill(33, 33, 57, 57, c.slot());
        graphics.item(paper, 37, 37);
        if (lx >= 32 && lx < 58 && ly >= 32 && ly < 58) this.hovered = paper;
        // the thread, an arrow down, the book
        slot(graphics, 100, 18, false);
        graphics.item(thread, 101, 19);
        if (lx >= 100 && lx < 118 && ly >= 18 && ly < 36) this.hovered = thread;
        graphics.text(this.font, "v", 106, 41, c.text(), false);
        graphics.pose().popMatrix();
        resultSlot(graphics, how, entry, locked, x + Math.round(100 * RING_SCALE), y + Math.round(54 * RING_SCALE), mouseX, mouseY);
        return y + Math.round(92 * RING_SCALE) + 4;
    }

    // An Information topic: a box with its title (cut short if it doesn't fit)
    private void infoBox(GuiGraphicsExtractor graphics, Entry entry, int x, int y, boolean selected, boolean hover) {
        Theme c = this.theme;
        graphics.fill(x, y, x + BOX_W, y + BOX_H, selected ? c.selected() : hover ? c.trimLight() : c.slotEdge());
        graphics.fill(x + 1, y + 1, x + BOX_W - 1, y + BOX_H - 1, selected ? c.panelDark() : c.slot());
        String text = title(entry);
        int room = BOX_W - 10;
        if (this.font.width(text) > room) {
            while (text.length() > 1 && this.font.width(text + "..") > room) text = text.substring(0, text.length() - 1);
            text += "..";
        }
        graphics.text(this.font, text, x + 5, y + 4, selected ? c.gold() : c.text(), false);
    }

    private void itemSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        slot(graphics, x, y, false);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        if (inside(mouseX, mouseY, x, y, 18, 18)) this.hovered = stack;
    }

    private void slot(GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
        graphics.fill(x, y, x + 18, y + 18, selected ? this.theme.selected() : this.theme.slotEdge());
        graphics.fill(x + 1, y + 1, x + 17, y + 17, this.theme.slot());
    }

    // ── Clicking ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int l = this.left;
        int t = this.top;

        for (int i = 0; i < this.sections.size(); i++) {
            if (inside(mx, my, l + 8 + i * 76, t + SECTION_Y, 72, 14)) {
                this.section = this.sections.get(i);
                this.page = 0;
                this.selected = firstOf(tab());
                return true;
            }
        }

        List<Tab> tabs = tabsIn(this.section);
        for (int i = 0; i < tabs.size(); i++) {
            if (inside(mx, my, l + 8 + i * 30, t + TAB_Y, 26, 22)) {
                this.tabs.put(this.section, tabs.get(i));
                this.page = 0;
                this.selected = firstOf(tab());
                return true;
            }
        }

        List<Entry> list = inTab(tab());
        for (int i = 0; i < perPage(); i++) {
            int index = this.page * perPage() + i;
            if (index >= list.size()) break;
            int[] cell = cell(i);
            if (inside(mx, my, l + cell[0], t + cell[1], cell[2], cell[3])) {
                this.selected = list.get(index);
                return true;
            }
        }

        int ny = t + navY();
        if (inside(mx, my, l + GRID_X, ny - 2, 12, 12) && this.page > 0) {
            this.page--;
            return true;
        }
        if (inside(mx, my, l + GRID_X + navW() - 10, ny - 2, 12, 12) && this.page < pageCount() - 1) {
            this.page++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}

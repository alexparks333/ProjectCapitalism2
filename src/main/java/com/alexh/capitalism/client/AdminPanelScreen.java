package com.alexh.capitalism.client;

import com.alexh.capitalism.core.AdminPanel;
import com.alexh.capitalism.network.AdminPanelPacket;
import com.alexh.capitalism.network.AdminSetPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// /admin panel (core/AdminPanel): the server's live admin panel, operators only. Three tabs:
//  - Settings: every setting in capitalism-server.toml by section. Click a value to change it (Enter saves, Esc
//    cancels); on/off settings flip with a click. Hover a row for what it does.
//  - Banker: every item the Banker buys with its price ("54" = 54 nuggets each, "1/2" = a nugget for 2, "0" = not
//    bought). What it really pays shows under the name (NPC shop prices and crafting caps can make it less).
//    "+ Held item" adds what's in your hand.
//  - Commands: every operator command and what it does. Click one and it's typed into chat for you to finish.
// The search box filters the tab. Changes go to the server (network/AdminSetPacket), which saves them, says so in chat
// and sends the new figures back.
public class AdminPanelScreen extends Screen {

    private static final int PANEL = 0xF0181820;
    private static final int EDGE = 0xFF8A2A2A;
    private static final int EDGE_LIGHT = 0xFFE0A040;
    private static final int ROW = 0x30FFFFFF;
    private static final int ROW_HOVER = 0x50FFE0A0;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int DIM = 0xFF9A9AA8;
    private static final int GOLD = 0xFFFFD34A;
    private static final int GREEN = 0xFF6CF08A;
    private static final int RED = 0xFFFF6A5A;
    private static final int SECTION = 0xFFE0A040;
    private static final int VALUE_W = 74;
    private static final int LIST_TOP = 58;
    private static final String[] TABS = {"Settings", "Banker", "Commands"};

    // the last figures from the server (kept so the panel reopens where it was)
    private static List<String> settings = List.of();
    private static List<String> banker = List.of();
    private static int lastTab;
    // the Settings section showing (null: the first); a search looks through every section
    private static @Nullable String shownSection;

    private int tab = lastTab;
    private int scroll;
    private final List<Row> rows = new ArrayList<>();
    private EditBox search;
    private EditBox editor;
    private Button addHeld;
    private @Nullable Row editing;

    // one line of the list: a section heading, a setting, a Banker price or a command
    private record Row(Kind kind, String key, String title, String value, String note, String help, String type, @Nullable ItemStack icon) {
        int height() {
            return kind == Kind.SECTION ? 16 : 24;
        }
    }

    private enum Kind { SECTION, SETTING, PRICE, COMMAND }

    // every operator command: what's typed, what it does (keep matching the commands; Economy.md > Admin panel)
    private static final String[][] COMMANDS = {
        {"Admin", null},
        {"/admin panel", "This panel."},
        {"/admin jobs reset <player>", "Back to the very start: no jobs, one Essential and one Fun slot. Asks to confirm if they'd lose Wishing Star slots."},
        {"/admin jobs clear <player>", "Clears their jobs so they pick again in /pick; their job slots are kept."},
        {"/admin stock refill <player>", "They can buy the Blood Merchant's Essential Wishing Star again."},
        {"/admin stock drain <player>", "They can't buy the Essential Wishing Star (counted as having had it)."},
        {"/admin draft essential add <player>", "Job draft: add someone to the Essential draft (also: fun). remove <name>, start, skip, stop, status."},
        {"/admin draft fun status", "Job draft: who's on the Fun draft and whose turn it is."},
        {"Economy", null},
        {"/banker", "A Banker right where you stand, facing you. Can't be hurt, never despawns."},
        {"/banker remove", "Takes away the nearest Banker (within 6 blocks)."},
        {"/deathtax <player>", "What a player owes Death."},
        {"/deathtax <player> forgive", "Wipes a player's death tax debt (out of Purgatory)."},
        {"/property create <name>", "A new property from your selection. Then: /property <name> price|rent|sell|lease|evict."},
        {"/property delete <name>", "Removes a property."},
        {"Town & NPCs", null},
        {"/npc setspot <job>", "Where a job's NPC (or a shopkeeper, like the butcher) stands: here."},
        {"/npc removespot <job>", "Removes that NPC's spot."},
        {"/npc list", "Every NPC spot."},
        {"/npc bloodmerchant", "Places the Blood Merchant here (remove: /npc bloodmerchant remove)."},
        {"/npc bountyclerk", "Places the Bounty Clerk here. Also: tasks <player> [reset], winners [reset]."},
        {"/npc fisherman", "Places the Fisherman here. Also: collection <player> [reset]."},
        {"/npc travelingmerchant arrive", "The Traveling Merchant arrives now (also: leave, status; no word = place him here)."},
        {"/townhall appoint <office> <player>", "Gives a player a town office."},
        {"/townhall remove <office>", "Empties a town office."},
        {"/teleport town", "Takes you to the town."},
        {"Calendar & events", null},
        {"/calendar set <season> <day>", "Moves the date (keeps the time of day). Year optional."},
        {"/calendar next", "Jumps to the next morning."},
        {"/calendar night", "Jumps to 8 pm tonight."},
        {"/calendar midnight", "Half a minute before midnight."},
        {"/calendar goto <festival>", "Jumps to a festival's morning."},
        {"/decorations auto", "Seasonal decorations follow the calendar (also: up, down to force them)."},
        {"/event bloodmoon start", "Starts a Blood Moon now (a boss name picks the boss)."},
        {"/event bloodmoon end", "Ends the Blood Moon."},
        {"/event bloodmoon delay <time>", "Puts off the next Blood Moon."},
        {"/event bloodmoon tp", "Takes you to the Crimson Revenant."},
        {"/event purge start", "Starts the Purge now (also: schedule, end)."},
        {"Players", null},
        {"/cravings <player>", "A player's food and drink cravings today (add reroll for new ones)."},
        {"/discoveries all <player>", "Unlocks every guide entry for them (reset locks them again)."},
        {"/fishlog", "Recent rare catches. Also: <player>, restrict|unrestrict|clear <player>."},
        {"Testing", null},
        {"/dev kit", "A kit of the mod's items for testing."},
        {"/dmg <amount>", "Hurts you by that much (testing armor and insurance)."},
        {"/devpanel", "Debug info on screen."},
    };

    public AdminPanelScreen() {
        super(Component.literal("Admin Panel"));
    }

    // figures from the server: open the panel, or refresh it if it's open
    public static void receive(AdminPanelPacket packet) {
        settings = List.copyOf(packet.settings());
        banker = List.copyOf(packet.banker());
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof AdminPanelScreen open) open.rebuild();
        else if (packet.open()) mc.gui.setScreen(new AdminPanelScreen());
    }

    private int left() {
        return (this.width - panelW()) / 2;
    }

    private int panelW() {
        return Math.min(this.width - 16, 400);
    }

    private int top() {
        return 8;
    }

    private int bottom() {
        return this.height - 8;
    }

    private int listTop() {
        if (this.tab != 0) return this.top() + LIST_TOP;
        List<int[]> chips = chips();
        int lowest = chips.isEmpty() ? this.top() + LIST_TOP - 16 : chips.getLast()[1];
        return lowest + 16;
    }

    // ── the Settings tab's section tabs ──

    private static String currentSection() {
        List<String> sections = sections();
        if (shownSection != null && sections.contains(shownSection)) return shownSection;
        return sections.isEmpty() ? "" : sections.getFirst();
    }

    // the sections, in the order the settings come
    private static List<String> sections() {
        List<String> out = new ArrayList<>();
        for (String line : settings) {
            String s = line.substring(0, Math.max(0, line.indexOf(AdminPanel.SEP)));
            if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }

    // each section tab's x, y, width (wrapping onto a second row if they don't fit)
    private List<int[]> chips() {
        List<int[]> out = new ArrayList<>();
        int x0 = left() + 8, x = x0, y = this.top() + 55, right = left() + panelW() - 8;
        for (String s : sections()) {
            int w = this.font.width(sectionTitle(s)) + 10;
            if (x + w > right && x > x0) {
                x = x0;
                y += 15;
            }
            out.add(new int[] {x, y, w});
            x += w + 3;
        }
        return out;
    }

    private int listBottom() {
        return this.bottom() - 6;
    }

    @Override
    protected void init() {
        int l = left(), w = panelW();
        String typed = this.search == null ? "" : this.search.getValue();
        this.search = new EditBox(this.font, l + 8, this.top() + 36, w - 16 - 84, 14, Component.literal("Search"));
        this.search.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setValue(typed);
        this.search.setResponder(s -> {
            this.scroll = 0;
            this.cancelEdit();
            this.rebuild();
        });
        this.addRenderableWidget(this.search);
        this.addHeld = Button.builder(Component.literal("+ Held item"), b -> send(AdminPanel.BANK_ADD, ""))
            .bounds(l + w - 8 - 78, this.top() + 35, 78, 16).build();
        this.addRenderableWidget(this.addHeld);
        this.editor = new EditBox(this.font, 0, 0, VALUE_W - 4, 14, Component.literal("Value"));
        this.editor.setMaxLength(40);
        this.editor.setVisible(false);
        this.addRenderableWidget(this.editor);
        this.editing = null;
        this.rebuild();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.search);
    }

    private void rebuild() {
        this.rows.clear();
        String find = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
        if (this.addHeld != null) this.addHeld.visible = this.tab == 1;
        if (this.tab == 0) {
            String section = null;
            for (String line : settings) {
                String[] f = line.split(AdminPanel.SEP, -1);
                if (f.length < 7) continue;
                String title = words(f[1].substring(f[1].lastIndexOf('.') + 1));
                String range = f[4].isEmpty() ? "" : "From " + f[4] + " to " + f[5] + ".";
                if (!matches(find, title, f[1], f[6])) continue;
                if (find.isEmpty() && !f[0].equals(currentSection())) continue;
                if (!find.isEmpty() && !f[0].equals(section)) {
                    section = f[0];
                    this.rows.add(new Row(Kind.SECTION, "", sectionTitle(section), "", "", "", "", null));
                }
                this.rows.add(new Row(Kind.SETTING, f[1], title, f[3], range, f[6], f[2], null));
            }
        } else if (this.tab == 1) {
            List<Row> priced = new ArrayList<>();
            for (String line : banker) {
                String[] f = line.split(AdminPanel.SEP, -1);
                if (f.length < 3) continue;
                Identifier id = Identifier.tryParse(f[0]);
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                String name = stack.getHoverName().getString();
                if (!matches(find, name, f[0])) continue;
                String pays = f[2].isEmpty() ? "Not bought" : "Pays " + f[2];
                boolean capped = !f[2].isEmpty() && !f[2].equals(textOf(f[1]));
                priced.add(new Row(Kind.PRICE, AdminPanel.BANK + f[0], name, f[1], pays + (capped ? " (capped: an NPC shop or crafting sells it for less)" : ""),
                    f[0], capped ? "C" : "", stack));
            }
            priced.sort(java.util.Comparator.comparing(Row::title));
            this.rows.addAll(priced);
        } else {
            String section = null;
            List<Row> inSection = new ArrayList<>();
            for (String[] command : COMMANDS) {
                if (command[1] == null) {
                    flushSection(section, inSection);
                    section = command[0];
                    continue;
                }
                if (matches(find, command[0], command[1])) inSection.add(new Row(Kind.COMMAND, command[0], command[0], "", command[1], "", "", null));
            }
            flushSection(section, inSection);
        }
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
        if (this.editing != null) {
            // the edited row's new figures (or it's gone)
            Row was = this.editing;
            this.editing = this.rows.stream().filter(r -> r.key.equals(was.key)).findFirst().orElse(null);
            if (this.editing == null) this.cancelEdit();
        }
    }

    private void flushSection(@Nullable String section, List<Row> inSection) {
        if (section != null && !inSection.isEmpty()) {
            this.rows.add(new Row(Kind.SECTION, "", section, "", "", "", "", null));
            this.rows.addAll(inSection);
        }
        inSection.clear();
    }

    private static boolean matches(String find, String... texts) {
        if (find.isEmpty()) return true;
        for (String t : texts) if (t.toLowerCase(Locale.ROOT).contains(find)) return true;
        return false;
    }

    // "1/2" -> what BankerEvents.Price.text says ("1 gold nugget for 2"), to spot a capped price
    private static String textOf(String raw) {
        try {
            int slash = raw.indexOf('/');
            int nuggets = Integer.parseInt(slash < 0 ? raw : raw.substring(0, slash));
            int per = slash < 0 ? 1 : Integer.parseInt(raw.substring(slash + 1));
            return new com.alexh.capitalism.economy.BankerEvents.Price(nuggets, per).text();
        } catch (RuntimeException e) {
            return raw;
        }
    }

    // "investmentDailyPercent" -> "Investment daily percent"
    static String words(String key) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean upper = Character.isUpperCase(c), digit = Character.isDigit(c);
            if (i > 0 && ((upper && !Character.isUpperCase(key.charAt(i - 1))) || (digit && !Character.isDigit(key.charAt(i - 1))))) out.append(' ');
            out.append(i == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static final Map<String, String> SECTIONS = Map.ofEntries(
        Map.entry("thirst", "Thirst & water"), Map.entry("weaponsmith", "Wear & tear"),
        Map.entry("difficulty", "Mobs"), Map.entry("chef", "Cooks & food"), Map.entry("economy", "Economy"),
        Map.entry("wellness", "Diet"), Map.entry("occupations", "Jobs"), Map.entry("town", "The town"),
        Map.entry("events", "Events"), Map.entry("insurance", "Death tax"));

    private static String sectionTitle(String section) {
        return SECTIONS.getOrDefault(section, words(section));
    }

    // ── changes ──

    private static void send(String key, String value) {
        ClientPacketDistributor.sendToServer(new AdminSetPacket(key, value));
    }

    private void startEdit(Row row, int y) {
        this.editing = row;
        this.editor.setValue(row.value);
        this.editor.setVisible(true);
        this.editor.setX(valueX() + 2);
        this.editor.setY(y + 5);
        this.setFocused(this.editor);
        this.editor.moveCursorToEnd(false);
        this.editor.setHighlightPos(0);
    }

    private void commitEdit() {
        if (this.editing == null) return;
        send(this.editing.key, this.editor.getValue());
        this.cancelEdit();
    }

    private void cancelEdit() {
        this.editing = null;
        if (this.editor != null) {
            this.editor.setVisible(false);
            if (this.getFocused() == this.editor) this.setFocused(null);
        }
    }

    private int valueX() {
        return left() + panelW() - 14 - VALUE_W;
    }

    // ── input ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.editing != null) {
            if (event.isEscape()) {
                this.cancelEdit();
                return true;
            }
            if (event.isConfirmation()) {
                this.commitEdit();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (this.editing != null && this.editor.isMouseOver(mx, my)) return super.mouseClicked(event, doubleClick);
        // the tabs
        for (int i = 0; i < TABS.length; i++) {
            if (my >= this.top() + 16 && my < this.top() + 30 && mx >= tabX(i) && mx < tabX(i) + tabW()) {
                if (i != this.tab) {
                    this.tab = lastTab = i;
                    this.scroll = 0;
                    this.cancelEdit();
                    this.rebuild();
                }
                return true;
            }
        }
        // a section tab (Settings)
        if (this.tab == 0) {
            List<String> sections = sections();
            List<int[]> chips = chips();
            for (int i = 0; i < chips.size(); i++) {
                int[] r = chips.get(i);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + 13) {
                    shownSection = sections.get(i);
                    this.scroll = 0;
                    this.cancelEdit();
                    if (!this.search.getValue().isEmpty()) this.search.setValue("");
                    this.rebuild();
                    return true;
                }
            }
        }
        // a row
        if (my >= listTop() && my < listBottom() && mx >= left() + 6 && mx < left() + panelW() - 6) {
            int y = listTop() - this.scroll;
            for (Row row : this.rows) {
                if (my >= y && my < y + row.height()) {
                    if (this.editing != null) this.commitEdit();
                    click(row, mx, y);
                    return true;
                }
                y += row.height();
            }
        }
        if (this.editing != null) this.commitEdit();
        return super.mouseClicked(event, doubleClick);
    }

    private void click(Row row, int mx, int y) {
        switch (row.kind) {
            case SETTING -> {
                if (row.type.equals("B")) send(row.key, String.valueOf(!Boolean.parseBoolean(row.value)));
                else startEdit(row, y);
            }
            case PRICE -> startEdit(row, y);
            case COMMAND -> {
                // typed into chat up to the first thing to fill in
                String text = row.key;
                int arg = text.indexOf(" <");
                if (arg >= 0) text = text.substring(0, arg + 1);
                this.minecraft.gui.setScreen(new ChatScreen(text, false));
            }
            default -> {
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.editing != null) this.commitEdit();
        this.scroll = Mth.clamp(this.scroll - (int) (scrollY * 24), 0, maxScroll());
        return true;
    }

    private int contentHeight() {
        int h = 0;
        for (Row row : this.rows) h += row.height();
        return h;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom() - listTop()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int tabW() {
        return 70;
    }

    private int tabX(int i) {
        return left() + 8 + i * (tabW() + 4);
    }

    // ── drawing ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int l = left(), w = panelW(), t = top(), b = bottom();
        g.fill(0, 0, this.width, this.height, 0x90000000);
        g.fill(l - 2, t - 2, l + w + 2, b + 2, EDGE);
        g.fill(l - 1, t - 1, l + w + 1, b + 1, EDGE_LIGHT);
        g.fill(l, t, l + w, b, PANEL);
        Component title = Component.literal("ADMIN PANEL").withStyle(ChatFormatting.BOLD);
        g.text(this.font, title, l + 8, t + 5, RED, false);
        String note = this.tab == 2 ? "Click a command to type it into chat" : "Changes save at once";
        g.text(this.font, note, l + w - 8 - this.font.width(note), t + 5, DIM, false);
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX(i);
            boolean on = i == this.tab, hover = mouseX >= x && mouseX < x + tabW() && mouseY >= t + 16 && mouseY < t + 30;
            g.fill(x, t + 16, x + tabW(), t + 30, on ? EDGE_LIGHT : hover ? 0xFF5A3A2A : 0xFF3A2A2A);
            g.centeredText(this.font, TABS[i], x + tabW() / 2, t + 19, on ? 0xFF201008 : TEXT);
        }
        int count = (int) this.rows.stream().filter(r -> r.kind != Kind.SECTION).count();
        String counted = count + (this.tab == 0 ? " settings" : this.tab == 1 ? " items" : " commands");
        g.text(this.font, counted, l + w - 8 - this.font.width(counted), t + 19, DIM, false);
        if (this.tab == 0) {
            List<String> sections = sections();
            List<int[]> chips = chips();
            boolean searching = !this.search.getValue().trim().isEmpty();
            for (int i = 0; i < chips.size(); i++) {
                int[] r = chips.get(i);
                boolean on = !searching && sections.get(i).equals(currentSection());
                boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + 13;
                g.fill(r[0], r[1], r[0] + r[2], r[1] + 13, on ? 0xFFB07A30 : hover ? 0xFF4A3A30 : 0xFF2E2A2A);
                g.centeredText(this.font, sectionTitle(sections.get(i)), r[0] + r[2] / 2, r[1] + 3, on ? 0xFF201008 : TEXT);
            }
        }
        g.fill(l + 6, listTop() - 2, l + w - 6, listTop() - 1, 0x60E0A040);

        // the list
        @Nullable Row hovered = null;
        g.enableScissor(l + 6, listTop(), l + w - 6, listBottom());
        int y = listTop() - this.scroll;
        for (Row row : this.rows) {
            int h = row.height();
            if (y + h > listTop() && y < listBottom()) {
                boolean hover = mouseY >= Math.max(y, listTop()) && mouseY < Math.min(y + h, listBottom()) && mouseX >= l + 6 && mouseX < l + w - 6;
                if (hover && row.kind != Kind.SECTION) hovered = row;
                drawRow(g, row, l, w, y, hover);
            }
            y += h;
        }
        g.disableScissor();
        if (this.rows.isEmpty()) g.centeredText(this.font, settings.isEmpty() ? "Waiting for the server..." : "Nothing matches.", l + w / 2, listTop() + 12, DIM);

        // the scroll bar
        int max = maxScroll();
        if (max > 0) {
            int track = listBottom() - listTop();
            int knob = Math.max(16, track * track / (track + max));
            int ky = listTop() + (track - knob) * this.scroll / max;
            g.fill(l + w - 5, listTop(), l + w - 2, listBottom(), 0x30FFFFFF);
            g.fill(l + w - 5, ky, l + w - 2, ky + knob, EDGE_LIGHT);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        String about = hovered == null ? "" : hovered.kind == Kind.SETTING ? hovered.help : hovered.kind == Kind.COMMAND ? hovered.note : "";
        if (hovered != null && this.editing == null && !about.isEmpty()) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hovered.title).withStyle(ChatFormatting.GOLD));
            for (var part : this.font.split(Component.literal(about), 240)) {
                StringBuilder sb = new StringBuilder();
                part.accept((i, style, cp) -> {
                    sb.appendCodePoint(cp);
                    return true;
                });
                tip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
            if (hovered.kind == Kind.SETTING) tip.add(Component.literal(hovered.key + (hovered.note.isEmpty() ? "" : "   " + hovered.note)).withStyle(ChatFormatting.DARK_GRAY));
            g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    private void drawRow(GuiGraphicsExtractor g, Row row, int l, int w, int y, boolean hover) {
        if (row.kind == Kind.SECTION) {
            g.text(this.font, Component.literal(row.title.toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD), l + 10, y + 5, SECTION, false);
            return;
        }
        g.fill(l + 8, y + 1, l + w - 8, y + row.height() - 1, hover ? ROW_HOVER : ROW);
        int textX = l + 12;
        if (row.icon != null) {
            g.item(row.icon, l + 11, y + 4);
            textX = l + 31;
        }
        int textW = valueX() - textX - 6;
        if (row.kind == Kind.COMMAND) textW = l + w - 14 - textX;
        g.text(this.font, this.font.plainSubstrByWidth(row.title, textW), textX, y + 3, row.kind == Kind.COMMAND ? GOLD : TEXT, false);
        // the line under it, small
        String under = row.kind == Kind.SETTING ? row.help : row.note;
        int underColor = row.kind == Kind.PRICE ? (row.type.equals("C") ? RED : row.note.startsWith("Not") ? DIM : GREEN) : DIM;
        g.pose().pushMatrix();
        g.pose().translate(textX, y + 14);
        g.pose().scale(0.75F, 0.75F);
        g.text(this.font, this.font.plainSubstrByWidth(under, (int) (textW / 0.75F)), 0, 0, underColor, false);
        g.pose().popMatrix();
        if (row.kind == Kind.COMMAND) return;
        // the value, right
        int vx = valueX();
        if (this.editing != null && this.editing.key.equals(row.key)) return;
        if (row.type.equals("B")) {
            boolean on = Boolean.parseBoolean(row.value);
            g.fill(vx, y + 5, vx + VALUE_W, y + 19, on ? 0xFF2A6A3A : 0xFF6A2A2A);
            g.centeredText(this.font, on ? "ON" : "OFF", vx + VALUE_W / 2, y + 8, on ? GREEN : RED);
        } else {
            g.fill(vx, y + 5, vx + VALUE_W, y + 19, 0xFF000000);
            g.fill(vx, y + 5, vx + VALUE_W, y + 6, 0xFF505060);
            String shown = this.font.plainSubstrByWidth(row.value, VALUE_W - 6);
            g.text(this.font, shown, vx + VALUE_W - 4 - this.font.width(shown), y + 8, row.kind == Kind.PRICE && row.value.equals("0") ? DIM : GOLD, false);
        }
    }

    // (client/PhotoShoot) which Settings section shows
    public static void showSection(String section) {
        shownSection = section;
    }

    // (client/PhotoShoot) a tab, and what's typed in the search box
    public void showTab(int tab, String find) {
        this.tab = lastTab = tab;
        this.scroll = 0;
        this.cancelEdit();
        this.search.setValue(find);
        this.rebuild();
    }

    @Override
    public void removed() {
        lastTab = this.tab;
        super.removed();
    }
}

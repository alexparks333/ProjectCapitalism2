package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.SkillBook;
import com.alexh.capitalism.librarian.SkillBookItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Reading a skill book (librarian/SkillBookItem): while you hold right-click, the book lies open in the middle of the
// screen. Left page: the book, its title, Rough / Delicate, Temporary / Persistent. Right page: what it teaches, written
// in as you read, with faint lines filling the rest. Under it, how far you've read. Let go early and nothing happens.
// GUI layer "capitalism:reading" (ClientModEvents).
public final class ReadingHud {

    private static final int W = 230;
    private static final int H = 132;
    private static final int PAGE = 0xFFF4EAD0;
    private static final int PAGE_EDGE = 0xFFC9B48A;
    private static final int INK = 0xFF3A2A1A;
    private static final int INK_DIM = 0xFF8A7458;
    private static final int GOLD = 0xFFB8862A;

    private ReadingHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gui.screen() != null || !player.isUsingItem()) return;
        ItemStack stack = player.getUseItem();
        if (!stack.is(ModItems.SKILL_BOOK.get())) return;
        SkillBook book = SkillBookItem.book(stack);
        Skill skill = book == null ? null : book.resolve();
        if (skill == null) return;
        int total = SkillBookItem.readTicks(skill);
        float read = (total - player.getUseItemRemainingTicks() + delta.getGameTimeDeltaPartialTick(false)) / total;
        read = Math.max(0.0F, Math.min(1.0F, read));

        Font font = mc.font;
        int x0 = (mc.getWindow().getGuiScaledWidth() - W) / 2;
        int y0 = (mc.getWindow().getGuiScaledHeight() - H) / 2 - 20;
        int mid = x0 + W / 2;

        // the cover (in the skill's color) behind the two pages, and the spine
        int cover = 0xFF000000 | darken(skill.color);
        graphics.fill(x0 - 5, y0 - 4, x0 + W + 5, y0 + H + 5, 0xFF1A120A);
        graphics.fill(x0 - 4, y0 - 3, x0 + W + 4, y0 + H + 4, cover);
        graphics.fill(x0, y0, mid - 1, y0 + H, PAGE);
        graphics.fill(mid + 1, y0, x0 + W, y0 + H, PAGE);
        graphics.fill(mid - 1, y0, mid + 1, y0 + H, PAGE_EDGE);
        graphics.fill(x0, y0 + H - 2, x0 + W, y0 + H, PAGE_EDGE);

        // left page: the book, its name, what kind of book
        int leftCenter = x0 + W / 4;
        graphics.item(stack, leftCenter - 8, y0 + 10);
        int y = y0 + 32;
        for (String line : wrap(font, skill.title, W / 2 - 16)) {
            graphics.centeredText(font, line, leftCenter, y, skill.trait ? GOLD : INK);
            y += 10;
        }
        y += 6;
        graphics.centeredText(font, book.delicate() ? "Delicate Book" : "Rough Book", leftCenter, y, INK_DIM);
        y += 11;
        graphics.centeredText(font, skill.trait ? "Persistent" : skill.bloodMoon() ? "Blood Moon" : skill.instant ? "Instant" : "Temporary", leftCenter, y,
            skill.trait ? GOLD : skill.bloodMoon() ? 0xFFB01C30 : skill.instant ? 0xFF2A8A80 : 0xFF3E7A3A);

        // right page: what it teaches, written in as you read, then faint lines of text
        int textX = mid + 8;
        int textW = W / 2 - 16;
        List<String> lines = wrap(font, skill.description(book.delicate() ? 2 : 1), textW);
        int chars = 0;
        for (String line : lines) chars += line.length();
        // the description is written over the first 40% of the read, the faint lines over the rest
        int shown = Math.round(chars * Math.min(1.0F, read / 0.4F));
        y = y0 + 12;
        for (String line : lines) {
            int take = Math.min(line.length(), shown);
            if (take > 0) graphics.text(font, line.substring(0, take), textX, y, INK, false);
            shown -= take;
            y += 10;
        }
        y += 6;
        float scribble = Math.max(0.0F, (read - 0.4F) / 0.6F);
        int scribbleLines = (y0 + H - 12 - y) / 7;
        for (int i = 0; i < scribbleLines; i++) {
            float lineDone = Math.max(0.0F, Math.min(1.0F, scribble * scribbleLines - i));
            int full = textW - (i % 3 == 2 ? 30 : i % 2 == 0 ? 6 : 16);
            int width = Math.round(full * lineDone);
            if (width > 0) graphics.fill(textX, y + i * 7, textX + width, y + i * 7 + 2, 0x553A2A1A);
        }

        // under the book: how far along you are
        int barY = y0 + H + 10;
        int seconds = (int) Math.ceil(player.getUseItemRemainingTicks() / 20.0);
        graphics.centeredText(font, "Reading... " + seconds + "s", mid, barY, 0xFFF2EAD6);
        graphics.fill(x0, barY + 11, x0 + W, barY + 15, 0xFF1A120A);
        graphics.fill(x0 + 1, barY + 12, x0 + 1 + Math.round((W - 2) * read), barY + 14, skill.trait ? 0xFFE8C14A : 0xFF8AF09A);
    }

    // Word-wrap to a width
    private static List<String> wrap(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (font.width(next) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static int darken(int rgb) {
        int r = ((rgb >> 16) & 0xFF) * 3 / 4;
        int g = ((rgb >> 8) & 0xFF) * 3 / 4;
        int b = (rgb & 0xFF) * 3 / 4;
        return (r << 16) | (g << 8) | b;
    }
}

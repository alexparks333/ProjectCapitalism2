package com.alexh.capitalism.client;

import com.alexh.capitalism.network.DayNoticePacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

// The new day's card (Calendar.md), bottom right: a small wooden card like the clock's, with a heading ("Good
// morning!", or the new season's name in its colour), the date, and today's or tomorrow's festival if there is one. It
// slides in from the right, stays a few seconds, and fades away. Sent each morning (calendar/CalendarEvents.morning).
public final class DayNoticeHud {

    private static final float SCALE = 0.85F;
    private static final long SLIDE_MS = 350;
    private static final long SHOW_MS = 7000;
    private static final long FADE_MS = 800;
    private static final int WOOD = 0xFF7A4A28;
    private static final int WOOD_DARK = 0xFF4A2A14;
    private static final int WOOD_LIGHT = 0xFFA06A3A;
    private static final int GOLD = 0xFFE8B850;
    private static final int PAPER = 0xFFF6E6C4;
    private static final int INK = 0xFF4A2E1A;

    private static DayNoticePacket notice;
    private static long shownAt;

    private DayNoticeHud() {
    }

    public static void show(DayNoticePacket packet) {
        notice = packet;
        shownAt = System.currentTimeMillis();
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        DayNoticePacket n = notice;
        Minecraft mc = Minecraft.getInstance();
        if (n == null || mc.player == null) return;
        long age = System.currentTimeMillis() - shownAt;
        if (age > SHOW_MS + FADE_MS) {
            notice = null;
            return;
        }
        Font font = mc.font;
        boolean hasFestival = !n.festival().isEmpty();
        int w = Math.max(Math.max(font.width(n.head()), font.width(n.date())), hasFestival ? font.width(n.festival()) : 0) + 14;
        int h = hasFestival ? 40 : 29;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        // sliding in, then fading out
        float in = Mth.clamp(age / (float) SLIDE_MS, 0.0F, 1.0F);
        in = 1 - (1 - in) * (1 - in) * (1 - in);
        float fade = age < SHOW_MS ? 1.0F : 1.0F - (age - SHOW_MS) / (float) FADE_MS;
        int alpha = Mth.clamp((int) (fade * 255), 4, 255);
        float ax = sw - 4;
        float ay = sh - 4;
        graphics.pose().pushMatrix();
        graphics.pose().translate((1 - in) * (w + 8), 0);
        graphics.pose().translate(ax, ay);
        graphics.pose().scale(SCALE, SCALE);
        graphics.pose().translate(-ax, -ay);
        int x = (int) ax - w;
        int y = (int) ay - h;
        graphics.fill(x, y, x + w, y + h, a(WOOD_DARK, alpha));
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, a(WOOD, alpha));
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, a(WOOD_LIGHT, alpha));
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, a(GOLD, alpha));
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, a(PAPER, alpha));
        graphics.fill(x + 3, y + 14, x + w - 3, y + 15, a(0xFFD8C098, alpha));
        int tx = x + 7;
        graphics.text(font, n.head(), tx, y + 5, a(0xFF000000 | darken(n.headColor()), alpha), false);
        graphics.text(font, n.date(), tx, y + 17, a(INK, alpha), false);
        if (hasFestival) {
            graphics.fill(x + 3, y + 27, x + w - 3, y + h - 3, a(0xFF000000 | n.festivalColor(), alpha));
            graphics.text(font, n.festival(), tx, y + 29, a(0xFF2A1A10, alpha), false);
        }
        graphics.pose().popMatrix();
    }

    private static int a(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    // a colour a little darker, to read on the paper
    private static int darken(int rgb) {
        int r = ((rgb >> 16) & 0xFF) * 3 / 4, g = ((rgb >> 8) & 0xFF) * 3 / 4, b = (rgb & 0xFF) * 3 / 4;
        return (r << 16) | (g << 8) | b;
    }
}

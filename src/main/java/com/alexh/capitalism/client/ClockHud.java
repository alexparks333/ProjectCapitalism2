package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.calendar.Festival;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

// The clock (Calendar.md), top right: a wooden panel with a little sky dial on the left (the sky's colour following
// the time, the sun or moon travelling across its arc over a strip of hills), the day and date ("Mon. 14") and the
// season with its icon on the right, and along the bottom the time in 10-minute steps ("9:40 am") and the weather. On a
// festival day a ribbon under it names the festival. Drawn in pixels (fills); it moves down below the status effect
// icons when there are any.
public final class ClockHud {

    // the clock at 85% size
    private static final float SCALE = 0.85F;
    private static final int W = 92;
    private static final int H = 44;
    private static final int WOOD = 0xFF7A4A28;
    private static final int WOOD_DARK = 0xFF4A2A14;
    private static final int WOOD_LIGHT = 0xFFA06A3A;
    private static final int GOLD = 0xFFE8B850;
    private static final int PAPER = 0xFFF6E6C4;
    private static final int INK = 0xFF4A2E1A;

    // the little pictures: one letter a pixel, '.' clear
    private static final String[] SUN = {
        "..y..y..", ".y.oo.y.", "..oyyo..", "yoyyyyoy", "yoyyyyoy", "..oyyo..", ".y.oo.y.", "..y..y.."};
    private static final String[] MOON = {
        "..www...", ".wwg....", "wwg.....", "wwg.....", "wwg.....", "wwgg....", ".wwwg...", "..www..."};
    private static final String[] SPRING = {
        "..pp.pp.", ".pPPpPPp", ".pPyyPPp", "..pyyPp.", ".pPPpPPp", ".pPPpPP.", "..pp.gg.", "......g."};
    private static final String[] SUMMER = {
        "...oo...", ".o.yy.o.", "..yyyy..", "oyyyyyyo", "oyyyyyyo", "..yyyy..", ".o.yy.o.", "...oo..."};
    private static final String[] FALL = {
        "....rr..", "...rOOr.", "..rOOOr.", ".rOOrOr.", ".rOrOOr.", ".rOOOr..", "..rrr...", ".b......"};
    private static final String[] WINTER = {
        "...c....", ".c.c.c..", "..ccc...", "ccc.ccc.", "..ccc...", ".c.c.c..", "...c....", "........"};
    private static final String[] RAIN = {
        "..ggg...", ".gGGGg..", "gGGGGGg.", "gggggggg", "........", ".c..c..c", "c..c..c.", "........"};
    private static final String[] STORM = {
        "..ddd...", ".dDDDd..", "dDDDDDd.", "dddddddd", "...yy...", "..yy....", ".yyyy...", "...y...."};

    private ClockHud() {
    }

    // Something that fills rectangles: the clock's picture is painted through this into an image (paint), not straight
    // onto the screen
    @FunctionalInterface
    private interface Painter {
        void fill(int x0, int y0, int x1, int y1, int color);
    }

    // The clock's picture (the panel, the sky dial with its sun or moon and hills, the paper cards, the season and
    // weather icons) is painted into a small image only when what it shows changes (every 100 ticks, or the weather,
    // the day or the season), and each frame draws that image once, then the words on top. Drawn straight onto the
    // screen it was ~100 rectangles every frame, the mod's biggest cost (each one a new object for the HUD, and the HUD
    // slows down with every rectangle): the game stuttered.
    private static final Identifier PICTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "clock_hud");
    private static DynamicTexture picture;
    private static long pictureKey = Long.MIN_VALUE;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Font font = mc.font;
        long clock = Calendar.clock(mc.level);
        Calendar.Date date = Calendar.date(clock);
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw - W - 3;
        int y = 3 + effectRows(mc) * 26;
        refreshText(clock, date);
        String[] weather = mc.level.isThundering() ? STORM : mc.level.isRaining() ? RAIN : Calendar.isDay(clock) ? SUN : MOON;
        long key = Math.floorDiv(clock, 100L) * 31 + System.identityHashCode(weather);   // (no string made each frame)
        if (picture == null || key != pictureKey) {
            pictureKey = key;
            paint(mc, font, clock, date, weather);
        }
        // drawn SCALE of full size, shrunk toward its top right corner
        float ax = sw - 3;
        graphics.pose().pushMatrix();
        graphics.pose().translate(ax, y);
        graphics.pose().scale(SCALE, SCALE);
        graphics.pose().translate(-ax, -y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PICTURE, x, y, 0.0F, 0.0F, W, H, W, H);

        // the words: the date and the season on the card, the time along the bottom
        int px = x + 40, py = y + 4, pw = W - 44;
        graphics.text(font, dayText, px + (pw - font.width(dayText)) / 2, py + 2, INK, false);
        String season = date.season().title;
        int sx = px + (pw - font.width(season) - 10) / 2;
        graphics.text(font, season, sx + 10, py + 12, darken(date.season().color), false);
        graphics.text(font, timeText, x + 8, y + 30, INK, false);

        // a festival: a ribbon underneath
        Festival festival = Festival.on(date);
        if (festival != null) {
            String name = festivalText;
            int rw = Math.max(W - 10, font.width(name) + 10);
            int rx = x + W - rw - 5;
            int ry = y + H - 1;
            graphics.fill(rx, ry, rx + rw, ry + 12, WOOD_DARK);
            graphics.fill(rx + 1, ry, rx + rw - 1, ry + 11, festival.color);
            graphics.text(font, name, rx + (rw - font.width(name)) / 2, ry + 2, 0xFF2A1A10, false);
        }
        graphics.pose().popMatrix();
    }

    // paints the clock's picture (everything but the words) into its image, from (0, 0)
    private static void paint(Minecraft mc, Font font, long clock, Calendar.Date date, String[] weather) {
        if (picture == null) {
            picture = new DynamicTexture(() -> "capitalism clock", W, H, true);
            mc.getTextureManager().register(PICTURE, picture);
        }
        NativeImage image = picture.getPixels();
        if (image == null) return;
        image.fillRect(0, 0, W, H, 0);
        Painter p = (x0, y0, x1, y1, color) -> {
            for (int yy = Math.max(0, y0); yy < Math.min(H, y1); yy++) {
                for (int xx = Math.max(0, x0); xx < Math.min(W, x1); xx++) image.setPixel(xx, yy, color);
            }
        };
        // the panel: a dark frame, wood, a gold line, the paper inside
        p.fill(0, 0, W, H, WOOD_DARK);
        p.fill(1, 1, W - 1, H - 1, WOOD);
        p.fill(1, 1, W - 1, 2, WOOD_LIGHT);
        p.fill(2, 2, W - 2, H - 2, GOLD);
        p.fill(3, 3, W - 3, H - 3, WOOD_DARK);
        // the dial: a round-topped window of sky, the sun or moon on its arc, hills along its foot
        dial(p, clock, 4, 4, 34, 22);
        // the paper card for the date and season, with the season's icon
        int px = 40, py = 4, pw = W - 44, ph = 22;
        p.fill(px, py, px + pw, py + ph, PAPER);
        p.fill(px, py + ph - 1, px + pw, py + ph, 0xFFD8C098);
        String[] icon = switch (date.season()) {
            case SPRING -> SPRING;
            case SUMMER -> SUMMER;
            case FALL -> FALL;
            case WINTER -> WINTER;
        };
        int sx = px + (pw - font.width(date.season().title) - 10) / 2;
        pixels(p, icon, sx, py + 12);
        // the strip for the time, with the weather
        int tx = 4, ty = 28, tw = W - 8, th = 12;
        p.fill(tx, ty, tx + tw, ty + th, PAPER);
        pixels(p, weather, tx + tw - 11, ty + 2);
        picture.upload();
    }

    // the clock's words, made again only when the shown time changes (every 10 game minutes), not 60+ times a second
    private static long textKey = Long.MIN_VALUE;
    private static String dayText = "";
    private static String timeText = "";
    private static String festivalText = "";

    private static void refreshText(long clock, Calendar.Date date) {
        long key = Math.floorDiv(clock, 100L);
        if (key == textKey) return;
        textKey = key;
        dayText = date.shortText();
        timeText = Calendar.timeText(clock);
        Festival f = Festival.on(date);
        festivalText = f == null ? "" : "✦ " + f.title;
    }

    // the sky dial: sky by the time of day (dawn, day, dusk, night), the sun by day and the moon by night riding a
    // half-circle from left to right, a few stars at night, green hills along the bottom
    private static void dial(Painter graphics, long clock, int x, int y, int w, int h) {
        int minute = Calendar.minuteOfDay(clock);
        int top;
        int bottom;
        if (minute >= 7 * 60 && minute < 17 * 60) {
            top = 0xFF3A8CE0;
            bottom = 0xFF9AD4FF;
        } else if (minute >= 5 * 60 && minute < 7 * 60 || minute >= 17 * 60 && minute < 19 * 60) {
            top = 0xFF5A4AA0;
            bottom = 0xFFFF9A50;
        } else {
            top = 0xFF0A0E2A;
            bottom = 0xFF26306A;
        }
        float cx = x + w / 2.0F;
        float r = w / 2.0F - 1;
        for (int row = 0; row < h; row++) {
            int color = mix(top, bottom, row / (float) (h - 1));
            float dy = (y + h - 4) - (y + row);
            // the round top: only within the half circle
            float half = dy > 0 ? (float) Math.sqrt(Math.max(0, r * r - dy * dy)) : r;
            if (dy > r) continue;
            graphics.fill((int) (cx - half), y + row, (int) Math.ceil(cx + half), y + row + 1, color);
        }
        boolean day = Calendar.isDay(clock);
        if (!day) {
            int[][] stars = {{5, 6}, {11, 3}, {22, 5}, {28, 9}, {16, 10}, {8, 12}};
            for (int[] s : stars) graphics.fill(x + s[0], y + s[1], x + s[0] + 1, y + s[1] + 1, 0xFFEFF3FF);
        }
        // the sun or the moon on its arc
        float a = Calendar.arc(clock);
        double angle = Math.PI * (1 - a);
        int ix = (int) (cx + Math.cos(angle) * (r - 5)) - 4;
        int iy = (int) ((y + h - 4) - Math.sin(angle) * (r - 5)) - 4;
        pixels(graphics, day ? SUN : MOON, ix, iy);
        // the hills: neighbouring columns of the same height drawn as one rectangle (each separate rectangle costs the HUD)
        int color = day ? 0xFF4C9A3A : 0xFF1C3A26;
        int start = 0;
        for (int i = 1; i <= w; i++) {
            if (i < w && hill(i) == hill(start)) continue;
            graphics.fill(x + start, y + h - hill(start), x + i, y + h, color);
            start = i;
        }
    }

    private static int hill(int i) {
        return (int) (2 + 1.5 * Math.sin(i * 0.35) + Math.sin(i * 0.9));
    }

    // how many rows of status effect icons are showing (vanilla draws them in the top right)
    private static int effectRows(Minecraft mc) {
        boolean good = false;
        boolean bad = false;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (!effect.showIcon()) continue;
            if (effect.getEffect().value().isBeneficial()) good = true;
            else bad = true;
        }
        return (good ? 1 : 0) + (bad ? 1 : 0);
    }

    // a little picture, one letter a pixel; each run of same-coloured pixels in a row is one rectangle (drawn pixel by
    // pixel it was ~200 rectangles a frame, and the HUD slows down with every one: the clock was the mod's biggest cost)
    private static void pixels(Painter graphics, String[] art, int x, int y) {
        for (int row = 0; row < art.length; row++) {
            String line = art[row];
            int col = 0;
            while (col < line.length()) {
                int color = colorOf(line.charAt(col));
                int end = col + 1;
                while (end < line.length() && line.charAt(end) == line.charAt(col)) end++;
                if (color != 0) graphics.fill(x + col, y + row, x + end, y + row + 1, color);
                col = end;
            }
        }
    }

    private static int colorOf(char c) {
        return switch (c) {
            case 'y' -> 0xFFFFE040;
            case 'o' -> 0xFFFFA020;
            case 'w' -> 0xFFF4F2D8;
            case 'g' -> 0xFFB8B8B0;
            case 'G' -> 0xFFE0E0E0;
            case 'p' -> 0xFFE070A8;
            case 'P' -> 0xFFFFB0D8;
            case 'r' -> 0xFFA03010;
            case 'O' -> 0xFFF07820;
            case 'b' -> 0xFF6A3A1A;
            case 'c' -> 0xFF8AD8FF;
            case 'd' -> 0xFF505060;
            case 'D' -> 0xFF7A7A8A;
            default -> 0;
        };
    }

    private static int darken(int color) {
        int r = (color >> 16 & 0xFF) * 2 / 3;
        int g = (color >> 8 & 0xFF) * 2 / 3;
        int b = (color & 0xFF) * 2 / 3;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}

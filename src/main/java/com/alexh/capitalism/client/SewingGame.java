package com.alexh.capitalism.client;

import com.alexh.capitalism.designer.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The Sewing Machine's needle game (Designer.md > The needle game), sewing one material onto a Patch: the cloth feeds
// down the screen like it does through a real machine, with the seam chalked on it, and the needle sits near the bottom.
// Only moving the mouse LEFT and RIGHT moves the needle: keep it inside the chalk lines as the seam winds past. Touching
// a line ends the try (a red flash, one needle breaks, the material is kept: sew it again). Harder tiers
// (Pattern.Tier) run narrower, faster and longer; each material winds its own way (Pattern.Style). Light cloth gets
// dark tailor's chalk and the cloth outside the seam is shaded, so the seam always stands out. Played on the client like the
// dent game; at the end the number of mistakes goes to the server (SewingMachineMenu.SEW + mistakes), which does the
// sewing. Esc stops it before the end with nothing used.
final class SewingGame {

    private enum State { READY, RUNNING, DONE }

    // straight seam at the start, before it begins to wind (cloth pixels)
    private static final float LEAD_IN = 70;
    // extra seam width for string and wool on Luxury patches
    public static final float LUXURY_WAVY_WIDTH = 1.2F;
    public static final float FANCY_WAVY_WIDTH = 1.12F;
    // how much wider metal's (RIGID) seam may get on its sharp jumps (other seams: 1.8)
    public static final float RIGID_WIDEN = 3.0F;
    // metal's (RIGID) seam a little wider all along, and its jumps fewer (longer straight runs between them)
    public static final float RIGID_WIDTH = 1.1F;
    // ice seams pinch narrow where they freeze, so they get a little more room all along
    public static final float ICY_WIDTH = 1.22F;
    // after touching a line, this long before it counts again
    private static final long GRACE = 650;
    private static final long FLASH = 260;
    private static final long END_PAUSE = 1300;
    private static final long STITCH_SOUND = 85;

    private static final int OVERLAY = 0xEC0E1311;
    private static final int ENAMEL = 0xFF16201B;
    private static final int ENAMEL_LIGHT = 0xFF24342C;
    private static final int GOLD = 0xFFE2BC5C;
    private static final int GOLD_DARK = 0xFF8E6E26;
    private static final int CHALK = 0xFFF6F0DC;
    private static final int CHALK_DARK = 0xFF1C2A64;
    private static final int THREAD = 0xFFFFF4D6;
    private static final int THREAD_DARK = 0xFF3A2414;
    private static final int TITLE = 0xFFF6E2B0;
    private static final int TEXT = 0xFFC8D4C8;
    private static final int RED = 0xFFFF5A4A;

    // the cloth's color for each material (what's being sewn on)
    private static final Map<Item, Integer> CLOTH = Map.ofEntries(
        Map.entry(Items.LEATHER, 0xFF8A5430), Map.entry(Items.STRING, 0xFFE4DECC), Map.entry(Items.IRON_NUGGET, 0xFF8E949C),
        Map.entry(Items.IRON_INGOT, 0xFF9AA0A8), Map.entry(Items.GOLD_NUGGET, 0xFFD8B040), Map.entry(Items.GOLD_INGOT, 0xFFE0BC48),
        Map.entry(Items.DIAMOND, 0xFF4ACCC4), Map.entry(Items.EMERALD, 0xFF2AA858),
        Map.entry(com.alexh.capitalism.core.ModItems.COMPACTED_DIAMOND.get(), 0xFF2FA8A0), Map.entry(com.alexh.capitalism.core.ModItems.COMPACTED_IRON.get(), 0xFF7E848C), Map.entry(Items.WHEAT, 0xFFD8B65C), Map.entry(Items.HONEYCOMB, 0xFFE0A030),
        Map.entry(Items.WHEAT_SEEDS, 0xFF6E9A3E), Map.entry(Items.BONE_MEAL, 0xFFDCDCD0),
        Map.entry(Items.WOOL.purple(), 0xFF7A3AAA), Map.entry(Items.WOOL.pink(), 0xFFE890B4), Map.entry(Items.WOOL.green(), 0xFF4E6E22),
        Map.entry(Items.WOOL.red(), 0xFFA02C28), Map.entry(Items.WOOL.brown(), 0xFF744A2A), Map.entry(Items.WOOL.black(), 0xFF242428),
        Map.entry(Items.WOOL.white(), 0xFFEAEAE4), Map.entry(Items.WOOL.lightGray(), 0xFFA0A09C), Map.entry(Items.WOOL.gray(), 0xFF4A4E52),
        Map.entry(Items.WOOL.yellow(), 0xFFF0C030), Map.entry(Items.WOOL.orange(), 0xFFE87010), Map.entry(Items.WOOL.lime(), 0xFF70B818),
        Map.entry(Items.WOOL.cyan(), 0xFF158890), Map.entry(Items.WOOL.lightBlue(), 0xFF3AAFD8), Map.entry(Items.WOOL.blue(), 0xFF35399C),
        Map.entry(Items.WOOL.magenta(), 0xFFBE44B4),
        Map.entry(Items.ICE, 0xFF9CC8EC), Map.entry(Items.SNOWBALL, 0xFFEEF4FC));

    private final ItemStack material;
    private final float difficulty;
    private final int cloth;
    private final float speed;
    private final float length;
    private final float amp;
    private final float freq;
    private final float phase1;
    private final float phase2;
    private final long seed;
    private final Pattern.Style style;
    private final int needles;
    private final float widthScale;
    // the cloth's width this frame (pixels), for the steepness of the seam
    private float clothWidthNow = 200;
    // light cloth (string, wool...) gets dark chalk and thread; dark cloth white
    private final boolean light;

    private State state = State.READY;
    private long started;
    private long endedAt;
    private long lastSound;
    private long lastMistake = Long.MIN_VALUE / 2;
    private boolean off;
    private int mistakes;
    private float needleX;
    // where the stitches went: {cloth position, x from the cloth's middle}; and where the mistakes were
    private final List<float[]> trail = new ArrayList<>();
    private final List<float[]> misses = new ArrayList<>();

    SewingGame(Pattern.Tier tier, Pattern.Stitch stitch, ItemStack inSlot, ItemStack needle) {
        // the real thing from the slot (any color of wool shows as that color)
        this.material = inSlot.isEmpty() ? stitch.stack() : inSlot.copyWithCount(1);
        this.style = stitch.style();
        this.needles = Math.max(1, needle.getCount());
        this.difficulty = tier.difficulty;
        // string and wool swing the widest, so on Fancy and Luxury they get a little more room
        this.widthScale = tier.width * (this.style == Pattern.Style.RIGID ? RIGID_WIDTH : this.style == Pattern.Style.ICY ? ICY_WIDTH
            : this.style != Pattern.Style.WAVY ? 1.0F : tier == Pattern.Tier.LUXURY ? LUXURY_WAVY_WIDTH : tier == Pattern.Tier.FANCY ? FANCY_WAVY_WIDTH : 1.0F);
        this.cloth = CLOTH.getOrDefault(this.material.getItem(), 0xFFB89A78);
        this.light = luminance(this.cloth) > 140;
        // the same speed whichever needle sews it
        this.speed = (72 + 88 * this.difficulty) * tier.speed;
        this.length = LEAD_IN + this.speed * (3.3F + 1.2F * this.difficulty);
        this.amp = switch (this.style) {
            case WAVY -> 0.22F + 0.10F * this.difficulty;
            case WANDERING -> 0.12F + 0.15F * this.difficulty;
            case RIGID -> 0.16F + 0.13F * this.difficulty;
            case JITTERY -> 0.14F + 0.08F * this.difficulty;
            case ICY -> 0.17F + 0.11F * this.difficulty;
        };
        this.freq = 0.012F + 0.011F * this.difficulty;
        RandomSource random = RandomSource.create();
        this.phase1 = random.nextFloat() * Mth.TWO_PI;
        this.phase2 = random.nextFloat() * Mth.TWO_PI;
        this.seed = random.nextLong();
    }

    int mistakes() {
        return this.mistakes;
    }

    // over, and the result should go to the server
    boolean isOver() {
        return this.state == State.DONE && Util.getMillis() - this.endedAt > END_PAUSE;
    }

    // touched the edge: the try is over (a needle broke)
    boolean snapped() {
        return this.mistakes > 0;
    }

    // ── the seam ──

    private static int clothWidth(int screenWidth) {
        return Mth.clamp((int) (screenWidth * 0.42F), 150, 260);
    }

    // the middle of the seam, from the cloth's middle, at this far along (as a share of the cloth's width). Each
    // material winds its own way:
    //  - WAVY (string, wool): wide swings left and right, some quicker or wider than others
    //  - WANDERING (leather and the rest): smooth, unpredictable drifting
    //  - RIGID (iron, gold): straight runs, then a sharp diagonal jump to a new spot
    //  - JITTERY (diamond): short, quick random turns
    //  - ICY (ice, snowball): a frost crack, sharp straight zig-zags snapping from side to side
    private float seamCenter(float s) {
        float wind = Mth.clamp((s - LEAD_IN) / 110.0F, 0.0F, 1.0F);
        float x = switch (this.style) {
            case WAVY -> {
                // not a perfect sine: the swings speed up and slow down, and some go wide while others stay small
                float f = this.freq * 1.5F;
                float turn = s * f + this.phase1 + 0.9F * Mth.sin(s * f * 0.37F + this.phase2);
                float reach = 0.72F + 0.28F * Mth.sin(s * f * 0.53F + this.phase2 * 1.7F);
                yield Mth.sin(turn) * reach;
            }
            case WANDERING -> Mth.sin(s * this.freq + this.phase1) * 0.7F + Mth.sin(s * this.freq * 2.3F + this.phase2) * 0.3F;
            case RIGID -> {
                float seg = 90 - 26 * this.difficulty;
                int i = Mth.floor(s / seg);
                float t = s / seg - i;
                yield t < 0.45F ? this.random(i) : Mth.lerp((t - 0.45F) / 0.55F, this.random(i), this.random(i + 1));
            }
            case JITTERY -> {
                float seg = 52 - 8 * this.difficulty;
                int i = Mth.floor(s / seg);
                float t = s / seg - i;
                float smooth = t * t * (3 - 2 * t);
                yield Mth.lerp(smooth, this.random(i), this.random(i + 1)) * 0.9F + Mth.sin(s * 0.17F + this.phase2) * 0.1F;
            }
            case ICY -> {
                float seg = 80 - 14 * this.difficulty;
                int i = Mth.floor(s / seg);
                float t = s / seg - i;
                yield Mth.lerp(t, this.crack(i), this.crack(i + 1));
            }
        };
        return wind * this.amp * x;
    }

    // a frost crack's corner: alternately left and right of the middle, some reaching further than others
    private float crack(int i) {
        return (i % 2 == 0 ? 1 : -1) * (0.35F + 0.65F * Math.abs(this.random(i)));
    }

    // how frozen the seam is there (ice): frost creeps in and out, pinching it narrower (1 = not at all)
    private float freeze(float s) {
        return 0.78F + 0.22F * Mth.cos(s * 0.045F + this.phase1);
    }

    // a steady random number in -1..1 for each step of the seam (the same every frame)
    private float random(int i) {
        long h = this.seed ^ (i * 0x9E3779B97F4A7C15L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF * 2 - 1;
    }

    // the seam's width there (share of the cloth's width): narrower in harder tiers and for diamonds, breathing a little
    private float seamWidth(float s) {
        float base = (0.23F - 0.12F * this.difficulty) * this.widthScale;
        float wind = Mth.clamp((s - LEAD_IN) / 110.0F, 0.0F, 1.0F);
        float width = base + (1 - wind) * 0.08F + 0.02F * Mth.sin(s * 0.021F + this.phase2);
        // quick turns (gems), big swings (string, wool) and sharp jumps (metal) would leave steep stretches thin: the
        // steeper the seam, the wider, so it's never thinner across than a straight stretch
        float slope = (this.seamCenter(s + 2) - this.seamCenter(s - 2)) / 4 * this.clothWidthNow;
        // metal's sharp jumps are the steepest, so they may widen further (else the jumps come out thin)
        if (this.style == Pattern.Style.ICY) width *= this.freeze(s);
        return width * Math.min(this.style == Pattern.Style.RIGID ? RIGID_WIDEN : 1.8F, (float) Math.sqrt(1 + slope * slope));
    }

    private static int luminance(int color) {
        return (((color >> 16) & 0xFF) * 3 + ((color >> 8) & 0xFF) * 6 + (color & 0xFF)) / 10;
    }

    private float along(long now) {
        if (this.state == State.READY) return 0;
        long until = this.state == State.DONE ? this.endedAt : now;
        return Math.min(this.length, (until - this.started) / 1000.0F * this.speed);
    }

    // ── input ──

    void click() {
        if (this.state == State.READY) {
            this.state = State.RUNNING;
            this.started = Util.getMillis();
            play(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.4F, 0.6F);
        }
    }

    // ── a frame: move the needle with the mouse, sew, check the lines, then draw ──

    void draw(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX) {
        long now = Util.getMillis();
        int fw = clothWidth(screenWidth);
        this.clothWidthNow = fw;
        int x0 = (screenWidth - fw) / 2;
        int y0 = 34;
        int fh = screenHeight - y0 - 26;
        int ny = y0 + Math.round(fh * 0.7F);
        float mid = x0 + fw / 2.0F;
        this.needleX = Mth.clamp(mouseX, x0 + 3, x0 + fw - 3);
        float s = this.along(now);

        if (this.state == State.RUNNING) this.sew(now, s, (this.needleX - mid) / fw);

        graphics.fill(0, 0, screenWidth, screenHeight, OVERLAY);
        this.drawBed(graphics, x0, y0, fw, fh);
        this.drawCloth(graphics, x0, y0, fw, fh, ny, s);
        this.drawTrail(graphics, x0, y0, fw, fh, ny, s);
        this.drawNeedle(graphics, now, ny, y0);
        this.drawHud(graphics, font, now, screenWidth, x0, y0, fw, fh, s);
    }

    private void sew(long now, float s, float x) {
        // stitches every couple of cloth pixels
        if (this.trail.isEmpty() || s - this.trail.getLast()[0] >= 2.0F) this.trail.add(new float[]{s, x});
        if (now - this.lastSound > STITCH_SOUND) {
            this.lastSound = now;
            play(SoundEvents.UI_BUTTON_CLICK, 1.9F + 0.1F * (float) Math.sin(now / 40.0), 0.07F);
        }
        float half = this.seamWidth(s) / 2;
        boolean outside = Math.abs(x - this.seamCenter(s)) > half;
        if (outside && (!this.off || now - this.lastMistake > GRACE)) {
            this.mistakes++;
            this.lastMistake = now;
            this.misses.add(new float[]{s, x});
            play(SoundEvents.ITEM_BREAK, 1.7F, 0.35F);
        }
        this.off = outside;
        if (this.snapped()) {
            this.finish(now);
            play(SoundEvents.ITEM_BREAK, 0.6F, 0.8F);
        } else if (s >= this.length) {
            this.finish(now);
            play(SoundEvents.UI_LOOM_TAKE_RESULT, 1.2F, 0.9F);
        }
    }

    private void finish(long now) {
        this.state = State.DONE;
        this.endedAt = now;
    }

    // ── drawing ──

    // the machine's bed round the cloth: black enamel with gold pinstripes, and the feed plate under the needle
    private void drawBed(GuiGraphicsExtractor graphics, int x0, int y0, int fw, int fh) {
        int bx0 = x0 - 26;
        int bx1 = x0 + fw + 26;
        graphics.fill(bx0, y0 - 6, bx1, y0 + fh + 6, ENAMEL);
        graphics.fill(bx0 + 2, y0 - 4, bx1 - 2, y0 + fh + 4, ENAMEL_LIGHT);
        graphics.fill(bx0 + 4, y0 - 2, bx1 - 4, y0 + fh + 2, ENAMEL);
        for (int side = 0; side < 2; side++) {
            int lx = side == 0 ? bx0 + 8 : bx1 - 9;
            graphics.fill(lx, y0 + 4, lx + 1, y0 + fh - 4, GOLD_DARK);
            // little gold scrolls down the pinstripe
            for (int y = y0 + 16; y < y0 + fh - 10; y += 36) {
                graphics.fill(lx - 2, y, lx + 3, y + 1, GOLD);
                graphics.fill(lx - 1, y - 1, lx + 2, y + 2, GOLD_DARK);
            }
        }
    }

    // the cloth, its weave sliding down as it feeds, the seam's chalk lines and the finishing mark
    private void drawCloth(GuiGraphicsExtractor graphics, int x0, int y0, int fw, int fh, int ny, float s) {
        graphics.fill(x0, y0, x0 + fw, y0 + fh, this.cloth);
        int weave = darker(this.cloth, 0.9F);
        int light = lighter(this.cloth, 1.08F);
        int shift = Math.floorMod((int) s, 4);
        for (int y = y0 + shift; y < y0 + fh; y += 4) graphics.fill(x0, y, x0 + fw, y + 1, weave);
        for (int x = x0 + 2; x < x0 + fw; x += 4) graphics.fill(x, y0, x + 1, y0 + fh, light);
        float mid = x0 + fw / 2.0F;
        for (int y = y0; y < y0 + fh; y++) {
            float at = s + (ny - y);
            if (at < 0) continue;
            if (at > this.length) {
                // the end of the seam: a gold tailor's mark across the cloth
                if ((int) at == (int) this.length + 1 || (int) at == (int) this.length + 2) {
                    for (int x = x0; x < x0 + fw; x += 6) graphics.fill(x, y, x + 3, y + 1, GOLD);
                }
                continue;
            }
            float c = mid + this.seamCenter(at) * fw;
            float half = this.seamWidth(at) * fw / 2;
            int left = Math.round(c - half);
            int right = Math.round(c + half);
            // the cloth outside the seam shaded, the inside a touch lighter, and the chalk lines (dashed; dark on light cloth)
            graphics.fill(x0, y, Math.max(x0, left - 1), y + 1, 0x38000000);
            graphics.fill(Math.min(x0 + fw, right + 1), y, x0 + fw, y + 1, 0x38000000);
            graphics.fill(left + 1, y, right, y + 1, 0x20FFFFFF);
            if (this.style == Pattern.Style.ICY) {
                // frozen stretches glow icy blue; frost crystals glint on the cloth
                float frozen = 1 - this.freeze(at);
                if (frozen > 0.12F) graphics.fill(left + 1, y, right, y + 1, ((int) (frozen * 300) << 24) | 0x7AD8FF);
                long h = (long) (int) at * 0x9E3779B97F4A7C15L;
                if (((h >>> 40) & 31) == 0) {
                    int gx = x0 + (int) ((h >>> 20) & 0xFFFF) % Math.max(1, fw);
                    graphics.fill(gx, y, gx + 1, y + 1, 0xFFFFFFFF);
                    graphics.fill(gx - 1, y, gx + 2, y + 1, 0x90FFFFFF);
                }
            }
            int chalk = this.light ? CHALK_DARK : CHALK;
            if (((int) at / 3) % 2 == 0) {
                graphics.fill(left - 1, y, left + 1, y + 1, chalk);
                graphics.fill(right - 1, y, right + 1, y + 1, chalk);
            }
        }
        // a soft shadow at the top and bottom edges, where the cloth goes under the machine
        for (int i = 0; i < 6; i++) {
            int alpha = (6 - i) * 22;
            graphics.fill(x0, y0 + i, x0 + fw, y0 + i + 1, alpha << 24);
            graphics.fill(x0, y0 + fh - 1 - i, x0 + fw, y0 + fh - i, alpha << 24);
        }
    }

    // the stitches already sewn, sliding away below the needle, and red crosses where it went off
    private void drawTrail(GuiGraphicsExtractor graphics, int x0, int y0, int fw, int fh, int ny, float s) {
        float mid = x0 + fw / 2.0F;
        for (int i = 1; i < this.trail.size(); i++) {
            float[] p = this.trail.get(i);
            int y = Math.round(ny + (s - p[0]));
            if (y > y0 + fh - 2 || y < y0) continue;
            int x = Math.round(mid + p[1] * fw);
            // a dash of thread every other stitch, with a dark hole between
            if (i % 2 == 0) {
                graphics.fill(x, y - 1, x + 1, y + 2, this.light ? THREAD_DARK : THREAD);
                graphics.fill(x + 1, y, x + 2, y + 1, 0x60FFFFFF);
            } else {
                graphics.fill(x, y, x + 1, y + 1, 0x80000000);
            }
        }
        for (float[] miss : this.misses) {
            int y = Math.round(ny + (s - miss[0]));
            if (y > y0 + fh - 4 || y < y0 + 2) continue;
            int x = Math.round(mid + miss[1] * fw);
            for (int d = -2; d <= 2; d++) {
                graphics.fill(x + d, y + d, x + d + 1, y + d + 1, RED);
                graphics.fill(x + d, y - d, x + d + 1, y - d + 1, RED);
            }
        }
    }

    // the needle: its bar coming down from the machine's head, bobbing in and out of the cloth while it sews, the
    // presser foot round it and the thread running up to the spool
    private void drawNeedle(GuiGraphicsExtractor graphics, long now, int ny, int y0) {
        int x = Math.round(this.needleX);
        boolean sewing = this.state == State.RUNNING;
        int bob = sewing ? (Math.sin(now / 18.0) > 0 ? 2 : -1) : 0;
        boolean flash = now - this.lastMistake < FLASH;
        // the thread up to the top of the screen
        graphics.fill(x + 1, y0 - 30, x + 2, ny - 18 + bob, 0xB0FFF4D6);
        // the head and the needle bar
        graphics.fill(x - 5, ny - 34, x + 6, ny - 24, ENAMEL);
        graphics.fill(x - 4, ny - 33, x + 5, ny - 25, ENAMEL_LIGHT);
        graphics.fill(x - 4, ny - 29, x + 5, ny - 28, GOLD);
        graphics.fill(x - 1, ny - 24, x + 2, ny - 8 + bob, 0xFFB8BEC6);
        graphics.fill(x - 1, ny - 24, x, ny - 8 + bob, 0xFFE4E8EE);
        // the presser foot
        int foot = flash ? RED : 0xFFA4AAB2;
        graphics.fill(x - 7, ny - 2, x + 8, ny + 2, 0xFF50565E);
        graphics.fill(x - 6, ny - 1, x + 7, ny + 1, foot);
        graphics.fill(x - 1, ny - 8, x + 2, ny - 1, 0xFF7A8088);
        // the needle itself, a bright point
        graphics.fill(x, ny - 8 + bob, x + 1, ny + 3 + bob, 0xFFF4F8FF);
        if (flash) {
            int alpha = (int) (200 * (1 - (now - this.lastMistake) / (float) FLASH));
            graphics.fill(x - 10, ny - 10, x + 11, ny + 11, (alpha / 3) << 24 | 0xFF2020);
        }
    }

    // what's being sewn, the needles left, how far along, and what to do
    private void drawHud(GuiGraphicsExtractor graphics, Font font, long now, int screenWidth, int x0, int y0, int fw, int fh, float s) {
        int cx = screenWidth / 2;
        String title = "Sewing on " + this.material.getHoverName().getString();
        int tw = font.width(title) + 20;
        graphics.item(this.material, cx - tw / 2, y0 - 26);
        graphics.text(font, title, cx - tw / 2 + 20, y0 - 22, TITLE, true);

        // the needles you have, down the left: grey and crossed out as they break (a count past 8)
        int lx = x0 - 22;
        int shown = Math.min(this.needles, 8);
        if (this.needles > 8) graphics.centeredText(font, (this.needles - this.mistakes) + "x", lx + 4, y0 + 6 + shown * 16, TITLE);
        for (int i = 0; i < shown; i++) {
            int ly = y0 + 6 + i * 16;
            boolean gone = i < this.mistakes;
            int color = gone ? 0xFF5A5A5A : 0xFFE8ECF4;
            graphics.fill(lx + 3, ly, lx + 4, ly + 11, color);
            graphics.fill(lx + 2, ly + 1, lx + 5, ly + 3, gone ? 0xFF5A5A5A : GOLD);
            if (gone) {
                for (int d = 0; d < 7; d++) graphics.fill(lx + d, ly + 2 + d, lx + d + 1, ly + 3 + d, RED);
            }
        }

        // the spool on the right climbing as the seam gets done
        int rx = x0 + fw + 12;
        int barTop = y0 + 6;
        int barBottom = y0 + fh - 6;
        graphics.fill(rx, barTop, rx + 3, barBottom, 0xFF0C1210);
        float done = Mth.clamp(s / this.length, 0, 1);
        int fill = Math.round((barBottom - barTop) * done);
        graphics.fill(rx, barBottom - fill, rx + 3, barBottom, GOLD);
        int sy = barBottom - fill;
        graphics.fill(rx - 3, sy - 4, rx + 6, sy + 4, 0xFF6A4424);
        graphics.fill(rx - 2, sy - 3, rx + 5, sy + 3, THREAD);
        graphics.fill(rx - 2, sy - 1, rx + 5, sy, 0xFFD8CCA8);

        int by = y0 + fh + 8;
        if (this.state == State.READY) {
            int pulse = (int) (180 + 75 * Math.sin(now / 220.0));
            graphics.centeredText(font, "Put the needle on the seam, then click to start", cx, by, (pulse << 24) | 0xF6E2B0);
            graphics.centeredText(font, "Move the mouse left and right. Stay inside the chalk!", cx, y0 + 6, TEXT);
        } else if (this.state == State.DONE) {
            boolean ok = !this.snapped();
            String result = ok ? "Sewn on!" : "A needle broke!";
            int color = ok ? 0xFFFFD86A : RED;
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, y0 + fh / 3.0F);
            graphics.pose().scale(2.0F, 2.0F);
            graphics.centeredText(font, result, 0, 0, color);
            graphics.pose().popMatrix();
            if (!ok) graphics.centeredText(font, "You touched the edge. Try this one again.", cx, y0 + fh / 3 + 22, TEXT);
        } else {
            graphics.centeredText(font, "Esc to stop (nothing is used)", cx, by, 0xFF7E8C84);
        }
    }

    private static void play(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float pitch, float volume) {
        play(sound.value(), pitch, volume);
    }

    private static void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static int darker(int color, float f) {
        return 0xFF000000 | (Math.round(((color >> 16) & 0xFF) * f) << 16) | (Math.round(((color >> 8) & 0xFF) * f) << 8) | Math.round((color & 0xFF) * f);
    }

    private static int lighter(int color, float f) {
        return 0xFF000000 | (Math.min(255, Math.round(((color >> 16) & 0xFF) * f)) << 16)
            | (Math.min(255, Math.round(((color >> 8) & 0xFF) * f)) << 8) | Math.min(255, Math.round((color & 0xFF) * f));
    }
}

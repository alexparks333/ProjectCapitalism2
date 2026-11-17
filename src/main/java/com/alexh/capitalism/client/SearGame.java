package com.alexh.capitalism.client;

import com.alexh.capitalism.farmer.MeatCut;
import com.alexh.capitalism.farmer.Sear;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// The searing game, the Busy Grill (Cook.md > The Grill): up to three cuts on the grill at once, each cooking at its
// own pace, and the Cook juggles them:
//  - Each steak's thermometer (the doneness bar under it) climbs; PULL it off (its button, or Q / W / E) when it's at
//    Medium Rare (chicken: Well Done), the gold band. It rests a few degrees more on the board, then it's done.
//  - Each steak needs turning: after a while on one side a FLIP! bubble pops up over it, and it has to be flipped
//    (click it, or 1 / 2 / 3) before the bubble runs out, or it goes up in flames: burnt (Burnt Food).
// Esc pulls everything off. As each steak comes off its result (slot, final temperature) goes to the server
// (GrillMenu.RESULT); the game's over once they're all off.
final class SearGame {

    private static final float START = 68;
    private static final long FLIP_WINDOW = 1500;
    private static final long FLIP_ANIM = 260;
    private static final long REST_TIME = 1400;
    private static final long END_PAUSE = 2400;
    private static final int GAUGE_MIN = 60;
    private static final int GAUGE_MAX = 200;
    private static final int[] BAND_COLORS = {0xFF7A1A22, 0xFFB8283A, 0xFFD24A54, 0xFFC86A5E, 0xFFA87458, 0xFF7A5A44};
    private static final String[] LETTERS = {"RAW", "R", "MR", "M", "MW", "WD"};

    private final class Steak {
        final int slot;
        final MeatCut cut;
        final ItemStack icon;
        final int meatColor;
        final float heat;          // °F a second
        float temperature = START + SearGame.this.random.nextFloat() * 4;
        long sideSince;            // when it went on this side
        long sideTime;             // how long this side wants before it needs a flip
        long flipAt = Long.MIN_VALUE / 2;
        long flareUntil;
        boolean pulled;
        long pulledAt;
        float restFrom;
        float carry;
        boolean done;
        boolean sent;
        // where it was drawn
        int x0, y0, x1, y1, px0, py0, px1, py1;

        Steak(int slot, MeatCut cut, long now) {
            this.slot = slot;
            this.cut = cut;
            this.icon = new ItemStack(cut.cut());
            this.meatColor = switch (cut.animal) {
                case COW -> 0xB8322C;
                case PIG -> 0xE0867E;
                case SHEEP -> 0xAA4438;
                case CHICKEN -> 0xF0B49C;
                case RABBIT -> 0xC4766A;
            };
            // each cooks at its own pace, so they come due at different times
            this.heat = 3.0F + SearGame.this.random.nextFloat() * 2.4F;
            this.sideSince = now + SearGame.this.random.nextInt(800);
            this.sideTime = this.nextSideTime();
        }

        long nextSideTime() {
            return 3200 + SearGame.this.random.nextInt(2600);
        }

        boolean wantsFlip(long now) {
            return !this.pulled && now - this.sideSince >= this.sideTime;
        }

        Sear.Doneness goal() {
            return this.cut.animal == MeatCut.Animal.CHICKEN ? Sear.Doneness.WELL_DONE : Sear.Doneness.MEDIUM_RARE;
        }
    }

    private final RandomSource random = RandomSource.create();
    private final List<Steak> steaks = new ArrayList<>();
    private long lastFrame;
    private long lastSizzle;
    private long endedAt = -1;
    private final List<float[]> smoke = new ArrayList<>();

    // cuts: the Grill's slots (null where nothing went on)
    SearGame(MeatCut[] cuts) {
        long now = Util.getMillis();
        for (int i = 0; i < cuts.length; i++) {
            if (cuts[i] != null) this.steaks.add(new Steak(i, cuts[i], now));
        }
        this.lastFrame = now;
        play(SoundEvents.FIRECHARGE_USE, 1.3F, 0.5F);
    }

    boolean isOver() {
        return this.endedAt >= 0 && Util.getMillis() - this.endedAt > END_PAUSE;
    }

    // The steaks that came off since last asked, as GrillMenu.RESULT codes (slot x 256 + final temperature)
    List<Integer> takeResults() {
        List<Integer> out = new ArrayList<>();
        for (Steak s : this.steaks) {
            if (s.done && !s.sent) {
                s.sent = true;
                out.add(s.slot * 256 + Math.max(0, Math.min(250, Math.round(s.temperature))));
            }
        }
        return out;
    }

    // ── input ──

    void press(double x, double y, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) return;
        for (Steak s : this.steaks) {
            if (x >= s.px0 && x < s.px1 && y >= s.py0 && y < s.py1) this.pull(s);
            else if (x >= s.x0 && x < s.x1 && y >= s.y0 && y < s.y1) this.flip(s);
        }
    }

    void key(int key) {
        int flip = key == 49 ? 0 : key == 50 ? 1 : key == 51 ? 2 : -1;
        int pull = key == 81 ? 0 : key == 87 ? 1 : key == 69 ? 2 : -1;
        if (flip >= 0 && flip < this.steaks.size()) this.flip(this.steaks.get(flip));
        if (pull >= 0 && pull < this.steaks.size()) this.pull(this.steaks.get(pull));
    }

    void giveUp() {
        for (Steak s : this.steaks) this.pull(s);
    }

    private void flip(Steak s) {
        if (s.pulled) return;
        long now = Util.getMillis();
        s.flipAt = now;
        s.sideSince = now;
        s.sideTime = s.nextSideTime();
        play(SoundEvents.FIRE_EXTINGUISH, 1.6F, 0.25F);
    }

    private void pull(Steak s) {
        if (s.pulled) return;
        s.pulled = true;
        s.pulledAt = Util.getMillis();
        s.restFrom = s.temperature;
        s.carry = 3 + this.random.nextFloat() * 2;
        play(SoundEvents.ITEM_PICKUP, 0.8F, 0.6F);
    }

    // ── a frame ──

    private void update(long now, float seconds) {
        boolean allDone = true;
        for (Steak s : this.steaks) {
            if (!s.pulled) {
                s.temperature += s.heat * seconds;
                // not flipped in time: it goes up in flames and it's burnt
                if (now - s.sideSince >= s.sideTime + FLIP_WINDOW) {
                    s.flareUntil = now + 1000;
                    s.temperature = Sear.Doneness.BURNT + 20;
                    s.pulled = true;
                    s.done = true;
                    play(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
                    play(SoundEvents.FIRE_EXTINGUISH, 0.7F, 0.6F);
                    continue;
                }
                if (s.temperature >= GAUGE_MAX) this.pull(s);
            } else if (!s.done) {
                float t = Mth.clamp((now - s.pulledAt) / (float) REST_TIME, 0, 1);
                s.temperature = s.restFrom + s.carry * (1 - (1 - t) * (1 - t));
                if (t >= 1) {
                    s.done = true;
                    boolean burnt = s.temperature >= Sear.Doneness.BURNT;
                    boolean perfect = !burnt && Sear.Doneness.of(Math.round(s.temperature)) == s.goal();
                    play(burnt ? SoundEvents.FIRE_EXTINGUISH : perfect ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BELL.value(),
                        perfect ? 1.5F : 1.0F, 0.45F);
                }
            }
            allDone &= s.done;
        }
        boolean cooking = false;
        for (Steak s : this.steaks) cooking |= !s.pulled;
        if (cooking && now - this.lastSizzle > 380) {
            this.lastSizzle = now;
            play(SoundEvents.FIRE_EXTINGUISH, 1.7F + this.random.nextFloat() * 0.3F, 0.07F);
        }
        if (allDone && this.endedAt < 0) this.endedAt = now;
    }

    void draw(GuiGraphicsExtractor graphics, Font font, int sw, int sh, int mouseX, int mouseY) {
        long now = Util.getMillis();
        float seconds = Math.min(100, now - this.lastFrame) / 1000.0F;
        this.lastFrame = now;
        this.update(now, seconds);

        int n = Math.max(1, this.steaks.size());
        int station = 104;
        int w = Math.max(220, n * station + 16);
        int h = 214;
        int x0 = sw / 2 - w / 2;
        int y0 = Math.max(6, sh / 2 - h / 2);
        graphics.fill(0, 0, sw, sh, 0xE80C0806);
        graphics.fill(x0 - 3, y0 - 3, x0 + w + 3, y0 + h + 3, 0xFF050403);
        graphics.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, 0xFF5A4636);
        for (int y = 0; y < h; y += 2) {
            float t = y / (float) h;
            int r = (int) (30 + 34 * t * t);
            int g = (int) (24 + 12 * t * t);
            graphics.fill(x0, y0 + y, x0 + w, y0 + y + 2, 0xFF000000 | (r << 16) | (g << 8) | 18);
        }
        graphics.centeredText(font, "Busy Grill", x0 + w / 2, y0 + 6, 0xFFFFE6C0);
        graphics.centeredText(font, "Pull each at ★ Medium Rare", x0 + w / 2, y0 + 17, 0xFFFFD86A);

        // one long grill under all the steaks
        int gx0 = x0 + 8;
        int gx1 = x0 + w - 8;
        int gy0 = y0 + 30;
        int gy1 = y0 + 118;
        graphics.fill(gx0 - 2, gy0 - 2, gx1 + 2, gy1 + 2, 0xFF0A0908);
        boolean cooking = false;
        for (Steak s : this.steaks) cooking |= !s.pulled;
        for (int cy = gy0; cy < gy1; cy += 5) {
            for (int cx = gx0; cx < gx1; cx += 5) {
                float glow = 0.5F + 0.5F * Mth.sin(now / 300.0F + cx * 0.41F + cy * 0.67F);
                if (!cooking) glow *= 0.35F;
                int r = (int) (70 + 160 * glow);
                int g = (int) (16 + 80 * glow * glow);
                graphics.fill(cx, cy, Math.min(gx1, cx + 5), Math.min(gy1, cy + 5), 0xFF000000 | (r << 16) | (g << 8) | 8);
            }
        }
        for (int bx = gx0 + 3; bx < gx1; bx += 10) {
            graphics.fill(bx, gy0, bx + 3, gy1, 0xFF2E2E32);
            graphics.fill(bx, gy0, bx + 1, gy1, 0xFF6E6E76);
        }

        for (int i = 0; i < this.steaks.size(); i++) {
            Steak s = this.steaks.get(i);
            int cx = x0 + 8 + (w - 16) * (2 * i + 1) / (2 * n);
            this.drawStation(graphics, font, s, i, cx, gy0, gy1, now, mouseX, mouseY);
        }

        // smoke
        if (cooking && this.random.nextInt(3) == 0 && this.smoke.size() < 80) {
            this.smoke.add(new float[]{gx0 + this.random.nextFloat() * (gx1 - gx0), (gy0 + gy1) / 2.0F, now});
        }
        for (int i = this.smoke.size() - 1; i >= 0; i--) {
            float[] p = this.smoke.get(i);
            float age = (now - p[2]) / 1700.0F;
            if (age >= 1) {
                this.smoke.remove(i);
                continue;
            }
            int sx = Math.round(p[0] + Mth.sin(age * 6 + i) * 5);
            int sy = Math.round(p[1] - age * 70);
            graphics.fill(sx, sy, sx + 2 + Math.round(age * 3), sy + 2 + Math.round(age * 3), (Math.round(100 * (1 - age)) << 24) | 0xC8C0B8);
        }

        String hint = this.endedAt >= 0 ? "All off the grill!" : "Click a steak (1/2/3): flip · PULL (Q/W/E) · Esc: all off";
        graphics.centeredText(font, hint, x0 + w / 2, y0 + h - 12, this.endedAt >= 0 ? 0xFF9CF08A : 0xFFA89880);
    }

    // One steak: on the grill (or on the board once it's off), its FLIP! bubble, flames, its doneness bar and PULL
    private void drawStation(GuiGraphicsExtractor graphics, Font font, Steak s, int index, int cx, int gy0, int gy1,
                             long now, int mouseX, int mouseY) {
        int cy = (gy0 + gy1) / 2 + 6;
        boolean flare = now < s.flareUntil;
        if (s.pulled) {
            // the board it rests on
            graphics.fill(cx - 40, cy - 30, cx + 40, cy + 30, 0xFF9A6A3A);
            graphics.fill(cx - 38, cy - 28, cx + 38, cy + 28, 0xFFB07A44);
        }
        if (flare) {
            for (int k = 0; k < 10; k++) {
                int fx = cx - 36 + (int) ((k * 23L + now / 25) % 72);
                int fh = 10 + (int) (14 * Math.abs(Mth.sin(now / 70.0F + k)));
                graphics.fill(fx, cy + 20 - fh, fx + 4, cy + 20, 0xC8FF6A10);
                graphics.fill(fx + 1, cy + 24 - fh, fx + 3, cy + 20, 0xE0FFD040);
            }
        }
        // the steak (lifted and turned while it flips)
        float t = Mth.clamp((now - s.flipAt) / (float) FLIP_ANIM, 0, 1);
        float squash = t < 1 ? Math.abs(Mth.cos(t * Mth.PI)) : 1;
        int lift = t < 1 ? (int) (Mth.sin(t * Mth.PI) * 14) : 0;
        int hw = 34;
        int hh = Math.max(2, Math.round(24 * squash));
        float cooked = Mth.clamp((s.temperature - START) / (Sear.Doneness.BURNT - START), 0, 1.3F);
        this.drawSteak(graphics, s, cx, cy - lift, hw, hh, cooked);
        s.x0 = cx - hw;
        s.x1 = cx + hw;
        s.y0 = cy - 24;
        s.y1 = cy + 24;
        graphics.text(font, String.valueOf(index + 1), cx - hw - 2, cy - 28, 0xFFFFE6C0, true);
        graphics.item(s.icon, cx + hw - 14, cy - 30);

        // FLIP! bubble, its bar running out
        if (s.wantsFlip(now)) {
            float left = 1 - Mth.clamp((now - s.sideSince - s.sideTime) / (float) FLIP_WINDOW, 0, 1);
            int by = cy - 40;
            boolean blink = (now / 120) % 2 == 0;
            graphics.fill(cx - 20, by - 7, cx + 20, by + 7, left < 0.35F && blink ? 0xFFE04A3A : 0xFFFFD86A);
            graphics.fill(cx - 19, by + 4, cx - 19 + Math.round(38 * left), by + 6, 0xFF2A1A0E);
            graphics.centeredText(font, "FLIP!", cx, by - 4, 0xFF2A1A0E);
        }

        // its doneness bar: the bands, the goal in gold, a needle
        int bx0 = cx - 44;
        int bx1 = cx + 44;
        int by0 = gy1 + 8;
        int by1 = by0 + 8;
        graphics.fill(bx0 - 1, by0 - 1, bx1 + 1, by1 + 1, 0xFF0A0604);
        for (Sear.Doneness d : Sear.Doneness.values()) {
            int a = this.barX(bx0, bx1, Math.max(d.min, GAUGE_MIN));
            int b = this.barX(bx0, bx1, Math.min(d.max + 1, GAUGE_MAX));
            if (b <= a) continue;
            graphics.fill(a, by0, b, by1, BAND_COLORS[d.ordinal()]);
            if (d == s.goal()) {
                graphics.fill(a, by0 - 2, b, by0 - 1, 0xFFFFD86A);
                graphics.fill(a, by1 + 1, b, by1 + 2, 0xFFFFD86A);
            }
        }
        graphics.fill(this.barX(bx0, bx1, Sear.Doneness.BURNT), by0, bx1, by1, 0xFF1A0E08);
        int nx = this.barX(bx0, bx1, Mth.clamp(s.temperature, GAUGE_MIN, GAUGE_MAX));
        graphics.fill(nx - 1, by0 - 3, nx + 1, by1 + 3, 0xFFFFFFFF);

        // how done it is now, and the temperature
        boolean burnt = s.temperature >= Sear.Doneness.BURNT;
        Sear.Doneness done = Sear.Doneness.of(Math.round(s.temperature));
        String label = burnt ? "BURNT" : LETTERS[done.ordinal()] + "  " + Math.round(s.temperature) + "°";
        int color = burnt ? 0xFFFF5A4A : done == s.goal() ? 0xFFFFD86A : 0xFFFFFFFF;
        if (s.done && !burnt && done == s.goal()) label = "★ " + done.title;
        else if (s.done && !burnt) label = done.title;
        graphics.centeredText(font, label, cx, by1 + 5, color);

        // PULL
        s.px0 = cx - 30;
        s.px1 = cx + 30;
        s.py0 = by1 + 17;
        s.py1 = by1 + 33;
        if (!s.pulled) {
            boolean hover = mouseX >= s.px0 && mouseX < s.px1 && mouseY >= s.py0 && mouseY < s.py1;
            graphics.fill(s.px0, s.py0 + 2, s.px1, s.py1 + 2, 0xFF2A0A06);
            graphics.fill(s.px0, s.py0, s.px1, s.py1, hover ? 0xFFE0602E : 0xFFC24A22);
            graphics.fill(s.px0, s.py0, s.px1, s.py0 + 1, 0xFFFFA070);
            graphics.centeredText(font, "PULL " + "QWE".charAt(index), cx, s.py0 + 4, 0xFFFFFFFF);
        } else if (!s.done) {
            graphics.centeredText(font, "Resting...", cx, s.py0 + 4, 0xFFFFD86A);
        }
    }

    private int barX(int x0, int x1, float temperature) {
        return x0 + Math.round((temperature - GAUGE_MIN) / (GAUGE_MAX - GAUGE_MIN) * (x1 - x0));
    }

    // A steak from above: browning as it cooks, grill marks once seared, a strip of fat
    private void drawSteak(GuiGraphicsExtractor graphics, Steak s, int cx, int cy, int hw, int hh, float cooked) {
        int seared = mix(s.meatColor, 0x9A5428, Mth.clamp(cooked * 1.6F, 0, 1));
        int color = mix(seared, 0x2A1A12, Mth.clamp(Math.max(0, cooked - 1) * 2.5F, 0, 0.85F));
        for (int dy = -hh; dy <= hh; dy++) {
            float v = dy / (float) hh;
            int half = (int) (hw * Math.sqrt(Math.max(0, 1 - Math.pow(Math.abs(v), 3))));
            if (half <= 0) continue;
            int shade = v < -0.55F ? mix(color, 0xFFE0C0, 0.12F) : v > 0.6F ? mix(color, 0x000000, 0.22F) : color;
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF000000 | shade);
            graphics.fill(cx + half - 4, cy + dy, cx + half, cy + dy + 1, 0xFF000000 | mix(0xF2E8CC, color, Mth.clamp(cooked, 0, 1) * 0.55F));
        }
        if (cooked > 0.25F && hh > 6) {
            int mark = 0xFF000000 | mix(color, 0x140A06, 0.65F);
            for (int i = -2; i <= 2; i++) {
                int mx = cx + i * 12;
                for (int k = -hh + 4; k < hh - 4; k++) graphics.fill(mx + k / 3, cy + k, mx + k / 3 + 2, cy + k + 1, mark);
            }
        }
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}

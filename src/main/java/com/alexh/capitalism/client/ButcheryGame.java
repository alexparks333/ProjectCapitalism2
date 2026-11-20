package com.alexh.capitalism.client;

import com.alexh.capitalism.farmer.Freshness;
import com.alexh.capitalism.farmer.MeatCut;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// The butchery game (Farmer.md > Butchering), cutting up one Meat Slab on the Butcher's Table. The slab lies on the
// chopping block, a grid of little cells of meat, fat and (hidden) bone, in three steps:
//  1. TRIM: hold the mouse and drag the knife over the pale fat round the edges to slice off every bit of it. No clock:
//     it's about accuracy. Every bit of meat the knife takes counts as meat lost: lose too much and it's botched. The
//     last few bits of fat glow so they can be found.
//  2. SCAN: hold the mouse to look into the meat with the bone scanner: bones glow under the lens (and fade soon after),
//     and it ticks faster the nearer a bone is. Its battery only lasts a few seconds. Right-click drops a pin to mark a
//     spot (they stay for step 3). Space ends the scan early.
//  3. CUT: two cuts, each in one go from above the top line to below the bottom one, not too close together. The knife
//     may wind between the bones, but touching one is a "clunk": botched.
// Every slab has at least two clear ways through (lanes the bones are kept off). Rarer cuts have more bones and tighter
// gaps; ribs and spines are rows to cut between. Played on the client like the sewing game; the result goes to the server
// (ButchersTableMenu.SUCCESS / FAILED). Esc gives up (plain meat).
final class ButcheryGame {

    private enum Step { TRIM, SCAN, CUT, DONE }

    private static final int CELL = 2;
    private static final byte GONE = 0;
    private static final byte MEAT = 1;
    private static final byte FAT = 2;

    private static final long SCAN_TIME = 2500;
    private static final long CUT_TIME = 20000;
    private static final long INTRO = 1300;
    private static final long END_PAUSE = 2200;
    private static final long AFTERGLOW = 900;
    private static final long MESSAGE = 1800;
    // botched past this much meat lost while trimming; the leftover fat glows once this little is left
    private static final float MEAT_LIMIT = 0.05F;
    private static final float FAT_GLOW = 0.15F;
    // the knife's reach when trimming (shown as a ring round the mouse), and the scanner lens (cells)
    private static final float KNIFE = 2.0F;
    private static final float LENS = 9.0F;
    // the second cut must stay this far from the first (cells)
    private static final float MIN_GAP = 5.0F;
    private static final int MAX_PINS = 10;
    // the strip along the board's bottom, under the slab, where all the words go (never over the meat)
    private static final int TEXT_STRIP = 56;

    private static final int OVERLAY = 0xEC120C0A;
    private static final int TITLE = 0xFFF6E2B0;
    private static final int TEXT = 0xFFD8CCB8;
    private static final int DIM = 0xFF8A7E70;
    private static final int GOLD = 0xFFFFD86A;
    private static final int RED = 0xFFFF5A4A;
    private static final int GREEN = 0xFF9CF08A;
    private static final int SCAN = 0xFF7AE0FF;
    private static final int BONE = 0xFFEFE8D4;
    private static final int BONE_EDGE = 0xFFC2B494;

    private final MeatCut cut;
    // how much it gives (the slab's lean, Freshness.yield)
    private final int count;
    private final ItemStack slab;
    private final int gw;
    private final int gh;
    private final byte[] cells;
    private final boolean[] bone;
    private final byte[] shade;
    private final long[] seenAt;
    private final int meatColor;
    private final int fatColor;
    private final int meatTotal;
    private final int fatTotal;
    private int meatLost;
    private int fatLeft;
    // the clear lanes: {base x, swing, frequency, phase}
    private final float[][] lanes = new float[2][];
    // board decoration: {x, y, length, light?} scratches and {x, y, r} stains, as shares of the board
    private final float[][] scratches = new float[14][];
    private final float[][] stains = new float[4][];

    private Step step = Step.TRIM;
    private long stepAt;
    private long trimStarted = -1;
    private long scanUsed;
    // Steady Hands (Butcher's Bloody Mary): the scanner lasts 50% longer, trimming may lose 8% of the meat (not 5%)
    private final long scanTime;
    private final float meatLimit;
    private long lastTick;
    private long lastFrame;
    private long lastSound;
    private boolean held;
    private boolean hasLast;
    private float lastGx;
    private float lastGy;
    private final List<int[]> pins = new ArrayList<>();
    // the finished cuts: x for each row; and the cut being made
    private final List<float[]> cuts = new ArrayList<>();
    private final List<float[]> stroke = new ArrayList<>();
    private float[] strokeRows;
    private boolean stroking;
    private boolean success;
    private String why = "";
    private long endedAt;
    private String message = "";
    private long messageAt = Long.MIN_VALUE / 2;
    private long flashAt = Long.MIN_VALUE / 2;
    private long shakeAt = Long.MIN_VALUE / 2;
    private int hitX = -1;
    private int hitY = -1;
    private final List<float[]> chunks = new ArrayList<>();
    // how bent this slab's bones are (placeBones)
    private float bendScale = 1.0F;
    private final RandomSource random = RandomSource.create();
    // where the slab was drawn last frame
    private int sx0;
    private int sy0;

    // lean: the slab's lean % (Freshness): the leaner, the less fat there is to trim
    ButcheryGame(MeatCut cut, int count, int lean) {
        boolean steady = net.minecraft.client.Minecraft.getInstance().player != null
            && net.minecraft.client.Minecraft.getInstance().player.hasEffect(com.alexh.capitalism.core.ModEffects.STEADY_HANDS);
        this.scanTime = steady ? SCAN_TIME * 3 / 2 : SCAN_TIME;
        this.meatLimit = steady ? 0.08F : MEAT_LIMIT;
        this.cut = cut;
        this.count = count;
        this.slab = new ItemStack(cut.slab());
        RandomSource random = this.random;
        float exponent;
        switch (cut.animal) {
            case COW -> { this.gw = 88; this.gh = 54; exponent = 3.2F; this.meatColor = 0xB8322C; this.fatColor = 0xF2E8CC; }
            case PIG -> { this.gw = 84; this.gh = 52; exponent = 3.0F; this.meatColor = 0xE0867E; this.fatColor = 0xFAF2E2; }
            case SHEEP -> { this.gw = 76; this.gh = 48; exponent = 2.6F; this.meatColor = 0xAA4438; this.fatColor = 0xF4EAD0; }
            case CHICKEN -> { this.gw = 62; this.gh = 42; exponent = 2.2F; this.meatColor = 0xF0B49C; this.fatColor = 0xF6E2A2; }
            default -> { this.gw = 58; this.gh = 40; exponent = 2.4F; this.meatColor = 0xC4766A; this.fatColor = 0xF2E6CE; }
        }
        int size = this.gw * this.gh;
        this.cells = new byte[size];
        this.bone = new boolean[size];
        this.shade = new byte[size];
        this.seenAt = new long[size];
        Arrays.fill(this.seenAt, Long.MIN_VALUE / 2);
        this.strokeRows = new float[this.gh];

        // the slab's outline: a rounded box with a wobbly edge
        float p1 = random.nextFloat() * Mth.TWO_PI;
        float p2 = random.nextFloat() * Mth.TWO_PI;
        for (int y = 0; y < this.gh; y++) {
            for (int x = 0; x < this.gw; x++) {
                float u = (x + 0.5F - this.gw / 2.0F) / (this.gw / 2.0F);
                float v = (y + 0.5F - this.gh / 2.0F) / (this.gh / 2.0F);
                float angle = (float) Math.atan2(v, u);
                float edge = 0.95F + 0.035F * Mth.sin(angle * 3 + p1) + 0.02F * Mth.sin(angle * 7 + p2);
                float r = (float) Math.pow(Math.pow(Math.abs(u), exponent) + Math.pow(Math.abs(v), exponent), 1.0 / exponent);
                if (r < edge) this.cells[y * this.gw + x] = MEAT;
            }
        }
        // marbling: smooth shades of meat, with thin streaks of white through beef and pork
        float p3 = random.nextFloat() * Mth.TWO_PI;
        float p4 = random.nextFloat() * Mth.TWO_PI;
        boolean streaky = cut.animal == MeatCut.Animal.COW || cut.animal == MeatCut.Animal.PIG || cut.animal == MeatCut.Animal.SHEEP;
        for (int y = 0; y < this.gh; y++) {
            for (int x = 0; x < this.gw; x++) {
                float n = Mth.sin(x * 0.31F + p1) * Mth.cos(y * 0.43F + p2) + 0.6F * Mth.sin((x * 0.7F - y * 0.5F) * 0.35F + p3);
                byte s = (byte) (n < -0.8F ? 0 : n < 0.0F ? 1 : n < 0.9F ? 2 : 3);
                if (streaky && Math.abs(Mth.sin(x * 0.22F + y * 0.9F + p4 + Mth.sin(y * 0.3F + p1) * 2)) < 0.07F) s = 4;
                this.shade[y * this.gw + x] = s;
            }
        }

        int[] dist = this.edgeDistance();
        // fat: patches hugging the edge, thickest in their middle
        // how fatty it is: 0 at 100% lean, 1 at 60%
        float fatty = Mth.clamp((Freshness.LEAN_MAX - lean) / (float) (Freshness.LEAN_MAX - Freshness.LEAN_MIN), 0, 1);
        int patches = 2 + Math.round(5 * fatty) + random.nextInt(2);
        List<Integer> edgeCells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (dist[i] == 1) edgeCells.add(i);
        }
        for (int p = 0; p < patches && !edgeCells.isEmpty(); p++) {
            int at = edgeCells.get(random.nextInt(edgeCells.size()));
            int px = at % this.gw;
            int py = at / this.gw;
            float reach = 7 + random.nextInt(9);
            float thick = 1 + Math.round(2 * fatty) + random.nextInt(2 + Math.round(fatty));
            float phase = random.nextFloat() * 10;
            for (int y = Math.max(0, (int) (py - reach)); y < Math.min(this.gh, py + reach + 1); y++) {
                for (int x = Math.max(0, (int) (px - reach)); x < Math.min(this.gw, px + reach + 1); x++) {
                    int i = y * this.gw + x;
                    if (this.cells[i] == GONE) continue;
                    float d = Mth.sqrt((x - px) * (x - px) + (y - py) * (y - py)) / reach;
                    float jag = 0.6F * Mth.sin(x * 0.9F + y * 0.7F) + 0.9F * Mth.sin(x * 1.9F - y * 1.4F + phase);
                    if (d < 1 && dist[i] <= Math.max(1, thick * (1 - d * d) + jag)) this.cells[i] = FAT;
                }
            }
        }
        // loose specks of fat a little way in from the edge, easy to miss
        int specks = 2 + Math.round(10 * fatty) + random.nextInt(3);
        for (int s = 0; s < specks; s++) {
            int at = random.nextInt(size);
            if (this.cells[at] != MEAT || dist[at] < 2 || dist[at] > 4) continue;
            int px = at % this.gw;
            int py = at / this.gw;
            for (int y = py; y < Math.min(this.gh, py + 2); y++) {
                for (int x = px; x < Math.min(this.gw, px + 1 + random.nextInt(2)); x++) {
                    if (this.cells[y * this.gw + x] == MEAT) this.cells[y * this.gw + x] = FAT;
                }
            }
        }

        // the lanes kept clear of bone, about a third and two thirds across
        for (int i = 0; i < 2; i++) {
            this.lanes[i] = new float[]{this.gw * (i == 0 ? 0.33F : 0.67F) + random.nextInt(7) - 3, 1.5F + random.nextFloat() * 4.5F,
                0.06F + random.nextFloat() * 0.1F, random.nextFloat() * Mth.TWO_PI};
        }
        this.placeBones(dist);

        int meat = 0;
        int fat = 0;
        for (byte c : this.cells) {
            if (c == MEAT) meat++;
            else if (c == FAT) fat++;
        }
        this.meatTotal = Math.max(1, meat);
        this.fatTotal = Math.max(1, fat);
        this.fatLeft = fat;

        for (int i = 0; i < this.scratches.length; i++) {
            this.scratches[i] = new float[]{random.nextFloat(), random.nextFloat(), 6 + random.nextInt(14), random.nextInt(2)};
        }
        for (int i = 0; i < this.stains.length; i++) {
            this.stains[i] = new float[]{random.nextFloat(), random.nextFloat(), 3 + random.nextInt(5)};
        }
        long now = Util.getMillis();
        this.stepAt = now;
        this.lastFrame = now;
    }

    // ── making the slab ──

    // How many cells in from the edge each cell is (1 on the edge; 0 outside)
    private int[] edgeDistance() {
        int[] dist = new int[this.gw * this.gh];
        int[] queue = new int[dist.length];
        int head = 0;
        int tail = 0;
        for (int y = 0; y < this.gh; y++) {
            for (int x = 0; x < this.gw; x++) {
                int i = y * this.gw + x;
                if (this.cells[i] == GONE) continue;
                if (!this.solid(x - 1, y) || !this.solid(x + 1, y) || !this.solid(x, y - 1) || !this.solid(x, y + 1)) {
                    dist[i] = 1;
                    queue[tail++] = i;
                }
            }
        }
        while (head < tail) {
            int i = queue[head++];
            int x = i % this.gw;
            int y = i / this.gw;
            int[][] near = {{x - 1, y}, {x + 1, y}, {x, y - 1}, {x, y + 1}};
            for (int[] n : near) {
                if (!this.solid(n[0], n[1])) continue;
                int j = n[1] * this.gw + n[0];
                if (dist[j] == 0) {
                    dist[j] = dist[i] + 1;
                    queue[tail++] = j;
                }
            }
        }
        return dist;
    }

    // The top / bottom row of meat in a column (after trimming), or -1 if there's none
    private int topOf(int x) {
        for (int y = 0; y < this.gh; y++) if (this.solid(x, y)) return y;
        return -1;
    }

    private int bottomOf(int x) {
        for (int y = this.gh - 1; y >= 0; y--) if (this.solid(x, y)) return y;
        return -1;
    }

    private boolean solid(int x, int y) {
        return x >= 0 && y >= 0 && x < this.gw && y < this.gh && this.cells[y * this.gw + x] != GONE;
    }

    private float laneX(int lane, float y) {
        float[] l = this.lanes[lane];
        return l[0] + l[1] * Mth.sin(y * l[2] + l[3]);
    }

    // The bones: groups of capsules {ax, ay, bx, by, r} in the cut's style, each kept off the lanes and in from the edge
    private void placeBones(int[] dist) {
        RandomSource random = this.random;
        List<float[]> placed = new ArrayList<>();
        // rarer cuts leave tighter gaps round the lanes
        float margin = this.cut.rarity >= 2 ? 2.6F : this.cut.rarity == 1 ? 3.2F : 3.8F;
        float spineY = this.gh * (0.4F + random.nextFloat() * 0.2F);
        // the cut's bones, one more or one fewer at random
        int groups = Math.max(1, this.cut.boneCount + random.nextInt(3) - 1);
        // how bent this slab's bones are: gently bowed to well curled
        this.bendScale = 0.7F + random.nextFloat() * 1.1F;
        // plus a random handful of little bones
        int extras = 1 + random.nextInt(5);
        for (int g = 0; g < groups + extras; g++) {
            MeatCut.Bones style = g >= groups ? MeatCut.Bones.SMALL : this.cut.bones;
            // now and then a bone of another kind turns up
            if (g < groups && random.nextFloat() < 0.25F) style = MeatCut.Bones.values()[random.nextInt(MeatCut.Bones.values().length)];
            for (int attempt = 0; attempt < 80; attempt++) {
                List<float[]> group = this.boneGroup(style, spineY);
                boolean ok = true;
                for (float[] capsule : group) {
                    if (!this.clear(capsule, dist, margin)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    placed.addAll(group);
                    break;
                }
            }
        }
        for (int y = 0; y < this.gh; y++) {
            for (int x = 0; x < this.gw; x++) {
                int i = y * this.gw + x;
                if (this.cells[i] == GONE) continue;
                for (float[] c : placed) {
                    if (segmentDistance(x + 0.5F, y + 0.5F, c) <= c[4]) {
                        this.bone[i] = true;
                        break;
                    }
                }
            }
        }
    }

    // A bone group in the cut's style, made of curved bones (see curve): where it goes, how long, how bent and how thick
    // are all random
    private List<float[]> boneGroup(MeatCut.Bones style, float spineY) {
        RandomSource r = this.random;
        float cx = 4 + r.nextFloat() * (this.gw - 8);
        float cy = 4 + r.nextFloat() * (this.gh - 8);
        List<float[]> group = new ArrayList<>();
        switch (style) {
            case BLADE -> {
                // a shoulder blade: a knob with a fan of flat bones curling out of it
                float base = r.nextFloat() * Mth.TWO_PI;
                group.add(new float[]{cx, cy, cx, cy, 2.0F + r.nextFloat() * 0.8F});
                int fans = 2 + r.nextInt(3);
                for (int k = 0; k < fans; k++) {
                    float angle = base + (k - (fans - 1) / 2.0F) * (0.3F + r.nextFloat() * 0.3F);
                    float len = 5 + r.nextFloat() * 8;
                    curve(group, cx, cy, cx + Mth.cos(angle) * len, cy + Mth.sin(angle) * len, (r.nextFloat() - 0.5F) * len * 0.6F,
                        1.4F + r.nextFloat() * 0.6F, 0.8F + r.nextFloat() * 0.5F);
                }
            }
            case KNUCKLE -> {
                // a leg bone: a bowed shaft with a knob at each end
                float angle = r.nextFloat() * Mth.TWO_PI;
                float len = 8 + r.nextFloat() * 10;
                float thick = 1.6F + r.nextFloat() * 0.9F;
                float bx = cx + Mth.cos(angle) * len;
                float by = cy + Mth.sin(angle) * len;
                curve(group, cx, cy, bx, by, (r.nextFloat() - 0.5F) * len * 0.4F, thick, thick * (0.8F + r.nextFloat() * 0.3F));
                group.add(new float[]{cx, cy, cx, cy, thick + 1.0F + r.nextFloat() * 0.6F});
                group.add(new float[]{bx, by, bx, by, thick + 0.8F + r.nextFloat() * 0.6F});
            }
            case RIBS -> {
                // a rib arcing from top to bottom, tilted and bent its own way
                float angle = Mth.HALF_PI + (r.nextFloat() - 0.5F) * 0.7F;
                float len = this.gh * (0.4F + r.nextFloat() * 0.35F);
                float y0 = 2 + r.nextFloat() * Math.max(1, this.gh - 4 - len);
                float dx = Mth.cos(angle) * len;
                float bend = (r.nextBoolean() ? 1 : -1) * len * (0.08F + r.nextFloat() * 0.22F);
                curve(group, cx - dx / 2, y0, cx + dx / 2, y0 + Mth.sin(angle) * len, bend, 1.2F + r.nextFloat() * 0.7F, 1.0F + r.nextFloat() * 0.5F);
            }
            case SPINE -> {
                // a vertebra near the spine's line, with one or two curled spurs
                float y = spineY + (r.nextFloat() - 0.5F) * 4;
                float thick = 2.0F + r.nextFloat() * 1.0F;
                group.add(new float[]{cx, y, cx, y, thick});
                int spurs = 1 + r.nextInt(2);
                for (int k = 0; k < spurs; k++) {
                    float angle = -Mth.HALF_PI + (r.nextFloat() - 0.5F) * 1.6F + (k == 1 ? Mth.PI : 0);
                    float len = thick + 2 + r.nextFloat() * 3;
                    curve(group, cx, y, cx + Mth.cos(angle) * len, y + Mth.sin(angle) * len, (r.nextFloat() - 0.5F) * len, 1.0F, 0.6F);
                }
            }
            case SMALL -> {
                float angle = r.nextFloat() * Mth.TWO_PI;
                float len = 2 + r.nextFloat() * 7;
                curve(group, cx, cy, cx + Mth.cos(angle) * len, cy + Mth.sin(angle) * len, (r.nextFloat() - 0.5F) * len * 0.8F,
                    0.8F + r.nextFloat() * 0.9F, 0.6F + r.nextFloat() * 0.6F);
            }
        }
        return group;
    }

    // A curved bone from a to b, bowed `bend` cells to one side (a quadratic curve), as a chain of short capsules
    // thinning from r0 to r1
    private void curve(List<float[]> out, float ax, float ay, float bx, float by, float bend, float r0, float r1) {
        bend *= this.bendScale;
        float dx = bx - ax;
        float dy = by - ay;
        float len = Math.max(0.01F, Mth.sqrt(dx * dx + dy * dy));
        float kx = (ax + bx) / 2 - dy / len * bend;
        float ky = (ay + by) / 2 + dx / len * bend;
        int segments = Math.max(2, (int) (len / 2.0F));
        float px = ax;
        float py = ay;
        for (int s = 1; s <= segments; s++) {
            float t = s / (float) segments;
            float qx = (1 - t) * (1 - t) * ax + 2 * (1 - t) * t * kx + t * t * bx;
            float qy = (1 - t) * (1 - t) * ay + 2 * (1 - t) * t * ky + t * t * by;
            out.add(new float[]{px, py, qx, qy, r0 + (r1 - r0) * t});
            px = qx;
            py = qy;
        }
    }

    // A bone can go here: inside the slab, in from the edge, and clear of both lanes
    private boolean clear(float[] c, int[] dist, float margin) {
        float len = Mth.sqrt((c[2] - c[0]) * (c[2] - c[0]) + (c[3] - c[1]) * (c[3] - c[1]));
        int steps = (int) Math.ceil(len) + 1;
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            float px = c[0] + (c[2] - c[0]) * t;
            float py = c[1] + (c[3] - c[1]) * t;
            int ix = (int) px;
            int iy = (int) py;
            if (ix < 0 || iy < 0 || ix >= this.gw || iy >= this.gh) return false;
            if (dist[iy * this.gw + ix] < c[4] + 2) return false;
            for (int lane = 0; lane < 2; lane++) {
                for (float dy = -c[4]; dy <= c[4]; dy += 0.5F) {
                    if (Math.abs(px - this.laneX(lane, py + dy)) < c[4] + margin) return false;
                }
            }
        }
        return true;
    }

    private static float segmentDistance(float px, float py, float[] c) {
        float dx = c[2] - c[0];
        float dy = c[3] - c[1];
        float lengthSq = dx * dx + dy * dy;
        float t = lengthSq == 0 ? 0 : Mth.clamp(((px - c[0]) * dx + (py - c[1]) * dy) / lengthSq, 0, 1);
        float ex = px - (c[0] + dx * t);
        float ey = py - (c[1] + dy * t);
        return Mth.sqrt(ex * ex + ey * ey);
    }

    // ── the result ──

    boolean isOver() {
        return this.step == Step.DONE && Util.getMillis() - this.endedAt > END_PAUSE;
    }

    boolean succeeded() {
        return this.success;
    }

    boolean playing() {
        return this.step != Step.DONE;
    }

    // Esc: give up, the slab is botched (plain meat)
    void giveUp() {
        if (this.step != Step.DONE) this.finish(false, "You gave up.");
        this.endedAt = Long.MIN_VALUE / 2;
    }

    private void finish(boolean ok, String why) {
        this.step = Step.DONE;
        this.success = ok;
        this.why = why;
        this.endedAt = Util.getMillis();
        this.held = false;
        this.stroking = false;
        if (ok) {
            play(SoundEvents.PLAYER_LEVELUP, 1.6F, 0.5F);
            play(SoundEvents.VILLAGER_WORK_BUTCHER, 1.0F, 1.0F);
        } else {
            play(SoundEvents.VILLAGER_NO, 0.8F, 0.8F);
        }
    }

    private void next(Step step) {
        this.step = step;
        this.stepAt = Util.getMillis();
        this.held = false;
        this.hasLast = false;
        play(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.2F, 0.7F);
    }

    private boolean intro(long now) {
        return this.step != Step.TRIM && this.step != Step.DONE && now - this.stepAt < INTRO;
    }

    private void say(String text) {
        this.message = text;
        this.messageAt = Util.getMillis();
    }

    // ── input ──

    // A mouse button (InputConstants.MOUSE_BUTTON_LEFT / RIGHT: in 26.3 these are SDL's numbers, left 1, right 3)
    void press(double mouseX, double mouseY, int button) {
        long now = Util.getMillis();
        if (this.step == Step.DONE || this.intro(now)) return;
        float gx = (float) ((mouseX - this.sx0) / CELL);
        float gy = (float) ((mouseY - this.sy0) / CELL);
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (this.step == Step.SCAN || this.step == Step.CUT) this.pin(gx, gy);
            return;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT || this.held) return;
        this.held = true;
        this.hasLast = false;
        if (this.step == Step.TRIM && this.trimStarted < 0) this.trimStarted = now;
        if (this.step == Step.CUT) {
            // a cut starts at the top edge of the meat (what's left after trimming), in that column
            int top = this.topOf((int) Math.floor(gx));
            if (top >= 0 && gy <= top + 1.5F && gy >= top - 10) {
                this.stroking = true;
                this.stroke.clear();
                Arrays.fill(this.strokeRows, Float.NaN);
                this.stroke.add(new float[]{gx, gy});
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.35F);
            } else {
                this.say("Start at the top edge of the meat");
            }
        }
    }

    // A drag with the left button down: counts as holding even if the press itself was missed
    void drag(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && !this.held) this.press(mouseX, mouseY, button);
    }

    void release(int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !this.held) return;
        this.held = false;
        this.hasLast = false;
        if (this.stroking) {
            this.stroking = false;
            this.stroke.clear();
            this.say("Cut all the way through in one go");
            play(SoundEvents.VILLAGER_NO, 1.3F, 0.4F);
        }
    }

    // Space or Enter: done scanning
    void key(int key) {
        if (this.step == Step.SCAN && !this.intro(Util.getMillis()) && (key == 32 || key == 257 || key == 335)) this.next(Step.CUT);
    }

    private void pin(float gx, float gy) {
        if (gx < 0 || gy < 0 || gx >= this.gw || gy >= this.gh) return;
        for (int i = 0; i < this.pins.size(); i++) {
            int[] p = this.pins.get(i);
            if (Math.abs(p[0] - gx) < 3 && Math.abs(p[1] - gy) < 3) {
                this.pins.remove(i);
                play(SoundEvents.UI_BUTTON_CLICK, 0.8F, 0.4F);
                return;
            }
        }
        if (this.pins.size() >= MAX_PINS) {
            this.say("No more pins (right-click one to pick it up)");
            return;
        }
        this.pins.add(new int[]{(int) gx, (int) gy});
        play(SoundEvents.UI_BUTTON_CLICK, 1.6F, 0.4F);
    }

    // ── a frame: the knife or scanner follows the mouse, then everything is drawn ──

    void draw(GuiGraphicsExtractor graphics, Font font, int sw, int sh, int mouseX, int mouseY) {
        long now = Util.getMillis();
        long dt = Math.min(100, now - this.lastFrame);
        this.lastFrame = now;
        int pw = this.gw * CELL;
        int ph = this.gh * CELL;
        int bw = Math.max(pw + 64, 250);
        // the board: the steps and clock along the top, the slab, then a strip for the words along the bottom
        int bh = ph + 38 + TEXT_STRIP;
        int cx = sw / 2;
        int bx0 = cx - bw / 2;
        int by0 = Math.max(34, (sh - bh) / 2 - 6);
        int shake = now - this.shakeAt < 300 ? Math.round((this.random.nextFloat() - 0.5F) * 5) : 0;
        this.sx0 = cx - pw / 2 + shake;
        this.sy0 = by0 + 38;
        float gx = (mouseX - this.sx0) / (float) CELL;
        float gy = (mouseY - this.sy0) / (float) CELL;

        this.update(now, dt, gx, gy);

        graphics.fill(0, 0, sw, sh, OVERLAY);
        this.drawBoard(graphics, bx0, by0, bw, bh);
        this.drawSlab(graphics, now);
        this.drawBones(graphics, now, gx, gy);
        this.drawCuts(graphics, now, pw, ph);
        this.drawPins(graphics);
        this.drawChunks(graphics, now, dt);
        this.drawHud(graphics, font, now, cx, bx0, by0, bw, bh);
        this.drawCursor(graphics, now, mouseX, mouseY);
    }

    private void update(long now, long dt, float gx, float gy) {
        switch (this.step) {
            case TRIM -> {
                if (this.held) this.slice(gx, gy, now);
            }
            case SCAN -> {
                if (this.intro(now)) return;
                if (this.held) {
                    this.scanUsed += dt;
                    this.scan(gx, gy, now);
                    if (this.scanUsed >= this.scanTime) {
                        this.scanUsed = this.scanTime;
                        this.next(Step.CUT);
                        this.say("The scanner's battery ran out");
                    }
                }
            }
            case CUT -> {
                if (this.intro(now)) return;
                if (this.stroking && this.held) this.carve(gx, gy, now);
                if (this.step == Step.CUT && now - this.stepAt - INTRO > CUT_TIME) this.finish(false, "Too slow: the meat's gone warm.");
            }
            default -> { }
        }
        this.lastGx = gx;
        this.lastGy = gy;
        this.hasLast = this.held;
    }

    // Step 1: the knife takes everything it passes over
    private void slice(float gx, float gy, long now) {
        float fromX = this.hasLast ? this.lastGx : gx;
        float fromY = this.hasLast ? this.lastGy : gy;
        float len = Mth.sqrt((gx - fromX) * (gx - fromX) + (gy - fromY) * (gy - fromY));
        int steps = Math.max(1, (int) Math.ceil(len * 2));
        int fat = 0;
        int meat = 0;
        for (int s = 0; s <= steps; s++) {
            float px = fromX + (gx - fromX) * s / steps;
            float py = fromY + (gy - fromY) * s / steps;
            for (int y = (int) Math.floor(py - KNIFE); y <= (int) Math.ceil(py + KNIFE); y++) {
                for (int x = (int) Math.floor(px - KNIFE); x <= (int) Math.ceil(px + KNIFE); x++) {
                    if (!this.solid(x, y)) continue;
                    float dx = x + 0.5F - px;
                    float dy = y + 0.5F - py;
                    if (dx * dx + dy * dy > KNIFE * KNIFE) continue;
                    int i = y * this.gw + x;
                    boolean wasFat = this.cells[i] == FAT;
                    this.chunk(x, y, this.cellColor(i));
                    this.cells[i] = GONE;
                    this.bone[i] = false;
                    if (wasFat) {
                        fat++;
                        this.fatLeft--;
                    } else {
                        meat++;
                        this.meatLost++;
                    }
                }
            }
        }
        if (meat > 0) {
            this.flashAt = now;
            if (now - this.lastSound > 90) {
                this.lastSound = now;
                play(SoundEvents.SLIME_SQUISH_SMALL, 1.2F, 0.6F);
            }
        } else if (fat > 0 && now - this.lastSound > 70) {
            this.lastSound = now;
            play(SoundEvents.SHEEP_SHEAR, 1.3F + this.random.nextFloat() * 0.3F, 0.35F);
        }
        if (this.meatLost > this.meatTotal * this.meatLimit) {
            this.finish(false, "You cut off too much good meat.");
        } else if (this.fatLeft <= 0) {
            this.next(Step.SCAN);
        }
    }

    // Step 2: bones under the lens light up; the nearer one is, the faster it ticks
    private void scan(float gx, float gy, long now) {
        float nearest = Float.MAX_VALUE;
        for (int y = (int) Math.floor(gy - LENS); y <= (int) Math.ceil(gy + LENS); y++) {
            for (int x = (int) Math.floor(gx - LENS); x <= (int) Math.ceil(gx + LENS); x++) {
                if (!this.solid(x, y)) continue;
                int i = y * this.gw + x;
                if (!this.bone[i]) continue;
                float d = Mth.sqrt((x + 0.5F - gx) * (x + 0.5F - gx) + (y + 0.5F - gy) * (y + 0.5F - gy));
                if (d > LENS) continue;
                this.seenAt[i] = now;
                nearest = Math.min(nearest, d);
            }
        }
        if (nearest <= LENS && now - this.lastTick > 50 + nearest * 24) {
            this.lastTick = now;
            play(SoundEvents.NOTE_BLOCK_HAT, 1.0F + (1 - nearest / LENS), 0.45F);
        }
    }

    // Step 3: the cut follows the mouse down; a bone stops it dead
    private void carve(float gx, float gy, long now) {
        float[] from = this.stroke.getLast();
        float len = Mth.sqrt((gx - from[0]) * (gx - from[0]) + (gy - from[1]) * (gy - from[1]));
        int steps = Math.max(1, (int) Math.ceil(len * 2));
        for (int s = 1; s <= steps; s++) {
            float px = from[0] + (gx - from[0]) * s / steps;
            float py = from[1] + (gy - from[1]) * s / steps;
            int ix = (int) Math.floor(px);
            int iy = (int) Math.floor(py);
            if (this.solid(ix, iy) && this.bone[iy * this.gw + ix]) {
                this.hitX = ix;
                this.hitY = iy;
                this.shakeAt = now;
                this.stroke.add(new float[]{px, py});
                play(SoundEvents.SKELETON_HURT, 0.5F, 1.0F);
                play(SoundEvents.ANVIL_LAND, 1.8F, 0.4F);
                this.finish(false, "Clunk! You hit a bone.");
                return;
            }
            if (iy >= 0 && iy < this.gh) {
                if (!this.cuts.isEmpty()) {
                    float other = this.cuts.getFirst()[iy];
                    if (!Float.isNaN(other) && Math.abs(px - other) < MIN_GAP) {
                        this.stroking = false;
                        this.stroke.clear();
                        this.say("Too close to your first cut");
                        play(SoundEvents.VILLAGER_NO, 1.3F, 0.4F);
                        return;
                    }
                }
                this.strokeRows[iy] = px;
            }
            // through once it's past the bottom edge of the meat in this column
            int bottom = this.bottomOf((int) Math.floor(px));
            if ((bottom >= 0 && py > bottom + 1.0F) || py > this.gh + 1.0F) {
                this.stroke.add(new float[]{px, py});
                this.endCut();
                return;
            }
        }
        this.stroke.add(new float[]{gx, gy});
        if (this.solid((int) Math.floor(gx), (int) Math.floor(gy)) && Util.getMillis() - this.lastSound > 110) {
            this.lastSound = Util.getMillis();
            play(SoundEvents.SLIME_SQUISH_SMALL, 1.6F, 0.25F);
        }
    }

    // A cut reached the bottom: it counts if it went through the meat
    private void endCut() {
        this.stroking = false;
        int through = 0;
        for (int y = 0; y < this.gh; y++) {
            float x = this.strokeRows[y];
            if (Float.isNaN(x)) {
                // a fast flick can skip a row: take the row above
                if (y > 0) this.strokeRows[y] = this.strokeRows[y - 1];
                continue;
            }
            if (this.solid((int) Math.floor(x), y)) through++;
        }
        this.stroke.clear();
        if (through < this.gh * 0.4F) {
            this.say("That cut missed the meat");
            play(SoundEvents.VILLAGER_NO, 1.3F, 0.4F);
            return;
        }
        this.cuts.add(this.strokeRows.clone());
        play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.8F);
        if (this.cuts.size() >= 2) this.finish(true, "Clean cuts, no bones.");
        else this.say("One more cut!");
    }

    private void chunk(int x, int y, int color) {
        if (this.chunks.size() > 160 || this.random.nextInt(3) != 0) return;
        float vx = (this.random.nextFloat() - 0.5F) * 120;
        float vy = -40 - this.random.nextFloat() * 90;
        this.chunks.add(new float[]{this.sx0 + x * CELL, this.sy0 + y * CELL, vx, vy, Util.getMillis(), color});
    }

    // ── drawing ──

    // The chopping block: end-grain squares of wood, worn with knife scratches and old stains
    private void drawBoard(GuiGraphicsExtractor graphics, int x0, int y0, int w, int h) {
        graphics.fill(x0 - 4, y0 - 4, x0 + w + 4, y0 + h + 4, 0xFF24160C);
        graphics.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, 0xFF5A3A20);
        int tile = 18;
        for (int ty = 0; ty * tile < h; ty++) {
            for (int tx = 0; tx * tile < w; tx++) {
                int ax = x0 + tx * tile;
                int ay = y0 + ty * tile;
                int bx = Math.min(x0 + w, ax + tile);
                int by = Math.min(y0 + h, ay + tile);
                boolean light = (tx + ty) % 2 == 0;
                graphics.fill(ax, ay, bx, by, light ? 0xFFA67646 : 0xFF98683C);
                // growth rings
                int mx = (ax + bx) / 2;
                int my = (ay + by) / 2;
                int ring = light ? 0xFF946234 : 0xFF8A5A30;
                for (int r = 3; r < tile / 2; r += 3) {
                    if (mx - r >= ax && mx + r < bx && my - r >= ay && my + r < by) {
                        graphics.fill(mx - r, my - r, mx + r, my - r + 1, ring);
                        graphics.fill(mx - r, my + r - 1, mx + r, my + r, ring);
                        graphics.fill(mx - r, my - r, mx - r + 1, my + r, ring);
                        graphics.fill(mx + r - 1, my - r, mx + r, my + r, ring);
                    }
                }
                graphics.fill(ax, ay, bx, ay + 1, 0x30000000);
                graphics.fill(ax, ay, ax + 1, by, 0x30000000);
            }
        }
        for (float[] s : this.stains) {
            int sx = x0 + 6 + Math.round(s[0] * (w - 12));
            int sy = y0 + 6 + Math.round(s[1] * (h - 12));
            int r = (int) s[2];
            for (int dy = -r; dy <= r; dy++) {
                int half = (int) Math.sqrt(r * r - dy * dy);
                graphics.fill(sx - half, sy + dy, sx + half + 1, sy + dy + 1, 0x38601010);
            }
        }
        for (float[] s : this.scratches) {
            int sx = x0 + 4 + Math.round(s[0] * (w - 30));
            int sy = y0 + 4 + Math.round(s[1] * (h - 30));
            int color = s[3] > 0 ? 0x50FFE8C8 : 0x50301808;
            for (int d = 0; d < (int) s[2]; d++) graphics.fill(sx + d, sy + d / 2, sx + d + 1, sy + d / 2 + 1, color);
        }
    }

    private int cellColor(int i) {
        int s = this.shade[i];
        if (this.cells[i] == FAT) return shadeColor(this.fatColor, s == 0 ? 0.93F : s == 3 ? 1.03F : 0.98F);
        // marbling streaks: lighter meat, kept well off the fat's color so they don't look like something to trim
        if (s == 4) return mix(this.meatColor, 0xFFB0A0, 0.3F);
        return shadeColor(this.meatColor, s == 0 ? 0.82F : s == 1 ? 0.92F : s == 2 ? 1.0F : 1.1F);
    }

    // The slab, cell by cell (runs of one color drawn at once), lit from above, with its shadow; after clean cuts the
    // pieces ease apart
    private void drawSlab(GuiGraphicsExtractor graphics, long now) {
        float apart = this.step == Step.DONE && this.success ? Mth.clamp((now - this.endedAt) / 500.0F, 0, 1) : 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < this.gh; y++) {
                int runStart = -1;
                int runColor = 0;
                int runShift = 0;
                for (int x = 0; x <= this.gw; x++) {
                    int color = 0;
                    int shift = 0;
                    if (x < this.gw && this.solid(x, y)) {
                        int i = y * this.gw + x;
                        if (pass == 0) {
                            color = 0x60000000;
                        } else {
                            color = this.cellColor(i);
                            if (!this.solid(x, y - 1)) color = shadeColor(color, 1.12F);
                            else if (!this.solid(x, y + 1)) color = shadeColor(color, 0.72F);
                            else if (!this.solid(x - 1, y) || !this.solid(x + 1, y)) color = shadeColor(color, 0.88F);
                            if (now - this.flashAt < 160 && this.cells[i] == MEAT) color = mix(color, 0xFF2020, 0.25F);
                            // the last bits of fat glow, so the slivers can be found
                            if (this.step == Step.TRIM && this.cells[i] == FAT && this.fatLeft <= this.fatTotal * FAT_GLOW) {
                                color = mix(color, 0xFFD040, 0.35F + 0.3F * Mth.sin(now / 150.0F));
                            }
                        }
                        if (apart > 0) {
                            for (float[] cut : this.cuts) {
                                if (!Float.isNaN(cut[y]) && x + 0.5F > cut[y]) shift++;
                            }
                            shift = Math.round(shift * 4 * apart);
                        }
                    }
                    if (runStart >= 0 && (color != runColor || shift != runShift)) {
                        int off = pass == 0 ? 2 : 0;
                        graphics.fill(this.sx0 + runStart * CELL + runShift + off, this.sy0 + y * CELL + off,
                            this.sx0 + x * CELL + runShift + off, this.sy0 + (y + 1) * CELL + off, pass == 0 ? runColor : 0xFF000000 | runColor);
                        runStart = -1;
                    }
                    if (color != 0 && runStart < 0) {
                        runStart = x;
                        runColor = color;
                        runShift = shift;
                    }
                }
            }
        }
    }

    // Bones: glowing under the scanner and fading after; all of them shown at the end
    private void drawBones(GuiGraphicsExtractor graphics, long now, float gx, float gy) {
        boolean reveal = this.step == Step.DONE;
        for (int y = 0; y < this.gh; y++) {
            for (int x = 0; x < this.gw; x++) {
                int i = y * this.gw + x;
                if (!this.bone[i] || this.cells[i] == GONE) continue;
                int px = this.sx0 + x * CELL;
                int py = this.sy0 + y * CELL;
                if (reveal) {
                    float shown = Mth.clamp((now - this.endedAt) / 400.0F, 0, 1);
                    boolean edge = !this.isBone(x - 1, y) || !this.isBone(x + 1, y) || !this.isBone(x, y - 1) || !this.isBone(x, y + 1);
                    int alpha = Math.round(shown * 255);
                    graphics.fill(px, py, px + CELL, py + CELL, (alpha << 24) | ((edge ? BONE_EDGE : BONE) & 0xFFFFFF));
                    continue;
                }
                float fade = 1 - (now - this.seenAt[i]) / (float) AFTERGLOW;
                if (fade <= 0) continue;
                int alpha = Math.round(Mth.clamp(fade, 0, 1) * 230);
                graphics.fill(px, py, px + CELL, py + CELL, (alpha << 24) | 0xD8F6FF);
            }
        }
        if (reveal && !this.success && this.hitX >= 0) {
            int px = this.sx0 + this.hitX * CELL;
            int py = this.sy0 + this.hitY * CELL;
            int pulse = (int) (Math.sin(now / 90.0) * 2);
            graphics.fill(px - 4 - pulse, py - 4 - pulse, px + 6 + pulse, py + 6 + pulse, 0x90FF2020);
        }
    }

    private boolean isBone(int x, int y) {
        return this.solid(x, y) && this.bone[y * this.gw + x];
    }

    // The cuts made (dark grooves), the one being made, and in step 3 gold ticks along the meat's top and bottom edges
    private void drawCuts(GuiGraphicsExtractor graphics, long now, int pw, int ph) {
        float apart = this.step == Step.DONE && this.success ? Mth.clamp((now - this.endedAt) / 500.0F, 0, 1) : 0;
        if (this.step == Step.CUT) {
            int alpha = (int) (150 + 90 * Math.sin(now / 260.0));
            for (int x = 0; x < this.gw; x += 2) {
                int top = this.topOf(x);
                if (top < 0) continue;
                int px = this.sx0 + x * CELL;
                graphics.fill(px, this.sy0 + top * CELL - 2, px + CELL, this.sy0 + top * CELL - 1, (alpha << 24) | 0xFFD86A);
                int bottom = this.bottomOf(x);
                graphics.fill(px, this.sy0 + (bottom + 1) * CELL + 1, px + CELL, this.sy0 + (bottom + 1) * CELL + 2, (alpha << 24) | 0xFFD86A);
            }
        }
        if (apart == 0) {
            for (float[] cut : this.cuts) {
                for (int y = 0; y < this.gh; y++) {
                    if (Float.isNaN(cut[y]) || !this.solid((int) Math.floor(cut[y]), y)) continue;
                    int x = this.sx0 + Math.round(cut[y] * CELL);
                    graphics.fill(x - 1, this.sy0 + y * CELL, x + 1, this.sy0 + (y + 1) * CELL, 0xFF3A0806);
                }
            }
        }
        for (int i = 1; i < this.stroke.size(); i++) {
            float[] a = this.stroke.get(i - 1);
            float[] b = this.stroke.get(i);
            int steps = Math.max(1, (int) (Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1])) * 2);
            for (int s = 0; s <= steps; s++) {
                float px = a[0] + (b[0] - a[0]) * s / steps;
                float py = a[1] + (b[1] - a[1]) * s / steps;
                int x = this.sx0 + Math.round(px * CELL);
                int y = this.sy0 + Math.round(py * CELL);
                boolean inMeat = this.solid((int) Math.floor(px), (int) Math.floor(py));
                graphics.fill(x - 1, y, x + 1, y + 1, inMeat ? 0xFF4A0A08 : 0x80E0E8F0);
            }
        }
    }

    private void drawPins(GuiGraphicsExtractor graphics) {
        for (int[] p : this.pins) {
            int x = this.sx0 + p[0] * CELL + 1;
            int y = this.sy0 + p[1] * CELL + 1;
            graphics.fill(x, y - 3, x + 1, y + 1, 0xFFB8BEC6);
            graphics.fill(x - 2, y - 7, x + 3, y - 3, 0xFF7A1010);
            graphics.fill(x - 1, y - 6, x + 2, y - 4, 0xFFE83A3A);
            graphics.fill(x - 1, y - 6, x, y - 5, 0xFFFFB0B0);
        }
    }

    // Trimmings flying off the board
    private void drawChunks(GuiGraphicsExtractor graphics, long now, long dt) {
        float seconds = dt / 1000.0F;
        for (int i = this.chunks.size() - 1; i >= 0; i--) {
            float[] c = this.chunks.get(i);
            float age = now - c[4];
            if (age > 700) {
                this.chunks.remove(i);
                continue;
            }
            c[3] += 420 * seconds;
            c[0] += c[2] * seconds;
            c[1] += c[3] * seconds;
            int alpha = Math.round(255 * (1 - age / 700.0F));
            graphics.fill(Math.round(c[0]), Math.round(c[1]), Math.round(c[0]) + 2, Math.round(c[1]) + 2, (alpha << 24) | ((int) c[5] & 0xFFFFFF));
        }
    }

    // A cleaver for the knife steps, the scanner's lens for the scan
    private void drawCursor(GuiGraphicsExtractor graphics, long now, int mx, int my) {
        if (this.step == Step.DONE) return;
        if (this.step == Step.SCAN) {
            int r = Math.round(LENS * CELL);
            boolean on = this.held && !this.intro(now) && this.scanUsed < this.scanTime;
            for (int dy = -r; dy <= r; dy++) {
                int half = (int) Math.sqrt(r * r - dy * dy);
                if (on) graphics.fill(mx - half, my + dy, mx + half + 1, my + dy + 1, 0x2858C8FF);
                graphics.fill(mx - half, my + dy, mx - half + 1, my + dy + 1, on ? SCAN : 0x907AE0FF);
                graphics.fill(mx + half, my + dy, mx + half + 1, my + dy + 1, on ? SCAN : 0x907AE0FF);
            }
            graphics.fill(mx - r / 2, my - r, mx + r / 2, my - r + 1, on ? SCAN : 0x907AE0FF);
            graphics.fill(mx - r / 2, my + r, mx + r / 2, my + r + 1, on ? SCAN : 0x907AE0FF);
            if (on) {
                int sweep = (int) Math.round(Math.sin(now / 140.0) * (r - 2));
                int half = (int) Math.sqrt(Math.max(0, r * r - sweep * sweep));
                graphics.fill(mx - half + 1, my + sweep, mx + half, my + sweep + 1, 0x90B0F0FF);
            }
            graphics.fill(mx, my, mx + 1, my + 1, 0xFFFFFFFF);
            return;
        }
        // trimming: a ring showing just what the knife takes
        if (this.step == Step.TRIM) {
            int r = Math.round(KNIFE * CELL);
            int ring = this.held ? 0xE0FFF4D0 : 0x90FFF4D0;
            for (int dy = -r; dy <= r; dy++) {
                int half = (int) Math.round(Math.sqrt(r * r - dy * dy));
                graphics.fill(mx - half, my + dy, mx - half + 1, my + dy + 1, ring);
                graphics.fill(mx + half, my + dy, mx + half + 1, my + dy + 1, ring);
            }
            graphics.fill(mx - r / 2, my - r, mx + r / 2 + 1, my - r + 1, ring);
            graphics.fill(mx - r / 2, my + r, mx + r / 2 + 1, my + r + 1, ring);
        }
        // the cleaver: its edge at the mouse
        graphics.fill(mx - 1, my - 10, mx + 7, my + 1, 0xFF5E646C);
        graphics.fill(mx, my - 9, mx + 7, my, 0xFFC8CED6);
        graphics.fill(mx, my - 1, mx + 7, my, 0xFFF4F8FF);
        graphics.fill(mx + 5, my - 8, mx + 6, my - 7, 0xFF5E646C);
        graphics.fill(mx + 7, my - 8, mx + 14, my - 5, 0xFF3A2414);
        graphics.fill(mx + 8, my - 7, mx + 13, my - 6, 0xFF7A4A28);
    }

    private void drawHud(GuiGraphicsExtractor graphics, Font font, long now, int cx, int bx0, int by0, int bw, int bh) {
        // what's being cut
        String title = "Butchering: " + this.slab.getHoverName().getString();
        int tw = font.width(title) + 20;
        graphics.item(this.slab, cx - tw / 2, by0 - 26);
        graphics.text(font, title, cx - tw / 2 + 20, by0 - 22, TITLE, true);

        // the three steps along the top of the board
        String[] names = {"1 Trim", "2 Scan", "3 Cut"};
        int pillW = 54;
        int px0 = cx - (pillW * 3 + 8) / 2;
        for (int s = 0; s < 3; s++) {
            int x = px0 + s * (pillW + 4);
            boolean current = this.step.ordinal() == s;
            boolean done = this.step.ordinal() > s && (this.step != Step.DONE || this.success || s < this.failedStep());
            int body = current ? 0xE0402810 : 0xA0201008;
            graphics.fill(x, by0 + 5, x + pillW, by0 + 17, body);
            graphics.fill(x, by0 + 5, x + pillW, by0 + 6, current ? GOLD : done ? 0xFF5A9A4A : 0xFF5A4030);
            graphics.centeredText(font, (done ? "✔ " : "") + names[s], x + pillW / 2, by0 + 7, current ? GOLD : done ? GREEN : DIM);
        }

        // the clock (or the scanner's battery)
        int barX0 = bx0 + 12;
        int barX1 = bx0 + bw - 12;
        int barY = by0 + 21;
        float left = switch (this.step) {
            case TRIM -> 1 - this.meatLost / (this.meatTotal * this.meatLimit);
            case SCAN -> 1 - this.scanUsed / (float) this.scanTime;
            case CUT -> this.intro(now) ? 1 : 1 - (now - this.stepAt - INTRO) / (float) CUT_TIME;
            default -> 0;
        };
        left = Mth.clamp(left, 0, 1);
        graphics.fill(barX0, barY, barX1, barY + 4, 0xC0140A04);
        int barColor = this.step == Step.SCAN ? SCAN : left < 0.25F ? RED : GOLD;
        if (this.step != Step.DONE) graphics.fill(barX0, barY, barX0 + Math.round((barX1 - barX0) * left), barY + 4, barColor);

        // the words: in the strip along the bottom of the board, under the slab
        int sy = by0 + bh - TEXT_STRIP + 4;
        graphics.fill(bx0 + 6, sy - 2, bx0 + bw - 6, by0 + bh - 5, 0xA0140A04);
        graphics.fill(bx0 + 6, sy - 2, bx0 + bw - 6, sy - 1, 0x40FFE8B0);
        if (this.step == Step.DONE) {
            this.big(graphics, font, this.success ? "Clean Cuts!" : "Botched!", cx, sy + 2, this.success ? GOLD : RED);
            graphics.centeredText(font, this.why, cx, sy + 18, TEXT);
            ItemStack result = this.cut.result(this.success).copyWithCount(this.count);
            graphics.centeredText(font, "You get " + result.getCount() + " " + result.getHoverName().getString(), cx, sy + 30, this.success ? GREEN : TEXT);
            return;
        }
        if (this.intro(now) || (this.step == Step.TRIM && this.trimStarted < 0)) {
            String banner = switch (this.step) {
                case TRIM -> "Step 1: Trim the Fat";
                case SCAN -> "Step 2: Scan for Bones";
                default -> "Step 3: Make Two Cuts";
            };
            this.big(graphics, font, banner, cx, sy + 4, GOLD);
            String sub = switch (this.step) {
                case TRIM -> "Click and drag over the pale fat to start";
                case SCAN -> "Hold to scan. Right-click to pin a bone.";
                default -> "From the meat's top edge down through it";
            };
            graphics.centeredText(font, sub, cx, sy + 22, (((int) (180 + 75 * Math.sin(now / 220.0))) << 24) | 0xF6E2B0);
            graphics.centeredText(font, "Esc gives up (plain meat)", cx, sy + 34, 0xFF6A5E52);
            return;
        }
        int y = sy + 2;
        String stats;
        String hint;
        switch (this.step) {
            case TRIM -> {
                int fat = Math.max(0, this.fatLeft) == 0 ? 0 : Math.max(1, Math.round(100F * this.fatLeft / this.fatTotal));
                int kept = 100 - Math.round(100F * this.meatLost / this.meatTotal);
                stats = "Fat left: " + fat + "%   Accuracy: " + kept + "% (min " + Math.round(100 - this.meatLimit * 100) + "%)";
                hint = "Get every bit of fat. Spare the meat!";
            }
            case SCAN -> {
                stats = String.format("Scanner: %.1fs    Pins: %d/%d", (this.scanTime - this.scanUsed) / 1000.0F, this.pins.size(), MAX_PINS);
                hint = "Hold to scan. Right-click: pin. Space: done";
            }
            case CUT -> {
                stats = "Cuts: " + this.cuts.size() + "/2    Pins: " + this.pins.size();
                hint = "Top edge of the meat down out the bottom. Miss the bones!";
            }
            default -> {
                stats = "";
                hint = "";
            }
        }
        if (!stats.isEmpty()) graphics.centeredText(font, stats, cx, y, TEXT);
        if (!hint.isEmpty()) graphics.centeredText(font, hint, cx, y + 12, DIM);
        boolean saying = now - this.messageAt < MESSAGE;
        graphics.centeredText(font, saying ? this.message : "Esc gives up (plain meat)", cx, y + 26, saying ? 0xFFFFB040 : 0xFF6A5E52);
    }

    // the step it went wrong in (for the ticks on the step pills)
    private int failedStep() {
        if (!this.cuts.isEmpty() || this.hitX >= 0 || this.why.startsWith("Too slow")) return 2;
        if (this.scanUsed > 0 || this.fatLeft <= 0) return 2;
        return 0;
    }

    private void big(GuiGraphicsExtractor graphics, Font font, String text, int cx, int y, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, y);
        graphics.pose().scale(1.6F, 1.6F);
        graphics.centeredText(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static int shadeColor(int color, float f) {
        return (Math.min(255, Math.round(((color >> 16) & 0xFF) * f)) << 16) | (Math.min(255, Math.round(((color >> 8) & 0xFF) * f)) << 8)
            | Math.min(255, Math.round((color & 0xFF) * f));
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void play(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float pitch, float volume) {
        play(sound.value(), pitch, volume);
    }

    private static void play(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}

package com.alexh.capitalism.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.function.IntUnaryOperator;

// Building blocks for the Premium boats after the Pirate Ship (SpeedboatModel, YachtModel, LongshipModel,
// PaddlewheelerModel, SwampRaftModel, SteamboatModel and the rest). Every one has its own texture, drawn at 4x
// (1024 x 1024) by tools/make_boats.ps1 + tools/BoatTex.cs, all with the SAME layout of zones below (keep them the
// same there); a zone can look different on each boat (DECK is teak on the yacht, spruce on the longship...).
// Model pixels as for every boat: +x the bow, +z port (left), y DOWN, y = 0 about the waterline.
final class BoatKit {

    record Zone(int u, int v, int w, int h) {
    }

    // the hull sides: one picture each, lined up across the slices (sideWall): port column c is x = PORT_K - c,
    // starboard column c is x = c - STAR_K; row r is y = r - SIDE_C (both). Good for x from -45 to 48, y -18 to 2.
    static final Zone PORT = new Zone(0, 0, 128, 20);
    static final Zone STAR = new Zone(128, 0, 128, 20);
    static final int PORT_K = 56;
    static final int STAR_K = 48;
    static final int SIDE_C = 18;
    static final Zone FIBER = new Zone(0, 20, 64, 32);      // the boat's main colour (white on the yacht...)
    static final Zone BOTTOM = new Zone(64, 20, 64, 32);    // under the water
    static final Zone LEATHER = new Zone(128, 20, 40, 24);
    static final Zone CHROME = new Zone(168, 20, 24, 24);
    static final Zone LINING = new Zone(192, 20, 40, 12);
    static final Zone BLACK = new Zone(232, 20, 24, 12);
    static final Zone RED = new Zone(192, 32, 32, 12);
    static final Zone GOLD = new Zone(224, 32, 32, 12);
    static final Zone TINT = new Zone(0, 52, 48, 32);       // tinted glass
    static final Zone GAUGE = new Zone(48, 52, 16, 8);      // glowing dials
    static final Zone NAV_RED = new Zone(64, 52, 8, 8);
    static final Zone NAV_GREEN = new Zone(72, 52, 8, 8);
    static final Zone NAV_WHITE = new Zone(80, 52, 8, 8);
    static final Zone BLUE_GLOW = new Zone(88, 52, 8, 8);
    static final Zone WARM = new Zone(96, 52, 32, 16);      // lit windows
    static final Zone WATER = new Zone(48, 60, 16, 8);      // glowing water (a hot tub)
    static final Zone FIRE = new Zone(64, 60, 16, 8);       // flames and embers (glow)
    static final Zone GHOST = new Zone(80, 60, 16, 8);      // spectral glow
    static final Zone ROPE = new Zone(48, 68, 16, 16);
    static final Zone NAVY = new Zone(64, 68, 32, 16);
    static final Zone ACCENT = new Zone(96, 68, 32, 16);    // a second colour (stripes, trim)
    static final Zone BRASS = new Zone(128, 44, 40, 40);
    static final Zone ENGINE = new Zone(168, 44, 40, 40);   // glossy black machinery
    static final Zone GREY = new Zone(208, 44, 48, 40);     // metal
    static final Zone CANVAS = new Zone(0, 84, 64, 32);     // cushions, tarps
    static final Zone WHITE = new Zone(64, 84, 64, 32);     // big plain boxes
    static final Zone WOOD = new Zone(128, 84, 64, 32);     // planks and beams
    static final Zone DARKWOOD = new Zone(192, 84, 64, 32);
    // decking: column c is x = c - DECK_K, row r is z = DECK_Z - r (deck())
    static final Zone DECK = new Zone(0, 116, 128, 32);
    static final int DECK_K = 56;
    static final int DECK_Z = 11;
    static final Zone ICE = new Zone(128, 116, 64, 32);
    static final Zone STEEL = new Zone(192, 116, 64, 32);
    static final Zone MOSS = new Zone(0, 148, 64, 32);
    static final Zone VINE = new Zone(64, 148, 32, 32);
    static final Zone BAMBOO = new Zone(96, 148, 32, 32);
    static final Zone OLDWOOD = new Zone(128, 148, 64, 32);
    static final Zone BONE = new Zone(192, 148, 32, 32);
    static final Zone SCALE = new Zone(224, 148, 32, 32);   // carved dragon scales
    // a lamp's beam: clear in the texture, soft light in the glow (the Submersible's flashlight)
    static final Zone BEAM = new Zone(0, 180, 64, 40);
    // pictures (sails, emblems...) for each boat: from y 180 down
    static final int PICTURE_V = 180;

    final PartDefinition root;
    final CubeListBuilder statics = CubeListBuilder.create();
    private int count;

    BoatKit(PartDefinition root) {
        this.root = root;
        resetNudges();
    }

    void box(Zone zone, int du, int dv, float x, float y, float z, float w, float h, float d) {
        box(this.statics, zone, du, dv, x, y, z, w, h, d);
    }

    void box(Zone zone, float x, float y, float z, float w, float h, float d) {
        box(this.statics, zone, 0, 0, x, y, z, w, h, d);
    }

    // a box textured from a zone, offset (du, dv) into it (kept inside it), grown by its own hair (nudge)
    static void box(CubeListBuilder builder, Zone zone, int du, int dv, float x, float y, float z, float w, float h, float d) {
        int fw = (int) Math.ceil(2 * (w + d));
        int fh = (int) Math.ceil(d + h);
        du = Math.max(0, Math.min(du, zone.w - fw));
        dv = Math.max(0, Math.min(dv, zone.h - fh));
        builder.texOffs(zone.u + du, zone.v + dv).addBox(x, y, z, w, h, d, nudge(x, y, z, w, h, d, false));
    }

    // Boxes whose faces lie in exactly the same place flicker against each other (their textures fight). So every
    // box grows by a hair, one of 11 sizes up to 0.13 px (never more than 0.3 of its thinnest side), chosen so it
    // differs from every box already made that shares a face plane with it (facing the same way, overlapping): like
    // colouring a map so no neighbours match. A bar (line(): along its own y) doesn't grow along its length; flat
    // boxes (sails, flags, shields) don't grow. Each model starts afresh (resetNudges), so it always comes out the same.
    private static final java.util.List<float[]> PLACED = new java.util.ArrayList<>();
    private static int nudges;

    static void resetNudges() {
        PLACED.clear();
        nudges = 0;
    }

    static net.minecraft.client.model.geom.builders.CubeDeformation nudge(float x, float y, float z, float w, float h, float d, boolean bar) {
        float thinnest = Math.min(w, Math.min(h, d));
        if (thinnest <= 0.0F) return net.minecraft.client.model.geom.builders.CubeDeformation.NONE;
        float cap = Math.min(0.13F, 0.3F * thinnest);
        float[] box = {x, y, z, x + w, y + h, z + d, 0};
        float best = 0;
        int fewest = Integer.MAX_VALUE;
        int start = nudges++;
        for (int i = 0; i < 11; i++) {
            float g = cap * ((start + i * 4) % 11 + 1) / 11.0F;
            int clashes = clashes(box, g);
            if (clashes < fewest) {
                fewest = clashes;
                best = g;
                if (clashes == 0) break;
            }
        }
        box[6] = best;
        PLACED.add(box);
        return new net.minecraft.client.model.geom.builders.CubeDeformation(best, bar ? 0.0F : best, best);
    }

    // boxes already made with this growth that have a face in the same plane as one of this box's, facing the same
    // way, overlapping it (after growing)
    private static int clashes(float[] b, float g) {
        int n = 0;
        for (float[] o : PLACED) {
            if (Math.abs(o[6] - g) > 1e-5F) continue;
            for (int axis = 0; axis < 3; axis++) {
                boolean samePlane = Math.abs(o[axis] - b[axis]) < 1e-4F || Math.abs(o[axis + 3] - b[axis + 3]) < 1e-4F;
                if (!samePlane) continue;
                int a1 = (axis + 1) % 3;
                int a2 = (axis + 2) % 3;
                float margin = 2 * g + 0.01F;
                if (o[a1] < b[a1 + 3] + margin && b[a1] < o[a1 + 3] + margin && o[a2] < b[a2 + 3] + margin && b[a2] < o[a2 + 3] + margin) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    // a box mirrored to both sides (z and -z - d)
    void pair(Zone zone, float x, float y, float z, float w, float h, float d) {
        this.box(zone, x, y, z, w, h, d);
        this.box(zone, x, y, -z - d, w, h, d);
    }

    PartDefinition part(CubeListBuilder cubes, PartPose pose) {
        return this.root.addOrReplaceChild("k" + (this.count++), cubes, pose);
    }

    PartDefinition part(String name, CubeListBuilder cubes, PartPose pose) {
        return this.root.addOrReplaceChild(name, cubes, pose);
    }

    PartDefinition group(String name) {
        return this.root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
    }

    // a straight bar from a to b, this thick (into the root, or a parent)
    void line(Zone zone, float ax, float ay, float az, float bx, float by, float bz, float thick) {
        line(this.root, "k" + (this.count++), zone, ax, ay, az, bx, by, bz, thick);
    }

    static void line(PartDefinition parent, String name, Zone zone, float ax, float ay, float az, float bx, float by, float bz, float thick) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01F) return;
        dx /= len;
        dy /= len;
        dz /= len;
        float xRot = (float) Math.asin(-dz);
        float zRot = (float) Math.atan2(dx, -dy);
        CubeListBuilder cubes = CubeListBuilder.create();
        int fw = (int) Math.ceil(4 * thick);
        int fh = (int) Math.ceil(thick + len);
        cubes.texOffs(zone.u, zone.v).addBox(-thick / 2, -len, -thick / 2, thick, len, thick,
            nudge(ax, ay, az, thick, len, thick, true));
        parent.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(ax, ay, az, xRot, 0.0F, zRot));
    }

    // ── the hull ──

    // A hull from slice `stern` to slice `bow`: side walls from top(x) down to y = 1 (their texture making one picture
    // per side, PORT / STAR), then the bottom under the water in layers {top y, bottom y, share of the width} from the
    // BOTTOM zone, narrowing toward the bow by `rake` px per layer. Returns each slice's wall thickness (by x - stern).
    int[] hull(int stern, int bow, IntUnaryOperator halfWidth, IntUnaryOperator top, float[][] layers, int rake) {
        int[] thick = new int[bow - stern + 1];
        for (int x = stern; x <= bow; x++) {
            int w = halfWidth.applyAsInt(x);
            int prev = x > stern ? halfWidth.applyAsInt(x - 1) : w;
            int next = x < bow ? halfWidth.applyAsInt(x + 1) : 0;
            int t = Math.min(w, Math.min(3, Math.max(1, Math.max(Math.abs(w - prev), Math.abs(w - next)))));
            thick[x - stern] = t;
            int tp = top.applyAsInt(x);
            this.sideWall(x, tp, 1, w, t, true);
            this.sideWall(x, tp, 1, w, t, false);
            if ((x - stern) % 2 == 0) {
                for (int l = 0; l < layers.length; l++) {
                    int ahead = Math.min(bow, x + rake * (l + 1));
                    int lw = Math.round(halfWidth.applyAsInt(ahead) * layers[l][2]) - (x + rake * (l + 1) > bow ? 1 : 0);
                    if (lw <= 0) continue;
                    float y0 = layers[l][0];
                    float h = layers[l][1] - y0;
                    box(this.statics, BOTTOM, ((x - stern) * 3 + l * 9) % 30, 0, x, y0, -lw, 2, h, 2 * lw);
                }
            }
        }
        return thick;
    }

    // a hull side wall at slice x, from top down to bottom, `thick` thick, its outside at +-w; its texture lined up
    // with its neighbours' so the slices make one picture
    void sideWall(int x, int top, int bottom, int w, int thick, boolean port) {
        int h = bottom - top;
        int v = SIDE_C + top - thick;
        if (port) {
            int u = PORT_K - x - 2 - 2 * thick;
            this.statics.texOffs(PORT.u + u, PORT.v + v).addBox(x, top, w - thick, 1, h, thick);
        } else {
            int u = STAR_K + x - thick;
            this.statics.texOffs(STAR.u + u, STAR.v + v).addBox(x, top, -w, 1, h, thick);
        }
    }

    // decking across slice x, its top at y, out to +-inner (planks along the boat, lined up across slices)
    void deck(int x, float y, int inner) {
        if (inner < 1) return;
        this.statics.texOffs(DECK.u + DECK_K + x - 2 * inner, DECK.v + DECK_Z - inner).addBox(x, y - 0.2F, -inner, 1, 0.2F, 2 * inner);
    }

    // a railing along a line of posts (each point {x, y = its foot, z}), `height` tall
    void railing(Zone zone, float[][] points, float height, float thick) {
        for (int i = 0; i < points.length; i++) {
            float[] p = points[i];
            this.box(zone, 0, 0, p[0] - thick / 2, p[1] - height, p[2] - thick / 2, thick, height, thick);
            if (i > 0) {
                float[] q = points[i - 1];
                this.line(zone, q[0], q[1] - height, q[2], p[0], p[1] - height, p[2], thick + 0.05F);
            }
        }
    }

    // a ring of n bars round the x axis (a wheel's rim), into a part; every other bar a hair narrower, so where they
    // meet their faces aren't in one plane
    static void rim(PartDefinition wheel, String prefix, Zone zone, float radius, float thick, float width, int n) {
        float seg = 2 * radius * (float) Math.tan(Math.PI / n) + 0.1F;
        for (int k = 0; k < n; k++) {
            CubeListBuilder bar = CubeListBuilder.create();
            float wk = k % 2 == 0 ? width : width * 0.9F;
            box(bar, zone, 0, 0, -wk / 2, -radius - thick / 2, -seg / 2, wk, thick, seg);
            wheel.addOrReplaceChild(prefix + k, bar, PartPose.offsetAndRotation(0, 0, 0, (float) (k * 2 * Math.PI / n), 0, 0));
        }
    }

    // spokes from the hub out to `radius` round the x axis (a little thinner across than they are wide, and each a
    // hair different, so they don't share a plane with the rim or each other where they cross)
    static void spokes(PartDefinition wheel, String prefix, Zone zone, float radius, float thick, int n, float offset) {
        for (int k = 0; k < n; k++) {
            CubeListBuilder spoke = CubeListBuilder.create();
            float across = thick * (k % 2 == 0 ? 0.72F : 0.56F) - 0.01F * (k % 4);
            float hub = Math.min(2.4F, radius * 0.42F);   // from just outside the hub (where they'd all cross)
            box(spoke, zone, 0, 0, -across / 2, -radius, -thick / 2, across, radius - hub, thick);
            wheel.addOrReplaceChild(prefix + k, spoke, PartPose.offsetAndRotation(0, 0, 0, (float) (offset + k * 2 * Math.PI / n), 0, 0));
        }
    }

    // a ring of n upright panels round a vertical axis at (cx, cz), radius r, from y down h, `thick` thick: a slice of a
    // round body (a teapot, a tub); every other panel a hair thicker so where they meet they don't share a plane
    void ringY(Zone zone, float cx, float y, float cz, float r, float h, float thick, int n, int du) {
        this.ringY(zone, cx, y, cz, r, h, thick, n, du, 2.0F);
    }

    // ... leaving out the panels facing forward (+x) more than `gap` (a cosine: 2 leaves none out): a window
    void ringY(Zone zone, float cx, float y, float cz, float r, float h, float thick, int n, int du, float gap) {
        float seg = 2 * r * (float) Math.tan(Math.PI / n) + 0.15F;
        for (int k = 0; k < n; k++) {
            if (Math.cos(k * 2 * Math.PI / n) > gap) continue;
            CubeListBuilder panel = CubeListBuilder.create();
            float t = k % 2 == 0 ? thick : thick * 0.92F;
            float inset = k % 2 == 0 ? 0.0F : 0.04F;   // every other panel a hair shorter: no shared tops and bottoms
            box(panel, zone, (du + k * 3) % Math.max(1, zone.w - 8), 0, r - t, inset, -seg / 2, t, h - 2 * inset, seg);
            this.part(panel, PartPose.offsetAndRotation(cx, y, cz, 0.0F, (float) (k * 2 * Math.PI / n), 0.0F));
        }
    }

    // a flat disc (a deck, a lid, a saucer): crossed boxes making an octagon-ish round, `h` thick, centred at (cx, cz)
    void disc(Zone zone, float cx, float y, float cz, float r, float h) {
        float s = 2 * r * (float) Math.tan(Math.PI / 8);
        this.box(zone, 0, 0, cx - r, y, cz - s / 2, 2 * r, h, s);
        this.box(zone, 4, 4, cx - s / 2, y + 0.01F, cz - r, s, h * 0.98F, 2 * r);
        CubeListBuilder d1 = CubeListBuilder.create();
        box(d1, zone, 8, 8, -r * 0.98F, 0.0F, -s / 2, 2 * r * 0.98F, h * 0.96F, s);
        this.part(d1, PartPose.offsetAndRotation(cx, y + 0.02F, cz, 0.0F, (float) (Math.PI / 4), 0.0F));
        CubeListBuilder d2 = CubeListBuilder.create();
        box(d2, zone, 12, 12, -r * 0.96F, 0.0F, -s / 2, 2 * r * 0.96F, h * 0.94F, s);
        this.part(d2, PartPose.offsetAndRotation(cx, y + 0.03F, cz, 0.0F, (float) (-Math.PI / 4), 0.0F));
    }

    // a gear round the x axis: a disc of crossed boxes and n teeth
    static void gear(PartDefinition gear, Zone zone, float radius, float width, int teeth) {
        for (int k = 0; k < 4; k++) {
            CubeListBuilder disc = CubeListBuilder.create();
            float side = 2 * radius * (float) Math.tan(Math.PI / 8);
            float wk = width * (1.0F - 0.04F * k);
            box(disc, zone, 0, 0, -wk / 2, -radius, -side / 2, wk, 2 * radius, side);
            gear.addOrReplaceChild("disc" + k, disc, PartPose.offsetAndRotation(0, 0, 0, (float) (k * Math.PI / 4), 0, 0));
        }
        for (int k = 0; k < teeth; k++) {
            CubeListBuilder tooth = CubeListBuilder.create();
            box(tooth, zone, 4, 4, -width / 2 + 0.05F, -radius - 0.7F, -0.4F, width - 0.1F, 0.9F, 0.8F);
            gear.addOrReplaceChild("tooth" + k, tooth, PartPose.offsetAndRotation(0, 0, 0, (float) (k * 2 * Math.PI / teeth), 0, 0));
        }
    }

    // flag strips (no thickness, waving: see PremiumBoatModel.wave), streaming back from a pole at x
    static void flag(PartDefinition parent, Zone zone, int u, int v, float x, float y, int strips, float height, boolean picture) {
        for (int i = 0; i < strips; i++) {
            CubeListBuilder strip = CubeListBuilder.create();
            if (picture) strip.texOffs(u + i, v).addBox(-1.0F, 0.0F, 0.0F, 1.0F, height, 0.0F);
            else box(strip, zone, i, 0, -1.0F, 0.0F, 0.0F, 1.0F, height, 0.0F);
            parent.addOrReplaceChild("f" + i, strip, PartPose.offset(x - i, y, 0.0F));
        }
    }
}

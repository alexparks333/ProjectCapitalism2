package com.alexh.capitalism.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.BoatKit.*;

// The Blood Moon decorations' models (decor/BloodDecorKind, Decorations.md > Blood Moon decorations), drawn by
// client/BloodDecorRenderer. Built with BoatKit like the seasonal ones (client/DecorModels: model pixels, y DOWN, y = 0
// the ground under the block, the front +x), each with its own texture painted by tools/BoatTex.cs
// (textures/entity/<id>.png and <id>_glow.png: the blood-moon palette, BloodDecor there). The state is the time in
// ticks; for the Vampire Coffin it's how far its lid is open (0 to 1).
public final class BloodDecorModels {

    private BloodDecorModels() {
    }

    // what a model is drawn with: the time (ticks), how far it's open (0 to 1: the coffin's lid, a door), whether it's
    // lit, and three more numbers some use (a clock's hands, where the portrait's eyes look)
    public record Anim(float time, float open, boolean lit, float x, float y, float z) {
    }

    public abstract static class Base extends net.minecraft.client.model.Model<Anim> {
        protected final ModelPart decor;

        protected Base(ModelPart root) {
            super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
            this.decor = root.getChild("decor");
        }
    }

    // pictures below y 180 (tools/BoatTex.cs paints them for each decoration that uses one)
    static final Zone PICTURE = new Zone(0, 180, 256, 76);

    // BoatKit with plain numbers (no F on every one)
    static final class K {
        final BoatKit k;

        K(PartDefinition part) {
            this.k = new BoatKit(part);
        }

        K box(Zone z, double x, double y, double zz, double w, double h, double d) {
            this.k.box(z, (float) x, (float) y, (float) zz, (float) w, (float) h, (float) d);
            return this;
        }

        K box(Zone z, int du, int dv, double x, double y, double zz, double w, double h, double d) {
            this.k.box(z, du, dv, (float) x, (float) y, (float) zz, (float) w, (float) h, (float) d);
            return this;
        }

        K line(Zone z, double ax, double ay, double az, double bx, double by, double bz, double t) {
            this.k.line(z, (float) ax, (float) ay, (float) az, (float) bx, (float) by, (float) bz, (float) t);
            return this;
        }

        // a box centred on (x, y, z)
        K cube(Zone z, double x, double y, double zz, double s) {
            return this.box(z, x - s / 2, y - s / 2, zz - s / 2, s, s, s);
        }

        // an upright panel along the line (x1, z1) to (x2, z2), from yTop down h: `inner` thick on its inside (local -z:
        // the left as you go round a shape anticlockwise seen from above... the way the coffin's corners are listed),
        // `outer` on its outside, running `ext` past both ends (to close the corners)
        K panel(Zone zone, int du, double x1, double z1, double x2, double z2, double yTop, double h, double inner, double outer, double ext) {
            double dx = x2 - x1, dz = z2 - z1, len = Math.sqrt(dx * dx + dz * dz);
            CubeListBuilder c = CubeListBuilder.create();
            BoatKit.box(c, zone, du, 0, (float) -ext, 0.0F, (float) -inner, (float) (len + 2 * ext), (float) h, (float) (inner + outer));
            this.k.part(c, PartPose.offsetAndRotation((float) x1, (float) yTop, (float) z1, 0.0F, (float) Math.atan2(-dz, dx), 0.0F));
            return this;
        }

        // a flat slab the shape of a convex outline (corners in order, as for panel): a box down its middle `half` wide
        // either side of z = 0, and a strip `strip` wide inside each slanted edge
        K slab(Zone mid, Zone edges, double[][] corners, double y, double h, double half, double strip) {
            double minX = 1e9, maxX = -1e9;
            for (double[] c : corners) {
                minX = Math.min(minX, c[0]);
                maxX = Math.max(maxX, c[0]);
            }
            this.box(mid, minX, y, -half, maxX - minX, h, 2 * half);
            for (int i = 0; i < corners.length; i++) {
                double[] a = corners[i], b = corners[(i + 1) % corners.length];
                if (Math.abs(a[0] - b[0]) < 1e-3) continue;   // the straight ends: the middle box covers them
                this.panel(edges, i * 5, a[0], a[1], b[0], b[1], y, h * (0.97 - 0.01 * i), strip, 0.0, 0.6);
            }
            return this;
        }

        PartDefinition part(CubeListBuilder c, double x, double y, double z, double xr, double yr, double zr) {
            return this.k.part(c, PartPose.offsetAndRotation((float) x, (float) y, (float) z, (float) xr, (float) yr, (float) zr));
        }

        void done() {
            this.k.root.addOrReplaceChild("statics", this.k.statics, PartPose.ZERO);
        }
    }

    static CubeListBuilder cubes() {
        return CubeListBuilder.create();
    }

    static void bx(CubeListBuilder c, Zone z, double x, double y, double zz, double w, double h, double d) {
        BoatKit.box(c, z, 0, 0, (float) x, (float) y, (float) zz, (float) w, (float) h, (float) d);
    }

    static PartPose at(double x, double y, double z) {
        return PartPose.offset((float) x, (float) y, (float) z);
    }

    static PartPose at(double x, double y, double z, double xr, double yr, double zr) {
        return PartPose.offsetAndRotation((float) x, (float) y, (float) z, (float) xr, (float) yr, (float) zr);
    }

    // A flat picture (a banner, a wing): one of its sizes 0. It isn't drawn as a box with no thickness: the entity render
    // types don't cull back faces, so its two faces would lie in one plane and flicker against each other (dark stripes).
    // Instead each face is its own one-sided sheet, the two a hair (SHEET_GAP) either side of the plane.
    static final float SHEET_GAP = 0.06F;

    static void sheet(CubeListBuilder c, int u, int v, float x, float y, float z, float w, float h, float d) {
        if (w == 0.0F) {
            c.texOffs(u, v).addBox(x + SHEET_GAP, y, z, 0.0F, h, d, java.util.EnumSet.of(net.minecraft.core.Direction.EAST));
            c.texOffs(u, v).addBox(x - SHEET_GAP, y, z, 0.0F, h, d, java.util.EnumSet.of(net.minecraft.core.Direction.WEST));
        } else {
            c.texOffs(u, v).addBox(x, y, z - SHEET_GAP, w, h, 0.0F, java.util.EnumSet.of(net.minecraft.core.Direction.NORTH));
            c.texOffs(u, v).addBox(x, y, z + SHEET_GAP, w, h, 0.0F, java.util.EnumSet.of(net.minecraft.core.Direction.SOUTH));
        }
    }

    // a flat picture seen from one side only (a painting, a dial on something solid): just its front face (+x)
    static void front(CubeListBuilder c, int u, int v, float x, float y, float z, float h, float d) {
        c.texOffs(u, v).addBox(x, y, z, 0.0F, h, d, java.util.EnumSet.of(net.minecraft.core.Direction.EAST));
    }

    // a crescent moon in gold, its horns up, standing in the y-z plane (seen from the front) round (x, cy, 0)
    static void crescent(K k, Zone z, double x, double cy, double r, double thick) {
        int n = 7;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * i / n, a1 = Math.PI * (i + 1) / n;
            double t0 = thick * (0.45 + 0.55 * Math.sin(a0)), t1 = thick * (0.45 + 0.55 * Math.sin(a1));
            k.line(z, x, cy + r * Math.sin(a0), r * Math.cos(a0), x, cy + r * Math.sin(a1), r * Math.cos(a1), Math.max(0.35, (t0 + t1) / 2));
        }
    }

    // ═════════════ the Blood Moon Lantern ═════════════
    // A black iron lantern on a stepped foot: corner posts, a cage of bars with pointed gothic arches traced in gold, a
    // gold band round it, a stepped iron roof with little spires at its corners and a gold crescent moon on top. Inside
    // on a gold cup floats the blood moon itself: a glowing crimson orb, slowly turning, swelling and dimming.
    public static class Lantern extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_moon_lantern");
        private final ModelPart orb;

        public Lantern(ModelPart root) {
            super(root);
            this.orb = this.decor.getChild("orb");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            this.orb.yRot = t * 0.025F;
            this.orb.y = -7.6F + 0.35F * Mth.sin(t * 0.05F);
            float sc = 1.0F + 0.06F * Mth.sin(t * 0.09F);
            this.orb.xScale = sc;
            this.orb.yScale = sc;
            this.orb.zScale = sc;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the stepped foot, gold rims
            k.box(ENGINE, 0, 0, -5, -1.4, -5, 10, 1.4, 10);
            k.box(GOLD, -5.15, -1.75, -5.15, 10.3, 0.45, 0.7).box(GOLD, -5.15, -1.75, 4.45, 10.3, 0.45, 0.7);
            k.box(GOLD, -5.15, -1.72, -4.45, 0.7, 0.42, 8.9).box(GOLD, 4.45, -1.72, -4.45, 0.7, 0.42, 8.9);
            k.box(ENGINE, 6, 6, -4.2, -2.7, -4.2, 8.4, 1.3, 8.4);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    k.cube(BRASS, sx * 4.6, -0.5, sz * 4.6, 1.2);                      // little gold feet
                    k.box(ENGINE, 2, 2, sx * 3.6 - 0.6, -12.4, sz * 3.6 - 0.6, 1.2, 9.7, 1.2);   // the corner posts
                    // the roof's corner spires, gold tipped
                    k.box(ENGINE, 4, 4, sx * 4.2 - 0.5, -14.6, sz * 4.2 - 0.5, 1.0, 1.6, 1.0);
                    k.box(GOLD, sx * 4.2 - 0.3, -15.6, sz * 4.2 - 0.3, 0.6, 1.0, 0.6);
                    k.box(GOLD, sx * 4.2 - 0.15, -16.3, sz * 4.2 - 0.15, 0.3, 0.7, 0.3);
                }
            }
            // the cage: two bars on each side, a pointed arch over them in gold, a gold band round the middle
            for (int side = 0; side < 4; side++) {
                double a = side * Math.PI / 2;
                double nx = Math.cos(a), nz = Math.sin(a), tx = -nz, tz = nx;
                for (int b = -1; b <= 1; b += 2) {
                    double px = nx * 3.6 + tx * b * 1.25, pz = nz * 3.6 + tz * b * 1.25;
                    k.box(ENGINE, 8 + side, 0, px - 0.22, -12.0, pz - 0.22, 0.44, 9.3, 0.44);
                }
                double fx = nx * 3.75, fz = nz * 3.75;
                k.line(BRASS, fx + tx * 3.0, -9.0, fz + tz * 3.0, fx, -11.9, fz, 0.42);
                k.line(BRASS, fx - tx * 3.0, -9.0, fz - tz * 3.0, fx, -11.9, fz, 0.42);
                k.box(BRASS, 4 * side, 0, fx - Math.abs(tx) * 3.6 - Math.abs(nx) * 0.2, -5.0, fz - Math.abs(tz) * 3.6 - Math.abs(nz) * 0.2,
                    Math.abs(tx) * 7.2 + Math.abs(nx) * 0.4, 0.5, Math.abs(tz) * 7.2 + Math.abs(nz) * 0.4);
            }
            k.box(BRASS, 20, 20, -1.5, -3.7, -1.5, 3.0, 1.0, 3.0);                          // the cup the orb floats over
            // rubies set in the foot, gold brackets on the corner posts
            for (int side = 0; side < 4; side++) {
                double a = side * Math.PI / 2, nx = Math.cos(a), nz = Math.sin(a);
                k.cube(NAV_RED, nx * 5.05, -0.75, nz * 5.05, 0.6);
            }
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    k.line(BRASS, sx * 4.25, -3.0, sz * 4.25, sx * 4.25, -4.6, sz * 4.25, 0.35);
                    k.line(BRASS, sx * 4.25, -10.8, sz * 4.25, sx * 4.25, -12.2, sz * 4.25, 0.35);
                }
            }
            k.box(GOLD, -1.0, -4.1, -1.0, 2.0, 0.4, 2.0);
            // the roof: an iron plate with a gold rim, steps up to a point, the crescent moon on top
            k.box(ENGINE, 0, 14, -4.8, -13.2, -4.8, 9.6, 0.9, 9.6);
            k.box(GOLD, -4.95, -12.55, -4.95, 9.9, 0.35, 0.5).box(GOLD, -4.95, -12.55, 4.45, 9.9, 0.35, 0.5);
            k.box(GOLD, -4.95, -12.52, -4.45, 0.5, 0.32, 8.9).box(GOLD, 4.45, -12.52, -4.45, 0.5, 0.32, 8.9);
            k.box(ENGINE, 4, 16, -3.8, -14.1, -3.8, 7.6, 0.95, 7.6);
            k.box(ENGINE, 8, 18, -2.8, -15.0, -2.8, 5.6, 0.95, 5.6);
            k.box(ENGINE, 12, 20, -1.8, -15.9, -1.8, 3.6, 0.95, 3.6);
            k.box(ENGINE, 16, 22, -0.9, -16.8, -0.9, 1.8, 0.95, 1.8);
            k.box(BRASS, -0.35, -18.2, -0.35, 0.7, 1.5, 0.7);
            crescent(k, BRASS, 0, -20.4, 2.3, 0.85);
            k.cube(NAV_RED, 0, -18.6, 0, 0.8);
            // the blood moon: a glowing orb (three crossed boxes, round enough), turning
            CubeListBuilder orb = cubes();
            bx(orb, WARM, -2.5, -1.6, -1.6, 5.0, 3.2, 3.2);
            BoatKit.box(orb, WARM, 6, 2, -1.6F, -2.5F, -1.6F, 3.2F, 5.0F, 3.2F);
            BoatKit.box(orb, WARM, 12, 4, -1.6F, -1.6F, -2.5F, 3.2F, 3.2F, 5.0F);
            BoatKit.box(orb, WARM, 18, 6, -2.1F, -2.1F, -2.1F, 4.2F, 4.2F, 4.2F);
            root.addOrReplaceChild("orb", orb, at(0, -7.6, 0));
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Crimson Candelabra ═════════════
    // A tall black iron candelabra on three clawed feet with gold balls, a stem with gold knops, and S-curled arms
    // spreading to five tall red candles (the middle one highest) on gold drip cups hung with little ruby drops. Wax
    // has run down the candles and pooled in the cups; each flame flickers on its own.
    public static class Candelabra extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_candelabra");
        // each candle: z, the top of its cup, its height
        static final double[][] CANDLES = {{0, -19.2, 6.8}, {3.8, -17.0, 5.6}, {-3.8, -17.0, 5.6}, {7.2, -16.0, 4.6}, {-7.2, -16.0, 4.6}};
        private final List<ModelPart> flames = new ArrayList<>();

        public Candelabra(ModelPart root) {
            super(root);
            for (int i = 0; i < CANDLES.length; i++) this.flames.add(this.decor.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            for (int i = 0; i < this.flames.size(); i++) {
                ModelPart f = this.flames.get(i);
                f.yScale = 0.85F + 0.18F * Mth.sin(t * 0.55F + i * 1.9F) + 0.1F * Mth.sin(t * 1.37F + i * 0.7F);
                f.xScale = 1.0F - 0.08F * Mth.sin(t * 0.9F + i);
                f.zRot = 0.08F * Mth.sin(t * 0.21F + i * 2.3F);
                f.xRot = 0.06F * Mth.sin(t * 0.17F + i * 1.1F);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the base: a round iron foot, three clawed legs with gold balls
            k.box(ENGINE, 0, 0, -3.0, -1.6, -3.0, 6.0, 1.0, 6.0);
            k.box(ENGINE, 4, 4, -2.2, -2.6, -2.2, 4.4, 1.0, 4.4);
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3 + Math.PI / 6;
                double c = Math.cos(a), s = Math.sin(a);
                k.line(ENGINE, c * 1.8, -1.8, s * 1.8, c * 4.2, -1.6, s * 4.2, 1.1);
                k.line(ENGINE, c * 4.2, -1.6, s * 4.2, c * 5.4, -0.7, s * 5.4, 0.9);
                k.cube(BRASS, c * 5.5, -0.6, s * 5.5, 1.3);
                k.line(ENGINE, c * 5.9, -0.9, s * 5.9, c * 6.5, -0.1, s * 6.5, 0.35);
            }
            // the stem: iron, gold knops, an urn at its top
            k.box(ENGINE, 8, 0, -0.85, -12.2, -0.85, 1.7, 9.8, 1.7);
            k.box(BRASS, 0, 0, -1.5, -4.6, -1.5, 3.0, 1.4, 3.0);
            k.box(BRASS, 8, 0, -1.2, -8.2, -1.2, 2.4, 1.0, 2.4);
            k.box(BRASS, 14, 0, -1.0, -9.6, -1.0, 2.0, 0.6, 2.0);
            k.box(ENGINE, 12, 8, -1.7, -13.6, -1.7, 3.4, 1.8, 3.4);
            k.box(GOLD, -1.85, -13.9, -1.85, 3.7, 0.4, 3.7);
            k.box(GOLD, -1.25, -12.0, -1.25, 2.5, 0.35, 2.5);
            // the arms: S-curls out to the candles, gold beads at the bends
            k.line(ENGINE, 0, -13.5, 0, 0, -19.2, 0, 1.0);
            for (int side = -1; side <= 1; side += 2) {
                k.line(ENGINE, 0, -13.2, 0, 0, -12.6, side * 2.0, 0.8);
                k.line(ENGINE, 0, -12.6, side * 2.0, 0, -13.6, side * 3.4, 0.75);
                k.line(ENGINE, 0, -13.6, side * 3.4, 0, -17.0, side * 3.8, 0.75);
                k.line(ENGINE, 0, -13.0, side * 1.0, 0, -11.6, side * 4.0, 0.75);
                k.line(ENGINE, 0, -11.6, side * 4.0, 0, -11.8, side * 6.2, 0.7);
                k.line(ENGINE, 0, -11.8, side * 6.2, 0, -13.4, side * 7.4, 0.7);
                k.line(ENGINE, 0, -13.4, side * 7.4, 0, -16.0, side * 7.2, 0.7);
                k.cube(BRASS, 0, -12.6, side * 2.0, 0.9);
                k.cube(BRASS, 0, -11.65, side * 4.0, 0.9);
                k.cube(BRASS, 0, -13.4, side * 7.4, 0.85);
                // a little curl hanging under the outer arm
                k.line(ENGINE, 0, -11.7, side * 5.0, 0, -10.6, side * 5.6, 0.45);
                k.line(ENGINE, 0, -10.6, side * 5.6, 0, -10.9, side * 6.3, 0.4);
            }
            for (int i = 0; i < CANDLES.length; i++) {
                double z = CANDLES[i][0], y0 = CANDLES[i][1], h = CANDLES[i][2];
                // the drip cup, two ruby drops hanging from it
                k.box(BRASS, 4 * i, 30, -1.45, y0, z - 1.45, 2.9, 0.55, 2.9);
                k.box(GOLD, -0.9, y0 + 0.5, z - 0.9, 1.8, 0.5, 1.8);
                for (int d = -1; d <= 1; d += 2) {
                    k.line(GOLD, 0, y0 + 0.5, z + d * 1.2, 0, y0 + 1.5, z + d * 1.3, 0.2);
                    k.cube(RED, 0, y0 + 1.75, z + d * 1.3, 0.55);
                }
                // the candle, wax pooled at its foot and run down its sides, a melted rim, the wick
                k.box(BAMBOO, 2 * i, 0, -0.8, y0 - h, z - 0.8, 1.6, h, 1.6);
                k.box(BAMBOO, 12 + i, 8, -1.15, y0 - 0.45, z - 1.15, 2.3, 0.45, 2.3);
                k.box(BAMBOO, 16, 16, -0.9, y0 - h - 0.25, z - 0.9, 1.8, 0.5, 1.8);
                double drip1 = 1.4 + (i % 3) * 0.9, drip2 = 2.4 - (i % 2) * 0.9;
                k.box(BAMBOO, 20, 2, 0.75, y0 - h + 0.1, z - 0.35, 0.3, drip1, 0.55);
                k.box(BAMBOO, 22, 2, 0.7, y0 - h + 0.05 + drip1, z - 0.3, 0.4, 0.45, 0.6);
                k.box(BAMBOO, 24, 2, -0.35 + (i % 2) * 0.3, y0 - h + 0.1, z + 0.75, 0.5, drip2, 0.3);
                k.box(BAMBOO, 26, 2, -1.05, y0 - h + 0.1, z - 0.1 - (i % 2) * 0.4, 0.3, drip2 * 0.6, 0.45);
                k.box(BLACK, -0.12, y0 - h - 0.85, z - 0.12, 0.24, 0.65, 0.24);
                CubeListBuilder flame = cubes();
                bx(flame, FIRE, -0.5, -1.5, -0.5, 1.0, 1.5, 1.0);
                BoatKit.box(flame, FIRE, 6, 2, -0.3F, -2.3F, -0.3F, 0.6F, 0.9F, 0.6F);
                BoatKit.box(flame, WARM, 0, 0, -0.14F, -2.9F, -0.14F, 0.28F, 0.7F, 0.28F);
                root.addOrReplaceChild("flame" + i, flame, at(0, y0 - h - 0.7, z));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Crimson Throne (about 2.5 blocks tall) ═════════════
    // A high-backed gothic throne of black lacquered wood: clawed gold feet, a seat with a carved apron trimmed in gold,
    // a deep crimson velvet cushion and armrests padded in velvet with gold scroll ends. Its tall back rises to a pointed
    // gothic arch, button-tufted crimson velvet inside, traced in gold, gold finials on its posts and a ruby set in a gold
    // crescent at its peak; from the top of its posts carved bat wings spread wide.
    public static class Throne extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_throne");
        // the wing picture (tools/BoatTex.cs): its two sides side by side
        static final int WING_U = 0, WING_V = 180;
        static final float WING_W = 16.0F, WING_H = 15.0F;

        private final ModelPart[] bats = new ModelPart[3];

        public Throne(ModelPart root) {
            super(root);
            for (int i = 0; i < 3; i++) this.bats[i] = this.decor.getChild("bat" + i);
        }

        // someone sits on it (open) and it's night (z): bats circle over the lord's head
        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            boolean circling = s.open() > 0.5F && s.z() > 0.5F;
            for (int i = 0; i < 3; i++) {
                this.bats[i].visible = circling;
                if (circling) BloodDecorModels2.flyBat(this.bats[i], s.time(), i, 9.0F, -58.0F - i * 3.0F);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // legs, clawed gold feet
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    double x = sx * 5.3, z = sz * 5.3;
                    k.box(OLDWOOD, 2 * sx + 4, 0, x - 0.9, -8.0, z - 0.9, 1.8, 6.8, 1.8);
                    k.box(OLDWOOD, 10, 4, x - 1.25, -5.6, z - 1.25, 2.5, 1.8, 2.5);
                    k.box(BRASS, 0, 0, x - 1.3, -1.3, z - 1.3, 2.6, 1.3, 2.6);
                    for (int c = -1; c <= 1; c++) k.line(BRASS, x + sx * 0.6, -1.0, z + c * 0.7, x + sx * 1.9, -0.2, z + c * 1.0, 0.45);
                }
            }
            // the seat: frame, carved apron in front, gold trim, the velvet cushion
            k.box(OLDWOOD, 0, 8, -6.5, -9.2, -6.5, 13.0, 1.8, 13.0);
            k.box(OLDWOOD, 26, 0, 6.1, -7.6, -5.2, 0.6, 1.4, 10.4);
            k.box(OLDWOOD, 26, 4, 6.15, -6.4, -3.0, 0.5, 0.8, 6.0);
            k.box(BRASS, 0, 10, 6.45, -9.0, -6.3, 0.35, 0.5, 12.6);
            k.box(BRASS, 0, 14, -6.3, -9.0, 6.45, 12.6, 0.5, 0.35).box(BRASS, 0, 16, -6.3, -9.0, -6.8, 12.6, 0.5, 0.35);
            k.cube(NAV_RED, 6.75, -6.9, 0, 0.7);
            k.box(CANVAS, 0, 0, -5.4, -11.0, -5.8, 11.6, 1.9, 11.6);
            k.box(CANVAS, 6, 18, -5.0, -11.3, -5.4, 10.8, 0.4, 10.8);
            k.box(GOLD, 6.2, -10.9, -5.8, 0.25, 0.3, 11.6);
            // armrests: posts, a padded top, gold scrolls at the front
            for (int s = -1; s <= 1; s += 2) {
                double z = s * 6.2;
                k.box(OLDWOOD, 20, 10, -5.4, -14.6, z - 0.9, 11.0, 1.4, 1.8);
                k.box(CANVAS, 30, 2, -4.6, -15.2, z - 0.7, 9.4, 0.6, 1.4);
                k.box(OLDWOOD, 24, 14, 4.0, -13.2, z - 0.7, 1.4, 4.0, 1.4);
                k.box(OLDWOOD, 28, 14, 1.0, -13.2, z - 0.55, 1.1, 4.0, 1.1);
                k.box(BRASS, 6, 20, 5.0, -15.0, z - 1.1, 1.3, 2.3, 2.2);
                k.cube(GOLD, 6.0, -15.5, z, 1.0);
                // a carved side panel under the arm
                k.box(OLDWOOD, 2, 24, -5.0, -12.9, z - 0.4, 9.0, 3.6, 0.8);
                k.box(BRASS, 10, 26, -3.0, -12.2, z + (s > 0 ? 0.35 : -0.45), 5.0, 0.3, 0.1);
            }
            // the back: posts up the sides, the velvet panel, the pointed arch over it, gold tracing
            for (int s = -1; s <= 1; s += 2) {
                double z = s * 5.9;
                k.box(OLDWOOD, 4 + (s > 0 ? 8 : 0), 0, -7.1, -37.0, z - 0.95, 1.9, 28.0, 1.9);
                k.cube(BRASS, -6.15, -37.9, z, 1.8);
                k.box(GOLD, -6.4, -40.2, z - 0.25, 0.5, 1.8, 0.5);
                k.box(GOLD, -6.3, -41.0, z - 0.15, 0.3, 0.8, 0.3);
                k.box(BRASS, 16, 0, -7.25, -24.0, z - 1.1, 2.2, 0.6, 2.2);
            }
            k.box(OLDWOOD, 40, 0, -7.0, -10.5, -5.0, 1.6, 1.4, 10.0);
            k.box(CANVAS, 40, 0, -6.5, -31.5, -5.0, 1.1, 21.0, 10.0);
            // the arch: velvet steps rising to a point, the wooden frame round it
            double[][] arch = {{-31.5, 4.4}, {-33.0, 3.6}, {-34.5, 2.7}, {-36.0, 1.8}, {-37.5, 0.9}};
            for (int i = 0; i < arch.length; i++) {
                k.box(CANVAS, 44 + i, 2, -6.45, arch[i][0] - 1.5, -arch[i][1], 1.0, 1.55, 2 * arch[i][1]);
            }
            k.line(OLDWOOD, -6.2, -31.0, 5.6, -6.2, -39.6, 0, 1.6);
            k.line(OLDWOOD, -6.2, -31.0, -5.6, -6.2, -39.6, 0, 1.6);
            k.line(BRASS, -5.3, -31.0, 4.8, -5.3, -38.6, 0, 0.45);
            k.line(BRASS, -5.3, -31.0, -4.8, -5.3, -38.6, 0, 0.45);
            k.box(BRASS, 30, 30, -5.45, -31.0, 4.85, 0.4, 20.0, 0.45);
            k.box(BRASS, 32, 30, -5.45, -31.0, -5.3, 0.4, 20.0, 0.45);
            k.box(BRASS, 4, 34, -5.45, -11.2, -5.0, 0.4, 0.45, 10.0);
            // the crest: a gold crescent holding a ruby at the peak
            crescent(k, BRASS, -6.0, -41.8, 1.9, 0.75);
            k.box(BRASS, -6.6, -40.5, -0.8, 1.0, 1.0, 1.6);
            k.cube(NAV_RED, -5.95, -41.6, 0, 1.15);
            for (int i = 0; i < 3; i++) BloodDecorModels2.flyingBat(root, "bat" + i, BLACK, OLDWOOD);
            // bat wings spreading from the top of the posts (pictures: carved black wood, gold edged)
            for (int s = -1; s <= 1; s += 2) {
                CubeListBuilder wing = cubes();
                sheet(wing, WING_U, WING_V, 0.0F, 0.0F, 0.0F, WING_W, WING_H, 0.0F);
                // (the -z wing shows the picture's second side: painted mirrored, so both are the same wing)
                PartDefinition w = root.addOrReplaceChild("wing" + (s > 0 ? "R" : "L"), wing, at(-6.6, -46.5, s * 5.6, 0, s > 0 ? -1.28 : 1.28, 0));
                // raised gold ribs on the carved wing (in the picture's frame)
                BoatKit.line(w, "arm", BRASS, 0.0F, 10.0F, 0.0F, 5.6F, 3.2F, 0.0F, 0.7F);
                float[][] tips = {{15.8F, 0.3F}, {15.6F, 6.4F}, {12.2F, 11.4F}, {7.0F, 14.6F}};
                for (int f = 0; f < tips.length; f++) {
                    BoatKit.line(w, "f" + f, BRASS, 5.6F, 3.2F, 0.0F, 5.6F + (tips[f][0] - 5.6F) * 0.94F, 3.2F + (tips[f][1] - 3.2F) * 0.94F, 0.0F, 0.42F);
                }
                BoatKit.line(w, "claw", BRASS, 5.6F, 3.2F, 0.0F, 4.8F, 1.4F, 0.0F, 0.35F);
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Vampire Coffin (2 blocks long) ═════════════
    // A classic six-sided coffin of black ebony on a gold-striped plinth moulding, gold handles down its sides. Inside it's
    // lined with pleated red satin, a crimson velvet pillow at its head with a single red rose on it. The lid has a
    // raised panel with a long gold cross, a ruby where its arms meet, gold caps on its corners and a gold bat at its foot.
    // Right-click: the lid slides off and leans against its side, showing the lining (the state: 0 shut, 1 open).
    public static class Coffin extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("vampire_coffin");
        // its outline from above, the head toward -x: round from the head's left corner (interior on each edge's left)
        static final double[][] OUTER = {{-15, 4.5}, {-8, 7}, {15, 4}, {15, -4}, {-8, -7}, {-15, -4.5}};
        static final double[][] INNER = {{-14, 3.8}, {-8, 5.95}, {14, 3.12}, {14, -3.12}, {-8, -5.95}, {-14, -3.8}};
        static final double[][] RAISED = {{-13.2, 3.1}, {-8, 5.0}, {13.0, 2.5}, {13.0, -2.5}, {-8, -5.0}, {-13.2, -3.1}};
        static final float LID_Y = -8.6F;
        private final ModelPart lid;

        public Coffin(ModelPart root) {
            super(root);
            this.lid = this.decor.getChild("lid");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float o = Mth.clamp(s.open(), 0.0F, 1.0F);
            // eased: it lifts, slides aside and settles leaning on the coffin's side
            float e = o * o * (3 - 2 * o);
            this.lid.y = LID_Y + (-4.3F - LID_Y) * e - 2.5F * Mth.sin(e * Mth.PI);
            this.lid.z = 10.6F * e;
            this.lid.xRot = -0.74F * e;
        }

            public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // (round each outline every edge has its inside on its local -z)
            double[][] outer = OUTER, inner = INNER, raised = RAISED;
            // the floor, the walls, a moulding round the foot with a gold stripe, a gold stripe round the top
            k.slab(DECK, DARKWOOD, outer, -1.0, 1.0, 4.0, 3.4);
            for (int i = 0; i < 6; i++) {
                double[] a = outer[i], b = outer[(i + 1) % 6];
                k.panel(DARKWOOD, i * 7, a[0], a[1], b[0], b[1], -8.0, 8.0 - 0.02 * i, 1.0, 0.0, 0.5);
                k.panel(DARKWOOD, 3 + i * 7, a[0], a[1], b[0], b[1], -1.3, 1.3, 0.0, 0.45, 0.2);
                k.panel(BRASS, i * 3, a[0], a[1], b[0], b[1], -1.65, 0.35, 0.0, 0.5, 0.15);
                k.panel(BRASS, 2 + i * 3, a[0], a[1], b[0], b[1], -7.6, 0.4, 0.0, 0.18, 0.1);
            }
            // the satin lining and the floor's satin, the pillow, the rose on it
            for (int i = 0; i < 6; i++) {
                double[] a = inner[i], b = inner[(i + 1) % 6];
                k.panel(WHITE, i * 6, a[0], a[1], b[0], b[1], -7.3, 6.0, 0.35, 0.0, 0.4);
            }
            k.slab(WHITE, WHITE, inner, -1.35, 0.35, 3.1, 3.0);
            k.box(CANVAS, 0, 0, -13.6, -3.0, -3.3, 4.2, 1.7, 6.6);
            k.box(CANVAS, 30, 0, -13.2, -3.3, -2.9, 3.4, 0.4, 5.8);
            k.box(GOLD, -13.65, -2.0, -3.35, 4.3, 0.2, 0.2).box(GOLD, -13.65, -2.0, 3.15, 4.3, 0.2, 0.2);
            k.line(VINE, -11.2, -3.6, 1.6, -7.0, -1.6, -0.6, 0.3);
            k.cube(RED, -11.4, -3.9, 1.8, 1.1);
            k.cube(RED, -11.2, -4.25, 1.6, 0.7);
            k.box(VINE, -9.0, -2.5, -0.1, 0.9, 0.15, 0.6);
            // gold handles down each side
            for (int s = -1; s <= 1; s += 2) {
                double[] a = s > 0 ? OUTER[1] : OUTER[4], b = s > 0 ? OUTER[2] : OUTER[3];
                for (int h = 0; h < 2; h++) {
                    double t = 0.25 + h * 0.45;
                    double x = a[0] + (b[0] - a[0]) * t, z = a[1] + (b[1] - a[1]) * t;
                    double ang = Math.atan2(-(b[1] - a[1]), b[0] - a[0]);
                    CubeListBuilder handle = cubes();
                    double out = s > 0 ? 0.0 : -0.55;
                    bx(handle, BRASS, -2.0, 0.0, out, 4.0, 0.5, 0.55);
                    bx(handle, BRASS, -2.0, -0.9, out + 0.05, 0.5, 1.0, 0.45);
                    bx(handle, BRASS, 1.5, -0.9, out + 0.05, 0.5, 1.0, 0.45);
                    k.part(handle, x, -4.6, z, 0, ang, 0);
                }
            }
            // the lid (its own part: it swings off), built round its middle at LID_Y
            PartDefinition lid = root.addOrReplaceChild("lid", cubes(), at(0, LID_Y, 0));
            K l = new K(lid);
            l.slab(DECK, DARKWOOD, outer, -0.6, 1.2, 4.0, 3.4);
            l.slab(OLDWOOD, OLDWOOD, raised, -1.2, 0.6, 2.4, 2.4);
            // gold caps on its corners
            for (double[] c : OUTER) l.box(BRASS, c[0] - 0.8 * Math.signum(c[0]) - 0.6, -0.85, c[1] - 0.8 * Math.signum(c[1]) - 0.6, 1.2, 0.3, 1.2);
            // the long gold cross, the ruby where its arms meet
            l.box(BRASS, 0, 0, -11.0, -1.65, -0.75, 10.0, 0.45, 1.5);
            l.box(BRASS, 0, 4, -1.0, -1.65, -0.75, 10.0, 0.45, 1.5);
            l.box(BRASS, 0, 8, -7.4, -1.68, -3.6, 1.5, 0.48, 7.2);
            l.box(BRASS, 4, 12, -11.5, -1.62, -1.2, 1.0, 0.42, 2.4);
            l.box(BRASS, 4, 16, -7.85, -1.62, -4.0, 2.4, 0.42, 1.0);
            l.box(BRASS, 4, 20, -7.85, -1.62, 3.0, 2.4, 0.42, 1.0);
            l.box(BRASS, 4, 24, 8.6, -1.62, -1.2, 1.0, 0.42, 2.4);
            l.box(GOLD, -7.9, -1.95, -1.0, 2.0, 0.3, 2.0);
            l.cube(RED, -6.9, -2.1, 0, 1.2);
            // a gold bat at its foot: body, head with ears, wings
            l.box(BRASS, 10.6, -1.55, -0.5, 2.0, 0.4, 1.0);
            l.box(BRASS, 12.4, -1.6, -0.45, 0.9, 0.42, 0.9);
            l.line(BRASS, 13.1, -1.7, -0.35, 13.7, -1.7, -0.6, 0.25);
            l.line(BRASS, 13.1, -1.7, 0.35, 13.7, -1.7, 0.6, 0.25);
            for (int s = -1; s <= 1; s += 2) {
                l.line(BRASS, 11.8, -1.55, s * 0.4, 12.6, -1.55, s * 2.6, 0.32);
                l.line(BRASS, 12.6, -1.55, s * 2.6, 11.6, -1.55, s * 3.4, 0.28);
                l.line(BRASS, 11.6, -1.55, s * 3.4, 11.0, -1.55, s * 2.4, 0.26);
                l.line(BRASS, 11.0, -1.55, s * 2.4, 10.4, -1.55, s * 2.9, 0.26);
                l.line(BRASS, 10.4, -1.55, s * 2.9, 10.8, -1.55, s * 0.5, 0.26);
            }
            l.done();
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Blood Fountain (2 x 2 blocks) ═════════════
    // A gothic stone fountain in three tiers. A wide sixteen-sided basin on a stepped plinth, buttresses round it and four
    // spires on its rim set with rubies, brimming with glowing blood; ripples spread where the streams land and bubbles
    // swell and pop. From the carved pillar four stone bat heads spit arcing jets of blood out into the basin, drops
    // racing along them. Above, a bowl overflows in four falling streams, and a small top bowl spills into it in turn;
    // at the very top a gold crown holds the blood moon, glowing and slowly turning. (Mist, spray and bubbling sounds:
    // decor/BloodDecorBlock.animateTick.)
    public static class Fountain extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_fountain");
        // the blood (tools/BloodDecorTex.cs paints this whole patch: glowing blood)
        static final Zone POOL = new Zone(0, 180, 128, 40);
        static final int BUBBLES = 9;
        static final float SURFACE = -6.3F;
        // a spout's jet, in its own part (local +x outward): from the bat's mouth out to the water
        static final float JET_X0 = 4.9F, JET_Y0 = -10.7F, JET_X1 = 10.8F;
        private final List<ModelPart> bubbles = new ArrayList<>();
        private final List<ModelPart> drops = new ArrayList<>();
        private final List<ModelPart> jetDrops = new ArrayList<>();
        private final List<ModelPart> ripples = new ArrayList<>();
        private final ModelPart moon;

        public Fountain(ModelPart root) {
            super(root);
            for (int i = 0; i < BUBBLES; i++) this.bubbles.add(this.decor.getChild("bubble" + i));
            for (int i = 0; i < 8; i++) this.drops.add(this.decor.getChild("drop" + i));
            for (int i = 0; i < 4; i++) {
                ModelPart spout = this.decor.getChild("spout" + i);
                for (int d = 0; d < 3; d++) this.jetDrops.add(spout.getChild("jd" + d));
                this.ripples.add(this.decor.getChild("ripple" + i));
                this.ripples.add(this.decor.getChild("splash" + i));
            }
            this.moon = this.decor.getChild("moon");
        }

        static float jetY(float f) {
            return JET_Y0 - 3.2F * f + (SURFACE - JET_Y0 + 3.2F) * f * f;
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            for (int i = 0; i < BUBBLES; i++) {
                float phase = (t * 0.03F + i * 0.29F) % 1.0F;
                ModelPart b = this.bubbles.get(i);
                float sc = phase < 0.85F ? 0.3F + phase * 1.0F : 0.0F;
                b.xScale = sc;
                b.yScale = sc * 0.8F;
                b.zScale = sc;
            }
            for (int i = 0; i < this.drops.size(); i++) {
                float phase = (t * 0.045F + i * 0.53F) % 1.0F;
                this.drops.get(i).y = i < 4 ? -16.0F + phase * 9.6F : -21.8F + phase * 5.0F;
            }
            for (int i = 0; i < this.jetDrops.size(); i++) {
                float f = (t * 0.035F + (i % 3) / 3.0F + (i / 3) * 0.11F) % 1.0F;
                ModelPart d = this.jetDrops.get(i);
                d.x = JET_X0 + (JET_X1 - JET_X0) * f;
                d.y = jetY(f);
            }
            for (int i = 0; i < this.ripples.size(); i++) {
                float p = (t * 0.03F + i * 0.17F) % 1.0F;
                ModelPart r = this.ripples.get(i);
                r.visible = p < 0.9F;
                r.xScale = 0.35F + 1.5F * p;
                r.zScale = 0.35F + 1.5F * p;
            }
            this.moon.yRot = t * 0.02F;
            this.moon.y = -26.2F + 0.3F * Mth.sin(t * 0.05F);
        }

        // a thin ring lying flat, radius r (for the ripples)
        static CubeListBuilder ring(float r) {
            CubeListBuilder c = cubes();
            int n = 10;
            for (int k = 0; k < n; k++) {
                double a = k * 2 * Math.PI / n;
                float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
                BoatKit.box(c, POOL, 60 + k, 30, x - 0.45F, -0.08F - (k % 2) * 0.02F, z - 0.45F, 0.9F, 0.06F, 0.9F);
            }
            return c;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the stepped plinth, the basin wall, its carved rim, a moulding at its foot
            k.k.ringY(GREY, 0.0F, -1.2F, 0.0F, 16.0F, 1.2F, 3.2F, 16, 0);
            k.k.ringY(GREY, 0.0F, -7.6F, 0.0F, 14.9F, 6.4F, 2.2F, 16, 6);
            k.k.ringY(WHITE, 0.0F, -8.7F, 0.0F, 15.5F, 1.15F, 3.0F, 16, 12);
            k.k.ringY(WHITE, 0.0F, -2.2F, 0.0F, 15.25F, 1.0F, 0.6F, 16, 2);
            k.k.ringY(BRASS, 0.0F, -9.05F, 0.0F, 15.65F, 0.3F, 0.35F, 16, 0);
            // buttresses at the joints, each with a little carved arch and a gold stud
            for (int i = 0; i < 8; i++) {
                CubeListBuilder b = cubes();
                bx(b, WHITE, 14.7, -7.6, -0.9, 1.6, 6.4, 1.8);
                bx(b, WHITE, 15.0, -3.0, -1.1, 1.6, 1.8, 2.2);
                bx(b, BLACK, 16.33, -6.4, -0.45, 0.06, 2.6, 0.9);
                bx(b, GOLD, 16.35, -6.95, -0.25, 0.06, 0.5, 0.5);
                k.part(b, 0, 0, 0, 0, (2 * i + 1) * Math.PI / 16, 0);
            }
            // spires on the rim, rubies in them
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + i * Math.PI / 2, c = Math.cos(a) * 14.9, sn = Math.sin(a) * 14.9;
                k.box(WHITE, c - 0.9, -10.6, sn - 0.9, 1.8, 1.9, 1.8);
                k.box(WHITE, c - 0.6, -12.0, sn - 0.6, 1.2, 1.4, 1.2);
                k.box(WHITE, c - 0.35, -13.2, sn - 0.35, 0.7, 1.2, 0.7);
                k.box(GOLD, c - 0.2, -14.0, sn - 0.2, 0.4, 0.8, 0.4);
                k.cube(NAV_RED, c + Math.cos(a) * 0.95, -9.75, sn + Math.sin(a) * 0.95, 0.5);
            }
            k.k.disc(DECK, 0.0F, -2.3F, 0.0F, 13.6F, 1.0F);
            // the blood in the basin
            k.k.disc(POOL, 0.0F, SURFACE, 0.0F, 13.75F, 0.45F);
            // the pillar: a block hidden in the blood, a carved shaft with colonnettes and gold bands
            k.box(GREY, -3.5, -7.0, -3.5, 7.0, 4.8, 7.0);
            k.box(WHITE, 0, 0, -2.2, -14.6, -2.2, 4.4, 8.0, 4.4);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) k.box(WHITE, 20, 0, sx * 2.4 - 0.5, -14.0, sz * 2.4 - 0.5, 1.0, 7.0, 1.0);
            }
            k.box(BRASS, -2.45, -8.0, -2.45, 4.9, 0.5, 4.9);
            k.box(BRASS, -2.45, -14.1, -2.45, 4.9, 0.4, 4.9);
            // four stone bat heads spitting arcing jets of blood into the basin
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + i * Math.PI / 2;
                PartDefinition spout = root.addOrReplaceChild("spout" + i, cubes(), at(0, 0, 0, 0, -a, 0));
                CubeListBuilder head = cubes();
                bx(head, WHITE, 2.2, -12.4, -1.25, 2.3, 2.5, 2.5);
                bx(head, WHITE, 4.4, -11.4, -0.85, 0.9, 1.3, 1.7);
                bx(head, BLACK, 5.25, -11.15, -0.6, 0.12, 0.8, 1.2);
                bx(head, NAV_RED, 4.45, -12.0, -1.05, 0.12, 0.45, 0.5);
                bx(head, NAV_RED, 4.45, -12.0, 0.55, 0.12, 0.45, 0.5);
                spout.addOrReplaceChild("head", head, PartPose.ZERO);
                for (int s = -1; s <= 1; s += 2) {
                    String n = s < 0 ? "L" : "R";
                    BoatKit.line(spout, "ear" + n, WHITE, 2.9F, -12.3F, s * 0.9F, 2.6F, -14.4F, s * 1.6F, 0.7F);
                    BoatKit.line(spout, "fang" + n, BONE, 5.0F, -10.9F, s * 0.5F, 5.05F, -10.3F, s * 0.5F, 0.25F);
                    // wings folded back against the pillar
                    BoatKit.line(spout, "wing" + n, WHITE, 2.5F, -11.6F, s * 1.2F, 2.3F, -9.0F, s * 2.4F, 0.6F);
                }
                // the jet: a parabola of glowing blood, thinning as it falls
                int n = 9;
                float px = JET_X0, py = JET_Y0;
                for (int j = 1; j <= n; j++) {
                    float f = j / (float) n;
                    float x = JET_X0 + (JET_X1 - JET_X0) * f, y = jetY(f);
                    BoatKit.line(spout, "jet" + j, POOL, px, py, 0.0F, x, y, 0.0F, 0.75F - 0.3F * f);
                    px = x;
                    py = y;
                }
                for (int d = 0; d < 3; d++) {
                    CubeListBuilder drop = cubes();
                    bx(drop, POOL, -0.45, -0.45, -0.45, 0.9, 0.9, 0.9);
                    spout.addOrReplaceChild("jd" + d, drop, at(JET_X0, JET_Y0, 0));
                }
                // its ripple where it lands
                double c = Math.cos(a), sn = Math.sin(a);
                root.addOrReplaceChild("ripple" + i, ring(1.6F), at(c * JET_X1, SURFACE, sn * JET_X1));
            }
            // the middle bowl and its four falling streams
            k.k.disc(GREY, 0.0F, -15.5F, 0.0F, 4.0F, 0.9F);
            k.k.disc(GREY, 0.0F, -16.3F, 0.0F, 6.0F, 0.8F);
            k.k.ringY(WHITE, 0.0F, -17.8F, 0.0F, 6.6F, 1.6F, 1.1F, 12, 0);
            k.k.ringY(BRASS, 0.0F, -18.05F, 0.0F, 6.75F, 0.25F, 0.3F, 12, 0);
            k.k.disc(POOL, 0.0F, -16.95F, 0.0F, 5.7F, 0.4F);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2, c = Math.cos(a), sn = Math.sin(a);
                k.line(POOL, c * 5.6, -17.0, sn * 5.6, c * 7.0, -16.6, sn * 7.0, 0.8);
                k.box(POOL, 20 + i * 4, 0, c * 7.1 - 0.35, -16.6, sn * 7.1 - 0.35, 0.7, 10.2, 0.7);
                CubeListBuilder drop = cubes();
                bx(drop, POOL, -0.5, -0.6, -0.5, 1.0, 1.2, 1.0);
                root.addOrReplaceChild("drop" + i, drop, at(c * 7.1, -16.0, sn * 7.1));
                root.addOrReplaceChild("splash" + i, ring(0.9F), at(c * 7.1, SURFACE, sn * 7.1));
            }
            // the top tier: a shaft, a small bowl spilling into the middle one
            k.box(WHITE, 30, 0, -1.2, -21.4, -1.2, 2.4, 3.8, 2.4);
            k.box(BRASS, -1.4, -19.6, -1.4, 2.8, 0.5, 2.8);
            k.k.disc(GREY, 0.0F, -21.9F, 0.0F, 2.4F, 0.6F);
            k.k.ringY(WHITE, 0.0F, -22.9F, 0.0F, 2.8F, 1.0F, 0.7F, 8, 0);
            k.k.disc(POOL, 0.0F, -22.3F, 0.0F, 2.3F, 0.35F);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + i * Math.PI / 2, c = Math.cos(a), sn = Math.sin(a);
                k.box(POOL, 40 + i * 3, 0, c * 3.0 - 0.25, -22.4, sn * 3.0 - 0.25, 0.5, 5.4, 0.5);
                CubeListBuilder drop = cubes();
                bx(drop, POOL, -0.35, -0.45, -0.35, 0.7, 0.9, 0.7);
                root.addOrReplaceChild("drop" + (4 + i), drop, at(c * 3.0, -21.6, sn * 3.0));
            }
            // the crown, and the blood moon in it
            k.box(BRASS, -1.0, -23.6, -1.0, 2.0, 0.7, 2.0);
            for (int p = 0; p < 4; p++) {
                double a = p * Math.PI / 2 + Math.PI / 4, c = Math.cos(a), sn = Math.sin(a);
                k.line(BRASS, c * 0.7, -23.6, sn * 0.7, c * 2.0, -25.8, sn * 2.0, 0.4);
                k.line(BRASS, c * 2.0, -25.8, sn * 2.0, c * 1.6, -27.6, sn * 1.6, 0.3);
            }
            CubeListBuilder moon = cubes();
            bx(moon, WARM, -1.6, -1.0, -1.0, 3.2, 2.0, 2.0);
            BoatKit.box(moon, WARM, 6, 2, -1.0F, -1.6F, -1.0F, 2.0F, 3.2F, 2.0F);
            BoatKit.box(moon, WARM, 12, 4, -1.0F, -1.0F, -1.6F, 2.0F, 2.0F, 3.2F);
            BoatKit.box(moon, WARM, 18, 6, -1.3F, -1.3F, -1.3F, 2.6F, 2.6F, 2.6F);
            root.addOrReplaceChild("moon", moon, at(0, -26.2, 0));
            // bubbles on the surface
            for (int i = 0; i < BUBBLES; i++) {
                CubeListBuilder bubble = cubes();
                bx(bubble, POOL, -0.7, -0.7, -0.7, 1.4, 1.4, 1.4);
                double a = i * 2.39996, r = 4.0 + (i % 5) * 2.0;
                root.addOrReplaceChild("bubble" + i, bubble, at(Math.cos(a) * r, SURFACE - 0.05, Math.sin(a) * r));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Blood Moon Banner (3 blocks tall) ═════════════
    // A tall black iron pole on a cross foot with gold ball feet and gold rings, a gold crescent with a ruby at its top. An
    // arm reaches forward from it to an iron rod with gold finials, and from the rod hangs the banner, in front of the
    // pole: tattered crimson, swallow-tailed and frayed, a gold border and the Blood Moon crest (a great glowing red moon,
    // bats across it, blood dripping from it). Gold-tasselled cords hang from the rod's ends, clear of the cloth.
    // The cloth is one sheet (each face painted once, tools/BloodDecorTex.cs), cut into bands across that hang one from
    // the next, so it ripples without any two pieces overlapping.
    public static class Banner extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_moon_banner");
        // the banner picture: its back at (BANNER_U, BANNER_V), its front 16 px to the right, both 16 x 34
        static final int BANNER_U = 0, BANNER_V = 180;
        static final int BANDS = 17;
        static final float BAND = 2.0F;
        static final float TOP = -43.4F;
        // the pole, behind the cloth
        static final float POLE_X = -3.0F;
        private final List<ModelPart> bands = new ArrayList<>();

        public Banner(ModelPart root) {
            super(root);
            ModelPart p = this.decor.getChild("cloth");
            for (int i = 0; i < BANDS; i++) {
                p = p.getChild("b" + i);
                this.bands.add(p);
            }
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            // a ripple running down it; it leans forward a hair, away from the pole
            for (int i = 0; i < BANDS; i++) {
                this.bands.get(i).zRot = -0.004F - 0.009F * Mth.sin(t * 0.05F - i * 0.35F) - 0.003F * Mth.sin(t * 0.13F - i * 0.8F);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the cross foot, gold ball feet, the hub
            k.box(ENGINE, 0, 0, POLE_X - 5.0, -1.2, -1.0, 10.0, 1.2, 2.0);
            k.box(ENGINE, 0, 6, POLE_X - 1.0, -1.25, -5.0, 2.0, 1.25, 10.0);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2;
                k.cube(BRASS, POLE_X + Math.cos(a) * 5.2, -0.7, Math.sin(a) * 5.2, 1.5);
            }
            k.box(ENGINE, 12, 12, POLE_X - 1.8, -3.0, -1.8, 3.6, 1.8, 3.6);
            k.box(GOLD, POLE_X - 1.95, -3.35, -1.95, 3.9, 0.4, 3.9);
            // the pole in two lengths, gold rings
            k.box(ENGINE, 24, 0, POLE_X - 0.7, -24.5, -0.7, 1.4, 21.5, 1.4);
            k.box(ENGINE, 30, 0, POLE_X - 0.65, -46.0, -0.65, 1.3, 21.6, 1.3);
            for (double y : new double[]{-12.0, -24.8, -36.0}) k.box(BRASS, POLE_X - 1.05, y, -1.05, 2.1, 0.8, 2.1);
            // the top: a gold knop, the crescent, a spike, the ruby
            k.box(BRASS, POLE_X - 1.0, -47.0, -1.0, 2.0, 1.0, 2.0);
            k.line(BRASS, POLE_X, -47.0, 0, POLE_X, -53.6, 0, 0.6);
            crescent(k, BRASS, POLE_X, -50.4, 2.6, 0.9);
            k.cube(NAV_RED, POLE_X, -49.6, 0, 1.1);
            // the arm forward to the rod, a gold collar on the pole, a curl of iron under the arm
            k.box(ENGINE, 36, 20, POLE_X + 0.6, TOP - 1.3, -0.55, -POLE_X - 0.25, 1.1, 1.1);
            k.box(BRASS, POLE_X - 0.95, TOP - 1.6, -0.95, 1.9, 1.7, 1.9);
            k.line(ENGINE, POLE_X + 0.6, TOP + 1.8, 0, -0.9, TOP - 0.5, 0, 0.5);
            // the rod, just above the cloth (it doesn't touch it), its finials, the cords and tassels clear of the cloth
            for (int s = -1; s <= 1; s += 2) {
                k.box(ENGINE, 0, 20, -0.5, TOP - 1.15, s > 0 ? 0 : -10.0, 1.0, 1.0, 10.0);
                k.cube(BRASS, 0, TOP - 0.65, s * 10.5, 1.7);
                k.line(BRASS, 0, TOP - 0.65, s * 11.2, 0, TOP - 0.65, s * 12.8, 0.6);
                k.line(ROPE, 0, TOP + 0.2, s * 10.5, 0, TOP + 6.5, s * 10.5, 0.35);
                k.box(BRASS, -0.45, TOP + 6.4, s * 10.5 - 0.45, 0.9, 1.0, 0.9);
                for (int f = 0; f < 4; f++) {
                    k.line(GOLD, (f % 2) * 0.3 - 0.15, TOP + 7.3, s * 10.5 + (f - 1.5) * 0.22, (f % 2) * 0.5 - 0.25, TOP + 9.2, s * 10.5 + (f - 1.5) * 0.32, 0.16);
                }
            }
            // the banner: bands across, each hanging from the one above
            PartDefinition p = root.addOrReplaceChild("cloth", cubes(), at(0, TOP, 0));
            for (int i = 0; i < BANDS; i++) {
                CubeListBuilder band = cubes();
                sheet(band, BANNER_U, (int) (BANNER_V + i * BAND) - 16, 0.0F, 0.0F, -8.0F, 0.0F, BAND, 16.0F);
                p = p.addOrReplaceChild("b" + i, band, i == 0 ? PartPose.ZERO : at(0, BAND, 0));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Crimson Rug (3 x 2 blocks, flat) ═════════════
    // A rich crimson rug lying flat: a gold border with a running pattern, black inner borders, the Blood Moon in a
    // medallion at its heart with bats round it, crescent moons in its corners; gold tassels along its short ends.
    public static class Rug extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_rug");
        // its picture: the top of a 48 x 32 box at (RUG_U, RUG_V) (top face at RUG_U + 32)
        static final int RUG_U = 0, RUG_V = 180;

        public Rug(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            CubeListBuilder rug = cubes();
            rug.texOffs(RUG_U, RUG_V).addBox(-24.0F, -0.3F, -16.0F, 48.0F, 0.3F, 32.0F);
            root.addOrReplaceChild("rug", rug, PartPose.ZERO);
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 15; i++) {
                    double z = -14.0 + i * 2.0;
                    k.box(ROPE, (i * 3) % 12, 0, s > 0 ? 24.0 : -25.5, -0.18, z - 0.3, 1.5, 0.18, 0.6);
                    k.cube(GOLD, s * 24.15, -0.2, z, 0.5);
                }
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ Revenant's Greatsword in Stone (about 3 blocks tall) ═════════════
    // The Crimson Revenant's huge greatsword driven point-first into a cracked stone plinth: blackened steel with bright
    // bevelled edges and a red-hot line glowing down both faces of the blade, glowing runes near the guard, a heavy gold
    // crossguard with upswept tips and a ruby, a leather grip bound in gold, a gold pommel with a ruby. Red-hot cracks run
    // out from the blade across the plinth and down its sides; rubble lies round its foot.
    public static class Greatsword extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("revenant_greatsword_statue");

        public Greatsword(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the plinth: a broad foot, the block, a cracked cap
            k.box(MOSS, 0, 0, -7.0, -2.6, -7.0, 14.0, 2.6, 14.0);
            k.box(GREY, 0, 0, -5.6, -9.0, -5.6, 11.2, 6.4, 11.2);
            k.box(MOSS, 4, 4, -6.3, -10.6, -6.3, 12.6, 1.6, 12.6);
            k.box(WHITE, 0, 0, -6.0, -3.4, -6.0, 12.0, 0.8, 12.0);
            // a chunk broken from the cap's corner, rubble round the foot
            k.part(cubesOf(GREY, -1.4, -1.0, -1.2, 2.8, 2.0, 2.4), 8.4, -0.9, 6.2, 0.3, 0.5, 0.2);
            k.part(cubesOf(GREY, -1.0, -0.8, -1.0, 2.0, 1.6, 2.0), -8.6, -0.7, -5.0, -0.2, 0.9, 0.4);
            k.part(cubesOf(GREY, -0.7, -0.6, -0.7, 1.4, 1.2, 1.4), 7.8, -0.5, -7.4, 0.5, 0.2, -0.3);
            k.part(cubesOf(MOSS, -1.6, -0.7, -1.0, 3.2, 1.4, 2.0), -7.6, -0.6, 7.2, 0.0, -0.6, 0.15);
            k.part(cubesOf(GREY, -0.5, -0.5, -0.5, 1.0, 1.0, 1.0), 9.2, -0.4, -2.0, 0.6, 0.6, 0.0);
            // red-hot cracks: across the cap from the blade, down the block's faces
            double[][] top = {{4.0, 2.6, 6.2, 4.4}, {-3.6, 2.2, -6.2, 3.2}, {2.2, -4.0, 3.6, -6.2}, {-2.6, -3.0, -5.0, -6.2}, {5.0, -0.6, 6.25, -1.4}};
            for (double[] c : top) {
                k.line(FIRE, Math.signum(c[0]) * 0.8, -10.62, c[1] * 0.25, c[0], -10.62, c[1], 0.35);
                k.line(FIRE, c[0], -10.62, c[1], c[2], -10.62, c[3], 0.28);
            }
            k.line(FIRE, 5.65, -9.0, 1.2, 5.65, -6.4, 2.6, 0.3);
            k.line(FIRE, 5.65, -6.4, 2.6, 5.65, -4.0, 1.4, 0.26);
            k.line(FIRE, 5.65, -6.4, 2.6, 5.65, -5.2, 4.2, 0.22);
            k.line(FIRE, -5.65, -9.0, -2.0, -5.65, -5.6, -3.4, 0.3);
            k.line(FIRE, 1.4, -9.0, 5.65, 2.8, -5.2, 5.65, 0.28);
            k.line(FIRE, -2.0, -9.0, -5.65, -3.0, -6.0, -5.65, 0.28);
            k.line(FIRE, -3.0, -6.0, -5.65, -1.6, -3.6, -5.65, 0.24);
            k.box(BLACK, -1.0, -10.75, -2.6, 2.0, 0.2, 5.2);
            // an iron chain slung across the plinth's front, links turned in turn, a padlock
            for (int i = 0; i < 11; i++) {
                double z = -5.0 + i * 1.0, y = -7.4 + 1.6 * Math.sin(i / 10.0 * Math.PI);
                if (i % 2 == 0) k.box(ENGINE, 0, 30, 5.6, y - 0.3, z - 0.55, 0.3, 0.6, 1.1);
                else k.box(ENGINE, 2, 30, 5.55, y - 0.55, z - 0.15, 0.4, 1.1, 0.3);
            }
            k.box(BRASS, 5.55, -6.6, -0.6, 0.6, 1.4, 1.2);
            k.line(BRASS, 5.85, -6.6, -0.35, 5.85, -7.3, 0.35, 0.22);
            // the blade: blackened steel, bright edges, the red-hot line down both faces, runes by the guard
            k.box(STEEL, 0, 0, -0.6, -31.4, -2.2, 1.2, 21.0, 4.4);
            k.box(STEEL, 14, 0, -0.75, -32.0, -2.65, 1.5, 2.6, 5.3);
            k.box(CHROME, 0, 0, -0.4, -31.3, 2.2, 0.8, 20.8, 0.45);
            k.box(CHROME, 4, 0, -0.4, -31.3, -2.65, 0.8, 20.8, 0.45);
            for (int s = -1; s <= 1; s += 2) {
                double x = s > 0 ? 0.6 : -0.75;
                k.box(ICE, s > 0 ? 0 : 8, 0, x, -30.2, -0.42, 0.15, 19.4, 0.84);
                for (int r = 0; r < 3; r++) k.box(ICE, 20 + r * 4, 6, x, -29.2 + r * 1.6, s * (1.0 + (r % 2) * 0.3) - 0.2, 0.15, 0.7, 0.4);
            }
            // the crossguard: heavy gold, upswept tips, black spikes, a ruby each side
            k.box(BRASS, 0, 0, -1.4, -33.6, -7.0, 2.8, 1.8, 14.0);
            for (int s = -1; s <= 1; s += 2) {
                k.line(BRASS, 0, -32.8, s * 6.6, 0, -35.2, s * 9.0, 1.5);
                k.line(BRASS, 0, -35.2, s * 9.0, 0, -36.8, s * 9.2, 0.9);
                k.line(ENGINE, 0, -32.2, s * 4.0, 0, -30.4, s * 4.8, 0.6);
                k.box(NAV_RED, s > 0 ? 1.8 : -2.0, -33.6, -0.8, 0.2, 1.6, 1.6);
            }
            k.box(BRASS, 20, 0, -1.8, -34.6, -2.3, 3.6, 3.4, 4.6);
            k.box(GOLD, -1.9, -31.6, -1.7, 3.8, 0.5, 3.4);
            // the grip and the pommel
            k.box(LEATHER, 0, 0, -0.9, -43.2, -0.9, 1.8, 8.8, 1.8);
            k.box(BRASS, 30, 0, -1.15, -37.0, -1.15, 2.3, 0.6, 2.3);
            k.box(BRASS, 30, 4, -1.15, -40.2, -1.15, 2.3, 0.6, 2.3);
            k.box(BRASS, 4, 20, -1.6, -45.8, -1.6, 3.2, 2.7, 3.2);
            k.box(BRASS, 16, 20, -1.2, -46.4, -1.2, 2.4, 0.6, 2.4);
            k.box(BRASS, -0.5, -47.6, -0.5, 1.0, 1.2, 1.0);
            k.box(NAV_RED, 1.6, -44.9, -0.7, 0.2, 1.4, 1.4);
            k.box(NAV_RED, -1.8, -44.9, -0.7, 0.2, 1.4, 1.4);
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    static CubeListBuilder cubesOf(Zone z, double x, double y, double zz, double w, double h, double d) {
        CubeListBuilder c = cubes();
        bx(c, z, x, y, zz, w, h, d);
        return c;
    }

    // ═════════════ the Gorefang Skull Trophy (on a wall) ═════════════
    // Gorefang's huge bone skull mounted on a dark wooden shield plaque edged in gold with gold studs: a long wolf's
    // muzzle full of fangs, its jaw a little open, a heavy brow, ram's horns curling back round each side, and red fire
    // burning in its eye holes. Its back (-x) is against the wall.
    public static class SkullTrophy extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("gorefang_skull_trophy");

        public SkullTrophy(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the shield plaque: dark wood, an arched top, a pointed foot, gold edging and studs
            k.box(DARKWOOD, 0, 0, -8.0, -14.6, -6.6, 1.6, 10.2, 13.2);
            k.box(DARKWOOD, 0, 16, -8.0, -15.4, -5.2, 1.6, 0.8, 10.4);
            k.box(DARKWOOD, 30, 0, -8.0, -4.4, -5.4, 1.6, 1.4, 10.8);
            k.box(DARKWOOD, 30, 4, -8.0, -3.0, -3.7, 1.6, 1.3, 7.4);
            k.box(DARKWOOD, 30, 8, -8.0, -1.7, -1.9, 1.6, 1.0, 3.8);
            k.box(OLDWOOD, 0, 0, -6.6, -13.6, -5.4, 0.4, 8.6, 10.8);
            for (int s = -1; s <= 1; s += 2) {
                k.box(BRASS, s > 0 ? 0 : 4, 0, -6.45, -14.4, s * 6.3 - 0.25, 0.35, 10.0, 0.5);
                k.line(BRASS, -6.3, -4.5, s * 6.3, -6.3, -0.85, 0, 0.5);
                k.line(BRASS, -6.3, -14.4, s * 6.3, -6.3, -15.3, s * 5.0, 0.5);
                k.cube(GOLD, -6.3, -13.6, s * 5.6, 0.7);
                k.cube(GOLD, -6.3, -5.6, s * 5.6, 0.7);
            }
            k.box(BRASS, 8, 0, -6.45, -15.6, -5.0, 0.35, 0.5, 10.0);
            k.cube(GOLD, -6.3, -1.8, 0, 0.7);
            // a gold nameplate under the skull, blood on its fangs
            k.box(BRASS, -6.45, -3.6, -2.2, 0.3, 1.0, 4.4);
            k.box(BLACK, -6.18, -3.3, -1.6, 0.05, 0.15, 3.2);
            k.box(RED, 5.15, -5.6, 1.15, 0.4, 0.5, 0.45);
            k.box(RED, 5.15, -5.6, -1.6, 0.4, 0.5, 0.45);
            // the cranium, brow, cheekbones
            k.box(BONE, 0, 0, -6.4, -13.0, -4.2, 5.4, 6.2, 8.4);
            k.box(BONE, 0, 16, -2.4, -13.4, -4.5, 2.6, 1.7, 9.0);
            k.box(BONE, 12, 18, -3.8, -11.0, -4.9, 2.6, 2.2, 0.8);
            k.box(BONE, 12, 22, -3.8, -11.0, 4.1, 2.6, 2.2, 0.8);
            k.line(BONE, -5.0, -13.4, 0, -1.0, -13.6, 0, 0.9);   // a crest down the skull
            // the muzzle, tapering, the nose
            k.box(BONE, 0, 0, -1.2, -11.2, -2.1, 6.4, 3.4, 4.2);
            k.box(BONE, 20, 0, 5.1, -10.8, -1.7, 1.9, 2.8, 3.4);
            k.box(BLACK, 6.95, -10.5, -0.9, 0.15, 1.2, 1.8);
            k.box(BLACK, 5.6, -11.25, -0.6, 1.2, 0.1, 1.2);
            // eye holes, fire burning in them, a scar across one
            for (int s = -1; s <= 1; s += 2) {
                k.box(BLACK, -1.1, -12.4, s > 0 ? 2.15 : -3.95, 0.12, 1.9, 1.8);
                k.box(NAV_RED, -1.0, -12.1, s > 0 ? 2.45 : -3.65, 0.14, 1.2, 1.2);
            }
            k.line(BLACK, -1.0, -13.4, -3.2, -0.8, -10.6, -1.8, 0.18);
            // upper fangs: long canines, a row of teeth behind
            for (int s = -1; s <= 1; s += 2) {
                k.line(BONE, 5.6, -7.9, s * 1.55, 5.4, -5.3, s * 1.45, 0.65);
                k.line(BONE, 5.4, -5.3, s * 1.45, 5.2, -4.6, s * 1.35, 0.35);
                for (int t = 0; t < 4; t++) k.box(BONE, 2 * t, 26, 0.8 + t * 1.1, -8.0, s * 1.85 - 0.25, 0.5, 0.7, 0.5);
            }
            // the jaw, a little open, its own fangs, the dark of the mouth
            k.box(BLACK, -0.8, -8.0, -1.6, 5.4, 0.8, 3.2);
            PartDefinition jaw = root.addOrReplaceChild("jaw", cubes(), at(-1.4, -7.7, 0, 0, 0, 0.22));
            K j = new K(jaw);
            j.box(BONE, 0, 0, 0.0, 0.0, -1.9, 7.0, 1.4, 3.8);
            j.box(BONE, 0, 8, -1.5, -0.3, -2.3, 2.0, 1.8, 4.6);
            for (int s = -1; s <= 1; s += 2) {
                j.line(BONE, 6.2, 0.1, s * 1.35, 6.0, -1.9, s * 1.25, 0.55);
                for (int t = 0; t < 3; t++) j.box(BONE, 2 * t, 20, 1.6 + t * 1.3, -0.5, s * 1.55 - 0.2, 0.45, 0.55, 0.4);
            }
            j.done();
            // ram's horns curling back, down and round each side
            for (int s = -1; s <= 1; s += 2) {
                int n = 16;
                double px = 0, py = 0, pz = 0;
                for (int i = 0; i <= n; i++) {
                    double f = i / (double) n, th = f * 1.8 * Math.PI, r = 4.3 * (1 - 0.5 * f);
                    double x = -2.8 - r * Math.sin(th), y = -9.2 - r * Math.cos(th), z = s * (3.6 + 3.2 * f);
                    if (i > 0) k.line(SCALE, px, py, pz, x, y, z, 2.6 - 1.6 * f);
                    px = x;
                    py = y;
                    pz = z;
                }
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Sanguine Witch's Crystal Staff (about 1.5 blocks) ═════════════
    // The witch's bone staff, ringed like a spine and wound with a crimson ribbon, standing in a gold cup on an ornate
    // stone pedestal (corner colonnettes with gold caps, a gold band set with rubies, a carved capital). Bone prongs at its
    // head cradle a glowing blood crystal that slowly turns and bobs; three small shards circle it.
    public static class StaffPedestal extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("sanguine_staff_pedestal");
        static final float CRYSTAL_Y = -23.2F;
        private final ModelPart crystal;
        private final ModelPart orbit;

        public StaffPedestal(ModelPart root) {
            super(root);
            this.crystal = this.decor.getChild("crystal");
            this.orbit = this.decor.getChild("orbit");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            this.crystal.yRot = t * 0.03F;
            this.crystal.y = CRYSTAL_Y + 0.45F * Mth.sin(t * 0.06F);
            this.orbit.yRot = -t * 0.045F;
            this.orbit.y = CRYSTAL_Y + 0.3F * Mth.sin(t * 0.05F + 1.0F);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the pedestal: a stepped foot, the shaft, colonnettes, a gold band with rubies, the capital
            k.box(GREY, 0, 0, -5.2, -1.6, -5.2, 10.4, 1.6, 10.4);
            k.box(WHITE, 0, 0, -4.4, -2.6, -4.4, 8.8, 1.0, 8.8);
            k.box(GOLD, -4.55, -1.75, -4.55, 9.1, 0.25, 9.1);
            k.box(GREY, 0, 16, -2.7, -8.4, -2.7, 5.4, 5.8, 5.4);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    k.box(WHITE, 24, 0, sx * 2.95 - 0.5, -8.2, sz * 2.95 - 0.5, 1.0, 5.6, 1.0);
                    k.box(BRASS, sx * 2.95 - 0.7, -8.6, sz * 2.95 - 0.7, 1.4, 0.5, 1.4);
                    k.box(BRASS, sx * 2.95 - 0.7, -3.0, sz * 2.95 - 0.7, 1.4, 0.4, 1.4);
                }
            }
            k.box(BRASS, -2.85, -5.9, -2.85, 5.7, 0.55, 5.7);
            for (int s = 0; s < 4; s++) {
                double a = s * Math.PI / 2, c = Math.cos(a), sn = Math.sin(a);
                k.cube(NAV_RED, c * 2.9, -5.62, sn * 2.9, 0.7);
                // a carved gothic arch on each face of the shaft
                k.line(BLACK, c * 2.73 - sn * 1.0, -4.0, sn * 2.73 + c * 1.0, c * 2.73, -7.2, sn * 2.73, 0.12);
                k.line(BLACK, c * 2.73 + sn * 1.0, -4.0, sn * 2.73 - c * 1.0, c * 2.73, -7.2, sn * 2.73, 0.12);
            }
            k.box(WHITE, 4, 14, -3.8, -9.6, -3.8, 7.6, 1.2, 7.6);
            k.box(GREY, 4, 20, -3.2, -10.4, -3.2, 6.4, 0.8, 6.4);
            k.box(GOLD, -3.95, -9.8, -3.95, 7.9, 0.3, 7.9);
            k.box(BRASS, 10, 10, -1.3, -11.4, -1.3, 2.6, 1.0, 2.6);
            k.box(GOLD, -1.5, -11.7, -1.5, 3.0, 0.35, 3.0);
            // the staff: bone, ringed like a spine, a crimson ribbon wound round it and trailing
            k.box(BONE, 0, 0, -0.62, -20.0, -0.62, 1.24, 8.6, 1.24);
            for (int i = 0; i < 4; i++) k.box(BONE, 12, 2 * i, -0.9, -12.6 - i * 2.0, -0.9, 1.8, 0.6, 1.8);
            double px = 0, py = 0, pz = 0;
            for (int i = 0; i <= 10; i++) {
                double a = i * 0.95, y = -14.2 - i * 0.42;
                double x = Math.cos(a) * 0.85, z = Math.sin(a) * 0.85;
                if (i > 0) k.line(CANVAS, px, py, pz, x, y, z, 0.38);
                px = x;
                py = y;
                pz = z;
            }
            k.line(CANVAS, 0.8, -14.2, 0.3, 1.5, -12.6, 1.4, 0.36);
            k.line(CANVAS, 1.5, -12.6, 1.4, 1.3, -11.0, 2.3, 0.34);
            k.line(CANVAS, 0.6, -14.3, -0.5, 0.9, -12.8, -1.6, 0.32);
            // the head: a bone collar, a gold ring, four prongs cradling the crystal
            k.box(BONE, 20, 0, -1.15, -20.8, -1.15, 2.3, 1.0, 2.3);
            k.box(GOLD, -1.3, -20.3, -1.3, 2.6, 0.3, 2.6);
            for (int p = 0; p < 4; p++) {
                double a = Math.PI / 4 + p * Math.PI / 2, c = Math.cos(a), sn = Math.sin(a);
                k.line(BONE, c * 0.7, -20.6, sn * 0.7, c * 2.5, -22.4, sn * 2.5, 0.5);
                k.line(BONE, c * 2.5, -22.4, sn * 2.5, c * 2.1, -25.0, sn * 2.1, 0.42);
                k.line(BONE, c * 2.1, -25.0, sn * 2.1, c * 1.0, -26.4, sn * 1.0, 0.32);
            }
            // the blood crystal: a long double point (two boxes crossed), turning
            CubeListBuilder crystal = cubes();
            bx(crystal, ICE, -0.95, -2.3, -0.95, 1.9, 4.6, 1.9);
            BoatKit.box(crystal, ICE, 10, 0, -0.62F, -3.5F, -0.62F, 1.24F, 1.3F, 1.24F);
            BoatKit.box(crystal, ICE, 16, 0, -0.3F, -4.3F, -0.3F, 0.6F, 0.9F, 0.6F);
            BoatKit.box(crystal, ICE, 20, 0, -0.62F, 2.2F, -0.62F, 1.24F, 1.1F, 1.24F);
            BoatKit.box(crystal, ICE, 26, 0, -0.3F, 3.2F, -0.3F, 0.6F, 0.7F, 0.6F);
            PartDefinition cr = root.addOrReplaceChild("crystal", crystal, at(0, CRYSTAL_Y, 0));
            CubeListBuilder twist = cubes();
            BoatKit.box(twist, ICE, 30, 4, -0.85F, -2.0F, -0.85F, 1.7F, 4.0F, 1.7F);
            cr.addOrReplaceChild("twist", twist, at(0, 0, 0, 0, Math.PI / 4, 0));
            // three little shards circling it
            PartDefinition orbit = root.addOrReplaceChild("orbit", cubes(), at(0, CRYSTAL_Y, 0));
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3;
                CubeListBuilder shard = cubes();
                bx(shard, ICE, -0.35, -0.7, -0.35, 0.7, 1.4, 0.7);
                BoatKit.box(shard, ICE, 40, 0, -0.18F, -1.15F, -0.18F, 0.36F, 0.45F, 0.36F);
                orbit.addOrReplaceChild("shard" + i, shard, at(Math.cos(a) * 3.6, (i - 1) * 1.0, Math.sin(a) * 3.6, 0.3, a, 0.2));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Bat Gargoyle ═════════════
    // A weathered stone gargoyle crouched on a mossy, cracked plinth (a carved gothic arch on its front, a chipped corner,
    // rubble): clawed feet and hands hooked over its front edge, a hunched ribbed body leaning out, a ridge of spines down
    // its back, bat wings half spread (carved membranes with raised bone ribs and a hooked thumb claw), tall pointed ears,
    // curled ram-like horns, a snarling pug face (wrinkled snout, fangs top and bottom), red eyes glowing under a heavy
    // brow, a long tail curling down the plinth's back to a spade tip.
    public static class Gargoyle extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("bat_gargoyle");
        static final int WING_U = 0, WING_V = 180;
        static final float WING_W = 14.0F, WING_H = 13.0F;

        public Gargoyle(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the plinth: mouldings, a carved arch on its front, cracks, moss, a chipped corner, rubble
            k.box(GREY, 0, 0, -5.8, -5.0, -5.8, 11.6, 4.2, 11.6);
            k.box(WHITE, 0, 0, -6.5, -1.0, -6.5, 13.0, 1.0, 13.0);
            k.box(WHITE, 0, 16, -6.5, -6.0, -6.5, 13.0, 1.0, 13.0);
            k.box(GREY, 20, 20, 5.85, -4.6, -2.6, 0.3, 3.4, 5.2);
            k.line(BLACK, 6.17, -1.4, -2.0, 6.17, -3.6, -2.0, 0.14);
            k.line(BLACK, 6.17, -1.4, 2.0, 6.17, -3.6, 2.0, 0.14);
            k.line(BLACK, 6.17, -3.6, -2.0, 6.17, -4.5, 0.0, 0.14);
            k.line(BLACK, 6.17, -3.6, 2.0, 6.17, -4.5, 0.0, 0.14);
            k.cube(NAV_RED, 6.2, -2.6, 0, 0.5);
            k.line(BLACK, 5.86, -4.9, 3.6, 5.86, -3.2, 4.4, 0.12);
            k.line(BLACK, 5.86, -3.2, 4.4, 5.86, -1.2, 3.9, 0.12);
            k.line(BLACK, -2.0, -4.9, 5.86, -3.4, -2.4, 5.86, 0.12);
            k.line(BLACK, -5.86, -4.6, -1.0, -5.86, -2.0, 0.6, 0.12);
            k.box(MOSS, -6.6, -6.3, 2.0, 4.0, 0.3, 4.6);
            k.box(MOSS, 0, 8, 5.86, -4.6, -5.0, 0.28, 2.6, 2.0);
            k.box(MOSS, 0, 12, -6.6, -1.25, -6.6, 5.0, 0.3, 3.0);
            k.box(MOSS, 0, 16, 2.0, -1.25, 6.2, 4.0, 0.3, 0.5);
            k.box(MOSS, 0, 20, -5.86, -5.0, 3.0, 0.3, 1.6, 2.4);
            k.part(cubesOf(GREY, -0.9, -0.8, -0.8, 1.8, 1.6, 1.6), 7.6, -0.8, -5.6, 0.4, 0.3, 0.2);
            k.part(cubesOf(GREY, -0.6, -0.5, -0.6, 1.2, 1.0, 1.2), -7.4, -0.5, 6.4, 0.2, 0.8, 0.5);
            k.part(cubesOf(WHITE, -0.5, -0.4, -0.5, 1.0, 0.8, 1.0), 7.2, -0.4, 4.8, 0.6, 0.1, 0.3);
            // haunches, feet with three hooked claws each over the front edge
            for (int s = -1; s <= 1; s += 2) {
                k.box(WHITE, 30, 0, -4.4, -10.6, s > 0 ? 1.6 : -4.6, 5.0, 4.6, 3.0);
                k.box(WHITE, 30, 8, -0.4, -9.4, s > 0 ? 2.2 : -4.0, 2.2, 3.0, 1.8);
                k.box(WHITE, 30, 14, 0.0, -7.6, s > 0 ? 2.0 : -4.4, 4.0, 1.6, 2.4);
                for (int c = 0; c < 3; c++) {
                    double z = s * (2.45 + c * 0.75);
                    k.line(WHITE, 3.8, -6.9, z, 6.0, -6.6, z, 0.6);
                    k.line(BONE, 6.0, -6.6, z, 6.75, -5.9, z, 0.45);
                    k.line(BONE, 6.75, -5.9, z, 6.7, -5.0, z, 0.3);
                }
            }
            // the body: hunched, ribbed, leaning out over the edge, spines down its back
            PartDefinition body = root.addOrReplaceChild("body", cubes(), at(-1.0, -8.0, 0, 0, 0, 0.38));
            K b = new K(body);
            b.box(WHITE, 0, 0, -3.0, -8.0, -2.7, 5.4, 8.6, 5.4);
            b.box(WHITE, 24, 16, 0.6, -7.0, -2.0, 2.4, 5.4, 4.0);
            b.line(BLACK, 3.02, -6.0, -0.9, 3.02, -1.8, -0.7, 0.12);
            for (int r = 0; r < 4; r++) b.box(GREY, 3.0, -6.6 + r * 1.3, -1.7, 0.22, 0.32, 3.4);
            for (int sp = 0; sp < 5; sp++) {
                double y = -7.6 + sp * 1.7;
                b.line(GREY, -3.0, y, 0, -4.3 + sp * 0.12, y - 0.9, 0, 0.55 - sp * 0.05);
            }
            b.done();
            // arms down to hands hooked over the edge
            for (int s = -1; s <= 1; s += 2) {
                k.line(WHITE, 1.4, -14.2, s * 3.0, 3.4, -10.6, s * 2.4, 1.6);
                k.line(WHITE, 3.4, -10.6, s * 2.4, 4.4, -6.9, s * 1.2, 1.4);
                k.box(WHITE, 40, 0, 3.6, -7.4, s > 0 ? 0.3 : -2.0, 2.4, 1.2, 1.7);
                for (int c = 0; c < 3; c++) {
                    double z = s * (0.5 + c * 0.6);
                    k.line(BONE, 5.8, -6.6, z, 6.75, -5.7, z, 0.38);
                    k.line(BONE, 6.75, -5.7, z, 6.7, -4.9, z, 0.26);
                }
            }
            // the head: snarling pug face, wrinkled snout, glowing eyes under a heavy brow, fangs, tall ears, curled horns
            PartDefinition head = root.addOrReplaceChild("head", cubes(), at(3.0, -15.0, 0));
            K h = new K(head);
            h.box(WHITE, 0, 0, -2.4, -4.4, -2.5, 4.6, 4.4, 5.0);
            h.box(WHITE, 20, 0, 2.2, -2.5, -1.5, 1.3, 1.7, 3.0);
            h.box(GREY, 3.5, -2.45, -1.0, 0.12, 0.25, 2.0);
            h.box(GREY, 3.5, -1.95, -1.2, 0.12, 0.2, 2.4);
            h.box(BLACK, 3.55, -2.2, -0.8, 0.08, 0.5, 0.5).box(BLACK, 3.55, -2.2, 0.3, 0.08, 0.5, 0.5);
            h.box(BLACK, 2.2, -0.75, -1.3, 1.25, 0.6, 2.6);
            h.box(WHITE, 28, 0, 1.0, -0.4, -1.5, 2.4, 0.9, 3.0);
            h.box(NAV_RED, 2.25, -0.65, -0.9, 0.1, 0.35, 1.8);
            for (int s = -1; s <= 1; s += 2) {
                h.box(BLACK, 2.21, -3.45, s > 0 ? 0.3 : -1.6, 0.06, 1.0, 1.3);
                h.box(NAV_RED, 2.25, -3.3, s > 0 ? 0.45 : -1.45, 0.12, 0.75, 1.0);
                h.line(WHITE, 2.4, -3.45, s * 0.15, 2.35, -4.35, s * 2.3, 0.6);
                h.line(BONE, 3.0, -0.8, s * 0.95, 3.05, 0.45, s * 0.9, 0.42);
                h.line(BONE, 3.1, -0.2, s * 0.55, 3.1, -1.0, s * 0.55, 0.3);
                h.line(BONE, 2.7, -0.3, s * 1.25, 2.7, -0.95, s * 1.25, 0.26);
                h.line(GREY, 3.2, -2.9, s * 0.6, 3.5, -2.6, s * 1.1, 0.14);
                // ears: tall and pointed, dark inside
                h.line(WHITE, -0.6, -4.2, s * 1.7, -1.3, -8.4, s * 3.0, 1.5);
                h.line(WHITE, -1.3, -8.4, s * 3.0, -1.6, -9.8, s * 3.4, 0.6);
                h.line(GREY, -0.15, -4.5, s * 1.8, -0.6, -7.6, s * 2.8, 0.5);
                // curled horns sweeping back and round
                h.line(GREY, 0.8, -4.3, s * 1.1, 0.2, -5.7, s * 1.8, 0.75);
                h.line(GREY, 0.2, -5.7, s * 1.8, -1.0, -6.2, s * 2.3, 0.6);
                h.line(GREY, -1.0, -6.2, s * 2.3, -1.9, -5.4, s * 2.6, 0.48);
                h.line(GREY, -1.9, -5.4, s * 2.6, -1.8, -4.4, s * 2.8, 0.36);
            }
            h.done();
            // the wings, half spread: carved stone membranes (pictures), raised bone ribs, a hooked thumb claw
            for (int s = -1; s <= 1; s += 2) {
                CubeListBuilder wing = cubes();
                sheet(wing, WING_U, WING_V, 0.0F, -10.0F, 0.0F, WING_W, WING_H, 0.0F);
                PartDefinition w = root.addOrReplaceChild("wing" + (s > 0 ? "R" : "L"), wing, at(-2.6, -12.6, s * 2.4, 0, s > 0 ? -2.0 : 2.0, 0));
                // (in the picture's frame: x = its column, y = its row - 10)
                float[] root0 = {0.0F, 0.3F}, wrist = {5.2F, -7.2F};
                float[][] tips = {{13.8F, -9.7F}, {13.6F, -4.2F}, {10.6F, 0.4F}, {6.2F, 2.8F}};
                BoatKit.line(w, "arm", WHITE, root0[0], root0[1], 0.0F, wrist[0], wrist[1], 0.0F, 0.95F);
                for (int f = 0; f < tips.length; f++) {
                    float mx = wrist[0] + (tips[f][0] - wrist[0]) * 0.55F, my = wrist[1] + (tips[f][1] - wrist[1]) * 0.55F;
                    BoatKit.line(w, "f" + f + "a", WHITE, wrist[0], wrist[1], 0.0F, mx, my, 0.0F, 0.62F);
                    BoatKit.line(w, "f" + f + "b", WHITE, mx, my, 0.0F, tips[f][0] - (tips[f][0] - wrist[0]) * 0.04F,
                        tips[f][1] - (tips[f][1] - wrist[1]) * 0.04F, 0.0F, 0.42F);
                }
                BoatKit.line(w, "knuckle", WHITE, wrist[0] - 0.5F, wrist[1], 0.0F, wrist[0] + 0.5F, wrist[1], 0.0F, 1.1F);
                BoatKit.line(w, "thumb", BONE, wrist[0], wrist[1], 0.0F, wrist[0] - 0.6F, wrist[1] - 1.6F, 0.0F, 0.42F);
                BoatKit.line(w, "thumbTip", BONE, wrist[0] - 0.6F, wrist[1] - 1.6F, 0.0F, wrist[0] - 0.1F, wrist[1] - 2.2F, 0.0F, 0.28F);
            }
            // the tail, curling down the back of the plinth to a spade tip
            k.line(WHITE, -3.6, -6.4, 0.0, -6.2, -5.8, 1.2, 1.0);
            k.line(WHITE, -6.2, -5.8, 1.2, -7.0, -3.4, 2.4, 0.8);
            k.line(WHITE, -7.0, -3.4, 2.4, -6.8, -1.4, 0.8, 0.65);
            k.line(WHITE, -6.8, -1.4, 0.8, -6.95, -1.4, -0.9, 0.9);
            k.line(WHITE, -6.95, -1.6, -0.9, -6.95, -1.1, -1.8, 0.5);
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }
}

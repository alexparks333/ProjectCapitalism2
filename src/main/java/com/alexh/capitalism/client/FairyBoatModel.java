package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.BoatKit.*;

// The Fairy Blossom (boats/FairyBoat, Boats.md): a slender leaf-shaped boat, pearl white blushing to lilac at the
// waterline with gold filigree vines painted along its sides (the hull-side pictures), its sheer sweeping up at both
// ends, and smothered in flowers:
//  - garlands of little flowers (rose, white, lavender, apricot, periwinkle, gold hearts) all along its gunwales, and
//    swags of blossoms draped down its sides between roses;
//  - at the bow a green vine rising and curling over like a fern frond, leaves along it, a glowing bell flower
//    swaying from its tip on a thread; at the stern a smaller curl with a cascade of flowers;
//  - an arbor of two silver birch arches over its loveseat, joined by a ridge and rails, flowers along them, fairy
//    lights glowing pink, mint and white, and wisteria hanging in long lilac clusters that sway;
//  - for a mast, a tall stem with two great leaves and a lotus blooming on top: three rings of petals, a glowing gold
//    heart with stamens (it sways a little);
//  - four see-through wings on glowing jewels at its sides (veined in pale gold, iridescent pink, lilac and aqua:
//    the glow pass), folded up and gently breathing on the water, spread wide and fluttering in the air
//    (PremiumBoatRenderer.State.flight);
//  - a birch wheel with a rose at its hub (it turns as you steer), a mossy loveseat and stool with rose cushions.
// Its pictures (wings, petals, leaves) are in the texture below y 180 (tools/BoatTex.cs paints them: keep the places
// the same).
public class FairyBoatModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "fairy_boat"), "main");

    static final int STERN = -20;
    static final int BOW = 22;
    static final float MAIN = -2.0F;
    static final float[] ARCHES = {-7.0F, 4.0F};
    static final float ARCH_TOP = -27.0F;
    static final float MAST_X = 12.0F;
    static final float LOTUS_Y = -33.0F;
    static final float HELM_X = -9.0F;
    static final int RACEMES = 11;

    // ── the pictures (u, v in the 256 x 256 layout), each twice side by side (a flat panel's two sides) ──
    static final int WING_F_U = 0, WING_V = 180;    // the forewings: 9 wide (the chord), 22 long (root at the top)
    static final float WING_F_C = 9.0F, WING_F_S = 22.0F;
    static final int WING_H_U = 18;                 // the hindwings: 7 x 16
    static final float WING_H_C = 7.0F, WING_H_S = 16.0F;
    static final int PETAL_U = 32, PETAL_V = 180;   // a lotus petal, 4 x 7 (tip at the top)
    static final int INNER_U = 40;                  // a smaller, deeper petal, 3 x 5
    static final int LEAF_U = 46;                   // a leaf, 3 x 5
    static final int BIG_LEAF_U = 52;               // a great leaf, 5 x 8

    // the flowers' colours: rose, white, lavender, apricot, periwinkle
    static final Zone[] BLOOMS = {RED, WHITE, NAVY, BRASS, STEEL, RED, WHITE};

    static int halfWidth(int x) {
        double t = (x + 0.5 - STERN) / (BOW + 1.0 - STERN);
        return Math.max(1, (int) Math.round(7.2 * Math.pow(Math.max(0, Math.sin(Math.PI * t)), 0.75)));
    }

    // the sheer sweeping up to both ends
    static int top(int x) {
        double d = (x - 1) / 21.0;
        return x >= 1 ? -4 - (int) Math.round(4.5 * d * d) : -4 - (int) Math.round(3.5 * d * d);
    }

    private final ModelPart bell;
    private final ModelPart lotus;
    private final ModelPart helm;
    private final ModelPart[] wings = new ModelPart[4];
    private final List<ModelPart> racemes = new ArrayList<>();

    public FairyBoatModel(ModelPart root) {
        // drawn see-through (its wings are tinted glass-like membranes: tools/BoatTex.cs)
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucentCull);
        this.bell = this.boat.getChild("bell");
        this.lotus = this.boat.getChild("lotus");
        this.helm = this.boat.getChild("helm");
        String[] names = {"wing_fore_port", "wing_fore_star", "wing_hind_port", "wing_hind_star"};
        for (int i = 0; i < 4; i++) this.wings[i] = this.boat.getChild(names[i]);
        for (int i = 0; i < RACEMES; i++) this.racemes.add(this.boat.getChild("raceme" + i));
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        float flight = state instanceof PremiumBoatRenderer.State s ? s.flight : 0.0F;
        this.rock(state, 0.03F * (1 - flight), 0.02F * (1 - flight), 0.0F);
        // the bell flower and the wisteria sway (back as it goes faster), the lotus nods
        this.bell.zRot = 0.18F * Mth.sin(t * 0.06F) + 0.35F * speed;
        this.bell.xRot = 0.12F * Mth.sin(t * 0.045F + 1.0F);
        for (int i = 0; i < this.racemes.size(); i++) {
            ModelPart r = this.racemes.get(i);
            r.zRot = 0.1F * Mth.sin(t * 0.05F + i * 0.7F) + 0.3F * speed;
            r.xRot = 0.08F * Mth.sin(t * 0.04F + i * 1.3F);
        }
        this.lotus.zRot = 0.035F * Mth.sin(t * 0.04F);
        this.lotus.xRot = 0.03F * Mth.sin(t * 0.033F + 0.5F);
        this.helm.xRot = Mth.clamp(turn(state), -6.0F, 6.0F) * 0.3F;
        // the wings: folded up and slowly breathing on the water; spread and fluttering in the air
        for (int i = 0; i < 4; i++) {
            boolean fore = i < 2;
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float restLift = (fore ? 66.0F : 58.0F) + 6.0F * Mth.sin(t * 0.04F + (fore ? 0.0F : 0.6F));
            float flyLift = 16.0F + 34.0F * Mth.sin(t * 0.85F + (fore ? 0.0F : 0.7F));
            float lift = Mth.lerp(flight, restLift, flyLift) * Mth.DEG_TO_RAD;
            float sweep = Mth.lerp(flight, fore ? -18.0F : -42.0F, fore ? 12.0F : -26.0F) * Mth.DEG_TO_RAD;
            ModelPart w = this.wings[i];
            // laid out to its side (port +z, starboard -z), lifted up, swept back (see createLayer)
            w.xRot = side * ((float) Math.PI / 2 + lift);
            w.yRot = side * sweep;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, FairyBoatModel::halfWidth, FairyBoatModel::top,
            new float[][]{{1, 2.5F, 0.85F}, {2.5F, 3.5F, 0.5F}}, 1);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int tp = top(x);
            int inner = w - t;
            k.box(GOLD, (x - STERN) % 30, 0, x, tp - 0.4F, w - t - 0.1F, 1, 0.4F, t + 0.2F);
            k.box(GOLD, (x - STERN) % 30, 0, x, tp - 0.4F, -w - 0.1F, 1, 0.4F, t + 0.2F);
            if (inner < 1) continue;
            k.box(DARKWOOD, (x - STERN) % 30, 0, x, MAIN, -inner, 1, 1 - MAIN, 2 * inner);
            k.deck(x, MAIN, inner);
        }
        garlands(k, thick);
        swags(k);
        bowCurl(k, boat);
        sternCurl(k);
        arbor(k, boat);
        mast(k, boat);
        seats(k, boat);
        wings(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // a little flower: four petals in a cross, a gold heart; its bottom at y
    static void flower(BoatKit k, Zone zone, float cx, float y, float cz, float s) {
        k.box(zone, 0, 0, cx - s / 2, y - s * 0.32F, cz - s * 0.17F, s, s * 0.32F, s * 0.34F);
        k.box(zone, 2, 2, cx - s * 0.17F, y - s * 0.33F, cz - s / 2, s * 0.34F, s * 0.3F, s);
        k.box(GOLD, cx - s * 0.15F, y - s * 0.45F, cz - s * 0.15F, s * 0.3F, s * 0.2F, s * 0.3F);
    }

    static Zone bloom(int i) {
        return BLOOMS[Math.floorMod(i * 7 + (i / 3) * 3, BLOOMS.length)];
    }

    // a flat picture (a leaf, a petal) standing at (x, y, z), its base there, turned round the upright by yaw and
    // leaning over by lean (radians)
    static void picture(BoatKit k, PartDefinition parent, int u, int v, float w, float h, float x, float y, float z, float yaw, float lean) {
        CubeListBuilder c = CubeListBuilder.create();
        c.texOffs(u, v).addBox(-w / 2, -h, 0.0F, w, h, 0.0F);
        parent.addOrReplaceChild("pic" + (PICTURES++), c, PartPose.offsetAndRotation(x, y, z, lean, yaw, 0.0F));
    }

    private static int PICTURES;

    // garlands along both gunwales: a flower every 1.5 px, a leaf between
    private static void garlands(BoatKit k, int[] thick) {
        int n = 0;
        for (float x = STERN + 2.5F; x < BOW - 2.0F; x += 1.5F) {
            int xi = (int) Math.floor(x);
            int w = halfWidth(xi);
            float zc = w - thick[xi - STERN] * 0.5F;
            float y = top(xi) - 0.4F;
            for (int s = -1; s <= 1; s += 2) {
                flower(k, bloom(n++), x, y, s * zc, n % 5 == 0 ? 1.3F : 0.95F);
                if (n % 2 == 0) k.box(ACCENT, 0, 0, x + 0.5F, y - 0.15F, s > 0 ? zc + 0.2F : -zc - 0.9F, 0.7F, 0.15F, 0.7F);
            }
        }
    }

    // swags of tiny flowers draped down the hull sides between roses
    private static void swags(BoatKit k) {
        float[] anchors = {-15.0F, -9.0F, -3.0F, 3.0F, 9.0F, 15.0F};
        int n = 0;
        for (int s = -1; s <= 1; s += 2) {
            for (int a = 0; a + 1 < anchors.length; a++) {
                float x0 = anchors[a];
                float x1 = anchors[a + 1];
                for (int i = 1; i < 9; i++) {
                    float u = i / 9.0F;
                    float x = x0 + (x1 - x0) * u;
                    int xi = (int) Math.floor(x);
                    float y = top(xi) + 0.9F + 2.6F * 4 * u * (1 - u);
                    float z = s * (halfWidth(xi) + 0.15F);
                    Zone c = i % 3 == 0 ? ACCENT : bloom(n++);
                    k.box(c, 0, 0, x - 0.3F, y - 0.3F, z - 0.3F, 0.6F, 0.6F, 0.6F);
                }
            }
            for (float x : anchors) {
                int xi = (int) Math.floor(x);
                float z = s * (halfWidth(xi) + 0.35F);
                k.box(RED, 4, 4, x - 0.6F, top(xi) + 0.2F, z - 0.6F, 1.2F, 1.2F, 1.2F);                 // a rose
                k.box(RED, 8, 2, x - 0.8F, top(xi) + 0.45F, z - 0.4F, 1.6F, 0.7F, 0.8F);
                k.box(ACCENT, 0, 0, x - 1.4F, top(xi) + 1.1F, z - 0.25F, 0.9F, 0.2F, 0.5F);             // its leaves
                k.box(ACCENT, 2, 0, x + 0.5F, top(xi) + 1.1F, z - 0.25F, 0.9F, 0.2F, 0.5F);
            }
        }
    }

    // the vine curling up and over at the bow like a fern frond, leaves and flowers along it, the glowing bell flower
    // swaying from its tip
    private static void bowCurl(BoatKit k, PartDefinition boat) {
        float[][] curl = {{BOW - 1.0F, top(BOW - 1) + 0.5F}, {24.0F, -10.0F}, {25.8F, -13.5F}, {26.6F, -17.5F}, {26.0F, -21.2F},
            {24.2F, -23.8F}, {21.6F, -24.6F}, {19.4F, -23.4F}, {18.8F, -21.2F}, {19.8F, -19.6F}, {21.4F, -19.4F}, {22.2F, -20.6F}, {21.6F, -21.8F}};
        for (int i = 0; i + 1 < curl.length; i++) {
            float th = 1.7F - 1.1F * i / (curl.length - 2);
            k.line(ACCENT, curl[i][0], curl[i][1], 0.0F, curl[i + 1][0], curl[i + 1][1], 0.0F, th);
        }
        // leaves along it, alternating sides, and a few flowers
        int[] at = {1, 2, 4, 6, 8};
        for (int j = 0; j < at.length; j++) {
            float[] p = curl[at[j]];
            float side = j % 2 == 0 ? 1.0F : -1.0F;
            picture(k, boat, LEAF_U, PETAL_V, 3.0F, 5.0F, p[0], p[1], side * 0.4F, side * 1.2F, side * 1.0F);
        }
        flower(k, RED, curl[3][0] + 0.6F, curl[3][1], 0.0F, 1.3F);
        flower(k, WHITE, curl[5][0], curl[5][1] - 0.6F, 0.0F, 1.1F);
        flower(k, NAVY, curl[2][0] + 0.2F, curl[2][1], 0.9F, 0.9F);
        flower(k, BRASS, curl[7][0] - 0.4F, curl[7][1], -0.8F, 0.9F);
        // the bell flower: hanging on a thread from the curl's outer edge (it sways: setupAnim); glowing petals,
        // a flared rim, a glowing heart below
        CubeListBuilder bell = CubeListBuilder.create();
        box(bell, ACCENT, 0, 0, -0.12F, 0.0F, -0.12F, 0.24F, 4.0F, 0.24F);
        box(bell, ACCENT, 4, 0, -0.7F, 4.0F, -0.7F, 1.4F, 0.4F, 1.4F);          // its green cap
        box(bell, ICE, 0, 0, -1.1F, 4.4F, -1.1F, 2.2F, 1.6F, 2.2F);
        box(bell, ICE, 8, 4, -1.5F, 6.0F, -1.5F, 3.0F, 0.5F, 3.0F);
        box(bell, NAV_WHITE, 0, 0, -0.3F, 6.2F, -0.3F, 0.6F, 1.1F, 0.6F);
        for (int p = 0; p < 4; p++) {                                              // the petal tips curling out
            double a = p * Math.PI / 2 + Math.PI / 4;
            box(bell, ICE, 12, 0, (float) Math.cos(a) * 1.5F - 0.35F, 6.3F, (float) Math.sin(a) * 1.5F - 0.35F, 0.7F, 0.35F, 0.7F);
        }
        boat.addOrReplaceChild("bell", bell, PartPose.offset(26.4F, -17.0F, 0.0F));
    }

    // the smaller curl at the stern, a cascade of flowers at its foot
    private static void sternCurl(BoatKit k) {
        float[][] curl = {{STERN + 1.0F, top(STERN + 1) + 0.5F}, {-21.6F, -9.6F}, {-22.8F, -12.6F}, {-22.4F, -15.2F}, {-20.8F, -16.4F},
            {-19.2F, -15.6F}, {-19.0F, -14.0F}, {-20.2F, -13.4F}};
        for (int i = 0; i + 1 < curl.length; i++) {
            float th = 1.5F - 1.0F * i / (curl.length - 2);
            k.line(ACCENT, curl[i][0], curl[i][1], 0.0F, curl[i + 1][0], curl[i + 1][1], 0.0F, th);
        }
        for (int i = 0; i < 7; i++) {
            float y = top(STERN + 1) - 0.2F + i * 0.75F;
            flower(k, bloom(i + 3), STERN + 0.4F - (i % 2) * 0.5F, y, (i % 3 - 1) * 0.9F, 1.0F);
        }
        flower(k, RED, curl[4][0], curl[4][1] - 0.2F, 0.0F, 1.2F);
    }

    // two silver birch arches over the loveseat, a ridge and rails between them, flowers, fairy lights, wisteria
    private static void arbor(BoatKit k, PartDefinition boat) {
        int steps = 10;
        float[][][] pts = new float[ARCHES.length][steps + 1][];
        for (int a = 0; a < ARCHES.length; a++) {
            float x = ARCHES[a];
            int xi = (int) Math.floor(x);
            float base = top(xi) - 0.4F;
            float r = halfWidth(xi) - 0.8F;
            for (int i = 0; i <= steps; i++) {
                double ang = Math.PI * i / steps;
                pts[a][i] = new float[]{x, base - (base - ARCH_TOP) * (float) Math.sin(ang), r * (float) Math.cos(ang)};
            }
            for (int i = 0; i < steps; i++) {
                float[] p = pts[a][i];
                float[] q = pts[a][i + 1];
                k.line(WOOD, p[0], p[1], p[2], q[0], q[1], q[2], 0.6F);
                // fairy lights hanging under the middle of each piece, pink, mint and white in turn
                Zone glow = i % 3 == 0 ? NAV_RED : i % 3 == 1 ? NAV_GREEN : NAV_WHITE;
                float mx = (p[0] + q[0]) / 2;
                float my = (p[1] + q[1]) / 2;
                float mz = (p[2] + q[2]) / 2;
                k.box(glow, 0, 0, mx - 0.25F + (a == 0 ? -0.5F : 0.5F), my + 0.5F, mz - 0.25F, 0.5F, 0.5F, 0.5F);
                if (i % 2 == 1) flower(k, bloom(i + a * 5), p[0] + (a == 0 ? 0.3F : -0.3F), p[1] + 0.1F, p[2], 1.0F);
            }
        }
        // the ridge and two rails between the arches, a little vine of leaves along the ridge
        k.line(WOOD, ARCHES[0] - 2.0F, ARCH_TOP, 0.0F, ARCHES[1] + 2.0F, ARCH_TOP, 0.0F, 0.5F);
        for (int side : new int[]{3, 7}) {
            float[] p = pts[0][side];
            float[] q = pts[1][side];
            k.line(WOOD, p[0], p[1], p[2], q[0], q[1], q[2], 0.45F);
        }
        for (float x = ARCHES[0] + 1.0F; x < ARCHES[1]; x += 2.0F) {
            k.box(ACCENT, 0, 0, x - 0.4F, ARCH_TOP - 0.55F, -0.6F, 0.8F, 0.25F, 1.2F);
            flower(k, bloom((int) x + 9), x + 1.0F, ARCH_TOP - 0.25F, 0.0F, 0.8F);
        }
        // wisteria: long lilac clusters hanging from the ridge, the rails and the arches' outer sides (they sway)
        List<float[]> hangs = new ArrayList<>();
        hangs.add(new float[]{-4.5F, ARCH_TOP + 0.3F, 0.0F});
        hangs.add(new float[]{-1.5F, ARCH_TOP + 0.3F, 0.0F});
        hangs.add(new float[]{1.5F, ARCH_TOP + 0.3F, 0.0F});
        for (int s = -1; s <= 1; s += 2) {
            int idx = s > 0 ? 3 : 7;
            float[] p = pts[0][idx];
            float[] q = pts[1][idx];
            hangs.add(new float[]{Mth.lerp(0.3F, p[0], q[0]), Mth.lerp(0.3F, p[1], q[1]) + 0.3F, Mth.lerp(0.3F, p[2], q[2])});
            hangs.add(new float[]{Mth.lerp(0.7F, p[0], q[0]), Mth.lerp(0.7F, p[1], q[1]) + 0.3F, Mth.lerp(0.7F, p[2], q[2])});
            for (int a = 0; a < ARCHES.length; a++) {
                float[] o = pts[a][s > 0 ? 1 : steps - 1];
                hangs.add(new float[]{o[0] + (a == 0 ? -0.6F : 0.6F), o[1] + 0.3F, o[2] + s * 0.3F});
            }
        }
        for (int i = 0; i < RACEMES; i++) {
            float[] h = hangs.get(i);
            CubeListBuilder raceme = CubeListBuilder.create();
            int segs = 6 + i % 3;
            float y = 0.0F;
            for (int j = 0; j < segs; j++) {
                float w = 1.6F - j * 1.1F / segs;
                float off = (j % 2 == 0 ? 0.12F : -0.12F);
                box(raceme, NAVY, j * 3, j * 2, -w / 2 + off, y, -w / 2 - off, w, 0.95F, w);
                y += 0.85F;
            }
            box(raceme, ACCENT, 0, 0, -0.1F, -0.6F, -0.1F, 0.2F, 0.7F, 0.2F);
            boat.addOrReplaceChild("raceme" + i, raceme, PartPose.offset(h[0], h[1], h[2]));
        }
    }

    // the mast: a tall stem with two great leaves, a lotus blooming on top (it nods: setupAnim)
    private static void mast(BoatKit k, PartDefinition boat) {
        float[][] stem = {{MAST_X - 1.0F, MAIN}, {MAST_X - 0.4F, -12.0F}, {MAST_X + 0.4F, -22.0F}, {MAST_X, LOTUS_Y + 0.5F}};
        for (int i = 0; i + 1 < stem.length; i++) {
            k.line(ACCENT, stem[i][0], stem[i][1], 0.0F, stem[i + 1][0], stem[i + 1][1], 0.0F, 1.0F - i * 0.12F);
        }
        k.box(MOSS, MAST_X - 2.5F, MAIN - 1.0F, -1.5F, 3.0F, 1.0F, 3.0F);                         // its mossy foot
        flower(k, WHITE, MAST_X - 2.0F, MAIN - 1.0F, 1.0F, 0.9F);
        flower(k, NAVY, MAST_X - 0.2F, MAIN - 1.0F, -1.1F, 0.8F);
        // two great leaves off the stem, curving out to either side
        for (int s = -1; s <= 1; s += 2) {
            float y = s > 0 ? -11.0F : -17.0F;
            CubeListBuilder leaf = CubeListBuilder.create();
            leaf.texOffs(BIG_LEAF_U, PETAL_V).addBox(-2.5F, -8.0F, 0.0F, 5.0F, 8.0F, 0.0F);
            boat.addOrReplaceChild("big_leaf" + s, leaf, PartPose.offsetAndRotation(MAST_X - 0.2F, y, 0.0F, s * 1.05F, 0.4F, 0.0F));
        }
        // the lotus: three rings of petals opening out, a glowing heart with stamens
        PartDefinition lotus = boat.addOrReplaceChild("lotus", CubeListBuilder.create(), PartPose.offset(MAST_X, LOTUS_Y, 0.0F));
        for (int i = 0; i < 8; i++) {
            addPetal(lotus, "outer" + i, PETAL_U, 4.0F, 7.0F, (float) (i * Math.PI / 4), 1.15F);
        }
        for (int i = 0; i < 6; i++) {
            addPetal(lotus, "middle" + i, PETAL_U, 4.0F, 7.0F, (float) (i * Math.PI / 3 + Math.PI / 6), 0.62F);
        }
        for (int i = 0; i < 5; i++) {
            addPetal(lotus, "inner" + i, INNER_U, 3.0F, 5.0F, (float) (i * Math.PI * 2 / 5), 0.22F);
        }
        CubeListBuilder heart = CubeListBuilder.create();
        box(heart, GOLD, 0, 0, -1.0F, -1.4F, -1.0F, 2.0F, 1.4F, 2.0F);
        box(heart, WARM, 0, 0, -0.7F, -1.9F, -0.7F, 1.4F, 0.5F, 1.4F);
        lotus.addOrReplaceChild("heart", heart, PartPose.ZERO);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            CubeListBuilder stamen = CubeListBuilder.create();
            box(stamen, GOLD, 0, 0, -0.1F, -2.2F, -0.1F, 0.2F, 2.2F, 0.2F);
            box(stamen, WARM, 4, 0, -0.22F, -2.6F, -0.22F, 0.44F, 0.44F, 0.44F);
            lotus.addOrReplaceChild("stamen" + i, stamen, PartPose.offsetAndRotation((float) Math.cos(a) * 1.0F, -1.2F,
                (float) Math.sin(a) * 1.0F, (float) Math.sin(a) * 0.35F, 0.0F, (float) -Math.cos(a) * 0.35F));
        }
    }

    // a petal standing on its base, turned round by yaw and opening out by lean
    private static void addPetal(PartDefinition parent, String name, int u, float w, float h, float yaw, float lean) {
        CubeListBuilder c = CubeListBuilder.create();
        c.texOffs(u, PETAL_V).addBox(-w / 2, -h, 0.0F, w, h, 0.0F);
        parent.addOrReplaceChild(name, c, PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, lean, yaw, 0.0F));
    }

    // the mossy loveseat with rose cushions under the arbor, the helm's stool, the birch wheel (it turns: setupAnim)
    private static void seats(BoatKit k, PartDefinition boat) {
        k.box(MOSS, 0, 0, -4.5F, MAIN - 1.2F, -5.0F, 5.0F, 1.2F, 10.0F);
        k.box(LEATHER, 0, 0, -4.2F, MAIN - 1.8F, -4.6F, 4.4F, 0.6F, 4.4F);
        k.box(LEATHER, 2, 2, -4.2F, MAIN - 1.82F, 0.2F, 4.4F, 0.62F, 4.4F);
        k.box(MOSS, 4, 4, -5.4F, MAIN - 5.0F, -5.0F, 1.0F, 5.0F, 10.0F);                           // its back
        k.box(LEATHER, 6, 0, -4.8F, MAIN - 4.6F, -4.4F, 0.5F, 2.8F, 8.8F);
        for (int i = 0; i < 5; i++) flower(k, bloom(i + 20), -5.0F, MAIN - 5.0F, -4.0F + i * 2.0F, 0.9F);
        k.box(MOSS, 2, 2, -13.4F, MAIN - 1.4F, -1.6F, 3.0F, 1.4F, 3.2F);                           // the stool
        k.box(LEATHER, 4, 4, -13.1F, MAIN - 2.0F, -1.3F, 2.4F, 0.6F, 2.6F);
        // the wheel on its post
        k.box(WOOD, 0, 0, HELM_X + 0.3F, MAIN - 7.0F, -0.35F, 0.7F, 7.0F, 0.7F);
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, RED, 4, 4, -0.5F, -0.6F, -0.6F, 1.0F, 1.2F, 1.2F);
        box(hub, GOLD, 0, 0, -0.6F, -0.25F, -0.25F, 1.2F, 0.5F, 0.5F);
        PartDefinition helm = boat.addOrReplaceChild("helm", hub, PartPose.offset(HELM_X, MAIN - 7.0F, 0.0F));
        rim(helm, "rim", WOOD, 2.2F, 0.35F, 0.35F, 12);
        spokes(helm, "spoke", WOOD, 2.9F, 0.28F, 6, 0.0F);
    }

    // the four wings on glowing jewels at its sides. Each is a flat picture with its root at its jewel: laid out to
    // its side and lifted, and swept back (setupAnim)
    private static void wings(BoatKit k, PartDefinition boat) {
        float[][] roots = {{1.0F, 6.0F}, {-1.5F, 5.8F}};
        String[] names = {"wing_fore_port", "wing_fore_star", "wing_hind_port", "wing_hind_star"};
        for (int i = 0; i < 4; i++) {
            boolean fore = i < 2;
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float[] r = roots[fore ? 0 : 1];
            float c = fore ? WING_F_C : WING_H_C;
            float s = fore ? WING_F_S : WING_H_S;
            CubeListBuilder wing = CubeListBuilder.create();
            wing.texOffs(fore ? WING_F_U : WING_H_U, WING_V).addBox(-c / 2, 0.0F, 0.0F, c, s, 0.0F);
            int xi = (int) Math.floor(r[0]);
            boat.addOrReplaceChild(names[i], wing, PartPose.offsetAndRotation(r[0], top(xi) - 1.4F, side * r[1], side * 2.6F, 0.0F, 0.0F));
        }
        for (int s = -1; s <= 1; s += 2) {
            for (float[] r : roots) {
                int xi = (int) Math.floor(r[0]);
                k.box(ICE, 0, 0, r[0] - 0.7F, top(xi) - 2.1F, s * r[1] - 0.7F, 1.4F, 1.4F, 1.4F);
                k.box(GOLD, r[0] - 0.9F, top(xi) - 0.8F, s * r[1] - 0.9F, 1.8F, 0.4F, 1.8F);
            }
        }
    }
}

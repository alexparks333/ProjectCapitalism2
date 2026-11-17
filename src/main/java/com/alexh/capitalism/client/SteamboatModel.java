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

// The River Queen (boats/Steamboat, Boats.md): a grand white Mississippi sternwheeler in the style of the great
// theme-park riverboats, about 5 blocks long with its wheel.
//  - A white hull with a red band and gold pinstripe (the hull-side pictures), a white guard rim along the main deck.
//  - Three tiers of deck, their fronts ROUNDED: the main deck (the cabin aft; forward, two black boilers on a brick
//    bed with glowing firebox doors, under the stacks), the promenade deck (the saloon, tall lit windows under gold
//    arches) and the hurricane deck on top. Turned white posts with gold capitals; between every two posts a panel of
//    lacy gingerbread fretwork (arches, holes, a hanging drop); railings of turned balusters (flat pictures); red,
//    white and blue bunting fans hung along the promenade railing.
//  - The texas cabin on the hurricane deck, and the pilothouse on the FRONT of its roof, windows lit all round, a
//    fringe under its roof, a gold crest on its brow and a gold finial; the bell behind it.
//  - The twin tall black stacks FORWARD of the pilothouse, close together, with feathered gold crowns, a gold arch
//    between them carrying a star in a ring, an iron brace lower down, guy chains.
//  - The big red sternwheel (turning with its speed) on its timbers, driven by pitman arms from the engine
//    cylinders; a jackstaff with a pennant at the bow, the Stars and Stripes at the stern; the helm before the texas.
// The flat pictures (railing, fretwork, fans, flag, crest, ornament) are in the texture below y 180 (tools/BoatTex.cs
// paints them: keep the places the same).
public class SteamboatModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "steamboat"), "main");

    static final int STERN = -32;
    static final int BOW = 34;
    static final float MAIN = -3.0F;      // the main deck
    static final float GUARD = -6.0F;     // the top of the guard rim the side posts stand on
    static final float D2 = -15.0F;       // the promenade deck
    static final float D3 = -27.0F;       // the hurricane deck
    // the upper decks' plan: straight sides HALF out, from BACK_X to CURVE_X, then a round front out to FRONT_X
    static final float HALF = 12.5F;
    static final float BACK_X = -30.0F;
    static final float CURVE_X = 19.0F;
    static final float FRONT_X = 31.0F;
    static final float POST_IN = 0.7F;    // the posts and railings stand this far in from the deck's edge
    // the texas cabin and the pilothouse on the front of its roof
    static final float TX0 = -14.0F;
    static final float TX1 = 7.0F;
    static final float TZ = 6.0F;
    static final float TEXAS_TOP = -36.0F;
    static final float ROOF = -37.0F;     // the top of the texas roof
    static final float PH0 = -1.0F;
    static final float PH1 = 6.5F;
    static final float PZ = 3.6F;
    // the stacks
    static final float STACK_X = 15.0F;
    static final float STACK_Z = 4.5F;
    static final float STACK_TOP = -64.0F;
    // the sternwheel and its engine
    static final float WHEEL_X = -44.0F;
    static final float WHEEL_Y = -6.0F;
    static final float WHEEL_R = 11.0F;
    static final float WHEEL_HALF = 7.6F;
    static final int BOARDS = 14;
    static final float CRANK = 3.5F;
    static final float ENGINE_X = -22.0F;
    static final float ENGINE_Y = -9.0F;
    static final float PITMAN_Z = 11.8F;
    static final float HELM_X = 12.5F;
    static final int FLAG_STRIPS = 12;

    // ── the pictures (u, v in the 256 x 256 layout) ──
    static final int RAIL_U = 0, RAIL_V = 180;          // turned balusters, 2.4 tall (repeating every 0.8)
    static final int LOWRAIL_U = 0, LOWRAIL_V = 183;    // a low railing, 1.6 tall
    static final int FRET_U = 0, FRET_V = 185;          // gingerbread fretwork, 2 tall (an arch every 4)
    static final int FAN_U = 64, FAN_V = 180;           // a bunting fan, 3.2 x 1.6 (twice: its two sides)
    static final int FLAG_U = 72, FLAG_V = 180;         // the Stars and Stripes, 13 columns x 7
    static final int CREST_U = 88, CREST_V = 180;       // the pilothouse crest, 6 x 3.5 (twice)
    static final int STAR_U = 100, STAR_V = 180;        // the star in a ring between the stacks, 4 x 4 (twice)
    static final float FAN_W = 3.2F;
    static final float FAN_H = 1.6F;

    static int halfWidth(int x) {
        double xc = x + 0.5;
        if (xc <= 16) return 11;
        double t = Math.min(1.0, (xc - 16) / 18.0);
        return Math.max(1, (int) Math.round(11 * Math.sqrt(1 - t * t)));
    }

    static int top(int x) {
        return x >= 26 ? -6 : -5;
    }

    // half the upper decks' width at x (their round front)
    static float deckHalf(float x) {
        if (x <= CURVE_X) return HALF;
        float t = Math.min(1.0F, (x - CURVE_X) / (FRONT_X - CURVE_X));
        return HALF * Mth.sqrt(1 - t * t);
    }

    // The posts round a deck, in order: down the starboard side from the stern, round the front, back up the port
    // side ({x, z, 1 if it's on the straight side})
    static List<float[]> outline() {
        List<float[]> points = new ArrayList<>();
        float side = HALF - POST_IN;
        for (float x = BACK_X + 1.0F; x < CURVE_X - 0.01F; x += 4.0F) points.add(new float[]{x, -side, 1});
        float a = FRONT_X - POST_IN - CURVE_X;
        for (int k = 0; k <= 9; k++) {
            double th = -Math.PI / 2 + Math.PI * k / 9;
            points.add(new float[]{CURVE_X + a * (float) Math.cos(th), side * (float) Math.sin(th), k == 0 || k == 9 ? 1 : 0});
        }
        for (float x = CURVE_X - 4.0F; x > BACK_X + 0.99F; x -= 4.0F) points.add(new float[]{x, side, 1});
        return points;
    }

    private final ModelPart wheel;
    private final ModelPart pitmanPort;
    private final ModelPart pitmanStar;
    private final ModelPart helm;
    private final List<ModelPart> bowPennant;
    private final List<ModelPart> flag;

    public SteamboatModel(ModelPart root) {
        super(root);
        this.wheel = this.boat.getChild("sternwheel").getChild("spin");
        this.pitmanPort = this.boat.getChild("pitman_port");
        this.pitmanStar = this.boat.getChild("pitman_star");
        this.helm = this.boat.getChild("helm");
        this.bowPennant = strips(this.boat.getChild("bow_pennant"), 8);
        this.flag = strips(this.boat.getChild("flag"), FLAG_STRIPS);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.01F, 0.008F, 0.0F);
        // the wheel turns (slowly at rest), the pitman arms follow its cranks
        float angle = t * (0.025F + 0.28F * speed);
        this.wheel.xRot = -angle;
        pitman(this.pitmanPort, angle);
        pitman(this.pitmanStar, angle + (float) Math.PI / 2);
        this.helm.xRot = Mth.clamp(turn(state), -6.0F, 6.0F) * 0.3F;
        wave(this.bowPennant, t, 0.4F + 0.9F * speed, 0.45F, 0.7F);
        wave(this.flag, t, 0.3F + 0.8F * speed, 0.35F, 0.5F);
    }

    // a pitman arm: from the crank pin on the wheel (which goes round with it) to the engine
    private static void pitman(ModelPart arm, float angle) {
        float px = WHEEL_X + CRANK * Mth.cos(angle);
        float py = WHEEL_Y + CRANK * Mth.sin(angle);
        arm.x = px;
        arm.y = py;
        arm.zRot = (float) Math.atan2(ENGINE_Y - py, ENGINE_X - px);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, SteamboatModel::halfWidth, SteamboatModel::top,
            new float[][]{{1, 2.5F, 0.9F}, {2.5F, 3.5F, 0.6F}}, 2);
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
        mainDeck(k);
        deckSlabs(k);
        postsAndTrim(k);
        promenade(k);
        texasAndPilothouse(k, boat);
        stacks(k);
        sternwheel(k, boat);
        flags(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // A flat picture standing between two points of the deck plan (a, b), its top at y, h tall, set `out` px to the
    // outside (the outline runs round the deck so the outside is always to its right); its picture at (u, v), its
    // two sides side by side (2 x its length)
    private static void panel(BoatKit k, int u, int v, float ax, float az, float bx, float bz, float y, float h, float out) {
        float dx = bx - ax;
        float dz = bz - az;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 0.05F) return;
        float nx = dz / len;
        float nz = -dx / len;
        CubeListBuilder c = CubeListBuilder.create();
        c.texOffs(u, v).addBox(0.0F, 0.0F, 0.0F, len, h, 0.0F);
        k.part(c, PartPose.offsetAndRotation(ax + nx * out, y, az + nz * out, 0.0F, (float) Math.atan2(-dz, dx), 0.0F));
    }

    // ── the main deck ──

    // the guard rim, the cabin aft, the boilers and fireboxes forward, the capstan and bitts on the foredeck
    private static void mainDeck(BoatKit k) {
        for (float x = BACK_X; x < CURVE_X - 0.01F; x += 7.0F) k.pair(WHITE, x, GUARD, HALF - 1.8F, 7.0F, 0.6F, 1.8F);
        // ...following the round front as far as the hull's sides are low (further on, the posts stand on the hull)
        for (int x = (int) CURVE_X; x < 26; x++) k.pair(WHITE, x, GUARD, deckHalf(x + 0.5F) - 1.8F, 1.0F, 0.6F, 1.8F);
        // the cabin: white walls, lit windows, doors at its front
        for (float x = -26; x < 4; x += 7.5F) k.box(WHITE, 0, 0, x, D2 + 1, -8.0F, 7.5F, MAIN - D2 - 1, 16.0F);
        for (float x = -24; x < 3; x += 3) k.pair(WARM, x, -11.0F, 8.0F, 1.6F, 4.0F, 0.1F);
        k.box(WARM, 4.0F, -10.5F, -2.0F, 0.1F, 6.0F, 4.0F);
        k.box(GOLD, 4.05F, -11.2F, -2.4F, 0.12F, 0.5F, 4.8F);
        // the boilers: two long black drums on a brick bed, banded in gold, their firebox doors glowing at the front
        for (int i = 0; i < 3; i++) for (int s = 0; s < 2; s++) k.box(RED, i * 3, s * 2, 5.5F + i * 4.5F, -4.2F, -7.0F + s * 7.0F, 4.5F, 1.2F, 7.0F);
        for (int s = -1; s <= 1; s += 2) {
            float cz = s * 3.5F;
            float r = 2.6F;
            float cy = -4.2F - r;
            k.box(ENGINE, 0, 0, 6.0F, cy - r, cz - r * 0.62F, 12.5F, 2 * r, 1.24F * r);
            k.box(ENGINE, 4, 4, 6.05F, cy - r * 0.62F, cz - r, 12.4F, 1.24F * r, 2 * r);
            for (float bx = 8.0F; bx < 18; bx += 4.0F) k.box(GOLD, bx, cy - r - 0.05F, cz - r * 0.66F, 0.5F, 2 * r + 0.1F, 1.32F * r);
            k.line(GREY, 12.0F, cy - r, cz, 12.0F, D2 + 1, cz * 0.6F, 0.9F);            // steam pipes up
        }
        k.box(ENGINE, 18.5F, -11.0F, -6.6F, 0.8F, 6.8F, 13.2F);                          // the firebox front
        for (int s = -1; s <= 1; s += 2) {
            for (int d = -1; d <= 1; d += 2) {
                float z = s * 3.5F + d * 1.0F - 0.6F;
                k.box(FIRE, 19.3F, -7.6F, z, 0.1F, 1.6F, 1.2F);                          // glowing doors
                k.box(GOLD, 19.35F, -7.9F, z - 0.1F, 0.1F, 0.3F, 1.4F);
            }
        }
        // the foredeck: a capstan with a gold cap, a pair of bitts each side
        k.box(DARKWOOD, 0, 0, 25.0F, MAIN - 2.4F, -1.2F, 2.4F, 2.4F, 2.4F);
        k.box(DARKWOOD, 4, 0, 25.3F, MAIN - 2.4F, -1.5F, 1.8F, 2.0F, 3.0F);
        k.box(GOLD, 25.1F, MAIN - 2.9F, -1.1F, 2.2F, 0.5F, 2.2F);
        for (int s = -1; s <= 1; s += 2) {
            k.box(BLACK, 29.0F, MAIN - 1.6F, s * 3.0F - 0.4F, 0.8F, 1.6F, 0.8F);
            k.box(BLACK, 29.0F, MAIN - 1.6F, s * 4.6F - 0.4F, 0.8F, 1.6F, 0.8F);
        }
    }

    // where a main deck post stands: on the guard rim (aft of x 26), on the hull's side (its gold cap) further
    // forward, or on the deck if it's well inside the hull
    static float mainFoot(float x, float z) {
        if (x < 26.0F) return GUARD;
        return Math.abs(z) > halfWidth((int) Math.floor(x)) - 1.5F ? top((int) Math.floor(x)) - 0.4F : MAIN;
    }

    // the promenade and hurricane deck slabs (round fronts), decked on top, edged in gold
    private static void deckSlabs(BoatKit k) {
        for (float y : new float[]{D2, D3}) {
            for (float x = BACK_X; x < CURVE_X - 0.01F; x += 7.0F) k.box(WHITE, 0, 0, x, y, -HALF, 7.0F, 1.0F, 2 * HALF);
            for (int x = (int) CURVE_X; x < FRONT_X; x++) {
                float h = deckHalf(x + 0.5F);
                if (h < 0.5F) continue;
                k.box(WHITE, 0, 0, x, y, -h, 1.0F, 1.0F, 2 * h);
            }
            for (int x = (int) BACK_X; x < FRONT_X; x++) {
                int inner = Math.min(11, (int) Math.floor(deckHalf(x + 0.5F) - 0.3F));
                k.deck(x, y, inner);
            }
            for (int i = 0; i < 7; i++) k.pair(GOLD, BACK_X - 0.1F + i * 7.0F, y + 0.25F, HALF, 7.0F, 0.4F, 0.12F);
        }
    }

    // posts down both decks round the outline (gold capitals), the fretwork in every bay, the railings, the bunting
    private static void postsAndTrim(BoatKit k) {
        List<float[]> pts = outline();
        for (float floor : new float[]{MAIN, D2}) {
            float ceiling = floor == MAIN ? D2 + 1 : D3 + 1;
            for (float[] p : pts) {
                float foot = floor == MAIN ? mainFoot(p[0], p[1]) : floor;
                k.box(WHITE, 0, 0, p[0] - 0.3F, ceiling, p[1] - 0.3F, 0.6F, foot - ceiling, 0.6F);
                k.box(GOLD, 0, 0, p[0] - 0.4F, ceiling, p[1] - 0.4F, 0.8F, 0.5F, 0.8F);
                k.box(WHITE, 4, 0, p[0] - 0.4F, foot - 0.5F, p[1] - 0.4F, 0.8F, 0.5F, 0.8F);
            }
            for (int i = 0; i + 1 < pts.size(); i++) {
                float[] a = pts.get(i);
                float[] b = pts.get(i + 1);
                panel(k, FRET_U, FRET_V, a[0], a[1], b[0], b[1], ceiling, 2.0F, 0.0F);
            }
        }
        // the promenade's back: fretwork across the stern
        float side = HALF - POST_IN;
        float back = BACK_X + 1.0F;
        panel(k, FRET_U, FRET_V, back, side, back, -side, D3 + 1, 2.0F, 0.08F);
        // railings: the promenade and the hurricane deck, all round (and across the back)
        for (float floor : new float[]{D2, D3}) {
            for (int i = 0; i + 1 < pts.size(); i++) {
                float[] a = pts.get(i);
                float[] b = pts.get(i + 1);
                panel(k, RAIL_U, RAIL_V, a[0], a[1], b[0], b[1], floor - 2.4F, 2.4F, 0.0F);
                k.line(WHITE, a[0], floor - 2.5F, a[1], b[0], floor - 2.5F, b[1], 0.36F);
            }
            panel(k, RAIL_U, RAIL_V, back, side, back, -side, floor - 2.4F, 2.4F, 0.08F);
            float rs = side - 0.35F;   // (stopping short of the side rails)
            for (int i = 0; i < 3; i++) k.line(WHITE, back, floor - 2.5F, -rs + i * 2 * rs / 3, back, floor - 2.5F, -rs + (i + 1) * 2 * rs / 3, 0.36F);
        }
        // the hurricane deck's newel posts (the promenade's are its posts), gold knobs
        for (float[] p : pts) {
            k.box(WHITE, 0, 0, p[0] - 0.25F, D3 - 2.8F, p[1] - 0.25F, 0.5F, 2.8F, 0.5F);
            k.box(GOLD, 0, 0, p[0] - 0.3F, D3 - 3.3F, p[1] - 0.3F, 0.6F, 0.5F, 0.6F);
        }
        k.box(WHITE, 0, 0, back - 0.25F, D3 - 2.8F, -0.25F, 0.5F, 2.8F, 0.5F);
        // red, white and blue bunting fans hung on the promenade railing: every other bay down the sides, and three
        // round the front
        for (int i = 0; i + 1 < pts.size(); i++) {
            float[] a = pts.get(i);
            float[] b = pts.get(i + 1);
            boolean straight = a[2] > 0 && b[2] > 0;
            if (straight ? i % 2 != 0 : !(i == 13 || i == 16 || i == 19)) continue;
            float dx = b[0] - a[0];
            float dz = b[1] - a[1];
            float len = Mth.sqrt(dx * dx + dz * dz);
            float f = (len - FAN_W) / 2 / len;
            panel(k, FAN_U, FAN_V, a[0] + dx * f, a[1] + dz * f, b[0] - dx * f, b[1] - dz * f, D2 - 2.3F, FAN_H, 0.22F);
        }
    }

    // the promenade saloon: white walls, tall lit windows under gold arches, its front doors
    private static void promenade(BoatKit k) {
        for (int i = 0; i < 5; i++) k.box(WHITE, 0, 0, -22.0F + i * 6.8F, D3 + 1, -7.5F, 6.8F, D2 - D3 - 1, 15.0F);
        for (float x = -20.5F; x < 11; x += 3) {
            k.pair(WARM, x, -23.0F, 7.5F, 1.6F, 6.0F, 0.1F);
            k.pair(GOLD, x - 0.2F, -23.8F, 7.55F, 2.0F, 0.6F, 0.12F);
        }
        k.box(WARM, 12.0F, -23.0F, -3.0F, 0.1F, 6.5F, 6.0F);
        k.box(GOLD, 12.05F, -23.8F, -3.4F, 0.12F, 0.6F, 6.8F);
        k.box(WARM, -22.1F, -23.0F, -2.5F, 0.1F, 6.0F, 5.0F);
    }

    // the texas cabin, the pilothouse on the front of its roof (lit all round, a fringe, a gold crest and finial),
    // the bell, the helm before the texas
    private static void texasAndPilothouse(BoatKit k, PartDefinition boat) {
        for (float x = TX0; x < TX1 - 0.01F; x += 7.0F) k.box(WHITE, 0, 0, x, TEXAS_TOP, -TZ, 7.0F, D3 - TEXAS_TOP, 2 * TZ);
        for (float x = TX0 + 1.5F; x < TX1 - 1; x += 3) k.pair(WARM, x, -33.6F, TZ, 1.6F, 2.4F, 0.1F);
        k.box(WARM, TX1, -34.0F, -1.2F, 0.1F, 5.0F, 2.4F);                              // the texas door
        for (int s = -1; s <= 1; s += 2) k.box(WARM, TX1, -33.6F, s * 3.5F - 0.8F, 0.1F, 2.4F, 1.6F);
        // its roof, overhanging, a fringe of fretwork under the edge
        k.box(WHITE, 0, 0, TX0 - 1.0F, ROOF, -TZ - 1.0F, 11.5F, 1.0F, 2 * TZ + 2.0F);
        k.box(WHITE, 0, 0, TX0 + 10.5F, ROOF, -TZ - 1.0F, 11.5F, 1.0F, 2 * TZ + 2.0F);
        k.pair(GOLD, TX0 - 1.1F, ROOF + 0.3F, TZ + 1.05F, 11.6F, 0.35F, 0.12F);
        k.pair(GOLD, TX0 + 10.5F, ROOF + 0.3F, TZ + 1.05F, 11.6F, 0.35F, 0.12F);
        float ez = TZ + 0.6F;
        for (float x = TX0 - 0.5F; x < TX1; x += 4.0F) {
            float x2 = Math.min(TX1 + 0.5F, x + 4.0F);
            panel(k, FRET_U, FRET_V, x, -ez, x2, -ez, TEXAS_TOP + 0.01F, 1.2F, 0.0F);
            panel(k, FRET_U, FRET_V, x2, ez, x, ez, TEXAS_TOP + 0.01F, 1.2F, 0.0F);
        }
        // a low railing round the texas roof
        float rz = TZ + 0.5F;
        float r0 = TX0 - 0.5F;
        float r1 = TX1 + 0.5F;
        float[][] roofRail = {{r0, -rz}, {r1, -rz}, {r1, rz}, {r0, rz}, {r0, -rz}};
        for (int i = 0; i + 1 < roofRail.length; i++) {
            float[] a = roofRail[i];
            float[] b = roofRail[i + 1];
            if (i == 1) {   // the front: only either side of the pilothouse
                panel(k, LOWRAIL_U, LOWRAIL_V, r1, -rz, r1, -PZ - 0.6F, ROOF - 1.6F, 1.6F, 0.0F);
                panel(k, LOWRAIL_U, LOWRAIL_V, r1, PZ + 0.6F, r1, rz, ROOF - 1.6F, 1.6F, 0.0F);
                continue;
            }
            panel(k, LOWRAIL_U, LOWRAIL_V, a[0], a[1], b[0], b[1], ROOF - 1.6F, 1.6F, 0.0F);
            k.box(WHITE, 0, 0, a[0] - 0.25F, ROOF - 1.9F, a[1] - 0.25F, 0.5F, 1.9F, 0.5F);
        }

        // the pilothouse: a white base, windows lit all round between white mullions, a header, a two-step roof
        float pw = PH1 - PH0;
        k.box(WHITE, 0, 0, PH0, ROOF - 2.4F, -PZ, pw, 2.4F, 2 * PZ);
        k.box(WARM, PH0 + 0.2F, ROOF - 6.4F, -PZ + 0.2F, pw - 0.4F, 4.0F, 2 * PZ - 0.4F);
        for (int s = -1; s <= 1; s += 2) {
            for (float px : new float[]{PH0, PH1 - 0.5F}) k.box(WHITE, 0, 0, px, ROOF - 7.0F, s * PZ - (s > 0 ? 0.5F : 0.0F), 0.5F, 4.6F, 0.5F);
            for (float x = PH0 + 1.8F; x < PH1 - 1; x += 1.8F) k.box(WHITE, 0, 0, x, ROOF - 6.4F, s * (PZ - 0.05F) - 0.1F, 0.25F, 4.0F, 0.2F);
        }
        for (float z = -PZ + 1.8F; z < PZ - 1; z += 1.8F) {
            k.box(WHITE, 0, 0, PH1 - 0.15F, ROOF - 6.4F, z - 0.12F, 0.2F, 4.0F, 0.25F);
            k.box(WHITE, 0, 0, PH0 - 0.05F, ROOF - 6.4F, z - 0.12F, 0.2F, 4.0F, 0.25F);
        }
        k.box(WHITE, 0, 0, PH0 - 0.1F, ROOF - 7.0F, -PZ - 0.1F, pw + 0.2F, 0.6F, 2 * PZ + 0.2F);
        float roofY = ROOF - 8.0F;
        k.box(WHITE, 0, 0, PH0 - 1.0F, roofY, -PZ - 1.0F, pw + 2.0F, 1.0F, 2 * PZ + 2.0F);
        k.box(WHITE, 4, 4, PH0 + 0.3F, roofY - 0.8F, -PZ + 0.3F, pw - 0.6F, 0.8F, 2 * PZ - 0.6F);
        k.box(GOLD, PH0 - 1.1F, roofY + 0.3F, -PZ - 1.1F, 0.12F, 0.35F, 2 * PZ + 2.2F);
        k.box(GOLD, PH1 + 0.98F, roofY + 0.3F, -PZ - 1.1F, 0.12F, 0.35F, 2 * PZ + 2.2F);
        // the fringe under its roof
        float fz = PZ + 0.6F;
        float f0 = PH0 - 0.6F;
        float f1 = PH1 + 0.6F;
        panel(k, FRET_U, FRET_V, f0, -fz, f1, -fz, roofY + 1.0F, 1.0F, 0.0F);
        panel(k, FRET_U, FRET_V, f1, -fz, f1, fz, roofY + 1.0F, 1.0F, 0.0F);
        panel(k, FRET_U, FRET_V, f1, fz, f0, fz, roofY + 1.0F, 1.0F, 0.0F);
        panel(k, FRET_U, FRET_V, f0, fz, f0, -fz, roofY + 1.0F, 1.0F, 0.0F);
        // the gold crest on its brow, a gold finial on top
        panel(k, CREST_U, CREST_V, PH1 + 0.6F, -3.0F, PH1 + 0.6F, 3.0F, roofY - 3.5F, 3.5F, 0.0F);
        float mid = (PH0 + PH1) / 2;
        k.box(GOLD, mid - 0.5F, roofY - 2.0F, -0.5F, 1.0F, 1.2F, 1.0F);
        k.box(GOLD, mid - 0.2F, roofY - 3.4F, -0.2F, 0.4F, 1.4F, 0.4F);
        k.box(GOLD, mid - 0.35F, roofY - 3.9F, -0.35F, 0.7F, 0.5F, 0.7F);

        // the bell on the texas roof behind the pilothouse, on a white frame
        float bx = -5.0F;
        k.box(WHITE, 0, 0, bx - 0.2F, ROOF - 4.0F, -1.4F, 0.4F, 4.0F, 0.4F);
        k.box(WHITE, 0, 0, bx - 0.2F, ROOF - 4.0F, 1.0F, 0.4F, 4.0F, 0.4F);
        k.box(WHITE, 4, 0, bx - 0.3F, ROOF - 4.4F, -1.6F, 0.6F, 0.4F, 3.2F);
        k.box(BRASS, 0, 0, bx - 0.8F, ROOF - 3.6F, -0.8F, 1.6F, 1.6F, 1.6F);
        k.box(BRASS, 4, 4, bx - 0.55F, ROOF - 4.0F, -0.55F, 1.1F, 0.4F, 1.1F);

        // the helm on the hurricane deck, before the texas (it turns: setupAnim)
        k.box(DARKWOOD, 0, 0, HELM_X + 0.4F, D3 - 7.0F, -0.4F, 0.8F, 7.0F, 0.8F);
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, BRASS, 0, 0, -0.4F, -0.5F, -0.5F, 0.8F, 1.0F, 1.0F);
        PartDefinition helm = boat.addOrReplaceChild("helm", hub, PartPose.offset(HELM_X, D3 - 7.0F, 0.0F));
        rim(helm, "rim", DARKWOOD, 2.6F, 0.4F, 0.4F, 12);
        spokes(helm, "spoke", DARKWOOD, 3.4F, 0.3F, 8, 0.0F);
    }

    // the twin stacks: plain black, close together, feathered gold crowns, a gold arch between them carrying a star
    // in a ring, an iron brace lower down, guy chains, a whistle
    private static void stacks(BoatKit k) {
        float h = STACK_TOP - D3;
        for (int s = -1; s <= 1; s += 2) {
            float z = s * STACK_Z;
            k.box(ENGINE, 0, 0, STACK_X - 1.5F, STACK_TOP, z - 1.0F, 3.0F, -h, 2.0F);
            k.box(ENGINE, 8, 0, STACK_X - 1.0F, STACK_TOP, z - 1.5F, 2.0F, -h, 3.0F);
            k.box(GOLD, STACK_X - 2.0F, D3 - 1.0F, z - 2.0F, 4.0F, 1.0F, 4.0F);               // the collar at the deck
            k.box(GOLD, STACK_X - 1.7F, STACK_TOP + 0.4F, z - 1.7F, 3.4F, 0.7F, 3.4F);         // the crown's band
            // the feathered crown: gold plumes flaring out, curling at the tips
            for (int p = 0; p < 12; p++) {
                double a = p * Math.PI / 6;
                float cx = (float) Math.cos(a);
                float cz = (float) Math.sin(a);
                float tx = STACK_X + cx * 2.9F;
                float tz = z + cz * 2.9F;
                k.line(GOLD, STACK_X + cx * 1.3F, STACK_TOP, z + cz * 1.3F, tx, STACK_TOP - 4.6F, tz, 0.5F);
                k.line(GOLD, tx, STACK_TOP - 4.6F, tz, STACK_X + cx * 3.4F, STACK_TOP - 4.2F, z + cz * 3.4F, 0.35F);
            }
            k.line(ROPE, STACK_X, -50.0F, z, STACK_X - 6.0F, D3 - 1.0F, z + s * 6.5F, 0.15F);
            k.line(ROPE, STACK_X, -50.0F, z, STACK_X + 7.0F, D3 - 1.0F, z + s * 6.0F, 0.15F);
        }
        // the gold arch between them, the star in its ring hanging under it
        float inner = STACK_Z - 1.5F;
        k.line(GOLD, STACK_X, -55.0F, -inner, STACK_X, -57.6F, -1.0F, 0.45F);
        k.line(GOLD, STACK_X, -57.6F, -1.0F, STACK_X, -57.6F, 1.0F, 0.45F);
        k.line(GOLD, STACK_X, -57.6F, 1.0F, STACK_X, -55.0F, inner, 0.45F);
        panel(k, STAR_U, STAR_V, STACK_X + 0.05F, -2.0F, STACK_X + 0.05F, 2.0F, -57.4F, 4.0F, 0.0F);
        k.box(ENGINE, 16, 0, STACK_X - 0.3F, -44.0F, -inner, 0.6F, 0.6F, 2 * inner);       // the iron brace
        k.box(BRASS, 0, 0, STACK_X - 2.5F, -48.0F, STACK_Z + 0.6F, 0.8F, 2.2F, 0.8F);       // the whistle
        k.box(BRASS, 4, 0, STACK_X - 2.6F, -48.4F, STACK_Z + 0.5F, 1.0F, 0.4F, 1.0F);
    }

    // the sternwheel on its timbers (turning: setupAnim), the engine cylinders and pitman arms
    private static void sternwheel(BoatKit k, PartDefinition boat) {
        float tz = WHEEL_HALF + 1.4F;
        for (int s = -1; s <= 1; s += 2) {
            k.line(DARKWOOD, STERN + 1.0F, -5.5F, s * tz, WHEEL_X - 3.0F, WHEEL_Y + 0.5F, s * tz, 1.0F);
            k.line(DARKWOOD, STERN + 1.0F, D2 + 1.0F, s * tz, WHEEL_X - 1.0F, WHEEL_Y - 1.0F, s * tz, 0.8F);
            k.box(GREY, 0, 0, WHEEL_X - 0.9F, WHEEL_Y - 0.9F, s > 0 ? tz - 0.9F : -tz - 0.9F, 1.8F, 1.8F, 1.8F);
            // the engine cylinder alongside the hull, where the pitman arm is driven from
            k.box(GREY, 4, 0, ENGINE_X - 3.0F, ENGINE_Y - 1.0F, s > 0 ? PITMAN_Z - 0.4F : -PITMAN_Z - 1.0F, 5.0F, 2.0F, 1.4F);
            k.box(BRASS, ENGINE_X - 3.2F, ENGINE_Y - 1.2F, s > 0 ? PITMAN_Z - 0.5F : -PITMAN_Z - 1.1F, 0.4F, 2.4F, 1.6F);
        }
        k.box(GREY, 0, 0, WHEEL_X - 0.5F, WHEEL_Y - 0.5F, -tz, 1.0F, 1.0F, 2 * tz);              // the shaft
        PartDefinition mount = boat.addOrReplaceChild("sternwheel", CubeListBuilder.create(),
            PartPose.offsetAndRotation(WHEEL_X, WHEEL_Y, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
        PartDefinition spin = mount.addOrReplaceChild("spin", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition face = spin.addOrReplaceChild("face" + side, CubeListBuilder.create(), PartPose.offset(side * WHEEL_HALF, 0, 0));
            rim(face, "rim", ACCENT, WHEEL_R, 0.7F, 0.6F, 20);
            rim(face, "inner", WHITE, WHEEL_R * 0.55F, 0.4F, 0.48F, 14);
            spokes(face, "spoke", ACCENT, WHEEL_R, 0.5F, 14, 0.0F);
        }
        // a middle ring, so the long boards don't sag
        PartDefinition middle = spin.addOrReplaceChild("middle", CubeListBuilder.create(), PartPose.ZERO);
        rim(middle, "rim", ACCENT, WHEEL_R * 0.8F, 0.5F, 0.5F, 18);
        for (int b = 0; b < BOARDS; b++) {
            CubeListBuilder board = CubeListBuilder.create();
            box(board, ACCENT, b * 2, 0, -WHEEL_HALF - 0.1F, -WHEEL_R + 0.2F, -0.3F, 2 * WHEEL_HALF + 0.2F, 3.0F, 0.6F);
            box(board, WHITE, 0, 0, -WHEEL_HALF - 0.3F, -WHEEL_R + 0.05F, -0.36F, 2 * WHEEL_HALF + 0.6F, 0.3F, 0.72F);
            spin.addOrReplaceChild("board" + b, board, PartPose.offsetAndRotation(0, 0, 0, (float) (b * 2 * Math.PI / BOARDS + Math.PI / BOARDS), 0, 0));
        }
        // the pitman arms, alongside the hull (they pump: setupAnim)
        float len = Mth.sqrt((ENGINE_X - WHEEL_X) * (ENGINE_X - WHEEL_X) + (ENGINE_Y - WHEEL_Y) * (ENGINE_Y - WHEEL_Y));
        for (int s = -1; s <= 1; s += 2) {
            CubeListBuilder arm = CubeListBuilder.create();
            box(arm, DARKWOOD, 0, 0, 0.0F, -0.45F, -0.3F, len, 0.9F, 0.6F);
            box(arm, GREY, 0, 0, -0.5F, -0.6F, -0.4F, 1.0F, 1.2F, 0.8F);
            boat.addOrReplaceChild(s > 0 ? "pitman_port" : "pitman_star", arm, PartPose.offset(WHEEL_X, WHEEL_Y, s * PITMAN_Z));
        }
    }

    // a pennant on the jackstaff at the bow, the Stars and Stripes on a staff at the stern
    private static void flags(BoatKit k, PartDefinition boat) {
        k.box(WHITE, 0, 0, 31.6F, -20.0F, -0.2F, 0.4F, 20.0F + top(31), 0.4F);
        k.box(GOLD, 31.5F, -20.6F, -0.3F, 0.6F, 0.6F, 0.6F);
        PartDefinition bow = boat.addOrReplaceChild("bow_pennant", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float h = 2.0F * (1 - i / 10.0F) + 0.3F;
            CubeListBuilder strip = CubeListBuilder.create();
            box(strip, i % 2 == 0 ? RED : WHITE, i * 2, 0, -1.0F, 0, 0, 1.0F, h, 0.0F);
            bow.addOrReplaceChild("f" + i, strip, PartPose.offset(31.8F - i, -19.8F, 0.0F));
        }
        float sx = BACK_X - 0.6F;
        k.box(WHITE, 0, 0, sx - 0.2F, D3 - 19.0F, -0.2F, 0.4F, 19.0F, 0.4F);
        k.box(GOLD, sx - 0.35F, D3 - 19.7F, -0.35F, 0.7F, 0.7F, 0.7F);
        PartDefinition flag = boat.addOrReplaceChild("flag", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit.flag(flag, WHITE, FLAG_U, FLAG_V, sx, D3 - 18.6F, FLAG_STRIPS, 7.0F, true);
    }
}

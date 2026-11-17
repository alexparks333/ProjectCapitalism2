package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.client.BoatKit.Zone;
import com.alexh.capitalism.fishing.RareCatch;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import static com.alexh.capitalism.client.BloodDecorModels.K;

// The catches mounted on Fish Plaques (client/FishPlaqueRenderer, Fishing.md > Plaques), sculpted from boxes like a
// taxidermist's trophy: each fish a body of stacked slices along a bent spine (thick through the middle, tapering to
// the snout and the tail, every slice a wide flank box with a narrower back and belly box on it, so it's round in
// section), its fins and tail separate angled plates, eyes on both sides, the shark's and the anglerfish's jaws open
// with teeth. The Kraken Tentacle curls out of the board; the Leviathan Scale stands on a little brass shelf. Each has
// its own 64 x 64 texture (textures/entity/plaque/<id>.png, painted one texel a pixel by tools/FishTex.cs).
//
// Model space is the plaque's (client/FishPlaqueRenderer.createLayer): pixels, the board's front at x -6.6 (+x out of
// the wall), y down (the board from y -14 to -2, the nameplate from -7 to -3), +z to the right as you face it; a
// 1-wide board runs z -7.4 to 7.4, a 2-wide one to 23.4, a 3-wide one from -23.4 to 23.4.
public final class FishTrophies {

    // the texture's zones (64 x 64)
    static final Zone BACK = new Zone(0, 0, 32, 16);
    static final Zone BELLY = new Zone(32, 0, 32, 16);
    static final Zone SIDE = new Zone(0, 16, 32, 16);
    static final Zone FIN = new Zone(32, 16, 32, 16);
    static final Zone EYE = new Zone(0, 32, 8, 8);
    static final Zone WHITE = new Zone(8, 32, 8, 8);
    static final Zone MOUTH = new Zone(16, 32, 8, 8);
    static final Zone DARK = new Zone(24, 32, 8, 8);
    static final Zone BAND = new Zone(0, 40, 32, 8);
    static final Zone ACCENT = new Zone(32, 32, 32, 16);
    static final Zone ACCENT2 = new Zone(0, 48, 32, 16);
    static final Zone GLOW = new Zone(32, 48, 32, 16);

    static final double BOARD = -6.6;

    public static final Map<RareCatch, ModelLayerLocation> LAYERS = new EnumMap<>(RareCatch.class);

    static {
        for (RareCatch c : RareCatch.values()) {
            if (c.plaque > 0) LAYERS.put(c, new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "fish_trophy_" + c.id), "main"));
        }
    }

    private FishTrophies() {
    }

    public static Identifier texture(RareCatch c) {
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/plaque/" + c.id + ".png");
    }

    // the Ghost Koi is see-through and glows; the anglerfish's lure, the eel's sparks and the scale's runes glow
    public static boolean seeThrough(RareCatch c) {
        return c == RareCatch.GHOST_KOI;
    }

    public static boolean glows(RareCatch c) {
        return c == RareCatch.ANGLERFISH || c == RareCatch.ELECTRIC_EEL || c == RareCatch.GOLDEN_LEVIATHAN_SCALE;
    }

    // A trophy: its body ("decor") and, drawn again at full brightness, its glowing bits ("glow"): Anim.lit picks which
    public static final class Trophy extends Model<BloodDecorModels.Anim> {
        private final ModelPart body;
        private final ModelPart glow;

        public Trophy(ModelPart root, boolean seeThrough) {
            super(root, seeThrough ? RenderTypes::entityTranslucent : RenderTypes::entityCutout);
            this.body = root.getChild("decor");
            this.glow = root.getChild("glow");
        }

        @Override
        public void setupAnim(BloodDecorModels.Anim s) {
            super.setupAnim(s);
            this.body.visible = !s.lit();
            this.glow.visible = s.lit();
        }
    }

    // ── a fish's body ──

    // a profile along the fish (s: 0 at the snout, 1 at the root of the tail): `head` at the snout, 1 at `peak`, `tail`
    // at the tail
    static DoubleUnaryOperator prof(double head, double peak, double tail) {
        return s -> s < peak ? head + (1 - head) * Math.sin(Math.PI / 2 * s / peak)
            : tail + (1 - tail) * Math.cos(Math.PI / 2 * (s - peak) / (1 - peak));
    }

    static final class Fish {
        double x, y = -10.8, z, len = 11, h = 3, t = 1.8, step = 1;
        DoubleUnaryOperator hp = prof(0.55, 0.38, 0.32), tp = prof(0.6, 0.35, 0.35), bend = s -> 0;
        IntFunction<Zone> side = i -> SIDE;
        Zone back = BACK, belly = BELLY;
        // a crest along the back (the oarfish's red ribbon) / a fin strip along the belly (the eel's), from `keelFrom` on
        double crest, keel, keelFrom;

        Fish(double h, double t) {
            this.h = h;
            this.t = t;
            this.x = BOARD + t + 1.0;
        }

        double y(double s) {
            return this.y + this.bend.applyAsDouble(s);
        }

        double z(double s) {
            return this.z + s * this.len;
        }

        double h(double s) {
            return this.h * this.hp.applyAsDouble(s);
        }

        double t(double s) {
            return this.t * this.tp.applyAsDouble(s);
        }

        // the spine's tilt at s (as an x rotation: a part's +z along the spine)
        double tilt(double s) {
            double a = Math.max(0, s - 0.02), b = Math.min(1, s + 0.02);
            return -Math.atan2(this.y(b) - this.y(a), this.z(b) - this.z(a));
        }

        double top(double s) {
            return this.y(s) - this.h(s);
        }

        double bottom(double s) {
            return this.y(s) + this.h(s);
        }
    }

    static void box(CubeListBuilder c, Zone zone, int du, int dv, double x, double y, double z, double w, double h, double d) {
        BoatKit.box(c, zone, du, dv, (float) x, (float) y, (float) z, (float) w, (float) h, (float) d);
    }

    // the body: a slice every `step` along the spine, each a wide flank box with a narrower back and belly box over it
    static void body(K k, Fish f) {
        int n = Math.max(2, (int) Math.ceil(f.len / f.step));
        for (int i = 0; i < n; i++) {
            double s0 = (double) i / n, s1 = (double) (i + 1) / n, sm = (s0 + s1) / 2;
            double y0 = f.y(s0), y1 = f.y(s1), z0 = f.z(s0), z1 = f.z(s1);
            double seg = Math.hypot(y1 - y0, z1 - z0) + 0.1;
            double hh = Math.max(0.3, f.h(sm)), tt = Math.max(0.25, f.t(sm));
            CubeListBuilder c = CubeListBuilder.create();
            int du = (i * 7) % 20, dv = (i * 3) % 6;
            box(c, f.side.apply(i), du, dv, -tt, -hh * 0.58, 0, 2 * tt, hh * 1.16, seg);
            box(c, f.back, du, dv, -tt * 0.68, -hh, 0, tt * 1.36, hh * 0.5, seg);
            box(c, f.belly, du, dv, -tt * 0.68, hh * 0.5, 0, tt * 1.36, hh * 0.5, seg);
            if (f.crest > 0) {
                double cr = f.crest * (0.75 + 0.25 * Math.sin(i * 1.3));
                box(c, ACCENT, i % 10, 0, -0.2, -hh - cr, 0, 0.4, cr + 0.3, seg);
            }
            if (f.keel > 0 && sm > f.keelFrom) box(c, FIN, i % 10, 0, -0.2, hh - 0.3, 0, 0.4, f.keel + 0.3, seg);
            k.part(c, f.x, y0, z0, -Math.atan2(y1 - y0, z1 - z0), 0, 0);
        }
    }

    // a fin standing up off the body: rows `height` tall in all, its base `len` long along +z, its tip swept back
    // `sweep` (before turning: xr tips it with the spine, zr leans it over)
    static void fin(K k, Zone zone, double x, double y, double z, double len, double height, double sweep, double xr, double yr, double zr) {
        CubeListBuilder c = CubeListBuilder.create();
        int rows = Math.max(1, (int) Math.round(height / 0.6));
        double rh = height / rows;
        for (int r = 0; r < rows; r++) {
            double f = (r + 0.5) / rows;
            double l = Math.max(0.35, len * (1 - f * 0.85));
            box(c, zone, r * 2, r, -0.2, -(r + 1) * rh, sweep * f, 0.4, rh + 0.04, l);
        }
        k.part(c, x, y, z, xr, yr, zr);
    }

    // a lobe (a tail half, a pectoral fin): a thin plate `len` long along +z, `w0` wide at its root narrowing to `w1`,
    // curving back like a crescent by `curve`; turned by (xr, yr, zr)
    static void lobe(K k, Zone zone, double x, double y, double z, double len, double w0, double w1, double curve, double xr, double yr, double zr) {
        CubeListBuilder c = CubeListBuilder.create();
        int n = Math.max(2, (int) Math.ceil(len / 0.7));
        double seg = len / n;
        for (int j = 0; j < n; j++) {
            double f = (j + 0.5) / n;
            double w = w0 + (w1 - w0) * f;
            double off = curve * f * f;
            box(c, zone, j * 2, 0, -0.2, -w / 2 + off, j * seg, 0.4, w, seg + 0.05);
        }
        k.part(c, x, y, z, xr, yr, zr);
    }

    // the tail: an upper and a lower lobe off the root, `up` and `down` radians off the spine
    static void tail(K k, Fish f, Zone zone, double upLen, double downLen, double w0, double w1, double up, double down) {
        double tilt = f.tilt(1), y = f.y(1), z = f.z(1) - 0.4;
        lobe(k, zone, f.x, y, z, upLen, w0, w1, 0.5, tilt + up, 0, 0);
        lobe(k, zone, f.x, y, z, downLen, w0, w1, -0.5, tilt - down, 0, 0);
    }

    // pectoral fins, one each side, swept back and out (`out` radians) and down (`down`)
    static void pectorals(K k, Fish f, Zone zone, double s, double len, double w, double out, double down) {
        for (int side = -1; side <= 1; side += 2) {
            lobe(k, zone, f.x + side * f.t(s) * 0.8, f.y(s) + f.h(s) * 0.35, f.z(s), len, w, w * 0.35, 0.3, f.tilt(s) - down, side * out, 0);
        }
    }

    // eyes on both sides: a black bead and a white glint
    static void eyes(K k, Fish f, double s, double up, double size) {
        double y = f.y(s) - f.h(s) * up, z = f.z(s);
        double half = f.t(s) * (up > 0.4 ? 0.7 : 1.0);
        for (int side = -1; side <= 1; side += 2) {
            double x = f.x + side * (half + size * 0.2);
            k.cube(EYE, x, y, z, size);
            k.cube(WHITE, x + side * size * 0.45, y - size * 0.22, z - size * 0.22, size * 0.32);
        }
    }

    // ── the catches ──

    public static LayerDefinition create(RareCatch c) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        PartDefinition glowRoot = mesh.getRoot().addOrReplaceChild("glow", CubeListBuilder.create(), PartPose.ZERO);
        K g = new K(glowRoot);
        K k = new K(root);
        switch (c) {
            case GIANT_CATFISH -> catfish(k);
            case GOLDEN_KOI, GHOST_KOI -> koi(k);
            case PUFFERFISH_KING -> puffer(k);
            case BLOBFISH -> blobfish(k);
            case SWORDFISH -> swordfish(k);
            case OCEAN_SUNFISH -> sunfish(k);
            case ELECTRIC_EEL -> eel(k, g);
            case MAKO_SHARK -> mako(k);
            case ANGLERFISH -> angler(k, g);
            case KRAKEN_TENTACLE -> kraken(k);
            case OARFISH -> oarfish(k);
            case GOLDEN_LEVIATHAN_SCALE -> scale(k, g);
            default -> {
            }
        }
        k.done();
        g.done();
        return LayerDefinition.create(mesh, 64, 64);
    }

    // a broad flat-headed catfish, whiskers trailing from its mouth, a little arched
    static void catfish(K k) {
        Fish f = new Fish(3.0, 2.3);
        f.z = -6.9;
        f.len = 11.6;
        f.hp = prof(0.75, 0.24, 0.36);
        f.tp = prof(0.98, 0.18, 0.38);
        f.bend = s -> -0.8 * Math.sin(Math.PI * s) + 0.7 * s * s;
        body(k, f);
        fin(k, FIN, f.x, f.top(0.3) + 0.4, f.z(0.28), 2.4, 1.8, 1.0, f.tilt(0.3), 0, 0);
        fin(k, FIN, f.x, f.top(0.72) + 0.3, f.z(0.7), 1.4, 0.8, 0.4, f.tilt(0.72), 0, 0);
        tail(k, f, FIN, 3.4, 3.2, 2.4, 1.4, 0.45, 0.45);
        pectorals(k, f, FIN, 0.2, 2.8, 1.6, 0.9, 0.5);
        double my = f.y(0) + 0.6, mz = f.z(0);
        for (int side = -1; side <= 1; side += 2) {
            k.line(ACCENT2, f.x + side * 1.6, my, mz + 0.3, f.x + side * 3.0, my + 3.2, mz - 2.6, 0.32);
            k.line(ACCENT2, f.x + side * 2.0, my - 0.6, mz + 0.4, f.x + side * 3.4, my - 2.4, mz - 3.0, 0.32);
            k.line(ACCENT2, f.x + side * 1.0, my + 0.8, mz + 0.6, f.x + side * 1.4, my + 2.6, mz - 0.6, 0.26);
        }
        k.box(MOUTH, f.x - 1.8, my - 0.2, mz - 0.25, 3.6, 0.7, 0.5);
        eyes(k, f, 0.12, 0.42, 0.8);
    }

    // a koi curving up into a long flowing tail, barbels at its lips
    static void koi(K k) {
        Fish f = new Fish(2.7, 1.6);
        f.z = -6.5;
        f.len = 9.6;
        f.hp = prof(0.55, 0.38, 0.3);
        f.tp = prof(0.62, 0.3, 0.3);
        f.bend = s -> -1.0 * Math.sin(Math.PI * s) + 1.4 * s * s * s;
        body(k, f);
        fin(k, FIN, f.x, f.top(0.28) + 0.3, f.z(0.25), 4.0, 1.6, 1.4, f.tilt(0.3), 0, 0);
        tail(k, f, FIN, 4.4, 4.0, 2.4, 1.6, 0.75, 0.7);
        pectorals(k, f, FIN, 0.24, 2.6, 1.6, 0.8, 0.5);
        for (int side = -1; side <= 1; side += 2) {
            lobe(k, FIN, f.x + side * f.t(0.55) * 0.6, f.bottom(0.55) - 0.2, f.z(0.55), 1.8, 1.0, 0.4, 0.2, f.tilt(0.55) - 0.9, side * 0.4, 0);
            k.line(ACCENT2, f.x + side * 0.7, f.y(0) + 0.6, f.z(0) + 0.3, f.x + side * 1.3, f.y(0) + 1.9, f.z(0) - 1.2, 0.26);
        }
        k.box(MOUTH, f.x - 0.8, f.y(0) + 0.2, f.z(0) - 0.2, 1.6, 0.6, 0.4);
        eyes(k, f, 0.12, 0.3, 0.8);
    }

    // the Pufferfish King, puffed up and furious: long spikes all round, a red eye under a brow scowling down, a downturned
    // mouth, an old scar across his side, royal purple fins, a big jagged crown set with rubies and sapphires
    static void puffer(K k) {
        double cx = BOARD + 4.3, cy = -11.0, cz = 0.0, r = 3.8;
        for (double zz = -r + 0.5; zz < r; zz += 1.0) {
            double rr = Math.sqrt(Math.max(0.2, r * r - zz * zz));
            int du = (int) ((zz + r) * 3) % 12;
            k.box(SIDE, du, 0, cx - rr, cy - rr * 0.62, cz + zz - 0.5, 2 * rr, rr * 1.24, 1.0);
            k.box(BACK, du, 0, cx - rr * 0.7, cy - rr, cz + zz - 0.5, rr * 1.4, rr * 0.45, 1.0);
            k.box(BELLY, du, 0, cx - rr * 0.7, cy + rr * 0.55, cz + zz - 0.5, rr * 1.4, rr * 0.45, 1.0);
        }
        // the spikes: long, thick at the root, needle-thin at the tip, all round (not into the board)
        for (int lat = -2; lat <= 2; lat++) {
            double phi = lat * Math.PI / 6;
            int count = lat == 0 ? 12 : Math.abs(lat) == 1 ? 9 : 5;
            for (int j = 0; j < count; j++) {
                double th = j * 2 * Math.PI / count + lat * 0.4;
                double dx = Math.cos(phi) * Math.cos(th), dz = Math.cos(phi) * Math.sin(th), dy = Math.sin(phi);
                if (dx < -0.35) continue;
                if (dy < -0.8 && Math.abs(dz) < 0.5) continue;   // under the crown
                double l = (j + lat) % 2 == 0 ? 2.6 : 1.9;
                k.line(ACCENT2, cx + dx * (r - 0.4), cy + dy * (r - 0.4), cz + dz * (r - 0.4), cx + dx * (r + 0.9), cy + dy * (r + 0.9), cz + dz * (r + 0.9), 0.46);
                k.line(ACCENT2, cx + dx * (r + 0.8), cy + dy * (r + 0.8), cz + dz * (r + 0.8), cx + dx * (r + l), cy + dy * (r + l), cz + dz * (r + l), 0.26);
            }
        }
        double ez = cz - r * 0.62;
        for (int side = -1; side <= 1; side += 2) {
            double ex = cx + side * 2.75;
            // the eye: white, a red iris glaring forward, a heavy brow slanting down to the front
            k.cube(WHITE, ex, cy - 1.2, ez, 1.5);
            k.cube(EYE, ex + side * 0.45, cy - 1.1, ez - 0.35, 0.8);
            k.line(MOUTH, ex + side * 0.55, cy - 2.75, ez + 1.2, ex + side * 0.55, cy - 1.95, ez - 0.95, 0.55);
            lobe(k, FIN, cx + side * r * 0.9, cy + 0.8, cz - 0.6, 2.6, 2.0, 0.6, 0.3, -0.35, side * 1.15, 0);
        }
        // the scowl: a hard line, its corners turned down
        k.box(MOUTH, cx - 1.1, cy + 0.9, cz - r - 0.2, 2.2, 0.45, 0.6);
        k.box(MOUTH, cx - 1.3, cy + 1.2, cz - r - 0.05, 0.45, 0.6, 0.5);
        k.box(MOUTH, cx + 0.85, cy + 1.2, cz - r - 0.05, 0.45, 0.6, 0.5);
        // an old scar across his side
        k.line(ACCENT2, cx + 3.75, cy - 2.4, cz - 1.6, cx + 3.85, cy + 0.9, cz + 1.0, 0.3);
        k.line(ACCENT2, cx + 3.8, cy - 1.1, cz - 0.9, cx + 3.85, cy - 0.5, cz + 0.2, 0.24);
        // a royal purple tail
        lobe(k, FIN, cx, cy - 0.3, cz + r - 0.5, 2.6, 2.6, 1.6, 0.4, 0.35, 0, 0);
        lobe(k, FIN, cx, cy + 0.3, cz + r - 0.5, 2.6, 2.6, 1.6, -0.4, -0.35, 0, 0);
        // the crown: a broad gold band, jagged points tall and short all round, gems set in it
        double top = cy - r + 0.25;
        k.box(ACCENT, cx - 2.2, top - 1.0, cz - 2.2, 4.4, 1.0, 4.4);
        k.box(ACCENT, cx - 2.35, top - 0.5, cz - 2.35, 4.7, 0.5, 4.7);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            double px = cx + Math.cos(a) * 1.9, pz = cz + Math.sin(a) * 1.9;
            double ph = i % 2 == 0 ? 1.5 : 0.8;
            k.box(ACCENT, px - 0.35, top - 1.0 - ph, pz - 0.35, 0.7, ph, 0.7);
            k.cube(ACCENT, px, top - 1.15 - ph, pz, 0.4);
        }
        k.cube(DARK, cx + 2.3, top - 0.5, cz, 0.7);
        k.cube(BAND, cx + 2.3, top - 0.5, cz - 1.3, 0.55);
        k.cube(BAND, cx + 2.3, top - 0.5, cz + 1.3, 0.55);
        k.cube(DARK, cx, top - 0.5, cz - 2.3, 0.7);
        k.cube(DARK, cx, top - 0.5, cz + 2.3, 0.7);
        
    }
    // a saggy pink blob: a droopy nose hanging over a sad little mouth, a floppy tail
    static void blobfish(K k) {
        double cx = BOARD + 3.7;
        for (int i = 0; i < 9; i++) {
            double u = i / 8.0, z0 = -5.0 + i;
            double hh = 3.2 * (1 - u * 0.7), tt = 2.8 * (1 - u * 0.55), cy = -10.4 + u * 0.8;
            int du = (i * 5) % 16;
            k.box(BACK, du, 0, cx - tt * 0.75, cy - hh, z0, tt * 1.5, hh, 1.05);
            k.box(SIDE, du, 0, cx - tt - 0.3, cy - hh * 0.3, z0, 2 * tt + 0.6, hh * 0.95, 1.05);
            k.box(BELLY, du, 0, cx - tt * 0.9, cy + hh * 0.4, z0, tt * 1.8, hh * 0.55, 1.05);
        }
        // the nose: a droopy lump over the front
        k.box(ACCENT, cx - 1.3, -10.9, -6.2, 2.6, 2.2, 1.6);
        k.box(ACCENT, cx - 1.5, -9.2, -6.5, 3.0, 1.7, 1.9);
        // the sad mouth: a line, its corners down
        k.box(DARK, cx - 1.7, -7.4, -5.6, 3.4, 0.35, 0.5);
        k.box(DARK, cx - 1.9, -7.2, -5.4, 0.4, 0.6, 0.4);
        k.box(DARK, cx + 1.5, -7.2, -5.4, 0.4, 0.6, 0.4);
        for (int side = -1; side <= 1; side += 2) {
            k.cube(EYE, cx + side * 2.0, -12.0, -4.4, 0.75);
            k.cube(WHITE, cx + side * 2.3, -12.2, -4.8, 0.3);
            lobe(k, FIN, cx + side * 2.4, -9.0, -2.0, 1.8, 1.2, 0.5, 0.3, -0.7, side * 0.9, 0);
        }
        lobe(k, FIN, cx, -9.4, 3.6, 2.4, 1.8, 1.2, 0.8, -0.5, 0, 0);
    }

    // a swordfish leaping: its long bill out in front, a tall sail, a big crescent tail
    static void swordfish(K k) {
        Fish f = new Fish(3.0, 1.8);
        f.z = 1.0;
        f.len = 17.0;
        f.hp = prof(0.45, 0.3, 0.2);
        f.tp = prof(0.55, 0.3, 0.3);
        f.bend = s -> -1.6 * Math.sin(Math.PI * s) + 1.4 * s * s;
        body(k, f);
        double by = f.y(0) + 0.1, bz = f.z(0) + 0.6;
        k.line(ACCENT, f.x, by, bz, f.x, by - 0.9, bz - 6.0, 0.8);
        k.line(ACCENT, f.x, by - 0.9, bz - 6.0, f.x, by - 1.4, bz - 9.2, 0.5);
        k.line(ACCENT, f.x, by + 0.8, bz, f.x, by + 0.6, bz - 2.0, 0.5);
        fin(k, FIN, f.x, f.top(0.16) + 0.4, f.z(0.12), 3.2, 4.6, 2.4, f.tilt(0.15), 0, 0);
        fin(k, FIN, f.x, f.top(0.86) + 0.2, f.z(0.84), 0.8, 0.8, 0.3, f.tilt(0.86), 0, 0);
        tail(k, f, FIN, 5.4, 4.2, 1.8, 0.5, 0.95, 0.9);
        pectorals(k, f, FIN, 0.2, 4.0, 1.1, 0.7, 0.7);
        eyes(k, f, 0.07, 0.15, 1.0);
    }

    // a tall grey disc: a fin straight up and one down behind, its ruffled stub of a tail, a little round mouth
    static void sunfish(K k) {
        Fish f = new Fish(3.9, 1.3);
        f.z = 3.0;
        f.len = 10.5;
        f.y = -11.2;
        f.hp = prof(0.7, 0.45, 0.85);
        f.tp = prof(0.7, 0.4, 0.6);
        body(k, f);
        fin(k, FIN, f.x, f.top(0.78) + 0.4, f.z(0.72), 2.4, 4.2, 1.4, 0, 0, 0);
        fin(k, FIN, f.x, f.bottom(0.78) - 0.4, f.z(0.72) + 2.4, 2.4, 3.0, 1.0, Math.PI + 0.45, 0, 0);
        for (int j = -3; j <= 3; j++) k.cube(FIN, f.x, f.y(1) + j * 1.05, f.z(1) + 0.3 + (j % 2 == 0 ? 0.3 : 0), 1.0);
        pectorals(k, f, FIN, 0.3, 1.8, 1.2, 1.0, 0.2);
        k.box(MOUTH, f.x - 0.6, f.y(0) + 0.6, f.z(0) - 0.4, 1.2, 1.0, 0.6);
        eyes(k, f, 0.14, 0.18, 1.0);
    }

    // an eel writhing in an S, yellow bands down it, a fin strip along its belly, sparks crackling off it
    static void eel(K k, K g) {
        Fish f = new Fish(1.25, 1.0);
        f.z = -6.8;
        f.len = 13.8;
        f.step = 0.8;
        f.hp = prof(0.85, 0.15, 0.35);
        f.tp = prof(0.9, 0.15, 0.4);
        f.bend = s -> 2.2 * Math.sin(2.2 * Math.PI * s + 0.3);
        f.side = i -> i % 4 == 2 ? BAND : SIDE;
        f.keel = 0.6;
        f.keelFrom = 0.3;
        body(k, f);
        tail(k, f, FIN, 1.4, 1.4, 1.2, 0.5, 0.4, 0.4);
        k.box(MOUTH, f.x - 0.6, f.y(0) + 0.2, f.z(0) - 0.2, 1.2, 0.5, 0.4);
        eyes(k, f, 0.06, 0.3, 0.55);
        double[][] sparks = {{0.2, -2.2}, {0.45, 2.3}, {0.65, -2.0}, {0.85, 2.0}, {0.32, 2.0}};
        for (double[] s : sparks) {
            double sy = f.y(s[0]) + s[1], sz = f.z(s[0]);
            g.line(GLOW, f.x + 0.6, sy, sz, f.x + 0.9, sy + (s[1] > 0 ? 0.9 : -0.9), sz + 0.6, 0.22);
            g.cube(GLOW, f.x + 0.9, sy + (s[1] > 0 ? 0.9 : -0.9), sz + 0.6, 0.3);
        }
    }

    // a mako leaping, jaws open on rows of teeth, gill slits, a tall fin, a crescent tail
    static void mako(K k) {
        Fish f = new Fish(3.4, 2.4);
        f.z = -6.0;
        f.len = 22.5;
        f.hp = prof(0.3, 0.38, 0.2);
        f.tp = prof(0.35, 0.35, 0.3);
        f.bend = s -> -1.3 * Math.sin(Math.PI * s) + 1.0 * s * s;
        body(k, f);
        // the lower jaw, hinged under the eye and dropped open; the dark of the mouth; teeth on both jaws
        double hs = 0.17, hy = f.y(hs) + f.h(hs) * 0.3, hz = f.z(hs), jt = f.t(0.1) * 0.8;
        CubeListBuilder jaw = CubeListBuilder.create();
        box(jaw, BELLY, 0, 0, -jt, 0.0, -3.4, 2 * jt, 0.9, 3.4);
        for (int i = 0; i < 5; i++) {
            box(jaw, WHITE, 0, 0, -jt + 0.1, -0.45, -3.2 + i * 0.7, 0.3, 0.5, 0.3);
            box(jaw, WHITE, 0, 0, jt - 0.4, -0.45, -3.2 + i * 0.7, 0.3, 0.5, 0.3);
        }
        k.part(jaw, f.x, hy, hz, f.tilt(hs) + 0.42, 0, 0);
        k.box(MOUTH, f.x - jt * 0.9, hy - 0.3, f.z(0.03), jt * 1.8, 1.3, hz - f.z(0.03));
        for (int i = 0; i < 5; i++) {
            double s = 0.03 + i * 0.028;
            for (int side = -1; side <= 1; side += 2) k.box(WHITE, f.x + side * f.t(s) * 0.7 - 0.15, f.y(s) + f.h(s) * 0.28, f.z(s), 0.3, 0.55, 0.3);
        }
        for (int i = 0; i < 4; i++) {
            double s = 0.21 + i * 0.024;
            for (int side = -1; side <= 1; side += 2) k.box(DARK, f.x + side * (f.t(s) + 0.02) - 0.08, f.y(s) - f.h(s) * 0.35, f.z(s), 0.16, f.h(s) * 0.75, 0.3);
        }
        fin(k, FIN, f.x, f.top(0.38) + 0.4, f.z(0.35), 4.2, 4.4, 2.4, f.tilt(0.38), 0, 0);
        fin(k, FIN, f.x, f.top(0.8) + 0.2, f.z(0.79), 1.0, 1.0, 0.4, f.tilt(0.8), 0, 0);
        tail(k, f, FIN, 6.0, 4.6, 1.9, 0.4, 1.0, 0.95);
        pectorals(k, f, FIN, 0.3, 5.0, 2.0, 0.6, 0.75);
        eyes(k, f, 0.11, 0.25, 0.9);
    }

    // an anglerfish gaping: a jutting jaw of needle teeth, its lure glowing out in front on a stalk
    static void angler(K k, K g) {
        Fish f = new Fish(3.7, 2.8);
        f.z = -4.6;
        f.len = 9.4;
        f.hp = prof(0.9, 0.3, 0.3);
        f.tp = prof(0.85, 0.3, 0.35);
        f.bend = s -> -0.4 * Math.sin(Math.PI * s);
        body(k, f);
        double hs = 0.22, hy = f.y(hs) + f.h(hs) * 0.35, hz = f.z(hs), jt = f.t(0.15) * 0.92;
        CubeListBuilder jaw = CubeListBuilder.create();
        box(jaw, BELLY, 0, 0, -jt, 0.0, -4.0, 2 * jt, 1.4, 4.0);
        for (int i = 0; i < 6; i++) {
            box(jaw, WHITE, 0, 0, -jt + 0.15, -1.1, -3.8 + i * 0.65, 0.25, 1.15, 0.25);
            box(jaw, WHITE, 0, 0, jt - 0.4, -1.1, -3.5 + i * 0.65, 0.25, 1.15, 0.25);
        }
        k.part(jaw, f.x, hy, hz, f.tilt(hs) + 0.5, 0, 0);
        k.box(MOUTH, f.x - jt * 0.85, hy - 0.6, f.z(0.0) - 0.2, jt * 1.7, 2.0, hz - f.z(0.0));
        for (int i = 0; i < 6; i++) {
            double z = f.z(0.0) + 0.1 + i * 0.6;
            for (int side = -1; side <= 1; side += 2) k.box(WHITE, f.x + side * jt * 0.75 - 0.12, hy - 0.7, z, 0.25, 1.0, 0.25);
        }
        // the lure: a stalk off the top of the head, arching forward, a glowing bulb on its end
        double sx = f.x, sy = f.top(0.16), sz = f.z(0.16);
        k.line(ACCENT2, sx, sy + 0.3, sz, sx, -16.6, sz - 1.4, 0.35);
        k.line(ACCENT2, sx, -16.6, sz - 1.4, sx, -15.6, sz - 4.0, 0.3);
        g.cube(GLOW, sx, -15.0, sz - 4.3, 1.5);
        g.cube(GLOW, sx, -15.0, sz - 4.3, 0.9);
        fin(k, FIN, f.x, f.top(0.55) + 0.3, f.z(0.52), 2.6, 1.2, 0.8, f.tilt(0.55), 0, 0);
        tail(k, f, FIN, 2.6, 2.4, 2.0, 1.2, 0.5, 0.5);
        pectorals(k, f, FIN, 0.5, 2.2, 1.6, 1.1, 0.3);
        eyes(k, f, 0.3, 0.6, 0.75);
    }

    // a great tentacle coming out of the board on the left, sweeping out and curling round on itself, pale suckers
    // down the inside of the curl
    static void kraken(K k) {
        int n = 36;
        double zc = 8.5, yc = -10.8;
        double[][] p = new double[n + 1][4];
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            double a = Math.PI * 0.92 + t * 2.6 * Math.PI;
            double rad = 7.0 * (1 - 0.8 * t);
            p[i][0] = BOARD + 0.4 + 4.6 * Math.sin(Math.PI * Math.min(1, t * 1.15)) * (1 - 0.3 * t) + 1.4 * t;
            p[i][1] = yc - rad * Math.sin(a) * 0.55;
            p[i][2] = zc + rad * Math.cos(a);
            p[i][3] = 2.2 * (1 - 0.78 * t) + 0.3;
        }
        // where it comes out of the board: a ring of flesh
        k.box(SIDE, BOARD, p[0][1] - p[0][3] - 0.6, p[0][2] - p[0][3] - 0.6, 1.2, 2 * p[0][3] + 1.2, 2 * p[0][3] + 1.2);
        for (int i = 0; i < n; i++) {
            double[] a = p[i], b = p[i + 1];
            double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2], len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double r = (a[3] + b[3]) / 2;
            CubeListBuilder c = CubeListBuilder.create();
            int du = (i * 5) % 16;
            box(c, SIDE, du, 0, -r, -r * 0.72, 0, 2 * r, r * 1.44, len + 0.15);
            box(c, BACK, du, 0, -r * 0.72, -r, 0, r * 1.44, r * 2, len + 0.15);
            k.part(c, a[0], a[1], a[2], -Math.asin(dy / len), Math.atan2(dx, dz), 0);
            // a sucker every other slice, on the side towards the middle of the curl
            if (i % 2 == 0 && i > 1) {
                double iy = yc - a[1], iz = zc - a[2], il = Math.max(0.01, Math.sqrt(iy * iy + iz * iz));
                double sr = Math.max(0.45, r * 0.5);
                double sy = a[1] + iy / il * r * 0.9, sz = a[2] + iz / il * r * 0.9, sx = a[0] + r * 0.35;
                k.cube(WHITE, sx, sy, sz, sr * 1.3);
                k.cube(MOUTH, sx + sr * 0.35, sy, sz, sr * 0.7);
            }
        }
    }

    // an oarfish undulating the whole length of the board: a silver ribbon, its red crest all down its back, red plumes
    // streaming back off its head, long red rays hanging under it
    static void oarfish(K k) {
        Fish f = new Fish(2.0, 0.8);
        f.z = -21.8;
        f.len = 42.0;
        f.step = 1.2;
        f.hp = prof(0.85, 0.1, 0.25);
        f.tp = prof(0.9, 0.1, 0.3);
        f.bend = s -> 1.6 * Math.sin(2.4 * Math.PI * s);
        f.crest = 1.0;
        body(k, f);
        double hy = f.top(0.03), hz = f.z(0.03);
        double[][] plumes = {{-4.6, 2.0}, {-5.4, 3.2}, {-5.0, 4.6}, {-3.8, 5.8}, {-2.6, 6.4}};
        for (double[] pl : plumes) k.line(ACCENT, f.x, hy + 0.2, hz, f.x, hy + pl[0], hz + pl[1], 0.34);
        for (int side = -1; side <= 1; side += 2) {
            k.line(ACCENT, f.x + side * 0.4, f.bottom(0.04) - 0.2, f.z(0.04), f.x + side * 0.9, f.bottom(0.04) + 3.4, f.z(0.04) + 3.6, 0.3);
        }
        lobe(k, ACCENT, f.x, f.y(1), f.z(1) - 0.2, 1.6, 1.0, 0.3, 0.2, f.tilt(1), 0, 0);
        k.box(MOUTH, f.x - 0.4, f.y(0) + 0.1, f.z(0) - 0.2, 0.8, 0.5, 0.4);
        eyes(k, f, 0.025, 0.15, 0.75);
    }

    // the Golden Leviathan Scale: a jagged shard of black-gold armour torn from something in the abyss, standing in a
    // black iron claw on a little shelf: a cruel point, hooked barbs down both edges, ridged in layers, and down its face
    // a molten slit like an eye opening (it glows)
    static void scale(K k, K g) {
        // the shelf and the claw that holds it: black iron, spiked
        k.box(ACCENT2, BOARD, -7.9, -3.6, 3.8, 0.7, 7.2);
        k.box(ACCENT2, BOARD + 0.4, -9.0, -1.8, 3.0, 1.1, 3.6);
        for (int sz = -1; sz <= 1; sz += 2) {
            k.line(ACCENT2, -4.2, -8.8, sz * 1.6, -4.0, -11.2, sz * 2.4, 0.5);
            k.line(ACCENT2, -4.0, -11.1, sz * 2.4, -4.0, -11.9, sz * 1.8, 0.35);
            k.line(ACCENT2, BOARD + 3.6, -7.3, sz * 3.4, BOARD + 4.4, -6.2, sz * 4.2, 0.4);
        }
        k.line(ACCENT2, -2.9, -8.9, 0.0, -2.1, -10.6, 0.0, 0.45);
        // the shard, leaning back against the board
        double[] widths = {3.0, 4.4, 5.3, 5.8, 5.8, 5.3, 4.6, 3.7, 2.8, 1.8, 1.0, 0.45};
        double rh = 0.8, th = 1.7;
        CubeListBuilder c = CubeListBuilder.create();
        CubeListBuilder eye = CubeListBuilder.create();
        for (int i = 0; i < widths.length; i++) {
            double w = widths[i], y = -(i + 1) * rh;
            double jag = (i % 2 == 0 ? 0.25 : -0.15);
            box(c, ACCENT, (i * 3) % 10, 0, -th / 2, y, -w / 2 + jag, th, rh + 0.03, w);
            // a ridge down the middle of each layer, front and back (the layers alternate darker)
            if (w > 1.4) {
                box(c, i % 2 == 0 ? ACCENT : BAND, (i * 5) % 10, 2, th / 2 - 0.05, y, -(w - 1.3) / 2 + jag, 0.4, rh * 0.7, w - 1.3);
                box(c, BAND, (i * 7) % 10, 2, -th / 2 - 0.3, y, -(w - 1.6) / 2, 0.35, rh * 0.7, w - 1.6);
            }
            // hooked barbs down both edges, pointing down and out
            if (i >= 1 && i <= 8 && i % 2 == 1) {
                for (int side = -1; side <= 1; side += 2) {
                    double ez = side * (w / 2) + jag;
                    box(c, ACCENT, 0, 0, -0.35, y, side > 0 ? ez : ez - 0.9, 0.7, 0.45, 0.9);
                    box(c, ACCENT, 0, 0, -0.25, y + 0.35, side > 0 ? ez + 0.6 : ez - 0.95, 0.5, 0.6, 0.35);
                }
            }
        }
        // the slit: a glowing almond with a black pupil down it
        double fx = th / 2 + 0.38;
        double[][] slit = {{1, 0.5}, {2, 1.1}, {3, 1.6}, {4, 1.8}, {5, 1.8}, {6, 1.6}, {7, 1.1}, {8, 0.5}};
        for (double[] sl : slit) box(eye, GLOW, 0, 0, fx, -(sl[0] + 1) * rh, -sl[1] / 2, 0.12, rh + 0.02, sl[1]);
        box(c, DARK, 0, 0, fx + 0.1, -8.4 * rh, -0.15, 0.12, 6.0 * rh, 0.3);
        k.part(c, -4.4, -9.0, 0.0, 0, 0, -0.16);
        g.part(eye, -4.4, -9.0, 0.0, 0, 0, -0.16);
    }}

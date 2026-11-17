package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

// The Pirate Ship (boats/PirateShip, Boats.md): a pirate galleon built out of about a thousand little boxes. Model
// pixels (16 to a block): +x is the bow, +z is port (left), y is DOWN, and y = 0 is about the waterline.
//  - The hull, slice by slice (1 px along its length): a curved, tapering shape with a pointed bow and a flat stern,
//    black bulwarks lined in red, a red waterline band, gold stripes; a raised forecastle (bow) and quarterdeck (stern).
//  - Three masts (fore, main, mizzen) with fighting tops; square sails (tattered, black; a skull and crossbones on the
//    main course, crossed cutlasses on the fore course) that billow more the faster it goes; a gaff sail on the mizzen,
//    two jibs out to the bowsprit; a Jolly Roger and two red pennants that wave; shrouds with ratlines, and stays.
//  - Six cannons through open gunports, a ship's wheel that turns when you steer, the stern gallery (glowing windows,
//    gold carving), quarter galleries, lanterns that glow at night (the glow pass: PremiumBoatRenderer), a treasure
//    chest, barrels, a ship's bell, anchors at the catheads, a gold skull figurehead, a hatch grating, the rudder.
// The texture is drawn by tools/make_boats.ps1 (tools/PirateShipTex.cs) at 4x (1024 x 1024 for this 256 x 256 layout): every box takes its
// texture from one of the ZONES below (the tool paints the same ones), and the sails, flag and stern take theirs from
// places worked out to the pixel so their pictures line up across the boxes.
public class PirateShipModel extends EntityModel<BoatRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pirate_ship"), "main");

    // ── texture zones (u, v, width, height) in the 256 x 256 layout: keep tools/PirateShipTex.cs the same ──
    record Zone(int u, int v, int w, int h) {
    }

    static final Zone DARK = new Zone(0, 0, 96, 40);     // black hull planks, red waterline band (rows: y = row - 24)
    static final Zone WOOD = new Zone(96, 0, 96, 40);    // brown planks (rows: y = row - 20)
    static final Zone GOLD = new Zone(192, 0, 64, 24);
    static final Zone RED = new Zone(192, 24, 64, 16);   // red planks (inside the bulwarks, gunport lids, pennants)
    static final Zone DECK = new Zone(0, 40, 128, 40);   // deck planks (rows: z = 12 - row; columns: x = column - 50)
    static final Zone MAST = new Zone(128, 40, 32, 80);  // upright spar wood
    static final Zone SPAR = new Zone(160, 40, 84, 36);  // lengthwise spar wood
    static final Zone ROPE = new Zone(244, 40, 12, 84);
    static final Zone IRON = new Zone(160, 76, 28, 24);
    static final Zone BLACK = new Zone(188, 76, 28, 24);
    static final Zone GLASS = new Zone(216, 76, 28, 24); // warm lantern glass (glows)
    static final Zone BONE = new Zone(160, 100, 28, 20);
    static final Zone GRATE = new Zone(188, 100, 56, 20);
    static final Zone CLOTH = new Zone(0, 80, 128, 40);  // black sail cloth (jibs, gaff sail)
    // the square sails' pictures: a block 2 x the sail's width from SAIL_U, rows from SAIL_V (see sail())
    static final int SAIL_V = 124;
    static final int MAIN_COURSE_U = 0;     // 30 wide, 14 tall: the skull and crossbones
    static final int FORE_COURSE_U = 60;    // 24 x 12: crossed cutlasses
    static final int MAIN_TOPSAIL_U = 108;  // 24 x 14
    static final int FORE_TOPSAIL_U = 156;  // 18 x 10
    static final int FLAG_U = 192;          // the Jolly Roger, 16 x 10 (18 columns: its back shows columns 1 to 17)
    static final int FLAG_V = 124;
    static final int STERN_U = 0;           // the stern gallery's picture, 18 x 15 at (0, 160)
    static final int STERN_V = 160;

    // ── the hull's shape ──
    static final int STERN_X = -26;         // the first slice (its back face is the stern)
    static final int BOW_X = 29;            // the last slice
    static final int QUARTER_END = -10;     // the quarterdeck: slices up to here (its front edge at x = -9)
    static final int FORECASTLE_START = 15; // the forecastle: slices from here
    static final float MAIN_DECK = -1;
    static final float FORE_DECK = -5;
    static final float QUARTER_DECK = -7;

    // half the hull's width at slice x (outside of the bulwarks), in whole pixels
    static int halfWidth(int x) {
        double xc = x + 0.5;
        double hw;
        if (xc <= -12) hw = 11 - 2.5 * Math.pow((-12 - xc) / 14.0, 2);
        else if (xc <= 12) hw = 11;
        else {
            double t = Math.min(1.0, (xc - 12) / 18.0);
            hw = 11 * Math.pow(1 - Math.pow(t, 2.2), 0.55);
        }
        return Math.max(1, (int) Math.round(hw));
    }

    // the top of the bulwarks at slice x (higher on the forecastle and the quarterdeck)
    static int wallTop(int x) {
        if (x >= FORECASTLE_START) return -9 - (int) Math.round(2 * Math.pow((x - FORECASTLE_START) / 14.0, 2));
        if (x <= QUARTER_END) return -11 - (int) Math.round(2 * Math.pow((QUARTER_END - x) / 16.0, 2));
        return -6;
    }

    static float deckAt(int x) {
        return x >= FORECASTLE_START ? FORE_DECK : x <= QUARTER_END ? QUARTER_DECK : MAIN_DECK;
    }

    // ── the moving parts ──
    private record SailRow(ModelPart mid, ModelPart port, ModelPart starboard, float x, float half, float bulge) {
    }

    private record Strip(ModelPart part, float reach) {
    }

    protected final ModelPart ship;
    private final List<SailRow> sails = new ArrayList<>();
    private final List<Strip> flag = new ArrayList<>();
    private final List<Strip> pennants = new ArrayList<>();
    private final ModelPart wheel;
    private final ModelPart mainTrunk;
    private final ModelPart foreTrunk;

    public PirateShipModel(ModelPart root) {
        // faces drawn one side only: the sails, flag and pennants are flat, their back and front pictures in the same
        // plane (drawing both sides makes them flicker against each other)
        this(root, RenderTypes::entityCutoutCull);
    }

    // (the Spectral Galleon draws the same ship see-through: GhostShipModel)
    protected PirateShipModel(ModelPart root, java.util.function.Function<Identifier, net.minecraft.client.renderer.rendertype.RenderType> renderType) {
        super(root, renderType);
        this.ship = root.getChild("ship");
        for (SailSpec spec : SAILS) {
            ModelPart part = this.ship.getChild(spec.name);
            for (int j = 0; j < spec.rows; j++) {
                float bulge = spec.bulge * Mth.sin((float) Math.PI * (j + 0.5F) / spec.rows);
                this.sails.add(new SailRow(part.getChild("m" + j), part.getChild("p" + j), part.getChild("s" + j), spec.x(), spec.width / 3, bulge));
            }
        }
        ModelPart flagPart = this.ship.getChild("flag");
        for (int i = 0; i < FLAG_STRIPS; i++) this.flag.add(new Strip(flagPart.getChild("f" + i), i / (float) (FLAG_STRIPS - 1)));
        for (String name : new String[]{"fore_pennant", "mizzen_pennant"}) {
            ModelPart pennant = this.ship.getChild(name);
            for (int i = 0; i < PENNANT_STRIPS; i++) this.pennants.add(new Strip(pennant.getChild("f" + i), i / (float) (PENNANT_STRIPS - 1)));
        }
        this.wheel = this.ship.getChild("wheel");
        this.mainTrunk = this.ship.getChild("main_trunk");
        this.foreTrunk = this.ship.getChild("fore_trunk");
    }

    // The square sails: which mast (its x and half its thickness), the top row's height, how many 2 px rows, the width
    // (a multiple of 3), where its picture is (u, at SAIL_V) and how far it billows forward at most
    private record SailSpec(String name, float mastX, float mastHalf, float topY, int rows, int width, int u, float bulge) {
        float x() {
            return this.mastX + this.mastHalf + 0.8F;
        }
    }

    static final float MAIN_X = -3.0F;
    static final float FORE_X = 16.0F;
    static final float MIZZEN_X = -25.0F;
    private static final List<SailSpec> SAILS = List.of(
        new SailSpec("main_course", MAIN_X, 1.5F, -40.0F, 7, 30, MAIN_COURSE_U, 4.0F),
        new SailSpec("main_topsail", MAIN_X, 1.0F, -64.0F, 7, 24, MAIN_TOPSAIL_U, 3.2F),
        new SailSpec("fore_course", FORE_X, 1.25F, -40.0F, 6, 24, FORE_COURSE_U, 3.4F),
        new SailSpec("fore_topsail", FORE_X, 1.0F, -60.0F, 5, 18, FORE_TOPSAIL_U, 2.6F));
    private static final int FLAG_STRIPS = 16;
    private static final int PENNANT_STRIPS = 12;

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        PremiumBoatRenderer.State ship = state instanceof PremiumBoatRenderer.State s ? s : null;
        float speed = ship == null ? 0.0F : ship.speed;
        float turn = ship == null ? 0.0F : ship.turn;
        // a gentle roll and pitch on the water
        if (!state.isUnderWater) {
            this.ship.xRot = 0.02F * Mth.sin(t * 0.07F);
            this.ship.zRot = 0.012F * Mth.sin(t * 0.05F + 1.3F);
        }
        // the sails fill with the wind of its speed (and flutter a little)
        int k = 0;
        for (SailRow row : this.sails) {
            float fill = 0.45F + 0.55F * speed + 0.05F * Mth.sin(t * 0.21F + k * 0.7F);
            float b = row.bulge * fill;
            row.mid.x = row.x + b;
            row.port.x = row.x + b;
            row.starboard.x = row.x + b;
            float angle = (float) Math.atan2(b * 0.7F, row.half);
            row.port.yRot = -angle;
            row.starboard.yRot = angle;
            k++;
        }
        // the Jolly Roger and the pennants stream back and ripple (harder when it's moving)
        float wave = 0.5F + 0.9F * speed;
        wave(this.flag, t, wave, 0.33F, 0.55F);
        wave(this.pennants, t, wave * 0.8F, 0.45F, 0.7F);
        // the wheel turns as it steers
        this.wheel.xRot = Mth.clamp(turn, -6.0F, 6.0F) * 0.35F;
        // riding it in first person: the lower masts in front of your seat are out of the way (NaN: not riding)
        float viewer = ship == null ? Float.NaN : ship.viewerX;
        this.mainTrunk.visible = !(viewer < MAIN_X);
        this.foreTrunk.visible = !(viewer < FORE_X);
    }

    private static void wave(List<Strip> strips, float t, float amplitude, float rate, float step) {
        for (int i = 0; i < strips.size(); i++) {
            Strip strip = strips.get(i);
            float phase = t * rate - (i % FLAG_STRIPS) * step;
            strip.part.z = amplitude * strip.reach * Mth.sin(phase);
            strip.part.yRot = -amplitude * 0.35F * strip.reach * Mth.cos(phase);
        }
    }

    // ═══════════════════════════════ building it ═══════════════════════════════

    // Static boxes go into a few big parts (one per texture "set" is not needed: each box has its own offset)
    static final class Builder {
        final PartDefinition ship;
        final CubeListBuilder statics = CubeListBuilder.create();
        int count;

        Builder(PartDefinition ship) {
            this.ship = ship;
        }

        // a box textured from a zone, offset (du, dv) into it (kept inside it)
        void box(Zone zone, int du, int dv, float x, float y, float z, float w, float h, float d) {
            box(this.statics, zone, du, dv, x, y, z, w, h, d);
        }

        static void box(CubeListBuilder builder, Zone zone, int du, int dv, float x, float y, float z, float w, float h, float d) {
            int fw = (int) Math.ceil(2 * (w + d));
            int fh = (int) Math.ceil(d + h);
            du = Math.max(0, Math.min(du, zone.w - fw));
            dv = Math.max(0, Math.min(dv, zone.h - fh));
            builder.texOffs(zone.u + du, zone.v + dv).addBox(x, y, z, w, h, d, BoatKit.nudge(x, y, z, w, h, d, false));
        }


        PartDefinition part(CubeListBuilder cubes, PartPose pose) {
            return this.ship.addOrReplaceChild("p" + (this.count++), cubes, pose);
        }

        // a straight line (rope or spar) from a to b, this thick, from a zone
        void line(Zone zone, float ax, float ay, float az, float bx, float by, float bz, float thick) {
            float dx = bx - ax;
            float dy = by - ay;
            float dz = bz - az;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01F) return;
            dx /= len;
            dy /= len;
            dz /= len;
            // the box runs along -y; turn it (x then z) to point from a to b
            float xRot = (float) Math.asin(-dz);
            float zRot = (float) Math.atan2(dx, -dy);
            CubeListBuilder cubes = CubeListBuilder.create();
            cubes.texOffs(zone.u, zone.v).addBox(-thick / 2, -len, -thick / 2, thick, len, thick, BoatKit.nudge(ax, ay, az, thick, len, thick, true));
            this.part(cubes, PartPose.offsetAndRotation(ax, ay, az, xRot, 0.0F, zRot));
        }

        void rope(float ax, float ay, float az, float bx, float by, float bz) {
            this.line(ROPE, ax, ay, az, bx, by, bz, 0.3F);
        }
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(createMesh(), 256, 256);
    }

    // the ship's parts (the Spectral Galleon adds its ghost to them: GhostShipModel)
    static MeshDefinition createMesh() {
        BoatKit.resetNudges();
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition ship = root.addOrReplaceChild("ship", CubeListBuilder.create(), PartPose.ZERO);
        Builder b = new Builder(ship);
        hull(b);
        castles(b);
        stern(b);
        bow(b);
        deckThings(b);
        cannons(b);
        masts(b);
        rigging(b);
        ship.addOrReplaceChild("hull", b.statics, PartPose.ZERO);
        return mesh;
    }

    // ── the hull: slice by slice ──
    private static void hull(Builder b) {
        for (int x = STERN_X; x <= BOW_X; x++) {
            int w = halfWidth(x);
            int prev = x > STERN_X ? halfWidth(x - 1) : w;
            int next = x < BOW_X ? halfWidth(x + 1) : 0;
            // thick enough to close the steps where the hull narrows
            int thick = Math.max(1, Math.max(Math.abs(w - prev), Math.abs(w - next)));
            thick = Math.min(thick, w);
            int top = wallTop(x);
            int h = (int) (MAIN_DECK - top);
            int i = x - STERN_X;
            // bulwarks (black, rows lined up with the height: dv = 24 + top - depth), port and starboard
            int dv = 24 + top - thick;
            b.box(DARK, (i * 3) % 60, dv, x, top, w - thick, 1, h, thick);
            b.box(DARK, (i * 3 + 30) % 60, dv, x, top, -w, 1, h, thick);
            // red lining inside them
            float lining = 0.2F;
            if (w - thick > 1) {
                b.box(RED, (i * 2) % 40, 0, x, top + 0.5F, w - thick - lining, 1, h - 0.5F, lining);
                b.box(RED, (i * 2 + 20) % 40, 0, x, top + 0.5F, -w + thick, 1, h - 0.5F, lining);
            }
            // rail caps (gold on the castles), a touch wider than the wall
            boolean castle = x >= FORECASTLE_START || x <= QUARTER_END;
            Zone cap = castle ? GOLD : SPAR;
            b.box(cap, (i * 2) % 40, 0, x, top - 0.5F, w - thick - 0.25F, 1, 0.5F, thick + 0.5F);
            b.box(cap, (i * 2 + 7) % 40, 0, x, top - 0.5F, -w - 0.25F, 1, 0.5F, thick + 0.5F);
            // gold stripes along the side: below the rail and above the waterline band
            b.box(GOLD, (i * 2) % 50, 4, x, top + 1.5F, w, 1, 0.5F, 0.12F);
            b.box(GOLD, (i * 2) % 50, 4, x, top + 1.5F, -w - 0.12F, 1, 0.5F, 0.12F);
            b.box(GOLD, (i * 2 + 9) % 50, 8, x, -1.6F, w, 1, 0.45F, 0.12F);
            b.box(GOLD, (i * 2 + 9) % 50, 8, x, -1.6F, -w - 0.12F, 1, 0.45F, 0.12F);
            // the solid hull below the deck: the waterline band (dark red), then brown planks, narrowing to the keel
            b.box(DARK, (i * 2) % 50, 24 - 1 - 2 * w, x, MAIN_DECK, -w, 1, 3, 2 * w);
            if ((x - STERN_X) % 2 == 0) {
                int w1 = Math.max(1, Math.round(w * 0.93F));
                int w2 = Math.max(0, Math.round(halfWidth(Math.min(BOW_X, x + 2)) * 0.8F) - (x + 2 > BOW_X ? 1 : 0));
                int w3 = Math.max(0, Math.round(halfWidth(Math.min(BOW_X, x + 4)) * 0.55F) - (x + 4 > BOW_X ? 1 : 0));
                b.box(WOOD, (i * 3) % 46, 20 + 2 - 2 * w1, x, 2, -w1, 2, 2, 2 * w1);
                if (w2 > 0) b.box(WOOD, (i * 3 + 11) % 46, 20 + 4 - 2 * w2, x, 4, -w2, 2, 2, 2 * w2);
                if (w3 > 0) b.box(WOOD, (i * 3 + 23) % 46, 20 + 6 - 2 * w3, x, 6, -w3, 2, 2, 2 * w3);
            }
            // deck planks (rows and columns lined up with z and x across the slices)
            int inner = w - thick;
            if (inner >= 1) {
                float deck = deckAt(x);
                int du = 50 + x - 2 * inner;
                b.box(DECK, du, 12 - inner, x, deck - 0.2F, -inner, 1, 0.2F, 2 * inner);
            }
        }
        // the keel
        b.box(WOOD, 0, 30, STERN_X + 2, 8, -0.75F, BOW_X - STERN_X - 2, 1.5F, 1.5F);
    }

    // ── forecastle and quarterdeck: raised decks, their bulkheads, railings and ladders ──
    private static void castles(Builder b) {
        for (int x = STERN_X; x <= BOW_X; x++) {
            boolean fore = x >= FORECASTLE_START;
            boolean quarter = x <= QUARTER_END;
            if (!fore && !quarter) continue;
            int inner = halfWidth(x) - Math.max(1, Math.min(halfWidth(x), 1));
            if (inner < 1) continue;
            float deck = fore ? FORE_DECK : QUARTER_DECK;
            b.box(WOOD, ((x - STERN_X) * 2) % 30, 0, x, deck, -inner, 1, MAIN_DECK - deck, 2 * inner);
        }
        // the quarterdeck's front (bulkhead at x = -9): a door with gold fittings, two lit windows, a ladder, a railing
        float qx = QUARTER_END + 1;
        b.box(WOOD, 60, 10, qx, -6.2F, 2.0F, 0.3F, 5.2F, 2.8F);                 // door
        b.box(GOLD, 0, 12, qx + 0.3F, -3.8F, 2.3F, 0.25F, 0.5F, 0.5F);           // its handle
        b.box(IRON, 0, 0, qx + 0.3F, -5.6F, 2.05F, 0.1F, 0.4F, 2.7F);            // hinges
        b.box(IRON, 0, 0, qx + 0.3F, -2.2F, 2.05F, 0.1F, 0.4F, 2.7F);
        for (int side = -1; side <= 1; side += 2) {
            float wz = side > 0 ? 5.5F : -7.5F;
            b.box(GOLD, 4, 12, qx, -5.6F, wz - 0.3F, 0.25F, 2.6F, 2.6F);         // window frames
            b.box(GLASS, 0, 0, qx + 0.1F, -5.3F, wz, 0.25F, 2.0F, 2.0F);         // and their glow
        }
        for (int r = 0; r < 3; r++) b.box(SPAR, 0, r * 2, qx + 0.3F, -1.8F - r * 1.9F, -1.4F, 0.4F, 0.4F, 2.8F); // ladder rungs
        b.box(SPAR, 10, 0, qx + 0.3F, -7.0F, -1.6F, 0.4F, 6.0F, 0.3F);
        b.box(SPAR, 10, 0, qx + 0.3F, -7.0F, 1.3F, 0.4F, 6.0F, 0.3F);
        railing(b, qx - 0.6F, QUARTER_DECK, halfWidth(QUARTER_END) - 1, 2.0F);
        // the forecastle's back (bulkhead at x = 15): a door, a ladder, a railing, lanterns either side of the door
        float fx = FORECASTLE_START;
        b.box(WOOD, 60, 10, fx - 0.3F, -4.6F, -3.6F, 0.3F, 3.8F, 2.8F);
        b.box(GOLD, 0, 12, fx - 0.55F, -2.9F, -3.2F, 0.25F, 0.5F, 0.5F);
        for (int r = 0; r < 2; r++) b.box(SPAR, 0, r * 2, fx - 0.7F, -1.9F - r * 1.6F, 1.0F, 0.4F, 0.4F, 2.6F);
        b.box(SPAR, 10, 0, fx - 0.7F, -5.0F, 0.8F, 0.4F, 4.0F, 0.3F);
        b.box(SPAR, 10, 0, fx - 0.7F, -5.0F, 3.4F, 0.4F, 4.0F, 0.3F);
        railing(b, fx + 0.2F, FORE_DECK, halfWidth(FORECASTLE_START) - 1, 1.4F);
        lantern(b, fx - 1.2F, -4.0F, -6.5F);
        lantern(b, fx - 1.2F, -4.0F, 6.5F);
    }

    // a railing across the ship at x, on a deck, out to +-half, leaving a gap in the middle for the ladder
    private static void railing(Builder b, float x, float deck, int half, float gap) {
        for (float z = -half + 0.5F; z <= half - 0.5F; z += 1.6F) {
            if (Math.abs(z) < gap) continue;
            b.box(SPAR, 20, 2, x, deck - 3.0F, z - 0.25F, 0.5F, 3.0F, 0.5F);   // balusters
        }
        b.box(GOLD, 0, 16, x - 0.1F, deck - 3.5F, -half, 0.7F, 0.5F, half - gap);   // top rail, both halves
        b.box(GOLD, 0, 16, x - 0.1F, deck - 3.5F, gap, 0.7F, 0.5F, half - gap);
    }

    // a lantern hanging at (x, y = its bottom, z): iron frame, glowing glass, a cap and ring
    private static void lantern(Builder b, float x, float y, float z) {
        b.box(IRON, 0, 0, x - 0.8F, y - 0.4F, z - 0.8F, 1.6F, 0.4F, 1.6F);
        b.box(GLASS, 2, 2, x - 0.6F, y - 2.2F, z - 0.6F, 1.2F, 1.8F, 1.2F);
        for (int cx = -1; cx <= 1; cx += 2) {
            for (int cz = -1; cz <= 1; cz += 2) b.box(IRON, 4, 0, x + cx * 0.6F - 0.1F, y - 2.2F, z + cz * 0.6F - 0.1F, 0.2F, 1.8F, 0.2F);
        }
        b.box(IRON, 0, 4, x - 0.8F, y - 2.6F, z - 0.8F, 1.6F, 0.4F, 1.6F);
        b.box(IRON, 0, 8, x - 0.4F, y - 3.2F, z - 0.4F, 0.8F, 0.6F, 0.8F);
        b.box(IRON, 8, 8, x - 0.1F, y - 3.8F, z - 0.3F, 0.2F, 0.6F, 0.6F);
    }

    // ── the stern: the gallery (its picture: STERN), gold carving, lanterns, the rudder, quarter galleries ──
    private static void stern(Builder b) {
        int w = halfWidth(STERN_X);
        float sx = STERN_X;
        // the gallery plate: 18 wide (z) x 15 tall, its back face showing the picture at (STERN_U, STERN_V)
        b.statics.texOffs(STERN_U, STERN_V - 2 * w).addBox(sx - 0.4F, -13.0F, -w, 0.4F, 15.0F, 2 * w);
        // a balcony rail under the windows
        b.box(GOLD, 0, 18, sx - 1.4F, -4.6F, -w + 1, 1.0F, 0.4F, 2 * w - 2);
        for (float z = -w + 1.5F; z <= w - 1.5F; z += 1.5F) b.box(GOLD, 6, 18, sx - 1.3F, -4.2F, z - 0.15F, 0.3F, 1.6F, 0.3F);
        b.box(GOLD, 0, 20, sx - 1.4F, -2.8F, -w + 1, 1.0F, 0.3F, 2 * w - 2);
        // the crest above it, and the big stern lantern on its bracket
        b.box(GOLD, 0, 0, sx - 0.8F, -15.0F, -3.0F, 0.8F, 2.0F, 6.0F);
        b.box(GOLD, 20, 0, sx - 0.8F, -16.0F, -1.5F, 0.8F, 1.0F, 3.0F);
        b.box(IRON, 10, 0, sx - 3.0F, -16.6F, -0.2F, 3.0F, 0.4F, 0.4F);
        lantern(b, sx - 3.0F, -16.2F, 0.0F);
        // corner lanterns on posts
        for (int side = -1; side <= 1; side += 2) {
            float z = side * (w - 1.5F);
            b.box(SPAR, 30, 0, sx + 0.5F, -15.5F, z - 0.3F, 0.6F, 2.6F, 0.6F);
            lantern(b, sx + 0.8F, -15.5F, z);
        }
        // the rudder, with iron hinges
        b.box(WOOD, 70, 20, sx - 1.4F, -6.0F, -0.5F, 1.0F, 15.0F, 1.0F);
        for (int i = 0; i < 4; i++) b.box(IRON, 12, 0, sx - 1.5F, -4.0F + i * 3.5F, -0.6F, 1.2F, 0.4F, 1.2F);
        b.box(SPAR, 40, 0, sx - 0.4F, -8.0F, -0.4F, 1.4F, 0.6F, 0.8F);
        // quarter galleries: window boxes on each side near the stern
        for (int side = -1; side <= 1; side += 2) {
            int qw = halfWidth(-21);
            float zOut = side > 0 ? qw : -qw - 1.5F;
            float glassZ = side > 0 ? qw + 1.5F : -qw - 1.6F;
            b.box(WOOD, 40, 10, -24.0F, -11.0F, zOut, 5.0F, 4.0F, 1.5F);
            b.box(GOLD, 10, 4, -24.2F, -11.4F, zOut - 0.1F, 5.4F, 0.4F, 1.7F);
            b.box(GOLD, 10, 4, -24.2F, -7.0F, zOut - 0.1F, 5.4F, 0.4F, 1.7F);
            b.box(GLASS, 4, 0, -23.4F, -10.4F, glassZ, 1.6F, 2.6F, 0.1F);
            b.box(GLASS, 4, 0, -21.0F, -10.4F, glassZ, 1.6F, 2.6F, 0.1F);
            b.box(GOLD, 2, 12, -21.8F, -10.6F, glassZ, 0.4F, 3.0F, 0.12F);
        }
    }

    // ── the bow: the stem, bowsprit and jib boom, the figurehead, catheads and anchors ──
    static final float BOWSPRIT_X = BOW_X - 1.0F;
    static final float BOWSPRIT_Y = -8.0F;
    static final float BOWSPRIT_ANGLE = (float) Math.toRadians(15);

    // a point along the bowsprit, `along` pixels out
    static float sprX(float along) {
        return BOWSPRIT_X + along * Mth.cos(BOWSPRIT_ANGLE);
    }

    static float sprY(float along) {
        return BOWSPRIT_Y - along * Mth.sin(BOWSPRIT_ANGLE);
    }

    private static void bow(Builder b) {
        // the stem post, gold at the top
        b.box(WOOD, 80, 20, BOW_X + 1.0F, -11.0F, -0.75F, 1.2F, 18.0F, 1.5F);
        b.box(GOLD, 30, 4, BOW_X + 0.9F, -12.0F, -0.85F, 1.4F, 1.0F, 1.7F);
        // the bowsprit (a thick spar up and forward) and the jib boom beyond it, with gold bands
        CubeListBuilder sprit = CubeListBuilder.create();
        Builder.box(sprit, SPAR, 0, 0, 0.0F, -1.0F, -1.0F, 20.0F, 2.0F, 2.0F);
        Builder.box(sprit, SPAR, 0, 20, 18.0F, -0.5F, -0.5F, 12.0F, 1.0F, 1.0F);
        Builder.box(sprit, GOLD, 40, 0, 6.0F, -1.15F, -1.15F, 0.6F, 2.3F, 2.3F);
        Builder.box(sprit, GOLD, 40, 0, 12.0F, -1.15F, -1.15F, 0.6F, 2.3F, 2.3F);
        Builder.box(sprit, GOLD, 40, 0, 19.0F, -1.15F, -1.15F, 0.6F, 2.3F, 2.3F);
        Builder.box(sprit, GOLD, 40, 6, 29.5F, -0.7F, -0.7F, 0.8F, 1.4F, 1.4F);
        b.part(sprit, PartPose.offsetAndRotation(BOWSPRIT_X, BOWSPRIT_Y, 0.0F, 0.0F, 0.0F, -BOWSPRIT_ANGLE));
        // the figurehead under it: a gold skull over crossed bones
        float fx = BOW_X + 1.6F;
        b.box(GOLD, 0, 0, fx, -7.5F, -1.5F, 2.6F, 3.0F, 3.0F);              // skull
        b.box(GOLD, 10, 0, fx + 0.3F, -4.5F, -1.0F, 2.0F, 1.2F, 2.0F);      // jaw
        b.box(BLACK, 0, 0, fx + 2.6F, -6.6F, 0.25F, 0.05F, 1.0F, 1.0F);     // eye sockets
        b.box(BLACK, 0, 0, fx + 2.6F, -6.6F, -1.25F, 0.05F, 1.0F, 1.0F);
        b.box(BLACK, 0, 0, fx + 2.6F, -5.3F, -0.3F, 0.05F, 0.7F, 0.6F);     // nose
        b.box(BLACK, 0, 0, fx + 2.35F, -3.9F, -0.8F, 0.05F, 0.3F, 1.6F);    // teeth line
        for (int s = -1; s <= 1; s += 2) {
            CubeListBuilder bone = CubeListBuilder.create();
            Builder.box(bone, GOLD, 20, 10, -0.4F, -0.4F, -4.5F, 0.8F, 0.8F, 9.0F);
            Builder.box(bone, GOLD, 0, 18, -0.6F, -0.8F, -5.2F, 1.2F, 1.6F, 1.0F);
            Builder.box(bone, GOLD, 0, 18, -0.6F, -0.8F, 4.2F, 1.2F, 1.6F, 1.0F);
            b.part(bone, PartPose.offsetAndRotation(fx + 0.8F, -5.0F, 0.0F, s * 0.7F, 0.0F, 0.0F));
        }
        // catheads (beams out over the sides) with anchors hanging from them
        int ax = 23;
        int aw = halfWidth(ax);
        for (int side = -1; side <= 1; side += 2) {
            float zBeam = side > 0 ? aw - 0.5F : -aw - 1.5F;
            b.box(SPAR, 50, 0, ax - 0.6F, wallTop(ax) + 0.5F, zBeam, 1.2F, 1.2F, 2.0F);
            float az = side * (aw + 1.0F);
            anchor(b, ax, wallTop(ax) + 2.0F, az);
        }
    }

    // an iron anchor hanging flat against the hull: ring, stock, shank, arms and flukes
    private static void anchor(Builder b, float x, float top, float z) {
        b.box(IRON, 0, 12, x - 0.6F, top - 0.6F, z - 0.15F, 1.2F, 0.6F, 0.3F);        // ring
        b.box(SPAR, 60, 0, x - 2.0F, top + 0.4F, z - 0.3F, 4.0F, 0.6F, 0.6F);          // wooden stock
        b.box(IRON, 4, 12, x - 0.35F, top, z - 0.35F, 0.7F, 7.0F, 0.7F);               // shank
        for (int s = -1; s <= 1; s += 2) {
            CubeListBuilder arm = CubeListBuilder.create();
            Builder.box(arm, IRON, 8, 12, s > 0 ? 0.0F : -3.2F, -0.3F, -0.3F, 3.2F, 0.6F, 0.6F);
            Builder.box(arm, IRON, 14, 12, s > 0 ? 2.4F : -3.4F, -1.2F, -0.4F, 1.0F, 1.0F, 0.8F);   // fluke
            b.part(arm, PartPose.offsetAndRotation(x, top + 7.0F, z, 0.0F, 0.0F, s * -0.7F));
        }
    }

    // ── on deck: the wheel, a treasure chest, barrels, the bell, the hatch grating, coils of rope ──
    private static void deckThings(Builder b) {
        // the ship's wheel at the front of the quarterdeck, on a pedestal in front of it (it turns: setupAnim)
        float wheelX = -11.0F;
        float wheelY = -16.0F;
        b.box(WOOD, 50, 20, wheelX + 0.7F, wheelY, -0.8F, 1.6F, QUARTER_DECK - wheelY, 1.6F);
        b.box(GOLD, 30, 8, wheelX + 0.5F, QUARTER_DECK - 1.0F, -1.1F, 2.0F, 1.0F, 2.2F);
        b.box(IRON, 16, 0, wheelX - 0.2F, wheelY - 0.3F, -0.3F, 1.2F, 0.6F, 0.6F);   // axle
        CubeListBuilder hub = CubeListBuilder.create();
        Builder.box(hub, GOLD, 50, 8, -0.4F, -0.8F, -0.8F, 0.8F, 1.6F, 1.6F);
        PartDefinition wheel = b.ship.addOrReplaceChild("wheel", hub, PartPose.offset(wheelX, wheelY, 0.0F));
        float radius = 4.6F;
        float seg = 2 * radius * (float) Math.tan(Math.PI / 8) + 0.2F;
        for (int k = 0; k < 8; k++) {
            float a = (float) (k * Math.PI / 4);
            CubeListBuilder rim = CubeListBuilder.create();
            Builder.box(rim, WOOD, 30, 30, -0.3F, -radius - 0.4F, -seg / 2, 0.6F, 0.8F, seg);
            wheel.addOrReplaceChild("rim" + k, rim, PartPose.offsetAndRotation(0, 0, 0, a, 0, 0));
            CubeListBuilder spoke = CubeListBuilder.create();
            Builder.box(spoke, WOOD, 40, 30, -0.2F, -radius - 1.6F, -0.2F, 0.4F, radius + 0.8F, 0.4F);
            Builder.box(spoke, WOOD, 44, 30, -0.3F, -radius - 2.2F, -0.3F, 0.6F, 0.8F, 0.6F);
            wheel.addOrReplaceChild("spoke" + k, spoke, PartPose.offsetAndRotation(0, 0, 0, a + (float) (Math.PI / 8), 0, 0));
        }
        // a treasure chest on the quarterdeck, its lid open on a heap of gold
        float cx = -23.0F;
        float cz = -8.0F;
        b.box(WOOD, 20, 20, cx, QUARTER_DECK - 2.4F, cz, 3.0F, 2.4F, 3.6F);
        b.box(GOLD, 20, 4, cx - 0.05F, QUARTER_DECK - 2.45F, cz + 0.4F, 3.1F, 2.5F, 0.3F);
        b.box(GOLD, 20, 4, cx - 0.05F, QUARTER_DECK - 2.45F, cz + 2.9F, 3.1F, 2.5F, 0.3F);
        b.box(GOLD, 36, 4, cx + 0.6F, QUARTER_DECK - 2.9F, cz + 0.3F, 1.8F, 0.6F, 3.0F);   // gold heap
        b.box(GOLD, 36, 8, cx + 1.0F, QUARTER_DECK - 3.3F, cz + 1.0F, 1.0F, 0.5F, 1.6F);
        CubeListBuilder lid = CubeListBuilder.create();
        Builder.box(lid, WOOD, 20, 30, 0.0F, -1.0F, 0.0F, 3.0F, 1.0F, 3.6F);
        Builder.box(lid, GOLD, 50, 12, 1.2F, -0.2F, 3.6F, 0.6F, 0.8F, 0.2F);
        b.part(lid, PartPose.offsetAndRotation(cx, QUARTER_DECK - 2.4F, cz, -1.1F, 0.0F, 0.0F));
        // a barrel by the chest, and two by the forecastle
        barrel(b, -21.5F, QUARTER_DECK, 6.8F);
        barrel(b, 13.0F, MAIN_DECK, -4.4F);
        barrel(b, 13.0F, MAIN_DECK, 4.4F);
        // the ship's bell on the forecastle, under a small gold frame
        float bx = 17.5F;
        float bz = 4.0F;
        b.box(SPAR, 0, 30, bx - 0.25F, FORE_DECK - 4.0F, bz - 1.6F, 0.5F, 4.0F, 0.5F);
        b.box(SPAR, 0, 30, bx - 0.25F, FORE_DECK - 4.0F, bz + 1.1F, 0.5F, 4.0F, 0.5F);
        b.box(SPAR, 4, 30, bx - 0.3F, FORE_DECK - 4.4F, bz - 1.7F, 0.6F, 0.5F, 3.4F);
        b.box(GOLD, 44, 12, bx - 0.8F, FORE_DECK - 3.6F, bz - 0.8F, 1.6F, 1.8F, 1.6F);
        b.box(GOLD, 44, 16, bx - 1.0F, FORE_DECK - 2.0F, bz - 1.0F, 2.0F, 0.4F, 2.0F);
        // the hatch grating amidships, and rope coiled at the masts' feet
        b.box(GRATE, 0, 0, 5.0F, MAIN_DECK - 0.5F, -2.5F, 5.0F, 0.5F, 5.0F);
        b.box(SPAR, 60, 10, 4.7F, MAIN_DECK - 0.6F, -2.8F, 5.6F, 0.4F, 0.3F);
        b.box(SPAR, 60, 10, 4.7F, MAIN_DECK - 0.6F, 2.5F, 5.6F, 0.4F, 0.3F);
        coil(b, -6.5F, MAIN_DECK, 3.5F);
        coil(b, -6.5F, MAIN_DECK, -3.5F);
        coil(b, 18.5F, FORE_DECK, -3.0F);
    }

    private static void barrel(Builder b, float x, float deck, float z) {
        b.box(WOOD, 60, 20, x - 1.2F, deck - 3.2F, z - 0.8F, 2.4F, 3.2F, 1.6F);
        b.box(WOOD, 60, 20, x - 0.8F, deck - 3.17F, z - 1.2F, 1.6F, 3.14F, 2.4F);
        b.box(IRON, 0, 16, x - 1.3F, deck - 2.8F, z - 0.9F, 2.6F, 0.3F, 1.8F);
        b.box(IRON, 0, 16, x - 0.9F, deck - 2.78F, z - 1.3F, 1.8F, 0.26F, 2.6F);
        b.box(IRON, 0, 16, x - 1.3F, deck - 0.8F, z - 0.9F, 2.6F, 0.3F, 1.8F);
        b.box(IRON, 0, 16, x - 0.9F, deck - 0.78F, z - 1.3F, 1.8F, 0.26F, 2.6F);
    }

    private static void coil(Builder b, float x, float deck, float z) {
        b.box(ROPE, 0, 0, x - 1.2F, deck - 0.5F, z - 0.8F, 2.4F, 0.5F, 1.6F);
        b.box(ROPE, 0, 0, x - 0.8F, deck - 0.47F, z - 1.2F, 1.6F, 0.44F, 2.4F);
        b.box(ROPE, 0, 4, x - 0.8F, deck - 0.9F, z - 0.5F, 1.6F, 0.4F, 1.0F);
    }

    // ── cannons: on carriages, run out through open gunports, three a side ──
    private static void cannons(Builder b) {
        float[][] guns = {{-15.0F, QUARTER_DECK}, {-7.5F, MAIN_DECK}, {11.0F, MAIN_DECK}};
        for (float[] gun : guns) {
            for (int side = -1; side <= 1; side += 2) cannon(b, gun[0], gun[1], side);
        }
    }

    private static void cannon(Builder b, float x, float deck, int side) {
        int sx = (int) Math.floor(x);
        int w = halfWidth(sx);
        int thick = 1;
        float wallIn = w - thick;
        float barrelY = deck - 2.6F;
        // the carriage and its wheels
        float c0 = side > 0 ? wallIn - 3.6F : -wallIn + 0.4F;
        b.box(WOOD, 60, 30, x - 1.4F, deck - 1.8F, c0, 2.8F, 1.4F, 3.2F);
        for (int wx = -1; wx <= 1; wx += 2) {
            for (int wz = 0; wz <= 1; wz++) b.box(BLACK, 2, 2, x + wx * 1.5F - 0.5F, deck - 1.0F, c0 + 0.3F + wz * 2.0F, 1.0F, 1.0F, 0.6F);
        }
        // the barrel: from the carriage out through the wall, a muzzle ring at the end, the knob at the back
        float zIn = side > 0 ? wallIn - 3.4F : -w - 2.4F;
        float len = thick + 5.8F;
        b.box(IRON, 0, 0, x - 0.8F, barrelY - 0.5F, zIn, 1.6F, 1.0F, len);
        b.box(IRON, 0, 0, x - 0.5F, barrelY - 0.8F, zIn, 1.0F, 1.6F, len);
        float muzzle = side > 0 ? w + 2.0F : -w - 2.4F;
        b.box(IRON, 10, 10, x - 1.0F, barrelY - 1.0F, muzzle, 2.0F, 2.0F, 0.5F);
        b.box(BLACK, 4, 4, x - 0.4F, barrelY - 0.4F, side > 0 ? muzzle + 0.5F : muzzle - 0.02F, 0.8F, 0.8F, 0.02F);
        float knob = side > 0 ? zIn - 0.6F : zIn + len;
        b.box(IRON, 10, 16, x - 0.4F, barrelY - 0.4F, knob, 0.8F, 0.8F, 0.6F);
        // the gunport: a dark square on the hull, its red lid propped open above
        float portZ = side > 0 ? w : -w - 0.06F;
        b.box(BLACK, 8, 8, x - 1.4F, barrelY - 1.4F, portZ, 2.8F, 2.8F, 0.06F);
        b.box(GOLD, 30, 12, x - 1.6F, barrelY + 1.4F, portZ - 0.02F, 3.2F, 0.3F, 0.1F);
        CubeListBuilder lidBox = CubeListBuilder.create();
        Builder.box(lidBox, RED, 44, 0, -1.4F, -2.8F, 0.0F, 2.8F, 2.8F, 0.3F);
        b.part(lidBox, PartPose.offsetAndRotation(x, barrelY - 1.4F, side > 0 ? w + 0.06F : -w - 0.36F, side * -0.55F, 0.0F, 0.0F));
    }

    // ── masts, tops, yards, the square sails, the gaff sail, jibs, the flag and pennants ──
    private static void masts(Builder b) {
        // the main mast: lower mast, fighting top, topmast, pole
        trunk(b, "main_trunk", MAIN_X, 1.5F, -46.0F, MAIN_DECK, new float[]{-6, -18, -30, -42});
        b.box(MAST, 14, 0, MAIN_X - 1.0F, -68.0F, -1.0F, 2.0F, 24.0F, 2.0F);
        b.box(MAST, 24, 0, MAIN_X - 0.5F, -76.0F, -0.5F, 1.0F, 9.0F, 1.0F);
        b.box(GOLD, 50, 16, MAIN_X - 0.7F, -77.0F, -0.7F, 1.4F, 1.0F, 1.4F);       // the truck (ball on top)
        top(b, MAIN_X, -46.0F, 5);
        // the fore mast (on the forecastle)
        trunk(b, "fore_trunk", FORE_X, 1.25F, -45.0F, FORE_DECK, new float[]{-10, -22, -34});
        b.box(MAST, 14, 0, FORE_X - 1.0F, -64.0F, -1.0F, 2.0F, 20.0F, 2.0F);
        b.box(MAST, 24, 0, FORE_X - 0.5F, -68.0F, -0.5F, 1.0F, 5.0F, 1.0F);
        top(b, FORE_X, -45.0F, 4);
        // the mizzen (at the stern)
        b.box(MAST, 0, 0, MIZZEN_X - 1.0F, -54.0F, -1.0F, 2.0F, 47.0F, 2.0F);
        b.box(MAST, 24, 0, MIZZEN_X - 0.5F, -58.0F, -0.5F, 1.0F, 5.0F, 1.0F);
        mastBands(b, MIZZEN_X, 1.0F, new float[]{-12, -24, -36});

        for (SailSpec spec : SAILS) sail(b, spec);

        // the gaff sail behind the mizzen: from its gaff (angled up and back) down to the boom
        float luff = MIZZEN_X - 1.0F;
        b.line(SPAR, luff, -50.0F, 0.0F, -38.0F, -54.0F, 0.0F, 0.8F);    // gaff
        b.box(SPAR, 0, 30, -40.0F, -23.6F, -0.4F, luff + 40.0F, 0.8F, 0.8F);  // boom
        polygonSail(b, new float[][]{{luff, -50}, {-38, -54}, {-40, -24}, {luff, -24}}, 0.0F, 1.2F, 0);
        // two jibs from the fore mast out to the bowsprit
        polygonSail(b, new float[][]{{FORE_X + 1.2F, -56}, {sprX(14), sprY(14) - 1}, {31, -11}}, -0.3F, 0.9F, 8);
        polygonSail(b, new float[][]{{FORE_X + 1.2F, -62}, {sprX(26), sprY(26) - 1}, {37, -13}}, 0.3F, 0.9F, 20);

        // the Jolly Roger at the main mast's head, streaming back
        PartDefinition flag = b.ship.addOrReplaceChild("flag", CubeListBuilder.create(), PartPose.ZERO);
        float pole = MAIN_X - 0.5F;
        for (int i = 0; i < FLAG_STRIPS; i++) {
            CubeListBuilder strip = CubeListBuilder.create().texOffs(FLAG_U + i, FLAG_V).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 10.0F, 0.0F);
            flag.addOrReplaceChild("f" + i, strip, PartPose.offset(pole - i, -75.0F, 0.0F));
        }
        // red pennants on the fore and mizzen heads, tapering as they go
        pennant(b, "fore_pennant", FORE_X - 0.5F, -67.5F);
        pennant(b, "mizzen_pennant", MIZZEN_X - 0.5F, -57.5F);
    }

    // A lower mast (deck to its top) with its iron bands, as a part of its own: hidden while you ride the ship in first
    // person with it in front of your seat, so it doesn't block the view ahead (setupAnim)
    private static void trunk(Builder b, String name, float x, float half, float top, float deck, float[] bands) {
        CubeListBuilder cubes = CubeListBuilder.create();
        Builder.box(cubes, MAST, 0, 0, x - half, top, -half, 2 * half, deck - top, 2 * half);
        for (float y : bands) Builder.box(cubes, IRON, 0, 20, x - half - 0.1F, y - 0.4F, -half - 0.1F, 2 * half + 0.2F, 0.4F, 2 * half + 0.2F);
        b.ship.addOrReplaceChild(name, cubes, PartPose.ZERO);
    }

    private static void mastBands(Builder b, float x, float half, float[] ys) {
        for (float y : ys) b.box(IRON, 0, 20, x - half - 0.1F, y - 0.4F, -half - 0.1F, 2 * half + 0.2F, 0.4F, 2 * half + 0.2F);
    }

    // a fighting top: a round-ish platform on the mast with a gold-railed rim
    private static void top(Builder b, float x, float y, int half) {
        b.box(WOOD, 0, 0, x - half, y, -half + 1, 2 * half, 1.0F, 2 * half - 2);
        b.box(WOOD, 0, 0, x - half + 1, y + 0.03F, -half, 2 * half - 2, 0.94F, 2 * half);
        // posts and a rail all round
        for (int i = -half + 1; i <= half - 1; i += 2) {
            b.box(SPAR, 70, 0, x + i - 0.2F, y - 2.5F, half - 0.4F, 0.4F, 2.5F, 0.4F);
            b.box(SPAR, 70, 0, x + i - 0.2F, y - 2.5F, -half, 0.4F, 2.5F, 0.4F);
            b.box(SPAR, 70, 0, x - half, y - 2.5F, i - 0.2F, 0.4F, 2.5F, 0.4F);
            b.box(SPAR, 70, 0, x + half - 0.4F, y - 2.5F, i - 0.2F, 0.4F, 2.5F, 0.4F);
        }
        b.box(GOLD, 0, 0, x - half, y - 3.0F, half - 0.5F, 2 * half, 0.5F, 0.6F);
        b.box(GOLD, 0, 0, x - half, y - 3.0F, -half - 0.1F, 2 * half, 0.5F, 0.6F);
        b.box(GOLD, 0, 0, x - half - 0.1F, y - 3.0F, -half, 0.6F, 0.5F, 2 * half);
        b.box(GOLD, 0, 0, x + half - 0.5F, y - 3.0F, -half, 0.6F, 0.5F, 2 * half);
    }

    // A square sail hung from its yard, in rows 2 px tall, each row three flat pieces (port, middle, starboard) so it
    // curves: the middle pushed forward by its billow, the sides angled back (setupAnim). Every piece is a box with no
    // thickness: its back face (seen from behind the ship) and front face take the sail's picture from a block 2 x the
    // width wide at (u, SAIL_V): piece k (0 port, 1 middle, 2 starboard) at u + 2L*k, L = a third of the width.
    private static void sail(Builder b, SailSpec spec) {
        String name = spec.name;
        float mastX = spec.mastX;
        float mastHalf = spec.mastHalf;
        float topY = spec.topY;
        int rows = spec.rows;
        int width = spec.width;
        int u = spec.u;
        int third = width / 3;
        float x = spec.x();
        // the yard it hangs from, with its ends and the ropes holding it
        b.box(SPAR, 0, 0, mastX + mastHalf, topY - 1.4F, -width / 2.0F - 2.0F, 1.4F, 1.4F, width + 4.0F);
        b.box(IRON, 20, 0, mastX + mastHalf - 0.1F, topY - 1.5F, -1.0F, 1.6F, 1.6F, 2.0F);
        PartDefinition sail = b.ship.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        for (int j = 0; j < rows; j++) {
            float y = topY + 2 * j;
            int v = SAIL_V + 2 * j - third;
            float half = third / 2.0F;
            sail.addOrReplaceChild("p" + j, CubeListBuilder.create().texOffs(u, v).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, third),
                PartPose.offset(x, y, half));
            sail.addOrReplaceChild("m" + j, CubeListBuilder.create().texOffs(u + 2 * third, v).addBox(0.0F, 0.0F, -half, 0.0F, 2.0F, third),
                PartPose.offset(x, y, 0.0F));
            sail.addOrReplaceChild("s" + j, CubeListBuilder.create().texOffs(u + 4 * third, v).addBox(0.0F, 0.0F, -third, 0.0F, 2.0F, third),
                PartPose.offset(x, y, -half));
        }
        // the clew lines down from the sail's bottom corners to the deck
        float bottom = topY + 2 * rows;
        int deckX = (int) mastX + 3;
        b.rope(x, bottom, width / 2.0F - 0.5F, deckX, wallTop(deckX), halfWidth(deckX) - 1.0F);
        b.rope(x, bottom, -width / 2.0F + 0.5F, deckX, wallTop(deckX), -halfWidth(deckX) + 1.0F);
    }

    // A fore-and-aft sail (in the ship's middle plane) cut from a polygon: rows 2 px tall across it, billowing out to
    // one side by up to `bulge` (and set `dz` off the middle), from the CLOTH zone (rows from dv)
    private static void polygonSail(Builder b, float[][] poly, float dz, float bulge, int dv) {
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] p : poly) {
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        int rows = (int) Math.ceil((maxY - minY) / 2.0F);
        for (int j = 0; j < rows; j++) {
            float y0 = minY + 2 * j;
            float y = Math.min(y0 + 1.0F, maxY - 0.01F);
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            for (int e = 0; e < poly.length; e++) {
                float[] a = poly[e];
                float[] c = poly[(e + 1) % poly.length];
                if ((y < Math.min(a[1], c[1])) || (y > Math.max(a[1], c[1])) || a[1] == c[1]) continue;
                float xi = a[0] + (y - a[1]) / (c[1] - a[1]) * (c[0] - a[0]);
                lo = Math.min(lo, xi);
                hi = Math.max(hi, xi);
            }
            if (hi - lo < 0.5F) continue;
            float h = Math.min(2.0F, maxY - y0);
            float out = bulge * Mth.sin((float) Math.PI * (j + 0.5F) / rows);
            Builder.box(b.statics, CLOTH, 2 + (j * 7) % 40, dv + (j % 6) * 2, lo, y0, dz + out, hi - lo, h, 0.0F);
        }
    }

    // a red pennant: strips tapering from 2 px tall at the mast to a point, streaming back (they wave: setupAnim)
    private static void pennant(Builder b, String name, float x, float y) {
        PartDefinition pennant = b.ship.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < PENNANT_STRIPS; i++) {
            float h = 2.0F * (1.0F - i / (float) PENNANT_STRIPS) + 0.2F;
            CubeListBuilder strip = CubeListBuilder.create();
            Builder.box(strip, RED, 2 + i * 2, 4, -1.0F, -h / 2, 0.0F, 1.0F, h, 0.0F);
            pennant.addOrReplaceChild("f" + i, strip, PartPose.offset(x - i, y, 0.0F));
        }
    }

    // ── rigging: shrouds (with ratlines) from the sides up to the tops, backstays, and the stays between the masts ──
    private static void rigging(Builder b) {
        for (int side = -1; side <= 1; side += 2) {
            // main shrouds: three a side from the bulwarks to the main top, with ratlines across them
            shrouds(b, side, new float[]{MAIN_X - 2, MAIN_X + 1, MAIN_X + 4}, -6.0F, MAIN_X, -46.0F, 4.6F, 9);
            // topmast shrouds: from the top's rim up to the topmast
            b.rope(MAIN_X - 1.0F, -46.0F, side * 4.6F, MAIN_X, -66.0F, side * 0.8F);
            b.rope(MAIN_X + 1.0F, -46.0F, side * 4.6F, MAIN_X, -66.0F, side * 0.8F);
            // fore shrouds
            shrouds(b, side, new float[]{FORE_X - 2, FORE_X + 1, FORE_X + 4}, wallTop((int) FORE_X), FORE_X, -45.0F, 3.6F, 7);
            b.rope(FORE_X, -45.0F, side * 3.6F, FORE_X, -62.0F, side * 0.8F);
            // mizzen shrouds
            shrouds(b, side, new float[]{MIZZEN_X}, wallTop((int) MIZZEN_X), MIZZEN_X, -44.0F, 1.0F, 0);
            // backstays from the main topmast down to the quarterdeck's sides
            b.rope(MAIN_X, -66.0F, side * 0.8F, -19.0F, wallTop(-19), side * (halfWidth(-19) - 0.5F));
        }
        // stays, fore and aft: main top to fore mast, fore top to the jib boom, mizzen top to the main mast
        b.rope(MAIN_X + 1.0F, -46.0F, 0.0F, FORE_X - 1.0F, -14.0F, 0.0F);
        b.rope(MAIN_X + 0.5F, -72.0F, 0.0F, FORE_X - 0.8F, -45.0F, 0.0F);
        b.rope(FORE_X + 1.0F, -45.0F, 0.0F, sprX(14), sprY(14) - 1.0F, 0.0F);
        b.rope(FORE_X + 0.8F, -63.0F, 0.0F, sprX(29), sprY(29) - 0.5F, 0.0F);
        b.rope(MIZZEN_X + 1.0F, -52.0F, 0.0F, MAIN_X - 1.5F, -40.0F, 0.0F);
        // the bobstay under the bowsprit, down to the stem
        b.rope(sprX(20), sprY(20) + 1.0F, 0.0F, BOW_X + 1.5F, 0.0F, 0.0F);
    }

    // shrouds from deadeyes on the side (at these x, height deckY) up to the mast's top (its rim at +-reach),
    // with `rungs` ratlines tied across them
    private static void shrouds(Builder b, int side, float[] xs, float deckY, float mastX, float topY, float reach, int rungs) {
        float[][] from = new float[xs.length][];
        float[][] to = new float[xs.length][];
        for (int i = 0; i < xs.length; i++) {
            int w = halfWidth((int) Math.floor(xs[i]));
            from[i] = new float[]{xs[i], deckY + 1.0F, side * (w + 0.6F)};
            to[i] = new float[]{mastX + (i - (xs.length - 1) / 2.0F) * 1.0F, topY, side * reach};
            b.rope(from[i][0], from[i][1], from[i][2], to[i][0], to[i][1], to[i][2]);
            // the deadeye and chain plate on the hull
            b.box(BLACK, 0, 10, xs[i] - 0.5F, deckY + 0.6F, side > 0 ? w : -w - 0.6F, 1.0F, 1.0F, 0.6F);
            b.box(IRON, 18, 4, xs[i] - 0.15F, deckY + 1.6F, side > 0 ? w + 0.1F : -w - 0.3F, 0.3F, 3.0F, 0.2F);
        }
        for (int r = 1; r <= rungs; r++) {
            float t = r / (float) (rungs + 1);
            float[] a = from[0];
            float[] c = from[xs.length - 1];
            float[] ta = to[0];
            float[] tc = to[xs.length - 1];
            b.line(ROPE, a[0] + (ta[0] - a[0]) * t, a[1] + (ta[1] - a[1]) * t, a[2] + (ta[2] - a[2]) * t,
                c[0] + (tc[0] - c[0]) * t, c[1] + (tc[1] - c[1]) * t, c[2] + (tc[2] - c[2]) * t, 0.2F);
        }
    }
}

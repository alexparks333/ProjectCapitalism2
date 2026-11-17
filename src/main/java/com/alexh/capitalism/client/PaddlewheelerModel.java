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

import java.util.List;

import static com.alexh.capitalism.client.BoatKit.*;

// The Steampunk Paddlewheeler (boats/Paddlewheeler, Boats.md): about 3.6 blocks long. An oxblood hull with riveted
// brass bands and brass-rimmed portholes that glow (the hull-side pictures); two great side paddlewheels under
// sunburst paddle boxes, turning with its speed; an open engine bay where it all moves: the big drive gear on the
// wheels' shaft and the pinion it meshes with (turning faster, the other way), a spoked flywheel, two brass cylinders
// whose piston rods pump back and forth, a centrifugal governor whose balls fly out as it speeds up; a riveted boiler
// (glowing firebox, gauges) feeding brass pipes with red valve wheels; twin smokestacks with brass crowns (smoke and
// steam pour out when it's driven: boats/Paddlewheeler); the deck house with lit portholes and the pilothouse on top
// with its big spoked wheel; a bow searchlight, bell and capstan; brass rails; a pennant.
public class PaddlewheelerModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "paddlewheeler"), "main");

    static final int STERN = -28;
    static final int BOW = 30;
    static final float DECK_Y = -2.0F;
    static final float WHEEL_Y = -4.0F;
    static final float WHEEL_R = 8.0F;
    static final float SHAFT_X = 0.0F;
    static final float PINION_R = 2.0F;
    static final float GEAR_R = 4.5F;

    static int halfWidth(int x) {
        double xc = x + 0.5;
        if (xc <= 10) return 9;
        double t = Math.min(1.0, (xc - 10) / 20.0);
        return Math.max(1, (int) Math.round(9 * Math.pow(1 - t * t, 0.6)));
    }

    static int top(int x) {
        return x >= 20 ? -6 : -5;
    }

    private final ModelPart wheelPort;
    private final ModelPart wheelStar;
    private final ModelPart gear;
    private final ModelPart pinion;
    private final ModelPart flywheel;
    private final ModelPart pistonA;
    private final ModelPart pistonB;
    private final ModelPart governor;
    private final ModelPart pilotWheel;
    private final List<ModelPart> pennant;

    public PaddlewheelerModel(ModelPart root) {
        super(root);
        this.wheelPort = this.boat.getChild("wheel_port").getChild("spin");
        this.wheelStar = this.boat.getChild("wheel_star").getChild("spin");
        this.gear = this.boat.getChild("gear_mount").getChild("spin");
        this.pinion = this.boat.getChild("pinion_mount").getChild("spin");
        this.flywheel = this.boat.getChild("flywheel_mount").getChild("spin");
        this.pistonA = this.boat.getChild("piston_a");
        this.pistonB = this.boat.getChild("piston_b");
        this.governor = this.boat.getChild("governor");
        this.pilotWheel = this.boat.getChild("pilot_wheel");
        this.pennant = strips(this.boat.getChild("pennant"), 8);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.012F, 0.01F, 0.0F);
        // everything turns off the wheels' shaft (a slow idle when it's still)
        float angle = t * (0.03F + 0.3F * speed);
        this.wheelPort.xRot = angle;
        this.wheelStar.xRot = angle;
        this.gear.xRot = angle;
        this.flywheel.xRot = angle;
        this.pinion.xRot = -angle * (GEAR_R / PINION_R);
        // the piston rods pump with the pinion's crank
        float crank = -angle * (GEAR_R / PINION_R);
        this.pistonA.x = 1.2F * Mth.cos(crank);
        this.pistonB.x = 1.2F * Mth.cos(crank + (float) Math.PI);
        // the governor spins, its balls flying out with speed
        this.governor.yRot = t * (0.1F + 0.6F * speed);
        float fling = 0.35F + 0.7F * speed;
        this.governor.getChild("arm0").zRot = fling;
        this.governor.getChild("arm1").zRot = -fling;
        this.pilotWheel.xRot = Mth.clamp(turn(state), -6.0F, 6.0F) * 0.3F;
        wave(this.pennant, t, 0.4F + 1.0F * speed, 0.45F, 0.7F);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, PaddlewheelerModel::halfWidth, PaddlewheelerModel::top,
            new float[][]{{1, 2.5F, 0.85F}, {2.5F, 3.5F, 0.5F}}, 2);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int tp = top(x);
            int inner = w - t;
            k.box(BRASS, (x - STERN) % 30, 0, x, tp - 0.4F, w - t - 0.1F, 1, 0.45F, t + 0.3F);
            k.box(BRASS, (x - STERN) % 30, 0, x, tp - 0.4F, -w - 0.2F, 1, 0.45F, t + 0.3F);
            if (inner < 1) continue;
            k.box(DARKWOOD, (x - STERN) % 30, 0, x, DECK_Y, -inner, 1, 1 - DECK_Y, 2 * inner);
            k.deck(x, DECK_Y, inner);
        }
        wheels(k, boat);
        engine(k, boat);
        boilerAndStacks(k);
        cabins(k, boat);
        bow(k);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // a part turning round the z axis (across the boat): a mount turned a quarter, a "spin" child turning round its x
    private static PartDefinition acrossSpinner(PartDefinition parent, String name, float x, float y, float z) {
        PartDefinition mount = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, 0.0F, (float) (Math.PI / 2), 0.0F));
        return mount.addOrReplaceChild("spin", CubeListBuilder.create(), PartPose.ZERO);
    }

    // the two paddlewheels (they turn: setupAnim) and their sunburst paddle boxes
    private static void wheels(BoatKit k, PartDefinition boat) {
        for (int s = -1; s <= 1; s += 2) {
            float zc = s * 12.0F;
            PartDefinition spin = acrossSpinner(boat, s > 0 ? "wheel_port" : "wheel_star", SHAFT_X, WHEEL_Y, zc);
            CubeListBuilder hub = CubeListBuilder.create();
            box(hub, BRASS, 0, 0, -2.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F);
            spin.addOrReplaceChild("hub", hub, PartPose.ZERO);
            for (int side = -1; side <= 1; side += 2) {
                PartDefinition face = spin.addOrReplaceChild("face" + side, CubeListBuilder.create(), PartPose.offset(side * 1.5F, 0, 0));
                rim(face, "rim", BRASS, WHEEL_R, 0.5F, 0.5F, 16);
                rim(face, "inner", BRASS, WHEEL_R * 0.55F, 0.35F, 0.35F, 12);
                spokes(face, "spoke", DARKWOOD, WHEEL_R, 0.4F, 8, 0.0F);
            }
            for (int b = 0; b < 8; b++) {
                CubeListBuilder board = CubeListBuilder.create();
                box(board, WOOD, b * 2, 0, -1.8F, -WHEEL_R + 0.2F, -0.3F, 3.6F, 2.4F, 0.6F);
                box(board, GREY, 0, 0, -1.85F, -WHEEL_R + 0.1F, -0.35F, 3.7F, 0.3F, 0.7F);
                spin.addOrReplaceChild("board" + b, board, PartPose.offsetAndRotation(0, 0, 0, (float) (b * Math.PI / 4 + Math.PI / 8), 0, 0));
            }
            // the paddle box: an arch over the wheel's top half, and its outer face a sunburst of red and brass
            float zOut = s > 0 ? zc + 2.4F : zc - 2.4F;
            for (int a = 0; a < 9; a++) {
                double ang = Math.PI * (a + 0.5) / 9;
                float px = (float) (-Math.cos(ang) * (WHEEL_R + 1.2F));
                float py = (float) (-Math.sin(ang) * (WHEEL_R + 1.2F));
                CubeListBuilder seg = CubeListBuilder.create();
                box(seg, DARKWOOD, a * 3, 0, -1.6F, -0.6F, -2.6F, 3.2F, 1.2F, 5.2F);
                k.part(seg, PartPose.offsetAndRotation(SHAFT_X + px, WHEEL_Y + py, zc, 0, 0, (float) (ang - Math.PI / 2)));
                CubeListBuilder ray = CubeListBuilder.create();
                box(ray, a % 2 == 0 ? RED : BRASS, 0, 0, -0.9F, -(WHEEL_R + 1.0F), -0.15F, 1.8F, WHEEL_R + 1.0F, 0.3F);
                k.part(ray, PartPose.offsetAndRotation(SHAFT_X, WHEEL_Y, zOut, 0, 0, (float) (ang - Math.PI / 2)));
            }
            k.box(BRASS, SHAFT_X - 1.5F, WHEEL_Y - 1.5F, s > 0 ? zOut : zOut - 0.4F, 3.0F, 3.0F, 0.4F);     // the sunburst's boss
            k.box(GOLD, SHAFT_X - 0.6F, WHEEL_Y - 0.6F, s > 0 ? zOut + 0.4F : zOut - 0.6F, 1.2F, 1.2F, 0.2F);
            k.box(DARKWOOD, SHAFT_X - WHEEL_R - 1.8F, WHEEL_Y, s > 0 ? zc - 2.6F : zc - 2.6F, 2.0F, 1.0F, 5.2F);  // the box's feet
            k.box(DARKWOOD, SHAFT_X + WHEEL_R - 0.2F, WHEEL_Y, zc - 2.6F, 2.0F, 1.0F, 5.2F);
            // sponson: the deck reaching out to carry the wheel
            k.box(DARKWOOD, SHAFT_X - WHEEL_R - 2.0F, WHEEL_Y + 0.4F, s > 0 ? 9.0F : -14.6F, 2 * WHEEL_R + 4.0F, 0.8F, 5.6F);
        }
        // the shaft across, through the engine bay
        k.box(GREY, 0, 0, SHAFT_X - 0.6F, WHEEL_Y - 0.6F, -14.0F, 1.2F, 1.2F, 28.0F);
    }

    // the engine bay: drive gear and pinion, flywheel, cylinders and pistons, the governor
    private static void engine(BoatKit k, PartDefinition boat) {
        // bed plate
        k.box(ENGINE, 0, 0, -6.0F, DECK_Y - 0.6F, -7.0F, 13.0F, 0.6F, 14.0F);
        // the drive gear on the shaft, the pinion above it, the flywheel
        PartDefinition gear = acrossSpinner(boat, "gear_mount", SHAFT_X, WHEEL_Y, 4.5F);
        gear(gear, BRASS, GEAR_R, 0.8F, 18);
        PartDefinition pinion = acrossSpinner(boat, "pinion_mount", SHAFT_X + 4.6F, WHEEL_Y - 4.6F, 4.5F);
        gear(pinion, BRASS, PINION_R, 0.55F, 8);
        k.box(GREY, 0, 0, SHAFT_X + 4.0F, WHEEL_Y - 5.2F, 2.8F, 1.2F, 1.2F, 4.0F);       // pinion's axle and bearings
        k.box(ENGINE, 0, 10, SHAFT_X + 3.7F, WHEEL_Y - 5.5F, 2.2F, 1.8F, 6.3F, 0.8F);
        PartDefinition flywheel = acrossSpinner(boat, "flywheel_mount", SHAFT_X, WHEEL_Y, -4.5F);
        rim(flywheel, "rim", ENGINE, 3.6F, 0.9F, 0.9F, 12);
        spokes(flywheel, "spoke", BRASS, 3.6F, 0.4F, 6, 0.0F);
        // two brass cylinders lying fore-and-aft, their piston rods pumping (setupAnim)
        for (int c = 0; c < 2; c++) {
            float z = c == 0 ? 2.2F : -2.2F;
            k.box(BRASS, 0, 0, 7.0F, DECK_Y - 4.2F, z - 1.3F, 4.5F, 2.6F, 2.6F);
            k.box(BRASS, 0, 0, 7.5F, DECK_Y - 4.6F, z - 0.9F, 3.5F, 3.4F, 1.8F);
            k.box(ENGINE, 0, 0, 6.8F, DECK_Y - 4.4F, z - 1.4F, 0.3F, 3.0F, 2.8F);
            k.box(ENGINE, 0, 0, 11.4F, DECK_Y - 4.4F, z - 1.4F, 0.3F, 3.0F, 2.8F);
            CubeListBuilder rod = CubeListBuilder.create();
            box(rod, GREY, 0, 0, 0.0F, -0.25F, -0.25F, 4.0F, 0.5F, 0.5F);
            box(rod, BRASS, 4, 4, 0.0F, -0.6F, -0.6F, 0.8F, 1.2F, 1.2F);
            boat.addOrReplaceChild(c == 0 ? "piston_a" : "piston_b", rod, PartPose.offset(3.0F, DECK_Y - 2.9F, z));
        }
        // the centrifugal governor on its column: spinning, two brass balls on arms
        k.box(ENGINE, 0, 0, 2.6F, DECK_Y - 8.0F, -0.4F, 0.8F, 8.0F, 0.8F);
        CubeListBuilder spindle = CubeListBuilder.create();
        box(spindle, BRASS, 0, 0, -0.15F, -3.0F, -0.15F, 0.3F, 3.0F, 0.3F);
        box(spindle, BRASS, 4, 0, -0.4F, -0.6F, -0.4F, 0.8F, 0.6F, 0.8F);
        PartDefinition gov = boat.addOrReplaceChild("governor", spindle, PartPose.offset(3.0F, DECK_Y - 8.0F, 0.0F));
        for (int a = 0; a < 2; a++) {
            CubeListBuilder arm = CubeListBuilder.create();
            box(arm, GREY, 0, 0, -0.1F, 0.0F, -0.1F, 0.2F, 2.2F, 0.2F);
            box(arm, GOLD, 0, 0, -0.5F, 1.9F, -0.5F, 1.0F, 1.0F, 1.0F);
            gov.addOrReplaceChild("arm" + a, arm, PartPose.offset(0.0F, -3.0F, 0.0F));
        }
        // railings round the bay
        float[][] railP = {{-6, DECK_Y, 7}, {7, DECK_Y, 7}};
        float[][] railS = {{-6, DECK_Y, -7}, {7, DECK_Y, -7}};
        k.railing(BRASS, railP, 2.4F, 0.3F);
        k.railing(BRASS, railS, 2.4F, 0.3F);
    }

    // the boiler (glowing firebox), gauges, pipes and valves, the twin stacks
    private static void boilerAndStacks(BoatKit k) {
        float bx = 12.0F;
        k.box(ENGINE, 0, 0, bx, DECK_Y - 7.0F, -2.5F, 9.0F, 7.0F, 5.0F);
        k.box(ENGINE, 0, 0, bx, DECK_Y - 6.0F, -3.5F, 9.0F, 5.0F, 7.0F);
        for (float x = bx + 1; x < bx + 9; x += 2.5F) {
            k.box(BRASS, 0, 0, x, DECK_Y - 7.2F, -2.7F, 0.5F, 7.2F, 5.4F);
            k.box(BRASS, 0, 0, x, DECK_Y - 6.2F, -3.7F, 0.5F, 5.2F, 7.4F);
        }
        k.box(FIRE, bx - 0.1F, DECK_Y - 3.6F, -1.5F, 0.1F, 2.4F, 3.0F);          // the firebox door's glow
        for (int i = 0; i < 3; i++) k.box(ENGINE, bx - 0.2F, DECK_Y - 3.4F + i * 0.8F, -1.6F, 0.15F, 0.25F, 3.2F);
        k.box(GAUGE, 0, 0, bx + 2.0F, DECK_Y - 6.4F, 3.55F, 1.2F, 1.2F, 0.06F);
        k.box(GAUGE, 4, 0, bx + 4.5F, DECK_Y - 6.4F, 3.55F, 1.2F, 1.2F, 0.06F);
        // pipes to the cylinders, valve wheels
        k.line(BRASS, bx, DECK_Y - 5.0F, 2.6F, 11.2F, DECK_Y - 4.0F, 2.2F, 0.6F);
        k.line(BRASS, bx, DECK_Y - 5.0F, -2.6F, 11.2F, DECK_Y - 4.0F, -2.2F, 0.6F);
        k.line(BRASS, bx + 4.0F, DECK_Y - 7.0F, 0.0F, bx + 4.0F, DECK_Y - 10.0F, 0.0F, 0.5F);
        k.box(RED, bx + 3.2F, DECK_Y - 10.4F, -0.8F, 1.6F, 0.3F, 1.6F);
        k.line(BRASS, bx + 8.0F, DECK_Y - 3.0F, 3.6F, bx + 8.0F, DECK_Y - 3.0F, 6.5F, 0.5F);
        k.box(RED, bx + 7.2F, DECK_Y - 3.8F, 5.0F, 1.6F, 1.6F, 0.3F);
        // the twin smokestacks with flared brass crowns and a crossbar
        for (int s = -1; s <= 1; s += 2) {
            float z = s * 3.2F;
            float sx = bx + 6.0F;
            k.box(ENGINE, 0, 0, sx - 1.2F, -40.0F, z - 0.8F, 2.4F, 40.0F + DECK_Y - 7.0F, 1.6F);
            k.box(ENGINE, 0, 0, sx - 0.8F, -40.0F, z - 1.2F, 1.6F, 40.0F + DECK_Y - 7.0F, 2.4F);
            for (float y = -38; y <= -14; y += 8) k.box(BRASS, 0, 0, sx - 1.35F, y, z - 1.35F, 2.7F, 0.5F, 2.7F);
            k.box(BRASS, 0, 0, sx - 1.8F, -42.0F, z - 1.8F, 3.6F, 2.0F, 3.6F);           // the crown
            for (int p = 0; p < 4; p++) {
                double a = p * Math.PI / 2 + Math.PI / 4;
                k.box(BRASS, 0, 0, sx + (float) Math.cos(a) * 1.8F - 0.4F, -43.6F, z + (float) Math.sin(a) * 1.8F - 0.4F, 0.8F, 1.6F, 0.8F);
            }
            k.box(BLACK, 0, 0, sx - 1.0F, -42.05F, z - 1.0F, 2.0F, 0.05F, 2.0F);
        }
        k.box(BRASS, bx + 5.6F, -30.0F, -3.2F, 0.8F, 0.6F, 6.4F);
        k.box(GOLD, bx + 5.4F, -31.2F, -0.8F, 1.2F, 1.8F, 1.6F);
        k.box(BRASS, bx + 4.6F, -36.0F, 3.6F, 0.8F, 2.4F, 0.8F);                     // steam whistle
    }

    // the deck house aft (lit portholes), the pilothouse on top with its big wheel, the pennant
    private static void cabins(BoatKit k, PartDefinition boat) {
        float x0 = -26.0F;
        float x1 = -8.0F;
        for (float x = x0; x < x1; x += 6) k.box(DARKWOOD, 0, 0, x, -12.0F, -7.5F, 6.0F, 10.0F, 15.0F);
        for (float x = x0 + 2; x < x1 - 1; x += 4) {
            k.pair(BRASS, x - 0.2F, -9.2F, 7.5F, 2.0F, 2.0F, 0.15F);
            k.pair(WARM, x + 0.2F, -8.8F, 7.6F, 1.2F, 1.2F, 0.1F);
        }
        k.box(BRASS, x0 - 0.6F, -12.6F, -8.1F, x1 - x0 + 1.2F, 0.6F, 16.2F);         // the roof
        k.box(DARKWOOD, x1 - 0.1F, -9.0F, -1.2F, 0.2F, 7.0F, 2.4F);                   // door
        k.box(GOLD, x1 + 0.1F, -5.6F, 0.6F, 0.2F, 0.4F, 0.4F);
        // the pilothouse: an open bridge on the deck house roof, waist-high panelled sides, a glowing windscreen in front,
        // a canopy high up on brass posts (with a brass finial), so whoever steers can stand (sit) tall
        float px0 = -21.0F;
        float px1 = -12.0F;
        float floor = -12.6F;
        k.pair(DARKWOOD, px0, floor - 4.0F, 4.2F, px1 - px0, 4.0F, 0.8F);
        k.box(DARKWOOD, 0, 0, px0, floor - 4.0F, -4.2F, 0.8F, 4.0F, 8.4F);
        k.box(DARKWOOD, 0, 0, px1 - 0.8F, floor - 4.0F, -4.2F, 0.8F, 4.0F, 8.4F);
        k.box(WARM, px1 - 0.5F, floor - 8.6F, -4.0F, 0.1F, 4.6F, 8.0F);
        k.box(BRASS, 0, 0, px1 - 0.7F, floor - 9.0F, -4.4F, 0.5F, 0.4F, 8.8F);
        for (int s = -1; s <= 1; s += 2) {
            k.box(BRASS, 0, 0, px0 + 0.2F, floor - 24.0F, s * 4.6F - 0.2F, 0.4F, 20.0F, 0.4F);
            k.box(BRASS, 0, 0, px1 - 0.6F, floor - 24.0F, s * 4.6F - 0.2F, 0.4F, 20.0F, 0.4F);
        }
        k.box(DARKWOOD, 0, 0, px0 - 0.6F, floor - 25.0F, -5.6F, px1 - px0 + 1.2F, 1.0F, 11.2F);
        k.box(BRASS, 0, 0, px0 + 1.0F, floor - 26.0F, -3.6F, px1 - px0 - 2.0F, 1.0F, 7.2F);
        k.box(GOLD, (px0 + px1) / 2 - 0.5F, floor - 27.4F, -0.5F, 1.0F, 1.4F, 1.0F);
        // the big spoked pilot's wheel, upright and facing aft toward whoever steers (it turns: setupAnim)
        k.line(BRASS, px1 - 0.8F, floor - 8.0F, 0.0F, px1 - 2.3F, floor - 8.0F, 0.0F, 0.5F);   // its bracket off the front
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, BRASS, 0, 0, -0.4F, -0.5F, -0.5F, 0.8F, 1.0F, 1.0F);
        PartDefinition wheel = boat.addOrReplaceChild("pilot_wheel", hub, PartPose.offset(px1 - 2.5F, floor - 8.0F, 0.0F));
        rim(wheel, "rim", DARKWOOD, 2.6F, 0.4F, 0.4F, 12);
        spokes(wheel, "spoke", DARKWOOD, 3.4F, 0.3F, 8, 0.0F);
        // the pennant on the canopy
        k.box(BRASS, 0, 0, px0 + 0.8F, floor - 33.0F, -0.15F, 0.3F, 8.0F, 0.3F);
        PartDefinition pennant = boat.addOrReplaceChild("pennant", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float h = 1.6F * (1 - i / 8.0F) + 0.2F;
            CubeListBuilder strip = CubeListBuilder.create();
            box(strip, RED, i * 2, 0, -1.0F, 0, 0, 1.0F, h, 0.0F);
            pennant.addOrReplaceChild("f" + i, strip, PartPose.offset(px0 + 0.8F - i, floor - 32.8F, 0.0F));
        }
    }

    // the bow: brass rail, searchlight (glowing), bell, capstan
    private static void bow(BoatKit k) {
        float[][] port = new float[5][];
        float[][] star = new float[5][];
        int[] xs = {14, 18, 22, 26, 29};
        for (int i = 0; i < xs.length; i++) {
            float z = halfWidth(xs[i]) - 0.6F;
            port[i] = new float[]{xs[i], top(xs[i]), z};
            star[i] = new float[]{xs[i], top(xs[i]), -z};
        }
        k.railing(BRASS, port, 2.4F, 0.3F);
        k.railing(BRASS, star, 2.4F, 0.3F);
        // searchlight on a post
        k.box(BRASS, 0, 0, 25.6F, DECK_Y - 5.0F, -0.3F, 0.6F, 5.0F, 0.6F);
        k.box(BRASS, 0, 0, 24.8F, DECK_Y - 6.8F, -1.0F, 2.6F, 2.0F, 2.0F);
        k.box(NAV_WHITE, 27.4F, DECK_Y - 6.5F, -0.7F, 0.15F, 1.4F, 1.4F);
        // the ship's bell under a little frame
        k.box(BRASS, 0, 0, 21.0F, DECK_Y - 4.0F, 2.0F, 0.4F, 4.0F, 0.4F);
        k.box(BRASS, 0, 0, 21.0F, DECK_Y - 4.0F, 4.6F, 0.4F, 4.0F, 0.4F);
        k.box(BRASS, 0, 0, 20.9F, DECK_Y - 4.4F, 2.0F, 0.6F, 0.4F, 3.0F);
        k.box(GOLD, 0, 0, 20.4F, DECK_Y - 3.6F, 2.6F, 1.4F, 1.6F, 1.6F);
        // capstan
        k.box(BRASS, 0, 0, 22.0F, DECK_Y - 2.0F, -4.0F, 2.0F, 2.0F, 2.0F);
        k.box(DARKWOOD, 0, 0, 21.6F, DECK_Y - 2.4F, -4.4F, 2.8F, 0.4F, 2.8F);
        k.line(DARKWOOD, 20.0F, DECK_Y - 2.2F, -3.0F, 26.0F, DECK_Y - 2.2F, -3.0F, 0.3F);
    }
}

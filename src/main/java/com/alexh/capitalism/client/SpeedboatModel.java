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

// The Speedboat (boats/Speedboat, Boats.md): a sleek deep-V runabout about 2.5 blocks long. Glossy white hull with red
// racing stripes sweeping up to the bow, a gold lightning bolt and speed slashes (the hull-side pictures), a red bottom,
// a chrome rub rail; the foredeck with a cream sun pad, chrome bow rail, cleat and red/green running lights; a tinted
// wraparound windshield in chrome; a cockpit of teak with white leather bucket seats, a bench behind, a dash with
// glowing gauges, a chrome wheel that turns as you steer and a throttle; twin black outboards whose propellers spin
// with its speed; a stern light on a pole with a little red pennant; blue underwater lights. It lifts its bow as it
// gets up to speed.
public class SpeedboatModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "speedboat"), "main");

    static final int STERN = -18;
    static final int BOW = 21;
    static final int COCKPIT_END = 4;    // the cockpit up to here; the foredeck from 5
    static final float FLOOR = -1.0F;

    static int halfWidth(int x) {
        double xc = x + 0.5;
        if (xc <= 2) return 9;
        double t = Math.min(1.0, (xc - 2) / 20.0);
        return Math.max(1, (int) Math.round(9 * Math.pow(1 - t * t, 0.6)));
    }

    static int top(int x) {
        return -5 - (x >= 13 ? 1 : 0) - (x >= 18 ? 1 : 0);
    }

    private final ModelPart wheel;
    private final ModelPart[] props = new ModelPart[2];
    private final List<ModelPart> pennant;

    public SpeedboatModel(ModelPart root) {
        super(root);
        this.wheel = this.boat.getChild("wheel_mount").getChild("wheel");
        this.props[0] = this.boat.getChild("prop0");
        this.props[1] = this.boat.getChild("prop1");
        this.pennant = strips(this.boat.getChild("pennant"), 8);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        // planing: the bow lifts as it speeds up
        this.rock(state, 0.015F, 0.01F, -0.07F * speed);
        // propellers spin with the throttle, the wheel turns as it steers
        for (ModelPart prop : this.props) prop.xRot = t * (0.15F + 2.2F * speed);
        this.wheel.xRot = Mth.clamp(turn(state), -6.0F, 6.0F) * 0.3F;
        wave(this.pennant, t, 0.4F + 1.2F * speed, 0.5F, 0.8F);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, SpeedboatModel::halfWidth, SpeedboatModel::top,
            new float[][]{{1, 2.5F, 0.82F}, {2.5F, 3.8F, 0.5F}, {3.8F, 4.8F, 0.22F}}, 1);
        k.box(BOTTOM, 0, 26, STERN + 1, 4.8F, -0.5F, BOW - STERN - 4, 0.8F, 1.0F);   // keel strip
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int tp = top(x);
            int inner = w - t;
            // chrome rub rail along the top of each side
            k.box(CHROME, (x - STERN) % 20, 0, x, tp - 0.4F, w - t - 0.1F, 1, 0.45F, t + 0.4F);
            k.box(CHROME, (x - STERN) % 20, 0, x, tp - 0.4F, -w - 0.3F, 1, 0.45F, t + 0.4F);
            if (inner < 1) continue;
            if (x <= STERN + 1 || x > COCKPIT_END) {
                // the stern deck over the engines, and the foredeck: solid glossy white to the top
                k.box(FIBER, (x - STERN) * 2 % 30, 0, x, tp, -inner, 1, 1 - tp, 2 * inner);
            } else {
                // the cockpit: teak floor, cream vinyl inside the sides, a dark well under the floor
                k.box(BLACK, 0, 0, x, FLOOR, -inner, 1, 2.0F, 2 * inner);
                k.deck(x, FLOOR, inner);
                k.box(LINING, (x - STERN) % 20, 0, x, tp + 0.4F, inner - 0.15F, 1, FLOOR - tp - 0.4F, 0.15F);
                k.box(LINING, (x - STERN) % 20, 0, x, tp + 0.4F, -inner, 1, FLOOR - tp - 0.4F, 0.15F);
            }
        }
        foredeck(k);
        windshieldAndDash(k, boat);
        seats(k);
        stern(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // sun pad, bow rail, cleat, running lights
    private static void foredeck(BoatKit k) {
        k.box(CANVAS, 0, 0, 8.0F, top(8) - 0.9F, -4.0F, 6.0F, 0.9F, 8.0F);           // sun pad
        k.box(CANVAS, 10, 10, 12.8F, top(12) - 1.5F, -3.6F, 1.4F, 0.7F, 3.2F);        // two pillows
        k.box(CANVAS, 10, 10, 12.8F, top(12) - 1.5F, 0.4F, 1.4F, 0.7F, 3.2F);
        k.box(CHROME, 0, 4, 7.7F, top(8) - 0.3F, -4.3F, 6.6F, 0.3F, 0.3F);           // its chrome trim
        k.box(CHROME, 0, 4, 7.7F, top(8) - 0.3F, 4.0F, 6.6F, 0.3F, 0.3F);
        // the bow rail: up each side to the bow, posts on the gunwale
        float[][] port = new float[5][];
        float[][] star = new float[5][];
        int[] xs = {9, 12, 15, 18, 20};
        for (int i = 0; i < xs.length; i++) {
            float z = halfWidth(xs[i]) - 0.6F;
            port[i] = new float[]{xs[i], top(xs[i]), z};
            star[i] = new float[]{xs[i], top(xs[i]), -z};
        }
        k.railing(CHROME, port, 2.2F, 0.3F);
        k.railing(CHROME, star, 2.2F, 0.3F);
        k.line(CHROME, 20.0F, top(20) - 2.2F, halfWidth(20) - 0.6F, 20.0F, top(20) - 2.2F, -halfWidth(20) + 0.6F, 0.35F);
        // cleat at the bow
        k.box(CHROME, 4, 8, 18.2F, top(18) - 0.5F, -0.3F, 0.6F, 0.5F, 0.6F);
        k.box(CHROME, 4, 8, 17.6F, top(18) - 0.8F, -0.25F, 1.8F, 0.3F, 0.5F);
        // red (port) and green (starboard) running lights on the bow sides
        int lx = 15;
        k.box(NAV_RED, lx, top(lx) + 0.2F, halfWidth(lx), 1.2F, 0.8F, 0.3F);
        k.box(NAV_GREEN, lx, top(lx) + 0.2F, -halfWidth(lx) - 0.3F, 1.2F, 0.8F, 0.3F);
    }

    // the wraparound windshield (centre and two angled sides, chrome-framed), the dash shelf, gauges, wheel, throttle
    private static void windshieldAndDash(BoatKit k, PartDefinition boat) {
        float wx = 5.4F;
        float base = top(5);
        // centre pane, leaning back
        CubeListBuilder centre = CubeListBuilder.create();
        box(centre, TINT, 0, 0, -0.15F, -5.0F, -5.0F, 0.3F, 5.0F, 10.0F);
        box(centre, CHROME, 0, 0, -0.3F, -5.3F, -5.2F, 0.6F, 0.4F, 10.4F);
        box(centre, CHROME, 2, 0, -0.25F, -5.0F, -0.15F, 0.5F, 5.0F, 0.3F);
        k.part(centre, PartPose.offsetAndRotation(wx, base, 0.0F, 0.0F, 0.0F, -0.45F));
        // side panes sweeping back
        for (int side = -1; side <= 1; side += 2) {
            CubeListBuilder pane = CubeListBuilder.create();
            box(pane, TINT, 20, 0, -0.15F, -4.4F, side > 0 ? 0.0F : -4.2F, 0.3F, 4.4F, 4.2F);
            box(pane, CHROME, 4, 0, -0.3F, -4.7F, side > 0 ? 0.0F : -4.2F, 0.6F, 0.4F, 4.2F);
            k.part(pane, PartPose.offsetAndRotation(wx - 0.3F, base, side * 5.0F, 0.0F, side * -0.85F, -0.2F));
        }
        // the dash shelf under it, with gauges facing the driver
        k.box(BLACK, 0, 0, 3.6F, -7.6F, -7.0F, 2.6F, 1.2F, 14.0F);
        k.box(CHROME, 4, 4, 3.5F, -7.8F, -7.1F, 2.8F, 0.25F, 14.2F);
        for (int g = 0; g < 3; g++) k.box(GAUGE, g * 4, 0, 3.55F, -7.4F, -5.6F + g * 1.3F, 0.06F, 0.9F, 0.9F);
        k.box(GAUGE, 12, 0, 3.55F, -7.4F, 2.0F, 0.06F, 0.8F, 2.4F);   // the passenger side's screen
        // the wheel, tilted toward the driver (it turns: setupAnim)
        PartDefinition mount = boat.addOrReplaceChild("wheel_mount", CubeListBuilder.create(), PartPose.offsetAndRotation(3.2F, -7.6F, -3.5F, 0.0F, 0.0F, -0.55F));
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, CHROME, 0, 0, -0.3F, -0.4F, -0.4F, 0.6F, 0.8F, 0.8F);
        box(hub, BLACK, 0, 0, 0.0F, -0.3F, -0.3F, 1.0F, 0.6F, 0.6F);
        PartDefinition wheel = mount.addOrReplaceChild("wheel", hub, PartPose.ZERO);
        rim(wheel, "rim", CHROME, 1.9F, 0.35F, 0.35F, 10);
        spokes(wheel, "spoke", CHROME, 1.9F, 0.2F, 3, 0.0F);
        // throttle beside the driver
        k.box(CHROME, 6, 6, 0.0F, -4.5F, -7.2F, 1.2F, 0.6F, 1.0F);
        k.line(CHROME, 0.6F, -4.6F, -6.7F, 1.6F, -7.0F, -6.7F, 0.25F);
        k.box(RED, 0, 0, 1.3F, -7.6F, -7.05F, 0.7F, 0.7F, 0.7F);
    }

    // white leather bucket seats for the driver and passenger, the bench behind, a centre console
    private static void seats(BoatKit k) {
        for (int side = -1; side <= 1; side += 2) {
            float zc = side * 3.5F;
            float seatX = -5.0F;
            k.box(LEATHER, 0, 0, seatX - 2.0F, -3.0F, zc - 2.0F, 4.0F, 2.0F, 4.0F);            // cushion
            k.box(LEATHER, 0, 10, seatX - 3.0F, -9.0F, zc - 2.0F, 1.0F, 6.0F, 4.0F);           // back
            k.box(LEATHER, 16, 10, seatX - 3.2F, -10.4F, zc - 1.2F, 1.0F, 1.4F, 2.4F);         // headrest
            k.box(LEATHER, 20, 0, seatX - 2.0F, -3.5F, zc - 2.1F, 4.0F, 0.5F, 0.5F);           // bolsters
            k.box(LEATHER, 20, 0, seatX - 2.0F, -3.5F, zc + 1.6F, 4.0F, 0.5F, 0.5F);
            k.box(CHROME, 0, 8, seatX - 3.4F, -9.6F, zc - 0.1F, 0.2F, 0.6F, 0.2F);             // headrest posts
        }
        // the bench across the back
        k.box(LEATHER, 0, 0, -14.5F, -3.0F, -7.5F, 4.5F, 2.0F, 15.0F);
        k.box(LEATHER, 0, 10, -15.5F, -8.0F, -7.5F, 1.0F, 5.0F, 15.0F);
        k.box(LEATHER, 20, 0, -14.5F, -4.4F, -7.6F, 4.0F, 1.4F, 0.8F);    // armrests
        k.box(LEATHER, 20, 0, -14.5F, -4.4F, 6.8F, 4.0F, 1.4F, 0.8F);
        // centre console with chrome cup holders
        k.box(FIBER, 0, 0, -6.0F, -4.0F, -1.0F, 7.0F, 3.0F, 2.0F);
        k.box(CHROME, 6, 0, -4.5F, -4.15F, -0.6F, 1.2F, 0.2F, 1.2F);
        k.box(CHROME, 6, 0, -2.5F, -4.15F, -0.6F, 1.2F, 0.2F, 1.2F);
        // grab rails along the cockpit sides
        for (int side = -1; side <= 1; side += 2) {
            float[][] rail = new float[4][];
            for (int i = 0; i < 4; i++) {
                int x = -14 + i * 5;
                rail[i] = new float[]{x, top(x), side * (halfWidth(x) - 0.6F)};
            }
            k.railing(CHROME, rail, 1.2F, 0.25F);
        }
    }

    // twin outboards (spinning props), stern light pole with a pennant, underwater lights, a swim ladder
    private static void stern(BoatKit k, PartDefinition boat) {
        for (int e = 0; e < 2; e++) {
            float zc = e == 0 ? 3.5F : -3.5F;
            k.box(CHROME, 0, 0, STERN - 0.8F, -5.0F, zc - 1.0F, 0.8F, 4.0F, 2.0F);            // bracket
            k.box(ENGINE, 0, 0, STERN - 5.0F, -10.0F, zc - 1.7F, 4.2F, 5.2F, 3.4F);           // cowling
            k.box(ENGINE, 0, 14, STERN - 4.6F, -10.6F, zc - 1.3F, 3.4F, 0.6F, 2.6F);          // its rounded top
            k.box(ENGINE, 0, 14, STERN - 5.4F, -9.4F, zc - 1.3F, 0.4F, 4.0F, 2.6F);           // its rounded back
            k.box(CHROME, 10, 10, STERN - 5.05F, -8.2F, zc - 0.8F, 0.06F, 0.6F, 1.6F);        // badge
            k.box(BLACK, 0, 0, STERN - 3.6F, -4.8F, zc - 0.75F, 1.6F, 6.4F, 1.5F);            // midsection
            k.box(GREY, 0, 0, STERN - 5.0F, 1.6F, zc - 1.8F, 3.6F, 0.3F, 3.6F);               // cavitation plate
            k.box(BLACK, 0, 6, STERN - 5.0F, 1.9F, zc - 0.7F, 4.0F, 1.4F, 1.4F);              // gear case
            k.box(BLACK, 0, 6, STERN - 3.4F, 3.3F, zc - 0.3F, 1.0F, 1.8F, 0.6F);              // skeg
            CubeListBuilder hub = CubeListBuilder.create();
            box(hub, CHROME, 0, 0, -0.5F, -0.45F, -0.45F, 0.8F, 0.9F, 0.9F);
            PartDefinition prop = boat.addOrReplaceChild("prop" + e, hub, PartPose.offset(STERN - 5.3F, 2.6F, zc));
            for (int b = 0; b < 3; b++) {
                CubeListBuilder blade = CubeListBuilder.create();
                box(blade, GREY, 4, 4, -0.15F, -2.3F, -0.55F, 0.3F, 1.9F, 1.1F);
                prop.addOrReplaceChild("blade" + b, blade, PartPose.offsetAndRotation(0, 0, 0, (float) (b * 2 * Math.PI / 3), 0.35F, 0));
            }
        }
        // stern light on a chrome pole, a little red pennant streaming from it
        k.box(CHROME, 0, 0, STERN + 0.6F, -15.0F, -0.15F, 0.3F, 10.0F, 0.3F);
        k.box(NAV_WHITE, STERN + 0.35F, -16.0F, -0.4F, 0.8F, 1.0F, 0.8F);
        k.box(CHROME, 0, 4, STERN + 0.3F, -16.3F, -0.45F, 0.9F, 0.3F, 0.9F);
        PartDefinition pennant = boat.addOrReplaceChild("pennant", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float h = 1.6F * (1.0F - i / 8.0F) + 0.2F;
            CubeListBuilder strip = CubeListBuilder.create();
            box(strip, RED, i * 2, 0, -1.0F, 0.0F, 0.0F, 1.0F, h, 0.0F);
            pennant.addOrReplaceChild("f" + i, strip, PartPose.offset(STERN + 0.6F - i, -14.8F, 0.0F));
        }
        // blue underwater lights on the transom
        k.box(BLUE_GLOW, STERN - 0.1F, 0.2F, 5.8F, 0.1F, 0.8F, 1.4F);
        k.box(BLUE_GLOW, STERN - 0.1F, 0.2F, -7.2F, 0.1F, 0.8F, 1.4F);
        // a chrome swim ladder between the engines
        k.box(CHROME, 0, 0, STERN - 0.6F, -4.8F, 0.8F, 0.25F, 6.0F, 0.25F);
        k.box(CHROME, 0, 0, STERN - 0.6F, -4.8F, -1.05F, 0.25F, 6.0F, 0.25F);
        for (int r = 0; r < 3; r++) k.box(CHROME, 0, 0, STERN - 0.7F, -3.5F + r * 2.0F, -1.0F, 0.4F, 0.25F, 2.0F);
    }
}

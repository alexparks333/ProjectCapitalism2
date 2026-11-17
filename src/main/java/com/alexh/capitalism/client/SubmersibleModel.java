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

import static com.alexh.capitalism.client.BoatKit.*;

// The Clockwork Submersible (boats/Submersible, Boats.md): a waxed oxidized copper submarine riding half under the
// surface. Its round hull (verdigris plates, rivets, round portholes: the hull-side pictures) with gold bands, iron
// trapdoor hatches on its deck, lightning rods; the conning tower with its periscope (it sweeps round); the great
// wooden propeller at the stern in a copper shroud (it spins), fins and rudder; and at the front a glass dome, mostly
// under the water, where its riders sit at a glowing redstone control panel and look out (the glass is clear).
public class SubmersibleModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "submersible"), "main");

    static final int STERN = -24;
    static final int BOW = 4;
    static final float HULL_Y = 5.0F;       // the hull's middle (a round hull from y -5 to 15)
    static final float DOME_X = 15.0F;
    static final float DOME_Y = 15.0F;
    static final float DOME_R = 12.0F;

    static int halfWidth(int x) {
        double xc = x + 0.5;
        if (xc >= -16) return 9;
        double t = (-16 - xc) / 8.5;
        return Math.max(2, (int) Math.round(9 * Math.sqrt(Math.max(0, 1 - t * t))));
    }

    static int top(int x) {
        return -5;
    }

    private final ModelPart prop;
    private final ModelPart periscope;
    private final ModelPart beam;
    private final ModelPart panel;

    public SubmersibleModel(ModelPart root) {
        super(root);
        this.prop = this.boat.getChild("prop");
        this.periscope = this.boat.getChild("periscope");
        this.beam = this.boat.getChild("beam");
        this.panel = this.boat.getChild("panel");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.01F, 0.01F, 0.0F);
        this.prop.xRot = t * (0.1F + 1.4F * speed);
        this.periscope.yRot = 0.9F * Mth.sin(t * 0.012F);
        // the control panel isn't in the way of its rider's own view
        this.panel.visible = Float.isNaN(viewer(state));
        // the flashlight's beam, pointing where its rider looks (yaw: + right turns it to starboard, -z; pitch: + down)
        PremiumBoatRenderer.State lamp = state instanceof PremiumBoatRenderer.State s && s.lamp ? s : null;
        this.beam.visible = lamp != null;
        // its rider, looking from the dome, sees no shell (from inside it's a ring) but a soft haze down the beam;
        // everyone else sees the shell
        boolean rider = !Float.isNaN(viewer(state));
        if (lamp != null) {
            ModelPart tiltPart = this.beam.getChild("tilt");
            for (int i = 0; i < BEAM_PANELS; i++) tiltPart.getChild("spin" + i).visible = !rider;
            for (int i = 0; i < HAZE.length; i++) tiltPart.getChild("haze" + i).visible = rider;
        }
        if (lamp != null) {
            this.beam.yRot = lamp.lampYaw;
            this.beam.getChild("tilt").zRot = lamp.lampPitch;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        // the round hull: sides down to the waterline, then rounding under to the bottom
        int[] thick = k.hull(STERN, BOW, SubmersibleModel::halfWidth, SubmersibleModel::top,
            new float[][]{{1, 5, 1.0F}, {5, 9, 0.93F}, {9, 12, 0.75F}, {12, 14.5F, 0.45F}}, 0);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int inner = w - thick[x - STERN];
            if (inner < 1) continue;
            k.box(FIBER, (x - STERN) % 30, 0, x, -5.0F, -inner, 1, 6.0F, 2 * inner);    // the hull's top, rounded
            k.box(FIBER, (x - STERN) % 30, 4, x, -6.2F, -inner + 2, 1, 1.2F, 2 * inner - 4);
            k.deck(x, -6.2F, inner - 2);
        }
        // gold bands round the hull
        for (float bx : new float[]{-18.0F, -8.0F, 2.0F}) {
            PartDefinition band = boat.addOrReplaceChild("band" + (int) bx, CubeListBuilder.create(), PartPose.offset(bx, HULL_Y, 0.0F));
            rim(band, "rim", GOLD, 10.2F, 0.7F, 1.4F, 20);
        }
        // iron trapdoor hatches on the deck, with handles
        for (float hx : new float[]{-20.0F, -14.0F}) {
            k.box(GREY, hx - 1.5F, -6.6F, -1.5F, 3.0F, 0.4F, 3.0F);
            k.box(GREY, hx - 0.6F, -7.0F, -0.2F, 1.2F, 0.4F, 0.4F);
        }
        tower(k, boat);
        dome(k, boat);
        stern(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // the conning tower: a rounded copper tower, gold rim, its hatch, lightning rods, the periscope (it sweeps)
    private static void tower(BoatKit k, PartDefinition boat) {
        k.box(FIBER, 0, 0, -10.0F, -16.0F, -3.0F, 12.0F, 10.0F, 6.0F);
        k.box(FIBER, 4, 4, -9.0F, -16.0F, -4.0F, 10.0F, 10.0F, 8.0F);
        k.box(GOLD, 0, 0, -10.3F, -16.6F, -4.3F, 12.6F, 0.6F, 8.6F);
        k.box(GREY, -6.0F, -17.0F, -1.5F, 3.0F, 0.4F, 3.0F);
        k.pair(BRASS, -6.5F, -12.0F, 3.95F, 2.4F, 2.4F, 0.12F);                    // round ports on the tower
        k.pair(NAV_RED, -6.0F, -11.5F, 4.0F, 1.4F, 1.4F, 0.1F);
        for (float[] r : new float[][]{{-9.0F, -3.0F}, {1.0F, 3.0F}}) {
            k.line(FIBER, r[0], -16.6F, r[1], r[0], -24.0F, r[1], 0.35F);
            k.box(GOLD, r[0] - 0.3F, -24.6F, r[1] - 0.3F, 0.6F, 0.6F, 0.6F);
        }
        CubeListBuilder scope = CubeListBuilder.create();
        box(scope, BRASS, 0, 0, -0.5F, -18.0F, -0.5F, 1.0F, 18.0F, 1.0F);
        box(scope, BRASS, 4, 0, -0.8F, -19.5F, -0.8F, 2.6F, 1.6F, 1.6F);
        box(scope, TINT, 0, 0, 1.8F, -19.3F, -0.6F, 0.1F, 1.2F, 1.2F);
        box(scope, BRASS, 0, 6, -0.8F, -6.0F, -0.8F, 1.6F, 0.6F, 1.6F);
        boat.addOrReplaceChild("periscope", scope, PartPose.offset(-2.0F, -16.6F, 0.0F));
    }

    // the glass dome at the front (rings of clear glass round a copper frame), the cockpit inside it
    private static void dome(BoatKit k, PartDefinition boat) {
        for (float x = DOME_X - DOME_R + 1.0F; x <= DOME_X + DOME_R - 1.0F; x += 2.0F) {
            float r = Mth.sqrt(Math.max(1.0F, DOME_R * DOME_R - (x - DOME_X) * (x - DOME_X)));
            PartDefinition ring = boat.addOrReplaceChild("dome" + (int) (x * 10), CubeListBuilder.create(), PartPose.offset(x, DOME_Y, 0.0F));
            rim(ring, "glass", TINT, r, 0.3F, 2.0F, 20);
        }
        // the copper ribs of its frame, and a brass collar where it meets the hull
        PartDefinition collar = boat.addOrReplaceChild("collar", CubeListBuilder.create(), PartPose.offset(DOME_X - DOME_R + 0.5F, DOME_Y, 0.0F));
        rim(collar, "rim", BRASS, 8.0F, 0.8F, 1.2F, 20);
        for (float fx : new float[]{DOME_X - 6.0F, DOME_X, DOME_X + 6.0F}) {
            float r = Mth.sqrt(DOME_R * DOME_R - (fx - DOME_X) * (fx - DOME_X)) + 0.1F;
            PartDefinition band = boat.addOrReplaceChild("frame" + (int) fx, CubeListBuilder.create(), PartPose.offset(fx, DOME_Y, 0.0F));
            rim(band, "arc", FIBER, r, 0.45F, 0.5F, 20);
        }
        // the cockpit: one seat in the middle of the dome (a floor under it), a glowing redstone panel and levers
        k.disc(FIBER, DOME_X - 1.0F, DOME_Y + 11.5F, 0.0F, 4.6F, 0.8F);
        k.box(LEATHER, DOME_X - 5.0F, DOME_Y + 10.0F, -2.2F, 4.4F, 1.5F, 4.4F);
        k.box(LEATHER, DOME_X - 6.0F, DOME_Y + 3.0F, -2.2F, 1.0F, 7.0F, 4.4F);
        k.box(BRASS, DOME_X - 3.6F, DOME_Y + 11.5F, -0.4F, 0.8F, 0.6F, 0.8F);
        // (its own part: hidden from the rider's own view in first person, as it sits right before their eyes)
        CubeListBuilder panel = CubeListBuilder.create();
        box(panel, ENGINE, 0, 0, DOME_X + 7.0F, DOME_Y - 1.0F, -5.0F, 2.0F, 8.0F, 10.0F);
        for (int b = 0; b < 6; b++) box(panel, NAV_RED, 0, 0, DOME_X + 6.95F, DOME_Y + 0.5F + (b / 3) * 2.0F, -3.5F + (b % 3) * 2.6F, 0.06F, 1.0F, 1.0F);
        box(panel, GAUGE, 0, 0, DOME_X + 6.95F, DOME_Y + 4.5F, -4.0F, 0.06F, 1.6F, 8.0F);
        k.line(BRASS, DOME_X + 6.5F, DOME_Y + 1.0F, 4.2F, DOME_X + 4.5F, DOME_Y - 2.0F, 4.2F, 0.3F);
        k.box(RED, DOME_X + 4.2F, DOME_Y - 2.4F, 3.9F, 0.6F, 0.6F, 0.6F);
        // the flashlight: a brass lamp at the dome's nose, and its beam (shown when it's on: setupAnim)
        box(panel, BRASS, 0, 0, DOME_X + DOME_R - 0.6F, DOME_Y - 1.2F, -1.2F, 1.2F, 2.4F, 2.4F);
        box(panel, NAV_WHITE, 0, 0, DOME_X + DOME_R + 0.6F, DOME_Y - 0.9F, -0.9F, 0.1F, 1.8F, 1.8F);
        boat.addOrReplaceChild("panel", panel, PartPose.ZERO);   // (the lamp too: right before the rider's eyes)
        // The beam: a cone of faint light, as wide as the light it casts (Submersible.CONE_DEG), widening and fading
        // away from the lamp: its shell of BEAM_PANELS long tapering panels round its axis, each leaning out by the
        // cone's angle, painted (tools/BoatTex.cs, the glow only) narrow at the lamp and wide at the far end so they meet
        // edge to edge. From the side it shows as a soft cone, brighter along its edges; from the dome you look down
        // into it. It turns to where its rider looks: "beam" turns left and right, "tilt" up and down.
        PartDefinition beam = boat.addOrReplaceChild("beam", CubeListBuilder.create(), PartPose.offset(com.alexh.capitalism.boats.Submersible.LAMP_X, com.alexh.capitalism.boats.Submersible.LAMP_Y, 0.0F));
        PartDefinition tilt = beam.addOrReplaceChild("tilt", CubeListBuilder.create(), PartPose.ZERO);
        float lean = com.alexh.capitalism.boats.Submersible.CONE_DEG * Mth.DEG_TO_RAD;
        for (int i = 0; i < BEAM_PANELS; i++) {
            PartDefinition spin = tilt.addOrReplaceChild("spin" + i, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0, 0, 0, (float) (i * 2 * Math.PI / BEAM_PANELS), 0, 0));
            CubeListBuilder shell = CubeListBuilder.create();
            shell.texOffs(BEAM_U, BEAM_V).addBox(0.0F, -BEAM_W / 2, 0.0F, BEAM_L, BEAM_W, 0.0F);
            spin.addOrReplaceChild("shell", shell, PartPose.offsetAndRotation(0, 0, 0, 0, lean, 0));
        }
        for (int i = 0; i < HAZE.length; i++) {
            float[] h = HAZE[i];
            CubeListBuilder haze = CubeListBuilder.create();
            haze.texOffs((int) h[2], (int) (HAZE_V - h[1])).addBox(h[0], -h[1] / 2, -h[1] / 2, 0.0F, h[1], h[1]);
            tilt.addOrReplaceChild("haze" + i, haze, PartPose.ZERO);
        }
    }

    // the beam's shell: how many panels, each one's length (model pixels, along its slant) and its width at the far
    // end (so the panels meet round the cone), and where its picture is (2 x BEAM_L wide, BEAM_W tall: keep
    // tools/BoatTex.cs the same)
    static final int BEAM_PANELS = 14;
    static final float BEAM_L = 112.0F;
    static final float BEAM_W = 18.0F;
    static final int BEAM_U = 0;
    static final int BEAM_V = 180;
    // the haze its rider sees: soft round glows across the beam {how far along, how wide, u} (spots 2 x wide, wide
    // tall, from HAZE_V: keep tools/BoatTex.cs the same)
    static final float[][] HAZE = {{40.0F, 20.0F, 0.0F}, {80.0F, 36.0F, 40.0F}, {128.0F, 52.0F, 112.0F}};
    static final int HAZE_V = 200;

    // the stern: the propeller (it spins) in its shroud, fins and rudder, lightning rods
    private static void stern(BoatKit k, PartDefinition boat) {
        float px = STERN - 3.0F;
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, GOLD, 0, 0, -1.2F, -1.2F, -1.2F, 2.4F, 2.4F, 2.4F);
        box(hub, GOLD, 0, 0, -2.4F, -0.6F, -0.6F, 1.2F, 1.2F, 1.2F);
        PartDefinition prop = boat.addOrReplaceChild("prop", hub, PartPose.offset(px, HULL_Y, 0.0F));
        for (int b = 0; b < 4; b++) {
            CubeListBuilder blade = CubeListBuilder.create();
            box(blade, WOOD, b * 4, 0, -0.3F, -7.0F, -1.4F, 0.6F, 6.0F, 2.8F);
            box(blade, WOOD, b * 4, 6, -0.25F, -7.6F, -1.0F, 0.5F, 0.8F, 2.0F);
            prop.addOrReplaceChild("blade" + b, blade, PartPose.offsetAndRotation(0, 0, 0, (float) (b * Math.PI / 2), 0.35F, 0.0F));
        }
        PartDefinition shroud = boat.addOrReplaceChild("shroud", CubeListBuilder.create(), PartPose.offset(px, HULL_Y, 0.0F));
        rim(shroud, "rim", FIBER, 8.0F, 0.6F, 3.0F, 20);
        k.box(FIBER, STERN - 1.5F, HULL_Y - 1.0F, -1.0F, 2.0F, 2.0F, 2.0F);
        // fins: the rudder above and below, the diving planes either side
        k.box(FIBER, px - 1.0F, HULL_Y - 10.0F, -0.3F, 4.0F, 5.0F, 0.6F);
        k.box(FIBER, px - 1.0F, HULL_Y + 5.0F, -0.3F, 4.0F, 5.0F, 0.6F);
        k.box(FIBER, px - 1.0F, HULL_Y - 0.3F, 8.0F, 4.0F, 0.6F, 4.0F);
        k.box(FIBER, px - 1.0F, HULL_Y - 0.3F, -12.0F, 4.0F, 0.6F, 4.0F);
        k.line(FIBER, STERN + 2.0F, -6.0F, 0.0F, STERN - 2.0F, -14.0F, 0.0F, 0.35F);
        k.box(GOLD, STERN - 2.3F, -14.6F, -0.3F, 0.6F, 0.6F, 0.6F);
    }
}

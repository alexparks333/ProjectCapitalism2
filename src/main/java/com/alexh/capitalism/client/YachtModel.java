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

// The Yacht (boats/Yacht, Boats.md): a white superyacht about four blocks long. The hull white over a navy band with a
// gold pinstripe and a row of lit portholes (the hull-side pictures); a teak main deck; the salon and the upper deck
// with tinted window bands that glow warm at night; an open flybridge with the helm (glowing screens, a wheel that
// turns), captain's seats and the radar arch (a dome, a spinning radar, the masthead light, antennas); on the bow a
// raised hot tub of glowing water with bubbles, sun pads, chrome rails and the anchor; the aft deck's sofa lounge round
// a teak table; a tender (an orange-tubed RIB) on a davit; the swim platform with a ladder; the flag at the stern;
// running lights, blue underwater lights.
public class YachtModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "yacht"), "main");

    static final int STERN = -32;
    static final int BOW = 34;
    static final float MAIN_DECK = -4.0F;
    static final int FOREDECK_START = 18;
    static final float FOREDECK = -8.0F;
    static final float FLY = -18.0F;      // the flybridge floor
    // the yacht's flag picture: 12 x 6 at (FLAG_U, PICTURE_V) (tools/BoatTex.cs)
    static final int FLAG_U = 0;

    static int halfWidth(int x) {
        double xc = x + 0.5;
        if (xc <= 8) return xc < -29 ? 11 : 12;
        double t = Math.min(1.0, (xc - 8) / 26.0);
        return Math.max(1, (int) Math.round(12 * Math.pow(1 - Math.pow(t, 1.8), 0.6)));
    }

    static int top(int x) {
        return x <= 10 ? -8 : -8 - (int) Math.round(3 * Math.pow((x - 10) / 24.0, 2));
    }

    private final ModelPart wheel;
    private final ModelPart radar;
    private final ModelPart bubbles;
    private final List<ModelPart> flag;

    public YachtModel(ModelPart root) {
        super(root);
        this.wheel = this.boat.getChild("wheel_mount").getChild("wheel");
        this.radar = this.boat.getChild("radar");
        this.bubbles = this.boat.getChild("bubbles");
        this.flag = strips(this.boat.getChild("flag"), 12);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.012F, 0.008F, -0.025F * speed);
        this.radar.yRot = t * 0.22F;
        this.wheel.xRot = Mth.clamp(turn(state), -6.0F, 6.0F) * 0.3F;
        wave(this.flag, t, 0.4F + 1.0F * speed, 0.4F, 0.6F);
        for (int i = 0; i < 6; i++) {
            ModelPart b = this.bubbles.getChild("b" + i);
            float phase = (t * 0.06F + i * 0.37F) % 1.0F;
            b.y = -phase * 1.2F;
            b.visible = phase < 0.85F;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, YachtModel::halfWidth, YachtModel::top,
            new float[][]{{1, 3, 0.85F}, {3, 5, 0.55F}, {5, 6.5F, 0.25F}}, 2);
        k.box(BOTTOM, 0, 28, STERN + 2, 6.5F, -0.5F, BOW - STERN - 6, 0.8F, 1.0F);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int tp = top(x);
            int inner = w - t;
            k.box(CHROME, (x - STERN) % 20, 0, x, tp - 0.35F, w - t - 0.1F, 1, 0.35F, t + 0.2F);
            k.box(CHROME, (x - STERN) % 20, 0, x, tp - 0.35F, -w - 0.1F, 1, 0.35F, t + 0.2F);
            if (inner < 1) continue;
            float deck = x >= FOREDECK_START ? FOREDECK : MAIN_DECK;
            k.box(WHITE, (x - STERN) % 40, 0, x, deck, -inner, 1, 1 - deck, 2 * inner);
            k.deck(x, deck, inner);
            if (deck == MAIN_DECK) {
                k.box(FIBER, (x - STERN) % 30, 0, x, tp + 0.3F, inner - 0.15F, 1, deck - tp - 0.3F, 0.15F);
                k.box(FIBER, (x - STERN) % 30, 0, x, tp + 0.3F, -inner, 1, deck - tp - 0.3F, 0.15F);
            }
        }
        superstructure(k);
        flybridge(k, boat);
        bow(k, boat);
        aft(k, boat);
        lights(k);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // the salon and the upper deck: white, with tinted window bands (chrome between the panes), roofs that overhang
    private static void superstructure(BoatKit k) {
        // salon: x -14..18, up to y -12
        for (int x = -14; x < 18; x += 8) k.box(WHITE, (x + 14) % 20, 0, x, -12.0F, -9.0F, Math.min(8, 18 - x), 8.0F, 18.0F);
        k.box(WHITE, 10, 0, -15.0F, -12.6F, -9.6F, 34.0F, 0.6F, 0.6F);      // roof overhang edges
        k.box(WHITE, 10, 0, -15.0F, -12.6F, 9.0F, 34.0F, 0.6F, 0.6F);
        for (int x = -12; x < 16; x += 4) {
            k.pair(TINT, x, -10.6F, 9.0F, 3.6F, 5.0F, 0.1F);
            k.pair(CHROME, x + 3.6F, -10.8F, 9.0F, 0.4F, 5.4F, 0.15F);
        }
        k.box(TINT, 18.0F, -10.6F, -7.5F, 0.1F, 4.6F, 15.0F);                // salon front windows
        k.box(TINT, -14.1F, -10.6F, -6.0F, 0.1F, 6.6F, 12.0F);               // aft glass doors
        k.box(CHROME, -14.2F, -10.6F, -0.2F, 0.15F, 6.6F, 0.4F);
        // upper deck: x -6..12, up to y -18
        for (int x = -6; x < 12; x += 6) k.box(WHITE, (x + 6) % 20, 0, x, FLY, -7.5F, 6.0F, 6.0F, 15.0F);
        for (int x = -4; x < 11; x += 3) {
            k.pair(TINT, x, -16.8F, 7.5F, 2.7F, 3.6F, 0.1F);
            k.pair(CHROME, x + 2.7F, -17.0F, 7.5F, 0.3F, 4.0F, 0.15F);
        }
        // the upper deck's raked windscreen
        CubeListBuilder screen = CubeListBuilder.create();
        box(screen, TINT, 0, 0, -0.1F, -6.4F, -6.5F, 0.2F, 6.4F, 13.0F);
        k.part(screen, PartPose.offsetAndRotation(12.0F, -12.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        k.box(WHITE, 30, 0, 12.0F, -12.6F, -7.5F, 3.2F, 0.6F, 15.0F);        // the windscreen's sill on the salon roof
    }

    // the flybridge: teak floor, glass-and-chrome rail, the helm (screens, wheel), seats, the radar arch
    private static void flybridge(BoatKit k, PartDefinition boat) {
        for (int x = -6; x < 12; x++) k.deck(x, FLY, 7);
        float[][] port = {{-6, FLY, 6.8F}, {0, FLY, 6.8F}, {6, FLY, 6.8F}, {11.5F, FLY, 6.8F}};
        float[][] star = {{-6, FLY, -6.8F}, {0, FLY, -6.8F}, {6, FLY, -6.8F}, {11.5F, FLY, -6.8F}};
        k.railing(CHROME, port, 2.6F, 0.3F);
        k.railing(CHROME, star, 2.6F, 0.3F);
        k.pair(TINT, -6.0F, FLY - 2.4F, 6.75F, 17.5F, 2.2F, 0.1F);          // glass panels in the rail
        k.line(CHROME, 11.5F, FLY - 2.6F, 6.8F, 11.5F, FLY - 2.6F, -6.8F, 0.35F);
        // the helm console with its screens
        k.box(WHITE, 0, 0, 6.0F, FLY - 3.0F, -6.0F, 2.5F, 3.0F, 12.0F);
        k.box(TINT, 0, 0, 6.8F, FLY - 4.4F, -6.0F, 0.2F, 1.4F, 12.0F);       // its little windscreen
        k.box(GAUGE, 0, 0, 5.95F, FLY - 2.6F, -5.2F, 0.06F, 1.2F, 3.4F);
        k.box(GAUGE, 8, 0, 5.95F, FLY - 2.6F, -1.4F, 0.06F, 1.2F, 2.0F);
        PartDefinition mount = boat.addOrReplaceChild("wheel_mount", CubeListBuilder.create(), PartPose.offsetAndRotation(5.6F, FLY - 3.0F, -3.0F, 0.0F, 0.0F, -0.55F));
        CubeListBuilder hub = CubeListBuilder.create();
        box(hub, CHROME, 0, 0, -0.3F, -0.4F, -0.4F, 0.6F, 0.8F, 0.8F);
        PartDefinition wheel = mount.addOrReplaceChild("wheel", hub, PartPose.ZERO);
        rim(wheel, "rim", CHROME, 1.8F, 0.35F, 0.35F, 10);
        spokes(wheel, "spoke", CHROME, 1.8F, 0.2F, 3, 0.0F);
        // the captain's seats
        for (int side = -1; side <= 1; side += 2) {
            float zc = side * 3.0F;
            k.box(LEATHER, 0, 0, -1.0F, FLY - 1.4F, zc - 2.0F, 4.0F, 1.4F, 4.0F);
            k.box(LEATHER, 0, 10, -2.0F, FLY - 6.0F, zc - 2.0F, 1.0F, 4.6F, 4.0F);
            k.box(CHROME, 0, 8, 0.6F, FLY - 0.2F, zc - 0.4F, 0.8F, 0.2F, 0.8F);
        }
        // the radar arch behind them: two raked legs, a crossbar, the dome, the spinning radar, the masthead, antennas
        for (int side = -1; side <= 1; side += 2) {
            CubeListBuilder leg = CubeListBuilder.create();
            box(leg, WHITE, 0, 0, -0.8F, -10.0F, -0.6F, 1.6F, 10.0F, 1.2F);
            k.part(leg, PartPose.offsetAndRotation(-4.5F, FLY, side * 6.2F, 0.0F, 0.0F, 0.25F));
        }
        k.box(WHITE, 0, 0, -8.0F, FLY - 10.6F, -6.8F, 3.0F, 1.0F, 13.6F);     // crossbar
        k.box(WHITE, 20, 0, -7.6F, FLY - 12.6F, -1.4F, 2.4F, 2.0F, 2.8F);     // radar dome
        k.box(WHITE, 20, 0, -7.2F, FLY - 13.2F, -1.0F, 1.6F, 0.6F, 2.0F);
        k.box(GREY, 0, 0, -6.9F, FLY - 11.6F, 3.2F, 1.0F, 1.0F, 1.0F);        // the radar's pedestal
        CubeListBuilder bar = CubeListBuilder.create();
        box(bar, BLACK, 0, 0, -0.3F, -0.6F, -3.5F, 0.6F, 0.6F, 7.0F);
        box(bar, GREY, 0, 4, -0.2F, -0.2F, -0.2F, 0.4F, 0.4F, 0.4F);
        boat.addOrReplaceChild("radar", bar, PartPose.offset(-6.4F, FLY - 11.6F, 3.7F));
        k.box(WHITE, 30, 10, -6.7F, FLY - 18.0F, -3.3F, 0.4F, 7.4F, 0.4F);    // masthead pole
        k.box(NAV_WHITE, -6.9F, FLY - 19.0F, -3.5F, 0.8F, 1.0F, 0.8F);
        k.line(WHITE, -6.0F, FLY - 10.6F, -5.0F, -8.5F, FLY - 21.0F, -5.6F, 0.25F);   // antennas
        k.line(WHITE, -6.0F, FLY - 10.6F, 5.0F, -8.5F, FLY - 20.0F, 5.6F, 0.25F);
        k.box(GREY, 4, 4, -5.6F, FLY - 11.2F, -6.0F, 1.2F, 0.6F, 0.6F);       // horn
    }

    // the raised hot tub (glowing water, bubbles), sun pads, the bow rail, anchor
    private static void bow(BoatKit k, PartDefinition boat) {
        float rim = FOREDECK - 1.2F;
        k.box(WHITE, 0, 0, 19.0F, rim, -4.0F, 9.0F, 1.2F, 0.8F);
        k.box(WHITE, 0, 0, 19.0F, rim, 3.2F, 9.0F, 1.2F, 0.8F);
        k.box(WHITE, 0, 0, 19.0F, rim, -3.2F, 0.8F, 1.2F, 6.4F);
        k.box(WHITE, 0, 0, 27.2F, rim, -3.2F, 0.8F, 1.2F, 6.4F);
        k.box(CHROME, 0, 0, 18.9F, rim - 0.2F, -4.1F, 9.2F, 0.2F, 0.3F);
        k.box(CHROME, 0, 0, 18.9F, rim - 0.2F, 3.8F, 9.2F, 0.2F, 0.3F);
        k.box(WATER, 19.8F, FOREDECK - 0.8F, -3.2F, 7.4F, 0.4F, 6.4F);
        PartDefinition bubbles = boat.addOrReplaceChild("bubbles", CubeListBuilder.create(), PartPose.offset(0, FOREDECK - 0.9F, 0));
        float[][] spots = {{20.5F, -2.2F}, {22.5F, 1.6F}, {24.6F, -0.4F}, {26.2F, 2.1F}, {21.7F, 0.3F}, {25.3F, -2.4F}};
        for (int i = 0; i < spots.length; i++) {
            CubeListBuilder b = CubeListBuilder.create();
            box(b, WHITE, 60, 0, -0.2F, -0.4F, -0.2F, 0.4F, 0.4F, 0.4F);
            bubbles.addOrReplaceChild("b" + i, b, PartPose.offset(spots[i][0], 0, spots[i][1]));
        }
        k.box(CANVAS, 0, 0, 28.4F, FOREDECK - 0.8F, -2.6F, 3.0F, 0.8F, 5.2F);        // sun pad
        k.box(CANVAS, 10, 10, 18.0F, FOREDECK - 0.8F, -6.0F, 0.8F, 0.8F, 12.0F);     // the tub's back cushion
        // bow rail
        float[][] port = new float[6][];
        float[][] star = new float[6][];
        int[] xs = {12, 17, 22, 26, 30, 33};
        for (int i = 0; i < xs.length; i++) {
            float z = halfWidth(xs[i]) - 0.6F;
            port[i] = new float[]{xs[i], top(xs[i]), z};
            star[i] = new float[]{xs[i], top(xs[i]), -z};
        }
        k.railing(CHROME, port, 2.4F, 0.3F);
        k.railing(CHROME, star, 2.4F, 0.3F);
        k.box(GREY, 8, 8, BOW + 0.4F, top(BOW) + 1.5F, -0.6F, 0.8F, 2.4F, 1.2F);    // the anchor at the stem
        k.box(GREY, 8, 8, BOW + 0.2F, top(BOW) + 3.6F, -1.4F, 1.0F, 0.6F, 2.8F);
    }

    // the aft deck lounge, the tender on its davit, the swim platform, the flag
    private static void aft(BoatKit k, PartDefinition boat) {
        // a U of sofas round a teak table
        k.box(WHITE, 0, 0, -30.0F, MAIN_DECK - 1.2F, -8.5F, 3.0F, 1.2F, 17.0F);
        k.box(CANVAS, 0, 0, -30.0F, MAIN_DECK - 1.6F, -8.5F, 3.0F, 0.5F, 17.0F);
        k.box(CANVAS, 0, 10, -31.0F, MAIN_DECK - 4.4F, -8.5F, 1.0F, 3.0F, 17.0F);
        for (int side = -1; side <= 1; side += 2) {
            float z = side > 0 ? 6.0F : -8.5F;
            k.box(WHITE, 0, 0, -27.0F, MAIN_DECK - 1.2F, z, 7.0F, 1.2F, 2.5F);
            k.box(CANVAS, 0, 0, -27.0F, MAIN_DECK - 1.6F, z, 7.0F, 0.5F, 2.5F);
            k.box(CANVAS, 0, 10, -27.0F, MAIN_DECK - 3.6F, side > 0 ? 8.0F : -8.5F, 7.0F, 2.0F, 0.5F);
        }
        k.box(DARKWOOD, 0, 0, -25.0F, MAIN_DECK - 1.0F, -3.0F, 4.0F, 0.3F, 6.0F);
        k.box(CHROME, 0, 0, -23.3F, MAIN_DECK - 0.7F, -0.3F, 0.6F, 0.7F, 0.6F);
        // the tender on the salon roof, its davit crane
        k.box(GREY, 0, 0, -13.0F, -13.4F, -2.0F, 5.5F, 1.2F, 4.0F);
        k.box(ACCENT, 0, 0, -13.2F, -14.0F, -2.6F, 5.9F, 1.2F, 1.0F);
        k.box(ACCENT, 0, 0, -13.2F, -14.0F, 1.6F, 5.9F, 1.2F, 1.0F);
        k.box(ACCENT, 0, 0, -8.0F, -14.0F, -1.6F, 1.0F, 1.2F, 3.2F);
        k.box(BLACK, 0, 0, -14.2F, -14.6F, -0.5F, 1.0F, 1.6F, 1.0F);
        k.box(WHITE, 40, 0, -6.6F, -18.0F, -5.0F, 0.8F, 6.0F, 0.8F);
        k.line(WHITE, -6.2F, -17.6F, -4.6F, -10.5F, -17.6F, -1.0F, 0.6F);
        k.line(ROPE, -10.5F, -17.4F, -1.0F, -10.5F, -14.0F, 0.0F, 0.15F);
        // the swim platform, its ladder, steps up to the aft deck
        for (int x = STERN - 4; x < STERN; x++) {
            k.box(WHITE, 0, 0, x, -0.6F, -10.0F, 1, 1.0F, 20.0F);
            k.deck(x, -0.6F, 10);
        }
        k.box(CHROME, 0, 0, STERN - 3.8F, -0.6F, -8.6F, 0.25F, 3.6F, 0.25F);
        k.box(CHROME, 0, 0, STERN - 3.8F, -0.6F, -7.0F, 0.25F, 3.6F, 0.25F);
        for (int r = 0; r < 3; r++) k.box(CHROME, 0, 0, STERN - 3.9F, 0.4F + r * 1.0F, -8.6F, 0.4F, 0.2F, 1.8F);
        k.box(WHITE, 0, 0, STERN - 1.4F, -2.6F, -2.0F, 1.4F, 2.0F, 4.0F);
        k.box(WHITE, 0, 0, STERN - 0.8F, -4.0F, -2.0F, 0.8F, 1.4F, 4.0F);
        // the flag on a chrome pole at the stern
        k.box(CHROME, 0, 0, STERN + 0.4F, -16.0F, 9.2F, 0.3F, 8.0F, 0.3F);
        k.box(GOLD, 0, 0, STERN + 0.3F, -16.5F, 9.1F, 0.5F, 0.5F, 0.5F);
        PartDefinition flag = boat.addOrReplaceChild("flag", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 9.35F));
        flag(flag, NAVY, FLAG_U, PICTURE_V, STERN + 0.4F, -15.8F, 12, 6.0F, true);
    }

    // running lights on the salon, underwater lights along the hull and transom
    private static void lights(BoatKit k) {
        k.box(NAV_RED, 15.5F, -11.4F, 9.05F, 1.4F, 0.9F, 0.3F);
        k.box(NAV_GREEN, 15.5F, -11.4F, -9.35F, 1.4F, 0.9F, 0.3F);
        for (int x : new int[]{-28, -18, -8, 2}) {
            int w = halfWidth(x);
            k.box(BLUE_GLOW, x, 0.4F, w, 1.2F, 0.7F, 0.08F);
            k.box(BLUE_GLOW, x, 0.4F, -w - 0.08F, 1.2F, 0.7F, 0.08F);
        }
        k.box(BLUE_GLOW, STERN - 0.08F, 0.6F, -6.0F, 0.08F, 0.7F, 1.4F);
        k.box(BLUE_GLOW, STERN - 0.08F, 0.6F, 4.6F, 0.08F, 0.7F, 1.4F);
    }
}

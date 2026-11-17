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

// The Nordic Longship (boats/Longship, Boats.md): a Viking dragon ship about 5 blocks long. A long, narrow clinker-built
// spruce hull (overlapping strakes, tarred below: the hull-side pictures) sweeping up at both ends; a carved dragon at
// the prow (a scaled neck, open jaws with teeth and a forked tongue, horns, a spiked frill, glowing ember eyes) and its
// curling tail at the stern; painted round shields all along both sides; oars through the oar ports that row when
// you paddle; one great square sail in red and white stripes that billows with its speed, on a yard and mast with its
// rigging and a gilded weathervane; rowing benches, sea chests, the steering oar on the starboard quarter; and in the
// middle, a stone-ringed campfire whose flames flicker (and glow).
public class LongshipModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "longship"), "main");

    static final int STERN = -36;
    static final int BOW = 38;
    static final float DECK_Y = -0.5F;
    static final float MAST_X = 0.0F;
    // pictures (tools/BoatTex.cs): four shield designs 4 x 4 from (4, PICTURE_V); the sail 2 x 33 wide, 16 tall from
    // (0, SAIL_V)
    static final int SHIELD_V = PICTURE_V;
    static final int SAIL_V = PICTURE_V + 6;
    static final int SAIL_W = 33;
    static final int SAIL_ROWS = 8;
    static final float SAIL_TOP = -40.0F;
    static final int OARS = 5;

    static int halfWidth(int x) {
        double t = Math.abs(x + 0.5 - 1.0) / 37.5;
        if (t >= 1) return 1;
        return Math.max(1, (int) Math.round(8 * Math.pow(1 - Math.pow(t, 2.4), 0.6)));
    }

    static int top(int x) {
        double d = Math.abs(x + 0.5 - 1.0) - 16;
        return d <= 0 ? -4 : -4 - (int) Math.round(5.5 * Math.pow(d / 21.0, 2));
    }

    private record SailRow(ModelPart mid, ModelPart port, ModelPart star, float bulge) {
    }

    private final List<SailRow> sail = new ArrayList<>();
    private final List<ModelPart> portOars = new ArrayList<>();
    private final List<ModelPart> starOars = new ArrayList<>();
    private final ModelPart flames;
    private final ModelPart mast;
    private final ModelPart vane;

    public LongshipModel(ModelPart root) {
        super(root);
        ModelPart s = this.boat.getChild("sail");
        for (int j = 0; j < SAIL_ROWS; j++) {
            float bulge = 4.0F * Mth.sin((float) Math.PI * (j + 0.5F) / SAIL_ROWS);
            this.sail.add(new SailRow(s.getChild("m" + j), s.getChild("p" + j), s.getChild("s" + j), bulge));
        }
        for (int i = 0; i < OARS; i++) {
            this.portOars.add(this.boat.getChild("oar_p" + i));
            this.starOars.add(this.boat.getChild("oar_s" + i));
        }
        this.flames = this.boat.getChild("flames");
        this.mast = this.boat.getChild("mast");
        this.vane = this.boat.getChild("vane");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.02F, 0.012F, 0.0F);
        // the sail fills with its speed
        float x = MAST_X + 1.6F;
        int k = 0;
        for (SailRow row : this.sail) {
            float b = row.bulge * (0.4F + 0.6F * speed + 0.05F * Mth.sin(t * 0.2F + k++ * 0.6F));
            row.mid.x = x + b;
            row.port.x = x + b;
            row.star.x = x + b;
            float angle = (float) Math.atan2(b * 0.7F, SAIL_W / 3.0F);
            row.port.yRot = -angle;
            row.star.yRot = angle;
        }
        // the oars row with the paddling (like a boat's paddles), each a little behind the one in front
        for (int i = 0; i < OARS; i++) {
            oar(this.portOars.get(i), state.rowingTimeLeft - i * 0.25F, 1);
            oar(this.starOars.get(i), state.rowingTimeRight - i * 0.25F, -1);
        }
        // the fire flickers
        for (int i = 0; i < 5; i++) {
            ModelPart flame = this.flames.getChild("flame" + i);
            float f = 0.75F + 0.35F * Mth.sin(t * (0.5F + i * 0.13F) + i * 1.7F) + 0.15F * Mth.sin(t * 1.9F + i);
            flame.yScale = f;
            flame.xScale = 1.1F - 0.2F * f;
            flame.zScale = 1.1F - 0.2F * f;
        }
        // the weathervane turns a little in the wind
        this.vane.yRot = 0.25F * Mth.sin(t * 0.05F);
        // riding it in first person from behind the mast: the mast is out of the way
        float viewer = viewer(state);
        this.mast.visible = !(viewer < MAST_X);
    }

    private static void oar(ModelPart oar, float time, int side) {
        float dip = Mth.clampedLerp((Mth.sin(-time) + 1.0F) / 2.0F, 0.15F, 0.6F);
        float sweep = Mth.clampedLerp((Mth.sin(-time + 1.0F) + 1.0F) / 2.0F, -0.5F, 0.5F);
        oar.xRot = -side * dip;
        oar.yRot = sweep;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, LongshipModel::halfWidth, LongshipModel::top,
            new float[][]{{1, 2.5F, 0.75F}, {2.5F, 3.6F, 0.4F}}, 1);
        // keel, rising into the stem and sternpost
        k.box(DARKWOOD, 0, 20, STERN + 2, 3.6F, -0.6F, BOW - STERN - 4, 1.2F, 1.2F);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int tp = top(x);
            int inner = w - t;
            // the gunwale: a dark oak rail along the top
            k.box(DARKWOOD, (x - STERN) % 40, 0, x, tp - 0.5F, w - t - 0.2F, 1, 0.5F, t + 0.4F);
            k.box(DARKWOOD, (x - STERN) % 40, 0, x, tp - 0.5F, -w - 0.2F, 1, 0.5F, t + 0.4F);
            if (inner < 1) continue;
            k.box(WOOD, (x - STERN) % 30, 0, x, DECK_Y, -inner, 1, 1 - DECK_Y, 2 * inner);
            k.deck(x, DECK_Y, inner);
            // ribs (frames) inside the hull every 4
            if ((x - STERN) % 4 == 0) {
                k.box(DARKWOOD, 0, 10, x, tp + 0.3F, inner - 0.5F, 0.8F, DECK_Y - tp - 0.3F, 0.5F);
                k.box(DARKWOOD, 0, 10, x, tp + 0.3F, -inner, 0.8F, DECK_Y - tp - 0.3F, 0.5F);
            }
        }
        dragon(k, boat);
        tail(k);
        shields(k);
        oars(k, boat);
        rig(k, boat);
        deckThings(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // the dragon's neck rising from the stem, and its head
    private static void dragon(BoatKit k, PartDefinition boat) {
        float[][] neck = {{BOW, top(BOW) + 3.0F}, {BOW + 1.5F, -9}, {BOW + 3.5F, -13}, {BOW + 5.0F, -17}, {BOW + 5.8F, -20.5F}, {BOW + 6.0F, -24.0F}};
        float[] thick = {2.8F, 2.6F, 2.4F, 2.2F, 2.0F};
        for (int i = 0; i + 1 < neck.length; i++) {
            k.line(SCALE, neck[i][0], neck[i][1], 0, neck[i + 1][0], neck[i + 1][1], 0, thick[i]);
            // spikes of the frill down the back of the neck
            float mx = (neck[i][0] + neck[i + 1][0]) / 2 - 1.2F;
            float my = (neck[i][1] + neck[i + 1][1]) / 2;
            k.line(DARKWOOD, mx, my, 0, mx - 2.0F, my - 0.8F, 0, 0.5F);
            // gold bands
            k.line(GOLD, neck[i][0], neck[i][1] - 0.01F, 0, neck[i][0] + 0.01F, neck[i][1] - 0.6F, 0, thick[i] + 0.3F);
        }
        // the head, looking forward and a little down
        PartDefinition head = boat.addOrReplaceChild("dragon_head", CubeListBuilder.create(), PartPose.offsetAndRotation(BOW + 6.2F, -25.5F, 0.0F, 0.0F, 0.0F, -0.15F));
        CubeListBuilder skull = CubeListBuilder.create();
        box(skull, SCALE, 0, 0, -1.5F, -1.8F, -1.6F, 4.2F, 3.2F, 3.2F);       // skull
        box(skull, SCALE, 10, 0, 2.6F, -1.0F, -1.1F, 3.8F, 1.6F, 2.2F);       // snout
        box(skull, SCALE, 10, 10, 1.5F, -2.3F, -1.3F, 2.0F, 0.6F, 2.6F);      // brow ridge
        box(skull, FIRE, 0, 0, 1.8F, -1.6F, 1.55F, 1.0F, 0.7F, 0.12F);        // glowing eyes
        box(skull, FIRE, 0, 0, 1.8F, -1.6F, -1.67F, 1.0F, 0.7F, 0.12F);
        box(skull, BLACK, 0, 0, 6.35F, -0.8F, 0.3F, 0.1F, 0.4F, 0.4F);        // nostrils
        box(skull, BLACK, 0, 0, 6.35F, -0.8F, -0.7F, 0.1F, 0.4F, 0.4F);
        for (int i = 0; i < 4; i++) {                                         // upper teeth
            box(skull, BONE, 0, 0, 3.0F + i * 0.85F, 0.6F, 0.75F, 0.35F, 0.7F, 0.25F);
            box(skull, BONE, 0, 0, 3.0F + i * 0.85F, 0.6F, -1.0F, 0.35F, 0.7F, 0.25F);
        }
        PartDefinition skullPart = head.addOrReplaceChild("skull", skull, PartPose.ZERO);
        CubeListBuilder jaw = CubeListBuilder.create();
        box(jaw, SCALE, 0, 10, 0.0F, 0.0F, -0.93F, 5.0F, 0.9F, 1.86F);
        for (int i = 0; i < 4; i++) {
            box(jaw, BONE, 0, 0, 1.4F + i * 0.85F, -0.6F, 0.6F, 0.35F, 0.6F, 0.25F);
            box(jaw, BONE, 0, 0, 1.4F + i * 0.85F, -0.6F, -0.85F, 0.35F, 0.6F, 0.25F);
        }
        box(jaw, RED, 0, 0, 1.0F, -0.2F, -0.3F, 4.6F, 0.2F, 0.6F);            // tongue
        box(jaw, RED, 0, 0, 5.4F, -0.2F, -0.5F, 1.0F, 0.2F, 0.3F);            // forked
        box(jaw, RED, 0, 0, 5.4F, -0.2F, 0.2F, 1.0F, 0.2F, 0.3F);
        skullPart.addOrReplaceChild("jaw", jaw, PartPose.offsetAndRotation(1.0F, 0.8F, 0.0F, 0.0F, 0.0F, 0.35F));
        // horns sweeping back, and the frill behind the head
        for (int s = -1; s <= 1; s += 2) {
            line(skullPart, "horn_a" + s, BONE, -0.5F, -1.6F, s * 1.2F, -3.0F, -3.4F, s * 1.8F, 0.7F);
            line(skullPart, "horn_b" + s, BONE, -3.0F, -3.4F, s * 1.8F, -4.8F, -3.0F, s * 2.0F, 0.5F);
            line(skullPart, "frill" + s, DARKWOOD, -1.2F, -0.4F, s * 1.6F, -3.2F, 0.4F, s * 3.0F, 0.4F);
        }
        line(skullPart, "crest", DARKWOOD, -1.0F, -1.8F, 0.0F, -3.2F, -2.8F, 0.0F, 0.5F);
    }

    // the tail curling up from the sternpost
    private static void tail(BoatKit k) {
        float[][] curl = {{STERN, top(STERN) + 3.0F}, {STERN - 2.5F, -12}, {STERN - 4.2F, -17}, {STERN - 4.4F, -22}, {STERN - 2.8F, -25.6F},
            {STERN - 0.2F, -26.6F}, {STERN + 2.0F, -25.0F}, {STERN + 2.4F, -22.4F}, {STERN + 1.0F, -20.8F}, {STERN - 1.0F, -21.4F}};
        for (int i = 0; i + 1 < curl.length; i++) {
            float th = 2.6F - i * 0.2F;
            k.line(SCALE, curl[i][0], curl[i][1], 0, curl[i + 1][0], curl[i + 1][1], 0, th);
            if (i < 5) {
                float mx = (curl[i][0] + curl[i + 1][0]) / 2 + 1.0F;
                float my = (curl[i][1] + curl[i + 1][1]) / 2;
                k.line(DARKWOOD, mx, my, 0, mx + 1.6F, my - 0.6F, 0, 0.45F);
            }
        }
        k.box(GOLD, STERN - 1.4F, -21.9F, -0.6F, 1.2F, 1.2F, 1.2F);
    }

    // round shields along both sides, each a painted disc (its picture has a round edge) with an iron boss
    private static void shields(BoatKit k) {
        int n = 0;
        for (float x = -27.0F; x <= 27.0F; x += 3.2F) {
            int xi = (int) Math.floor(x);
            int w = halfWidth(xi);
            float y = top(xi) - 1.2F;
            int design = n % 4;
            float zp = w + 0.35F + (n % 2) * 0.08F;   // every other one a little further out: they overlap like a real shield wall
            // port: the face is the box's +z side; starboard: its -z side (no thickness, the back against the hull)
            k.statics.texOffs(design * 4, SHIELD_V).addBox(x - 2.0F, y, zp, 4.0F, 4.0F, 0.0F);
            k.statics.texOffs(design * 4 + 4, SHIELD_V).addBox(x - 2.0F, y, -zp, 4.0F, 4.0F, 0.0F);
            k.box(GREY, 0, 0, x - 0.5F, y + 1.5F, zp, 1.0F, 1.0F, 0.5F);          // bosses
            k.box(GREY, 0, 0, x - 0.5F, y + 1.5F, -zp - 0.5F, 1.0F, 1.0F, 0.5F);
            n++;
        }
    }

    // oars through the oar ports below the shields (they row: setupAnim)
    private static void oars(BoatKit k, PartDefinition boat) {
        for (int i = 0; i < OARS; i++) {
            float x = 14.0F - i * 7.0F;
            int xi = (int) Math.floor(x);
            int w = halfWidth(xi);
            float y = top(xi) + 2.4F;
            for (int s = -1; s <= 1; s += 2) {
                CubeListBuilder oar = CubeListBuilder.create();
                box(oar, WOOD, 0, 0, -0.3F, -0.3F, s > 0 ? -3.0F : -14.0F, 0.6F, 0.6F, 17.0F);
                box(oar, WOOD, 20, 0, -0.9F, -0.15F, s > 0 ? 10.5F : -14.0F, 1.8F, 0.3F, 3.5F);
                box(oar, RED, 0, 0, -0.95F, -0.2F, s > 0 ? 13.0F : -14.05F, 1.9F, 0.4F, 1.0F);
                boat.addOrReplaceChild(s > 0 ? "oar_p" + i : "oar_s" + i, oar, PartPose.offset(x, y, s * w));
                k.box(BLACK, 0, 0, x - 0.5F, y - 0.5F, s > 0 ? w : -w - 0.05F, 1.0F, 1.0F, 0.05F);   // the oar port
            }
        }
    }

    // the mast, yard, the striped sail, rigging, the weathervane
    private static void rig(BoatKit k, PartDefinition boat) {
        CubeListBuilder mast = CubeListBuilder.create();
        box(mast, WOOD, 40, 0, MAST_X - 1.2F, -46.0F, -1.2F, 2.4F, 46.0F + DECK_Y, 2.4F);
        box(mast, GREY, 0, 0, MAST_X - 1.35F, -10.0F, -1.35F, 2.7F, 0.5F, 2.7F);
        box(mast, GREY, 0, 0, MAST_X - 1.35F, -30.0F, -1.35F, 2.7F, 0.5F, 2.7F);
        boat.addOrReplaceChild("mast", mast, PartPose.ZERO);
        k.box(DARKWOOD, 0, 0, MAST_X - 2.0F, DECK_Y - 1.0F, -2.0F, 4.0F, 1.0F, 4.0F);     // the mast fish (its step)
        // the yard
        k.box(WOOD, 0, 0, MAST_X + 1.2F, SAIL_TOP - 1.4F, -SAIL_W / 2.0F - 2.0F, 1.4F, 1.4F, SAIL_W + 4.0F);
        // the sail: rows of three flat pieces (see PirateShipModel.sail), its picture in red and white stripes
        int third = SAIL_W / 3;
        float x = MAST_X + 1.6F;
        PartDefinition sail = boat.addOrReplaceChild("sail", CubeListBuilder.create(), PartPose.ZERO);
        for (int j = 0; j < SAIL_ROWS; j++) {
            float y = SAIL_TOP + 2 * j;
            int v = SAIL_V + 2 * j - third;
            float half = third / 2.0F;
            sail.addOrReplaceChild("p" + j, CubeListBuilder.create().texOffs(0, v).addBox(0, 0, 0, 0, 2, third), PartPose.offset(x, y, half));
            sail.addOrReplaceChild("m" + j, CubeListBuilder.create().texOffs(2 * third, v).addBox(0, 0, -half, 0, 2, third), PartPose.offset(x, y, 0));
            sail.addOrReplaceChild("s" + j, CubeListBuilder.create().texOffs(4 * third, v).addBox(0, 0, -third, 0, 2, third), PartPose.offset(x, y, -half));
        }
        float bottom = SAIL_TOP + 2 * SAIL_ROWS;
        // rigging: forestay to the dragon's neck, backstay to the tail, shrouds, the sheets from the sail's corners
        k.line(ROPE, MAST_X, -45.0F, 0, BOW + 2.0F, -10.0F, 0, 0.3F);
        k.line(ROPE, MAST_X, -45.0F, 0, STERN - 1.5F, -11.0F, 0, 0.3F);
        for (int s = -1; s <= 1; s += 2) {
            k.line(ROPE, MAST_X, -42.0F, 0, MAST_X - 3.0F, top(-3) - 0.5F, s * (halfWidth(-3) - 0.5F), 0.3F);
            k.line(ROPE, MAST_X, -42.0F, 0, MAST_X + 3.0F, top(3) - 0.5F, s * (halfWidth(3) - 0.5F), 0.3F);
            k.line(ROPE, x, bottom, s * (SAIL_W / 2.0F - 0.5F), -10.0F, top(-10) - 0.5F, s * (halfWidth(-10) - 0.5F), 0.25F);
        }
        // the gilded weathervane at the masthead (it swings: setupAnim)
        CubeListBuilder vane = CubeListBuilder.create();
        box(vane, GOLD, 0, 0, -0.2F, -3.0F, -0.15F, 0.4F, 3.0F, 0.3F);
        box(vane, GOLD, 0, 0, -3.4F, -3.0F, -0.1F, 3.4F, 2.2F, 0.2F);
        box(vane, GOLD, 0, 0, -3.0F, -0.8F, -0.1F, 2.6F, 0.6F, 0.2F);
        box(vane, RED, 0, 0, -4.4F, -2.6F, -0.05F, 1.0F, 0.4F, 0.1F);
        box(vane, RED, 0, 0, -4.4F, -1.6F, -0.05F, 1.0F, 0.4F, 0.1F);
        boat.addOrReplaceChild("vane", vane, PartPose.offset(MAST_X, -46.0F, 0.0F));
    }

    // rowing benches, sea chests, the steering oar, the campfire
    private static void deckThings(BoatKit k, PartDefinition boat) {
        for (int x : new int[]{-23, -17, 11, 17, 21}) {
            int inner = halfWidth(x) - 1;
            k.box(DARKWOOD, 0, 0, x - 1.2F, -2.5F, -inner, 2.4F, 0.6F, 2 * inner);
        }
        // sea chests (their lids iron-banded)
        for (int[] c : new int[][]{{-20, 4}, {-20, -4}, {14, 4}, {14, -4}}) {
            k.box(WOOD, 20, 10, c[0] - 1.0F, DECK_Y - 2.0F, c[1] - 1.2F, 2.0F, 2.0F, 2.4F);
            k.box(GREY, 0, 0, c[0] - 1.05F, DECK_Y - 2.05F, c[1] - 0.2F, 2.1F, 2.1F, 0.4F);
        }
        // the helmsman's chest at the stern, and the steering oar on the starboard quarter with its tiller
        k.box(WOOD, 20, 10, -28.5F, DECK_Y - 2.2F, -1.5F, 3.0F, 2.2F, 3.0F);
        int sx = -30;
        int sw = halfWidth(sx);
        k.line(WOOD, sx, top(sx) - 1.0F, -sw - 0.8F, sx - 3.0F, 5.0F, -sw - 1.4F, 1.0F);
        k.box(WOOD, 10, 0, sx - 3.8F, 2.0F, -sw - 1.7F, 2.0F, 4.0F, 0.6F);
        k.line(DARKWOOD, sx, top(sx) - 1.6F, -sw - 0.8F, sx + 2.0F, top(sx) - 2.0F, -2.0F, 0.5F);
        // the campfire: a ring of stones, crossed logs, embers and flames (they flicker: setupAnim), all glowing
        float fx = -7.0F;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            k.box(GREY, (i * 3) % 12, 0, fx + (float) Math.cos(a) * 2.2F - 0.6F, DECK_Y - 0.9F, (float) Math.sin(a) * 2.2F - 0.6F, 1.2F, 0.9F, 1.2F);
        }
        k.box(GREY, 0, 10, fx - 2.0F, DECK_Y - 0.2F, -2.0F, 4.0F, 0.2F, 4.0F);
        k.line(DARKWOOD, fx - 1.6F, DECK_Y - 0.4F, -1.2F, fx + 1.6F, DECK_Y - 1.0F, 1.2F, 0.6F);
        k.line(DARKWOOD, fx - 1.6F, DECK_Y - 0.4F, 1.2F, fx + 1.6F, DECK_Y - 1.0F, -1.2F, 0.6F);
        k.box(FIRE, fx - 1.2F, DECK_Y - 0.5F, -1.2F, 2.4F, 0.3F, 2.4F);
        PartDefinition flames = boat.addOrReplaceChild("flames", CubeListBuilder.create(), PartPose.offset(fx, DECK_Y - 0.6F, 0.0F));
        float[][] spots = {{0, 0, 1.2F, 3.0F}, {0.6F, 0.5F, 0.8F, 2.2F}, {-0.6F, -0.4F, 0.8F, 2.4F}, {0.4F, -0.7F, 0.6F, 1.6F}, {-0.5F, 0.6F, 0.6F, 1.8F}};
        for (int i = 0; i < spots.length; i++) {
            float[] f = spots[i];
            CubeListBuilder flame = CubeListBuilder.create();
            box(flame, FIRE, i * 2, 0, -f[2] / 2, -f[3], -f[2] / 2, f[2], f[3], f[2]);
            flames.addOrReplaceChild("flame" + i, flame, PartPose.offset(f[0], 0, f[1]));
        }
        // a horn hanging by the helm, a coil of rope, a round shield or two leaning on the chests
        k.line(BONE, -26.0F, top(-26) + 0.2F, 2.5F, -24.0F, top(-26) + 1.2F, 3.4F, 0.6F);
        k.box(ROPE, 0, 0, 22.5F, DECK_Y - 0.5F, -1.2F, 2.4F, 0.5F, 2.4F);
    }
}

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

// The Swamp Smuggler's Raft (boats/SwampRaft, Boats.md): about 2.6 blocks long. Six mossy, rotting logs lashed under
// three cross logs with vines, a rickety half-planked deck, lily pads (one in flower) tangled round it; at the back a
// lean-to of patched tarp on crooked posts, hanging moss that sways from its edge, crates of contraband, a moonshine
// jug and bottles, a warm lantern; at the front a totem pole with a horned skull, dangling charms and an eerie green
// lantern; a punt pole, a steering oar, a fishing rod with its line and bobber, and a frog on a log. Fireflies drift
// round it (boats/SwampRaft).
public class SwampRaftModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "swamp_raft"), "main");

    static final float DECK_Y = -3.6F;
    static final float[] LOGS = {-9.6F, -5.8F, -1.9F, 1.9F, 5.8F, 9.6F};
    static final float[] BEAMS = {-13.0F, 0.0F, 13.0F};
    static final float ROOF_FRONT_X = -4.0F;
    static final float ROOF_FRONT_Y = -27.0F;
    static final float ROOF_BACK_X = -19.0F;
    static final float ROOF_BACK_Y = -23.0F;
    static final int STRANDS = 9;

    private final List<ModelPart> moss = new ArrayList<>();
    private final ModelPart lantern;
    private final ModelPart charms;

    public SwampRaftModel(ModelPart root) {
        super(root);
        for (int i = 0; i < STRANDS; i++) this.moss.add(this.boat.getChild("moss" + i));
        this.lantern = this.boat.getChild("lantern");
        this.charms = this.boat.getChild("charms");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.03F, 0.02F, 0.0F);
        for (int i = 0; i < STRANDS; i++) {
            ModelPart m = this.moss.get(i);
            m.zRot = 0.12F * Mth.sin(t * 0.06F + i * 0.9F) + 0.35F * speed;
            m.xRot = 0.08F * Mth.sin(t * 0.05F + i * 1.3F);
        }
        this.lantern.zRot = 0.15F * Mth.sin(t * 0.08F) + 0.3F * speed;
        this.charms.zRot = 0.2F * Mth.sin(t * 0.1F + 1.0F) + 0.4F * speed;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        logs(k);
        shack(k, boat);
        totem(k, boat);
        gear(k);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // the logs (each two crossed boxes, in two lengths), the cross logs, vine lashings, planks, moss, lily pads
    private static void logs(BoatKit k) {
        for (int i = 0; i < LOGS.length; i++) {
            float z = LOGS[i];
            float x0 = -19.0F - (i % 2);
            float x1 = 20.0F + (i % 3);
            float mid = (x0 + x1) / 2 + (i % 2 == 0 ? 1.5F : -1.5F);
            for (float[] seg : new float[][]{{x0, mid}, {mid, x1}}) {
                float len = seg[1] - seg[0];
                k.box(OLDWOOD, (i * 7) % 16, 0, seg[0], -1.6F, z - 1.2F, len, 3.6F, 2.4F);
                k.box(OLDWOOD, (i * 7 + 5) % 16, 8, seg[0], -1.0F, z - 1.8F, len, 2.4F, 3.6F);
            }
            k.box(WOOD, 40, 20, x1, -1.4F, z - 1.5F, 0.1F, 3.0F, 3.0F);    // the sawn ends
            k.box(WOOD, 40, 20, x0 - 0.1F, -1.4F, z - 1.5F, 0.1F, 3.0F, 3.0F);
            // moss on top here and there
            k.box(MOSS, i * 6, 0, x0 + 4 + i * 3, -1.75F, z - 1.0F, 3.0F + (i % 3), 0.2F, 2.0F);
            k.box(MOSS, i * 6 + 20, 8, mid + 3 - i, -1.75F, z - 0.9F, 2.0F, 0.2F, 1.8F);
        }
        for (float bx : BEAMS) {
            k.box(DARKWOOD, 0, 0, bx - 1.2F, -3.6F, -11.8F, 2.4F, 2.4F, 23.6F);
            k.box(DARKWOOD, 0, 6, bx - 0.8F, -4.0F, -11.6F, 1.6F, 3.2F, 23.2F);
            for (float z : LOGS) k.box(VINE, 0, 0, bx - 1.5F, -4.1F, z - 1.1F, 3.0F, 2.8F, 2.2F);   // lashings
        }
        // a rickety deck of planks at the back, gaps between them
        for (int x = -18; x <= -2; x++) {
            if (x % 5 == 0 || x == -9) continue;
            k.deck(x, DECK_Y - 0.4F, 9);
        }
        k.box(WOOD, 0, 0, 4.0F, -4.0F, -4.0F, 6.0F, 0.4F, 1.6F);                // loose planks up front
        k.box(WOOD, 0, 0, 6.0F, -4.0F, 1.0F, 5.0F, 0.4F, 1.6F);
        // lily pads tangled round the edges, one with a pink flower
        float[][] pads = {{-14, 12.8F}, {-4, 13.2F}, {9, 12.6F}, {18, -12.9F}, {2, -13.4F}, {-16, -12.8F}, {22.5F, 3.0F}};
        for (int i = 0; i < pads.length; i++) {
            k.box(MOSS, 40, 16, pads[i][0] - 1.4F, -0.15F, pads[i][1] - 1.4F, 2.8F, 0.12F, 2.8F);
        }
        k.box(RED, 0, 0, -4.4F, -0.7F, 12.8F, 0.8F, 0.6F, 0.8F);
        k.box(CANVAS, 30, 0, -4.2F, -0.9F, 13.0F, 0.4F, 0.3F, 0.4F);
    }

    // the lean-to: crooked posts, a sloping patched tarp, hanging moss (it sways), crates, jug, bottles, lantern
    private static void shack(BoatKit k, PartDefinition boat) {
        for (int s = -1; s <= 1; s += 2) {
            k.line(OLDWOOD, ROOF_FRONT_X, DECK_Y, s * 9.0F, ROOF_FRONT_X + 0.4F, ROOF_FRONT_Y + 0.5F, s * 9.4F, 1.0F);
            k.line(OLDWOOD, ROOF_BACK_X, DECK_Y, s * 9.0F, ROOF_BACK_X - 0.3F, ROOF_BACK_Y + 0.5F, s * 9.2F, 1.0F);
        }
        k.line(OLDWOOD, ROOF_FRONT_X, ROOF_FRONT_Y + 0.6F, -10.0F, ROOF_FRONT_X, ROOF_FRONT_Y + 0.6F, 10.0F, 0.8F);   // ridge
        k.line(OLDWOOD, ROOF_BACK_X, ROOF_BACK_Y + 0.6F, -10.0F, ROOF_BACK_X, ROOF_BACK_Y + 0.6F, 10.0F, 0.8F);
        float len = Mth.sqrt((ROOF_FRONT_X - ROOF_BACK_X) * (ROOF_FRONT_X - ROOF_BACK_X) + (ROOF_FRONT_Y - ROOF_BACK_Y) * (ROOF_FRONT_Y - ROOF_BACK_Y));
        float angle = -(float) Math.atan2(ROOF_BACK_Y - ROOF_FRONT_Y, ROOF_FRONT_X - ROOF_BACK_X);
        CubeListBuilder roof = CubeListBuilder.create();
        box(roof, CANVAS, 0, 0, -len - 1.0F, -0.3F, -11.0F, len + 2.0F, 0.3F, 11.0F);
        box(roof, CANVAS, 4, 4, -len - 1.0F, -0.3F, 0.0F, len + 2.0F, 0.3F, 11.0F);
        k.part(roof, PartPose.offsetAndRotation(ROOF_FRONT_X + 1.0F, ROOF_FRONT_Y, 0.0F, 0.0F, 0.0F, angle));
        // hanging moss along the front edge of the tarp
        for (int i = 0; i < STRANDS; i++) {
            float z = -10.0F + i * 2.5F;
            float h = 3.0F + (i * 7 % 4) * 1.3F;
            CubeListBuilder strand = CubeListBuilder.create();
            box(strand, VINE, i * 3, 0, -0.4F, 0.0F, -0.3F, 0.8F, h, 0.6F);
            box(strand, VINE, i * 3, 6, -0.25F, h, -0.2F, 0.5F, 1.2F, 0.4F);
            boat.addOrReplaceChild("moss" + i, strand, PartPose.offset(ROOF_FRONT_X + 1.2F, ROOF_FRONT_Y + 0.3F, z));
        }
        // contraband: crates with stencils, a barrel, the moonshine jug and bottles
        k.box(WOOD, 0, 0, -17.0F, DECK_Y - 4.0F, -8.5F, 4.0F, 4.0F, 4.0F);
        k.box(WOOD, 0, 0, -16.5F, DECK_Y - 7.0F, -8.0F, 3.0F, 3.0F, 3.0F);
        k.box(BLACK, 0, 0, -13.0F, DECK_Y - 3.0F, -7.6F, 0.05F, 1.6F, 2.2F);      // stencil marks
        k.box(DARKWOOD, 0, 0, -17.5F, DECK_Y - 4.4F, 4.6F, 3.4F, 4.4F, 3.4F);
        k.box(GREY, 0, 0, -17.6F, DECK_Y - 3.6F, 4.5F, 3.6F, 0.4F, 3.6F);
        k.box(GREY, 0, 0, -17.6F, DECK_Y - 1.4F, 4.5F, 3.6F, 0.4F, 3.6F);
        k.box(BONE, 0, 0, -12.6F, DECK_Y - 2.8F, 6.4F, 2.0F, 2.8F, 2.0F);         // the jug
        k.box(BONE, 0, 0, -12.1F, DECK_Y - 3.6F, 6.9F, 1.0F, 0.8F, 1.0F);
        k.box(BLACK, 0, 0, -11.95F, DECK_Y - 3.9F, 7.05F, 0.7F, 0.3F, 0.7F);
        k.box(RED, 0, 0, -11.55F, DECK_Y - 2.2F, 6.5F, 0.05F, 0.8F, 1.8F);         // "XXX" label
        for (int b = 0; b < 3; b++) {
            k.box(TINT, 0, 0, -13.8F + b * 1.1F, DECK_Y - 2.0F, 3.0F, 0.8F, 2.0F, 0.8F);
            k.box(TINT, 0, 0, -13.6F + b * 1.1F, DECK_Y - 2.8F, 3.2F, 0.4F, 0.8F, 0.4F);
        }
        // a lantern on a rope from the front of the tarp (it sways)
        CubeListBuilder lamp = CubeListBuilder.create();
        box(lamp, ROPE, 0, 0, -0.1F, 0.0F, -0.1F, 0.2F, 2.6F, 0.2F);
        box(lamp, GREY, 0, 0, -0.7F, 2.6F, -0.7F, 1.4F, 0.4F, 1.4F);
        box(lamp, WARM, 0, 0, -0.5F, 3.0F, -0.5F, 1.0F, 1.6F, 1.0F);
        box(lamp, GREY, 0, 0, -0.7F, 4.6F, -0.7F, 1.4F, 0.4F, 1.4F);
        boat.addOrReplaceChild("lantern", lamp, PartPose.offset(ROOF_FRONT_X + 0.8F, ROOF_FRONT_Y + 1.0F, -6.5F));
    }

    // the totem at the front: a crooked pole, crossbar, horned skull, charms (they sway), the green lantern
    private static void totem(BoatKit k, PartDefinition boat) {
        float px = 16.0F;
        float pz = -8.0F;
        k.line(OLDWOOD, px, -1.6F, pz, px + 0.6F, -21.0F, pz + 0.3F, 0.9F);
        k.line(OLDWOOD, px + 0.5F, -17.0F, pz - 3.0F, px + 0.5F, -17.6F, pz + 3.5F, 0.6F);
        k.box(BONE, 0, 0, px - 0.6F, -23.4F, pz - 1.0F, 2.4F, 2.0F, 2.6F);                 // the skull
        k.box(BONE, 0, 0, px + 1.6F, -22.6F, pz - 0.7F, 1.6F, 1.2F, 2.0F);                 // snout
        k.box(BLACK, 0, 0, px + 1.8F, -22.9F, pz + 0.5F, 0.05F, 0.5F, 0.5F);
        k.box(BLACK, 0, 0, px + 1.8F, -22.9F, pz - 0.5F, 0.05F, 0.5F, 0.5F);
        k.line(BONE, px + 0.2F, -23.2F, pz + 1.2F, px - 0.8F, -25.4F, pz + 3.2F, 0.5F);    // horns
        k.line(BONE, px + 0.2F, -23.2F, pz - 1.2F, px - 0.8F, -25.4F, pz - 3.2F, 0.5F);
        // charms on strings from the crossbar
        CubeListBuilder charm = CubeListBuilder.create();
        box(charm, ROPE, 0, 0, -0.08F, 0.0F, -0.08F, 0.16F, 2.4F, 0.16F);
        box(charm, BONE, 0, 0, -0.3F, 2.4F, -0.3F, 0.6F, 0.9F, 0.2F);
        box(charm, ROPE, 0, 0, -0.08F, 0.0F, 1.92F, 0.16F, 1.6F, 0.16F);
        box(charm, RED, 0, 0, -0.25F, 1.6F, 1.8F, 0.5F, 1.2F, 0.1F);                       // a feather
        boat.addOrReplaceChild("charms", charm, PartPose.offset(px + 0.5F, -17.0F, pz + 1.5F));
        // the eerie green lantern hanging off the far end of the crossbar
        k.line(ROPE, px + 0.5F, -17.4F, pz - 2.8F, px + 0.5F, -14.6F, pz - 2.8F, 0.15F);
        k.box(GREY, 0, 0, px - 0.2F, -14.6F, pz - 3.5F, 1.4F, 0.4F, 1.4F);
        k.box(NAV_GREEN, px, -14.2F, pz - 3.3F, 1.0F, 1.6F, 1.0F);
        k.box(GREY, 0, 0, px - 0.2F, -12.6F, pz - 3.5F, 1.4F, 0.4F, 1.4F);
    }

    // the punt pole, steering oar, fishing rod and line, and a frog
    private static void gear(BoatKit k) {
        k.line(OLDWOOD, -14.0F, DECK_Y - 0.5F, 10.4F, 6.0F, -24.0F, 12.5F, 0.6F);           // punt pole leaning on the shack
        k.line(DARKWOOD, -18.5F, DECK_Y - 1.0F, 0.0F, -27.0F, 2.0F, 0.0F, 0.7F);            // steering oar
        k.box(DARKWOOD, 0, 0, -28.5F, 0.4F, -0.2F, 2.6F, 3.0F, 0.4F);
        k.line(DARKWOOD, -18.5F, DECK_Y - 1.0F, 0.0F, -15.0F, DECK_Y - 2.4F, 0.0F, 0.5F);   // tiller
        k.line(DARKWOOD, 8.0F, -4.0F, -10.0F, 14.0F, -14.5F, -15.5F, 0.35F);                // fishing rod
        k.line(ROPE, 14.0F, -14.5F, -15.5F, 15.5F, 0.0F, -16.5F, 0.12F);
        k.box(RED, 0, 0, 15.1F, -0.6F, -16.9F, 0.8F, 0.6F, 0.8F);
        k.box(CANVAS, 30, 0, 15.1F, -0.9F, -16.9F, 0.8F, 0.3F, 0.8F);
        // a frog sitting on a log
        k.box(MOSS, 50, 0, 10.0F, -2.8F, 9.0F, 1.6F, 1.2F, 1.4F);
        k.box(MOSS, 50, 0, 11.1F, -3.2F, 9.1F, 0.8F, 0.6F, 1.2F);
        k.box(GOLD, 0, 0, 11.5F, -3.5F, 9.0F, 0.4F, 0.4F, 0.4F);
        k.box(GOLD, 0, 0, 11.5F, -3.5F, 9.9F, 0.4F, 0.4F, 0.4F);
    }
}

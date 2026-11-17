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

// The Wandering Teapot (boats/Teapot, Boats.md): a great round teapot of glazed white porcelain (blue flowers, gold
// bands) sitting on a broad birch saucer; a curving spout in front, a looped handle behind, its lid tipped open on its
// hinge so you can see in: brewing stands with their bottles, a cake, cushions, the riders sitting in the pot. A
// little campfire glows under the pot (real smoke: boats/Teapot), and from the spout billows a great plume of "steam"
// (white glassy puffs and cobwebs) for a sail, swelling and drifting with its speed.
public class TeapotModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "teapot"), "main");

    static final float BOTTOM = -1.0F;      // the pot's bottom (on the saucer)
    static final float POT_TOP = -21.0F;    // its rim
    static final int PUFFS = 9;

    // the pot's radius at height y (round in the middle, narrower at the foot and the shoulder)
    static float radius(float y) {
        float t = (y - POT_TOP) / (BOTTOM - POT_TOP);   // 0 at the rim, 1 at the bottom
        return 7.5F + 6.0F * Mth.sin((float) Math.PI * (0.15F + 0.75F * t));
    }

    private final List<ModelPart> puffs = new ArrayList<>();
    private final ModelPart lid;

    public TeapotModel(ModelPart root) {
        super(root);
        for (int i = 0; i < PUFFS; i++) this.puffs.add(this.boat.getChild("puff" + i));
        this.lid = this.boat.getChild("lid");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.03F, 0.02F, 0.0F);
        // the steam swells and drifts back with its speed
        for (int i = 0; i < PUFFS; i++) {
            ModelPart puff = this.puffs.get(i);
            float s = 0.9F + 0.15F * Mth.sin(t * 0.08F + i * 1.3F) + 0.25F * speed;
            puff.xScale = s;
            puff.yScale = s;
            puff.zScale = s;
            puff.yRot = 0.1F * Mth.sin(t * 0.03F + i);
        }
        this.lid.zRot = -1.9F + 0.04F * Mth.sin(t * 0.1F);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        // the birch saucer: a broad disc with a raised rim, floating
        k.disc(WOOD, 0.0F, 0.6F, 0.0F, 19.0F, 1.6F);
        k.ringY(WOOD, 0.0F, -0.4F, 0.0F, 19.0F, 1.2F, 1.2F, 24, 0);
        k.ringY(GOLD, 0.0F, -0.6F, 0.0F, 19.2F, 0.4F, 0.3F, 24, 0);
        // the campfire under the pot: logs and a glow, seen round the pot's foot
        k.line(DARKWOOD, -6.0F, 0.2F, -2.0F, 6.0F, 0.2F, 2.0F, 0.8F);
        k.line(DARKWOOD, -6.0F, 0.2F, 2.0F, 6.0F, 0.2F, -2.0F, 0.8F);
        k.box(FIRE, -5.0F, -0.6F, -5.0F, 10.0F, 0.4F, 10.0F);
        // the pot: rings of porcelain panels, round, hollow (you see in from the open top)
        // (a wide window in its front, from the rim down, so whoever sits inside can see out)
        for (float y = POT_TOP; y < BOTTOM; y += 2.0F) {
            float r = radius(y + 1.0F);
            k.ringY(FIBER, 0.0F, y, 0.0F, r, 2.0F, 1.0F, 20, (int) (y * 3), y < -9.0F ? 0.55F : 2.0F);
        }
        k.line(GOLD, 9.0F, -9.4F, -9.6F, 9.0F, -9.4F, 9.6F, 0.5F);           // a gold sill under the window
        k.disc(FIBER, 0.0F, BOTTOM - 1.0F, 0.0F, radius(BOTTOM - 1.0F) - 0.5F, 1.0F);    // its floor inside
        k.ringY(GOLD, 0.0F, POT_TOP - 0.5F, 0.0F, radius(POT_TOP) + 0.1F, 0.6F, 1.2F, 20, 0);   // the gold rim
        k.ringY(GOLD, 0.0F, -11.4F, 0.0F, radius(-11.0F) + 0.15F, 0.5F, 0.3F, 20, 0);          // a gold band round its belly
        // the spout: curving up and out in front
        float[][] spout = {{10.5F, -6.0F}, {14.0F, -9.0F}, {17.0F, -13.5F}, {19.0F, -18.0F}, {20.5F, -22.0F}};
        float[] th = {3.6F, 3.0F, 2.6F, 2.2F};
        for (int i = 0; i + 1 < spout.length; i++) k.line(FIBER, spout[i][0], spout[i][1], 0, spout[i + 1][0], spout[i + 1][1], 0, th[i]);
        k.line(GOLD, 20.3F, -21.6F, 0, 20.9F, -22.6F, 0, 2.4F);
        // the handle: a loop behind
        float[][] handle = {{-12.0F, -16.0F}, {-17.0F, -17.5F}, {-19.5F, -13.0F}, {-18.0F, -8.0F}, {-12.5F, -6.0F}};
        for (int i = 0; i + 1 < handle.length; i++) k.line(FIBER, handle[i][0], handle[i][1], 0, handle[i + 1][0], handle[i + 1][1], 0, 2.0F);
        // the lid, tipped open on its hinge at the back of the rim (with its knob)
        CubeListBuilder lid = CubeListBuilder.create();
        float lr = radius(POT_TOP) - 0.5F;
        box(lid, FIBER, 0, 0, 0.0F, -1.0F, -lr * 0.7F, 2 * lr, 1.0F, lr * 1.4F);
        box(lid, FIBER, 4, 4, lr * 0.3F, -1.6F, -lr * 0.45F, lr * 1.4F, 0.6F, lr * 0.9F);
        box(lid, GOLD, 0, 0, lr - 1.0F, -3.2F, -1.0F, 2.0F, 1.6F, 2.0F);
        box(lid, GOLD, 0, 0, 0.0F, -0.4F, -lr * 0.72F, 2 * lr, 0.4F, 0.3F);
        boat.addOrReplaceChild("lid", lid, PartPose.offsetAndRotation(-lr, POT_TOP - 0.6F, 0.0F, 0.0F, 0.0F, -1.9F));
        interior(k);
        steam(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // inside the pot: brewing stands with bottles, a cake on a stand, cushions
    private static void interior(BoatKit k) {
        float floor = BOTTOM - 1.0F;
        for (int s = -1; s <= 1; s += 2) {
            float x = -8.0F;
            float z = s * 5.5F;
            k.box(GREY, x - 1.2F, floor - 0.4F, z - 1.2F, 2.4F, 0.4F, 2.4F);
            k.box(BRASS, x - 0.2F, floor - 5.0F, z - 0.2F, 0.4F, 4.6F, 0.4F);
            for (int a = 0; a < 3; a++) {
                double ang = a * 2 * Math.PI / 3;
                float bx = x + (float) Math.cos(ang) * 1.4F;
                float bz = z + (float) Math.sin(ang) * 1.4F;
                k.line(BRASS, x, floor - 4.0F, z, bx, floor - 3.2F, bz, 0.25F);
                k.box(a == 0 ? NAV_RED : a == 1 ? BLUE_GLOW : WATER, bx - 0.4F, floor - 2.2F, bz - 0.4F, 0.8F, 1.6F, 0.8F);
            }
        }
        // the cake on a little gold stand
        k.box(GOLD, 6.0F, floor - 2.0F, -1.0F, 2.0F, 2.0F, 2.0F);
        k.disc(CANVAS, 7.0F, floor - 4.2F, 0.0F, 2.6F, 2.2F);
        k.disc(RED, 7.0F, floor - 4.4F, 0.0F, 2.4F, 0.3F);
        k.box(WHITE, 6.6F, floor - 5.4F, -0.4F, 0.8F, 1.0F, 0.8F);
        k.box(FIRE, 6.85F, floor - 5.9F, -0.15F, 0.3F, 0.5F, 0.3F);
        // cushions to sit on
        k.box(LEATHER, -6.0F, floor - 1.2F, -4.0F, 5.0F, 1.2F, 8.0F);
        k.box(LEATHER, 0.5F, floor - 1.2F, -6.0F, 4.0F, 1.2F, 4.5F);
        k.box(LEATHER, 0.5F, floor - 1.2F, 1.5F, 4.0F, 1.2F, 4.5F);
    }

    // the plume of steam from the spout: puffs of white glass and cobweb billowing up and back (they swell: setupAnim)
    private static void steam(BoatKit k, PartDefinition boat) {
        float[][] puffs = {{21.0F, -25.0F, 0.0F, 2.6F}, {20.0F, -29.0F, 1.0F, 3.4F}, {18.0F, -33.5F, -1.0F, 4.2F}, {15.0F, -38.0F, 1.5F, 5.0F},
            {11.0F, -42.0F, -1.5F, 5.6F}, {6.0F, -45.0F, 1.0F, 6.0F}, {1.0F, -47.0F, -0.5F, 5.6F}, {-4.0F, -48.0F, 1.0F, 4.6F}, {-8.5F, -48.5F, 0.0F, 3.4F}};
        for (int i = 0; i < puffs.length; i++) {
            float[] p = puffs[i];
            float r = p[3];
            CubeListBuilder puff = CubeListBuilder.create();
            box(puff, CANVAS, i * 3, 0, -r * 0.9F, -r * 0.75F, -r * 0.9F, 1.8F * r, 1.5F * r, 1.8F * r);
            box(puff, VINE, i * 2, 0, -r * 0.45F, -r * 1.1F, -r * 0.45F, 0.9F * r, 0.8F * r, 0.9F * r);
            boat.addOrReplaceChild("puff" + i, puff, PartPose.offset(p[0], p[1], p[2]));
        }
    }
}

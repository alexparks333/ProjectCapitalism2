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

// The Leviathan's Wake (boats/LeviathanWake, Boats.md): scavengers built a dark oak deck inside a sea monster's
// skeleton. A dark oak hull lashed with bone (the hull-side pictures); a great ribcage of curved bone ribs arching up
// either side, their tips curling out, blue soul lanterns hanging from them on chains; the monster's skull at the bow
// (glowing eyes, fangs, tusks) and its tail of vertebrae trailing behind; a mast of stacked vertebrae carrying a fan
// of jagged amethyst shards and tinted glass that shimmers; and in the middle, its heart of crying obsidian, beating.
public class LeviathanModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "leviathan"), "main");

    static final int STERN = -24;
    static final int BOW = 28;
    static final float DECK_Y = -1.5F;
    static final float MAST_X = 6.0F;
    static final float HEART_X = 10.0F;
    static final int[] RIBS = {-21, -15, -9, -3, 3, 9, 15, 21};
    static final int SHARDS = 14;

    static int halfWidth(int x) {
        double t = Math.abs(x + 0.5 - 2.0) / 26.5;
        if (t >= 1) return 1;
        return Math.max(1, (int) Math.round(9 * Math.pow(1 - Math.pow(t, 2.2), 0.55)));
    }

    static int top(int x) {
        return -4;
    }

    private final ModelPart heart;
    private final List<ModelPart> shards = new ArrayList<>();
    private final List<ModelPart> lanterns = new ArrayList<>();

    public LeviathanModel(ModelPart root) {
        super(root);
        this.heart = this.boat.getChild("heart");
        for (int i = 0; i < SHARDS; i++) this.shards.add(this.boat.getChild("shard" + i));
        for (int i = 0; i < RIBS.length * 2; i++) this.lanterns.add(this.boat.getChild("lantern" + i));
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        this.rock(state, 0.02F, 0.015F, 0.0F);
        // the heart beats: two quick throbs, then a rest
        float beat = (t % 24.0F) / 24.0F;
        float pulse = Math.max(0.0F, Mth.sin(beat * (float) Math.PI * 6)) * (beat < 0.34F ? 1.0F : 0.0F);
        float s = 1.0F + 0.12F * pulse;
        this.heart.xScale = s;
        this.heart.yScale = s;
        this.heart.zScale = s;
        // the amethyst shimmers
        for (int i = 0; i < SHARDS; i++) {
            ModelPart shard = this.shards.get(i);
            float shimmer = 1.0F + 0.04F * Mth.sin(t * 0.15F + i * 0.9F) + 0.05F * speed;
            shard.yScale = shimmer;
        }
        // the soul lanterns swing on their chains
        for (int i = 0; i < this.lanterns.size(); i++) {
            ModelPart lantern = this.lanterns.get(i);
            lantern.zRot = 0.12F * Mth.sin(t * 0.07F + i * 0.6F) + 0.25F * speed;
            lantern.xRot = 0.08F * Mth.sin(t * 0.05F + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        int[] thick = k.hull(STERN, BOW, LeviathanModel::halfWidth, LeviathanModel::top,
            new float[][]{{1, 2.5F, 0.8F}, {2.5F, 3.6F, 0.45F}}, 1);
        for (int x = STERN; x <= BOW; x++) {
            int w = halfWidth(x);
            int t = thick[x - STERN];
            int inner = w - t;
            k.box(BONE, (x - STERN) % 20, 0, x, top(x) - 0.5F, w - t - 0.2F, 1, 0.5F, t + 0.4F);
            k.box(BONE, (x - STERN) % 20, 0, x, top(x) - 0.5F, -w - 0.2F, 1, 0.5F, t + 0.4F);
            if (inner < 1) continue;
            k.box(DARKWOOD, (x - STERN) % 30, 0, x, DECK_Y, -inner, 1, 1 - DECK_Y, 2 * inner);
            k.deck(x, DECK_Y, inner);
        }
        ribs(k, boat);
        skullAndTail(k);
        mastAndSails(k, boat);
        heart(k, boat);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // the ribcage: curved bone ribs either side, tips curling out, soul lanterns on chains (they swing: setupAnim)
    private static void ribs(BoatKit k, PartDefinition boat) {
        int n = 0;
        for (int x : RIBS) {
            float f = 1.0F - 0.45F * (float) Math.pow(Math.abs(x) / 26.0, 2);
            for (int s = -1; s <= 1; s += 2) {
                float[][] rib = {{2.0F, 10.5F}, {-8.0F, 11.6F}, {-18.0F, 11.2F}, {-27.0F, 9.6F}, {-33.0F, 7.2F}, {-35.5F, 8.6F}};
                float[] th = {1.9F, 1.7F, 1.5F, 1.2F, 0.9F};
                for (int i = 0; i + 1 < rib.length; i++) {
                    k.line(BONE, x, rib[i][0] * f, s * rib[i][1], x, rib[i + 1][0] * f, s * rib[i + 1][1], th[i]);
                }
                k.box(BONE, x - 0.9F, top(x) - 1.8F, s > 0 ? halfWidth(x) - 0.6F : -halfWidth(x) - 1.2F, 1.8F, 1.8F, 1.8F);  // where it joins
                // the lantern hanging from the curled tip
                float tipY = -35.5F * f;
                float tipZ = s * 8.6F;
                CubeListBuilder lantern = CubeListBuilder.create();
                box(lantern, GREY, 0, 0, -0.08F, 0.0F, -0.08F, 0.16F, 3.0F, 0.16F);
                box(lantern, GREY, 0, 0, -0.7F, 3.0F, -0.7F, 1.4F, 0.4F, 1.4F);
                box(lantern, BLUE_GLOW, 0, 0, -0.5F, 3.4F, -0.5F, 1.0F, 1.6F, 1.0F);
                box(lantern, GREY, 0, 0, -0.7F, 5.0F, -0.7F, 1.4F, 0.4F, 1.4F);
                boat.addOrReplaceChild("lantern" + (n++), lantern, PartPose.offset(x, tipY, tipZ));
            }
        }
    }

    // the skull at the bow (glowing eyes, fangs, tusks), the tail of vertebrae behind
    private static void skullAndTail(BoatKit k) {
        float sx = BOW - 1.0F;
        k.box(BONE, 0, 0, sx, -9.0F, -4.0F, 7.0F, 6.0F, 8.0F);                    // cranium
        k.box(BONE, 10, 0, sx + 6.0F, -7.0F, -3.0F, 4.0F, 3.4F, 6.0F);            // snout
        k.box(BONE, 10, 10, sx + 1.0F, -3.0F, -3.4F, 8.0F, 1.6F, 6.8F);           // jaw
        k.box(BLACK, sx + 3.0F, -8.2F, 4.0F, 2.4F, 2.0F, 0.1F);                   // eye sockets
        k.box(BLACK, sx + 3.0F, -8.2F, -4.1F, 2.4F, 2.0F, 0.1F);
        k.box(GHOST, sx + 3.5F, -7.8F, 4.05F, 1.4F, 1.2F, 0.1F);                  // their glow
        k.box(GHOST, sx + 3.5F, -7.8F, -4.15F, 1.4F, 1.2F, 0.1F);
        for (int i = 0; i < 4; i++) {                                              // fangs
            k.box(BONE, 4, 4, sx + 6.2F + i * 0.9F, -3.6F, 2.2F, 0.5F, 1.4F, 0.5F);
            k.box(BONE, 4, 4, sx + 6.2F + i * 0.9F, -3.6F, -2.7F, 0.5F, 1.4F, 0.5F);
        }
        for (int s = -1; s <= 1; s += 2) {                                         // tusks and horns
            k.line(BONE, sx + 8.0F, -4.0F, s * 2.6F, sx + 12.0F, -7.0F, s * 4.2F, 0.8F);
            k.line(BONE, sx + 1.0F, -8.6F, s * 3.2F, sx - 3.0F, -13.0F, s * 5.0F, 1.0F);
            k.line(BONE, sx - 3.0F, -13.0F, s * 5.0F, sx - 6.0F, -12.0F, s * 6.2F, 0.7F);
        }
        // the tail: vertebrae shrinking away behind, rising to a bony fin
        float[][] tail = {{STERN, -2.0F, 2.6F}, {STERN - 3.0F, -2.5F, 2.3F}, {STERN - 6.0F, -3.5F, 2.0F}, {STERN - 9.0F, -5.0F, 1.7F},
            {STERN - 12.0F, -7.0F, 1.4F}, {STERN - 14.5F, -9.5F, 1.1F}, {STERN - 16.5F, -12.0F, 0.9F}};
        for (float[] v : tail) {
            k.box(BONE, v[0] - v[2] / 2, v[1] - v[2] / 2, -v[2] / 2, v[2] * 0.8F, v[2], v[2]);
            k.line(BONE, v[0], v[1] - v[2] / 2, 0, v[0] - 0.4F, v[1] - v[2] * 1.4F, 0, 0.4F);   // the spine's spikes
        }
        k.line(BONE, STERN - 16.5F, -12.0F, 0, STERN - 18.0F, -17.0F, 0, 0.6F);
        k.line(BONE, STERN - 16.5F, -12.0F, 0, STERN - 20.0F, -11.0F, 0, 0.6F);
    }

    // the vertebra mast and the amethyst sail: shards fanning out from it, tinted glass between (they shimmer)
    private static void mastAndSails(BoatKit k, PartDefinition boat) {
        for (float y = DECK_Y - 2.0F; y > -42.0F; y -= 2.6F) {
            k.box(BONE, MAST_X - 1.1F, y - 1.6F, -1.1F, 2.2F, 1.6F, 2.2F);
            k.box(BONE, MAST_X - 0.7F, y - 2.6F, -0.7F, 1.4F, 1.0F, 1.4F);
        }
        for (int i = 0; i < SHARDS; i++) {
            float len = 10.0F + 6.0F * Mth.sin((float) Math.PI * i / (SHARDS - 1)) + (i % 3) * 1.5F;
            float y0 = -16.0F - (i % 4) * 4.0F;
            CubeListBuilder shard = CubeListBuilder.create();
            float th = 1.2F + (i % 3) * 0.4F;
            box(shard, ACCENT, i * 2, 0, -th / 2, -len, -th / 2, th, len, th);
            box(shard, ACCENT, i * 2 + 1, 4, -th * 0.3F, -len - 1.6F, -th * 0.3F, th * 0.6F, 1.6F, th * 0.6F);
            boat.addOrReplaceChild("shard" + i, shard, PartPose.offsetAndRotation(MAST_X + 1.0F, y0, 0.0F, -1.25F + i * 2.5F / (SHARDS - 1), 0.0F, 0.0F));
        }
        // tinted glass panes between the shards (flat, facing fore and aft)
        float[][] panes = {{-30, -8, 10}, {-34, -2, 9}, {-36, 4, 9}, {-32, 9, 8}, {-26, -12, 7}, {-24, 12, 6}};
        for (int i = 0; i < panes.length; i++) {
            float[] p = panes[i];
            k.box(TINT, MAST_X + 0.9F + i * 0.06F, p[0], p[1] - 3.5F, 0.0F, p[2], 7.0F);   // (each a hair apart)
        }
    }

    // the crying obsidian heart in a cradle of bone claws (it beats: setupAnim)
    private static void heart(BoatKit k, PartDefinition boat) {
        CubeListBuilder heart = CubeListBuilder.create();
        box(heart, NAVY, 0, 0, -2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F);
        box(heart, NAVY, 8, 0, -1.5F, -2.6F, -1.5F, 3.0F, 0.6F, 3.0F);
        boat.addOrReplaceChild("heart", heart, PartPose.offset(HEART_X, DECK_Y - 5.0F, 0.0F));
        for (int c = 0; c < 4; c++) {
            double a = c * Math.PI / 2 + Math.PI / 4;
            float cx = HEART_X + (float) Math.cos(a) * 3.0F;
            float cz = (float) Math.sin(a) * 3.0F;
            k.line(BONE, cx, DECK_Y, cz, HEART_X + (float) Math.cos(a) * 2.2F, DECK_Y - 6.0F, (float) Math.sin(a) * 2.2F, 0.6F);
        }
        k.disc(BONE, HEART_X, DECK_Y - 1.0F, 0.0F, 3.4F, 1.0F);
    }
}

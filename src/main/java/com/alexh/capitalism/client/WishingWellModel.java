package com.alexh.capitalism.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

import static com.alexh.capitalism.client.BoatKit.*;

// ═════════════ the Wishing Well (3 x 3 blocks, 3 high) ═════════════
// A round fieldstone well: two courses of stones (each course offset half a stone, like real stonework) under a rim of
// capstones, moss creeping over them. Inside, clear water (its own see-through model, Water below) over a floor of wet
// pebbles with coins thrown in. Two dark oak posts stand on the ring at either side, braced, holding a log axle with
// rope coiled round it and a crank on one end; the rope hangs down to an oak bucket (staves, two iron hoops, an iron
// handle, water in it) swaying over the well. Over it all a pitched roof of red shingles in rows, a ridge cap along its
// top, plank gables at its ends, and a little lantern hanging under the ridge.
// In model pixels: y down from the ground, the ring round the middle, the posts at the sides (z), the roof's ridge along
// z, its slopes falling toward the front and back (x). Matches occupation/WishingWellBlock's collision.
public class WishingWellModel extends DecorModels.Base {

    public static final ModelLayerLocation LAYER = DecorModels.layer("wishing_well");
    private final ModelPart bucket;

    public WishingWellModel(ModelPart root) {
        super(root);
        this.bucket = this.decor.getChild("bucket");
    }

    @Override
    public void setupAnim(Float time) {
        super.setupAnim(time);
        // the bucket sways gently on its rope
        this.bucket.zRot = 0.05F * Mth.sin(time * 0.045F);
        this.bucket.xRot = 0.04F * Mth.sin(time * 0.031F + 1.3F);
    }

    static final float RING_OUT = 22.0F;
    static final float RING_IN = 16.5F;

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        BoatKit k = new BoatKit(root);
        float thick = RING_OUT - RING_IN;
        // two courses of stones round the ring, the upper one turned half a stone
        int stones = 18;
        float stoneW = 2 * Mth.PI * RING_OUT / stones + 0.8F;
        for (int course = 0; course < 2; course++) {
            for (int s = 0; s < stones; s++) {
                float a = (s + course * 0.5F) * 2 * Mth.PI / stones;
                CubeListBuilder stone = CubeListBuilder.create();
                box(stone, GREY, (s * 7) % 30, course * 10, RING_IN, -7.0F * (course + 1), -stoneW / 2, thick, 6.9F, stoneW);
                k.part(stone, PartPose.offsetAndRotation(0, 0, 0, 0, a, 0));
            }
        }
        // the rim: flat capstones, a little wider than the wall
        for (int s = 0; s < stones; s++) {
            float a = (s + 0.25F) * 2 * Mth.PI / stones;
            CubeListBuilder cap = CubeListBuilder.create();
            box(cap, GREY, (s * 5) % 24, 22, RING_IN - 0.8F, -15.6F, -stoneW / 2 - 0.2F, thick + 1.8F, 1.6F, stoneW + 0.4F);
            k.part(cap, PartPose.offsetAndRotation(0, 0, 0, 0, a, 0));
        }
        // moss creeping over the stones here and there
        float[][] moss = {{0.4F, -14.0F}, {1.9F, -7.5F}, {3.3F, -15.7F}, {4.6F, -3.0F}, {5.5F, -15.7F}};
        for (float[] m : moss) {
            CubeListBuilder patch = CubeListBuilder.create();
            box(patch, MOSS, 0, 0, RING_OUT - 0.4F, m[1], -3.0F, 0.8F, 4.0F, 6.0F);
            k.part(patch, PartPose.offsetAndRotation(0, 0, 0, 0, m[0], 0));
        }
        // the bottom of the well: wet pebbles, coins and a few stones lying on them, seen through the water
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                float wx = -16.0F + i * 4.0F, wz = -16.0F + j * 4.0F;
                if (inPool(wx, wz)) k.box(NAVY, (i * 4) % 16, (j * 3) % 11, wx, -3.0F, wz, 4.0F, 1.0F, 4.0F);
            }
        }
        float[][] coins = {{4, 3}, {-6, 2}, {2, -7}, {-3, -4}, {8, -2}, {-9, -6}, {6, 9}, {-2, 10}, {11, 5}};
        for (int c = 0; c < coins.length; c++) {
            CubeListBuilder coin = CubeListBuilder.create();
            box(coin, GOLD, c * 3, 0, -0.8F, -0.25F, -0.8F, 1.6F, 0.25F, 1.6F);
            k.part(coin, PartPose.offsetAndRotation(coins[c][0], -3.0F, coins[c][1], 0, c * 0.7F, (c % 3 - 1) * 0.12F));
        }
        float[][] pebbles = {{-11, 4, 2.2F}, {9, -9, 1.8F}, {-4, 12, 1.6F}, {12, 1, 1.4F}};
        for (float[] p : pebbles) k.box(GREY, (int) p[0] + 12, 30, p[0], -3.0F - p[2] * 0.6F, p[1], p[2], p[2] * 0.6F, p[2] * 1.2F);
        // the posts, standing on the ring at both sides, with knee braces up to the axle
        for (int side = -1; side <= 1; side += 2) {
            float z = side * 20.0F;
            k.box(DARKWOOD, (side + 1) * 8, 0, -2.0F, -44.0F, z - 2.0F, 4.0F, 16.0F, 4.0F);
            k.box(DARKWOOD, (side + 1) * 8 + 4, 4, -2.0F, -28.0F, z - 2.0F, 4.0F, 15.0F, 4.0F);
            k.line(DARKWOOD, -1.5F, -36.0F, z, -9.0F, -41.5F, z, 1.4F);
            k.line(DARKWOOD, 1.5F, -36.0F, z, 9.0F, -41.5F, z, 1.4F);
        }
        // the axle between them, rope coiled round its middle (each turn an eighth round from the last, so it looks round)
        k.box(WOOD, 0, 0, -1.6F, -30.1F, -18.0F, 3.2F, 3.2F, 18.0F);
        k.box(WOOD, 10, 4, -1.6F, -30.1F, 0.0F, 3.2F, 3.2F, 18.0F);
        for (int coil = 0; coil < 4; coil++) {
            CubeListBuilder turn = CubeListBuilder.create();
            box(turn, ROPE, 0, coil * 2, -2.2F, -2.2F, -1.15F, 4.4F, 4.4F, 2.3F);
            k.part(turn, PartPose.offsetAndRotation(0.0F, -28.5F, -3.45F + coil * 2.3F, 0, 0, (coil % 2) * Mth.PI / 4));
        }
        // the crank on the right end, outside the post
        k.box(DARKWOOD, 0, 20, -1.0F, -29.5F, 22.0F, 2.0F, 2.0F, 2.0F);
        k.line(DARKWOOD, 0.0F, -28.5F, 24.2F, 5.5F, -23.5F, 24.2F, 1.4F);
        k.box(WOOD, 4, 20, 4.6F, -24.4F, 24.4F, 1.8F, 1.8F, 4.5F);
        // the rope down to the bucket (the bucket sways: its own part, hung from the axle)
        PartDefinition bucket = root.addOrReplaceChild("bucket", CubeListBuilder.create(), PartPose.offset(0.0F, -28.5F, 0.0F));
        BoatKit b = new BoatKit(bucket);
        // the rope, knotted round the top of the handle
        b.line(ROPE, 0, 1.6F, 0, 0, 8.4F, 0, 0.7F);
        b.box(ROPE, 0, 0, -0.65F, 7.8F, -0.65F, 1.3F, 1.1F, 1.3F);
        // the bucket: oak staves round a circle, wider at the top, bound with two iron hoops
        float top = 11.0F, tall = 6.4F, rTop = 3.3F, rBot = 2.7F;
        int staves = 14;
        float lean = -(float) Math.atan2(rTop - rBot, tall);
        for (int s = 0; s < staves; s++) {
            CubeListBuilder stave = CubeListBuilder.create();
            box(stave, OLDWOOD, (s * 5) % 56, (s * 7) % 24, -0.75F, 0.0F, rTop - 0.6F, 1.5F, tall, 0.6F);
            box(stave, STEEL, (s * 3) % 50, 0, -0.8F, 0.8F, rTop - 0.05F, 1.6F, 0.75F, 0.3F);
            box(stave, STEEL, (s * 3) % 50, 6, -0.8F, 4.9F, rTop - 0.05F, 1.6F, 0.75F, 0.3F);
            b.part(stave, PartPose.offsetAndRotation(0.0F, top, 0.0F, lean, (s + 0.5F) * 2 * Mth.PI / staves, 0));
        }
        // its bottom, and the water in it near the brim
        b.box(OLDWOOD, 0, 0, -2.2F, top + tall - 0.7F, -1.3F, 4.4F, 0.6F, 2.6F);
        b.box(OLDWOOD, 20, 0, -1.3F, top + tall - 0.69F, -2.2F, 2.6F, 0.6F, 4.4F);
        b.box(WATER, 0, 0, -2.5F, top + 0.9F, -1.4F, 5.0F, 0.2F, 2.8F);
        b.box(WATER, 0, 0, -1.4F, top + 0.91F, -2.5F, 2.8F, 0.2F, 5.0F);
        // the iron handle: ears on both sides, an arc up to the rope
        for (int side = -1; side <= 1; side += 2) b.box(STEEL, 0, 12, side > 0 ? 3.15F : -3.85F, top + 0.4F, -0.55F, 0.7F, 1.4F, 1.1F);
        float px = 3.5F, py = top + 1.0F;
        for (int seg = 1; seg <= 6; seg++) {
            float t = seg * Mth.PI / 6;
            float qx = 3.5F * Mth.cos(t), qy = top + 1.0F - 3.0F * Mth.sin(t);
            b.line(BLACK, px, py, 0, qx, qy, 0, 0.45F);
            px = qx;
            py = qy;
        }
        bucket.addOrReplaceChild("statics", b.statics, PartPose.ZERO);
        // the roof: two slopes of red shingles falling from the ridge to the front and back, rows overlapping
        float slope = (float) Math.atan2(16.0, 28.0);
        float len = (float) Math.sqrt(28.0 * 28.0 + 16.0 * 16.0) + 1.0F;
        for (int side = -1; side <= 1; side += 2) {
            // (in strips across the ridge: each box must fit its patch of the texture)
            CubeListBuilder deck = CubeListBuilder.create();
            for (int strip = 0; strip < 4; strip++) {
                box(deck, DECK, strip * 8, 0, side > 0 ? 0 : -len, 0.0F, -29.0F + strip * 14.5F, len, 1.6F, 14.5F);
            }
            for (int row = 0; row < 4; row++) {
                float x0 = row * 8.0F + 1.0F;
                for (int half = 0; half < 2; half++) {
                    box(deck, DECK, row * 12 + half * 6, 6, side > 0 ? x0 : -x0 - 8.6F, -0.9F, -29.4F + half * 29.4F, 8.6F, 0.9F, 29.4F);
                }
            }
            k.part(deck, PartPose.offsetAndRotation(0.0F, -47.0F, 0.0F, 0, 0, side * slope));
        }
        // the ridge cap, and plank gables at both ends
        k.box(DARKWOOD, 0, 0, -2.0F, -48.6F, -29.6F, 4.0F, 2.2F, 20.0F);
        k.box(DARKWOOD, 4, 4, -2.0F, -48.6F, -9.6F, 4.0F, 2.2F, 19.2F);
        k.box(DARKWOOD, 8, 8, -2.0F, -48.6F, 9.6F, 4.0F, 2.2F, 20.0F);
        for (int end = -1; end <= 1; end += 2) {
            float z = end * 27.5F - 0.5F;
            k.box(WOOD, 0, 0, -6.0F, -45.0F, z, 12.0F, 3.0F, 1.0F);
            k.box(WOOD, 4, 4, -12.0F, -42.0F, z, 24.0F, 3.0F, 1.0F);
            k.box(WOOD, 8, 8, -18.0F, -39.0F, z, 18.0F, 3.0F, 1.0F);
            k.box(WOOD, 12, 8, 0.0F, -39.0F, z, 18.0F, 3.0F, 1.0F);
        }
        // a little lantern hung under the ridge
        k.line(BLACK, 0, -46.0F, 0, 0, -42.0F, 0, 0.4F);
        k.box(BLACK, 0, 0, -1.8F, -42.2F, -1.8F, 3.6F, 0.6F, 3.6F);
        k.box(WARM, 0, 0, -1.4F, -41.6F, -1.4F, 2.8F, 3.2F, 2.8F);
        k.box(BLACK, 0, 0, -1.8F, -38.4F, -1.8F, 3.6F, 0.6F, 3.6F);
        // flowers and tufts round its foot
        float[][] tufts = {{0.9F, 23.5F}, {2.5F, 24.0F}, {4.1F, 23.2F}, {5.6F, 24.2F}};
        for (float[] t : tufts) {
            CubeListBuilder tuft = CubeListBuilder.create();
            box(tuft, VINE, 0, 0, t[1], -2.5F, -1.5F, 2.0F, 2.5F, 3.0F);
            k.part(tuft, PartPose.offsetAndRotation(0, 0, 0, 0, t[0], 0));
        }
        root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // a 4 x 4 tile of the inside (from wx, wz) that some of the water or floor shows in
    static boolean inPool(float wx, float wz) {
        float cx = wx + 2.0F, cz = wz + 2.0F;
        return cx * cx + cz * cz <= (RING_IN + 1.0F) * (RING_IN + 1.0F);
    }

    // ── the water, drawn see-through (WishingWellRenderer) ──
    // A hair-thin sheet of tiles, each textured from where it lies in one picture of the whole pool (64..112, 180..220
    // in the texture: ripples round the middle, darker near the stones), so it reads as one surface with no seams.
    public static final ModelLayerLocation WATER_LAYER = DecorModels.layer("wishing_well_water");

    public static class Water extends Model<Float> {
        private final ModelPart surface;

        public Water(ModelPart root) {
            super(root, RenderTypes::entityTranslucentCull);
            this.surface = root.getChild("surface");
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            // it breathes a little
            this.surface.y = 0.12F * Mth.sin(time * 0.05F);
        }
    }

    public static LayerDefinition createWaterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder tiles = CubeListBuilder.create();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                float wx = -16.0F + i * 4.0F, wz = -16.0F + j * 4.0F;
                if (!inPool(wx, wz)) continue;
                // a box's top face is (u0 + depth, v0) on: so this tile shows the picture at (68 + wx + 16, 180 + wz + 16)
                tiles.texOffs(64 + i * 4, 180 + j * 4).addBox(wx, -9.0F, wz, 4.0F, 0.05F, 4.0F);
            }
        }
        mesh.getRoot().addOrReplaceChild("surface", tiles, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}

package com.alexh.capitalism.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import static com.alexh.capitalism.client.BloodDecorModels.Anim;
import static com.alexh.capitalism.client.BloodDecorModels.Base;
import static com.alexh.capitalism.client.BloodDecorModels.K;
import static com.alexh.capitalism.client.BoatKit.*;

// The Blood Merchant's Halloween decorations (decor/BloodDecorKind: GRINNING_JACK_O_LANTERN, HAUNTED_TOMBSTONE,
// POSSESSED_SCARECROW; Decorations.md > Blood Moon decorations > Halloween), drawn by client/BloodDecorRenderer like
// the Blood Moon ones: model pixels, y DOWN, y = 0 the ground, the front +x; textures painted by tools/BloodDecorTex.cs.
// Anim: x = the scarecrow's head turn (radians toward the nearest player, client/BloodDecorExtras), z = 1 at night.
public final class SpookyDecorModels {

    private SpookyDecorModels() {
    }

    // ═════════════ the Grinning Jack-o'-Lantern ═════════════
    // A big ribbed pumpkin, a curly stem and a leaf on top, a wide carved face (slanted eyes, a nose, a toothy grin)
    // glowing from the candle inside; the glow flickers.
    public static class JackOLantern extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("grinning_jack_o_lantern");
        private final ModelPart face;

        public JackOLantern(ModelPart root) {
            super(root);
            this.face = this.decor.getChild("face");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            // the face holds still (scaling it to "flicker" made the eyes and grin jitter); the candlelight's life comes
            // from the smoke and flame particles over it (BloodDecorBlock)
            this.face.yScale = 1.0F;
            this.face.zScale = 1.0F;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            DecorModels.pumpkin(k.k, STEEL, 0.0F, 0.0F, 0.0F, 6.0F, 10.5F, false);
            // a curly stem and a leaf
            k.box(DARKWOOD, -0.8, -12.8, -0.8, 1.6, 2.6, 1.6);
            k.line(DARKWOOD, 0, -12.6, 0, 1.6, -13.8, 0.8, 0.9);
            k.line(DARKWOOD, 1.6, -13.8, 0.8, 2.6, -13.2, 1.8, 0.6);
            k.box(VINE, -3.6, -11.25, 0.6, 3.2, 0.4, 2.4);
            k.line(VINE, -2.0, -11.4, 1.6, -4.6, -11.2, 3.4, 0.35);
            k.done();
            // the carved face on the front (+x), glowing
            CubeListBuilder face = CubeListBuilder.create();
            for (int side = -1; side <= 1; side += 2) {
                // slanted triangle eyes: three steps, wider at the bottom
                BoatKit.box(face, FIRE, 0, 0, 0.0F, -7.8F, side * 2.6F - 0.5F, 0.2F, 0.8F, 1.0F);
                BoatKit.box(face, FIRE, 0, 0, 0.0F, -7.0F, side * 2.6F - 1.1F, 0.2F, 0.8F, 2.2F);
                BoatKit.box(face, FIRE, 0, 0, 0.0F, -6.2F, side * 2.6F - 1.7F, 0.2F, 0.8F, 3.4F);
            }
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -5.2F, -0.6F, 0.2F, 1.0F, 1.2F);                 // the nose
            // the grin: a wide curved mouth, two teeth hanging down, one poking up
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -3.4F, -3.6F, 0.2F, 1.0F, 7.2F);
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -4.0F, -4.4F, 0.2F, 1.0F, 1.2F);
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -4.0F, 3.2F, 0.2F, 1.0F, 1.2F);
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -2.6F, -2.6F, 0.2F, 0.8F, 1.6F);
            BoatKit.box(face, FIRE, 0, 0, 0.0F, -2.6F, 1.0F, 0.2F, 0.8F, 1.6F);
            root.addOrReplaceChild("face", face, PartPose.offset(6.02F, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Haunted Tombstone ═════════════
    // An old grey headstone with a rounded top on a plinth, moss creeping up it, a crack, a carved skull over the
    // epitaph (drawn by the renderer: whatever a Name Tag carved). At night a ghostly wisp rises and bobs over it.
    public static class Tombstone extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("haunted_tombstone");
        private final ModelPart wisp;

        public Tombstone(ModelPart root) {
            super(root);
            this.wisp = this.decor.getChild("wisp");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            this.wisp.visible = s.z() > 0.5F;
            this.wisp.y = -19.0F + 1.2F * Mth.sin(t * 0.06F);
            this.wisp.z = 1.5F * Mth.sin(t * 0.023F);
            this.wisp.yRot = t * 0.04F;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            k.box(GREY, -3.4, -1.6, -7.2, 6.8, 1.6, 14.4);                        // the plinth
            k.box(WHITE, -2.4, -12.0, -6.2, 4.8, 10.4, 12.4);                     // the stone
            k.box(WHITE, 8, 0, -2.4, -13.4, -5.2, 4.8, 1.4, 10.4);                // its rounded top, in steps
            k.box(WHITE, 16, 0, -2.4, -14.4, -3.6, 4.8, 1.0, 7.2);
            k.box(WHITE, 24, 0, -2.4, -14.9, -1.8, 4.8, 0.5, 3.6);
            k.box(MOSS, -2.5, -4.6, -6.3, 5.0, 3.0, 2.6);                         // moss creeping up a corner
            k.box(MOSS, 10, 0, -2.5, -2.6, 2.2, 5.0, 1.0, 4.1);
            // the crack, and the skull carved over the epitaph (both on the front, +x)
            k.line(BLACK, 2.42, -13.6, 1.6, 2.42, -11.2, 2.6, 0.18);
            k.line(BLACK, 2.42, -11.2, 2.6, 2.42, -9.6, 1.8, 0.18);
            k.box(BONE, 2.38, -12.0, -1.3, 0.12, 2.0, 2.6);                       // the skull
            k.box(BONE, 2.38, -10.0, -0.8, 0.12, 0.8, 1.6);
            k.box(BLACK, 2.46, -11.4, -0.9, 0.08, 0.6, 0.6).box(BLACK, 2.46, -11.4, 0.3, 0.08, 0.6, 0.6);
            k.box(BLACK, 2.46, -2.6, -5.0, 0.08, 0.2, 10.0);                      // a line under the epitaph
            k.done();
            // the wisp: a glowing ghostly flame, its tail trailing down
            CubeListBuilder wisp = CubeListBuilder.create();
            BoatKit.box(wisp, GHOST, 0, 0, -1.4F, -1.4F, -1.4F, 2.8F, 2.8F, 2.8F);
            BoatKit.box(wisp, GHOST, 0, 0, -1.0F, 1.4F, -1.0F, 2.0F, 1.6F, 2.0F);
            BoatKit.box(wisp, GHOST, 0, 0, -0.6F, 3.0F, -0.6F, 1.2F, 1.4F, 1.2F);
            BoatKit.box(wisp, GHOST, 0, 0, -0.3F, 4.4F, -0.3F, 0.6F, 1.0F, 0.6F);
            root.addOrReplaceChild("wisp", wisp, PartPose.offset(0.0F, -19.0F, 0.0F));
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Possessed Scarecrow ═════════════
    // A tattered scarecrow on a post, arms out along a crossbar, a ragged black coat with straw bursting from the cuffs
    // and collar, a burlap sack for a head with a stitched grin and two glowing eyes, a floppy pointed hat. Its head
    // turns to follow whoever is nearest. A crow perches on its arm.
    public static class Scarecrow extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("possessed_scarecrow");
        private final ModelPart head;
        private final ModelPart crowHead;

        public Scarecrow(ModelPart root) {
            super(root);
            this.head = this.decor.getChild("head");
            this.crowHead = this.decor.getChild("crow").getChild("crow_head");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            // the head turns to the nearest player (x: radians toward the side it faces), with a little twitch
            this.head.yRot = -s.x() + 0.03F * Mth.sin(t * 0.7F) * (Mth.sin(t * 0.05F) > 0.8F ? 1 : 0);
            this.head.zRot = 0.12F + 0.03F * Mth.sin(t * 0.03F);
            this.crowHead.yRot = 0.8F * Mth.sin(t * 0.05F) * (Mth.sin(t * 0.013F) > 0 ? 1 : 0.2F);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            k.box(OLDWOOD, -1.2, -24.0, -1.2, 2.4, 24.0, 2.4);                    // the post
            k.box(OLDWOOD, 8, 0, -1.0, -26.5, -12.0, 2.0, 2.0, 24.0);             // the crossbar
            // the coat, its tails in tatters
            k.box(CANVAS, -3.4, -30.0, -4.8, 6.8, 13.0, 9.6);
            double[] tails = {5.0, 3.0, 6.0, 4.0, 5.5};
            for (int i = 0; i < tails.length; i++) k.box(CANVAS, 2 + i * 3, 4, -3.2 + (i % 2) * 0.2, -17.0, -4.8 + i * 1.9, 6.4, tails[i], 1.8);
            k.box(LEATHER, 0, 0, -3.6, -21.5, -5.0, 7.2, 1.0, 10.0);               // a burlap belt
            k.box(LEATHER, 30, 0, 3.45, -26.0, 1.2, 0.1, 2.4, 2.4);               // a burlap patch
            // sleeves along the crossbar, straw out of the cuffs and the collar
            for (int s = -1; s <= 1; s += 2) {
                k.box(CANVAS, 8, 0, -2.0, -28.0, s > 0 ? 4.8 : -12.0, 4.0, 3.8, 7.2);
                for (int i = 0; i < 5; i++) {
                    double a = i * 1.25;
                    k.line(BAMBOO, 0, -26.2, s * 12.0, Math.cos(a) * 1.6, -26.2 + Math.sin(a) * 1.8 + 0.8, s * (14.6 + (i % 3) * 0.5), 0.45);
                }
            }
            for (int i = 0; i < 5; i++) k.line(BAMBOO, 0, -30.0, -2.4 + i * 1.2, (i - 2) * 0.5, -32.0, -2.8 + i * 1.4, 0.45);
            k.done();
            // the sack head on a short neck of straw, turning; stitched grin, glowing eyes, a floppy hat
            CubeListBuilder head = CubeListBuilder.create();
            BoatKit.box(head, LEATHER, 0, 0, -3.6F, -8.2F, -3.6F, 7.2F, 8.2F, 7.2F);    // the sack (burlap: LEATHER)
            BoatKit.box(head, LEATHER, 0, 16, -2.8F, 0.0F, -2.6F, 5.6F, 1.0F, 5.2F);    // tied off at the neck
            BoatKit.box(head, BLACK, 0, 0, 3.62F, -2.8F, -2.6F, 0.1F, 0.4F, 5.2F);      // the stitched grin
            for (int i = 0; i < 6; i++) BoatKit.box(head, BLACK, 0, 0, 3.64F, -3.3F, -2.4F + i * 0.95F, 0.1F, 1.4F, 0.25F);
            BoatKit.box(head, FIRE, 0, 0, 3.64F, -6.0F, -2.6F, 0.12F, 1.4F, 1.6F);      // the glowing eyes
            BoatKit.box(head, FIRE, 0, 0, 3.64F, -6.0F, 1.0F, 0.12F, 1.4F, 1.6F);
            BoatKit.box(head, CANVAS, 0, 0, -5.8F, -8.9F, -5.8F, 11.6F, 0.7F, 11.6F);   // the hat's brim
            BoatKit.box(head, CANVAS, 4, 4, -3.2F, -11.6F, -3.2F, 6.4F, 2.8F, 6.4F);    // its crown, flopping over
            BoatKit.box(head, CANVAS, 8, 8, -2.4F, -13.6F, -1.6F, 4.0F, 2.0F, 4.0F);
            BoatKit.box(head, CANVAS, 12, 12, -3.4F, -14.6F, -0.2F, 2.6F, 1.2F, 2.6F);
            root.addOrReplaceChild("head", head, PartPose.offset(0.0F, -31.0F, 0.0F));
            // the crow on its arm (its head looks about)
            CubeListBuilder crow = CubeListBuilder.create();
            BoatKit.box(crow, BLACK, 0, 0, -1.8F, -2.6F, -1.1F, 3.6F, 2.6F, 2.2F);
            BoatKit.box(crow, BLACK, 0, 4, -4.0F, -2.1F, -0.8F, 2.4F, 0.7F, 1.6F);
            BoatKit.box(crow, BLACK, 4, 4, -1.2F, -2.3F, -1.25F, 2.6F, 1.6F, 0.2F);
            BoatKit.box(crow, GOLD, 0, 0, -0.2F, 0.0F, -0.6F, 0.4F, 1.0F, 0.3F);
            BoatKit.box(crow, GOLD, 0, 0, -0.2F, 0.0F, 0.3F, 0.4F, 1.0F, 0.3F);
            PartDefinition crowPart = root.addOrReplaceChild("crow", crow, PartPose.offset(0.0F, -26.6F, 9.5F));
            CubeListBuilder crowHead = CubeListBuilder.create();
            BoatKit.box(crowHead, BLACK, 0, 0, -1.0F, -1.8F, -0.9F, 2.0F, 1.8F, 1.8F);
            BoatKit.box(crowHead, GOLD, 0, 0, 1.0F, -1.2F, -0.3F, 1.4F, 0.6F, 0.6F);
            BoatKit.box(crowHead, FIRE, 0, 0, 0.4F, -1.5F, 0.9F, 0.45F, 0.45F, 0.06F);
            BoatKit.box(crowHead, FIRE, 0, 0, 0.4F, -1.5F, -0.96F, 0.45F, 0.45F, 0.06F);
            crowPart.addOrReplaceChild("crow_head", crowHead, PartPose.offset(1.2F, -2.6F, 0.0F));
            return LayerDefinition.create(mesh, 256, 256);
        }
    }
}

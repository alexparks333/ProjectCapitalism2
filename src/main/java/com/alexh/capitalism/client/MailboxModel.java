package com.alexh.capitalism.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import static com.alexh.capitalism.client.BoatKit.*;

// ═════════════ the Mailbox (mail/MailboxBlock) ═════════════
// A country mailbox: a long box of painted, ribbed metal with a round top (thin strips turned round its length) on a
// cross board on a wooden post, a door at the front with a brass handle and a hinge along its bottom, a brass name
// plate on its left side, and on its right the little red flag, which stands up while there's mail in it.
// In model pixels: y down from the ground, the door toward +x (MailboxRenderer turns it the way it was placed).
// The animation value is the flag: 0 down (along the box, pointing at the door), 1 up.
public class MailboxModel extends DecorModels.Base {

    public static final ModelLayerLocation LAYER = DecorModels.layer("mailbox");
    private final ModelPart flag;

    public MailboxModel(ModelPart root) {
        super(root);
        this.flag = this.decor.getChild("flag");
    }

    @Override
    public void setupAnim(Float up) {
        super.setupAnim(up);
        this.flag.zRot = (1.0F - up) * Mth.HALF_PI;
    }

    static final float BOTTOM = -13.0F;   // the box's floor
    static final float SPRING = -18.0F;   // where its round top starts
    static final float R = 3.5F;          // the round top's radius (half the box's width)
    static final float HALF_LEN = 6.0F;

    // The Town Hall Mailbox (white and gold, textures/entity/town_mailbox.png): the same box on a grander stand: a
    // stepped marble plinth, a fluted post with gold bands and a capital, gold scroll brackets under the box, gold
    // bands round both ends, a gold ridge along the top with a finial, a bigger gold plate.
    public static final ModelLayerLocation TOWN_LAYER = DecorModels.layer("town_mailbox");

    public static LayerDefinition createLayer() {
        return build(false);
    }

    public static LayerDefinition createTownLayer() {
        return build(true);
    }

    private static LayerDefinition build(boolean grand) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        BoatKit k = new BoatKit(root);
        if (grand) {
            // the plinth (two steps), the fluted post with gold bands, its capital, the cross board
            k.box(DARKWOOD, 0, 0, -3.5F, -1.0F, -3.5F, 7.0F, 1.0F, 7.0F);
            k.box(DARKWOOD, 0, 10, -2.6F, -2.0F, -2.6F, 5.2F, 1.0F, 5.2F);
            k.box(WOOD, 0, 0, -1.6F, -11.4F, -1.6F, 3.2F, 9.4F, 3.2F);
            k.box(GOLD, 0, 0, -1.8F, -3.0F, -1.8F, 3.6F, 0.7F, 3.6F);
            k.box(GOLD, 0, 4, -1.8F, -10.6F, -1.8F, 3.6F, 0.7F, 3.6F);
            k.box(DARKWOOD, 0, 18, -2.4F, -12.2F, -2.4F, 4.8F, 0.8F, 4.8F);
            k.box(DARKWOOD, 20, 0, -5.5F, -13.0F, -2.0F, 11.0F, 0.8F, 4.0F);
            // gold scroll brackets from the post out under the box, front and back
            for (int end = -1; end <= 1; end += 2) {
                k.line(GOLD, end * 1.6F, -8.0F, 0.0F, end * 3.6F, -11.0F, 0.0F, 0.7F);
                k.line(GOLD, end * 3.6F, -11.0F, 0.0F, end * 5.2F, -12.9F, 0.0F, 0.7F);
            }
        } else {
            // the post on its foot, and the cross board the box sits on
            k.box(DARKWOOD, 0, 0, -2.5F, -1.0F, -2.5F, 5.0F, 1.0F, 5.0F);
            k.box(WOOD, 0, 0, -1.5F, -12.0F, -1.5F, 3.0F, 11.0F, 3.0F);
            k.box(DARKWOOD, 0, 8, -5.5F, -13.0F, -2.0F, 11.0F, 1.0F, 4.0F);
            k.line(DARKWOOD, 0.0F, -6.0F, 1.2F, 0.0F, -12.2F, 1.8F, 0.9F);
        }
        if (grand) {
            // gold bands round both ends of the box (rows like the ends', a little bigger), the ridge, the finial
            for (int end = -1; end <= 1; end += 2) {
                float x = end > 0 ? HALF_LEN - 1.2F : -HALF_LEN + 0.4F;
                k.box(GOLD, 0, 0, x, SPRING, -R - 0.15F, 0.8F, -SPRING + BOTTOM - 0.45F, 2 * R + 0.3F);
                int rows = 5;
                float rowH = (R + 0.15F) / rows;
                for (int i = 0; i < rows; i++) {
                    float mid = (i + 0.5F) * rowH;
                    float half = (float) Math.sqrt((R + 0.15F) * (R + 0.15F) - mid * mid);
                    k.box(GOLD, 0, 0, x, SPRING - (i + 1) * rowH, -half, 0.8F, rowH, 2 * half);
                }
            }
            k.box(GOLD, 0, 0, -HALF_LEN + 1.0F, SPRING - R - 0.5F, -0.5F, 2 * HALF_LEN - 2.0F, 0.6F, 1.0F);
            k.box(GOLD, 0, 0, -0.5F, SPRING - R - 1.6F, -0.5F, 1.0F, 1.2F, 1.0F);
            k.box(GOLD, 0, 0, -1.0F, SPRING - R - 3.4F, -0.6F, 2.0F, 1.8F, 1.2F);
            k.box(GOLD, 0, 0, -0.6F, SPRING - R - 3.8F, -1.0F, 1.2F, 2.6F, 2.0F);
            k.box(GOLD, 0, 0, -0.3F, SPRING - R - 4.6F, -0.3F, 0.6F, 0.9F, 0.6F);
            k.box(GOLD, 8, 4, -3.4F, SPRING + 0.3F, -R - 0.35F, 6.8F, 2.6F, 0.35F);
        }
        // the box: floor, straight sides, the round top in strips
        k.box(STEEL, 0, 0, -HALF_LEN, BOTTOM - 0.6F, -R, 2 * HALF_LEN, 0.6F, 2 * R);
        k.box(STEEL, 0, 10, -HALF_LEN, SPRING, -R, 2 * HALF_LEN, SPRING * -1 + BOTTOM - 0.6F, 0.6F);
        k.box(STEEL, 20, 10, -HALF_LEN, SPRING, R - 0.6F, 2 * HALF_LEN, SPRING * -1 + BOTTOM - 0.6F, 0.6F);
        int strips = 8;
        float stripW = 2 * R * (float) Math.tan(Math.PI / 2 / strips) + 0.12F;
        for (int s = 0; s < strips; s++) {
            float a = -Mth.HALF_PI + (s + 0.5F) * Mth.PI / strips;
            CubeListBuilder strip = CubeListBuilder.create();
            box(strip, STEEL, (s * 3) % 30, 18, -HALF_LEN, -R, -stripW / 2, 2 * HALF_LEN, 0.6F, stripW);
            k.part(strip, PartPose.offsetAndRotation(0.0F, SPRING, 0.0F, a, 0, 0));
        }
        // the back, and the door at the front: the box's end shape (square below, rounded above)
        end(k, -HALF_LEN - 0.4F, 0.5F, 0);
        end(k, HALF_LEN - 0.05F, 0.6F, 14);
        // the door's hinge along its bottom, its handle near the top
        k.box(grand ? GOLD : BLACK, 0, 0, HALF_LEN + 0.1F, BOTTOM - 0.9F, -R + 0.2F, 0.7F, 0.6F, 2 * R - 0.4F);
        k.box(GOLD, 0, 0, HALF_LEN + 0.5F, SPRING - 2.0F, -0.3F, 0.6F, 0.6F, 0.6F);
        k.box(GOLD, 4, 0, HALF_LEN + 1.0F, SPRING - 2.3F, -1.0F, 0.6F, 1.2F, 2.0F);
        // the name plate on the left side
        if (!grand) k.box(GOLD, 8, 0, -2.4F, SPRING + 0.6F, -R - 0.3F, 4.8F, 1.8F, 0.35F);
        // the flag on the right side: a red arm on a pivot, a red flag at its tip
        k.box(grand ? GOLD : BLACK, 4, 4, -3.9F, SPRING + 1.0F, R - 0.1F, 0.8F, 0.8F, 0.6F);
        PartDefinition flag = root.addOrReplaceChild("flag", CubeListBuilder.create(), PartPose.offset(-3.5F, SPRING + 1.4F, R + 0.45F));
        BoatKit f = new BoatKit(flag);
        f.box(RED, 0, 0, -0.35F, -7.0F, 0.0F, 0.7F, 7.4F, 0.4F);
        f.box(RED, 6, 0, -0.35F, -7.2F, 0.0F, 3.6F, 2.6F, 0.4F);
        flag.addOrReplaceChild("statics", f.statics, PartPose.ZERO);
        root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    // one end of the box at x (this thick): a square below the spring line, then rows narrowing round the top
    private static void end(BoatKit k, float x, float thick, int du) {
        k.box(STEEL, du, 0, x, SPRING, -R, thick, -SPRING + BOTTOM - 0.6F + 0.0F, 2 * R);
        int rows = 5;
        float rowH = R / rows;
        for (int i = 0; i < rows; i++) {
            float mid = (i + 0.5F) * rowH;
            float half = (float) Math.sqrt(R * R - mid * mid);
            k.box(STEEL, du, 12, x, SPRING - (i + 1) * rowH, -half, thick, rowH, 2 * half);
        }
    }
}

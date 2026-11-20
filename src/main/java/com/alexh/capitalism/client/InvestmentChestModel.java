package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// The Investment Chest (economy/InvestmentChestBlock): the bank's vault chest, plated in polished gold with riveted dark
// iron straps and corner caps, a big silver combination dial on its front (it spins while the chest is open) and a coin
// slot in the lid. Behind it two dark posts carry a crossbar, and from it, on two chains, hangs a crooked cream sign
// (the renderer letters it "INVESTMENT / CHEST"); off its corner dangles a little tag ("(totally safe!)"). The sign sways.
// Model pixels: the front +x, y down (the ground at 0), the width along z; the sign reaches up to y -33 (two blocks).
// Texture: textures/entity/investment_chest.png (128 x 128, tools/make_investment_chest.ps1, which knows these boxes).
public class InvestmentChestModel extends Model<InvestmentChestModel.Anim> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "investment_chest"), "main");

    // how open (0 shut, 1 open), and the time in ticks (the sign's sway, the dial's spin)
    public record Anim(float open, float time) {
    }

    // (the board hangs IN FRONT of the posts, from a deep crossbar: it's wider than the gap between them, so swaying in
    // the posts' own plane would push its ends through them)
    // where the sign hangs (the renderer letters it in the same place)
    public static final float SIGN_X = -7.5F, SIGN_Y = -32.0F;
    public static final float BOARD_FRONT = 2.0F, BOARD_MID_Y = 4.0F;
    public static final float TAG_X = 1.3F, TAG_Y = 6.2F, TAG_Z = 8.2F, TAG_ROLL = -0.38F;
    public static final float TAG_FRONT = 0.6F, TAG_MID_Y = 1.7F, TAG_MID_Z = 4.0F;

    private final ModelPart lid;
    private final ModelPart dial;
    private final ModelPart sign;

    public InvestmentChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.lid = root.getChild("lid");
        this.dial = root.getChild("dial");
        this.sign = root.getChild("sign");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder base = CubeListBuilder.create()
            .texOffs(0, 0).addBox(-7.0F, -10.0F, -7.0F, 14, 10, 14)
            // the iron straps round it
            .texOffs(60, 0).addBox(-7.2F, -10.1F, -5.1F, 14.4F, 10.2F, 1.2F)
            .texOffs(60, 0).addBox(-7.2F, -10.1F, 3.9F, 14.4F, 10.2F, 1.2F);
        // iron caps on its bottom corners
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) base.texOffs(96, 0).addBox(sx * 7.0F - 1.1F, -2.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
        }
        root.addOrReplaceChild("base", base, PartPose.ZERO);
        // the two posts behind it and the crossbar on top
        root.addOrReplaceChild("posts", CubeListBuilder.create()
            .texOffs(0, 44).addBox(-8.0F, -33.0F, -8.0F, 1, 33, 1)
            .texOffs(0, 44).addBox(-8.0F, -33.0F, 7.0F, 1, 33, 1)
            .texOffs(8, 44).addBox(-8.2F, -33.6F, -8.6F, 3.0F, 1.4F, 17.2F), PartPose.ZERO);

        // the combination dial on the front (spins on x)
        CubeListBuilder dial = CubeListBuilder.create()
            .texOffs(96, 8).addBox(0.0F, -2.0F, -2.0F, 0.6F, 4, 4)
            .texOffs(96, 26).addBox(0.6F, -0.5F, -0.5F, 0.6F, 1, 1);    // its knob
        root.addOrReplaceChild("dial", dial, PartPose.offset(7.0F, -5.5F, 0.0F));

        // the lid, hinged on its back edge, a coin slot in its top
        CubeListBuilder lid = CubeListBuilder.create()
            .texOffs(0, 24).addBox(0.0F, -4.0F, -7.0F, 14, 4, 14)
            .texOffs(60, 24).addBox(-0.2F, -4.1F, -5.1F, 14.4F, 4.2F, 1.2F)
            .texOffs(60, 24).addBox(-0.2F, -4.1F, 3.9F, 14.4F, 4.2F, 1.2F)
            .texOffs(96, 16).addBox(6.5F, -4.15F, -2.5F, 1, 0.2F, 5)        // the coin slot
            .texOffs(96, 32).addBox(14.05F, -1.6F, -1.5F, 0.6F, 3.2F, 3);   // the latch
        for (int sz = -1; sz <= 1; sz += 2) {
            lid.texOffs(96, 0).addBox(-1.1F, -5.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
            lid.texOffs(96, 0).addBox(12.9F, -5.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
        }
        root.addOrReplaceChild("lid", lid, PartPose.offset(-7.0F, -10.0F, 0.0F));

        // the sign: hangs from the crossbar on two chains, a little crooked
        CubeListBuilder board = CubeListBuilder.create()
            .texOffs(50, 44).addBox(1.4F, 0.0F, -6.2F, 0.4F, 1.2F, 0.4F)
            .texOffs(50, 44).addBox(1.4F, 0.0F, 5.8F, 0.4F, 1.2F, 0.4F)
            .texOffs(0, 80).addBox(1.2F, 1.0F, -9.0F, 0.8F, 6, 18);
        PartDefinition sign = root.addOrReplaceChild("sign", board, PartPose.offset(SIGN_X, SIGN_Y, 0.0F));
        // the little tag dangling off its corner by one nail
        sign.addOrReplaceChild("tag", CubeListBuilder.create().texOffs(40, 80).addBox(0.0F, 0.0F, 0.0F, 0.6F, 3.4F, 8.0F),
            PartPose.offsetAndRotation(TAG_X, TAG_Y, TAG_Z, TAG_ROLL, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    // the sign's tilt (about x: crooked, swaying a little) and its swing (about z: in and out)
    public static float signRoll(float time) {
        return 0.06F + 0.03F * Mth.sin(time * 0.045F);
    }

    public static float signSwing(float time) {
        return 0.05F * Mth.sin(time * 0.06F + 1.0F);
    }

    @Override
    public void setupAnim(Anim anim) {
        super.setupAnim(anim);
        float o = 1.0F - anim.open();
        o = 1.0F - o * o * o;
        this.lid.zRot = -o * 1.45F;
        // the dial spins while it's open ("cracking the safe"), and settles when it shuts
        this.dial.xRot = anim.open() * anim.time() * 0.35F;
        this.sign.xRot = signRoll(anim.time());
        this.sign.zRot = signSwing(anim.time());
    }
}

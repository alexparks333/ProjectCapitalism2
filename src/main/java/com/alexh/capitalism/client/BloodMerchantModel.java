package com.alexh.capitalism.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

// The Blood Merchant (town/BloodMerchant): a tall, gaunt vampire standing very straight. A deep black hood over a pale,
// hollow-cheeked face with burning red eyes and two fangs over his lip; a high, stiff collar flaring up behind his head,
// black outside and crimson inside; a long black cape lined with crimson falling from his shoulders to his heels; a
// dark burgundy doublet with a lace jabot and a blood-red gem at his throat; thin arms brought forward, his long pale
// hands steepled in front of him, black claw-like nails. He barely moves: a
// slow breath, the cape stirring, the head following you. Texture: textures/entity/blood_merchant.png (128 x 64,
// tools/make_blood_merchant.ps1, which knows these boxes), the eyes and the gem in blood_merchant_glow.png.
public class BloodMerchantModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart cape;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public BloodMerchantModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.cape = this.body.getChild("cape");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
    }

    // Model units are pixels, y down, the ground at y = 24. He's tall: legs 11, body 13, head 8 (about 2 blocks).
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, 0.0F, -1.5F, 3, 11, 3),
            PartPose.offset(1.7F, 13.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(14, 46).addBox(-1.5F, 0.0F, -1.5F, 3, 11, 3),
            PartPose.offset(-1.7F, 13.0F, 0.0F));
        // the doublet (8 x 13 x 4) and a skirt of the coat to the knees
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.0F, -13.0F, -2.0F, 8, 13, 4)
                .texOffs(28, 46).addBox(-4.5F, -1.0F, -2.5F, 9, 6, 5),
            PartPose.offset(0.0F, 13.0F, 0.0F));
        // the head, and round it the deep hood (open at the front: its front face is a frame) with a peak falling back
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.6F, -8.8F, -4.9F, 9, 10, 9, new net.minecraft.client.model.geom.builders.CubeDeformation(0.05F)),
            PartPose.offset(0.0F, -13.0F, 0.0F));
        // the two fangs over his lip
        head.addOrReplaceChild("fangs", CubeListBuilder.create()
                .texOffs(88, 12).addBox(-1.6F, -2.2F, -4.25F, 1, 1, 1)
                .texOffs(88, 12).addBox(0.6F, -2.2F, -4.25F, 1, 1, 1),
            PartPose.ZERO);
        // the high collar: two stiff wings flaring up and out behind the head
        body.addOrReplaceChild("collar_left", CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, -7.0F, 0.0F, 6, 7, 1),
            PartPose.offsetAndRotation(0.5F, -12.0F, 1.6F, -0.25F, -0.55F, 0.35F));
        body.addOrReplaceChild("collar_right", CubeListBuilder.create().texOffs(106, 0).addBox(-6.0F, -7.0F, 0.0F, 6, 7, 1),
            PartPose.offsetAndRotation(-0.5F, -12.0F, 1.6F, -0.25F, 0.55F, -0.35F));
        // the cape: from the shoulders to the heels, a little wider than him, hanging just behind
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(26, 20).addBox(-6.0F, 0.0F, 0.0F, 12, 24, 1),
            PartPose.offsetAndRotation(0.0F, -13.0F, 2.2F, 0.06F, 0.0F, 0.0F));
        // the gem at his throat
        body.addOrReplaceChild("clasp", CubeListBuilder.create().texOffs(88, 8).addBox(-1.0F, -1.0F, -0.6F, 2, 2, 1),
            PartPose.offset(0.0F, -12.0F, -2.0F));
        // arms: brought forward, his long hands steepled in front of him
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 20).addBox(-1.0F, -1.0F, -1.5F, 3, 13, 3),
            PartPose.offsetAndRotation(4.5F, -12.0F, 0.0F, -0.95F, 0.45F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(70, 20).addBox(-2.0F, -1.0F, -1.5F, 3, 13, 3),
            PartPose.offsetAndRotation(-4.5F, -12.0F, 0.0F, -0.95F, -0.45F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // a slow breath, the cape stirring, his hands rising and falling a little
        this.body.y = 13.0F + Mth.sin(t * 0.05F) * 0.25F;
        this.cape.xRot = 0.06F + Mth.sin(t * 0.04F) * 0.03F + Math.max(0.0F, Mth.sin(t * 0.013F)) * 0.04F;
        this.rightArm.xRot = -0.95F + Mth.sin(t * 0.06F) * 0.04F;
        this.leftArm.xRot = -0.95F + Mth.sin(t * 0.06F) * 0.04F;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }
}

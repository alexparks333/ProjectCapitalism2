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

// The Apprentice (enchanter/Apprentice): a cute little iron figure (big head, small body, stubby arms and legs) in a
// tall violet wizard hat with a gold band and a floppy tip. Texture: textures/entity/apprentice.png
// (tools/make_apprentice_entity.ps1, which knows this UV layout). It bobs, sways its arms and its hat tip wobbles.
public class ApprenticeModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart head;
    private final ModelPart hatTip;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart body;

    public ApprenticeModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.hatTip = this.head.getChild("hat_brim").getChild("hat_1").getChild("hat_2");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
    }

    // Model units are pixels, y down, the ground at y = 24.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // legs: 2x3x2
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 24).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
            PartPose.offset(1.5F, 21.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 24).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
            PartPose.offset(-1.5F, 21.0F, 0.0F));
        // body: 6x5x4, pivot at the hips so it can bob
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -5.0F, -2.0F, 6, 5, 4),
            PartPose.offset(0.0F, 21.0F, 0.0F));
        // arms: 2x4x2 from the shoulders
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).addBox(0.0F, 0.0F, -1.0F, 2, 4, 2),
            PartPose.offset(3.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(-2.0F, 0.0F, -1.0F, 2, 4, 2),
            PartPose.offset(-3.0F, -5.0F, 0.0F));
        // the big head: 8x7x7
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -3.5F, 8, 7, 7),
            PartPose.offset(0.0F, -5.0F, 0.0F));
        // the hat: a wide brim, then tiers getting smaller, each tipped back a little more, and a floppy tip
        PartDefinition brim = head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -1.0F, -5.0F, 10, 1, 10),
            PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.08F, 0.0F, 0.05F));
        PartDefinition hat1 = brim.addOrReplaceChild("hat_1", CubeListBuilder.create().texOffs(40, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 3, 6),
            PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition hat2 = hat1.addOrReplaceChild("hat_2", CubeListBuilder.create().texOffs(40, 12).addBox(-2.0F, -3.0F, -2.0F, 4, 3, 4),
            PartPose.offsetAndRotation(0.0F, -3.0F, 0.5F, -0.25F, 0.0F, 0.0F));
        PartDefinition hat3 = hat2.addOrReplaceChild("hat_3", CubeListBuilder.create().texOffs(40, 22).addBox(-1.0F, -3.0F, -1.0F, 2, 3, 2),
            PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.45F, 0.0F, 0.0F));
        hat3.addOrReplaceChild("hat_tip", CubeListBuilder.create().texOffs(50, 22).addBox(-0.5F, -2.0F, -0.5F, 1, 2, 1),
            PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // a gentle bob, arms swaying out of step, the hat tip nodding along
        this.body.y = 21.0F + Mth.sin(t * 0.1F) * 0.35F;
        this.head.zRot = Mth.sin(t * 0.05F) * 0.06F;
        this.leftArm.zRot = -0.12F - Mth.sin(t * 0.1F) * 0.08F;
        this.rightArm.zRot = 0.12F + Mth.sin(t * 0.1F + 1.0F) * 0.08F;
        this.hatTip.xRot = -0.25F + Mth.sin(t * 0.12F) * 0.12F;
    }
}

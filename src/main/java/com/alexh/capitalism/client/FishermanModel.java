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

// The Fisherman (town/Fisherman): an old salt, a little stooped and broad. A bright yellow oilskin coat to his thighs,
// hanging open over dark green rubber waders that come up to his chest on braces, black buckles down its edges; a
// yellow sou'wester with a short brim tipped up in front and a broad one sloping down over his neck behind; a weathered,
// tanned face with bushy grey brows, a big bushy grey beard and moustache, a briar pipe in the corner of his mouth (its
// ember glows); yellow sleeves, tanned hands, dark green boots. He barely moves: a slow breath, his arms swaying a
// little, the head following you. Texture: textures/entity/fisherman.png (128 x 64, tools/make_fisherman.ps1, which
// knows these boxes), the pipe's ember in fisherman_glow.png.
public class FishermanModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public FishermanModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
    }

    // Model units are pixels, y down, the ground at y = 24. Legs 12, body 12, head 8 (2 blocks), the hat on top.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // the waders' legs, boots at the bottom
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
            PartPose.offset(2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 36).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
            PartPose.offset(-2.0F, 12.0F, 0.0F));
        // the body (the waders' bib and braces over a knitted jumper), round it the open oilskin coat and its skirt
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -12.0F, -2.0F, 8, 12, 4)
                .texOffs(24, 16).addBox(-4.5F, -12.2F, -2.5F, 9, 12, 5)
                .texOffs(52, 16).addBox(-5.0F, -1.0F, -3.0F, 10, 6, 6),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        // the head and his bushy beard (over his chin and down onto his chest)
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                .texOffs(108, 0).addBox(-4.0F, -3.0F, -4.6F, 8, 5, 2),
            PartPose.offset(0.0F, -12.0F, 0.0F));
        // the pipe in the corner of his mouth: a stem and the bowl at its end
        head.addOrReplaceChild("pipe", CubeListBuilder.create()
                .texOffs(108, 10).addBox(-0.5F, -0.5F, -3.0F, 1, 1, 3)
                .texOffs(118, 10).addBox(-1.0F, -3.0F, -4.4F, 2, 3, 2),
            PartPose.offsetAndRotation(-2.0F, -2.4F, -4.6F, 0.12F, 0.25F, 0.0F));
        // the sou'wester: its crown, a short brim tipped up in front, a broad brim sloping down behind
        head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -11.5F, -4.5F, 9, 4, 9),
            PartPose.ZERO);
        head.addOrReplaceChild("brim_front", CubeListBuilder.create().texOffs(70, 0).addBox(-5.0F, -0.5F, -3.0F, 10, 1, 3),
            PartPose.offsetAndRotation(0.0F, -8.0F, -4.4F, -0.3F, 0.0F, 0.0F));
        head.addOrReplaceChild("brim_back", CubeListBuilder.create().texOffs(70, 6).addBox(-5.5F, -0.5F, 0.0F, 11, 1, 8),
            PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, -0.42F, 0.0F, 0.0F));
        // arms: yellow sleeves hanging at his sides, a little forward
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(84, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(5.5F, -10.0F, 0.0F, -0.12F, 0.0F, -0.06F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(100, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(-5.5F, -10.0F, 0.0F, -0.12F, 0.0F, 0.06F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // a slow breath, his arms swaying a little
        this.body.y = 12.0F + Mth.sin(t * 0.05F) * 0.2F;
        this.rightArm.xRot = -0.12F + Mth.sin(t * 0.045F) * 0.05F;
        this.leftArm.xRot = -0.12F - Mth.sin(t * 0.045F) * 0.05F;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }
}

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

// The Bounty Clerk (town/BountyClerk): a neat, upright town official. A bottle-green frock coat with brass buttons and
// tails to his knees over a buff waistcoat, a white shirt and a cravat; grey trousers, polished black shoes; a lean
// clean-shaven face, neat side-parted chestnut hair, round gold-rimmed spectacles, a white quill tucked behind his
// right ear. His left hand holds a thick red ledger up in front of him; his right hangs at his side. Off to his right,
// stands a notice board on two posts, its cork pinned with bounty papers (part of the model; it doesn't turn with his
// head). He barely moves: a slow breath, the head following you. Texture: textures/entity/bounty_clerk.png (128 x 64,
// tools/make_bounty_clerk.ps1, which knows these boxes).
public class BountyClerkModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;

    public BountyClerkModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
    }

    // Model units are pixels, y down, the ground at y = 24. Legs 12, body 12, head 8.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // trousers and shoes
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
            PartPose.offset(1.95F, 12.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 36).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
            PartPose.offset(-1.95F, 12.0F, 0.0F));
        // the body (waistcoat, shirt, cravat), the frock coat over it, its tails behind down to his knees
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -12.0F, -2.0F, 8, 12, 4)
                .texOffs(24, 16).addBox(-4.4F, -12.2F, -2.4F, 9, 12, 5, new net.minecraft.client.model.geom.builders.CubeDeformation(-0.1F))
                .texOffs(52, 16).addBox(-4.5F, -0.5F, 0.6F, 9, 7, 2),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        // the head; his neat hair a little proud of it; the spectacles; the quill behind his right ear
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new net.minecraft.client.model.geom.builders.CubeDeformation(0.3F)),
            PartPose.offset(0.0F, -12.0F, 0.0F));
        // (flat pieces are one-sided, a hair apart: the entity render types don't cull back faces, so a flat box's two
        // faces would lie in one plane and flicker against each other)
        java.util.EnumSet<net.minecraft.core.Direction> north = java.util.EnumSet.of(net.minecraft.core.Direction.NORTH);
        head.addOrReplaceChild("spectacles", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.6F, -5.2F, -4.5F, 3, 2, 0, north)
                .texOffs(64, 3).addBox(0.6F, -5.2F, -4.5F, 3, 2, 0, north)
                .texOffs(72, 0).addBox(-0.6F, -4.7F, -4.55F, 1.2F, 0.4F, 0.2F),
            PartPose.ZERO);
        head.addOrReplaceChild("quill", CubeListBuilder.create()
                .texOffs(76, 0).addBox(-0.25F, -7.0F, -0.25F, 0.5F, 7, 0.5F)
                .texOffs(80, 0).addBox(0.03F, -7.5F, -1.5F, 0, 5, 3, java.util.EnumSet.of(net.minecraft.core.Direction.EAST))
                .texOffs(80, 0).addBox(-0.03F, -7.5F, -1.5F, 0, 5, 3, java.util.EnumSet.of(net.minecraft.core.Direction.WEST)),
            PartPose.offsetAndRotation(-4.6F, -3.0F, 0.5F, 0.35F, 0.0F, -0.3F));
        // the left arm reaching a little forward, the ledger held upright in front of his hand (clear of his body and
        // the arm: the arm slopes back towards his shoulder above it)
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(84, 16).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(5.0F, -10.0F, 0.0F, -0.6F, 0.0F, -0.08F));
        leftArm.addOrReplaceChild("ledger", CubeListBuilder.create().texOffs(64, 36).addBox(-3.0F, -6.5F, -2.2F, 6, 8, 2),
            PartPose.offsetAndRotation(1.0F, 9.0F, -2.2F, 0.6F, 0.0F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(100, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
            PartPose.offsetAndRotation(-5.0F, -10.0F, 0.0F, -0.05F, 0.0F, 0.06F));
        // the notice board well off to his right and a little behind him (nothing of his reaches it): cork in an oak
        // frame on two posts, a little shingled roof over it; the posts tuck up inside the board (no faces meeting)
        PartDefinition board = root.addOrReplaceChild("board", CubeListBuilder.create()
                .texOffs(32, 36).addBox(-7.0F, -27.0F, -0.5F, 14, 11, 1)
                .texOffs(32, 52).addBox(-8.0F, -27.9F, -1.4F, 16, 1, 3)
                .texOffs(104, 36).addBox(-6.5F, -16.6F, -0.35F, 1, 16.6F, 0.7F)
                .texOffs(108, 36).addBox(5.5F, -16.6F, -0.35F, 1, 16.6F, 0.7F),
            PartPose.offset(-18.0F, 24.0F, 5.0F));
        // bounty papers pinned to the board: one-sided sheets just in front of the cork
        board.addOrReplaceChild("papers", CubeListBuilder.create()
                .texOffs(64, 48).addBox(-6.0F, -26.0F, -0.75F, 4, 5, 0, north)
                .texOffs(72, 48).addBox(-1.5F, -26.4F, -0.75F, 4, 4, 0, north)
                .texOffs(80, 48).addBox(3.0F, -25.8F, -0.75F, 3, 5, 0, north)
                .texOffs(88, 48).addBox(-4.0F, -20.6F, -0.75F, 5, 4, 0, north)
                .texOffs(64, 54).addBox(2.0F, -20.4F, -0.75F, 4, 4, 0, north),
            PartPose.ZERO);        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        this.body.y = 12.0F + Mth.sin(t * 0.05F) * 0.15F;
        this.rightArm.xRot = -0.05F + Mth.sin(t * 0.045F) * 0.04F;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
    }
}

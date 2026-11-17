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

// The Whisperer (town/Whisperer): an old man in a long hooded cloak, bent far forward, a wrinkled face in the hood
// shadow (bushy white brows, deep-set eyes, a long hooked nose, a white beard hanging down), leaning on a gnarled cane in his right hand. Texture: textures/entity/whisperer.png
// (tools/make_whisperer.ps1, which knows this UV layout). He hobbles: short steps, the cane planted with every other
// one, a slow bob.
public class WhispererModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart cane;

    public WhispererModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.cane = root.getChild("cane");
    }

    // Model units are pixels, y down, the ground at y = 24.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // legs under the cloak: 3x7x3
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3),
            PartPose.offset(1.8F, 17.0F, 0.5F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(12, 40).addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3),
            PartPose.offset(-1.8F, 17.0F, 0.5F));
        // the cloak: a body bent far forward from the hips, with a skirt hanging to the knees
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -11.0F, -2.5F, 8, 11, 5)
                .texOffs(26, 16).addBox(-4.5F, -1.0F, -3.0F, 9, 4, 6),
            PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.55F, 0.0F, 0.0F));
        // the hump on his back
        body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(30, 30).addBox(-3.0F, -3.0F, 0.0F, 6, 5, 2),
            PartPose.offset(0.0F, -8.0F, 2.5F));
        // the hood (with the head inside), pushed forward and tipped back up so he still looks ahead
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
                .texOffs(32, 0).addBox(-4.5F, -8.5F, -4.6F, 9, 9, 1),
            PartPose.offsetAndRotation(0.0F, -11.0F, -1.0F, -0.45F, 0.0F, 0.0F));
        // an old man's face in the hood: a long hooked nose and a white beard hanging down
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(56, 10).addBox(-0.5F, -4.5F, -5.2F, 1, 2, 1),
            PartPose.ZERO);
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(40, 10).addBox(-3.0F, -2.5F, -4.3F, 6, 5, 1),
            PartPose.ZERO);
        // the hood's peak, falling back
        head.addOrReplaceChild("hood_tip", CubeListBuilder.create().texOffs(52, 0).addBox(-1.5F, -1.0F, 0.0F, 3, 2, 3),
            PartPose.offsetAndRotation(0.0F, -8.0F, 3.0F, 0.6F, 0.0F, 0.0F));
        // sleeves
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 40).addBox(-1.0F, 0.0F, -1.5F, 3, 9, 3),
            PartPose.offsetAndRotation(4.0F, -10.0F, 0.0F, -0.3F, 0.0F, -0.1F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 40).addBox(-2.0F, 0.0F, -1.5F, 3, 9, 3),
            PartPose.offsetAndRotation(-4.0F, -10.0F, 0.0F, -1.15F, 0.0F, 0.1F));
        // the cane: planted in front of his right foot, up to his hand, with a knob on top
        root.addOrReplaceChild("cane", CubeListBuilder.create()
                .texOffs(24, 40).addBox(-0.5F, -13.0F, -0.5F, 1, 13, 1)
                .texOffs(28, 40).addBox(-1.0F, -14.5F, -1.0F, 2, 2, 2),
            PartPose.offsetAndRotation(-5.0F, 24.0F, -7.0F, 0.12F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        // short, stiff steps
        this.leftLeg.xRot = Mth.cos(walk * 0.6F) * 0.6F * speed;
        this.rightLeg.xRot = Mth.cos(walk * 0.6F + Mth.PI) * 0.6F * speed;
        // the cane swings forward with the right leg and plants
        this.cane.xRot = 0.12F + Mth.cos(walk * 0.6F + Mth.PI) * 0.25F * speed;
        this.rightArm.xRot = -1.15F + Mth.cos(walk * 0.6F + Mth.PI) * 0.15F * speed;
        this.leftArm.xRot = -0.3F + Mth.cos(walk * 0.6F) * 0.2F * speed;
        // a slow bob and a little sway, even standing
        float t = state.ageInTicks;
        this.body.y = 18.0F + Mth.sin(t * 0.08F) * 0.3F + Math.abs(Mth.sin(walk * 0.6F)) * 0.5F * speed;
        this.body.zRot = Mth.sin(t * 0.05F) * 0.03F;
        // the hood follows where he looks
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = -0.45F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
    }
}

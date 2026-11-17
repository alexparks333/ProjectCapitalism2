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

// The Traveling Merchant (town/TravelingMerchant) and his wagon, in one model.
//  - Him: a weary old traveller, stooped forward, a long grey beard to his chest, a big nose, a battered wide-brimmed
//    brown hat, a red scarf, a long patched brown coat to his knees, a pack with a rolled bedroll on his back, a pipe in
//    his mouth. His right hand leans on a tall gnarled staff with a little lantern hanging from its top; his left hangs.
//  - Beside him (on his left), parallel to the way he faces: a covered wagon, Oregon Trail style. A plank wagon bed on
//    four big spoked wheels with iron rims and axles, a white canvas bonnet over hoops (patched, dusty at the hem), the
//    tongue reaching forward to the ground, a water barrel lashed to the side, crates stacked behind it, a lantern hung
//    at the front hoop.
// He barely moves: a slow breath, his head following you, the lanterns swaying. Texture: textures/entity/
// traveling_merchant.png (256 x 256, tools/make_traveling_merchant.ps1, which knows these boxes), the lantern flames in
// traveling_merchant_glow.png.
public class TravelingMerchantModel extends EntityModel<LivingEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart lantern;
    private final ModelPart wagonLantern;

    public TravelingMerchantModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.lantern = root.getChild("staff").getChild("lantern");
        this.wagonLantern = root.getChild("wagon").getChild("wagon_lantern");
    }

    // Model units are pixels, y down, the ground at y = 24; the front is -z, his left +x.
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── him ──
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-1.5F, 0.0F, -1.5F, 3, 11, 3),
            PartPose.offset(1.8F, 13.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-1.5F, 0.0F, -1.5F, 3, 11, 3),
            PartPose.offset(-1.8F, 13.0F, 0.0F));
        // stooped: the body leans forward from the hips
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -11.0F, -2.0F, 8, 11, 4)
                .texOffs(24, 32).addBox(-4.5F, -1.0F, -2.5F, 9, 7, 5),
            PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
            PartPose.offset(0.0F, -11.0F, -0.5F));
        // the long beard, from the chin down over the chest
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(88, 16).addBox(-3.0F, 0.0F, 0.0F, 6, 7, 1),
            PartPose.offsetAndRotation(0.0F, -3.0F, -4.4F, 0.12F, 0.0F, 0.0F));
        // the battered hat: a wide brim, drooping a little, and a dented crown
        head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(0, 16).addBox(-7.0F, 0.0F, -7.0F, 14, 1, 14),
            PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.06F, 0.0F, 0.03F));
        head.addOrReplaceChild("hat_crown", CubeListBuilder.create().texOffs(56, 16).addBox(-4.0F, -4.0F, -4.0F, 8, 4, 8),
            PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.0F, 0.1F, -0.05F));
        // the pipe in the corner of his mouth
        head.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(136, 0).addBox(0.0F, 0.0F, -3.0F, 1, 1, 3)
                .texOffs(136, 4).addBox(-0.5F, -2.0F, -4.0F, 2, 2, 2),
            PartPose.offset(1.0F, -2.0F, -4.0F));
        body.addOrReplaceChild("scarf", CubeListBuilder.create().texOffs(102, 16).addBox(-4.5F, 0.0F, -2.5F, 9, 2, 5),
            PartPose.offset(0.0F, -11.5F, 0.0F));
        body.addOrReplaceChild("backpack", CubeListBuilder.create().texOffs(76, 32).addBox(-3.0F, 0.0F, 0.0F, 6, 8, 4),
            PartPose.offset(0.0F, -10.0F, 2.0F));
        body.addOrReplaceChild("bedroll", CubeListBuilder.create().texOffs(96, 32).addBox(-4.0F, 0.0F, 0.0F, 8, 3, 3),
            PartPose.offset(0.0F, -12.5F, 2.5F));
        // right arm reaching forward to the staff, left arm hanging
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 32).addBox(-2.0F, -1.0F, -1.5F, 3, 12, 3),
            PartPose.offsetAndRotation(-5.0F, -10.0F, 0.0F, -0.75F, 0.0F, 0.12F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 32).addBox(-1.0F, -1.0F, -1.5F, 3, 12, 3),
            PartPose.offsetAndRotation(5.0F, -10.0F, 0.0F, -0.2F, 0.0F, -0.05F));
        // the staff, planted in front of him to his right, a lantern hanging from a hook at the top
        PartDefinition staff = root.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(120, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 26, 1),
            PartPose.offsetAndRotation(-6.5F, -2.0F, -6.0F, -0.04F, 0.0F, 0.05F));
        staff.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
            PartPose.offset(-1.5F, 1.0F, 0.0F));

        // ── the wagon ── (its middle 28 to his left, 6 behind him; 3 blocks long along z, 2.5 tall)
        PartDefinition wagon = root.addOrReplaceChild("wagon", CubeListBuilder.create()
                // the plank bed
                .texOffs(0, 64).addBox(-12.0F, -20.0F, -24.0F, 24, 8, 48)
                // the canvas bonnet: the wide lower part over the hoops, the rounded top
                .texOffs(0, 120).addBox(-13.0F, -33.0F, -22.0F, 26, 13, 44)
                .texOffs(0, 177).addBox(-9.0F, -40.0F, -22.0F, 18, 7, 44),
            PartPose.offset(28.0F, 24.0F, 6.0F));
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0) ? -12.6F : 12.6F;
            float z = i < 2 ? -15.0F : 15.0F;
            wagon.addOrReplaceChild("wheel_" + i, CubeListBuilder.create().texOffs(144, 64).addBox(-0.5F, -8.0F, -8.0F, 1, 16, 16),
                PartPose.offset(x, -8.0F, z));
        }
        wagon.addOrReplaceChild("axle_front", CubeListBuilder.create().texOffs(144, 100).addBox(-13.0F, -0.5F, -0.5F, 26, 1, 1),
            PartPose.offset(0.0F, -8.0F, -15.0F));
        wagon.addOrReplaceChild("axle_back", CubeListBuilder.create().texOffs(144, 100).addBox(-13.0F, -0.5F, -0.5F, 26, 1, 1),
            PartPose.offset(0.0F, -8.0F, 15.0F));
        // the tongue, reaching forward and down to the ground (no oxen: they're grazing somewhere)
        wagon.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(180, 64).addBox(-1.0F, -1.0F, -20.0F, 2, 2, 20),
            PartPose.offsetAndRotation(0.0F, -13.0F, -24.0F, -0.55F, 0.0F, 0.0F));
        wagon.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(144, 104).addBox(-3.0F, -4.0F, -3.0F, 6, 8, 6),
            PartPose.offset(15.5F, -17.0F, 4.0F));
        wagon.addOrReplaceChild("crate_1", CubeListBuilder.create().texOffs(170, 104).addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7),
            PartPose.offsetAndRotation(4.0F, 0.0F, 30.0F, 0.0F, 0.2F, 0.0F));
        wagon.addOrReplaceChild("crate_2", CubeListBuilder.create().texOffs(170, 104).addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7),
            PartPose.offsetAndRotation(-5.0F, 0.0F, 29.0F, 0.0F, -0.15F, 0.0F));
        wagon.addOrReplaceChild("crate_3", CubeListBuilder.create().texOffs(170, 104).addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7),
            PartPose.offsetAndRotation(-0.5F, -7.0F, 29.5F, 0.0F, 0.5F, 0.0F));
        wagon.addOrReplaceChild("wagon_lantern", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2),
            PartPose.offset(0.0F, -31.0F, -23.0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // a slow, tired breath; his head follows you (still stooped)
        this.body.y = 13.0F + Mth.sin(t * 0.045F) * 0.2F;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.2F;
        // the lanterns swing a little
        this.lantern.zRot = Mth.sin(t * 0.07F) * 0.12F;
        this.wagonLantern.xRot = Mth.sin(t * 0.05F + 1.3F) * 0.1F;
    }
}

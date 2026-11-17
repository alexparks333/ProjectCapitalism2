package com.alexh.capitalism.client;

import com.alexh.capitalism.event.CrimsonRevenant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

import static com.alexh.capitalism.client.BoatKit.*;

// ═════════════ the Crimson Revenant (event/CrimsonRevenant) ═════════════
// A hulking undead knight (drawn 1.75x by its SCALE attribute, so near four blocks tall). Blackened iron plate over a
// dark body, molten red light glowing through the cracks in its breastplate and its visor (the glow texture), a horned
// helm with a crest, huge spiked pauldrons, a tattered crimson cape, and in its right hand a greatsword nearly as long as
// it is tall, a red-hot line down its blade.
// Poses (CrimsonRevenantRenderer.State): walking, its sword swing, the SLAM (sword raised high, then smashed down), the
// LEAP (arms back, legs tucked), the SUMMON (arms spread wide, head back). Its cape sways; it breathes.
// Model pixels as for any humanoid: y down, feet at 24, facing -z.
public class CrimsonRevenantModel extends EntityModel<CrimsonRevenantRenderer.State> {

    public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_revenant");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart sword;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CrimsonRevenantModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.cape = this.body.getChild("cape");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.sword = this.rightArm.getChild("sword");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        BoatKit.resetNudges();

        // ── the body: breastplate with glowing cracks, dark middle, belt, plates over the hips, spikes down the back ──
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 0.0F));
        BoatKit b = new BoatKit(body);
        b.box(STEEL, 0, 0, -5.5F, 0.0F, -3.5F, 11.0F, 7.0F, 7.0F);
        b.box(STEEL, 0, 14, -4.0F, -0.8F, -3.0F, 8.0F, 1.0F, 6.0F);
        b.box(ENGINE, 0, 0, -4.5F, 7.0F, -3.0F, 9.0F, 5.0F, 6.0F);
        b.box(LEATHER, 0, 0, -5.0F, 11.0F, -3.4F, 10.0F, 1.6F, 6.8F);
        b.box(GOLD, 0, 0, -1.2F, 10.8F, -3.75F, 2.4F, 2.0F, 0.5F);
        for (int side = -1; side <= 1; side += 2) {
            b.box(STEEL, 20, 0, side < 0 ? -4.6F : 0.6F, 12.4F, -3.7F, 4.0F, 4.5F, 0.8F);
            b.box(STEEL, 20, 6, side < 0 ? -4.6F : 0.6F, 12.4F, 2.9F, 4.0F, 4.0F, 0.8F);
        }
        // the cracks in the breastplate, red-hot (glow)
        b.line(FIRE, -3.6F, 1.2F, -3.62F, -1.4F, 3.6F, -3.62F, 0.45F);
        b.line(FIRE, -1.4F, 3.6F, -3.62F, -2.4F, 6.2F, -3.62F, 0.4F);
        b.line(FIRE, 3.4F, 0.8F, -3.62F, 1.2F, 2.6F, -3.62F, 0.45F);
        b.line(FIRE, 1.2F, 2.6F, -3.62F, 2.0F, 5.4F, -3.62F, 0.4F);
        b.line(FIRE, 0.0F, 4.0F, -3.62F, 0.4F, 6.8F, -3.62F, 0.35F);
        // a glowing heart behind a cracked plate
        b.box(FIRE, 0, 0, -0.9F, 2.6F, -3.75F, 1.8F, 1.8F, 0.3F);
        // spikes down its back
        for (int i = 0; i < 3; i++) b.line(STEEL, 0.0F, 1.5F + i * 3.0F, 3.5F, 0.0F, 0.2F + i * 3.0F, 6.0F, 1.0F);
        body.addOrReplaceChild("statics", b.statics, PartPose.ZERO);

        // ── the head: horned helm, crest, visor slit with burning eyes ──
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -0.5F));
        BoatKit h = new BoatKit(head);
        h.box(STEEL, 0, 0, -4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F);
        h.box(STEEL, 30, 0, -0.8F, -10.6F, -4.2F, 1.6F, 2.0F, 8.6F);
        h.box(BLACK, 0, 0, -3.6F, -5.8F, -4.75F, 7.2F, 1.4F, 0.4F);
        h.box(NAV_RED, 0, 0, -3.0F, -5.6F, -4.9F, 1.8F, 1.0F, 0.3F);
        h.box(NAV_RED, 0, 0, 1.2F, -5.6F, -4.9F, 1.8F, 1.0F, 0.3F);
        h.box(STEEL, 0, 20, -4.2F, -2.6F, -5.0F, 8.4F, 2.8F, 1.0F);
        h.box(GOLD, 0, 0, -4.7F, -6.6F, -4.7F, 9.4F, 0.6F, 0.4F);
        for (int side = -1; side <= 1; side += 2) {
            h.line(BONE, side * 4.4F, -7.0F, -0.5F, side * 7.6F, -8.6F, -1.5F, 1.8F);
            h.line(BONE, side * 7.6F, -8.6F, -1.5F, side * 8.8F, -12.0F, -3.4F, 1.4F);
            h.line(BONE, side * 8.8F, -12.0F, -3.4F, side * 8.6F, -14.0F, -5.2F, 0.9F);
        }
        head.addOrReplaceChild("statics", h.statics, PartPose.ZERO);

        // ── the cape: tattered crimson, hanging from the shoulders ──
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.2F, 3.6F, 0.12F, 0, 0));
        BoatKit c = new BoatKit(cape);
        c.box(CANVAS, 0, 0, -5.5F, 0.0F, 0.0F, 11.0F, 21.0F, 0.6F);
        c.box(CANVAS, 30, 0, -5.0F, 21.0F, 0.0F, 3.0F, 2.5F, 0.6F);
        c.box(CANVAS, 30, 4, 0.5F, 21.0F, 0.0F, 4.0F, 3.5F, 0.6F);
        cape.addOrReplaceChild("statics", c.statics, PartPose.ZERO);

        // ── the arms: dark sleeves, plate vambraces and gauntlets, the great spiked pauldrons ──
        for (int side = -1; side <= 1; side += 2) {
            boolean right = side < 0;
            PartDefinition arm = body.addOrReplaceChild(right ? "right_arm" : "left_arm", CubeListBuilder.create(),
                PartPose.offset(side * 7.5F, 1.5F, 0.0F));
            BoatKit a = new BoatKit(arm);
            a.box(ENGINE, 0, 12, -2.5F, -1.0F, -2.5F, 5.0F, 12.0F, 5.0F);
            a.box(STEEL, 0, 22, -2.8F, 5.0F, -2.8F, 5.6F, 5.0F, 5.6F);
            a.box(STEEL, 24, 22, -2.9F, 10.0F, -2.9F, 5.8F, 3.2F, 5.8F);
            float px = right ? -4.6F : -2.4F;
            a.box(STEEL, 0, 0, px, -3.6F, -3.8F, 7.0F, 4.8F, 7.6F);
            a.box(GOLD, 0, 0, px - 0.1F, 0.8F, -3.9F, 7.2F, 0.6F, 7.8F);
            float sx = right ? -2.0F : 2.0F;
            a.line(BONE, sx, -3.5F, -1.8F, sx + side * 1.5F, -8.0F, -2.2F, 1.3F);
            a.line(BONE, sx, -3.5F, 1.8F, sx + side * 1.2F, -7.0F, 2.4F, 1.1F);
            a.line(FIRE, -1.5F, 6.0F, -2.86F, 1.0F, 8.5F, -2.86F, 0.35F);
            if (right) {
                // the greatsword, held in the fist, its blade along -z (tipped down toward the ground when resting)
                PartDefinition sword = arm.addOrReplaceChild("sword", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 11.5F, 0.0F, 0.55F, 0, 0));
                BoatKit s = new BoatKit(sword);
                s.box(LEATHER, 10, 0, -0.8F, -0.8F, -1.5F, 1.6F, 1.6F, 5.5F);
                s.box(GOLD, 8, 4, -1.2F, -1.2F, 4.0F, 2.4F, 2.4F, 2.0F);
                s.box(GOLD, 16, 0, -4.2F, -1.0F, -3.0F, 8.4F, 2.0F, 1.6F);
                s.box(GOLD, 16, 4, -1.6F, -1.4F, -3.4F, 3.2F, 2.8F, 2.2F);
                // (the blade and its red-hot line in lengths: each box must fit its patch of the texture)
                s.box(GREY, 0, 0, -1.6F, -0.45F, -15.0F, 3.2F, 0.9F, 12.0F);
                s.box(GREY, 14, 0, -1.6F, -0.45F, -27.0F, 3.2F, 0.9F, 12.0F);
                s.box(GREY, 0, 26, -1.1F, -0.4F, -29.0F, 2.2F, 0.8F, 2.0F);
                s.box(GREY, 6, 26, -0.6F, -0.35F, -30.4F, 1.2F, 0.7F, 1.4F);
                for (int seg = 0; seg < 4; seg++) s.box(FIRE, 0, 0, -0.3F, -0.5F, -25.0F + seg * 5.25F, 0.6F, 1.0F, 5.25F);
                sword.addOrReplaceChild("statics", s.statics, PartPose.ZERO);
            }
            arm.addOrReplaceChild("statics", a.statics, PartPose.ZERO);
        }

        // ── the legs: dark, greaves, armoured boots, spiked knees ──
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition leg = root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create(),
                PartPose.offset(side * 2.6F, 12.0F, 0.0F));
            BoatKit l = new BoatKit(leg);
            l.box(ENGINE, 20, 12, -2.4F, 0.0F, -2.4F, 4.8F, 12.0F, 4.8F);
            l.box(STEEL, 0, 0, -2.7F, 4.5F, -2.8F, 5.4F, 5.5F, 5.6F);
            l.box(STEEL, 22, 0, -2.8F, 10.0F, -4.0F, 5.6F, 2.0F, 6.8F);
            l.line(STEEL, 0.0F, 4.8F, -2.8F, 0.0F, 3.2F, -4.8F, 1.1F);
            leg.addOrReplaceChild("statics", l.statics, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(CrimsonRevenantRenderer.State state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // the head turns to look
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // walking: a heavy stride
        float walk = state.walkAnimationPos * 0.55F;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        this.rightLeg.xRot = Mth.cos(walk) * 1.1F * amount;
        this.leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.1F * amount;
        this.leftArm.xRot = Mth.cos(walk) * 0.7F * amount;
        this.rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.35F * amount - 0.15F;
        this.leftArm.zRot = -0.12F;
        this.rightArm.zRot = 0.12F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.body.xRot = 0.06F + 0.02F * Mth.sin(t * 0.07F);
        this.body.y = -1.0F + 0.25F * Mth.sin(t * 0.07F);
        this.sword.xRot = 0.55F;
        this.cape.xRot = 0.12F + 0.25F * amount + 0.05F * Mth.sin(t * 0.09F);
        // the sword swing: up and over, down hard
        if (state.attack > 0.0F) {
            float swing = Mth.sin(state.attack * Mth.PI);
            this.rightArm.xRot = -2.4F * swing + (state.attack > 0.5F ? -0.6F * (1 - state.attack) : 0.0F);
            this.rightArm.yRot = 0.4F * swing;
            this.body.xRot += 0.25F * swing;
            this.sword.xRot = 0.55F - 0.5F * swing;
        }
        float a = state.actionTicks;
        switch (state.action) {
            case CrimsonRevenant.SLAM_WINDUP -> {
                // the sword rises high in both hands, it leans back
                float up = Math.min(1.0F, a / 10.0F);
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -3.0F);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -2.8F);
                this.leftArm.zRot = Mth.lerp(up, -0.12F, 0.45F);
                this.rightArm.zRot = Mth.lerp(up, 0.12F, -0.2F);
                this.sword.xRot = Mth.lerp(up, 0.55F, 1.35F);
                this.body.xRot = Mth.lerp(up, this.body.xRot, -0.2F) + 0.02F * Mth.sin(t * 1.3F);
            }
            case CrimsonRevenant.SLAM -> {
                // smashed down, leaning in over it
                float down = Math.min(1.0F, a / 3.0F);
                this.rightArm.xRot = Mth.lerp(down, -3.0F, -1.2F);
                this.leftArm.xRot = Mth.lerp(down, -2.8F, -1.1F);
                this.leftArm.zRot = 0.45F;
                this.sword.xRot = Mth.lerp(down, 1.35F, 1.0F);
                this.body.xRot = Mth.lerp(down, -0.2F, 0.45F);
            }
            case CrimsonRevenant.LEAP -> {
                this.rightArm.xRot = 0.9F;
                this.leftArm.xRot = 0.9F;
                this.rightLeg.xRot = -0.9F;
                this.leftLeg.xRot = 0.4F;
                this.body.xRot = 0.35F;
                this.sword.xRot = 1.2F;
                this.cape.xRot = 0.9F;
            }
            case CrimsonRevenant.SUMMON -> {
                // arms spread wide, the sword held up, head thrown back
                float up = Math.min(1.0F, a / 12.0F);
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -2.6F);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -2.2F);
                this.rightArm.zRot = Mth.lerp(up, 0.12F, 0.7F);
                this.leftArm.zRot = Mth.lerp(up, -0.12F, -1.0F);
                this.sword.xRot = Mth.lerp(up, 0.55F, 1.45F);
                this.head.xRot = Mth.lerp(up, this.head.xRot, -0.6F);
                this.body.xRot = Mth.lerp(up, this.body.xRot, -0.15F);
                this.cape.xRot = 0.3F + 0.15F * Mth.sin(t * 0.5F);
            }
            case CrimsonRevenant.SWEEP_WINDUP -> {
                // the sword drawn back low to its right, the body twisted round to wind up
                float up = Math.min(1.0F, a / 8.0F);
                this.body.yRot = Mth.lerp(up, 0.0F, 0.9F);
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -1.2F);
                this.rightArm.yRot = Mth.lerp(up, 0.0F, 1.1F);
                this.rightArm.zRot = Mth.lerp(up, 0.12F, 0.5F);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -0.6F);
                this.sword.xRot = Mth.lerp(up, 0.55F, 1.5F);
                this.body.xRot = 0.15F;
            }
            case CrimsonRevenant.SWEEP -> {
                // a full circle, the sword held out flat
                float spin = Math.min(1.0F, a / 7.0F);
                this.body.yRot = 0.9F - spin * (Mth.TWO_PI + 0.9F);
                this.rightArm.xRot = -1.45F;
                this.rightArm.yRot = 0.3F;
                this.rightArm.zRot = 0.4F;
                this.leftArm.xRot = -0.5F;
                this.leftArm.zRot = -0.8F;
                this.sword.xRot = 1.5F;
                this.cape.xRot = 0.6F;
                this.body.xRot = 0.1F;
            }
            case CrimsonRevenant.BLOOD_LINE -> {
                // both hands raise the sword point-down, then drive it into the ground before it
                float up = Math.min(1.0F, a / 8.0F);
                float down = Math.max(0.0F, Math.min(1.0F, (a - 10.0F) / 2.5F));
                float armX = Mth.lerp(down, Mth.lerp(up, this.rightArm.xRot, -2.7F), -0.9F);
                this.rightArm.xRot = armX;
                this.leftArm.xRot = armX + 0.1F;
                this.rightArm.zRot = 0.3F;
                this.leftArm.zRot = -0.45F;
                this.rightArm.yRot = -0.35F;
                this.leftArm.yRot = 0.35F;
                this.sword.xRot = Mth.lerp(down, 2.4F, 1.25F);
                this.body.xRot = Mth.lerp(down, -0.15F, 0.55F);
            }
            default -> {
                this.body.yRot = 0.0F;
            }
        }
        if (state.action != CrimsonRevenant.SWEEP_WINDUP && state.action != CrimsonRevenant.SWEEP) this.body.yRot = 0.0F;
        // hit: it reels back
        float f = state.flinch * state.flinch;
        if (f > 0.0F) {
            this.body.xRot -= 0.32F * f;
            this.head.xRot -= 0.35F * f;
            this.rightArm.xRot -= 0.25F * f;
            this.leftArm.xRot -= 0.35F * f;
            this.leftArm.zRot -= 0.25F * f;
            this.cape.xRot += 0.3F * f;
        }
    }
}

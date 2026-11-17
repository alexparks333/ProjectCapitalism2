package com.alexh.capitalism.client;

import com.alexh.capitalism.event.SanguineWitch;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import static com.alexh.capitalism.client.BoatKit.*;

// ═════════════ the Sanguine Witch (event/SanguineWitch) ═════════════
// A tall, gaunt blood sorceress (drawn 1.5x by her SCALE attribute, over three blocks tall) floating well off the ground:
// great bat-like wings of blood-red membrane stretched on bone, robes of deep crimson silk falling in tiers to a hem
// that frays away into wisps, a near-black hood, long black hair streaming down her back, a towering collar fanned with
// bone, a corpse-pale face with burning eyes and tears of blood, a tall crown of bone; long sleeves, pale clawed hands, a
// bone staff crowned with a glowing crystal of blood in her left hand; three orbs of blood circling her.
// Poses (BloodBossRenderer.State): floating, wings slowly beating; BOLTS (a hand drawn back and flung), RAIN and SPEARS
// (arms and staff raised high), DRAIN (a hand held out), BLINK (wings folding round her), SUMMON (arms spread), NOVA
// (risen, wings spread wide, arms flung out). She recoils when hit.
// Model pixels: y down, the ground at 24, facing -z.
public class SanguineWitchModel extends EntityModel<BloodBossRenderer.State> {

    public static final ModelLayerLocation LAYER = DecorModels.layer("sanguine_witch");
    private static final float FLOAT_Y = -3.5F;
    // how big her wings are drawn
    private static final float WS = 1.3F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart skirt;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart[] orbs = new ModelPart[3];

    public SanguineWitchModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.skirt = this.body.getChild("skirt");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWing = this.body.getChild("left_wing");
        for (int i = 0; i < 3; i++) this.orbs[i] = root.getChild("orb" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        BoatKit.resetNudges();

        // ── the body: bodice, shawl, belt with a blood jewel, the towering collar fanned with bone ──
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, FLOAT_Y, 0.0F));
        BoatKit b = new BoatKit(body);
        b.box(CANVAS, 0, 0, -4.0F, -1.0F, -2.5F, 8.0F, 11.0F, 5.0F);
        b.box(DARKWOOD, 0, 0, -5.0F, -1.5F, -3.0F, 10.0F, 3.0F, 6.0F);
        b.box(DARKWOOD, 0, 12, -2.5F, 1.5F, -2.9F, 5.0F, 7.5F, 0.6F);
        b.box(GOLD, 0, 0, -4.3F, 9.0F, -2.8F, 8.6F, 1.2F, 5.6F);
        b.box(FIRE, 0, 0, -0.9F, 8.8F, -3.0F, 1.8F, 1.6F, 0.4F);
        for (int i = 0; i < 4; i++) b.line(GOLD, -2.4F + i * 1.6F, 1.6F, -2.95F, -2.0F + i * 1.3F, 8.8F, -2.95F, 0.3F);
        // the collar: a dark fan behind her head, ribbed with bone
        b.box(DARKWOOD, 34, 0, -5.0F, -9.5F, 2.2F, 10.0F, 8.5F, 1.0F);
        for (int i = -3; i <= 3; i++) {
            float x = i * 1.75F;
            b.line(BONE, x * 0.8F, -1.5F, 2.9F, x * 1.6F, -12.5F + Math.abs(i) * 1.2F, 3.8F, 0.8F);
        }
        body.addOrReplaceChild("statics", b.statics, PartPose.ZERO);

        // ── the skirt: tiers widening down, then frayed strips trailing off into wisps ──
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));
        BoatKit s = new BoatKit(skirt);
        s.box(CANVAS, 0, 0, -4.6F, 0.0F, -3.0F, 9.2F, 4.0F, 6.0F);
        s.box(CANVAS, 0, 0, -5.6F, 3.5F, -4.0F, 11.2F, 4.0F, 8.0F);
        s.box(CANVAS, 0, 0, -6.6F, 7.0F, -5.0F, 13.2F, 4.0F, 10.0F);
        s.box(DARKWOOD, 0, 0, -5.7F, 3.3F, -4.1F, 11.4F, 0.6F, 8.2F);
        s.box(GOLD, 0, 0, -6.7F, 10.4F, -5.1F, 13.4F, 0.6F, 0.4F);
        s.box(GOLD, 0, 0, -6.7F, 10.4F, 4.7F, 13.4F, 0.6F, 0.4F);
        s.box(GOLD, 0, 0, -6.7F, 10.4F, -4.7F, 0.4F, 0.6F, 9.4F);
        s.box(GOLD, 0, 0, 6.3F, 10.4F, -4.7F, 0.4F, 0.6F, 9.4F);
        // strips hanging from the hem, longest at the back, a drop of blood glowing at some tips
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI * 2 / 12 + 0.2;
            float x = (float) Math.cos(ang) * 6.0F, z = (float) Math.sin(ang) * 4.6F;
            float len = 3.0F + (z > 0 ? 3.5F * z / 4.6F : 0) + (i % 3) * 0.8F;
            s.line(CANVAS, x, 10.8F, z, x * 1.08F, 10.8F + len, z * 1.1F + 0.6F, 1.4F);
            if (i % 4 == 1) s.box(FIRE, 0, 0, x * 1.08F - 0.3F, 10.8F + len - 0.2F, z * 1.1F + 0.3F, 0.6F, 0.8F, 0.6F);
        }
        skirt.addOrReplaceChild("statics", s.statics, PartPose.ZERO);

        // ── the head: pale face in a deep hood, burning eyes, tears of blood, a tall crown of bone, long black hair ──
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 0.0F));
        BoatKit h = new BoatKit(head);
        h.box(WHITE, 0, 0, -3.0F, -7.5F, -3.2F, 6.0F, 7.0F, 6.0F);
        h.box(DARKWOOD, 0, 0, -3.8F, -8.4F, -2.6F, 7.6F, 8.0F, 6.6F);
        for (int side = -1; side <= 1; side += 2) h.box(DARKWOOD, 30, 0, side < 0 ? -3.8F : 3.0F, -8.2F, -3.5F, 0.8F, 8.0F, 0.9F);
        h.box(DARKWOOD, 30, 12, -3.8F, -8.4F, -3.5F, 7.6F, 1.2F, 0.9F);
        h.box(BLACK, 0, 0, -2.6F, -5.1F, -3.3F, 2.0F, 1.2F, 0.2F);
        h.box(BLACK, 0, 0, 0.6F, -5.1F, -3.3F, 2.0F, 1.2F, 0.2F);
        h.box(NAV_RED, 0, 0, -2.3F, -4.9F, -3.4F, 1.5F, 0.8F, 0.3F);
        h.box(NAV_RED, 0, 0, 0.8F, -4.9F, -3.4F, 1.5F, 0.8F, 0.3F);
        h.box(DARKWOOD, 0, 0, -2.6F, -5.9F, -3.3F, 5.2F, 0.5F, 0.2F);
        h.box(FIRE, 0, 0, -1.1F, -2.1F, -3.35F, 2.2F, 0.5F, 0.2F);
        h.line(FIRE, -1.6F, -4.1F, -3.36F, -1.9F, -0.6F, -3.36F, 0.35F);
        h.line(FIRE, 1.6F, -4.1F, -3.36F, 1.5F, -1.2F, -3.36F, 0.35F);
        // the crown: a band of bone, tall spikes, a blood jewel at the front
        h.box(BONE, 0, 0, -3.9F, -9.2F, -2.9F, 7.8F, 0.8F, 7.0F);
        for (int i = 0; i < 7; i++) {
            double ang = -Math.PI + i * Math.PI / 6;
            float cx = (float) Math.cos(ang) * 3.6F, cz = (float) Math.sin(ang) * 3.4F + 0.6F;
            float tall = i == 3 ? 7.0F : i == 2 || i == 4 ? 5.2F : i == 1 || i == 5 ? 3.8F : 2.6F;
            h.line(BONE, cx, -9.0F, cz, cx * 1.3F, -9.0F - tall, cz * 1.2F, 0.85F);
        }
        h.box(FIRE, 0, 0, -0.7F, -10.4F, -3.2F, 1.4F, 1.4F, 0.5F);
        // long black hair streaming down her back
        for (int i = -2; i <= 2; i++) h.line(DARKWOOD, i * 1.3F, -7.0F, 3.0F, i * 1.6F, 5.0F + Math.abs(i) * -1.2F, 5.2F, 1.5F);
        head.addOrReplaceChild("statics", h.statics, PartPose.ZERO);

        // ── the arms: long sleeves flaring at the cuff, pale hands, long bone claws; the staff in her left hand ──
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition arm = body.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create(),
                PartPose.offset(side * 5.2F, 0.5F, 0.0F));
            BoatKit a = new BoatKit(arm);
            a.box(CANVAS, 0, 0, -1.4F, -1.0F, -1.4F, 2.8F, 8.0F, 2.8F);
            a.box(DARKWOOD, 0, 0, -2.1F, 6.0F, -2.1F, 4.2F, 3.4F, 4.2F);
            a.line(DARKWOOD, 0.0F, 9.0F, 1.6F, side * 0.6F, 14.0F, 3.6F, 2.6F);
            a.box(GOLD, 0, 0, -2.2F, 8.8F, -2.2F, 4.4F, 0.6F, 4.4F);
            a.box(WHITE, 0, 0, -1.0F, 9.0F, -1.0F, 2.0F, 3.0F, 2.0F);
            for (int c = -1; c <= 1; c++) a.line(BONE, c * 0.6F, 11.6F, -0.5F, c * 0.75F, 15.0F, -1.3F, 0.45F);
            if (side > 0) {
                // the staff: a bone shaft, a cage of bone prongs round a glowing crystal of blood
                a.line(BONE, 0.0F, 22.0F, -1.6F, 0.0F, -2.0F, -1.6F, 0.9F);
                a.box(GOLD, 0, 0, -0.8F, 10.0F, -2.4F, 1.6F, 1.0F, 1.6F);
                a.box(FIRE, 0, 0, -1.0F, -6.2F, -2.6F, 2.0F, 3.4F, 2.0F);
                a.box(FIRE, 8, 0, -0.5F, -7.4F, -2.1F, 1.0F, 1.4F, 1.0F);
                for (int p = 0; p < 4; p++) {
                    float px = (p % 2 == 0 ? 1 : -1) * (p < 2 ? 1.0F : 0.0F), pz = (p % 2 == 0 ? 1 : -1) * (p < 2 ? 0.0F : 1.0F);
                    a.line(BONE, px * 0.6F, -2.0F, -1.6F + pz * 0.6F, px * 1.8F, -5.0F, -1.6F + pz * 1.8F, 0.5F);
                    a.line(BONE, px * 1.8F, -5.0F, -1.6F + pz * 1.8F, px * 0.5F, -7.8F, -1.6F + pz * 0.5F, 0.45F);
                }
            }
            arm.addOrReplaceChild("statics", a.statics, PartPose.ZERO);
        }

        // ── the wings: bone arms spreading out from her shoulder blades, fingers of bone, a blood-red membrane ──
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition wing = body.addOrReplaceChild(side < 0 ? "right_wing" : "left_wing", CubeListBuilder.create(),
                PartPose.offset(side * 1.8F, 1.0F, 3.0F));
            BoatKit w = new BoatKit(wing);
            float[][] strips = {{1, -5.5F, 3.5F}, {4, -7.5F, 4.5F}, {7, -9.5F, 2.5F}, {10, -10.5F, -0.5F}, {13, -10.5F, -3.5F}};
            for (int i = 0; i < strips.length; i++) {
                float x0 = strips[i][0] * WS, wdt = (i == strips.length - 1 ? 2.5F : 3.0F) * WS;
                float x = side > 0 ? x0 : -x0 - wdt;
                w.box(ACCENT, 0, 0, x, strips[i][1] * WS, 0.0F, wdt, (strips[i][2] - strips[i][1]) * WS, 0.2F);
            }
            w.line(BONE, (0.0F) * WS, (0.0F) * WS, -0.2F, (side * 7.0F) * WS, (-8.0F) * WS, -0.3F, 1.0F);
            w.line(BONE, (side * 7.0F) * WS, (-8.0F) * WS, -0.3F, (side * 15.5F) * WS, (-11.0F) * WS, -0.3F, 0.8F);
            w.line(BONE, (side * 7.0F) * WS, (-8.0F) * WS, -0.3F, (side * 15.5F) * WS, (-3.5F) * WS, -0.3F, 0.6F);
            w.line(BONE, (side * 7.0F) * WS, (-8.0F) * WS, -0.3F, (side * 11.5F) * WS, (2.5F) * WS, -0.3F, 0.6F);
            w.line(BONE, (side * 7.0F) * WS, (-8.0F) * WS, -0.3F, (side * 5.5F) * WS, (4.5F) * WS, -0.3F, 0.6F);
            w.line(BONE, (side * 7.0F) * WS, (-8.0F) * WS, -0.4F, (side * 7.6F) * WS, (-10.4F) * WS, -0.4F, 0.7F);
            wing.addOrReplaceChild("statics", w.statics, PartPose.ZERO);
        }

        // ── three orbs of blood circling her ──
        for (int i = 0; i < 3; i++) {
            PartDefinition orb = root.addOrReplaceChild("orb" + i, CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
            BoatKit o = new BoatKit(orb);
            o.box(FIRE, 0, 0, -1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F);
            o.box(FIRE, 8, 0, -0.6F, -1.4F, -0.6F, 1.2F, 2.8F, 1.2F);
            orb.addOrReplaceChild("statics", o.statics, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(BloodBossRenderer.State state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float a = state.actionTicks;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        // floating: bobbing, leaning into her glide, the hem swaying
        float bob = Mth.sin(t * 0.09F);
        this.body.y = FLOAT_Y + 1.2F * bob;
        this.body.xRot = 0.12F * amount;
        this.body.zRot = 0.03F * Mth.sin(t * 0.05F);
        this.body.yRot = 0.0F;
        this.skirt.xRot = 0.25F * amount + 0.05F * Mth.sin(t * 0.11F);
        this.head.yRot = Mth.clamp(state.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // arms hanging a little out, drifting
        this.rightArm.xRot = -0.1F + 0.06F * Mth.sin(t * 0.07F) + 0.2F * amount;
        this.leftArm.xRot = -0.25F + 0.04F * Mth.sin(t * 0.07F + 1.0F);
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = 0.22F + 0.04F * Mth.sin(t * 0.06F);
        this.leftArm.zRot = -0.12F;
        // the wings: half folded back, slowly beating
        float beat = Mth.sin(t * 0.12F);
        float open = 0.55F;
        float lift = 0.0F;
        // the orbs circle her waist, rising and falling
        float orbit = t * 0.08F;
        float spread = 1.0F;
        float orbY = 6.0F;
        switch (state.action) {
            case SanguineWitch.BOLTS -> {
                // the right hand drawn back, then flung forward
                float draw = Math.min(1.0F, a / 10.0F);
                float fling = Math.max(0.0F, Math.min(1.0F, (a - 12.0F) / 3.0F));
                this.rightArm.xRot = Mth.lerp(fling, Mth.lerp(draw, this.rightArm.xRot, -2.6F), -1.4F);
                this.rightArm.yRot = Mth.lerp(fling, 0.4F * draw, -0.2F);
                this.leftArm.xRot = -0.6F;
                this.body.yRot = Mth.lerp(fling, 0.35F * draw, -0.2F);
                orbit *= 3.0F;
                spread = 0.7F;
                orbY = 0.0F;
                open = 0.75F;
            }
            case SanguineWitch.RAIN, SanguineWitch.SPEARS -> {
                // the staff and a clawed hand raised high, head back, wings opening
                float up = Math.min(1.0F, a / 10.0F);
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -2.9F);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -2.95F);
                this.rightArm.zRot = Mth.lerp(up, 0.22F, -0.3F);
                this.leftArm.zRot = Mth.lerp(up, -0.12F, 0.2F);
                this.head.xRot = Mth.lerp(up, this.head.xRot, -0.5F);
                orbY = Mth.lerp(up, 6.0F, -16.0F);
                spread = 0.6F;
                orbit *= 4.0F;
                open = Mth.lerp(up, 0.55F, 1.0F);
                lift = 0.3F * up;
            }
            case SanguineWitch.DRAIN -> {
                // a clawed hand held out at her prey, the staff planted
                this.rightArm.xRot = -1.55F + 0.05F * Mth.sin(t * 0.8F);
                this.rightArm.yRot = 0.1F;
                this.leftArm.xRot = -0.4F;
                this.body.xRot = -0.12F;
                this.head.xRot -= 0.15F;
                spread = 0.5F;
                orbY = 2.0F;
                open = 0.8F;
            }
            case SanguineWitch.BLINK -> {
                // the wings fold round her
                float c = Math.min(1.0F, a / 5.0F) * (1.0F - Math.max(0.0F, (a - 8.0F) / 6.0F));
                this.rightArm.xRot = Mth.lerp(c, this.rightArm.xRot, -1.2F);
                this.leftArm.xRot = Mth.lerp(c, this.leftArm.xRot, -1.2F);
                this.rightArm.yRot = -0.9F * c;
                this.leftArm.yRot = 0.9F * c;
                this.head.xRot = Mth.lerp(c, this.head.xRot, 0.5F);
                spread = 1.0F - 0.7F * c;
                open = Mth.lerp(c, 0.55F, -0.6F);
            }
            case SanguineWitch.SUMMON -> {
                // arms spread wide, head thrown back
                float up = Math.min(1.0F, a / 12.0F);
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -1.9F);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -1.9F);
                this.rightArm.zRot = Mth.lerp(up, 0.22F, 1.1F);
                this.leftArm.zRot = Mth.lerp(up, -0.12F, -1.1F);
                this.head.xRot = Mth.lerp(up, this.head.xRot, -0.6F);
                this.body.xRot = -0.15F * up;
                spread = 1.0F + 0.8F * up;
                orbit *= 2.0F;
                open = Mth.lerp(up, 0.55F, 1.0F);
            }
            case SanguineWitch.EXHAUSTED -> {
                // spent: sunk low, head hanging, wings drooping, the orbs dimly circling her feet
                this.body.y = FLOAT_Y + 3.5F + 0.3F * bob;
                this.body.xRot = 0.35F;
                this.head.xRot = 0.6F;
                this.rightArm.xRot = 0.2F;
                this.leftArm.xRot = 0.1F;
                this.rightArm.zRot = 0.1F;
                this.leftArm.zRot = -0.05F;
                open = 0.2F;
                lift = -0.35F;
                beat *= 0.2F;
                orbY = 16.0F;
                spread = 0.6F;
                orbit *= 0.4F;
            }
            case SanguineWitch.NOVA -> {
                // risen, wings spread wide and beating hard, arms flung out; at each ring she throws them wide
                float up = Math.min(1.0F, a / 14.0F);
                float burst = Math.max(Mth.clamp(1.0F - Math.abs(a - 27.0F) / 4.0F, 0.0F, 1.0F), Mth.clamp(1.0F - Math.abs(a - 41.0F) / 4.0F, 0.0F, 1.0F));
                this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -0.4F - 0.4F * burst);
                this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -0.4F - 0.4F * burst);
                this.rightArm.zRot = Mth.lerp(up, 0.22F, 1.2F + 0.3F * burst);
                this.leftArm.zRot = Mth.lerp(up, -0.12F, -1.2F - 0.3F * burst);
                this.head.xRot = Mth.lerp(up, this.head.xRot, -0.45F);
                this.body.xRot = -0.1F * up;
                this.skirt.xRot = 0.15F * Mth.sin(t * 0.5F);
                beat = Mth.sin(t * 0.45F);
                open = 1.05F + 0.15F * burst;
                lift = 0.25F;
                spread = 1.0F + 1.2F * burst;
                orbit *= 3.0F;
                orbY = 2.0F;
            }
            default -> {
            }
        }
        // the wings: yRot folds them back, zRot lifts them, beating
        float fold = (1.0F - open) * 1.25F;
        this.rightWing.yRot = fold + 0.12F * beat;
        this.leftWing.yRot = -fold - 0.12F * beat;
        this.rightWing.zRot = -lift - 0.1F * beat;
        this.leftWing.zRot = lift + 0.1F * beat;
        this.rightWing.xRot = 0.1F;
        this.leftWing.xRot = 0.1F;
        for (int i = 0; i < 3; i++) {
            float ang = orbit + i * Mth.TWO_PI / 3.0F;
            this.orbs[i].x = Mth.cos(ang) * 11.0F * spread;
            this.orbs[i].z = Mth.sin(ang) * 11.0F * spread;
            this.orbs[i].y = orbY + 2.0F * Mth.sin(t * 0.13F + i * 2.1F) + this.body.y;
            this.orbs[i].yRot = t * 0.1F + i;
            this.orbs[i].xRot = t * 0.07F;
        }
        // hit: she recoils
        float f = state.flinch * state.flinch;
        if (f > 0.0F) {
            this.body.xRot -= 0.25F * f;
            this.head.xRot -= 0.35F * f;
            this.rightArm.zRot += 0.4F * f;
            this.leftArm.zRot -= 0.4F * f;
            this.rightWing.yRot -= 0.3F * f;
            this.leftWing.yRot += 0.3F * f;
        }
    }
}

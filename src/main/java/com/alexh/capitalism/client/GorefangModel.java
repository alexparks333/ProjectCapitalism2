package com.alexh.capitalism.client;

import com.alexh.capitalism.event.Gorefang;
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

// ═════════════ Gorefang, the Blood Wolf (event/Gorefang) ═════════════
// A huge wolf (drawn 1.6x by its SCALE attribute, near three blocks tall at the shoulder with its head up): crimson fur,
// a great shaggy near-black mane over its chest and neck, a mask of bone over its brow, bone spikes down its back, long
// fangs, a jaw that opens on a furnace glow in its throat, burning eyes, a heavy brush of a tail.
// Poses (BloodBossRenderer.State): trotting / galloping, its bite (the head lunges, the jaw snaps), the POUNCE crouch and
// leap, the CHARGE (head down, scraping the ground, then a gallop), DAZED (head lolling), the HOWL (rearing, head up,
// jaw wide). It reels back when hit.
// Model pixels: y down, feet at 24, facing -z.
public class GorefangModel extends EntityModel<BloodBossRenderer.State> {

    public static final ModelLayerLocation LAYER = DecorModels.layer("gorefang");
    // where the legs hang from
    private static final float FRONT_Y = 10.0F;
    private static final float BACK_Y = 12.0F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart rightFront;
    private final ModelPart leftFront;
    private final ModelPart rightBack;
    private final ModelPart leftBack;

    public GorefangModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.rightFront = root.getChild("right_front");
        this.leftFront = root.getChild("left_front");
        this.rightBack = root.getChild("right_back");
        this.leftBack = root.getChild("left_back");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        BoatKit.resetNudges();

        // ── the body: a hunched, maned dire wolf. The great shoulders (a hump of muscle under a spiky mane), a
        // spiked iron collar, the lean rear with its ribs showing through the hide, fire glowing behind them, a ridge
        // of bone spikes down the spine ──
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 9.0F, 0.0F));
        BoatKit b = new BoatKit(body);
        b.box(WHITE, 0, 0, -7.0F, -7.0F, -12.0F, 14.0F, 14.0F, 12.0F);
        b.box(CANVAS, 0, 0, -6.0F, -10.0F, -10.0F, 12.0F, 5.0F, 9.0F);
        b.box(CANVAS, 0, 0, -5.5F, -4.5F, 0.0F, 11.0F, 9.5F, 12.0F);
        b.box(ENGINE, 0, 0, -4.5F, 3.5F, -1.0F, 9.0F, 3.0F, 11.0F);
        // the ribs: bone bars over a gash in the hide, fire glowing behind them
        for (int side = -1; side <= 1; side += 2) {
            float sx = side * 5.56F;
            b.box(FIRE, 0, 0, sx - 0.1F, -2.6F, 1.2F, 0.2F, 4.5F, 3.4F);
            b.box(FIRE, 0, 0, sx - 0.1F, -2.6F, 4.6F, 0.2F, 4.5F, 3.4F);
            for (int r = 0; r < 4; r++) {
                float z = 1.6F + r * 1.9F;
                b.line(BONE, side * 5.75F, -3.4F, z, side * 5.9F, 2.6F, z + 0.9F, 0.8F);
            }
        }
        // the mane: spiky tufts of near-black fur bristling back from the neck and shoulders
        for (int i = 0; i <= 8; i++) {
            double ang = -Math.PI / 2 + (i - 4) * Math.PI / 8.5;
            float cx = (float) Math.cos(ang) * 6.6F, cy = (float) Math.sin(ang) * 7.5F - 2.5F;
            float ox = (float) Math.cos(ang) * 2.8F, oy = (float) Math.sin(ang) * 3.0F;
            b.line(WHITE, cx, cy, -11.0F, cx + ox, cy + oy - 1.0F, -5.5F, 2.2F);
            b.line(WHITE, cx * 0.95F, cy + 0.5F, -6.5F, cx * 1.05F + ox * 0.7F, cy + oy * 0.7F - 0.5F, -1.5F, 1.8F);
        }
        // the beard of fur under its throat
        for (int x = -1; x <= 1; x++) b.line(WHITE, x * 3.2F, 5.5F, -11.0F, x * 4.0F, 10.0F, -12.5F, 1.8F);
        // the spiked iron collar
        b.box(GREY, 0, 0, -7.6F, -8.0F, -12.4F, 15.2F, 1.4F, 2.0F);
        for (int side = -1; side <= 1; side += 2) b.box(GREY, 36, 0, side < 0 ? -7.6F : 6.2F, -6.6F, -12.4F, 1.4F, 4.6F, 2.0F);
        for (int i = 0; i < 3; i++) {
            float x = -4.0F + i * 4.0F;
            b.line(GREY, x, -8.0F, -11.4F, x, -10.6F, -12.2F, 0.9F);
        }
        for (int side = -1; side <= 1; side += 2) {
            b.line(GREY, side * 7.6F, -2.0F, -11.4F, side * 10.0F, -2.2F, -11.8F, 0.9F);
            // a length of broken chain hanging from it
            b.line(GREY, side * 7.4F, -2.2F, -11.6F, side * 8.4F, 3.6F, -10.8F, 0.8F);
            b.box(GREY, 0, 12, side * 8.5F - 0.7F, 3.4F, -11.1F, 1.4F, 1.6F, 0.6F);
        }
        // the ridge of bone spikes down its spine, biggest over the shoulders
        for (int i = 0; i < 7; i++) {
            float z = -9.0F + i * 3.3F;
            float top = i < 3 ? -10.0F : -4.5F;
            float len = i < 3 ? 6.0F - i * 0.6F : 3.6F - (i - 3) * 0.6F;
            b.line(BONE, 0.0F, top + 0.6F, z, 0.0F, top - len, z + 2.4F, 1.7F - i * 0.13F);
            if (i < 3) for (int side = -1; side <= 1; side += 2)
                b.line(BONE, side * 3.0F, top + 0.8F, z + 0.8F, side * 4.4F, top - len * 0.55F, z + 2.4F, 1.0F);
        }
        // tufts of fur along its back and flanks
        for (int i = 0; i < 4; i++) {
            float z = 1.0F + i * 2.8F;
            for (int side = -1; side <= 1; side += 2) b.line(CANVAS, side * 4.6F, -4.0F, z, side * 6.4F, -5.6F, z + 3.0F, 1.6F);
        }
        // a scar of fire across its shoulder (glow)
        b.line(FIRE, -7.05F, -5.0F, -9.0F, -7.05F, 1.0F, -3.0F, 0.45F);
        body.addOrReplaceChild("statics", b.statics, PartPose.ZERO);

        // ── the head: a wolf's skull under a mask of bone, eye holes burning, horns sweeping back, a mouthful of
        // fangs, fur bristling from its cheeks; the jaw hinges open on the fire in its throat ──
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, -12.0F));
        BoatKit h = new BoatKit(head);
        h.box(CANVAS, 0, 0, -5.0F, -5.0F, -8.0F, 10.0F, 9.0F, 8.0F);
        h.box(CANVAS, 36, 0, -3.0F, -1.5F, -15.0F, 6.0F, 4.0F, 7.0F);
        h.box(BLACK, 0, 0, -1.4F, -1.9F, -15.4F, 2.8F, 1.6F, 1.6F);
        h.box(BONE, 0, 0, -4.2F, -5.8F, -9.0F, 8.4F, 1.4F, 7.0F);
        h.box(BONE, 0, 9, -2.2F, -2.4F, -14.6F, 4.4F, 1.2F, 5.6F);
        h.box(BONE, 0, 18, -4.0F, -4.8F, -9.2F, 8.0F, 1.2F, 0.8F);
        for (int side = -1; side <= 1; side += 2) {
            // the mask: plates over the cheeks, dark eye holes, eyes burning in them
            h.box(BONE, 20, 18, side < 0 ? -5.3F : 4.4F, -4.6F, -8.8F, 0.9F, 3.0F, 4.0F);
            float ex = side < 0 ? -3.6F : 1.4F;
            h.box(BLACK, 0, 0, ex, -3.8F, -8.35F, 2.2F, 1.6F, 0.3F);
            h.box(NAV_RED, 0, 0, ex + 0.3F, -3.5F, -8.5F, 1.6F, 1.0F, 0.3F);
            // horns, swept back like a ram's
            h.line(BONE, side * 3.4F, -5.2F, -5.0F, side * 6.4F, -8.2F, -1.4F, 1.9F);
            h.line(BONE, side * 6.4F, -8.2F, -1.4F, side * 7.6F, -8.8F, 2.8F, 1.5F);
            h.line(BONE, side * 7.6F, -8.8F, 2.8F, side * 7.2F, -6.6F, 6.0F, 1.0F);
            // ears, flat back
            h.line(CANVAS, side * 3.0F, -4.4F, -2.0F, side * 3.6F, -8.4F, 0.6F, 2.0F);
            // fur bristling from the cheeks
            h.line(WHITE, side * 4.8F, -1.0F, -4.0F, side * 7.4F, 1.2F, 0.4F, 1.6F);
            h.line(WHITE, side * 4.6F, 1.5F, -3.0F, side * 6.6F, 3.8F, 0.6F, 1.4F);
            // upper fangs: long canines, a row of teeth behind
            h.line(BONE, side * 2.1F, 2.2F, -14.0F, side * 1.9F, 5.6F, -13.8F, 0.8F);
            h.line(BONE, side * 2.5F, 2.2F, -11.6F, side * 2.4F, 3.8F, -11.6F, 0.55F);
            h.line(BONE, side * 2.6F, 2.2F, -9.8F, side * 2.5F, 3.4F, -9.8F, 0.5F);
        }
        // the glow of its throat, seen when the jaw opens
        h.box(FIRE, 0, 0, -1.8F, 2.1F, -11.6F, 3.6F, 0.4F, 4.2F);
        head.addOrReplaceChild("statics", h.statics, PartPose.ZERO);
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 2.5F, -7.0F));
        BoatKit j = new BoatKit(jaw);
        j.box(ENGINE, 0, 0, -2.8F, 0.0F, -8.0F, 5.6F, 2.0F, 8.0F);
        j.box(WHITE, 0, 0, -3.4F, 0.8F, -3.0F, 6.8F, 2.4F, 3.5F);
        for (int side = -1; side <= 1; side += 2) {
            j.line(BONE, side * 1.8F, 0.2F, -7.4F, side * 1.7F, -2.2F, -7.3F, 0.7F);
            j.line(BONE, side * 2.2F, 0.2F, -5.0F, side * 2.1F, -1.2F, -5.0F, 0.5F);
        }
        for (int x = -1; x <= 1; x++) j.line(WHITE, x * 1.8F, 2.6F, -4.0F, x * 2.2F, 5.4F, -3.0F, 1.4F);
        jaw.addOrReplaceChild("statics", j.statics, PartPose.ZERO);

        // ── the tail: long and bristling, its tip smouldering like a coal ──
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.0F, 11.5F, 0.9F, 0, 0));
        BoatKit t = new BoatKit(tail);
        t.box(CANVAS, 0, 0, -1.9F, -1.9F, 0.0F, 3.8F, 3.8F, 7.0F);
        t.box(WHITE, 0, 0, -2.3F, -2.3F, 6.5F, 4.6F, 4.6F, 6.0F);
        for (int i = 0; i < 3; i++) {
            float z = 2.0F + i * 3.5F;
            t.line(WHITE, 0.0F, -2.0F, z, 0.0F, -4.4F, z + 2.6F, 1.4F);
            for (int side = -1; side <= 1; side += 2) t.line(WHITE, side * 2.0F, 0.0F, z, side * 3.8F, -0.6F, z + 2.8F, 1.3F);
        }
        t.box(FIRE, 0, 0, -1.2F, -1.2F, 12.3F, 2.4F, 2.4F, 2.4F);
        tail.addOrReplaceChild("statics", t.statics, PartPose.ZERO);

        // ── the legs: thick fur, tufts at the elbows, dark lower legs with veins of fire, huge paws, claws of bone ──
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2;
            int side = i % 2 == 0 ? -1 : 1;
            String name = (side < 0 ? "right_" : "left_") + (front ? "front" : "back");
            PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(),
                PartPose.offset(side * (front ? 4.8F : 4.0F), front ? FRONT_Y : BACK_Y, front ? -7.0F : 8.0F));
            BoatKit l = new BoatKit(leg);
            float foot = front ? 14.0F : 12.0F;
            if (front) {
                l.box(WHITE, 0, 0, -2.8F, -2.0F, -2.8F, 5.6F, 8.0F, 5.6F);
                l.line(WHITE, side * 1.6F, 4.0F, 2.0F, side * 2.6F, 7.5F, 4.4F, 1.6F);
                l.box(ENGINE, 0, 0, -1.8F, 6.0F, -1.8F, 3.6F, 6.5F, 3.6F);
                l.line(FIRE, side * 0.5F, 6.5F, -1.85F, side * 0.2F, 11.5F, -1.85F, 0.35F);
            } else {
                l.box(CANVAS, 0, 0, -2.8F, -3.0F, -3.5F, 5.6F, 8.0F, 6.5F);
                l.line(CANVAS, side * 1.8F, 3.5F, 2.4F, side * 2.6F, 6.5F, 4.8F, 1.6F);
                l.box(ENGINE, 0, 0, -1.6F, 4.5F, -0.6F, 3.2F, 5.8F, 3.2F);
                l.line(FIRE, side * 0.3F, 5.0F, -0.65F, side * 0.3F, 9.6F, -0.65F, 0.35F);
            }
            l.box(LEATHER, 0, 0, -2.4F, foot - 1.8F, -3.2F, 4.8F, 1.8F, 5.0F);
            for (int c = -1; c <= 1; c++) l.line(BONE, c * 1.5F, foot - 0.9F, -3.0F, c * 1.7F, foot - 0.2F, -4.8F, 0.6F);
            leg.addOrReplaceChild("statics", l.statics, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(BloodBossRenderer.State state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float a = state.actionTicks;
        this.head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F + 0.1F;
        // trotting: diagonal pairs
        float speed = state.frenzy ? 0.85F : 0.7F;
        float walk = state.walkAnimationPos * speed;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        this.rightFront.xRot = Mth.cos(walk) * 1.0F * amount;
        this.leftBack.xRot = Mth.cos(walk) * 1.0F * amount;
        this.leftFront.xRot = Mth.cos(walk + Mth.PI) * 1.0F * amount;
        this.rightBack.xRot = Mth.cos(walk + Mth.PI) * 1.0F * amount;
        this.body.y = 9.0F + 0.3F * Mth.sin(t * 0.12F) + 0.5F * Mth.abs(Mth.sin(walk)) * amount;
        this.body.xRot = 0.0F;
        this.body.zRot = 0.0F;
        this.jaw.xRot = 0.08F + 0.05F * Mth.sin(t * 0.15F) + (state.frenzy ? 0.25F : 0.0F);
        this.tail.xRot = 0.9F - 0.2F * amount + 0.05F * Mth.sin(t * 0.1F);
        this.tail.yRot = 0.25F * Mth.sin(t * 0.13F + walk * 0.5F);
        this.rightFront.y = FRONT_Y;
        this.leftFront.y = FRONT_Y;
        this.rightBack.y = BACK_Y;
        this.leftBack.y = BACK_Y;
        // the bite: the head lunges, the jaw snaps
        if (state.attack > 0.0F) {
            float snap = Mth.sin(state.attack * Mth.PI);
            this.head.xRot += 0.35F * snap;
            this.head.z = -12.0F - 2.0F * snap;
            this.jaw.xRot = 0.9F * Mth.sin(Math.min(1.0F, state.attack * 1.6F) * Mth.PI);
            this.body.xRot = 0.1F * snap;
        } else {
            this.head.z = -12.0F;
        }
        switch (state.action) {
            case Gorefang.POUNCE_CROUCH -> {
                // crouched low, head level, the hindquarters bunched
                float c = Math.min(1.0F, a / 6.0F);
                this.body.y += 3.0F * c;
                this.body.xRot = 0.08F * c;
                this.head.xRot = Mth.lerp(c, this.head.xRot, -0.1F);
                this.rightFront.xRot = -0.5F * c;
                this.leftFront.xRot = -0.5F * c;
                this.rightBack.xRot = 0.6F * c;
                this.leftBack.xRot = 0.6F * c;
                for (ModelPart leg : new ModelPart[]{this.rightFront, this.leftFront, this.rightBack, this.leftBack}) leg.y += 2.0F * c;
                this.jaw.xRot = 0.3F;
                this.tail.xRot = 1.2F + 0.1F * Mth.sin(t * 1.2F);
            }
            case Gorefang.POUNCE -> {
                // stretched out, forelegs reaching, jaw wide
                this.body.xRot = -0.25F;
                this.rightFront.xRot = -1.3F;
                this.leftFront.xRot = -1.1F;
                this.rightBack.xRot = 1.1F;
                this.leftBack.xRot = 1.2F;
                this.head.xRot = -0.1F;
                this.jaw.xRot = 0.85F;
                this.tail.xRot = 1.5F;
            }
            case Gorefang.CHARGE_WINDUP -> {
                // head down, a forepaw scraping the ground
                float c = Math.min(1.0F, a / 6.0F);
                this.head.xRot = Mth.lerp(c, this.head.xRot, 0.55F);
                this.body.xRot = 0.12F * c;
                this.body.y += 1.0F * c;
                this.rightFront.xRot = c * (0.3F + 0.6F * Mth.sin(a * 0.9F));
                this.leftFront.xRot = -0.2F * c;
                this.rightBack.xRot = 0.35F * c;
                this.leftBack.xRot = 0.35F * c;
                this.jaw.xRot = 0.35F;
                this.tail.xRot = 1.35F;
            }
            case Gorefang.CHARGE -> {
                // a flat-out gallop, head low
                float g = t * 1.1F;
                this.rightFront.xRot = Mth.cos(g) * 1.3F;
                this.leftFront.xRot = Mth.cos(g + 0.4F) * 1.3F;
                this.rightBack.xRot = Mth.cos(g + Mth.PI) * 1.3F;
                this.leftBack.xRot = Mth.cos(g + Mth.PI + 0.4F) * 1.3F;
                this.body.xRot = 0.08F * Mth.sin(g);
                this.body.y = 9.0F + 0.8F * Mth.abs(Mth.sin(g));
                this.head.xRot = 0.45F;
                this.jaw.xRot = 0.5F;
                this.tail.xRot = 1.5F;
            }
            case Gorefang.DAZED -> {
                // head lolling, swaying on its feet
                this.head.xRot = 0.5F + 0.15F * Mth.sin(t * 0.3F);
                this.head.zRot = 0.35F * Mth.sin(t * 0.25F);
                this.body.zRot = 0.08F * Mth.sin(t * 0.25F);
                this.jaw.xRot = 0.45F;
                this.tail.xRot = 0.4F;
            }
            case Gorefang.HOWL -> {
                // rears up, head thrown back, jaw wide
                float c = Math.min(1.0F, a / 8.0F);
                float fall = Math.max(0.0F, Math.min(1.0F, (a - 28.0F) / 8.0F));
                float r = c * (1.0F - fall);
                this.body.xRot = -0.4F * r;
                this.body.y -= 1.5F * r;
                this.head.xRot = Mth.lerp(r, this.head.xRot, -1.05F);
                this.head.yRot *= 1.0F - r;
                this.jaw.xRot = Mth.lerp(r, this.jaw.xRot, 0.9F);
                this.rightFront.xRot = -0.7F * r;
                this.leftFront.xRot = -0.5F * r;
                this.rightFront.y = FRONT_Y - 3.0F * r;
                this.leftFront.y = FRONT_Y - 3.0F * r;
                this.tail.xRot = 0.6F;
            }
            default -> {
            }
        }
        if (state.action != Gorefang.DAZED) this.head.zRot = 0.0F;
        // hit: it reels back
        float f = state.flinch * state.flinch;
        if (f > 0.0F) {
            this.body.xRot -= 0.15F * f;
            this.head.xRot -= 0.4F * f;
            this.jaw.xRot += 0.3F * f;
            this.rightFront.xRot -= 0.3F * f;
            this.leftFront.xRot -= 0.3F * f;
        }
    }
}

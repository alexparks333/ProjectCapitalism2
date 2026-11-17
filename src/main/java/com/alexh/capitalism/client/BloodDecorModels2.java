package com.alexh.capitalism.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.BloodDecorModels.*;
import static com.alexh.capitalism.client.BoatKit.*;

// The second set of Blood Moon decorations' models (decor/BloodDecorKind, Decorations.md > Blood Moon decorations): the
// grandfather clock, chandelier, pipe organ, secret bookshelf door, haunted portrait, iron maiden, altar, bat roost,
// canopy bed and fireplace. Built like client/BloodDecorModels (model pixels, y DOWN, y = 0 the ground under the block,
// the front +x; textures by tools/BloodDecorTex.cs).
public final class BloodDecorModels2 {

    private BloodDecorModels2() {
    }

    // a red candle with a flame (the flame its own part, for flickering and hiding when unlit): its foot at (x, y, z)
    static void candle(K k, PartDefinition root, String flameName, double x, double y, double z, double h, double r) {
        k.box(BAMBOO, x - r, y - h, z - r, 2 * r, h, 2 * r);
        k.box(BAMBOO, 20, 2, x + r - 0.05, y - h + 0.1, z - 0.25, 0.25, h * 0.45, 0.45);
        k.box(BLACK, x - 0.1, y - h - 0.5, z - 0.1, 0.2, 0.5, 0.2);
        CubeListBuilder flame = cubes();
        bx(flame, FIRE, -0.42, -1.3, -0.42, 0.84, 1.3, 0.84);
        BoatKit.box(flame, FIRE, 6, 2, -0.24F, -2.0F, -0.24F, 0.48F, 0.8F, 0.48F);
        BoatKit.box(flame, WARM, 0, 0, -0.12F, -2.5F, -0.12F, 0.24F, 0.6F, 0.24F);
        root.addOrReplaceChild(flameName, flame, at(x, y - h - 0.4, z));
    }

    static void flicker(ModelPart f, float t, int i, boolean on) {
        f.visible = on;
        f.yScale = 0.85F + 0.18F * Mth.sin(t * 0.55F + i * 1.9F) + 0.1F * Mth.sin(t * 1.37F + i * 0.7F);
        f.zRot = 0.08F * Mth.sin(t * 0.21F + i * 2.3F);
        f.xRot = 0.06F * Mth.sin(t * 0.17F + i * 1.1F);
    }

    // a small bat in flight (body, ears, red eyes, two wings that flap: flyBat), made in `root`
    static PartDefinition flyingBat(PartDefinition root, String name, Zone body, Zone wing) {
        CubeListBuilder b = cubes();
        bx(b, body, -1.1, -0.5, -0.45, 2.2, 1.0, 0.9);
        bx(b, body, 1.0, -0.65, -0.45, 1.0, 0.9, 0.9);
        bx(b, body, 1.3, -1.15, -0.4, 0.3, 0.6, 0.25);
        bx(b, body, 1.3, -1.15, 0.15, 0.3, 0.6, 0.25);
        bx(b, NAV_RED, 1.98, -0.45, -0.38, 0.06, 0.22, 0.22);
        bx(b, NAV_RED, 1.98, -0.45, 0.16, 0.06, 0.22, 0.22);
        PartDefinition bat = root.addOrReplaceChild(name, b, PartPose.ZERO);
        CubeListBuilder wl = cubes();
        bx(wl, wing, -1.0, -0.05, -3.8, 2.4, 0.1, 3.8);
        bat.addOrReplaceChild("wingL", wl, at(0, 0, -0.45));
        CubeListBuilder wr = cubes();
        bx(wr, wing, -1.0, -0.05, 0.0, 2.4, 0.1, 3.8);
        bat.addOrReplaceChild("wingR", wr, at(0, 0, 0.45));
        return bat;
    }

    // bat `i` circling round the middle at `radius`, about `height`, flapping
    static void flyBat(ModelPart bat, float t, int i, float radius, float height) {
        float a = t * (0.06F + i * 0.012F) + i * 2.1F;
        float r = radius + 2.0F * Mth.sin(t * 0.021F + i);
        bat.x = Mth.cos(a) * r;
        bat.z = Mth.sin(a) * r;
        bat.y = height + 3.0F * Mth.sin(a * 2.0F + i);
        bat.yRot = -a - Mth.HALF_PI;
        bat.zRot = 0.3F * Mth.cos(a * 2.0F);
        float flap = 0.9F * Mth.sin(t * 1.4F + i * 2.0F);
        bat.getChild("wingL").xRot = flap;
        bat.getChild("wingR").xRot = -flap;
    }

    // a bat wing picture (two sides) as a flat part spreading out from (x, y, z), turned `yaw`
    static PartDefinition wing(PartDefinition root, String name, int u, int v, float w, float h, double x, double y, double z, double yaw, double tilt) {
        CubeListBuilder c = cubes();
        sheet(c, u, v, 0.0F, 0.0F, 0.0F, w, h, 0.0F);
        return root.addOrReplaceChild(name, c, at(x, y, z, tilt, yaw, 0));
    }

    // ═════════════ the Gothic Grandfather Clock (about 2.5 blocks tall) ═════════════
    // A tall clock case of black carved wood on clawed gold feet. Through its glass door (gold glazing bars) you see a
    // dark velvet back, two brass weights on chains and the brass pendulum with a ruby in its bob, swinging. The hood holds
    // a blood-moon dial (gold numerals round a crimson moon) whose hands show the real in-game time; twisted gold columns
    // either side, a pointed gothic pediment with spires and, at its peak, a gold crescent with bat wings and a ruby.
    public static class Clock extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("grandfather_clock");
        // the dial picture (tools/BloodDecorTex.cs): 9 x 9 at (DIAL_U, DIAL_V), on the hood's front
        static final int DIAL_U = 80, DIAL_V = 190;
        static final float DIAL_Y = -31.5F;
        private final ModelPart pendulum;
        private final ModelPart hourHand;
        private final ModelPart minuteHand;

        public Clock(ModelPart root) {
            super(root);
            this.pendulum = this.decor.getChild("pendulum");
            this.hourHand = this.decor.getChild("hour");
            this.minuteHand = this.decor.getChild("minute");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            // a slow tick-tock: two seconds a swing
            this.pendulum.xRot = 0.22F * Mth.sin(t * Mth.PI / 40.0F);
            this.hourHand.xRot = -s.x();
            this.minuteHand.xRot = -s.y();
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the plinth, its gold trim and clawed feet
            k.box(DARKWOOD, 0, 0, -5.2, -3.6, -6.2, 10.4, 3.0, 12.4);
            k.box(DARKWOOD, 30, 0, -4.8, -4.4, -5.8, 9.6, 0.8, 11.6);
            k.box(GOLD, 5.2, -3.3, -6.0, 0.25, 0.35, 12.0);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    k.box(BRASS, sx * 4.7 - 0.8, -0.8, sz * 5.7 - 0.8, 1.6, 0.8, 1.6);
                    k.line(BRASS, sx * 4.7, -0.7, sz * 5.7, sx * 5.6, -0.1, sz * 6.3, 0.4);
                }
            }
            // the trunk: back, sides, the door frame with its glazing; inside, velvet
            k.box(DARKWOOD, 0, 16, -4.2, -24.5, -4.6, 1.2, 20.1, 9.2);
            for (int s = -1; s <= 1; s += 2) {
                k.box(DARKWOOD, 22, 0, -4.2, -24.5, s > 0 ? 3.6 : -4.6, 8.0, 20.1, 1.0);
                k.box(OLDWOOD, 3.4, -24.0, s > 0 ? 2.9 : -3.9, 0.9, 19.2, 1.0);
                k.box(GOLD, 4.3, -23.6, s > 0 ? 3.15 : -3.45, 0.2, 18.4, 0.3);
            }
            k.box(OLDWOOD, 0, 0, 3.4, -24.4, -3.0, 0.9, 1.0, 6.0);
            k.box(OLDWOOD, 0, 4, 3.4, -5.4, -3.0, 0.9, 1.0, 6.0);
            k.box(OLDWOOD, 10, 8, 3.5, -11.4, -3.0, 0.7, 0.5, 6.0);
            k.box(GOLD, 3.7, -24.0, -0.1, 0.2, 18.8, 0.2);
            k.box(GOLD, 3.7, -15.0, -2.9, 0.2, 0.2, 5.8);
            // (the glazing's pointed arch at the top of the door)
            k.line(GOLD, 3.8, -20.0, -2.9, 3.8, -23.2, 0, 0.22);
            k.line(GOLD, 3.8, -20.0, 2.9, 3.8, -23.2, 0, 0.22);
            k.box(CANVAS, 0, 0, -3.1, -23.6, -3.6, 0.4, 18.4, 7.2);
            // the weights on their chains
            for (int s = -1; s <= 1; s += 2) {
                k.line(ENGINE, -1.2, -23.6, s * 1.9, -1.2, -15.8, s * 1.9, 0.18);
                k.box(BRASS, -1.8, -15.8, s * 1.9 - 0.6, 1.2, 4.0, 1.2);
                k.box(GOLD, -1.9, -15.9, s * 1.9 - 0.7, 1.4, 0.3, 1.4);
            }
            // the pendulum: a gold rod and its bob with a ruby, swinging
            PartDefinition pend = root.addOrReplaceChild("pendulum", cubes(), at(1.4, -24.0, 0));
            BoatKit.line(pend, "rod", BRASS, 0.0F, 0.0F, 0.0F, 0.0F, 13.6F, 0.0F, 0.32F);
            CubeListBuilder bob = cubes();
            bx(bob, BRASS, -0.4, 13.2, -1.6, 0.8, 3.2, 3.2);
            bx(bob, BRASS, -0.42, 13.8, -2.0, 0.84, 2.0, 4.0);
            bx(bob, NAV_RED, 0.38, 14.3, -0.5, 0.12, 1.0, 1.0);
            pend.addOrReplaceChild("bob", bob, PartPose.ZERO);
            // the hood: its case, the dial, twisted gold columns, a moulded top
            k.box(DARKWOOD, 2, 0, -5.0, -37.0, -5.8, 10.0, 12.5, 11.6);
            k.box(DARKWOOD, 40, 20, -5.4, -25.4, -6.2, 10.8, 1.0, 12.4);
            k.box(GOLD, 5.0, -25.2, -6.0, 0.3, 0.3, 12.0);
            k.box(OLDWOOD, 4.9, -36.4, -5.2, 0.3, 10.4, 10.4);
            CubeListBuilder dial = cubes();
            front(dial, DIAL_U - 9, DIAL_V - 9, 0.0F, -4.5F, -4.5F, 9.0F, 9.0F);
            root.addOrReplaceChild("dial", dial, at(5.25, DIAL_Y, 0));
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 6; i++) {
                    double y0 = -26.0 - i * 1.7, y1 = y0 - 1.7;
                    k.line(BRASS, 5.0, y0, s * (5.3 + ((i % 2) * 0.25)), 5.0, y1, s * (5.3 + (((i + 1) % 2) * 0.25)), 0.55);
                }
            }
            // hands (turned round the dial's middle): the hour hand short and broad, the minute hand long, both gold
            PartDefinition hour = root.addOrReplaceChild("hour", cubes(), at(5.35, DIAL_Y, 0));
            CubeListBuilder hh = cubes();
            bx(hh, BLACK, 0.0, -2.6, -0.35, 0.12, 2.9, 0.7);
            bx(hh, GOLD, 0.05, -2.9, -0.2, 0.12, 0.6, 0.4);
            hour.addOrReplaceChild("hand", hh, PartPose.ZERO);
            PartDefinition minute = root.addOrReplaceChild("minute", cubes(), at(5.47, DIAL_Y, 0));
            CubeListBuilder mh = cubes();
            bx(mh, BLACK, 0.0, -3.9, -0.22, 0.12, 4.3, 0.44);
            bx(mh, GOLD, 0.05, -0.35, -0.35, 0.12, 0.7, 0.7);
            minute.addOrReplaceChild("hand", mh, PartPose.ZERO);
            // the pediment: a pointed arch with gold tracing, spires, the crest
            k.box(DARKWOOD, 6, 6, -5.4, -38.0, -6.2, 10.8, 1.0, 12.4);
            double[][] steps = {{-39.0, 5.4}, {-40.0, 4.2}, {-41.0, 3.0}, {-42.0, 1.8}};
            for (int i = 0; i < steps.length; i++) k.box(DARKWOOD, 10 + i * 4, 10, -4.6, steps[i][0], -steps[i][1], 8.6, 1.0, 2 * steps[i][1]);
            k.line(BRASS, 4.2, -38.0, -5.6, 4.2, -42.6, 0, 0.4);
            k.line(BRASS, 4.2, -38.0, 5.6, 4.2, -42.6, 0, 0.4);
            for (int s = -1; s <= 1; s += 2) {
                k.box(DARKWOOD, 30, 20, -0.7, -40.4, s * 5.6 - 0.7, 1.4, 2.4, 1.4);
                k.box(GOLD, -0.3, -41.6, s * 5.6 - 0.3, 0.6, 1.2, 0.6);
            }
            crescent(k, BRASS, 3.0, -44.6, 1.6, 0.6);
            k.cube(NAV_RED, 3.0, -44.0, 0, 0.8);
            for (int s = -1; s <= 1; s += 2) {
                k.line(BRASS, 3.0, -44.2, s * 1.0, 3.0, -45.6, s * 3.0, 0.35);
                k.line(BRASS, 3.0, -45.6, s * 3.0, 3.0, -44.4, s * 3.6, 0.3);
                k.line(BRASS, 3.0, -44.4, s * 3.6, 3.0, -43.6, s * 2.2, 0.28);
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Crimson Chandelier (hangs from a ceiling) ═════════════
    // From a gold ceiling rose a black iron chain drops to a column with gold knops. Two wrought iron rings hang from it on
    // S-curled arms: eight red candles on the wide ring, four on the small one, their flames flickering (only while lit).
    // Blood crystal drops hang all round the wide ring and swags of crystal beads loop between the arms; a long crystal
    // drop with a ruby hangs from its bottom.
    public static class Chandelier extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_chandelier");
        static final int BIG = 8, SMALL = 4;
        private final List<ModelPart> flames = new ArrayList<>();
        private final ModelPart sway;

        public Chandelier(ModelPart root) {
            super(root);
            this.sway = this.decor.getChild("sway");
            for (int i = 0; i < BIG + SMALL; i++) this.flames.add(this.sway.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            this.sway.xRot = 0.012F * Mth.sin(t * 0.03F);
            this.sway.zRot = 0.01F * Mth.sin(t * 0.023F + 1.0F);
            for (int i = 0; i < this.flames.size(); i++) flicker(this.flames.get(i), t, i, s.lit());
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K top = new K(root);
            top.k.disc(BRASS, 0.0F, -16.0F, 0.0F, 2.4F, 0.6F);
            top.box(GOLD, -1.0, -15.4, -1.0, 2.0, 0.5, 2.0);
            top.done();
            // everything below the rose sways a hair
            PartDefinition sway = root.addOrReplaceChild("sway", cubes(), at(0, -15.0, 0));
            K k = new K(sway);
            // the chain (links turned alternately), the column, gold knops, the bottom drop
            for (int i = 0; i < 5; i++) {
                double y = 0.2 + i * 1.0;
                if (i % 2 == 0) k.box(ENGINE, i * 2, 0, -0.5, y, -0.15, 1.0, 1.1, 0.3);
                else k.box(ENGINE, i * 2, 4, -0.15, y, -0.5, 0.3, 1.1, 1.0);
            }
            k.box(BRASS, -0.9, 5.2, -0.9, 1.8, 0.7, 1.8);
            k.box(ENGINE, 0, 8, -0.7, 5.9, -0.7, 1.4, 5.0, 1.4);
            k.box(BRASS, -1.3, 7.4, -1.3, 2.6, 0.8, 2.6);
            k.box(BRASS, -1.6, 10.6, -1.6, 3.2, 1.0, 3.2);
            k.box(ENGINE, 4, 12, -1.1, 11.6, -1.1, 2.2, 1.0, 2.2);
            k.box(BRASS, -0.6, 12.6, -0.6, 1.2, 0.6, 1.2);
            k.box(ICE, 0, 0, -0.45, 13.2, -0.45, 0.9, 1.4, 0.9);
            k.box(ICE, 4, 0, -0.25, 14.6, -0.25, 0.5, 0.7, 0.5);
            // the rings
            k.k.ringY(ENGINE, 0.0F, 9.4F, 0.0F, 7.0F, 0.6F, 0.6F, 16, 0);
            k.k.ringY(BRASS, 0.0F, 10.0F, 0.0F, 7.05F, 0.2F, 0.4F, 16, 0);
            k.k.ringY(ENGINE, 0.0F, 6.6F, 0.0F, 4.0F, 0.5F, 0.5F, 12, 0);
            // arms out to the wide ring: an S-curl each
            for (int i = 0; i < BIG; i++) {
                double a = i * 2 * Math.PI / BIG, c = Math.cos(a), s = Math.sin(a);
                k.line(ENGINE, c * 1.2, 10.8, s * 1.2, c * 3.6, 11.4, s * 3.6, 0.45);
                k.line(ENGINE, c * 3.6, 11.4, s * 3.6, c * 5.8, 10.6, s * 5.8, 0.42);
                k.line(ENGINE, c * 5.8, 10.6, s * 5.8, c * 6.7, 9.6, s * 6.7, 0.4);
                k.cube(BRASS, c * 3.6, 11.4, s * 3.6, 0.6);
                // the candle on its cup
                k.box(BRASS, c * 6.7 - 0.9, 9.0, s * 6.7 - 0.9, 1.8, 0.4, 1.8);
                candle(k, sway, "flame" + i, c * 6.7, 9.0, s * 6.7, 3.0, 0.55);
                // crystal drops hanging between the candles
                double a2 = a + Math.PI / BIG, c2 = Math.cos(a2), s2 = Math.sin(a2);
                k.line(GOLD, c2 * 6.9, 10.0, s2 * 6.9, c2 * 6.9, 11.6, s2 * 6.9, 0.12);
                k.cube(ICE, c2 * 6.9, 12.1, s2 * 6.9, 0.7);
                k.box(ICE, 8, 0, c2 * 6.9 - 0.2, 12.5, s2 * 6.9 - 0.2, 0.4, 0.6, 0.4);
                // a swag of crystal beads looping from this arm to the next
                for (int b = 1; b < 5; b++) {
                    double f = b / 5.0, ab = a + f * 2 * Math.PI / BIG;
                    double r = 5.2, sag = 1.6 * Math.sin(f * Math.PI);
                    k.cube(ICE, Math.cos(ab) * r, 11.2 + sag, Math.sin(ab) * r, 0.42);
                }
            }
            for (int i = 0; i < SMALL; i++) {
                double a = Math.PI / 4 + i * 2 * Math.PI / SMALL, c = Math.cos(a), s = Math.sin(a);
                k.line(ENGINE, c * 0.8, 7.6, s * 0.8, c * 3.8, 6.8, s * 3.8, 0.38);
                k.box(BRASS, c * 4.0 - 0.75, 6.2, s * 4.0 - 0.75, 1.5, 0.4, 1.5);
                candle(k, sway, "flame" + (BIG + i), c * 4.0, 6.2, s * 4.0, 2.6, 0.5);
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Blood Pipe Organ (2 x 2 blocks, 3 tall) ═════════════
    // A great organ of black carved wood: a console with two manuals of bone and ebony keys, a music stand with an open book,
    // a candle at either end; behind, a rank of tall dark silver pipes with gold mouths and collars, tallest in the middle
    // and rising again into towers at the ends, against a crimson velvet back. Spires top the towers; over the middle a
    // round blood-moon window glows, and great carved bat wings spread from it. While it plays its keys dance.
    public static class Organ extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_pipe_organ");
        static final int WING_U = 0, WING_V = 180;
        static final int KEYS = 14;
        private final List<ModelPart> keys = new ArrayList<>();
        private final List<ModelPart> flames = new ArrayList<>();

        public Organ(ModelPart root) {
            super(root);
            for (int i = 0; i < KEYS; i++) this.keys.add(this.decor.getChild("key" + i));
            for (int i = 0; i < 2; i++) this.flames.add(this.decor.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            for (int i = 0; i < KEYS; i++) {
                boolean down = s.lit() && Mth.sin(t * 0.7F + i * 2.7F) + Mth.sin(t * 0.31F + i * 1.3F) > 1.1F;
                this.keys.get(i).y = down ? 0.25F : 0.0F;
            }
            for (int i = 0; i < 2; i++) flicker(this.flames.get(i), t, i, true);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the console in two halves, a gold-trimmed plinth
            for (int s = -1; s <= 1; s += 2) {
                k.box(DARKWOOD, s > 0 ? 0 : 30, 0, -8.0, -12.0, s > 0 ? 0 : -15.0, 13.0, 12.0, 15.0);
                k.box(OLDWOOD, s > 0 ? 0 : 30, 0, 4.9, -10.0, s > 0 ? 1.0 : -14.0, 0.5, 8.0, 13.0);
            }
            k.box(GOLD, 5.0, -1.6, -15.0, 0.3, 0.4, 30.0);
            k.box(GOLD, 5.0, -11.6, -15.0, 0.3, 0.4, 30.0);
            // the key bed and two manuals: bone keys, ebony keys, stepped
            k.box(OLDWOOD, 0, 20, 4.0, -13.0, -11.0, 4.6, 1.0, 22.0);
            k.box(OLDWOOD, 20, 20, 2.0, -14.6, -11.0, 3.0, 1.6, 22.0);
            k.box(BONE, 0, 0, 5.0, -13.4, -10.4, 3.4, 0.4, 20.8);
            k.box(BONE, 0, 8, 2.6, -15.0, -10.4, 2.6, 0.4, 20.8);
            for (int i = 0; i < 20; i++) {
                if (i % 7 == 2 || i % 7 == 6) continue;
                k.box(BLACK, 6.2, -13.75, -10.0 + i * 1.04, 2.0, 0.35, 0.55);
                k.box(BLACK, 3.6, -15.35, -10.0 + i * 1.04, 1.5, 0.35, 0.55);
            }
            for (int i = 0; i < KEYS; i++) {
                CubeListBuilder key = cubes();
                bx(key, BONE, 0, 0, 0, 3.0, 0.3, 0.85);
                root.addOrReplaceChild("key" + i, key, at(5.3, -13.72, -9.6 + i * 1.45));
            }
            for (int s = -1; s <= 1; s += 2) {
                k.box(DARKWOOD, 40, 0, 2.0, -16.0, s > 0 ? 11.0 : -12.4, 6.6, 3.0, 1.4);
                k.box(BRASS, 7.0, -16.4, s * 11.7 - 0.6, 1.2, 0.4, 1.2);
                candle(k, root, "flame" + (s > 0 ? 1 : 0), 7.6, -16.4, s * 11.7, 3.2, 0.5);
            }
            // the music stand and its open book
            k.box(DARKWOOD, 36, 20, 0.6, -21.0, -4.0, 0.8, 5.0, 8.0);
            k.box(WHITE, 1.4, -20.6, -3.6, 0.3, 4.2, 3.5);
            k.box(WHITE, 1.4, -20.6, 0.1, 0.3, 4.2, 3.5);
            k.box(BLACK, 1.72, -19.8, -3.2, 0.05, 0.25, 2.6).box(BLACK, 1.72, -18.8, -3.2, 0.05, 0.25, 2.2);
            k.box(BLACK, 1.72, -19.8, 0.5, 0.05, 0.25, 2.6).box(BLACK, 1.72, -18.8, 0.5, 0.05, 0.25, 2.2);
            k.box(RED, 1.45, -20.8, -0.15, 0.3, 4.6, 0.3);
            // the pipe case: velvet back, a frame of black wood
            for (int s = 0; s < 2; s++) {
                k.box(CANVAS, 0, 0, -8.0, -42.0, s == 0 ? -15.0 : 0.0, 1.0, 15.0, 15.0);
                k.box(CANVAS, 30, 0, -8.0, -27.0, s == 0 ? -15.0 : 0.0, 1.0, 15.0, 15.0);
            }
            for (int s = -1; s <= 1; s += 2) {
                k.box(DARKWOOD, 50, 0, -8.0, -46.0, s > 0 ? 14.0 : -16.0, 4.0, 34.0, 2.0);
                k.box(DARKWOOD, 50, 0, -7.0, -50.0, s > 0 ? 13.6 : -15.6, 2.0, 4.0, 2.0);
                k.box(GOLD, -6.5, -51.4, s * 14.6 - 0.5, 1.0, 1.4, 1.0);
                k.box(GOLD, -6.3, -52.6, s * 14.6 - 0.3, 0.6, 1.2, 0.6);
            }
            // the pipes: tall in the middle, falling away, rising into towers at the ends
            int n = 13;
            for (int i = 0; i < n; i++) {
                double z = -12.6 + i * 2.1;
                double f = Math.abs(i - (n - 1) / 2.0) / ((n - 1) / 2.0);
                double h = 30.0 - 16.0 * Math.sin(f * Math.PI / 2) + (f > 0.8 ? 10.0 * (f - 0.8) / 0.2 : 0.0);
                double x = -5.6 + (i % 2) * 1.2;
                double y0 = -12.0;
                double top = y0 - h;
                double mid = y0 - Math.min(h, 20.0);
                k.box(STEEL, (i * 5) % 40, 0, x - 0.85, mid, z - 0.85, 1.7, y0 - mid, 1.7);
                if (top < mid) k.box(STEEL, (i * 5 + 3) % 40, 4, x - 0.8, top, z - 0.8, 1.6, mid - top, 1.6);
                k.box(BRASS, x - 1.0, y0 - 3.4, z - 1.0, 2.0, 0.5, 2.0);
                k.box(BLACK, x + 0.82, y0 - 5.6, z - 0.45, 0.06, 1.6, 0.9);
                k.box(GOLD, x + 0.8, y0 - 6.0, z - 0.5, 0.12, 0.3, 1.0);
                k.box(STEEL, 20, 20, x - 0.55, top - 1.2, z - 0.55, 1.1, 1.2, 1.1);
                k.box(STEEL, 26, 20, x - 0.3, top - 2.0, z - 0.3, 0.6, 0.8, 0.6);
            }
            // the rose window, glowing, its gold tracery; the wings spreading from it
            k.k.disc(WARM, -6.6F, -44.0F, 0.0F, 3.4F, 0.6F);
            k.box(DARKWOOD, 44, 24, -7.6, -48.6, -4.2, 1.2, 9.4, 8.4);
            CubeListBuilder window = cubes();
            bx(window, WARM, 0.0, -3.4, -3.4, 0.3, 6.8, 6.8);
            root.addOrReplaceChild("window", window, at(-6.25, -44.0, 0, Math.PI / 4, 0, 0));
            CubeListBuilder window2 = cubes();
            bx(window2, WARM, 0.0, -3.0, -3.0, 0.32, 6.0, 6.0);
            root.addOrReplaceChild("window2", window2, at(-6.22, -44.0, 0));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                k.line(BRASS, -5.85, -44.0, 0, -5.85, -44.0 + Math.sin(a) * 4.2, Math.cos(a) * 4.2, 0.3);
            }
            for (int s = -1; s <= 1; s += 2) {
                wing(root, "wing" + (s > 0 ? "R" : "L"), WING_U, WING_V, 16.0F, 15.0F, -6.4, -53.0, s * 3.0, s > 0 ? -1.36 : 1.36, 0.0);
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Secret Bookshelf Door (1 x 2 blocks) ═════════════
    // A tall gothic bookshelf of black wood, crowded with old leather books (crimson, green, black, brown, plum, gold
    // titles), a skull bookend and a candle stub on one shelf, a carved pointed arch over the top shelf. One red book
    // sticks out. Opened, the whole shelf swings inward on a hinge at its side (the state: how far open).
    public static class BookshelfDoor extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("secret_bookshelf_door");
        private final ModelPart door;

        public BookshelfDoor(ModelPart root) {
            super(root);
            this.door = this.decor.getChild("door");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float o = Mth.clamp(s.open(), 0.0F, 1.0F);
            this.door.yRot = -Mth.HALF_PI * (o * o * (3 - 2 * o));
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            // all of it hangs on the hinge at its back left edge (x 3, z -8)
            PartDefinition door = root.addOrReplaceChild("door", cubes(), at(3.0, 0, -8.0));
            K k = new K(door);
            // (in the door's frame: x 0 to 5 front, z 0 to 16, y 0 to -32)
            k.box(DARKWOOD, 0, 0, 0.0, -31.0, 0.6, 0.8, 30.0, 14.8);
            for (int s = 0; s < 2; s++) k.box(DARKWOOD, 32, 0, 0.0, -32.0, s == 0 ? 0.0 : 15.0, 5.0, 32.0, 1.0);
            k.box(DARKWOOD, 0, 20, 0.0, -32.0, 1.0, 5.0, 1.2, 14.0);
            k.box(DARKWOOD, 20, 20, 0.0, -1.2, 1.0, 5.0, 1.2, 14.0);
            double[] shelves = {-8.6, -16.4, -24.2};
            for (double y : shelves) {
                k.box(DARKWOOD, 40, 20, 0.0, y, 1.0, 5.0, 0.9, 14.0);
                k.box(GOLD, 4.95, y + 0.25, 1.0, 0.12, 0.25, 14.0);
            }
            // the carved arch over the top shelf, gold tracing; carved side posts
            k.line(DARKWOOD, 4.6, -24.0, 1.4, 4.6, -30.6, 8.0, 1.2);
            k.line(DARKWOOD, 4.6, -24.0, 14.6, 4.6, -30.6, 8.0, 1.2);
            k.line(GOLD, 5.1, -24.4, 1.6, 5.1, -30.2, 8.0, 0.25);
            k.line(GOLD, 5.1, -24.4, 14.4, 5.1, -30.2, 8.0, 0.25);
            k.cube(NAV_RED, 5.15, -30.2, 8.0, 0.6);
            for (int s = 0; s < 2; s++) {
                double z = s == 0 ? 0.5 : 15.5;
                for (int i = 0; i < 4; i++) k.box(OLDWOOD, 4.9, -30.0 + i * 8.0, z - 0.4, 0.3, 4.0, 0.8);
            }
            // books on each shelf: heights and widths varied, now and then one leaning
            Zone[] covers = {RED, NAVY, LEATHER, ACCENT, BLACK, LEATHER, RED, NAVY};
            double[] bottoms = {-1.2, -9.5, -17.3};
            for (int sh = 0; sh < 3; sh++) {
                double z = 1.2;
                int i = sh * 5;
                while (z < 14.2) {
                    double w = 0.8 + ((i * 37) % 7) * 0.12;
                    double h = 4.8 + ((i * 53) % 9) * 0.25;
                    if (z + w > 14.8) break;
                    Zone c = covers[(i * 3 + sh) % covers.length];
                    boolean skip = sh == 1 && z > 9.0 && z < 12.6;   // the gap for the skull and candle
                    if (!skip) {
                        if ((i * 7) % 11 == 3 && z < 12.0) {
                            CubeListBuilder lean = cubes();
                            bx(lean, c, 0.0, -h, 0.0, 3.6, h, w);
                            k.part(lean, 0.9, bottoms[sh], z + 0.4, 0.28, 0, 0);
                            z += w + 1.0;
                        } else {
                            k.box(c, (i * 3) % 12, 0, 0.9, bottoms[sh] - h, z, 3.6, h, w);
                            z += w + 0.06;
                        }
                    } else z += 0.6;
                    i++;
                }
            }
            // the skull bookend and a candle stub
            k.box(BONE, 1.6, -12.4, 9.6, 2.6, 2.9, 2.4);
            k.box(BONE, 2.2, -10.4, 10.0, 2.2, 0.9, 1.6);
            k.box(BLACK, 4.22, -11.8, 9.9, 0.05, 0.8, 0.7).box(BLACK, 4.22, -11.8, 11.0, 0.05, 0.8, 0.7);
            k.box(BAMBOO, 2.4, -11.6, 12.6, 1.1, 2.1, 1.1);
            k.box(BAMBOO, 20, 4, 2.2, -9.75, 12.4, 1.5, 0.3, 1.5);
            // the red book that sticks out (pull it!)
            CubeListBuilder lever = cubes();
            bx(lever, RED, 0.0, -5.6, 0.0, 5.4, 5.6, 1.3);
            bx(lever, GOLD, 5.42, -5.0, 0.15, 0.08, 0.35, 1.0);
            bx(lever, GOLD, 5.42, -1.0, 0.15, 0.08, 0.35, 1.0);
            k.part(lever, 0.9, -9.5, 6.0, 0, 0, -0.12);
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Haunted Portrait (on a wall, 1 x 2) ═════════════
    // The Blood Merchant in oils, in an ornate gold frame (a bead inside, rosettes in its corners, a bat crest on top). His
    // eyes follow you: the pupils shift toward the nearest player; at night they glow red. (Its back, -x, is to the wall.)
    public static class Portrait extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("haunted_portrait");
        // the painting: 11 x 26 at (PIC_U, PIC_V)
        static final int PIC_U = 20, PIC_V = 200;
        // where his eyes are in the painting (from its middle: z across, y down)
        static final float EYE_Y = -21.2F, EYE_Z = 1.25F;
        private final ModelPart[] pupils = new ModelPart[2];
        private final ModelPart[] glows = new ModelPart[2];

        public Portrait(ModelPart root) {
            super(root);
            for (int i = 0; i < 2; i++) {
                this.pupils[i] = this.decor.getChild("pupil" + i);
                this.glows[i] = this.decor.getChild("glow" + i);
            }
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            for (int i = 0; i < 2; i++) {
                float z = (i == 0 ? -EYE_Z : EYE_Z) + s.x() * 0.32F;
                float y = EYE_Y + s.y() * 0.18F;
                this.pupils[i].z = z;
                this.pupils[i].y = y;
                this.glows[i].z = z;
                this.glows[i].y = y;
                this.glows[i].visible = s.z() > 0.5F;
                this.pupils[i].visible = s.z() <= 0.5F;
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the frame: a deep gold moulding, a bead inside, rosettes in the corners
            k.box(DARKWOOD, 0, 0, -8.0, -31.0, -7.0, 0.6, 30.0, 14.0);
            for (int s = -1; s <= 1; s += 2) {
                k.box(BRASS, s > 0 ? 0 : 4, 0, -7.6, -31.0, s > 0 ? 5.2 : -7.0, 1.0, 30.0, 1.8);
                k.box(GOLD, -6.75, -29.6, s > 0 ? 5.0 : -5.3, 0.3, 27.2, 0.3);
            }
            k.box(BRASS, 8, 0, -7.6, -31.0, -5.2, 1.0, 1.8, 10.4);
            k.box(BRASS, 8, 4, -7.6, -2.8, -5.2, 1.0, 1.8, 10.4);
            k.box(GOLD, -6.75, -29.6, -5.0, 0.3, 0.3, 10.0);
            k.box(GOLD, -6.75, -2.7, -5.0, 0.3, 0.3, 10.0);
            for (int sy = 0; sy < 2; sy++) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    double y = sy == 0 ? -30.1 : -1.9, z = sz * 6.1;
                    k.cube(BRASS, -6.5, y, z, 1.5);
                    k.cube(NAV_RED, -5.8, y, z, 0.5);
                }
            }
            // the crest: a gold bat on top, ruby-eyed
            k.box(BRASS, -7.5, -33.0, -1.0, 1.2, 2.2, 2.0);
            for (int s = -1; s <= 1; s += 2) {
                k.line(BRASS, -7.0, -32.0, s * 0.8, -7.0, -33.6, s * 3.6, 0.45);
                k.line(BRASS, -7.0, -33.6, s * 3.6, -7.0, -31.6, s * 4.4, 0.35);
                k.line(BRASS, -7.0, -31.6, s * 4.4, -7.0, -31.9, s * 2.2, 0.3);
                k.line(BRASS, -7.0, -33.0, s * 0.4, -7.0, -34.0, s * 0.7, 0.3);
            }
            // the painting
            CubeListBuilder pic = cubes();
            front(pic, PIC_U - 11, PIC_V - 11, 0.0F, -26.0F, -5.5F, 26.0F, 11.0F);
            root.addOrReplaceChild("painting", pic, at(-7.1, -2.5, 0));
            // his pupils (they move), and their red glow by night
            for (int i = 0; i < 2; i++) {
                CubeListBuilder p = cubes();
                bx(p, BLACK, 0, -0.28, -0.28, 0.06, 0.56, 0.56);
                root.addOrReplaceChild("pupil" + i, p, at(-7.02, EYE_Y, i == 0 ? -EYE_Z : EYE_Z));
                CubeListBuilder g = cubes();
                bx(g, NAV_RED, 0, -0.32, -0.32, 0.07, 0.64, 0.64);
                root.addOrReplaceChild("glow" + i, g, at(-7.01, EYE_Y, i == 0 ? -EYE_Z : EYE_Z));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Iron Maiden (1 x 2) ═════════════
    // A classic iron maiden on a stone plinth: a black iron casket shaped like a robed woman, riveted bands and gold
    // trim, a crowned head with a stern iron face on its door. It's hollow: a thin iron shell round a person-shaped space
    // lined with dark red velvet, old bloodstains on it, rows of iron spikes pointing in from its back wall and from the
    // inside of its door. The door, hinged at its side, swings open while someone has its chest open or while it's armed,
    // and slams shut on a victim (the state: how far open).
    public static class IronMaiden extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("iron_maiden");
        // the face picture: 6 x 8 at (FACE_U, FACE_V)
        static final int FACE_U = 40, FACE_V = 190;
        // its outline: each band's top (y) and half width, from the plinth up to its crown
        static final double[][] BANDS = {{-1.5, 5.6}, {-6.0, 5.4}, {-10.5, 4.9}, {-15.0, 4.4}, {-19.0, 4.6}, {-23.0, 5.0}, {-25.5, 4.4},
            {-27.5, 3.2}, {-30.0, 2.6}};
        // its back is at x -6, its door's outside at x 5; the shell 1 px thick
        static final double BACK = -6.0, FRONT = 5.0, WALL = 1.0;
        private final ModelPart door;

        public IronMaiden(ModelPart root) {
            super(root);
            this.door = this.decor.getChild("door");
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float o = Mth.clamp(s.open(), 0.0F, 1.0F);
            this.door.yRot = 1.85F * (o * o * (3 - 2 * o));
        }

        // the half width at height y (between its bands)
        static double halfAt(double y) {
            for (int i = 1; i < BANDS.length; i++) if (y >= BANDS[i][0]) return BANDS[i][1];
            return BANDS[BANDS.length - 1][1];
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the plinth, a gold edge
            k.box(GREY, 0, 0, -6.8, -1.5, -6.8, 12.6, 1.5, 13.6);
            k.box(GOLD, -6.9, -1.7, -6.9, 12.8, 0.25, 13.8);
            // the shell, band by band: the back wall, the two side walls; inside them, velvet; the floor
            double prev = BANDS[0][0];
            for (int i = 1; i < BANDS.length; i++) {
                double y = BANDS[i][0], h = prev - y, half = BANDS[i][1];
                k.box(ENGINE, (i * 4) % 20, (i * 3) % 12, BACK, y, -half, WALL, h, 2 * half);
                k.box(ENGINE, (i * 5) % 20, (i * 2) % 12, BACK + WALL, y, half - WALL, FRONT - 1.0 - BACK - WALL, h, WALL);
                k.box(ENGINE, (i * 6) % 20, (i * 4) % 12, BACK + WALL, y, -half, FRONT - 1.0 - BACK - WALL, h, WALL);
                double in = half - WALL;
                if (in > 0.6) {
                    k.box(CANVAS, (i * 7) % 40, 0, BACK + WALL, y + 0.02, -in, 0.15, h - 0.04, 2 * in);
                    k.box(CANVAS, (i * 5) % 40, 8, BACK + WALL + 0.15, y + 0.02, in - 0.15, FRONT - 1.15 - BACK - WALL, h - 0.04, 0.15);
                    k.box(CANVAS, (i * 3) % 40, 16, BACK + WALL + 0.15, y + 0.02, -in, FRONT - 1.15 - BACK - WALL, h - 0.04, 0.15);
                }
                prev = y;
            }
            // the top under the crown
            k.box(ENGINE, 20, 20, BACK, -31.0, -2.6, FRONT - 1.0 - BACK, 1.0, 5.2);
            k.box(CANVAS, 30, 20, BACK + WALL, -30.0 + 0.02, -1.6, FRONT - 1.15 - BACK - WALL, 0.15, 3.2);
            k.box(CANVAS, 0, 24, BACK + WALL, -2.0, -4.6, FRONT - 1.15 - BACK - WALL, 0.45, 9.2);
            // old bloodstains: down the back, on the floor, splashed on a wall
            k.box(RED, BACK + WALL + 0.17, -22.0, -1.4, 0.05, 6.5, 1.0);
            k.box(RED, BACK + WALL + 0.17, -15.5, -1.0, 0.05, 2.4, 0.5);
            k.box(RED, BACK + WALL + 0.17, -12.0, 1.2, 0.05, 4.0, 0.7);
            k.box(RED, -2.0, -1.6, -1.6, 3.6, 0.05, 2.6);
            k.box(RED, 0.0, -18.0, 3.2, 1.8, 2.6, 0.05);
            // iron spikes pointing in from the back wall, bloodied tips
            for (int r = 0; r < 5; r++) {
                double y = -5.0 - r * 4.8;
                double in = halfAt(y) - WALL - 0.8;
                for (double z = -in; z <= in + 0.01; z += Math.max(1.0, in)) {
                    k.line(GREY, BACK + WALL + 0.1, y, z, BACK + WALL + 2.0, y, z, 0.42);
                    k.line(BONE, BACK + WALL + 2.0, y, z, BACK + WALL + 2.8, y, z, 0.2);
                }
            }
            // riveted bands round the outside, gold studs
            for (double y : new double[]{-5.5, -14.8, -24.0}) {
                double half = halfAt(y);
                k.box(GREY, BACK - 0.25, y, -half - 0.2, FRONT - 1.0 - BACK + 0.25, 0.8, 0.2);
                k.box(GREY, BACK - 0.25, y, half, FRONT - 1.0 - BACK + 0.25, 0.8, 0.2);
                k.box(GREY, BACK - 0.25, y, -half, 0.25, 0.8, 2 * half);
                for (int i = 0; i < 4; i++) k.cube(GOLD, BACK - 0.3, y + 0.4, -half + 0.8 + i * (2 * half - 1.6) / 3, 0.4);
            }
            // the crown on her head
            for (int i = 0; i < 5; i++) {
                double z = -2.4 + i * 1.2;
                k.box(BRASS, -2.6, -33.0 - (i % 2) * 0.8, z - 0.3, 1.6, 2.0 + (i % 2) * 0.8, 0.6);
            }
            k.box(BRASS, -2.8, -31.6, -2.8, 2.0, 0.6, 5.6);
            k.cube(NAV_RED, -1.8, -32.4, 0, 0.6);
            k.done();
            // the door: her front, hinged at the -z side of the shell (in the door's frame: x -1 to 0, z 0 to 11.2)
            PartDefinition door = root.addOrReplaceChild("door", cubes(), at(FRONT, 0, -BANDS[0][1]));
            K d = new K(door);
            prev = BANDS[0][0];
            double mid = BANDS[0][1];
            for (int i = 1; i < BANDS.length; i++) {
                double y = BANDS[i][0], h = prev - y, half = BANDS[i][1];
                d.box(ENGINE, (i * 5) % 20, (i * 2) % 12, -WALL, y, mid - half, WALL, h, 2 * half);
                if (half - WALL > 0.6) d.box(CANVAS, (i * 4) % 40, 4, -WALL - 0.15, y + 0.02, mid - half + WALL, 0.15, h - 0.04, 2 * (half - WALL));
                prev = y;
            }
            // spikes on its inside, pointing in
            for (int r = 0; r < 5; r++) {
                double y = -5.0 - r * 4.8;
                double in = halfAt(y) - WALL - 0.8;
                for (double z = -in; z <= in + 0.01; z += Math.max(1.0, in)) {
                    d.line(GREY, -WALL - 0.1, y, mid + z, -WALL - 2.0, y, mid + z, 0.42);
                    d.line(BONE, -WALL - 2.0, y, mid + z, -WALL - 2.8, y, mid + z, 0.2);
                }
            }
            d.box(RED, -WALL - 0.17, -12.0, mid - 1.0, 0.05, 3.5, 0.8);
            // outside: riveted bands, her robe's folds, crossed arms, the stern face, the latch
            for (double y : new double[]{-5.5, -14.8, -24.0}) {
                double half = halfAt(y);
                d.box(GREY, 0.0, y, mid - half + 0.3, 0.25, 0.8, 2 * half - 0.6);
                for (int i = 0; i < 4; i++) d.cube(GOLD, 0.3, y + 0.4, mid - half + 1.2 + i * (2 * half - 2.4) / 3, 0.4);
            }
            for (int i = 0; i < 3; i++) d.box(GREY, 0.1, -13.5, mid - 2.0 + i * 2.0, 0.3, 11.0, 0.4);
            d.line(GREY, 0.3, -19.0, mid - 3.6, 0.3, -16.0, mid + 3.0, 0.8);
            d.line(GREY, 0.3, -19.0, mid + 3.6, 0.3, -16.0, mid - 3.0, 0.8);
            CubeListBuilder face = cubes();
            front(face, FACE_U - 6, FACE_V - 6, 0.0F, -8.0F, -3.0F, 8.0F, 6.0F);
            door.addOrReplaceChild("face", face, at(0.12, -21.0, mid));
            d.box(BRASS, 0.0, -14.0, mid + 4.0, 0.7, 1.4, 0.9);
            d.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Blood Moon Altar ═════════════
    // A carved stone altar on two steps: a bat carved on its front, a crimson altar cloth with gold fringe over its top, a
    // gold-rimmed bowl of glowing blood, four tall candles at its corners. Over it a blood crystal floats, slowly turning,
    // three red runes circling it.
    public static class Altar extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_moon_altar");
        private final ModelPart crystal;
        private final ModelPart runes;
        private final List<ModelPart> flames = new ArrayList<>();

        public Altar(ModelPart root) {
            super(root);
            this.crystal = this.decor.getChild("crystal");
            this.runes = this.decor.getChild("runes");
            for (int i = 0; i < 4; i++) this.flames.add(this.decor.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            this.crystal.yRot = t * 0.035F;
            this.crystal.y = -21.0F + 0.7F * Mth.sin(t * 0.05F);
            this.runes.yRot = -t * 0.025F;
            this.runes.y = -20.0F + 0.4F * Mth.sin(t * 0.04F + 1.0F);
            for (int i = 0; i < 4; i++) flicker(this.flames.get(i), t, i, true);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the steps
            k.box(DECK, 0, 0, -6.4, -1.4, -12.0, 12.8, 1.4, 24.0);
            k.box(WHITE, 0, 0, -5.4, -2.6, -10.6, 10.8, 1.2, 21.2);
            // the altar block, its carved front, mouldings
            k.box(MOSS, 0, 0, -4.8, -11.0, -9.6, 9.6, 8.4, 19.2);
            k.box(WHITE, 30, 0, -5.2, -12.0, -10.2, 10.4, 1.0, 20.4);
            k.box(WHITE, 30, 8, -5.2, -3.6, -10.2, 10.4, 1.0, 20.4);
            k.box(GREY, 20, 20, 4.8, -10.0, -4.0, 0.3, 5.6, 8.0);
            // the carved bat on its front
            k.box(WHITE, 5.1, -8.6, -0.6, 0.35, 2.6, 1.2);
            k.cube(NAV_RED, 5.35, -8.2, -0.25, 0.25);
            k.cube(NAV_RED, 5.35, -8.2, 0.25, 0.25);
            for (int s = -1; s <= 1; s += 2) {
                k.line(WHITE, 5.25, -7.8, s * 0.5, 5.25, -9.4, s * 3.4, 0.4);
                k.line(WHITE, 5.25, -9.4, s * 3.4, 5.25, -6.6, s * 3.6, 0.32);
                k.line(WHITE, 5.25, -6.6, s * 3.6, 5.25, -7.2, s * 2.2, 0.3);
                k.line(WHITE, 5.25, -7.2, s * 2.2, 5.25, -6.6, s * 1.0, 0.3);
            }
            // the altar cloth: across the top and hanging over the front, gold fringe
            k.box(CANVAS, 0, 0, -4.4, -12.4, -6.0, 9.6, 0.4, 12.0);
            k.box(CANVAS, 0, 12, 5.2, -12.2, -6.0, 0.25, 5.6, 12.0);
            for (int i = 0; i < 12; i++) k.box(GOLD, 5.2, -6.6, -5.8 + i * 1.0, 0.3, 0.6, 0.5);
            k.box(GOLD, 5.25, -12.0, -6.0, 0.28, 0.3, 12.0);
            // the bowl of blood
            k.k.ringY(BRASS, 0.0F, -14.0F, 0.0F, 2.6F, 1.6F, 0.6F, 10, 0);
            k.k.disc(ICE, 0.0F, -13.7F, 0.0F, 2.1F, 0.3F);
            k.k.disc(BRASS, 0.0F, -12.6F, 0.0F, 1.6F, 0.4F);
            // candles at its corners, tall at the back
            double[][] spots = {{-3.6, -8.6, 5.0}, {-3.6, 8.6, 5.0}, {3.2, -8.6, 3.4}, {3.2, 8.6, 3.4}};
            for (int i = 0; i < 4; i++) {
                k.box(BRASS, spots[i][0] - 0.9, -12.9, spots[i][1] - 0.9, 1.8, 0.5, 1.8);
                candle(k, root, "flame" + i, spots[i][0], -12.9, spots[i][1], spots[i][2], 0.6);
            }
            // the floating crystal and its runes
            CubeListBuilder crystal = cubes();
            bx(crystal, ICE, -1.2, -3.0, -1.2, 2.4, 6.0, 2.4);
            BoatKit.box(crystal, ICE, 10, 0, -0.8F, -4.5F, -0.8F, 1.6F, 1.6F, 1.6F);
            BoatKit.box(crystal, ICE, 16, 0, -0.4F, -5.4F, -0.4F, 0.8F, 1.0F, 0.8F);
            BoatKit.box(crystal, ICE, 20, 0, -0.8F, 2.9F, -0.8F, 1.6F, 1.4F, 1.6F);
            BoatKit.box(crystal, ICE, 26, 0, -0.4F, 4.2F, -0.4F, 0.8F, 0.8F, 0.8F);
            PartDefinition cr = root.addOrReplaceChild("crystal", crystal, at(0, -21.0, 0));
            CubeListBuilder twist = cubes();
            BoatKit.box(twist, ICE, 30, 4, -1.1F, -2.7F, -1.1F, 2.2F, 5.4F, 2.2F);
            cr.addOrReplaceChild("twist", twist, at(0, 0, 0, 0, Math.PI / 4, 0));
            PartDefinition runes = root.addOrReplaceChild("runes", cubes(), at(0, -20.0, 0));
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3;
                CubeListBuilder rune = cubes();
                bx(rune, NAV_RED, -0.08, -0.9, -0.15, 0.16, 1.8, 0.3);
                bx(rune, NAV_RED, -0.08, -0.9, -0.15, 0.16, 0.3, 1.0);
                bx(rune, NAV_RED, -0.08, 0.1, -0.6, 0.16, 0.3, 0.75);
                runes.addOrReplaceChild("rune" + i, rune, at(Math.cos(a) * 4.0, (i - 1) * 0.8, Math.sin(a) * 4.0, 0, -a, 0));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Bat Roost ═════════════
    // A twisted dead branch on an iron stand with a stone foot, its crooked limbs reaching out. By day small bats hang
    // from them upside down, wings wrapped round them, asleep. At night they wake and flutter round it in looping flights
    // (the state's z: 1 at night).
    public static class BatRoost extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("bat_roost");
        static final int BATS = 5;
        // where each hangs: x, y (its feet), z
        static final double[][] PERCH = {{3.6, -24.0, 0.8}, {-1.0, -27.4, 4.2}, {-3.4, -22.4, -3.0}, {1.6, -29.8, -3.2}, {-4.2, -17.6, 1.8}};
        private final List<ModelPart> hanging = new ArrayList<>();
        private final List<ModelPart> flying = new ArrayList<>();
        private final List<ModelPart[]> wings = new ArrayList<>();

        public BatRoost(ModelPart root) {
            super(root);
            for (int i = 0; i < BATS; i++) {
                this.hanging.add(this.decor.getChild("hang" + i));
                ModelPart f = this.decor.getChild("fly" + i);
                this.flying.add(f);
                this.wings.add(new ModelPart[]{f.getChild("wingL"), f.getChild("wingR")});
            }
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            boolean night = s.z() > 0.5F;
            for (int i = 0; i < BATS; i++) {
                ModelPart h = this.hanging.get(i);
                h.visible = !night || i == 4;
                h.zRot = 0.06F * Mth.sin(t * 0.04F + i);
                ModelPart f = this.flying.get(i);
                f.visible = night && i != 4;
                // each its own looping flight round the roost: a tilted circle, bobbing, quick
                float a = t * (0.06F + i * 0.012F) + i * 1.7F;
                float r = 9.0F + 3.0F * Mth.sin(t * 0.021F + i);
                f.x = Mth.cos(a) * r;
                f.z = Mth.sin(a) * r;
                f.y = -26.0F - i * 2.5F + 4.0F * Mth.sin(a * 2.0F + i);
                f.yRot = -a - Mth.HALF_PI;
                f.zRot = 0.3F * Mth.cos(a * 2.0F);
                float flap = 0.9F * Mth.sin(t * 1.4F + i * 2.0F);
                this.wings.get(i)[0].xRot = flap;
                this.wings.get(i)[1].xRot = -flap;
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the stone foot, the iron stand
            k.k.disc(GREY, 0.0F, -1.4F, 0.0F, 5.0F, 1.4F);
            k.k.disc(WHITE, 0.0F, -2.2F, 0.0F, 3.6F, 0.8F);
            k.box(GOLD, -0.9, -3.0, -0.9, 1.8, 0.8, 1.8);
            k.box(ENGINE, -0.7, -9.0, -0.7, 1.4, 6.0, 1.4);
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3;
                k.line(ENGINE, Math.cos(a) * 0.6, -7.0, Math.sin(a) * 0.6, Math.cos(a) * 3.6, -1.6, Math.sin(a) * 3.6, 0.45);
            }
            // the twisted branch: a crooked trunk and limbs, twigs
            double[][] trunk = {{0, -8.6, 0}, {0.8, -13.0, 0.4}, {-0.6, -17.6, -0.4}, {0.6, -22.0, 0.6}, {-0.4, -26.0, 0.0}, {0.4, -30.0, -0.6}, {0.0, -33.0, 0.2}};
            for (int i = 1; i < trunk.length; i++) {
                k.line(OLDWOOD, trunk[i - 1][0], trunk[i - 1][1], trunk[i - 1][2], trunk[i][0], trunk[i][1], trunk[i][2], 1.9 - i * 0.22);
            }
            double[][][] limbs = {
                {{0.4, -20.0, 0.4}, {2.6, -22.4, 0.6}, {4.6, -24.4, 0.8}, {6.0, -24.0, 1.4}},
                {{0.0, -24.0, 0.2}, {-0.6, -26.4, 2.4}, {-1.2, -27.6, 4.4}, {-2.2, -28.6, 5.6}},
                {{-0.2, -18.0, -0.2}, {-2.0, -20.6, -1.6}, {-3.6, -22.6, -3.2}, {-4.6, -22.0, -4.4}},
                {{0.2, -27.6, -0.2}, {1.2, -29.4, -1.8}, {1.8, -30.2, -3.4}, {2.8, -31.4, -4.2}},
                {{-0.3, -14.4, 0.0}, {-2.4, -16.4, 0.8}, {-4.4, -17.8, 1.8}, {-5.6, -19.2, 2.2}}};
            for (double[][] limb : limbs) {
                for (int i = 1; i < limb.length; i++) {
                    k.line(OLDWOOD, limb[i - 1][0], limb[i - 1][1], limb[i - 1][2], limb[i][0], limb[i][1], limb[i][2], 1.1 - i * 0.22);
                }
            }
            k.line(OLDWOOD, 0.0, -33.0, 0.2, 1.4, -35.2, 1.0, 0.4);
            k.line(OLDWOOD, 0.0, -32.6, 0.2, -1.6, -34.6, -0.6, 0.35);
            // the sleeping bats, hanging upside down from the limbs, wrapped in their wings
            for (int i = 0; i < BATS; i++) {
                CubeListBuilder bat = cubes();
                bx(bat, BLACK, -0.15, 0.0, -0.15, 0.3, 0.6, 0.3);
                bx(bat, ACCENT, -0.75, 0.5, -0.65, 1.5, 2.6, 1.3);
                bx(bat, BLACK, -0.55, 3.0, -0.55, 1.1, 1.0, 1.1);
                bx(bat, BLACK, -0.45, 3.9, -0.5, 0.3, 0.6, 0.25);
                bx(bat, BLACK, -0.45, 3.9, 0.25, 0.3, 0.6, 0.25);
                root.addOrReplaceChild("hang" + i, bat, at(PERCH[i][0], PERCH[i][1], PERCH[i][2]));
            }
            // the bats awake: a body, a head with ears and red eyes, two wings that flap
            for (int i = 0; i < BATS; i++) {
                CubeListBuilder body = cubes();
                bx(body, BLACK, -1.1, -0.5, -0.45, 2.2, 1.0, 0.9);
                bx(body, BLACK, 1.0, -0.65, -0.45, 1.0, 0.9, 0.9);
                bx(body, BLACK, 1.3, -1.15, -0.4, 0.3, 0.6, 0.25);
                bx(body, BLACK, 1.3, -1.15, 0.15, 0.3, 0.6, 0.25);
                bx(body, NAV_RED, 1.98, -0.45, -0.38, 0.06, 0.22, 0.22);
                bx(body, NAV_RED, 1.98, -0.45, 0.16, 0.06, 0.22, 0.22);
                PartDefinition fly = root.addOrReplaceChild("fly" + i, body, at(0, -26.0, 0));
                CubeListBuilder wl = cubes();
                bx(wl, ACCENT, -1.0, -0.05, -3.8, 2.4, 0.1, 3.8);
                bx(wl, BLACK, 0.8, -0.1, -3.8, 0.3, 0.2, 3.8);
                fly.addOrReplaceChild("wingL", wl, at(0, 0, -0.45));
                CubeListBuilder wr = cubes();
                bx(wr, ACCENT, -1.0, -0.05, 0.0, 2.4, 0.1, 3.8);
                bx(wr, BLACK, 0.8, -0.1, 0.0, 0.3, 0.2, 3.8);
                fly.addOrReplaceChild("wingR", wr, at(0, 0, 0.45));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Four-Poster Crimson Bed (2 blocks long) ═════════════
    // Four tall carved posts of black wood with gold finials, a tall headboard with a pointed arch of tufted crimson velvet,
    // a lower footboard, white linen, two satin pillows, a crimson satin coverlet with gold trim turned down at the head.
    // Overhead a crimson velvet canopy with a scalloped valance and gold tassels at its corners; crimson curtains gathered
    // at the posts, drawn shut while someone sleeps in it (the state: how far shut). The head is -x.
    public static class CanopyBed extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("crimson_canopy_bed");
        static final float POST_X = 14.6F, POST_Z = 6.9F, TOP = -46.0F;
        // the curtains: the four running along the sides from each post, and two at the foot
        private final List<ModelPart> curtains = new ArrayList<>();

        public CanopyBed(ModelPart root) {
            super(root);
            for (int i = 0; i < 6; i++) this.curtains.add(this.decor.getChild("curtain" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float o = Mth.clamp(s.open(), 0.0F, 1.0F);
            float e = o * o * (3 - 2 * o);
            // gathered: a narrow bunch at the post; drawn: halfway along the side (or the foot)
            for (int i = 0; i < 4; i++) this.curtains.get(i).xScale = 2.2F + (13.6F - 2.2F) * e;
            for (int i = 4; i < 6; i++) this.curtains.get(i).zScale = 1.6F + (6.0F - 1.6F) * e;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the posts: carved, turned bulges, gold collars and finials
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    double x = sx * POST_X, z = sz * POST_Z;
                    k.box(DARKWOOD, 0, 0, x - 0.9, -24.0, z - 0.9, 1.8, 24.0, 1.8);
                    k.box(DARKWOOD, 8, 0, x - 0.8, TOP, z - 0.8, 1.6, -TOP - 24.0, 1.6);
                    k.box(DARKWOOD, 16, 0, x - 1.3, -2.0, z - 1.3, 2.6, 2.0, 2.6);
                    k.box(DARKWOOD, 16, 6, x - 1.25, -12.5, z - 1.25, 2.5, 2.2, 2.5);
                    k.box(BRASS, x - 1.05, -10.3, z - 1.05, 2.1, 0.5, 2.1);
                    k.box(BRASS, x - 1.05, -24.4, z - 1.05, 2.1, 0.5, 2.1);
                    k.box(DARKWOOD, 16, 12, x - 1.2, -27.5, z - 1.2, 2.4, 2.0, 2.4);
                    k.cube(BRASS, x, TOP - 1.2, z, 1.6);
                    k.box(GOLD, x - 0.3, TOP - 3.2, z - 0.3, 0.6, 1.4, 0.6);
                }
            }
            // the frame: side rails, the mattress, linen, the coverlet turned down, pillows
            for (int sz = -1; sz <= 1; sz += 2) k.box(DARKWOOD, 30, 0, -13.8, -5.4, sz > 0 ? 6.4 : -7.4, 27.6, 2.4, 1.0);
            k.box(DECK, 0, 0, -13.8, -8.6, -6.5, 27.6, 3.2, 13.0);
            k.box(ICE, 0, 0, -3.4, -9.0, -6.9, 17.2, 0.5, 13.8);
            k.box(LEATHER, 0, 12, 13.75, -9.0, -6.9, 0.35, 4.4, 13.8);
            for (int sz = -1; sz <= 1; sz += 2) k.box(LEATHER, 20, 12, -3.4, -9.0, sz > 0 ? 6.85 : -7.2, 17.2, 4.2, 0.35);
            k.box(GOLD, -3.5, -9.1, -6.95, 0.35, 0.55, 13.9);
            k.box(LEATHER, 30, 16, -5.6, -9.4, -6.9, 2.4, 0.8, 13.8);
            k.box(GOLD, -3.3, -9.05, -6.95, 17.0, 0.12, 0.2);
            for (int sz = -1; sz <= 1; sz += 2) {
                k.box(WHITE, 20, 20, -13.2, -10.6, sz > 0 ? 0.5 : -6.0, 4.2, 2.2, 5.5);
                k.box(RED, -12.8, -11.0, sz > 0 ? 0.9 : -5.6, 3.4, 0.5, 4.7);
            }
            // the headboard: tall, a pointed arch of tufted velvet, gold tracing, a ruby
            k.box(DARKWOOD, 40, 0, -15.2, -26.0, -6.4, 1.2, 21.0, 12.8);
            k.box(CANVAS, 0, 0, -14.1, -23.6, -5.2, 0.6, 13.6, 10.4);
            double[][] arch = {{-25.2, 4.2}, {-26.6, 3.2}, {-28.0, 2.2}, {-29.4, 1.2}};
            for (int i = 0; i < arch.length; i++) {
                k.box(DARKWOOD, 30, 10 + i, -15.2, arch[i][0] - 1.4, -arch[i][1] - 1.2, 1.2, 1.6, 2 * arch[i][1] + 2.4);
                k.box(CANVAS, 44 + i, 2, -14.15, arch[i][0] - 1.0, -arch[i][1], 0.6, 1.5, 2 * arch[i][1]);
            }
            k.line(BRASS, -13.95, -23.6, 5.3, -13.95, -30.6, 0, 0.35);
            k.line(BRASS, -13.95, -23.6, -5.3, -13.95, -30.6, 0, 0.35);
            k.box(BRASS, -14.0, -23.8, -5.4, 0.3, 0.35, 10.8);
            k.cube(NAV_RED, -13.9, -30.2, 0, 0.8);
            // the footboard
            k.box(DARKWOOD, 40, 20, 14.2, -14.0, -6.4, 1.0, 9.0, 12.8);
            k.box(OLDWOOD, 15.15, -12.6, -4.8, 0.3, 6.0, 9.6);
            k.box(GOLD, 15.2, -14.2, -6.2, 0.3, 0.3, 12.4);
            // the canopy: a frame, the velvet over it, a scalloped valance all round, tassels
            for (int sz = -1; sz <= 1; sz += 2) k.box(DARKWOOD, 0, 28, -15.0, TOP + 0.6, sz * POST_Z - 0.5, 30.0, 1.0, 1.0);
            for (int sx = -1; sx <= 1; sx += 2) k.box(DARKWOOD, 30, 28, sx * POST_X - 0.5, TOP + 0.6, -7.4, 1.0, 1.0, 14.8);
            k.box(PORT, 0, 0, -15.4, TOP + 0.2, -7.6, 30.8, 0.35, 15.2);
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int i = 0; i < 10; i++) {
                    double x = -15.0 + i * 3.0;
                    k.box(CANVAS, (i * 4) % 40, 4, x, TOP + 0.55, sz > 0 ? 7.55 : -7.85, 3.0, 2.4 + (i % 2) * 0.5, 0.3);
                }
                k.box(GOLD, -15.4, TOP + 0.5, sz > 0 ? 7.65 : -7.95, 30.8, 0.3, 0.3);
            }
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int i = 0; i < 5; i++) {
                    double z = -7.5 + i * 3.0;
                    k.box(CANVAS, (i * 4) % 40, 8, sx > 0 ? 15.3 : -15.6, TOP + 0.55, z, 0.3, 2.4 + (i % 2) * 0.5, 3.0);
                }
            }
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    k.line(ROPE, sx * 15.6, TOP + 1.0, sz * 7.9, sx * 15.6, TOP + 4.5, sz * 7.9, 0.3);
                    k.box(BRASS, sx * 15.6 - 0.4, TOP + 4.4, sz * 7.9 - 0.4, 0.8, 0.9, 0.8);
                    for (int f = 0; f < 4; f++) {
                        k.line(GOLD, sx * 15.6 + (f % 2 - 0.5) * 0.4, TOP + 5.2, sz * 7.9 + (f / 2 - 0.5) * 0.4,
                            sx * 15.6 + (f % 2 - 0.5) * 0.6, TOP + 6.8, sz * 7.9 + (f / 2 - 0.5) * 0.6, 0.15);
                    }
                }
            }
            // the curtains (scaled along their run as they're drawn: CanopyBed.setupAnim), tie-backs at the posts
            int n = 0;
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int sx = -1; sx <= 1; sx += 2) {
                    CubeListBuilder c = cubes();
                    // one px along the side: setupAnim stretches it
                    BoatKit.box(c, CANVAS, 0, 0, sx > 0 ? -1.0F : 0.0F, 0.0F, -0.2F, 1.0F, -TOP - 1.4F, 0.4F);
                    root.addOrReplaceChild("curtain" + (n++), c, at(sx * (POST_X - 0.9), TOP + 1.4, sz * (POST_Z + 0.9)));
                }
            }
            for (int sz = -1; sz <= 1; sz += 2) {
                CubeListBuilder c = cubes();
                BoatKit.box(c, CANVAS, 0, 0, -0.2F, 0.0F, sz > 0 ? -1.0F : 0.0F, 0.4F, -TOP - 1.4F, 1.0F);
                root.addOrReplaceChild("curtain" + (n++), c, at(POST_X + 0.9, TOP + 1.4, sz * (POST_Z - 0.9)));
            }
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) k.box(ROPE, sx * POST_X - 1.1, -20.0, sz * POST_Z - 1.1, 2.2, 0.5, 2.2);
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Blood Crystal Fireplace (2 wide) ═════════════
    // A gothic stone fireplace against a wall (its back is -x): a hearthstone, carved jambs with colonnettes, a pointed
    // arch over the firebox, a marble mantel shelf on corbels with gold trim, candles and a skull on it, and above it a
    // chimney breast with a carved stone bat, its eyes rubies. In the hearth blood crystals sit on iron andirons; lit, a
    // crimson fire roars round them and embers glow (the flames only while lit).
    public static class Fireplace extends Base {
        public static final ModelLayerLocation LAYER = DecorModels.layer("blood_crystal_fireplace");
        static final int FLAMES = 9;
        private final List<ModelPart> flames = new ArrayList<>();
        private final List<ModelPart> candles = new ArrayList<>();

        public Fireplace(ModelPart root) {
            super(root);
            for (int i = 0; i < FLAMES; i++) this.flames.add(this.decor.getChild("fire" + i));
            for (int i = 0; i < 2; i++) this.candles.add(this.decor.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Anim s) {
            super.setupAnim(s);
            float t = s.time();
            for (int i = 0; i < FLAMES; i++) {
                ModelPart f = this.flames.get(i);
                f.visible = s.lit();
                f.yScale = 0.8F + 0.3F * Mth.sin(t * 0.45F + i * 1.7F) + 0.15F * Mth.sin(t * 1.2F + i);
                f.xScale = 1.0F + 0.1F * Mth.sin(t * 0.7F + i * 2.1F);
                f.zRot = 0.1F * Mth.sin(t * 0.3F + i * 1.3F);
            }
            for (int i = 0; i < 2; i++) flicker(this.candles.get(i), t, i, true);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = DecorModels.start(mesh);
            K k = new K(root);
            // the hearthstone
            k.box(DECK, 0, 0, -8.0, -1.0, -13.0, 13.0, 1.0, 26.0);
            k.box(GOLD, 4.95, -0.8, -13.0, 0.2, 0.3, 26.0);
            // the jambs: stone blocks, colonnettes with gold capitals
            for (int s = -1; s <= 1; s += 2) {
                k.box(GREY, s > 0 ? 0 : 20, 0, -8.0, -21.0, s > 0 ? 8.0 : -15.0, 8.0, 20.0, 7.0);
                k.box(WHITE, 0, 0, -0.2, -20.0, s * 8.7 - 0.7, 1.4, 18.6, 1.4);
                k.box(BRASS, -0.4, -20.6, s * 8.7 - 0.9, 1.8, 0.7, 1.8);
                k.box(BRASS, -0.4, -2.0, s * 8.7 - 0.9, 1.8, 0.6, 1.8);
            }
            // the firebox: sooty back and sides
            k.box(ENGINE, 0, 0, -8.0, -19.0, -8.0, 1.0, 18.0, 16.0);
            for (int s = -1; s <= 1; s += 2) k.box(ENGINE, 2, 2, -7.0, -19.0, s > 0 ? 7.6 : -8.0, 6.0, 18.0, 0.4);
            // the pointed arch over it: stone voussoirs, a keystone with a ruby
            k.box(DECK, 0, 0, -8.0, -25.0, -15.0, 8.0, 2.0, 30.0);
            k.box(DECK, 40, 0, -8.0, -23.0, -15.0, 8.0, 2.0, 30.0);
            k.line(WHITE, 0.2, -19.6, -8.0, 0.2, -23.6, 0.0, 1.4);
            k.line(WHITE, 0.2, -19.6, 8.0, 0.2, -23.6, 0.0, 1.4);
            k.box(WHITE, 30, 20, -0.4, -25.0, -1.0, 1.2, 2.6, 2.0);
            k.cube(NAV_RED, 0.85, -23.8, 0, 0.6);
            // the mantel shelf on corbels, gold trim; candles and a skull on it
            k.box(DECK, 0, 0, -8.0, -27.0, -16.0, 11.0, 2.0, 32.0);
            k.box(GOLD, 2.95, -26.6, -16.0, 0.2, 0.3, 32.0);
            for (int s = -1; s <= 1; s += 2) k.box(WHITE, 20, 8, -0.5, -25.0, s * 12.5 - 1.0, 2.6, 1.8, 2.0);
            for (int s = -1; s <= 1; s += 2) {
                k.box(BRASS, -0.8, -27.4, s * 13.0 - 0.8, 1.6, 0.4, 1.6);
                candle(k, root, "flame" + (s > 0 ? 1 : 0), 0.0, -27.4, s * 13.0, 3.2, 0.5);
            }
            k.box(BONE, -2.6, -29.9, 7.6, 2.4, 2.5, 2.6);
            k.box(BONE, -2.0, -28.2, 8.0, 2.2, 0.8, 1.8);
            k.box(BLACK, -0.18, -29.4, 7.9, 0.05, 0.8, 0.8).box(BLACK, -0.18, -29.4, 9.1, 0.05, 0.8, 0.8);
            // the chimney breast and its carved bat
            k.box(MOSS, 0, 0, -8.0, -42.0, -11.0, 5.0, 15.0, 22.0);
            k.box(WHITE, 40, 0, -3.2, -38.0, -1.2, 0.8, 4.0, 2.4);
            k.box(WHITE, 40, 8, -3.0, -39.6, -0.9, 0.7, 1.8, 1.8);
            k.cube(NAV_RED, -2.35, -38.9, -0.45, 0.4);
            k.cube(NAV_RED, -2.35, -38.9, 0.45, 0.4);
            for (int s = -1; s <= 1; s += 2) {
                k.line(WHITE, -2.8, -37.6, s * 1.0, -2.8, -40.4, s * 6.4, 0.7);
                k.line(WHITE, -2.8, -40.4, s * 6.4, -2.8, -35.2, s * 7.0, 0.55);
                k.line(WHITE, -2.8, -35.2, s * 7.0, -2.8, -36.4, s * 4.6, 0.5);
                k.line(WHITE, -2.8, -36.4, s * 4.6, -2.8, -35.2, s * 2.6, 0.5);
                k.line(WHITE, -2.8, -35.2, s * 2.6, -2.8, -36.0, s * 1.0, 0.45);
                k.line(WHITE, -3.0, -39.4, s * 0.6, -3.0, -40.8, s * 0.9, 0.35);
            }
            k.box(GOLD, -3.1, -42.4, -11.0, 0.3, 0.4, 22.0);
            // the andirons, logs, the blood crystals
            for (int s = -1; s <= 1; s += 2) {
                k.box(ENGINE, -2.0, -2.8, s * 4.6 - 0.4, 3.6, 0.8, 0.8);
                k.box(ENGINE, 1.0, -4.4, s * 4.6 - 0.45, 0.9, 2.4, 0.9);
                k.cube(BRASS, 1.45, -4.8, s * 4.6, 0.9);
            }
            k.line(DARKWOOD, -4.0, -2.4, -5.6, -2.6, -2.6, 5.4, 1.4);
            k.line(DARKWOOD, -5.2, -3.6, -4.4, -1.4, -2.4, 3.6, 1.2);
            double[][] crystals = {{-3.4, 0, 3.2, 0.0}, {-2.2, -2.6, 2.4, 0.4}, {-4.0, 2.4, 2.6, -0.3}, {-1.0, 1.0, 1.8, 0.5}, {-1.4, -1.2, 1.6, -0.5}};
            for (double[] c : crystals) {
                CubeListBuilder cry = cubes();
                bx(cry, ICE, -0.7, -c[2], -0.7, 1.4, c[2], 1.4);
                bx(cry, ICE, -0.4, -c[2] - 0.9, -0.4, 0.8, 0.9, 0.8);
                k.part(cry, c[0], -3.0, c[1], c[3], 0.4 + c[3], c[3] * 0.6);
            }
            for (int e = 0; e < 9; e++) k.cube(NAV_RED, -5.4 + (e % 3) * 2.2, -1.1, -4.0 + (e / 3) * 3.6 + (e % 2) * 0.6, 0.5);
            // the crimson fire
            for (int i = 0; i < FLAMES; i++) {
                CubeListBuilder f = cubes();
                double h = 3.2 + (i % 3) * 1.4;
                bx(f, FIRE, -0.9, -h, -0.9, 1.8, h, 1.8);
                BoatKit.box(f, FIRE, 6, 2, -0.5F, (float) (-h - 1.4), -0.5F, 1.0F, 1.4F, 1.0F);
                BoatKit.box(f, WARM, 0, 0, -0.3F, (float) (-h * 0.6), -0.3F, 0.6F, (float) (h * 0.6), 0.6F);
                double z = -5.6 + i * 1.4, x = -3.2 + ((i * 7) % 3) * 0.9;
                root.addOrReplaceChild("fire" + i, f, at(x, -2.6, z));
            }
            k.done();
            return LayerDefinition.create(mesh, 256, 256);
        }
    }
}

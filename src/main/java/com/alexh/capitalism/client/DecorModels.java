package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static com.alexh.capitalism.client.BoatKit.*;

// The seasonal decorations' models (decor/, Decorations.md), drawn by client/DecorRenderer on their blocks. Built with
// BoatKit like the boats (model pixels: y DOWN, y = 0 the ground under the block, the front +x), each with its own
// texture painted by tools/BoatTex.cs (textures/entity/<name>.png, <name>_glow.png). The state is the time (ticks).
public final class DecorModels {

    private DecorModels() {
    }

    static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, name), "main");
    }

    public abstract static class Base extends Model<Float> {
        protected final ModelPart decor;

        protected Base(ModelPart root) {
            super(root, RenderTypes::entityCutout);
            this.decor = root.getChild("decor");
        }
    }

    static PartDefinition start(MeshDefinition mesh) {
        return mesh.getRoot().addOrReplaceChild("decor", CubeListBuilder.create(), PartPose.ZERO);
    }

    // a branch, a limb, a petal: a child part turned round the upright (yaw) then tipped down (droop)
    static PartDefinition spoke(PartDefinition parent, String name, float x, float y, float z, float yaw, float droop) {
        PartDefinition spin = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, 0.0F, yaw, 0.0F));
        return spin.addOrReplaceChild("tip", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, droop));
    }

    // a rounded lump (a pumpkin, a gourd): two crossed boxes making an octagon, ribs from the texture, a stem
    static void pumpkin(BoatKit k, Zone zone, float cx, float cy, float cz, float r, float h, boolean stem) {
        k.box(zone, 0, 0, cx - r, cy - h, cz - r * 0.62F, 2 * r, h, 1.24F * r);
        k.box(zone, 6, 2, cx - r * 0.62F, cy - h - 0.05F, cz - r, 1.24F * r, h * 0.98F, 2 * r);
        k.box(zone, 10, 4, cx - r * 0.85F, cy - h * 1.06F, cz - r * 0.85F, 1.7F * r, h * 0.2F, 1.7F * r);
        if (stem) {
            k.box(DARKWOOD, 0, 0, cx - 0.5F, cy - h - 1.8F, cz - 0.5F, 1.0F, 2.0F, 1.0F);
            k.line(DARKWOOD, cx, cy - h - 1.6F, cz, cx + 1.2F, cy - h - 2.6F, cz + 0.4F, 0.6F);
        }
    }

    // ═════════════ the Grand Christmas Tree ═════════════
    // A tall fir about 9 blocks high: tiers of drooping branches, wider at the bottom; baubles hanging from the branch
    // tips (red, gold, blue, silver, purple), lights twinkling in three sets, gold tinsel spiralling up it, a star on
    // top (gold, its heart glowing, slowly turning), a red tree skirt and presents round the trunk.
    public static class ChristmasTree extends Base {
        public static final ModelLayerLocation LAYER = layer("christmas_tree");
        static final int TIERS = 15;
        static final float BOTTOM = -12.0F;
        static final float STEP = 7.6F;
        static final float SPREAD = 54.0F;
        // the fir needles: a big picture zone below y 180 (tools/BoatTex.cs)
        static final Zone FIR = new Zone(0, 180, 256, 76);

        private final ModelPart star;
        private final List<List<ModelPart>> lights = new ArrayList<>();

        public ChristmasTree(ModelPart root) {
            super(root);
            this.star = this.decor.getChild("star");
            for (int g = 0; g < 3; g++) this.lights.add(new ArrayList<>());
            // every branch's lights, by their set (the same branches as createLayer makes)
            for (int i = 0; i < TIERS; i++) {
                float r = radiusAt(BOTTOM - i * STEP);
                int branches = Math.max(5, Math.round(r / 4.0F) + 4);
                for (int b = 0; b < branches; b++) {
                    ModelPart tip = this.decor.getChild("branch" + i + "_" + b).getChild("tip");
                    for (int g = 0; g < 3; g++) if (tip.hasChild("lights" + g)) this.lights.get(g).add(tip.getChild("lights" + g));
                }
            }
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            float t = time;
            this.star.yRot = t * 0.02F;
            // the three sets of lights twinkle in turn: each dims and brightens on its own beat
            for (int g = 0; g < 3; g++) {
                boolean on = Mth.sin(t * 0.09F + g * 2.1F) > -0.35F;
                for (ModelPart p : this.lights.get(g)) p.visible = on;
            }
        }

        static float radiusAt(float y) {
            float f = (y - BOTTOM) / (-(TIERS * STEP));
            return SPREAD * (1 - Math.min(1, f)) + 2.0F;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            // trunk, skirt
            k.box(DARKWOOD, 0, 0, -3.0F, -16.0F, -3.0F, 6.0F, 16.0F, 6.0F);
            k.disc(DECK, 0, -1.0F, 0, 17.0F, 1.0F);   // (DECK: the red velvet skirt on the tree)
            Zone[] baubles = {RED, BRASS, NAVY, WHITE, ACCENT};
            Zone[] bulbs = {NAV_RED, WARM, NAV_GREEN, BLUE_GLOW};
            int n = 0;
            for (int i = 0; i < TIERS; i++) {
                float y = BOTTOM - i * STEP;
                float r = radiusAt(y);
                int branches = Math.max(5, Math.round(r / 4.0F) + 4);
                float width = Math.min(26.0F, 2 * (float) Math.PI * r / branches * 1.5F);
                for (int b = 0; b < branches; b++) {
                    // each branch a little different: longer or shorter, drooping more or less, a little off its place
                    float j1 = (float) Math.floorMod((i * 7919 + b * 104729) % 1000, 1000) / 1000.0F;
                    float j2 = (float) Math.floorMod((i * 104723 + b * 7907) % 1000, 1000) / 1000.0F;
                    float yaw = (float) (b * 2 * Math.PI / branches + i * 0.45 + (j1 - 0.5) * 0.35);
                    float rr = r * (0.84F + 0.3F * j2);
                    PartDefinition tip = spoke(root, "branch" + i + "_" + b, 0, y, 0, yaw, 0.2F + 0.24F * j1);
                    CubeListBuilder branch = CubeListBuilder.create();
                    box(branch, FIR, (i * 23 + b * 37) % 120, (b * 7) % 30, 0.0F, -2.2F, -width / 2, rr, 4.4F, width);
                    box(branch, FIR, (i * 31 + b * 13) % 120, (b * 11) % 30, rr * 0.35F, -4.6F, -width * 0.35F, rr * 0.55F, 2.6F, width * 0.7F);
                    tip.addOrReplaceChild("needles", branch, PartPose.ZERO);
                    // a bauble hanging off every other branch tip
                    if ((b + i) % 2 == 0 && r > 6) {
                        CubeListBuilder bauble = CubeListBuilder.create();
                        Zone z = baubles[n % baubles.length];
                        float s = 3.2F + (n % 3) * 0.6F;
                        box(bauble, z, (n * 3) % 20, 0, -s / 2, -s, -s / 2, s, s, s);
                        box(bauble, GOLD, 0, 0, -0.5F, -s - 0.8F, -0.5F, 1.0F, 0.8F, 1.0F);
                        // on top of the branch near its tip, where it shows
                        tip.addOrReplaceChild("bauble", bauble, PartPose.offset(rr - 3.5F, -2.0F, (n % 2 == 0 ? 1 : -1) * width * 0.18F));
                        n++;
                    }
                    // lights along the branch: three sets that twinkle
                    if (r > 5) {
                        int g = (b + i) % 3;
                        CubeListBuilder bulb = CubeListBuilder.create();
                        Zone z = bulbs[(b + i * 2) % bulbs.length];
                        box(bulb, z, 0, 0, rr * 0.78F, -3.8F, width * 0.3F, 1.8F, 1.8F, 1.8F);
                        box(bulb, bulbs[(b + i * 2 + 1) % bulbs.length], 0, 0, rr * 0.5F, -4.6F, -width * 0.3F, 1.8F, 1.8F, 1.8F);
                        box(bulb, bulbs[(b + i * 2 + 2) % bulbs.length], 0, 0, rr * 0.95F, -2.8F, -width * 0.05F, 1.6F, 1.6F, 1.6F);
                        tip.addOrReplaceChild("lights" + g, bulb, PartPose.ZERO);
                    }
                }
                // the heart of the tier, so you can't see through the middle
                k.box(FIR, (i * 17) % 100, (i * 5) % 40, -r * 0.35F, y - 6.0F, -r * 0.35F, r * 0.7F, 7.0F, r * 0.7F);
            }
            // gold tinsel spiralling up
            float prevX = 0, prevY = 0, prevZ = 0;
            int steps = 90;
            for (int s = 0; s <= steps; s++) {
                float y = BOTTOM - 4.0F - s * (TIERS * STEP - 10.0F) / steps;
                float a = s * (float) (Math.PI * 7 / steps);
                float r = radiusAt(y) * 0.92F + 1.0F;
                float x = Mth.cos(a) * r;
                float z = Mth.sin(a) * r;
                if (s > 0) k.line(GOLD, prevX, prevY, prevZ, x, y, z, 0.7F);
                prevX = x;
                prevY = y;
                prevZ = z;
            }
            // the star: five gold points round a glowing heart, turning slowly
            float top = BOTTOM - TIERS * STEP - 2.0F;
            CubeListBuilder heart = CubeListBuilder.create();
            box(heart, WARM, 0, 0, -0.6F, -2.2F, -2.2F, 1.2F, 4.4F, 4.4F);
            PartDefinition star = root.addOrReplaceChild("star", heart, PartPose.offset(0, top - 6.0F, 0));
            for (int p = 0; p < 5; p++) {
                CubeListBuilder point = CubeListBuilder.create();
                box(point, GOLD, 0, 0, -0.5F, -7.5F, -1.2F, 1.0F, 6.0F, 2.4F);
                box(point, GOLD, 4, 0, -0.45F, -9.0F, -0.6F, 0.9F, 1.6F, 1.2F);
                star.addOrReplaceChild("point" + p, point, PartPose.offsetAndRotation(0, 0, 0, (float) (p * 2 * Math.PI / 5), 0, 0));
            }
            k.box(GOLD, -0.5F, top - 1.0F, -0.5F, 1.0F, 3.0F, 1.0F);
            // presents round the trunk
            float[][] gifts = {{9, 4, 7, 6}, {-11, 6, 7, 5}, {3, -12, 6, 7}, {-6, 11, 5, 5}, {13, -6, 6, 5}, {-14, -8, 7, 6}};
            Zone[] wraps = {RED, NAVY, ACCENT, WHITE, BRASS, RED};
            Zone[] ribbons = {GOLD, WHITE, GOLD, RED, NAVY, WHITE};
            for (int i = 0; i < gifts.length; i++) {
                float[] g = gifts[i];
                float w = g[2];
                float h = g[3];
                k.box(wraps[i], i * 4, 0, g[0] - w / 2, -h, g[1] - w / 2, w, h, w);
                k.box(ribbons[i], 0, 0, g[0] - 0.6F, -h - 0.05F, g[1] - w / 2 - 0.05F, 1.2F, h + 0.1F, w + 0.1F);
                k.box(ribbons[i], 2, 0, g[0] - w / 2 - 0.08F, -h - 0.08F, g[1] - 0.6F, w + 0.16F, h + 0.16F, 1.2F);
                k.line(ribbons[i], g[0], -h - 0.1F, g[1], g[0] + 1.6F, -h - 1.6F, g[1] + 1.2F, 0.7F);
                k.line(ribbons[i], g[0], -h - 0.1F, g[1], g[0] - 1.6F, -h - 1.6F, g[1] - 1.2F, 0.7F);
            }
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Spooky Scarecrow ═════════════
    // On a post, arms out along a crossbar: a patched purple coat with tattered tails, straw bursting from its cuffs, a
    // rope belt; a pumpkin for a head with a carved face that glows, a crooked witch's hat; a crow on one arm (it looks
    // about); two little pumpkins at its feet. It sways a little.
    public static class Scarecrow extends Base {
        public static final ModelLayerLocation LAYER = layer("spooky_scarecrow");
        private final ModelPart body;
        private final ModelPart crowHead;

        public Scarecrow(ModelPart root) {
            super(root);
            this.body = this.decor.getChild("body");
            this.crowHead = this.body.getChild("crow").getChild("head");
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            float t = time;
            this.body.xRot = 0.03F * Mth.sin(t * 0.04F);
            this.body.zRot = 0.02F * Mth.sin(t * 0.03F + 1.0F);
            this.crowHead.yRot = 0.8F * Mth.sin(t * 0.05F) * (Mth.sin(t * 0.013F) > 0 ? 1 : 0.2F);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            pumpkin(k, STEEL, 7.0F, 0.0F, -6.0F, 3.4F, 4.6F, true);
            pumpkin(k, STEEL, 5.0F, 0.0F, 7.0F, 2.6F, 3.4F, true);
            k.box(WOOD, 0, 0, -1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            // the rest sways from its foot
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
            BoatKit b = new BoatKit(body);
            b.box(WOOD, 0, 0, -1.4F, -42.0F, -1.4F, 2.8F, 19.0F, 2.8F);                     // the post (in two: each fits its wood)
            b.box(WOOD, 8, 0, -1.35F, -23.0F, -1.35F, 2.7F, 19.0F, 2.7F);
            b.box(WOOD, 4, 0, -1.2F, -33.5F, -16.0F, 2.4F, 2.4F, 32.0F);                    // the crossbar
            // the coat, its tails in tatters, patches, the belt
            b.box(CANVAS, 0, 0, -4.5F, -38.0F, -6.0F, 9.0F, 16.0F, 12.0F);
            float[] tails = {6.0F, 4.0F, 7.5F, 5.0F, 6.5F, 3.5F};
            for (int i = 0; i < tails.length; i++) {
                b.box(CANVAS, 2 + i * 3, 4, -4.3F + (i % 2) * 0.2F, -22.0F, -6.0F + i * 2.0F, 8.6F, tails[i], 1.9F);
            }
            b.box(NAVY, 0, 0, 4.55F, -33.0F, 1.0F, 0.1F, 3.0F, 3.0F);
            b.box(RED, 0, 0, 4.55F, -27.0F, -4.0F, 0.1F, 2.5F, 2.5F);
            b.box(ROPE, 0, 0, -4.7F, -27.5F, -6.2F, 9.4F, 1.2F, 12.4F);
            // sleeves along the crossbar, straw out of the cuffs
            for (int s = -1; s <= 1; s += 2) {
                b.box(CANVAS, 8, 0, -2.6F, -35.5F, s > 0 ? 6.0F : -16.0F, 5.2F, 5.0F, 10.0F);
                for (int i = 0; i < 6; i++) {
                    float a = i * 1.1F;
                    b.line(BAMBOO, 0, -33.0F, s * 16.0F, Mth.cos(a) * 2.0F, -33.0F + Mth.sin(a) * 2.2F + 1.0F, s * (19.5F + (i % 3) * 0.6F), 0.5F);
                }
            }
            for (int i = 0; i < 5; i++) b.line(BAMBOO, 0, -38.0F, -3.0F + i * 1.5F, (i - 2) * 0.6F, -40.5F, -3.5F + i * 1.7F, 0.5F);   // at the collar
            // the pumpkin head with its glowing face, the hat
            pumpkin(b, STEEL, 0.0F, -38.5F, 0.0F, 5.2F, 8.6F, false);
            b.box(FIRE, 5.32F, -45.0F, -3.4F, 0.12F, 1.6F, 1.8F);                             // eyes
            b.box(FIRE, 5.32F, -45.0F, 1.6F, 0.12F, 1.6F, 1.8F);
            b.box(FIRE, 5.32F, -44.0F, -2.9F, 0.12F, 0.6F, 0.8F);
            b.box(FIRE, 5.32F, -44.0F, 2.1F, 0.12F, 0.6F, 0.8F);
            b.box(FIRE, 5.32F, -42.6F, -0.5F, 0.12F, 1.0F, 1.0F);                             // nose
            for (int i = 0; i < 6; i++) {                                                     // the jagged grin
                b.box(FIRE, 5.32F, -40.8F + (i % 2) * 0.5F, -3.3F + i * 1.1F, 0.12F, 1.2F - (i % 2) * 0.5F, 1.0F);
            }
            b.disc(BLACK, 0, -47.6F, 0, 8.5F, 0.8F);
            b.box(BLACK, 0, 0, -3.6F, -51.5F, -3.6F, 7.2F, 4.0F, 7.2F);
            b.box(NAVY, 4, 0, -3.7F, -48.6F, -3.7F, 7.4F, 1.0F, 7.4F);
            b.box(BLACK, 4, 4, -2.4F, -54.5F, -2.4F, 4.8F, 3.2F, 4.8F);
            b.line(BLACK, 0, -54.5F, 0, -2.5F, -58.0F, 1.5F, 2.4F);
            b.line(BLACK, -2.5F, -58.0F, 1.5F, -5.0F, -58.5F, 3.2F, 1.4F);
            // the crow on its arm (its head looks about)
            CubeListBuilder crow = CubeListBuilder.create();
            box(crow, BLACK, 0, 0, -2.0F, -3.0F, -1.2F, 4.0F, 3.0F, 2.4F);
            box(crow, BLACK, 0, 4, -4.5F, -2.4F, -0.9F, 2.6F, 0.8F, 1.8F);
            box(crow, BLACK, 4, 4, -1.4F, -2.6F, -1.35F, 2.8F, 1.8F, 0.2F);
            box(crow, GOLD, 0, 0, -0.2F, 0.0F, -0.7F, 0.4F, 1.2F, 0.3F);
            box(crow, GOLD, 0, 0, -0.2F, 0.0F, 0.4F, 0.4F, 1.2F, 0.3F);
            PartDefinition crowPart = body.addOrReplaceChild("crow", crow, PartPose.offset(0, -34.8F, 12.0F));
            CubeListBuilder head = CubeListBuilder.create();
            box(head, BLACK, 0, 0, -1.1F, -2.0F, -1.0F, 2.2F, 2.0F, 2.0F);
            box(head, GOLD, 0, 0, 1.1F, -1.3F, -0.35F, 1.6F, 0.7F, 0.7F);
            box(head, WARM, 0, 0, 0.5F, -1.6F, 1.0F, 0.5F, 0.5F, 0.06F);
            box(head, WARM, 0, 0, 0.5F, -1.6F, -1.06F, 0.5F, 0.5F, 0.06F);
            crowPart.addOrReplaceChild("head", head, PartPose.offset(1.4F, -3.0F, 0.0F));
            body.addOrReplaceChild("frame", b.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Witch's Cauldron ═════════════
    // A pot-bellied iron cauldron on three legs over a crackling fire of logs, full to the brim with a glowing green
    // brew that bubbles (bubbles rising and popping, a skull bobbing in it), a witch's broom leaning on it, three
    // candles round it guttering.
    public static class WitchsCauldron extends Base {
        public static final ModelLayerLocation LAYER = layer("witchs_cauldron");
        static final int BUBBLES = 6;
        private final ModelPart skull;
        private final List<ModelPart> bubbles = new ArrayList<>();
        private final List<ModelPart> flames = new ArrayList<>();

        public WitchsCauldron(ModelPart root) {
            super(root);
            this.skull = this.decor.getChild("skull");
            for (int i = 0; i < BUBBLES; i++) this.bubbles.add(this.decor.getChild("bubble" + i));
            for (int i = 0; i < 5; i++) this.flames.add(this.decor.getChild("flame" + i));
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            float t = time;
            this.skull.y = -12.4F + 0.5F * Mth.sin(t * 0.08F);
            this.skull.yRot = t * 0.015F;
            for (int i = 0; i < BUBBLES; i++) {
                float phase = (t * 0.04F + i * 0.37F) % 1.0F;
                ModelPart b = this.bubbles.get(i);
                b.y = -12.0F - phase * 2.0F;
                float s = 0.4F + phase * 0.8F;
                b.xScale = s;
                b.yScale = s;
                b.zScale = s;
            }
            for (int i = 0; i < this.flames.size(); i++) {
                this.flames.get(i).yScale = 0.8F + 0.3F * Mth.sin(t * 0.5F + i * 1.7F) + 0.15F * Mth.sin(t * 1.3F + i);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            // the logs and the fire under it
            k.line(DARKWOOD, -6.0F, -0.8F, -3.0F, 6.0F, -0.8F, 3.0F, 1.6F);
            k.line(DARKWOOD, -6.0F, -0.8F, 3.0F, 6.0F, -0.8F, -3.0F, 1.6F);
            for (int i = 0; i < 5; i++) {
                CubeListBuilder flame = CubeListBuilder.create();
                float h = 3.0F + (i % 3);
                box(flame, FIRE, i * 2, 0, -0.8F, -h, -0.8F, 1.6F, h, 1.6F);
                double a = i * 2 * Math.PI / 5;
                root.addOrReplaceChild("flame" + i, flame, PartPose.offset((float) Math.cos(a) * 2.5F, -1.4F, (float) Math.sin(a) * 2.5F));
            }
            // three legs, the round iron belly, the rim
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3 + 0.5;
                k.box(GREY, (float) Math.cos(a) * 5.5F - 0.8F, -3.6F, (float) Math.sin(a) * 5.5F - 0.8F, 1.6F, 3.6F, 1.6F);
            }
            k.disc(GREY, 0, -4.4F, 0, 5.5F, 1.0F);
            k.ringY(GREY, 0, -6.0F, 0, 6.5F, 2.0F, 1.6F, 16, 0);
            k.ringY(GREY, 0, -11.0F, 0, 7.6F, 5.0F, 1.6F, 16, 4);
            k.ringY(GREY, 0, -12.8F, 0, 7.0F, 1.8F, 1.4F, 16, 8);
            k.ringY(GREY, 0, -13.6F, 0, 7.4F, 0.9F, 1.2F, 16, 12);                           // the rim
            k.box(GREY, -0.8F, -9.0F, 7.0F, 1.6F, 1.6F, 0.8F);                                // ring handles
            k.box(GREY, -0.8F, -9.0F, -7.8F, 1.6F, 1.6F, 0.8F);
            // the brew
            k.disc(ICE, 0, -12.6F, 0, 6.4F, 0.8F);   // (ICE: the glowing brew)
            k.disc(GREY, 0, -6.0F, 0, 6.0F, 1.0F);
            CubeListBuilder skull = CubeListBuilder.create();
            box(skull, BONE, 0, 0, -1.6F, -2.6F, -1.4F, 3.2F, 2.8F, 2.8F);
            box(skull, BLACK, 0, 0, 1.62F, -2.0F, -1.0F, 0.06F, 0.8F, 0.8F);
            box(skull, BLACK, 0, 0, 1.62F, -2.0F, 0.2F, 0.06F, 0.8F, 0.8F);
            root.addOrReplaceChild("skull", skull, PartPose.offset(2.0F, -12.4F, -1.5F));
            for (int i = 0; i < BUBBLES; i++) {
                CubeListBuilder bubble = CubeListBuilder.create();
                box(bubble, ICE, 0, 0, -0.6F, -0.6F, -0.6F, 1.2F, 1.2F, 1.2F);
                double a = i * 2.4;
                root.addOrReplaceChild("bubble" + i, bubble, PartPose.offset((float) Math.cos(a) * (2.0F + i % 3), -12.4F, (float) Math.sin(a) * (2.0F + i % 3)));
            }
            // the broom leaning on it
            k.line(WOOD, 9.0F, 0.0F, 7.0F, 3.0F, -24.0F, 5.5F, 0.9F);
            for (int i = 0; i < 7; i++) {
                k.line(BAMBOO, 8.6F, -4.5F, 6.9F, 9.6F + (i - 3) * 0.5F, 0.0F, 7.0F + (i % 3 - 1) * 0.6F, 0.6F);
            }
            k.box(ROPE, 8.0F, -5.2F, 6.3F, 1.4F, 1.0F, 1.4F);
            // candles round it
            float[][] candles = {{-9.0F, -6.0F, 5.0F}, {-8.0F, 7.0F, 3.5F}, {8.5F, -7.5F, 4.2F}};
            for (float[] cnd : candles) {
                k.box(WHITE, cnd[0] - 0.7F, -cnd[2], cnd[1] - 0.7F, 1.4F, cnd[2], 1.4F);
                k.box(FIRE, cnd[0] - 0.3F, -cnd[2] - 1.2F, cnd[1] - 0.3F, 0.6F, 1.2F, 0.6F);
            }
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Big Pumpkin Pile (2 x 2 blocks, 2 high) ═════════════
    // Two hay bales tied with twine and a third on top of them, pumpkins of every size heaped round and on them (orange,
    // a white one, a yellow squash, striped green gourds), a sheaf of dried corn stalks tied upright behind, rising
    // above it all, a few fallen leaves.
    public static class PumpkinPile extends Base {
        public static final ModelLayerLocation LAYER = layer("pumpkin_pile");

        public PumpkinPile(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            // the hay bales (DECK: hay) and their twine
            for (int s = -1; s <= 1; s += 2) {
                float z0 = s > 0 ? 0.5F : -15.5F;
                k.box(DECK, 0, 0, -15.0F, -10.0F, z0, 11.0F, 10.0F, 15.0F);
                k.box(ROPE, 0, 0, -15.1F, -10.1F, z0 + 4.0F, 11.2F, 10.2F, 0.8F);
                k.box(ROPE, 2, 0, -15.15F, -10.15F, z0 + 10.5F, 11.3F, 10.3F, 0.8F);
            }
            k.box(DECK, 8, 0, -14.5F, -20.0F, -9.0F, 11.0F, 10.0F, 18.0F);
            k.box(ROPE, 0, 0, -14.6F, -20.1F, -3.5F, 11.2F, 10.2F, 0.8F);
            k.box(ROPE, 2, 0, -14.65F, -20.15F, 3.0F, 11.3F, 10.3F, 0.8F);
            // the corn stalks tied in a sheaf behind, rising above it all (each in two pieces, to fit its wood)
            for (int i = 0; i < 11; i++) {
                float z = -7.5F + i * 1.5F;
                float lean = (i - 5) * 1.0F;
                float top = -38.0F - (i % 3) * 3;
                k.line(WOOD, -16.0F, 0.0F, z, -16.5F, top / 2, z + lean / 2, 1.0F);
                k.line(WOOD, -16.5F, top / 2, z + lean / 2, -17.0F, top, z + lean, 0.9F);
                k.line(WOOD, -16.5F, top * 0.6F, z + lean * 0.6F, -12.5F, top * 0.7F, z + lean * 0.4F + 2.5F, 0.6F);
            }
            k.box(ROPE, -17.8F, -22.0F, -9.0F, 2.6F, 1.6F, 18.0F);
            // the pumpkins (STEEL: orange and ribbed)
            pumpkin(k, STEEL, 5.5F, 0.0F, -1.5F, 8.5F, 10.0F, true);
            pumpkin(k, STEEL, 8.0F, 0.0F, 11.0F, 6.0F, 7.0F, true);
            pumpkin(k, STEEL, 5.0F, 0.0F, -12.5F, 5.5F, 6.5F, true);
            pumpkin(k, STEEL, -9.0F, -10.0F, -10.0F, 5.0F, 6.0F, true);
            pumpkin(k, WHITE, -9.0F, -10.0F, 10.0F, 4.5F, 5.5F, true);
            pumpkin(k, STEEL, -8.5F, -20.0F, 0.0F, 5.5F, 7.0F, true);
            pumpkin(k, STEEL, -2.0F, -10.0F, 0.5F, 3.2F, 4.0F, true);
            pumpkin(k, BRASS, -1.0F, 0.0F, 13.0F, 3.5F, 4.0F, true);
            pumpkin(k, MOSS, 13.0F, 0.0F, -7.0F, 2.4F, 2.6F, false);
            pumpkin(k, MOSS, 13.5F, 0.0F, 3.5F, 2.8F, 3.0F, false);
            pumpkin(k, MOSS, 2.0F, 0.0F, 15.0F, 2.0F, 2.2F, false);
            // fallen leaves
            for (int i = 0; i < 14; i++) {
                double a = i * 2.3;
                float r = 15.0F + (i % 3) * 2.0F;
                k.box(i % 3 == 0 ? RED : i % 3 == 1 ? ACCENT : BRASS, 0, 0, (float) Math.cos(a) * r, -0.15F, (float) Math.sin(a) * r, 2.0F, 0.15F, 1.6F);
            }
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Leaf Pile (2 x 2 blocks, 1 high) ═════════════
    // A big soft heap of autumn leaves (red, orange, gold, brown), lumpy, leaves scattered all round it on the ground,
    // a wooden rake leaning on it.
    public static class LeafPile extends Base {
        public static final ModelLayerLocation LAYER = layer("leaf_pile");

        public LeafPile(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            // the heap (DECK: leaves) in layers, lumps round its edge
            k.disc(DECK, 0, -5.0F, 0, 16.0F, 5.0F);
            k.disc(DECK, 0.5F, -9.0F, -0.5F, 14.0F, 4.0F);
            k.disc(DECK, -0.5F, -12.5F, 0.5F, 11.0F, 3.5F);
            k.disc(DECK, 0.5F, -15.0F, 0.0F, 7.5F, 2.5F);
            k.disc(DECK, 0.0F, -16.5F, 0.0F, 3.5F, 1.5F);
            float[][] lumps = {{11, 8, 6}, {-12, -7, 6}, {-8, 11, 5}, {9, -11, 5}, {-14, 3, 4}, {14, -1, 4}};
            for (float[] l : lumps) k.disc(DECK, l[0], -l[2] - 1.0F, l[1], l[2] + 1.5F, l[2]);
            // leaves scattered round it
            Zone[] colors = {RED, ACCENT, BRASS, DARKWOOD};
            for (int i = 0; i < 34; i++) {
                double a = i * 2.39996;
                float r = 16.0F + (i % 6) * 1.2F;
                CubeListBuilder leaf = CubeListBuilder.create();
                box(leaf, colors[i % colors.length], (i * 3) % 20, 0, -1.3F, -0.12F, -0.9F, 2.6F, 0.12F, 1.8F);
                root.addOrReplaceChild("leaf" + i, leaf, PartPose.offsetAndRotation((float) Math.cos(a) * r, 0.0F, (float) Math.sin(a) * r,
                    0.0F, (float) (a * 1.7), 0.0F));
            }
            // the rake leaning on it
            k.line(WOOD, 19.0F, 0.0F, 12.0F, 12.0F, -15.0F, 13.5F, 1.0F);
            k.line(WOOD, 12.0F, -15.0F, 13.5F, 5.0F, -30.0F, 15.0F, 1.0F);
            k.box(GREY, 17.5F, -2.0F, 8.0F, 1.2F, 1.2F, 8.0F);
            for (int i = 0; i < 7; i++) k.line(GREY, 18.1F, -1.5F, 8.4F + i * 1.2F, 20.0F, 0.0F, 8.2F + i * 1.25F, 0.35F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Present Pile (about 2 x 2 blocks) ═════════════
    // A heap of wrapped presents: big ones underneath, smaller stacked on top, each turned a little its own way, in
    // Christmas papers (red with white spots, blue with gold stars, green candy stripes, white with snowflakes, gold
    // foil, purple, tartan), tied with gold, red or silver ribbon, a bow of two loops on top of each.
    public static class PresentPile extends Base {
        public static final ModelLayerLocation LAYER = layer("present_pile");

        public PresentPile(ModelPart root) {
            super(root);
        }

        // {x, z, width, height, depth, the floor it sits on (y), its turn}
        static final float[][] GIFTS = {
            {-7, -7, 12, 10, 12, 0, 0.0F}, {7, -6, 10, 8, 10, 0, 0.25F}, {-6, 8, 10, 9, 9, 0, -0.2F}, {8, 8, 9, 7, 11, 0, 0.1F},
            {14, 1, 6, 5, 6, 0, 0.5F}, {-15, 1, 5, 6, 5, 0, -0.4F},
            {-6, -6, 9, 8, 9, -10, 0.4F}, {7, 7, 8, 6, 8, -7, -0.3F}, {-5, 7, 6, 5, 6, -9, 0.6F},
            {-5.5F, -5.5F, 6, 6, 6, -18, 0.9F}};
        // their papers and ribbons (BoatTex paints these zones as wrapping paper for the Present Pile)
        static final Zone[] PAPERS = {DECK, STEEL, CANVAS, WHITE, WOOD, OLDWOOD, WOOD, MOSS, DECK, STEEL};
        static final Zone[] RIBBONS = {GOLD, ICE, RED, RED, RED, ICE, RED, GOLD, ICE, GOLD};

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            for (int i = 0; i < GIFTS.length; i++) {
                float[] g = GIFTS[i];
                float w = g[2];
                float h = g[3];
                float d = g[4];
                Zone paper = PAPERS[i];
                Zone ribbon = RIBBONS[i];
                CubeListBuilder gift = CubeListBuilder.create();
                box(gift, paper, (i * 7) % 20, 0, -w / 2, -h, -d / 2, w, h, d);
                box(gift, paper, (i * 5) % 20 + 4, 4, -w / 2 - 0.25F, -h - 0.45F, -d / 2 - 0.25F, w + 0.5F, 1.2F, d + 0.5F);   // the lid
                box(gift, ribbon, 0, 0, -0.7F, -h - 0.5F, -d / 2 - 0.3F, 1.4F, h + 0.55F, d + 0.6F);
                box(gift, ribbon, 2, 0, -w / 2 - 0.35F, -h - 0.55F, -0.7F, w + 0.7F, h + 0.6F, 1.4F);
                box(gift, ribbon, 4, 2, -0.9F, -h - 1.4F, -0.9F, 1.8F, 0.9F, 1.8F);                                            // the knot
                PartDefinition part = root.addOrReplaceChild("gift" + i, gift, PartPose.offsetAndRotation(g[0], g[5], g[1], 0.0F, g[6], 0.0F));
                // the bow: two loops and two tails
                float s = Math.min(w, d) * 0.28F + 0.8F;
                line(part, "loop_a", ribbon, 0.0F, -h - 1.2F, 0.0F, s, -h - 1.2F - s * 0.8F, s * 0.4F, 0.7F);
                line(part, "loop_b", ribbon, s, -h - 1.2F - s * 0.8F, s * 0.4F, s * 0.4F, -h - 0.6F, s * 0.9F, 0.7F);
                line(part, "loop_c", ribbon, 0.0F, -h - 1.2F, 0.0F, -s, -h - 1.2F - s * 0.8F, -s * 0.4F, 0.7F);
                line(part, "loop_d", ribbon, -s, -h - 1.2F - s * 0.8F, -s * 0.4F, -s * 0.4F, -h - 0.6F, -s * 0.9F, 0.7F);
                line(part, "tail_a", ribbon, 0.0F, -h - 0.9F, 0.0F, s * 0.5F, -h - 0.4F, -s * 1.1F, 0.6F);
                line(part, "tail_b", ribbon, 0.0F, -h - 0.9F, 0.0F, -s * 0.5F, -h - 0.4F, s * 1.1F, 0.6F);
            }
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // a candy cane: a striped shaft `h` tall (WHITE: red and white stripes), its crook curling over toward +x, `r` its
    // thickness, into a part
    static void cane(PartDefinition part, String name, float h, float r, float crook) {
        CubeListBuilder shaft = CubeListBuilder.create();
        for (float y = 0; y < h - 0.01F; y += 8.0F) {
            float seg = Math.min(8.0F, h - y);
            box(shaft, WHITE, (int) (y * 0.75F) % 40, 0, -r / 2, -y - seg, -r / 2, r, seg, r);
        }
        part.addOrReplaceChild(name + "_shaft", shaft, PartPose.ZERO);
        float cx = crook;
        int steps = 7;
        float px = 0, py = -h;
        for (int i = 1; i <= steps; i++) {
            double a = Math.PI - Math.PI * i / steps;
            float x = cx + (float) Math.cos(a) * crook;
            float y = -h - (float) Math.sin(a) * crook;
            line(part, name + "_crook" + i, WHITE, px, py, 0, x, y, 0, r * 0.95F);
            px = x;
            py = y;
        }
        line(part, name + "_tip", WHITE, px, py, 0, px, py + crook * 0.6F, 0, r * 0.9F);
    }

    // ═════════════ the Candy Cane Pole (4 blocks tall) ═════════════
    // A giant striped candy cane standing up, its crook at the top, a green bow tied round it with a gold knot, a little
    // drift of snow at its foot. String lights hang from the top of it (it's a holder, like a String Light Hook).
    public static class CandyCanePole extends Base {
        public static final ModelLayerLocation LAYER = layer("candy_cane_pole");

        public CandyCanePole(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.disc(ICE, 0, -1.2F, 0, 4.5F, 1.2F);
            PartDefinition pole = root.addOrReplaceChild("pole", CubeListBuilder.create(), PartPose.ZERO);
            cane(pole, "cane", 56.0F, 3.4F, 4.2F);
            // the bow under the crook
            k.box(ACCENT, -1.9F, -47.0F, -1.9F, 3.8F, 2.0F, 3.8F);
            k.box(GOLD, 1.8F, -47.4F, -0.8F, 0.8F, 2.8F, 1.6F);
            k.line(ACCENT, 2.2F, -46.0F, 0.0F, 5.0F, -49.0F, 2.6F, 1.0F);
            k.line(ACCENT, 5.0F, -49.0F, 2.6F, 2.6F, -47.6F, 1.2F, 1.0F);
            k.line(ACCENT, 2.2F, -46.0F, 0.0F, 5.0F, -49.0F, -2.6F, 1.0F);
            k.line(ACCENT, 5.0F, -49.0F, -2.6F, 2.6F, -47.6F, -1.2F, 1.0F);
            k.line(ACCENT, 2.4F, -45.6F, 0.4F, 3.6F, -41.0F, 1.6F, 0.8F);
            k.line(ACCENT, 2.4F, -45.6F, -0.4F, 3.6F, -41.0F, -1.6F, 0.8F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Candy Canes (three in one) ═════════════
    // Three candy canes of different heights leaning together, their crooks turned different ways, tied round the middle
    // with a big red bow and a sprig of holly (green leaves, red berries), standing in a little drift of snow.
    public static class CandyCanes extends Base {
        public static final ModelLayerLocation LAYER = layer("candy_canes");

        public CandyCanes(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.disc(ICE, 0, -1.4F, 0, 7.0F, 1.4F);
            k.disc(ICE, 1.0F, -2.4F, 0.5F, 4.0F, 1.0F);
            // {x, z, height, turn, lean}
            float[][] canes = {{-1.6F, -1.2F, 24.0F, 0.0F, -0.10F}, {1.8F, -0.6F, 20.0F, 2.3F, -0.14F}, {0.0F, 1.9F, 16.0F, 4.1F, -0.18F}};
            for (int i = 0; i < canes.length; i++) {
                float[] cn = canes[i];
                PartDefinition tip = spoke(root, "cane" + i, cn[0], -1.2F, cn[1], cn[3], cn[4]);
                cane(tip, "c", cn[2], 2.2F, 2.8F);
            }
            // the bow round the middle, the holly
            k.box(RED, -3.4F, -10.0F, -3.0F, 6.8F, 1.8F, 6.8F);
            k.box(RED, 3.2F, -10.6F, -0.9F, 1.4F, 2.8F, 1.8F);
            k.line(RED, 4.0F, -9.6F, 0.0F, 7.0F, -12.6F, 2.8F, 1.1F);
            k.line(RED, 7.0F, -12.6F, 2.8F, 4.3F, -10.4F, 1.4F, 1.1F);
            k.line(RED, 4.0F, -9.6F, 0.0F, 7.0F, -12.6F, -2.8F, 1.1F);
            k.line(RED, 7.0F, -12.6F, -2.8F, 4.3F, -10.4F, -1.4F, 1.1F);
            k.line(RED, 4.2F, -9.0F, 0.6F, 5.2F, -4.5F, 2.2F, 0.9F);
            k.line(RED, 4.2F, -9.0F, -0.6F, 5.2F, -4.5F, -2.2F, 0.9F);
            k.box(MOSS, 2.6F, -12.2F, -3.6F, 3.0F, 0.4F, 1.6F);
            k.box(MOSS, 2.4F, -12.0F, 2.0F, 3.0F, 0.4F, 1.6F);
            k.box(RED, 3.4F, -12.8F, -0.9F, 0.9F, 0.9F, 0.9F);
            k.box(RED, 3.6F, -12.7F, 0.2F, 0.9F, 0.9F, 0.9F);
            k.box(RED, 2.8F, -13.2F, -0.3F, 0.9F, 0.9F, 0.9F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ════════════════════ FALL: the autumn trees ════════════════════

    // the leaves of the autumn trees: a big picture zone below y 180 (tools/BoatTex.cs paints it per tree)
    static final Zone FOLIAGE = new Zone(0, 180, 256, 76);

    // a canopy of leafy clumps (boxes turned every way) round (0, cy), radius rx out and ry up and down
    static void canopy(PartDefinition root, int clumps, float cy, float rx, float ry, float size) {
        for (int i = 0; i < clumps; i++) {
            double a = i * 2.39996;
            float f = (i % 5) / 4.0F;
            float r = rx * (0.35F + 0.65F * ((i * 37) % 11) / 10.0F);
            float y = cy + ry * (f * 2 - 1) * 0.8F + (r > rx * 0.8F ? ry * 0.25F : 0.0F);
            float s = size * (0.7F + 0.3F * ((i * 13) % 7) / 6.0F);
            CubeListBuilder clump = CubeListBuilder.create();
            box(clump, FOLIAGE, (i * 29) % 140, (i * 11) % 20, -s / 2, -s * 0.4F, -s / 2, s, s * 0.8F, s);
            root.addOrReplaceChild("clump" + i, clump, PartPose.offsetAndRotation((float) Math.cos(a) * r, y, (float) Math.sin(a) * r,
                (i % 3 - 1) * 0.35F, (float) a, (i % 2) * 0.3F));
        }
        CubeListBuilder top = CubeListBuilder.create();
        box(top, FOLIAGE, 60, 10, -size * 0.6F, -size * 0.5F, -size * 0.6F, size * 1.2F, size, size * 1.2F);
        root.addOrReplaceChild("crown", top, PartPose.offset(0, cy - ry * 0.9F, 0));
    }

    // fallen leaves in a ring round a tree
    static void fallen(BoatKit k, float r0, float r1, int n, Zone[] colors) {
        for (int i = 0; i < n; i++) {
            double a = i * 2.39996;
            float r = r0 + (r1 - r0) * ((i * 7) % 10) / 9.0F;
            k.box(colors[i % colors.length], 0, 0, (float) Math.cos(a) * r, -0.12F, (float) Math.sin(a) * r, 2.2F, 0.12F, 1.6F);
        }
    }

    // ═════════════ the Autumn Maple (about 6 blocks tall) ═════════════
    // A spreading maple in full autumn colour: a thick dark trunk with roots, branches reaching into a round crown of red,
    // orange and gold leaves, fallen leaves all round its foot.
    public static class AutumnMaple extends Base {
        public static final ModelLayerLocation LAYER = layer("autumn_maple");

        public AutumnMaple(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(DARKWOOD, 0, 0, -3.2F, -24.0F, -3.2F, 6.4F, 24.0F, 6.4F);
            k.box(DARKWOOD, 8, 0, -2.6F, -46.0F, -2.6F, 5.2F, 22.0F, 5.2F);
            for (int i = 0; i < 5; i++) {
                double a = i * 1.26;
                k.line(DARKWOOD, 0, -1.0F, 0, (float) Math.cos(a) * 6.0F, 0.0F, (float) Math.sin(a) * 6.0F, 2.0F);
            }
            float[][] branches = {{14, -62, 6}, {-12, -60, -9}, {6, -66, -15}, {-8, -68, 13}, {16, -56, -10}, {-15, -55, 4}};
            for (float[] b : branches) {
                k.line(DARKWOOD, 0, -40.0F, 0, b[0] * 0.55F, (b[1] - 40) / 2.0F, b[2] * 0.55F, 2.2F);
                k.line(DARKWOOD, b[0] * 0.55F, (b[1] - 40) / 2.0F, b[2] * 0.55F, b[0], b[1], b[2], 1.4F);
            }
            fallen(k, 6.0F, 30.0F, 30, new Zone[]{RED, ACCENT, BRASS, RED, DARKWOOD});
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            canopy(root, 30, -70.0F, 30.0F, 16.0F, 22.0F);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Autumn Birch (about 6.5 blocks tall) ═════════════
    // A slender white birch, black marks on its bark, a tall oval crown of golden yellow leaves, a side branch, golden
    // leaves fallen round it.
    public static class AutumnBirch extends Base {
        public static final ModelLayerLocation LAYER = layer("autumn_birch");

        public AutumnBirch(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(WOOD, 0, 0, -2.2F, -24.0F, -2.2F, 4.4F, 24.0F, 4.4F);
            k.box(WOOD, 8, 0, -1.9F, -48.0F, -1.9F, 3.8F, 24.0F, 3.8F);
            k.box(WOOD, 16, 0, -1.5F, -66.0F, -1.5F, 3.0F, 18.0F, 3.0F);
            k.line(WOOD, 0, -38.0F, 0, 9.0F, -52.0F, 3.0F, 1.6F);
            k.line(WOOD, 0, -46.0F, 0, -8.0F, -60.0F, -4.0F, 1.4F);
            fallen(k, 4.0F, 22.0F, 22, new Zone[]{BRASS, ACCENT, BRASS, DARKWOOD});
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            canopy(root, 24, -78.0F, 17.0F, 22.0F, 16.0F);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ════════════════════ HALLOWEEN: gravestones and bones ════════════════════

    // a grave mound in front of a stone (DECK: earth with grass), tufts of grass on it
    static void mound(BoatKit k) {
        k.box(DECK, 0, 0, 2.5F, -1.4F, -4.5F, 12.0F, 1.4F, 9.0F);
        k.box(DECK, 6, 4, 3.5F, -2.4F, -3.6F, 10.0F, 1.0F, 7.2F);
        for (int i = 0; i < 7; i++) {
            float x = 3.5F + i * 1.6F;
            float z = (i % 3 - 1) * 2.8F;
            k.line(MOSS, x, -2.2F, z, x + 0.3F, -3.6F, z + 0.4F, 0.4F);
        }
    }

    // RIP in carved letters on a stone's face (at x), centred at z = 0, its top at y
    static void rip(BoatKit k, float x, float y) {
        float t = 0.08F;
        // R
        k.box(BLACK, x, y, -3.6F, t, 4.0F, 0.6F);
        k.box(BLACK, x, y, -3.6F, t, 0.6F, 1.8F);
        k.box(BLACK, x, y + 1.7F, -3.6F, t, 0.6F, 1.8F);
        k.box(BLACK, x, y + 0.3F, -2.0F, t, 1.6F, 0.6F);
        k.box(BLACK, x, y + 2.2F, -2.4F, t, 1.8F, 0.6F);
        // I
        k.box(BLACK, x, y, -0.3F, t, 4.0F, 0.6F);
        // P
        k.box(BLACK, x, y, 1.4F, t, 4.0F, 0.6F);
        k.box(BLACK, x, y, 1.4F, t, 0.6F, 1.8F);
        k.box(BLACK, x, y + 1.7F, 1.4F, t, 0.6F, 1.8F);
        k.box(BLACK, x, y + 0.3F, 3.0F, t, 1.6F, 0.6F);
    }

    // ═════════════ the Rounded Gravestone ═════════════
    // A weathered grey headstone with a rounded top, RIP carved in it and lines too worn to read, a crack, moss creeping
    // over it, a grave mound in front with tufts of grass.
    public static class GraveRound extends Base {
        public static final ModelLayerLocation LAYER = layer("gravestone_round");

        public GraveRound(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(GREY, 0, 0, -2.5F, -2.0F, -7.0F, 5.0F, 2.0F, 14.0F);
            k.box(GREY, 4, 2, -1.5F, -15.0F, -6.0F, 3.0F, 13.0F, 12.0F);
            k.box(GREY, 8, 4, -1.5F, -16.5F, -5.0F, 3.0F, 1.5F, 10.0F);
            k.box(GREY, 10, 6, -1.5F, -17.6F, -3.5F, 3.0F, 1.1F, 7.0F);
            k.box(GREY, 12, 8, -1.5F, -18.3F, -1.8F, 3.0F, 0.7F, 3.6F);
            rip(k, 1.55F, -13.5F);
            k.box(BLACK, 1.55F, -8.0F, -3.5F, 0.08F, 0.4F, 7.0F);
            k.box(BLACK, 1.55F, -6.6F, -2.8F, 0.08F, 0.4F, 5.6F);
            k.line(BLACK, 1.56F, -16.0F, 3.2F, 1.56F, -11.0F, 1.6F, 0.25F);
            k.line(BLACK, 1.56F, -11.0F, 1.6F, 1.56F, -8.5F, 2.8F, 0.25F);
            k.box(MOSS, -1.6F, -16.8F, -4.8F, 3.2F, 0.8F, 3.0F);
            k.box(MOSS, 1.5F, -4.0F, 2.5F, 0.2F, 2.0F, 3.0F);
            k.box(MOSS, -2.6F, -2.2F, -6.8F, 5.2F, 0.4F, 3.0F);
            mound(k);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Cross Gravestone ═════════════
    // A stone cross with a ring round its heart (a Celtic cross) on a two-step plinth, mossy, a grave mound in front.
    public static class GraveCross extends Base {
        public static final ModelLayerLocation LAYER = layer("gravestone_cross");

        public GraveCross(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(GREY, 0, 0, -3.0F, -2.0F, -4.5F, 6.0F, 2.0F, 9.0F);
            k.box(GREY, 4, 2, -2.2F, -3.6F, -3.2F, 4.4F, 1.6F, 6.4F);
            k.box(GREY, 8, 0, -1.2F, -25.0F, -1.4F, 2.4F, 21.4F, 2.8F);
            k.box(GREY, 12, 4, -1.2F, -20.0F, -6.5F, 2.4F, 2.6F, 13.0F);
            int seg = 12;
            for (int i = 0; i < seg; i++) {
                double a0 = i * 2 * Math.PI / seg;
                double a1 = (i + 1) * 2 * Math.PI / seg;
                k.line(GREY, 0, -18.7F + (float) Math.sin(a0) * 4.2F, (float) Math.cos(a0) * 4.2F,
                    0, -18.7F + (float) Math.sin(a1) * 4.2F, (float) Math.cos(a1) * 4.2F, 1.0F);
            }
            k.box(MOSS, -1.3F, -25.4F, -1.0F, 2.6F, 0.6F, 2.0F);
            k.box(MOSS, -3.1F, -2.2F, 1.5F, 3.0F, 0.4F, 3.0F);
            k.box(BLACK, 1.25F, -12.0F, -0.8F, 0.08F, 3.0F, 0.3F);
            mound(k);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Old Tombstone ═════════════
    // A tall old stone with a pointed (gothic) top, leaning back a little, a skull and crossbones carved on its face,
    // cracked and chipped; a candle guttering on its plinth (it glows); a grave mound in front.
    public static class GraveTomb extends Base {
        public static final ModelLayerLocation LAYER = layer("gravestone_tomb");

        public GraveTomb(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(GREY, 0, 0, -3.0F, -2.0F, -7.0F, 6.0F, 2.0F, 14.0F);
            k.box(WHITE, 3.4F, -5.0F, 4.0F, 1.2F, 3.0F, 1.2F);
            k.box(FIRE, 3.7F, -6.2F, 4.3F, 0.6F, 1.2F, 0.6F);
            mound(k);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            PartDefinition slab = root.addOrReplaceChild("slab", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -2.0F, 0, 0, 0, -0.07F));
            BoatKit s = new BoatKit(slab);
            s.box(GREY, 0, 0, -1.6F, -20.0F, -5.0F, 3.2F, 20.0F, 10.0F);
            s.box(GREY, 4, 2, -1.6F, -22.0F, -4.0F, 3.2F, 2.0F, 8.0F);
            s.box(GREY, 8, 4, -1.6F, -23.5F, -2.6F, 3.2F, 1.5F, 5.2F);
            s.box(GREY, 10, 6, -1.6F, -24.6F, -1.2F, 3.2F, 1.1F, 2.4F);
            s.box(BONE, 1.6F, -17.0F, -1.8F, 0.3F, 3.0F, 3.6F);
            s.box(BONE, 1.6F, -14.4F, -1.1F, 0.3F, 1.0F, 2.2F);
            s.box(BLACK, 1.92F, -16.2F, -1.3F, 0.06F, 1.0F, 1.0F);
            s.box(BLACK, 1.92F, -16.2F, 0.3F, 0.06F, 1.0F, 1.0F);
            s.line(BONE, 1.75F, -12.6F, -3.0F, 1.75F, -9.6F, 3.0F, 0.6F);
            s.line(BONE, 1.75F, -12.6F, 3.0F, 1.75F, -9.6F, -3.0F, 0.6F);
            s.line(BLACK, 1.62F, -22.0F, -2.5F, 1.62F, -17.5F, -3.8F, 0.25F);
            s.line(BLACK, 1.62F, -6.0F, 2.0F, 1.62F, -2.0F, 3.6F, 0.25F);
            s.box(MOSS, -1.7F, -23.8F, -2.2F, 3.4F, 0.8F, 2.0F);
            slab.addOrReplaceChild("stone", s.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Pile of Bones ═════════════
    // A heap of old bones every which way, a ribcage, two skulls (one on top grinning out).
    public static class BonePile extends Base {
        public static final ModelLayerLocation LAYER = layer("bone_pile");

        public BonePile(ModelPart root) {
            super(root);
        }

        static void skull(BoatKit k, float x, float y, float z, float s) {
            k.box(BONE, x - 1.6F * s, y - 2.6F * s, z - 1.4F * s, 3.2F * s, 2.6F * s, 2.8F * s);
            k.box(BONE, x - 0.6F * s, y - 1.0F * s, z - 1.0F * s, 2.0F * s, 1.0F * s, 2.0F * s);
            k.box(BLACK, x + 1.62F * s, y - 2.0F * s, z - 1.0F * s, 0.06F, 0.8F * s, 0.8F * s);
            k.box(BLACK, x + 1.62F * s, y - 2.0F * s, z + 0.2F * s, 0.06F, 0.8F * s, 0.8F * s);
            k.box(BLACK, x + 1.42F * s, y - 0.8F * s, z - 0.3F * s, 0.06F, 0.4F * s, 0.6F * s);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            for (int i = 0; i < 20; i++) {
                double a = i * 2.39996;
                float r = 1.0F + (i % 5) * 1.3F;
                float y = -0.5F - (5 - i % 5) * 0.7F;
                float len = 4.0F + (i % 3) * 1.5F;
                float cx = (float) Math.cos(a) * r;
                float cz = (float) Math.sin(a) * r;
                double b = a * 1.7 + i;
                float dx = (float) Math.cos(b) * len / 2;
                float dz = (float) Math.sin(b) * len / 2;
                float dy = (i % 4 - 1.5F) * 0.6F;
                k.line(BONE, cx - dx, y + dy, cz - dz, cx + dx, y - dy, cz + dz, 0.6F);
                k.box(BONE, cx - dx - 0.5F, y + dy - 0.5F, cz - dz - 0.5F, 1.0F, 1.0F, 1.0F);
                k.box(BONE, cx + dx - 0.5F, y - dy - 0.5F, cz + dz - 0.5F, 1.0F, 1.0F, 1.0F);
            }
            for (int i = 0; i < 4; i++) {
                float z = -3.0F + i * 1.2F;
                k.line(BONE, -4.0F, -0.6F, z, -2.0F, -3.6F, z + 0.2F, 0.45F);
                k.line(BONE, -2.0F, -3.6F, z + 0.2F, 0.0F, -0.8F, z, 0.45F);
            }
            k.line(BONE, -2.0F, -3.7F, -3.2F, -2.0F, -3.7F, 1.0F, 0.6F);
            skull(k, 0.5F, -4.0F, 0.5F, 1.0F);
            skull(k, 4.5F, 0.0F, -3.5F, 0.8F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ════════════════════ SUMMER: the beach ════════════════════

    // ═════════════ the Beach Towel ═════════════
    // A striped beach towel spread on the ground (2 blocks long), its head end rolled up for a pillow, sunglasses and a
    // pair of flip-flops on it.
    public static class BeachTowel extends Base {
        public static final ModelLayerLocation LAYER = layer("beach_towel");

        public BeachTowel(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(DECK, 0, 0, -15.0F, -0.3F, -7.5F, 30.0F, 0.3F, 15.0F);
            k.box(DECK, 40, 0, -15.5F, -2.2F, -7.5F, 4.0F, 2.0F, 15.0F);
            k.box(BLACK, 4.0F, -0.8F, -2.6F, 1.6F, 0.5F, 2.0F);
            k.box(BLACK, 4.0F, -0.8F, 0.6F, 1.6F, 0.5F, 2.0F);
            k.box(BLACK, 4.5F, -0.75F, -0.6F, 0.4F, 0.3F, 1.2F);
            k.box(BLACK, 3.9F, -0.7F, -3.2F, 0.3F, 0.3F, 0.6F);
            k.box(RED, 10.0F, -0.8F, -3.6F, 4.6F, 0.5F, 1.8F);
            k.box(RED, 10.6F, -0.8F, -0.8F, 4.6F, 0.5F, 1.8F);
            k.line(WHITE, 13.0F, -0.8F, -3.0F, 11.6F, -1.3F, -2.4F, 0.3F);
            k.line(WHITE, 13.6F, -0.8F, -0.2F, 12.2F, -1.3F, 0.4F, 0.3F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Beach Ball ═════════════
    // A big striped beach ball (red, white, blue, yellow, green) with white caps, rocking gently as if the breeze
    // nudges it.
    public static class BeachBall extends Base {
        public static final ModelLayerLocation LAYER = layer("beach_ball");
        private final ModelPart ball;

        public BeachBall(ModelPart root) {
            super(root);
            this.ball = this.decor.getChild("ball");
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            this.ball.xRot = 0.08F * Mth.sin(time * 0.05F);
            this.ball.zRot = 0.06F * Mth.sin(time * 0.037F + 1.0F);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            PartDefinition ball = root.addOrReplaceChild("ball", CubeListBuilder.create(), PartPose.offset(0, -5.0F, 0));
            BoatKit k = new BoatKit(ball);
            float r = 5.0F;
            int layers = 10;
            // six coloured slices (red, white, blue, yellow, white, green), each a stack of wedges round the middle
            Zone[] slices = {ICE, WHITE, STEEL, MOSS, WHITE, VINE};
            for (int s = 0; s < 6; s++) {
                CubeListBuilder wedge = CubeListBuilder.create();
                for (int i = 0; i < layers; i++) {
                    float h = 2 * r / layers;
                    float y0 = -r + i * h;
                    float yc = y0 + h / 2;
                    float rr = Mth.sqrt(Math.max(0.5F, r * r - yc * yc));
                    BoatKit.box(wedge, slices[s], (i * 3) % 8, 0, 0.0F, y0 + s * 0.004F, -rr / 2, rr * 0.866F, h, rr);
                }
                ball.addOrReplaceChild("slice" + s, wedge, PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, (float) (s * Math.PI / 3 + Math.PI / 6), 0.0F));
            }
            k.box(WHITE, -1.0F, -r - 0.1F, -1.0F, 2.0F, 0.3F, 2.0F);
            k.box(WHITE, -1.0F, r - 0.2F, -1.0F, 2.0F, 0.3F, 2.0F);
            ball.addOrReplaceChild("shell", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Beach Umbrella (about 3 blocks tall) ═════════════
    // A tall white pole stuck in a little mound of sand, a tilted canopy of red and white panels with a fringe round its
    // edge and a gold knob on top, swaying a little in the breeze.
    public static class BeachUmbrella extends Base {
        public static final ModelLayerLocation LAYER = layer("beach_umbrella");
        private final ModelPart canopy;

        public BeachUmbrella(ModelPart root) {
            super(root);
            this.canopy = this.decor.getChild("canopy");
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            this.canopy.zRot = 0.16F + 0.03F * Mth.sin(time * 0.05F);
            this.canopy.xRot = 0.03F * Mth.sin(time * 0.041F + 0.7F);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.disc(BAMBOO, 0, -1.5F, 0, 4.0F, 1.5F);
            k.line(WHITE, 0, 0.0F, 0, 0, -22.0F, 0, 1.3F);
            k.line(WHITE, 0, -22.0F, 0, 0, -44.0F, 0, 1.2F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            PartDefinition canopy = root.addOrReplaceChild("canopy", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -44.0F, 0, 0, 0, 0.16F));
            for (int p = 0; p < 8; p++) {
                PartDefinition rib = canopy.addOrReplaceChild("rib" + p, CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, (float) (p * Math.PI / 4), 0));
                CubeListBuilder panel = CubeListBuilder.create();
                Zone z = p % 2 == 0 ? DECK : WHITE;
                box(panel, z, p * 3, 0, 0.0F, -0.3F, -4.4F, 20.0F, 0.6F, 8.8F);
                box(panel, p % 2 == 0 ? WHITE : DECK, 0, 0, 19.6F, -0.1F, -4.0F, 0.8F, 1.6F, 8.0F);
                rib.addOrReplaceChild("panel", panel, PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.42F));
            }
            CubeListBuilder knob = CubeListBuilder.create();
            box(knob, GOLD, 0, 0, -0.9F, -2.4F, -0.9F, 1.8F, 1.8F, 1.8F);
            canopy.addOrReplaceChild("knob", knob, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Beach Chair ═════════════
    // A low wooden folding beach chair with a blue-and-white striped canvas seat and back, armrests, and a glass of
    // orange juice with a slice of lemon on one armrest.
    public static class BeachChair extends Base {
        public static final ModelLayerLocation LAYER = layer("beach_chair");

        public BeachChair(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            for (int s = -1; s <= 1; s += 2) {
                float z = s * 5.6F;
                k.line(WOOD, 6.0F, 0.0F, z, 4.0F, -5.0F, z, 0.8F);
                k.line(WOOD, -4.0F, 0.0F, z, -1.0F, -4.4F, z, 0.8F);
                k.line(WOOD, -1.0F, -4.0F, z, 5.5F, -5.0F, z, 0.8F);
                k.line(WOOD, -1.0F, -4.0F, z, -6.0F, -15.0F, z, 0.8F);
                k.line(WOOD, 5.2F, -8.0F, s * 6.0F, -3.0F, -8.0F, s * 6.0F, 0.9F);
                k.line(WOOD, 4.5F, -5.0F, s * 6.0F, 5.0F, -8.0F, s * 6.0F, 0.7F);
            }
            k.line(WOOD, -6.0F, -15.0F, -5.6F, -6.0F, -15.0F, 5.6F, 0.8F);
            k.box(CANVAS, 0, 0, -1.2F, -4.9F, -5.0F, 6.6F, 0.5F, 10.0F);
            CubeListBuilder back = CubeListBuilder.create();
            box(back, CANVAS, 8, 0, -0.25F, -11.0F, -5.0F, 0.5F, 11.0F, 10.0F);
            root.addOrReplaceChild("back", back, PartPose.offsetAndRotation(-1.2F, -4.6F, 0, 0, 0, -0.42F));
            k.box(TINT, 1.6F, -10.4F, 5.4F, 1.4F, 2.2F, 1.4F);
            k.box(BRASS, 1.7F, -10.1F, 5.5F, 1.2F, 0.4F, 1.2F);
            k.box(BRASS, 2.7F, -10.6F, 5.9F, 0.6F, 0.9F, 0.2F);
            k.line(WHITE, 2.2F, -10.0F, 6.0F, 1.6F, -12.2F, 6.4F, 0.2F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ════════════════════ SPRING: cute things ════════════════════

    // a little egg (two boxes making an oval), pastel
    static void egg(BoatKit k, Zone zone, float x, float y, float z, float s) {
        k.box(zone, 0, 0, x - 1.1F * s, y - 3.0F * s, z - 1.1F * s, 2.2F * s, 3.0F * s, 2.2F * s);
        k.box(zone, 4, 2, x - 0.8F * s, y - 3.5F * s, z - 0.8F * s, 1.6F * s, 3.8F * s, 1.6F * s);
    }

    // ═════════════ the Egg Basket ═════════════
    // A round wicker basket with a handle arching over, green grass spilling over its rim, full of pastel painted eggs
    // (pink with dots, baby blue with stripes, mint with zigzags, butter yellow, lavender).
    public static class EggBasket extends Base {
        public static final ModelLayerLocation LAYER = layer("egg_basket");

        public EggBasket(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.disc(WOOD, 0, -1.0F, 0, 5.0F, 1.0F);
            k.ringY(WOOD, 0, -6.0F, 0, 5.6F, 5.4F, 0.8F, 14, 0);
            k.ringY(WOOD, 0, -6.6F, 0, 6.0F, 0.8F, 1.0F, 14, 6);
            k.disc(MOSS, 0, -5.4F, 0, 5.0F, 0.6F);
            int seg = 10;
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * i / seg;
                double a1 = Math.PI * (i + 1) / seg;
                k.line(WOOD, 0, -6.5F - (float) Math.sin(a0) * 7.5F, (float) Math.cos(a0) * 5.6F,
                    0, -6.5F - (float) Math.sin(a1) * 7.5F, (float) Math.cos(a1) * 5.6F, 0.9F);
            }
            for (int i = 0; i < 12; i++) {
                double a = i * 2 * Math.PI / 12;
                float x = (float) Math.cos(a) * 5.4F;
                float z = (float) Math.sin(a) * 5.4F;
                k.line(MOSS, x, -6.4F, z, x * 1.15F, -8.0F, z * 1.15F, 0.35F);
            }
            Zone[] colors = {RED, NAVY, ACCENT, BRASS, STEEL, RED, NAVY};
            float[][] eggs = {{-2.0F, -1.5F}, {1.5F, -2.2F}, {2.4F, 1.2F}, {-0.6F, 2.4F}, {-2.8F, 1.0F}, {0.4F, 0.0F}, {2.8F, -0.6F}};
            for (int i = 0; i < eggs.length; i++) egg(k, colors[i], eggs[i][0], -5.2F - (i == 5 ? 1.2F : 0.0F), eggs[i][1], 1.0F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Giant Painted Egg ═════════════
    // A big egg (over a block tall) painted in pastel bands, zigzags and dots, sitting in a nest of straw.
    public static class GiantEgg extends Base {
        public static final ModelLayerLocation LAYER = layer("giant_egg");

        public GiantEgg(ModelPart root) {
            super(root);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            int layers = 16;
            for (int i = 0; i < layers; i++) {
                float t = i / (float) (layers - 1);
                float y = -1.5F - t * 19.0F;
                float r = 6.6F * (float) Math.pow(Math.sin(Math.PI * (0.06 + 0.94 * t)), 0.85) * (1 - 0.18F * t) + 0.4F;
                k.disc(DECK, 0, y - 1.25F, 0, r, 1.25F);
            }
            for (int i = 0; i < 16; i++) {
                double a = i * 2 * Math.PI / 16;
                float x = (float) Math.cos(a) * 6.5F;
                float z = (float) Math.sin(a) * 6.5F;
                k.line(BAMBOO, x, 0.0F, z, (float) Math.cos(a + 0.6) * 7.8F, -2.6F, (float) Math.sin(a + 0.6) * 7.8F, 0.6F);
                k.line(BAMBOO, x, -1.0F, z, (float) Math.cos(a - 0.5) * 7.4F, -3.2F, (float) Math.sin(a - 0.5) * 7.4F, 0.5F);
            }
            k.disc(BAMBOO, 0, -0.8F, 0, 7.0F, 0.8F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Spring Bunny ═════════════
    // A cute fluffy white bunny sitting up, long ears pink inside (they twitch), black eyes, a pink nose and whiskers, a
    // cotton tail, holding a little yellow flower.
    public static class SpringBunny extends Base {
        public static final ModelLayerLocation LAYER = layer("spring_bunny");
        private final ModelPart earL;
        private final ModelPart earR;

        public SpringBunny(ModelPart root) {
            super(root);
            this.earL = this.decor.getChild("ear_l");
            this.earR = this.decor.getChild("ear_r");
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            float twitch = Mth.sin(time * 0.6F) * (Mth.sin(time * 0.05F) > 0.85F ? 0.25F : 0.0F);
            this.earL.xRot = 0.18F + twitch;
            this.earR.xRot = -0.18F - 0.5F * twitch;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(WHITE, 0, 0, -4.0F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F);
            k.box(WHITE, 8, 0, -3.0F, -8.0F, -3.0F, 5.0F, 1.2F, 6.0F);
            k.box(WHITE, 4, 4, 1.0F, -12.0F, -2.6F, 5.0F, 5.0F, 5.2F);
            k.box(WHITE, 12, 4, 4.6F, -9.6F, -1.8F, 1.6F, 1.8F, 3.6F);
            k.box(BLACK, 6.02F, -11.0F, -1.9F, 0.06F, 1.0F, 0.8F);
            k.box(BLACK, 6.02F, -11.0F, 1.1F, 0.06F, 1.0F, 0.8F);
            k.box(RED, 6.2F, -9.8F, -0.4F, 0.3F, 0.6F, 0.8F);
            for (int s = -1; s <= 1; s += 2) {
                k.line(GREY, 6.3F, -9.4F, s * 0.6F, 6.6F, -9.8F, s * 3.0F, 0.12F);
                k.line(GREY, 6.3F, -9.2F, s * 0.6F, 6.6F, -9.0F, s * 3.0F, 0.12F);
                k.box(WHITE, 2.8F, -1.6F, s > 0 ? 1.2F : -2.6F, 2.4F, 1.6F, 1.4F);
                k.box(WHITE, -3.6F, -1.4F, s > 0 ? 3.4F : -5.0F, 4.8F, 1.4F, 1.6F);
                CubeListBuilder ear = CubeListBuilder.create();
                box(ear, WHITE, 16, 0, -0.7F, -6.5F, -0.6F, 1.4F, 6.5F, 1.2F);
                box(ear, RED, 0, 0, 0.6F, -5.6F, -0.35F, 0.12F, 5.0F, 0.7F);
                root.addOrReplaceChild(s > 0 ? "ear_l" : "ear_r", ear, PartPose.offsetAndRotation(2.8F, -11.6F, s * 1.4F, s * 0.18F, 0, -0.12F));
            }
            k.box(WHITE, -5.4F, -3.4F, -1.2F, 1.6F, 2.4F, 2.4F);
            k.line(ACCENT, 4.4F, -1.8F, 0.0F, 6.0F, -8.0F, 0.6F, 0.35F);
            k.box(BRASS, 5.4F, -9.0F, 0.0F, 1.4F, 1.4F, 1.4F);
            k.box(GOLD, 5.7F, -8.7F, 0.3F, 0.8F, 0.8F, 0.8F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            return LayerDefinition.create(mesh, 256, 256);
        }
    }

    // ═════════════ the Tulip Bed (2 x 2 blocks) ═════════════
    // A bed of dark earth bordered with grass, full of tulips (red, yellow, purple, white, pink) and a few daisies with
    // their leaves, swaying gently.
    public static class TulipBed extends Base {
        public static final ModelLayerLocation LAYER = layer("tulip_bed");
        private final ModelPart[] groups = new ModelPart[3];

        public TulipBed(ModelPart root) {
            super(root);
            for (int g = 0; g < 3; g++) this.groups[g] = this.decor.getChild("flowers" + g);
        }

        @Override
        public void setupAnim(Float time) {
            super.setupAnim(time);
            for (int g = 0; g < 3; g++) {
                this.groups[g].zRot = 0.04F * Mth.sin(time * 0.05F + g * 2.0F);
                this.groups[g].xRot = 0.03F * Mth.sin(time * 0.04F + g);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = start(mesh);
            BoatKit k = new BoatKit(root);
            k.box(DECK, 0, 0, -15.0F, -2.0F, -15.0F, 30.0F, 2.0F, 30.0F);
            root.addOrReplaceChild("statics", k.statics, PartPose.ZERO);
            Zone[] colors = {RED, BRASS, STEEL, WHITE, NAVY};
            for (int g = 0; g < 3; g++) {
                PartDefinition group = root.addOrReplaceChild("flowers" + g, CubeListBuilder.create(), PartPose.offset(0, -2.0F, 0));
                BoatKit f = new BoatKit(group);
                for (int i = g; i < 30; i += 3) {
                    float x = -12.0F + (i % 6) * 4.8F + ((i * 7) % 5 - 2) * 0.5F;
                    float z = -12.0F + (i / 6) * 5.6F + ((i * 3) % 5 - 2) * 0.5F;
                    float h = 6.0F + (i % 4) * 1.2F;
                    f.line(ACCENT, x, 0.0F, z, x + ((i % 3) - 1) * 0.4F, -h, z, 0.35F);
                    f.box(ACCENT, 0, 0, x - 1.6F, -2.2F, z - 0.2F, 3.2F, 0.3F, 0.8F);
                    if (i % 7 == 3) {
                        f.box(WHITE, x - 1.4F, -h - 0.3F, z - 0.4F, 2.8F, 0.3F, 0.8F);
                        f.box(WHITE, x - 0.4F, -h - 0.32F, z - 1.4F, 0.8F, 0.3F, 2.8F);
                        f.box(BRASS, x - 0.45F, -h - 0.55F, z - 0.45F, 0.9F, 0.3F, 0.9F);
                    } else {
                        Zone c = colors[i % colors.length];
                        f.box(c, x - 0.9F, -h - 2.0F, z - 0.9F, 1.8F, 2.0F, 1.8F);
                        f.box(c, x - 1.0F, -h - 2.6F, z - 0.3F, 0.5F, 0.8F, 0.6F);
                        f.box(c, x + 0.5F, -h - 2.6F, z - 0.3F, 0.5F, 0.8F, 0.6F);
                        f.box(c, x - 0.3F, -h - 2.7F, z - 1.0F, 0.6F, 0.8F, 0.5F);
                    }
                }
                group.addOrReplaceChild("stems", f.statics, PartPose.ZERO);
            }
            return LayerDefinition.create(mesh, 256, 256);
        }
    }
}

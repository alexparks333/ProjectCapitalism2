package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.decor.DecorKind;
import com.alexh.capitalism.decor.SeasonalDecorBlock;
import com.alexh.capitalism.decor.SeasonalDecorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Draws a seasonal decoration that's up (decor/SeasonalDecorBlock, Decorations.md): its model (client/DecorModels) on
// its block, turned to face the way it was placed, with a second, glowing pass (<name>_glow.png: lights, flames, the
// brew, the star). A String Light Hook draws its little iron hook and each strand of lights it runs to another hook:
// a wire sagging between them, bulbs every half block in turn red, gold, green, blue and pink, glowing and twinkling.
// Packed away, nothing. Seen from well beyond its block (a tree is 9 blocks tall; a strand reaches 24 blocks).
public class DecorRenderer implements BlockEntityRenderer<SeasonalDecorBlockEntity, DecorRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public DecorKind kind;
        public boolean up;
        public float turn;
        public float time;
        public float daylight = 1.0F;
        public Vec3 camRel = Vec3.ZERO;
        // the Ground Fog: the height of the ground in each block of the 5 x 5 round it (NaN: no ground to lie on)
        public float[] ground;   // (the fog's, shared from its once-a-second cache; null for anything else)
        // where its strands start (a holder: blocks above its foot), and each strand's run from there to its far end
        public float hookY;
        public double camDistSq;   // (a holder: how far the camera is, squared; far away its lights are drawn simpler)
        public List<Vec3> strands = List.of();   // (a holder's only: no list made for the rest)
    }

    // glows: whether it has glowing parts (GLOWING; most don't: trees, leaves, gravestones...). Each decoration was drawn
    // twice a frame, the second time for its glowing parts; without any, that second pass is skipped.
    private record Look(DecorModels.Base model, Identifier texture, Identifier glow, boolean glows) {
    }

    // the classic colours, in turn: red, green, blue, amber, pink
    private static final int[] BULBS = {0xFFFF2A20, 0xFF30E050, 0xFF3078FF, 0xFFFFA020, 0xFFFF50C0};
    // a white spot in the tree's texture, for the wire and the bulbs (coloured by their vertices)
    private static final Identifier WHITE_TEX = tex("christmas_tree");
    private static final float WHITE_U = 70.0F / 256.0F;
    private static final float WHITE_V = 90.0F / 256.0F;

    private final java.util.Map<DecorKind, Look> looks = new java.util.EnumMap<>(DecorKind.class);

    public DecorRenderer(BlockEntityRendererProvider.Context context) {
        this.add(context, DecorKind.CHRISTMAS_TREE, "christmas_tree", DecorModels.ChristmasTree.LAYER, DecorModels.ChristmasTree::new);
        this.add(context, DecorKind.SCARECROW, "spooky_scarecrow", DecorModels.Scarecrow.LAYER, DecorModels.Scarecrow::new);
        this.add(context, DecorKind.WITCHS_CAULDRON, "witchs_cauldron", DecorModels.WitchsCauldron.LAYER, DecorModels.WitchsCauldron::new);
        this.add(context, DecorKind.PUMPKIN_PILE, "pumpkin_pile", DecorModels.PumpkinPile.LAYER, DecorModels.PumpkinPile::new);
        this.add(context, DecorKind.LEAF_PILE, "leaf_pile", DecorModels.LeafPile.LAYER, DecorModels.LeafPile::new);
        this.add(context, DecorKind.PRESENT_PILE, "present_pile", DecorModels.PresentPile.LAYER, DecorModels.PresentPile::new);
        this.add(context, DecorKind.CANDY_CANE_POLE, "candy_cane_pole", DecorModels.CandyCanePole.LAYER, DecorModels.CandyCanePole::new);
        this.add(context, DecorKind.CANDY_CANES, "candy_canes", DecorModels.CandyCanes.LAYER, DecorModels.CandyCanes::new);
        this.add(context, DecorKind.GRAVESTONE_ROUND, "gravestone_round", DecorModels.GraveRound.LAYER, DecorModels.GraveRound::new);
        this.add(context, DecorKind.GRAVESTONE_CROSS, "gravestone_cross", DecorModels.GraveCross.LAYER, DecorModels.GraveCross::new);
        this.add(context, DecorKind.GRAVESTONE_TOMB, "gravestone_tomb", DecorModels.GraveTomb.LAYER, DecorModels.GraveTomb::new);
        this.add(context, DecorKind.BONE_PILE, "bone_pile", DecorModels.BonePile.LAYER, DecorModels.BonePile::new);
        this.add(context, DecorKind.AUTUMN_MAPLE, "autumn_maple", DecorModels.AutumnMaple.LAYER, DecorModels.AutumnMaple::new);
        this.add(context, DecorKind.AUTUMN_BIRCH, "autumn_birch", DecorModels.AutumnBirch.LAYER, DecorModels.AutumnBirch::new);
        this.add(context, DecorKind.BEACH_TOWEL, "beach_towel", DecorModels.BeachTowel.LAYER, DecorModels.BeachTowel::new);
        this.add(context, DecorKind.BEACH_BALL, "beach_ball", DecorModels.BeachBall.LAYER, DecorModels.BeachBall::new);
        this.add(context, DecorKind.BEACH_UMBRELLA, "beach_umbrella", DecorModels.BeachUmbrella.LAYER, DecorModels.BeachUmbrella::new);
        this.add(context, DecorKind.BEACH_CHAIR, "beach_chair", DecorModels.BeachChair.LAYER, DecorModels.BeachChair::new);
        this.add(context, DecorKind.EGG_BASKET, "egg_basket", DecorModels.EggBasket.LAYER, DecorModels.EggBasket::new);
        this.add(context, DecorKind.GIANT_EGG, "giant_egg", DecorModels.GiantEgg.LAYER, DecorModels.GiantEgg::new);
        this.add(context, DecorKind.SPRING_BUNNY, "spring_bunny", DecorModels.SpringBunny.LAYER, DecorModels.SpringBunny::new);
        this.add(context, DecorKind.TULIP_BED, "tulip_bed", DecorModels.TulipBed.LAYER, DecorModels.TulipBed::new);

    }

    private void add(BlockEntityRendererProvider.Context context, DecorKind kind, String name,
                     net.minecraft.client.model.geom.ModelLayerLocation layer, Function<ModelPart, DecorModels.Base> model) {
        this.looks.put(kind, new Look(model.apply(context.bakeLayer(layer)), tex(name), tex(name + "_glow"), GLOWING.contains(kind)));
    }

    // the decorations whose models use a glowing part of the texture (lights, a carved face, the brew, a candle). Checked
    // by hand: the glow textures share the boats' layout and always hold their glowing patches, used or not.
    private static final java.util.Set<DecorKind> GLOWING = java.util.EnumSet.of(DecorKind.CHRISTMAS_TREE, DecorKind.SCARECROW,
        DecorKind.WITCHS_CAULDRON, DecorKind.GRAVESTONE_TOMB);

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SeasonalDecorBlockEntity decor, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(decor, state, partialTicks, cameraPosition, breakProgress);
        var block = decor.getBlockState();
        state.kind = block.getBlock() instanceof SeasonalDecorBlock d ? d.kind() : null;
        state.up = block.hasProperty(SeasonalDecorBlock.UP) && block.getValue(SeasonalDecorBlock.UP);
        Direction facing = block.hasProperty(SeasonalDecorBlock.FACING) ? block.getValue(SeasonalDecorBlock.FACING) : Direction.SOUTH;
        // the model's front is +x; turn it to face the way it was placed (after the y-down flip)
        state.turn = switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        };
        state.time = decor.getLevel() == null ? 0 : decor.getLevel().getGameTime() + partialTicks;
        if (decor.getLevel() != null && state.kind == DecorKind.GROUND_FOG) {
            long t = Math.floorMod(com.alexh.capitalism.calendar.Calendar.clock(decor.getLevel()), 24000L);
            state.daylight = (float) Math.max(0, Math.min(1, 0.5 + 2 * Math.cos(2 * Math.PI * (t - 6000) / 24000.0)));
            state.camRel = cameraPosition.subtract(Vec3.atLowerCornerOf(decor.getBlockPos()));
            state.ground = cachedGround(decor.getLevel(), decor.getBlockPos());
        }
        state.strands = List.of();
        if (state.kind != null && state.kind.isHook() && decor.getLevel() != null) {
            state.strands = new ArrayList<>(decor.strands().size());
            state.hookY = state.kind.hookHeight();
            state.camDistSq = cameraPosition.distanceToSqr(Vec3.atCenterOf(decor.getBlockPos()));
            Vec3 start = new Vec3(0.5, state.hookY, 0.5);
            for (BlockPos to : decor.strands()) {
                var there = decor.getLevel().getBlockState(to);
                if (there.getBlock() instanceof SeasonalDecorBlock d && d.kind().isHook() && there.getValue(SeasonalDecorBlock.UP)) {
                    Vec3 end = Vec3.atLowerCornerOf(to.subtract(decor.getBlockPos())).add(0.5, d.kind().hookHeight(), 0.5);
                    state.strands.add(end.subtract(start));
                }
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.up || state.kind == null) return;
        if (state.kind == DecorKind.GROUND_FOG) {
            this.fog(state, poseStack, collector);
            return;
        }
        if (state.kind.isHook()) this.lights(state, poseStack, collector);
        if (state.kind == DecorKind.STRING_LIGHTS) return;
        Look look = this.looks.get(state.kind);
        if (look == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, state.turn);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(look.model(), state.time, poseStack, look.texture(), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (look.glows()) collector.submitModel(look.model(), state.time, poseStack, RenderTypes.eyes(look.glow()), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    // the hook, and its strands of lights: a twisted pair of dark green wires sagging between the hooks, and every 0.4
    // blocks a bulb hanging from it on a little green socket: a glowing teardrop (red, green, blue, amber, pink in turn)
    // with a soft halo of its own colour round it, gently twinkling, now and then a sparkle
    private void lights(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        int light = state.lightCoords;
        float time = state.time;
        float sy = state.hookY;
        boolean hook = state.kind == DecorKind.STRING_LIGHTS;
        // past 20 blocks: one plain wire, no sockets (a pixel wide from there); the bulbs and their glow stay
        boolean far = state.camDistSq > 400.0;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(WHITE_TEX), (pose, buffer) -> {
            // this render type shows both sides already: each face once
            oneSided = true;
            // a hook: an iron bracket with a ring under it (a Candy Cane Pole is its own model)
            if (hook) {
                cube(buffer, pose, 0.5F, 0.56F, 0.5F, 0.05F, 0xFF2A2A2E, light);
                cube(buffer, pose, 0.5F, 0.47F, 0.5F, 0.03F, 0xFF3A3A40, light);
            }
            for (Vec3 to : state.strands) {
                wire(buffer, pose, sy, to, light, far);
                if (far) continue;
                int count = bulbCount(to);
                for (int i = 1; i < count; i++) {
                    along(sy, to, i / (float) count);
                    box(buffer, pose, AT[0], AT[1] - 0.035F, AT[2], 0.022F, 0.035F, 0xFF1C3A1C, light);
                }
            }
            oneSided = false;
        });
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(WHITE_TEX), (pose, buffer) -> {
            for (Vec3 to : state.strands) {
                int count = bulbCount(to);
                for (int i = 1; i < count; i++) {
                    along(sy, to, i / (float) count);
                    int c = scale(BULBS[i % BULBS.length], twinkle(time, i));
                    float x = AT[0];
                    float y = AT[1] - 0.13F;
                    float z = AT[2];
                    box(buffer, pose, x, y, z, 0.042F, 0.06F, c, 0xF000F0);
                    box(buffer, pose, x, y - 0.065F, z, 0.025F, 0.025F, c, 0xF000F0);
                }
            }
        });
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(HALO_TEX), (pose, buffer) -> {
            for (Vec3 to : state.strands) {
                int count = bulbCount(to);
                for (int i = 1; i < count; i++) {
                    along(sy, to, i / (float) count);
                    float k = twinkle(time, i);
                    int c = (((int) (150 * k)) << 24) | (BULBS[i % BULBS.length] & 0xFFFFFF);
                    halo(buffer, pose, AT[0], AT[1] - 0.14F, AT[2], 0.2F + 0.05F * k, c);
                }
            }
        });
    }

    private static final Identifier HALO_TEX = tex("light_halo");

    private static int bulbCount(Vec3 to) {
        return Math.max(2, (int) Math.round(to.length() / 0.4));
    }

    // a bulb's brightness now: a slow, gentle breathing of its own, and now and then a quick sparkle
    private static float twinkle(float time, int i) {
        float slow = 0.82F + 0.18F * Mth.sin(time * 0.05F + i * 2.3F);
        float sparkle = Mth.sin(time * 0.37F + i * 5.1F) > 0.985F ? 1.25F : 1.0F;
        return Math.min(1.0F, slow * sparkle);
    }

    // The geometry below makes no objects while it draws: everything is plain numbers and a few shared scratch arrays
    // (drawing is all on the render thread). It used to make a new point for every corner of every bulb, wire bit and
    // glow, every frame: gigabytes of garbage a minute with a few strands up, and the pauses to clear it away made the
    // game stutter.

    // AT: a point along a strand (along() fills it)
    private static final float[] AT = new float[3];
    private static final float[] CORNERS = new float[24];
    private static final int[][] FACES = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

    // a point on a strand to `to` (from this hook at (0.5, sy, 0.5)), t from 0 to 1: sagging in the middle; into AT
    private static void along(float sy, Vec3 to, float t) {
        float len = (float) to.length();
        float sag = 0.25F + 0.07F * len;
        AT[0] = 0.5F + (float) to.x * t;
        AT[1] = sy + (float) to.y * t - sag * 4 * t * (1 - t);
        AT[2] = 0.5F + (float) to.z * t;
    }

    // two thin wires twisted round each other along the strand
    private static void wire(VertexConsumer buffer, PoseStack.Pose pose, float sy, Vec3 to, int light, boolean far) {
        float len = (float) to.length();
        int segments = far ? Math.max(4, (int) Math.ceil(len * 2)) : Math.max(6, (int) Math.ceil(len * 6));
        for (int strand = 0; strand < (far ? 1 : 2); strand++) {
            float px = 0, py = 0, pz = 0;
            for (int i = 0; i <= segments; i++) {
                float t = i / (float) segments;
                float twist = t * len * 9 + strand * Mth.PI;
                along(sy, to, t);
                float x = AT[0] + Mth.cos(twist) * 0.012F;
                float y = AT[1] + Mth.sin(twist) * 0.012F;
                float z = AT[2] + Mth.sin(twist) * 0.012F;
                if (i > 0) ribbon(buffer, pose, px, py, pz, x, y, z, 0.012F, 0xFF1C3A1C, light);
                px = x;
                py = y;
                pz = z;
            }
        }
    }

    // a soft glow: three crossed squares of the halo texture, so it looks round from any side
    private static void halo(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float r, int color) {
        texQuad(buffer, pose, x - r, y - r, z, x + r, y - r, z, x + r, y + r, z, x - r, y + r, z, color);
        texQuad(buffer, pose, x, y - r, z - r, x, y - r, z + r, x, y + r, z + r, x, y + r, z - r, color);
        texQuad(buffer, pose, x - r, y, z - r, x + r, y, z - r, x + r, y, z + r, x - r, y, z + r, color);
    }

    // a textured square (the whole texture), both sides
    private static void texQuad(VertexConsumer buffer, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                                float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        texVertex(buffer, pose, ax, ay, az, 0, 1, color);
        texVertex(buffer, pose, bx, by, bz, 1, 1, color);
        texVertex(buffer, pose, cx, cy, cz, 1, 0, color);
        texVertex(buffer, pose, dx, dy, dz, 0, 0, color);
        texVertex(buffer, pose, dx, dy, dz, 0, 0, color);
        texVertex(buffer, pose, cx, cy, cz, 1, 0, color);
        texVertex(buffer, pose, bx, by, bz, 1, 1, color);
        texVertex(buffer, pose, ax, ay, az, 0, 1, color);
    }

    private static void texVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // a box half-width hw, half-height hh round (x, y, z)
    private static void box(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float hw, float hh, int color, int light) {
        for (int i = 0; i < 8; i++) {
            CORNERS[i * 3] = x + ((i & 1) == 0 ? -hw : hw);
            CORNERS[i * 3 + 1] = y + ((i & 2) == 0 ? -hh : hh);
            CORNERS[i * 3 + 2] = z + ((i & 4) == 0 ? -hw : hw);
        }
        for (int[] f : FACES) {
            quad(buffer, pose, CORNERS[f[0] * 3], CORNERS[f[0] * 3 + 1], CORNERS[f[0] * 3 + 2], CORNERS[f[1] * 3], CORNERS[f[1] * 3 + 1],
                CORNERS[f[1] * 3 + 2], CORNERS[f[2] * 3], CORNERS[f[2] * 3 + 1], CORNERS[f[2] * 3 + 2], CORNERS[f[3] * 3],
                CORNERS[f[3] * 3 + 1], CORNERS[f[3] * 3 + 2], color, light);
        }
    }

    private static void cube(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float r, int color, int light) {
        box(buffer, pose, x, y, z, r, r, color, light);
    }

    // a thin flat strip from a to b, twice crossed so it shows from any side
    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                               float w, int color, int light) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (dl < 1.0E-6F) return;
        dx /= dl;
        dy /= dl;
        dz /= dl;
        // side = d x (0, 1, 0)
        float sx = -dz, sy = 0, sz = dx;
        float sl = Mth.sqrt(sx * sx + sz * sz);
        if (sl < 0.01F) {
            sx = 1;
            sz = 0;
            sl = 1;
        }
        sx = sx / sl * w;
        sz = sz / sl * w;
        // up = d x side
        float ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
        float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul > 1.0E-6F) {
            ux = ux / ul * w;
            uy = uy / ul * w;
            uz = uz / ul * w;
        }
        quad(buffer, pose, ax + sx, ay + sy, az + sz, ax - sx, ay - sy, az - sz, bx - sx, by - sy, bz - sz, bx + sx, by + sy, bz + sz, color, light);
        quad(buffer, pose, ax + ux, ay + uy, az + uz, ax - ux, ay - uy, az - uz, bx - ux, by - uy, bz - uz, bx + ux, by + uy, bz + uz, color, light);
    }

    // while drawing with a render type that shows both sides anyway: each square once (half the vertices)
    private static boolean oneSided;

    // a flat-coloured square, both windings, so it shows whichever way the render type culls
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int color, int light) {
        vertex(buffer, pose, ax, ay, az, color, light);
        vertex(buffer, pose, bx, by, bz, color, light);
        vertex(buffer, pose, cx, cy, cz, color, light);
        vertex(buffer, pose, dx, dy, dz, color, light);
        if (oneSided) return;
        vertex(buffer, pose, dx, dy, dz, color, light);
        vertex(buffer, pose, cx, cy, cz, color, light);
        vertex(buffer, pose, bx, by, bz, color, light);
        vertex(buffer, pose, ax, ay, az, color, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color, int light) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(WHITE_U, WHITE_V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // ── the Ground Fog: dry ice fog over about 4 x 4 blocks ──
    // Soft white puffs (fog_puff.png) well up near the middle, creep outward low along the ground, swell and thin out,
    // then start again, each on its own clock, so the fog keeps rolling. Flat puffs lying on the ground make it thick
    // seen from above; upright puffs turned to face you make it billow seen from the side. Each puff lies on the ground
    // under it (State.ground), so the fog follows steps and never hangs over a drop. Drawn emissive and lit by hand
    // (white by day, a pale ghostly grey at night, never darker than a lamp beside it).
    private static final Identifier FOG_TEX = tex("fog_puff");
    private static final int FOG_FLAT = 26;
    private static final int FOG_UPRIGHT = 18;

    private void fog(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float t = state.time;
        int sky = (state.lightCoords >> 20) & 15;
        int lamp = (state.lightCoords >> 4) & 15;
        float bright = Math.max(lamp / 15.0F, sky / 15.0F * (0.42F + 0.58F * state.daylight));
        int shade = Math.max(0, Math.min(255, (int) (bright * 255)));
        int rgb = ((236 * shade / 255) << 16) | ((240 * shade / 255) << 8) | (246 * shade / 255);
        BlockPos at = state.blockPos;
        long salt = at.asLong() * 0x9E3779B97F4A7C15L;
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(FOG_TEX), (pose, buffer) -> {
            for (int i = 0; i < FOG_FLAT + FOG_UPRIGHT; i++) {
                boolean flat = i < FOG_FLAT;
                double r1 = unit(salt, i, 1), r2 = unit(salt, i, 2), r3 = unit(salt, i, 3), r4 = unit(salt, i, 4);
                // its life: born near the middle, creeping out along its own heading, fading in and out
                float life = (float) ((t / (220.0 + 160.0 * r1) + r2) % 1.0);
                double heading = r3 * Math.PI * 2;
                double start = 0.3 + 0.5 * r4;
                double reach = start + life * (1.0 + 0.4 * r1);
                float x = 0.5F + (float) (Math.cos(heading) * reach);
                float z = 0.5F + (float) (Math.sin(heading) * reach);
                double out = Math.sqrt((x - 0.5) * (x - 0.5) + (z - 0.5) * (z - 0.5));
                if (out > 2.1) continue;
                int cx = (int) Math.floor(x) + 2;
                int cz = (int) Math.floor(z) + 2;
                if (cx < 0 || cx > 4 || cz < 0 || cz > 4) continue;
                float ground = state.ground[cx * 5 + cz];
                if (Float.isNaN(ground)) continue;
                float swell = 0.55F + 0.75F * life;
                float alpha = Mth.sin(life * Mth.PI) * (float) Math.max(0, 1 - Math.pow(out / 2.1, 3));
                int a = (int) (255 * alpha * (flat ? 0.55F : 0.32F));
                if (a < 3) continue;
                int color = (a << 24) | rgb;
                float spin = (float) (r2 * 6.28 + t * 0.002 * (r1 - 0.5));
                if (flat) {
                    float y = ground + 0.04F + 0.12F * (float) r4 + 0.02F * (i % 4);
                    float s = swell * (0.75F + 0.5F * (float) r1);
                    float c = Mth.cos(spin) * s, n = Mth.sin(spin) * s;
                    texQuad(buffer, pose, x - c + n, y, z - n - c, x + c + n, y, z + n - c,
                        x + c - n, y, z + n + c, x - c - n, y, z - n + c, color);
                } else {
                    // upright: wide and low, turned to the camera (round the vertical only)
                    double fx = state.camRel.x - x, fz = state.camRel.z - z;
                    double len = Math.max(1.0E-4, Math.sqrt(fx * fx + fz * fz));
                    float rx = (float) (-fz / len), rz = (float) (fx / len);
                    float w = swell * (0.8F + 0.4F * (float) r1);
                    float h = 0.28F + 0.3F * life + 0.1F * (float) r4;
                    float y0 = ground - 0.02F;
                    texQuad(buffer, pose, x - rx * w, y0, z - rz * w, x + rx * w, y0, z + rz * w,
                        x + rx * w, y0 + h, z + rz * w, x - rx * w, y0 + h, z - rz * w, color);
                }
            }
        });
    }

    // 0 to 1, fixed for this fog block, puff and slot
    private static double unit(long salt, int i, int slot) {
        long h = salt + i * 0x632BE59BD9B4E019L + slot * 0x85EBCA77C2B2AE63L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    // The ground under a fog block barely ever changes, so it's looked up once a second per fog block and kept, not every
    // frame (that was up to 100 block lookups a frame for each fog block, enough to make the game stutter).
    private static final java.util.Map<Long, GroundCache> GROUND = new java.util.HashMap<>();

    private record GroundCache(long tick, float[] ground) {
    }

    private static float[] cachedGround(net.minecraft.world.level.Level level, BlockPos pos) {
        long now = level.getGameTime();
        GroundCache cached = GROUND.get(pos.asLong());
        if (cached == null || now - cached.tick() >= 20 || now < cached.tick()) {
            float[] ground = new float[25];
            fogGround(level, pos, ground);
            cached = new GroundCache(now, ground);
            if (GROUND.size() > 512) GROUND.entrySet().removeIf(e -> now - e.getValue().tick() > 200 || now < e.getValue().tick());
            GROUND.put(pos.asLong(), cached);
        }
        return cached.ground();
    }

    // the ground in each block of the 5 x 5 round the fog: the top of the first solid thing under open space, from a
    // block above the fog down to two below it (NaN: none, the fog doesn't go there)
    private static void fogGround(net.minecraft.world.level.Level level, BlockPos pos, float[] ground) {
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                float g = Float.NaN;
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos q = pos.offset(cx, dy, cz);
                    if (!level.getBlockState(q).getCollisionShape(level, q).isEmpty()) continue;
                    BlockPos below = q.below();
                    var shape = level.getBlockState(below).getCollisionShape(level, below);
                    if (shape.isEmpty()) continue;
                    g = dy - 1 + (float) shape.max(Direction.Axis.Y);
                    break;
                }
                ground[(cx + 2) * 5 + (cz + 2)] = g;
            }
        }
    }

    private static int scale(int argb, float k) {
        int r = (int) ((argb >> 16 & 0xFF) * k);
        int g = (int) ((argb >> 8 & 0xFF) * k);
        int b = (int) ((argb & 0xFF) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    // Only drawn when its box (getRenderBoundingBox: the model and every strand of lights from it) is on screen. This was
    // true, which drew every decoration every frame wherever you looked (behind you, out of sight), the biggest cost of all.
    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(SeasonalDecorBlockEntity decor) {
        // asked every frame: worked out once and kept on the decoration until its strands change (no new boxes each frame)
        int key = decor.strands().hashCode() * 31 + decor.getBlockPos().hashCode();
        if (decor.renderBox != null && decor.renderBoxKey == key) return decor.renderBox;
        BlockPos pos = decor.getBlockPos();
        AABB box = new AABB(pos).inflate(4.0, 0.0, 4.0).expandTowards(0.0, 10.0, 0.0);
        for (BlockPos to : decor.strands()) box = box.minmax(new AABB(to));
        decor.renderBox = box;
        decor.renderBoxKey = key;
        return box;
    }
}

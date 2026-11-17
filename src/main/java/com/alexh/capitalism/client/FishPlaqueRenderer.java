package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.fishing.CatchInfo;
import com.alexh.capitalism.fishing.FishPlaqueBlock;
import com.alexh.capitalism.fishing.FishPlaqueBlockEntity;
import com.alexh.capitalism.fishing.RareCatch;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

import static com.alexh.capitalism.client.BloodDecorModels.K;
import static com.alexh.capitalism.client.BoatKit.*;

// A Fish Plaque on a wall (fishing/FishPlaqueBlock, Fishing.md > Plaques): a polished walnut board with a dark carved
// rim, as wide as the catch needs (1 to 3 blocks), the catch mounted on it (sculpted in 3D: client/FishTrophies), and under it a
// brass nameplate with screws, engraved with the catch's name, weight, who caught it and when.
public class FishPlaqueRenderer implements BlockEntityRenderer<FishPlaqueBlockEntity, FishPlaqueRenderer.State> {

    // the boards: [width - 1][style], style by the catch's tier (0 walnut and brass, 1 dark wood and silver, 2 ebony and
    // gold, 3 obsidian, gold and glowing runes)
    public static final ModelLayerLocation[][] LAYERS = new ModelLayerLocation[3][4];
    private static final int FULL_BRIGHT = 0xF000F0;
    // the nameplate's light: as if a lamp hung by it (block light 13), so it reads at night
    private static final int PLATE_LIGHT = 0xD000D0;

    static {
        for (int w = 1; w <= 3; w++) {
            for (int s = 0; s < 4; s++) LAYERS[w - 1][s] = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "fish_plaque_" + w + "_" + s), "main");
        }
    }

    static int style(@Nullable RareCatch kind) {
        if (kind == null) return 0;
        return switch (kind.tier()) {
            case UNCOMMON, RARE -> 0;
            case EPIC -> 1;
            case LEGENDARY -> 2;
            case MYTHIC -> 3;
        };
    }

    // the board's texture: walnut, dark polished wood, ebony (the Ghost Koi's with crimson for the Blood Moon), obsidian
    static Identifier boardTexture(@Nullable RareCatch kind) {
        String name = switch (style(kind)) {
            case 1 -> "fish_plaque_epic";
            case 2 -> kind == RareCatch.GHOST_KOI ? "fish_plaque_blood" : "fish_plaque_legendary";
            case 3 -> "fish_plaque_mythic";
            default -> "fish_plaque";
        };
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + name + ".png");
    }

    // the engraving's colour: dark on brass and silver, gold (or crimson) on black, glowing ember on obsidian
    static int[] textColours(@Nullable RareCatch kind) {
        return switch (style(kind)) {
            case 1 -> new int[]{0xFF14181E, 0xFF262C36};
            case 2 -> kind == RareCatch.GHOST_KOI ? new int[]{0xFFFF5A6A, 0xFFE8A0A8} : new int[]{0xFFFFCC44, 0xFFE8C070};
            case 3 -> new int[]{0xFFFFB030, 0xFFFFD890};
            default -> new int[]{0xFF2A1606, 0xFF3A2410};
        };
    }

    public static class State extends BlockEntityRenderState {
        public int width = 1;
        public Direction facing = Direction.NORTH;
        public String title = "";
        public String weight = "";
        public String story = "";
        public @Nullable RareCatch kind;
    }

    private final Board[][] boards = new Board[3][4];
    private final Map<RareCatch, FishTrophies.Trophy> trophies = new EnumMap<>(RareCatch.class);
    private final Font font;

    public FishPlaqueRenderer(BlockEntityRendererProvider.Context context) {
        for (int w = 0; w < 3; w++) for (int s = 0; s < 4; s++) this.boards[w][s] = new Board(context.bakeLayer(LAYERS[w][s]));
        for (var e : FishTrophies.LAYERS.entrySet()) this.trophies.put(e.getKey(), new FishTrophies.Trophy(context.bakeLayer(e.getValue()), FishTrophies.seeThrough(e.getKey())));
        this.font = context.font();
    }

    // the board's outline (model pixels, y down): its top and bottom, and the nameplate's top and bottom: a strip along
    // the bottom of the board, the catch mounted in the space above it
    static final double TOP = -14.0, BOT = -0.4, PLATE_TOP = -4.6, PLATE_BOT = -1.3;

    // the nameplate runs the whole width of the board, inside its rim (model pixels)
    static double plateWidth(int width) {
        return width * 16 - 1.2 - 1.8;
    }

    // a strip along z from a to b cut in lengths of at most `max` (so no box runs off its patch of the texture)
    static void strip(K k, Zone zone, double x, double y, double a, double b, double w, double h, double max) {
        int n = Math.max(1, (int) Math.ceil((b - a) / max));
        for (int i = 0; i < n; i++) k.box(zone, (i * 5) % 10, 0, x, y, a + (b - a) * i / n, w, h, (b - a) / n + (i < n - 1 ? 0.02 : 0));
    }

    // the board, its rim, the nameplate along its bottom, `width` blocks wide (model pixels: the front +x, the wall at
    // x -8, to the right +z), grander with `style`: 1 a carved border round the catch, silver rosettes, a silver edge on
    // the plate; 2 gold filigree at the corners, a gold edge on the plate and a gold finial on top; 3 an ornate gold frame
    // round the whole board, black spikes off it, rubies set in it, runes glowing along it
    public static LayerDefinition createLayer(int width, int style) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        PartDefinition glowRoot = mesh.getRoot().addOrReplaceChild("glow", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition plateRoot = mesh.getRoot().addOrReplaceChild("plate", CubeListBuilder.create(), PartPose.ZERO);
        K g = new K(glowRoot);
        K p = new K(plateRoot);
        K k = new K(root);
        // from the main block's left edge, or a block further left for a 3-wide one (centred: FishPlaqueBlock.start)
        double shift = FishPlaqueBlock.start(width) * 16.0;
        double z0 = -7.4 + shift, z1 = width * 16 - 8.6 + shift, len = z1 - z0;
        double mid = (width - 1) * 8.0 + shift;
        // the board in lengths (each fits its patch of the texture), the carved rim round it
        for (int i = 0; i < width; i++) {
            double a = z0 + len * i / width, b = z0 + len * (i + 1) / width;
            k.box(DARKWOOD, (i * 9) % 20, 0, -8.0, TOP, a, 1.4, BOT - TOP, b - a + (i < width - 1 ? 0.02 : 0));
        }
        strip(k, OLDWOOD, -6.6, TOP, z0, z1, 0.45, 0.8, 16);
        strip(k, OLDWOOD, -6.6, BOT - 0.8, z0, z1, 0.45, 0.8, 16);
        strip(k, GOLD, -6.2, TOP + 0.05, z0 + 0.2, z1 - 0.2, 0.12, 0.25, 15);
        k.box(OLDWOOD, 0, 8, -6.6, TOP + 0.8, z0, 0.45, BOT - TOP - 1.6, 0.8);
        k.box(OLDWOOD, 0, 12, -6.6, TOP + 0.8, z1 - 0.8, 0.45, BOT - TOP - 1.6, 0.8);
        // the nameplate: the whole width, along the bottom; a thin edge along its top; a screw at each corner
        double pa = z0 + 0.9, pb = z1 - 0.9;
        // (the plate is its own part, drawn lit as if by a lamp so the engraving reads at night)
        strip(p, BRASS, -6.6, PLATE_TOP, pa, pb, 0.35, PLATE_BOT - PLATE_TOP, 15);
        strip(p, style == 1 ? CHROME : GOLD, -6.28, PLATE_TOP - 0.06, pa, pb, 0.1, 0.22, 10);
        for (int sy = 0; sy < 2; sy++) {
            for (int sz = 0; sz < 2; sz++) k.cube(style >= 1 ? CHROME : ENGINE, -6.2, sy == 0 ? PLATE_TOP + 0.55 : PLATE_BOT - 0.5, sz == 0 ? pa + 0.5 : pb - 0.5, 0.35);
        }
        // the hook it hangs from, at the top
        k.box(style >= 2 ? GOLD : BRASS, -7.0, TOP - 1.0, mid - 0.6, 0.6, 1.2, 1.2);
        Zone trim = style == 1 ? CHROME : GOLD;
        if (style >= 1) {
            // a carved border round the catch's space, rosettes at its top corners, a raised edge over the plate
            double iz0 = z0 + 1.3, iz1 = z1 - 1.3, by = TOP + 1.0, bh = PLATE_TOP - 0.9 - by;
            strip(k, OLDWOOD, -6.6, by, iz0, iz1, 0.7, 0.45, 15);
            k.box(OLDWOOD, 0, 2, -6.6, by, iz0, 0.7, bh, 0.45);
            k.box(OLDWOOD, 0, 2, -6.6, by, iz1 - 0.45, 0.7, bh, 0.45);
            for (int sz = 0; sz < 2; sz++) k.cube(trim, -6.0, by + 0.2, sz == 0 ? iz0 + 0.2 : iz1 - 0.2, 0.9);
            strip(k, trim, -6.6, PLATE_TOP - 0.5, pa, pb, 0.55, 0.45, 10);
        }
        if (style >= 2) {
            // gold filigree at the four corners of the catch's space: an L of gold, a curl and a scroll
            for (int cz = 0; cz < 2; cz++) {
                for (int cy = 0; cy < 2; cy++) {
                    double zc = cz == 0 ? z0 + 0.5 : z1 - 0.5, yc = cy == 0 ? TOP + 0.5 : PLATE_TOP - 0.8, dz = cz == 0 ? 1 : -1, dy = cy == 0 ? 1 : -1;
                    k.box(GOLD, -6.55, yc - 0.25, Math.min(zc, zc + dz * 3.2), 0.5, 0.5, 3.2);
                    k.box(GOLD, -6.55, Math.min(yc, yc + dy * 3.2), zc - 0.25, 0.5, 3.2, 0.5);
                    k.cube(GOLD, -6.0, yc + dy * 1.2, zc + dz * 1.2, 0.9);
                    k.line(GOLD, -6.1, yc + dy * 1.2, zc + dz * 1.2, -6.1, yc + dy * 2.6, zc + dz * 2.2, 0.3);
                    k.line(GOLD, -6.1, yc + dy * 1.2, zc + dz * 1.2, -6.1, yc + dy * 2.2, zc + dz * 2.6, 0.3);
                }
            }
            // a finial over the hook
            k.cube(GOLD, -6.4, TOP - 0.6, mid, 1.0);
            k.line(GOLD, -6.4, TOP - 0.8, mid, -6.4, TOP - 2.4, mid, 0.4);
            k.cube(style == 3 ? RED : GOLD, -6.4, TOP - 2.6, mid, 0.7);
        }
        if (style >= 3) {
            // the ornate frame round the whole board, standing proud of it
            strip(k, GOLD, -7.6, TOP - 0.9, z0 - 0.9, z1 + 0.9, 1.6, 0.9, 13);
            strip(k, GOLD, -7.6, BOT, z0 - 0.9, z1 + 0.9, 1.6, 0.9, 13);
            double fh = (BOT - TOP + 1.8) / 2;
            for (int half = 0; half < 2; half++) {
                k.box(GOLD, -7.6, TOP - 0.9 + half * fh, z0 - 0.9, 1.6, fh + 0.02, 0.9);
                k.box(GOLD, -7.6, TOP - 0.9 + half * fh, z1, 1.6, fh + 0.02, 0.9);
            }
            // black spikes off its corners and along its top, rubies at the corners
            for (int cz = 0; cz < 2; cz++) {
                double zc = cz == 0 ? z0 - 0.45 : z1 + 0.45, dz = cz == 0 ? -1 : 1;
                k.line(ENGINE, -6.8, TOP - 0.6, zc, -6.8, TOP - 3.4, zc + dz * 1.4, 0.55);
                k.line(ENGINE, -6.8, TOP - 0.4, zc, -6.8, TOP - 1.6, zc + dz * 2.6, 0.45);
                k.line(ENGINE, -6.8, BOT + 0.4, zc, -6.8, BOT + 2.0, zc + dz * 1.8, 0.45);
                k.cube(RED, -6.6, TOP - 0.45, zc, 0.95);
                k.cube(RED, -6.6, BOT + 0.45, zc, 0.95);
            }
            for (int i = 1; i < 4 * width; i++) {
                double z = z0 + len * i / (4 * width);
                if (Math.abs(z - mid) < 1.5) continue;
                k.line(ENGINE, -6.8, TOP - 0.8, z, -6.8, TOP - 2.0 - (i % 2) * 0.6, z, 0.35);
            }
            // runes glowing along the top of the board and over the plate
            CubeListBuilder runes = CubeListBuilder.create();
            for (int row = 0; row < 2; row++) {
                double ry = row == 0 ? TOP + 0.45 : PLATE_TOP - 1.25;
                for (int i = 0; i < 5 * width; i++) {
                    double z = z0 + 1.6 + (len - 3.2) * (i + 0.5) / (5 * width);
                    int kind = (i + row) % 3;
                    BoatKit.box(runes, ICE, i % 10, 0, -6.15F, (float) ry, (float) z - 0.12F, 0.1F, kind == 0 ? 0.6F : 0.35F, 0.24F);
                    if (kind == 1) BoatKit.box(runes, ICE, 0, 0, -6.15F, (float) ry + 0.15F, (float) z - 0.35F, 0.1F, 0.18F, 0.7F);
                    if (kind == 2) BoatKit.box(runes, ICE, 0, 0, -6.15F, (float) ry, (float) z + 0.12F, 0.1F, 0.2F, 0.24F);
                }
            }
            g.k.part(runes, PartPose.ZERO);
        }
        k.done();
        g.done();
        p.done();
        return LayerDefinition.create(mesh, 256, 256);
    }

    // A board: its wood and trim ("decor"), its nameplate ("plate", drawn lamp-lit) and its runes ("glow", full bright);
    // Anim.time picks which part a pass draws (0, 1, 2)
    public static final class Board extends net.minecraft.client.model.Model<BloodDecorModels.Anim> {
        private final ModelPart body, plate, glow;

        public Board(ModelPart root) {
            super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
            this.body = root.getChild("decor");
            this.plate = root.getChild("plate");
            this.glow = root.getChild("glow");
        }

        @Override
        public void setupAnim(BloodDecorModels.Anim s) {
            super.setupAnim(s);
            int pass = (int) s.time();
            this.body.visible = pass == 0;
            this.plate.visible = pass == 1;
            this.glow.visible = pass == 2;
        }
    }
    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FishPlaqueBlockEntity plaque, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plaque, state, partialTicks, cameraPosition, breakProgress);
        var block = plaque.getBlockState();
        state.facing = block.hasProperty(FishPlaqueBlock.FACING) ? block.getValue(FishPlaqueBlock.FACING) : Direction.NORTH;
        RareCatch kind = plaque.kind();
        state.kind = kind;
        state.width = kind == null ? 1 : Math.max(1, Math.min(3, kind.plaque));
        CatchInfo info = plaque.info();
        state.title = kind == null ? "" : kind.title;
        state.weight = info == null || info.weight() <= 0 ? "" : info.weightText();
        state.story = info == null ? "" : info.catcher() + " · " + info.date();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // the board (turned like the decorations: the model's front to the way it faces)
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, switch (state.facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        });
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        int style = style(state.kind);
        Board board = this.boards[state.width - 1][style];
        Identifier boardTex = boardTexture(state.kind);
        collector.submitModel(board, new BloodDecorModels.Anim(0, 0, false, 0, 0, 0), poseStack, boardTex, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        collector.submitModel(board, new BloodDecorModels.Anim(1, 0, false, 0, 0, 0), poseStack, boardTex, PLATE_LIGHT, OverlayTexture.NO_OVERLAY, 0);
        if (style == 3) collector.submitModel(board, new BloodDecorModels.Anim(2, 0, false, 0, 0, 0), poseStack, boardTex, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        // the catch, in the board's space; the Ghost Koi glows (full bright), and so do the lure, sparks and runes
        FishTrophies.Trophy trophy = state.kind == null ? null : this.trophies.get(state.kind);
        if (trophy != null) {
            Identifier tex = FishTrophies.texture(state.kind);
            int light = FishTrophies.seeThrough(state.kind) ? FULL_BRIGHT : state.lightCoords;
            collector.submitModel(trophy, new BloodDecorModels.Anim(0, 0, false, 0, 0, 0), poseStack, tex, light, OverlayTexture.NO_OVERLAY, 0);
            if (FishTrophies.glows(state.kind)) {
                collector.submitModel(trophy, new BloodDecorModels.Anim(0, 0, true, 0, 0, 0), poseStack, tex, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            }
        }
        poseStack.popPose();
        // the engraving, in a frame with +z out of the wall, +x to the right along it, +y up
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        float mid = (state.width - 1) * 0.5F + FishPlaqueBlock.start(state.width);
        // the engraving on the nameplate: the name big and bold, under it weight · catcher · day; each as big as fits
        // the plate; full bright (readable at night) and a hair in front of the plate
        float plateW = (float) plateWidth(state.width) / 16.0F * 0.94F;
        String second = state.weight.isEmpty() ? state.story : state.weight + " · " + state.story;
        Component[] lines = {Component.literal(state.title).withStyle(ChatFormatting.BOLD), Component.literal(second)};
        float[] ys = {(float) (-PLATE_TOP - 1.05) / 16.0F, (float) (-PLATE_BOT - 0.0) / 16.0F + 0.042F};
        float[] max = {0.0125F, 0.0072F};
        int[] colours = textColours(state.kind);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].getString().isEmpty()) continue;
            FormattedCharSequence text = lines[i].getVisualOrderText();
            int w = this.font.width(text);
            float scale = Math.min(max[i], plateW / Math.max(1, w));
            poseStack.pushPose();
            poseStack.translate(mid, ys[i], -0.374F);
            poseStack.scale(scale, -scale, scale);
            collector.submitText(poseStack, -w / 2.0F, -4.0F, text, false, Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT,
                i == 0 ? colours[0] : colours[1], 0, 0);
            poseStack.popPose();
        }        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FishPlaqueBlockEntity plaque) {
        return new AABB(plaque.getBlockPos()).inflate(3.0, 0.5, 3.0);
    }
}

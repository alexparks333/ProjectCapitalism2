package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.decor.BloodBedBlock;
import com.alexh.capitalism.decor.BloodDecorBlock;
import com.alexh.capitalism.decor.BloodDecorBlockEntity;
import com.alexh.capitalism.decor.BloodDecorKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

// Draws a Blood Moon decoration (decor/BloodDecorBlock, Decorations.md): its model (client/BloodDecorModels) on its
// block, turned to face the way it was placed, and a second, glowing pass (<id>_glow.png: the lantern's moon, flames,
// the blood, the crystal, red eyes and gems). The Vampire Coffin's lid swings open and shut smoothly (its block entity
// keeps how far it has gone).
public class BloodDecorRenderer implements BlockEntityRenderer<BloodDecorBlockEntity, BloodDecorRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public BloodDecorKind kind;
        public float turn;
        public float shiftX, shiftZ;
        // the model's state (BloodDecorModels.Anim)
        public float time, open, x, y, z;
        public boolean lit;
    }

    private record Look(BloodDecorModels.Base model, Identifier texture, Identifier glow) {
    }

    private final Map<BloodDecorKind, Look> looks = new EnumMap<>(BloodDecorKind.class);

    // each decoration's model (also drawn as its item: client/BloodDecorItemRenderer)
    record Def(ModelLayerLocation layer, Function<ModelPart, BloodDecorModels.Base> model) {
    }

    static final Map<BloodDecorKind, Def> DEFS = new EnumMap<>(BloodDecorKind.class);

    private static void def(BloodDecorKind kind, ModelLayerLocation layer, Function<ModelPart, BloodDecorModels.Base> model) {
        DEFS.put(kind, new Def(layer, model));
    }

    static {
        def(BloodDecorKind.BLOOD_MOON_LANTERN, BloodDecorModels.Lantern.LAYER, BloodDecorModels.Lantern::new);
        def(BloodDecorKind.CRIMSON_CANDELABRA, BloodDecorModels.Candelabra.LAYER, BloodDecorModels.Candelabra::new);
        def(BloodDecorKind.CRIMSON_THRONE, BloodDecorModels.Throne.LAYER, BloodDecorModels.Throne::new);
        def(BloodDecorKind.VAMPIRE_COFFIN, BloodDecorModels.Coffin.LAYER, BloodDecorModels.Coffin::new);
        def(BloodDecorKind.BLOOD_FOUNTAIN, BloodDecorModels.Fountain.LAYER, BloodDecorModels.Fountain::new);
        def(BloodDecorKind.BLOOD_MOON_BANNER, BloodDecorModels.Banner.LAYER, BloodDecorModels.Banner::new);
        def(BloodDecorKind.CRIMSON_RUG, BloodDecorModels.Rug.LAYER, BloodDecorModels.Rug::new);
        def(BloodDecorKind.REVENANT_GREATSWORD_STATUE, BloodDecorModels.Greatsword.LAYER, BloodDecorModels.Greatsword::new);
        def(BloodDecorKind.GOREFANG_SKULL_TROPHY, BloodDecorModels.SkullTrophy.LAYER, BloodDecorModels.SkullTrophy::new);
        def(BloodDecorKind.SANGUINE_STAFF_PEDESTAL, BloodDecorModels.StaffPedestal.LAYER, BloodDecorModels.StaffPedestal::new);
        def(BloodDecorKind.BAT_GARGOYLE, BloodDecorModels.Gargoyle.LAYER, BloodDecorModels.Gargoyle::new);
        def(BloodDecorKind.GRANDFATHER_CLOCK, BloodDecorModels2.Clock.LAYER, BloodDecorModels2.Clock::new);
        def(BloodDecorKind.CRIMSON_CHANDELIER, BloodDecorModels2.Chandelier.LAYER, BloodDecorModels2.Chandelier::new);
        def(BloodDecorKind.BLOOD_PIPE_ORGAN, BloodDecorModels2.Organ.LAYER, BloodDecorModels2.Organ::new);
        def(BloodDecorKind.SECRET_BOOKSHELF_DOOR, BloodDecorModels2.BookshelfDoor.LAYER, BloodDecorModels2.BookshelfDoor::new);
        def(BloodDecorKind.HAUNTED_PORTRAIT, BloodDecorModels2.Portrait.LAYER, BloodDecorModels2.Portrait::new);
        def(BloodDecorKind.IRON_MAIDEN, BloodDecorModels2.IronMaiden.LAYER, BloodDecorModels2.IronMaiden::new);
        def(BloodDecorKind.BLOOD_MOON_ALTAR, BloodDecorModels2.Altar.LAYER, BloodDecorModels2.Altar::new);
        def(BloodDecorKind.BAT_ROOST, BloodDecorModels2.BatRoost.LAYER, BloodDecorModels2.BatRoost::new);
        def(BloodDecorKind.CRIMSON_CANOPY_BED, BloodDecorModels2.CanopyBed.LAYER, BloodDecorModels2.CanopyBed::new);
        def(BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE, BloodDecorModels2.Fireplace.LAYER, BloodDecorModels2.Fireplace::new);
    }

    public BloodDecorRenderer(BlockEntityRendererProvider.Context context) {
        for (var e : DEFS.entrySet()) this.add(context, e.getKey(), e.getValue().layer(), e.getValue().model());
    }

    private void add(BlockEntityRendererProvider.Context context, BloodDecorKind kind, ModelLayerLocation layer, Function<ModelPart, BloodDecorModels.Base> model) {
        this.looks.put(kind, new Look(model.apply(context.bakeLayer(layer)), tex(kind.id), tex(kind.id + "_glow")));
    }

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BloodDecorBlockEntity decor, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(decor, state, partialTicks, cameraPosition, breakProgress);
        var block = decor.getBlockState();
        state.kind = block.getBlock() instanceof BloodDecorKind.Holder d ? d.kind() : null;
        Direction facing = block.hasProperty(BloodDecorBlock.FACING) ? block.getValue(BloodDecorBlock.FACING) : Direction.WEST;
        state.shiftX = 0;
        state.shiftZ = 0;
        if (block.getBlock() instanceof BloodBedBlock) {
            // a bed: drawn from its head, centred on the line between its two blocks, its front (+x) toward the foot
            facing = facing.getOpposite();
            state.shiftX = facing.getStepX() * 0.5F;
            state.shiftZ = facing.getStepZ() * 0.5F;
        } else if (state.kind != null && (state.kind.wide > 1 || state.kind.deep > 1)) {
            // a big piece: drawn in the middle of its footprint (to the right of its main block, and back)
            Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
            float w = (state.kind.wide - 1) * 0.5F, d = (state.kind.deep - 1) * 0.5F;
            state.shiftX = right.getStepX() * w + back.getStepX() * d;
            state.shiftZ = right.getStepZ() * w + back.getStepZ() * d;
        }
        // the model's front is +x; turn it to face the way it was placed (after the y-down flip)
        state.turn = switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        };
        float now = decor.getLevel() == null ? 0 : decor.getLevel().getGameTime() + partialTicks;
        state.time = now;
        state.lit = block.hasProperty(BloodDecorBlock.LIT) && block.getValue(BloodDecorBlock.LIT);
        // how far open it is (a lid, a door, curtains): eases toward open or shut, about a second either way
        boolean opened = block.hasProperty(BloodDecorBlock.OPEN) && block.getValue(BloodDecorBlock.OPEN);
        // the iron maiden: open while someone has its chest open or while it's armed; slammed shut on a victim
        if (state.kind == BloodDecorKind.IRON_MAIDEN && block.hasProperty(BloodDecorBlock.TRIPPED)) {
            opened = (opened || block.getValue(BloodDecorBlock.LIT)) && !block.getValue(BloodDecorBlock.TRIPPED);
        }
        if (block.hasProperty(BloodBedBlock.OCCUPIED) && state.kind == BloodDecorKind.CRIMSON_CANOPY_BED) opened = block.getValue(BloodBedBlock.OCCUPIED);
        float target = opened ? 1.0F : 0.0F;
        if (decor.lid < 0) decor.lid = target;
        else {
            float dt = Math.max(0.0F, Math.min(5.0F, now - decor.lidTime));
            float step = dt / 18.0F;
            decor.lid = decor.lid < target ? Math.min(target, decor.lid + step) : Math.max(target, decor.lid - step);
        }
        decor.lidTime = now;
        state.open = decor.lid;
        state.x = 0;
        state.y = 0;
        state.z = 0;
        if (decor.getLevel() != null) BloodDecorExtras.extract(decor, state, cameraPosition, partialTicks);
    }

    // drawn smaller than built (the lantern and candelabra)
    private static float scaleOf(BloodDecorKind kind) {
        if (kind == BloodDecorKind.BLOOD_MOON_ALTAR) return 1.25F;
        return kind == BloodDecorKind.BLOOD_MOON_LANTERN || kind == BloodDecorKind.CRIMSON_CANDELABRA ? 0.75F : 1.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.kind == null) return;
        Look look = this.looks.get(state.kind);
        if (look == null) return;
        BloodDecorModels.Anim anim = new BloodDecorModels.Anim(state.time, state.open, state.lit, state.x, state.y, state.z);
        poseStack.pushPose();
        poseStack.translate(0.5F + state.shiftX, 0.0F, 0.5F + state.shiftZ);
        poseStack.rotateDegrees(Axis.YP, state.turn);
        float s = scaleOf(state.kind);
        poseStack.scale(-s, -s, s);
        collector.submitModel(look.model(), anim, poseStack, look.texture(), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (state.kind.glows) collector.submitModel(look.model(), anim, poseStack, RenderTypes.eyes(look.glow()), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    // the bigger ones reach beyond their block (the coffin, fountain, rug; the banner, throne and sword stand 3 high)
    @Override
    public AABB getRenderBoundingBox(BloodDecorBlockEntity decor) {
        // (asked every frame: made once and kept)
        if (decor.renderBox == null) decor.renderBox = new AABB(decor.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 2.5, 0.0);
        return decor.renderBox;
    }
}

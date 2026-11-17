package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.occupation.WishingWellBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

// Draws the Wishing Well (occupation/WishingWellBlock) from its core block: the whole 3 x 3 x 3 model, turned to the
// way it was placed, and again for its glowing parts (the lantern, a faint shimmer on the water); the water itself is see-through. Its box covers the whole well, so it's
// only drawn when some of it is on screen.
public class WishingWellRenderer implements BlockEntityRenderer<WishingWellBlock.WishingWellBlockEntity, WishingWellRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/wishing_well.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/wishing_well_glow.png");

    public static class State extends BlockEntityRenderState {
        public float turn;
        public float time;
    }

    private final WishingWellModel model;
    private final WishingWellModel.Water water;

    public WishingWellRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new WishingWellModel(context.bakeLayer(WishingWellModel.LAYER));
        this.water = new WishingWellModel.Water(context.bakeLayer(WishingWellModel.WATER_LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WishingWellBlock.WishingWellBlockEntity well, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(well, state, partialTicks, cameraPosition, breakProgress);
        var block = well.getBlockState();
        Direction facing = block.hasProperty(WishingWellBlock.FACING) ? block.getValue(WishingWellBlock.FACING) : Direction.NORTH;
        // the model's front is +x; turn it to face the way it was placed (after the y-down flip)
        state.turn = switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        };
        state.time = well.getLevel() == null ? 0 : well.getLevel().getGameTime() + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, state.turn);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state.time, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        collector.submitModel(this.model, state.time, poseStack, RenderTypes.eyes(GLOW), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        // the clear water over the pebbles and coins, and its faint glow
        collector.submitModel(this.water, state.time, poseStack, RenderTypes.entityTranslucentCull(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        collector.submitModel(this.water, state.time, poseStack, RenderTypes.eyes(GLOW), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(WishingWellBlock.WishingWellBlockEntity well) {
        return new AABB(well.getBlockPos()).inflate(2.0, 0.0, 2.0).expandTowards(0.0, 3.0, 0.0);
    }
}

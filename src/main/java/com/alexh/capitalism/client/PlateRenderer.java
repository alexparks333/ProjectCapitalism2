package com.alexh.capitalism.client;

import com.alexh.capitalism.block.PlateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Draws the food on a plate, lying flat; a few servings stack up (up to 3 shown).
public class PlateRenderer implements BlockEntityRenderer<PlateBlockEntity, PlateRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public int shown;
        public int seed;
    }

    private final ItemModelResolver itemModelResolver;

    public PlateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PlateBlockEntity plate, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTicks, cameraPosition, breakProgress);
        state.seed = (int) plate.getBlockPos().asLong();
        state.shown = Math.min(3, plate.food().getCount());
        this.itemModelResolver.updateForTopItem(state.item, plate.food(), ItemDisplayContext.FIXED, plate.getLevel(), null, state.seed);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        for (int i = 0; i < state.shown; i++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.14F + i * 0.035F, 0.5F);
            poseStack.rotateDegrees(Axis.YP, (state.seed & 0xFF) * 1.4F + i * 35F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}

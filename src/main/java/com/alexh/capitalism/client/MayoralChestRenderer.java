package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.town.MayoralChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Draws the Mayoral Chest (town/MayoralChestBlock) with MayoralChestModel, its lid as open as the chest's lid
// controller says (it opens while the Mayor or Vice Mayor has it open).
public class MayoralChestRenderer implements BlockEntityRenderer<MayoralChestBlockEntity, MayoralChestRenderer.State> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/mayoral_chest.png");

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float open;
    }

    private final MayoralChestModel model;

    public MayoralChestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new MayoralChestModel(context.bakeLayer(MayoralChestModel.LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MayoralChestBlockEntity chest, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
        var block = chest.getBlockState();
        state.facing = block.hasProperty(ChestBlock.FACING) ? block.getValue(ChestBlock.FACING) : Direction.NORTH;
        state.open = chest.getOpenNess(partialTicks);
    }

    // the model's front (+x) turned to the way it faces (as the decorations are)
    static float turn(Direction facing) {
        return switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, turn(state.facing));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state.open, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}

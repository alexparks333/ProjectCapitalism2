package com.alexh.capitalism.client;

import com.alexh.capitalism.librarian.KnowledgeCreatorBlock;
import com.alexh.capitalism.librarian.KnowledgeCreatorBlockEntity;
import com.alexh.capitalism.librarian.KnowledgeQuality;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// What's in a Knowledge Creator's bell jar (librarian/KnowledgeCreatorBlockEntity, synced). While it works: the
// Enchanted Book floats in the jar, turning and bobbing, getting smaller as it's used up, and the Knowledge Piece forms
// under it, growing as it gets done. Idle: the book (if any) rests in the jar; a finished piece waits at the bottom.
public class KnowledgeCreatorRenderer implements BlockEntityRenderer<KnowledgeCreatorBlockEntity, KnowledgeCreatorRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState book = new ItemStackRenderState();
        public final ItemStackRenderState piece = new ItemStackRenderState();
        public boolean working;
        public float progress;
        public float time;
    }

    private static final float JAR_FLOOR = 9.05F / 16.0F;

    private final ItemModelResolver itemModelResolver;

    public KnowledgeCreatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KnowledgeCreatorBlockEntity creator, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(creator, state, partialTicks, cameraPosition, breakProgress);
        state.working = creator.getBlockState().getValue(KnowledgeCreatorBlock.WORKING);
        state.progress = Math.min(1.0F, creator.progress() / (float) KnowledgeCreatorBlockEntity.MAKE_TICKS);
        state.time = creator.getLevel() == null ? 0 : creator.getLevel().getGameTime() + partialTicks;
        int seed = (int) creator.getBlockPos().asLong();
        ItemStack book = creator.book();
        this.itemModelResolver.updateForTopItem(state.book, book, ItemDisplayContext.FIXED, creator.getLevel(), null, seed);
        ItemStack piece = state.working ? KnowledgeQuality.byPower(KnowledgeQuality.power(book)).stack(1) : creator.getItem(KnowledgeCreatorBlockEntity.OUTPUT);
        this.itemModelResolver.updateForTopItem(state.piece, piece, ItemDisplayContext.FIXED, creator.getLevel(), null, seed + 1);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.book.isEmpty()) {
            poseStack.pushPose();
            if (state.working) {
                // floating and turning, shrinking as it's used up
                float bob = (float) Math.sin(state.time * 0.1) * 0.03F;
                poseStack.translate(0.5F, 0.82F + bob, 0.5F);
                poseStack.rotateDegrees(Axis.YP, state.time * 3.0F);
                float size = 0.36F * (1.0F - 0.5F * state.progress);
                poseStack.scale(size, size, size);
            } else {
                // resting on the jar floor
                poseStack.translate(0.5F, JAR_FLOOR + 0.02F, 0.5F);
                poseStack.rotateDegrees(Axis.XP, 90.0F);
                poseStack.scale(0.36F, 0.36F, 0.36F);
            }
            state.book.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (!state.piece.isEmpty() && (state.working || state.book.isEmpty())) {
            poseStack.pushPose();
            if (state.working) {
                // forming under the book, growing as it gets done
                poseStack.translate(0.5F, JAR_FLOOR + 0.08F, 0.5F);
                poseStack.rotateDegrees(Axis.YP, -state.time * 2.0F);
                float size = 0.05F + 0.25F * state.progress;
                poseStack.scale(size, size, size);
            } else {
                poseStack.translate(0.5F, JAR_FLOOR + 0.02F, 0.5F);
                poseStack.rotateDegrees(Axis.XP, 90.0F);
                poseStack.scale(0.3F, 0.3F, 0.3F);
            }
            state.piece.submit(poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}

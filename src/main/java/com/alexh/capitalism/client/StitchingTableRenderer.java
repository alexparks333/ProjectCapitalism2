package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.librarian.StitchingTableBlock;
import com.alexh.capitalism.librarian.StitchingTableBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// What's on a Stitching Table's mat (librarian/StitchingTableBlockEntity, synced when it changes). While it sews: the
// book being made (Rough or Delicate, by the thread) lies on the mat and the needle works its way round its edge,
// stabbing down through it and pulling back up. Idle: the finished books, or the Paper, with the needle lying beside it.
// Turned with the table (the model's front is north).
public class StitchingTableRenderer implements BlockEntityRenderer<StitchingTableBlockEntity, StitchingTableRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState onMat = new ItemStackRenderState();
        public final ItemStackRenderState needle = new ItemStackRenderState();
        public boolean sewing;
        public float turn;
        public float time;
    }

    private static final float MAT_Y = 13.02F / 16.0F;

    private final ItemModelResolver itemModelResolver;

    public StitchingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StitchingTableBlockEntity table, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        state.sewing = table.getBlockState().getValue(StitchingTableBlock.SEWING);
        Direction facing = table.getBlockState().getValue(StitchingTableBlock.FACING);
        state.turn = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        state.time = table.getLevel() == null ? 0 : table.getLevel().getGameTime() + partialTicks;
        ItemStack shown;
        if (state.sewing) {
            shown = new ItemStack(table.thread().is(Items.DIAMOND) ? ModItems.DELICATE_BOOK.get() : ModItems.ROUGH_BOOK.get());
        } else if (!table.output().isEmpty()) {
            shown = table.output();
        } else {
            shown = table.paper();
        }
        int seed = (int) table.getBlockPos().asLong();
        this.itemModelResolver.updateForTopItem(state.onMat, shown, ItemDisplayContext.FIXED, table.getLevel(), null, seed);
        this.itemModelResolver.updateForTopItem(state.needle, new ItemStack(ModItems.STITCHING_NEEDLE.get()), ItemDisplayContext.FIXED, table.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // turn like the model: the blockstate's y rotation is clockwise seen from above
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.turn);
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        // the book / Paper flat on the mat (left of the spool)
        float cx = 6.0F / 16.0F;
        float cz = 9.0F / 16.0F;
        if (!state.onMat.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(cx, MAT_Y, cz);
            poseStack.rotateDegrees(Axis.YP, 15.0F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.42F, 0.42F, 0.42F);
            state.onMat.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        poseStack.pushPose();
        if (state.sewing) {
            // round the edge of the book, a stitch every second: down through it, then pulled back up
            float t = state.time;
            double around = (t / 20.0) * 0.35;
            float ex = cx + (float) Math.cos(around) * 0.13F;
            float ez = cz + (float) Math.sin(around) * 0.13F;
            float phase = (t % 20.0F) / 20.0F;
            float lift = (float) Math.abs(Math.sin(phase * Math.PI)) * 0.16F;
            poseStack.translate(ex, MAT_Y + 0.06F + lift, ez);
            poseStack.rotateDegrees(Axis.YP, (float) Math.toDegrees(-around) + 45.0F);
            // the needle points down at the book, tipping a little as it pulls out
            poseStack.rotateDegrees(Axis.ZP, -45.0F - lift * 60.0F);
        } else {
            // lying on the mat beside the book
            poseStack.translate(11.5F / 16.0F, MAT_Y + 0.01F, 11.5F / 16.0F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.rotateDegrees(Axis.ZP, 20.0F);
        }
        poseStack.scale(0.32F, 0.32F, 0.32F);
        state.needle.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        poseStack.popPose();
    }
}

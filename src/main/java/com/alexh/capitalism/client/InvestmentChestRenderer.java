package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.InvestmentChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Draws the Investment Chest (economy/InvestmentChestBlock) with InvestmentChestModel, its lid as open as anyone has it,
// and letters its hanging sign "INVESTMENT / CHEST" (bold glowing gold, outlined, on the deep green board) and the tag off it
// "(totally safe!)" (red), in the board's own sway so the words move with it.
public class InvestmentChestRenderer implements BlockEntityRenderer<InvestmentChestBlockEntity, InvestmentChestRenderer.State> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/investment_chest.png");
    private static final Component LINE_1 = Component.literal("INVESTMENT").withStyle(s -> s.withBold(true));
    private static final Component LINE_2 = Component.literal("CHEST").withStyle(s -> s.withBold(true));
    private static final Component TAG = Component.literal("(totally safe!)").withStyle(s -> s.withBold(true));
    // the sign's letters glow like glow-ink, so they read at night and in a dim bank
    private static final int GLOW = 0xF000F0;

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float open;
        public float time;
    }

    private final InvestmentChestModel model;
    private final Font font;

    public InvestmentChestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new InvestmentChestModel(context.bakeLayer(InvestmentChestModel.LAYER));
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InvestmentChestBlockEntity chest, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
        var block = chest.getBlockState();
        state.facing = block.hasProperty(HorizontalDirectionalBlock.FACING) ? block.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        state.open = chest.getOpenNess(partialTicks);
        state.time = chest.getLevel() == null ? 0 : chest.getLevel().getGameTime() + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, MayoralChestRenderer.turn(state.facing));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, new InvestmentChestModel.Anim(state.open, state.time), poseStack, TEXTURE, state.lightCoords,
            OverlayTexture.NO_OVERLAY, 0);
        this.letter(state, poseStack, collector);
        poseStack.popPose();
    }

    // the words, in the model's space (pixels / 16, y down, the front +x), following the sign as ModelPart turns it
    private void letter(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.translate(InvestmentChestModel.SIGN_X / 16.0F, InvestmentChestModel.SIGN_Y / 16.0F, 0.0F);
        poseStack.rotate(new org.joml.Quaternionf().rotationZYX(InvestmentChestModel.signSwing(state.time), 0.0F,
            InvestmentChestModel.signRoll(state.time)));
        // the board: two lines across its front
        poseStack.pushPose();
        poseStack.translate((InvestmentChestModel.BOARD_FRONT + 0.02F) / 16.0F, InvestmentChestModel.BOARD_MID_Y / 16.0F, 0.0F);
        this.face(poseStack);
        float s = 0.0135F;
        this.line(poseStack, collector, LINE_1, s, -1.25F / 16.0F, 0xFFFFD34A, 0xFF1A0E02, GLOW);
        this.line(poseStack, collector, LINE_2, s, 1.35F / 16.0F, 0xFFFFD34A, 0xFF1A0E02, GLOW);
        poseStack.popPose();
        // the tag, hung on its own nail
        poseStack.translate(InvestmentChestModel.TAG_X / 16.0F, InvestmentChestModel.TAG_Y / 16.0F, InvestmentChestModel.TAG_Z / 16.0F);
        poseStack.rotate(Axis.XP, InvestmentChestModel.TAG_ROLL);
        poseStack.translate((InvestmentChestModel.TAG_FRONT + 0.02F) / 16.0F, InvestmentChestModel.TAG_MID_Y / 16.0F,
            InvestmentChestModel.TAG_MID_Z / 16.0F);
        this.face(poseStack);
        this.line(poseStack, collector, TAG, 0.0072F, 0.0F, 0xFFB01010, 0, state.lightCoords);
        poseStack.popPose();
    }

    // turn so text drawn in x (right) and y (down) lies on a face looking out along the model's +x
    private void face(PoseStack poseStack) {
        poseStack.rotateDegrees(Axis.YP, -90.0F);
        poseStack.scale(1.0F, 1.0F, -1.0F);
    }

    private void line(PoseStack poseStack, SubmitNodeCollector collector, Component text, float scale, float y, int color, int outline, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.scale(scale, scale, scale);
        int w = this.font.width(text);
        collector.submitText(poseStack, -w / 2.0F, -4.0F, text.getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET, light, color, 0, outline);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(InvestmentChestBlockEntity chest) {
        var p = chest.getBlockPos();
        return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 3, p.getZ() + 2);
    }
}

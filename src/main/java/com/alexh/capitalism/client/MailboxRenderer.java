package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.mail.MailboxBlock;
import com.alexh.capitalism.mail.MailboxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;


// Draws the Mailbox (mail/MailboxBlock): the model turned the way it was placed, its flag rising while there's mail in
// it (and falling once it's empty). Over it, only for its owner and only when they're near (fading in from 8 blocks),
// how many letters (filled slots) are waiting: "✉ 5", bobbing gently.
public class MailboxRenderer implements BlockEntityRenderer<MailboxBlockEntity, MailboxRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/mailbox.png");
    private static final Identifier TOWN_TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/town_mailbox.png");
    private static final float NEAR = 6.0F;
    private static final float FAR = 8.0F;

    public static class State extends BlockEntityRenderState {
        public float turn;
        public float flag;
        public float time;
        public int letters;
        public float countAlpha;
        public boolean town;
    }

    private final MailboxModel model;
    private final MailboxModel townModel;
    // the number's text, made once per count
    private static final FormattedCharSequence[] TEXTS = new FormattedCharSequence[MailboxBlockEntity.SIZE + 1];
    // each mailbox's flag, eased toward up or down (client only)
    private final java.util.Map<Long, float[]> flags = new java.util.HashMap<>();

    public MailboxRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new MailboxModel(context.bakeLayer(MailboxModel.LAYER));
        this.townModel = new MailboxModel(context.bakeLayer(MailboxModel.TOWN_LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MailboxBlockEntity mailbox, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mailbox, state, partialTicks, cameraPosition, breakProgress);
        var block = mailbox.getBlockState();
        Direction facing = block.hasProperty(MailboxBlock.FACING) ? block.getValue(MailboxBlock.FACING) : Direction.NORTH;
        // the model's door is +x; turn it to face the way it was placed (after the flip)
        state.turn = switch (facing) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> -90.0F;
            default -> 0.0F;
        };
        state.time = mailbox.getLevel() == null ? 0 : mailbox.getLevel().getGameTime() + partialTicks;
        state.letters = Math.min(mailbox.letters(), MailboxBlockEntity.SIZE);
        state.town = mailbox.town();
        // the flag eases toward where it should be
        float[] flag = this.flags.computeIfAbsent(mailbox.getBlockPos().asLong(), p -> new float[]{state.letters > 0 ? 1 : 0, state.time});
        float dt = Math.max(0.0F, Math.min(5.0F, state.time - flag[1]));
        flag[1] = state.time;
        float target = state.letters > 0 ? 1.0F : 0.0F;
        flag[0] += (target - flag[0]) * Math.min(1.0F, dt * 0.15F);
        state.flag = flag[0];
        // the count, for its owner, when they're near
        state.countAlpha = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        if (state.letters > 0 && mc.player != null && mailbox.shownTo(mc.player.getUUID())) {
            double dist = Math.sqrt(cameraPosition.distanceToSqr(Vec3.atCenterOf(mailbox.getBlockPos())));
            state.countAlpha = (float) Math.max(0.0, Math.min(1.0, (FAR - dist) / (FAR - NEAR)));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, state.turn);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(state.town ? this.townModel : this.model, state.flag, poseStack, state.town ? TOWN_TEXTURE : TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (state.countAlpha > 0.02F) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            FormattedCharSequence text = TEXTS[state.letters];
            if (text == null) {
                text = Component.literal("✉ " + state.letters).getVisualOrderText();
                TEXTS[state.letters] = text;
            }
            float bob = 0.04F * (float) Math.sin(state.time * 0.08F);
            poseStack.translate(0.0F, (state.town ? 2.0F : 1.75F) + bob, 0.0F);
            poseStack.rotate(mc.gameRenderer.mainCamera().rotation());
            poseStack.scale(0.03F, -0.03F, 0.03F);
            float a = state.countAlpha;
            collector.submitText(poseStack, -font.width(text) / 2.0F, -4.0F, text, false, Font.DisplayMode.NORMAL, 0xF000F0,
                ARGB.color(a, 0xFFE8A0), ARGB.color(a * 0.55F, 0x2A1A0C), ARGB.color(a, 0x3A2410));
        }
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public AABB getRenderBoundingBox(MailboxBlockEntity mailbox) {
        return new AABB(mailbox.getBlockPos()).expandTowards(0.0, 1.6, 0.0);
    }
}

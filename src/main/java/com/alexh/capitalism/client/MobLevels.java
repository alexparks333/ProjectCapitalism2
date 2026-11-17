package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.TougherMobs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

// A hostile mob's level over its head (NPCs.md > Mob levels, core/TougherMobs): "I", "II", or a white skull for a Boss.
// Only up close: fully there within FADE_FROM blocks, fading out by FADE_TO, like a pet's name. Drawn as part of the
// world, so walls hide it (never seen through blocks). The level comes from the mob's max-health modifier, which the
// game already sends to everyone.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class MobLevels {

    public static final double FADE_FROM = 8.0;
    public static final double FADE_TO = 14.0;
    // the level and how faded it is, on the mob's render state (MobHealthBars sits above it when both show)
    public static final ContextKey<Shown> LEVEL = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "mob_level"));

    public record Shown(TougherMobs.Level level, float alpha) {
    }

    // the marks' text, made once (not per mob per frame)
    private static final net.minecraft.util.FormattedCharSequence[] TEXTS = {
        Component.literal("I").getVisualOrderText(), Component.literal("II").getVisualOrderText(), Component.literal("☠").getVisualOrderText()};

    private MobLevels() {
    }

    // with the mob's render state (ClientModEvents registers this for every living thing)
    public static void extract(LivingEntity entity, LivingEntityRenderState state) {
        if (!(entity instanceof Enemy) || entity.isInvisible() || entity.isDeadOrDying()) return;
        TougherMobs.Level level = TougherMobs.levelOf(entity);
        Minecraft mc = Minecraft.getInstance();
        if (level == null || mc.player == null) return;
        double distance = Math.sqrt(mc.player.distanceToSqr(entity));
        float alpha = (float) Mth.clamp((FADE_TO - distance) / (FADE_TO - FADE_FROM), 0.0, 1.0);
        if (alpha > 0.02F) state.setRenderData(LEVEL, new Shown(level, alpha));
    }

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        Shown shown = state.getRenderData(LEVEL);
        if (shown == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        float lift = state.boundingBoxHeight + (state.nameTag != null ? 0.75F : 0.45F);
        boolean boss = shown.level() == TougherMobs.Level.BOSS;
        String text = switch (shown.level()) {
            case ONE -> "I";
            case TWO -> "II";
            case BOSS -> "☠";
        };
        // I a soft grey, II a brighter gold, the Boss skull bright white and bigger
        int color = switch (shown.level()) {
            case ONE -> 0xC8C8C8;
            case TWO -> 0xFFD860;
            case BOSS -> 0xFFFFFF;
        };
        float scale = boss ? 1.5F : 1.0F;
        pose.pushPose();
        pose.translate(0.0F, lift, 0.0F);
        pose.rotate(mc.gameRenderer.mainCamera().rotation());
        pose.scale(0.025F * scale, -0.025F * scale, 0.025F * scale);
        float a = shown.alpha();
        event.getSubmitNodeCollector().submitText(pose, -font.width(text) / 2.0F, -4.0F, TEXTS[shown.level().ordinal()], false,
            Font.DisplayMode.NORMAL, state.lightCoords, ARGB.color(Math.max(a, 0.05F), color), 0,
            ARGB.color(Math.max(a, 0.05F), boss ? 0x400000 : 0x101010));
        pose.popPose();
    }
}

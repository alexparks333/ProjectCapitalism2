package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.pets.Pets;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

// Pets' floating names (PetTrainer.md): drawn by us instead of the game, so they can fade. Fully there up close, fading
// out between FADE_FROM and FADE_TO blocks away, gone past that. And never through walls: drawn as part of the world (the
// game also draws a faint copy that shows through blocks; pets' names leave that out).
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class PetNames {

    public static final double FADE_FROM = 10.0;
    public static final double FADE_TO = 16.0;
    // how much of a pet's name shows (0..1), on its render state
    private static final ContextKey<Float> ALPHA = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_name_alpha"));

    private PetNames() {
    }

    // With the pet's render state (PetHatLayer.extract): how faded its name is by how far away it is
    public static void extract(Entity entity, EntityRenderState state) {
        if (!(entity instanceof Animal animal) || !Pets.isPet(animal) || state.nameTag == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double distance = Math.sqrt(mc.player.distanceToSqr(animal));
        float alpha = (float) Mth.clamp((FADE_TO - distance) / (FADE_TO - FADE_FROM), 0.0, 1.0);
        if (alpha <= 0.02F) state.nameTag = null;
        else state.setRenderData(ALPHA, alpha);
    }

    // Draw it ourselves: the game's name tag, but faded, and without the copy that shows through walls
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent.DoRender event) {
        EntityRenderState state = event.getEntityRenderState();
        Float alpha = state.getRenderData(ALPHA);
        if (alpha == null || event.getContent() != state.nameTag || state.nameTagAttachment == null) return;
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5, state.nameTagAttachment.z);
        poseStack.rotate(event.getCameraRenderState().orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);
        float x = -mc.font.width(state.nameTag) / 2.0F;
        float background = mc.options.getBackgroundOpacity(0.25F) * alpha;
        int textColor = ARGB.color(Math.max(alpha, 0.05F), -1);
        event.getSubmitNodeCollector().submitText(poseStack, x, 0, state.nameTag.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
            state.lightCoords, textColor, ARGB.color(background, -16777216), 0);
        poseStack.popPose();
    }
}

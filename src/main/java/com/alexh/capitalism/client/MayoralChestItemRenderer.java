package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

// The Mayoral Chest as an item: its own 3D model, shut, its front to the item's north (like a block's), filling the slot
// like a chest does (items/mayoral_chest.json: a "minecraft:special" model of type capitalism:mayoral_chest; its base
// models/item/mayoral_chest.json gives the display transforms).
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class MayoralChestItemRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "mayoral_chest");

    private final MayoralChestModel model;

    private MayoralChestItemRenderer(MayoralChestModel model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // (turned so its front faces the camera in a slot, as the standing Blood Moon decorations are)
        poseStack.rotateDegrees(Axis.YP, -90.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, 0.0F, poseStack, MayoralChestRenderer.TEXTURE, lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int i = 0; i < 8; i++) output.accept(new Vector3f((i & 1) == 0 ? 0.0F : 1.0F, (i & 2) == 0 ? 0.0F : 1.0F, (i & 4) == 0 ? 0.0F : 1.0F));
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public MayoralChestItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MayoralChestItemRenderer(new MayoralChestModel(context.entityModelSet().bakeLayer(MayoralChestModel.LAYER)));
        }
    }

    @SubscribeEvent
    public static void onRegister(RegisterSpecialModelRendererEvent event) {
        event.register(TYPE, Unbaked.MAP_CODEC);
    }
}

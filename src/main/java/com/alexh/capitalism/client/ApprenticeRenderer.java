package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.enchanter.Apprentice;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// Draws an Apprentice (enchanter/Apprentice) as the little iron figure in a wizard hat (ApprenticeModel), facing the
// way it was placed, like an armor stand. Its armor is only data (how much it holds): it isn't drawn.
public class ApprenticeRenderer extends LivingEntityRenderer<Apprentice, ApprenticeRenderer.State, ApprenticeModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "apprentice"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/apprentice.png");

    public static class State extends LivingEntityRenderState {
        float facing;
    }

    public ApprenticeRenderer(EntityRendererProvider.Context context) {
        super(context, new ApprenticeModel(context.bakeLayer(LAYER)), 0.3F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Apprentice apprentice, State state, float partialTicks) {
        super.extractRenderState(apprentice, state, partialTicks);
        state.facing = Mth.rotLerp(partialTicks, apprentice.yRotO, apprentice.getYRot());
    }

    @Override
    protected void setupRotations(State state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }
}

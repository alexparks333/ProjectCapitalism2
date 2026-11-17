package com.alexh.capitalism.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// The base of the Premium boats' models built with BoatKit: faces drawn one side only (sails and flags are flat, their
// two pictures in one plane), the speed / turn / first-person seat the renderer passes (PremiumBoatRenderer.State),
// and helpers: waving flag strips, a gentle rock on the water.
public abstract class PremiumBoatModel extends EntityModel<BoatRenderState> {

    protected final ModelPart boat;

    protected PremiumBoatModel(ModelPart root) {
        this(root, RenderTypes::entityCutoutCull);
    }

    protected PremiumBoatModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.boat = root.getChild("boat");
    }

    protected static float speed(BoatRenderState state) {
        return state instanceof PremiumBoatRenderer.State s ? s.speed : 0.0F;
    }

    protected static float turn(BoatRenderState state) {
        return state instanceof PremiumBoatRenderer.State s ? s.turn : 0.0F;
    }

    protected static float viewer(BoatRenderState state) {
        return state instanceof PremiumBoatRenderer.State s ? s.viewerX : Float.NaN;
    }

    // a gentle roll and pitch on the water (plus extra pitch: + raises the bow)
    protected void rock(BoatRenderState state, float roll, float pitch, float bowUp) {
        if (state.isUnderWater) return;
        float t = state.ageInTicks;
        this.boat.xRot = roll * Mth.sin(t * 0.07F);
        this.boat.zRot = pitch * Mth.sin(t * 0.05F + 1.3F) - bowUp;
    }

    // the strips of a flag group (f0, f1, ...)
    protected static List<ModelPart> strips(ModelPart group, int n) {
        List<ModelPart> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(group.getChild("f" + i));
        return out;
    }

    // a flag streaming back and rippling, harder the faster it goes
    protected static void wave(List<ModelPart> strips, float t, float amplitude, float rate, float step) {
        for (int i = 0; i < strips.size(); i++) {
            float reach = i / (float) Math.max(1, strips.size() - 1);
            float phase = t * rate - i * step;
            ModelPart strip = strips.get(i);
            strip.z = amplitude * reach * Mth.sin(phase);
            strip.yRot = -amplitude * 0.35F * reach * Mth.cos(phase);
        }
    }
}

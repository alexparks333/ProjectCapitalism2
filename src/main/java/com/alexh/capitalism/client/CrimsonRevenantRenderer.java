package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.event.CrimsonRevenant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws the Crimson Revenant (event/CrimsonRevenant) with CrimsonRevenantModel, and over it the glowing parts (its eyes,
// the cracks in its armour, the line down its blade) at full brightness, so it burns red in the dark.
public class CrimsonRevenantRenderer extends MobRenderer<CrimsonRevenant, CrimsonRevenantRenderer.State, CrimsonRevenantModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/crimson_revenant.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/crimson_revenant_glow.png");
    private static final RenderType GLOW_TYPE = RenderTypes.eyes(GLOW);

    public static class State extends LivingEntityRenderState {
        public int action;
        public float actionTicks;
        public float attack;
        // 1 just hit, falling to 0: it flinches
        public float flinch;
    }

    public CrimsonRevenantRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonRevenantModel(context.bakeLayer(CrimsonRevenantModel.LAYER)), 1.1F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW_TYPE;
            }
        });
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrimsonRevenant revenant, State state, float partialTicks) {
        super.extractRenderState(revenant, state, partialTicks);
        state.action = revenant.action();
        state.actionTicks = revenant.actionTicks() + partialTicks;
        state.attack = revenant.getSwingAnimation(partialTicks);
        state.flinch = revenant.hurtTime > 0 ? Math.max(0.0F, (revenant.hurtTime - partialTicks) / 10.0F) : 0.0F;
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }
}

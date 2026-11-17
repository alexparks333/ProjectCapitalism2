package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.event.BloodMoonBoss;
import com.alexh.capitalism.event.Gorefang;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws Gorefang and the Sanguine Witch (event/, Events.md > The bosses): their model, and over it the glowing parts
// (eyes, the fire in the throat, the orbs of blood) at full brightness. Textures: textures/entity/<id>.png and
// <id>_glow.png (tools/BoatTex.cs).
public class BloodBossRenderer<T extends BloodMoonBoss> extends MobRenderer<T, BloodBossRenderer.State, EntityModel<BloodBossRenderer.State>> {

    private final Identifier texture;
    private final RenderType glowType;

    public static class State extends LivingEntityRenderState {
        public int action;
        public float actionTicks;
        public float attack;
        // 1 just hit, falling to 0: it flinches
        public float flinch;
        // Gorefang's frenzy
        public boolean frenzy;
    }

    public BloodBossRenderer(EntityRendererProvider.Context context, EntityModel<State> model, String id, float shadow) {
        super(context, model, shadow);
        this.texture = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + id + ".png");
        this.glowType = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + id + "_glow.png"));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return BloodBossRenderer.this.glowType;
            }
        });
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T boss, State state, float partialTicks) {
        super.extractRenderState(boss, state, partialTicks);
        state.action = boss.action();
        state.actionTicks = boss.actionTicks() + partialTicks;
        state.attack = boss.getSwingAnimation(partialTicks);
        state.flinch = boss.hurtTime > 0 ? Math.max(0.0F, (boss.hurtTime - partialTicks) / 10.0F) : 0.0F;
        state.frenzy = boss instanceof Gorefang wolf && wolf.frenzied();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return this.texture;
    }
}

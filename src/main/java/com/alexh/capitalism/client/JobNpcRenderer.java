package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.npc.JobNpc;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

// Job NPCs (npc/JobNpc) and the Boat Master (npc/BoatMaster) look like villagers: the vanilla villager model, with one skin per job
// (textures/entity/npc/<job>.png: a villager in that job's outfit and hat) and the thing they hold shown across their
// folded arms (a sword for the Weaponsmith...). Skins are put together by tools/make_npc_skins.ps1.
public class JobNpcRenderer extends MobRenderer<JobNpc, VillagerRenderState, VillagerModel> {

    private static final Identifier FALLBACK = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    // The render state knows which job it's drawing
    public static class State extends VillagerRenderState {
        String skin = "";
    }

    public JobNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(JobNpc npc, VillagerRenderState state, float partialTicks) {
        super.extractRenderState(npc, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(npc, state, this.itemModelResolver);
        if (state instanceof State jobState) jobState.skin = npc.skin();
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        String skin = state instanceof State jobState ? jobState.skin : "";
        return skin.isEmpty() ? FALLBACK : Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/npc/" + skin + ".png");
    }
}

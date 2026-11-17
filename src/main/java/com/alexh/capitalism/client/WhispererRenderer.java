package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.town.Whisperer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

// Draws the Whisperer (town/Whisperer) with WhispererModel and textures/entity/whisperer.png.
public class WhispererRenderer extends MobRenderer<Whisperer, LivingEntityRenderState, WhispererModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "whisperer"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/whisperer.png");

    public WhispererRenderer(EntityRendererProvider.Context context) {
        super(context, new WhispererModel(context.bakeLayer(LAYER)), 0.45F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}

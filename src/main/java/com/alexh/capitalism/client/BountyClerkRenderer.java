package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.town.BountyClerk;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

// Draws the Bounty Clerk (town/BountyClerk) with BountyClerkModel and textures/entity/bounty_clerk.png.
public class BountyClerkRenderer extends MobRenderer<BountyClerk, LivingEntityRenderState, BountyClerkModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "bounty_clerk"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/bounty_clerk.png");

    public BountyClerkRenderer(EntityRendererProvider.Context context) {
        super(context, new BountyClerkModel(context.bakeLayer(LAYER)), 0.45F);
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

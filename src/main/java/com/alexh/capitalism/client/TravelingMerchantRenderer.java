package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.town.TravelingMerchant;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

// Draws the Traveling Merchant and his wagon (TravelingMerchantModel, textures/entity/traveling_merchant.png), the
// lantern flames glowing (traveling_merchant_glow.png). While he's away (invisible) nothing is drawn. The wagon reaches
// well past his own small box, so the culling box is grown to cover it (otherwise it would vanish at screen edges).
public class TravelingMerchantRenderer extends MobRenderer<TravelingMerchant, LivingEntityRenderState, TravelingMerchantModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "traveling_merchant"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/traveling_merchant.png");
    private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/traveling_merchant_glow.png"));

    public TravelingMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new TravelingMerchantModel(context.bakeLayer(LAYER)), 0.4F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }

            @Override
            public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light,
                               LivingEntityRenderState state, float yRot, float xRot) {
                if (state.isInvisible) return;
                super.submit(pose, collector, light, state, yRot, xRot);
            }
        });
    }

    @Override
    protected AABB getBoundingBoxForCulling(TravelingMerchant entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(4.0, 0.0, 4.0).expandTowards(0.0, 0.8, 0.0);
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

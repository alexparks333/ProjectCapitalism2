package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.town.BloodMerchant;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws the Blood Merchant (town/BloodMerchant) with BloodMerchantModel and textures/entity/blood_merchant.png, his eyes
// and the gem at his throat glowing (blood_merchant_glow.png). By day he's invisible, so nothing is drawn.
public class BloodMerchantRenderer extends MobRenderer<BloodMerchant, LivingEntityRenderState, BloodMerchantModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_merchant"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/blood_merchant.png");
    private static final RenderType GLOW = RenderTypes.eyes(Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/blood_merchant_glow.png"));

    public BloodMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new BloodMerchantModel(context.bakeLayer(LAYER)), 0.4F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }

            // by day (invisible) not even his eyes
            @Override
            public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light,
                               LivingEntityRenderState state, float yRot, float xRot) {
                if (state.isInvisible) return;
                super.submit(pose, collector, light, state, yRot, xRot);
            }
        });
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

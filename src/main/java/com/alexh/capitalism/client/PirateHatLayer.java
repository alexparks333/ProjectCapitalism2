package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import static com.alexh.capitalism.client.BloodDecorModels.K;
import static com.alexh.capitalism.client.BoatKit.*;

// The Pirate Hat on a player's head (fishing/RareFindItems.PirateHat, Fishing.md): a black felt tricorn, its brim turned
// up on three sides with gold braid along the edge, a bone-white skull and crossbones on its front and a red feather
// sweeping back. Worn in the head slot (the item has no armour look of its own; this draws it).
public class PirateHatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pirate_hat"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/pirate_hat.png");

    private final BloodDecorModels.Base model;

    public PirateHatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.model = new BloodDecorModels.Base(root) {
        };
    }

    // In the head's space (pixels, y down: the head from y -8 to 0, its face at z -4)
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        K k = new K(root);
        // the crown
        k.box(DECK, -4.4, -11.2, -4.4, 8.8, 3.4, 8.8);
        k.box(DECK, 40, 4, -3.8, -12.0, -3.8, 7.6, 0.9, 7.6);
        k.box(CANVAS, -4.5, -8.4, -4.5, 9.0, 0.5, 9.0);
        // (felt from DECK and braid from CANVAS: the big zones, so no box runs off its patch of the texture)
        // the brim: a flat ring, then turned up on three sides (a triangle, its point over the face), gold braid on top
        k.box(DECK, 60, 6, -6.4, -8.2, -6.4, 12.8, 0.5, 12.8);
        for (int s = 0; s < 3; s++) {
            double a = Math.PI / 2 + s * 2 * Math.PI / 3;
            double a2 = a + 2 * Math.PI / 3;
            double r = 7.4;
            double x1 = Math.cos(a) * r, z1 = -Math.sin(a) * r, x2 = Math.cos(a2) * r, z2 = -Math.sin(a2) * r;
            double dx = x2 - x1, dz = z2 - z1, len = Math.sqrt(dx * dx + dz * dz);
            CubeListBuilder wall = CubeListBuilder.create();
            BoatKit.box(wall, DECK, s * 30, 22, 0.0F, -4.2F, -0.35F, (float) len, 4.2F, 0.7F);
            BoatKit.box(wall, CANVAS, 0, 10 + s * 4, 0.0F, -4.6F, -0.42F, (float) len, 0.45F, 0.84F);
            k.part(wall, x1, -7.9, z1, -0.18, Math.atan2(-dz, dx), 0);
        }
        // the skull and crossbones on the brim's point, over the face (just in front of where its two front walls meet)
        double sz = -8.95;
        k.box(BONE, -1.1, -11.4, sz, 2.2, 1.9, 0.2);
        k.box(BONE, -0.7, -9.65, sz, 1.4, 0.6, 0.2);
        k.box(BLACK, -0.75, -10.9, sz - 0.04, 0.5, 0.5, 0.1).box(BLACK, 0.25, -10.9, sz - 0.04, 0.5, 0.5, 0.1);
        k.line(BONE, -1.8, -12.0, sz + 0.02, 1.8, -9.2, sz + 0.02, 0.35);
        k.line(BONE, 1.8, -12.0, sz + 0.02, -1.8, -9.2, sz + 0.02, 0.35);
        // a red feather sweeping back from the side
        k.line(RED, 3.8, -10.5, 1.0, 5.6, -14.0, 4.6, 0.7);
        k.line(RED, 5.6, -14.0, 4.6, 6.0, -15.0, 7.0, 0.5);
        k.done();
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.headEquipment.is(ModItems.PIRATE_HAT.get())) return;
        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        collector.submitModel(this.model, new BloodDecorModels.Anim(0, 0, false, 0, 0, 0), poseStack, TEXTURE, lightCoords,
            OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

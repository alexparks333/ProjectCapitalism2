package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.back.Back;
import com.alexh.capitalism.designer.Bag;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

// The Designer's bag on a player's back (back/Back, designer/Bag), for everyone to see: the bag on the back with its
// flap and buckle, and its straps over the shoulders and down the chest. One model for every bag, each with its own
// texture (textures/entity/bag_<bag>.png, tools/make_designer_textures.ps1).
public class BagLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "bag"), "main");
    // on the player's render state: the bag they're wearing
    public static final ContextKey<Bag> WORN = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "worn_bag"));

    private final Model<Unit> model;

    public BagLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.model = new Model.Simple(root, RenderTypes::entityCutout);
    }

    // In the body's space (pixels, y down, the back at z = 2)
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 1.5F, 2.0F, 7, 10, 3), PartPose.ZERO);
        root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, 1.2F, 1.8F, 7, 3, 4, new net.minecraft.client.model.geom.builders.CubeDeformation(0.15F)), PartPose.ZERO);
        root.addOrReplaceChild("left_strap", CubeListBuilder.create().texOffs(24, 0).addBox(1.5F, -0.25F, -2.5F, 1, 1, 5)
            .texOffs(40, 0).addBox(1.5F, 0.0F, -2.55F, 1, 8, 0.5F), PartPose.ZERO);
        root.addOrReplaceChild("right_strap", CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, -0.25F, -2.5F, 1, 1, 5)
            .texOffs(40, 0).addBox(-2.5F, 0.0F, -2.55F, 1, 8, 0.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static void extract(Player player, AvatarRenderState state) {
        Bag bag = Bag.of(Back.get(player));
        if (bag != null) state.setRenderData(WORN, bag);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Bag bag = state.getRenderData(WORN);
        if (bag == null || state.isInvisible) return;
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        Identifier texture = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/bag_" + bag.name().toLowerCase(Locale.ROOT) + ".png");
        collector.submitModel(this.model, Unit.INSTANCE, poseStack, texture, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}

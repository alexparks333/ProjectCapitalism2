package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// The Mayoral Chest (town/MayoralChestBlock): a grand chest of dark mahogany, two bands of polished gold round it and
// its lid, gold caps on every corner, a gold lock plate on the lid's front, and the town's crest on the front (white
// enamel in a gold rim, a crimson shield on it, like the Town Hall Mailbox). Inside, crimson velvet (the base's top and
// the lid's underside). The lid swings up on its back edge. Model pixels: the front +x, y down (the ground at 0), the
// width along z. Texture: textures/entity/mayoral_chest.png (128 x 64, tools/make_mayoral_chest.ps1, which knows these
// boxes). Anim: how far open (0 shut, 1 open).
public class MayoralChestModel extends Model<Float> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "mayoral_chest"), "main");

    private final ModelPart lid;

    public MayoralChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.lid = root.getChild("lid");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder base = CubeListBuilder.create()
            .texOffs(0, 0).addBox(-7.0F, -10.0F, -7.0F, 14, 10, 14)
            // the gold bands round it
            .texOffs(60, 0).addBox(-7.2F, -10.1F, -5.1F, 14.4F, 10.2F, 1.2F)
            .texOffs(60, 0).addBox(-7.2F, -10.1F, 3.9F, 14.4F, 10.2F, 1.2F)
            // the crest on its front
            .texOffs(96, 16).addBox(7.05F, -8.2F, -2.5F, 0.5F, 5, 5);
        // gold caps on its bottom corners
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) base.texOffs(96, 0).addBox(sx * 7.0F - 1.1F, -2.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
        }
        root.addOrReplaceChild("base", base, PartPose.ZERO);
        // the lid, hinged on its back edge
        CubeListBuilder lid = CubeListBuilder.create()
            .texOffs(0, 24).addBox(0.0F, -4.0F, -7.0F, 14, 4, 14)
            .texOffs(60, 24).addBox(-0.2F, -4.1F, -5.1F, 14.4F, 4.2F, 1.2F)
            .texOffs(60, 24).addBox(-0.2F, -4.1F, 3.9F, 14.4F, 4.2F, 1.2F)
            // the lock plate, hanging over the front
            .texOffs(96, 8).addBox(14.05F, -1.6F, -1.5F, 0.6F, 3.2F, 3);
        for (int sz = -1; sz <= 1; sz += 2) {
            lid.texOffs(96, 0).addBox(-1.1F, -5.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
            lid.texOffs(96, 0).addBox(12.9F, -5.1F, sz * 7.0F - 1.1F, 2.2F, 2.2F, 2.2F);
        }
        root.addOrReplaceChild("lid", lid, PartPose.offset(-7.0F, -10.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Float open) {
        super.setupAnim(open);
        float o = 1.0F - open;
        o = 1.0F - o * o * o;
        this.lid.zRot = -o * 1.45F;
    }
}

package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// The Spectral Galleon (boats/GhostShip, Boats.md): the pirate galleon's frame (PirateShipModel) come back from the
// deep, drawn see-through (its texture, by tools/PirateShipTex.cs in its ghost style: rotted, holed, pale and
// half-there, the sails shredded and glowing) with a spectral glow over its edges, lanterns of soul fire, and a ghost
// floating in the main top, a hooded figure bobbing and turning, holding up a soul lantern, its robe trailing away.
// Soul particles drift up off it (boats/GhostShip).
public class GhostShipModel extends PirateShipModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "ghost_ship"), "main");

    // the ghost's texture, in the galleon's layout below its pictures (tools/PirateShipTex.cs)
    static final Zone SPECTRE = new Zone(0, 180, 64, 40);
    static final Zone SPECTRE_GLOW = new Zone(64, 180, 16, 16);
    private static final float GHOST_Y = -47.5F;
    private static final int TAIL = 6;

    private final ModelPart ghost;

    public GhostShipModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucentCull);
        this.ghost = this.ship.getChild("ghost");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        this.ghost.y = GHOST_Y + 1.4F * Mth.sin(t * 0.07F);
        this.ghost.yRot = 0.7F * Mth.sin(t * 0.025F);
        this.ghost.getChild("lantern_arm").zRot = -1.9F + 0.15F * Mth.sin(t * 0.09F);
        this.ghost.getChild("other_arm").zRot = 0.25F + 0.2F * Mth.sin(t * 0.06F + 1.0F);
        for (int i = 0; i < TAIL; i++) {
            ModelPart tail = this.ghost.getChild("tail" + i);
            tail.xRot = 0.25F * Mth.sin(t * 0.15F + i * 1.1F);
            tail.zRot = 0.3F * Mth.sin(t * 0.11F + i * 0.8F) - 0.2F;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = createMesh();
        PartDefinition ship = mesh.getRoot().getChild("ship");
        CubeListBuilder body = CubeListBuilder.create();
        Builder.box(body, SPECTRE, 0, 0, -1.6F, -9.2F, -1.6F, 3.2F, 3.4F, 3.2F);          // the hood
        Builder.box(body, SPECTRE, 14, 0, 1.4F, -8.6F, -1.1F, 0.3F, 2.6F, 2.2F);          // the dark face in it
        Builder.box(body, SPECTRE_GLOW, 0, 0, 1.75F, -7.8F, 0.25F, 0.1F, 0.6F, 0.6F);     // glowing eyes
        Builder.box(body, SPECTRE_GLOW, 0, 0, 1.75F, -7.8F, -0.85F, 0.1F, 0.6F, 0.6F);
        Builder.box(body, SPECTRE, 0, 8, -1.5F, -5.8F, -2.0F, 3.0F, 2.8F, 4.0F);          // shoulders
        Builder.box(body, SPECTRE, 0, 16, -1.7F, -3.0F, -2.2F, 3.4F, 3.2F, 4.4F);         // the robe
        Builder.box(body, SPECTRE, 20, 16, -1.9F, 0.2F, -2.4F, 3.8F, 1.6F, 4.8F);
        PartDefinition ghost = ship.addOrReplaceChild("ghost", body, PartPose.offset(MAIN_X, GHOST_Y, 0.0F));
        // the arm holding up a soul lantern, and the other
        CubeListBuilder arm = CubeListBuilder.create();
        Builder.box(arm, SPECTRE, 30, 0, -0.6F, 0.0F, -0.6F, 1.2F, 4.2F, 1.2F);
        Builder.box(arm, SPECTRE, 30, 8, -0.4F, 4.2F, -0.4F, 0.8F, 0.8F, 0.8F);
        Builder.box(arm, IRON, 0, 0, -0.5F, 5.0F, -0.5F, 1.0F, 0.3F, 1.0F);
        Builder.box(arm, SPECTRE_GLOW, 4, 4, -0.45F, 5.3F, -0.45F, 0.9F, 1.4F, 0.9F);
        Builder.box(arm, IRON, 0, 4, -0.5F, 6.7F, -0.5F, 1.0F, 0.3F, 1.0F);
        ghost.addOrReplaceChild("lantern_arm", arm, PartPose.offset(0.0F, -5.4F, -2.2F));
        CubeListBuilder other = CubeListBuilder.create();
        Builder.box(other, SPECTRE, 30, 0, -0.6F, 0.0F, -0.6F, 1.2F, 4.4F, 1.2F);
        ghost.addOrReplaceChild("other_arm", other, PartPose.offset(0.0F, -5.4F, 2.2F));
        // tattered tail strands of the robe, trailing below (they drift: setupAnim)
        for (int i = 0; i < TAIL; i++) {
            float z = -2.0F + i * 0.8F;
            float len = 3.5F + (i % 3) * 1.2F;
            CubeListBuilder tail = CubeListBuilder.create();
            Builder.box(tail, SPECTRE, 40 + i * 2, 0, -1.5F, 0.0F, -0.35F, 3.0F, len, 0.7F);
            ghost.addOrReplaceChild("tail" + i, tail, PartPose.offset(0.0F, 1.8F, z));
        }
        return LayerDefinition.create(mesh, 256, 256);
    }
}

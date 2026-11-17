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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

import static com.alexh.capitalism.client.BoatKit.*;

// The Castaway's Tub (boats/CastawayTub, Boats.md): half a giant barrel of stripped jungle wood, its staves bulging
// and bound with rusty iron hoops, permanently half full of water; a composter for the captain's chair; a sad little
// white banner drooping off a crooked bamboo shoot; a wooden shovel for a paddle (it paddles when you do); a message
// in a bottle bobbing in the bilge, a coconut.
public class CastawayTubModel extends PremiumBoatModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "castaway_tub"), "main");

    static final float RIM = -6.0F;
    static final float FLOOR = 2.0F;
    static final float WATER_Y = -1.0F;

    private final ModelPart shovel;
    private final List<ModelPart> banner;
    private final ModelPart bottle;

    public CastawayTubModel(ModelPart root) {
        super(root);
        this.shovel = this.boat.getChild("shovel");
        this.banner = strips(this.boat.getChild("banner"), 6);
        this.bottle = this.boat.getChild("bottle");
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float speed = speed(state);
        // it wallows
        this.rock(state, 0.06F, 0.05F, 0.0F);
        // the shovel paddles with the right paddle
        float dip = Mth.clampedLerp((Mth.sin(-state.rowingTimeRight) + 1.0F) / 2.0F, 0.2F, 0.9F);
        float sweep = Mth.clampedLerp((Mth.sin(-state.rowingTimeRight + 1.0F) + 1.0F) / 2.0F, -0.6F, 0.6F);
        this.shovel.xRot = dip;
        this.shovel.yRot = sweep;
        // the banner barely stirs (a sad banner)
        for (int i = 0; i < this.banner.size(); i++) {
            ModelPart strip = this.banner.get(i);
            strip.z = (0.15F + 0.4F * speed) * (i / 5.0F) * Mth.sin(t * 0.12F - i * 0.5F);
            strip.xRot = 0.08F * Mth.sin(t * 0.07F + i * 0.3F);
        }
        this.bottle.y = WATER_Y - 0.4F + 0.25F * Mth.sin(t * 0.09F);
        this.bottle.zRot = 0.3F * Mth.sin(t * 0.05F);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition boat = mesh.getRoot().addOrReplaceChild("boat", CubeListBuilder.create(), PartPose.ZERO);
        BoatKit k = new BoatKit(boat);
        // the staves: rings bulging out in the middle, the bottom
        for (float y = RIM; y < FLOOR; y += 2.0F) {
            float mid = (y + 1.0F - RIM) / (FLOOR - RIM);
            float r = 9.0F + 1.2F * Mth.sin((float) Math.PI * Math.min(1.0F, mid * 0.8F + 0.1F));
            k.ringY(WOOD, 0.0F, y, 0.0F, r, 2.0F, 1.0F, 18, (int) (y * 5));
        }
        k.disc(WOOD, 0.0F, FLOOR, 0.0F, 9.6F, 1.2F);
        // rusty iron hoops
        k.ringY(GREY, 0.0F, RIM + 0.6F, 0.0F, 9.6F, 0.8F, 0.25F, 18, 0);
        k.ringY(GREY, 0.0F, FLOOR - 2.0F, 0.0F, 10.0F, 0.8F, 0.25F, 18, 0);
        // the water sloshing in the bottom
        k.disc(ICE, 0.0F, WATER_Y, 0.0F, 9.2F, 0.2F);
        // the composter captain's chair, its compost on top
        k.box(OLDWOOD, -3.5F, -4.6F, -2.5F, 5.0F, 5.6F, 5.0F);
        k.box(MOSS, -3.2F, -4.7F, -2.2F, 4.4F, 0.2F, 4.4F);
        for (int i = 0; i < 4; i++) k.box(DARKWOOD, -3.6F + (i % 2) * 4.6F, -4.6F, -2.6F + (i / 2) * 4.6F, 0.6F, 5.6F, 0.6F);
        // the crooked bamboo shoot and its sad banner
        k.line(BAMBOO, -5.0F, WATER_Y, 5.5F, -6.0F, -12.0F, 6.5F, 0.7F);
        k.line(BAMBOO, -6.0F, -12.0F, 6.5F, -5.0F, -22.0F, 7.5F, 0.6F);
        k.line(BAMBOO, -5.0F, -22.0F, 7.5F, -6.5F, -27.0F, 8.5F, 0.5F);
        k.line(VINE, -5.6F, -14.0F, 6.8F, -3.6F, -16.0F, 6.8F, 0.3F);      // a leaf
        PartDefinition banner = boat.addOrReplaceChild("banner", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 8.0F));
        for (int i = 0; i < 6; i++) {
            float h = 6.0F - i * 0.5F;
            CubeListBuilder strip = CubeListBuilder.create();
            box(strip, CANVAS, i * 2, 0, -1.0F, 0.0F, 0.0F, 1.0F, h, 0.0F);
            banner.addOrReplaceChild("f" + i, strip, PartPose.offset(-6.0F - i, -26.0F + i * 0.4F, 0.0F));
        }
        // the wooden shovel paddle, held over the starboard side (it paddles: setupAnim)
        CubeListBuilder shovel = CubeListBuilder.create();
        box(shovel, WOOD, 0, 0, -0.3F, -0.3F, -14.0F, 0.6F, 0.6F, 12.0F);
        box(shovel, DARKWOOD, 0, 0, -1.4F, -0.2F, -17.0F, 2.8F, 0.4F, 3.4F);
        box(shovel, WOOD, 0, 0, -0.8F, -0.8F, -2.6F, 1.6F, 1.6F, 0.6F);
        boat.addOrReplaceChild("shovel", shovel, PartPose.offset(1.0F, RIM - 0.5F, -6.0F));
        // a message in a bottle bobbing in the water, a coconut
        CubeListBuilder bottle = CubeListBuilder.create();
        box(bottle, TINT, 0, 0, -1.2F, -0.5F, -0.5F, 2.4F, 1.0F, 1.0F);
        box(bottle, TINT, 0, 0, 1.2F, -0.25F, -0.25F, 0.8F, 0.5F, 0.5F);
        box(bottle, CANVAS, 0, 0, -0.8F, -0.3F, -0.3F, 1.4F, 0.6F, 0.6F);
        box(bottle, OLDWOOD, 0, 0, 2.0F, -0.2F, -0.2F, 0.3F, 0.4F, 0.4F);
        boat.addOrReplaceChild("bottle", bottle, PartPose.offset(4.5F, WATER_Y - 0.4F, 4.0F));
        k.box(DARKWOOD, -6.5F, WATER_Y - 1.4F, -5.0F, 1.6F, 1.6F, 1.6F);
        boat.addOrReplaceChild("hull", k.statics, PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}

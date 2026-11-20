package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static com.alexh.capitalism.client.BloodDecorModels.K;
import static com.alexh.capitalism.client.BoatKit.*;

// The Traveling Merchant's cosmetic hats on a player's head (Town.md > The Traveling Merchant): for roleplaying your job.
// Worn in the head slot (no armour, no effect); each a little model of its own, all on one palette texture
// (textures/entity/cosmetic_hats.png, tools/make_cosmetic_hats.ps1, painted in BoatKit's zones):
//  - Chef's Hat: a tall puffed white toque on a pleated band
//  - Welder's Mask: a grey steel face plate with a dark green visor slot, strapped round the head
//  - Wizard's Hat: a tall navy cone bending back, a wide brim, a gold band and gold stars
//  - Farmer's Hat: a wide straw sun hat with a red band and an ear of wheat tucked in
//  - Traveler's Pipe: just a clay-and-wood pipe hanging from the corner of your mouth, an ember glowing in its bowl
//  - Ghost Sheet (the Blood Merchant's, Halloween): a white bedsheet over you down to the knees, two black eye holes,
//    a ragged hem
public class CosmeticHatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public enum Kind {
        CHEFS_HAT(ModItems.CHEFS_HAT), WELDERS_MASK(ModItems.WELDERS_MASK), WIZARDS_HAT(ModItems.WIZARDS_HAT),
        FARMERS_HAT(ModItems.FARMERS_HAT), TRAVELERS_PIPE(ModItems.TRAVELERS_PIPE),
        // the Blood Merchant's Halloween Ghost Sheet (player/SpookyGear): the sheet hangs from the shoulders and turns with
        // the body; its hood (GHOST_HOOD, drawn along with it) sits on the head and turns and nods with it
        GHOST_SHEET(ModItems.GHOST_SHEET), GHOST_HOOD(ModItems.GHOST_SHEET);

        final Supplier<? extends Item> item;
        public final ModelLayerLocation layer;

        Kind(Supplier<? extends Item> item) {
            this.item = item;
            this.layer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "cosmetic_" + this.name().toLowerCase(Locale.ROOT)), "main");
        }
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/cosmetic_hats.png");
    private final Map<Kind, BloodDecorModels.Base> models = new EnumMap<>(Kind.class);

    public CosmeticHatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, Map<Kind, ModelPart> roots) {
        super(renderer);
        roots.forEach((kind, root) -> this.models.put(kind, new BloodDecorModels.Base(root) {
        }));
    }

    // In the head's space (pixels, y down: the head from y -8 to 0, its face at z -4)
    public static LayerDefinition createLayer(Kind kind) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = DecorModels.start(mesh);
        K k = new K(root);
        switch (kind) {
            case CHEFS_HAT -> {
                k.box(WHITE, -4.3, -9.6, -4.3, 8.6, 1.9, 8.6);            // the pleated band
                for (int i = 0; i < 8; i++) k.box(CANVAS, -4.35 + i * 1.2, -9.5, -4.4, 0.25, 1.7, 0.1);   // pleats
                k.box(WHITE, 20, 0, -4.9, -13.4, -4.9, 9.8, 3.8, 9.8);     // the puff
                k.box(WHITE, 40, 10, -4.2, -14.3, -4.2, 8.4, 0.9, 8.4);    // its rounded top
            }
            case WELDERS_MASK -> {
                k.box(STEEL, -4.4, -7.8, -4.95, 8.8, 7.9, 0.6);            // the face plate
                k.box(STEEL, 20, 8, -4.4, -7.8, -4.95, 0.6, 7.9, 1.6);     // its edges wrapping the cheeks
                k.box(STEEL, 30, 8, 3.8, -7.8, -4.95, 0.6, 7.9, 1.6);
                k.box(TINT, -3.0, -5.8, -5.1, 6.0, 1.6, 0.2);              // the dark visor slot
                k.box(BLACK, -3.2, -6.0, -5.06, 6.4, 2.0, 0.1);
                k.box(LEATHER, -4.6, -6.4, -3.4, 0.4, 1.2, 8.0);           // straps round the head
                k.box(LEATHER, 10, 0, 4.2, -6.4, -3.4, 0.4, 1.2, 8.0);
                k.box(LEATHER, 20, 0, -4.6, -6.4, 4.2, 9.2, 1.2, 0.4);
                k.box(LEATHER, 0, 10, -0.6, -8.6, -4.0, 1.2, 0.4, 8.6);    // and over the top
                k.box(BRASS, -4.9, -5.5, -4.0, 0.5, 1.0, 1.0).box(BRASS, 4.4, -5.5, -4.0, 0.5, 1.0, 1.0);   // the hinges
            }
            case WIZARDS_HAT -> {
                k.box(NAVY, -6.2, -8.5, -6.2, 12.4, 0.5, 12.4);            // the wide brim
                k.box(NAVY, 0, 6, -4.2, -11.5, -4.2, 8.4, 3.0, 8.4);       // the cone, bending back as it rises
                k.box(NAVY, 0, 10, -3.2, -14.2, -2.8, 6.4, 2.8, 6.4);
                k.box(NAVY, 20, 0, -2.2, -16.6, -1.4, 4.4, 2.5, 4.4);
                k.box(NAVY, 20, 8, -1.3, -18.6, 0.0, 2.6, 2.1, 2.6);
                k.box(NAVY, 24, 12, -0.6, -20.0, 1.4, 1.2, 1.5, 1.2);
                k.box(GOLD, -4.3, -9.3, -4.3, 8.6, 0.8, 8.6);              // the gold band
                k.box(GOLD, -2.2, -12.4, -4.35, 1.0, 1.0, 0.2).box(GOLD, 1.4, -10.9, -4.35, 0.7, 0.7, 0.2);   // stars
                k.box(GOLD, 0.2, -15.2, -2.95, 0.8, 0.8, 0.2).box(GOLD, -2.5, -12.0, 1.0, 0.8, 0.8, 0.2);
            }
            case FARMERS_HAT -> {
                k.box(CANVAS, -7.0, -8.5, -7.0, 14.0, 0.5, 14.0);          // the wide straw brim
                k.box(CANVAS, 0, 16, -4.0, -11.2, -4.0, 8.0, 2.8, 8.0);    // the crown
                k.box(RED, -4.05, -9.0, -4.05, 8.1, 0.6, 8.1);             // the red band
                k.line(GOLD, 3.9, -9.2, -1.5, 5.6, -13.4, 0.5, 0.35);      // an ear of wheat tucked in the band
                k.box(GOLD, 5.2, -14.6, 0.1, 0.8, 1.6, 0.8);
            }
            case TRAVELERS_PIPE -> {
                // from the corner of the mouth, the stem dipping forward, the bowl rising at the end
                k.line(WOOD, 1.4, -1.5, -4.1, 2.9, -0.9, -8.4, 0.85);
                k.box(DARKWOOD, 2.0, -3.6, -9.6, 2.2, 2.9, 2.2);
                k.box(FIRE, 2.3, -3.75, -9.3, 1.6, 0.3, 1.6);
            }
            case GHOST_SHEET -> {
                k.box(WHITE, 0, 10, -8.4, 0.4, -3.6, 16.8, 10.0, 7.2);       // hanging off the shoulders, over the arms
                k.box(WHITE, 0, 0, -7.6, 0.0, -3.0, 15.2, 0.6, 6.0);
                double[] hem = {2.0, 1.2, 2.6, 1.4, 2.2, 1.0, 2.4};          // the ragged hem
                for (int i = 0; i < hem.length; i++) {
                    k.box(WHITE, i * 4, 20, -8.4 + i * 2.4, 10.4, -3.6, 2.4, hem[i], 0.8);
                    k.box(WHITE, i * 4, 24, -8.4 + i * 2.4, 10.4, 2.8, 2.4, hem[hem.length - 1 - i], 0.8);
                }
            }
            case GHOST_HOOD -> {
                // in the head's space: snug over the head (and the hat layer), so it turns and nods with it
                k.box(WHITE, -4.7, -9.0, -4.7, 9.4, 1.0, 9.4);              // over the top of the head
                k.box(WHITE, 20, 0, -5.0, -8.0, -5.0, 10.0, 8.8, 10.0);       // round the head, down over the neck
                k.box(BLACK, -2.9, -5.6, -5.15, 1.8, 2.2, 0.2).box(BLACK, 1.1, -5.6, -5.15, 1.8, 2.2, 0.2);   // the eye holes
                k.box(BLACK, -1.0, -2.6, -5.1, 2.0, 1.2, 0.15);                                             // a little "oh" mouth
            }
        }
        k.done();
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        for (Kind kind : Kind.values()) {
            if (!state.headEquipment.is(kind.item.get())) continue;
            BloodDecorModels.Base model = this.models.get(kind);
            if (model == null) return;
            poseStack.pushPose();
            (kind == Kind.GHOST_SHEET ? this.getParentModel().body : this.getParentModel().head).translateAndRotate(poseStack);
            collector.submitModel(model, new BloodDecorModels.Anim(0, 0, false, 0, 0, 0), poseStack, TEXTURE, lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
            BloodDecorModels.Base hood = kind == Kind.GHOST_SHEET ? this.models.get(Kind.GHOST_HOOD) : null;
            if (hood != null) {
                poseStack.pushPose();
                this.getParentModel().head.translateAndRotate(poseStack);
                collector.submitModel(hood, new BloodDecorModels.Anim(0, 0, false, 0, 0, 0), poseStack, TEXTURE, lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
            return;
        }
    }
}

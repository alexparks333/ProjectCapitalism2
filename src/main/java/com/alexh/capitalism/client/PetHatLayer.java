package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.designer.Hat;
import com.alexh.capitalism.pets.Pets;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

// A pet's hat (designer/Hat), on its head for everyone to see: each hat a little model of its own (a top hat with a
// green and red band, a checked santa hat with a white trim and bobble, a monogrammed baseball cap with its peak to
// the front, a fedora with a silver band), textured by tools/make_designer_textures.ps1. It sits on the top of the
// animal's skull (the biggest box of its model's head part), scaled to the skull's width, and turns with the head.
// Added to every living thing's renderer; only pets with a hat (HAT, set from the pet's data) get one.
public class PetHatLayer extends RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {

    public static final ContextKey<Hat> HAT = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_hat"));
    public static final Map<Hat, ModelLayerLocation> LAYERS = new EnumMap<>(Hat.class);

    static {
        for (Hat hat : Hat.values()) {
            LAYERS.put(hat, new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_hat_" + hat.name().toLowerCase(Locale.ROOT)), "main"));
        }
    }

    // the top of a head's skull, in the head part's own space (pixels): its middle, its top, its width
    private record Skull(float x, float top, float z, float width) {
    }

    private static final Map<ModelPart, Skull> SKULLS = new WeakHashMap<>();

    // the hat models, baked once (ClientModEvents)
    private static final Map<Hat, Model<Unit>> MODELS = new EnumMap<>(Hat.class);

    public PetHatLayer(RenderLayerParent<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> renderer, Map<Hat, ModelPart> roots) {
        super(renderer);
        roots.forEach((hat, root) -> MODELS.put(hat, new Model.Simple(root, RenderTypes::entityCutout)));
    }

    // ── the hats, built for a head 8 pixels wide, sitting on y = 0 (up is -y, the front is -z) ──

    public static LayerDefinition createLayer(Hat hat) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (hat) {
            case GUCCI_TOP_HAT -> {
                root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -1.0F, -5.5F, 11, 1, 11), PartPose.ZERO);
                root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -9.0F, -3.5F, 7, 8, 7), PartPose.ZERO);
                root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 32).addBox(-3.5F, -3.0F, -3.5F, 7, 2, 7, new CubeDeformation(0.15F)), PartPose.ZERO);
            }
            case BURBERRY_SANTA_HAT -> {
                root.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -2.0F, -4.5F, 9, 2, 9), PartPose.ZERO);
                root.addOrReplaceChild("cone", CubeListBuilder.create().texOffs(0, 14).addBox(-3.5F, -5.0F, -3.5F, 7, 3, 7)
                    .texOffs(0, 26).addBox(-2.5F, -7.5F, -2.0F, 5, 3, 5)
                    .texOffs(24, 26).addBox(-1.5F, -9.0F, 0.0F, 3, 2, 3), PartPose.ZERO);
                root.addOrReplaceChild("bobble", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -9.0F, 2.5F, 2, 2, 2), PartPose.ZERO);
            }
            case LOUIS_VUITTON_CAP -> {
                root.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(0, 0).addBox(-4.2F, -3.0F, -4.2F, 8.4F, 3, 8.4F), PartPose.ZERO);
                root.addOrReplaceChild("peak", CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -0.8F, -8.0F, 8, 1, 4), PartPose.ZERO);
                root.addOrReplaceChild("button", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -3.8F, -1.0F, 2, 1, 2), PartPose.ZERO);
            }
            case FEEDING_HAT -> {
                // a straw sun hat with a little red feed bowl on top, a carrot poking out of it
                root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -1.0F, -6.0F, 12, 1, 12), PartPose.ZERO);
                root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -4.0F, -3.0F, 6, 3, 6), PartPose.ZERO);
                root.addOrReplaceChild("bowl", CubeListBuilder.create().texOffs(32, 16).addBox(-2.0F, -5.0F, -2.0F, 4, 1, 4), PartPose.ZERO);
                root.addOrReplaceChild("carrot", CubeListBuilder.create().texOffs(48, 16).addBox(-0.5F, -7.0F, -0.5F, 1, 2, 1),
                    PartPose.rotation(0.2F, 0.0F, 0.25F));
            }
            case PRADA_FEDORA -> {
                root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -1.0F, -6.0F, 12, 1, 12), PartPose.ZERO);
                root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -5.0F, -4.0F, 8, 4, 8), PartPose.ZERO);
                root.addOrReplaceChild("band", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -2.2F, -4.0F, 8, 1, 8, new CubeDeformation(0.15F)), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    // With the animal's render state: the hat its pet data says it wears (and its name's fade, PetNames)
    public static void extract(Entity entity, LivingEntityRenderState state) {
        if (!(entity instanceof Animal animal) || !Pets.isPet(animal)) return;
        PetNames.extract(entity, state);
        Hat hat = Pets.data(animal).hat();
        if (hat != null) state.setRenderData(HAT, hat);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, LivingEntityRenderState state, float yRot, float xRot) {
        Hat hat = state.getRenderData(HAT);
        if (hat == null || state.isInvisible) return;
        ModelPart root = this.getParentModel().root();
        List<ModelPart> path = headPath(root);
        if (path == null) return;
        poseStack.pushPose();
        // the root first: some models are scaled or moved as a whole (left out, the hat sank into the head)
        root.translateAndRotate(poseStack);
        for (int i = 0; i < path.size() - 1; i++) path.get(i).translateAndRotate(poseStack);
        submitOn(poseStack, collector, hat, path.getLast(), lightCoords, state.outlineColor);
        poseStack.popPose();
    }

    // The hat on a head part, from the space the head part sits in (it applies the head's own place and turn): used here
    // and for the head peeking out of an Animal Bjorn (BjornLayer)
    public static void submitOn(PoseStack poseStack, SubmitNodeCollector collector, Hat hat, ModelPart head, int lightCoords, int outlineColor) {
        Model<Unit> model = MODELS.get(hat);
        Skull skull = skull(head);
        if (model == null || skull == null) return;
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        // sunk a touch into the top of the skull, so it sits snug rather than floating
        poseStack.translate(skull.x() / 16.0F, (skull.top() + 0.6F) / 16.0F, skull.z() / 16.0F);
        float scale = Math.max(0.35F, skull.width() / 8.0F);
        poseStack.scale(scale, scale, scale);
        Identifier texture = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/pet_hat_" + hat.name().toLowerCase(Locale.ROOT) + ".png");
        collector.submitModel(model, Unit.INSTANCE, poseStack, texture, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    // The parts from the model's root down to its head ("head", or "head" inside "body"), or null if it has none
    private static @Nullable List<ModelPart> headPath(ModelPart root) {
        List<ModelPart> path = new ArrayList<>();
        if (root.hasChild("head")) {
            path.add(root.getChild("head"));
            return path;
        }
        if (root.hasChild("body") && root.getChild("body").hasChild("head")) {
            path.add(root.getChild("body"));
            path.add(root.getChild("body").getChild("head"));
            return path;
        }
        return null;
    }

    // The skull: the biggest box of the head part itself (not its ears or snout children)
    private static @Nullable Skull skull(ModelPart head) {
        return SKULLS.computeIfAbsent(head, part -> {
            ModelPart.Cube[] best = new ModelPart.Cube[1];
            part.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (!path.isEmpty()) return;
                float volume = (cube.maxX - cube.minX) * (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ);
                if (best[0] == null || volume > (best[0].maxX - best[0].minX) * (best[0].maxY - best[0].minY) * (best[0].maxZ - best[0].minZ)) best[0] = cube;
            });
            ModelPart.Cube cube = best[0];
            if (cube == null) return null;
            // a little toward the front of the skull: many heads sink back into a taller body (a polar bear's), which would
            // swallow a hat set in the middle
            return new Skull((cube.minX + cube.maxX) / 2, cube.minY, cube.minZ + (cube.maxZ - cube.minZ) * 0.4F, Math.min(cube.maxX - cube.minX, cube.maxZ - cube.minZ));
        });
    }
}

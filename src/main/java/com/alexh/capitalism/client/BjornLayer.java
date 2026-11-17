package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.back.Back;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.pets.AnimalBjornItem;
import com.alexh.capitalism.pets.CarriedAnimal;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The Animal Bjorn worn in the Back slot (back/Back, pets/AnimalBjornItem), drawn on players: a padded carrier on the
// chest with straps over the shoulders (texture textures/entity/animal_bjorn.png, tools/make_pet_textures.ps1), and the
// animal's head poking out of its top front, slowly looking around (an animal with no head part, like a bee: the whole
// animal, shrunk). Everyone sees it (the Back slot is synced).
// The animal is a stand-in made on the client from the bjorn's saved data (one per player, made again when it changes);
// its render state is worked out with the player's (MODIFIER) and drawn here.
public class BjornLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Capitalism.MODID, "animal_bjorn"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/animal_bjorn.png");
    // on the player's render state: wearing a bjorn, and the animal in it (if any)
    public static final ContextKey<Boolean> WEARING = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "wearing_bjorn"));
    public static final ContextKey<LivingEntityRenderState> ANIMAL = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "bjorn_animal"));
    // ... and the hat it wears (designer/Hat), drawn on the head peeking out (PetHatLayer.submitOn)
    public static final ContextKey<com.alexh.capitalism.designer.Hat> HAT = new ContextKey<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "bjorn_hat"));
    // how big the animal is drawn: its biggest side this many blocks
    private static final float ANIMAL_SIZE = 0.55F;

    private record Standin(CarriedAnimal carried, Entity entity) {
    }

    private static final Map<UUID, Standin> STANDINS = new HashMap<>();
    // a model of just the head, for each animal model's head part
    private static final Map<ModelPart, Model<Unit>> HEADS = new java.util.WeakHashMap<>();
    // the head peeking out is a bit smaller than the real thing
    private static final float HEAD_SCALE = 0.7F;
    // the head looks around at most this many degrees from straight ahead
    private static final float LOOK_CONE = 40.0F;
    private static int nextStandinId = -1_000_000;

    private final Model<Unit> carrier;

    public BjornLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, ModelPart root) {
        super(renderer);
        this.carrier = new Model.Simple(root, RenderTypes::entityCutout);
    }

    // The carrier, in the body's space (pixels, y down, the front at z = -2): a padded pouch on the chest, straps up over
    // both shoulders and down the back, and a band round the waist behind
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // shallow (2 deep), snug against the chest, so the head poking out of it doesn't clip into it
        root.addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 3.0F, -4.0F, 8, 8, 2), PartPose.ZERO);
        root.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(0, 12).addBox(-4.5F, 2.5F, -4.5F, 9, 1, 3), PartPose.ZERO);
        root.addOrReplaceChild("left_strap", CubeListBuilder.create().texOffs(26, 0).addBox(2.0F, -0.2F, -2.6F, 1, 1, 5.2F)
            .texOffs(40, 0).addBox(2.0F, 0.0F, 2.0F, 1, 10, 0.6F), PartPose.ZERO);
        root.addOrReplaceChild("right_strap", CubeListBuilder.create().texOffs(26, 0).addBox(-3.0F, -0.2F, -2.6F, 1, 1, 5.2F)
            .texOffs(40, 0).addBox(-3.0F, 0.0F, 2.0F, 1, 10, 0.6F), PartPose.ZERO);
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(0, 18).addBox(-4.3F, 9.0F, 2.0F, 8.6F, 1, 0.7F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    // With the player's render state: are they wearing a bjorn, and what's in it (its render state, from the stand-in)
    public static void extract(Player player, AvatarRenderState state, float partialTick) {
        ItemStack worn = Back.get(player);
        if (!worn.is(ModItems.ANIMAL_BJORN.get())) {
            STANDINS.remove(player.getUUID());
            return;
        }
        state.setRenderData(WEARING, true);
        CarriedAnimal carried = AnimalBjornItem.carried(worn);
        Entity standin = carried == null ? null : standin(player, carried);
        if (standin == null) return;
        standin.setPos(player.getX(), player.getY(), player.getZ());
        standin.tickCount = player.tickCount;
        LivingEntityRenderState animal;
        try {
            if (!(Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(standin, partialTick) instanceof LivingEntityRenderState living)) return;
            animal = living;
        } catch (RuntimeException broken) {
            // never crash the game over the little animal: just don't draw it
            com.mojang.logging.LogUtils.getLogger().warn("[{}] Can't draw the animal in {}'s Animal Bjorn: {}", Capitalism.MODID, player.getName().getString(), broken.toString());
            return;
        }
        animal.bodyRot = 180.0F;
        animal.yRot = 0.0F;
        animal.xRot = 0.0F;
        animal.walkAnimationSpeed = 0.0F;
        animal.nameTag = null;
        animal.leashStates = null;
        animal.shadowPieces.clear();
        animal.shadowRadius = 0.0F;
        animal.displayFireAnimation = false;
        animal.lightCoords = state.lightCoords;
        animal.outlineColor = state.outlineColor;
        state.setRenderData(ANIMAL, animal);
        if (standin instanceof net.minecraft.world.entity.animal.Animal pet && com.alexh.capitalism.pets.Pets.isPet(pet)) {
            com.alexh.capitalism.designer.Hat hat = com.alexh.capitalism.pets.Pets.data(pet).hat();
            if (hat != null) state.setRenderData(HAT, hat);
        }
    }

    // The stand-in animal for this player's bjorn (made again when what's in it changes)
    private static @Nullable Entity standin(Player player, CarriedAnimal carried) {
        Standin known = STANDINS.get(player.getUUID());
        if (known != null && known.carried().equals(carried)) return known.entity();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(carried.type());
        Entity entity = EntityType.create(type, TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), carried.data()),
            player.level(), EntitySpawnReason.LOAD).orElse(null);
        if (entity == null) return null;
        // never added to the world, so it needs an id of its own (renderers ask for it); negative ones are never real
        entity.setId(nextStandinId--);
        STANDINS.put(player.getUUID(), new Standin(carried, entity));
        return entity;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (!Boolean.TRUE.equals(state.getRenderData(WEARING)) || state.isInvisible) return;
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        collector.submitModel(this.carrier, Unit.INSTANCE, poseStack, TEXTURE, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        LivingEntityRenderState animal = state.getRenderData(ANIMAL);
        if (animal != null && !this.submitHead(poseStack, collector, lightCoords, state, animal)) {
            // an animal with no head part (a bee): the whole animal, shrunk, in the pouch
            poseStack.translate(0.0F, 9.0F / 16.0F, -3.0F / 16.0F);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            float size = Math.max(animal.boundingBoxWidth, animal.boundingBoxHeight);
            float scale = Math.min(0.8F, ANIMAL_SIZE / Math.max(0.1F, size));
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getEntityRenderDispatcher().submit((EntityRenderState) animal, new CameraRenderState(), 0.0, 0.0, 0.0, poseStack, collector);
        }
        poseStack.popPose();
    }

    // Just the animal's head, poking out of the top front of the pouch and slowly looking around (a few slow waves of
    // yaw and pitch, so it never quite repeats). Its own model's head part, drawn on its own with its texture. False if
    // its model has no head part.
    @SuppressWarnings("unchecked")
    private boolean submitHead(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, LivingEntityRenderState animal) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer((EntityRenderState) animal)
            instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> living)) return false;
        net.minecraft.client.renderer.entity.LivingEntityRenderer<?, LivingEntityRenderState, ?> renderer =
            (net.minecraft.client.renderer.entity.LivingEntityRenderer<?, LivingEntityRenderState, ?>) living;
        net.minecraft.client.model.EntityModel<?> model = renderer.getModel();
        ModelPart head = head(model.root());
        if (head == null) return false;
        Model<Unit> only = HEADS.computeIfAbsent(head, part -> new Model.Simple(part, model.renderType()));
        net.minecraft.client.model.geom.PartPose pose = head.getInitialPose();
        // where it looks: a slow wander inside a cone LOOK_CONE degrees round straight ahead (never right round to the
        // side, up or down), so it looks about but mostly faces forward
        float t = state.ageInTicks;
        float yaw = (float) (Math.sin(t * 0.031) * 0.65 + Math.sin(t * 0.097 + 1.3) * 0.35);
        float pitch = (float) (Math.sin(t * 0.047 + 2.1) * 0.6 + Math.sin(t * 0.073 + 0.4) * 0.4);
        float reach = (float) Math.sqrt(yaw * yaw + pitch * pitch);
        if (reach > 1.0F) {
            yaw /= reach;
            pitch /= reach;
        }
        yaw *= LOOK_CONE;
        pitch *= LOOK_CONE;
        poseStack.pushPose();
        // the top front of the pouch, the neck just under the rim
        poseStack.translate(0.0F, 3.5F / 16.0F, -4.0F / 16.0F);
        poseStack.mulPose(new org.joml.Matrix4f().rotationYXZ(yaw * net.minecraft.util.Mth.DEG_TO_RAD, pitch * net.minecraft.util.Mth.DEG_TO_RAD, 0.0F));
        poseStack.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
        // the head part puts itself at its spot on the animal: move that spot to here
        poseStack.translate(-pose.x() / 16.0F, -pose.y() / 16.0F, -pose.z() / 16.0F);
        collector.submitModel(only, Unit.INSTANCE, poseStack, renderer.getTextureLocation(animal), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        com.alexh.capitalism.designer.Hat hat = state.getRenderData(HAT);
        if (hat != null) PetHatLayer.submitOn(poseStack, collector, hat, head, lightCoords, state.outlineColor);
        poseStack.popPose();
        return true;
    }

    // The head part: "head" at the top of the model, or inside its "body"
    private static @Nullable ModelPart head(ModelPart root) {
        if (root.hasChild("head")) return root.getChild("head");
        if (root.hasChild("body") && root.getChild("body").hasChild("head")) return root.getChild("body").getChild("head");
        return null;
    }
}

package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.boats.PremiumBoat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.function.Function;

// Draws a Premium boat (boats/PremiumBoat, Boats.md) with its model: turned, tipped when hit and rocked like any boat
// (as vanilla's AbstractBoatRenderer), then a second, glowing pass (textures/entity/<name>_glow.png: only its lights,
// lit windows, fire...) so those shine at night. Tells the model how fast it's going, how fast it's turning and, if
// you're riding it in first person, where you sit.
// Riding it in first person, you see it GHOSTED: drawn see-through, so the masts, sails, decks and cabins in front of
// you don't block your view (everyone else, and you in third person, see it solid).
// It isn't lit by its own light blocks (they light what's round it). Seen from further away than a boat: the models
// reach well past the boat's little box (`side` blocks out, `up` blocks above).
public class PremiumBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {

    // a boat's top speed on water is about this (blocks a tick): "full speed" for the animations from here
    private static final float FULL_SPEED = 0.35F;
    // how solid the boat looks to whoever rides it in first person (alpha out of 255)
    private static final int GHOST_ALPHA = 0x60;

    public static class State extends BoatRenderState {
        public float speed;
        public float turn;
        // the seat (model x) of the player looking from aboard in first person; NaN if that's not you
        public float viewerX = Float.NaN;
        // a lamp on (the Submersible's flashlight)
        public boolean lamp;
        // where it points, relative to the boat (radians: + to the right, + down)
        public float lampYaw;
        public float lampPitch;
        // how far its wings are spread (the Fairy Blossom: 0 folded on the water, 1 open in the air)
        public float flight;
    }

    private final EntityModel<BoatRenderState> model;
    private final Identifier texture;
    private final Identifier glow;
    private final double side;
    private final double up;

    public PremiumBoatRenderer(EntityRendererProvider.Context context, String name, ModelLayerLocation layer,
                               Function<ModelPart, EntityModel<BoatRenderState>> model, float shadow, double side, double up) {
        super(context);
        this.model = model.apply(context.bakeLayer(layer));
        this.texture = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + name + ".png");
        this.glow = Identifier.fromNamespaceAndPath(Capitalism.MODID, "textures/entity/" + name + "_glow.png");
        this.shadowRadius = shadow;
        this.side = side;
        this.up = up;
    }

    @Override
    public BoatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AbstractBoat entity, BoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
        state.bubbleAngle = entity.getBubbleAngle(partialTicks);
        state.isUnderWater = entity.isUnderWater();
        state.rowingTimeLeft = entity.getRowingTime(0, partialTicks);
        state.rowingTimeRight = entity.getRowingTime(1, partialTicks);
        if (state instanceof State boat) {
            boat.speed = Mth.clamp((float) entity.getDeltaMovement().horizontalDistance() / FULL_SPEED, 0.0F, 1.0F);
            boat.turn = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
            Minecraft mc = Minecraft.getInstance();
            int seat = mc.player == null ? -1 : entity.getPassengers().indexOf(mc.player);
            boat.viewerX = seat >= 0 && mc.options.getCameraType().isFirstPerson() && entity instanceof PremiumBoat premium
                ? premium.seatForward(seat) : Float.NaN;
            boat.flight = entity instanceof com.alexh.capitalism.boats.FairyBoat fairy ? fairy.wingSpread(partialTicks) : 0.0F;
            boat.lamp = entity instanceof com.alexh.capitalism.boats.Submersible sub && sub.flashlightOn();
            if (boat.lamp) {
                float[] aim = ((com.alexh.capitalism.boats.Submersible) entity).lampAim(partialTicks);
                boat.lampYaw = aim[0] * Mth.DEG_TO_RAD;
                boat.lampPitch = aim[1] * Mth.DEG_TO_RAD;
            }
        }
    }

    @Override
    public void submit(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        float hurt = state.hurtTime;
        if (hurt > 0.0F) poseStack.rotateDegrees(Axis.XP, Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir);
        if (!state.isUnderWater && !Mth.equal(state.bubbleAngle, 0.0F)) {
            poseStack.rotate(new Quaternionf().setAngleAxis(state.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.rotateDegrees(Axis.YP, 90.0F);
        boolean ghosted = state instanceof State s && !Float.isNaN(s.viewerX);
        if (ghosted) {
            collector.submitModel(this.model, state, poseStack, RenderTypes.entityTranslucentCull(this.texture), state.lightCoords,
                OverlayTexture.NO_OVERLAY, (GHOST_ALPHA << 24) | 0xFFFFFF, null, state.outlineColor);
        } else {
            collector.submitModel(this.model, state, poseStack, this.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        // the glow pass: lights, windows, fire... full bright
        collector.submitModel(this.model, state, poseStack, RenderTypes.eyes(this.glow), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    // The boat isn't lit by its own light blocks: its own brightest light is taken off the light on it, every frame
    // the same, so it never flickers as its lights move along with it. Other lights (torches) still light it.
    @Override
    protected int getBlockLightLevel(AbstractBoat entity, net.minecraft.core.BlockPos pos) {
        int light = super.getBlockLightLevel(entity, pos);
        if (entity instanceof PremiumBoat boat) return Math.max(0, light - boat.maxOwnLight());
        return light;
    }

    @Override
    protected AABB getBoundingBoxForCulling(AbstractBoat entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(this.side, 0.0, this.side).expandTowards(0.0, this.up, 0.0);
    }
}

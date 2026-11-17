package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.decor.BloodDecorKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

// A Blood Moon decoration as an item (in the inventory, the hand, on the ground, in a frame): its own 3D model and
// glow, the same as when it's placed (client/BloodDecorRenderer.DEFS), not a flat picture. Each item's
// items/<id>.json is a "minecraft:special" model of type capitalism:blood_decor naming its decoration; its "base" item
// model (models/item/blood_decor.json, or blood_decor_flat.json for the wall pieces, seen face-on) gives the display
// transforms. The model is turned with its front to the item's north (-z) side, shrunk or grown so its biggest side is
// one block, and centred on the item's block, so every one sits in a slot like a block does. It's animated (flames
// flicker, the fountain runs), lit (the chandelier and fireplace burn) and shut (the coffin, the maiden).
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class BloodDecorItemRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_decor");

    private final BloodDecorKind kind;
    private final BloodDecorModels.Base model;
    private final Identifier texture;
    private final Identifier glow;
    // fitting it to a block: its box's middle (after the turn) and the scale that makes its biggest side 1
    private final Vector3f middle = new Vector3f();
    private final float fit;

    private BloodDecorItemRenderer(BloodDecorKind kind, BloodDecorModels.Base model) {
        this.kind = kind;
        this.model = model;
        this.texture = BloodDecorRenderer.tex(kind.id);
        this.glow = BloodDecorRenderer.tex(kind.id + "_glow");
        // Fitted to the view it's seen from in a slot (VIEW: the gui rotation in its base item model), so that seen from
        // there it fills a unit square: as big as it can be, edge to edge, in every slot (models/item/blood_decor.json then
        // draws it a hair smaller than the slot). The same size in the hand, on the ground and in a frame.
        org.joml.Quaternionf view = view(kind);
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        PoseStack pose = new PoseStack();
        turn(pose, kind);
        this.model.setupAnim(anim(0.0F));
        this.model.root().getExtentsForGui(pose, v -> {
            Vector3f p = view.transform(new Vector3f(v));
            min[0] = Math.min(min[0], p.x());
            min[1] = Math.min(min[1], p.y());
            min[2] = Math.min(min[2], p.z());
            max[0] = Math.max(max[0], p.x());
            max[1] = Math.max(max[1], p.y());
            max[2] = Math.max(max[2], p.z());
        });
        // the middle of what you see, taken back into the model's frame
        this.middle.set((min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2);
        new org.joml.Quaternionf(view).conjugate().transform(this.middle);
        float biggest = Math.max(max[0] - min[0], max[1] - min[1]);
        this.fit = biggest <= 0 ? 1.0F : 1.0F / biggest;
    }

    // the view it's seen from in a slot: models/item/blood_decor.json's gui rotation (a 3/4 view from above), or face-on
    // for the wall pieces (blood_decor_flat.json). Keep them the same as those files.
    static final float VIEW_X = 25.0F, VIEW_Y = 210.0F;

    static org.joml.Quaternionf view(BloodDecorKind kind) {
        if (kind.wallMounted) return new org.joml.Quaternionf();
        return new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(VIEW_X), (float) Math.toRadians(VIEW_Y), 0.0F);
    }

    // the model as it's placed (y up, its front to the south), before it's fitted
    private static void turn(PoseStack pose, BloodDecorKind kind) {
        // (the standing ones turned round so their fronts face the camera in a slot; the wall pieces were already right)
        pose.rotateDegrees(Axis.YP, kind.wallMounted ? 90.0F : -90.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
    }

    // how it's drawn as an item: animated by the clock, lit, shut, by day, its eyes ahead
    private BloodDecorModels.Anim anim(float time) {
        return new BloodDecorModels.Anim(time, 0.0F, true, this.kind == BloodDecorKind.GRANDFATHER_CLOCK ? 4.4F : 0.0F,
            this.kind == BloodDecorKind.GRANDFATHER_CLOCK ? 1.1F : 0.0F, 0.0F);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        float time = (float) (net.minecraft.util.Util.getMillis() % 3_600_000L) / 50.0F;
        BloodDecorModels.Anim anim = this.anim(time);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(this.fit, this.fit, this.fit);
        poseStack.translate(-this.middle.x(), -this.middle.y(), -this.middle.z());
        turn(poseStack, this.kind);
        collector.submitModel(this.model, anim, poseStack, this.texture, lightCoords, overlayCoords, outlineColor);
        if (this.kind.glows) collector.submitModel(this.model, anim, poseStack, RenderTypes.eyes(this.glow), lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }

    // the box it fills in the item's block (the GUI sizes the slot's drawing from it)
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int i = 0; i < 8; i++) output.accept(new Vector3f((i & 1) == 0 ? 0.0F : 1.0F, (i & 2) == 0 ? 0.0F : 1.0F, (i & 4) == 0 ? 0.0F : 1.0F));
    }

    public record Unbaked(String decor) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("decor").forGetter(Unbaked::decor)).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @Nullable BloodDecorItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            for (BloodDecorKind kind : BloodDecorKind.values()) {
                if (!kind.id.equals(this.decor)) continue;
                BloodDecorRenderer.Def def = BloodDecorRenderer.DEFS.get(kind);
                if (def == null) return null;
                return new BloodDecorItemRenderer(kind, def.model().apply(context.entityModelSet().bakeLayer(def.layer())));
            }
            return null;
        }
    }

    @SubscribeEvent
    public static void onRegister(RegisterSpecialModelRendererEvent event) {
        event.register(TYPE, Unbaked.MAP_CODEC);
    }
}

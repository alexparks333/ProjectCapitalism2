package com.alexh.capitalism.mixin;

import com.alexh.capitalism.client.BloodMoonClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A Blood Moon (client/BloodMoonClient, event/BloodMoon): the moon is full, bigger and blood red; the night sky turns a
// deep red and the stars fade. Eased in and out with BloodMoonClient.strength().
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void capitalism$bloodSky(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
        float s = BloodMoonClient.strength();
        if (s <= 0.0F || state.skybox != DimensionType.Skybox.OVERWORLD || state.skyColor == null) return;
        Vector3f sky = new Vector3f(state.skyColor);
        state.skyColor = sky.lerp(new Vector3f(0.32F, 0.02F, 0.03F), s * 0.85F);
        state.moonPhase = MoonPhase.FULL_MOON;
        state.starBrightness *= 1.0F - 0.7F * s;
    }

    // the moon's colour: red
    @ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"), index = 1)
    private Vector4f capitalism$redMoon(Vector4f color) {
        float s = BloodMoonClient.strength();
        if (s <= 0.0F) return color;
        return new Vector4f(color.x(), color.y() * (1.0F - 0.78F * s), color.z() * (1.0F - 0.85F * s), color.w());
    }

    // and its size: bigger
    @ModifyArg(method = "renderMoon", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"), index = 2)
    private float capitalism$bigMoon(float scale) {
        return scale * (1.0F + 0.7F * BloodMoonClient.strength());
    }
}

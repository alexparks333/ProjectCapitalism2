package com.alexh.capitalism.mixin;

import com.alexh.capitalism.client.WinterWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Winter (Calendar.md): it's winter everywhere, so whatever falls from the sky is snow, in every biome (deserts too).
// Client only (the mixin's in the "client" list) and only on the render thread (in single player the server shares the
// game, and its rain stays rain: no snow piling up, fire still put out), only while client/WinterWeather says it's
// winter in the overworld.
@Mixin(Biome.class)
public abstract class BiomeWinterMixin {

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void capitalism$winterSnow(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (WinterWeather.snowNow()) cir.setReturnValue(Biome.Precipitation.SNOW);
    }
}

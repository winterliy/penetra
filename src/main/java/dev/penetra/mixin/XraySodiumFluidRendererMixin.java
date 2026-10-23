package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public abstract class XraySodiumFluidRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void penetra$hideNonTargetFluids(@Coerce Object level, BlockState state, FluidState fluidState,
            BlockPos pos, BlockPos offset, @Coerce Object collector, @Coerce Object builder,
            @Coerce Object material, @Coerce Object colorProvider, FluidModel model, CallbackInfo ci) {
        if (PenetraFeatures.xrayEnabled() && !PenetraFeatures.isXrayTarget(state)) ci.cancel();
    }
}

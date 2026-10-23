package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class XrayFluidRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void penetra$hideNonTargetFluids(BlockAndTintGetter level, BlockPos pos,
            FluidRenderer.Output output, BlockState state, FluidState fluid, CallbackInfo ci) {
        if (PenetraFeatures.xrayEnabled() && !PenetraFeatures.isXrayTarget(state)) ci.cancel();
    }
}

package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
public abstract class XrayModelBlockRendererMixin {
    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void penetra$hideNonTargets(BlockQuadOutput output, float x, float y, float z,
            BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model,
            long randomSeed, CallbackInfo ci) {
        if (PenetraFeatures.xrayEnabled() && !PenetraFeatures.isXrayTarget(state)) ci.cancel();
    }

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private void penetra$drawTargetsThroughWalls(BlockAndTintGetter level, BlockState state,
            Direction direction, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (PenetraFeatures.xrayEnabled() && PenetraFeatures.isXrayTarget(state)) cir.setReturnValue(true);
    }
}

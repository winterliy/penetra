package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class XraySodiumBlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, require = 0)
    private void penetra$hideNonTargetBlocks(BlockStateModel model, BlockState state, BlockPos pos,
            BlockPos modelOffset, CallbackInfo ci) {
        if (PenetraFeatures.xrayEnabled() && !PenetraFeatures.isXrayTarget(state)) ci.cancel();
    }
}

package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public abstract class XraySodiumBlockRenderContextMixin {
    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos;

    @Inject(method = "shouldDrawSide(Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void penetra$renderTargetFacesThroughBlocks(Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (PenetraFeatures.xrayEnabled() && PenetraFeatures.isXrayTarget(state)) {
            cir.setReturnValue(true);
        }
    }
}

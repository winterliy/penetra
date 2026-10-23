package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XrayBlockStateMixin {
    @Inject(method = "getShadeBrightness", at = @At("RETURN"), cancellable = true)
    private void penetra$brightenXrayTargets(BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<Float> cir) {
        if (PenetraFeatures.xrayEnabled()) cir.setReturnValue(1.0F);
    }
}

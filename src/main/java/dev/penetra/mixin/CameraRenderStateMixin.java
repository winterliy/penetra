package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import dev.penetra.PenetraZoom;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.Camera.class)
public abstract class CameraRenderStateMixin {
    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void penetra$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(PenetraZoom.applyToFov(cir.getReturnValue()));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void penetra$offsetFreecam(CameraRenderState state, net.minecraft.client.DeltaTracker deltaTracker,
            CallbackInfo ci) {
        if (!PenetraFeatures.freecamEnabled()) return;
        Vec3 offset = PenetraFeatures.freecamOffset();
        state.pos = state.pos.add(offset);
        state.blockPos = BlockPos.containing(state.pos);
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("RETURN"), cancellable = true)
    private void penetra$extendCameraDistance(float desiredDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (PenetraFeatures.cameraDistanceEnabled()) cir.setReturnValue(Math.max(cir.getReturnValue(), 24.0F));
    }
}

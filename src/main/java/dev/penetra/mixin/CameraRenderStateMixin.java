package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import dev.penetra.PenetraZoom;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.Camera.class)
public abstract class CameraRenderStateMixin {
    @Shadow private boolean detached;

    @Shadow protected abstract void setPosition(Vec3 pos);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER))
    private void penetra$moveFreecam(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!PenetraFeatures.freecamEnabled()) return;
        detached = true;
        setPosition(PenetraFeatures.freecamPosition(deltaTracker.getGameTimeDeltaPartialTick(true)));
        setRotation(PenetraFeatures.freecamYaw(), PenetraFeatures.freecamPitch());
    }

    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void penetra$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(PenetraZoom.applyToFov(cir.getReturnValue()));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void penetra$offsetFreecam(CameraRenderState state, net.minecraft.client.DeltaTracker deltaTracker,
            CallbackInfo ci) {
        if (PenetraFeatures.freecamEnabled()) state.smartCull = false;
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("RETURN"), cancellable = true)
    private void penetra$extendCameraDistance(float desiredDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (PenetraFeatures.cameraDistanceEnabled()) cir.setReturnValue(Math.max(cir.getReturnValue(), 24.0F));
    }
}

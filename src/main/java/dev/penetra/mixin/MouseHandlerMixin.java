package dev.penetra.mixin;

import dev.penetra.PenetraZoom;
import dev.penetra.PenetraFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer(D)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void penetra$turnFreecam(LocalPlayer player, double deltaYaw, double deltaPitch) {
        if (PenetraFeatures.freecamEnabled() && !PenetraFeatures.guiOpen()) {
            PenetraFeatures.turnFreecam(deltaYaw, deltaPitch);
        } else {
            player.turn(deltaYaw, deltaPitch);
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void penetra$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical == 0.0 || Minecraft.getInstance().gui.screen() != null) return;
        if (PenetraFeatures.cameraDistanceEnabled()) {
            PenetraFeatures.adjustCameraDistance(vertical);
            ci.cancel();
        } else if (PenetraZoom.isZooming()) {
            PenetraZoom.adjustFromScroll(vertical);
            ci.cancel();
        } else if (PenetraFeatures.freecamControlsScroll()) {
            PenetraFeatures.adjustFreecamSpeed(vertical);
            ci.cancel();
        }
    }
}

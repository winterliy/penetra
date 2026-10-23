package dev.penetra.mixin;

import dev.penetra.PenetraZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void penetra$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical == 0.0 || !PenetraZoom.isZooming() || Minecraft.getInstance().gui.screen() != null) return;
        PenetraZoom.adjustFromScroll(vertical);
        ci.cancel();
    }
}

package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true)
    private void penetra$handleHotkeys(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (PenetraFeatures.consumeFreecamKey(event.key(), action)) {
            ci.cancel();
            return;
        }
        if (action == 1 && !PenetraFeatures.guiOpen()) PenetraFeatures.toggle(event.key());
    }
}

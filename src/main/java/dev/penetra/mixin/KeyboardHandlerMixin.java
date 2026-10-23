package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import dev.penetra.PenetraHotkeys;
import dev.penetra.PenetraZoom;
import com.mojang.blaze3d.platform.InputConstants;
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
        // Minecraft 26.3 switched to SDL: key() is the physical SDL scancode,
        // while keycode() is the layout-dependent character keycode.
        int keyCode = event.key();
        PenetraZoom.handleKey(keyCode, action);
        if (PenetraFeatures.consumeFreecamKey(keyCode, action)) {
            ci.cancel();
            return;
        }
        if (action == InputConstants.PRESS && Minecraft.getInstance().gui.screen() == null) {
            PenetraHotkeys.onKeyPressed(keyCode);
        }
    }
}

package dev.penetra.mixin;

import dev.penetra.PenetraZoom;
import dev.penetra.PenetraFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void penetra$trackGui(Screen screen, CallbackInfo ci) {
        PenetraFeatures.setGuiOpen(screen != null);
        if (screen != null) PenetraZoom.releaseKey();
    }
}

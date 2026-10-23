package dev.penetra.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import dev.penetra.PenetraZoom;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void penetra$registerZoomKey(CallbackInfo ci) {
        KeyMapping zoomKey = PenetraZoom.getOrCreateKeyMapping();
        this.keyMappings = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 1);
        this.keyMappings[this.keyMappings.length - 1] = zoomKey;
    }
}

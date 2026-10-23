package dev.penetra.mixin;

import dev.penetra.PenetraFeatures;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VisGraph.class)
public abstract class XrayVisGraphMixin {
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private void penetra$disableSectionOcclusion(CallbackInfoReturnable<VisibilitySet> cir) {
        if (!PenetraFeatures.xrayEnabled()) return;
        VisibilitySet visible = new VisibilitySet();
        visible.setAll(true);
        cir.setReturnValue(visible);
    }
}

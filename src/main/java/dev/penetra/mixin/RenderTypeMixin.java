package dev.penetra.mixin;

import dev.penetra.accessor.IRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin implements IRenderType {
    @Shadow private static RenderType create(String name, RenderSetup setup) { return null; }

    @Override public RenderType penetra$createRenderType(String name, RenderSetup setup) {
        return create(name, setup);
    }
}

package dev.penetra.accessor;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface IRenderType {
    RenderType penetra$createRenderType(String name, RenderSetup setup);
}

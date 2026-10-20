package dev.penetra.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.penetra.accessor.IRenderPipelines;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderPipelines.class)
public abstract class RenderPipelinesMixin implements IRenderPipelines {
    @Shadow @Final private static RenderPipeline.Snippet LINES_SNIPPET;
    @Shadow private static RenderPipeline register(RenderPipeline pipeline) { return null; }

    @Override public RenderPipeline.Snippet penetra$getLinesSnippet() { return LINES_SNIPPET; }
    @Override public RenderPipeline penetra$registerPipeline(RenderPipeline pipeline) { return register(pipeline); }
}

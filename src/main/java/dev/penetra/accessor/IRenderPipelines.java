package dev.penetra.accessor;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

public interface IRenderPipelines {
    RenderPipeline.Snippet penetra$getLinesSnippet();
    RenderPipeline penetra$registerPipeline(RenderPipeline pipeline);
}

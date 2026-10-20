package dev.penetra.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.penetra.PenetraRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void penetra$renderOutlines(GraphicsResourceAllocator resourceAllocator, boolean renderOutline,
            CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor,
            boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci) {
        PenetraRenderer.render(cameraState);
    }
}

package dev.penetra;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.penetra.accessor.IRenderPipelines;
import dev.penetra.accessor.IRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.OptionalDouble;

public final class PenetraRenderer {
    private static final StagedVertexBuffer VERTICES = new StagedVertexBuffer(() -> "Penetra outlines", 256);
    private static final RenderType XRAY_LINES = createXrayLines();

    private PenetraRenderer() {}

    public static void initialize() {
        // Force registration before the renderer creates its shader pipelines.
        XRAY_LINES.pipeline();
    }

    private static RenderType createXrayLines() {
        IRenderPipelines access = (IRenderPipelines) (Object) new RenderPipelines();
        RenderPipeline pipeline = RenderPipeline.builder(access.penetra$getLinesSnippet())
                .withLocation("pipelines/penetra_xray_lines")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .build();
        access.penetra$registerPipeline(pipeline);
        IRenderType types = (IRenderType) (Object) RenderTypes.lines();
        return types.penetra$createRenderType("penetra_xray_lines",
                RenderSetup.builder(pipeline).createRenderSetup());
    }

    public static void render(CameraRenderState cameraState) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !FindState.belongsTo(minecraft.level) || FindState.positions().isEmpty()) return;

        int rgb = FindState.rgb();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        PoseStack pose = new PoseStack();
        pose.mulPose(cameraState.viewRotationMatrix);
        StagedVertexBuffer.Draw draw = VERTICES.appendDraw(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
                PrimitiveTopology.LINES);
        BufferBuilder vertices = (BufferBuilder) VERTICES.getVertexBuilder(draw);

        double cameraX = cameraState.pos.x;
        double cameraY = cameraState.pos.y;
        double cameraZ = cameraState.pos.z;
        int centerX = minecraft.player.blockPosition().getX() >> 4;
        int centerZ = minecraft.player.blockPosition().getZ() >> 4;
        int renderDistance = minecraft.options.renderDistance().get();
        ClientChunkCache chunks = minecraft.level.getChunkSource();
        for (var block : FindState.positions()) {
            int chunkX = block.getX() >> 4;
            int chunkZ = block.getZ() >> 4;
            if (Math.abs(chunkX - centerX) > renderDistance || Math.abs(chunkZ - centerZ) > renderDistance
                    || chunks.getChunk(chunkX, chunkZ, null, false) == null) continue;
            AABB box = new AABB(block).inflate(0.002);
            emitBox(vertices, pose, box, cameraX, cameraY, cameraZ, red, green, blue);
        }

        VERTICES.upload();
        StagedVertexBuffer.ExecuteInfo info = VERTICES.getExecuteInfo(draw);
        if (info != null) {
            var prepared = XRAY_LINES.prepare();
            var target = minecraft.gameRenderer.mainRenderTarget();
            try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Penetra X-ray outlines", target.getColorTextureView(), Optional.empty(),
                    target.getDepthTextureView(), OptionalDouble.empty())) {
                prepared.drawFromBuffer(info, pass);
            }
        }
        VERTICES.endFrame();
    }

    private static void emitBox(BufferBuilder out, PoseStack pose, AABB box,
            double cameraX, double cameraY, double cameraZ, float red, float green, float blue) {
        double x0 = box.minX - cameraX, x1 = box.maxX - cameraX;
        double y0 = box.minY - cameraY, y1 = box.maxY - cameraY;
        double z0 = box.minZ - cameraZ, z1 = box.maxZ - cameraZ;
        line(out, pose, x0, y0, z0, x1, y0, z0, 1, 0, 0, red, green, blue);
        line(out, pose, x1, y0, z0, x1, y0, z1, 0, 0, 1, red, green, blue);
        line(out, pose, x1, y0, z1, x0, y0, z1, -1, 0, 0, red, green, blue);
        line(out, pose, x0, y0, z1, x0, y0, z0, 0, 0, -1, red, green, blue);
        line(out, pose, x0, y1, z0, x1, y1, z0, 1, 0, 0, red, green, blue);
        line(out, pose, x1, y1, z0, x1, y1, z1, 0, 0, 1, red, green, blue);
        line(out, pose, x1, y1, z1, x0, y1, z1, -1, 0, 0, red, green, blue);
        line(out, pose, x0, y1, z1, x0, y1, z0, 0, 0, -1, red, green, blue);
        line(out, pose, x0, y0, z0, x0, y1, z0, 0, 1, 0, red, green, blue);
        line(out, pose, x1, y0, z0, x1, y1, z0, 0, 1, 0, red, green, blue);
        line(out, pose, x1, y0, z1, x1, y1, z1, 0, 1, 0, red, green, blue);
        line(out, pose, x0, y0, z1, x0, y1, z1, 0, 1, 0, red, green, blue);
    }

    private static void line(BufferBuilder out, PoseStack pose,
            double x0, double y0, double z0, double x1, double y1, double z1,
            float nx, float ny, float nz, float red, float green, float blue) {
        var matrix = pose.last();
        out.addVertex(matrix, (float) x0, (float) y0, (float) z0)
                .setColor(red, green, blue, 1.0F).setNormal(matrix, nx, ny, nz).setLineWidth(2.0F);
        out.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                .setColor(red, green, blue, 1.0F).setNormal(matrix, nx, ny, nz).setLineWidth(2.0F);
    }
}

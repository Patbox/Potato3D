package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SoftRenderPipeline(String name, List<CreateInfo.Shader> shaders, List<CreateInfo.VertexBuffer> vertexBuffers,
                                 List<CreateInfo.AttribBinding> attribBindings, List<BindGroupLayout.UniformDescription> uniforms,
                                 int pushConstantsSize, @Nullable DepthStencilState depthStencilState,
                                 PolygonMode polygonMode, boolean cull, List<@Nullable ColorTargetState> colorTargetStates,
                                 PrimitiveTopology primitiveTopology, RenderPipeline renderPipeline) implements BackendRenderPipeline {
    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {

    }
}

package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record SoftRenderPipeline(String name, List<CreateInfo.Shader> shaders, int vertexLength,
                                 Object2IntMap<String> elementPosition,
                                 List<BindGroupLayout.UniformDescription> uniforms,
                                 Map<String, BindGroupLayout.UniformDescription> uniformByName,
                                 int pushConstantsSize, @Nullable DepthStencilState depthStencilState,
                                 PolygonMode polygonMode, boolean cull,
                                 List<@Nullable ColorTargetState> colorTargetStates,
                                 PrimitiveTopology primitiveTopology,
                                 RenderPipeline renderPipeline) implements BackendRenderPipeline {
    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {

    }
}

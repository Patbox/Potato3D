package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import org.jspecify.annotations.Nullable;

public record SoftCompiledRenderPipeline(RenderPipeline info) implements CompiledRenderPipeline {
    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {

    }
}

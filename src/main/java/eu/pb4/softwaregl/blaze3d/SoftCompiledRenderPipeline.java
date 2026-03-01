package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import org.jspecify.annotations.Nullable;

public class SoftCompiledRenderPipeline implements CompiledRenderPipeline {
    public SoftCompiledRenderPipeline(RenderPipeline pipeline, @Nullable ShaderSource shaderSource) {
    }

    @Override
    public boolean isValid() {
        return true;
    }
}

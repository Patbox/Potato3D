package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlSurface;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.*;
import eu.pb4.potato3d.Potato3D;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

public class SoftDevice implements GpuDeviceBackend {
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo("Software Renderer", "Tiny Potato",
            Potato3D.MOD_VERSION,
            SoftRenderPass.USE_ZERO_TO_ONE_Z, Potato3D.MOD_NAME, 0.1f,
            new DeviceLimits(1, 1, Short.MAX_VALUE), Set.of(),
            new HintsAndWorkarounds(false, false),
            DeviceType.CPU
    );
    private final long window;
    private final ShaderSource shaderSource;
    private final GpuDebugOptions gpuDebugOptions;
    private final int textureSize;

    public SoftDevice(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions) {
        this.window = windowHandle;
        this.shaderSource = defaultShaderSource;
        this.gpuDebugOptions = debugOptions;
        this.textureSize = Short.MAX_VALUE;
        GLFW.glfwMakeContextCurrent(windowHandle);
        GLFW.glfwSetWindowSizeLimits(windowHandle, -1, -1, this.textureSize, this.textureSize);
        GL.createCapabilities();
    }

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        return new GlSurface(windowHandle) {
            public void blitFromTexture(final CommandEncoderBackend commandEncoder, final GpuTextureView textureView) {
                ((SoftCommandEncoder)commandEncoder).presentTexture(textureView);
            }
        };
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        return new SoftCommandEncoder();
    }

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new SoftSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return this.createTexture(label != null ? label.get() : null, usage, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new SoftTexture(usage, label, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return createTextureView(texture, 0, 0);
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        return new SoftTextureView(texture, baseMipLevel, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
        return new SoftBuffer(label != null ? label.get() : null, usage, size);
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
        return new SoftBuffer(label != null ? label.get() : null, usage, data);
    }

    @Override
    public List<String> getLastDebugMessages() {
        return List.of();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource shaderSource) {
        return new SoftCompiledRenderPipeline(pipeline, shaderSource);
    }

    @Override
    public void clearPipelineCache() {

    }

    @Override
    public void close() {

    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int size) {
        return new GpuQueryPool() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public OptionalLong getValue(int index) {
                return OptionalLong.empty();
            }

            @Override
            public OptionalLong[] getValues(int index, int count) {
                var t = new OptionalLong[count];
                Arrays.fill(t, OptionalLong.empty());
                return t;
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public long getTimestampNow() {
        return 0;
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return DEVICE_INFO;
    }
}

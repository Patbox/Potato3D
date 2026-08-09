package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.*;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.backend.opengl.GlSurface;
import eu.pb4.potato3d.Potato3D;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Surface;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

public class SoftDevice implements GpuDeviceBackend {
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo("Software Renderer", "Tiny Potato",
            Potato3D.MOD_VERSION,
            SoftRenderPass.USE_ZERO_TO_ONE_Z, Potato3D.MOD_NAME, 0.1f,
            new DeviceLimits(1, 1, Short.MAX_VALUE, Integer.MAX_VALUE, 1, 1),
            new DeviceFeatures(false, false, false, false, false, false, true), Set.of(),
            new HintsAndWorkarounds(false, false),
            DeviceType.CPU
    );
    protected final long window;
    private final GpuDebugOptions gpuDebugOptions;
    private final int textureSize;

    public SoftDevice(long windowHandle, GpuDebugOptions debugOptions) {
        this.window = windowHandle;
        this.gpuDebugOptions = debugOptions;
        this.textureSize = Short.MAX_VALUE;

        SDLVideo.SDL_GL_CreateContext(window);
        GL.createCapabilities();
    }

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle) {
        return new GlSurface(windowHandle) {
            public void blitFromTexture(final CommandEncoderBackend commandEncoder, final GpuTextureView textureView) {
                ((SoftCommandEncoder) commandEncoder).presentTexture(textureView);
            }
        };
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        return new SoftCommandEncoder(this);
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
    public @Nullable CompiledRenderPipeline compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource) {
        return new SoftCompiledRenderPipeline(pipeline);
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

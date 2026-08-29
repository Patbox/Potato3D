package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.*;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import eu.pb4.potato3d.Potato3D;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SoftDevice implements GpuDeviceBackend {
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo("Software Renderer", "Tiny Potato",
            Potato3D.MOD_VERSION,
            SoftRenderPass.USE_ZERO_TO_ONE_Z, Potato3D.MOD_NAME, 0.1f,
            new DeviceLimits(1, 1, Short.MAX_VALUE, Integer.MAX_VALUE, 1, 1, 1),
            new DeviceFeatures(false, false, false, false, false, false, false, true), Set.of(),
            new HintsAndWorkarounds(false, false, false, false),
            DeviceType.CPU
    );
    private final GpuDebugOptions gpuDebugOptions;
    private final int textureSize;

    public SoftDevice(GpuDebugOptions debugOptions) {
        this.gpuDebugOptions = debugOptions;
        this.textureSize = Short.MAX_VALUE;
    }

    @Override
    public GpuSurfaceBackend createSurface(long windowHandle, BooleanSupplier isIconified) {
        var render = SDLRender.SDL_CreateRenderer(windowHandle, SDLRender.SDL_GetRenderDriver(0));
        if (render == 0) {
            throw new IllegalStateException(SDLError.SDL_GetError());
        }

        return new GpuSurfaceBackend() {
            private ByteBuffer out;
            private final SDL_Rect rect = SDL_Rect.create();
            private @Nullable SDL_Texture texture;
            private int swapchainHeight;
            private int swapchainWidth;

            @Override
            public void configure(GpuSurface.Configuration config) throws SurfaceException {
                this.swapchainWidth = config.width();
                this.swapchainHeight = config.height();
                if (this.texture != null) {
                    SDLRender.SDL_DestroyTexture(this.texture);
                }

                this.texture = SDLRender.SDL_CreateTexture(render,
                        SDLPixels.SDL_PIXELFORMAT_RGBA8888,
                        SDLRender.SDL_TEXTUREACCESS_STREAMING, config.width(), config.height());

                this.out = ByteBuffer.allocateDirect(config.width() * config.height() * 4);
                this.rect.set(0, 0, config.width(), config.height());
                SDLRender.SDL_SetRenderViewport(render, this.rect);
            }

            @Override
            public boolean isSuboptimal() {
                return false;
            }

            @Override
            public void acquireNextTexture() throws SurfaceException {
                if (isIconified.getAsBoolean()) {
                    //throw new SurfaceException("Cannot acquire minimized window");
                }
            }

            @Override
            public void blitFromTexture(CommandEncoderBackend commandEncoder, GpuTextureView texture) {
                var outWidth = Potato3D.framebufferWidth;
                var outHeight = Potato3D.framebufferHeight;
                var width = texture.getWidth(0);
                var height = texture.getHeight(0);
                var txt = (SoftTexture) texture.texture();


                var sX = Math.min(outWidth / width, outHeight / height);
                var offsetBaseX = (outWidth - width * sX) / 2;
                var offsetBaseY = (outHeight - height * sX) / 2;
                for (int y = 0; y < height; y++) {
                    var yOff = y * width;
                    var ys = (y * sX + offsetBaseY) * outWidth + offsetBaseX;
                    for (int x = 0; x < width; x++) {
                        var xs = x * sX;
                        var color = Integer.reverseBytes(txt.rgba[0].data()[x + yOff] | 0xFF);

                        for (var xa = 0; xa < sX; xa++) {
                            for (var ya = 0; ya < sX; ya++) {
                                out.putInt((xs + xa + ys + ya * outWidth) * 4, color);
                            }
                        }
                    }
                }


                SDLRender.SDL_UpdateTexture(this.texture, null, this.out, outWidth * 4);
            }

            @Override
            public void present() {
                SDLRender.SDL_RenderClear(render);
                if (this.texture != null) {
                    SDLRender.SDL_RenderTextureRotated(render, this.texture, null, null, 0, null, SDLSurface.SDL_FLIP_VERTICAL);
                }
                SDLRender.SDL_RenderPresent(render);
            }

            @Override
            public Collection<GpuSurface.PresentMode> supportedPresentModes() {
                return List.of(GpuSurface.PresentMode.IMMEDIATE, GpuSurface.PresentMode.FIFO);
            }

            @Override
            public void close() {
                SDLRender.SDL_DestroyTexture(this.texture);
                SDLRender.SDL_DestroyRenderer(render);
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
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return new SoftTexture(usage, label, format, width, height, depthOrLayers, mipLevels);
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
    public BackendRenderPipeline.Pending compilePipeline(BackendRenderPipeline.CreateInfo pipelineCreateInfo) {
        return new BackendRenderPipeline.Pending() {
            @Override
            public @Nullable BackendRenderPipeline finishCompile() {
                try {
                    if (RenderPipelines.optionalPipelines().stream().anyMatch(x -> x.getLocation().toString().equals(pipelineCreateInfo.name()))) {
                        return null;
                    }


                    return new SoftRenderPipeline(pipelineCreateInfo.name(), pipelineCreateInfo.shaders(), pipelineCreateInfo.vertexBuffers(), pipelineCreateInfo.attribBindings(),
                            pipelineCreateInfo.uniforms(), pipelineCreateInfo.pushConstantsSize(), pipelineCreateInfo.depthStencilState(), pipelineCreateInfo.polygonMode(), pipelineCreateInfo.cull(),
                            pipelineCreateInfo.colorTargetStates(), pipelineCreateInfo.primitiveTopology(),
                            RenderPipelines.requiredPipelines().stream().filter(x -> x.getLocation().toString().equals(pipelineCreateInfo.name())).findAny().orElse(RenderPipelines.VIGNETTE)
                    );
                } catch (Throwable e) {
                    return null;
                }
            }
        };
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
    public long getTimestampCalibrationOffset() {
        return 0;
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return DEVICE_INFO;
    }
}

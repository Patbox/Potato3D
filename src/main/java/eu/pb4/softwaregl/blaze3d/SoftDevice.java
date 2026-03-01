package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.*;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

public class SoftDevice implements GpuDeviceBackend {
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
    public CommandEncoderBackend createCommandEncoder() {
        return new SoftCommandEncoder();
    }

    @Override
    public GpuSampler createSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        return new SoftSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> label, @GpuTexture.Usage int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        return this.createTexture(label != null ? label.get() : null, usage, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
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
    public String getImplementationInformation() {
        return "SoftwareGL";
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
    public String getVendor() {
        return "Taterion";
    }

    @Override
    public String getBackendName() {
        return "SoftwareGL";
    }

    @Override
    public String getVersion() {
        return "0";
    }

    @Override
    public String getRenderer() {
        return "Potato";
    }

    @Override
    public int getMaxTextureSize() {
        return this.textureSize;
    }

    @Override
    public int getUniformOffsetAlignment() {
        return 1;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSource shaderSource) {
        return new SoftCompiledRenderPipeline(pipeline, shaderSource);
    }

    @Override
    public void clearPipelineCache() {

    }

    @Override
    public List<String> getEnabledExtensions() {
        return List.of();
    }

    @Override
    public int getMaxSupportedAnisotropy() {
        return 1;
    }

    @Override
    public void close() {

    }

    @Override
    public void setVsync(boolean enabled) {
        GLFW.glfwSwapInterval(enabled ? 1 : 0);
    }

    @Override
    public void presentFrame() {
        GLFW.glfwSwapBuffers(this.window);
    }

    @Override
    public boolean isZZeroToOne() {
        return false;
    }
}

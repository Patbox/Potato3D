package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.logging.LogUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public class SoftBackend implements GpuBackend {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String getName() {
        return "SoftwareGL";
    }

    @Override
    public void setWindowHints() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    }

    @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {

    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions) {
        return new GpuDevice(new SoftDevice(window, defaultShaderSource, debugOptions));
    }
}

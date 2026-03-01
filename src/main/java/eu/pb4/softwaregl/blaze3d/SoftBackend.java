package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.WindowAndDevice;
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
    public WindowAndDevice createDeviceWithWindow(int width, int height, String title, long monitor, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions) throws BackendCreationException {
        GLFWErrorCapture glfwErrors = new GLFWErrorCapture();

        WindowAndDevice result;
        try (GLFWErrorScope var10 = new GLFWErrorScope(glfwErrors)) {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

            long window = GLFW.glfwCreateWindow(width, height, title, monitor, 0L);
            if (window == 0L) {
                GLFWErrorCapture.Error error = glfwErrors.firstError();
                if (error != null) {
                    if (error.error() == 65542) {
                        throw new BackendCreationException("Driver does not support OpenGL");
                    }

                    if (error.error() == 65543) {
                        throw new BackendCreationException("Driver does not support OpenGL 3.3");
                    }

                    throw new BackendCreationException(error.toString());
                }

                throw new BackendCreationException("Failed to create window with OpenGL context");
            }

            result = new WindowAndDevice(window, new GpuDevice(new SoftDevice(window, defaultShaderSource, debugOptions)));
        }

        for(GLFWErrorCapture.Error error : glfwErrors) {
            LOGGER.error("GLFW error collected during GL backend initialization: {}", error);
        }

        return result;
    }
}

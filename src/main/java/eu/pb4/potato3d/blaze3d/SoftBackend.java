package eu.pb4.potato3d.blaze3d;


import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SoftBackend implements GpuBackend {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String getName() {
        return "SoftwareGL";
    }

    @Override
    public void loadLibrary() throws BackendCreationException {

    }

    @Override
    public void unloadLibrary() {

    }

    @Override
    public void setWindowHints() {

    }

    @Override
    public long getWindowFlags() {
        return 2;
    }

    @Override
    public void handleWindowCreationErrors(@Nullable String error) throws BackendCreationException {

    }

    @Override
    public GpuDevice createDevice(long window, GpuDebugOptions debugOptions) throws BackendCreationException {
        return new FrontendGpuDevice(new SoftDevice(window, debugOptions));
    }
}

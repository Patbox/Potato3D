package eu.pb4.potato3d;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector2i;
import org.slf4j.Logger;


public class Potato3D implements ModInitializer {
    public static final String MOD_ID = "potato3d";
    public static final String MOD_VERSION;
    public static final String MOD_NAME;
    public static final boolean MODIFY_CLIENT_BEHAVIOUR = true;

    public static final Logger LOGGER = LogUtils.getLogger();
    public static int framebufferWidth;
    public static int framebufferHeight;

    static {
        var c = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        MOD_VERSION = c.getMetadata().getVersion().getFriendlyString();
        MOD_NAME = c.getMetadata().getName();
    }

    public static Vector2i updateRealResolutionAndScaleDown(int framebufferWidth, int framebufferHeight) {
        if (framebufferWidth == 0 || framebufferHeight == 0) return new Vector2i(framebufferWidth, framebufferHeight);

        Potato3D.framebufferWidth = framebufferWidth;
        Potato3D.framebufferHeight = framebufferHeight;

        if (!MODIFY_CLIENT_BEHAVIOUR) return new Vector2i(framebufferWidth, framebufferHeight);

        return new Vector2i(Math.min(320, framebufferWidth), Math.min(240, framebufferHeight));
        //return new Vector2i(Math.min(280, framebufferWidth), Math.min(210, framebufferHeight));
    }

    public static double remapMouseY(Window window, double x) {
        var outWidth = Potato3D.framebufferWidth;
        var outHeight = Potato3D.framebufferHeight;
        var width = window.getWidth();
        var height = window.getHeight();

        var scale = Math.min(outWidth / width, outHeight / height);
        var offsetBase = (outHeight - height * scale) / 2;
        return ((x - offsetBase) / (double) (window.getScreenHeight() - offsetBase * 2)) * ((double) window.getGuiScaledHeight());
    }

    public static double remapMouseX(Window window, double x) {
        var outWidth = Potato3D.framebufferWidth;
        var outHeight = Potato3D.framebufferHeight;
        var width = window.getWidth();
        var height = window.getHeight();

        var scale = Math.min(outWidth / width, outHeight / height);

        var offsetBase = (outWidth - width * scale) / 2;
        return ((x - offsetBase) / (double) (window.getScreenWidth() - offsetBase * 2)) * ((double) window.getGuiScaledWidth());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("The ~~Future~~ Past of Rendering is Now, and it's all in Java™!");
    }
}
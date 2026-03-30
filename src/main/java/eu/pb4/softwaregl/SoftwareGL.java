package eu.pb4.softwaregl;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;


public class SoftwareGL implements ModInitializer {
    public static final String MOD_ID = "softwaregl";
    public static final String MOD_VERSION;
    public static final String MOD_NAME;

    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        var c = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        MOD_VERSION = c.getMetadata().getVersion().getFriendlyString();
        MOD_NAME = c.getMetadata().getName();
    }

    @Override
    public void onInitialize() {

    }
}
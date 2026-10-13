package matybachy.argentumforge;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public final class ArgentumConfig {
    private static final String CAT_RENDERER = "renderer";
    private static final String CAT_FEATURES = "features";
    private static final String CAT_FALLBACKS = "fallbacks";

    public static boolean enableExperimentalRenderer = false;
    public static int renderChunkRadius = 3;
    public static int rebuildBudgetPerTick = 2;
    public static boolean enableLightingAo = true;
    public static boolean enableTransparency = true;
    public static boolean enableModelFallback = true;
    public static boolean enableOptionalEffects = false;
    public static boolean enableEntityHooks = false;
    public static boolean enableWeatherHooks = false;
    public static boolean enableCloudHooks = false;

    private ArgentumConfig() {
    }

    public static void load(FMLPreInitializationEvent event) {
        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
        try {
            cfg.load();
            enableExperimentalRenderer = cfg.getBoolean("enableExperimentalRenderer", CAT_RENDERER, false, "Enable experimental Argentum terrain renderer.");
            renderChunkRadius = cfg.getInt("renderChunkRadius", CAT_RENDERER, 3, 1, 8, "Chunk radius around camera to keep meshed.");
            rebuildBudgetPerTick = cfg.getInt("rebuildBudgetPerTick", CAT_RENDERER, 2, 1, 64, "Maximum chunk rebuilds per client tick.");
            enableLightingAo = cfg.getBoolean("enableLightingAo", CAT_FEATURES, true, "Enable world-light based brightness and lightweight ambient occlusion.");
            enableTransparency = cfg.getBoolean("enableTransparency", CAT_FEATURES, true, "Enable cutout/translucent split and fluid translucency.");
            enableOptionalEffects = cfg.getBoolean("enableOptionalEffects", CAT_FEATURES, false, "Enable optional post/effect pipeline when supported.");
            enableEntityHooks = cfg.getBoolean("enableEntityHooks", CAT_FEATURES, false, "Enable optional entity/item hooks with vanilla fallback.");
            enableWeatherHooks = cfg.getBoolean("enableWeatherHooks", CAT_FEATURES, false, "Enable optional rain/snow hooks with vanilla fallback.");
            enableCloudHooks = cfg.getBoolean("enableCloudHooks", CAT_FEATURES, false, "Enable optional cloud hooks with vanilla fallback.");
            enableModelFallback = cfg.getBoolean("enableModelFallback", CAT_FALLBACKS, true, "Enable conservative block-bounds fallback when model data is missing.");
        } finally {
            if (cfg.hasChanged()) cfg.save();
        }
    }
}

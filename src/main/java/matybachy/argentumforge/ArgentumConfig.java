package matybachy.argentumforge;

import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
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
    public static boolean disableOptionalEffectsWithFeatherOpt = true;
    public static boolean enableEntityHooks = false;
    public static boolean enableEntityDistanceCulling = false;
    public static int entityCullingDistance = 96;
    public static boolean enableItemHooks = false;
    public static boolean enableParticleHooks = false;
    public static int particleThrottleSetting = 1;
    public static boolean enableWeatherHooks = false;
    public static int weatherRenderRadiusBlocks = 128;
    public static int weatherFrustumMinDotPercent = -35;
    public static boolean enableCloudHooks = false;
    public static boolean enableCloudRenderer = false;
    public static int cloudRenderRadiusBlocks = 128;
    public static boolean enableBlockEntityHooks = false;
    public static int optionalEffectsCompositeAlpha = 100;
    public static int optionalEffectsTintRed = 100;
    public static int optionalEffectsTintGreen = 100;
    public static int optionalEffectsTintBlue = 100;

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
            disableOptionalEffectsWithFeatherOpt = cfg.getBoolean("disableOptionalEffectsWithFeatherOpt", CAT_FEATURES, true, "Keep the optional framebuffer effect disabled when FeatherOpt is present unless compatibility is verified.");
            enableEntityHooks = cfg.getBoolean("enableEntityHooks", CAT_FEATURES, false, "Enable optional entity/item hooks with vanilla fallback.");
            enableEntityDistanceCulling = cfg.getBoolean("enableEntityDistanceCulling", CAT_FEATURES, false, "Enable opt-in distance culling for living-entity renders.");
            entityCullingDistance = cfg.getInt("entityCullingDistance", CAT_FEATURES, 96, 16, 512, "Maximum render distance in blocks when entity distance culling is enabled.");
            enableItemHooks = cfg.getBoolean("enableItemHooks", CAT_FEATURES, false, "Enable optional item-render optimization hooks (fallback-only in this build).");
            enableParticleHooks = cfg.getBoolean("enableParticleHooks", CAT_FEATURES, false, "Enable optional particle throttling hooks with vanilla fallback.");
            particleThrottleSetting = cfg.getInt("particleThrottleSetting", CAT_FEATURES, 1, 0, 2, "Particle quality floor when particle throttling is enabled (0=all, 1=decreased, 2=minimal).");
            enableWeatherHooks = cfg.getBoolean("enableWeatherHooks", CAT_FEATURES, false, "Enable optional rain/snow hooks with vanilla fallback.");
            weatherRenderRadiusBlocks = cfg.getInt("weatherRenderRadiusBlocks", CAT_FEATURES, 128, 32, 256, "Maximum horizontal weather render radius in blocks for the optional optimized weather renderer.");
            weatherFrustumMinDotPercent = cfg.getInt("weatherFrustumMinDotPercent", CAT_FEATURES, -35, -100, 100, "Minimum horizontal forward-dot threshold percent used for conservative weather frustum filtering (higher values cull more behind-camera columns).");
            enableCloudHooks = cfg.getBoolean("enableCloudHooks", CAT_FEATURES, false, "Enable optional cloud hooks with vanilla fallback.");
            enableCloudRenderer = cfg.getBoolean("enableCloudRenderer", CAT_FEATURES, false, "Enable conservative cloud layer renderer when vanilla clouds are disabled.");
            cloudRenderRadiusBlocks = cfg.getInt("cloudRenderRadiusBlocks", CAT_FEATURES, 128, 32, 512, "Radius in blocks for the conservative optional cloud layer renderer.");
            enableBlockEntityHooks = cfg.getBoolean("enableBlockEntityHooks", CAT_FEATURES, false, "Enable optional block-entity optimization hooks (fallback-only in this build).");
            optionalEffectsCompositeAlpha = cfg.getInt("optionalEffectsCompositeAlpha", CAT_FEATURES, 100, 25, 100, "Composite alpha percentage for optional framebuffer post-effects.");
            optionalEffectsTintRed = cfg.getInt("optionalEffectsTintRed", CAT_FEATURES, 100, 0, 100, "Red channel multiplier percent for optional framebuffer post-effects.");
            optionalEffectsTintGreen = cfg.getInt("optionalEffectsTintGreen", CAT_FEATURES, 100, 0, 100, "Green channel multiplier percent for optional framebuffer post-effects.");
            optionalEffectsTintBlue = cfg.getInt("optionalEffectsTintBlue", CAT_FEATURES, 100, 0, 100, "Blue channel multiplier percent for optional framebuffer post-effects.");
            enableModelFallback = cfg.getBoolean("enableModelFallback", CAT_FALLBACKS, true, "Enable conservative block-bounds fallback when model data is missing.");
        } finally {
            if (cfg.hasChanged()) cfg.save();
        }
    }

    public static boolean shouldUseOptionalEffects() {
        return enableOptionalEffects
                && !(disableOptionalEffectsWithFeatherOpt && CompatibilityEnvironment.isFeatherOptPresent());
    }
}

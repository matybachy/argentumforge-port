package matybachy.argentumforge;

import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public final class ArgentumConfig {
    private static final String CAT_RENDERER = "renderer";
    private static final String CAT_FEATURES = "features";
    private static final String CAT_FALLBACKS = "fallbacks";

    public static final String TERRAIN_MODE_INTEGRATED = "integrated";
    public static final String TERRAIN_MODE_OVERLAY = "overlay";

    public static boolean enableExperimentalRenderer = false;
    public static String terrainMode = TERRAIN_MODE_INTEGRATED;
    public static int terrainSnapshotsPerFrame = 16;
    public static int terrainMaxPendingSnapshots = 256;
    public static boolean enableTerrainBoundsCulling = true;
    public static boolean disableExperimentalRendererWithFeatherOpt = false;
    public static int renderChunkRadius = 3;
    public static int rebuildBudgetPerTick = 2;
    public static boolean enableLightingAo = true;
    public static boolean enableTransparency = true;
    public static boolean enableModelFallback = true;
    public static boolean enableOptionalEffects = false;
    public static boolean disableOptionalEffectsWithFeatherOpt = false;
    public static boolean enableEntityHooks = false;
    public static boolean enableEntityDistanceCulling = false;
    public static int entityCullingDistance = 96;
    public static boolean enableItemHooks = false;
    public static boolean enableItemDistanceCulling = false;
    public static int itemCullingDistance = 96;
    public static boolean enableBlockEntityHooks = false;
    public static boolean enableBlockEntityDistanceCulling = false;
    public static int blockEntityCullingDistance = 48;
    public static boolean enableParticleHooks = false;
    public static int particleThrottleSetting = 1;
    public static boolean enableWeatherHooks = false;
    public static int weatherRenderRadiusBlocks = 128;
    public static int weatherFrustumMinDotPercent = -35;
    public static boolean enableCloudHooks = false;
    public static boolean enableCloudRenderer = false;
    public static int cloudRenderRadiusBlocks = 128;
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
            enableExperimentalRenderer = cfg.getBoolean("enableExperimentalRenderer", CAT_RENDERER, false, "Master switch for ArgentumForge rendering changes. terrainMode selects the terrain path; every optional hook below also requires this switch.");
            String mode = cfg.getString("terrainMode", CAT_RENDERER, TERRAIN_MODE_INTEGRATED,
                    "integrated: install the snapshot-based section compiler and bounds culling inside RenderGlobal's own chunk lifecycle (no overlay). overlay: legacy late RenderWorldLastEvent overlay drawn after vanilla terrain (can duplicate terrain).",
                    new String[]{TERRAIN_MODE_INTEGRATED, TERRAIN_MODE_OVERLAY});
            terrainMode = TERRAIN_MODE_OVERLAY.equalsIgnoreCase(mode) ? TERRAIN_MODE_OVERLAY : TERRAIN_MODE_INTEGRATED;
            terrainSnapshotsPerFrame = cfg.getInt("terrainSnapshotsPerFrame", CAT_RENDERER, 16, 0, 256, "Integrated mode: maximum section snapshots captured on the client thread per frame. Sections over budget compile from vanilla's region cache. 0 disables snapshots.");
            terrainMaxPendingSnapshots = cfg.getInt("terrainMaxPendingSnapshots", CAT_RENDERER, 256, 16, 4096, "Integrated mode: maximum captured snapshots waiting for a worker (about 50 KB each).");
            enableTerrainBoundsCulling = cfg.getBoolean("enableTerrainBoundsCulling", CAT_RENDERER, true, "Integrated mode: skip section layers whose rendered-block bounds are outside the camera side planes.");
            disableExperimentalRendererWithFeatherOpt = cfg.getBoolean("disableExperimentalRendererWithFeatherOpt", CAT_RENDERER, false, "Optionally disable only the terrain path (integrated compiler or legacy overlay) when FeatherOpt is present; unrelated optional hooks remain controlled by their own settings.");
            renderChunkRadius = cfg.getInt("renderChunkRadius", CAT_RENDERER, 3, 1, 8, "Overlay mode only: chunk radius around camera to keep meshed.");
            rebuildBudgetPerTick = cfg.getInt("rebuildBudgetPerTick", CAT_RENDERER, 2, 1, 64, "Overlay mode only: maximum chunk rebuilds per client tick.");
            enableLightingAo = cfg.getBoolean("enableLightingAo", CAT_FEATURES, true, "Enable world-light based brightness and lightweight ambient occlusion.");
            enableTransparency = cfg.getBoolean("enableTransparency", CAT_FEATURES, true, "Enable cutout/translucent split and fluid translucency.");
            enableOptionalEffects = cfg.getBoolean("enableOptionalEffects", CAT_FEATURES, false, "Enable optional post/effect pipeline when supported.");
            disableOptionalEffectsWithFeatherOpt = cfg.getBoolean("disableOptionalEffectsWithFeatherOpt", CAT_FEATURES, false, "Optionally disable framebuffer effects when a runtime FeatherOpt conflict is observed.");
            enableEntityHooks = cfg.getBoolean("enableEntityHooks", CAT_FEATURES, false, "Enable opt-in entity render-event hooks with vanilla fallback.");
            enableEntityDistanceCulling = cfg.getBoolean("enableEntityDistanceCulling", CAT_FEATURES, false, "Enable opt-in distance culling for living-entity renders.");
            entityCullingDistance = cfg.getInt("entityCullingDistance", CAT_FEATURES, 96, 16, 512, "Maximum render distance in blocks when entity distance culling is enabled.");
            enableItemHooks = cfg.getBoolean("enableItemHooks", CAT_FEATURES, false, "Enable opt-in dropped-item render-distance adjustment.");
            enableItemDistanceCulling = cfg.getBoolean("enableItemDistanceCulling", CAT_FEATURES, false, "Enable opt-in distance culling for dropped item entities.");
            itemCullingDistance = cfg.getInt("itemCullingDistance", CAT_FEATURES, 96, 16, 512, "Maximum render distance in blocks when dropped-item distance culling is enabled.");
            enableBlockEntityHooks = cfg.getBoolean("enableBlockEntityHooks", CAT_FEATURES, false, "Enable opt-in block-entity renderer hooks.");
            enableBlockEntityDistanceCulling = cfg.getBoolean("enableBlockEntityDistanceCulling", CAT_FEATURES, false, "Skip vanilla chest, ender chest, sign, skull and banner renderers beyond blockEntityCullingDistance.");
            blockEntityCullingDistance = cfg.getInt("blockEntityCullingDistance", CAT_FEATURES, 48, 8, 256, "Distance in blocks for block-entity distance culling.");
            enableParticleHooks = cfg.getBoolean("enableParticleHooks", CAT_FEATURES, false, "Enable optional particle throttling hooks with vanilla fallback.");
            particleThrottleSetting = cfg.getInt("particleThrottleSetting", CAT_FEATURES, 1, 0, 2, "Particle quality floor when particle throttling is enabled (0=all, 1=decreased, 2=minimal).");
            enableWeatherHooks = cfg.getBoolean("enableWeatherHooks", CAT_FEATURES, false, "Enable optional rain/snow hooks with vanilla fallback.");
            weatherRenderRadiusBlocks = cfg.getInt("weatherRenderRadiusBlocks", CAT_FEATURES, 128, 32, 256, "Maximum horizontal weather render radius in blocks for the optional optimized weather renderer.");
            weatherFrustumMinDotPercent = cfg.getInt("weatherFrustumMinDotPercent", CAT_FEATURES, -35, -100, 100, "Minimum horizontal forward-dot threshold percent used for conservative weather frustum filtering (higher values cull more behind-camera columns).");
            enableCloudHooks = cfg.getBoolean("enableCloudHooks", CAT_FEATURES, false, "Enable optional cloud hooks with vanilla fallback.");
            enableCloudRenderer = cfg.getBoolean("enableCloudRenderer", CAT_FEATURES, false, "Enable conservative cloud layer renderer when vanilla clouds are disabled.");
            cloudRenderRadiusBlocks = cfg.getInt("cloudRenderRadiusBlocks", CAT_FEATURES, 128, 32, 512, "Radius in blocks for the conservative optional cloud layer renderer.");
            optionalEffectsCompositeAlpha = cfg.getInt("optionalEffectsCompositeAlpha", CAT_FEATURES, 100, 25, 100, "Composite alpha percentage for the optional late custom-pass framebuffer effect.");
            optionalEffectsTintRed = cfg.getInt("optionalEffectsTintRed", CAT_FEATURES, 100, 0, 100, "Red channel multiplier percent for optional framebuffer post-effects.");
            optionalEffectsTintGreen = cfg.getInt("optionalEffectsTintGreen", CAT_FEATURES, 100, 0, 100, "Green channel multiplier percent for optional framebuffer post-effects.");
            optionalEffectsTintBlue = cfg.getInt("optionalEffectsTintBlue", CAT_FEATURES, 100, 0, 100, "Blue channel multiplier percent for optional framebuffer post-effects.");
            enableModelFallback = cfg.getBoolean("enableModelFallback", CAT_FALLBACKS, true, "Enable conservative block-bounds fallback when model data is missing.");
        } finally {
            if (cfg.hasChanged()) cfg.save();
        }
    }

    public static boolean isIntegratedTerrainMode() {
        return TERRAIN_MODE_INTEGRATED.equals(terrainMode);
    }

    public static boolean isOverlayTerrainMode() {
        return TERRAIN_MODE_OVERLAY.equals(terrainMode);
    }

    public static boolean isExperimentalRendererDisabledByCompatibility() {
        return disableExperimentalRendererWithFeatherOpt
                && CompatibilityEnvironment.isFeatherOptPresent();
    }

    public static boolean shouldUseOptionalEffects() {
        return enableOptionalEffects
                && !(disableOptionalEffectsWithFeatherOpt && CompatibilityEnvironment.isFeatherOptPresent());
    }
}

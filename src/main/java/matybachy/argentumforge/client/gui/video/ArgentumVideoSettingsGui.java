package matybachy.argentumforge.client.gui.video;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;

/**
 * Replacement for Options > Video Settings: the vanilla video options most players use, the two ArgentumForge options
 * that change the terrain path, and links to six pages that hold every other ArgentumForge option. Installed by
 * {@code ClientProxy} from {@code GuiOpenEvent}; see {@code ArgentumConfig.replaceVideoSettingsScreen}.
 */
public final class ArgentumVideoSettingsGui extends ArgentumSettingsScreen {
    private static final int EFFECTS_ID = 101;
    private static final int TERRAIN_ID = 102;
    private static final int DETAILS_ID = 103;
    private static final int PERFORMANCE_ID = 104;
    private static final int PARTICLES_ID = 105;
    private static final int OTHER_ID = 106;

    public ArgentumVideoSettingsGui(GuiScreen parent) {
        super(parent, "options.videoTitle");
    }

    @Override
    protected void addControls() {
        addVanillaOption(GameSettings.Options.GRAPHICS);
        addVanillaOption(GameSettings.Options.RENDER_DISTANCE);
        addVanillaOption(GameSettings.Options.AMBIENT_OCCLUSION);
        addVanillaOption(GameSettings.Options.FRAMERATE_LIMIT);
        addVanillaOption(GameSettings.Options.GUI_SCALE);
        addVanillaOption(GameSettings.Options.VIEW_BOBBING);
        addVanillaOption(GameSettings.Options.GAMMA);
        // The vanilla screen hides this option when the driver has no VBO support.
        if (OpenGlHelper.vboSupported) addVanillaOption(GameSettings.Options.USE_VBO);
        addConfigOption(ConfigOption.reload("enableExperimentalRenderer"));
        addConfigOption(ConfigOption.reload("terrainMode"));
        endRow();
        addPageButton(EFFECTS_ID, "argentumforge.video.page.effects");
        addPageButton(TERRAIN_ID, "argentumforge.video.page.terrain");
        addPageButton(DETAILS_ID, "argentumforge.video.page.details");
        addPageButton(PERFORMANCE_ID, "argentumforge.video.page.performance");
        addPageButton(PARTICLES_ID, "argentumforge.video.page.particles");
        addPageButton(OTHER_ID, "argentumforge.video.page.other");
    }

    @Override
    protected void onPageButton(GuiButton button) {
        GuiScreen page = page(button.id);
        if (page != null) openChild(page);
    }

    private GuiScreen page(int id) {
        switch (id) {
            case EFFECTS_ID:
                // Framebuffer composite/tint: only the legacy overlay pass uses it.
                return new ArgentumOptionsPage(this, "argentumforge.video.title.effects",
                        ConfigOption.overlayOnly("enableOptionalEffects"),
                        ConfigOption.overlayOnly("optionalEffectsCompositeAlpha"),
                        ConfigOption.overlayOnly("optionalEffectsTintRed"),
                        ConfigOption.overlayOnly("optionalEffectsTintGreen"),
                        ConfigOption.overlayOnly("optionalEffectsTintBlue"));
            case TERRAIN_ID:
                return new ArgentumOptionsPage(this, "argentumforge.video.title.terrain",
                        ConfigOption.reload("terrainMode"),
                        ConfigOption.of("enableTerrainBoundsCulling"),
                        ConfigOption.overlayOnly("enableLightingAo"),
                        ConfigOption.overlayOnly("enableTransparency"),
                        ConfigOption.overlayOnly("enableModelFallback"));
            case DETAILS_ID:
                return new ArgentumOptionsPage(this, "argentumforge.video.title.details",
                        ConfigOption.of("enableCloudHooks"),
                        ConfigOption.of("enableCloudRenderer"),
                        ConfigOption.of("cloudRenderRadiusBlocks"),
                        ConfigOption.of("enableWeatherHooks"),
                        ConfigOption.of("weatherRenderRadiusBlocks"),
                        ConfigOption.of("weatherFrustumMinDotPercent"),
                        ConfigOption.of("enableItemHooks"),
                        ConfigOption.of("enableItemDistanceCulling"),
                        ConfigOption.of("itemCullingDistance"));
            case PERFORMANCE_ID:
                return new ArgentumOptionsPage(this, "argentumforge.video.title.performance",
                        ConfigOption.of("terrainSnapshotsPerFrame"),
                        ConfigOption.of("terrainMaxPendingSnapshots"),
                        ConfigOption.overlayOnly("rebuildBudgetPerTick"),
                        ConfigOption.overlayOnly("renderChunkRadius"),
                        ConfigOption.of("enableEntityHooks"),
                        ConfigOption.of("enableEntityDistanceCulling"),
                        ConfigOption.of("entityCullingDistance"),
                        ConfigOption.of("enableNonLivingEntityCulling"),
                        ConfigOption.of("nonLivingEntityCullingDistance"),
                        ConfigOption.of("enableBlockEntityHooks"),
                        ConfigOption.of("enableBlockEntityDistanceCulling"),
                        ConfigOption.of("blockEntityCullingDistance"));
            case PARTICLES_ID:
                return new ArgentumOptionsPage(this, "argentumforge.video.title.particles",
                        ConfigOption.of("enableParticleHooks"),
                        ConfigOption.of("particleThrottleSetting"));
            case OTHER_ID:
                return new ArgentumOtherPage(this);
            default:
                return null;
        }
    }
}

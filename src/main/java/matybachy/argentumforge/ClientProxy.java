package matybachy.argentumforge;

import matybachy.argentumforge.client.ArgentumRenderer;
import matybachy.argentumforge.client.SmokeTest;
import matybachy.argentumforge.client.blockentity.BlockEntityDistanceCulling;
import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import matybachy.argentumforge.client.entity.NonLivingEntityCulling;
import matybachy.argentumforge.client.gui.ArgentumConfigScreen;
import matybachy.argentumforge.client.gui.video.ArgentumVideoSettingsGui;
import matybachy.argentumforge.client.mesh.ClientWorldHooks;
import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import matybachy.argentumforge.client.mesh.RendererValidation;
import matybachy.argentumforge.client.terrain.IntegratedTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private ArgentumRenderer renderer;
    private final ChunkMeshManager meshManager = ArgentumForge.MESH_MANAGER;
    private ClientWorldHooks worldHooks;
    private final KeyBinding openSettingsKey = new KeyBinding("Open ArgentumForge settings", Keyboard.KEY_NONE, "ArgentumForge");

    @Override
    public void preInit() {
        RendererValidation.validateStaticInvariants();
        renderer = new ArgentumRenderer();
        // ArgentumConfig.sync() enables the legacy overlay mesh manager only in terrainMode=overlay.
        worldHooks = new ClientWorldHooks(meshManager);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(worldHooks);
        SmokeTest.registerIfRequested();
    }

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(openSettingsKey);
    }

    @Override
    public void postInit() {
        // This build has no Mixin, coremod or tweaker (the Mixin build is on the mixin-variant branch): if a log shows
        // "mixins.argentumforge.json" or "FontRendererMixin", an older ArgentumForge jar is installed.
        LOGGER.info("Active parts: mixins=none, enableExperimentalRenderer={}, enableEntityHooks={}, enableItemHooks={}, "
                        + "enableBlockEntityHooks={}, enableParticleHooks={}, enableWeatherHooks={}, enableCloudHooks={}, "
                        + "enableOptionalEffects={}, replaceVideoSettingsScreen={}; detected: OptiFine={}, Dawn={}, FeatherOpt={}",
                ArgentumConfig.enableExperimentalRenderer, ArgentumConfig.enableEntityHooks, ArgentumConfig.enableItemHooks,
                ArgentumConfig.enableBlockEntityHooks, ArgentumConfig.enableParticleHooks, ArgentumConfig.enableWeatherHooks,
                ArgentumConfig.enableCloudHooks, ArgentumConfig.enableOptionalEffects,
                ArgentumConfig.replaceVideoSettingsScreen && !CompatibilityEnvironment.isOptiFinePresent(),
                CompatibilityEnvironment.isOptiFinePresent(), CompatibilityEnvironment.isDawnPresent(),
                CompatibilityEnvironment.isFeatherOptPresent());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (openSettingsKey.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new ArgentumConfigScreen(null));
        }
        if (renderer != null) renderer.onClientTick();
        // The FeatherOpt terrain gate suppresses only the terrain path; optional hooks
        // (entity, item, particle, weather, cloud, block entity) keep their own settings.
        if (ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER
                && ArgentumConfig.isOverlayTerrainMode()
                && !ArgentumConfig.isExperimentalRendererDisabledByCompatibility()) {
            meshManager.onTick();
        }
        IntegratedTerrain.onClientTick(mc);
        BlockEntityDistanceCulling.onClientTick();
        NonLivingEntityCulling.onClientTick();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // RenderGlobal.setWorldAndLoadRenderers(null) keeps the old ViewFrustum, so pending
        // snapshots would otherwise keep the unloaded world's chunks reachable.
        if (event.world != null && event.world.isRemote) IntegratedTerrain.onClientWorldUnload();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) IntegratedTerrain.onRenderFrameStart();
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || !mc.gameSettings.showDebugInfo || event.left == null) return;
        event.left.add("[Argentum] compat: Dawn " + yesNo(CompatibilityEnvironment.isDawnPresent())
                + ", FeatherOpt " + yesNo(CompatibilityEnvironment.isFeatherOptPresent())
                + ", OptiFine " + yesNo(CompatibilityEnvironment.isOptiFinePresent())
                + "; terrain " + IntegratedTerrain.describeState());
        IntegratedTerrain.appendDebugLines(event.left);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!ArgentumForge.MODID.equals(event.modID)) return;
        if (ArgentumConfig.getConfiguration().hasChanged()) ArgentumConfig.getConfiguration().save();
        ArgentumConfig.sync();
    }

    /**
     * Shows the ArgentumForge video settings instead of the vanilla screen. Only the exact vanilla class is replaced
     * (never subclasses or our own screen), not when OptiFine is detected, and regardless of the master switch, so
     * the renderer can be turned on from there. The parent is the screen open when the event fires.
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null || event.gui.getClass() != GuiVideoSettings.class) return;
        if (!ArgentumConfig.replaceVideoSettingsScreen || CompatibilityEnvironment.isOptiFinePresent()) return;
        event.gui = new ArgentumVideoSettingsGui(Minecraft.getMinecraft().currentScreen);
    }

    public ChunkMeshManager getMeshManager() {
        return meshManager;
    }
}

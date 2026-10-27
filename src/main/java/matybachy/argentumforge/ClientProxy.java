package matybachy.argentumforge;

import matybachy.argentumforge.client.ArgentumRenderer;
import matybachy.argentumforge.client.SmokeTest;
import matybachy.argentumforge.client.blockentity.BlockEntityDistanceCulling;
import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import matybachy.argentumforge.client.entity.NonLivingEntityCulling;
import matybachy.argentumforge.client.gui.ArgentumConfigScreen;
import matybachy.argentumforge.client.mesh.ClientWorldHooks;
import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import matybachy.argentumforge.client.mesh.RendererValidation;
import matybachy.argentumforge.client.terrain.IntegratedTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
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
        // client post-init
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

    public ChunkMeshManager getMeshManager() {
        return meshManager;
    }
}

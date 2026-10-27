package matybachy.argentumforge.client;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import matybachy.argentumforge.client.terrain.IntegratedTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Development smoke test, active only with {@code -Dargentumforge.smokeTest=true}. It creates a creative world,
 * measures a vanilla phase, turns on every ArgentumForge feature in memory (the config file is not written),
 * spawns particles around the player, flips the VBO setting so both terrain variants are installed, checks that
 * the terrain path reports activity, saves a screenshot, turns the renderer off again, checks that vanilla objects are
 * restored and quits. FPS figures are logged for reference only; they are not a controlled benchmark.
 * Results are logged with the prefix {@code [SmokeTest]}; the last line is PASS or FAIL.
 */
public final class SmokeTest {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final int VANILLA_START = 150;
    private static final int FEATURES_ON = 250;
    private static final int FIRST_MODE_MEASURE = 330;
    private static final int TOGGLE_VBO = 400;
    private static final int SECOND_MODE_MEASURE = 480;
    private static final int FINISH = 560;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean worldRequested;
    private int ticksInWorld;
    private long vanillaFpsSum;
    private int vanillaSamples;
    private final long[] modeFpsSum = new long[2];
    private final int[] modeSamples = new int[2];
    private final boolean[] modeInstalled = new boolean[2];
    private boolean originalVbo;

    public static void registerIfRequested() {
        if (Boolean.getBoolean("argentumforge.smokeTest")) {
            LOGGER.info("[SmokeTest] enabled");
            MinecraftForge.EVENT_BUS.register(new SmokeTest());
        }
    }

    private void launchWorldFromMenu() {
        worldRequested = true;
        mc.gameSettings.pauseOnLostFocus = false;
        WorldSettings settings = new WorldSettings(8675309L, WorldSettings.GameType.CREATIVE, true, false, WorldType.DEFAULT);
        LOGGER.info("[SmokeTest] launching world");
        mc.launchIntegratedServer("argentum-smoke", "argentum-smoke", settings);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Launching from GuiOpenEvent would run inside Minecraft.startGame, before the client is fully initialized.
        if (!worldRequested && mc.currentScreen instanceof GuiMainMenu) launchWorldFromMenu();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        ticksInWorld++;
        int fps = Minecraft.getDebugFPS();
        if (ticksInWorld >= VANILLA_START && ticksInWorld < FEATURES_ON) {
            vanillaFpsSum += fps;
            vanillaSamples++;
        } else if (ticksInWorld == FEATURES_ON) {
            enableAllFeatures();
        } else if (ticksInWorld >= FIRST_MODE_MEASURE && ticksInWorld < TOGGLE_VBO) {
            sampleMode(fps);
        } else if (ticksInWorld == TOGGLE_VBO) {
            mc.gameSettings.useVbo = !mc.gameSettings.useVbo;
            mc.renderGlobal.loadRenderers();
            LOGGER.info("[SmokeTest] switched useVbo to {}", mc.gameSettings.useVbo);
        } else if (ticksInWorld >= SECOND_MODE_MEASURE && ticksInWorld < FINISH) {
            sampleMode(fps);
        }
        if (ticksInWorld > FEATURES_ON) {
            mc.thePlayer.rotationYaw += 1.5F;
            spawnParticles();
        }
        if (ticksInWorld == FINISH) finish();
    }

    private void sampleMode(int fps) {
        if (!IntegratedTerrain.isInstalled()) return;
        int mode = IntegratedTerrain.isInstalledWithVbo() ? 1 : 0;
        modeInstalled[mode] = true;
        modeFpsSum[mode] += fps;
        modeSamples[mode]++;
    }

    private void enableAllFeatures() {
        originalVbo = mc.gameSettings.useVbo;
        ArgentumConfig.enableExperimentalRenderer = true;
        ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER = true;
        ArgentumConfig.terrainMode = ArgentumConfig.TERRAIN_MODE_INTEGRATED;
        ArgentumConfig.enableEntityHooks = true;
        ArgentumConfig.enableEntityDistanceCulling = true;
        ArgentumConfig.enableNonLivingEntityCulling = true;
        ArgentumConfig.enableItemHooks = true;
        ArgentumConfig.enableItemDistanceCulling = true;
        ArgentumConfig.enableBlockEntityHooks = true;
        ArgentumConfig.enableBlockEntityDistanceCulling = true;
        ArgentumConfig.enableParticleHooks = true;
        ArgentumConfig.enableWeatherHooks = true;
        ArgentumConfig.enableCloudHooks = true;
        mc.theWorld.getWorldInfo().setRaining(true);
        LOGGER.info("[SmokeTest] all features enabled");
    }

    private void spawnParticles() {
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY + 1.0D;
        double z = mc.thePlayer.posZ;
        for (int i = 0; i < 24; i++) {
            double angle = i * (Math.PI * 2.0D / 24.0D);
            mc.theWorld.spawnParticle(EnumParticleTypes.FLAME,
                    x + Math.cos(angle) * 6.0D, y, z + Math.sin(angle) * 6.0D, 0.0D, 0.02D, 0.0D);
        }
    }

    private void finish() {
        List<String> failures = new ArrayList<String>();
        List<String> debug = new ArrayList<String>();
        IntegratedTerrain.appendDebugLines(debug);
        for (String line : debug) LOGGER.info("[SmokeTest] {}", line);

        if (!modeInstalled[0]) failures.add("integrated terrain never installed in display-list mode");
        if (!modeInstalled[1]) failures.add("integrated terrain never installed in VBO mode");
        if (debug.isEmpty() || !debug.get(0).contains("snapshot builds") || debug.get(0).contains("snapshot builds 0,")) {
            failures.add("no snapshot-backed section builds");
        }

        LOGGER.info("[SmokeTest] average FPS (reference only) vanilla={} display-list={} vbo={}",
                average(vanillaFpsSum, vanillaSamples), average(modeFpsSum[0], modeSamples[0]),
                average(modeFpsSum[1], modeSamples[1]));
        try {
            LOGGER.info("[SmokeTest] {}", ScreenShotHelper.saveScreenshot(mc.mcDataDir, "argentum-smoke.png",
                    mc.displayWidth, mc.displayHeight, mc.getFramebuffer()).getUnformattedText());
        } catch (RuntimeException e) {
            LOGGER.warn("[SmokeTest] screenshot failed", e);
        }
        checkRestore(failures);
        LOGGER.info("[SmokeTest] {}", failures.isEmpty() ? "PASS" : "FAIL " + failures);
        // Leave the world exactly like GuiIngameMenu's quit button, so the integrated server finishes saving before
        // the JVM exits. Opening the menu releases mouse focus; otherwise this frame's camera update hits a null player.
        mc.theWorld.sendQuittingDisconnectingPacket();
        mc.loadWorld(null);
        mc.displayGuiScreen(new GuiMainMenu());
        mc.shutdown();
    }

    private static long average(long sum, int samples) {
        return samples == 0 ? 0 : sum / samples;
    }

    /** Turning the master switch off must put the vanilla chunk factory and container back. */
    private void checkRestore(List<String> failures) {
        ArgentumConfig.enableExperimentalRenderer = false;
        ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER = false;
        IntegratedTerrain.onClientTick(mc);
        if (IntegratedTerrain.isInstalled()) failures.add("integrated terrain still installed after disabling");
        if (mc.gameSettings.useVbo != originalVbo) {
            mc.gameSettings.useVbo = originalVbo;
            mc.renderGlobal.loadRenderers();
        }
    }
}

package matybachy.argentumforge.client.gui.video;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.terrain.IntegratedTerrain;
import net.minecraft.client.Minecraft;

/**
 * State shared by the video settings screens while they are open. Ordinary options take effect on click; options that
 * reload the chunk renderer (master switch, terrainMode, a reset) are only written to the config until the screens
 * are left, then applied once through the normal path: {@link ArgentumConfig#sync()} and
 * {@link IntegratedTerrain#onClientTick}, which installs or restores the chunk factory and calls loadRenderers.
 */
final class VideoSettingsSession {
    private static boolean reloadPending;

    private VideoSettingsSession() {
    }

    /** A config value was written by a screen. */
    static void onChanged(boolean reloadOption) {
        if (reloadOption) reloadPending = true;
        // Applies every non-reload option now; the master switch and terrainMode keep their applied values.
        ArgentumConfig.sync(false, false);
    }

    /** The video settings screens were left (not a move between them): save and apply everything. */
    static void onLeft(Minecraft mc) {
        ArgentumConfig.sync(true, true);
        if (!reloadPending) return;
        reloadPending = false;
        IntegratedTerrain.onClientTick(mc);
    }
}

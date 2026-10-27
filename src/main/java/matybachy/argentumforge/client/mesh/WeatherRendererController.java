package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.IRenderHandler;

public final class WeatherRendererController {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final OptimizedWeatherRenderer optimizedWeatherRenderer = new OptimizedWeatherRenderer();
    private WorldClient installedWorld;

    public void onClientTick() {
        WorldClient world = mc != null ? mc.theWorld : null;
        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER || !ArgentumConfig.enableWeatherHooks || world == null || world.provider == null) {
            uninstallOptimizedRenderer();
            return;
        }
        if (installedWorld != world) {
            uninstallOptimizedRenderer();
        }

        IRenderHandler current = world.provider.getWeatherRenderer();
        if (current == optimizedWeatherRenderer) {
            installedWorld = world;
            return;
        }
        if (current != null) return;

        try {
            world.provider.setWeatherRenderer(optimizedWeatherRenderer);
            installedWorld = world;
        } catch (Throwable ignored) {
            uninstallOptimizedRenderer();
        }
    }

    private void uninstallOptimizedRenderer() {
        if (installedWorld == null || installedWorld.provider == null) {
            installedWorld = null;
            return;
        }
        try {
            if (installedWorld.provider.getWeatherRenderer() == optimizedWeatherRenderer) {
                installedWorld.provider.setWeatherRenderer(null);
            }
        } catch (Throwable ignored) {
        } finally {
            installedWorld = null;
        }
    }
}

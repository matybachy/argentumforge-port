package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.IRenderHandler;

public final class WeatherRendererController {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final OptimizedWeatherRenderer optimizedWeatherRenderer = new OptimizedWeatherRenderer();
    private WorldClient installedWorld;

    public void onClientTick() {
        WorldClient world = mc != null ? mc.theWorld : null;
        // A provider weather renderer makes Forge return before the vanilla body of EntityRenderer.renderRainSnow,
        // which is where Dawn hooks its weather options and OptiFine applies Rain & Snow; so with either present this
        // stays off unless opted in.
        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER || !ArgentumConfig.enableWeatherHooks || world == null || world.provider == null
                || (CompatibilityEnvironment.isDawnPresent() && !ArgentumConfig.enableWeatherHooksWithDawn)
                || (CompatibilityEnvironment.isOptiFinePresent() && !ArgentumConfig.enableWeatherHooksWithOptiFine)) {
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

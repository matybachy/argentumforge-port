package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;

public final class OptionalRenderHooks {
    public void onBeforeTerrainRender() {
    }

    public void onAfterTerrainRender() {
    }

    public boolean useEntityHooks() {
        return ArgentumConfig.enableEntityHooks;
    }

    public boolean useWeatherHooks() {
        return ArgentumConfig.enableWeatherHooks;
    }

    public boolean useCloudHooks() {
        return ArgentumConfig.enableCloudHooks;
    }
}

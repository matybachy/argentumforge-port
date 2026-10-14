package matybachy.argentumforge.client.mesh;

public final class RenderOptimizationMath {
    private RenderOptimizationMath() {
    }

    public static boolean shouldCullByDistance(double distanceSq, double maxDistanceBlocks) {
        double safeMax = Math.max(0.0D, maxDistanceBlocks);
        return distanceSq > safeMax * safeMax;
    }

    public static int clampedParticleSetting(int value) {
        return Math.max(0, Math.min(2, value));
    }

    public static int throttledParticleSetting(int currentSetting, int minimumThrottleSetting) {
        int clampedCurrent = clampedParticleSetting(currentSetting);
        int clampedMinimum = clampedParticleSetting(minimumThrottleSetting);
        return Math.max(clampedCurrent, clampedMinimum);
    }

    public static boolean shouldRunCloudFallback(int cloudsSetting, boolean cloudHooksEnabled, boolean cloudRendererEnabled, boolean hasCustomSkyRenderer) {
        return cloudHooksEnabled && cloudRendererEnabled && cloudsSetting == 0 && !hasCustomSkyRenderer;
    }

    public static boolean shouldUseEntityDistanceCulling(boolean rendererEnabled, boolean entityHooksEnabled,
                                                         boolean distanceCullingEnabled, boolean hasCustomName, boolean isLeashed) {
        return rendererEnabled && entityHooksEnabled && distanceCullingEnabled && !hasCustomName && !isLeashed;
    }

    public static boolean shouldRunOptionalEffects(boolean configEnabled, boolean framebufferEnabled, boolean disabledByFailure, boolean onClientThread) {
        return configEnabled && framebufferEnabled && !disabledByFailure && onClientThread;
    }
}

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

    public static int clampedPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static double weatherMaxDistanceSq(int maxDistanceBlocks) {
        double safeDistance = Math.max(1, maxDistanceBlocks);
        return safeDistance * safeDistance;
    }

    public static boolean shouldCullWeatherColumn(double distanceSq, double maxDistanceSq, double forwardDot, double minForwardDot, double nearDistanceSq) {
        if (distanceSq <= Math.max(0.0D, nearDistanceSq)) return false;
        if (distanceSq > Math.max(1.0D, maxDistanceSq)) return true;
        return forwardDot < minForwardDot;
    }
}

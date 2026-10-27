package matybachy.argentumforge.client.mesh;

import org.junit.Assert;
import org.junit.Test;

public class RenderOptimizationMathTest {
    @Test
    public void distanceCullingUsesSquaredDistance() {
        Assert.assertFalse(RenderOptimizationMath.shouldCullByDistance(63.0D, 8.0D));
        Assert.assertTrue(RenderOptimizationMath.shouldCullByDistance(65.0D, 8.0D));
    }

    @Test
    public void particleSettingIsClamped() {
        Assert.assertEquals(0, RenderOptimizationMath.clampedParticleSetting(-1));
        Assert.assertEquals(2, RenderOptimizationMath.clampedParticleSetting(5));
    }

    @Test
    public void particleThrottleNeverIncreasesDetail() {
        Assert.assertEquals(1, RenderOptimizationMath.throttledParticleSetting(0, 1));
        Assert.assertEquals(2, RenderOptimizationMath.throttledParticleSetting(2, 1));
    }

    @Test
    public void cloudFallbackRequiresVanillaCloudsDisabledAndNoCustomSky() {
        Assert.assertTrue(RenderOptimizationMath.shouldRunCloudFallback(0, true, true, false));
        Assert.assertFalse(RenderOptimizationMath.shouldRunCloudFallback(2, true, true, false));
        Assert.assertFalse(RenderOptimizationMath.shouldRunCloudFallback(0, true, true, true));
    }

    @Test
    public void entityDistanceCullingSkipsNamedOrLeashedEntities() {
        Assert.assertTrue(RenderOptimizationMath.shouldUseEntityDistanceCulling(true, true, true, false, false));
        Assert.assertFalse(RenderOptimizationMath.shouldUseEntityDistanceCulling(true, true, true, true, false));
        Assert.assertFalse(RenderOptimizationMath.shouldUseEntityDistanceCulling(true, true, true, false, true));
    }

    @Test
    public void optionalEffectsRequireCapabilitiesAndMainThread() {
        Assert.assertTrue(RenderOptimizationMath.shouldRunOptionalEffects(true, true, false, true));
        Assert.assertFalse(RenderOptimizationMath.shouldRunOptionalEffects(true, true, false, false));
        Assert.assertFalse(RenderOptimizationMath.shouldRunOptionalEffects(true, false, false, true));
        Assert.assertFalse(RenderOptimizationMath.shouldRunOptionalEffects(true, true, true, true));
    }

    @Test
    public void percentClampStaysWithinRange() {
        Assert.assertEquals(0, RenderOptimizationMath.clampedPercent(-5));
        Assert.assertEquals(50, RenderOptimizationMath.clampedPercent(50));
        Assert.assertEquals(100, RenderOptimizationMath.clampedPercent(500));
    }

    @Test
    public void weatherColumnCullingKeepsNearbyColumnsAndCullsDistantBehindCameraColumns() {
        double maxDistanceSq = RenderOptimizationMath.weatherMaxDistanceSq(64);
        Assert.assertFalse(RenderOptimizationMath.shouldCullWeatherColumn(16.0D, maxDistanceSq, -1.0D, -0.25D, 24.0D * 24.0D));
        Assert.assertTrue(RenderOptimizationMath.shouldCullWeatherColumn(maxDistanceSq + 1.0D, maxDistanceSq, 1.0D, -0.25D, 24.0D * 24.0D));
        Assert.assertTrue(RenderOptimizationMath.shouldCullWeatherColumn(40.0D * 40.0D, maxDistanceSq, -0.8D, -0.25D, 24.0D * 24.0D));
    }
}

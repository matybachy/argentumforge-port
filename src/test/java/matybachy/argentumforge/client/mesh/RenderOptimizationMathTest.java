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
    public void entityDistanceCullingSkipsCompatibilitySensitiveCases() {
        Assert.assertTrue(RenderOptimizationMath.shouldSkipEntityDistanceCulling(true, false, false, false, true));
        Assert.assertTrue(RenderOptimizationMath.shouldSkipEntityDistanceCulling(false, true, false, false, true));
        Assert.assertTrue(RenderOptimizationMath.shouldSkipEntityDistanceCulling(false, false, true, false, true));
        Assert.assertTrue(RenderOptimizationMath.shouldSkipEntityDistanceCulling(false, false, false, false, false));
        Assert.assertFalse(RenderOptimizationMath.shouldSkipEntityDistanceCulling(false, false, false, false, true));
    }

    @Test
    public void itemDistanceCullingUsesReducedRenderWeightWhenFar() {
        Assert.assertEquals(1.0D, RenderOptimizationMath.itemRenderDistanceWeightForDistance(1024.0D, 64.0D, 1.0D), 0.0001D);
        Assert.assertEquals(0.10D, RenderOptimizationMath.itemRenderDistanceWeightForDistance(5000.0D, 64.0D, 1.0D), 0.0001D);
    }

    @Test
    public void blockEntityOptimizationRequiresSupportedHook() {
        Assert.assertTrue(RenderOptimizationMath.isBlockEntityOptimizationSupported(true));
        Assert.assertFalse(RenderOptimizationMath.isBlockEntityOptimizationSupported(false));
    }
}

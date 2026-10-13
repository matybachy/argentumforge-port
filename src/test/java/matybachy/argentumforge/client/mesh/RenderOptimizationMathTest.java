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
}

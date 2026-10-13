package matybachy.argentumforge.client.mesh;

import org.junit.Assert;
import org.junit.Test;

public class MeshMathTest {
    @Test
    public void clamp01BoundsValues() {
        Assert.assertEquals(0.0f, MeshMath.clamp01(-2.0f), 0.0f);
        Assert.assertEquals(0.5f, MeshMath.clamp01(0.5f), 0.0f);
        Assert.assertEquals(1.0f, MeshMath.clamp01(9.0f), 0.0f);
    }

    @Test
    public void uvInsetUsesTextureSpan() {
        Assert.assertEquals(1.0f / 256.0f, MeshMath.uvInset(0.0f, 1.0f), 1.0e-6f);
        Assert.assertEquals(0.0f, MeshMath.uvInset(1.0f, 1.0f), 0.0f);
    }

    @Test
    public void fluidHeightIsClampedToExpectedRange() {
        Assert.assertEquals(1.0f, MeshMath.fluidHeightForLevel(0), 0.0f);
        Assert.assertEquals(0.125f, MeshMath.fluidHeightForLevel(7), 0.0f);
        Assert.assertEquals(0.125f, MeshMath.fluidHeightForLevel(100), 0.0f);
    }
}

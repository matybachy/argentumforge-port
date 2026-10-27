package matybachy.argentumforge.client.mesh;

import org.junit.Assert;
import org.junit.Test;

public class ChunkCoordTest {
    @Test
    public void keyRoundTripsCoordinates() {
        long key = ChunkCoord.key(-15, 42);
        Assert.assertEquals(-15, ChunkCoord.chunkX(key));
        Assert.assertEquals(42, ChunkCoord.chunkZ(key));
    }

    @Test
    public void chunkFromBlockUsesFloorDivision() {
        Assert.assertEquals(0, ChunkCoord.chunkFromBlock(15.999D));
        Assert.assertEquals(1, ChunkCoord.chunkFromBlock(16.0D));
        Assert.assertEquals(-1, ChunkCoord.chunkFromBlock(-0.1D));
    }

    @Test
    public void radiusCheckUsesChunkDistance() {
        Assert.assertTrue(ChunkCoord.isWithinRadius(0, 0, 2, 2, 3));
        Assert.assertFalse(ChunkCoord.isWithinRadius(0, 0, 4, 4, 3));
    }

    @Test
    public void radiusCheckIsEuclideanAndInclusive() {
        Assert.assertTrue(ChunkCoord.isWithinRadius(0, 0, 3, 0, 3));
        Assert.assertFalse(ChunkCoord.isWithinRadius(0, 0, 4, 0, 3));
        Assert.assertTrue(ChunkCoord.isWithinRadius(-2, 5, -4, 7, 3));
        Assert.assertFalse(ChunkCoord.isWithinRadius(-2, 5, -5, 8, 3));
    }
}

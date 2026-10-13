package matybachy.argentumforge.client.mesh;

final class ChunkCoord {
    private ChunkCoord() {
    }

    static long key(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    static int chunkX(long key) {
        return (int) (key >> 32);
    }

    static int chunkZ(long key) {
        return (int) key;
    }

    static int chunkFromBlock(double position) {
        return (int) Math.floor(position / 16.0D);
    }

    static double distanceSq(int fromChunkX, int fromChunkZ, int toChunkX, int toChunkZ) {
        double dx = fromChunkX - toChunkX;
        double dz = fromChunkZ - toChunkZ;
        return dx * dx + dz * dz;
    }

    static boolean isWithinRadius(int centerChunkX, int centerChunkZ, int chunkX, int chunkZ, int radiusChunks) {
        return distanceSq(centerChunkX, centerChunkZ, chunkX, chunkZ) <= (radiusChunks * radiusChunks);
    }
}

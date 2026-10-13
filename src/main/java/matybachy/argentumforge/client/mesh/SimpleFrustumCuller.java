package matybachy.argentumforge.client.mesh;

import net.minecraft.entity.Entity;

/**
 * Lightweight chunk visibility culling used by the terrain prototype.
 * This is intentionally simple: it culls chunks beyond a radius around the camera.
 * This is a stepping stone toward real frustum culling in a full Forge 1.8.9 renderer.
 */
public final class SimpleFrustumCuller {
    private final int maxDistanceChunks;

    public SimpleFrustumCuller(int maxDistanceChunks) {
        this.maxDistanceChunks = maxDistanceChunks;
    }

    public boolean shouldRenderChunk(Entity camera, int chunkX, int chunkZ) {
        if (camera == null) {
            return false;
        }

        int camChunkX = ChunkCoord.chunkFromBlock(camera.posX);
        int camChunkZ = ChunkCoord.chunkFromBlock(camera.posZ);
        return shouldRenderChunk(camChunkX, camChunkZ, chunkX, chunkZ);
    }

    boolean shouldRenderChunk(int cameraChunkX, int cameraChunkZ, int chunkX, int chunkZ) {
        return ChunkCoord.isWithinRadius(cameraChunkX, cameraChunkZ, chunkX, chunkZ, maxDistanceChunks);
    }
}

package matybachy.argentumforge.client.mesh;

import net.minecraft.entity.Entity;

/**
 * Lightweight chunk visibility culling used by the terrain prototype.
 * This is intentionally simple: it culls chunks beyond a radius around the camera.
 * This is a stepping stone toward real frustum culling in a full Forge 1.8.9 renderer.
 */
public final class SimpleFrustumCuller {
    private static final double DIRECTIONAL_CULL_DISTANCE_BLOCKS = 48.0D;
    private static final double BACKFACE_DOT_THRESHOLD = -0.35D;
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
        if (!shouldRenderChunk(camChunkX, camChunkZ, chunkX, chunkZ)) {
            return false;
        }

        return passesDirectionalCull(camera.posX, camera.posZ, camera.rotationYaw, chunkX, chunkZ);
    }

    boolean shouldRenderChunk(int cameraChunkX, int cameraChunkZ, int chunkX, int chunkZ) {
        return ChunkCoord.isWithinRadius(cameraChunkX, cameraChunkZ, chunkX, chunkZ, maxDistanceChunks);
    }

    boolean passesDirectionalCull(double cameraX, double cameraZ, float yawDegrees, int chunkX, int chunkZ) {
        double centerX = (chunkX << 4) + 8.0D;
        double centerZ = (chunkZ << 4) + 8.0D;
        double dx = centerX - cameraX;
        double dz = centerZ - cameraZ;
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq <= DIRECTIONAL_CULL_DISTANCE_BLOCKS * DIRECTIONAL_CULL_DISTANCE_BLOCKS) {
            return true;
        }

        double radians = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double length = Math.sqrt(distanceSq);
        if (length <= 0.0001D) {
            return true;
        }
        double dot = (dx * forwardX + dz * forwardZ) / length;
        return dot >= BACKFACE_DOT_THRESHOLD;
    }
}

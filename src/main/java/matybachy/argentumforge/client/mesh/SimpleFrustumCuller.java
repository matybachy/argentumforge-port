package matybachy.argentumforge.client.mesh;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;

/** Conservative visibility checks for the experimental chunk-mesh overlay. */
public final class SimpleFrustumCuller {
    private final int maxDistanceChunks;
    private double cameraX;
    private double cameraZ;
    private Frustum cameraFrustum;

    public SimpleFrustumCuller(int maxDistanceChunks) {
        this.maxDistanceChunks = Math.max(1, maxDistanceChunks);
    }

    /**
     * Captures the active 1.8.9 camera planes before the overlay applies its
     * world-space camera translation. If the planes cannot be read, rendering
     * fails open and the distance limit remains the only culling condition.
     */
    public void prepare(Entity camera, double cameraX, double cameraY, double cameraZ) {
        this.cameraX = cameraX;
        this.cameraZ = cameraZ;
        cameraFrustum = null;
        if (camera == null) return;

        try {
            Frustum frustum = new Frustum();
            frustum.setPosition(cameraX, cameraY, cameraZ);
            cameraFrustum = frustum;
        } catch (RuntimeException ignored) {
            // Keep the conservative radius fallback if the renderer is unavailable.
        }
    }

    public boolean shouldRenderChunk(Entity camera, int chunkX, int chunkZ) {
        if (camera == null) return false;
        double x = cameraFrustum == null ? camera.posX : cameraX;
        double z = cameraFrustum == null ? camera.posZ : cameraZ;
        int cameraChunkX = ChunkCoord.chunkFromBlock(x);
        int cameraChunkZ = ChunkCoord.chunkFromBlock(z);
        if (!ChunkCoord.isWithinRadius(cameraChunkX, cameraChunkZ, chunkX, chunkZ, maxDistanceChunks)) {
            return false;
        }

        if (cameraFrustum == null) return true;
        double minX = (double) chunkX * 16.0D;
        double minZ = (double) chunkZ * 16.0D;
        try {
            return cameraFrustum.isBoxInFrustum(minX, 0.0D, minZ,
                    minX + 16.0D, 256.0D, minZ + 16.0D);
        } catch (RuntimeException ignored) {
            return true;
        }
    }
}


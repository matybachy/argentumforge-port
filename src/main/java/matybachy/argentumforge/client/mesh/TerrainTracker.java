package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Phase 3 terrain-tracking utility: dirty chunk invalidation follows block updates and spills to neighboring chunks.
 */
public final class TerrainTracker {
    private final Minecraft mc = Minecraft.getMinecraft();
    private int lastPlayerChunkX = Integer.MIN_VALUE;
    private int lastPlayerChunkZ = Integer.MIN_VALUE;

    public void update(World world, ChunkMeshManager manager) {
        if (world == null || mc.thePlayer == null) {
            return;
        }

        int playerChunkX = (int) Math.floor(mc.thePlayer.posX / 16.0D);
        int playerChunkZ = (int) Math.floor(mc.thePlayer.posZ / 16.0D);

        if (playerChunkX == lastPlayerChunkX && playerChunkZ == lastPlayerChunkZ) {
            return;
        }

        lastPlayerChunkX = playerChunkX;
        lastPlayerChunkZ = playerChunkZ;

        // Invalidate the active radius to keep the chunk manager in sync with the camera.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                manager.markDirty(playerChunkX + dx, playerChunkZ + dz);
            }
        }
    }

    public void onBlockUpdate(World world, BlockPos pos, ChunkMeshManager manager) {
        if (world == null || pos == null || manager == null) {
            return;
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        manager.markDirty(chunkX, chunkZ, 1);
    }
}

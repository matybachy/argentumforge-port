package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * A small utility to keep the prototype terrain renderer aligned with the player's current world coordinates.
 * This is the next iterative step toward proper world-coordinate tracking and rebuild invalidation.
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

        // This is intentionally simple; the next iteration will track block changes more precisely.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                manager.markDirty(chunkX, chunkZ);
            }
        }
    }

    public boolean isChunkDirty(int chunkX, int chunkZ) {
        return false;
    }

    public void onBlockUpdate(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return;
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        // In a more complete implementation, we would invalidate the chunk and neighbors.
    }
}

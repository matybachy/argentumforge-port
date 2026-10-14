package matybachy.argentumforge.client.mesh;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

/**
 * An immutable thread-safe snapshot of a chunk and its 1-block perimeter for background meshing.
 */
public final class ChunkSnapshot {
    private final int chunkX;
    private final int chunkZ;
    private final long revision;
    private final IBlockState[] states; // 18 x 256 x 18 array flattened
    private final byte[] blockLight;
    private final byte[] skyLight;
    private final BiomeGenBase[] biomes; // 16 x 16

    private static final int SIZE_X = 18;
    private static final int SIZE_Z = 18;
    private static final int HEIGHT = 256;

    public ChunkSnapshot(World world, int chunkX, int chunkZ, long revision) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.revision = revision;

        this.states = new IBlockState[SIZE_X * HEIGHT * SIZE_Z];
        this.blockLight = new byte[SIZE_X * HEIGHT * SIZE_Z];
        this.skyLight = new byte[SIZE_X * HEIGHT * SIZE_Z];
        this.biomes = new BiomeGenBase[16 * 16];

        capture(world);
    }

    private void capture(World world) {
        int minX = (chunkX << 4) - 1;
        int minZ = (chunkZ << 4) - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int wx = minX + x;
                int wz = minZ + z;

                if (x >= 1 && x <= 16 && z >= 1 && z <= 16) {
                    BiomeGenBase biome = world.getBiomeGenForCoords(pos.set(wx, 0, wz));
                    biomes[(x - 1) * 16 + (z - 1)] = biome;
                }

                for (int y = 0; y < HEIGHT; y++) {
                    pos.set(wx, y, wz);
                    int idx = (x * SIZE_Z + z) * HEIGHT + y;
                    IBlockState state = world.getBlockState(pos);
                    states[idx] = state != null ? state : Blocks.air.getDefaultState();
                    blockLight[idx] = (byte) world.getLightFromNeighbors(pos);
                    skyLight[idx] = (byte) world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos);
                }
            }
        }
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public long getRevision() {
        return revision;
    }

    public IBlockState getBlockState(BlockPos pos) {
        int relX = pos.getX() - ((chunkX << 4) - 1);
        int relZ = pos.getZ() - ((chunkZ << 4) - 1);
        int y = pos.getY();

        if (relX < 0 || relX >= SIZE_X || relZ < 0 || relZ >= SIZE_Z || y < 0 || y >= HEIGHT) {
            return Blocks.air.getDefaultState();
        }
        int idx = (relX * SIZE_Z + relZ) * HEIGHT + y;
        return states[idx];
    }

    public int getCombinedLight(BlockPos pos) {
        int relX = pos.getX() - ((chunkX << 4) - 1);
        int relZ = pos.getZ() - ((chunkZ << 4) - 1);
        int y = pos.getY();

        if (relX < 0 || relX >= SIZE_X || relZ < 0 || relZ >= SIZE_Z || y < 0 || y >= HEIGHT) {
            return 15 << 20; // Full sky light default
        }
        int idx = (relX * SIZE_Z + relZ) * HEIGHT + y;
        int bl = blockLight[idx] & 0xF;
        int sl = skyLight[idx] & 0xF;
        return (sl << 20) | (bl << 4);
    }
}

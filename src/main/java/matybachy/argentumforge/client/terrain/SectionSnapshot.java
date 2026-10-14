package matybachy.argentumforge.client.terrain;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Render-thread copy of the inputs needed to compile one 16x16x16 render section: block states,
 * raw sky/block light, biomes and tile-entity references for a 20x20x20 region (two blocks of
 * padding, the same footprint vanilla's {@code RegionRenderCache} covers). The arrays are filled
 * once during {@link #capture} and never mutated, so worker threads can read them without touching
 * live {@link Chunk} storage. Tile-entity objects themselves are shared references, exactly as in
 * vanilla's worker-side chunk compilation.
 */
final class SectionSnapshot {
    static final int PADDING = 2;
    static final int SIZE = 16 + PADDING * 2;
    private static final int VOLUME = SIZE * SIZE * SIZE;

    final World world;
    final int originX;
    final int originY;
    final int originZ;
    final boolean hasNoSky;
    final boolean levelsEmpty;
    final GeometryBounds bounds;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final IBlockState[] states;
    private final byte[] skyLight;
    private final byte[] blockLight;
    private final BiomeGenBase[] biomes;
    private final Map<BlockPos, TileEntity> tileEntities;

    private SectionSnapshot(World world, int originX, int originY, int originZ, boolean hasNoSky, boolean levelsEmpty,
                            GeometryBounds bounds, IBlockState[] states, byte[] skyLight, byte[] blockLight,
                            BiomeGenBase[] biomes, Map<BlockPos, TileEntity> tileEntities) {
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.hasNoSky = hasNoSky;
        this.levelsEmpty = levelsEmpty;
        this.bounds = bounds;
        this.minX = originX - PADDING;
        this.minY = originY - PADDING;
        this.minZ = originZ - PADDING;
        this.states = states;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
        this.biomes = biomes;
        this.tileEntities = tileEntities;
    }

    /** Must be called on the client thread; it reads live chunk storage. */
    static SectionSnapshot capture(World world, BlockPos origin) {
        final int originX = origin.getX();
        final int originY = origin.getY();
        final int originZ = origin.getZ();
        final int minX = originX - PADDING;
        final int minY = originY - PADDING;
        final int minZ = originZ - PADDING;
        final int maxX = minX + SIZE - 1;
        final int maxY = minY + SIZE - 1;
        final int maxZ = minZ + SIZE - 1;
        final int chunkMinX = minX >> 4;
        final int chunkMinZ = minZ >> 4;
        final int chunkMaxX = maxX >> 4;
        final int chunkMaxZ = maxZ >> 4;

        Chunk[][] chunks = new Chunk[chunkMaxX - chunkMinX + 1][chunkMaxZ - chunkMinZ + 1];
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                chunks[cx - chunkMinX][cz - chunkMinZ] = world.getChunkFromChunkCoords(cx, cz);
            }
        }

        // Same emptiness rule as ChunkCache for RenderChunk's (origin - 1, origin + 16) request.
        boolean levelsEmpty = true;
        for (int cx = (originX - 1) >> 4; cx <= (originX + 16) >> 4; cx++) {
            for (int cz = (originZ - 1) >> 4; cz <= (originZ + 16) >> 4; cz++) {
                Chunk chunk = chunks[cx - chunkMinX][cz - chunkMinZ];
                if (chunk != null && !chunk.getAreLevelsEmpty(originY - 1, originY + 16)) {
                    levelsEmpty = false;
                }
            }
        }

        IBlockState air = Blocks.air.getDefaultState();
        IBlockState[] states = new IBlockState[VOLUME];
        byte[] sky = new byte[VOLUME];
        byte[] block = new byte[VOLUME];
        Map<BlockPos, TileEntity> tiles = new HashMap<BlockPos, TileEntity>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int gMinX = Integer.MAX_VALUE;
        int gMinY = Integer.MAX_VALUE;
        int gMinZ = Integer.MAX_VALUE;
        int gMaxX = Integer.MIN_VALUE;
        int gMaxY = Integer.MIN_VALUE;
        int gMaxZ = Integer.MIN_VALUE;

        for (int y = 0; y < SIZE; y++) {
            int wy = minY + y;
            boolean inWorld = wy >= 0 && wy < 256;
            boolean innerY = wy >= originY && wy < originY + 16;
            for (int z = 0; z < SIZE; z++) {
                int wz = minZ + z;
                boolean innerYZ = innerY && wz >= originZ && wz < originZ + 16;
                for (int x = 0; x < SIZE; x++) {
                    int wx = minX + x;
                    int index = (y * SIZE + z) * SIZE + x;
                    Chunk chunk = chunks[(wx >> 4) - chunkMinX][(wz >> 4) - chunkMinZ];
                    if (!inWorld || chunk == null) {
                        states[index] = air;
                        sky[index] = (byte) EnumSkyBlock.SKY.defaultLightValue;
                        block[index] = (byte) EnumSkyBlock.BLOCK.defaultLightValue;
                        continue;
                    }

                    cursor.set(wx, wy, wz);
                    IBlockState state = chunk.getBlockState(cursor);
                    states[index] = state;
                    sky[index] = (byte) chunk.getLightFor(EnumSkyBlock.SKY, cursor);
                    block[index] = (byte) chunk.getLightFor(EnumSkyBlock.BLOCK, cursor);

                    if (innerYZ && wx >= originX && wx < originX + 16) {
                        Block blockType = state.getBlock();
                        if (blockType.getRenderType() != -1) {
                            if (wx < gMinX) gMinX = wx;
                            if (wy < gMinY) gMinY = wy;
                            if (wz < gMinZ) gMinZ = wz;
                            if (wx > gMaxX) gMaxX = wx;
                            if (wy > gMaxY) gMaxY = wy;
                            if (wz > gMaxZ) gMaxZ = wz;
                        }
                        if (blockType.hasTileEntity(state)) {
                            // RegionRenderCache uses QUEUED; doing it here keeps creation on the client thread.
                            BlockPos immutable = new BlockPos(wx, wy, wz);
                            TileEntity tile = chunk.getTileEntity(immutable, Chunk.EnumCreateEntityType.QUEUED);
                            if (tile != null) tiles.put(immutable, tile);
                        }
                    }
                }
            }
        }

        // Padding-ring tile entities can be queried by neighbour-dependent block models.
        for (Chunk[] column : chunks) {
            for (Chunk chunk : column) {
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, TileEntity> entry : chunk.getTileEntityMap().entrySet()) {
                    BlockPos pos = entry.getKey();
                    TileEntity tile = entry.getValue();
                    if (pos == null || tile == null || tile.isInvalid() || tiles.containsKey(pos)) continue;
                    if (pos.getX() >= minX && pos.getX() <= maxX
                            && pos.getY() >= minY && pos.getY() <= maxY
                            && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                        tiles.put(pos, tile);
                    }
                }
            }
        }

        BiomeGenBase[] biomes = new BiomeGenBase[SIZE * SIZE];
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                biomes[z * SIZE + x] = world.getBiomeGenForCoords(new BlockPos(minX + x, 64, minZ + z));
            }
        }

        GeometryBounds bounds = gMinX == Integer.MAX_VALUE
                ? null
                : new GeometryBounds(gMinX, gMinY, gMinZ, gMaxX, gMaxY, gMaxZ);
        return new SectionSnapshot(world, originX, originY, originZ, world.provider.getHasNoSky(), levelsEmpty,
                bounds, states, sky, block, biomes, tiles);
    }

    boolean matches(World requestedWorld, BlockPos from) {
        return requestedWorld == world
                && from.getX() == originX - 1
                && from.getY() == originY - 1
                && from.getZ() == originZ - 1;
    }

    boolean contains(int x, int y, int z) {
        return x >= minX && x < minX + SIZE
                && y >= minY && y < minY + SIZE
                && z >= minZ && z < minZ + SIZE;
    }

    boolean containsColumn(int x, int z) {
        return x >= minX && x < minX + SIZE && z >= minZ && z < minZ + SIZE;
    }

    IBlockState getBlockState(int x, int y, int z) {
        return states[index(x, y, z)];
    }

    int getLight(EnumSkyBlock type, int x, int y, int z) {
        if (type == EnumSkyBlock.SKY) return skyLight[index(x, y, z)];
        if (type == EnumSkyBlock.BLOCK) return blockLight[index(x, y, z)];
        return type.defaultLightValue;
    }

    BiomeGenBase getBiome(int x, int z) {
        return biomes[(z - minZ) * SIZE + (x - minX)];
    }

    TileEntity getTileEntity(BlockPos pos) {
        return tileEntities.get(pos);
    }

    private int index(int x, int y, int z) {
        return ((y - minY) * SIZE + (z - minZ)) * SIZE + (x - minX);
    }
}

package matybachy.argentumforge.client.terrain;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * {@link RegionRenderCache} returned from Forge's {@code RenderChunk.createRegionRenderCache} hook.
 * Every lookup the block renderers make inside the captured region is answered from an immutable
 * {@link SectionSnapshot}; the light logic mirrors {@code ChunkCache.getLightForExt}. Requests
 * outside the captured region (not expected from vanilla renderers) fall back to the vanilla cache.
 */
final class SnapshotRegionRenderCache extends RegionRenderCache {
    private final SectionSnapshot snapshot;

    SnapshotRegionRenderCache(World world, BlockPos from, BlockPos to, int subtract, SectionSnapshot snapshot) {
        super(world, from, to, subtract);
        this.snapshot = snapshot;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (y < 0 || y >= 256) return Blocks.air.getDefaultState();
        if (snapshot.contains(x, y, z)) return snapshot.getBlockState(x, y, z);
        recordFallback("getBlockState", x, y, z);
        return super.getBlockState(pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        if (snapshot.contains(pos.getX(), pos.getY(), pos.getZ())) return snapshot.getTileEntity(pos);
        recordFallback("getTileEntity", pos.getX(), pos.getY(), pos.getZ());
        return super.getTileEntity(pos);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!snapshot.contains(x - 1, y - 1, z - 1) || !snapshot.contains(x + 1, y + 1, z + 1)) {
            recordFallback("getCombinedLight", x, y, z);
            return super.getCombinedLight(pos, lightValue);
        }
        int sky = lightForExt(EnumSkyBlock.SKY, x, y, z);
        int block = lightForExt(EnumSkyBlock.BLOCK, x, y, z);
        if (block < lightValue) block = lightValue;
        return sky << 20 | block << 4;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (y < 0 || y >= 256) return type.defaultLightValue;
        if (snapshot.contains(x, y, z)) return snapshot.getLight(type, x, y, z);
        recordFallback("getLightFor", x, y, z);
        return super.getLightFor(type, pos);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
        if (snapshot.containsColumn(pos.getX(), pos.getZ())) {
            BiomeGenBase biome = snapshot.getBiome(pos.getX(), pos.getZ());
            if (biome != null) return biome;
        }
        TerrainStats.recordOutOfSnapshotRead();
        return super.getBiomeGenForCoords(pos);
    }

    /**
     * A read the snapshot cannot answer goes to the vanilla cache. Inside the region that is normal (empty sections
     * store no arrays, light near the region edge); outside it, vanilla's unchecked index would be wrong.
     */
    private void recordFallback(String access, int x, int y, int z) {
        if (!snapshot.inRegion(x, y, z)) {
            TerrainStats.recordOutOfSnapshotRead();
            if (TerrainDebug.isEnabled()) TerrainDebug.reportOutOfRegion(access, x, y, z, snapshot);
        }
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return snapshot.levelsEmpty;
    }

    private int lightForExt(EnumSkyBlock type, int x, int y, int z) {
        if (type == EnumSkyBlock.SKY && snapshot.hasNoSky) return 0;
        if (y < 0 || y >= 256) return type.defaultLightValue;
        if (snapshot.getBlockState(x, y, z).getBlock().getUseNeighborBrightness()) {
            int max = 0;
            for (EnumFacing facing : EnumFacing.values()) {
                int light = rawLight(type, x + facing.getFrontOffsetX(), y + facing.getFrontOffsetY(), z + facing.getFrontOffsetZ());
                if (light > max) max = light;
                if (max >= 15) return max;
            }
            return max;
        }
        return snapshot.getLight(type, x, y, z);
    }

    private int rawLight(EnumSkyBlock type, int x, int y, int z) {
        if (y < 0 || y >= 256) return type.defaultLightValue;
        return snapshot.getLight(type, x, y, z);
    }
}

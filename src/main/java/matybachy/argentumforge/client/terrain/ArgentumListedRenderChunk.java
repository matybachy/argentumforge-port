package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Display-list-mode render section. Vanilla/Forge {@code rebuildChunk} still performs the block, layer, tile-entity
 * and visibility-graph compilation; this subclass only supplies snapshot-backed block access.
 */
final class ArgentumListedRenderChunk extends ListedRenderChunk implements SnapshotRenderChunk {
    // Assigned after the super constructor; super-constructor callbacks must tolerate null.
    private final SnapshotCompileSupport support = new SnapshotCompileSupport();
    private World snapshotWorld;

    ArgentumListedRenderChunk(World world, RenderGlobal renderGlobal, BlockPos pos, int index) {
        super(world, renderGlobal, pos, index);
        this.snapshotWorld = world;
    }

    @Override
    public ChunkCompileTaskGenerator makeCompileTaskChunk() {
        ChunkCompileTaskGenerator task = super.makeCompileTaskChunk();
        if (support != null) support.onCompileTaskCreated(task, snapshotWorld, getPosition());
        return task;
    }

    @Override
    public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGenerator generator) {
        support.beginRebuild(generator);
        try {
            super.rebuildChunk(x, y, z, generator);
        } finally {
            support.finishRebuild(generator);
        }
    }

    @Override
    protected RegionRenderCache createRegionRenderCache(World world, BlockPos from, BlockPos to, int subtract) {
        RegionRenderCache cache = support.createCache(world, from, to, subtract);
        return cache != null ? cache : super.createRegionRenderCache(world, from, to, subtract);
    }

    @Override
    public void stopCompileTask() {
        super.stopCompileTask();
        if (support != null) support.discardPending();
    }

    @Override
    public void deleteGlResources() {
        super.deleteGlResources();
        if (support != null) support.dispose();
        snapshotWorld = null;
    }

    @Override
    public SnapshotCompileSupport getSnapshotSupport() {
        return support;
    }
}

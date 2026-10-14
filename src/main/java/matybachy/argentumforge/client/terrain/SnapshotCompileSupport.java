package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-RenderChunk state shared by the VBO and display-list subclasses.
 *
 * <p>Lifecycle: vanilla creates a compile task on the client thread
 * ({@code ChunkRenderDispatcher.updateChunkLater/updateChunkNow -> makeCompileTaskChunk}). At that
 * point a {@link SectionSnapshot} is captured and paired with that exact task object. When a
 * {@code ChunkRenderWorker} later runs {@code rebuildChunk} for the task, Forge's
 * {@code createRegionRenderCache} hook hands the block renderers a snapshot-backed cache instead of a
 * cache over live chunks. A snapshot is only consumed by the task it was captured for; a newer task,
 * a position change or GL resource deletion discards it. Vanilla's task status checks still decide
 * whether a finished compile is uploaded, so stale output is rejected by the existing dispatcher.</p>
 */
final class SnapshotCompileSupport {
    private static final ThreadLocal<ChunkCompileTaskGenerator> ACTIVE_TASK = new ThreadLocal<ChunkCompileTaskGenerator>();
    private static final ThreadLocal<SectionSnapshot> ACTIVE_SNAPSHOT = new ThreadLocal<SectionSnapshot>();

    private final AtomicReference<Pending> pending = new AtomicReference<Pending>();
    private final Map<CompiledChunk, GeometryBounds> boundsByCompiledChunk =
            Collections.synchronizedMap(new WeakHashMap<CompiledChunk, GeometryBounds>());

    /** Client thread, called with the RenderChunk compile lock held by the dispatcher. */
    void onCompileTaskCreated(ChunkCompileTaskGenerator task, World world, BlockPos origin) {
        discardPending();
        if (task == null || world == null || origin == null) return;
        if (!IntegratedTerrain.tryReserveSnapshot()) return;

        boolean stored = false;
        try {
            pending.set(new Pending(task, SectionSnapshot.capture(world, origin)));
            stored = true;
        } catch (RuntimeException failure) {
            IntegratedTerrain.onSnapshotFailure(failure);
        } finally {
            if (!stored) IntegratedTerrain.releaseSnapshot();
        }
    }

    void beginRebuild(ChunkCompileTaskGenerator task) {
        ACTIVE_TASK.set(task);
        ACTIVE_SNAPSHOT.remove();
    }

    /** Worker thread (or client thread for updateChunkNow), inside rebuildChunk. */
    RegionRenderCache createCache(World world, BlockPos from, BlockPos to, int subtract) {
        ChunkCompileTaskGenerator task = ACTIVE_TASK.get();
        Pending candidate = pending.get();
        if (task == null || candidate == null || candidate.task != task || !pending.compareAndSet(candidate, null)) {
            TerrainStats.recordVanillaRegionBuild();
            return null;
        }
        IntegratedTerrain.releaseSnapshot();
        if (!candidate.snapshot.matches(world, from)) {
            TerrainStats.recordVanillaRegionBuild();
            return null;
        }
        ACTIVE_SNAPSHOT.set(candidate.snapshot);
        TerrainStats.recordSnapshotBuild();
        return new SnapshotRegionRenderCache(world, from, to, subtract, candidate.snapshot);
    }

    void finishRebuild(ChunkCompileTaskGenerator task) {
        try {
            SectionSnapshot used = ACTIVE_SNAPSHOT.get();
            CompiledChunk compiled = task != null ? task.getCompiledChunk() : null;
            if (compiled != null && used != null && used.bounds != null) {
                boundsByCompiledChunk.put(compiled, used.bounds);
            }
            // rebuildChunk returns early for tasks that were cancelled before it started.
            Pending leftover = pending.get();
            if (leftover != null && leftover.task == task && pending.compareAndSet(leftover, null)) {
                IntegratedTerrain.releaseSnapshot();
            }
        } finally {
            ACTIVE_TASK.remove();
            ACTIVE_SNAPSHOT.remove();
        }
    }

    /** Bounds recorded for exactly this compiled result, or null if it came from a vanilla-cache build. */
    GeometryBounds boundsFor(CompiledChunk compiled) {
        return compiled == null ? null : boundsByCompiledChunk.get(compiled);
    }

    void discardPending() {
        if (pending.getAndSet(null) != null) IntegratedTerrain.releaseSnapshot();
    }

    void dispose() {
        discardPending();
        boundsByCompiledChunk.clear();
    }

    private static final class Pending {
        private final ChunkCompileTaskGenerator task;
        private final SectionSnapshot snapshot;

        private Pending(ChunkCompileTaskGenerator task, SectionSnapshot snapshot) {
            this.task = task;
            this.snapshot = snapshot;
        }
    }
}

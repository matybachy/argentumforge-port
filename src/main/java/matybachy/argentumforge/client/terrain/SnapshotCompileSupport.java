package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-RenderChunk state shared by the VBO and display-list subclasses.
 *
 * <p>Lifecycle: vanilla creates a compile task on the client thread
 * ({@code ChunkRenderDispatcher.updateChunkLater/updateChunkNow -> makeCompileTaskChunk}). At that
 * point a {@link SectionSnapshot} is captured and the {@link SnapshotRegionRenderCache} is
 * constructed, both on the client thread, so the RegionRenderCache/ChunkCache superclass constructor
 * (which calls {@code World.getChunkFromChunkCoords} and {@code Chunk.getAreLevelsEmpty}) never runs
 * on a worker. The pair is bound to that exact task object. When a {@code ChunkRenderWorker} later
 * runs {@code rebuildChunk} for the task, Forge's {@code createRegionRenderCache} hook returns the
 * prebuilt cache only if task, world and region bounds match. A newer task, a position change, GL
 * resource deletion or a world unload discards it. Vanilla's task status checks still decide whether
 * a finished compile is uploaded.</p>
 */
final class SnapshotCompileSupport {
    private static final ThreadLocal<ChunkCompileTaskGenerator> ACTIVE_TASK = new ThreadLocal<ChunkCompileTaskGenerator>();
    private static final ThreadLocal<SectionSnapshot> ACTIVE_SNAPSHOT = new ThreadLocal<SectionSnapshot>();
    private static final Set<SnapshotCompileSupport> LIVE =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<SnapshotCompileSupport, Boolean>()));

    private final AtomicReference<Pending> pending = new AtomicReference<Pending>();
    /** Written by workers when a snapshot build finishes; consumed when vanilla installs the result. */
    private final Map<CompiledChunk, GeometryBounds[]> finishedBounds =
            Collections.synchronizedMap(new WeakHashMap<CompiledChunk, GeometryBounds[]>());
    /** Read lock-free by the render containers every frame. */
    private volatile ActiveBounds activeBounds;

    SnapshotCompileSupport() {
        LIVE.add(this);
    }

    /** Client thread, called with the RenderChunk compile lock held by the dispatcher. */
    void onCompileTaskCreated(ChunkCompileTaskGenerator task, World world, BlockPos origin) {
        discardPending();
        if (task == null || world == null || origin == null) return;
        if (!IntegratedTerrain.tryReserveSnapshot()) return;

        boolean stored = false;
        try {
            SectionSnapshot snapshot = SectionSnapshot.capture(world, origin);
            if (snapshot != null) {
                BlockPos from = origin.add(-1, -1, -1);
                BlockPos to = origin.add(16, 16, 16);
                SnapshotRegionRenderCache cache = new SnapshotRegionRenderCache(world, from, to, 1, snapshot);
                pending.set(new Pending(task, snapshot, cache, from, to));
                stored = true;
            }
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
        if (subtract != 1 || candidate.snapshot.world != world || !candidate.from.equals(from) || !candidate.to.equals(to)) {
            TerrainStats.recordVanillaRegionBuild();
            return null;
        }
        ACTIVE_SNAPSHOT.set(candidate.snapshot);
        TerrainStats.recordSnapshotBuild();
        return candidate.cache;
    }

    void finishRebuild(ChunkCompileTaskGenerator task) {
        try {
            SectionSnapshot used = ACTIVE_SNAPSHOT.get();
            CompiledChunk compiled = task != null ? task.getCompiledChunk() : null;
            if (compiled != null && used != null) finishedBounds.put(compiled, used.layerBounds);
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

    /** Called from RenderChunk.setCompiledChunk, which vanilla invokes when a compile result is installed. */
    void onCompiledChunkInstalled(CompiledChunk compiled) {
        GeometryBounds[] bounds = compiled == null ? null : finishedBounds.remove(compiled);
        if (bounds != null) {
            activeBounds = new ActiveBounds(compiled, bounds);
            return;
        }
        // A transparency resort re-installs the same CompiledChunk; anything else has no bounds.
        ActiveBounds current = activeBounds;
        if (current != null && current.owner != compiled) activeBounds = null;
    }

    /** Bounds for exactly this installed compile result and layer, or null (draw). */
    GeometryBounds boundsFor(CompiledChunk compiled, EnumWorldBlockLayer layer) {
        ActiveBounds current = activeBounds;
        if (current == null || compiled == null || current.owner != compiled) return null;
        return current.bounds[layer.ordinal()];
    }

    void discardPending() {
        if (pending.getAndSet(null) != null) IntegratedTerrain.releaseSnapshot();
    }

    void dispose() {
        discardPending();
        finishedBounds.clear();
        activeBounds = null;
        LIVE.remove(this);
    }

    /** Client thread: drops every pending snapshot, e.g. when the client world is unloaded. */
    static void discardAll() {
        SnapshotCompileSupport[] supports;
        synchronized (LIVE) {
            supports = LIVE.toArray(new SnapshotCompileSupport[0]);
        }
        for (SnapshotCompileSupport support : supports) {
            if (support != null) support.discardPending();
        }
    }

    private static final class Pending {
        private final ChunkCompileTaskGenerator task;
        private final SectionSnapshot snapshot;
        private final SnapshotRegionRenderCache cache;
        private final BlockPos from;
        private final BlockPos to;

        private Pending(ChunkCompileTaskGenerator task, SectionSnapshot snapshot, SnapshotRegionRenderCache cache,
                        BlockPos from, BlockPos to) {
            this.task = task;
            this.snapshot = snapshot;
            this.cache = cache;
            this.from = from;
            this.to = to;
        }
    }

    private static final class ActiveBounds {
        private final CompiledChunk owner;
        private final GeometryBounds[] bounds;

        private ActiveBounds(CompiledChunk owner, GeometryBounds[] bounds) {
            this.owner = owner;
            this.bounds = bounds;
        }
    }
}

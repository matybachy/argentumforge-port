package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manages nearby chunk meshes and their render passes. */
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Manages nearby chunk meshes, asynchronous snapshot building, and GPU uploads. */
public final class ChunkMeshManager {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final long FAILED_REBUILD_RETRY_TICKS = 20L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Long, ChunkMesh> meshes = new HashMap<Long, ChunkMesh>();
    private final Set<Long> dirtyChunks = new HashSet<Long>();
    private final Map<Long, Long> retryAfterTick = new HashMap<Long, Long>();
    private final Map<Long, Long> chunkRevisions = new ConcurrentHashMap<Long, Long>();
    private final ConcurrentLinkedQueue<BuildResult> completedBuilds = new ConcurrentLinkedQueue<BuildResult>();

    private final ExecutorService workerExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ArgentumMeshWorker-" + count.getAndIncrement());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
            }
    );

    private SimpleFrustumCuller culler = new SimpleFrustumCuller(8);
    private boolean enabled;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;
    private World worldRef;
    private int dimension = Integer.MIN_VALUE;
    private long clientTick;

    private int getActiveRadius() {
        return ArgentumConfig.renderChunkRadius;
    }

    private int getRebuildBudget() {
        return ArgentumConfig.rebuildBudgetPerTick;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        culler = new SimpleFrustumCuller(Math.max(8, getActiveRadius() + 2));
        if (!enabled) dispose();
    }

    public void markDirty(int chunkX, int chunkZ) {
        markDirty(chunkX, chunkZ, 0);
    }

    public void markDirty(int chunkX, int chunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long id = ChunkCoord.key(chunkX + dx, chunkZ + dz);
                retryAfterTick.remove(id);
                dirtyChunks.add(id);
                Long currentRev = chunkRevisions.get(id);
                chunkRevisions.put(id, Long.valueOf((currentRev != null ? currentRev.longValue() : 0L) + 1L));
            }
        }
    }

    /** Marks a block's chunk and adjacent chunks dirty for boundary-face changes. */
    public void markBlockDirty(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        markDirty(chunkX, chunkZ);
        if ((blockX & 15) == 0) markDirty(chunkX - 1, chunkZ);
        if ((blockX & 15) == 15) markDirty(chunkX + 1, chunkZ);
        if ((blockZ & 15) == 0) markDirty(chunkX, chunkZ - 1);
        if ((blockZ & 15) == 15) markDirty(chunkX, chunkZ + 1);
    }

    public void rebuildChunk(int chunkX, int chunkZ, World world) {
        if (world == null || !world.getChunkProvider().chunkExists(chunkX, chunkZ)) return;
        long id = ChunkCoord.key(chunkX, chunkZ);
        Long rev = chunkRevisions.get(id);
        long targetRev = rev != null ? rev.longValue() : 1L;

        try {
            final ChunkSnapshot snapshot = new ChunkSnapshot(world, chunkX, chunkZ, targetRev);
            dirtyChunks.remove(id);
            retryAfterTick.remove(id);

            workerExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        BlockFaceMesher mesher = new BlockFaceMesher();
                        BlockFaceMesher.MeshData data = mesher.buildChunkMesh(world, snapshot.getChunkX(), snapshot.getChunkZ());
                        completedBuilds.add(new BuildResult(id, snapshot.getChunkX(), snapshot.getChunkZ(), snapshot.getRevision(), data, null));
                    } catch (Throwable t) {
                        completedBuilds.add(new BuildResult(id, snapshot.getChunkX(), snapshot.getChunkZ(), snapshot.getRevision(), null, t));
                    }
                }
            });
        } catch (Throwable t) {
            retryAfterTick.put(id, Long.valueOf(clientTick + FAILED_REBUILD_RETRY_TICKS));
        }
    }

    public void onWorldChanged(World world) {
        if (world == null) return;
        if (worldRef != world || dimension != world.provider.getDimensionId()) {
            dispose();
            worldRef = world;
            dimension = world.provider.getDimensionId();
        }
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        long id = ChunkCoord.key(chunkX, chunkZ);
        ChunkMesh mesh = meshes.remove(id);
        if (mesh != null) mesh.dispose();
        dirtyChunks.remove(id);
        retryAfterTick.remove(id);
    }

    public void onTick() {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) return;

        clientTick++;
        onWorldChanged(mc.theWorld);
        processCompletedBuilds();
        int centerX = MathHelper.floor_double(mc.thePlayer.posX) >> 4;
        int centerZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
        World world = mc.theWorld;
        int activeRadius = getActiveRadius();

        for (int dx = -activeRadius; dx <= activeRadius; dx++) {
            for (int dz = -activeRadius; dz <= activeRadius; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                long key = ChunkCoord.key(cx, cz);
                if (world.getChunkProvider().chunkExists(cx, cz)
                        && !meshes.containsKey(key)
                        && !isWaitingToRetry(key)) {
                    dirtyChunks.add(key);
                }
            }
        }

        if (lastCenterX != centerX || lastCenterZ != centerZ) {
            lastCenterX = centerX;
            lastCenterZ = centerZ;
            unloadOutside(centerX, centerZ);
        }
        prunePending(centerX, centerZ, activeRadius + 1);

        List<Long> sorted = prioritizedDirty(centerX, centerZ, activeRadius + 1);
        int rebuilt = 0;
        int budget = getRebuildBudget();
        for (Long id : sorted) {
            if (rebuilt >= budget) break;
            int cx = ChunkCoord.chunkX(id.longValue());
            int cz = ChunkCoord.chunkZ(id.longValue());
            if (Math.abs(cx - centerX) <= activeRadius
                    && Math.abs(cz - centerZ) <= activeRadius
                    && world.getChunkProvider().chunkExists(cx, cz)) {
                rebuildChunk(cx, cz, world);
                rebuilt++;
            }
        }
    }

    private List<Long> prioritizedDirty(int centerX, int centerZ, int candidateRadius) {
        List<Long> ids = new ArrayList<Long>();
        for (Long id : dirtyChunks) {
            int cx = ChunkCoord.chunkX(id.longValue());
            int cz = ChunkCoord.chunkZ(id.longValue());
            if (Math.abs(cx - centerX) <= candidateRadius
                    && Math.abs(cz - centerZ) <= candidateRadius
                    && !isWaitingToRetry(id.longValue())) {
                ids.add(id);
            }
        }
        Collections.sort(ids, new Comparator<Long>() {
            @Override
            public int compare(Long left, Long right) {
                double ld = distanceSq(left.longValue(), centerX, centerZ);
                double rd = distanceSq(right.longValue(), centerX, centerZ);
                return Double.compare(ld, rd);
            }
        });
        return ids;
    }

    private double distanceSq(long id, int centerX, int centerZ) {
        return ChunkCoord.distanceSq(ChunkCoord.chunkX(id), ChunkCoord.chunkZ(id), centerX, centerZ);
    }

    private boolean isWaitingToRetry(long id) {
        Long retryTick = retryAfterTick.get(Long.valueOf(id));
        if (retryTick == null) return false;
        if (clientTick < retryTick.longValue()) return true;
        retryAfterTick.remove(Long.valueOf(id));
        return false;
    }

    private void prunePending(int centerX, int centerZ, int radius) {
        for (Iterator<Long> it = dirtyChunks.iterator(); it.hasNext();) {
            long id = it.next().longValue();
            if (Math.abs(ChunkCoord.chunkX(id) - centerX) > radius
                    || Math.abs(ChunkCoord.chunkZ(id) - centerZ) > radius) {
                it.remove();
                retryAfterTick.remove(Long.valueOf(id));
            }
        }
        for (Iterator<Long> it = retryAfterTick.keySet().iterator(); it.hasNext();) {
            long id = it.next().longValue();
            if (Math.abs(ChunkCoord.chunkX(id) - centerX) > radius
                    || Math.abs(ChunkCoord.chunkZ(id) - centerZ) > radius) {
                it.remove();
            }
        }
    }

    private void unloadOutside(int centerX, int centerZ) {
        int activeRadius = getActiveRadius();
        for (Iterator<Map.Entry<Long, ChunkMesh>> it = meshes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, ChunkMesh> entry = it.next();
            int cx = ChunkCoord.chunkX(entry.getKey().longValue());
            int cz = ChunkCoord.chunkZ(entry.getKey().longValue());
            if (Math.abs(cx - centerX) > activeRadius || Math.abs(cz - centerZ) > activeRadius) {
                entry.getValue().dispose();
                it.remove();
            }
        }
    }

    public void render() {
        if (!enabled || mc.theWorld == null || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();
        List<Map.Entry<Long, ChunkMesh>> visible = visibleMeshes(view);

        GLState.pushRenderState();
        GLState.pushMatrix();
        try {
            GLState.beginOpaquePass();
            for (Map.Entry<Long, ChunkMesh> entry : visible) entry.getValue().renderSolid();

            GLState.beginCutoutPass();
            for (Map.Entry<Long, ChunkMesh> entry : visible) entry.getValue().renderCutout();

            Collections.sort(visible, new DistanceComparator(view));
            GLState.beginTranslucentPass();
            for (Map.Entry<Long, ChunkMesh> entry : visible) entry.getValue().renderTranslucent();
        } finally {
            GLState.popMatrix();
            GLState.popRenderState();
        }
    }

    /** Capture camera planes before ArgentumRenderer applies its overlay translation. */
    public void prepareVisibility(Entity view, double cameraX, double cameraY, double cameraZ) {
        if (view != null) culler.prepare(view, cameraX, cameraY, cameraZ);
    }

    private List<Map.Entry<Long, ChunkMesh>> visibleMeshes(Entity view) {
        List<Map.Entry<Long, ChunkMesh>> result = new ArrayList<Map.Entry<Long, ChunkMesh>>();
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            int cx = ChunkCoord.chunkX(entry.getKey().longValue());
            int cz = ChunkCoord.chunkZ(entry.getKey().longValue());
            if (culler.shouldRenderChunk(view, cx, cz)) result.add(entry);
        }
        return result;
    }

    private void processCompletedBuilds() {
        BuildResult result;
        while ((result = completedBuilds.poll()) != null) {
            long id = result.id;
            Long currentRev = chunkRevisions.get(id);

            if (result.error != null) {
                ChunkMesh stale = meshes.remove(id);
                if (stale != null) stale.dispose();
                retryAfterTick.put(id, Long.valueOf(clientTick + FAILED_REBUILD_RETRY_TICKS));
                LOGGER.warn("Custom chunk mesh rebuild failed for chunk {}, {}; vanilla terrain remains available", result.chunkX, result.chunkZ, result.error);
                continue;
            }

            if (currentRev != null && currentRev.longValue() > result.revision) {
                // Discard stale build
                continue;
            }

            ChunkMesh replacement = new ChunkMesh(result.chunkX, result.chunkZ);
            replacement.uploadData(result.meshData);
            ChunkMesh previous = meshes.put(id, replacement);
            if (previous != null) previous.dispose();
        }
    }

    public void dispose() {
        for (ChunkMesh mesh : meshes.values()) mesh.dispose();
        meshes.clear();
        dirtyChunks.clear();
        retryAfterTick.clear();
        chunkRevisions.clear();
        completedBuilds.clear();
        clientTick = 0L;
        lastCenterX = Integer.MIN_VALUE;
        lastCenterZ = Integer.MIN_VALUE;
        worldRef = null;
        dimension = Integer.MIN_VALUE;
    }

    private static final class BuildResult {
        private final long id;
        private final int chunkX;
        private final int chunkZ;
        private final long revision;
        private final BlockFaceMesher.MeshData meshData;
        private final Throwable error;

        private BuildResult(long id, int chunkX, int chunkZ, long revision, BlockFaceMesher.MeshData meshData, Throwable error) {
            this.id = id;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.revision = revision;
            this.meshData = meshData;
            this.error = error;
        }
    }

    private static final class DistanceComparator implements Comparator<Map.Entry<Long, ChunkMesh>> {
        private final double viewX;
        private final double viewZ;

        private DistanceComparator(Entity view) {
            viewX = view.posX;
            viewZ = view.posZ;
        }

        @Override
        public int compare(Map.Entry<Long, ChunkMesh> left, Map.Entry<Long, ChunkMesh> right) {
            return Double.compare(distance(right.getKey()), distance(left.getKey()));
        }

        private double distance(long id) {
            int cx = ChunkCoord.chunkX(id);
            int cz = ChunkCoord.chunkZ(id);
            double dx = (cx << 4) + 8.0D - viewX;
            double dz = (cz << 4) + 8.0D - viewZ;
            return dx * dx + dz * dz;
        }
    }
}


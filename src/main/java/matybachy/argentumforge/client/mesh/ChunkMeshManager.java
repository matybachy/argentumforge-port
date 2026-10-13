package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

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
public final class ChunkMeshManager {
    private static final int ACTIVE_RADIUS = 2;
    private static final int MAX_REBUILDS_PER_TICK = 2;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Long, ChunkMesh> meshes = new HashMap<Long, ChunkMesh>();
    private final Set<Long> dirtyChunks = new HashSet<Long>();
    private final SimpleFrustumCuller culler = new SimpleFrustumCuller(8);
    private boolean enabled;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) dispose();
    }

    public void markDirty(int chunkX, int chunkZ) {
        markDirty(chunkX, chunkZ, 0);
    }

    public void markDirty(int chunkX, int chunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                dirtyChunks.add(key(chunkX + dx, chunkZ + dz));
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
        long id = key(chunkX, chunkZ);
        ChunkMesh old = meshes.remove(id);
        if (old != null) old.dispose();

        ChunkMesh mesh = new ChunkMesh(chunkX, chunkZ);
        mesh.build(world);
        meshes.put(id, mesh);
        dirtyChunks.remove(id);
    }

    public void onTick() {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) return;

        int centerX = MathHelper.floor_double(mc.thePlayer.posX) >> 4;
        int centerZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
        World world = mc.theWorld;

        for (int dx = -ACTIVE_RADIUS; dx <= ACTIVE_RADIUS; dx++) {
            for (int dz = -ACTIVE_RADIUS; dz <= ACTIVE_RADIUS; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                if (!meshes.containsKey(key(cx, cz))) dirtyChunks.add(key(cx, cz));
            }
        }

        if (lastCenterX != centerX || lastCenterZ != centerZ) {
            lastCenterX = centerX;
            lastCenterZ = centerZ;
            unloadOutside(centerX, centerZ);
        }

        int rebuilt = 0;
        for (Iterator<Long> it = dirtyChunks.iterator(); it.hasNext() && rebuilt < MAX_REBUILDS_PER_TICK;) {
            long id = it.next();
            int cx = (int) (id >> 32);
            int cz = (int) id;
            if (Math.abs(cx - centerX) <= ACTIVE_RADIUS && Math.abs(cz - centerZ) <= ACTIVE_RADIUS) {
                rebuildChunk(cx, cz, world);
                rebuilt++;
            }
        }
    }

    private void unloadOutside(int centerX, int centerZ) {
        for (Iterator<Map.Entry<Long, ChunkMesh>> it = meshes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, ChunkMesh> entry = it.next();
            int cx = (int) (entry.getKey() >> 32);
            int cz = (int) (long) entry.getKey();
            if (Math.abs(cx - centerX) > ACTIVE_RADIUS || Math.abs(cz - centerZ) > ACTIVE_RADIUS) {
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
            GLState.translate(-view.posX, -view.posY, -view.posZ);
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

    private List<Map.Entry<Long, ChunkMesh>> visibleMeshes(Entity view) {
        List<Map.Entry<Long, ChunkMesh>> result = new ArrayList<Map.Entry<Long, ChunkMesh>>();
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            int cx = (int) (entry.getKey() >> 32);
            int cz = (int) (long) entry.getKey();
            if (culler.shouldRenderChunk(view, cx, cz)) result.add(entry);
        }
        return result;
    }

    private long key(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    public void dispose() {
        for (ChunkMesh mesh : meshes.values()) mesh.dispose();
        meshes.clear();
        dirtyChunks.clear();
        lastCenterX = Integer.MIN_VALUE;
        lastCenterZ = Integer.MIN_VALUE;
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
            int cx = (int) (id >> 32);
            int cz = (int) (long) id;
            double dx = (cx << 4) + 8.0D - viewX;
            double dz = (cz << 4) + 8.0D - viewZ;
            return dx * dx + dz * dz;
        }
    }
}

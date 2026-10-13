package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages nearby chunk meshes and renders opaque, cutout, and translucent layers.
 * Translucent chunks are drawn back-to-front using a coarse chunk-center sort.
 */
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

        int chunkX = MathHelper.floor_double(mc.thePlayer.posX) >> 4;
        int chunkZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4;
        World world = mc.theWorld;

        for (int dx = -ACTIVE_RADIUS; dx <= ACTIVE_RADIUS; dx++) {
            for (int dz = -ACTIVE_RADIUS; dz <= ACTIVE_RADIUS; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                if (!meshes.containsKey(key(cx, cz))) markDirty(cx, cz);
            }
        }

        if (lastCenterX != chunkX || lastCenterZ != chunkZ) {
            lastCenterX = chunkX;
            lastCenterZ = chunkZ;
            for (Iterator<Map.Entry<Long, ChunkMesh>> it = meshes.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Long, ChunkMesh> entry = it.next();
                int cx = (int) (entry.getKey() >> 32);
                int cz = (int) (long) entry.getKey();
                if (Math.abs(cx - chunkX) > ACTIVE_RADIUS || Math.abs(cz - chunkZ) > ACTIVE_RADIUS) {
                    entry.getValue().dispose();
                    it.remove();
                }
            }
        }

        int rebuilt = 0;
        for (Iterator<Long> it = dirtyChunks.iterator(); it.hasNext() && rebuilt < MAX_REBUILDS_PER_TICK;) {
            long id = it.next();
            int cx = (int) (id >> 32);
            int cz = (int) id;
            if (Math.abs(cx - chunkX) <= ACTIVE_RADIUS && Math.abs(cz - chunkZ) <= ACTIVE_RADIUS) {
                rebuildChunk(cx, cz, world);
                rebuilt++;
            }
        }
    }

    public void render() {
        if (!enabled || mc.theWorld == null || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();
        List<Map.Entry<Long, ChunkMesh>> visible = visibleMeshes(view);

        GLState.pushRenderState();
        GLState.pushMatrix();
        GLState.translate(-view.posX, -view.posY, -view.posZ);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GLState.beginOpaquePass();
        for (Map.Entry<Long, ChunkMesh> entry : visible) {
            entry.getValue().renderSolid();
        }

        GLState.beginCutoutPass();
        for (Map.Entry<Long, ChunkMesh> entry : visible) {
            entry.getValue().renderCutout();
        }

        Collections.sort(visible, new TranslucentDistanceComparator(view));
        GLState.beginTranslucentPass();
        for (Map.Entry<Long, ChunkMesh> entry : visible) {
            entry.getValue().renderTranslucent();
        }

        GLState.popMatrix();
        GLState.popRenderState();
    }

    private List<Map.Entry<Long, ChunkMesh>> visibleMeshes(Entity view) {
        List<Map.Entry<Long, ChunkMesh>> result = new ArrayList<Map.Entry<Long, ChunkMesh>>();
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            if (visible(view, entry.getKey())) result.add(entry);
        }
        return result;
    }

    private boolean visible(Entity view, long id) {
        return culler.shouldRenderChunk(view, (int) (id >> 32), (int) id);
    }

    private long key(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    public void dispose() {
        for (ChunkMesh mesh : meshes.values()) mesh.dispose();
        meshes.clear();
        dirtyChunks.clear();
    }

    private static final class TranslucentDistanceComparator implements Comparator<Map.Entry<Long, ChunkMesh>> {
        private final double viewX;
        private final double viewZ;

        private TranslucentDistanceComparator(Entity view) {
            this.viewX = view.posX;
            this.viewZ = view.posZ;
        }

        @Override
        public int compare(Map.Entry<Long, ChunkMesh> left, Map.Entry<Long, ChunkMesh> right) {
            long leftKey = left.getKey();
            long rightKey = right.getKey();
            double leftDistance = distanceSquared(leftKey);
            double rightDistance = distanceSquared(rightKey);
            return Double.compare(rightDistance, leftDistance);
        }

        private double distanceSquared(long id) {
            int chunkX = (int) (id >> 32);
            int chunkZ = (int) id;
            double centerX = (chunkX << 4) + 8.0D;
            double centerZ = (chunkZ << 4) + 8.0D;
            double dx = centerX - viewX;
            double dz = centerZ - viewZ;
            return dx * dx + dz * dz;
        }
    }
}

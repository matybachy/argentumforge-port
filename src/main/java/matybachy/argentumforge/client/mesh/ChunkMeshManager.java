package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Manages ChunkMesh instances for nearby chunks. Builds simple meshes on demand and renders them.
 * This is the next incremental step beyond a static prototype: rebuilds are now tracked as dirty chunks,
 * so the manager can invalidate and rebuild chunks without rebuilding the full radius every tick.
 */
public final class ChunkMeshManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Long, ChunkMesh> meshes = new HashMap<Long, ChunkMesh>();
    private final Set<Long> dirtyChunks = new HashSet<Long>();
    private final SimpleFrustumCuller culler = new SimpleFrustumCuller(8);
    private boolean enabled = false;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void markDirty(int chunkX, int chunkZ) {
        long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        dirtyChunks.add(key);
    }

    public void rebuildChunk(int chunkX, int chunkZ, World world) {
        long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        ChunkMesh mesh = new ChunkMesh(chunkX, chunkZ);
        mesh.build(world);
        meshes.put(key, mesh);
        dirtyChunks.remove(key);
    }

    public void onTick() {
        if (!enabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);
        int chunkX = px >> 4;
        int chunkZ = pz >> 4;

        // Maintain a small radius, but only rebuild dirty chunks to avoid unnecessary work.
        int radius = 2;
        World world = mc.theWorld;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
                if (!meshes.containsKey(key)) {
                    markDirty(cx, cz);
                }
            }
        }

        if (lastCenterX != chunkX || lastCenterZ != chunkZ) {
            lastCenterX = chunkX;
            lastCenterZ = chunkZ;
            for (Iterator<Map.Entry<Long, ChunkMesh>> it = meshes.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Long, ChunkMesh> entry = it.next();
                long key = entry.getKey();
                int cx = (int) (key >> 32);
                int cz = (int) key;
                if (Math.abs(cx - chunkX) > radius || Math.abs(cz - chunkZ) > radius) {
                    entry.getValue().dispose();
                    it.remove();
                }
            }
        }

        for (Iterator<Long> it = dirtyChunks.iterator(); it.hasNext();) {
            long key = it.next();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            // Only rebuild chunks near the player.
            if (Math.abs(cx - chunkX) <= radius && Math.abs(cz - chunkZ) <= radius) {
                rebuildChunk(cx, cz, world);
            }
        }
    }

    public void render() {
        if (!enabled) return;
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();
        double px = view.posX;
        double py = view.posY;
        double pz = view.posZ;

        GLState.pushMatrix();
        GLState.translate(-px, -py, -pz);

        Iterator<Map.Entry<Long, ChunkMesh>> it = meshes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ChunkMesh> entry = it.next();
            long key = entry.getKey();
            int chunkX = (int) (key >> 32);
            int chunkZ = (int) key;
            if (!culler.shouldRenderChunk(view, chunkX, chunkZ)) {
                continue;
            }
            entry.getValue().render();
        }

        GLState.popMatrix();
    }

    public void dispose() {
        for (ChunkMesh mesh : meshes.values()) {
            mesh.dispose();
        }
        meshes.clear();
        dirtyChunks.clear();
    }
}

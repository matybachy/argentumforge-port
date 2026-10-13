package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages ChunkMesh instances for nearby chunks. Builds simple meshes on demand and renders them.
 */
public final class ChunkMeshManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Long, ChunkMesh> meshes = new HashMap<Long, ChunkMesh>();
    private final SimpleFrustumCuller culler = new SimpleFrustumCuller(8);
    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void rebuildChunk(int chunkX, int chunkZ, World world) {
        long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        ChunkMesh mesh = new ChunkMesh(chunkX, chunkZ);
        mesh.build(world);
        meshes.put(key, mesh);
    }

    public void onTick() {
        if (!enabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);
        int chunkX = px >> 4;
        int chunkZ = pz >> 4;
        int radius = 2;
        World world = mc.theWorld;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
                if (!meshes.containsKey(key)) {
                    rebuildChunk(cx, cz, world);
                }
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
    }
}

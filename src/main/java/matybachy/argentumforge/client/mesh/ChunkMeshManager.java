package matybachy.argentumforge.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Manages nearby chunk meshes and renders them in opaque, cutout, then translucent passes. */
public final class ChunkMeshManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Long, ChunkMesh> meshes = new HashMap<Long, ChunkMesh>();
    private final Set<Long> dirtyChunks = new HashSet<Long>();
    private final SimpleFrustumCuller culler = new SimpleFrustumCuller(8);
    private boolean enabled;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void markDirty(int chunkX, int chunkZ) { markDirty(chunkX, chunkZ, 0); }

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
        int radius = 2;
        World world = mc.theWorld;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
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
                if (Math.abs(cx - chunkX) > radius || Math.abs(cz - chunkZ) > radius) {
                    entry.getValue().dispose();
                    it.remove();
                }
            }
        }

        for (Iterator<Long> it = dirtyChunks.iterator(); it.hasNext();) {
            long id = it.next();
            int cx = (int) (id >> 32);
            int cz = (int) id;
            if (Math.abs(cx - chunkX) <= radius && Math.abs(cz - chunkZ) <= radius) {
                rebuildChunk(cx, cz, world);
            }
        }
    }

    public void render() {
        if (!enabled || mc.theWorld == null || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();

        GLState.pushMatrix();
        GLState.translate(-view.posX, -view.posY, -view.posZ);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            if (visible(view, entry.getKey())) entry.getValue().renderSolid();
        }

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.5f);
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            if (visible(view, entry.getKey())) entry.getValue().renderCutout();
        }
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        for (Map.Entry<Long, ChunkMesh> entry : meshes.entrySet()) {
            if (visible(view, entry.getKey())) entry.getValue().renderTranslucent();
        }
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);

        GLState.popMatrix();
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
}

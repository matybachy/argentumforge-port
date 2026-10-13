package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * A chunk mesh containing separate solid and translucent VBOs.
 * This is the next incremental step toward a real terrain renderer while still preserving a simple compatibility layer.
 */
public final class ChunkMesh {
    private final int chunkX;
    private final int chunkZ;
    private final GlVbo solidVbo = new GlVbo();
    private final GlVbo translucentVbo = new GlVbo();
    private final BlockFaceMesher mesher = new BlockFaceMesher();
    private boolean built = false;

    public ChunkMesh(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void build(World world) {
        BlockFaceMesher.MeshData data = mesher.buildChunkMesh(world, chunkX, chunkZ);
        solidVbo.upload(data.getSolidVertices());
        translucentVbo.upload(data.getTranslucentVertices());
        built = true;
    }

    /**
     * Draw only the solid geometry. GL state (textures, blending) must be set by the caller.
     */
    public void renderSolid() {
        if (!built) return;
        solidVbo.draw();
    }

    /**
     * Draw only the translucent geometry. Caller must set blending and depth write state appropriately.
     */
    public void renderTranslucent() {
        if (!built) return;
        translucentVbo.draw();
    }

    /**
     * Convenience: draw both layers (solid then translucent) using default simple state.
     */
    public void render() {
        if (!built) return;

        // default simple ordering
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        renderSolid();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        renderTranslucent();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public void dispose() {
        solidVbo.delete();
        translucentVbo.delete();
        built = false;
    }
}

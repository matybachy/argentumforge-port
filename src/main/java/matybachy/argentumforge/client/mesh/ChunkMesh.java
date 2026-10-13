package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Chunk mesh with opaque, alpha-tested cutout, and blended translucent VBOs. */
public final class ChunkMesh {
    private final int chunkX;
    private final int chunkZ;
    private final GlVbo solidVbo = new GlVbo();
    private final GlVbo cutoutVbo = new GlVbo();
    private final GlVbo translucentVbo = new GlVbo();
    private final BlockFaceMesher mesher = new BlockFaceMesher();
    private boolean built;

    public ChunkMesh(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void build(World world) {
        BlockFaceMesher.MeshData data = mesher.buildChunkMesh(world, chunkX, chunkZ);
        solidVbo.upload(data.getSolidVertices());
        cutoutVbo.upload(data.getCutoutVertices());
        translucentVbo.upload(data.getTranslucentVertices());
        built = true;
    }

    public void renderSolid() {
        if (built) solidVbo.draw();
    }

    public void renderCutout() {
        if (built) cutoutVbo.draw();
    }

    public void renderTranslucent() {
        if (built) translucentVbo.draw();
    }

    public void render() {
        if (!built) return;
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        renderSolid();

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.5f);
        renderCutout();
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        renderTranslucent();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public void dispose() {
        solidVbo.delete();
        cutoutVbo.delete();
        translucentVbo.delete();
        built = false;
    }
}

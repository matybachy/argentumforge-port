package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.client.compat.GpuBuffer;
import net.minecraft.world.World;

/** Chunk mesh containing opaque, alpha-tested cutout, and blended translucent VBOs. */
public final class ChunkMesh {
    private final int chunkX;
    private final int chunkZ;
    private final GpuBuffer solidVbo = new GlVbo();
    private final GpuBuffer cutoutVbo = new GlVbo();
    private final GpuBuffer translucentVbo = new GlVbo();
    private final BlockFaceMesher mesher = new BlockFaceMesher();
    private boolean built;

    public ChunkMesh(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void build(World world) {
        if (world == null) {
            built = false;
            return;
        }

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

    public void dispose() {
        solidVbo.delete();
        cutoutVbo.delete();
        translucentVbo.delete();
        built = false;
    }
}

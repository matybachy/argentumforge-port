package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;

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

    public void render() {
        if (!built) return;

        // render solid first
        solidVbo.draw();

        // render translucent second (simple alpha overlay)
        translucentVbo.draw();
    }

    public void dispose() {
        solidVbo.delete();
        translucentVbo.delete();
        built = false;
    }
}

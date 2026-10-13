package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;

/**
 * A lightweight chunk mesh that holds a VBO for a simple mesh representation.
 * This prototype builds a simple face mesh; future work will add UVs, lighting and per-layer rendering.
 */
public final class ChunkMesh {
    private final int chunkX;
    private final int chunkZ;
    private final GlVbo vbo = new GlVbo();
    private final BlockFaceMesher mesher = new BlockFaceMesher();
    private boolean built = false;

    public ChunkMesh(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void build(World world) {
        float[] verts = mesher.buildChunkMesh(world, chunkX, chunkZ);
        vbo.upload(verts);
        built = true;
    }

    public void render() {
        if (!built) return;
        vbo.draw();
    }

    public void dispose() {
        vbo.delete();
        built = false;
    }
}

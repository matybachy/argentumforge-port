package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;

/**
 * A lightweight chunk mesh that holds a VBO for a simple mesh representation.
 * Currently this creates a single top-face per column (heightmap) as a proof of concept.
 */
public final class ChunkMesh {
    private final int chunkX;
    private final int chunkZ;
    private final GlVbo vbo = new GlVbo();
    private boolean built = false;

    public ChunkMesh(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /**
     * Build mesh data for the given world. This is a simple heightmap-based mesh: one quad per column.
     * Replace with full block-face meshing later.
     */
    public void build(World world) {
        // Build a small proof-of-concept mesh: for each column x=0..15,z=0..15 compute top Y and emit a quad.
        float[] verts = new float[16 * 16 * 6 * 7]; // 6 vertices (two triangles) * 7 floats per vertex
        int idx = 0;
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int worldX = baseX + dx;
                int worldZ = baseZ + dz;
                int topY = world.getHeightValue(worldX, worldZ);
                float x0 = worldX;
                float z0 = worldZ;
                float x1 = worldX + 1;
                float z1 = worldZ + 1;
                float y = topY;
                // two triangles: (x0,y,z0)-(x1,y,z0)-(x1,y,z1) and (x0,y,z0)-(x1,y,z1)-(x0,y,z1)
                float r = 0.4f, g = 0.8f, b = 0.4f, a = 0.9f;
                // tri 1
                verts[idx++] = x0; verts[idx++] = y; verts[idx++] = z0; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
                verts[idx++] = x1; verts[idx++] = y; verts[idx++] = z0; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
                verts[idx++] = x1; verts[idx++] = y; verts[idx++] = z1; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
                // tri 2
                verts[idx++] = x0; verts[idx++] = y; verts[idx++] = z0; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
                verts[idx++] = x1; verts[idx++] = y; verts[idx++] = z1; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
                verts[idx++] = x0; verts[idx++] = y; verts[idx++] = z1; verts[idx++] = r; verts[idx++] = g; verts[idx++] = b; verts[idx++] = a;
            }
        }
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

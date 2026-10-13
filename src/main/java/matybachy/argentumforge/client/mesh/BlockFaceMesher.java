package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal block-face mesher used to replace the heightmap prototype.
 * It emits visible faces for blocks in a chunk using simple color vertices.
 */
public final class BlockFaceMesher {
    public float[] buildChunkMesh(World world, int chunkX, int chunkZ) {
        List<Float> verts = new ArrayList<Float>();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    int wx = baseX + x;
                    int wy = y;
                    int wz = baseZ + z;
                    if (world.isAirBlock(wx, wy, wz)) continue;
                    Block block = world.getBlock(wx, wy, wz);
                    if (block == null) continue;

                    if (isFaceVisible(world, wx + 1, wy, wz)) emitFace(verts, wx + 1, wy, wz, -1, 0, 0, block);
                    if (isFaceVisible(world, wx - 1, wy, wz)) emitFace(verts, wx, wy, wz, 1, 0, 0, block);
                    if (isFaceVisible(world, wx, wy + 1, wz)) emitFace(verts, wx, wy + 1, wz, 0, -1, 0, block);
                    if (isFaceVisible(world, wx, wy - 1, wz)) emitFace(verts, wx, wy, wz, 0, 1, 0, block);
                    if (isFaceVisible(world, wx, wy, wz + 1)) emitFace(verts, wx, wy, wz + 1, 0, 0, -1, block);
                    if (isFaceVisible(world, wx, wy, wz - 1)) emitFace(verts, wx, wy, wz, 0, 0, 1, block);
                }
            }
        }

        float[] out = new float[verts.size()];
        for (int i = 0; i < verts.size(); i++) {
            out[i] = verts.get(i);
        }
        return out;
    }

    private boolean isFaceVisible(World world, int nx, int ny, int nz) {
        if (ny < 0 || ny >= world.getHeight()) return true;
        return world.isAirBlock(nx, ny, nz);
    }

    private void emitFace(List<Float> verts, int px, int py, int pz, int nx, int ny, int nz, Block block) {
        float r = block.getMaterial().isLiquid() ? 0.2f : 0.6f;
        float g = block.getMaterial().isSolid() ? 0.8f : 0.4f;
        float b = 0.4f + (Math.abs(block.hashCode()) % 100) / 250.0f;
        float a = 1.0f;

        float x0 = px - (nx == 1 ? 1 : 0);
        float x1 = x0 + 1.0f;
        float y0 = py - (ny == 1 ? 1 : 0);
        float y1 = y0 + 1.0f;
        float z0 = pz - (nz == 1 ? 1 : 0);
        float z1 = z0 + 1.0f;

        pushVertex(verts, x0, y0, z0, r, g, b, a);
        pushVertex(verts, x1, y0, z0, r, g, b, a);
        pushVertex(verts, x1, y1, z1, r, g, b, a);

        pushVertex(verts, x0, y0, z0, r, g, b, a);
        pushVertex(verts, x1, y1, z1, r, g, b, a);
        pushVertex(verts, x0, y1, z1, r, g, b, a);
    }

    private void pushVertex(List<Float> verts, float x, float y, float z, float r, float g, float b, float a) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(r);
        verts.add(g);
        verts.add(b);
        verts.add(a);
    }
}

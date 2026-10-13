package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunk mesher with separate opaque, alpha-tested cutout, and blended translucent geometry.
 * Vertex layout: x, y, z, u, v, r, g, b, a.
 */
public final class BlockFaceMesher {
    public MeshData buildChunkMesh(World world, int chunkX, int chunkZ) {
        List<Float> solid = new ArrayList<Float>();
        List<Float> cutout = new ArrayList<Float>();
        List<Float> translucent = new ArrayList<Float>();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    int wx = baseX + x;
                    int wz = baseZ + z;
                    if (world.isAirBlock(wx, y, wz)) continue;

                    Block block = world.getBlock(wx, y, wz);
                    if (block == null) continue;

                    boolean blended = TerrainTextureAtlas.isTransparent(block);
                    boolean alphaTested = !blended && TerrainTextureAtlas.isCutout(block);
                    List<Float> target = blended ? translucent : (alphaTested ? cutout : solid);

                    if (isFaceVisible(world, wx + 1, y, wz, block)) emitFace(world, target, wx + 1, y, wz, -1, 0, 0, block, blended || alphaTested);
                    if (isFaceVisible(world, wx - 1, y, wz, block)) emitFace(world, target, wx, y, wz, 1, 0, 0, block, blended || alphaTested);
                    if (isFaceVisible(world, wx, y + 1, wz, block)) emitFace(world, target, wx, y + 1, wz, 0, -1, 0, block, blended || alphaTested);
                    if (isFaceVisible(world, wx, y - 1, wz, block)) emitFace(world, target, wx, y, wz, 0, 1, 0, block, blended || alphaTested);
                    if (isFaceVisible(world, wx, y, wz + 1, block)) emitFace(world, target, wx, y, wz + 1, 0, 0, -1, block, blended || alphaTested);
                    if (isFaceVisible(world, wx, y, wz - 1, block)) emitFace(world, target, wx, y, wz, 0, 0, 1, block, blended || alphaTested);
                }
            }
        }

        return new MeshData(toFloatArray(solid), toFloatArray(cutout), toFloatArray(translucent));
    }

    private boolean isFaceVisible(World world, int x, int y, int z, Block source) {
        if (y < 0 || y >= world.getHeight()) return true;
        if (world.isAirBlock(x, y, z)) return true;

        Block neighbor = world.getBlock(x, y, z);
        if (neighbor == null) return true;
        return TerrainTextureAtlas.isTransparent(neighbor) &&
                TerrainTextureAtlas.isTransparent(source) && neighbor != source;
    }

    private void emitFace(World world, List<Float> vertices, int px, int py, int pz,
                          int nx, int ny, int nz, Block block, boolean special) {
        TerrainTextureAtlas.FaceUV uv = TerrainTextureAtlas.getFaceUVs(block, nx, ny, nz);
        float[] color = TerrainTextureAtlas.getFaceColor(block, special);
        float light = TerrainLighting.getFaceLight(world, px, py, pz, nx, ny, nz);
        float alpha = TerrainTextureAtlas.getAlpha(block, special);

        float r = clamp(color[0] * light);
        float g = clamp(color[1] * light);
        float b = clamp(color[2] * light);

        float x0 = px - (nx == 1 ? 1 : 0);
        float x1 = x0 + 1.0f;
        float y0 = py - (ny == 1 ? 1 : 0);
        float y1 = y0 + 1.0f;
        float z0 = pz - (nz == 1 ? 1 : 0);
        float z1 = z0 + 1.0f;

        push(vertices, x0, y0, z0, uv.u0, uv.v0, r, g, b, alpha);
        push(vertices, x1, y0, z0, uv.u1, uv.v0, r, g, b, alpha);
        push(vertices, x1, y1, z1, uv.u1, uv.v1, r, g, b, alpha);
        push(vertices, x0, y0, z0, uv.u0, uv.v0, r, g, b, alpha);
        push(vertices, x1, y1, z1, uv.u1, uv.v1, r, g, b, alpha);
        push(vertices, x0, y1, z1, uv.u0, uv.v1, r, g, b, alpha);
    }

    private void push(List<Float> values, float x, float y, float z, float u, float v,
                      float r, float g, float b, float a) {
        values.add(x);
        values.add(y);
        values.add(z);
        values.add(u);
        values.add(v);
        values.add(r);
        values.add(g);
        values.add(b);
        values.add(a);
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    public static final class MeshData {
        private final float[] solidVertices;
        private final float[] cutoutVertices;
        private final float[] translucentVertices;

        public MeshData(float[] solidVertices, float[] cutoutVertices, float[] translucentVertices) {
            this.solidVertices = solidVertices;
            this.cutoutVertices = cutoutVertices;
            this.translucentVertices = translucentVertices;
        }

        public float[] getSolidVertices() { return solidVertices; }
        public float[] getCutoutVertices() { return cutoutVertices; }
        public float[] getTranslucentVertices() { return translucentVertices; }
    }
}

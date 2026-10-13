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
                    boolean special = blended || alphaTested;

                    emitIfVisible(world, target, wx, y, wz, block, 1, 0, 0, special);
                    emitIfVisible(world, target, wx, y, wz, block, -1, 0, 0, special);
                    emitIfVisible(world, target, wx, y, wz, block, 0, 1, 0, special);
                    emitIfVisible(world, target, wx, y, wz, block, 0, -1, 0, special);
                    emitIfVisible(world, target, wx, y, wz, block, 0, 0, 1, special);
                    emitIfVisible(world, target, wx, y, wz, block, 0, 0, -1, special);
                }
            }
        }

        return new MeshData(toFloatArray(solid), toFloatArray(cutout), toFloatArray(translucent));
    }

    private void emitIfVisible(World world, List<Float> vertices, int x, int y, int z,
                               Block source, int nx, int ny, int nz, boolean special) {
        if (isFaceVisible(world, x + nx, y + ny, z + nz, source)) {
            emitFace(world, vertices, x, y, z, nx, ny, nz, source, special);
        }
    }

    private boolean isFaceVisible(World world, int x, int y, int z, Block source) {
        if (y < 0 || y >= world.getHeight() || world.isAirBlock(x, y, z)) return true;

        Block neighbor = world.getBlock(x, y, z);
        if (neighbor == null) return true;

        boolean sourceBlended = TerrainTextureAtlas.isTransparent(source);
        boolean neighborBlended = TerrainTextureAtlas.isTransparent(neighbor);
        boolean sourceCutout = TerrainTextureAtlas.isCutout(source);
        boolean neighborCutout = TerrainTextureAtlas.isCutout(neighbor);

        // Opaque blocks hide neighboring opaque/cutout faces. Transparent blocks do not hide
        // each other's faces unless they are the same liquid/material family.
        if (!sourceBlended && !sourceCutout && !neighborBlended && !neighborCutout) return false;
        if (sourceBlended && neighborBlended && source == neighbor) return false;
        if (sourceCutout && neighborCutout && source == neighbor) return false;
        return true;
    }

    private void emitFace(World world, List<Float> vertices, int x, int y, int z,
                          int nx, int ny, int nz, Block block, boolean special) {
        TerrainTextureAtlas.FaceUV uv = TerrainTextureAtlas.getFaceUVs(block, nx, ny, nz);
        float[] color = TerrainTextureAtlas.getFaceColor(block, special);
        float light = TerrainLighting.getFaceLight(world, x + nx, y + ny, z + nz, nx, ny, nz);
        float alpha = TerrainTextureAtlas.getAlpha(block, special);
        float r = clamp(color[0] * light);
        float g = clamp(color[1] * light);
        float b = clamp(color[2] * light);

        float x0 = x;
        float x1 = x + 1.0f;
        float y0 = y;
        float y1 = y + 1.0f;
        float z0 = z;
        float z1 = z + 1.0f;

        if (nx == 1) {
            quad(vertices, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, uv, r, g, b, alpha);
        } else if (nx == -1) {
            quad(vertices, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, uv, r, g, b, alpha);
        } else if (ny == 1) {
            quad(vertices, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, uv, r, g, b, alpha);
        } else if (ny == -1) {
            quad(vertices, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, uv, r, g, b, alpha);
        } else if (nz == 1) {
            quad(vertices, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, uv, r, g, b, alpha);
        } else {
            quad(vertices, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, uv, r, g, b, alpha);
        }
    }

    private void quad(List<Float> vertices,
                      float ax, float ay, float az,
                      float bx, float by, float bz,
                      float cx, float cy, float cz,
                      float dx, float dy, float dz,
                      TerrainTextureAtlas.FaceUV uv,
                      float r, float g, float b, float a) {
        push(vertices, ax, ay, az, uv.u0, uv.v1, r, g, b, a);
        push(vertices, bx, by, bz, uv.u1, uv.v1, r, g, b, a);
        push(vertices, cx, cy, cz, uv.u1, uv.v0, r, g, b, a);
        push(vertices, ax, ay, az, uv.u0, uv.v1, r, g, b, a);
        push(vertices, cx, cy, cz, uv.u1, uv.v0, r, g, b, a);
        push(vertices, dx, dy, dz, uv.u0, uv.v0, r, g, b, a);
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

package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates water-surface quads for the active chunk so liquid blocks render in a translucent pass.
 */
public final class WaterMesher {
    public MeshData build(World world, int chunkX, int chunkZ) {
        List<Float> vertices = new ArrayList<Float>();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    int wx = baseX + x;
                    int wy = y;
                    int wz = baseZ + z;
                    Block block = world.getBlock(wx, wy, wz);
                    if (block == null || !block.getMaterial().isLiquid()) continue;

                    if (isWaterSurfaceVisible(world, wx, wy, wz)) {
                        emitSurface(vertices, wx, wy, wz, block);
                    }
                }
            }
        }

        return new MeshData(toFloatArray(vertices));
    }

    private boolean isWaterSurfaceVisible(World world, int x, int y, int z) {
        int aboveY = y + 1;
        if (aboveY >= world.getHeight()) return true;
        Block above = world.getBlock(x, aboveY, z);
        if (above == null) return true;
        return !above.getMaterial().isLiquid();
    }

    private void emitSurface(List<Float> verts, int x, int y, int z, Block block) {
        TerrainTextureAtlas.FaceUV uv = TerrainTextureAtlas.getFaceUVs(block, 0, 1, 0);
        float[] color = TerrainTextureAtlas.getFaceColor(block, true);
        float alpha = TerrainTextureAtlas.getAlpha(block, true);

        float r = clamp(color[0]);
        float g = clamp(color[1]);
        float b = clamp(color[2]);

        push(verts, x, y + 1, z, uv.u0, uv.v0, r, g, b, alpha);
        push(verts, x + 1, y + 1, z, uv.u1, uv.v0, r, g, b, alpha);
        push(verts, x + 1, y + 1, z + 1, uv.u1, uv.v1, r, g, b, alpha);

        push(verts, x, y + 1, z, uv.u0, uv.v0, r, g, b, alpha);
        push(verts, x + 1, y + 1, z + 1, uv.u1, uv.v1, r, g, b, alpha);
        push(verts, x, y + 1, z + 1, uv.u0, uv.v1, r, g, b, alpha);
    }

    private void push(List<Float> verts, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(u);
        verts.add(v);
        verts.add(r);
        verts.add(g);
        verts.add(b);
        verts.add(a);
    }

    private float clamp(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }

    private float[] toFloatArray(List<Float> values) {
        if (values.isEmpty()) return new float[0];
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    public static final class MeshData {
        private final float[] vertices;

        public MeshData(float[] vertices) {
            this.vertices = vertices;
        }

        public float[] getVertices() {
            return vertices;
        }
    }
}

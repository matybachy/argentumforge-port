package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;

import java.util.Locale;

/**
 * Lightweight texture atlas mapping for the prototype terrain renderer.
 * This is intentionally simple: it maps a few common block families to a small atlas-space UV table,
 * which gives the renderer deterministic texture coordinates without requiring a full modern atlas implementation.
 */
public final class TerrainTextureAtlas {
    private TerrainTextureAtlas() {
    }

    public static boolean isTransparent(Block block) {
        if (block == null) return false;
        String name = block.getUnlocalizedName().toLowerCase(Locale.ROOT);
        return name.contains("water") || name.contains("lava") || name.contains("glass") || name.contains("ice") || block.getMaterial().isLiquid();
    }

    public static boolean isCutout(Block block) {
        if (block == null) return false;
        String name = block.getUnlocalizedName().toLowerCase(Locale.ROOT);
        return name.contains("leaves") || name.contains("leaf") || name.contains("tallgrass") || name.contains("vine") || name.contains("plant");
    }

    public static float getAlpha(Block block, boolean transparent) {
        if (transparent) return 0.7f;
        if (isCutout(block)) return 0.8f;
        return 1.0f;
    }

    public static FaceUV getFaceUVs(Block block, int nx, int ny, int nz) {
        if (block == null) {
            return new FaceUV(0.0f, 0.0f, 1.0f, 1.0f);
        }

        String name = block.getUnlocalizedName().toLowerCase(Locale.ROOT);

        if (name.contains("grass")) {
            return new FaceUV(0.0f, 0.0f, 0.5f, 0.5f);
        }
        if (name.contains("dirt") || name.contains("soil")) {
            return new FaceUV(0.5f, 0.0f, 1.0f, 0.5f);
        }
        if (name.contains("stone") || name.contains("rock")) {
            return new FaceUV(0.0f, 0.5f, 0.5f, 1.0f);
        }
        if (name.contains("sand")) {
            return new FaceUV(0.5f, 0.5f, 1.0f, 1.0f);
        }
        if (name.contains("water") || name.contains("lava")) {
            return new FaceUV(0.25f, 0.25f, 0.75f, 0.75f);
        }
        if (name.contains("leaves") || name.contains("leaf")) {
            return new FaceUV(0.25f, 0.5f, 0.75f, 1.0f);
        }
        if (name.contains("glass")) {
            return new FaceUV(0.75f, 0.0f, 1.0f, 0.5f);
        }

        return new FaceUV(0.0f, 0.0f, 1.0f, 1.0f);
    }

    public static float[] getFaceColor(Block block, boolean transparent) {
        if (block == null) {
            return new float[] { 0.5f, 0.5f, 0.5f };
        }

        String name = block.getUnlocalizedName().toLowerCase(Locale.ROOT);

        if (name.contains("grass")) {
            return transparent ? new float[] { 0.3f, 0.7f, 0.3f } : new float[] { 0.45f, 0.8f, 0.45f };
        }
        if (name.contains("dirt") || name.contains("soil")) {
            return new float[] { 0.5f, 0.35f, 0.2f };
        }
        if (name.contains("stone") || name.contains("rock")) {
            return new float[] { 0.65f, 0.65f, 0.7f };
        }
        if (name.contains("sand")) {
            return new float[] { 0.9f, 0.8f, 0.5f };
        }
        if (name.contains("water")) {
            return new float[] { 0.25f, 0.45f, 0.9f };
        }
        if (name.contains("leaves") || name.contains("leaf")) {
            return new float[] { 0.35f, 0.75f, 0.35f };
        }
        if (name.contains("glass")) {
            return new float[] { 0.8f, 0.9f, 1.0f };
        }

        return transparent ? new float[] { 0.7f, 0.7f, 0.7f } : new float[] { 0.8f, 0.8f, 0.8f };
    }

    public static final class FaceUV {
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public FaceUV(float u0, float v0, float u1, float v1) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }
}

package matybachy.argentumforge.client.mesh;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * Simple lighting approximation for the terrain prototype.
 * This is intentionally lightweight: it approximates face brightness from a block's light value,
 * the world height, and the direction of the face. This keeps the renderer closer to a real
 * 1.8.9 block-light pipeline without requiring a full modern lightmap implementation.
 */
public final class TerrainLighting {
    private TerrainLighting() {
    }

    public static float getFaceLight(World world, int x, int y, int z, int nx, int ny, int nz) {
        int emitted = 0;
        if (world != null) {
            Block block = world.getBlock(x, y, z);
            if (block != null) {
                emitted = Math.max(emitted, block.getLightValue());
            }
            float heightBias = 1.0f - Math.min(1.0f, (float) y / 128.0f);
            emitted = Math.max(emitted, (int) (heightBias * 12.0f));
        }

        float brightness = 0.35f + ((emitted / 15.0f) * 0.65f);
        brightness *= 0.90f + (nx != 0 ? 0.08f : 0.0f);
        brightness *= 0.92f + (ny != 0 ? 0.10f : 0.0f);
        brightness *= 0.95f + (nz != 0 ? 0.06f : 0.0f);

        if (brightness < 0.2f) brightness = 0.2f;
        if (brightness > 1.0f) brightness = 1.0f;
        return brightness;
    }
}

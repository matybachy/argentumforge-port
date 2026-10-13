package matybachy.argentumforge.client.mesh;

/**
 * Simple render-layer classification used by the terrain prototype.
 * This is the first layer split in the chunk renderer and mirrors the common Forge 1.8.9 pattern:
 * solid geometry is rendered first, then translucent geometry.
 */
public enum RenderLayer {
    SOLID,
    CUTOUT,
    TRANSLUCENT
}

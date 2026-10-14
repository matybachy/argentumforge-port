package matybachy.argentumforge.client.terrain;

/** Inclusive world-space block range of the blocks a compiled section actually renders. */
final class GeometryBounds {
    final int minX;
    final int minY;
    final int minZ;
    final int maxX;
    final int maxY;
    final int maxZ;

    GeometryBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
}

package matybachy.argentumforge.client.mesh;

final class MeshMath {
    private MeshMath() {
    }

    static float clamp01(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }

    static float uvInset(float min, float max) {
        float span = max - min;
        if (span <= 0.0f) return 0.0f;
        return span / 256.0f;
    }

    static float fluidHeightForLevel(int level) {
        int bounded = Math.max(0, Math.min(7, level));
        return clamp01((8 - bounded) / 8.0f);
    }
}

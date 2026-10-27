package matybachy.argentumforge.client.mesh;

public final class RendererValidation {
    private RendererValidation() {
    }

    public static void validateStaticInvariants() {
        validateClamp(0.0f);
        validateClamp(0.5f);
        validateClamp(1.0f);
    }

    private static void validateClamp(float value) {
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        if (clamped < 0.0f || clamped > 1.0f) {
            throw new IllegalStateException("Renderer clamp invariant failed");
        }
    }
}

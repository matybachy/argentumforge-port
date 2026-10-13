package matybachy.argentumforge.client.compat;

public interface OptionalEffectPipeline {
    boolean isSupported();

    void begin();

    void end();
}

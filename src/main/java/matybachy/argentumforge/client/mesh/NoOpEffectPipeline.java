package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.client.compat.OptionalEffectPipeline;

public final class NoOpEffectPipeline implements OptionalEffectPipeline {
    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }
}

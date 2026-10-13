package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.OptionalEffectPipeline;
import net.minecraft.client.renderer.OpenGlHelper;

public final class ForgeOptionalEffectPipeline implements OptionalEffectPipeline {
    @Override
    public boolean isSupported() {
        return ArgentumConfig.enableOptionalEffects && OpenGlHelper.isFramebufferEnabled();
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }
}

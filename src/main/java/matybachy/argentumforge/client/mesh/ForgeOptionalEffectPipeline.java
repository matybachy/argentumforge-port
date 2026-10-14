package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.OptionalEffectPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public final class ForgeOptionalEffectPipeline implements OptionalEffectPipeline {
    private Framebuffer effectBuffer;
    private int bufferWidth = -1;
    private int bufferHeight = -1;
    private boolean active;
    private boolean disabledByFailure;

    @Override
    public boolean isSupported() {
        return ArgentumConfig.enableOptionalEffects && OpenGlHelper.isFramebufferEnabled() && !disabledByFailure;
    }

    @Override
    public void begin() {
        if (!isSupported()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        if (!ensureBuffer(mc)) return;
        effectBuffer.bindFramebuffer(true);
        effectBuffer.framebufferClear();
        active = true;
    }

    @Override
    public void end() {
        if (!active) return;
        Minecraft mc = Minecraft.getMinecraft();
        active = false;
        if (mc == null || effectBuffer == null) return;
        try {
            Framebuffer main = mc.getFramebuffer();
            if (main != null) {
                main.bindFramebuffer(true);
            }
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            float alpha = ArgentumConfig.optionalEffectsCompositeAlpha / 100.0f;
            GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
            effectBuffer.framebufferRenderExt(mc.displayWidth, mc.displayHeight, true);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } catch (Throwable ignored) {
            disableDueToFailure();
        }
    }

    private boolean ensureBuffer(Minecraft mc) {
        int width = Math.max(1, mc.displayWidth);
        int height = Math.max(1, mc.displayHeight);
        if (effectBuffer != null && (bufferWidth != width || bufferHeight != height)) {
            effectBuffer.deleteFramebuffer();
            effectBuffer = null;
            bufferWidth = -1;
            bufferHeight = -1;
        }
        if (effectBuffer == null) {
            try {
                effectBuffer = new Framebuffer(width, height, true);
                effectBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
                bufferWidth = width;
                bufferHeight = height;
            } catch (Throwable ignored) {
                disableDueToFailure();
                return false;
            }
        }
        return true;
    }

    private void disableDueToFailure() {
        disabledByFailure = true;
        if (effectBuffer != null) {
            effectBuffer.deleteFramebuffer();
            effectBuffer = null;
        }
        bufferWidth = -1;
        bufferHeight = -1;
    }
}

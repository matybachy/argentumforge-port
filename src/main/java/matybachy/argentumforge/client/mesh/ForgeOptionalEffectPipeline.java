package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.OptionalEffectPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public final class ForgeOptionalEffectPipeline implements OptionalEffectPipeline {
    private Framebuffer effectBuffer;
    private Framebuffer mainBuffer;
    private int bufferWidth = -1;
    private int bufferHeight = -1;
    private boolean active;
    private boolean disabledByFailure;

    @Override
    public boolean isSupported() {
        return RenderOptimizationMath.shouldRunOptionalEffects(
                ArgentumConfig.enableOptionalEffects,
                OpenGlHelper.isFramebufferEnabled(),
                disabledByFailure,
                isOnClientThread()
        );
    }

    @Override
    public void begin() {
        if (!isSupported()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !ensureBuffer(mc)) return;
        try {
            mainBuffer = mc.getFramebuffer();
            effectBuffer.bindFramebuffer(true);
            effectBuffer.framebufferClear();
            active = true;
        } catch (Throwable ignored) {
            active = false;
            restoreMainBuffer();
            disableDueToFailure();
        }
    }

    @Override
    public void end() {
        if (!active) return;
        active = false;
        if (!isOnClientThread()) {
            restoreMainBuffer();
            disableDueToFailure();
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || effectBuffer == null) {
            restoreMainBuffer();
            return;
        }

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alphaWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        try {
            restoreMainBuffer();
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            float alpha = RenderOptimizationMath.clampedPercent(ArgentumConfig.optionalEffectsCompositeAlpha) / 100.0f;
            float tintR = RenderOptimizationMath.clampedPercent(ArgentumConfig.optionalEffectsTintRed) / 100.0f;
            float tintG = RenderOptimizationMath.clampedPercent(ArgentumConfig.optionalEffectsTintGreen) / 100.0f;
            float tintB = RenderOptimizationMath.clampedPercent(ArgentumConfig.optionalEffectsTintBlue) / 100.0f;
            GL11.glColor4f(tintR, tintG, tintB, alpha);
            effectBuffer.framebufferRenderExt(mc.displayWidth, mc.displayHeight, true);
        } catch (Throwable ignored) {
            disableDueToFailure();
        } finally {
            restoreMainBuffer();
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glDepthMask(true);
            if (depthWasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (alphaWasEnabled) GL11.glEnable(GL11.GL_ALPHA_TEST);
            else GL11.glDisable(GL11.GL_ALPHA_TEST);
        }
    }

    private void restoreMainBuffer() {
        try {
            if (mainBuffer != null) mainBuffer.bindFramebuffer(true);
        } catch (Throwable ignored) {
        } finally {
            mainBuffer = null;
        }
    }

    private boolean ensureBuffer(Minecraft mc) {
        int width = Math.max(1, mc.displayWidth);
        int height = Math.max(1, mc.displayHeight);
        if (effectBuffer != null && (bufferWidth != width || bufferHeight != height)) {
            try {
                effectBuffer.deleteFramebuffer();
            } catch (Throwable ignored) {
            }
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
        restoreMainBuffer();
        if (effectBuffer != null) {
            try {
                effectBuffer.deleteFramebuffer();
            } catch (Throwable ignored) {
            }
            effectBuffer = null;
        }
        bufferWidth = -1;
        bufferHeight = -1;
    }

    private boolean isOnClientThread() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        try {
            Object result = mc.getClass().getMethod("isCallingFromMinecraftThread").invoke(mc);
            return result instanceof Boolean ? ((Boolean) result).booleanValue() : true;
        } catch (Throwable ignored) {
            return true;
        }
    }
}

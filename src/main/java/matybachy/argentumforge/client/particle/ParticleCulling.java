package matybachy.argentumforge.client.particle;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.AxisAlignedBB;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frustum culling for the particle layers that {@code EffectRenderer.renderParticles} draws in one batch
 * (layers 0-2). Layer 3 particles (pickup, footstep, large explosion, emitters) are drawn by
 * {@code renderLitParticles} and are never touched. Only vanilla particle classes are culled, because modded
 * particles may draw away from their bounding box. Render thread only.
 */
public final class ParticleCulling {
    /** Particle quads extend up to ~0.7 blocks (scale 7) past their position and move between ticks. */
    private static final double MARGIN = 2.0D;
    private static final String VANILLA_PACKAGE = "net.minecraft.client.particle.";

    // A private helper, so re-reading the matrices never disturbs the shared ClippingHelperImpl instance.
    private static ClippingHelperImpl clippingHelper;
    private static Frustum frustum;
    private static boolean active;
    private static final Map<Class<?>, Boolean> CULLABLE = new IdentityHashMap<Class<?>, Boolean>();
    private static long culledTotal;
    private static int culledLastFrame;
    private static int culledThisFrame;

    private ParticleCulling() {
    }

    public static boolean isEnabled() {
        return ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER
                && ArgentumConfig.enableParticleHooks
                && ArgentumConfig.enableParticleCulling;
    }

    /** Start of renderParticles: captures the camera planes, which stay current for the whole particle pass. */
    public static void begin(double cameraX, double cameraY, double cameraZ) {
        culledLastFrame = culledThisFrame;
        culledThisFrame = 0;
        active = false;
        if (!isEnabled()) return;
        try {
            if (clippingHelper == null) clippingHelper = new ClippingHelperImpl();
            clippingHelper.init();
            if (frustum == null) frustum = new Frustum(clippingHelper);
            frustum.setPosition(cameraX, cameraY, cameraZ);
            active = true;
        } catch (RuntimeException ignored) {
            // Draw everything if the matrices cannot be read.
        }
    }

    public static boolean shouldRender(EntityFX particle) {
        if (!active || particle == null || !isCullable(particle.getClass())) return true;
        AxisAlignedBB box = particle.getEntityBoundingBox();
        if (box == null) return true;
        if (frustum.isBoxInFrustum(box.minX - MARGIN, box.minY - MARGIN, box.minZ - MARGIN,
                box.maxX + MARGIN, box.maxY + MARGIN, box.maxZ + MARGIN)) {
            return true;
        }
        culledThisFrame++;
        culledTotal++;
        return false;
    }

    private static boolean isCullable(Class<?> type) {
        Boolean cached = CULLABLE.get(type);
        if (cached == null) {
            cached = Boolean.valueOf(type.getName().startsWith(VANILLA_PACKAGE));
            CULLABLE.put(type, cached);
        }
        return cached.booleanValue();
    }

    public static void appendDebugLines(List<String> lines) {
        if (!isEnabled()) return;
        lines.add("[Argentum] particles culled: " + culledLastFrame + " last frame, " + culledTotal + " total");
    }
}

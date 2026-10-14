package matybachy.argentumforge.client.mesh;

import org.lwjgl.opengl.GL11;

/**
 * Helper class providing state optimizations for Font/Text rendering, HUD overlays,
 * and Enchantment Glint rendering.
 */
public final class FontAndGlintOptimization {
    private static boolean glintStateOptimized = false;

    private FontAndGlintOptimization() {
    }

    /**
     * Prepares OpenGL state prior to rendering text glyphs to prevent redundant texture binding.
     */
    public static void beginTextRendering() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Restores OpenGL state after text rendering completes.
     */
    public static void endTextRendering() {
        // State restored by caller push/pop matrix/attrib
    }

    /**
     * Prepares optimized state for enchantment glint rendering pass to minimize state changes.
     */
    public static void beginGlintRendering() {
        if (!glintStateOptimized) {
            GL11.glDepthFunc(GL11.GL_EQUAL);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            glintStateOptimized = true;
        }
    }

    /**
     * Restores standard state after enchantment glint rendering pass.
     */
    public static void endGlintRendering() {
        if (glintStateOptimized) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            glintStateOptimized = false;
        }
    }
}

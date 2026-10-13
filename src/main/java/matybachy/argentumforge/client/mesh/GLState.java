package matybachy.argentumforge.client.mesh;

import org.lwjgl.opengl.GL11;

/**
 * Small GL state helper to avoid direct GL calls in manager.
 */
public final class GLState {
    private GLState() {}

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }
}

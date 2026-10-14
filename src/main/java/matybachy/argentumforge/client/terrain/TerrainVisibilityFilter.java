package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.chunk.RenderChunk;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Second-stage section culling. Vanilla's setupTerrain already tests each full 16x16x16 section box
 * against the frustum and walks the cave-culling visibility graph. This filter additionally tests the
 * tighter box of blocks that the compiled section actually renders (plus a one-block margin for
 * models that extend outside their block) against the left/right/top/bottom camera planes.
 *
 * <p>The planes are derived exactly like ClippingHelperImpl from the GL projection and modelview
 * matrices current when RenderGlobal.setupTerrain calls ChunkRenderContainer.initialize. Near/far
 * planes are ignored so a differing far distance can never cull geometry. If the matrices are not
 * usable, or a section has no bounds recorded (vanilla-cache build), the section is drawn.</p>
 */
final class TerrainVisibilityFilter {
    private static final double MARGIN = 1.0D;
    private final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelviewBuffer = BufferUtils.createFloatBuffer(16);
    private final float[] projection = new float[16];
    private final float[] modelview = new float[16];
    private final float[] clip = new float[16];
    private final float[][] planes = new float[4][4];
    private boolean valid;
    private double cameraX;
    private double cameraY;
    private double cameraZ;

    void prepare(double x, double y, double z) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
        valid = false;
        if (!IntegratedTerrain.isBoundsCullingActive()) return;
        try {
            projectionBuffer.clear();
            modelviewBuffer.clear();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewBuffer);
            projectionBuffer.rewind();
            modelviewBuffer.rewind();
            projectionBuffer.get(projection);
            modelviewBuffer.get(modelview);
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    clip[row * 4 + col] = modelview[row * 4] * projection[col]
                            + modelview[row * 4 + 1] * projection[4 + col]
                            + modelview[row * 4 + 2] * projection[8 + col]
                            + modelview[row * 4 + 3] * projection[12 + col];
                }
            }
            valid = setPlane(0, -1, 0) && setPlane(1, 1, 0) && setPlane(2, 1, 1) && setPlane(3, -1, 1);
        } catch (RuntimeException ignored) {
            valid = false;
        }
    }

    /** plane = column3 + sign * column(axis), matching ClippingHelperImpl's first four planes. */
    private boolean setPlane(int index, int sign, int axis) {
        float[] plane = planes[index];
        for (int i = 0; i < 4; i++) {
            plane[i] = clip[i * 4 + 3] + sign * clip[i * 4 + axis];
        }
        float length = (float) Math.sqrt(plane[0] * plane[0] + plane[1] * plane[1] + plane[2] * plane[2]);
        if (!(length > 1.0E-6F) || Float.isNaN(plane[3]) || Float.isInfinite(plane[3])) return false;
        for (int i = 0; i < 4; i++) plane[i] /= length;
        return true;
    }

    boolean shouldDraw(RenderChunk renderChunk) {
        if (!valid || !(renderChunk instanceof SnapshotRenderChunk)) return true;
        GeometryBounds bounds = ((SnapshotRenderChunk) renderChunk).getSnapshotSupport()
                .boundsFor(renderChunk.getCompiledChunk());
        if (bounds == null) return true;

        double minX = bounds.minX - MARGIN - cameraX;
        double minY = bounds.minY - MARGIN - cameraY;
        double minZ = bounds.minZ - MARGIN - cameraZ;
        double maxX = bounds.maxX + 1.0D + MARGIN - cameraX;
        double maxY = bounds.maxY + 1.0D + MARGIN - cameraY;
        double maxZ = bounds.maxZ + 1.0D + MARGIN - cameraZ;
        for (float[] plane : planes) {
            double px = plane[0] >= 0.0F ? maxX : minX;
            double py = plane[1] >= 0.0F ? maxY : minY;
            double pz = plane[2] >= 0.0F ? maxZ : minZ;
            if (plane[0] * px + plane[1] * py + plane[2] * pz + plane[3] < 0.0D) {
                TerrainStats.recordCulledSectionLayer();
                return false;
            }
        }
        return true;
    }
}

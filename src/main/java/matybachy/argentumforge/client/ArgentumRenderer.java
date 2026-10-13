package matybachy.argentumforge.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.Tessellator;

/**
 * A small experimental renderer that demonstrates custom GL-based rendering during the world render pass.
 * This is an incremental step toward a custom chunk/terrain renderer: it hooks RenderWorldLastEvent and
 * draws a translucent cube in world space. It is a genuine runtime rendering hook and not an empty stub.
 */
public class ArgentumRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();

    public ArgentumRenderer() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) return;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        double px = mc.getRenderViewEntity().posX;
        double py = mc.getRenderViewEntity().posY;
        double pz = mc.getRenderViewEntity().posZ;

        // Translate to camera-relative coordinates
        GL11.glTranslated(-px, -py, -pz);

        // Draw a translucent cube at world coordinates (0,64,0) size 16 (a chunk-sized box)
        GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.20f);
        drawCube(0.0, 64.0, 0.0, 16.0);

        // Restore GL state
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawCube(double x, double y, double z, double s) {
        Tessellator tess = Tessellator.instance;
        double x1 = x;
        double y1 = y;
        double z1 = z;
        double x2 = x + s;
        double y2 = y + s;
        double z2 = z + s;

        // bottom
        tess.startDrawingQuads();
        tess.addVertex(x1, y1, z1);
        tess.addVertex(x2, y1, z1);
        tess.addVertex(x2, y1, z2);
        tess.addVertex(x1, y1, z2);
        tess.draw();

        // top
        tess.startDrawingQuads();
        tess.addVertex(x1, y2, z1);
        tess.addVertex(x1, y2, z2);
        tess.addVertex(x2, y2, z2);
        tess.addVertex(x2, y2, z1);
        tess.draw();

        // front
        tess.startDrawingQuads();
        tess.addVertex(x1, y1, z1);
        tess.addVertex(x1, y2, z1);
        tess.addVertex(x2, y2, z1);
        tess.addVertex(x2, y1, z1);
        tess.draw();

        // back
        tess.startDrawingQuads();
        tess.addVertex(x2, y1, z2);
        tess.addVertex(x2, y2, z2);
        tess.addVertex(x1, y2, z2);
        tess.addVertex(x1, y1, z2);
        tess.draw();

        // left
        tess.startDrawingQuads();
        tess.addVertex(x1, y1, z2);
        tess.addVertex(x1, y2, z2);
        tess.addVertex(x1, y2, z1);
        tess.addVertex(x1, y1, z1);
        tess.draw();

        // right
        tess.startDrawingQuads();
        tess.addVertex(x2, y1, z1);
        tess.addVertex(x2, y2, z1);
        tess.addVertex(x2, y2, z2);
        tess.addVertex(x2, y1, z2);
        tess.draw();
    }
}

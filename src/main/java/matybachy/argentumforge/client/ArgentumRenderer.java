package matybachy.argentumforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * A small experimental renderer that demonstrates custom GL-based rendering during the world render pass.
 * This version uses direct GL calls compatible with Minecraft 1.8.9 and avoids Tessellator/mapping issues.
 */
public class ArgentumRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();

    public ArgentumRenderer() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.theWorld == null) {
            return;
        }

        Entity view = mc.getRenderViewEntity();
        if (view == null) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        double px = view.posX;
        double py = view.posY;
        double pz = view.posZ;

        // Translate to camera-relative coordinates
        GL11.glTranslated(-px, -py, -pz);

        // Draw a translucent wireframe cube at world coordinates (0,64,0) size 16 (a chunk-sized box)
        GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.20f);
        drawWireCube(0.0D, 64.0D, 0.0D, 16.0D);

        // Restore GL state
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private void drawWireCube(double x, double y, double z, double s) {
        double x1 = x;
        double y1 = y;
        double z1 = z;
        double x2 = x + s;
        double y2 = y + s;
        double z2 = z + s;

        GL11.glBegin(GL11.GL_LINES);

        // bottom
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x1, y1, z1);

        // top
        GL11.glVertex3d(x1, y2, z1); GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y2, z1); GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x2, y2, z2); GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y2, z2); GL11.glVertex3d(x1, y2, z1);

        // verticals
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x2, y2, z2);

        GL11.glEnd();
    }
}

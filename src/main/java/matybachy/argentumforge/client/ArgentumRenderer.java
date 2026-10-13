package matybachy.argentumforge.client;

import matybachy.argentumforge.ArgentumForge;
import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ArgentumRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ChunkMeshManager meshManager = ArgentumForge.MESH_MANAGER;

    public ArgentumRenderer() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER) return;
        if (mc.theWorld == null) return;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        double px = view.posX;
        double py = view.posY;
        double pz = view.posZ;
        GL11.glTranslated(-px, -py, -pz);

        if (meshManager != null) {
            meshManager.render();
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}

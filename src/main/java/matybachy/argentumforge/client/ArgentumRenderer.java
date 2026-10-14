package matybachy.argentumforge.client;

import matybachy.argentumforge.ArgentumForge;
import matybachy.argentumforge.client.compat.OptionalEffectPipeline;
import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import matybachy.argentumforge.client.mesh.ForgeOptionalEffectPipeline;
import matybachy.argentumforge.client.mesh.OptionalRenderHooks;
import matybachy.argentumforge.client.mesh.TerrainTextureAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ArgentumRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ChunkMeshManager meshManager = ArgentumForge.MESH_MANAGER;
    private final OptionalRenderHooks optionalHooks = new OptionalRenderHooks();
    private final OptionalEffectPipeline effectPipeline = new ForgeOptionalEffectPipeline();

    public ArgentumRenderer() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(optionalHooks);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER) return;
        if (mc.theWorld == null) return;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        boolean effectStarted = false;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            optionalHooks.onBeforeTerrainRender();

            // Evaluate support once. Calling isSupported() again after begin() can
            // observe a failure-disabled pipeline and skip end(), leaving a custom
            // framebuffer bound for FeatherOpt/vanilla rendering.
            if (effectPipeline.isSupported()) {
                effectPipeline.begin();
                effectStarted = true;
            }

            TerrainTextureAtlas.bindTerrainTextureAtlas();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
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
        } finally {
            // Always return to the main framebuffer before restoring GL state,
            // including when a renderer or compatibility hook throws.
            if (effectStarted) {
                try {
                    effectPipeline.end();
                } catch (Throwable ignored) {
                    // The effect pipeline is optional; vanilla rendering remains
                    // the fallback when a third-party renderer rejects the effect.
                }
            }
            try {
                optionalHooks.onAfterTerrainRender();
            } finally {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    public void onClientTick() {
        optionalHooks.onClientTick();
    }
}

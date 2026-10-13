package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public final class OptionalRenderHooks {
    private final Minecraft mc = Minecraft.getMinecraft();
    private Integer originalParticleSetting;

    public void onBeforeTerrainRender() {
    }

    public void onAfterTerrainRender() {
        if (useCloudHooks() && ArgentumConfig.enableCloudRenderer) renderCloudLayer();
    }

    public void onClientTick() {
        if (mc == null || mc.gameSettings == null) return;

        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER || !ArgentumConfig.enableParticleHooks) {
            restoreParticleSetting();
            return;
        }

        if (originalParticleSetting == null) {
            originalParticleSetting = Integer.valueOf(mc.gameSettings.particleSetting);
        }
        mc.gameSettings.particleSetting = RenderOptimizationMath.throttledParticleSetting(
                mc.gameSettings.particleSetting,
                ArgentumConfig.particleThrottleSetting
        );
    }

    private void restoreParticleSetting() {
        if (mc == null || mc.gameSettings == null || originalParticleSetting == null) return;
        mc.gameSettings.particleSetting = RenderOptimizationMath.clampedParticleSetting(originalParticleSetting.intValue());
        originalParticleSetting = null;
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (!ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER) return;
        if (!ArgentumConfig.enableEntityHooks || !ArgentumConfig.enableEntityDistanceCulling) return;
        if (event == null || event.entity == null) return;

        Entity view = mc != null ? mc.getRenderViewEntity() : null;
        if (!(event.entity instanceof EntityLivingBase) || view == null) return;
        if (event.entity == view) return;
        if (event.entity.ticksExisted < 1) return;

        if (RenderOptimizationMath.shouldCullByDistance(
                view.getDistanceSqToEntity(event.entity),
                ArgentumConfig.entityCullingDistance
        )) {
            event.setCanceled(true);
        }
    }

    private void renderCloudLayer() {
        if (mc == null || mc.theWorld == null || mc.gameSettings == null) return;
        if (mc.gameSettings.clouds != 0) return;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float cloudY = mc.theWorld.provider.getCloudHeight();
        int radius = Math.max(32, ArgentumConfig.cloudRenderRadiusBlocks);
        int step = 32;
        int baseX = MathHelper.floor_double(view.posX / step) * step;
        int baseZ = MathHelper.floor_double(view.posZ / step) * step;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0.95f, 0.95f, 0.98f, 0.24f);
        GL11.glBegin(GL11.GL_QUADS);
        for (int x = baseX - radius; x < baseX + radius; x += step) {
            for (int z = baseZ - radius; z < baseZ + radius; z += step) {
                GL11.glVertex3f(x, cloudY, z);
                GL11.glVertex3f(x + step, cloudY, z);
                GL11.glVertex3f(x + step, cloudY, z + step);
                GL11.glVertex3f(x, cloudY, z + step);
            }
        }
        GL11.glEnd();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public boolean useEntityHooks() {
        return ArgentumConfig.enableEntityHooks;
    }

    public boolean useWeatherHooks() {
        return ArgentumConfig.enableWeatherHooks;
    }

    public boolean useCloudHooks() {
        return ArgentumConfig.enableCloudHooks;
    }

    public boolean useItemHooks() {
        return ArgentumConfig.enableItemHooks;
    }

    public boolean useParticleHooks() {
        return ArgentumConfig.enableParticleHooks;
    }

    public boolean useBlockEntityHooks() {
        return ArgentumConfig.enableBlockEntityHooks;
    }
}

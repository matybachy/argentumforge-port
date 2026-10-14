package matybachy.argentumforge.client.entity;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Delegating wrapper for one exact vanilla non-living entity renderer. RenderGlobal.renderEntities
 * asks {@code RenderManager.shouldRender -> Render.shouldRender} before drawing; this wrapper only adds
 * a distance limit there. doRender and doRenderShadowAndFire go to the original renderer unchanged.
 */
final class DistanceCulledEntityRenderer extends Render<Entity> {
    private final Render<Entity> delegate;

    DistanceCulledEntityRenderer(RenderManager renderManager, Render<Entity> delegate) {
        super(renderManager);
        this.delegate = delegate;
    }

    Render<Entity> getDelegate() {
        return delegate;
    }

    @Override
    public boolean shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ) {
        if (!delegate.shouldRender(entity, camera, camX, camY, camZ)) return false;
        return !NonLivingEntityCulling.shouldSkip(entity, camX, camY, camZ);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        delegate.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        delegate.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        // Never used: all drawing is delegated.
        return null;
    }
}

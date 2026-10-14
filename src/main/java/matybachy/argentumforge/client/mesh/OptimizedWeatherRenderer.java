package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;

public final class OptimizedWeatherRenderer extends IRenderHandler {
    private static final ResourceLocation RAIN_TEXTURE = new ResourceLocation("textures/environment/rain.png");
    private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("textures/environment/snow.png");
    private static final double NEAR_DISTANCE_SQ = 24.0D * 24.0D;
    private boolean disabledByFailure;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        if (disabledByFailure) {
            if (world != null && world.provider != null && world.provider.getWeatherRenderer() == this) {
                world.provider.setWeatherRenderer(null);
            }
            return;
        }
        if (world == null || mc == null) return;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float weatherStrength = world.getRainStrength(partialTicks);
        if (weatherStrength <= 0.0F) return;

        double cameraX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double cameraY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double cameraZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        Vec3 look = view.getLook(partialTicks);
        double lookX = look != null ? look.xCoord : 0.0D;
        double lookZ = look != null ? look.zCoord : 0.0D;
        double lookLen = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLen > 1.0E-6D) {
            lookX /= lookLen;
            lookZ /= lookLen;
        } else {
            lookX = 0.0D;
            lookZ = 1.0D;
        }

        double sideX = -lookZ;
        double sideZ = lookX;
        int radius = Math.max(32, ArgentumConfig.weatherRenderRadiusBlocks);
        int radiusCeil = (int) Math.ceil(radius);
        double maxDistanceSq = RenderOptimizationMath.weatherMaxDistanceSq(radius);
        double minForwardDot = Math.max(-1.0D, Math.min(1.0D, ArgentumConfig.weatherFrustumMinDotPercent / 100.0D));
        int minY = MathHelper.floor_double(cameraY) - 10;
        int maxY = MathHelper.floor_double(cameraY) + 10;
        int minX = MathHelper.floor_double(cameraX) - radiusCeil;
        int maxX = MathHelper.floor_double(cameraX) + radiusCeil;
        int minZ = MathHelper.floor_double(cameraZ) - radiusCeil;
        int maxZ = MathHelper.floor_double(cameraZ) + radiusCeil;

        boolean lightmapEnabled = false;
        boolean attribPushed = false;
        try {
            mc.entityRenderer.enableLightmap(partialTicks);
            lightmapEnabled = true;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                    | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
            attribPushed = true;
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glDepthMask(false);

            drawPass(mc, world, partialTicks, cameraX, cameraY, cameraZ, weatherStrength, sideX, sideZ, lookX, lookZ, maxDistanceSq, minForwardDot, minY, maxY, minX, maxX, minZ, maxZ, false);
            drawPass(mc, world, partialTicks, cameraX, cameraY, cameraZ, weatherStrength, sideX, sideZ, lookX, lookZ, maxDistanceSq, minForwardDot, minY, maxY, minX, maxX, minZ, maxZ, true);

            GL11.glDepthMask(true);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } catch (Throwable ignored) {
            disabledByFailure = true;
            if (world.provider != null && world.provider.getWeatherRenderer() == this) {
                world.provider.setWeatherRenderer(null);
            }
        } finally {
            GL11.glDepthMask(true);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (attribPushed) GL11.glPopAttrib();
            if (lightmapEnabled) mc.entityRenderer.disableLightmap();
        }
    }

    private void drawPass(Minecraft mc,
                          WorldClient world,
                          float partialTicks,
                          double cameraX,
                          double cameraY,
                          double cameraZ,
                          float weatherStrength,
                          double sideX,
                          double sideZ,
                          double lookX,
                          double lookZ,
                          double maxDistanceSq,
                          double minForwardDot,
                          int minY,
                          int maxY,
                          int minX,
                          int maxX,
                          int minZ,
                          int maxZ,
                          boolean snowPass) {
        mc.getTextureManager().bindTexture(snowPass ? SNOW_TEXTURE : RAIN_TEXTURE);
        GL11.glBegin(GL11.GL_QUADS);
        float time = (float) world.getTotalWorldTime() + partialTicks;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                double centerX = x + 0.5D;
                double centerZ = z + 0.5D;
                double dx = centerX - cameraX;
                double dz = centerZ - cameraZ;
                double distanceSq = dx * dx + dz * dz;
                if (distanceSq > maxDistanceSq) continue;

                double forwardDot = 1.0D;
                if (distanceSq > 1.0E-6D) {
                    forwardDot = (dx * lookX + dz * lookZ) / Math.sqrt(distanceSq);
                }
                if (RenderOptimizationMath.shouldCullWeatherColumn(distanceSq, maxDistanceSq, forwardDot, minForwardDot, NEAR_DISTANCE_SQ)) {
                    continue;
                }

                BlockPos columnPos = new BlockPos(x, 0, z);
                BiomeGenBase biome = world.getBiomeGenForCoords(columnPos);
                if (biome == null) continue;
                boolean snow = biome.getEnableSnow();
                boolean rain = biome.canSpawnLightningBolt() && !snow;
                if (snowPass) {
                    if (!snow) continue;
                } else if (!rain) {
                    continue;
                }

                int precipitationY = world.getPrecipitationHeight(columnPos).getY();
                int yStart = Math.max(precipitationY, minY);
                int yEnd = maxY;
                if (yStart >= yEnd) continue;

                float alphaScale = (float) Math.max(0.0D, 1.0D - (distanceSq / maxDistanceSq));
                float alpha = weatherStrength * alphaScale * (snow ? 0.75F : 0.65F);
                if (alpha <= 0.01F) continue;

                double x0 = dx - sideX * 0.5D;
                double z0 = dz - sideZ * 0.5D;
                double x1 = dx + sideX * 0.5D;
                double z1 = dz + sideZ * 0.5D;
                float texShift = snow ? (time * 0.0045F) : (time * 0.02F);
                float v0 = yStart * 0.25F + texShift;
                float v1 = yEnd * 0.25F + texShift;

                GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
                GL11.glTexCoord2f(0.0F, v0);
                GL11.glVertex3d(x0, yStart - cameraY, z0);
                GL11.glTexCoord2f(1.0F, v0);
                GL11.glVertex3d(x1, yStart - cameraY, z1);
                GL11.glTexCoord2f(1.0F, v1);
                GL11.glVertex3d(x1, yEnd - cameraY, z1);
                GL11.glTexCoord2f(0.0F, v1);
                GL11.glVertex3d(x0, yEnd - cameraY, z0);
            }
        }
        GL11.glEnd();
    }
}

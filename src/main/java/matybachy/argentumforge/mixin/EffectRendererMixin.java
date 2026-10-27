package matybachy.argentumforge.mixin;

import matybachy.argentumforge.client.particle.ParticleCulling;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public abstract class EffectRendererMixin {
    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void argentumforge$beginParticleCulling(Entity entityIn, float partialTicks, CallbackInfo ci) {
        ParticleCulling.begin(
                entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks,
                entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks,
                entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks);
    }

    @Redirect(method = "renderParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void argentumforge$renderIfVisible(EntityFX particle, WorldRenderer buffer, Entity entityIn, float partialTicks,
                                               float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (ParticleCulling.shouldRender(particle)) {
            particle.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        }
    }
}

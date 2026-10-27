package matybachy.argentumforge.mixin;

import matybachy.argentumforge.client.font.FontWidthCache;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @Shadow private boolean unicodeFlag;

    @Unique private final FontWidthCache argentumforge$widthCache = new FontWidthCache();

    /** Subclasses may override getCharWidth with state this cache cannot observe, so only the vanilla class caches. */
    @Unique
    private boolean argentumforge$canCache() {
        return FontWidthCache.isEnabled() && ((Object) this).getClass() == FontRenderer.class;
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    private void argentumforge$cachedWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (!argentumforge$canCache()) return;
        int cached = argentumforge$widthCache.get(text);
        if (cached >= 0) cir.setReturnValue(Integer.valueOf(cached));
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"))
    private void argentumforge$storeWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (argentumforge$canCache()) argentumforge$widthCache.put(text, cir.getReturnValue().intValue());
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void argentumforge$clearOnReload(IResourceManager resourceManager, CallbackInfo ci) {
        argentumforge$widthCache.clear();
    }

    @Inject(method = "setUnicodeFlag", at = @At("HEAD"))
    private void argentumforge$clearOnUnicodeChange(boolean unicodeFlagIn, CallbackInfo ci) {
        if (unicodeFlagIn != unicodeFlag) argentumforge$widthCache.clear();
    }
}

package matybachy.argentumforge.core;

import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.File;
import java.util.Map;

/**
 * Coremod entry named by the jar manifest ({@code FMLCorePlugin}). It transforms nothing itself: the
 * manifest's {@code TweakClass} starts the Mixin tweaker, which applies {@code mixins.argentumforge.json}
 * and, through {@code ForceLoadAsMod}, keeps the {@code @Mod} class in this jar loadable.
 */
@IFMLLoadingPlugin.Name("ArgentumForgeCore")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class ArgentumForgeLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    /**
     * In the development environment the mod jar is on the classpath rather than in {@code mods/}. For
     * {@code ForceLoadAsMod}, Mixin's FML agent moves the jar to FML's reparseable coremods, which FML re-scans
     * only in the mods directory and skips on the classpath, so the {@code @Mod} class would never load.
     * Production (obfuscated) runs are left untouched: there the jar is in {@code mods/} and the reparse works.
     */
    @Override
    public void injectData(Map<String, Object> data) {
        if (!Boolean.FALSE.equals(data.get("runtimeDeobfuscationEnabled"))) return;
        try {
            File source = new File(ArgentumForgeLoadingPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!source.isFile()) return;
            CoreModManager.getIgnoredMods().remove(source.getName());
            CoreModManager.getReparseableCoremods().remove(source.getName());
        } catch (Exception ignored) {
            // Running from a classes directory: nothing was skipped.
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

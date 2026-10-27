package matybachy.argentumforge.client.compat;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;

/**
 * Runtime-only compatibility probes. No optional client is referenced at compile time,
 * so ordinary Forge installations remain loadable without FeatherOpt, OptiFine or Dawn.
 *
 * The class names below are taken from verified upstream sources:
 * - FeatherOpt declares {@code @Mod(modid = "featheropt")} on net.digitalingot.featheropt.FeatherOpt
 *   and uses net.digitalingot.featheropt.FeatherCoreMod as its FMLCorePlugin.
 * - OptiFine's Forge entry points are optifine.OptiFineForgeTweaker and optifine.Config.
 * - Dawn Client registers the Forge mod id {@code dawn} and launches through
 *   net.digitalingot.feather.launch.tweaker.FeatherTweaker. Its gg.dawn.feather.* classes are obfuscated and
 *   renamed between builds, so they are never probed.
 * Detection uses Class.forName(name, false, ...) with initialize=false, so it never triggers
 * static initialization of a foreign class, and every lookup is guarded.
 */
public final class CompatibilityEnvironment {
    private static final String FEATHEROPT_COREMOD = "net.digitalingot.featheropt.FeatherCoreMod";
    private static final String FEATHEROPT_MOD = "net.digitalingot.featheropt.FeatherOpt";
    private static final String OPTIFINE_TWEAKER = "optifine.OptiFineForgeTweaker";
    private static final String OPTIFINE_CONFIG = "optifine.Config";
    private static final String DAWN_MOD_ID = "dawn";
    private static final String DAWN_TWEAKER = "net.digitalingot.feather.launch.tweaker.FeatherTweaker";

    // Class presence cannot change after launch. Results are memoized because these checks run in
    // hot paths (per snapshot reservation and per frame), and a failed Class.forName throws.
    private static volatile Boolean featherOptPresent;
    private static volatile Boolean optiFinePresent;
    private static volatile Boolean dawnPresent;

    private CompatibilityEnvironment() {
    }

    public static boolean isFeatherOptPresent() {
        Boolean cached = featherOptPresent;
        if (cached == null) {
            cached = Boolean.valueOf(classPresent(FEATHEROPT_COREMOD) || classPresent(FEATHEROPT_MOD));
            featherOptPresent = cached;
        }
        return cached.booleanValue();
    }

    public static boolean isOptiFinePresent() {
        Boolean cached = optiFinePresent;
        if (cached == null) {
            cached = Boolean.valueOf(classPresent(OPTIFINE_TWEAKER) || classPresent(OPTIFINE_CONFIG));
            optiFinePresent = cached;
        }
        return cached.booleanValue();
    }

    /**
     * Dawn Client: Forge mod id {@code dawn}, or its launch tweaker class. A negative answer is memoized only once
     * FML has built the mod list (pre-initialization reached), because {@code Loader.isModLoaded} is false before that.
     */
    public static boolean isDawnPresent() {
        Boolean cached = dawnPresent;
        if (cached != null) return cached.booleanValue();
        boolean present = classPresent(DAWN_TWEAKER) || dawnModLoaded();
        if (present || modListFinal()) dawnPresent = Boolean.valueOf(present);
        return present;
    }

    private static boolean dawnModLoaded() {
        try {
            return Loader.isModLoaded(DAWN_MOD_ID);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean modListFinal() {
        try {
            return Loader.instance().hasReachedState(LoaderState.PREINITIALIZATION);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, CompatibilityEnvironment.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

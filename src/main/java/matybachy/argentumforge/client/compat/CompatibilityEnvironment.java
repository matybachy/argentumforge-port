package matybachy.argentumforge.client.compat;

/**
 * Runtime-only compatibility probes. No optional client is referenced at compile time,
 * so ordinary Forge installations remain loadable without FeatherOpt or OptiFine.
 *
 * The class names below are taken from verified upstream sources:
 * - FeatherOpt declares {@code @Mod(modid = "featheropt")} on net.digitalingot.featheropt.FeatherOpt
 *   and uses net.digitalingot.featheropt.FeatherCoreMod as its FMLCorePlugin.
 * - OptiFine's Forge entry points are optifine.OptiFineForgeTweaker and optifine.Config.
 * Detection uses Class.forName(name, false, ...) with initialize=false, so it never triggers
 * static initialization of a foreign class, and every lookup is guarded.
 */
public final class CompatibilityEnvironment {
    private static final String FEATHEROPT_COREMOD = "net.digitalingot.featheropt.FeatherCoreMod";
    private static final String FEATHEROPT_MOD = "net.digitalingot.featheropt.FeatherOpt";
    private static final String OPTIFINE_TWEAKER = "optifine.OptiFineForgeTweaker";
    private static final String OPTIFINE_CONFIG = "optifine.Config";

    private CompatibilityEnvironment() {
    }

    public static boolean isFeatherOptPresent() {
        return classPresent(FEATHEROPT_COREMOD) || classPresent(FEATHEROPT_MOD);
    }

    public static boolean isOptiFinePresent() {
        return classPresent(OPTIFINE_TWEAKER) || classPresent(OPTIFINE_CONFIG);
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

package matybachy.argentumforge.client.compat;

/**
 * Runtime-only compatibility probes. No optional client is referenced directly,
 * so ordinary Forge installations remain loadable without FeatherOpt or OptiFine.
 */
public final class CompatibilityEnvironment {
    private static final String FEATHEROPT_COREMOD = "net.digitalingot.featheropt.FeatherCoreMod";
    private static final String FEATHEROPT_MARKER = "net.digitalingot.featheropt.FeatherOpt";
    private static final String OPTIFINE_MARKER = "optifine.OptiFineForgeTweaker";

    private CompatibilityEnvironment() {
    }

    public static boolean isFeatherOptPresent() {
        return classPresent(FEATHEROPT_COREMOD) || classPresent(FEATHEROPT_MARKER);
    }

    public static boolean isOptiFinePresent() {
        return classPresent(OPTIFINE_MARKER) || classPresent("Config");
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

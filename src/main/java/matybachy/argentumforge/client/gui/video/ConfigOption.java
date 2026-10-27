package matybachy.argentumforge.client.gui.video;

import matybachy.argentumforge.ArgentumConfig;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * One ArgentumForge config key shown on the video settings screens. The Forge {@link Property} is the only source of
 * truth: the screen reads and writes it directly, and the property's type, valid values and min/max (from the
 * validation in {@link ArgentumConfig#sync()}) decide the widget and slider range.
 */
final class ConfigOption {
    enum Kind { TOGGLE, CYCLE, SLIDER }

    final String key;
    /** Changes the chunk renderer: applied only when the video settings are left, and labelled "(reload)". */
    final boolean reload;
    /** Used only by the legacy overlay terrain path: disabled while terrainMode is integrated. */
    final boolean overlayOnly;

    private ConfigOption(String key, boolean reload, boolean overlayOnly) {
        this.key = key;
        this.reload = reload;
        this.overlayOnly = overlayOnly;
    }

    static ConfigOption of(String key) {
        return new ConfigOption(key, false, false);
    }

    static ConfigOption reload(String key) {
        return new ConfigOption(key, true, false);
    }

    static ConfigOption overlayOnly(String key) {
        return new ConfigOption(key, false, true);
    }

    /** The backing property, looked up by key in every category; null if the key does not exist. */
    Property property() {
        Configuration cfg = ArgentumConfig.getConfiguration();
        if (cfg == null) return null;
        for (String category : cfg.getCategoryNames()) {
            Property property = cfg.getCategory(category).get(key);
            if (property != null) return property;
        }
        return null;
    }

    static Kind kindOf(Property property) {
        if (property.getType() == Property.Type.BOOLEAN) return Kind.TOGGLE;
        if (property.getType() == Property.Type.INTEGER) return Kind.SLIDER;
        String[] valid = property.getValidValues();
        return valid != null && valid.length > 0 ? Kind.CYCLE : null;
    }

    static int min(Property property) {
        return parse(property.getMinValue(), Integer.MIN_VALUE);
    }

    static int max(Property property) {
        return parse(property.getMaxValue(), Integer.MAX_VALUE);
    }

    private static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** True when the pending (not yet applied) terrainMode in the config is the integrated path. */
    static boolean pendingIntegratedMode() {
        Property mode = of("terrainMode").property();
        return mode == null || !ArgentumConfig.TERRAIN_MODE_OVERLAY.equalsIgnoreCase(mode.getString());
    }
}

package matybachy.argentumforge.client.font;

import matybachy.argentumforge.ArgentumConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded per-FontRenderer cache of {@code getStringWidth} results. Vanilla measures each character with a linear
 * {@code String.indexOf} over its 256-character table, so GUIs that measure the same strings every frame (chat,
 * tab list, scoreboard, tooltips) repeat that work. The owning renderer clears the cache whenever an input of the
 * width calculation changes: resource reload (glyph tables) and the unicode flag.
 */
public final class FontWidthCache {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_LENGTH = 256;

    private final ConcurrentHashMap<String, Integer> widths = new ConcurrentHashMap<String, Integer>();

    public static boolean isEnabled() {
        return ArgentumConfig.enableFontWidthCache;
    }

    /** Cached width, or -1 when the string is not cached. */
    public int get(String text) {
        if (text == null || text.length() > MAX_LENGTH) return -1;
        Integer width = widths.get(text);
        return width == null ? -1 : width.intValue();
    }

    public void put(String text, int width) {
        if (text == null || text.length() > MAX_LENGTH || width < 0) return;
        if (widths.size() >= MAX_ENTRIES) widths.clear();
        widths.put(text, Integer.valueOf(width));
    }

    public void clear() {
        widths.clear();
    }
}

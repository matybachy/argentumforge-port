package matybachy.argentumforge.client.terrain;

import matybachy.argentumforge.ArgentumConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Debug bounds checks for snapshot-backed region caches ({@code debugSnapshotBounds}). Vanilla
 * {@code RegionRenderCache.getBlockState} indexes a 20x20x20 array without a range check, so a read outside the
 * region throws or returns a wrong block. Clients that clamp that read (Dawn returns the default state) turn such a
 * bug into a silent rendering glitch. With the option on, every read outside the captured region is counted on F3
 * and the first ones are logged with the caller's stack trace. Worker threads call this concurrently.
 */
final class TerrainDebug {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final int MAX_LOGGED = 10;
    private static final AtomicLong VIOLATIONS = new AtomicLong();

    private TerrainDebug() {
    }

    static boolean isEnabled() {
        return ArgentumConfig.debugSnapshotBounds;
    }

    static void reportOutOfRegion(String access, int x, int y, int z, SectionSnapshot snapshot) {
        long count = VIOLATIONS.incrementAndGet();
        if (count <= MAX_LOGGED) {
            LOGGER.error("[debugSnapshotBounds] {} at {},{},{} is outside snapshot region {} ({} of first {} logged)",
                    access, x, y, z, snapshot.describeRegion(), count, MAX_LOGGED, new Throwable("caller"));
        }
    }

    static void appendDebugLines(List<String> lines) {
        if (!isEnabled()) return;
        lines.add("[Argentum] debugSnapshotBounds: " + VIOLATIONS.get() + " out-of-region reads logged");
    }
}

package matybachy.argentumforge.client.terrain;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Lock-free counters for the integrated terrain path, shown on the F3 screen. */
public final class TerrainStats {
    private static final AtomicLong SNAPSHOT_BUILDS = new AtomicLong();
    private static final AtomicLong VANILLA_REGION_BUILDS = new AtomicLong();
    private static final AtomicLong BUDGET_FALLBACKS = new AtomicLong();
    private static final AtomicLong CAPACITY_FALLBACKS = new AtomicLong();
    private static final AtomicLong OUT_OF_SNAPSHOT_READS = new AtomicLong();
    private static final AtomicLong CAPTURE_FAILURES = new AtomicLong();
    private static final AtomicLong CULLED_SECTION_LAYERS = new AtomicLong();

    private TerrainStats() {
    }

    static void recordSnapshotBuild() { SNAPSHOT_BUILDS.incrementAndGet(); }
    static void recordVanillaRegionBuild() { VANILLA_REGION_BUILDS.incrementAndGet(); }
    static void recordBudgetFallback() { BUDGET_FALLBACKS.incrementAndGet(); }
    static void recordCapacityFallback() { CAPACITY_FALLBACKS.incrementAndGet(); }
    static void recordOutOfSnapshotRead() { OUT_OF_SNAPSHOT_READS.incrementAndGet(); }
    static void recordCaptureFailure() { CAPTURE_FAILURES.incrementAndGet(); }
    static void recordCulledSectionLayer() { CULLED_SECTION_LAYERS.incrementAndGet(); }

    static void reset() {
        SNAPSHOT_BUILDS.set(0L);
        VANILLA_REGION_BUILDS.set(0L);
        BUDGET_FALLBACKS.set(0L);
        CAPACITY_FALLBACKS.set(0L);
        OUT_OF_SNAPSHOT_READS.set(0L);
        CAPTURE_FAILURES.set(0L);
        CULLED_SECTION_LAYERS.set(0L);
    }

    public static void appendDebugLines(List<String> lines, int pendingSnapshots) {
        lines.add("[Argentum] terrain: snapshot builds " + SNAPSHOT_BUILDS.get()
                + ", vanilla-cache builds " + VANILLA_REGION_BUILDS.get()
                + ", pending " + pendingSnapshots);
        lines.add("[Argentum] fallbacks: budget " + BUDGET_FALLBACKS.get()
                + ", capacity " + CAPACITY_FALLBACKS.get()
                + ", out-of-region reads " + OUT_OF_SNAPSHOT_READS.get()
                + ", capture failures " + CAPTURE_FAILURES.get()
                + ", bounds-culled layers " + CULLED_SECTION_LAYERS.get());
    }
}

package matybachy.argentumforge.client.terrain;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import matybachy.argentumforge.client.compat.CompatibilityEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.ListChunkFactory;
import net.minecraft.client.renderer.chunk.VboChunkFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Installs ArgentumForge's section compiler into the real RenderGlobal terrain lifecycle.
 *
 * <p>RenderGlobal keeps its chunk factory ({@code renderChunkFactory}, SRG {@code field_175007_a})
 * and draw container ({@code renderContainer}, SRG {@code field_174996_N}) in private fields and
 * passes the factory to every new ViewFrustum in {@code loadRenderers}. Replacing both and calling
 * {@code loadRenderers()} makes every terrain section an {@link ArgentumRenderChunk} or
 * {@link ArgentumListedRenderChunk}, compiled by the vanilla worker threads, uploaded by the vanilla
 * dispatcher on the client thread and drawn by the vanilla layer passes. No overlay is drawn and no
 * class is transformed. loadRenderers only reassigns these fields when the VBO setting flips; the
 * client-tick check reinstalls in that case. Any failure restores vanilla objects.</p>
 */
public final class IntegratedTerrain {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final int MAX_SNAPSHOT_FAILURES = 16;
    private static final int MAX_REINSTALLS_PER_WINDOW = 4;
    private static final int REINSTALL_WINDOW_TICKS = 200;

    private static final AtomicInteger OUTSTANDING_SNAPSHOTS = new AtomicInteger();
    private static final AtomicInteger SNAPSHOT_FAILURES = new AtomicInteger();
    private static volatile boolean installed;
    private static volatile boolean installedWithVbo;
    private static volatile boolean snapshotsDisabledByFailure;
    private static boolean installDisabledByFailure;
    private static int capturesThisFrame;
    private static int reinstallsInWindow;
    private static int windowTicks;
    private static Field containerField;
    private static Field factoryField;

    private IntegratedTerrain() {
    }

    public static boolean isRequested() {
        return ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER
                && ArgentumConfig.isIntegratedTerrainMode()
                && !ArgentumConfig.isExperimentalRendererDisabledByCompatibility()
                && !CompatibilityEnvironment.isOptiFinePresent();
    }

    public static boolean isInstalled() {
        return installed;
    }

    /** True when the installed objects are the VBO variants; meaningful only while {@link #isInstalled()}. */
    public static boolean isInstalledWithVbo() {
        return installedWithVbo;
    }

    static boolean isBoundsCullingActive() {
        return installed && ArgentumConfig.enableTerrainBoundsCulling;
    }

    /** Client world unload: release every pending snapshot (they pin chunks of the old world). */
    public static void onClientWorldUnload() {
        SnapshotCompileSupport.discardAll();
    }

    /** RenderTickEvent START: resets the per-frame snapshot capture budget. */
    public static void onRenderFrameStart() {
        capturesThisFrame = 0;
    }

    /** ClientTickEvent END. */
    public static void onClientTick(Minecraft mc) {
        if (++windowTicks >= REINSTALL_WINDOW_TICKS) {
            windowTicks = 0;
            reinstallsInWindow = 0;
        }
        if (mc == null || mc.theWorld == null || mc.renderGlobal == null) return;
        RenderGlobal renderGlobal = mc.renderGlobal;
        try {
            if (isRequested() && !installDisabledByFailure) {
                ensureInstalled(renderGlobal);
            } else if (installed) {
                restoreVanilla(renderGlobal);
            }
        } catch (Throwable failure) {
            installDisabledByFailure = true;
            LOGGER.error("Integrated terrain installation failed; restoring the vanilla chunk renderer", failure);
            try {
                restoreVanilla(renderGlobal);
            } catch (Throwable restoreFailure) {
                LOGGER.error("Could not restore the vanilla chunk renderer objects", restoreFailure);
            }
        }
    }

    private static void ensureInstalled(RenderGlobal renderGlobal) throws IllegalAccessException {
        resolveFields();
        boolean vbo = OpenGlHelper.useVbo();
        Object factory = factoryField.get(renderGlobal);
        Object container = containerField.get(renderGlobal);
        boolean factoryOk = factory instanceof ArgentumRenderChunkFactory
                && ((ArgentumRenderChunkFactory) factory).usesVbo() == vbo;
        boolean containerOk = vbo ? container instanceof ArgentumVboRenderList
                : container instanceof ArgentumDisplayListRenderList;
        if (factoryOk && containerOk) {
            installed = true;
            installedWithVbo = vbo;
            return;
        }
        if (++reinstallsInWindow > MAX_REINSTALLS_PER_WINDOW) {
            throw new IllegalStateException("RenderGlobal keeps replacing the integrated terrain objects");
        }

        ChunkRenderContainer replacementContainer = vbo ? new ArgentumVboRenderList() : new ArgentumDisplayListRenderList();
        containerField.set(renderGlobal, replacementContainer);
        factoryField.set(renderGlobal, new ArgentumRenderChunkFactory(vbo));
        installed = true;
        installedWithVbo = vbo;
        renderGlobal.loadRenderers();
        LOGGER.info("Integrated terrain compiler installed ({} mode)", vbo ? "VBO" : "display-list");
    }

    private static void restoreVanilla(RenderGlobal renderGlobal) throws IllegalAccessException {
        installed = false;
        resolveFields();
        boolean vbo = OpenGlHelper.useVbo();
        ChunkRenderContainer container = vbo ? new VboRenderList() : new RenderList();
        IRenderChunkFactory factory = vbo ? new VboChunkFactory() : new ListChunkFactory();
        containerField.set(renderGlobal, container);
        factoryField.set(renderGlobal, factory);
        renderGlobal.loadRenderers();
        SnapshotCompileSupport.discardAll();
        TerrainStats.reset();
        LOGGER.info("Vanilla chunk renderer objects restored");
    }

    private static void resolveFields() {
        if (containerField == null) {
            containerField = ReflectionHelper.findField(RenderGlobal.class, "renderContainer", "field_174996_N");
        }
        if (factoryField == null) {
            factoryField = ReflectionHelper.findField(RenderGlobal.class, "renderChunkFactory", "field_175007_a");
        }
    }

    /** Client thread only. Reserves one snapshot against the per-frame budget and memory cap. */
    static boolean tryReserveSnapshot() {
        if (!installed || snapshotsDisabledByFailure || !isRequested()) return false;
        int budget = ArgentumConfig.terrainSnapshotsPerFrame;
        if (budget <= 0) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !mc.isCallingFromMinecraftThread()) return false;
        if (capturesThisFrame >= budget) {
            TerrainStats.recordBudgetFallback();
            return false;
        }
        if (OUTSTANDING_SNAPSHOTS.incrementAndGet() > ArgentumConfig.terrainMaxPendingSnapshots) {
            OUTSTANDING_SNAPSHOTS.decrementAndGet();
            TerrainStats.recordCapacityFallback();
            return false;
        }
        capturesThisFrame++;
        return true;
    }

    static void releaseSnapshot() {
        while (true) {
            int current = OUTSTANDING_SNAPSHOTS.get();
            if (current <= 0 || OUTSTANDING_SNAPSHOTS.compareAndSet(current, current - 1)) return;
        }
    }

    static void onSnapshotFailure(RuntimeException failure) {
        TerrainStats.recordCaptureFailure();
        int failures = SNAPSHOT_FAILURES.incrementAndGet();
        if (failures == 1) {
            LOGGER.warn("Section snapshot capture failed; that section uses the vanilla region cache", failure);
        }
        if (failures >= MAX_SNAPSHOT_FAILURES && !snapshotsDisabledByFailure) {
            snapshotsDisabledByFailure = true;
            LOGGER.error("Disabling section snapshots after {} capture failures; vanilla region caches remain in use", failures);
        }
    }

    public static void appendDebugLines(List<String> lines) {
        if (!installed) return;
        TerrainStats.appendDebugLines(lines, OUTSTANDING_SNAPSHOTS.get());
    }
}

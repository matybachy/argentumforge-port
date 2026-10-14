package matybachy.argentumforge.client.blockentity;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityBannerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnchantmentTableRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityMobSpawnerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Opt-in distance limit for a fixed set of vanilla block-entity renderers (chest, ender chest, sign,
 * skull, banner, mob spawner, enchantment table). Pistons, beacons and end portals are excluded:
 * they draw moving blocks, global beams or portal surfaces that must stay visible.
 *
 * <p>Forge 1.8.9 has no cancelable tile-entity render event. TileEntityRendererDispatcher looks
 * renderers up in its private {@code mapSpecialRenderers} (SRG {@code field_147559_m}), so the entries
 * for the exact vanilla classes below are replaced with {@link DistanceCulledTileEntityRenderer}
 * wrappers - only if the registered renderer is still the exact vanilla renderer class. Modded
 * subclasses of these block entities, item-form renders (no world), block-breaking overlays and
 * global renderers (beacons) are never skipped. Disabling or a failure restores the original
 * renderer instances.</p>
 */
public final class BlockEntityDistanceCulling {
    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final Class<?>[][] TARGETS = {
            {TileEntityChest.class, TileEntityChestRenderer.class},
            {TileEntityEnderChest.class, TileEntityEnderChestRenderer.class},
            {TileEntitySign.class, TileEntitySignRenderer.class},
            {TileEntitySkull.class, TileEntitySkullRenderer.class},
            {TileEntityBanner.class, TileEntityBannerRenderer.class},
            // Spinning spawner entity and floating book: purely decorative, but render an entity/model per frame.
            {TileEntityMobSpawner.class, TileEntityMobSpawnerRenderer.class},
            {TileEntityEnchantmentTable.class, TileEntityEnchantmentTableRenderer.class},
    };
    private static final Map<Class<?>, Boolean> CULLABLE_CLASSES = new IdentityHashMap<Class<?>, Boolean>();

    static {
        for (Class<?>[] target : TARGETS) CULLABLE_CLASSES.put(target[0], Boolean.TRUE);
    }

    private static boolean installed;
    private static boolean disabledByFailure;

    private BlockEntityDistanceCulling() {
    }

    public static boolean isRequested() {
        return ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER
                && ArgentumConfig.enableBlockEntityHooks
                && ArgentumConfig.enableBlockEntityDistanceCulling
                && !disabledByFailure;
    }

    /** Client tick: keeps the wrapper installation in sync with configuration. */
    public static void onClientTick() {
        boolean wanted = isRequested();
        if (wanted == installed) return;
        try {
            if (wanted) install();
            else uninstall();
        } catch (Throwable failure) {
            disabledByFailure = true;
            LOGGER.error("Block-entity distance culling failed; restoring vanilla renderers", failure);
            try {
                uninstall();
            } catch (Throwable ignored) {
            }
        }
    }

    static boolean shouldSkip(TileEntity te, int destroyStage) {
        if (!installed || te == null || destroyStage >= 0) return false;
        if (!CULLABLE_CLASSES.containsKey(te.getClass())) return false;
        if (!te.hasWorldObj()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || te.getWorld() != mc.theWorld) return false;
        double limit = ArgentumConfig.blockEntityCullingDistance;
        return te.getDistanceSq(TileEntityRendererDispatcher.staticPlayerX,
                TileEntityRendererDispatcher.staticPlayerY,
                TileEntityRendererDispatcher.staticPlayerZ) > limit * limit;
    }

    @SuppressWarnings("unchecked")
    private static void install() {
        Map<Class<?>, TileEntitySpecialRenderer<?>> map = rendererMap();
        int wrapped = 0;
        for (Class<?>[] target : TARGETS) {
            TileEntitySpecialRenderer<?> current = map.get(target[0]);
            if (current == null || current.getClass() != target[1]) continue;
            map.put(target[0], new DistanceCulledTileEntityRenderer((TileEntitySpecialRenderer<TileEntity>) current));
            wrapped++;
        }
        installed = true;
        LOGGER.info("Block-entity distance culling wrapped {} vanilla renderers", wrapped);
    }

    private static void uninstall() {
        installed = false;
        Map<Class<?>, TileEntitySpecialRenderer<?>> map = rendererMap();
        for (Map.Entry<Class<?>, TileEntitySpecialRenderer<?>> entry : map.entrySet()) {
            if (entry.getValue() instanceof DistanceCulledTileEntityRenderer) {
                entry.setValue(((DistanceCulledTileEntityRenderer) entry.getValue()).getDelegate());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, TileEntitySpecialRenderer<?>> rendererMap() {
        Field field = ReflectionHelper.findField(TileEntityRendererDispatcher.class, "mapSpecialRenderers", "field_147559_m");
        try {
            return (Map<Class<?>, TileEntitySpecialRenderer<?>>) field.get(TileEntityRendererDispatcher.instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

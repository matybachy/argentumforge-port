package matybachy.argentumforge.client.entity;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.ArgentumForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.entity.RenderXPOrb;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Opt-in distance limit for a fixed set of vanilla non-living entities. Forge 1.8.9 has no cancelable
 * render event for them, so the exact vanilla renderer instances in
 * {@code RenderManager.entityRenderMap} ({@code field_78729_o}) are wrapped; the wrapper only answers
 * {@code shouldRender} differently. A renderer that another mod has already replaced is left alone.
 */
public final class NonLivingEntityCulling {
    private static final Object[][] TARGETS = {
            {EntityArrow.class, RenderArrow.class},
            {EntityXPOrb.class, RenderXPOrb.class},
            {EntitySnowball.class, RenderSnowball.class},
            {EntityEgg.class, RenderSnowball.class},
            {EntityEnderPearl.class, RenderSnowball.class},
            {EntityExpBottle.class, RenderSnowball.class},
    };
    private static final Set<Class<?>> EXACT = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());

    static {
        for (Object[] target : TARGETS) EXACT.add((Class<?>) target[0]);
    }

    private static final Logger LOGGER = LogManager.getLogger("ArgentumForge");
    private static final List<Class<?>> wrappedKeys = new ArrayList<Class<?>>();
    private static Field renderMapField;
    private static boolean failed;
    private static boolean installAttempted;
    private static volatile boolean active;
    private static volatile double maxDistanceSq;

    private NonLivingEntityCulling() {
    }

    private static boolean isRequested() {
        return ArgentumForge.ENABLE_EXPERIMENTAL_CHUNK_RENDERER
                && ArgentumConfig.enableEntityHooks
                && ArgentumConfig.enableNonLivingEntityCulling;
    }

    /** Client tick: install or remove wrappers to follow the configuration. */
    public static void onClientTick() {
        double distance = ArgentumConfig.nonLivingEntityCullingDistance;
        maxDistanceSq = distance * distance;
        boolean requested = isRequested() && !failed;
        if (requested && !installAttempted) {
            installAttempted = true;
            install();
        } else if (!requested && installAttempted) {
            uninstall();
            installAttempted = false;
        }
        active = requested && !failed && !wrappedKeys.isEmpty();
    }

    static boolean shouldSkip(Entity entity, double camX, double camY, double camZ) {
        if (!active || entity == null || !EXACT.contains(entity.getClass())) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (entity.worldObj != mc.theWorld) return false;
        if (entity.riddenByEntity != null || entity.ridingEntity != null) return false;
        if (entity.hasCustomName() || entity.getAlwaysRenderNameTag()) return false;
        double dx = entity.posX - camX;
        double dy = entity.posY - camY;
        double dz = entity.posZ - camZ;
        return dx * dx + dy * dy + dz * dz > maxDistanceSq;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Render<?>> renderMap(RenderManager manager) throws IllegalAccessException {
        if (renderMapField == null) {
            renderMapField = ReflectionHelper.findField(RenderManager.class, "entityRenderMap", "field_78729_o");
        }
        return (Map<Class<?>, Render<?>>) renderMapField.get(manager);
    }

    @SuppressWarnings("unchecked")
    private static void install() {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        if (manager == null) return;
        Map<Class<?>, Render<?>> map = null;
        try {
            map = renderMap(manager);
            for (Object[] target : TARGETS) {
                Class<?> key = (Class<?>) target[0];
                Render<?> current = map.get(key);
                // Exact class match only: subclasses or mod replacements keep their own behavior.
                if (current == null || current.getClass() != target[1]) continue;
                map.put(key, new DistanceCulledEntityRenderer(manager, (Render<Entity>) current));
                wrappedKeys.add(key);
            }
            if (!wrappedKeys.isEmpty()) LOGGER.info("Non-living entity culling wrapped {} vanilla renderers", wrappedKeys.size());
        } catch (Exception e) {
            // Roll back immediately so no partially wrapped map survives the failure.
            if (map != null) restoreWrappers(map);
            wrappedKeys.clear();
            fail(e);
        }
    }

    private static void uninstall() {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        try {
            if (manager != null) restoreWrappers(renderMap(manager));
        } catch (Exception e) {
            fail(e);
        } finally {
            wrappedKeys.clear();
            active = false;
        }
    }

    /**
     * Replaces every value that is one of our wrappers with its original renderer. This also covers
     * subclass keys that RenderManager.getEntityClassRenderObject cached with the wrapper as value.
     * Values installed by other mods are never touched.
     */
    private static void restoreWrappers(Map<Class<?>, Render<?>> map) {
        for (Map.Entry<Class<?>, Render<?>> entry : map.entrySet()) {
            Render<?> value = entry.getValue();
            if (value instanceof DistanceCulledEntityRenderer) {
                entry.setValue(((DistanceCulledEntityRenderer) value).getDelegate());
            }
        }
    }

    private static void fail(Exception e) {
        failed = true;
        active = false;
        LOGGER.warn("ArgentumForge non-living entity culling disabled: {}", e.toString());
    }
}

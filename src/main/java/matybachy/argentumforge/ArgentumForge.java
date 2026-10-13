package matybachy.argentumforge;

import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = ArgentumForge.MODID, name = ArgentumForge.NAME, version = ArgentumForge.VERSION)
public class ArgentumForge {
    public static final String MODID = "argentumforge";
    public static final String NAME = "Argentum Forge Port";
    public static final String VERSION = "0.0.1";
    public static final ChunkMeshManager MESH_MANAGER = new ChunkMeshManager();
    public static boolean ENABLE_EXPERIMENTAL_CHUNK_RENDERER = true;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // common pre-init not used here; client proxy handles renderer setup
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}

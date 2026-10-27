package matybachy.argentumforge;

import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = ArgentumForge.MODID, name = ArgentumForge.NAME, version = ArgentumForge.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true,
        guiFactory = "matybachy.argentumforge.client.gui.ArgentumGuiFactory")
public class ArgentumForge {
    public static final String MODID = "argentumforge";
    public static final String NAME = "Argentum Forge Port";
    public static final String VERSION = "0.1.0";
    public static final ChunkMeshManager MESH_MANAGER = new ChunkMeshManager();
    public static boolean ENABLE_EXPERIMENTAL_CHUNK_RENDERER = false;

    @SidedProxy(clientSide = "matybachy.argentumforge.ClientProxy", serverSide = "matybachy.argentumforge.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ArgentumConfig.load(event);
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }
}

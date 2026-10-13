package matybachy.argentumforge;

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

    public static final CommonProxy proxy = new CommonProxy();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
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

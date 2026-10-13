package matybachy.argentumforge;

import matybachy.argentumforge.client.ArgentumRenderer;
import matybachy.argentumforge.client.mesh.ChunkMeshManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    private ArgentumRenderer renderer;
    private final ChunkMeshManager meshManager = new ChunkMeshManager();

    @Override
    public void preInit() {
        renderer = new ArgentumRenderer();
        meshManager.setEnabled(true);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        // client init
    }

    @Override
    public void postInit() {
        // client post-init
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            meshManager.onTick();
        }
    }

    public ChunkMeshManager getMeshManager() {
        return meshManager;
    }
}

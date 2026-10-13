package matybachy.argentumforge;

import matybachy.argentumforge.client.ArgentumRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    private ArgentumRenderer renderer;

    @Override
    public void preInit() {
        renderer = new ArgentumRenderer();
    }

    @Override
    public void init() {
        // client init
    }

    @Override
    public void postInit() {
        // client post-init
    }
}

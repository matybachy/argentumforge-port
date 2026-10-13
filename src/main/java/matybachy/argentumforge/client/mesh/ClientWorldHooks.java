package matybachy.argentumforge.client.mesh;

import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientWorldHooks {
    private final ChunkMeshManager manager;

    public ClientWorldHooks(ChunkMeshManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world != null && event.world.isRemote) manager.onWorldChanged(event.world);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) manager.dispose();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world != null && event.world.isRemote && event.getChunk() != null) {
            manager.markDirty(event.getChunk().xPosition, event.getChunk().zPosition, 1);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.world != null && event.world.isRemote && event.getChunk() != null) {
            manager.unloadChunk(event.getChunk().xPosition, event.getChunk().zPosition);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        markBlock(event.world, event.pos.getX(), event.pos.getZ());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        markBlock(event.world, event.pos.getX(), event.pos.getZ());
    }

    private void markBlock(World world, int x, int z) {
        if (world != null && world.isRemote) manager.markBlockDirty(x, z);
    }
}

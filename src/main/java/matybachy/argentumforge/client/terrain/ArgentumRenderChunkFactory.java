package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/** Replacement for VboChunkFactory/ListChunkFactory used by RenderGlobal's ViewFrustum. */
public final class ArgentumRenderChunkFactory implements IRenderChunkFactory {
    private final boolean vbo;

    public ArgentumRenderChunkFactory(boolean vbo) {
        this.vbo = vbo;
    }

    public boolean usesVbo() {
        return vbo;
    }

    @Override
    public RenderChunk makeRenderChunk(World worldIn, RenderGlobal globalRenderer, BlockPos pos, int index) {
        return vbo
                ? new ArgentumRenderChunk(worldIn, globalRenderer, pos, index)
                : new ArgentumListedRenderChunk(worldIn, globalRenderer, pos, index);
    }
}

package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumWorldBlockLayer;

/**
 * {@link VboRenderList} that drops section layers whose recorded per-layer geometry bounds lie entirely outside the
 * camera's side planes. Drawing itself is unchanged vanilla code.
 */
public final class ArgentumVboRenderList extends VboRenderList {
    private final TerrainVisibilityFilter filter = new TerrainVisibilityFilter();

    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
        filter.prepare(viewEntityXIn, viewEntityYIn, viewEntityZIn);
    }

    @Override
    public void addRenderChunk(RenderChunk renderChunkIn, EnumWorldBlockLayer layer) {
        if (filter.shouldDraw(renderChunkIn, layer)) super.addRenderChunk(renderChunkIn, layer);
    }
}

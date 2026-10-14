package matybachy.argentumforge.client.terrain;

import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumWorldBlockLayer;

/**
 * {@link RenderList} that drops section layers whose recorded geometry bounds lie entirely outside the
 * camera's side planes. Drawing itself is unchanged vanilla code.
 */
public final class ArgentumDisplayListRenderList extends RenderList {
    private final TerrainVisibilityFilter filter = new TerrainVisibilityFilter();

    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
        filter.prepare(viewEntityXIn, viewEntityYIn, viewEntityZIn);
    }

    @Override
    public void addRenderChunk(RenderChunk renderChunkIn, EnumWorldBlockLayer layer) {
        if (filter.shouldDraw(renderChunkIn)) super.addRenderChunk(renderChunkIn, layer);
    }
}

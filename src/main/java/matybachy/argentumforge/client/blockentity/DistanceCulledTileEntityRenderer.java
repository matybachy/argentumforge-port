package matybachy.argentumforge.client.blockentity;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/**
 * Delegating wrapper around one vanilla tile-entity renderer instance. It only skips a draw when
 * {@link BlockEntityDistanceCulling#shouldSkip} says the in-world block entity is beyond the
 * configured distance; every other call goes to the original renderer unchanged.
 */
final class DistanceCulledTileEntityRenderer extends TileEntitySpecialRenderer<TileEntity> {
    private final TileEntitySpecialRenderer<TileEntity> delegate;

    DistanceCulledTileEntityRenderer(TileEntitySpecialRenderer<TileEntity> delegate) {
        this.delegate = delegate;
        this.rendererDispatcher = TileEntityRendererDispatcher.instance;
    }

    TileEntitySpecialRenderer<TileEntity> getDelegate() {
        return delegate;
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
        if (BlockEntityDistanceCulling.shouldSkip(te, destroyStage)) return;
        delegate.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage);
    }

    @Override
    public void renderTileEntityFast(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage,
                                     WorldRenderer worldRenderer) {
        if (BlockEntityDistanceCulling.shouldSkip(te, destroyStage)) return;
        delegate.renderTileEntityFast(te, x, y, z, partialTicks, destroyStage, worldRenderer);
    }

    @Override
    public void setRendererDispatcher(TileEntityRendererDispatcher rendererDispatcherIn) {
        super.setRendererDispatcher(rendererDispatcherIn);
        delegate.setRendererDispatcher(rendererDispatcherIn);
    }

    @Override
    public boolean forceTileEntityRender() {
        return delegate.forceTileEntityRender();
    }
}

package matybachy.argentumforge.client.compat;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public interface TextureLookup {
    void bindTerrainAtlas();

    TerrainFaceData resolveFace(World world, BlockPos pos, IBlockState state, EnumFacing face);

    Pass classifyPass(Block block);

    float getAlpha(Block block, Pass pass);

    boolean isEmissive(Block block);

    enum Pass {
        SOLID,
        CUTOUT,
        TRANSLUCENT
    }

    final class TerrainFaceData {
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;
        public final float r;
        public final float g;
        public final float b;

        public TerrainFaceData(float u0, float v0, float u1, float v1, float r, float g, float b) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}

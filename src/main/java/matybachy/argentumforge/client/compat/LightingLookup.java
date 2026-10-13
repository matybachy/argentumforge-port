package matybachy.argentumforge.client.compat;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public interface LightingLookup {
    float[] sampleVertexLight(World world, BlockPos pos, EnumFacing face, Block block, boolean emissive);
}

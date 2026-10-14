package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.LightingLookup;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public final class TerrainLighting implements LightingLookup {
    private static final TerrainLighting INSTANCE = new TerrainLighting();

    private TerrainLighting() {
    }

    public static TerrainLighting getInstance() {
        return INSTANCE;
    }

    @Override
    public float[] sampleVertexLight(World world, BlockPos pos, EnumFacing face, Block block, boolean emissive) {
        float[] values = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
        if (!ArgentumConfig.enableLightingAo || world == null || pos == null) return values;
        if (emissive) return values;

        BlockPos facePos = pos.offset(face);
        float base = sampleCombined(world, facePos, 0);
        float ao0 = sampleAo(world, facePos, face, -1, -1);
        float ao1 = sampleAo(world, facePos, face, 1, -1);
        float ao2 = sampleAo(world, facePos, face, 1, 1);
        float ao3 = sampleAo(world, facePos, face, -1, 1);
        values[0] = clamp(base * ao0);
        values[1] = clamp(base * ao1);
        values[2] = clamp(base * ao2);
        values[3] = clamp(base * ao3);
        return values;
    }

    public static float getFaceLight(World world, BlockPos pos, EnumFacing face, Block block, boolean emissive) {
        return INSTANCE.sampleVertexLight(world, pos, face, block, emissive)[0];
    }

    /**
     * Calculates lightmap UV coordinates (0..1 normalized) for block and sky light.
     */
    public float[] sampleLightmapUV(World world, BlockPos pos) {
        if (world == null || pos == null) return new float[]{0.0f, 0.0f};
        int combined = world.getCombinedLight(pos, 0);
        int sky = (combined >> 20) & 0xF;
        int block = (combined >> 4) & 0xF;
        return new float[]{(block * 16 + 8) / 256.0f, (sky * 16 + 8) / 256.0f};
    }

    private float sampleCombined(World world, BlockPos pos, int minBlockLight) {
        int combined = world.getCombinedLight(pos, minBlockLight);
        int sky = (combined >> 20) & 0xF;
        int block = (combined >> 4) & 0xF;
        int level = Math.max(sky, block);
        return 0.2f + (level / 15.0f) * 0.8f;
    }

    private float sampleAo(World world, BlockPos pos, EnumFacing face, int side, int up) {
        BlockPos sidePos = offsetForFace(pos, face, side, 0);
        BlockPos upPos = offsetForFace(pos, face, 0, up);
        BlockPos cornerPos = offsetForFace(pos, face, side, up);
        float sideAo = sampleBlockAo(world, sidePos);
        float upAo = sampleBlockAo(world, upPos);
        float cornerAo = sampleBlockAo(world, cornerPos);
        return clamp((sideAo + upAo + cornerAo + 1.0f) * 0.25f);
    }

    private BlockPos offsetForFace(BlockPos pos, EnumFacing face, int side, int up) {
        switch (face) {
            case UP:
            case DOWN:
                return pos.add(side, 0, up);
            case NORTH:
            case SOUTH:
                return pos.add(side, up, 0);
            case EAST:
            case WEST:
            default:
                return pos.add(0, up, side);
        }
    }

    private float clamp(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }

    private float sampleBlockAo(World world, BlockPos pos) {
        Block sample = world.getBlockState(pos).getBlock();
        if (sample == null) return 1.0f;
        float ao = sample.getAmbientOcclusionLightValue();
        if (sample.isOpaqueCube()) ao *= 0.85f;
        return clamp(ao);
    }
}

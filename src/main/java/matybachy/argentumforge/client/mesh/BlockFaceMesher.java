package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.ArgentumConfig;
import matybachy.argentumforge.client.compat.TextureLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class BlockFaceMesher {
    private final TerrainTextureAtlas textureLookup = TerrainTextureAtlas.getInstance();
    private final TerrainLighting lightingLookup = TerrainLighting.getInstance();

    public MeshData buildChunkMesh(World world, int chunkX, int chunkZ) {
        List<Float> solid = new ArrayList<Float>();
        List<Float> cutout = new ArrayList<Float>();
        List<Float> translucent = new ArrayList<Float>();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    if (block == null || block == Blocks.air || block.getMaterial() == Material.air) continue;

                    TextureLookup.Pass pass = textureLookup.classifyPass(block);
                    if (!ArgentumConfig.enableTransparency && pass != TextureLookup.Pass.SOLID) {
                        pass = TextureLookup.Pass.SOLID;
                    }

                    List<Float> target = pass == TextureLookup.Pass.TRANSLUCENT
                            ? translucent
                            : (pass == TextureLookup.Pass.CUTOUT ? cutout : solid);

                    if (block.getMaterial().isLiquid()) {
                        emitFluid(world, target, pos, state, block, pass);
                    } else if (emitModelQuads(world, target, pos, state, block, pass)) {
                        continue;
                    } else if (isCrossModel(block)) {
                        emitCross(world, target, pos, state, block, pass);
                    } else {
                        emitBox(world, target, pos, state, block, pass);
                    }
                }
            }
        }

        return new MeshData(toFloatArray(solid), toFloatArray(cutout), toFloatArray(translucent));
    }

    private boolean isCrossModel(Block block) {
        return block instanceof BlockBush
                || block instanceof BlockCrops
                || block instanceof BlockReed
                || block instanceof BlockVine
                || block instanceof BlockTorch;
    }

    private boolean emitModelQuads(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, TextureLookup.Pass pass) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        try {
            BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
            if (dispatcher == null) return false;
            IBakedModel model = dispatcher.getModelFromBlockState(state, world, pos);
            if (model == null) return false;

            boolean emitted = false;
            for (EnumFacing face : EnumFacing.values()) {
                if (!isFaceVisible(world, pos, block, face)) continue;
                List<BakedQuad> faceQuads = model.getFaceQuads(face);
                if (faceQuads == null || faceQuads.isEmpty()) continue;
                for (BakedQuad quad : faceQuads) {
                    emitted |= emitBakedQuad(world, vertices, pos, state, block, pass, face, quad);
                }
            }

            List<BakedQuad> generalQuads = model.getGeneralQuads();
            if (generalQuads != null && !generalQuads.isEmpty()) {
                for (BakedQuad quad : generalQuads) {
                    EnumFacing quadFace = quad != null && quad.getFace() != null ? quad.getFace() : EnumFacing.UP;
                    emitted |= emitBakedQuad(world, vertices, pos, state, block, pass, quadFace, quad);
                }
            }
            return emitted;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean emitBakedQuad(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block,
                                  TextureLookup.Pass pass, EnumFacing face, BakedQuad quad) {
        if (quad == null) return false;
        int[] data = quad.getVertexData();
        if (data == null || data.length < 4) return false;
        int stride = data.length / 4;
        if (stride < 6) return false;

        float[] light = lightingLookup.sampleVertexLight(world, pos, face, block, textureLookup.isEmissive(block));
        float alpha = textureLookup.getAlpha(block, pass);
        float[] tint = resolveTint(world, pos, state, quad);

        int[] order = new int[]{0, 1, 2, 0, 2, 3};
        for (int i = 0; i < order.length; i++) {
            int vertexIndex = order[i];
            int offset = vertexIndex * stride;
            float x = pos.getX() + Float.intBitsToFloat(data[offset]);
            float y = pos.getY() + Float.intBitsToFloat(data[offset + 1]);
            float z = pos.getZ() + Float.intBitsToFloat(data[offset + 2]);
            float u = Float.intBitsToFloat(data[offset + 4]);
            float v = Float.intBitsToFloat(data[offset + 5]);
            pushRaw(vertices, x, y, z, u, v, tint[0] * light[vertexIndex], tint[1] * light[vertexIndex], tint[2] * light[vertexIndex], alpha);
        }
        return true;
    }

    private float[] resolveTint(World world, BlockPos pos, IBlockState state, BakedQuad quad) {
        if (quad == null || !quad.hasTintIndex()) return new float[]{1.0f, 1.0f, 1.0f};
        int color;
        try {
            color = state.getBlock().colorMultiplier(world, pos, quad.getTintIndex());
        } catch (Throwable ignored) {
            color = -1;
        }
        if (color == -1) return new float[]{1.0f, 1.0f, 1.0f};
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f
        };
    }

    private void emitBox(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, TextureLookup.Pass pass) {
        float minX = 0.0f;
        float minY = 0.0f;
        float minZ = 0.0f;
        float maxX = 1.0f;
        float maxY = 1.0f;
        float maxZ = 1.0f;

        if (ArgentumConfig.enableModelFallback) {
            try {
                block.setBlockBoundsBasedOnState(world, pos);
                minX = (float) block.getBlockBoundsMinX();
                minY = (float) block.getBlockBoundsMinY();
                minZ = (float) block.getBlockBoundsMinZ();
                maxX = (float) block.getBlockBoundsMaxX();
                maxY = (float) block.getBlockBoundsMaxY();
                maxZ = (float) block.getBlockBoundsMaxZ();
            } catch (Throwable ignored) {
                minX = minY = minZ = 0.0f;
                maxX = maxY = maxZ = 1.0f;
            }
        }

        for (EnumFacing face : EnumFacing.values()) {
            if (isFaceVisible(world, pos, block, face)) {
                emitFace(world, vertices, pos, state, block, face, pass, minX, minY, minZ, maxX, maxY, maxZ);
            }
        }
    }

    private void emitCross(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, TextureLookup.Pass pass) {
        TextureLookup.TerrainFaceData faceData = textureLookup.resolveFace(world, pos, state, EnumFacing.UP);
        float[] lightA = lightingLookup.sampleVertexLight(world, pos, EnumFacing.UP, block, textureLookup.isEmissive(block));
        float alpha = textureLookup.getAlpha(block, pass);
        float x0 = pos.getX() + 0.15f;
        float x1 = pos.getX() + 0.85f;
        float z0 = pos.getZ() + 0.15f;
        float z1 = pos.getZ() + 0.85f;
        float y0 = pos.getY();
        float y1 = pos.getY() + 1.0f;

        quad(vertices, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, faceData, lightA, alpha);
        quad(vertices, x1, y0, z0, x0, y0, z1, x0, y1, z1, x1, y1, z0, faceData, lightA, alpha);
    }

    private void emitFluid(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, TextureLookup.Pass pass) {
        Material fluidMaterial = block.getMaterial();
        float hNW = fluidCornerHeight(world, pos, -1, -1, fluidMaterial);
        float hNE = fluidCornerHeight(world, pos, 1, -1, fluidMaterial);
        float hSE = fluidCornerHeight(world, pos, 1, 1, fluidMaterial);
        float hSW = fluidCornerHeight(world, pos, -1, 1, fluidMaterial);

        if (isFluidFaceVisible(world, pos, state, block, EnumFacing.UP, fluidMaterial)) {
            TextureLookup.TerrainFaceData uv = textureLookup.resolveFace(world, pos, state, EnumFacing.UP);
            float[] light = lightingLookup.sampleVertexLight(world, pos, EnumFacing.UP, block, textureLookup.isEmissive(block));
            float a = textureLookup.getAlpha(block, pass);
            float x = pos.getX();
            float z = pos.getZ();
            quad(vertices, x, pos.getY() + hSW, z + 1.0f, x + 1.0f, pos.getY() + hSE, z + 1.0f,
                    x + 1.0f, pos.getY() + hNE, z, x, pos.getY() + hNW, z, uv, light, a);
        }

        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            if (!isFluidFaceVisible(world, pos, state, block, side, fluidMaterial)) continue;
            emitFluidSide(world, vertices, pos, state, block, side, pass, hNW, hNE, hSE, hSW);
        }

        if (isFluidFaceVisible(world, pos, state, block, EnumFacing.DOWN, fluidMaterial)) {
            TextureLookup.TerrainFaceData uv = textureLookup.resolveFace(world, pos, state, EnumFacing.DOWN);
            float[] light = lightingLookup.sampleVertexLight(world, pos, EnumFacing.DOWN, block, textureLookup.isEmissive(block));
            float a = textureLookup.getAlpha(block, pass);
            float x = pos.getX();
            float y = pos.getY();
            float z = pos.getZ();
            quad(vertices, x, y, z, x + 1.0f, y, z, x + 1.0f, y, z + 1.0f, x, y, z + 1.0f, uv, light, a);
        }
    }

    private void emitFluidSide(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, EnumFacing side,
                               TextureLookup.Pass pass,
                               float hNW, float hNE, float hSE, float hSW) {
        TextureLookup.TerrainFaceData uv = textureLookup.resolveFace(world, pos, state, side);
        float[] light = lightingLookup.sampleVertexLight(world, pos, side, block, textureLookup.isEmissive(block));
        float a = textureLookup.getAlpha(block, pass);
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();

        if (side == EnumFacing.NORTH) {
            quad(vertices, x, y, z, x + 1.0f, y, z, x + 1.0f, y + hNE, z, x, y + hNW, z, uv, light, a);
        } else if (side == EnumFacing.SOUTH) {
            quad(vertices, x + 1.0f, y, z + 1.0f, x, y, z + 1.0f, x, y + hSW, z + 1.0f, x + 1.0f, y + hSE, z + 1.0f, uv, light, a);
        } else if (side == EnumFacing.WEST) {
            quad(vertices, x, y, z + 1.0f, x, y, z, x, y + hNW, z, x, y + hSW, z + 1.0f, uv, light, a);
        } else if (side == EnumFacing.EAST) {
            quad(vertices, x + 1.0f, y, z, x + 1.0f, y, z + 1.0f, x + 1.0f, y + hSE, z + 1.0f, x + 1.0f, y + hNE, z, uv, light, a);
        }
    }

    private float fluidCornerHeight(World world, BlockPos pos, int dx, int dz, Material fluidMaterial) {
        int samples = 0;
        float total = 0.0f;
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                BlockPos samplePos = pos.add(dx + ox, 0, dz + oz);
                IBlockState sample = world.getBlockState(samplePos);
                Block block = sample.getBlock();
                Block above = world.getBlockState(samplePos.up()).getBlock();
                if (isSameFluidMaterial(above, fluidMaterial)) {
                    return 1.0f;
                }
                if (isSameFluidMaterial(block, fluidMaterial)) {
                    int level = getFluidRenderLevel(sample);
                    total += MeshMath.fluidHeightForLevel(level);
                    samples++;
                }
            }
        }
        if (samples == 0) return 1.0f;
        return Math.max(0.1f, MeshMath.clamp01(total / samples));
    }

    private int getFluidRenderLevel(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLiquid && state != null) {
            try {
                int level = block.getMetaFromState(state);
                if (level >= 8) level = 0;
                return Math.max(0, Math.min(7, level));
            } catch (Throwable ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isFaceVisible(World world, BlockPos pos, Block source, EnumFacing side) {
        BlockPos nPos = pos.offset(side);
        Block neighbor = world.getBlockState(nPos).getBlock();
        if (neighbor == null || neighbor == Blocks.air) return true;

        if (!source.shouldSideBeRendered(world, nPos, side)) return false;

        TextureLookup.Pass srcPass = textureLookup.classifyPass(source);
        TextureLookup.Pass dstPass = textureLookup.classifyPass(neighbor);

        if (srcPass == TextureLookup.Pass.SOLID && dstPass == TextureLookup.Pass.SOLID && neighbor.isOpaqueCube()) return false;
        if (srcPass == TextureLookup.Pass.TRANSLUCENT && dstPass == TextureLookup.Pass.TRANSLUCENT) {
            if (source == neighbor) return false;
            if (source.getMaterial().isLiquid() && source.getMaterial() == neighbor.getMaterial()) return false;
        }
        if (srcPass == TextureLookup.Pass.CUTOUT && dstPass == TextureLookup.Pass.CUTOUT && source == neighbor) return false;
        return true;
    }

    private boolean isFluidFaceVisible(World world, BlockPos pos, IBlockState state, Block source, EnumFacing side, Material fluidMaterial) {
        BlockPos neighborPos = pos.offset(side);
        IBlockState neighborState = world.getBlockState(neighborPos);
        Block neighbor = neighborState.getBlock();
        if (neighbor == null || neighbor == Blocks.air) return true;
        if (isSameFluidMaterial(neighbor, fluidMaterial)) {
            if (side == EnumFacing.UP || side == EnumFacing.DOWN) return false;
            int sourceLevel = getFluidRenderLevel(state);
            int neighborLevel = getFluidRenderLevel(neighborState);
            return neighborLevel > sourceLevel;
        }
        return source.shouldSideBeRendered(world, neighborPos, side);
    }

    private boolean isSameFluidMaterial(Block block, Material material) {
        return block != null && block.getMaterial().isLiquid() && block.getMaterial() == material;
    }

    private void emitFace(World world, List<Float> vertices, BlockPos pos, IBlockState state, Block block, EnumFacing face,
                          TextureLookup.Pass pass, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        TextureLookup.TerrainFaceData faceData = textureLookup.resolveFace(world, pos, state, face);
        float[] light = lightingLookup.sampleVertexLight(world, pos, face, block, textureLookup.isEmissive(block));
        float alpha = textureLookup.getAlpha(block, pass);

        float x0 = pos.getX() + minX;
        float x1 = pos.getX() + maxX;
        float y0 = pos.getY() + minY;
        float y1 = pos.getY() + maxY;
        float z0 = pos.getZ() + minZ;
        float z1 = pos.getZ() + maxZ;

        switch (face) {
            case EAST:
                quad(vertices, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, faceData, light, alpha);
                return;
            case WEST:
                quad(vertices, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, faceData, light, alpha);
                return;
            case UP:
                quad(vertices, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, faceData, light, alpha);
                return;
            case DOWN:
                quad(vertices, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, faceData, light, alpha);
                return;
            case SOUTH:
                quad(vertices, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, faceData, light, alpha);
                return;
            case NORTH:
            default:
                quad(vertices, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, faceData, light, alpha);
        }
    }

    private void quad(List<Float> vertices,
                      float ax, float ay, float az,
                      float bx, float by, float bz,
                      float cx, float cy, float cz,
                      float dx, float dy, float dz,
                      TextureLookup.TerrainFaceData uv,
                      float[] light,
                      float alpha) {
        push(vertices, ax, ay, az, uv.u0, uv.v1, uv, light[0], alpha);
        push(vertices, bx, by, bz, uv.u1, uv.v1, uv, light[1], alpha);
        push(vertices, cx, cy, cz, uv.u1, uv.v0, uv, light[2], alpha);
        push(vertices, ax, ay, az, uv.u0, uv.v1, uv, light[0], alpha);
        push(vertices, cx, cy, cz, uv.u1, uv.v0, uv, light[2], alpha);
        push(vertices, dx, dy, dz, uv.u0, uv.v0, uv, light[3], alpha);
    }

    private void push(List<Float> values, float x, float y, float z, float u, float v,
                      TextureLookup.TerrainFaceData uv, float light, float alpha) {
        pushRaw(values, x, y, z, u, v, uv.r * light, uv.g * light, uv.b * light, alpha);
    }

    private void pushRaw(List<Float> values, float x, float y, float z, float u, float v,
                         float r, float g, float b, float alpha) {
        values.add(x);
        values.add(y);
        values.add(z);
        values.add(u);
        values.add(v);
        values.add(clamp(r));
        values.add(clamp(g));
        values.add(clamp(b));
        values.add(alpha);
    }

    private float clamp(float value) {
        return MeshMath.clamp01(value);
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    public static final class MeshData {
        private final float[] solidVertices;
        private final float[] cutoutVertices;
        private final float[] translucentVertices;

        public MeshData(float[] solidVertices, float[] cutoutVertices, float[] translucentVertices) {
            this.solidVertices = solidVertices;
            this.cutoutVertices = cutoutVertices;
            this.translucentVertices = translucentVertices;
        }

        public float[] getSolidVertices() { return solidVertices; }
        public float[] getCutoutVertices() { return cutoutVertices; }
        public float[] getTranslucentVertices() { return translucentVertices; }
    }
}

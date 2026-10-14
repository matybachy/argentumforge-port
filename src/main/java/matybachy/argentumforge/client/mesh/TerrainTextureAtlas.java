package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.client.compat.TextureLookup;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;

import java.util.List;

public final class TerrainTextureAtlas implements TextureLookup {
    private static final TerrainTextureAtlas INSTANCE = new TerrainTextureAtlas();

    private TerrainTextureAtlas() {
    }

    public static TerrainTextureAtlas getInstance() {
        return INSTANCE;
    }

    public static void bindTerrainTextureAtlas() {
        INSTANCE.bindTerrainAtlas();
    }

    @Override
    public void bindTerrainAtlas() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getTextureManager() != null) {
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        }
    }

    @Override
    public TerrainFaceData resolveFace(World world, BlockPos pos, IBlockState state, EnumFacing face) {
        TextureAtlasSprite sprite = resolveSprite(world, pos, state, face);
        if (sprite == null) {
            return new TerrainFaceData(0.0f, 0.0f, 1.0f, 1.0f, 0.8f, 0.8f, 0.8f);
        }

        float spanU = sprite.getMaxU() - sprite.getMinU();
        float spanV = sprite.getMaxV() - sprite.getMinV();
        float epsilonU = MeshMath.uvInset(sprite.getMinU(), sprite.getMaxU());
        float epsilonV = MeshMath.uvInset(sprite.getMinV(), sprite.getMaxV());

        int color = state.getBlock().colorMultiplier(world, pos, 0);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (color == -1) {
            r = g = b = 1.0f;
        }

        return new TerrainFaceData(
                sprite.getMinU() + epsilonU,
                sprite.getMinV() + epsilonV,
                sprite.getMaxU() - epsilonU,
                sprite.getMaxV() - epsilonV,
                r,
                g,
                b
        );
    }

    @Override
    public Pass classifyPass(Block block) {
        if (block == null) return Pass.SOLID;
        EnumWorldBlockLayer layer = block.getBlockLayer();
        if (layer == EnumWorldBlockLayer.TRANSLUCENT) return Pass.TRANSLUCENT;
        if (layer == EnumWorldBlockLayer.CUTOUT || layer == EnumWorldBlockLayer.CUTOUT_MIPPED) return Pass.CUTOUT;
        return Pass.SOLID;
    }

    @Override
    public float getAlpha(Block block, Pass pass) {
        if (pass == Pass.TRANSLUCENT) return 0.7f;
        if (pass == Pass.CUTOUT) return 0.95f;
        return 1.0f;
    }

    @Override
    public boolean isEmissive(Block block) {
        return block != null && block.getLightValue() > 0;
    }

    public static boolean isTransparent(Block block) {
        return INSTANCE.classifyPass(block) == Pass.TRANSLUCENT;
    }

    public static boolean isCutout(Block block) {
        return INSTANCE.classifyPass(block) == Pass.CUTOUT;
    }

    public static FaceUV getFaceUVs(World world, BlockPos pos, IBlockState state, EnumFacing face) {
        TerrainFaceData faceData = INSTANCE.resolveFace(world, pos, state, face);
        return new FaceUV(faceData.u0, faceData.v0, faceData.u1, faceData.v1);
    }

    public static float[] getFaceColor(World world, BlockPos pos, IBlockState state, boolean fallback) {
        TerrainFaceData faceData = INSTANCE.resolveFace(world, pos, state, EnumFacing.UP);
        if (fallback && (faceData.r + faceData.g + faceData.b) < 0.15f) {
            return new float[]{0.8f, 0.8f, 0.8f};
        }
        return new float[]{faceData.r, faceData.g, faceData.b};
    }

    public static float getAlpha(Block block, boolean transparent) {
        return INSTANCE.getAlpha(block, transparent ? Pass.TRANSLUCENT : Pass.SOLID);
    }

    private TextureAtlasSprite resolveSprite(World world, BlockPos pos, IBlockState state, EnumFacing face) {
        if (state == null || state.getBlock() == null) return null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return null;
        try {
            BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
            if (dispatcher == null || dispatcher.getBlockModelShapes() == null) return fallbackSprite();
            TextureAtlasSprite missing = fallbackSprite();

            TextureAtlasSprite stateTexture = dispatcher.getBlockModelShapes().getTexture(state);
            if (isUsableSprite(stateTexture, missing)) {
                return stateTexture;
            }

            IBakedModel model = dispatcher.getModelFromBlockState(state, world, pos);
            TextureAtlasSprite modelSprite = resolveModelSprite(model, face, missing);
            if (isUsableSprite(modelSprite, missing)) {
                return modelSprite;
            }

            IBlockState defaultState = state.getBlock().getDefaultState();
            if (defaultState != null && defaultState != state) {
                TextureAtlasSprite defaultTexture = dispatcher.getBlockModelShapes().getTexture(defaultState);
                if (isUsableSprite(defaultTexture, missing)) {
                    return defaultTexture;
                }
            }
            return missing;
        } catch (Throwable ignored) {
            return fallbackSprite();
        }
    }

    private TextureAtlasSprite resolveModelSprite(IBakedModel model, EnumFacing face, TextureAtlasSprite missing) {
        if (model == null) return missing;
        try {
            List<BakedQuad> faceQuads = model.getFaceQuads(face);
            if (faceQuads != null && !faceQuads.isEmpty()) {
                TextureAtlasSprite particle = model.getParticleTexture();
                if (isUsableSprite(particle, missing)) return particle;
            }
            List<BakedQuad> generalQuads = model.getGeneralQuads();
            if (generalQuads != null && !generalQuads.isEmpty()) {
                TextureAtlasSprite particle = model.getParticleTexture();
                if (isUsableSprite(particle, missing)) return particle;
            }
            TextureAtlasSprite particle = model.getParticleTexture();
            if (isUsableSprite(particle, missing)) return particle;
        } catch (Throwable ignored) {
            return missing;
        }
        return missing;
    }

    private boolean isUsableSprite(TextureAtlasSprite sprite, TextureAtlasSprite missing) {
        return sprite != null && sprite != missing;
    }

    private TextureAtlasSprite fallbackSprite() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureMapBlocks() == null) return null;
        return mc.getTextureMapBlocks().getMissingSprite();
    }

    public static final class FaceUV {
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public FaceUV(float u0, float v0, float u1, float v1) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }
}

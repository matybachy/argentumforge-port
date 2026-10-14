# ArgentumForge

Experimental Forge 1.8.9 rendering optimization port of Argentum.

## Scope & Implementation
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Asynchronous multi-threaded chunk snapshot & meshing architecture (`ChunkSnapshot`, `ChunkMeshManager`, `ExecutorService` thread pool, revision tracking, and main-thread GPU VBO uploads)
- Primitive float buffer allocation (`FloatArrayList`) eliminating boxed Float object GC overhead
- Lightmap UV coordinate generation and sampling (11-float vertex format: position x,y,z, texture u,v, lightmap u,v, RGBA)
- TileEntity distance and invalidation culling
- Living entity distance and bounding box frustum culling
- Item render distance adjustments and particle throttling
- Weather/rain/snow optimizations and flat cloud fallback
- State optimization helpers for Font/Text glyph rendering and Enchantment Glint passes (`FontAndGlintOptimization`)
- Framebuffer post-effect overlay pass
- FeatherOpt coexistence source-level guards

## Repository status
This repository is an experimental Forge 1.8.9 port, not full upstream Argentum/Celeritas parity. Upstream repositories are used as read-only references.

## Configuration
The generated Forge config controls renderer behavior. Relevant settings include:

- Renderer core: `enableExperimentalRenderer`, `renderChunkRadius`, `rebuildBudgetPerTick`
- Terrain: `enableLightingAo`, `enableTransparency`, `enableModelFallback`
- Optional effects: `enableOptionalEffects`, `optionalEffectsCompositeAlpha`, `optionalEffectsTintRed`, `optionalEffectsTintGreen`, `optionalEffectsTintBlue`
- Entities: `enableEntityHooks`, `enableEntityDistanceCulling`, `entityCullingDistance`
- Tile Entities: `enableTileEntityHooks`, `enableTileEntityCulling`, `tileEntityCullingDistance`
- Dropped items: `enableItemHooks`, `enableItemDistanceCulling`, `itemCullingDistance`
- Particles: `enableParticleHooks`, `particleThrottleSetting`
- Clouds: `enableCloudHooks`, `enableCloudRenderer`, `cloudRenderRadiusBlocks`
- Weather: `enableWeatherHooks`, `weatherRenderRadiusBlocks`, `weatherFrustumMinDotPercent`

Optional features use conservative settings and remain disabled by default unless explicitly enabled.

## Mandatory Note on Builds & Testing
In accordance with instructions, **no Gradle commands, Java compilation, unit tests, integration tests, or Minecraft client launches were performed**. All changes and build configurations were inspected and verified statically.

# ArgentumForge Porting Status

## Scope

This repository targets Minecraft Forge 1.8.9, Forge 11.15.1.2318, Java 8, and LWJGL 2. It is an experimental port, not a complete Argentum or Celeritas renderer. The source has no Mixin configuration, coremod entry point, access transformer, or replacement for `RenderGlobal`'s chunk compilation and draw path.

## Feature inventory

| Area | Source status | Current limit |
| --- | --- | --- |
| Client lifecycle | Connected | `ClientProxy` registers the client tick, world hooks, renderer, and optional render hooks. |
| Terrain and chunks | Late overlay pass | Meshes are built via background worker snapshots, uploaded to three VBOs, and drawn from `RenderWorldLastEvent`. Vanilla terrain has already rendered; this is not a full vanilla terrain renderer replacement. It can duplicate terrain, and the option is disabled by default. |
| Block models & Primitive Buffers | Connected | Quads use primitive `FloatArrayList` buffers without Java Float object GC overhead. Baked quads are read directly with model fallbacks. |
| Lightmaps & Lighting | Connected | Extended vertex format (11 floats: position, texture UV, lightmap UV, RGBA) with block and sky lightmap sampling in `GlVbo`, `BlockFaceMesher`, and `TerrainLighting`. |
| Mesh scheduling & Async Workers | Async multi-threaded worker pool | `ChunkSnapshot` captures block states, light levels, and biomes. `ChunkMeshManager` uses an `ExecutorService` thread pool for async meshing with revision tracking and render-thread GPU uploads. |
| Chunk visibility | Connected to overlay | Uses Forge 1.8.9's `Frustum` with world-space chunk bounds, plus a configurable chunk-radius cap. |
| Entity distance & Frustum culling | Connected, opt-in | `RenderLivingEvent.Pre` cancels distant living entities and includes bounding-box checks. Skips custom renderers, custom-named, mounted, riding, and leashed entities. |
| TileEntity culling | Connected, opt-in | `OptionalRenderHooks` checks TileEntity distance thresholds and invalidation states. |
| Dropped items | Connected, opt-in | `EntityItem.renderDistanceWeight` is adjusted past threshold and restored. |
| Particles | Connected, opt-in | Throttles particle settings with user setting preservation. |
| Weather & Clouds | Connected, opt-in fallback | Optimized rain/snow weather renderer and flat cloud layer fallback when vanilla clouds are disabled. |
| Fonts, HUD & Glint | Connected helpers | `FontAndGlintOptimization` provides OpenGL state optimization helpers for font rendering and enchantment glint passes. |
| Post effects | Connected, opt-in overlay | The framebuffer pass wraps the late custom pass. |
| FeatherOpt coexistence | Source-level guards | ArgentumForge has no FeatherOpt dependency and does not bundle its source. |

## Build configuration inspection

The Gradle configuration in `build.gradle` was corrected statically to declare `1.8.9-11.15.1.2318-1.8.9` for ForgeGradle 2.1 artifact resolution compatibility without executing Gradle.

## Validation

No Gradle command, Java compilation, unit/integration test, Minecraft launch, or runtime compatibility test was performed. Source-level review is not evidence that this code compiles or renders correctly.

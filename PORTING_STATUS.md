# ArgentumForge Porting Status

## Scope

This repository targets Minecraft Forge 1.8.9, Forge 11.15.1.2318, Java 8, and LWJGL 2. It is an experimental port, not a complete Argentum or Celeritas renderer. The source has no Mixin configuration, coremod entry point, access transformer, or replacement for `RenderGlobal`'s chunk compilation and draw path.

## Feature inventory

| Area | Source status | Current limit |
| --- | --- | --- |
| Client lifecycle | Connected | `ClientProxy` registers the client tick, world hooks, renderer, and optional render hooks. |
| Terrain and chunks | Prototype overlay | Meshes are made from live `World` state on the client tick, uploaded to three VBOs, and drawn from `RenderWorldLastEvent`. Vanilla terrain has already rendered; this is not a replacement or a terrain performance optimization. It can duplicate terrain, and the option is disabled by default. |
| Block models | Partial | Baked quads are read directly, with approximate fallback boxes and crosses. This does not follow Forge's full block-renderer pipeline for every modded block. |
| Fluids and lighting | Partial | Fluid geometry, biome tint, block/sky light, and ambient occlusion are approximations. The vertex format has no lightmap coordinates. |
| Mesh scheduling | Synchronous, bounded | Rebuilds run on the client tick and touch mutable world state. The radius, per-tick budget, retry delay, loaded-chunk filter, dirty queue pruning, and VBO cleanup are managed. There are no worker threads or immutable chunk snapshots. |
| Chunk visibility | Connected to overlay | Uses Forge 1.8.9's `Frustum` with world-space chunk bounds, plus a configurable chunk-radius cap. If frustum capture fails, it fails open to the radius filter. It does not control vanilla chunk visibility and has no section visibility graph or occlusion query. |
| Invalidation | Partial | Chunk load/unload, world changes, texture-atlas restitch, and Forge block place/break/multi-place events invalidate meshes. Changes made by arbitrary mod code without these events are not observed. |
| Entity distance culling | Connected, opt-in | `RenderLivingEvent.Pre` can cancel distant vanilla living-entity renders. It skips custom renderers, custom-named, mounted, riding, and leashed entities. It is disabled by default. |
| Dropped items | Connected, opt-in | `EntityItem.renderDistanceWeight` is adjusted past the configured threshold and restored. Minecraft's own range calculation still determines the effective cutoff; no item model cache or draw batching is present. |
| Particles | Connected, opt-in | The particle setting is temporarily throttled and restored, including ordinary setting changes observed while the hook is active. This is not particle visibility culling. |
| Block entities | Not implemented | There is no connected block-entity optimization. The old capability probe did not find a Forge 1.8.9 render event and has been removed. Renderers remain on vanilla behavior. |
| Weather | Connected, opt-in approximation | The provider renderer is installed only if no other renderer is present, uses biome and precipitation-height checks, and is removed on disable/world changes or render failure. Rain/snow visuals and visibility are not vanilla-equivalent. |
| Clouds | Connected, opt-in fallback | A flat translucent grid is drawn only with vanilla clouds disabled and no custom sky renderer. It does not reproduce vanilla cloud animation or geometry. |
| Post effects | Connected, opt-in overlay | The framebuffer pass wraps only the late custom pass. It does not capture the complete vanilla world frame or implement a shader chain. FeatherOpt presence disables it by default. |
| Items, fonts, labels, GUI/HUD, enchantment glint | Not implemented | No connected optimization hooks or caches are present for these paths. |
| FeatherOpt coexistence | Source-level precautions only | ArgentumForge has no FeatherOpt dependency and does not bundle its source. Renderer/framebuffer interaction has not been runtime-verified. |

`WaterMesher`, `TerrainTracker`, `RenderLayer`, and several compatibility helpers are not connected to the active draw path. Their presence is not counted as a shipped optimization.

## Source changes in this revision

- Added the missing item-culling configuration declarations referenced by `OptionalRenderHooks`; both item options remain disabled by default.
- Removed the disconnected block-entity hook setting and reflection probe rather than presenting it as supported.
- Restore item render-distance values when switching client worlds.
- Keep a previous mesh until its replacement is built successfully. If an experimental rebuild throws a runtime exception, discard that chunk's custom overlay, log the failure, and defer retry for 20 client ticks so vanilla terrain remains visible.
- Only queue loaded chunks, prioritize nearby dirty chunks, and prune pending/retry entries outside the active radius plus one chunk.
- Dispose meshes after block-atlas restitch so stored UVs are rebuilt for the new atlas.
- Restore VBO bindings and client vertex-array state around upload/draw calls; include texture/current-color state in the renderer's state stack.
- Interpolate the camera position for the late-pass transform and remove the second translation from `ChunkMeshManager`.
- Capture Forge 1.8.9 camera planes before the overlay transform and test full chunk bounds against them; retain a radius-only fail-open path.

These changes improve the safety and lifecycle of the opt-in overlay. They do not turn it into a replacement chunk renderer.

## Upstream review and reuse

- Argentum describes itself as an Ornithe 1.8.9 client mod built on Celeritas. Its source uses Mixin extensions to replace renderer entry points, section build tasks, cloned chunk data, and separate render systems. Its README states that Argentum is All Rights Reserved while its bundled Celeritas jar is LGPL-3.0.
- Celeritas documents an LGPL-3.0 license and a shared rendering core with version-specific adapters. Its Forge 1.12.2 path uses a `WorldSlice`/cloned block-access context and chunk build tasks. That API and runtime architecture do not match standard Forge 1.8.9.
- FeatherOpt targets Forge 1.8.9 through an FML core plugin, LaunchWrapper/Mixin bootstrap, access transformer, and mixin configuration. Its inspected mixins include `RenderGlobal.setupTerrain` and block-model/render helpers. Its repository includes LGPL-2.1 text.
- No upstream implementation code was copied into this revision. The current ArgentumForge repository has no root license file; no license was inferred for it.

## Not completed

A correct terrain replacement would need an integration point inside the real 1.8.9 chunk build, scheduling, and `RenderGlobal` layer draw path. Standard Forge events used here cannot cancel or replace that path. Adding a coremod/Mixin transformer would change the current packaging and introduce concrete interaction risks with FeatherOpt's `RenderGlobal` and block-model transformations; without a verified target/API and a runtime comparison, a speculative transformer would not be a safe implementation.

Background building is also not implemented: current meshing reads mutable `World` and baked-model data synchronously. A worker port needs captured chunk/light/biome/model inputs, cancellation and revision checks, and render-thread-only uploads. Exact translucent sorting, vanilla ambient occlusion/lightmap behavior, modded block renderers, block-entity culling, fonts/HUD/glint, GPU occlusion, and full weather/cloud parity remain open.

## Validation

No Gradle command, Java compilation, unit/integration test, Minecraft launch, or runtime compatibility test was performed. Source-level review is not evidence that this code compiles or renders correctly. FeatherOpt-only and FeatherOpt-plus-OptiFine compatibility are unverified.


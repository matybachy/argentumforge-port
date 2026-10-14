# ArgentumForge Technical Notes

## Target and entry points

- Minecraft 1.8.9, Forge 11.15.1.2318, Java 8, LWJGL 2.
- `ArgentumForge` loads the config and delegates to the sided proxy.
- `ClientProxy` registers itself and `ClientWorldHooks` on `MinecraftForge.EVENT_BUS`, enables/disables the shared manager, and calls its tick scheduler.
- `ArgentumRenderer` registers `RenderWorldLastEvent` and `OptionalRenderHooks`. The experimental terrain flag defaults off.
- No Mixin JSON, coremod manifest entry, access transformer, `IClassTransformer`, or RenderGlobal replacement exists in this project.

## Terrain path as written

`ClientProxy.onClientTick` calls `ChunkMeshManager.onTick`. For loaded nearby chunks, the manager calls `ChunkMesh.build`, which runs `BlockFaceMesher.buildChunkMesh` against the live `World`, allocates Java float lists, and uploads solid, cutout, and translucent arrays to three LWJGL 2 VBOs on that same client tick. Dirty chunks are prioritized by horizontal distance. There are no worker threads, chunk snapshots, cancellation tokens, or stale-result revision checks.

At draw time, `ArgentumRenderer.onRenderWorldLast` binds the block atlas and draws the meshes. Forge's `RenderWorldLastEvent` is not a terrain-layer replacement hook, and the event is not cancelable. Vanilla `RenderGlobal` remains in control of chunk compilation and terrain drawing. The current pass is therefore an optional late overlay. The custom path can repeat vanilla geometry and has no demonstrated frame-time benefit.

The mesh builder uses 1.8.9 baked-model quads when available, but bypasses the normal Forge block-model render pipeline. Its fallback bounds/cross/fluid generation, per-vertex brightness and AO, biome tint handling, layer classification, and translucent sorting do not reproduce all vanilla/modded rendering behavior. The vertex record contains position, atlas UV, and RGBA only; it does not carry the vanilla lightmap coordinates. Animated atlas textures can keep their UVs, but resource atlas restitch invalidates and discards all meshes so they are rebuilt.

Before applying the overlay camera translation, `ArgentumRenderer` asks `SimpleFrustumCuller` to capture Forge 1.8.9's current `Frustum`. The culler checks each world-space 16-by-256-by-16 chunk bound against those camera planes and also applies the configured radius cap. If plane capture or testing throws, it fails open to radius-only visibility. This culls only the overlay's draw calls; vanilla chunk visibility is unchanged, and there is no section-visibility graph or occlusion query. Translucent geometry is sorted only by chunk center; quads within a chunk are not sorted.

## Lifecycle and state safety

- Chunk/world load and unload plus block place/break/multi-place events feed invalidation. Arbitrary direct mod changes without these Forge events can still leave a stale overlay.
- Rebuilds only consider loaded chunks, retain the previous mesh until a replacement is ready, and dispose a failed/stale overlay before a delayed retry. Dirty/retry coordinates outside active radius plus one chunk are pruned.
- Block texture-atlas restitch disposes meshes so their UVs cannot survive a resource reload.
- `GlVbo` restores the previous array-buffer binding on upload and draw, and saves/restores legacy client vertex-array state around draw calls.
- `GLState` pushes enable, blend/depth, texture, and current-color state. This is still a compatibility-sensitive OpenGL path and has not been exercised with other render mods.
- No GPU resource destruction is scheduled from a worker because this implementation has no mesh worker.

## Other connected hooks

- Living entity culling listens to cancelable Forge `RenderLivingEvent.Pre`, but is opt-in and limited to eligible vanilla renderers. Custom renderers and entities with names, mounts, riders, or leashes are retained.
- Item culling adjusts `EntityItem.renderDistanceWeight` after a configurable threshold. It is opt-in and restored on world changes or disable; the final range still follows Minecraft's own entity range formula.
- Particle throttling temporarily changes `GameSettings.particleSetting` and restores the captured setting. It does not batch or cull individual particles.
- Weather uses `WorldProvider.setWeatherRenderer` only while the provider has no renderer, then returns to the provider's null/vanilla path when disabled or on world change. It draws an approximation each frame; it is not a cached weather VBO renderer.
- The optional cloud path draws a flat grid from `RenderWorldLastEvent`; it is not vanilla cloud rendering.
- The optional framebuffer begins in the late event, after the vanilla world pass. It composites the custom overlay only, so the feature is not a whole-scene post-processing pass.
- No code is registered for tile-entity, item-model, font, name-tag, GUI/HUD, or enchantment-glint batching/caching. A prior reflective check for `RenderTileEntityEvent.Pre` was removed because the standard Forge 1.8.9 source tree has no such event.

## Upstream architecture and compatibility

The inspected Argentum README identifies its renderer as an Ornithe 1.8.9 implementation based on Celeritas and describes its own source as All Rights Reserved; it separately identifies the bundled Celeritas jar as LGPL-3.0. Its renderer is installed through Mixin extensions and section build tasks, not a Forge render-last callback.

Celeritas describes its reusable renderer core as LGPL-3.0 and its Forge 1.12.2 support as a version-specific path. The inspected Celeritas path builds from captured `WorldSlice`/cloned block data with task outputs and section management. Those APIs cannot be dropped into standard Forge 1.8.9 directly. This project contains no copied upstream implementation code.

FeatherOpt's inspected source has a separate `IFMLLoadingPlugin` coremod entry, initializes Mixin, registers `mixins.featheropt.json`, and configures an access transformer. Its mixins touch `RenderGlobal.setupTerrain`, vanilla/Forge block-model render helpers, and several client structures. ArgentumForge avoids a hard dependency and does not alter FeatherOpt artifacts/configuration. Its current framebuffer guard is only a defensive source-level precaution; interaction between the custom overlay and FeatherOpt transformations has not been tested.

## Remaining work

A real terrain optimization needs an integration layer inside 1.8.9 chunk compilation, dirty scheduling, visibility selection, and `RenderGlobal` pass drawing, plus safe immutable data capture for worker builds and render-thread uploads. That requires a verified transformer/injection design and targeted runtime compatibility checks. This revision does not introduce a speculative coremod because the available API path is non-cancelable and FeatherOpt already transforms overlapping renderer code.

Exact block render semantics, lightmap/AO parity, biome tint parity, cutout/translucent ordering, arbitrary block updates, block-entity culling, entity/item batching, fonts/HUD/glint, and cached VBO weather/cloud rendering remain unimplemented or approximate.

## Validation record

No Gradle or wrapper command was run. No Java compilation, unit or integration test, Minecraft launch, or FeatherOpt runtime check was performed. Only source inspection and diff review were used; compilation and visual behavior are unverified.

## FeatherOpt coexistence review (static only)

FeatherOpt remains a separate optional coremod. ArgentumForge does not bundle it or add a required dependency. The renderer compatibility flag is opt-in and is intended to suppress only the experimental terrain overlay and its mesh rebuild tick. The framebuffer-effects compatibility flag is also opt-in because the inspected source did not establish a concrete conflict. This source review does not prove runtime compatibility.

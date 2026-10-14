# ArgentumForge Porting Status

## Scope

Target: Minecraft Forge 1.8.9 (Forge `1.8.9-11.15.1.2318-1.8.9`), MCP `stable_22`, Java 8, LWJGL 2. This is an experimental port, not a complete Argentum or Celeritas renderer. The project has no Mixin configuration, coremod, access transformer or `IClassTransformer`. Terrain integration replaces `RenderGlobal`'s chunk factory and render container at runtime through reflection.

**Validation status: nothing in this revision was compiled, built, unit-tested or run in Minecraft.** Every claim below comes from source inspection of this repository, the MCP 1.8.9 decompiled sources, the Forge 1.8.9 patches and userdev artifact (2318), the MCP `stable_22`/SRG mappings, and the upstream repositories named below.

## Feature matrix

Legend: **Connected** = called from a real Minecraft/Forge code path. **Partial** = connected, with documented gaps. **Disabled** = present but off by default or off by design. **Not implemented** = no source path.

| Area | Status | Source path and behavior | Remaining gaps |
| --- | --- | --- | --- |
| Master switch | Connected | `enableExperimentalRenderer` (default `false`) gates every feature. | - |
| Terrain integration (`terrainMode=integrated`) | Connected | `client/terrain/IntegratedTerrain` swaps `RenderGlobal.renderChunkFactory` (`field_175007_a`) and `renderContainer` (`field_174996_N`) for `ArgentumRenderChunkFactory` and `ArgentumVboRenderList`/`ArgentumDisplayListRenderList`, then calls `RenderGlobal.loadRenderers()`. Vanilla scheduling, worker compilation, uploads and the per-layer draws all go through these objects. No overlay is drawn. It reinstalls if a VBO toggle makes `loadRenderers` recreate vanilla objects, and restores vanilla objects when disabled or on failure. | Runtime behavior is unverified. It relies on Forge's `createRegionRenderCache` hook, which is present in the 2318 userdev patch. |
| Section compilation inputs | Partial | `SectionSnapshot` captures 20x20x20 block states, raw sky/block light, biomes and tile-entity references on the client thread, when `makeCompileTaskChunk` runs. Workers read it through `SnapshotRegionRenderCache`, so they never touch live chunk storage for in-region reads. Light logic mirrors `ChunkCache.getLightForExt`. | Tile-entity objects are shared live references, the same as vanilla. Reads outside the region (not expected) fall back to the vanilla cache and are counted. |
| Worker scheduling | Connected (vanilla) | Uses vanilla `ChunkRenderDispatcher` worker threads and its bounded queue (100 tasks). Uploads and GL deletion stay on the client thread. Stale results are rejected by vanilla task-status checks. ArgentumForge adds: snapshot paired to the exact task object, `terrainSnapshotsPerFrame` budget, a `terrainMaxPendingSnapshots` memory cap, and discard on new task, position change, `stopCompileTask` or `deleteGlResources`. Capture failures fall back per section, and snapshots are disabled after 16 failures. | No custom priority queue: vanilla's nearest-first queueing order is kept. Over-budget sections use vanilla's live-reading region cache. |
| Block meshing semantics | Connected (vanilla) | Vanilla/Forge `rebuildChunk` and `BlockRendererDispatcher` do the meshing: baked models, `ISmartBlockModel`, fluids, AO and lightmap coordinates, biome tint, `canRenderInLayer`, tile-entity collection, the visibility graph and translucent quad sorting. | Identical output to vanilla is expected but has not been verified. |
| Section visibility | Partial | Vanilla `setupTerrain` frustum and cave-culling BFS are unchanged. On top of that, `TerrainVisibilityFilter` drops a section layer when the rendered-block bounds of its compiled result (plus a 1-block margin) are outside the left/right/top/bottom planes taken from the GL matrices at `ChunkRenderContainer.initialize`. Near/far planes are ignored. It draws when it is unsure. | No GPU occlusion queries and no custom visibility graph. Plane capture assumes `setupTerrain` runs under the camera matrices (static reading of `EntityRenderer.renderWorldPass`). |
| Legacy overlay (`terrainMode=overlay`) | Disabled by default | The previous `RenderWorldLastEvent` overlay (`ChunkMeshManager`, `BlockFaceMesher`). Its meshers now use `FloatArrayList` instead of boxed `List<Float>`. | Still draws after vanilla and can duplicate terrain. Synchronous, approximate lighting and translucency. Kept only for comparison. |
| Block entities | Partial, opt-in | `BlockEntityDistanceCulling` wraps the exact vanilla chest, ender chest, sign, skull and banner renderers in `TileEntityRendererDispatcher.mapSpecialRenderers` (`field_147559_m`). Only in-world block entities of those exact classes beyond `blockEntityCullingDistance` are skipped. Uninstall restores the originals. | Modded block entities, beacons and item-form renders are untouched. No batching. |
| Entities | Partial, opt-in | `RenderLivingEvent.Pre` distance culling for conservative vanilla living entities. Package eligibility checks are now cached per class. | Non-living entities are not covered. No batching or instancing. |
| Dropped items | Partial, opt-in | Adjusts `EntityItem.renderDistanceWeight`, restored on world change or disable. | No item-model caching or batching. |
| Particles | Partial, opt-in | Raises the particle-setting floor and restores it afterwards. | No per-particle culling or batching. |
| Fonts, name tags, GUI/HUD, glint | Not implemented | - | No safe cache with complete invalidation was implemented in this pass. |
| Weather | Partial, opt-in | `OptimizedWeatherRenderer` (installed only when the provider has no weather renderer) now caches biome rain/snow type and precipitation height per column. The cache rebuilds at most once per world tick or when the camera column window moves, instead of twice per frame. It uses a `WeakReference` to the world. | Vanilla visuals are approximated. Immediate-mode drawing, no VBO. |
| Clouds | Partial, opt-in | Flat translucent grid, only when vanilla clouds are off and there is no custom sky renderer. It now runs regardless of terrain mode. | Not vanilla clouds. |
| Framebuffer effect | Disabled by default | Composites only the legacy overlay pass. Not used in integrated mode. | Not a whole-scene post-process. |
| FeatherOpt | Static review only | See below. | Runtime unverified. |
| OptiFine | Precaution | Integrated terrain does not install when OptiFine classes are present, because OptiFine replaces `RenderGlobal`/`RenderChunk` internals. Its source was not inspected. | - |
| Build configuration | Corrected statically | `minecraft.version` changed from `1.8.9-11.15.1.2318` to `1.8.9-11.15.1.2318-1.8.9`. Tracked `.gradle/` and `build/` outputs removed, and `.gitignore` added. | Not built. Gradle 2.14.1 wrapper vs. the MDK's Gradle 2.7 is untested. |

## Build coordinate finding

`https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml` lists `1.8.9-11.15.1.2318-1.8.9`. It has no `1.8.9-11.15.1.2318` entry: all later 1.8.9 builds carry the `-1.8.9` branch suffix, and only the older 1722/1761 builds do not. The official MDK zip for that version (`forge-1.8.9-11.15.1.2318-1.8.9-mdk.zip`) uses `version = "1.8.9-11.15.1.2318-1.8.9"` with ForgeGradle `2.1-SNAPSHOT` and `mappings = "stable_20"`. `de.oceanlabs.mcp:mcp_stable:22-1.8.9` also exists, so `stable_22` was kept. These are metadata findings only; Gradle was not run.

## Upstream review and licensing

- QuicksilverMC/Argentum README: "All Rights Reserved"; its bundled Celeritas jar is LGPL-3.0. Used only as a technical reference. No code copied.
- GTNewHorizons/Celeritas: LGPL-3.0 (`COPYING`, `COPYING.LESSER`). No code copied.
- FeatherMC/FeatherOpt: `LICENSE` is LGPL-2.1, plus an LGPL-3.0 text under `src/main/resources/LICENSE`. No code copied, bundled or depended on.
- The ArgentumForge repository still has no license file, and none is inferred.

## FeatherOpt compatibility review (static only)

The `mixins.featheropt.json` and `mixins.featheropt.cond.json` configurations were inspected. `FeatherOptMixinPlugin.shouldApplyMixin` returns true and `getMixins` adds only the conditional list.

- None of the registered mixins target `RenderChunk`, `ChunkRenderContainer`, `VboRenderList`, `RenderList`, `IRenderChunkFactory`, `RegionRenderCache` or `TileEntityRendererDispatcher`. `MixinRenderGlobal_*` classes exist in the source tree but are not listed in either config.
- `inline.MixinChunkCache_AvoidCopying` redirects `EnumFacing.values()` inside `ChunkCache.getLightForExt`. Snapshot-backed caches override `getCombinedLight`, so that redirect is simply not reached for those builds. Over-budget sections still use vanilla `ChunkCache` with FeatherOpt's redirect.
- `inline.MixinBlockModelRenderer_AvoidCopying` and the Forge model-pipeline mixins still apply, because meshing still uses those classes.
- ArgentumForge does not disable itself when FeatherOpt is present. `disableExperimentalRendererWithFeatherOpt` (default false) turns off only the terrain path, and `disableOptionalEffectsWithFeatherOpt` only the overlay framebuffer effect.
- This review does not prove runtime compatibility.

## Not completed

- Measured frame-time benefit, visual parity and compatibility: these need a real client.
- GPU occlusion culling, and a custom section graph or priority scheduler.
- Font/text/name-tag/HUD/glint caching, item-model caching or batching, particle culling or batching, entity batching.
- A VBO-cached weather or cloud renderer at vanilla parity.
